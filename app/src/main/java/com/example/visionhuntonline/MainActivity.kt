package com.example.visionhuntonline

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.visionhuntonline.data.UserSessionManager
import com.example.visionhuntonline.databinding.ActivityMainBinding
import com.example.visionhuntonline.ui.fragments.CameraFragment
import com.example.visionhuntonline.ui.fragments.TasksFragment
import com.example.visionhuntonline.ui.screens.AuthActivity
import com.example.visionhuntonline.ui.viewmodel.GameViewModel
import com.example.visionhuntonline.util.NetworkDialogHelper
import com.example.visionhuntonline.util.ServerHealthHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val networkHelper = NetworkDialogHelper(this)
    private val serverHealthHelper = ServerHealthHelper(this)
    private val viewModel: GameViewModel by viewModels()

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

        networkHelper.monitorNetwork(this)

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

        serverHealthHelper.checkHealthAndShowDialog(lifecycleScope) {
            viewModel.fetchAndSyncAccountState(this)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sessionExpiredEvent.collect { expired ->
                        if (expired) {
                            Toast.makeText(this@MainActivity, "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@MainActivity, AuthActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                            viewModel.resetSessionExpiredEvent()
                        }
                    }
                }
                launch {
                    viewModel.serverErrorSnackbarEvent.collect { msg ->
                        if (!msg.isNullOrEmpty()) {
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                                .setAction("إعادة المحاولة") {
                                    serverHealthHelper.checkHealthAndShowDialog(lifecycleScope) {
                                        viewModel.fetchAndSyncAccountState(this@MainActivity)
                                    }
                                }
                                .setBackgroundTint(Color.parseColor("#B91C1C"))
                                .setTextColor(Color.WHITE)
                                .setActionTextColor(Color.parseColor("#FEF08A"))
                                .show()
                            viewModel.resetServerErrorSnackbarEvent()
                        }
                    }
                }
            }
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

    override fun onResume() {
        super.onResume()
        if (UserSessionManager(this).isLoggedIn()) {
            serverHealthHelper.checkHealthAndShowDialog(lifecycleScope) {
                viewModel.fetchAndSyncAccountState(this)
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
