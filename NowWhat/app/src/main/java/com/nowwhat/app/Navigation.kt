// NowWhat/app/src/main/java/com/nowwhat/app/Navigation.kt
package com.nowwhat.app

import com.nowwhat.app.screens.AddSubTaskDialog
import com.nowwhat.app.screens.EditProjectDialog
import com.nowwhat.app.screens.EditTaskDialog
import com.nowwhat.app.screens.EditSubTaskDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nowwhat.app.screens.*
import com.nowwhat.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    startDestination: String = Routes.Dashboard
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.Dashboard) {
            DashboardScreenWrapper(viewModel, navController)
        }

        composable(Routes.Projects) {
            ProjectsScreenWrapper(viewModel, navController)
        }

        composable(
            route = Routes.ProjectDetail,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId")?.toInt() ?: 0
            ProjectDetailScreenWrapper(viewModel, navController, projectId)
        }

        composable(
            route = Routes.TaskDetail,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")?.toInt() ?: 0
            TaskDetailScreenWrapper(viewModel, navController, taskId)
        }

        composable(Routes.Settings) {
            SettingsScreenWrapper(viewModel, navController)
        }

        composable(Routes.Stats) {
            StatsScreenWrapper(viewModel, navController)
        }

        composable(
            route = Routes.FocusMode,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")?.toInt() ?: 0
            FocusModeScreenWrapper(viewModel, navController, taskId)
        }

        composable(Routes.Archive) {
            ArchiveScreenWrapper(viewModel, navController)
        }

        composable(Routes.Capacity) {
            CapacityScreenWrapper(viewModel, navController)
        }
    }
}

@Composable
fun DashboardScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val user by viewModel.user.collectAsState(initial = null)
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val subTasks by viewModel.subTasks.collectAsState(initial = emptyList())
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val availableMinutes by viewModel.availableMinutes.collectAsState(initial = 0)
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            projects = projects,
            onDismiss = { showCreateTaskDialog = false },
            onCreateTask = { task ->
                viewModel.createTask(task)
                showCreateTaskDialog = false
            }
        )
    }

    if (user != null) {
        DashboardScreen(
            user = user!!,
            projects = projects,
            tasks = tasks,
            subTasks = subTasks,
            calendarEvents = calendarEvents,
            availableMinutes = availableMinutes,
            onNavigateToProjects = { navController.navigate(Routes.Projects) },
            onNavigateToSettings = { navController.navigate(Routes.Settings) },
            onNavigateToStats = { navController.navigate(Routes.Stats) },
            onNavigateToCapacity = { navController.navigate(Routes.Capacity) },
            onStartFocus = { task ->
                navController.navigate(Routes.focusMode(task.id.toLong()))
            },
            onFilterClick = { filter ->
                navController.navigate("${Routes.Projects}?filter=$filter")
            },
            onCreateTask = {
                showCreateTaskDialog = true
            },
            onTaskClick = { task ->
                navController.navigate(Routes.taskDetail(task.id.toLong()))
            },
            onProjectClick = { project ->
                navController.navigate(Routes.projectDetail(project.id.toLong()))
            },
            onGetCapacityForRange = viewModel::getCapacityForPeriod
        )
    }
}

