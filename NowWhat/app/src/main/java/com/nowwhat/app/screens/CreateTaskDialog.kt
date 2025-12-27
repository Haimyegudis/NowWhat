package com.nowwhat.app.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nowwhat.app.R
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    projects: List<Project>,
    preSelectedProject: Project? = null,
    onDismiss: () -> Unit,
    onCreateTask: (Task) -> Unit
) {
    val context = LocalContext.current

    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }

    // Initialize selected project with preSelectedProject or the first one if list has only one
    var selectedProject by remember {
        mutableStateOf<Project?>(
            preSelectedProject ?: if (projects.size == 1) projects.first() else null
        )
    }

    var selectedPriority by remember { mutableStateOf(Priority.Medium) }
    var selectedSeverity by remember { mutableStateOf(Severity.Medium) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var estimatedHours by remember { mutableStateOf("2.0") }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Deadline Date Picker
    val deadlineCalendar = Calendar.getInstance()
    if (deadline != null) {
        deadlineCalendar.timeInMillis = deadline!!
    }
    val deadlineDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59)
            deadline = selectedCalendar.timeInMillis
        },
        deadlineCalendar.get(Calendar.YEAR),
        deadlineCalendar.get(Calendar.MONTH),
        deadlineCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // Reminder Logic
    val reminderCalendar = Calendar.getInstance()
    if (reminderTime != null) {
        reminderCalendar.timeInMillis = reminderTime!!
    }

    val reminderTimePicker = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            reminderCalendar.set(Calendar.MINUTE, minute)
            reminderCalendar.set(Calendar.SECOND, 0)
            reminderTime = reminderCalendar.timeInMillis
        },
        reminderCalendar.get(Calendar.HOUR_OF_DAY),
        reminderCalendar.get(Calendar.MINUTE),
        true
    )

    val reminderDatePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            reminderCalendar.set(year, month, dayOfMonth)
            reminderTimePicker.show()
        },
        reminderCalendar.get(Calendar.YEAR),
        reminderCalendar.get(Calendar.MONTH),
        reminderCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                reminderDatePicker.show()
            } else {
                Toast.makeText(context, "Notification permission required for reminders", Toast.LENGTH_SHORT).show()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.create_task_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = {
                        taskName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.create_task_name)) },
                    placeholder = { Text(stringResource(R.string.create_task_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && taskName.isBlank()
                )

                ExposedDropdownMenuBox(
                    expanded = showProjectDropdown,
                    onExpandedChange = { showProjectDropdown = !showProjectDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.create_task_project)) },
                        placeholder = { Text(stringResource(R.string.create_task_select_project)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProjectDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable), // Fixed deprecation
                        isError = showError && selectedProject == null
                    )
                    ExposedDropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    selectedProject = project
                                    showProjectDropdown = false
                                    showError = false
                                }
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.create_task_priority),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Priority.values().forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = {
                                Text(
                                    getPriorityString(priority),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    stringResource(R.string.create_task_severity),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Severity.values().forEach { severity ->
                        FilterChip(
                            selected = selectedSeverity == severity,
                            onClick = { selectedSeverity = severity },
                            label = {
                                Text(
                                    getSeverityString(severity),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it },
                    label = { Text(stringResource(R.string.create_task_estimated_hours)) },
                    placeholder = { Text(stringResource(R.string.create_task_estimated_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Deadline Button
                OutlinedButton(
                    onClick = { deadlineDatePicker.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (deadline != null) {
                            "📅 Deadline: " + SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(deadline!!))
                        } else {
                            stringResource(R.string.create_task_deadline)
                        }
                    )
                }

                // Reminder Button
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                reminderDatePicker.show()
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            reminderDatePicker.show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        if (reminderTime != null) {
                            "🔔 Reminder: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(reminderTime!!))
                        } else {
                            "🔔 Set Reminder"
                        }
                    )
                }

                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text(stringResource(R.string.create_task_description)) },
                    placeholder = { Text(stringResource(R.string.create_task_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                if (showError && errorMessage.isNotEmpty()) {
                    Text(
                        errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        taskName.isBlank() -> {
                            showError = true
                            errorMessage = context.getString(R.string.create_task_error_name)
                        }
                        selectedProject == null -> {
                            showError = true
                            errorMessage = context.getString(R.string.create_task_error_project)
                        }
                        else -> {
                            val hours = estimatedHours.toFloatOrNull() ?: 2.0f
                            val newTask = Task(
                                projectId = selectedProject!!.id,
                                title = taskName.trim(),
                                description = taskDescription.trim(),
                                priority = selectedPriority,
                                severity = selectedSeverity,
                                estimatedMinutes = (hours * 60).toInt(),
                                deadline = deadline,
                                createdAt = System.currentTimeMillis(),
                                reminderTime = reminderTime
                            )
                            onCreateTask(newTask)
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.create_task_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.create_task_cancel))
            }
        }
    )
}

@Composable
private fun getPriorityString(priority: Priority): String {
    return when (priority) {
        Priority.Critical -> stringResource(R.string.priority_critical)
        Priority.Immediate -> stringResource(R.string.priority_immediate)
        Priority.High -> stringResource(R.string.priority_high)
        Priority.Medium -> stringResource(R.string.priority_medium)
        Priority.Low -> stringResource(R.string.priority_low)
    }
}

@Composable
private fun getSeverityString(severity: Severity): String {
    return when (severity) {
        Severity.Critical -> stringResource(R.string.severity_critical)
        Severity.High -> stringResource(R.string.severity_high)
        Severity.Medium -> stringResource(R.string.severity_medium)
        Severity.Low -> stringResource(R.string.severity_low)
    }
}