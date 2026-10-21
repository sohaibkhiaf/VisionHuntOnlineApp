package com.example.visionhuntonline.ui.screens

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visionhuntonline.data.CapturedItemDbHelper
import com.example.visionhuntonline.databinding.ActivityCapturedImagesBinding
import com.example.visionhuntonline.ui.adapters.CapturedImagesAdapter

class CapturedImagesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCapturedImagesBinding
    private lateinit var dbHelper: CapturedItemDbHelper
    private val adapter = CapturedImagesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCapturedImagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val density = resources.displayMetrics.density
        val basePadding = (16 * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                basePadding,
                basePadding + systemBars.top,
                basePadding,
                basePadding + systemBars.bottom
            )
            insets
        }

        dbHelper = CapturedItemDbHelper(this)

        binding.toolbarTitle.text = "الصور المكتشفة"

        binding.rvCaptured.layoutManager = LinearLayoutManager(this)
        binding.rvCaptured.adapter = adapter

        loadCapturedImages()
    }

    private fun loadCapturedImages() {
        val items = dbHelper.getAllItems()
        adapter.submitList(items)
        if (items.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvCaptured.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvCaptured.visibility = View.VISIBLE
        }
    }
}
