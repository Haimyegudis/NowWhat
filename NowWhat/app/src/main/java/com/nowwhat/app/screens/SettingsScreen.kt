package com.nowwhat.app.screens

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import com.nowwhat.app.R
import com.nowwhat.app.data.CalendarInfo
import com.nowwhat.app.data.CalendarRepository
import com.nowwhat.app.data.LanguageManager
import com.nowwhat.app.model.AppLanguage
import com.nowwhat.app.model.Gender
import com.nowwhat.app.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserProfile,
    onBackClick: () -> Unit,
    onSaveSettings: (UserProfile) -> Unit,
    onClearData: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(user.name) }
    var role by remember { mutableStateOf(user.role) }
    var selectedGender by remember { mutableStateOf(user.gender) }
    var selectedLanguage by remember { mutableStateOf(user.language) }
    var startHour by remember { mutableIntStateOf(user.startWorkHour) }
    var endHour by remember { mutableIntStateOf(user.endWorkHour) }
    var selectedDays by remember { mutableStateOf(user.workDays) }
    var focusDuration by remember { mutableIntStateOf(user.focusDndMinutes) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    // Calendar Logic
    val calendarRepository = remember { CalendarRepository(context) }
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    var selectedCalendar by remember { mutableStateOf<CalendarInfo?>(null) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            availableCalendars = calendarRepository.getAvailableCalendars()
            selectedCalendar = availableCalendars.find { it.id == user.calendarId }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_data)) },
            text = { Text(stringResource(R.string.settings_clear_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.project_detail_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            }
        )
    }

    LaunchedEffect(showSavedSnackbar) {
        if (showSavedSnackbar) {
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.settings_saved)
            )
            showSavedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SettingsSection(title = stringResource(R.string.settings_profile)) {
                    // 1. Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.settings_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // 2. Role
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(stringResource(R.string.settings_role)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    // 3. Gender
                    AppDropdownSelector(
                        label = stringResource(R.string.settings_gender),
                        options = Gender.values().toList(),
                        selectedOption = selectedGender,
                        onOptionSelected = { selectedGender = it },
                        displayMapper = { getGenderString(context, it) }
                    )

                    Spacer(Modifier.height(8.dp))

                    // 4. Language
                    AppDropdownSelector(
                        label = stringResource(R.string.settings_language),
                        options = AppLanguage.values().toList(),
                        selectedOption = selectedLanguage,
                        onOptionSelected = { selectedLanguage = it },
                        displayMapper = { getLanguageDisplayName(it) }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_work)) {
                    Text(
                        stringResource(R.string.settings_work_hours),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

                    Text(
                        stringResource(R.string.settings_work_days),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WorkDaysSelector(
                        selectedDays = selectedDays,
                        onDaysChange = { selectedDays = it }
                    )
                }
            }

            item {
                SettingsSection(title = "Calendar Sync") {
                    if (availableCalendars.isEmpty()) {
                        Text(
                            "No calendars available or permission denied",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    } else {
                        val allOption = CalendarInfo(-1L, "All Calendars", "", "", 0, true)
                        val calendarOptions = listOf(allOption) + availableCalendars
                        val currentSelection = selectedCalendar ?: allOption

                        AppDropdownSelector(
                            label = "Select Calendar Source",
                            options = calendarOptions,
                            selectedOption = currentSelection,
                            onOptionSelected = { selectedCalendar = if(it.id == -1L) null else it },
                            displayMapper = {
                                if (it.id == -1L) "All Calendars"
                                else "${it.displayName} (${it.accountName})"
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_focus)) {
                    AppDropdownSelector(
                        label = stringResource(R.string.settings_focus_duration),
                        options = listOf(15, 30, 45, 60, 90, 120),
                        selectedOption = focusDuration,
                        onOptionSelected = { focusDuration = it },
                        displayMapper = { "$it minutes" }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_data)) {
                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_data))
                    }
                }
            }

            item {
                Text(
                    "${stringResource(R.string.settings_version)}: 1.0.0",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        val updatedUser = user.copy(
                            name = name,
                            role = role,
                            gender = selectedGender,
                            language = selectedLanguage,
                            startWorkHour = startHour,
                            endWorkHour = endHour,
                            workDays = selectedDays,
                            focusDndMinutes = focusDuration,
                            calendarId = selectedCalendar?.id ?: -1L
                        )

                        scope.launch {
                            onSaveSettings(updatedUser)

                            if (selectedLanguage != user.language) {
                                val sharedPrefs = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                                val languageCode = LanguageManager.getLanguageCode(selectedLanguage)
                                sharedPrefs.edit().putString("language", languageCode).apply()
                                activity?.recreate()
                            } else {
                                showSavedSnackbar = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            // תיקון: שימוש בצבע דינמי של ה-Theme במקום לבן קשיח
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                // תיקון: שימוש בצבע דינמי במקום שחור
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}