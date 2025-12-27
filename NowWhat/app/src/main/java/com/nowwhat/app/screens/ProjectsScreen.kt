package com.nowwhat.app.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nowwhat.app.R
import com.nowwhat.app.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    projects: List<Project>,
    tasks: List<Task>,
    subTasks: List<SubTask>,
    onProjectClick: (Project) -> Unit,
    onTaskClick: (Task) -> Unit,
    onCreateProject: () -> Unit,
    onCreateTask: (Project?) -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onToggleSubTaskDone: (SubTask) -> Unit,
    onProjectCompleted: (Project) -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    onArchiveProject: (Project) -> Unit,
    onRestoreProject: (Project) -> Unit,
    onEditProject: (Project) -> Unit,
    onEditTask: (Task) -> Unit,
    onEditSubTask: (SubTask) -> Unit,
    onCreateSubTask: (Task) -> Unit,
    onArchiveTask: (Task) -> Unit = {},
    onRestoreTask: (Task) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var expandedProjectId by remember { mutableStateOf<Int?>(null) }
    var expandedTaskId by remember { mutableStateOf<Int?>(null) }

    val activeProjects = projects.filter { !it.isCompleted && !it.isArchived }
    val completedProjects = projects.filter { it.isCompleted && !it.isArchived }

    val standaloneTasks = tasks.filter { it.projectId == null && !it.isArchived }
    val activeStandaloneTasks = standaloneTasks.filter { !it.isDone }
    val completedStandaloneTasks = standaloneTasks.filter { it.isDone }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToArchive) {
                        Icon(Icons.Default.Archive, "Archive")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProject,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Create Project", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.projects_active)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.projects_completed)) }
                )
            }

            val projectsToShow = if (selectedTab == 0) activeProjects else completedProjects
            val standaloneTasksToShow = if (selectedTab == 0) activeStandaloneTasks else completedStandaloneTasks

            ProjectList(
                projects = projectsToShow,
                standaloneTasks = standaloneTasksToShow,
                tasks = tasks,
                subTasks = subTasks,
                expandedProjectId = expandedProjectId,
                expandedTaskId = expandedTaskId,
                isCompletedTab = selectedTab == 1,
                onProjectClick = { expandedProjectId = if (expandedProjectId == it.id) null else it.id },
                onTaskClick = { expandedTaskId = if (expandedTaskId == it.id) null else it.id },
                onCreateTask = onCreateTask,
                onToggleTaskDone = onToggleTaskDone,
                onDeleteProject = onDeleteProject,
                onDeleteTask = onDeleteTask,
                onToggleSubTaskDone = onToggleSubTaskDone,
                onProjectCompleted = onProjectCompleted,
                onDeleteSubTask = onDeleteSubTask,
                onEditProject = onEditProject,
                onEditTask = onEditTask,
                onEditSubTask = onEditSubTask,
                onCreateSubTask = onCreateSubTask,
                onArchiveProject = onArchiveProject,
                onRestoreProject = onRestoreProject,
                onArchiveTask = onArchiveTask,
                onRestoreTask = onRestoreTask
            )
        }
    }
}