@Composable
fun ProjectsScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val subTasks by viewModel.subTasks.collectAsState(initial = emptyList())

    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var showCreateSubTaskDialog by remember { mutableStateOf(false) }

    var showEditProjectDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf(false) }
    var showEditSubTaskDialog by remember { mutableStateOf(false) }

    var selectedProjectForTask by remember { mutableStateOf<com.nowwhat.app.model.Project?>(null) }
    var selectedTaskForSubTask by remember { mutableStateOf<com.nowwhat.app.model.Task?>(null) }

    var selectedProjectForEdit by remember { mutableStateOf<com.nowwhat.app.model.Project?>(null) }
    var selectedTaskForEdit by remember { mutableStateOf<com.nowwhat.app.model.Task?>(null) }
    var selectedSubTaskForEdit by remember { mutableStateOf<com.nowwhat.app.model.SubTask?>(null) }

    LaunchedEffect(projects) {
        android.util.Log.d("ProjectsScreen", "Projects updated: ${projects.size} total")
    }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = {
                showCreateProjectDialog = false
            },
            onCreateProject = { project ->
                viewModel.createProject(project)
                showCreateProjectDialog = false
            }
        )
    }

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            projects = projects,
            preSelectedProject = selectedProjectForTask,
            onDismiss = {
                showCreateTaskDialog = false
                selectedProjectForTask = null
            },
            onCreateTask = { task ->
                viewModel.createTask(task)
                showCreateTaskDialog = false
                selectedProjectForTask = null
            }
        )
    }

    if (showCreateSubTaskDialog && selectedTaskForSubTask != null) {
        AddSubTaskDialog(
            taskId = selectedTaskForSubTask!!.id,
            onDismiss = {
                showCreateSubTaskDialog = false
                selectedTaskForSubTask = null
            },
            onConfirm = { subTask ->
                viewModel.createSubTask(subTask)
                showCreateSubTaskDialog = false
                selectedTaskForSubTask = null
            }
        )
    }

    if (showEditProjectDialog && selectedProjectForEdit != null) {
        EditProjectDialog(
            project = selectedProjectForEdit!!,
            onDismiss = {
                showEditProjectDialog = false
                selectedProjectForEdit = null
            },
            onSaveProject = { updatedProject ->
                viewModel.updateProject(updatedProject)
                showEditProjectDialog = false
                selectedProjectForEdit = null
            }
        )
    }

    if (showEditTaskDialog && selectedTaskForEdit != null) {
        EditTaskDialog(
            task = selectedTaskForEdit!!,
            onDismiss = {
                showEditTaskDialog = false
                selectedTaskForEdit = null
            },
            onSaveTask = { updatedTask ->
                viewModel.updateTask(updatedTask)
                showEditTaskDialog = false
                selectedTaskForEdit = null
            }
        )
    }

    if (showEditSubTaskDialog && selectedSubTaskForEdit != null) {
        EditSubTaskDialog(
            subTask = selectedSubTaskForEdit!!,
            onDismiss = {
                showEditSubTaskDialog = false
                selectedSubTaskForEdit = null
            },
            onConfirm = { updatedSubTask ->
                viewModel.updateSubTask(updatedSubTask)
                showEditSubTaskDialog = false
                selectedSubTaskForEdit = null
            },
            onDelete = {
                viewModel.deleteSubTask(selectedSubTaskForEdit!!)
                showEditSubTaskDialog = false
                selectedSubTaskForEdit = null
            }
        )
    }

    ProjectsScreen(
        projects = projects,
        tasks = tasks,
        subTasks = subTasks,
        onProjectClick = { project ->
            navController.navigate(Routes.projectDetail(project.id.toLong()))
        },
        onTaskClick = { task ->
            navController.navigate(Routes.taskDetail(task.id.toLong()))
        },
        onCreateProject = {
            showCreateProjectDialog = true
        },
        onCreateTask = { project ->
            selectedProjectForTask = project
            showCreateTaskDialog = true
        },
        onToggleTaskDone = { task ->
            viewModel.toggleTaskDone(task)
        },
        onDeleteProject = { project ->
            viewModel.deleteProject(project)
        },
        onDeleteTask = { task ->
            viewModel.deleteTask(task)
        },
        onBackClick = {
            navController.popBackStack()
        },
        onNavigateToArchive = {
            navController.navigate(Routes.Archive)
        },
        onToggleSubTaskDone = { subTask ->
            viewModel.toggleSubTaskDone(subTask)
        },
        onProjectCompleted = { project ->
            viewModel.markProjectComplete(project)
        },
        onDeleteSubTask = { subTask ->
            viewModel.deleteSubTask(subTask)
        },
        onArchiveProject = { project ->
            viewModel.archiveProject(project)
        },
        onRestoreProject = { project ->
            viewModel.restoreProject(project)
        },
        onEditProject = { project ->
            selectedProjectForEdit = project
            showEditProjectDialog = true
        },
        onEditTask = { task ->
            selectedTaskForEdit = task
            showEditTaskDialog = true
        },
        onEditSubTask = { subTask ->
            selectedSubTaskForEdit = subTask
            showEditSubTaskDialog = true
        },
        onCreateSubTask = { task ->
            selectedTaskForSubTask = task
            showCreateSubTaskDialog = true
        }
    )
}

