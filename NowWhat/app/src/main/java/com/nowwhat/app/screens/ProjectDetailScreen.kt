package com.nowwhat.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
    onDeleteProject: () -> Unit,
    onToggleProjectComplete: (Boolean) -> Unit = {}
) {
    val isDarkMode = isSystemInDarkTheme()
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)

    var selectedFilter by remember { mutableStateOf("all") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    val filteredTasks = tasks.filter { task ->
        when (selectedFilter) {
            "active" -> !task.isDone
            "completed" -> task.isDone
            "critical" -> (task.urgency == Urgency.Critical || task.urgency == Urgency.VeryHigh) && !task.isDone
            else -> true
        }
    }

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

    if (showCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteDialog = false },
            title = { Text("Mark Project as Complete?") },
            text = {
                Text(
                    if (project.isCompleted)
                        "Mark this project as incomplete? It will appear in active projects again."
                    else
                        "Mark this project as complete? It will be moved to completed projects."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteDialog = false
                        onToggleProjectComplete(!project.isCompleted)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (project.isCompleted) Color(0xFFFF9800) else Color(0xFF4CAF50)
                    )
                ) {
                    Text(stringResource(R.string.project_detail_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteDialog = false }) {
                    Text(stringResource(R.string.create_project_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showCompleteDialog = true }) {
                        Icon(
                            if (project.isCompleted) Icons.Default.Undo else Icons.Default.CheckCircle,
                            contentDescription = if (project.isCompleted) "Mark Incomplete" else "Mark Complete",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onEditProject) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE)
                )
            )
        },
        floatingActionButton = {
            if (!project.isCompleted) {
                FloatingActionButton(
                    onClick = onCreateTask,
                    containerColor = Color(0xFF6200EE)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ProjectInfoCard(project, isDarkMode, textColor, cardColor)

            Spacer(Modifier.height(16.dp))

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

            if (filteredTasks.isEmpty()) {
                EmptyTasksState(onCreateTask, project.isCompleted, isDarkMode, textColor)
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
                            onToggleDone = { onToggleTaskDone(task) },
                            isDarkMode = isDarkMode,
                            textColor = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectInfoCard(
    project: Project,
    isDarkMode: Boolean,
    textColor: Color,
    cardColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (project.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (project.description.isNotEmpty()) {
                Text(
                    project.description,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.LightGray else Color.Gray
                )
                Spacer(Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${stringResource(R.string.projects_progress)}: ${project.progress}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    "${project.completedTasks}/${project.totalTasks}",
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.LightGray else Color.Gray
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
                trackColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
            )

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
                        color = if (isDarkMode) Color.LightGray else Color.Gray
                    )
                }
            }

            if (!project.isCompleted) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Status:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                    RiskBadge(project.risk)
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    isDarkMode: Boolean,
    textColor: Color
) {
    val cardColor = if (task.isDone) {
        if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    } else {
        if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF4CAF50)
                )
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone) {
                        if (isDarkMode) Color.Gray else Color.Gray
                    } else textColor
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UrgencyChip(task.urgency)

                    Text(
                        "${task.estimatedMinutes / 60}h ${task.estimatedMinutes % 60}m",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.LightGray else Color.Gray
                    )

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
fun UrgencyChip(urgency: Urgency) {
    val (emoji, color) = when (urgency) {
        Urgency.Critical -> "🔴" to Color(0xFFD32F2F)
        Urgency.VeryHigh -> "🟠" to Color(0xFFFF5722)
        Urgency.High -> "🟡" to Color(0xFFFF9800)
        Urgency.Medium -> "🟢" to Color(0xFF4CAF50)
        Urgency.Low -> "🔵" to Color(0xFF2196F3)
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
fun EmptyTasksState(
    onCreateTask: () -> Unit,
    isProjectCompleted: Boolean,
    isDarkMode: Boolean,
    textColor: Color
) {
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
            tint = if (isDarkMode) Color.Gray else Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.project_detail_no_tasks),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(Modifier.height(8.dp))
        if (!isProjectCompleted) {
            Text(
                stringResource(R.string.project_detail_add_first_task),
                fontSize = 14.sp,
                color = if (isDarkMode) Color.LightGray else Color.Gray
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreateTask,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE)
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Add Task", color = Color.White)
            }
        }
    }
}

@Composable
fun RiskBadge(risk: RiskStatus) {
    val (text, color) = when (risk) {
        RiskStatus.Critical -> stringResource(R.string.risk_at_risk) to Color(0xFFD32F2F)
        RiskStatus.AtRisk -> stringResource(R.string.risk_warning) to Color(0xFFFF9800)
        RiskStatus.OnTrack -> stringResource(R.string.risk_on_track) to Color(0xFF4CAF50)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}