package com.nowwhat.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nowwhat.app.MainActivity
import com.nowwhat.app.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "task_reminders_channel"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Task Reminder"

        if (taskId != -1) {
            showNotification(context, taskId, taskTitle)
        }
    }

    private fun showNotification(context: Context, taskId: Int, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // יצירת ערוץ התראות (חובה באנדרואיד 8.0 ומעלה)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // לחיצה על ההתראה תפתח את האפליקציה
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_task_id", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // וודא שיש לך אייקון מתאים
            .setContentTitle("Task Reminder 🔔")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId, notification)
    }
}