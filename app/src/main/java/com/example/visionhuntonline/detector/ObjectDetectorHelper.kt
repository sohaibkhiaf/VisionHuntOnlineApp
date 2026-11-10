package com.example.visionhuntonline.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.model.Detection
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ObjectDetectorHelper(
    private val context: Context,
    private var scoreThreshold: Float = CocoTasks.DISPLAY_CONFIDENCE_THRESHOLD,
    private var maxResults: Int = 10,
    private val detectorListener: DetectorListener? = null
) : ImageAnalysis.Analyzer {

    interface DetectorListener {
        fun onError(error: String)
        fun onResults(
            results: List<Detection>,
            inferenceTime: Long,
            imageWidth: Int,
            imageHeight: Int,
            frameBitmap: Bitmap? = null
        )
    }

    private var interpreter: Interpreter? = null
    private val intValues = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
    private val imgData = ByteBuffer.allocateDirect(1 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 3).apply {
        order(ByteOrder.nativeOrder())
    }

    init {
        setupObjectDetector()
    }

    @Synchronized
    fun setupObjectDetector() {
        if (interpreter != null) return
        val modelPath = "models/efficientdet_lite3.tflite"

        try {
            val modelBuffer = loadModelFile(context, modelPath)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Successfully loaded TFLite Interpreter for $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "TFLite failed to load model: ${e.message}", e)
            detectorListener?.onError("فشل تحميل النموذج: ${e.message}")
        }
    }

    @Synchronized
    fun clearObjectDetector() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interpreter: ${e.message}", e)
        } finally {
            interpreter = null
        }
    }

    @Synchronized
    override fun analyze(imageProxy: ImageProxy) {
        val currentInterpreter = interpreter
        if (currentInterpreter == null) {
            imageProxy.close()
            return
        }

        val startTime = SystemClock.uptimeMillis()

        try {
            val bitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)

            imgData.rewind()
            scaledBitmap.getPixels(intValues, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)

            var pixelIndex = 0
            val totalPixels = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE
            while (pixelIndex < totalPixels) {
                val pixel = intValues[pixelIndex++]
                imgData.put((pixel shr 16 and 0xFF).toByte())
                imgData.put((pixel shr 8 and 0xFF).toByte())
                imgData.put((pixel and 0xFF).toByte())
            }

            val outputLocations = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
            val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
            val outputNumDetections = FloatArray(1)

            val outputs = mapOf(
                0 to outputLocations,
                1 to outputClasses,
                2 to outputScores,
                3 to outputNumDetections
            )

            currentInterpreter.runForMultipleInputsOutputs(arrayOf(imgData), outputs)

            val count = outputNumDetections[0].toInt().coerceIn(0, MAX_DETECTIONS)

            val detections = mutableListOf<Detection>()

            for (i in 0 until count) {
                val score = outputScores[0][i]
                if (score < scoreThreshold) continue

                val classId = outputClasses[0][i].toInt()
                val ymin = outputLocations[0][i][0].coerceIn(0f, 1f)
                val xmin = outputLocations[0][i][1].coerceIn(0f, 1f)
                val ymax = outputLocations[0][i][2].coerceIn(0f, 1f)
                val xmax = outputLocations[0][i][3].coerceIn(0f, 1f)

                if (ymax <= ymin || xmax <= xmin) continue

                val label = if (classId in CocoTasks.COCO_LABELS.indices) {
                    CocoTasks.COCO_LABELS[classId]
                } else {
                    "unknown_$classId"
                }

                if (label == "???") continue

                detections.add(
                    Detection(
                        classId = classId,
                        label = label,
                        score = score,
                        boundingBox = RectF(xmin, ymin, xmax, ymax)
                    )
                )

                if (detections.size >= maxResults) break
            }

            val inferenceTime = SystemClock.uptimeMillis() - startTime

            detectorListener?.onResults(
                results = detections,
                inferenceTime = inferenceTime,
                imageWidth = rotatedBitmap.width,
                imageHeight = rotatedBitmap.height,
                frameBitmap = rotatedBitmap
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error during detection frame analysis: ${e.message}", e)
            detectorListener?.onError("خطأ الاستدلال: ${e.message}")
        } finally {
            imageProxy.close()
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    companion object {
        private const val TAG = "ObjectDetectorHelper"
        private const val MODEL_INPUT_SIZE = 512
        private const val MAX_DETECTIONS = 25
    }
}
