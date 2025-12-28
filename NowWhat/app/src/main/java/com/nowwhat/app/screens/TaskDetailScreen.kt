package com.nowwhat.app.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
    onToggleSubTaskDone: (SubTask) -> Unit,
    onClearWaitingFor: () -> Unit,
    onUpdateReminder: (Long?) -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAutoTaskCompleteDialog by remember { mutableStateOf(false) }

    val reminderCalendar = remember { Calendar.getInstance() }

    fun showReminderPickers() {
        val currentRef = if (task.reminderTime != null) Date(task.reminderTime) else Date()
        val cal = Calendar.getInstance().apply { time = currentRef }

        val timePicker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                reminderCalendar.set(Calendar.MINUTE, minute)
                reminderCalendar.set(Calendar.SECOND, 0)
                onUpdateReminder(reminderCalendar.timeInMillis)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                reminderCalendar.set(year, month, dayOfMonth)
                timePicker.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) showReminderPickers()
            else Toast.makeText(context, "Permission required", Toast.LENGTH_SHORT).show()
        }
    )

    val onRequestEditReminder = {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                showReminderPickers()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            showReminderPickers()
        }
    }

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) { Text(stringResource(R.string.project_detail_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.create_project_cancel)) }
            }
        )
    }

    if (showAutoTaskCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showAutoTaskCompleteDialog = false },
            title = { Text("All Subtasks Done! 🎉") },
            text = { Text("You've finished all subtasks. Mark this main task as 'Complete'?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAutoTaskCompleteDialog = false
                        onToggleTaskDone()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Yes, Complete Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoTaskCompleteDialog = false }) {
                    Text("No, Not Yet")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onEditTask) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { showDeleteDialog = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
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
                FloatingActionButton(onClick = onStartFocus, containerColor = Color(0xFF6200EE)) {
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

            if (!task.waitingFor.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Waiting for:", fontSize = 12.sp, color = Color(0xFFEF6C00))
                                Text(task.waitingFor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                            Button(
                                onClick = onClearWaitingFor,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Got Reply", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            item { TaskStatusCard(task = task, onToggleDone = onToggleTaskDone) }

            item {
                TaskDetailsCard(
                    task = task,
                    onEditReminder = onRequestEditReminder,
                    onDeleteReminder = { onUpdateReminder(null) }
                )
            }

            item { TaskTimeCard(task = task) }

            if (task.description.isNotEmpty()) { item { TaskDescriptionCard(task = task) } }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.task_detail_subtasks), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onAddSubTask) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.task_detail_add_subtask))
                    }
                }
            }

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
                items(items = subTasks, key = { it.id }) { subTask ->
                    SwipeableSubTaskItem(
                        subTask = subTask,
                        isDarkMode = isDarkMode,
                        onToggleDone = {
                            if (!subTask.isDone) {
                                val otherActive = subTasks.count { !it.isDone && it.id != subTask.id }
                                if (otherActive == 0 && !task.isDone) {
                                    showAutoTaskCompleteDialog = true
                                }
                            }
                            onToggleSubTaskDone(subTask)
                        }
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun TaskDetailsCard(task: Task, onEditReminder: () -> Unit, onDeleteReminder: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(label = stringResource(R.string.create_task_priority), value = task.priority.name)
            DetailRow(label = stringResource(R.string.create_task_severity), value = task.severity.name)

            task.deadline?.let { deadline ->
                DetailRow(
                    label = stringResource(R.string.create_task_deadline),
                    value = "📅 ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(deadline))}"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reminder",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (task.reminderTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(task.reminderTime)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = onEditReminder, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDeleteReminder, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    TextButton(onClick = onEditReminder, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                        Text("Add Reminder", fontSize = 14.sp)
                    }
                }
            }
            DetailRow(label = "Created", value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(task.createdAt)))
        }
    }
}

@Composable
fun TaskStatusCard(task: Task, onToggleDone: () -> Unit) {
    val containerColor = if (task.isDone) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(if (task.isDone) "✅ Completed" else "⏳ In Progress", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (task.isDone) Color(0xFF4CAF50) else Color(0xFF2196F3))
                task.completedAt?.let { completedAt ->
                    Text(
                        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(completedAt)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(onClick = onToggleDone, colors = ButtonDefaults.buttonColors(containerColor = if (task.isDone) Color.Gray else Color(0xFF4CAF50))) {
                Text(if (task.isDone) stringResource(R.string.task_detail_mark_undone) else stringResource(R.string.task_detail_mark_done))
            }
        }
    }
}

@Composable
fun TaskTimeCard(task: Task) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("⏱️ Time Tracking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TimeItem(stringResource(R.string.task_detail_estimated), task.estimatedMinutes, Color(0xFF2196F3))
                TimeItem(stringResource(R.string.task_detail_actual), task.actualMinutes, Color(0xFFFF9800))
                TimeItem(stringResource(R.string.task_detail_remaining), task.remainingMinutes, Color(0xFF4CAF50))
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { task.progress / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF6200EE),
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun TaskDescriptionCard(task: Task) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(stringResource(R.string.task_detail_description), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(task.description, fontSize = 14.sp)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TimeItem(label: String, minutes: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Text("${minutes / 60}h ${minutes % 60}m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun SubTaskItem(subTask: SubTask, isDarkMode: Boolean, onToggleDone: () -> Unit) {
    val containerColor = if (subTask.isDone) (if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)) else (if (isDarkMode) Color(0xFF2C2C2C) else Color.White)
    val textColor = if (subTask.isDone) (if (isDarkMode) Color.White else Color.Gray) else (if (isDarkMode) Color.White else Color.Black)

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggleDone() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = subTask.isDone, onCheckedChange = { onToggleDone() }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50), checkmarkColor = Color.White))
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(subTask.title, fontSize = 14.sp, textDecoration = if (subTask.isDone) TextDecoration.LineThrough else TextDecoration.None, color = textColor)
                Text("${subTask.estimatedMinutes / 60}h ${subTask.estimatedMinutes % 60}m", fontSize = 12.sp, color = if (isDarkMode) Color.LightGray else Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableSubTaskItem(subTask: SubTask, isDarkMode: Boolean, onToggleDone: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.StartToEnd) {
                onToggleDone()
                false
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Color(0xFF4CAF50) else Color.Transparent
            Box(modifier = Modifier.fillMaxSize().padding(vertical = 4.dp).background(color, RoundedCornerShape(12.dp)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Icon(Icons.Default.Check, "Done", tint = Color.White)
            }
        },
        content = { SubTaskItem(subTask = subTask, isDarkMode = isDarkMode, onToggleDone = onToggleDone) },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false
    )
}