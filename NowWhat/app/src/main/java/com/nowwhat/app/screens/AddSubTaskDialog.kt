package com.nowwhat.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nowwhat.app.R
import com.nowwhat.app.model.Priority
import com.nowwhat.app.model.Severity
import com.nowwhat.app.model.SubTask

@Composable
fun AddSubTaskDialog(
    taskId: Int,
    onDismiss: () -> Unit,
    onCreateSubTask: (SubTask) -> Unit
) {
    var subTaskName by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("0.5") }
    var selectedPriority by remember { mutableStateOf(Priority.Low) }
    var selectedSeverity by remember { mutableStateOf(Severity.Low) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.add_subtask_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subTaskName,
                    onValueChange = {
                        subTaskName = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.add_subtask_name)) },
                    placeholder = { Text(stringResource(R.string.add_subtask_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && subTaskName.isBlank()
                )

                if (showError && subTaskName.isBlank()) {
                    Text(
                        stringResource(R.string.add_subtask_error_name),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    label = { Text(stringResource(R.string.add_subtask_estimated_hours)) },
                    placeholder = { Text("0.5") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subTaskName.isBlank()) {
                        showError = true
                    } else {
                        val hours = estimatedHours.toFloatOrNull() ?: 0.5f
                        val newSubTask = SubTask(
                            taskId = taskId,
                            title = subTaskName.trim(),
                            estimatedHours = hours,
                            priority = selectedPriority,
                            severity = selectedSeverity,
                            createdAt = System.currentTimeMillis()
                        )
                        onCreateSubTask(newSubTask)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.add_subtask_create))
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