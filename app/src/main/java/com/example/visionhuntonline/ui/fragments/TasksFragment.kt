package com.example.visionhuntonline.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.databinding.FragmentTasksBinding
import com.example.visionhuntonline.ui.adapters.TasksAdapter
import com.example.visionhuntonline.ui.screens.CapturedImagesActivity
import com.example.visionhuntonline.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    var onStartCameraClick: (() -> Unit)? = null

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding

    private val viewModel: GameViewModel by activityViewModels()
    private val tasksAdapter = TasksAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.rvTasks?.layoutManager = LinearLayoutManager(requireContext())
        binding?.rvTasks?.adapter = tasksAdapter

        val startCameraListener = View.OnClickListener {
            onStartCameraClick?.invoke()
        }
        binding?.btnStartCamera?.setOnClickListener(startCameraListener)
        binding?.btnStartCameraCard?.setOnClickListener(startCameraListener)

        val galleryListener = View.OnClickListener {
            val intent = Intent(requireContext(), CapturedImagesActivity::class.java)
            startActivity(intent)
        }
        binding?.btnGallery?.setOnClickListener(galleryListener)
        binding?.btnGalleryCard?.setOnClickListener(galleryListener)

        val resetGameListener = View.OnClickListener {
            showResetDialog()
        }
        binding?.btnResetGame?.setOnClickListener(resetGameListener)
        binding?.btnResetGameCard?.setOnClickListener(resetGameListener)

        val profileInfoListener = View.OnClickListener {
            val intent = Intent(requireContext(), com.example.visionhuntonline.ui.screens.ProfileActivity::class.java)
            startActivity(intent)
        }
        binding?.btnProfileInfo?.setOnClickListener(profileInfoListener)
        binding?.btnProfileInfoCard?.setOnClickListener(profileInfoListener)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.gameProgress.collect { progress ->
                        binding?.let { b ->
                            b.tvScore.text = "${progress.score}"
                            b.tvProgress.text = "المهام المكتملة: ${progress.completedCount}/${progress.totalTasks}"
                        }
                    }
                }
                launch {
                    viewModel.gameState.collect { state ->
                        tasksAdapter.submitData(CocoTasks.GAME_TASKS, state.completedTaskIds)
                    }
                }
            }
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("إعادة بدء اللعبة")
            .setMessage("هل أنت متأكد؟ سيتم مسح تقدمك الحالي والبدء من جديد.")
            .setPositiveButton("إعادة البدء") { _, _ ->
                viewModel.resetGame(requireContext())
            }
            .setNegativeButton("لإلغاء", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
