package com.example.visionhuntonline.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.visionhuntonline.databinding.FragmentCameraBinding
import com.example.visionhuntonline.detector.ObjectDetectorHelper
import com.example.visionhuntonline.model.Detection
import com.example.visionhuntonline.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding

    private val viewModel: GameViewModel by activityViewModels()
    private var cameraExecutor: ExecutorService? = null
    private var detectorHelper: ObjectDetectorHelper? = null
    private var cameraControl: androidx.camera.core.CameraControl? = null
    private var isFlashlightOn = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            context?.let {
                com.example.visionhuntonline.util.GameToast.show(it, "نحتاج إلى إذن الكاميرا لتشغيل الكشف", Toast.LENGTH_SHORT)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding?.btnFlashlightCard?.setOnClickListener {
            isFlashlightOn = !isFlashlightOn
            cameraControl?.enableTorch(isFlashlightOn)
            
            val tintColor = if (isFlashlightOn) android.graphics.Color.parseColor("#F59E0B") else android.graphics.Color.parseColor("#166534")
            binding?.ivFlashlight?.setColorFilter(tintColor)
        }

        binding?.swipeRefreshCamera?.setOnRefreshListener {
            viewModel.fetchCurrentEvent(requireContext())
            binding?.swipeRefreshCamera?.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rawDetections.collect { detections ->
                        binding?.let { b ->
                            b.detectionOverlay.updateDetections(detections)
                        }
                    }
                }
                launch {
                    viewModel.inferenceTime.collect { timeMs ->
                        binding?.let { b ->
                            b.tvBadge.text = "الكشف نشط (${timeMs}ms)"
                        }
                    }
                }
                launch {
                    viewModel.itemDetectedSuccessEvent.collect { pair ->
                        if (pair != null) {
                            val (taskNameAr, points) = pair
                            binding?.let { b ->
                                b.tvSuccessMessage.text = "🎉 تم اكتشاف: $taskNameAr (+${points} نقطة)"
                                b.cardSuccessBanner.visibility = View.VISIBLE
                                b.cardSuccessBanner.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .withEndAction {
                                        b.cardSuccessBanner.postDelayed({
                                            b.cardSuccessBanner.animate()
                                                .alpha(0f)
                                                .setDuration(400)
                                                .withEndAction {
                                                    b.cardSuccessBanner.visibility = View.GONE
                                                    viewModel.resetItemDetectedSuccessEvent()
                                                }
                                                .start()
                                        }, 2200)
                                    }
                                    .start()
                            }
                        }
                    }
                }
                launch {
                    viewModel.showOfflineDialogEvent.collect { show ->
                        if (show) {
                            activity?.let { act ->
                                com.example.visionhuntonline.util.NetworkDialogHelper(act).showNoInternetDialog()
                            }
                            viewModel.resetOfflineDialogEvent()
                        }
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val ctx = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        detectorHelper = ObjectDetectorHelper(
            context = ctx,
            detectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    activity?.runOnUiThread {
                        Log.e("CameraFragment", "Model error: $error")
                    }
                }

                override fun onResults(
                    results: List<Detection>,
                    inferenceTime: Long,
                    imageWidth: Int,
                    imageHeight: Int,
                    frameBitmap: android.graphics.Bitmap?
                ) {
                    activity?.runOnUiThread {
                        if (_binding != null) {
                            viewModel.onDetectionsReceived(results, inferenceTime, frameBitmap, context)
                        }
                    }
                }
            }
        )

        cameraProviderFuture.addListener({
            try {
                if (_binding == null) return@addListener
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also { p ->
                    binding?.previewView?.surfaceProvider?.let { sp ->
                        p.setSurfaceProvider(sp)
                    }
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build().also { analysis ->
                        detectorHelper?.let { helper ->
                            cameraExecutor?.let { exec ->
                                analysis.setAnalyzer(exec, helper)
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                cameraControl = camera.cameraControl
                
            } catch (e: Exception) {
                Log.e("CameraFragment", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    override fun onPause() {
        super.onPause()
        context?.let { ctx ->
            try {
                ProcessCameraProvider.getInstance(ctx).get().unbindAll()
            } catch (e: Exception) {
                Log.e("CameraFragment", "Error unbinding camera on pause", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detectorHelper?.clearObjectDetector()
        detectorHelper = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
    }
}
