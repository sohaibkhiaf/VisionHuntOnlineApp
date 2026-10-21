package com.example.visionhuntonline.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.visionhuntonline.MainActivity
import com.example.visionhuntonline.R
import com.example.visionhuntonline.data.UserSessionManager
import com.example.visionhuntonline.databinding.ActivityAuthBinding
import com.example.visionhuntonline.network.ApiClient
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var sessionManager: UserSessionManager
    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = UserSessionManager(this)

        setupUI()
    }

    private fun setupUI() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isRegisterMode = checkedId == R.id.btn_mode_register
                updateFormMode()
            }
        }

        binding.btnSubmit.setOnClickListener {
            handleFormSubmit()
        }
    }

    private fun updateFormMode() {
        binding.tvError.visibility = View.GONE
        if (isRegisterMode) {
            binding.tilPhone.visibility = View.VISIBLE
            binding.btnSubmit.text = "إنشاء حساب"
            binding.tvAuthSubtitle.text = "قم بإنشاء حساب جديد للانضمام إلى اللعبة"
        } else {
            binding.tilPhone.visibility = View.GONE
            binding.btnSubmit.text = "تسجيل الدخول"
            binding.tvAuthSubtitle.text = "سجل دخولك أو أنشئ حساباً للبدء باللعب"
        }
    }

    private fun handleFormSubmit() {
        val username = binding.etUsername.text?.toString()?.trim() ?: ""
        val password = binding.etPassword.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""

        binding.tvError.visibility = View.GONE

        if (username.isEmpty()) {
            showError("يرجى إدخال اسم المستخدم")
            return
        }

        if (password.isEmpty()) {
            showError("يرجى إدخال كلمة المرور")
            return
        }

        if (isRegisterMode && phone.isEmpty()) {
            showError("يرجى إدخال رقم الهاتف")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            if (isRegisterMode) {
                val result = ApiClient.register(username, password, phone)
                setLoading(false)
                if (result.isSuccess && result.data != null) {
                    val token = result.data.token
                    if (token.isNotEmpty()) {
                        sessionManager.saveSession(
                            token = token,
                            username = username,
                            phone = phone
                        )
                        Toast.makeText(
                            this@AuthActivity,
                            result.message ?: "تم إنشاء الحساب بنجاح",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMain()
                    } else {
                        // If register succeeded without token, switch to login mode automatically
                        Toast.makeText(
                            this@AuthActivity,
                            "تم إنشاء الحساب بنجاح! جاري تسجيل الدخول...",
                            Toast.LENGTH_SHORT
                        ).show()
                        attemptLogin(username, password)
                    }
                } else {
                    showError(result.message ?: "فشل إنشاء الحساب")
                }
            } else {
                attemptLogin(username, password)
            }
        }
    }

    private suspend fun attemptLogin(username: String, password: String) {
        val result = ApiClient.login(username, password)
        setLoading(false)
        if (result.isSuccess && result.data != null) {
            sessionManager.saveSession(
                token = result.data.token,
                username = username,
                phone = result.data.phone
            )
            Toast.makeText(
                this@AuthActivity,
                result.message ?: "تم تسجيل الدخول بنجاح",
                Toast.LENGTH_SHORT
            ).show()
            navigateToMain()
        } else {
            showError(result.message ?: "فشل تسجيل الدخول")
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            binding.progressIndicator.visibility = View.VISIBLE
            binding.btnSubmit.isEnabled = false
            binding.toggleGroup.isEnabled = false
        } else {
            binding.progressIndicator.visibility = View.GONE
            binding.btnSubmit.isEnabled = true
            binding.toggleGroup.isEnabled = true
        }
    }
}