@Composable
fun ProjectDetailScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController,
    projectId: Int
) {
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val availableMinutes by viewModel.availableMinutes.collectAsState(initial = 0)
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    val project = projects.find { it.id == projectId }
    val projectTasks = tasks.filter { it.projectId == projectId }

    if (showCreateTaskDialog && project != null) {
        CreateTaskDialog(
            projects = listOf(project),
            preSelectedProject = project,
            onDismiss = { showCreateTaskDialog = false },
            onCreateTask = { task ->
                viewModel.createTask(task)
                showCreateTaskDialog = false
            }
        )
    }

    if (project != null) {
        ProjectDetailScreen(
            project = project,
            tasks = projectTasks,
            availableMinutes = availableMinutes,
            onBackClick = { navController.popBackStack() },
            onTaskClick = { task ->
                navController.navigate(Routes.taskDetail(task.id.toLong()))
            },
            onCreateTask = {
                showCreateTaskDialog = true
            },
            onToggleTaskDone = { task ->
                viewModel.toggleTaskDone(task)
            },
            onEditProject = {
                // TODO: Edit project dialog
            },
            onDeleteProject = {
                viewModel.deleteProject(project)
                navController.popBackStack()
            },
            onToggleProjectComplete = { shouldComplete ->
                if (shouldComplete) {
                    viewModel.markProjectComplete(project)
                } else {
                    viewModel.markProjectIncomplete(project)
                }
            }
        )
    }
}

@Composable
fun TaskDetailScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController,
    taskId: Int
) {
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val subTasks by viewModel.subTasks.collectAsState(initial = emptyList())
    var showAddSubTaskDialog by remember { mutableStateOf(false) }

    val task = tasks.find { it.id == taskId }
    val project = task?.let { t -> projects.find { it.id == t.projectId } }
    val taskSubTasks = subTasks.filter { it.taskId == taskId }

    if (showAddSubTaskDialog && task != null) {
        AddSubTaskDialog(
            taskId = task.id,
            onDismiss = { showAddSubTaskDialog = false },
            onConfirm = { subTask ->
                viewModel.createSubTask(subTask)
                showAddSubTaskDialog = false
            }
        )
    }

    if (task != null && project != null) {
        TaskDetailScreen(
            task = task,
            project = project,
            subTasks = taskSubTasks,
            onBackClick = { navController.popBackStack() },
            onEditTask = {
                // TODO: Edit task dialog
            },
            onDeleteTask = {
                viewModel.deleteTask(task)
                navController.popBackStack()
            },
            onToggleTaskDone = {
                viewModel.toggleTaskDone(task)
            },
            onStartFocus = {
                navController.navigate(Routes.focusMode(task.id.toLong()))
            },
            onAddSubTask = {
                showAddSubTaskDialog = true
            },
            onToggleSubTaskDone = { subTask ->
                viewModel.toggleSubTaskDone(subTask)
            },
            onClearWaitingFor = {
                viewModel.updateTask(task.copy(waitingFor = null))
            }
        )
    }
}

