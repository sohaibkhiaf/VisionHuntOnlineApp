package com.example.visionhuntonline.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.visionhuntonline.databinding.ItemTaskBinding
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

        with(holder.binding) {
            tvTitleAr.text = task.displayNameAr
            tvTitleEn.text = task.displayNameEn
            tvRarity.text = "الندرة: ${task.rarity.displayNameAr}"
            tvPoints.text = "+${task.points}"

            if (isCompleted) {
                tvStatus.text = "✓ مكتمل"
                tvStatus.setTextColor(Color.parseColor("#15803D"))
                cardTask.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                cardTask.strokeColor = Color.parseColor("#86EFAC")
            } else {
                tvStatus.text = "○ لم يُكتشف"
                tvStatus.setTextColor(Color.parseColor("#64748B"))
                cardTask.setCardBackgroundColor(Color.WHITE)
                cardTask.strokeColor = Color.parseColor("#E2E8F0")
            }
        }
    }

    override fun getItemCount(): Int = tasksList.size
}
