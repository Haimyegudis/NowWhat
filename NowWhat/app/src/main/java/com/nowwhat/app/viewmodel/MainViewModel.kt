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

    // User profile
    val user: StateFlow<UserProfile?> = userPreferences.userProfileFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Tasks
    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Calendar events
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // Available minutes today
    private val _availableMinutes = MutableStateFlow(0)
    val availableMinutes: StateFlow<Int> = _availableMinutes.asStateFlow()

    // Projects with calculated stats
    val projects: StateFlow<List<Project>> = combine(
        dao.getAllProjects(),
        tasks,
        user
    ) { projects, tasks, user ->
        if (user == null) {
            projects
        } else {
            projects.map { project ->
                val projectTasks = tasks.filter { it.projectId == project.id }
                project.apply {
                    totalTasks = projectTasks.size
                    completedTasks = projectTasks.count { it.isDone }
                    risk = PriorityAlgorithm.calculateProjectRisk(
                        project = this,
                        tasks = projectTasks,
                        user = user
                    )
                    timeNeededMinutes = projectTasks
                        .filter { !it.isDone }
                        .sumOf { it.estimatedMinutes }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // SubTasks
    val subTasks: StateFlow<List<SubTask>> = dao.getAllSubTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refreshCalendarEvents()
    }

    // ========== Calendar ==========
    fun refreshCalendarEvents() {
        viewModelScope.launch {
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
        }
    }

    // ========== Projects ==========
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

    // ========== Tasks ==========
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

    // ========== SubTasks ==========
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

    // ========== Streak ==========
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
    // ========== Statistics ==========
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

    // ========== Clear All Data ==========
    suspend fun clearAllData() {
        userPreferences.clearAll()
    }
}
