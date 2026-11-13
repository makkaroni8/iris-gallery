package com.iris.gallery.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Manages completely offline PMTiles map files and generates the runtime MapLibre style JSON.
 *
 * Path A: Default bundled world base map unpacked from assets/world_base.pmtiles to filesDir.
 * Path B: User imported custom detail map copied via SAF to filesDir.
 */
class OfflineMapRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val baseFile: File = File(context.filesDir, BASE_MAP_FILE_NAME)
    val customFile: File = File(context.filesDir, CUSTOM_MAP_FILE_NAME)
    private val tmpFile: File = File(context.filesDir, TMP_MAP_FILE_NAME)
    val activeStyleFile: File = File(context.filesDir, ACTIVE_STYLE_FILE_NAME)
    val activeStyleDarkFile: File = File(context.filesDir, ACTIVE_STYLE_DARK_FILE_NAME)

    /**
     * Checks if a custom detail map is currently present and non-empty.
     */
    fun isUsingCustomMap(): Boolean {
        return customFile.exists() && (customFile.length() > 0L)
    }

    /**
     * Ensures base map is extracted to filesDir, selects the appropriate map source,
     * injects it into style_template.json or style_template_dark.json and writes the active style file.
     *
     * @return The file URI pointing to the active style: "file://${targetFile.absolutePath}"
     */
    suspend fun getActiveStyleUri(isDark: Boolean = false): String = withContext(ioDispatcher) {
        ensureBaseMapCopied()

        val isCustom = isUsingCustomMap()
        val pmtilesSourceUrl = if (isCustom) {
            "pmtiles://file://${customFile.absolutePath}"
        } else {
            "pmtiles://file://${baseFile.absolutePath}"
        }

        val assetName = if (isDark) STYLE_TEMPLATE_DARK_ASSET_NAME else STYLE_TEMPLATE_ASSET_NAME
        val targetFile = if (isDark) activeStyleDarkFile else activeStyleFile

        val template = runCatching {
            context.assets.open(assetName).bufferedReader().use {
                it.readText()
            }
        }.getOrElse {
            if (isDark) {
                """{"version":8,"name":"Iris Offline Protomaps Dark","sources":{"protomaps":{"type":"vector","url":"$PLACEHOLDER_SOURCE_URL"}},"layers":[{"id":"background","type":"background","paint":{"background-color":"#131417"}}]}"""
            } else {
                """{"version":8,"name":"Iris Offline Protomaps","sources":{"protomaps":{"type":"vector","url":"$PLACEHOLDER_SOURCE_URL"}},"layers":[{"id":"background","type":"background","paint":{"background-color":"#edeae2"}}]}"""
            }
        }
        val activeStyleContent = template.replace(PLACEHOLDER_SOURCE_URL, pmtilesSourceUrl)
        targetFile.writeText(activeStyleContent)

        "file://${targetFile.absolutePath}"
    }

    /**
     * Imports a user-selected PMTiles file via Storage Access Framework (SAF).
     *
     * 1. Copies the stream into custom_map.tmp while emitting progress.
     * 2. Validates that the first 7 ASCII bytes equal "PMTiles".
     * 3. Atomically renames custom_map.tmp to custom_detail_map.pmtiles.
     * 4. Updates active_style.json so MapLibre can immediately switch to the custom map.
     */
    suspend fun importCustomPmTiles(
        uri: Uri,
        onProgress: (Float?) -> Unit,
    ): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // Determine total file size for progress reporting
            var totalSize: Long? = null
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if ((sizeIndex != -1) && !cursor.isNull(sizeIndex)) {
                            val size = cursor.getLong(sizeIndex)
                            if (size > 0L) totalSize = size
                        }
                    }
                }
            } catch (_: Exception) {
                // Size query may fail on some providers; progress will be indeterminate
            }

            if (tmpFile.exists()) {
                tmpFile.delete()
            }

            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Konnte Eingabestrom für URI nicht öffnen: $uri")

            val buffer = ByteArray(64 * 1024)
            var bytesCopied = 0L
            var lastReportedPercent = -1

            inputStream.use { input ->
                tmpFile.outputStream().use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read

                        totalSize?.let { total ->
                            val progress = (bytesCopied.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            val percent = (progress * 100).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(progress)
                            }
                        } ?: run {
                            onProgress(null)
                        }
                    }
                    output.flush()
                }
            }

            // Verify ASCII PMTiles header (first 7 bytes)
            if (tmpFile.length() < 7L) {
                tmpFile.delete()
                throw IllegalArgumentException("Die Datei ist zu klein, um eine gültige PMTiles-Datei zu sein.")
            }

            val headerBytes = ByteArray(7)
            val headerRead = FileInputStream(tmpFile).use { it.read(headerBytes) }
            val headerString = if (headerRead == 7) String(headerBytes, Charsets.US_ASCII) else ""
            if (headerString != PMTILES_MAGIC_HEADER) {
                tmpFile.delete()
                throw IllegalArgumentException("Ungültiges Dateiformat: Datei-Header entspricht nicht 'PMTiles'.")
            }

            // Atomically replace customFile with tmpFile
            try {
                Files.move(
                    tmpFile.toPath(),
                    customFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    tmpFile.toPath(),
                    customFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

            // Regenerate style file
            getActiveStyleUri()
            Unit
        }
    }

    /**
     * Deletes the imported custom map file and falls back to the bundled base map.
     */
    suspend fun deleteCustomMap(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            if (customFile.exists()) {
                customFile.delete()
            }
            if (tmpFile.exists()) {
                tmpFile.delete()
            }
            getActiveStyleUri()
            Unit
        }
    }

    /**
     * Copies the bundled world base map from assets to internal storage if not already present.
     */
    private fun ensureBaseMapCopied() {
        if (!baseFile.exists() || (baseFile.length() == 0L)) {
            runCatching {
                context.assets.open(BASE_MAP_ASSET_NAME).use { input ->
                    baseFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    companion object {
        const val BASE_MAP_ASSET_NAME = "world_base.pmtiles"
        const val STYLE_TEMPLATE_ASSET_NAME = "style_template.json"
        const val STYLE_TEMPLATE_DARK_ASSET_NAME = "style_template_dark.json"
        const val BASE_MAP_FILE_NAME = "world_base.pmtiles"
        const val CUSTOM_MAP_FILE_NAME = "custom_detail_map.pmtiles"
        const val TMP_MAP_FILE_NAME = "custom_map.tmp"
        const val ACTIVE_STYLE_FILE_NAME = "active_style.json"
        const val ACTIVE_STYLE_DARK_FILE_NAME = "active_style_dark.json"
        const val PLACEHOLDER_SOURCE_URL = "{{PMTILES_SOURCE_URL}}"
        const val PMTILES_MAGIC_HEADER = "PMTiles"
    }
}
