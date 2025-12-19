package com.nowwhat.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.algorithm.PriorityAlgorithm
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    project: Project,
    tasks: List<Task>,
    availableMinutes: Int,
    onBackClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onCreateTask: () -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onEditProject: () -> Unit,
    onDeleteProject: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Filter tasks
    val filteredTasks = tasks.filter { task ->
        when (selectedFilter) {
            "active" -> !task.isDone
            "completed" -> task.isDone
            "critical" -> PriorityAlgorithm.isTaskCritical(task, availableMinutes) && !task.isDone
            else -> true // "all"
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.project_detail_delete)) },
            text = { Text(stringResource(R.string.project_detail_delete_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteProject()
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
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit
                    IconButton(onClick = onEditProject) {
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
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = Color(0xFF6200EE)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Project Info Card
            ProjectInfoCard(project)

            Spacer(Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text(stringResource(R.string.project_detail_all_tasks)) }
                )
                FilterChip(
                    selected = selectedFilter == "active",
                    onClick = { selectedFilter = "active" },
                    label = { Text(stringResource(R.string.project_detail_active)) }
                )
                FilterChip(
                    selected = selectedFilter == "completed",
                    onClick = { selectedFilter = "completed" },
                    label = { Text(stringResource(R.string.project_detail_completed)) }
                )
                FilterChip(
                    selected = selectedFilter == "critical",
                    onClick = { selectedFilter = "critical" },
                    label = { Text(stringResource(R.string.project_detail_critical)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Tasks List
            if (filteredTasks.isEmpty()) {
                EmptyTasksState(onCreateTask)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task) },
                            onToggleDone = { onToggleTaskDone(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectInfoCard(project: Project) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Description
            if (project.description.isNotEmpty()) {
                Text(
                    project.description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
            }

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${stringResource(R.string.projects_progress)}: ${project.progress}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${project.completedTasks}/${project.totalTasks}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = Color(0xFF6200EE),
                trackColor = Color(0xFFE0E0E0)
            )

            // Deadline
            project.deadline?.let { deadline ->
                Spacer(Modifier.height(16.dp))
                val today = System.currentTimeMillis()
                val daysLeft = TimeUnit.MILLISECONDS.toDays(deadline - today).toInt()
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = when {
                            daysLeft < 0 -> Color(0xFFD32F2F)
                            daysLeft <= 7 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${stringResource(R.string.projects_deadline)}: ${dateFormat.format(Date(deadline))}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Risk Status
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Status:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                RiskBadge(project.risk)
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50)
                )
            )

            Spacer(Modifier.width(12.dp))

            // Task Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone) Color.Gray else Color.Black
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority badge
                    PriorityChip(task.priority)

                    // Estimated time
                    Text(
                        "${task.estimatedMinutes / 60}h ${task.estimatedMinutes % 60}m",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    // Deadline
                    task.deadline?.let { deadline ->
                        val daysLeft = TimeUnit.MILLISECONDS.toDays(deadline - System.currentTimeMillis()).toInt()
                        Text(
                            when {
                                daysLeft < 0 -> "⚠️ Overdue"
                                daysLeft == 0 -> "🔴 Today"
                                daysLeft <= 3 -> "🟡 ${daysLeft}d"
                                else -> "🟢 ${daysLeft}d"
                            },
                            fontSize = 12.sp,
                            color = when {
                                daysLeft < 0 -> Color(0xFFD32F2F)
                                daysLeft <= 3 -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: Priority) {
    val (emoji, color) = when (priority) {
        Priority.Immediate -> "⚡" to Color(0xFFFF5722)
        Priority.High -> "🔴" to Color(0xFFFF9800)
        Priority.Medium -> "🟡" to Color(0xFFFFC107)
        Priority.Low -> "🟢" to Color(0xFF4CAF50)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            emoji,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
fun EmptyTasksState(onCreateTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.TaskAlt,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.project_detail_no_tasks),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.project_detail_add_first_task),
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateTask,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EE)
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Task")
        }
    }
}