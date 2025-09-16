package com.example.taskmaster.utils

import android.content.Context
import android.content.SharedPreferences


class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "TaskMasterPrefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        // --- ADD THIS NEW KEY ---
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LANGUAGE = "language_preference"
    }

    fun setBiometricEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, isEnabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    // --- ADD THESE NEW FUNCTIONS ---
    fun setNotificationsEnabled(isEnabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, isEnabled).apply()
    }

    fun areNotificationsEnabled(): Boolean {
        // Default to true to encourage users to use the feature
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    fun getLanguage(): String {
        // Default to English ("en")
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }
}