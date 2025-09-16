package com.example.taskmaster.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.taskmaster.MainActivity
import java.util.Locale

object LocaleManager {
    fun setLocale(context: Context, languageCode: String) {
        // Save the user's choice first
        SettingsManager(context).setLanguage(languageCode)

        // Method 1: Using AppCompatDelegate (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val appLocale = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        } else {
            // Method 2: Manual configuration for older versions
            updateConfiguration(context, languageCode)
            restartActivity(context)
        }
    }

    private fun updateConfiguration(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun restartActivity(context: Context) {
        if (context is Activity) {
            context.recreate() // This is better than starting a new activity
        } else {
            // Fallback if context is not an Activity
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
}