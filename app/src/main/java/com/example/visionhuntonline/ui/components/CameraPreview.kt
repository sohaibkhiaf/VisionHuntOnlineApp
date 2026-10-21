package com.example.visionhuntonline.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.visionhuntonline.detector.ObjectDetectorHelper
import com.example.visionhuntonline.model.Detection
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    onDetectionsReceived: (List<Detection>, Long) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val executor = remember { Executors.newSingleThreadExecutor() }

    val detectorHelper = remember {
        ObjectDetectorHelper(
            context = context,
            detectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    onError(error)
                }

                override fun onResults(
                    results: List<Detection>,
                    inferenceTime: Long,
                    imageWidth: Int,
                    imageHeight: Int,
                    frameBitmap: android.graphics.Bitmap?
                ) {
                    onDetectionsReceived(results, inferenceTime)
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            detectorHelper.clearObjectDetector()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build().also {
                                it.setAnalyzer(executor, detectorHelper)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Camera binding failed: ${e.message}", e)
                        onError("فشل ربط الكاميرا: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
