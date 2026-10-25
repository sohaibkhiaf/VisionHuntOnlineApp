package com.example.visionhuntonline.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.visionhuntonline.data.UserSessionManager
import com.example.visionhuntonline.databinding.ActivityProfileBinding
import com.example.visionhuntonline.network.ApiClient
import com.example.visionhuntonline.util.NetworkDialogHelper
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var sessionManager: UserSessionManager
    private val networkHelper = NetworkDialogHelper(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        networkHelper.monitorNetwork(this)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        loadUserData()
        setupListeners()
    }

    private fun loadUserData() {
        binding.etUsername.setText(sessionManager.getUsername())
        binding.etPhone.setText(sessionManager.getPhone())
    }

    private fun setupListeners() {
        binding.btnUpdateProfile.setOnClickListener {
            handleProfileUpdate()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("تسجيل الخروج")
            .setMessage("هل أنت متأكد من رغبتك في تسجيل الخروج؟")
            .setPositiveButton("تسجيل الخروج") { _, _ ->
                handleLogout()
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun handleProfileUpdate() {
        if (!networkHelper.checkAndShowDialog()) return

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            com.example.visionhuntonline.util.GameToast.show(this, "جلسة المستخدم انتهت، يرجى إعادة تسجيل الدخول", Toast.LENGTH_SHORT)
            navigateToAuth()
            return
        }

        val oldPassword = binding.etOldPassword.text?.toString()?.trim() ?: ""
        val newPhone = binding.etPhone.text?.toString()?.trim() ?: ""
        val newPassword = binding.etNewPassword.text?.toString()?.trim() ?: ""

        if (oldPassword.isEmpty()) {
            com.example.visionhuntonline.util.GameToast.show(this, "يرجى إدخال كلمة المرور الحالية لتأكيد التحديث", Toast.LENGTH_SHORT)
            binding.etOldPassword.requestFocus()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = ApiClient.updateProfile(
                token = token,
                oldPassword = oldPassword,
                phone = newPhone.ifEmpty { null },
                newPassword = newPassword.ifEmpty { null }
            )
            setLoading(false)

            if (result.isSuccess) {
                if (newPhone.isNotEmpty()) {
                    sessionManager.updatePhone(newPhone)
                }
                binding.etOldPassword.setText("")
                binding.etNewPassword.setText("")
                com.example.visionhuntonline.util.GameToast.show(this@ProfileActivity, result.message ?: "تم تحديث البيانات بنجاح", Toast.LENGTH_LONG)
            } else {
                com.example.visionhuntonline.util.GameToast.show(this@ProfileActivity, result.message ?: "فشل تحديث البيانات", Toast.LENGTH_LONG)
            }
        }
    }

    private fun handleLogout() {
        if (!networkHelper.checkAndShowDialog()) return

        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            sessionManager.clearSession()
            navigateToAuth()
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            val result = ApiClient.logout(token)
            setLoading(false)

            if (result.isSuccess) {
                sessionManager.clearSession()
                com.example.visionhuntonline.util.GameToast.show(this@ProfileActivity, result.message ?: "تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT)
                navigateToAuth()
            } else {
                com.example.visionhuntonline.util.GameToast.show(this@ProfileActivity, result.message ?: "فشل الاتصال بالسيرفر لتسجيل الخروج", Toast.LENGTH_LONG)
            }
        }
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressProfile.visibility = View.VISIBLE
            binding.btnUpdateProfile.isEnabled = false
            binding.btnLogout.isEnabled = false
        } else {
            binding.progressProfile.visibility = View.GONE
            binding.btnUpdateProfile.isEnabled = true
            binding.btnLogout.isEnabled = true
        }
    }
}