@Composable
fun ProjectList(
    projects: List<Project>,
    standaloneTasks: List<Task>,
    tasks: List<Task>,
    subTasks: List<SubTask>,
    expandedProjectId: Int?,
    expandedTaskId: Int?,
    isCompletedTab: Boolean,
    onProjectClick: (Project) -> Unit,
    onTaskClick: (Task) -> Unit,
    onCreateTask: (Project?) -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onToggleSubTaskDone: (SubTask) -> Unit,
    onProjectCompleted: (Project) -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    onEditProject: (Project) -> Unit,
    onEditTask: (Task) -> Unit,
    onEditSubTask: (SubTask) -> Unit,
    onCreateSubTask: (Task) -> Unit,
    onArchiveProject: (Project) -> Unit,
    onRestoreProject: (Project) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onRestoreTask: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (standaloneTasks.isNotEmpty() || !isCompletedTab) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks without Project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!isCompletedTab) {
                        IconButton(onClick = { onCreateTask(null) }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (standaloneTasks.isEmpty() && !isCompletedTab) {
                item {
                    Text("No independent tasks", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                items(standaloneTasks, key = { "task_${it.id}" }) { task ->                    TaskCard(
                        task = task,
                        subTasks = subTasks.filter { it.taskId == task.id },
                        isExpanded = expandedTaskId == task.id,
                        isStandalone = true,
                        isCompletedTab = isCompletedTab,
                        onTaskClick = { onTaskClick(task) },
                        onToggleTaskDone = { onToggleTaskDone(task) },
                        onDeleteTask = { onDeleteTask(task) },
                        onToggleSubTaskDone = onToggleSubTaskDone,
                        onDeleteSubTask = onDeleteSubTask,
                        onEditTask = { onEditTask(task) },
                        onEditSubTask = onEditSubTask,
                        onCreateSubTask = { onCreateSubTask(task) },
                        onArchiveTask = { onArchiveTask(task) },
                        onRestoreTask = { onRestoreTask(task) }
                    )
                }
            }
            item { Divider(Modifier.padding(vertical = 8.dp)) }
        }

        item {
            Text(
                text = "Projects",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (projects.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.projects_no_projects),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(projects, key = { "project_${it.id}" }) { project ->                ProjectCard(
                    project = project,
                    tasks = tasks.filter { it.projectId == project.id && !it.isArchived },
                    subTasks = subTasks,
                    isExpanded = expandedProjectId == project.id,
                    expandedTaskId = expandedTaskId,
                    isCompletedTab = isCompletedTab,
                    onProjectClick = { onProjectClick(project) },
                    onTaskClick = onTaskClick,
                    onCreateTask = { onCreateTask(project) },
                    onToggleTaskDone = onToggleTaskDone,
                    onDeleteProject = { onDeleteProject(project) },
                    onDeleteTask = onDeleteTask,
                    onToggleSubTaskDone = onToggleSubTaskDone,
                    onProjectCompleted = { onProjectCompleted(project) },
                    onDeleteSubTask = onDeleteSubTask,
                    onEditProject = { onEditProject(project) },
                    onEditTask = onEditTask,
                    onEditSubTask = onEditSubTask,
                    onCreateSubTask = onCreateSubTask,
                    onArchiveProject = { onArchiveProject(project) },
                    onRestoreProject = { onRestoreProject(project) },
                    onArchiveTask = onArchiveTask,
                    onRestoreTask = onRestoreTask
                )
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectCard(
    project: Project,
    tasks: List<Task>,
    subTasks: List<SubTask>,
    isExpanded: Boolean,
    expandedTaskId: Int?,
    isCompletedTab: Boolean,
    onProjectClick: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onCreateTask: () -> Unit,
    onToggleTaskDone: (Task) -> Unit,
    onDeleteProject: () -> Unit,
    onDeleteTask: (Task) -> Unit,
    onToggleSubTaskDone: (SubTask) -> Unit,
    onProjectCompleted: () -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    onEditProject: (Project) -> Unit,
    onEditTask: (Task) -> Unit,
    onEditSubTask: (SubTask) -> Unit,
    onCreateSubTask: (Task) -> Unit,
    onArchiveProject: (Project) -> Unit,
    onRestoreProject: (Project) -> Unit,
    onArchiveTask: (Task) -> Unit,
    onRestoreTask: (Task) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (!isCompletedTab) {
                        onProjectCompleted()          // ללא project!
                    } else {
                        onArchiveProject(project)     // עם project
                    }
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!isCompletedTab) {
                        onArchiveProject(project)     // עם project
                    } else {
                        onRestoreProject(project)     // עם project
                    }
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val color = if (!isCompletedTab) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF9800)
                    else -> Color.Transparent
                }
            } else {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF9800)
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFF4CAF50)
                    else -> Color.Transparent
                }
            }

            val icon = if (!isCompletedTab) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Archive
                    else -> Icons.Default.Done
                }
            } else {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Archive
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Restore
                    else -> Icons.Default.Done
                }
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onProjectClick() },
                        onLongClick = { onEditProject(project) }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (project.isCompleted)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                project.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (project.isCompleted) TextDecoration.LineThrough else null,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (project.description.isNotBlank()) {
                                Text(
                                    project.description,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PriorityBadge(project.priority)
                            }

                            if (project.deadline != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${stringResource(R.string.projects_deadline)}: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(project.deadline))}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = onCreateTask,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add Task") }

                            if (tasks.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                tasks.forEach { task ->
                                    TaskCard(
                                        task = task,
                                        subTasks = subTasks.filter { it.taskId == task.id },
                                        isExpanded = expandedTaskId == task.id,
                                        isStandalone = false,
                                        isCompletedTab = isCompletedTab,
                                        onTaskClick = { onTaskClick(task) },
                                        onToggleTaskDone = { onToggleTaskDone(task) },
                                        onDeleteTask = { onDeleteTask(task) },
                                        onToggleSubTaskDone = onToggleSubTaskDone,
                                        onDeleteSubTask = onDeleteSubTask,
                                        onEditTask = { onEditTask(task) },
                                        onEditSubTask = onEditSubTask,
                                        onCreateSubTask = { onCreateSubTask(task) },
                                        onArchiveTask = { onArchiveTask(task) },
                                        onRestoreTask = { onRestoreTask(task) }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: Task,
    subTasks: List<SubTask>,
    isExpanded: Boolean,
    isStandalone: Boolean,
    isCompletedTab: Boolean = false,
    onTaskClick: () -> Unit,
    onToggleTaskDone: () -> Unit,
    onDeleteTask: () -> Unit,
    onToggleSubTaskDone: (SubTask) -> Unit,
    onDeleteSubTask: (SubTask) -> Unit,
    onEditTask: () -> Unit,
    onEditSubTask: (SubTask) -> Unit,
    onCreateSubTask: () -> Unit,
    onArchiveTask: () -> Unit = {},
    onRestoreTask: () -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            if (isStandalone) {
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (!isCompletedTab) {
                            onToggleTaskDone()
                        } else {
                            onArchiveTask()
                        }
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (!isCompletedTab) {
                            onArchiveTask()
                        } else {
                            onRestoreTask()
                        }
                        true
                    }
                    else -> false
                }
            } else {
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onToggleTaskDone()
                        true
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDeleteTask()
                        true
                    }
                    else -> false
                }
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val (color, icon) = if (isStandalone) {
                if (!isCompletedTab) {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) to Icons.Default.Done
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF9800) to Icons.Default.Archive
                        else -> Color.Transparent to Icons.Default.Done
                    }
                } else {
                    when (direction) {
                        SwipeToDismissBoxValue.StartToEnd -> Color(0xFFFF9800) to Icons.Default.Archive
                        SwipeToDismissBoxValue.EndToStart -> Color(0xFF4CAF50) to Icons.Default.Restore
                        else -> Color.Transparent to Icons.Default.Done
                    }
                }
            } else {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary to Icons.Default.Done
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error to Icons.Default.Delete
                    else -> MaterialTheme.colorScheme.surface to Icons.Default.Done
                }
            }

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onTaskClick() },
                        onLongClick = { onEditTask() }
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (task.isDone)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    task.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (task.deadline != null) {
                                    Text(
                                        SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(task.deadline)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "${task.urgencyScore}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp)
                        ) {
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = onCreateSubTask,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Add SubTask")
                            }

                            if (subTasks.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                subTasks.forEach { st ->
                                    SubTaskCard(
                                        subTask = st,
                                        onToggleDone = { onToggleSubTaskDone(st) },
                                        onDelete = { onDeleteSubTask(st) },
                                        onEdit = { onEditSubTask(st) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubTaskCard(
    subTask: SubTask,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> { onToggleDone(); true }
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); true }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = if (direction == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Done else Icons.Default.Delete
            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = alignment
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        },
        content = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = { }, onLongClick = { onEdit() }),
                colors = CardDefaults.cardColors(
                    containerColor = if (subTask.isDone) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (subTask.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary))
                    Spacer(Modifier.width(8.dp))
                    Text(subTask.title, fontSize = 12.sp, textDecoration = if (subTask.isDone) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f))
                }
            }
        }
    )
}

@Composable
fun PriorityBadge(priority: Priority) {
    val (color, text) = when (priority) {
        Priority.Critical -> MaterialTheme.colorScheme.error to stringResource(R.string.priority_critical)
        Priority.Immediate -> MaterialTheme.colorScheme.error to stringResource(R.string.priority_immediate)
        Priority.High -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.priority_high)
        Priority.Medium -> MaterialTheme.colorScheme.primary to stringResource(R.string.priority_medium)
        Priority.Low -> MaterialTheme.colorScheme.secondary to stringResource(R.string.priority_low)
    }

    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(alpha = 0.2f)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}