package com.nowwhat.app.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nowwhat.app.R
import com.nowwhat.app.model.Priority
import com.nowwhat.app.model.Project
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (Project) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.Medium) }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var showError by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 23, 59, 59)
            }
            deadline = selectedDate.timeInMillis
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
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text(stringResource(R.string.create_project_name)) },
                    placeholder = { Text(stringResource(R.string.create_project_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && name.isBlank(),
                    supportingText = if (showError && name.isBlank()) {
                        { Text(stringResource(R.string.create_project_error_name)) }
                    } else null
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.create_project_description)) },
                    placeholder = { Text(stringResource(R.string.create_project_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.create_task_priority),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Priority.values().forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.name) },
                                leadingIcon = if (priority == p) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = deadline?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    onValueChange = { },
                    label = { Text(stringResource(R.string.create_project_deadline)) },
                    placeholder = { Text(stringResource(R.string.create_project_select_deadline)) },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Select Deadline")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(if (deadline == null) stringResource(R.string.create_project_select_deadline) else "Change Date")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                        Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val newProject = Project(
                        name = name.trim(),
                        description = description.trim(),
                        priority = priority,
                        deadline = deadline,
                        createdAt = System.currentTimeMillis(),
                        isCompleted = false,
                        isArchived = false
                    )

                    android.util.Log.d("CreateProjectDialog", "Creating project: ${newProject.name}, isCompleted=${newProject.isCompleted}, isArchived=${newProject.isArchived}")
                    Toast.makeText(context, "Creating project: ${newProject.name}", Toast.LENGTH_SHORT).show()

                    onCreateProject(newProject)
                    onDismiss()
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