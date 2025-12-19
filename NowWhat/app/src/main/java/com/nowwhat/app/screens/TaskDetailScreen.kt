package com.nowwhat.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    task: Task,
    project: Project,
    subTasks: List<SubTask>,
    onBackClick: () -> Unit,
    onEditTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onToggleTaskDone: () -> Unit,
    onStartFocus: () -> Unit,
    onAddSubTask: () -> Unit,
    onToggleSubTaskDone: (SubTask) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.task_detail_delete)) },
            text = { Text(stringResource(R.string.task_detail_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteTask()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(stringResource(R.string.project_detail_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit
                    IconButton(onClick = onEditTask) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    // Delete
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!task.isDone) {
                FloatingActionButton(
                    onClick = onStartFocus,
                    containerColor = Color(0xFF6200EE)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Focus")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Project Badge
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF6200EE).copy(alpha = 0.1f)
                ) {
                    Text(
                        "📁 ${project.name}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6200EE)
                    )
                }
            }

            // Status Card
            item {
                TaskStatusCard(
                    task = task,
                    onToggleDone = onToggleTaskDone
                )
            }

            // Details Card
            item {
                TaskDetailsCard(task = task)
            }

            // Time Card
            item {
                TaskTimeCard(task = task)
            }

            // Description
            if (task.description.isNotEmpty()) {
                item {
                    TaskDescriptionCard(task = task)
                }
            }

            // Sub-tasks Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.task_detail_subtasks),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onAddSubTask) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.task_detail_add_subtask))
                    }
                }
            }

            // Sub-tasks List
            if (subTasks.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.task_detail_no_subtasks),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(subTasks) { subTask ->
                    SubTaskItem(
                        subTask = subTask,
                        onToggleDone = { onToggleSubTaskDone(subTask) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TaskStatusCard(task: Task, onToggleDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (task.isDone) "✅ Completed" else "⏳ In Progress",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isDone) Color(0xFF4CAF50) else Color(0xFF2196F3)
                )
                task.completedAt?.let { completedAt ->
                    Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(completedAt)),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = onToggleDone,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (task.isDone) Color.Gray else Color(0xFF4CAF50)
                )
            ) {
                Text(
                    if (task.isDone)
                        stringResource(R.string.task_detail_mark_undone)
                    else
                        stringResource(R.string.task_detail_mark_done)
                )
            }
        }
    }
}

@Composable
fun TaskDetailsCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority
            DetailRow(
                label = stringResource(R.string.create_task_priority),
                value = when (task.priority) {
                    Priority.Immediate -> "⚡ ${stringResource(R.string.priority_immediate)}"
                    Priority.High -> "🔴 ${stringResource(R.string.priority_high)}"
                    Priority.Medium -> "🟡 ${stringResource(R.string.priority_medium)}"
                    Priority.Low -> "🟢 ${stringResource(R.string.priority_low)}"
                }
            )

            // Severity
            DetailRow(
                label = stringResource(R.string.create_task_severity),
                value = when (task.severity) {
                    Severity.Critical -> "💀 ${stringResource(R.string.severity_critical)}"
                    Severity.High -> "⚠️ ${stringResource(R.string.severity_high)}"
                    Severity.Medium -> "📊 ${stringResource(R.string.severity_medium)}"
                    Severity.Low -> "📋 ${stringResource(R.string.severity_low)}"
                }
            )

            // Deadline
            task.deadline?.let { deadline ->
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                DetailRow(
                    label = stringResource(R.string.create_task_deadline),
                    value = "📅 ${dateFormat.format(Date(deadline))}"
                )
            }

            // Created
            DetailRow(
                label = "Created",
                value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(task.createdAt))
            )
        }
    }
}

@Composable
fun TaskTimeCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                "⏱️ Time Tracking",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeItem(
                    label = stringResource(R.string.task_detail_estimated),
                    minutes = task.estimatedMinutes,
                    color = Color(0xFF2196F3)
                )
                TimeItem(
                    label = stringResource(R.string.task_detail_actual),
                    minutes = task.actualMinutes,
                    color = Color(0xFFFF9800)
                )
                TimeItem(
                    label = stringResource(R.string.task_detail_remaining),
                    minutes = task.remainingMinutes,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF6200EE),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
fun TaskDescriptionCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.task_detail_description),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                task.description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TimeItem(label: String, minutes: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${minutes / 60}h ${minutes % 60}m",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SubTaskItem(subTask: SubTask, onToggleDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (subTask.isDone) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = subTask.isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50)
                )
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subTask.title,
                    fontSize = 14.sp,
                    textDecoration = if (subTask.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (subTask.isDone) Color.Gray else Color.Black
                )
                Text(
                    "${subTask.estimatedMinutes / 60}h ${subTask.estimatedMinutes % 60}m",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}