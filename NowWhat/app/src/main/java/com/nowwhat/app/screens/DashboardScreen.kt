package com.nowwhat.app.screens

import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    calendarEvents: List<CalendarEvent>,
    availableMinutes: Int,
    onNavigateToProjects: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    onStartFocus: (Task) -> Unit,
    onFilterClick: (String) -> Unit,
    onCreateTask: () -> Unit = {}
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isDayTime = currentHour in 6..18

    val greeting = when (currentHour) {
        in 5..11 -> stringResource(R.string.dashboard_greeting_morning)
        in 12..17 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    val topTask = PriorityAlgorithm.recommendFocusTask(tasks, availableMinutes)

    val plannedTasks = PriorityAlgorithm.getTasksForToday(tasks, availableMinutes, user)
    val plannedMinutes = plannedTasks.sumOf { it.estimatedMinutes }
    val freeMinutes = availableMinutes - plannedMinutes

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDayTime) Icons.Default.WbSunny else Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "$greeting, ${user.name}! 👋",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource(R.string.dashboard_subtitle),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = "Stats",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDayTime) {
                        Color(0xFF6200EE)
                    } else {
                        Color(0xFF1A237E)
                    }
                )
            )
        },
        floatingActionButton = {
            if (projects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onCreateTask,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Task")
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
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    plannedMinutes = plannedMinutes,
                    availableMinutes = availableMinutes,
                    freeMinutes = freeMinutes,
                    user = user
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
                    onCriticalClick = { onFilterClick("critical") },
                    onAtRiskClick = { onFilterClick("at_risk") }
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
                        containerColor = Color(0xFF6200EE)
                    )
                ) {
                    Text(stringResource(R.string.dashboard_view_all_projects))
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun CapacityCard(
    selectedPeriod: String,
    onPeriodChange: (String) -> Unit,
    plannedMinutes: Int,
    availableMinutes: Int,
    freeMinutes: Int,
    user: UserProfile
) {
    val periods = listOf("Daily", "Weekly", "Sprint", "Monthly")

    val (totalCapacity, usedCapacity) = when (selectedPeriod) {
        "Daily" -> availableMinutes to plannedMinutes
        "Weekly" -> {
            val weeklyMinutes = user.workDays.size * user.dailyWorkMinutes
            weeklyMinutes to (plannedMinutes * user.workDays.size)
        }
        "Sprint" -> {
            val sprintMinutes = 10 * user.workDays.size * user.dailyWorkMinutes / 7
            sprintMinutes to (plannedMinutes * 10)
        }
        "Monthly" -> {
            val monthlyMinutes = 22 * user.dailyWorkMinutes
            monthlyMinutes to (plannedMinutes * 22)
        }
        else -> availableMinutes to plannedMinutes
    }

    val freeCapacity = (totalCapacity - usedCapacity).coerceAtLeast(0)

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
                "📊 Capacity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { onPeriodChange(period) },
                        label = { Text(period, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val progressPercent = if (totalCapacity > 0) {
                (usedCapacity.toFloat() / totalCapacity).coerceIn(0f, 1f)
            } else 0f

            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = if (progressPercent > 0.9f) Color(0xFFFF5722) else Color(0xFF4CAF50),
                trackColor = Color(0xFFE0E0E0)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CapacityItem(
                    label = stringResource(R.string.dashboard_workload),
                    minutes = usedCapacity,
                    color = Color(0xFF2196F3)
                )
                CapacityItem(
                    label = stringResource(R.string.dashboard_free_time),
                    minutes = freeCapacity,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
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
            containerColor = Color(0xFFF5F5F5)
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
                color = Color.Black
            )
            Spacer(Modifier.height(12.dp))

            events.take(5).forEach { event ->
                EventItem(
                    event = event,
                    isDuringWorkHours = event.isDuringWorkHours
                )
                Spacer(Modifier.height(8.dp))
            }

            if (events.size > 5) {
                Text(
                    stringResource(R.string.dashboard_more_events, events.size - 5),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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
                    if (isDuringWorkHours) Color(0xFFFF9800) else Color(0xFF9E9E9E)
                )
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                "${timeFormat.format(Date(event.startTime))} - ${timeFormat.format(Date(event.endTime))}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        if (!isDuringWorkHours) {
            Icon(
                Icons.Default.NightsStay,
                contentDescription = stringResource(R.string.dashboard_after_hours),
                tint = Color.Gray,
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
            color = Color.Black
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
                color = Color(0xFFFF5722),
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
                color = Color.Gray
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
            containerColor = Color(0xFFF5F5F5)
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
                color = Color.Black
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
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$overallProgress%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        stringResource(R.string.dashboard_complete),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.dashboard_tasks_done, completedTasks, totalTasks),
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Tap to see incomplete tasks",
                fontSize = 12.sp,
                color = Color(0xFF6200EE),
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
        Urgency.Critical -> Color(0xFFD32F2F)
        Urgency.VeryHigh -> Color(0xFFFF5722)
        Urgency.High -> Color(0xFFFF9800)
        Urgency.Medium -> Color(0xFF4CAF50)
        Urgency.Low -> Color(0xFF2196F3)
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
            containerColor = Color(0xFF6200EE)
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
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        task.title,
                        color = Color.White,
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
                        .background(Color.White)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start Focus",
                        tint = Color(0xFF6200EE),
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
        Urgency.Critical -> Triple("Critical", "🔴", Color(0xFFFFCDD2))
        Urgency.VeryHigh -> Triple("Very High", "🟠", Color(0xFFFFE0B2))
        Urgency.High -> Triple("High", "🟡", Color(0xFFFFF9C4))
        Urgency.Medium -> Triple("Medium", "🟢", Color(0xFFC8E6C9))
        Urgency.Low -> Triple("Low", "🔵", Color(0xFFBBDEFB))
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
            containerColor = Color(0xFFE8F5E9)
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
                color = Color(0xFF4CAF50)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.dashboard_create_task),
                fontSize = 14.sp,
                color = Color.Gray
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
            color = Color.Gray
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