@Composable
fun SettingsScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val user by viewModel.user.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    if (user != null) {
        SettingsScreen(
            user = user!!,
            onBackClick = { navController.popBackStack() },
            onSaveSettings = { updatedUser ->
                scope.launch {
                    viewModel.userPreferences.saveUserProfile(updatedUser)
                    viewModel.refreshCalendarEvents()
                }
            },
            onClearData = {
                scope.launch {
                    val context = navController.context
                    viewModel.userPreferences.clearAll()
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    context.startActivity(intent)
                    (context as? android.app.Activity)?.finish()
                }
            }
        )
    }
}

@Composable
fun StatsScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val user by viewModel.user.collectAsState(initial = null)
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var tasksCompletedToday by remember { mutableIntStateOf(0) }
    var tasksCompletedThisWeek by remember { mutableIntStateOf(0) }
    var tasksCompletedThisMonth by remember { mutableIntStateOf(0) }
    var timeWorkedToday by remember { mutableIntStateOf(0) }
    var timeWorkedThisWeek by remember { mutableIntStateOf(0) }
    var timeWorkedThisMonth by remember { mutableIntStateOf(0) }
    var avgAccuracy by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        scope.launch {
            tasksCompletedToday = viewModel.getTasksCompletedToday()
            tasksCompletedThisWeek = viewModel.getTasksCompletedThisWeek()
            tasksCompletedThisMonth = viewModel.getTasksCompletedThisMonth()
            timeWorkedToday = viewModel.getTimeWorkedToday()
            timeWorkedThisWeek = viewModel.getTimeWorkedThisWeek()
            timeWorkedThisMonth = viewModel.getTimeWorkedThisMonth()
            avgAccuracy = viewModel.getAvgEstimationAccuracy()
        }
    }

    if (user != null) {
        StatsScreen(
            user = user!!,
            projects = projects,
            tasks = tasks,
            tasksCompletedToday = tasksCompletedToday,
            tasksCompletedThisWeek = tasksCompletedThisWeek,
            tasksCompletedThisMonth = tasksCompletedThisMonth,
            timeWorkedToday = timeWorkedToday,
            timeWorkedThisWeek = timeWorkedThisWeek,
            timeWorkedThisMonth = timeWorkedThisMonth,
            avgAccuracy = avgAccuracy,
            onBackClick = { navController.popBackStack() }
        )
    }
}

@Composable
fun FocusModeScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController,
    taskId: Int
) {
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val user by viewModel.user.collectAsState(initial = null)

    val task = tasks.find { it.id == taskId }

    if (task != null && user != null) {
        FocusModeScreen(
            task = task,
            focusDuration = user!!.focusDndMinutes,
            onBackClick = { navController.popBackStack() },
            onFinishTask = { actualMinutes ->
                viewModel.finishTask(task, actualMinutes)
                navController.popBackStack()
            }
        )
    }
}

@Composable
fun ArchiveScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())

    val archivedProjects = projects.filter { it.isArchived }
    val archivedTasks = tasks.filter { it.isArchived }

    ArchiveScreen(
        completedProjects = archivedProjects,
        archivedTasks = archivedTasks,
        onRestoreProject = { project ->
            viewModel.restoreProject(project)
        },
        onDeleteProjectForever = { project ->
            viewModel.deleteProject(project)
        },
        onRestoreTask = { task ->
            viewModel.restoreTask(task)
        },
        onDeleteTaskForever = { task ->
            viewModel.deleteTask(task)
        },
        onBackClick = { navController.popBackStack() }
    )
}

@Composable
fun CapacityScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val user by viewModel.user.collectAsState(initial = null)
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val subTasks by viewModel.subTasks.collectAsState(initial = emptyList())

    if (user != null) {
        CapacityScreen(
            user = user!!,
            tasks = tasks,
            subTasks = subTasks,
            onBackClick = { navController.popBackStack() },
            onTaskClick = { task ->
                navController.navigate(Routes.taskDetail(task.id.toLong()))
            }
        )
    }
}