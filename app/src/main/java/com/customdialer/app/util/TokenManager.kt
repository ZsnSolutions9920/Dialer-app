package com.customdialer.app.util

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_dialer_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AGENT_ID = "agent_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CUSTOMER_ID = "customer_id"
        private const val KEY_CUSTOMER_EMAIL = "customer_email"
        private const val KEY_CUSTOMER_NAME = "customer_name"
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveAgentInfo(id: Int, username: String, displayName: String?) {
        prefs.edit()
            .putInt(KEY_AGENT_ID, id)
            .putString(KEY_USERNAME, username)
            .putString(KEY_DISPLAY_NAME, displayName)
            .apply()
    }

    fun getAgentId(): Int = prefs.getInt(KEY_AGENT_ID, -1)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun saveCustomerInfo(id: Int, email: String, name: String?) {
        prefs.edit()
            .putInt(KEY_CUSTOMER_ID, id)
            .putString(KEY_CUSTOMER_EMAIL, email)
            .putString(KEY_CUSTOMER_NAME, name)
            .apply()
    }

    fun getCustomerId(): Int = prefs.getInt(KEY_CUSTOMER_ID, -1)
    fun getCustomerEmail(): String? = prefs.getString(KEY_CUSTOMER_EMAIL, null)
    fun getCustomerName(): String? = prefs.getString(KEY_CUSTOMER_NAME, null)

    fun saveBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    fun getBaseUrl(): String? = prefs.getString(KEY_BASE_URL, null)

    fun saveDarkMode(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun clearAll() {
        val darkMode = isDarkMode()
        val baseUrl = getBaseUrl()
        prefs.edit().clear().apply()
        saveDarkMode(darkMode)
        if (baseUrl != null) saveBaseUrl(baseUrl)
    }

    fun isLoggedIn(): Boolean = getToken() != null
}
