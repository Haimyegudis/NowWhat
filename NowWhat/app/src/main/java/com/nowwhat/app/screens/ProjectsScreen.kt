package com.nowwhat.app.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<Project>,
    tasks: List<Task>,
    onProjectClick: (Project) -> Unit,
    onTaskClick: (Task) -> Unit,
    onCreateProject: () -> Unit,
    onCreateTask: () -> Unit = {},
    onToggleTaskDone: (Task) -> Unit,
    onBackClick: () -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()
    val textColor = if (isDarkMode) Color.White else Color.Black
    val backgroundColor = if (isDarkMode) Color(0xFF121212) else Color.White

    var selectedFilter by remember { mutableStateOf("active") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedProjectIds by remember { mutableStateOf(setOf<Int>()) }

    // Filter projects - only show non-completed by default
    val filteredProjects = projects.filter { project ->
        val matchesSearch = searchQuery.isEmpty() ||
                project.name.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "active" -> !project.isCompleted
            "completed" -> project.isCompleted
            "at_risk" -> project.risk == RiskStatus.AtRisk && !project.isCompleted
            else -> true // "all"
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects_title), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE)
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Create Task FAB
                if (projects.isNotEmpty()) {
                    SmallFloatingActionButton(
                        onClick = onCreateTask,
                        containerColor = Color(0xFF03DAC5)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Task", tint = Color.White)
                    }
                }
                // Create Project FAB
                FloatingActionButton(
                    onClick = onCreateProject,
                    containerColor = Color(0xFF6200EE)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.projects_add), tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.projects_search)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "active",
                    onClick = { selectedFilter = "active" },
                    label = { Text(stringResource(R.string.projects_active)) }
                )
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text(stringResource(R.string.projects_all)) }
                )
                FilterChip(
                    selected = selectedFilter == "completed",
                    onClick = { selectedFilter = "completed" },
                    label = { Text(stringResource(R.string.projects_completed)) }
                )
                FilterChip(
                    selected = selectedFilter == "at_risk",
                    onClick = { selectedFilter = "at_risk" },
                    label = { Text(stringResource(R.string.projects_at_risk)) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Projects List
            if (filteredProjects.isEmpty()) {
                EmptyProjectsState(onCreateProject, isDarkMode, textColor)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProjects) { project ->
                        val projectTasks = tasks.filter { it.projectId == project.id }
                        val isExpanded = expandedProjectIds.contains(project.id)

                        ExpandableProjectCard(
                            project = project,
                            tasks = projectTasks,
                            isExpanded = isExpanded,
                            onExpandToggle = {
                                expandedProjectIds = if (isExpanded) {
                                    expandedProjectIds - project.id
                                } else {
                                    expandedProjectIds + project.id
                                }
                            },
                            onProjectClick = { onProjectClick(project) },
                            onTaskClick = onTaskClick,
                            onToggleTaskDone = onToggleTaskDone,
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
fun ExpandableProjectCard(
    project: Project,
    tasks: List<Task>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onProjectClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    isDarkMode: Boolean,
    textColor: Color
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val deadline = project.deadline
    val daysUntilDeadline = if (deadline != null) {
        TimeUnit.MILLISECONDS.toDays(deadline - today).toInt()
    } else null

    val deadlineText = when {
        deadline == null -> null
        daysUntilDeadline!! < 0 -> stringResource(R.string.projects_overdue)
        daysUntilDeadline == 0 -> stringResource(R.string.projects_due_today)
        else -> stringResource(R.string.projects_due_in, daysUntilDeadline)
    }

    val deadlineColor = when {
        deadline == null -> Color.Gray
        daysUntilDeadline!! < 0 -> Color(0xFFD32F2F)
        daysUntilDeadline == 0 -> Color(0xFFFF9800)
        daysUntilDeadline <= 7 -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }

    val cardColor = if (project.isCompleted) {
        if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    } else {
        if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Project Header (Clickable for expand/collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            project.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (project.isCompleted) {
                                if (isDarkMode) Color.Gray else Color.Gray
                            } else textColor
                        )
                        if (tasks.isNotEmpty()) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand",
                                tint = if (isDarkMode) Color.LightGray else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (project.description.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            project.description,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.LightGray else Color.Gray,
                            maxLines = 2
                        )
                    }
                }

                // Risk Badge or Completed Icon
                if (project.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    RiskBadge(project.risk)
                }
            }

            // Progress Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                LinearProgressIndicator(
                    progress = { project.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (project.isCompleted) Color(0xFF4CAF50) else Color(0xFF6200EE),
                    trackColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${stringResource(R.string.projects_progress)}: ${project.progress}%",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.LightGray else Color.Gray
                    )
                    Text(
                        "${project.completedTasks}/${project.totalTasks} ${stringResource(R.string.projects_tasks)}",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.LightGray else Color.Gray
                    )
                }

                if (deadlineText != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = deadlineColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            deadlineText,
                            fontSize = 12.sp,
                            color = deadlineColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Expandable Tasks List
            AnimatedVisibility(
                visible = isExpanded && tasks.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Divider(color = if (isDarkMode) Color(0xFF424242) else Color.LightGray.copy(alpha = 0.5f))

                    tasks.forEach { task ->
                        TaskItemInProject(
                            task = task,
                            onClick = { onTaskClick(task) },
                            onToggleDone = { onToggleTaskDone(task) },
                            isDarkMode = isDarkMode,
                            textColor = textColor
                        )
                        Divider(color = if (isDarkMode) Color(0xFF424242) else Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }

            // View Details Button (always visible)
            TextButton(
                onClick = onProjectClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("View Details", color = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF6200EE))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isDarkMode) Color(0xFFBB86FC) else Color(0xFF6200EE)
                )
            }
        }
    }
}

@Composable
fun TaskItemInProject(
    task: Task,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    isDarkMode: Boolean,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggleDone() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50)
            )
        )

        Spacer(Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isDone) {
                    if (isDarkMode) Color.Gray else Color.Gray
                } else textColor
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Urgency indicator
                val urgencyEmoji = when (task.urgency) {
                    Urgency.Critical -> "🔴"
                    Urgency.VeryHigh -> "🟠"
                    Urgency.High -> "🟡"
                    Urgency.Medium -> "🟢"
                    Urgency.Low -> "🔵"
                }
                Text(urgencyEmoji, fontSize = 12.sp)

                // Time estimate
                Text(
                    "${task.estimatedMinutes / 60}h ${task.estimatedMinutes % 60}m",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.LightGray else Color.Gray
                )

                // Status
                if (task.isOverdue && !task.isDone) {
                    Text(
                        "⚠️ Overdue",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                } else if (task.isDueToday && !task.isDone) {
                    Text(
                        "🔴 Today",
                        fontSize = 11.sp,
                        color = Color(0xFFFF5722),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RiskBadge(risk: RiskStatus) {
    val (text, color) = when (risk) {
        RiskStatus.AtRisk -> stringResource(R.string.risk_at_risk) to Color(0xFFD32F2F)
        RiskStatus.Warning -> stringResource(R.string.risk_warning) to Color(0xFFFF9800)
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

@Composable
fun EmptyProjectsState(
    onCreateProject: () -> Unit,
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
            Icons.Default.FolderOpen,
            contentDescription = null,
            tint = if (isDarkMode) Color.Gray else Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.projects_no_projects),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.projects_create_first),
            fontSize = 14.sp,
            color = if (isDarkMode) Color.LightGray else Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateProject,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EE)
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.projects_add), color = Color.White)
        }
    }
}