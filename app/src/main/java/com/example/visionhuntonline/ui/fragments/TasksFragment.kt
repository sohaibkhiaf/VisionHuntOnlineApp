package com.example.visionhuntonline.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visionhuntonline.R
import com.example.visionhuntonline.data.CocoTasks
import com.example.visionhuntonline.data.UserSessionManager
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

        viewModel.fetchCurrentEvent(requireContext())
        viewModel.fetchMiniLeaderboard(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.gameProgress.collect { progress ->
                        binding?.let { b ->
                            val hasEvent = viewModel.currentEvent.value != null
                            if (hasEvent) {
                                b.tvScore.text = "${progress.score}"
                                b.tvProgress.text = "المهام المكتملة: ${progress.completedCount}/${progress.totalTasks}"
                                b.tvProgress.visibility = View.VISIBLE
                            } else {
                                b.tvScore.text = "-"
                                b.tvProgress.visibility = View.GONE
                            }
                        }
                    }
                }
                launch {
                    viewModel.gameState.collect { state ->
                        tasksAdapter.submitData(CocoTasks.GAME_TASKS, state.completedTaskIds)
                    }
                }
                launch {
                    viewModel.currentEvent.collect { event ->
                        binding?.let { b ->
                            val progress = viewModel.gameProgress.value
                            b.layoutCurrentEvent.visibility = View.VISIBLE
                            if (event != null) {
                                b.tvScore.text = "${progress.score}"
                                b.tvProgress.text = "المهام المكتملة: ${progress.completedCount}/${progress.totalTasks}"
                                b.tvProgress.visibility = View.VISIBLE

                                b.tvEventTitle.text = "🏆 ${event.title}"

                                val descText = StringBuilder()
                                if (!event.description.isNullOrEmpty()) {
                                    descText.append(event.description)
                                }

                                val startFormatted = formatEventDate(event.startsAt)
                                val endFormatted = formatEventDate(event.endsAt)

                                if (startFormatted.isNotEmpty() && endFormatted.isNotEmpty()) {
                                    if (descText.isNotEmpty()) descText.append(" • ")
                                    descText.append("من ").append(startFormatted).append(" إلى ").append(endFormatted)
                                } else if (startFormatted.isNotEmpty()) {
                                    if (descText.isNotEmpty()) descText.append(" • ")
                                    descText.append("من ").append(startFormatted)
                                } else if (endFormatted.isNotEmpty()) {
                                    if (descText.isNotEmpty()) descText.append(" • ")
                                    descText.append("حتى ").append(endFormatted)
                                }

                                b.tvEventDescription.text = descText.toString()
                                b.tvEventDescription.visibility = if (descText.isEmpty()) View.GONE else View.VISIBLE

                                if (viewModel.miniLeaderboard.value.isNotEmpty()) {
                                    b.cardMiniLeaderboard.visibility = View.VISIBLE
                                }
                            } else {
                                b.tvScore.text = "-"
                                b.tvProgress.visibility = View.GONE
                                b.cardMiniLeaderboard.visibility = View.GONE

                                b.tvEventTitle.text = "ℹ️ لا يوجد حدث جارٍ حالياً"
                                b.tvEventDescription.text = "تابعنا لمعرفة التحديات والفعاليات القادمة!"
                                b.tvEventDescription.visibility = View.VISIBLE
                            }
                        }
                    }
                }
                launch {
                    viewModel.miniLeaderboard.collect { players ->
                        binding?.let { b ->
                            val hasEvent = viewModel.currentEvent.value != null
                            if (hasEvent && players.isNotEmpty()) {
                                b.cardMiniLeaderboard.visibility = View.VISIBLE
                                b.layoutMiniLeaderboardRows.removeAllViews()

                                val currentUsername = UserSessionManager(requireContext()).getUsername()
                                val inflater = LayoutInflater.from(requireContext())

                                for (player in players) {
                                    val rowView = inflater.inflate(R.layout.item_mini_leaderboard_row, b.layoutMiniLeaderboardRows, false)

                                    val tvRank = rowView.findViewById<TextView>(R.id.tv_mini_rank)
                                    val tvUsername = rowView.findViewById<TextView>(R.id.tv_mini_username)
                                    val tvScore = rowView.findViewById<TextView>(R.id.tv_mini_score)

                                    tvRank.text = "#${player.rank}"
                                    tvUsername.text = player.username
                                    tvScore.text = "${player.score}"

                                    if (player.username.equals(currentUsername, ignoreCase = true)) {
                                        tvUsername.setTypeface(null, Typeface.BOLD)
                                        tvUsername.setTextColor(Color.parseColor("#FEF08A"))
                                    }

                                    b.layoutMiniLeaderboardRows.addView(rowView)
                                }
                            } else {
                                b.cardMiniLeaderboard.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatEventDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            if (dateStr.length >= 16) dateStr.substring(0, 16) else dateStr
        } catch (_: Exception) {
            dateStr
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
