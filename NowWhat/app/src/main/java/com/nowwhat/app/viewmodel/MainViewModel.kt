package com.nowwhat.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowwhat.app.NowWhatApplication
import com.nowwhat.app.algorithm.PriorityAlgorithm
import com.nowwhat.app.data.CalendarEvent
import com.nowwhat.app.data.CalendarRepository
import com.nowwhat.app.data.UserPreferences
import com.nowwhat.app.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NowWhatApplication
    private val dao = app.database.dao()
    private val calendarRepository = CalendarRepository(application)
    val userPreferences = UserPreferences(application)

    companion object {
        private const val TAG = "MainViewModel"
    }

    val user: StateFlow<UserProfile?> = userPreferences.userProfileFlow
        .catch { e ->
            Log.e(TAG, "Error in user flow", e)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _availableMinutes = MutableStateFlow(0)
    val availableMinutes: StateFlow<Int> = _availableMinutes.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        dao.getAllTasks(),
        availableMinutes
    ) { tasks, availableMins ->
        try {
            tasks.map { task ->
                task.apply {
                    urgencyScore = PriorityAlgorithm.calculateUrgency(this, availableMins)
                    urgency = PriorityAlgorithm.getUrgencyLevel(urgencyScore)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating urgency scores", e)
            tasks
        }
    }
        .catch { e ->
            Log.e(TAG, "Error in tasks flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Store project IDs that have been auto-completed to prevent loops
    private val autoCompletedProjects = mutableSetOf<Int>()

    val projects: StateFlow<List<Project>> = combine(
        dao.getAllProjects(),
        tasks,
        user
    ) { projects, allTasks, currentUser ->
        try {
            if (currentUser == null) {
                projects
            } else {
                projects.map { project ->
                    val projectTasks = allTasks.filter { it.projectId == project.id }
                    val allTasksDone = projectTasks.isNotEmpty() && projectTasks.all { it.isDone }

                    val updatedProject = project.apply {
                        totalTasks = projectTasks.size
                        completedTasks = projectTasks.count { it.isDone }
                        risk = PriorityAlgorithm.calculateProjectRisk(
                            project = this,
                            tasks = projectTasks,
                            user = currentUser
                        )
                        timeNeededMinutes = projectTasks
                            .filter { !it.isDone }
                            .sumOf { it.estimatedMinutes }
                    }

                    // Auto-complete project if all tasks are done and not manually marked
                    // Use a flag to prevent doing this multiple times
                    if (allTasksDone &&
                        !project.isCompleted &&
                        !project.markedComplete &&
                        project.totalTasks > 0 &&
                        !autoCompletedProjects.contains(project.id)) {

                        autoCompletedProjects.add(project.id)

                        // Launch in a separate coroutine to avoid blocking the flow
                        viewModelScope.launch {
                            try {
                                Log.d(TAG, "Auto-completing project: ${project.name}")
                                dao.updateProject(
                                    project.copy(
                                        isCompleted = true,
                                        completedAt = System.currentTimeMillis()
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error auto-completing project", e)
                                autoCompletedProjects.remove(project.id)
                            }
                        }
                    }

                    // Remove from set if project is no longer done
                    if (!allTasksDone && autoCompletedProjects.contains(project.id)) {
                        autoCompletedProjects.remove(project.id)
                    }

                    updatedProject
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in projects flow", e)
            projects
        }
    }
        .catch { e ->
            Log.e(TAG, "Error in projects flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val subTasks: StateFlow<List<SubTask>> = dao.getAllSubTasks()
        .catch { e ->
            Log.e(TAG, "Error in subTasks flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            try {
                user.collect { userProfile ->
                    if (userProfile != null) {
                        refreshCalendarEvents()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in user collection", e)
            }
        }

        // Auto-refresh calendar every 5 minutes
        viewModelScope.launch {
            try {
                while (true) {
                    kotlinx.coroutines.delay(5 * 60 * 1000L)
                    if (user.value != null) {
                        refreshCalendarEvents()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in auto-refresh loop", e)
            }
        }
    }

    fun refreshCalendarEvents() {
        viewModelScope.launch {
            try {
                user.value?.let { userProfile ->
                    val today = Calendar.getInstance()
                    val events = calendarRepository.getEventsForDate(
                        date = today,
                        user = userProfile
                    )
                    _calendarEvents.value = events

                    val availableMins = calendarRepository.calculateAvailableMinutesToday(
                        user = userProfile
                    )
                    _availableMinutes.value = availableMins

                    Log.d(TAG, "Calendar refreshed: ${events.size} events, $availableMins mins available")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing calendar", e)
                // Set default work minutes on error
                _availableMinutes.value = user.value?.dailyWorkMinutes ?: 480
            }
        }
    }

    fun createProject(project: Project) {
        viewModelScope.launch {
            try {
                dao.insertProject(project)
                Log.d(TAG, "Project created: ${project.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating project", e)
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            try {
                dao.updateProject(project)
                Log.d(TAG, "Project updated: ${project.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating project", e)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                dao.deleteProject(project)
                autoCompletedProjects.remove(project.id)
                Log.d(TAG, "Project deleted: ${project.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting project", e)
            }
        }
    }

    fun markProjectComplete(project: Project) {
        viewModelScope.launch {
            try {
                dao.updateProject(
                    project.copy(
                        isCompleted = true,
                        markedComplete = true,
                        completedAt = System.currentTimeMillis()
                    )
                )
                autoCompletedProjects.remove(project.id)
                Log.d(TAG, "Project marked complete: ${project.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking project complete", e)
            }
        }
    }

    fun markProjectIncomplete(project: Project) {
        viewModelScope.launch {
            try {
                dao.updateProject(
                    project.copy(
                        isCompleted = false,
                        markedComplete = false,
                        completedAt = null
                    )
                )
                autoCompletedProjects.remove(project.id)
                Log.d(TAG, "Project marked incomplete: ${project.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking project incomplete", e)
            }
        }
    }

    fun createTask(task: Task) {
        viewModelScope.launch {
            try {
                dao.insertTask(task)
                Log.d(TAG, "Task created: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating task", e)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                dao.updateTask(task)
                Log.d(TAG, "Task updated: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating task", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                dao.deleteTask(task)
                Log.d(TAG, "Task deleted: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task", e)
            }
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            try {
                val updated = task.copy(
                    isDone = !task.isDone,
                    completedAt = if (!task.isDone) System.currentTimeMillis() else null
                )
                dao.updateTask(updated)

                if (updated.isDone) {
                    updateStreak()
                }

                Log.d(TAG, "Task toggled: ${task.title}, done=${updated.isDone}")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling task", e)
            }
        }
    }

    fun finishTask(task: Task, actualMinutes: Int) {
        viewModelScope.launch {
            try {
                val updated = task.copy(
                    isDone = true,
                    actualMinutes = actualMinutes,
                    completedAt = System.currentTimeMillis()
                )
                dao.updateTask(updated)
                updateStreak()
                Log.d(TAG, "Task finished: ${task.title}, actual minutes: $actualMinutes")
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing task", e)
            }
        }
    }

    fun moveUnfinishedTasksToTomorrow() {
        viewModelScope.launch {
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                tasks.value
                    .filter { !it.isDone && it.deadline != null && it.deadline!! < today }
                    .forEach { task ->
                        val updated = task.copy(movedToNextDay = task.movedToNextDay + 1)
                        dao.updateTask(updated)
                    }

                Log.d(TAG, "Moved unfinished tasks to tomorrow")
            } catch (e: Exception) {
                Log.e(TAG, "Error moving tasks to tomorrow", e)
            }
        }
    }

    fun createSubTask(subTask: SubTask) {
        viewModelScope.launch {
            try {
                dao.insertSubTask(subTask)
                Log.d(TAG, "SubTask created: ${subTask.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating subtask", e)
            }
        }
    }

    fun updateSubTask(subTask: SubTask) {
        viewModelScope.launch {
            try {
                dao.updateSubTask(subTask)
                Log.d(TAG, "SubTask updated: ${subTask.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating subtask", e)
            }
        }
    }

    fun deleteSubTask(subTask: SubTask) {
        viewModelScope.launch {
            try {
                dao.deleteSubTask(subTask)
                Log.d(TAG, "SubTask deleted: ${subTask.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting subtask", e)
            }
        }
    }

    fun toggleSubTaskDone(subTask: SubTask) {
        viewModelScope.launch {
            try {
                val updated = subTask.copy(
                    isDone = !subTask.isDone,
                    completedAt = if (!subTask.isDone) System.currentTimeMillis() else null
                )
                dao.updateSubTask(updated)
                Log.d(TAG, "SubTask toggled: ${subTask.title}, done=${updated.isDone}")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling subtask", e)
            }
        }
    }

    private fun updateStreak() {
        viewModelScope.launch {
            try {
                user.value?.let { currentUser ->
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val todayTasks = tasks.value.filter {
                        it.completedAt != null && it.completedAt!! >= today
                    }

                    if (todayTasks.isNotEmpty()) {
                        userPreferences.updateStreak(currentUser.currentStreak + 1)
                        Log.d(TAG, "Streak updated: ${currentUser.currentStreak + 1}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating streak", e)
            }
        }
    }

    suspend fun getTasksCompletedToday(): Int {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            dao.getTasksCompletedInRange(today, tomorrow)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks completed today", e)
            0
        }
    }

    suspend fun getTasksCompletedThisWeek(): Int {
        return try {
            val startOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            dao.getTasksCompletedInRange(startOfWeek, endOfWeek)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks completed this week", e)
            0
        }
    }

    suspend fun getTasksCompletedThisMonth(): Int {
        return try {
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            dao.getTasksCompletedInRange(startOfMonth, endOfMonth)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks completed this month", e)
            0
        }
    }

    suspend fun getTimeWorkedToday(): Int {
        return try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            dao.getTotalMinutesWorkedInRange(today, tomorrow) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting time worked today", e)
            0
        }
    }

    suspend fun getTimeWorkedThisWeek(): Int {
        return try {
            val startOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            dao.getTotalMinutesWorkedInRange(startOfWeek, endOfWeek) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting time worked this week", e)
            0
        }
    }

    suspend fun getTimeWorkedThisMonth(): Int {
        return try {
            val startOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfMonth = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            dao.getTotalMinutesWorkedInRange(startOfMonth, endOfMonth) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting time worked this month", e)
            0
        }
    }

    suspend fun getAvgEstimationAccuracy(): Float {
        return try {
            dao.getAverageEstimationAccuracy() ?: 0f
        } catch (e: Exception) {
            Log.e(TAG, "Error getting estimation accuracy", e)
            0f
        }
    }

    suspend fun clearAllData() {
        try {
            userPreferences.clearAll()
            autoCompletedProjects.clear()
            Log.d(TAG, "All data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data", e)
        }
    }
}