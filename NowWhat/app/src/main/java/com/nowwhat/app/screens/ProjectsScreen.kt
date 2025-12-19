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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.RiskStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<Project>,
    onProjectClick: (Project) -> Unit,
    onCreateProject: () -> Unit,
    onBackClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    // Filter projects
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProject,
                containerColor = Color(0xFF6200EE)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.projects_add))
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
                shape = RoundedCornerShape(12.dp)
            )

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
                    label = { Text(stringResource(R.string.projects_all)) }
                )
                FilterChip(
                    selected = selectedFilter == "active",
                    onClick = { selectedFilter = "active" },
                    label = { Text(stringResource(R.string.projects_active)) }
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
                EmptyProjectsState(onCreateProject)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProjects) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onProjectClick(project) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (project.isCompleted) {
                Color(0xFFE8F5E9)
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.isCompleted) Color.Gray else Color.Black
                    )

                    if (project.description.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            project.description,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 2
                        )
                    }
                }

                // Risk Badge
                if (!project.isCompleted) {
                    RiskBadge(project.risk)
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { project.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (project.isCompleted) Color(0xFF4CAF50) else Color(0xFF6200EE),
                trackColor = Color(0xFFE0E0E0)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${stringResource(R.string.projects_progress)}: ${project.progress}%",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    "${project.completedTasks}/${project.totalTasks} ${stringResource(R.string.projects_tasks)}",
                    fontSize = 12.sp,
                    color = Color.Gray
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
fun EmptyProjectsState(onCreateProject: () -> Unit) {
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
            tint = Color.Gray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.projects_no_projects),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.projects_create_first),
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onCreateProject,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6200EE)
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.projects_add))
        }
    }
}
