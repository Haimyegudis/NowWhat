package com.nowwhat.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nowwhat.app.screens.CreateProjectDialog
import com.nowwhat.app.screens.CreateTaskDialog
import com.nowwhat.app.screens.DashboardScreen
import com.nowwhat.app.screens.ProjectDetailScreen
import com.nowwhat.app.screens.ProjectsScreen
import com.nowwhat.app.screens.TaskDetailScreen
import com.nowwhat.app.viewmodel.MainViewModel

// Navigation routes
object Routes {
    const val Dashboard = "dashboard"
    const val Projects = "projects"
    const val ProjectDetail = "project/{projectId}"
    const val TaskDetail = "task/{taskId}"
    const val Settings = "settings"
    const val Stats = "stats"
    const val FocusMode = "focus/{taskId}"

    fun projectDetail(projectId: Long) = "project/$projectId"
    fun taskDetail(taskId: Long) = "task/$taskId"
    fun focusMode(taskId: Long) = "focus/$taskId"
}

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
        // Dashboard
        composable(Routes.Dashboard) {
            DashboardScreenWrapper(viewModel, navController)
        }

        // Projects
        composable(Routes.Projects) {
            ProjectsScreenWrapper(viewModel, navController)
        }

        // Project Detail
        composable(
            route = Routes.ProjectDetail,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId")?.toInt() ?: 0
            ProjectDetailScreenWrapper(viewModel, navController, projectId)
        }

        // Task Detail
        composable(
            route = Routes.TaskDetail,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId")?.toInt() ?: 0
            TaskDetailScreenWrapper(viewModel, navController, taskId)
        }

        // Settings (TODO: נוסיף בהמשך)
        composable(Routes.Settings) {
            // SettingsScreen(viewModel, navController)
        }

        // Stats (TODO: נוסיף בהמשך)
        composable(Routes.Stats) {
            // StatsScreen(viewModel, navController)
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
            calendarEvents = calendarEvents,
            availableMinutes = availableMinutes,
            onNavigateToProjects = { navController.navigate(Routes.Projects) },
            onNavigateToSettings = { navController.navigate(Routes.Settings) },
            onNavigateToStats = { navController.navigate(Routes.Stats) },
            onStartFocus = { task ->
                navController.navigate(Routes.focusMode(task.id.toLong()))
            },
            onFilterClick = { filter ->
                navController.navigate("${Routes.Projects}?filter=$filter")
            },
            onCreateTask = {
                showCreateTaskDialog = true
            }
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
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreateProject = { project ->
                viewModel.createProject(project)
                showCreateProjectDialog = false
            }
        )
    }

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

    ProjectsScreen(
        projects = projects,
        tasks = tasks,
        onProjectClick = { project ->
            navController.navigate(Routes.projectDetail(project.id.toLong()))
        },
        onTaskClick = { task ->
            navController.navigate(Routes.taskDetail(task.id.toLong()))
        },
        onCreateProject = {
            showCreateProjectDialog = true
        },
        onCreateTask = {
            showCreateTaskDialog = true
        },
        onToggleTaskDone = { task ->
            viewModel.toggleTaskDone(task)
        },
        onBackClick = {
            navController.popBackStack()
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

    val task = tasks.find { it.id == taskId }
    val project = task?.let { t -> projects.find { it.id == t.projectId } }
    val taskSubTasks = subTasks.filter { it.taskId == taskId }

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
                // TODO: Add subtask dialog
            },
            onToggleSubTaskDone = { subTask ->
                viewModel.toggleSubTaskDone(subTask)
            }
        )
    }
}