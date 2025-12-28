// NowWhat/app/src/main/java/com/nowwhat/app/screens/DashboardScreen.kt
package com.nowwhat.app.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.algorithm.PriorityAlgorithm
import com.nowwhat.app.data.CalendarEvent
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: UserProfile,
    projects: List<Project>,
    tasks: List<Task>,
    subTasks: List<SubTask>,
    calendarEvents: List<CalendarEvent>,
    availableMinutes: Int,
    onNavigateToProjects: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToCapacity: () -> Unit = {},
    onStartFocus: (Task) -> Unit,
    onFilterClick: (String) -> Unit,
    onCreateTask: () -> Unit = {},
    onTaskClick: ((Task) -> Unit)? = null,
    onProjectClick: ((Project) -> Unit)? = null,
    onGetCapacityForRange: suspend (Long, Long) -> Int = { _, _ -> 0 }
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isDayTime = currentHour in 6..18

    val greeting = when (currentHour) {
        in 5..11 -> stringResource(R.string.dashboard_greeting_morning)
        in 12..17 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    val topTask = PriorityAlgorithm.recommendFocusTask(tasks, availableMinutes)

    val criticalTasks = tasks.filter {
        (it.urgency == Urgency.Critical || it.urgency == Urgency.VeryHigh) && !it.isDone
    }
    val atRiskProjects = projects.filter { it.risk == RiskStatus.AtRisk && !it.isCompleted }

    val totalTasks = tasks.count()
    val completedTasks = tasks.count { it.isDone }
    val overallProgress = if (totalTasks > 0) {
        (completedTasks * 100f / totalTasks).toInt()
    } else 0

    var selectedPeriod by remember { mutableStateOf("Daily") }

    // Dialog States
    var showQueueDialog by remember { mutableStateOf(false) }
    var showCriticalDialog by remember { mutableStateOf(false) }
    var showAtRiskDialog by remember { mutableStateOf(false) }

    val queueTasks = remember(tasks, selectedPeriod) {
        val (start, end) = PriorityAlgorithm.getRangeForPeriod(selectedPeriod)
        PriorityAlgorithm.getQueueTasks(tasks, start, end)
    }

    // --- DIALOGS ---

    if (showQueueDialog) {
        TaskQueueDialog(
            tasks = queueTasks,
            subTasks = subTasks,
            periodTitle = selectedPeriod,
            onDismiss = { showQueueDialog = false },
            onTaskClick = { task ->
                showQueueDialog = false
                onTaskClick?.invoke(task)
            }
        )
    }

    if (showCriticalDialog) {
        TaskQueueDialog(
            tasks = criticalTasks,
            subTasks = subTasks,
            periodTitle = "Critical Tasks",
            onDismiss = { showCriticalDialog = false },
            onTaskClick = { task ->
                showCriticalDialog = false
                onTaskClick?.invoke(task)
            }
        )
    }

    if (showAtRiskDialog) {
        AtRiskProjectsDialog(
            projects = atRiskProjects,
            onDismiss = { showAtRiskDialog = false },
            onProjectClick = { project ->
                showAtRiskDialog = false
                onProjectClick?.invoke(project)
            }
        )
    }

    // --- UI CONTENT ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDayTime) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "$greeting, ${user.name}!",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.dashboard_subtitle),
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCapacity) {
                        Icon(Icons.Default.CalendarMonth, "Capacity", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToStats) {
                        Icon(Icons.Default.EmojiEvents, "Stats", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
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

            if (topTask != null) {
                item {
                    RecommendedFocusCard(
                        task = topTask,
                        onStartFocus = { onStartFocus(topTask) }
                    )
                }
            } else {
                item {
                    NoTasksCard()
                }
            }

            item {
                CapacityCard(
                    tasks = tasks,
                    subTasks = subTasks,
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    availableMinutes = availableMinutes,
                    user = user,
                    onGetCapacity = onGetCapacityForRange,
                    onClick = { showQueueDialog = true }
                )
            }

            if (calendarEvents.isNotEmpty()) {
                item {
                    TodaysScheduleCard(
                        events = calendarEvents,
                        userWorkStart = user.startWorkHour,
                        userWorkEnd = user.endWorkHour
                    )
                }
            }

            item {
                OverviewSection(
                    criticalCount = criticalTasks.size,
                    atRiskCount = atRiskProjects.size,
                    onCriticalClick = { showCriticalDialog = true },
                    onAtRiskClick = { showAtRiskDialog = true }
                )
            }

            item {
                TotalProgressCard(
                    overallProgress = overallProgress,
                    completedTasks = completedTasks,
                    totalTasks = totalTasks,
                    tasks = tasks,
                    onProgressClick = { }
                )
            }

            item {
                Button(
                    onClick = onNavigateToProjects,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.dashboard_view_all_projects))
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

// --- Composable Functions ---

@Composable
fun CapacityCard(
    tasks: List<Task>,
    subTasks: List<SubTask>,
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    availableMinutes: Int,
    user: UserProfile,
    onGetCapacity: suspend (Long, Long) -> Int,
    onClick: (() -> Unit)? = null
) {
    val periods = listOf("Daily", "Weekly", "Monthly")

    var calculatedCapacity by remember { mutableIntStateOf(availableMinutes) }
    val (start, end) = remember(selectedPeriod) { PriorityAlgorithm.getRangeForPeriod(selectedPeriod) }

    LaunchedEffect(selectedPeriod, start, end, user) {
        if (selectedPeriod == "Daily") {
            calculatedCapacity = availableMinutes
        } else {
            calculatedCapacity = onGetCapacity(start, end)
        }
    }

    val usedCapacity = remember(tasks, subTasks, start, end) {
        PriorityAlgorithm.calculateWorkloadForRange(tasks, subTasks, start, end)
    }

    val freeCapacity = (calculatedCapacity - usedCapacity).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.capacity_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onClick != null) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "View Queue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onClick != null) {
                Text(
                    "Top 5 tasks for $selectedPeriod view",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = {
                            Text(
                                period,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val progressPercent = if (calculatedCapacity > 0) {
                (usedCapacity.toFloat() / calculatedCapacity).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (progressPercent > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CapacityItem(
                    label = stringResource(R.string.capacity_workload),
                    minutes = usedCapacity,
                    color = MaterialTheme.colorScheme.primary
                )
                CapacityItem(
                    label = stringResource(R.string.capacity_free_time),
                    minutes = freeCapacity,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun TaskQueueDialog(
    tasks: List<Task>,
    subTasks: List<SubTask>,
    periodTitle: String,
    onDismiss: () -> Unit,
    onTaskClick: (Task) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$periodTitle Queue (${tasks.size})") },
        text = {
            if (tasks.isEmpty()) {
                Text("No items found.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks) { task ->
                        val taskSubTasks = subTasks.filter { it.taskId == task.id && !it.isDone }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val urgencyColor = when(task.urgency) {
                                        Urgency.Critical -> MaterialTheme.colorScheme.error
                                        Urgency.VeryHigh -> Color(0xFFFF5722)
                                        Urgency.High -> Color(0xFFFF9800)
                                        else -> Color.Gray
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(urgencyColor)
                                    )
                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (task.deadline != null) {
                                                Text(
                                                    SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(task.deadline)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // SCORE BADGE
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "Score: ${task.urgencyScore}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // REASON
                                if (task.urgencyReason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Why: ${task.urgencyReason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                if (taskSubTasks.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(Modifier.height(4.dp))
                                    taskSubTasks.forEach { subTask ->
                                        Row(
                                            modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.SubdirectoryArrowRight, null, Modifier.size(14.dp))
                                            Text(subTask.title, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// --- AT RISK PROJECTS DIALOG ---
@Composable
fun AtRiskProjectsDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onProjectClick: (Project) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("At Risk Projects (${projects.size})") },
        text = {
            if (projects.isEmpty()) {
                Text("No projects currently at risk.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(projects) { project ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProjectClick(project) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (project.deadline != null) {
                                        Text(
                                            text = "Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(project.deadline))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "At Risk",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun TodaysScheduleCard(
    events: List<CalendarEvent>,
    userWorkStart: Int,
    userWorkEnd: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.dashboard_todays_schedule),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            events.forEach { event ->
                EventItem(
                    event = event,
                    isDuringWorkHours = event.isDuringWorkHours
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EventItem(event: CalendarEvent, isDuringWorkHours: Boolean) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isDuringWorkHours) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        if (!isDuringWorkHours) {
            Icon(
                Icons.Default.NightsStay,
                contentDescription = stringResource(R.string.dashboard_after_hours),
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun OverviewSection(
    criticalCount: Int,
    atRiskCount: Int,
    onCriticalClick: () -> Unit,
    onAtRiskClick: () -> Unit
) {
    Column {
        Text(
            stringResource(R.string.dashboard_overview),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InteractiveCard(
                title = stringResource(R.string.dashboard_critical_tasks),
                count = criticalCount,
                icon = Icons.Default.Error,
                color = MaterialTheme.colorScheme.error,
                onClick = onCriticalClick,
                modifier = Modifier.weight(1f)
            )
            InteractiveCard(
                title = stringResource(R.string.dashboard_at_risk),
                count = atRiskCount,
                icon = Icons.Default.Warning,
                color = Color(0xFFFF9800),
                onClick = onAtRiskClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InteractiveCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                count.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TotalProgressCard(
    overallProgress: Int,
    completedTasks: Int,
    totalTasks: Int,
    tasks: List<Task>,
    onProgressClick: () -> Unit
) {
    var showIncompleteDialog by remember { mutableStateOf(false) }

    if (showIncompleteDialog) {
        IncompleteTasksDialog(
            tasks = tasks.filter { !it.isDone },
            onDismiss = { showIncompleteDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showIncompleteDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                stringResource(R.string.dashboard_total_progress),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { overallProgress / 100f },
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$overallProgress%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.dashboard_complete),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.dashboard_tasks_done, completedTasks, totalTasks),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Tap to see incomplete tasks",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun IncompleteTasksDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incomplete Tasks (${tasks.size})") },
        text = {
            LazyColumn {
                items(tasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UrgencyIndicator(task.urgency)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            task.title,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun UrgencyIndicator(urgency: Urgency) {
    val color = when (urgency) {
        Urgency.Critical -> MaterialTheme.colorScheme.error
        Urgency.VeryHigh -> Color(0xFFFF5722)
        Urgency.High -> Color(0xFFFF9800)
        Urgency.Medium -> MaterialTheme.colorScheme.primary
        Urgency.Low -> MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun RecommendedFocusCard(
    task: Task,
    onStartFocus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.dashboard_recommended_focus),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        task.title,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    UrgencyBadge(task.urgency, task.urgencyScore)
                }

                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UrgencyBadge(urgency: Urgency, score: Int) {
    val (text, emoji, color) = when (urgency) {
        Urgency.Critical -> Triple("Critical", "閥", MaterialTheme.colorScheme.errorContainer)
        Urgency.VeryHigh -> Triple("Very High", "泛", Color(0xFFFFE0B2))
        Urgency.High -> Triple("High", "泯", Color(0xFFFFF9C4))
        Urgency.Medium -> Triple("Medium", "泙", Color(0xFFC8E6C9))
        Urgency.Low -> Triple("Low", "鳩", Color(0xFFBBDEFB))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 14.sp)
            Text(
                "$text ($score)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun NoTasksCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.dashboard_no_tasks),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.dashboard_create_task),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CapacityItem(label: String, minutes: Int, color: Color) {
    Column {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${minutes / 60}${stringResource(R.string.common_hour_short)} ${minutes % 60}${stringResource(R.string.common_minute_short)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}