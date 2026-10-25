package com.example.visionhuntonline.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.visionhuntonline.databinding.ItemCapturedImageBinding
import com.example.visionhuntonline.model.CapturedItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CapturedImagesAdapter : RecyclerView.Adapter<CapturedImagesAdapter.ViewHolder>() {

    private val items = mutableListOf<CapturedItem>()

    fun submitList(newItems: List<CapturedItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCapturedImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemCapturedImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CapturedItem) {
            val context = binding.root.context
            val isDarkTheme = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

            binding.tvTaskNameAr.text = item.taskNameAr
            binding.tvTaskNameEn.text = item.taskNameEn
            
            val scorePercent = if (item.score <= 1.0f) (item.score * 100).toInt() else item.score.toInt()
            binding.tvScore.text = "مكتشف $scorePercent%"

            if (isDarkTheme) {
                binding.tvScore.setTextColor(android.graphics.Color.parseColor("#34D399"))
                binding.ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#34D399"))
                binding.cardScoreBadge.setBackgroundResource(com.example.visionhuntonline.R.drawable.bg_3d_task_completed)
                binding.cardCapturedImage.setBackgroundResource(com.example.visionhuntonline.R.drawable.bg_3d_task_uncompleted)
            } else {
                binding.tvScore.setTextColor(android.graphics.Color.parseColor("#15803D"))
                binding.ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#15803D"))
                binding.cardScoreBadge.setBackgroundResource(com.example.visionhuntonline.R.drawable.bg_3d_task_completed)
                binding.cardCapturedImage.setBackgroundResource(com.example.visionhuntonline.R.drawable.bg_3d_task_uncompleted)
            }

            val dateStr = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault()).format(Date(item.timestamp))
            binding.tvTimestamp.text = dateStr

            val imgFile = File(item.imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.ivCapturedImage.setImageBitmap(bitmap)
            } else {
                binding.ivCapturedImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}
