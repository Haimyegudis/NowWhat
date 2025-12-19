package com.nowwhat.app.viewmodel

import android.app.Application
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

    val user: StateFlow<UserProfile?> = userPreferences.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    private val _availableMinutes = MutableStateFlow(0)
    val availableMinutes: StateFlow<Int> = _availableMinutes.asStateFlow()

    val tasks: StateFlow<List<Task>> = combine(
        dao.getAllTasks(),
        availableMinutes
    ) { tasks, availableMins ->
        tasks.map { task ->
            task.apply {
                urgencyScore = PriorityAlgorithm.calculateUrgency(this, availableMins)
                urgency = PriorityAlgorithm.getUrgencyLevel(urgencyScore)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val projects: StateFlow<List<Project>> = combine(
        dao.getAllProjects(),
        tasks,
        user
    ) { projects, allTasks, currentUser ->
        if (currentUser == null) {
            projects
        } else {
            projects.map { project ->
                val projectTasks = allTasks.filter { it.projectId == project.id }
                val allTasksDone = projectTasks.isNotEmpty() && projectTasks.all { it.isDone }

                project.apply {
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
                if (allTasksDone && !project.isCompleted && !project.markedComplete && project.totalTasks > 0) {
                    viewModelScope.launch {
                        try {
                            dao.updateProject(
                                project.copy(
                                    isCompleted = true,
                                    completedAt = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            // Ignore update errors
                        }
                    }
                }

                project
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val subTasks: StateFlow<List<SubTask>> = dao.getAllSubTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            user.collect { userProfile ->
                if (userProfile != null) {
                    refreshCalendarEvents()
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000L)
                if (user.value != null) {
                    refreshCalendarEvents()
                }
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
                }
            } catch (e: Exception) {
                // Ignore calendar errors - maybe no permission
                _availableMinutes.value = user.value?.dailyWorkMinutes ?: 480
            }
        }
    }

    fun createProject(project: Project) {
        viewModelScope.launch {
            dao.insertProject(project)
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            dao.updateProject(project)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            dao.deleteProject(project)
        }
    }

    fun markProjectComplete(project: Project) {
        viewModelScope.launch {
            dao.updateProject(
                project.copy(
                    isCompleted = true,
                    markedComplete = true,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun markProjectIncomplete(project: Project) {
        viewModelScope.launch {
            dao.updateProject(
                project.copy(
                    isCompleted = false,
                    markedComplete = false,
                    completedAt = null
                )
            )
        }
    }

    fun createTask(task: Task) {
        viewModelScope.launch {
            dao.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isDone = !task.isDone,
                completedAt = if (!task.isDone) System.currentTimeMillis() else null
            )
            dao.updateTask(updated)

            if (updated.isDone) {
                updateStreak()
            }
        }
    }

    fun finishTask(task: Task, actualMinutes: Int) {
        viewModelScope.launch {
            val updated = task.copy(
                isDone = true,
                actualMinutes = actualMinutes,
                completedAt = System.currentTimeMillis()
            )
            dao.updateTask(updated)
            updateStreak()
        }
    }

    fun moveUnfinishedTasksToTomorrow() {
        viewModelScope.launch {
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
        }
    }

    fun createSubTask(subTask: SubTask) {
        viewModelScope.launch {
            dao.insertSubTask(subTask)
        }
    }

    fun updateSubTask(subTask: SubTask) {
        viewModelScope.launch {
            dao.updateSubTask(subTask)
        }
    }

    fun deleteSubTask(subTask: SubTask) {
        viewModelScope.launch {
            dao.deleteSubTask(subTask)
        }
    }

    fun toggleSubTaskDone(subTask: SubTask) {
        viewModelScope.launch {
            val updated = subTask.copy(
                isDone = !subTask.isDone,
                completedAt = if (!subTask.isDone) System.currentTimeMillis() else null
            )
            dao.updateSubTask(updated)
        }
    }

    private fun updateStreak() {
        viewModelScope.launch {
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
                }
            }
        }
    }

    suspend fun getTasksCompletedToday(): Int {
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

        return dao.getTasksCompletedInRange(today, tomorrow)
    }

    suspend fun getTasksCompletedThisWeek(): Int {
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

        return dao.getTasksCompletedInRange(startOfWeek, endOfWeek)
    }

    suspend fun getTasksCompletedThisMonth(): Int {
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

        return dao.getTasksCompletedInRange(startOfMonth, endOfMonth)
    }

    suspend fun getTimeWorkedToday(): Int {
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

        return dao.getTotalMinutesWorkedInRange(today, tomorrow) ?: 0
    }

    suspend fun getTimeWorkedThisWeek(): Int {
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

        return dao.getTotalMinutesWorkedInRange(startOfWeek, endOfWeek) ?: 0
    }

    suspend fun getTimeWorkedThisMonth(): Int {
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

        return dao.getTotalMinutesWorkedInRange(startOfMonth, endOfMonth) ?: 0
    }

    suspend fun getAvgEstimationAccuracy(): Float {
        return dao.getAverageEstimationAccuracy() ?: 0f
    }

    suspend fun clearAllData() {
        userPreferences.clearAll()
    }
}