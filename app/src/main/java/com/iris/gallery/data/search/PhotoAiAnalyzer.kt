package com.iris.gallery.data.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Closeable

data class PhotoAnalysisResult(
    val ocrText: String,
    val labelsEn: List<String>,
    val labelsDe: List<String>,
)

class PhotoAiAnalyzer(
    val confidenceThreshold: Float = 0.65f,
    val maxDimension: Int = 1024,
    val ocrEnabled: Boolean = true,
    val labelingEnabled: Boolean = true,
) : Closeable {

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val imageLabeler: ImageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(confidenceThreshold)
                .build(),
        )
    }

    suspend fun analyze(context: Context, uri: Uri): PhotoAnalysisResult? = withContext(Dispatchers.Default) {
        val bitmap = loadDownscaledBitmap(context, uri, maxDimension) ?: return@withContext null
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            coroutineScope {
                val ocrDeferred = async {
                    if (!ocrEnabled) return@async ""
                    runCatching {
                        val textResult = textRecognizer.process(inputImage).await()
                        textResult.text.trim()
                    }.getOrDefault("")
                }

                val labelsDeferred = async {
                    if (!labelingEnabled) return@async emptyList()
                    runCatching {
                        val labels = imageLabeler.process(inputImage).await()
                        labels
                            .asSequence()
                            .filter { it.confidence >= confidenceThreshold }
                            .map { it.text.trim() }
                            .filter { it.isNotBlank() }
                            .toList()
                    }.getOrDefault(emptyList())
                }

                val ocrText = ocrDeferred.await()
                val labelsEn = labelsDeferred.await()
                val labelsDe = LabelTranslator.translateAll(labelsEn)

                PhotoAnalysisResult(
                    ocrText = ocrText,
                    labelsEn = labelsEn,
                    labelsDe = labelsDe,
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadDownscaledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= 28) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    if ((width > maxDim) || (height > maxDim)) {
                        val scale = maxOf(width.toFloat() / maxDim, height.toFloat() / maxDim)
                        val targetW = (width / scale).toInt().coerceAtLeast(1)
                        val targetH = (height / scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            }.getOrNull()
            decoded?.let { return it }
        }

        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if ((width <= 0) || (height <= 0)) return null

            var sampleSize = 1
            var currentWidth = width
            var currentHeight = height
            while ((currentWidth > maxDim) || (currentHeight > maxDim)) {
                sampleSize *= 2
                currentWidth /= 2
                currentHeight /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // Read EXIF orientation to correct rotation
            val orientation = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

            rotateBitmapIfNeeded(decodedBitmap, orientation)
        }.getOrNull()
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        if ((orientation == ExifInterface.ORIENTATION_NORMAL) || (orientation == ExifInterface.ORIENTATION_UNDEFINED)) {
            return bitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    override fun close() {
        runCatching { textRecognizer.close() }
        runCatching { imageLabeler.close() }
    }
}
