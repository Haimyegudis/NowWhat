package com.nowwhat.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.screens.OnboardingScreen
import com.nowwhat.app.theme.NowWhatTheme
import com.nowwhat.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    // Calendar permission launcher
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Calendar permission granted")
            viewModel.refreshCalendarEvents()
        } else {
            Log.w("MainActivity", "Calendar permission denied")
        }
    }

    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
            Toast.makeText(this, "Notifications disabled. You won't receive reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        try {
            val preferences = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
            val languageCode = preferences.getString("language", "en") ?: "en"
            val requestedLocale = LanguageManager.getLocale(languageCode)

            // Get current locale safely
            val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                newBase.resources.configuration.locales.get(0)
            } else {
                @Suppress("DEPRECATION")
                newBase.resources.configuration.locale
            }

            // Only change language if it's different
            val context = if (currentLocale.language != requestedLocale.language) {
                Log.d("MainActivity", "Changing language from ${currentLocale.language} to ${requestedLocale.language}")
                LanguageManager.setLocale(newBase, requestedLocale)
            } else {
                newBase
            }

            super.attachBaseContext(context)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in attachBaseContext", e)
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "onCreate called")

        // Request calendar permission
        requestCalendarPermission()

        // Request notification permission (Android 13+)
        requestNotificationPermission()

        // Request exact alarm permission (Android 12+)
        requestExactAlarmPermission()

        try {
            setContent {
                NowWhatTheme {
                    NowWhatApp(viewModel = viewModel)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in setContent", e)
        }
    }

    private fun requestCalendarPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Requesting calendar permission")
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                } else {
                    Log.d("MainActivity", "Calendar permission already granted")
                    viewModel.refreshCalendarEvents()
                }
            } else {
                viewModel.refreshCalendarEvents()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling calendar permission", e)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("MainActivity", "Requesting notification permission")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MainActivity", "Notification permission already granted")
            }
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.d("MainActivity", "Exact alarm permission not granted")
                try {
                    val intent = android.content.Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error requesting exact alarm permission", e)
                    Toast.makeText(
                        this,
                        "Please enable 'Alarms & reminders' permission in settings for task reminders",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                Log.d("MainActivity", "Exact alarm permission already granted")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")

        try {
            // Refresh calendar when returning to app
            viewModel.refreshCalendarEvents()

            // Check if permissions were granted while app was in background
            checkPermissionsStatus()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
        }
    }

    private fun checkPermissionsStatus() {
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasNotificationPermission) {
                Log.w("MainActivity", "Notification permission still not granted")
            }
        }

        // Check exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("MainActivity", "Exact alarm permission still not granted")
            }
        }

        // Check calendar permission
        val hasCalendarPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCalendarPermission) {
            Log.w("MainActivity", "Calendar permission still not granted")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
    }
}

@Composable
fun NowWhatApp(viewModel: MainViewModel) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    var isFirstTimeSetup by remember { mutableStateOf(false) }

    if (user == null) {
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            try {
                kotlinx.coroutines.delay(100)
            } catch (e: Exception) {
                Log.e("NowWhatApp", "Error in loading delay", e)
            } finally {
                isLoading = false
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Log.d("NowWhatApp", "Showing OnboardingScreen")
            OnboardingScreen(
                userPreferences = viewModel.userPreferences,
                onFinish = {
                    isFirstTimeSetup = true
                    viewModel.refreshCalendarEvents()
                }
            )
        }
    } else {
        Log.d("NowWhatApp", "Showing AppNavigation")
        AppNavigation(
            viewModel = viewModel,
            startDestination = Routes.Dashboard
        )
    }
}