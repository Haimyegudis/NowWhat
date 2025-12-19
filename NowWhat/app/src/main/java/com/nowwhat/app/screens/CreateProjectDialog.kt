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
import com.nowwhat.app.model.Priority
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.Severity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (Project) -> Unit
) {
    val context = LocalContext.current

    var projectName by remember { mutableStateOf("") }
    var projectDescription by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(Priority.Medium) }
    var selectedSeverity by remember { mutableStateOf(Severity.Medium) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var showError by remember { mutableStateOf(false) }

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
            Text(stringResource(R.string.create_project_title))
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
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.create_project_name)) },
                    placeholder = { Text(stringResource(R.string.create_project_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && projectName.isBlank()
                )

                if (showError && projectName.isBlank()) {
                    Text(
                        stringResource(R.string.create_project_error_name),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = projectDescription,
                    onValueChange = { projectDescription = it },
                    label = { Text(stringResource(R.string.create_project_description)) },
                    placeholder = { Text(stringResource(R.string.create_project_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

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

                OutlinedButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (deadline != null) {
                            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(Date(deadline!!))
                        } else {
                            stringResource(R.string.create_project_select_deadline)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isBlank()) {
                        showError = true
                    } else {
                        val newProject = Project(
                            name = projectName.trim(),
                            description = projectDescription.trim(),
                            priority = selectedPriority,
                            severity = selectedSeverity,
                            deadline = deadline,
                            createdAt = System.currentTimeMillis()
                        )
                        onCreateProject(newProject)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.create_project_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.create_project_cancel))
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