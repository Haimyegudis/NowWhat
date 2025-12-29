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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.nowwhat.app.utils.StringUtils
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
    var showHelpDialog by remember { mutableStateOf(false) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

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

    val startTimePicker = TimePickerDialog(
        context, { _, hour, _ -> startHour = hour }, startHour, 0, true
    )
    val endTimePicker = TimePickerDialog(
        context, { _, hour, _ -> endHour = hour }, endHour, 0, true
    )

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.help_intro))
                    HorizontalDivider()
                    Text("🚀 ${stringResource(R.string.help_feature_1_title)}\n${stringResource(R.string.help_feature_1_desc)}")
                    Text("⏱️ ${stringResource(R.string.help_feature_2_title)}\n${stringResource(R.string.help_feature_2_desc)}")
                    Text("📊 ${stringResource(R.string.help_feature_3_title)}\n${stringResource(R.string.help_feature_3_desc)}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text(stringResource(R.string.help_close)) }
            }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
            snackbarHostState.showSnackbar(context.getString(R.string.settings_saved))
            showSavedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Section 1: Profile ---
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_profile),
                    icon = Icons.Default.Person
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.settings_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = role,
                        onValueChange = { role = it },
                        label = { Text(stringResource(R.string.settings_role)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f)) {
                            SettingsDropdownSelector(
                                label = stringResource(R.string.settings_gender),
                                options = Gender.values().toList(),
                                selectedOption = selectedGender,
                                onOptionSelected = { selectedGender = it },
                                displayMapper = { StringUtils.getGenderString(context, it) }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            SettingsDropdownSelector(
                                label = stringResource(R.string.settings_language),
                                options = AppLanguage.values().toList(),
                                selectedOption = selectedLanguage,
                                onOptionSelected = { selectedLanguage = it },
                                displayMapper = { StringUtils.getLanguageDisplayName(it) }
                            )
                        }
                    }
                }
            }

            // --- Section 2: Work & Calendar ---
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_work),
                    icon = Icons.Default.Work
                ) {
                    Text(
                        stringResource(R.string.settings_work_hours),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
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
                            Text("${stringResource(R.string.time_start)}: ${String.format("%02d:00", startHour)}")
                        }
                        OutlinedButton(
                            onClick = { endTimePicker.show() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("${stringResource(R.string.time_end)}: ${String.format("%02d:00", endHour)}")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.settings_work_days),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsWorkDaysSelector(
                        selectedDays = selectedDays,
                        onDaysChange = { selectedDays = it }
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text("Calendar Integration", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    val calOptions = listOf(CalendarInfo(-1, "All Calendars", "", "", 0, true)) + availableCalendars
                    SettingsDropdownSelector(
                        label = "Source",
                        options = calOptions,
                        selectedOption = selectedCalendar ?: calOptions.first(),
                        onOptionSelected = { selectedCalendar = if (it.id == -1L) null else it },
                        displayMapper = { it.displayName }
                    )
                }
            }

            // --- Section 3: Focus & Data ---
            item {
                SettingsCard(
                    title = stringResource(R.string.settings_focus),
                    icon = Icons.Default.Timer
                ) {
                    SettingsDropdownSelector(
                        label = stringResource(R.string.settings_focus_duration),
                        options = listOf(15, 30, 45, 60, 90, 120),
                        selectedOption = focusDuration,
                        onOptionSelected = { focusDuration = it },
                        displayMapper = { "$it minutes" }
                    )

                    Spacer(Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_data))
                    }
                }
            }

            // --- Footer ---
            item {
                Text(
                    "${stringResource(R.string.settings_version)} 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
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
                                val sp = context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
                                val languageCode = LanguageManager.getLanguageCode(selectedLanguage)
                                sp.edit().putString("language", languageCode).apply()
                                activity?.recreate()
                            } else {
                                showSavedSnackbar = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(stringResource(R.string.settings_save))
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// --- Helper Composables (Private) ---

@Composable
private fun SettingsCard(
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
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            content()
        }
    }
}

@Composable
private fun SettingsWorkDaysSelector(
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
private fun <T> SettingsDropdownSelector(
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