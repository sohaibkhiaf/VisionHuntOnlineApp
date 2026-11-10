package com.example.visionhuntonline

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.example.visionhuntonline.util.GameToast
import com.example.visionhuntonline.util.NetworkDialogHelper
import com.example.visionhuntonline.util.ServerHealthHelper
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

    private var backPressedTime: Long = 0

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
                            GameToast.show(this@MainActivity, "انتهت صلاحية الجلسة، يرجى إعادة تسجيل الدخول", Toast.LENGTH_LONG)
                            val intent = Intent(this@MainActivity, AuthActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                            viewModel.resetSessionExpiredEvent()
                        }
                    }
                }
                launch {
                    viewModel.serverErrorSnackbarEvent.collect { notice ->
                        if (notice != null) {
                            val banner = binding.layoutErrorBanner.cardErrorBanner
                            banner.alpha = 1f
                            banner.translationX = 0f
                            banner.translationY = 0f
                            banner.visibility = View.VISIBLE

                            binding.layoutErrorBanner.tvErrorTitle.text = notice.title
                            binding.layoutErrorBanner.tvErrorSubtitle.text = notice.message

                            if (notice.isNoActiveEvent) {
                                banner.setBackgroundResource(R.drawable.bg_3d_card_16dp)
                                val isDark = isDarkTheme()
                                binding.layoutErrorBanner.tvErrorTitle.setTextColor(
                                    if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
                                )
                                binding.layoutErrorBanner.tvErrorSubtitle.setTextColor(
                                    if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
                                )
                                binding.layoutErrorBanner.btnRetryError.text = "فهمت"
                                binding.layoutErrorBanner.btnRetryError.setBackgroundResource(R.drawable.bg_3d_btn_primary)
                                binding.layoutErrorBanner.btnRetryError.setTextColor(Color.WHITE)

                                binding.layoutErrorBanner.btnRetryError.setOnClickListener {
                                    banner.visibility = View.GONE
                                    viewModel.resetServerErrorSnackbarEvent()
                                }
                            } else {
                                banner.setBackgroundResource(R.drawable.bg_3d_card_danger)
                                binding.layoutErrorBanner.tvErrorTitle.setTextColor(Color.WHITE)
                                binding.layoutErrorBanner.tvErrorSubtitle.setTextColor(Color.parseColor("#FEE2E2"))
                                binding.layoutErrorBanner.btnRetryError.text = "إعادة المحاولة"
                                binding.layoutErrorBanner.btnRetryError.setBackgroundResource(R.drawable.bg_3d_btn_gold)
                                binding.layoutErrorBanner.btnRetryError.setTextColor(Color.parseColor("#78350F"))

                                binding.layoutErrorBanner.btnRetryError.setOnClickListener {
                                    banner.visibility = View.GONE
                                    viewModel.resetServerErrorSnackbarEvent()
                                    serverHealthHelper.checkHealthAndShowDialog(lifecycleScope) {
                                        viewModel.fetchAndSyncAccountState(this@MainActivity)
                                    }
                                }
                            }

                            setupSwipeToDismiss(banner) {
                                viewModel.resetServerErrorSnackbarEvent()
                            }
                        }
                    }
                }
                launch {
                    viewModel.appUpdateEvent.collect { versionData ->
                        if (versionData != null) {
                            val banner = binding.layoutUpdateBanner.cardUpdateBanner
                            banner.alpha = 1f
                            banner.translationX = 0f
                            banner.translationY = 0f
                            banner.visibility = View.VISIBLE
                            binding.layoutUpdateBanner.tvUpdateTitle.text = "🚀 تحديث جديد v${versionData.version}"
                            binding.layoutUpdateBanner.tvUpdateSubtitle.text = "يتوفر إصدار جديد جاهز للتحميل والتثبيت."

                            binding.layoutUpdateBanner.btnDownloadUpdate.setOnClickListener {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(versionData.downloadUrl))
                                    startActivity(intent)
                                } catch (_: Exception) {
                                    GameToast.show(this@MainActivity, "تعذر فتح رابط التحميل", Toast.LENGTH_SHORT)
                                }
                            }

                            setupSwipeToDismiss(banner)
                        } else {
                            binding.layoutUpdateBanner.cardUpdateBanner.visibility = View.GONE
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.bottomNavigation.selectedItemId != R.id.nav_tasks) {
                    binding.bottomNavigation.selectedItemId = R.id.nav_tasks
                } else {
                    if (backPressedTime + 2000 > System.currentTimeMillis()) {
                        finish()
                    } else {
                        GameToast.show(this@MainActivity, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT)
                        backPressedTime = System.currentTimeMillis()
                    }
                }
            }
        })
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    private fun setupSwipeToDismiss(view: View, onDismissed: (() -> Unit)? = null) {
        var initialX = 0f
        var initialY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.rawX
                    initialY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialX
                    val deltaY = event.rawY - initialY

                    if (deltaX > 0 || deltaY > 0) {
                        if (Math.abs(deltaX) > Math.abs(deltaY)) {
                            view.translationX = Math.max(0f, deltaX)
                        } else {
                            view.translationY = Math.max(0f, deltaY)
                        }
                        val maxDist = 300f
                        val progress = Math.max(Math.max(0f, deltaX), Math.max(0f, deltaY)) / maxDist
                        view.alpha = (1f - progress).coerceIn(0f, 1f)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaX = event.rawX - initialX
                    val deltaY = event.rawY - initialY
                    val dismissThreshold = 100f

                    if (deltaX > dismissThreshold || deltaY > dismissThreshold) {
                        val anim = if (deltaX > deltaY) view.animate().translationX(view.width.toFloat()) else view.animate().translationY(view.height.toFloat())
                        anim.alpha(0f).setDuration(200).withEndAction {
                            view.visibility = View.GONE
                            view.translationX = 0f
                            view.translationY = 0f
                            view.alpha = 1f
                            onDismissed?.invoke()
                        }.start()
                    } else {
                        view.animate().translationX(0f).translationY(0f).alpha(1f).setDuration(150).start()
                    }
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
