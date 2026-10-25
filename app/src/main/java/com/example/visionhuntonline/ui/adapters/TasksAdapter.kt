package com.example.visionhuntonline.ui.adapters

import android.content.res.Configuration
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.visionhuntonline.R
import com.example.visionhuntonline.databinding.ItemTaskBinding
import com.example.visionhuntonline.model.Rarity
import com.example.visionhuntonline.model.TaskDefinition

class TasksAdapter : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    private var tasksList: List<TaskDefinition> = emptyList()
    private var completedTaskIds: Set<String> = emptySet()

    fun submitData(tasks: List<TaskDefinition>, completedIds: Set<String>) {
        this.tasksList = tasks
        this.completedTaskIds = completedIds
        notifyDataSetChanged()
    }

    class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasksList[position]
        val isCompleted = task.id in completedTaskIds

        val context = holder.itemView.context
        val isDarkTheme = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        with(holder.binding) {
            tvTitleAr.text = task.displayNameAr
            tvTitleEn.text = task.displayNameEn
            tvRarity.text = "⚡ ${task.rarity.displayNameAr}"
            val rarityColor = when (task.rarity) {
                Rarity.COMMON -> if (isDarkTheme) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
                Rarity.UNCOMMON -> if (isDarkTheme) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")
                Rarity.RARE -> if (isDarkTheme) Color.parseColor("#FBBF24") else Color.parseColor("#D97706")
            }
            tvRarity.setTextColor(rarityColor)
            tvPoints.text = "+${task.points}"

            if (isCompleted) {
                ivStatusIcon.setImageResource(R.drawable.ic_checked)
                ivStatusIcon.setColorFilter(Color.parseColor("#15803D"))
                tvStatus.text = "مكتمل"
                tvStatus.setTextColor(Color.parseColor("#15803D"))

                if (isDarkTheme) {
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#064E3B"))
                    cardTask.setCardBackgroundColor(Color.parseColor("#022C22"))
                    cardTask.strokeColor = Color.parseColor("#059669")
                } else {
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
                    cardTask.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                    cardTask.strokeColor = Color.parseColor("#86EFAC")
                }
            } else {
                ivStatusIcon.setImageResource(R.drawable.ic_unchecked)
                ivStatusIcon.setColorFilter(Color.parseColor("#64748B"))
                tvStatus.text = "لم يُكتشف"
                tvStatus.setTextColor(Color.parseColor("#64748B"))

                if (isDarkTheme) {
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#334155"))
                    cardTask.setCardBackgroundColor(Color.parseColor("#1E293B"))
                    cardTask.strokeColor = Color.parseColor("#334155")
                } else {
                    cardStatusBadge.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                    cardTask.setCardBackgroundColor(Color.WHITE)
                    cardTask.strokeColor = Color.parseColor("#E2E8F0")
                }
            }
        }
    }

    override fun getItemCount(): Int = tasksList.size
}
