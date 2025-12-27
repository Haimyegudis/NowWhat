// NowWhat/app/src/main/java/com/nowwhat/app/screens/StatsScreen.kt
package com.nowwhat.app.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.Task
import com.nowwhat.app.model.UserProfile
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    user: UserProfile,
    projects: List<Project>,
    tasks: List<Task>,
    tasksCompletedToday: Int,
    tasksCompletedThisWeek: Int,
    tasksCompletedThisMonth: Int,
    timeWorkedToday: Int,
    timeWorkedThisWeek: Int,
    timeWorkedThisMonth: Int,
    avgAccuracy: Float,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StreakCard(
                        title = stringResource(R.string.stats_current_streak),
                        days = user.currentStreak,
                        icon = Icons.Default.LocalFireDepartment,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    StreakCard(
                        title = stringResource(R.string.stats_longest_streak),
                        days = user.longestStreak,
                        icon = Icons.Default.EmojiEvents,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatsSection(title = stringResource(R.string.stats_tasks_completed)) {
                    StatItem(
                        label = stringResource(R.string.stats_today),
                        value = tasksCompletedToday.toString(),
                        icon = Icons.Default.Today,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_this_week),
                        value = tasksCompletedThisWeek.toString(),
                        icon = Icons.Default.DateRange,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_this_month),
                        value = tasksCompletedThisMonth.toString(),
                        icon = Icons.Default.CalendarMonth,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                StatsSection(title = stringResource(R.string.stats_time_worked)) {
                    StatItem(
                        label = stringResource(R.string.stats_today),
                        value = formatMinutes(timeWorkedToday),
                        icon = Icons.Default.AccessTime,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_this_week),
                        value = formatMinutes(timeWorkedThisWeek),
                        icon = Icons.Default.AccessTime,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_this_month),
                        value = formatMinutes(timeWorkedThisMonth),
                        icon = Icons.Default.AccessTime,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                val activeProjects = projects.count { !it.isCompleted }
                val completedProjects = projects.count { it.isCompleted }

                StatsSection(title = stringResource(R.string.projects_title)) {
                    StatItem(
                        label = stringResource(R.string.stats_total_projects),
                        value = projects.size.toString(),
                        icon = Icons.Default.Folder,
                        color = MaterialTheme.colorScheme.primary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_active_projects),
                        value = activeProjects.toString(),
                        icon = Icons.Default.FolderOpen,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    StatItem(
                        label = stringResource(R.string.stats_completed_projects),
                        value = completedProjects.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (avgAccuracy > 0) {
                item {
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.stats_avg_accuracy),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "${(avgAccuracy * 100).toInt()}%",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    avgAccuracy >= 0.9f -> MaterialTheme.colorScheme.primary
                                    avgAccuracy >= 0.7f -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun StreakCard(
    title: String,
    days: Int,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                days.toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                stringResource(R.string.stats_days),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}