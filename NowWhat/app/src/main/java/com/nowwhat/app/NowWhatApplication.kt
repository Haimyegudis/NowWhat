package com.nowwhat.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.nowwhat.app.data.AppDatabase
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.model.AppLanguage
import java.util.Locale

class NowWhatApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun attachBaseContext(base: Context) {
        // Default to English on first launch
        super.attachBaseContext(LanguageManager.setLocale(base, AppLanguage.English))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "task_reminders",
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders and focus mode"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}