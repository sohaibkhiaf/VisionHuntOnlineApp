package com.example.visionhuntonline

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.visionhuntonline.data.UserSessionManager
import com.example.visionhuntonline.databinding.ActivityMainBinding
import com.example.visionhuntonline.ui.fragments.CameraFragment
import com.example.visionhuntonline.ui.fragments.TasksFragment
import com.example.visionhuntonline.ui.screens.AuthActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tasksFragment by lazy {
        TasksFragment().apply {
            onStartCameraClick = {
                binding.bottomNavigation.selectedItemId = R.id.nav_camera
            }
        }
    }

    private val cameraFragment by lazy {
        CameraFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)

        val sessionManager = UserSessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            loadFragment(tasksFragment)
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> {
                    loadFragment(tasksFragment)
                    true
                }
                R.id.nav_camera -> {
                    loadFragment(cameraFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
