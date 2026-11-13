package com.iris.gallery.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaRepository
import com.iris.gallery.data.OfflineMapRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents a geotagged photo for display on the offline map.
 */
data class PhotoLocation(
    val id: Long,
    val imageUri: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * UI State for the offline photo map screen.
 */
data class PhotoMapUiState(
    val styleUri: String? = null,
    val isUsingCustomMap: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: Float? = null,
    val errorMessage: String? = null,
    val photos: List<PhotoLocation> = emptyList(),
    val isScanningGps: Boolean = false,
    val scannedCount: Int = 0,
    val totalToScan: Int = 0,
)

/**
 * ViewModel managing offline map state, map imports, base map reset, and photo markers.
 */
class PhotoMapViewModel @JvmOverloads constructor(
    application: Application,
    private val mapRepository: OfflineMapRepository = OfflineMapRepository(application),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PhotoMapUiState())
    val uiState: StateFlow<PhotoMapUiState> = _uiState.asStateFlow()

    private var currentIsDark: Boolean = false

    init {
        refreshStyle(currentIsDark)
    }

    /**
     * Initializes or refreshes the active style JSON matching light or dark mode.
     */
    fun refreshStyle(isDark: Boolean = currentIsDark) {
        currentIsDark = isDark
        viewModelScope.launch {
            try {
                val styleUri = mapRepository.getActiveStyleUri(isDark)
                val isCustom = mapRepository.isUsingCustomMap()
                _uiState.update {
                    it.copy(
                        styleUri = styleUri,
                        isUsingCustomMap = isCustom,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Fehler beim Initialisieren der Offline-Karte")
                }
            }
        }
    }

    /**
     * Sets the list of geotagged photos to be clustered and displayed on the map.
     */
    @Suppress("unused")
    fun setPhotos(photos: List<PhotoLocation>) {
        _uiState.update { it.copy(photos = photos) }
    }

    /**
     * Scans media images for GPS coordinates with persistent disk caching.
     * Only scans newly added/modified images. If already scanned, loads instantly with 0 UI delay.
     */
    fun scanMediaForLocations(context: Context, images: List<MediaImage>) {
        if (images.isEmpty()) {
            _uiState.update {
                it.copy(
                    photos = emptyList(),
                    isScanningGps = false,
                    scannedCount = 0,
                    totalToScan = 0,
                )
            }
            return
        }

        val currentSignature = images.size.toLong() xor (images.firstOrNull()?.id ?: 0L) xor (images.firstOrNull()?.dateModified ?: 0L)
        if ((lastScannedSignature == currentSignature) && hasScannedMedia) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            ensureGpsCacheLoaded(context)

            val imagesToScan = images.filter { image ->
                val cached = gpsCache[image.id]
                (cached == null) || (cached.dateModified != image.dateModified)
            }

            // If all images are already cached, load immediately without showing scan UI!
            if (imagesToScan.isEmpty()) {
                val foundLocations = images.mapNotNull { image ->
                    val cached = gpsCache[image.id]
                    if ((cached != null) && (!cached.lat.isNaN()) && (!cached.lng.isNaN())) {
                        PhotoLocation(
                            id = image.id,
                            imageUri = image.uri.toString(),
                            latitude = cached.lat,
                            longitude = cached.lng,
                        )
                    } else null
                }
                lastScannedSignature = currentSignature
                hasScannedMedia = true
                _uiState.update {
                    it.copy(
                        photos = foundLocations,
                        isScanningGps = false,
                        scannedCount = 0,
                        totalToScan = 0,
                    )
                }
                return@launch
            }

            // Only show scanning progress for new/unscanned images
            _uiState.update {
                it.copy(
                    isScanningGps = true,
                    scannedCount = 0,
                    totalToScan = imagesToScan.size,
                )
            }

            var hasNewData = false
            imagesToScan.forEachIndexed { index, image ->
                val coords = extractGpsCoordinates(context, image.uri, image.path)
                if (coords != null) {
                    gpsCache[image.id] = GpsCacheEntry(coords.first, coords.second, image.dateModified)
                } else {
                    gpsCache[image.id] = GpsCacheEntry(Double.NaN, Double.NaN, image.dateModified)
                }
                hasNewData = true

                if (((index % 5) == 0) || (index == imagesToScan.lastIndex)) {
                    _uiState.update {
                        it.copy(
                            scannedCount = index + 1,
                        )
                    }
                }
            }

            if (hasNewData) {
                saveGpsCacheToDisk(context)
            }

            val foundLocations = images.mapNotNull { image ->
                val cached = gpsCache[image.id]
                if ((cached != null) && (!cached.lat.isNaN()) && (!cached.lng.isNaN())) {
                    PhotoLocation(
                        id = image.id,
                        imageUri = image.uri.toString(),
                        latitude = cached.lat,
                        longitude = cached.lng,
                    )
                } else null
            }

            lastScannedSignature = currentSignature
            hasScannedMedia = true
            _uiState.update {
                it.copy(
                    photos = foundLocations,
                    isScanningGps = false,
                    scannedCount = imagesToScan.size,
                )
            }
        }
    }

    private fun extractGpsCoordinates(context: Context, uri: Uri, path: String?): Pair<Double, Double>? {
        val directFile = if (!path.isNullOrBlank()) File(path) else null
        val useDirectFile = (directFile != null) && directFile.exists() && directFile.canRead()
        return try {
            val exif = if (useDirectFile) {
                androidx.exifinterface.media.ExifInterface(directFile)
            } else {
                val targetUri = if ((Build.VERSION.SDK_INT >= 29) && (uri.scheme == android.content.ContentResolver.SCHEME_CONTENT)) {
                    runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
                } else uri
                context.contentResolver.openInputStream(targetUri)?.use { stream ->
                    androidx.exifinterface.media.ExifInterface(stream)
                }
            } ?: return null

            val latLong = exif.latLong ?: return null
            val lat = latLong.getOrNull(0) ?: return null
            val lng = latLong.getOrNull(1) ?: return null
            if ((lat == 0.0) && (lng == 0.0)) null else Pair(lat, lng)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Loads geotagged photos directly from the app's MediaRepository.
     */
    @Suppress("unused")
    fun loadPhotosFromRepository(mediaRepository: MediaRepository) {
        viewModelScope.launch {
            try {
                val images = mediaRepository.loadImages()
                val photoLocations = images.mapNotNull { image ->
                    val exif = com.iris.gallery.data.loadExifMetadata(getApplication(), image.uri, image.path)
                    val lat = exif.latitude
                    val lng = exif.longitude
                    if ((lat != null) && (lng != null) && ((lat != 0.0) || (lng != 0.0))) {
                        PhotoLocation(
                            id = image.id,
                            imageUri = image.uri.toString(),
                            latitude = lat,
                            longitude = lng,
                        )
                    } else {
                        null
                    }
                }
                _uiState.update { it.copy(photos = photoLocations) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Fehler beim Laden der Foto-Standorte")
                }
            }
        }
    }

    /**
     * Imports a user-selected .pmtiles file via SAF, displays progress, and switches the map to it upon success.
     */
    @Suppress("unused")
    fun importMap(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    importProgress = 0f,
                    errorMessage = null,
                )
            }

            val result = mapRepository.importCustomPmTiles(uri) { progress ->
                _uiState.update { it.copy(importProgress = progress) }
            }

            result.onSuccess {
                try {
                    val newStyleUri = mapRepository.getActiveStyleUri(currentIsDark)
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            styleUri = newStyleUri,
                            isUsingCustomMap = true,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importProgress = null,
                            errorMessage = e.localizedMessage ?: "Fehler beim Laden der neuen Kartendatei",
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importProgress = null,
                        errorMessage = error.localizedMessage ?: "Import der Detailkarte fehlgeschlagen",
                    )
                }
            }
        }
    }

    /**
     * Deletes the custom detail map and resets to the bundled world base map.
     */
    @Suppress("unused")
    fun resetToBundledMap() {
        viewModelScope.launch {
            val result = mapRepository.deleteCustomMap()
            result.onSuccess {
                try {
                    val fallbackStyleUri = mapRepository.getActiveStyleUri(currentIsDark)
                    _uiState.update {
                        it.copy(
                            styleUri = fallbackStyleUri,
                            isUsingCustomMap = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            errorMessage = e.localizedMessage ?: "Fehler beim Laden der Basiskarte",
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.localizedMessage ?: "Fehler beim Zurücksetzen auf die Basiskarte",
                    )
                }
            }
        }
    }

    /**
     * Dismisses the current error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    data class GpsCacheEntry(val lat: Double, val lng: Double, val dateModified: Long)

    companion object {
        private val gpsCache = ConcurrentHashMap<Long, GpsCacheEntry>()
        @Volatile
        private var isDiskCacheLoaded = false
        private var lastScannedSignature: Long = 0L
        @Volatile
        private var hasScannedMedia = false

        private fun getCacheFile(context: Context): File {
            return File(context.filesDir, "photo_gps_cache.json")
        }

        private fun ensureGpsCacheLoaded(context: Context) {
            if (isDiskCacheLoaded) return
            synchronized(this) {
                if (isDiskCacheLoaded) return
                val file = getCacheFile(context)
                if (file.exists()) {
                    try {
                        val text = file.readText()
                        val json = org.json.JSONObject(text)
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val id = key.toLongOrNull() ?: continue
                            val arr = json.optJSONArray(key) ?: continue
                            if (arr.length() >= 4) {
                                val hasGps = arr.optInt(0, 0) == 1
                                val lat = if (hasGps) arr.optDouble(1, Double.NaN) else Double.NaN
                                val lng = if (hasGps) arr.optDouble(2, Double.NaN) else Double.NaN
                                val dm = arr.optLong(3, 0L)
                                gpsCache[id] = GpsCacheEntry(lat, lng, dm)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PhotoMapViewModel", "Failed to load GPS cache", e)
                    }
                }
                isDiskCacheLoaded = true
            }
        }

        private fun saveGpsCacheToDisk(context: Context) {
            try {
                val json = org.json.JSONObject()
                for ((id, entry) in gpsCache) {
                    val arr = org.json.JSONArray()
                    val hasGps = !entry.lat.isNaN() && !entry.lng.isNaN()
                    arr.put(if (hasGps) 1 else 0)
                    arr.put(if (hasGps) entry.lat else 0.0)
                    arr.put(if (hasGps) entry.lng else 0.0)
                    arr.put(entry.dateModified)
                    json.put(id.toString(), arr)
                }
                val file = getCacheFile(context)
                val tmp = File(context.filesDir, "photo_gps_cache.tmp")
                tmp.writeText(json.toString())
                if (file.exists()) {
                    file.delete()
                }
                tmp.renameTo(file)
            } catch (e: Exception) {
                android.util.Log.e("PhotoMapViewModel", "Failed to save GPS cache", e)
            }
        }

        fun clearGpsCache(context: Context) {
            gpsCache.clear()
            lastScannedSignature = 0L
            hasScannedMedia = false
            try {
                val file = getCacheFile(context)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }

        fun extractGps(context: Context, uri: Uri, path: String?): Pair<Double, Double>? {
            val directFile = if (!path.isNullOrBlank()) File(path) else null
            val useDirectFile = (directFile != null) && directFile.exists() && directFile.canRead()
            return try {
                val exif = if (useDirectFile) {
                    androidx.exifinterface.media.ExifInterface(directFile)
                } else {
                    val targetUri = if ((Build.VERSION.SDK_INT >= 29) && (uri.scheme == android.content.ContentResolver.SCHEME_CONTENT)) {
                        runCatching { MediaStore.setRequireOriginal(uri) }.getOrDefault(uri)
                    } else uri
                    context.contentResolver.openInputStream(targetUri)?.use { stream ->
                        androidx.exifinterface.media.ExifInterface(stream)
                    }
                } ?: return null

                val latLong = exif.latLong ?: return null
                val lat = latLong.getOrNull(0) ?: return null
                val lng = latLong.getOrNull(1) ?: return null
                if ((lat == 0.0) && (lng == 0.0)) null else Pair(lat, lng)
            } catch (_: Exception) {
                null
            }
        }

        fun updateGpsEntry(id: Long, lat: Double, lng: Double, dateModified: Long) {
            gpsCache[id] = GpsCacheEntry(lat, lng, dateModified)
        }

        fun persistGpsCache(context: Context) {
            saveGpsCacheToDisk(context)
        }
    }
}
