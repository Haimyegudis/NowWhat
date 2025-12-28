package com.nowwhat.app.screens

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.nowwhat.app.R
import com.nowwhat.app.data.CalendarInfo
import com.nowwhat.app.data.CalendarRepository
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

    // --- Calendar Repo & Permissions ---
    val calendarRepository = remember { CalendarRepository(context) }
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            availableCalendars = calendarRepository.getAvailableCalendars()
            if (availableCalendars.isNotEmpty()) selectedCalendar = availableCalendars[0]
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            availableCalendars = calendarRepository.getAvailableCalendars()
            if (availableCalendars.isNotEmpty()) selectedCalendar = availableCalendars[0]
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    // --- Language Logic ---
    val sharedPrefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    val initialLanguageCode = sharedPrefs.getString("language", "en") ?: "en"
    val initialLanguage = LanguageManager.getAppLanguage(initialLanguageCode)

    // --- State ---
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf(Gender.NotSpecified) }
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var startHour by remember { mutableIntStateOf(9) }
    var endHour by remember { mutableIntStateOf(17) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var focusDndDuration by remember { mutableIntStateOf(30) }

    // Handle language change immediately
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

    val isFormValid = name.isNotBlank() && role.isNotBlank()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isFormValid) {
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
                            breakReminder = true,
                            calendarId = selectedCalendar?.id ?: -1L
                        )
                        scope.launch {
                            userPreferences.saveUserProfile(user)
                            onFinish()
                        }
                    }
                },
                containerColor = if (isFormValid) MaterialTheme.colorScheme.primary else Color.Gray,
                contentColor = if (isFormValid) MaterialTheme.colorScheme.onPrimary else Color.White
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Start")
            }
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
                Column {
                    Text(
                        text = stringResource(R.string.onboarding_welcome),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // --- Card 1: Personal Info ---
            item {
                OnboardingCard(title = "Who are you?", icon = Icons.Default.Person) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.onboarding_name_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_name_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(stringResource(R.string.onboarding_role_label)) },
                        placeholder = { Text(stringResource(R.string.onboarding_role_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            AppDropdownSelector(
                                label = stringResource(R.string.onboarding_gender_label),
                                options = Gender.values().toList(),
                                selectedOption = selectedGender,
                                onOptionSelected = { selectedGender = it },
                                displayMapper = { getGenderString(context, it) }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            AppDropdownSelector(
                                label = stringResource(R.string.onboarding_language_label),
                                options = AppLanguage.values().toList(),
                                selectedOption = selectedLanguage,
                                onOptionSelected = { selectedLanguage = it },
                                displayMapper = { getLanguageDisplayName(it) }
                            )
                        }
                    }
                }
            }

            // --- Card 2: Work Preferences ---
            item {
                OnboardingCard(title = "Work Preferences", icon = Icons.Default.Work) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { startTimePicker.show() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("${stringResource(R.string.time_start)}: ${String.format("%02d:00", startHour)}")
                        }
                        Button(
                            onClick = { endTimePicker.show() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("${stringResource(R.string.time_end)}: ${String.format("%02d:00", endHour)}")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.onboarding_work_days), fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    WorkDaysSelector(
                        selectedDays = selectedDays,
                        onDaysChange = { selectedDays = it }
                    )
                }
            }

            // --- Card 3: Setup & Integrations ---
            item {
                OnboardingCard(title = "Setup & Integrations", icon = Icons.Default.CalendarToday) {
                    val opts = listOf(CalendarInfo(-1, "All Calendars", "", "", 0, true)) + availableCalendars
                    AppDropdownSelector(
                        label = "Primary Calendar",
                        options = opts,
                        selectedOption = selectedCalendar ?: opts.first(),
                        onOptionSelected = { selectedCalendar = if (it.id == -1L) null else it },
                        displayMapper = { it.displayName }
                    )
                    Spacer(Modifier.height(12.dp))
                    AppDropdownSelector(
                        label = stringResource(R.string.onboarding_focus_dnd),
                        options = listOf(15, 30, 45, 60, 90, 120),
                        selectedOption = focusDndDuration,
                        onOptionSelected = { focusDndDuration = it },
                        displayMapper = { "$it min" }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// --- Helper Composables (PRIVATE to avoid conflicts) ---

@Composable
private fun OnboardingCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            content()
        }
    }
}

@Composable
private fun WorkDaysSelector(
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
            val isSelected = selectedDays.contains(dayNum)
            FilterChip(
                selected = isSelected,
                onClick = {
                    val newDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                    onDaysChange(newDays)
                },
                label = {
                    Text(
                        dayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> AppDropdownSelector(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    displayMapper: (T) -> String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayMapper(selectedOption),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(displayMapper(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun getGenderString(context: Context, gender: Gender): String {
    return when (gender) {
        Gender.Male -> context.getString(R.string.gender_male)
        Gender.Female -> context.getString(R.string.gender_female)
        Gender.NotSpecified -> context.getString(R.string.gender_not_specified)
    }
}

private fun getLanguageDisplayName(language: AppLanguage): String {
    return when (language) {
        AppLanguage.English -> "English"
        AppLanguage.Hebrew -> "עברית"
        AppLanguage.Russian -> "Русский"
    }
}