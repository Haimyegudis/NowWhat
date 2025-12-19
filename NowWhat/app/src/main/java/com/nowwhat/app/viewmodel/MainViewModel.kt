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

    // Projects with calculated stats
    val projects: StateFlow<List<Project>> = combine(
        dao.getAllProjects(),
        dao.getAllTasks()
    ) { projects, tasks ->
        projects.map { project ->
            val projectTasks = tasks.filter { it.projectId == project.id }
            project.apply {
                totalTasks = projectTasks.size
                completedTasks = projectTasks.count { it.isDone }
                risk = PriorityAlgorithm.calculateProjectRisk(
                    project = this,
                    tasks = projectTasks,
                    availableMinutes = _availableMinutes.value
                )
                timeNeededMinutes = projectTasks
                    .filter { !it.isDone }
                    .sumOf { it.estimatedMinutes }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tasks
    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // SubTasks
    val subTasks: StateFlow<List<SubTask>> = dao.getAllSubTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Calendar events
    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents.asStateFlow()

    // Available minutes today
    private val _availableMinutes = MutableStateFlow(0)
    val availableMinutes: StateFlow<Int> = _availableMinutes.asStateFlow()

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
                    workStartHour = userProfile.startWorkHour,
                    workEndHour = userProfile.endWorkHour
                )
                _calendarEvents.value = events

                val availableMins = calendarRepository.calculateAvailableMinutesToday(
                    workStartHour = userProfile.startWorkHour,
                    workEndHour = userProfile.endWorkHour,
                    workDays = userProfile.workDays
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
            updateProjectRisk(task.projectId)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
            updateProjectRisk(task.projectId)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
            updateProjectRisk(task.projectId)
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(
                isDone = !task.isDone,
                completedAt = if (!task.isDone) System.currentTimeMillis() else null
            )
            dao.updateTask(updated)
            updateProjectRisk(task.projectId)

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
            updateProjectRisk(task.projectId)
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
                .filter { !it.isDone && it.deadline != null && it.deadline < today }
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

    // ========== Project Risk ==========
    private fun updateProjectRisk(projectId: Int) {
        viewModelScope.launch {
            val project = dao.getProjectById(projectId)
            val projectTasks = tasks.value.filter { it.projectId == projectId }

            if (project != null) {
                val risk = PriorityAlgorithm.calculateProjectRisk(
                    project = project,
                    tasks = projectTasks,
                    availableMinutes = _availableMinutes.value
                )
                // Risk is calculated in the flow, no need to update separately
            }
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
                    it.completedAt != null && it.completedAt >= today
                }

                if (todayTasks.isNotEmpty()) {
                    userPreferences.updateStreak(currentUser.currentStreak + 1)
                }
            }
        }
    }
}
