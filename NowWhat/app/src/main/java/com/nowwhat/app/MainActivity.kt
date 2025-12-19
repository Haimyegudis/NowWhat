package com.nowwhat.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.screens.OnboardingScreen
import com.nowwhat.app.theme.NowWhatTheme
import com.nowwhat.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Calendar permission granted")
            viewModel.refreshCalendarEvents()
        } else {
            Log.w("MainActivity", "Calendar permission denied")
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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Requesting calendar permission")
                    requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
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

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")

        try {
            // Refresh calendar when returning to app
            viewModel.refreshCalendarEvents()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onResume", e)
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
            // No try-catch around Composables - Compose doesn't allow it
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
        // No try-catch around Composables - Compose doesn't allow it
        Log.d("NowWhatApp", "Showing AppNavigation")
        AppNavigation(
            viewModel = viewModel,
            startDestination = Routes.Dashboard
        )
    }
}