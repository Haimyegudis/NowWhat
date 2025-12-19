package com.nowwhat.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler {
    private const val TAG = "CrashHandler"
    private const val CRASH_LOG_FILE = "crash_logs.txt"

    fun setup(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }

            // Call the default handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun logCrash(context: Context, thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val crashInfo = buildString {
            appendLine("==================== CRASH ====================")
            appendLine("Time: $timestamp")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable.javaClass.simpleName}")
            appendLine("Message: ${throwable.message}")
            appendLine("\nStack Trace:")
            appendLine(getStackTrace(throwable))
            appendLine("==============================================\n")
        }

        // Log to Logcat
        Log.e(TAG, crashInfo)

        // Save to file
        try {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            crashFile.appendText(crashInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash to file", e)
        }
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }

    fun readCrashLogs(context: Context): String {
        return try {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            if (crashFile.exists()) {
                crashFile.readText()
            } else {
                "No crash logs found"
            }
        } catch (e: Exception) {
            "Error reading crash logs: ${e.message}"
        }
    }

    fun clearCrashLogs(context: Context) {
        try {
            val crashFile = File(context.filesDir, CRASH_LOG_FILE)
            if (crashFile.exists()) {
                crashFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash logs", e)
        }
    }
}