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
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.databinding.FragmentCameraBinding
import com.example.visionhuntonline.detector.ObjectDetectorHelper
import com.example.visionhuntonline.model.Detection
import com.example.visionhuntonline.model.TaskDefinition
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

    private var activePageTasks: List<TaskDefinition>? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            context?.let {
                Toast.makeText(it, "نحتاج إلى إذن الكاميرا لتشغيل الكشف", Toast.LENGTH_SHORT).show()
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

        val currentCompletedIds = viewModel.gameState.value.completedTaskIds
        activePageTasks = CocoTasks.GAME_TASKS.filter { it.id !in currentCompletedIds }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.rawDetections.collect { detections ->
                        binding?.let { b ->
                            b.detectionOverlay.updateDetections(detections)
                            b.tvDetectedCount.text = "الأجسام المكتشفة للكاميرا: ${detections.size}"
                        }
                    }
                }
                launch {
                    viewModel.gameDetections.collect { gameDetections ->
                        binding?.let { b ->
                            b.tvMatchedCount.text = "الأجسام المطابقة للشرط (85%+): ${gameDetections.size}"
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
                    viewModel.gameState.collect { state ->
                        binding?.let {
                            updateTasksStatusList(state.completedTaskIds)
                        }
                    }
                }
            }
        }
    }

    private fun updateTasksStatusList(completedIds: Set<String>) {
        val b = binding ?: return

        if (activePageTasks == null) {
            activePageTasks = CocoTasks.GAME_TASKS.filter { it.id !in completedIds }
        }

        val tasksToShow = activePageTasks ?: emptyList()

        val sb = StringBuilder()
        if (tasksToShow.isEmpty()) {
            sb.append("تهانينا! جميع المهام مكتملة 🎉\n")
        } else {
            sb.append("قائمة المهام غير المكتملة:\n")
            tasksToShow.forEach { task ->
                val isDone = task.id in completedIds
                val statusStr = if (isDone) "✓ مكتمل (+${task.points})" else "○ لم يُكتشف بعد"
                sb.append("• ").append(task.displayNameAr).append(": ").append(statusStr).append("\n")
            }
        }
        b.tvTaskStatusList.text = sb.toString()
    }

    private fun startCamera() {
        val ctx = context ?: return
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        detectorHelper = ObjectDetectorHelper(
            context = ctx,
            detectorListener = object : ObjectDetectorHelper.DetectorListener {
                override fun onError(error: String) {
                    activity?.runOnUiThread {
                        binding?.tvModelStatus?.text = "خطأ النموذج: $error"
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
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
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
        activePageTasks = null
        detectorHelper?.clearObjectDetector()
        detectorHelper = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        _binding = null
    }
}
