package com.example.visionhuntonline.ui.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
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
import com.example.visionhuntonline.network.EndedPlayer
import com.example.visionhuntonline.ui.adapters.TasksAdapter
import com.example.visionhuntonline.ui.screens.CapturedImagesActivity
import com.example.visionhuntonline.ui.viewmodel.GameViewModel
import kotlinx.coroutines.flow.combine
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

        binding?.swipeRefresh?.setOnRefreshListener {
            viewModel.fetchCurrentEvent(requireContext())
            viewModel.fetchMiniLeaderboard(requireContext())
            viewModel.fetchAndSyncAccountState(requireContext())
            binding?.swipeRefresh?.isRefreshing = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.gameProgress.collect { progress ->
                        binding?.let { b ->
                            val currentData = viewModel.currentEvent.value
                            val eventType = currentData?.eventType
                            val isOngoing = (eventType == "ongoing" && currentData.event != null)
                            if (isOngoing) {
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
                    combine(
                        viewModel.currentEvent,
                        viewModel.miniLeaderboard
                    ) { currentEventData, players ->
                        Pair(currentEventData, players)
                    }.collect { (currentEventData, players) ->
                        binding?.let { b ->
                            val progress = viewModel.gameProgress.value
                            b.layoutCurrentEvent.visibility = View.VISIBLE

                            val eventType = currentEventData?.eventType
                            val eventInfo = currentEventData?.event
                            val lastEnded = currentEventData?.lastEndedEvent

                            when (eventType) {
                                "ongoing" -> {
                                    if (eventInfo != null) {
                                        b.tvScore.text = "${progress.score}"
                                        b.tvProgress.text = "المهام المكتملة: ${progress.completedCount}/${progress.totalTasks}"
                                        b.tvProgress.visibility = View.VISIBLE

                                        b.tvEventTitle.text = "🏆 ${eventInfo.title}"
                                        b.tvEventDescription.text = buildEventDescription(eventInfo.description, eventInfo.startsAt, eventInfo.endsAt)
                                        b.tvEventDescription.visibility = View.VISIBLE

                                        if (players.isNotEmpty()) {
                                            b.cardMiniLeaderboard.visibility = View.VISIBLE
                                            b.layoutMiniLeaderboardRows.removeAllViews()

                                            val currentUsername = UserSessionManager(requireContext()).getUsername()
                                            val inflater = LayoutInflater.from(requireContext())

                                            for (player in players) {
                                                val rowView = inflater.inflate(R.layout.item_mini_leaderboard_row, b.layoutMiniLeaderboardRows, false)

                                                val tvRank = rowView.findViewById<TextView>(R.id.tv_mini_rank)
                                                val tvUsername = rowView.findViewById<TextView>(R.id.tv_mini_username)
                                                val tvScore = rowView.findViewById<TextView>(R.id.tv_mini_score)

                                                tvRank.text = if (player.rank > 0) "#${player.rank}" else "#..."
                                                tvUsername.text = player.username
                                                tvScore.text = "${player.score}"

                                                if (player.username.equals(currentUsername, ignoreCase = true)) {
                                                    tvUsername.setTypeface(null, Typeface.BOLD)
                                                    tvUsername.setTextColor(Color.parseColor("#166534"))
                                                } else {
                                                    val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                                                    tvUsername.setTextColor(if (isDarkTheme) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A"))
                                                }

                                                b.layoutMiniLeaderboardRows.addView(rowView)
                                            }
                                        } else {
                                            b.cardMiniLeaderboard.visibility = View.GONE
                                        }

                                        showNormalTasksList("قائمة التحديات")
                                    }
                                }
                                "upcoming" -> {
                                    b.tvScore.text = "-"
                                    b.tvProgress.visibility = View.GONE
                                    b.cardMiniLeaderboard.visibility = View.GONE

                                    if (eventInfo != null) {
                                        b.tvEventTitle.text = "⏳ الحدث القادم: ${eventInfo.title}"
                                        b.tvEventDescription.text = buildEventDescription(eventInfo.description, eventInfo.startsAt, eventInfo.endsAt)
                                        b.tvEventDescription.visibility = View.VISIBLE
                                    }

                                    if (lastEnded != null && lastEnded.players.isNotEmpty()) {
                                        showEndedEventTable("🏆 نتائج آخر حدث منتهٍ: ${lastEnded.title}", lastEnded.players)
                                    } else {
                                        showNormalTasksList("قائمة التحديات")
                                    }
                                }
                                "ended" -> {
                                    b.tvScore.text = "-"
                                    b.tvProgress.visibility = View.GONE
                                    b.cardMiniLeaderboard.visibility = View.GONE

                                    b.tvEventTitle.text = "ℹ️ لا يوجد حدث جارٍ حالياً"
                                    val endedDesc = if (eventInfo != null) "انتهى آخر حدث: ${eventInfo.title}" else "تابعنا لمعرفة التحديات والفعاليات القادمة!"
                                    b.tvEventDescription.text = endedDesc
                                    b.tvEventDescription.visibility = View.VISIBLE

                                    if (eventInfo != null && !eventInfo.players.isNullOrEmpty()) {
                                        showEndedEventTable("🏆 نتائج الحدث المنتهي: ${eventInfo.title}", eventInfo.players)
                                    } else {
                                        showNormalTasksList("قائمة التحديات")
                                    }
                                }
                                else -> {
                                    b.tvScore.text = "-"
                                    b.tvProgress.visibility = View.GONE
                                    b.cardMiniLeaderboard.visibility = View.GONE

                                    b.tvEventTitle.text = "ℹ️ لا يوجد حدث جارٍ حالياً"
                                    b.tvEventDescription.text = "تابعنا لمعرفة التحديات والفعاليات القادمة!"
                                    b.tvEventDescription.visibility = View.VISIBLE

                                    showNormalTasksList("قائمة التحديات")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showEndedEventTable(title: String, players: List<EndedPlayer>) {
        val b = binding ?: return
        b.tvTasksSectionTitle.text = title
        b.rvTasks.visibility = View.GONE
        b.layoutEndedTableContainer.visibility = View.VISIBLE
        b.layoutEndedTableContainer.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())
        val tableCard = inflater.inflate(R.layout.layout_ended_event_table, b.layoutEndedTableContainer, false)
        val tableLayout = tableCard.findViewById<TableLayout>(R.id.table_ended_leaderboard)

        val currentUsername = UserSessionManager(requireContext()).getUsername()
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        for ((index, player) in players.take(3).withIndex()) {
            val row = TableRow(requireContext()).apply {
                layoutParams = TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 12, 16, 16)
                gravity = Gravity.CENTER_VERTICAL
                if (index % 2 == 1) {
                    setBackgroundColor(if (isDarkTheme) Color.parseColor("#15000000") else Color.parseColor("#06000000"))
                }
            }

            // Rank on Right (RTL - index 0)
            val rankContainer = LinearLayout(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(70, TableRow.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
                setPadding(6, 6, 6, 6)
                
                val faceColor: Int
                val shadowColor: Int
                when (player.rankPosition) {
                    1 -> { faceColor = if (isDarkTheme) Color.parseColor("#B45309") else Color.parseColor("#FDE047"); shadowColor = if (isDarkTheme) Color.parseColor("#78350F") else Color.parseColor("#CA8A04") }
                    2 -> { faceColor = if (isDarkTheme) Color.parseColor("#64748B") else Color.parseColor("#E2E8F0"); shadowColor = if (isDarkTheme) Color.parseColor("#334155") else Color.parseColor("#94A3B8") }
                    else -> { faceColor = if (isDarkTheme) Color.parseColor("#9A3412") else Color.parseColor("#FFEDD5"); shadowColor = if (isDarkTheme) Color.parseColor("#7C2D12") else Color.parseColor("#C2410C") }
                }
                background = create3DPill(requireContext(), faceColor, shadowColor)
            }
            val tvRank = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "${player.rankPosition}"
                gravity = Gravity.CENTER
                val rankTextColor = when (player.rankPosition) {
                    1 -> if (isDarkTheme) Color.parseColor("#FEF08A") else Color.parseColor("#854D0E")
                    2 -> if (isDarkTheme) Color.parseColor("#F8FAFC") else Color.parseColor("#475569")
                    else -> if (isDarkTheme) Color.parseColor("#FFEDD5") else Color.parseColor("#9A3412")
                }
                setTextColor(rankTextColor)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            rankContainer.addView(tvRank)

            // Username in Middle (index 1)
            val tvUsername = TextView(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                text = player.username
                gravity = Gravity.CENTER
                setPadding(12, 0, 12, 0)
                textSize = 14f
                setMaxLines(1)
                setEllipsize(TextUtils.TruncateAt.END)
                if (player.username.equals(currentUsername, ignoreCase = true)) {
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#166534"))
                } else {
                    setTextColor(if (isDarkTheme) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A"))
                }
            }
            
            // Score on Left (index 2)
            val scoreContainer = LinearLayout(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(100, TableRow.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
                
                val faceColor = if (isDarkTheme) Color.parseColor("#064E3B") else Color.parseColor("#DCFCE7")
                val shadowColor = if (isDarkTheme) Color.parseColor("#022C22") else Color.parseColor("#86EFAC")
                val strokeColor = if (isDarkTheme) Color.parseColor("#059669") else Color.parseColor("#4ADE80")
                
                background = create3DPill(requireContext(), faceColor, shadowColor, strokeColor)
            }
            val tvScore = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = "${player.score} ⭐"
                gravity = Gravity.CENTER
                setTextColor(if (isDarkTheme) Color.parseColor("#34D399") else Color.parseColor("#15803D"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }
            scoreContainer.addView(tvScore)

            row.addView(rankContainer)
            row.addView(tvUsername)
            row.addView(scoreContainer)
            tableLayout.addView(row)
        }

        b.layoutEndedTableContainer.addView(tableCard)
    }

    private fun create3DPill(context: android.content.Context, faceColor: Int, shadowColor: Int, strokeColor: Int? = null): android.graphics.drawable.LayerDrawable {
        val density = context.resources.displayMetrics.density
        val offset = (4 * density).toInt()
        val strokeW = (1.5f * density).toInt()

        val shadow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f * density
            setColor(shadowColor)
        }
        val face = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 100f * density
            setColor(faceColor)
            if (strokeColor != null) {
                setStroke(strokeW, strokeColor)
            }
        }
        val layerDrawable = android.graphics.drawable.LayerDrawable(arrayOf(shadow, face))
        layerDrawable.setLayerInset(0, 0, offset, 0, 0)
        layerDrawable.setLayerInset(1, 0, 0, 0, offset)
        return layerDrawable
    }
    private fun showNormalTasksList(title: String) {
        val b = binding ?: return
        b.tvTasksSectionTitle.text = title
        b.rvTasks.visibility = View.VISIBLE
        b.layoutEndedTableContainer.visibility = View.GONE
        if (b.rvTasks.adapter != tasksAdapter) {
            b.rvTasks.adapter = tasksAdapter
        }
    }

    private fun buildEventDescription(description: String?, startsAt: String?, endsAt: String?): String {
        val descText = StringBuilder()
        if (!description.isNullOrEmpty()) {
            descText.append(description)
        }

        val startFormatted = formatEventDate(startsAt)
        val endFormatted = formatEventDate(endsAt)

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
        return descText.toString()
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
