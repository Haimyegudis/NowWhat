package com.nowwhat.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nowwhat.app.screens.DashboardScreen
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

        // Projects (TODO: נוסיף בהמשך)
        composable(Routes.Projects) {
            // ProjectsScreen(viewModel, navController)
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
private fun DashboardScreenWrapper(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val user by viewModel.user.collectAsState(initial = null)
    val projects by viewModel.projects.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())
    val calendarEvents by viewModel.calendarEvents.collectAsState(initial = emptyList())
    val availableMinutes by viewModel.availableMinutes.collectAsState(initial = 0)

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
            onStartFocus = { task -> navController.navigate(Routes.focusMode(task.id.toLong()))
            },
            onFilterClick = { filter ->
                // Navigate to projects with filter
                navController.navigate("${Routes.Projects}?filter=$filter")
            }
        )
    }
}