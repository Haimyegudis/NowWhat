package com.nowwhat.app.screens

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.data.UserPreferences
import com.nowwhat.app.model.AppLanguage
import com.nowwhat.app.model.Gender
import com.nowwhat.app.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    userPreferences: UserPreferences,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val sharedPrefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val initialLanguageCode = sharedPrefs.getString("language", "en") ?: "en"
    val initialLanguage = LanguageManager.getAppLanguage(initialLanguageCode)

    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.NotSpecified) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var startHour by remember { mutableIntStateOf(9) }
    var endHour by remember { mutableIntStateOf(17) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var focusDndDuration by remember { mutableIntStateOf(30) }

    // Handle language change
    LaunchedEffect(selectedLanguage) {
        val newLanguageCode = LanguageManager.getLanguageCode(selectedLanguage)
        val currentLanguageCode = sharedPrefs.getString("language", "en")

        if (newLanguageCode != currentLanguageCode) {
            sharedPrefs.edit().putString("language", newLanguageCode).apply()
            activity?.recreate()
        }
    }

    val startTimePicker = remember {
        TimePickerDialog(
            context,
            { _, hour, _ -> startHour = hour },
            startHour, 0, true
        )
    }

    val endTimePicker = remember {
        TimePickerDialog(
            context,
            { _, hour, _ -> endHour = hour },
            endHour, 0, true
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "🚀 " + stringResource(R.string.onboarding_welcome),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.onboarding_subtitle),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            item {
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_language_label),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppLanguage.values().forEach { language ->
                            FilterChip(
                                selected = selectedLanguage == language,
                                onClick = { selectedLanguage = language },
                                label = {
                                    Text(
                                        getLanguageDisplayName(language),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.onboarding_name_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text(stringResource(R.string.onboarding_role_label)) },
                    placeholder = { Text(stringResource(R.string.onboarding_role_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_gender_label),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Gender.values().forEach { gender ->
                            FilterChip(
                                selected = selectedGender == gender,
                                onClick = { selectedGender = gender },
                                label = {
                                    Text(
                                        getGenderString(gender),
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "⏰ " + stringResource(R.string.onboarding_work_hours),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { startTimePicker.show() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.time_start), fontSize = 12.sp)
                                Text(
                                    String.format("%02d:00", startHour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { endTimePicker.show() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.time_end), fontSize = 12.sp)
                                Text(
                                    String.format("%02d:00", endHour),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "📅 " + stringResource(R.string.onboarding_work_days),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    WorkDaysSelector(
                        selectedDays = selectedDays,
                        onDaysChange = { selectedDays = it }
                    )
                }
            }

            item {
                Column {
                    Text(
                        text = "🔇 " + stringResource(R.string.onboarding_focus_dnd),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 60, 90).forEach { minutes ->
                            FilterChip(
                                selected = focusDndDuration == minutes,
                                onClick = { focusDndDuration = minutes },
                                label = { Text("${minutes}m") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank() && role.isNotBlank()) {
                            val user = UserProfile(
                                name = name,
                                role = role,
                                gender = selectedGender,
                                language = selectedLanguage,
                                startWorkHour = startHour,
                                endWorkHour = endHour,
                                workDays = selectedDays,
                                focusModeDndDuration = focusDndDuration,
                                focusDndMinutes = focusDndDuration,
                                streak = 0,
                                breakReminder = true
                            )
                            scope.launch {
                                userPreferences.saveUserProfile(user)
                                onFinish()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = name.isNotBlank() && role.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6200EE)
                    )
                ) {
                    Text(
                        stringResource(R.string.onboarding_start_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun getGenderString(gender: Gender): String {
    return when (gender) {
        Gender.Male -> stringResource(R.string.gender_male)
        Gender.Female -> stringResource(R.string.gender_female)
        Gender.NotSpecified -> stringResource(R.string.gender_not_specified)
    }
}

@Composable
fun getLanguageDisplayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> "English"
        AppLanguage.Hebrew -> "עברית"
        AppLanguage.Russian -> "Русский"
    }
}

@Composable
fun WorkDaysSelector(
    selectedDays: Set<Int>,
    onDaysChange: (Set<Int>) -> Unit
) {
    val dayNames = listOf(
        1 to stringResource(R.string.day_sun),
        2 to stringResource(R.string.day_mon),
        3 to stringResource(R.string.day_tue),
        4 to stringResource(R.string.day_wed),
        5 to stringResource(R.string.day_thu),
        6 to stringResource(R.string.day_fri),
        7 to stringResource(R.string.day_sat)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        dayNames.forEach { (dayNum, dayName) ->
            FilterChip(
                selected = selectedDays.contains(dayNum),
                onClick = {
                    val newDays = if (selectedDays.contains(dayNum)) {
                        selectedDays - dayNum
                    } else {
                        selectedDays + dayNum
                    }
                    onDaysChange(newDays)
                },
                label = {
                    Text(
                        dayName,
                        fontSize = 12.sp,
                        fontWeight = if (selectedDays.contains(dayNum)) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF6200EE),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}