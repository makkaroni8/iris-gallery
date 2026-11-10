package com.iris.gallery.data.search

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import android.util.Log
import com.iris.gallery.data.MediaImage
import com.iris.gallery.data.MediaRepository

class PhotoIndexingWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    companion object {
        const val UNIQUE_WORK_NAME = "iris_photo_indexing_work"
        const val KEY_INDEXED_COUNT = "indexed_count"
        const val KEY_TOTAL_COUNT = "total_count"
        const val KEY_IS_INDEXING = "is_indexing"
        private const val BATCH_SIZE = 25

        fun enqueue(context: Context, force: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<PhotoIndexingWorker>()
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }

    private fun MediaImage.toMediaStorePhoto() = MediaStorePhoto(
        id = id,
        uri = uri,
        dateModified = dateModified,
    )

    private data class MediaStorePhoto(
        val id: Long,
        val uri: Uri,
        val dateModified: Long,
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        val database = PhotoSearchDatabase.getInstance(applicationContext)
        val dao = database.photoSearchDao()
        val settingsPref = com.iris.gallery.data.SettingsPreferences(applicationContext)
        if (!settingsPref.state.value.aiSearchEnabled) {
            return@withContext Result.success()
        }
        val analyzer = PhotoAiAnalyzer(
            confidenceThreshold = settingsPref.state.value.aiConfidenceThreshold,
            ocrEnabled = settingsPref.state.value.aiOcrEnabled,
            labelingEnabled = settingsPref.state.value.aiLabelingEnabled,
        )

        try {
            val repo = MediaRepository(applicationContext)
            val repoImages = runCatching { repo.loadImages(trashed = false) }.getOrNull()
            val currentPhotos = if (!repoImages.isNullOrEmpty()) {
                repoImages.asSequence().filterNot(MediaImage::isVideo).map { it.toMediaStorePhoto() }.toList()
            } else {
                loadMediaStorePhotos()
            }
            val currentMap = currentPhotos.associateBy(MediaStorePhoto::id)

            // Delta sync: Remove deleted photos from Room
            val indexedList = dao.getAllDateModified()
            val indexedMap = indexedList.associateBy({ it.mediaStoreId }) { it.dateModified }

            val deletedIds = indexedList
                .asSequence()
                .map { it.mediaStoreId }
                .filter { it !in currentMap }
                .toList()

            if (deletedIds.isNotEmpty()) {
                deletedIds.chunked(500).forEach { chunk ->
                    dao.deleteByIds(chunk)
                }
            }

            // Find new or modified photos to index
            val toIndex = currentPhotos.filter { photo ->
                val indexedDate = indexedMap[photo.id]
                (indexedDate == null) || (photo.dateModified > indexedDate)
            }

            val totalCount = toIndex.size
            if (totalCount == 0) {
                setProgress(
                    workDataOf(
                        KEY_INDEXED_COUNT to 0,
                        KEY_TOTAL_COUNT to 0,
                        KEY_IS_INDEXING to false,
                    ),
                )
                return@withContext Result.success()
            }

            setProgress(
                workDataOf(
                    KEY_INDEXED_COUNT to 0,
                    KEY_TOTAL_COUNT to totalCount,
                    KEY_IS_INDEXING to true,
                ),
            )

            var indexedCount = 0
            val batches = toIndex.chunked(BATCH_SIZE)

            for (batch in batches) {
                if (isStopped) {
                    return@withContext Result.retry()
                }

                val entities = mutableListOf<PhotoIndexEntity>()
                for ((photoId, photoUri, photoDateModified) in batch) {
                    if (isStopped) {
                        return@withContext Result.retry()
                    }

                    val analysis = analyzer.analyze(applicationContext, photoUri)
                    if (analysis != null) {
                        Log.d(
                            "PhotoSearch",
                            "Indexed photo #$photoId: OCR='${analysis.ocrText.take(20)}' Labels='${analysis.labelsDe.take(3)}'",
                        )
                        entities.add(
                            PhotoIndexEntity(
                                mediaStoreId = photoId,
                                contentUri = photoUri.toString(),
                                ocrText = analysis.ocrText,
                                labelsDe = analysis.labelsDe.joinToString(", "),
                                labelsEn = analysis.labelsEn.joinToString(", "),
                                dateModified = photoDateModified,
                            ),
                        )
                    } else {
                        // Mark as processed with empty labels so we don't block, but allow future re-indexing if modified
                        entities.add(
                            PhotoIndexEntity(
                                mediaStoreId = photoId,
                                contentUri = photoUri.toString(),
                                ocrText = "",
                                labelsDe = "",
                                labelsEn = "",
                                dateModified = photoDateModified,
                            ),
                        )
                    }
                }

                if (entities.isNotEmpty()) {
                    dao.upsertAll(entities)
                }

                indexedCount += batch.size
                setProgress(
                    workDataOf(
                        KEY_INDEXED_COUNT to indexedCount,
                        KEY_TOTAL_COUNT to totalCount,
                        KEY_IS_INDEXING to (indexedCount < totalCount),
                    ),
                )

                // Brief pause to prevent device overheating and reduce battery strain
                delay(30.milliseconds)
            }

            setProgress(
                workDataOf(
                    KEY_INDEXED_COUNT to totalCount,
                    KEY_TOTAL_COUNT to totalCount,
                    KEY_IS_INDEXING to false,
                ),
            )

            Result.success()
        } catch (e: Exception) {
            Log.e("PhotoSearch", "PhotoIndexingWorker failed", e)
            Result.retry()
        } finally {
            analyzer.close()
        }
    }

    private fun loadMediaStorePhotos(): List<MediaStorePhoto> {
        val collections = if (Build.VERSION.SDK_INT >= 29) {
            val names = runCatching { MediaStore.getExternalVolumeNames(applicationContext) }
                .getOrNull()
                ?.filter { it.isNotBlank() && (it != "external_primary") }

            if (!names.isNullOrEmpty()) {
                (listOf("external") + names).asSequence().distinct().map { MediaStore.Images.Media.getContentUri(it) }.toList()
            } else {
                listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            }
        } else {
            listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )

        val result = mutableListOf<MediaStorePhoto>()
        val resolver = applicationContext.contentResolver

        for (collection in collections) {
            runCatching {
                resolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val dateModified = cursor.getLong(dateModifiedCol)
                        val uri = ContentUris.withAppendedId(collection, id)
                        result.add(
                            MediaStorePhoto(
                                id = id,
                                uri = uri,
                                dateModified = dateModified,
                            )
                        )
                    }
                }
            }
        }

        return result
    }
}
