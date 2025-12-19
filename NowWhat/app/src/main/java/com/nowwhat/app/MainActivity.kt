package com.nowwhat.app

import android.content.Context
import android.os.Bundle
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
import com.nowwhat.app.screens.DashboardScreen
import com.nowwhat.app.screens.OnboardingScreen
import com.nowwhat.app.theme.NowWhatTheme
import com.nowwhat.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val requestPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refreshCalendarEvents()
        }
    }
    override fun attachBaseContext(newBase: Context) {
        val preferences = newBase.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val languageCode = preferences.getString("language", "en") ?: "en"
        val locale = LanguageManager.getLocale(languageCode)
        val context = LanguageManager.setLocale(newBase, locale)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request calendar permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_CALENDAR)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
            } else {
                viewModel.refreshCalendarEvents()
            }
        } else {
            viewModel.refreshCalendarEvents()
        }

        setContent {
            NowWhatTheme {
                NowWhatApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NowWhatApp(viewModel: MainViewModel) {
    // Observe user profile
    val user by viewModel.user.collectAsStateWithLifecycle()

    // Track if this is first time completing onboarding
    var isFirstTimeSetup by remember { mutableStateOf(false) }

    // Show loading while checking user
    if (user == null) {
        // Check if we're still loading or truly no user
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(100)
            isLoading = false
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // No user - show onboarding
            OnboardingScreen(
                userPreferences = viewModel.userPreferences,
                onFinish = {
                    isFirstTimeSetup = true
                    viewModel.refreshCalendarEvents()
                }
            )
        }
    } else {
        // User exists - show app with navigation
        AppNavigation(
            viewModel = viewModel,
            startDestination = Routes.Dashboard
        )
    }
}