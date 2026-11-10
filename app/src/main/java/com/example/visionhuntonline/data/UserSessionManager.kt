package com.example.visionhuntonline.data

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSession(token: String, username: String, phone: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PHONE, phone ?: "")
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun updatePhone(newPhone: String) {
        prefs.edit().putString(KEY_PHONE, newPhone).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getPhone(): String = prefs.getString(KEY_PHONE, "") ?: ""

    fun isLoggedIn(): Boolean =
        prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !getToken().isNullOrEmpty()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_PHONE)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "vision_hunt_user_session"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_PHONE = "phone"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
