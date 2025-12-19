package com.nowwhat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.nowwhat.app.data.AppDatabase
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.model.AppLanguage
import com.nowwhat.app.utils.CrashHandler

class NowWhatApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Setup crash handler FIRST
        try {
            CrashHandler.setup(this)
            Log.d("NowWhatApp", "Crash handler initialized")
        } catch (e: Exception) {
            Log.e("NowWhatApp", "Failed to setup crash handler", e)
        }

        createNotificationChannel()
    }

    override fun attachBaseContext(base: Context) {
        // Default to English on first launch
        val sharedPrefs = base.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = sharedPrefs.getString("language", "en") ?: "en"
        val locale = LanguageManager.getLocale(languageCode)

        // Prevent recreation loop
        val currentLocale = base.resources.configuration.locales[0]
        val context = if (currentLocale.language != locale.language) {
            Log.d("NowWhatApp", "Changing language from ${currentLocale.language} to ${locale.language}")
            LanguageManager.setLocale(base, locale)
        } else {
            base
        }

        super.attachBaseContext(context)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    "task_reminders",
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for task reminders and focus mode"
                }

                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
                Log.d("NowWhatApp", "Notification channel created")
            } catch (e: Exception) {
                Log.e("NowWhatApp", "Failed to create notification channel", e)
            }
        }
    }
}