package com.nowwhat.app.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nowwhat.app.R
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onCreateTask: (Task) -> Unit
) {
    val context = LocalContext.current

    var taskName by remember { mutableStateOf("") }
    var taskDescription by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var selectedPriority by remember { mutableStateOf(Priority.Medium) }
    var selectedSeverity by remember { mutableStateOf(Severity.Medium) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var estimatedHours by remember { mutableStateOf("2.0") }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val calendar = Calendar.getInstance()
    if (deadline != null) {
        calendar.timeInMillis = deadline!!
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, dayOfMonth, 23, 59, 59)
            deadline = selectedCalendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
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
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Task Name
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

                // Project Selection
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
                            .menuAnchor(),
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

                // Priority
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

                // Severity
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

                // Estimated Hours
                OutlinedTextField(
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it },
                    label = { Text(stringResource(R.string.create_task_estimated_hours)) },
                    placeholder = { Text(stringResource(R.string.create_task_estimated_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Deadline
                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (deadline != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(deadline!!))
                        } else {
                            stringResource(R.string.create_task_deadline)
                        }
                    )
                }

                // Task Description
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text(stringResource(R.string.create_task_description)) },
                    placeholder = { Text(stringResource(R.string.create_task_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                // Error Message
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
                                createdAt = System.currentTimeMillis()
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