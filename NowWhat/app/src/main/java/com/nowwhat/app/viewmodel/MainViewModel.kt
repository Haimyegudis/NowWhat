package com.nowwhat.app.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowwhat.app.NowWhatApplication
import com.nowwhat.app.algorithm.PriorityAlgorithm
import com.nowwhat.app.data.CalendarEvent
import com.nowwhat.app.data.CalendarRepository
import com.nowwhat.app.data.UserPreferences
import com.nowwhat.app.model.*
import com.nowwhat.app.utils.AlarmReceiver
import com.nowwhat.app.utils.NotificationScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NowWhatApplication
    private val dao = app.database.dao()
    private val calendarRepository = CalendarRepository(application)
    val userPreferences = UserPreferences(application)
    private val notificationScheduler = NotificationScheduler(application)

    private val taskUseCases = TaskUseCases()

    companion object {
        private const val TAG = "MainViewModel"
    }

    // Events for UI
    private val _projectCompletionEvent = MutableSharedFlow<Project>()
    val projectCompletionEvent = _projectCompletionEvent.asSharedFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
            withContext(Dispatchers.Default) {
                taskUseCases.getTasksWithUrgency(tasks, availableMins)
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

    val subTasks: StateFlow<List<SubTask>> = dao.getAllSubTasks()
        .catch { e ->
            Log.e(TAG, "Error in subTasks flow", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val autoCompletedProjects = mutableSetOf<Int>()

    val projects: StateFlow<List<Project>> = combine(
        dao.getAllProjects(),
        tasks,
        subTasks,
        user
    ) { projects, allTasks, allSubTasks, currentUser ->
        withContext(Dispatchers.Default) {
            try {
                projects.map { project ->
                    val projectTasks = allTasks.filter { it.projectId == project.id }

                    val totalTasksCount = projectTasks.size
                    val completedTasksCount = projectTasks.count { it.isDone }
                    val timeNeeded = projectTasks.filter { !it.isDone }.sumOf { it.estimatedMinutes }

                    val riskCalc = if (currentUser != null) {
                        try {
                            PriorityAlgorithm.calculateProjectRisk(
                                project = project,
                                tasks = projectTasks,
                                subTasks = allSubTasks,
                                user = currentUser
                            )
                        } catch (e: Exception) {
                            RiskStatus.OnTrack
                        }
                    } else {
                        project.risk
                    }

                    project.copy(
                        totalTasks = totalTasksCount,
                        completedTasks = completedTasksCount,
                        risk = riskCalc,
                        timeNeededMinutes = timeNeeded
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in projects flow logic", e)
                projects
            }
        }
    }
        .catch { e ->
            Log.e(TAG, "Error in projects flow collection", e)
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
            withContext(Dispatchers.IO) {
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
                    Log.e(TAG, "Error refreshing calendar", e)
                    _availableMinutes.value = user.value?.dailyWorkMinutes ?: 480
                }
            }
        }
    }

    suspend fun getCapacityForPeriod(startMillis: Long, endMillis: Long): Int {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = user.value
                if (currentUser != null) {
                    calendarRepository.calculateAvailableMinutesForRange(startMillis, endMillis, currentUser)
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating period capacity", e)
                0
            }
        }
    }

    fun createProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = dao.insertProject(project)
                if (project.deadline != null) {
                    notificationScheduler.scheduleDeadlineReminder(
                        id.toInt(),
                        "Project Deadline: ${project.name}",
                        project.deadline,
                        isProject = true
                    )
                }
                Log.d(TAG, "Project created: ${project.name}, id=$id")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating project", e)
            }
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateProject(project)
                if (project.deadline != null) {
                    if (project.isCompleted || project.isArchived) {
                        notificationScheduler.cancelDeadlineReminder(project.id)
                    } else {
                        notificationScheduler.scheduleDeadlineReminder(
                            project.id,
                            "Project Deadline: ${project.name}",
                            project.deadline,
                            isProject = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating project", e)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteProject(project)
                autoCompletedProjects.remove(project.id)
                notificationScheduler.cancelDeadlineReminder(project.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting project", e)
            }
        }
    }

    fun archiveProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateProject(project.copy(isArchived = true))
                notificationScheduler.cancelDeadlineReminder(project.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving project", e)
            }
        }
    }

    fun restoreProject(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateProject(
                    project.copy(
                        isCompleted = false,
                        isArchived = false,
                        markedComplete = false,
                        completedAt = null
                    )
                )
                autoCompletedProjects.remove(project.id)
                if (project.deadline != null) {
                    notificationScheduler.scheduleDeadlineReminder(
                        project.id,
                        "Project Deadline: ${project.name}",
                        project.deadline,
                        isProject = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring project", e)
            }
        }
    }

    fun markProjectComplete(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateProject(
                    project.copy(
                        isCompleted = true,
                        markedComplete = true,
                        isArchived = false,
                        completedAt = System.currentTimeMillis()
                    )
                )
                autoCompletedProjects.remove(project.id)
                notificationScheduler.cancelDeadlineReminder(project.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking project complete", e)
            }
        }
    }

    fun markProjectIncomplete(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateProject(
                    project.copy(
                        isCompleted = false,
                        markedComplete = false,
                        isArchived = false,
                        completedAt = null
                    )
                )
                autoCompletedProjects.remove(project.id)
                if (project.deadline != null) {
                    notificationScheduler.scheduleDeadlineReminder(
                        project.id,
                        "Project Deadline: ${project.name}",
                        project.deadline,
                        isProject = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error marking project incomplete", e)
            }
        }
    }

    fun createTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = dao.insertTask(task)

                // שימוש בלוגיקה החדשה להתראות
                if (task.reminderTime != null) {
                    scheduleTaskAlarm(task.copy(id = id.toInt()))
                }

                if (task.deadline != null && !task.isDone) {
                    notificationScheduler.scheduleDeadlineReminder(
                        id.toInt(),
                        "Task Deadline: ${task.title}",
                        task.deadline,
                        isProject = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating task", e)
            }
        }
    }

    // פונקציה ייעודית לעדכון התראה מהמסך החדש
    fun updateTaskReminder(task: Task, newReminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updatedTask = task.copy(reminderTime = newReminderTime)
                dao.updateTask(updatedTask)

                if (newReminderTime != null) {
                    scheduleTaskAlarm(updatedTask)
                } else {
                    cancelTaskAlarm(task)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating task reminder", e)
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateTask(task)

                if (task.deadline != null) {
                    if (task.isDone || task.isArchived) {
                        notificationScheduler.cancelDeadlineReminder(task.id)
                    } else {
                        notificationScheduler.scheduleDeadlineReminder(
                            task.id,
                            "Task Deadline: ${task.title}",
                            task.deadline,
                            isProject = false
                        )
                    }
                }

                // עדכון התראה רגילה - שימוש בלוגיקה החדשה
                if (task.reminderTime != null) {
                    if (task.isDone || task.isArchived) {
                        cancelTaskAlarm(task)
                    } else {
                        scheduleTaskAlarm(task)
                    }
                } else {
                    cancelTaskAlarm(task)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating task", e)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteTask(task)
                cancelTaskAlarm(task) // ביטול ההתראה החדשה
                notificationScheduler.cancelDeadlineReminder(task.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting task", e)
            }
        }
    }

    fun archiveTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateTask(task.copy(isArchived = true))
                cancelTaskAlarm(task)
                notificationScheduler.cancelDeadlineReminder(task.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error archiving task", e)
            }
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = task.copy(
                    isArchived = false,
                    isDone = false,
                    completedAt = null
                )
                dao.updateTask(updated)

                if (updated.deadline != null && updated.deadline > System.currentTimeMillis()) {
                    notificationScheduler.scheduleDeadlineReminder(
                        updated.id,
                        "Task Deadline: ${updated.title}",
                        updated.deadline,
                        isProject = false
                    )
                }

                // שחזור התראה אם קיימת ורלוונטית
                if (updated.reminderTime != null && updated.reminderTime > System.currentTimeMillis()) {
                    scheduleTaskAlarm(updated)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error restoring task", e)
            }
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!task.isDone && !task.waitingFor.isNullOrBlank()) {
                    _toastMessage.emit("Cannot complete task while waiting for: ${task.waitingFor}")
                    return@launch
                }

                val newIsDone = !task.isDone
                val updated = task.copy(
                    isDone = newIsDone,
                    completedAt = if (newIsDone) System.currentTimeMillis() else null,
                    isArchived = false
                )
                dao.updateTask(updated)

                if (newIsDone) {
                    cancelTaskAlarm(task)
                    notificationScheduler.cancelDeadlineReminder(task.id)
                } else {
                    task.reminderTime?.let {
                        if (it > System.currentTimeMillis()) {
                            scheduleTaskAlarm(task)
                        }
                    }
                    task.deadline?.let {
                        if (it > System.currentTimeMillis()) {
                            notificationScheduler.scheduleDeadlineReminder(task.id, "Task Deadline: ${task.title}", it, false)
                        }
                    }
                }

                if (updated.isDone && updated.projectId != null) {
                    updateStreak()
                    checkIfProjectIsFinished(updated.projectId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling task", e)
            }
        }
    }

    private suspend fun checkIfProjectIsFinished(projectId: Int) {
        try {
            val projectTasks = dao.getTasksForProjectSync(projectId)
            val allDone = projectTasks.isNotEmpty() && projectTasks.all { it.isDone }

            if (allDone) {
                val project = dao.getProjectById(projectId)
                if (project != null && !project.isCompleted && !autoCompletedProjects.contains(projectId)) {
                    autoCompletedProjects.add(projectId)
                    _projectCompletionEvent.emit(project)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking project finish", e)
        }
    }

    fun finishTask(task: Task, actualMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = task.copy(
                    isDone = true,
                    actualMinutes = actualMinutes,
                    completedAt = System.currentTimeMillis()
                )
                dao.updateTask(updated)

                cancelTaskAlarm(task)
                notificationScheduler.cancelDeadlineReminder(task.id)

                updateStreak()
                if (task.projectId != null) {
                    checkIfProjectIsFinished(task.projectId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finishing task", e)
            }
        }
    }

    fun moveUnfinishedTasksToTomorrow() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val allTasks = dao.getAllTasks().first()

                allTasks.filter { !it.isDone && it.deadline != null && it.deadline!! < today }
                    .forEach { task ->
                        val updated = task.copy(movedToNextDay = task.movedToNextDay + 1)
                        dao.updateTask(updated)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error moving tasks to tomorrow", e)
            }
        }
    }

    fun createSubTask(subTask: SubTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.insertSubTask(subTask)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating subtask", e)
            }
        }
    }

    fun updateSubTask(subTask: SubTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.updateSubTask(subTask)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating subtask", e)
            }
        }
    }

    fun deleteSubTask(subTask: SubTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dao.deleteSubTask(subTask)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting subtask", e)
            }
        }
    }

    fun toggleSubTaskDone(subTask: SubTask) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = subTask.copy(
                    isDone = !subTask.isDone,
                    completedAt = if (!subTask.isDone) System.currentTimeMillis() else null
                )
                dao.updateSubTask(updated)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling subtask", e)
            }
        }
    }

    private fun updateStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                user.value?.let { currentUser ->
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val todayTasks = dao.getTasksCompletedInRange(today, System.currentTimeMillis())

                    if (todayTasks > 0) {
                        userPreferences.updateStreak(currentUser.currentStreak + 1)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating streak", e)
            }
        }
    }

    suspend fun getTasksCompletedToday(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getTasksCompletedThisWeek(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getTasksCompletedThisMonth(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getTimeWorkedToday(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getTimeWorkedThisWeek(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getTimeWorkedThisMonth(): Int = withContext(Dispatchers.IO) {
        try {
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
            0
        }
    }

    suspend fun getAvgEstimationAccuracy(): Float = withContext(Dispatchers.IO) {
        try {
            dao.getAverageEstimationAccuracy() ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userPreferences.clearAll()
                autoCompletedProjects.clear()
                Log.d(TAG, "All data cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing data", e)
            }
        }
    }

    // --- Private Alarm Helpers (שימוש ב-AlarmReceiver החדש) ---

    private fun scheduleTaskAlarm(task: Task) {
        if (task.reminderTime == null) return

        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        task.reminderTime,
                        pendingIntent
                    )
                } else {
                    Log.w("MainViewModel", "Permission denied for Exact Alarms")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.reminderTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("MainViewModel", "Security Exception scheduling alarm", e)
        }
    }

    private fun cancelTaskAlarm(task: Task) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}