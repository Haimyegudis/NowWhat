package com.nowwhat.app.algorithm

import com.nowwhat.app.model.*
import java.util.*
import java.util.concurrent.TimeUnit

object PriorityAlgorithm {

    /**
     * Calculate priority score for a task
     * Returns a score from 0 to 2000+ (higher = more urgent)
     */
    fun calculatePriority(
        task: Task,
        availableMinutes: Int,
        currentTime: Long = System.currentTimeMillis()
    ): Int {
        if (task.isDone) {
            return -2000 // Completed tasks sink to bottom
        }

        var score = 0

        // 1. Base Priority Score (0-200)
        val priorityScore = when (task.priority) {
            Priority.Immediate -> 200
            Priority.High -> 150
            Priority.Medium -> 90
            Priority.Low -> 40
        }

        // 2. Base Severity Score (0-150)
        val severityScore = when (task.severity) {
            Severity.Critical -> 150
            Severity.High -> 100
            Severity.Medium -> 60
            Severity.Low -> 30
        }

        // 3. Dynamic Weighting based on deadline proximity
        val deadline = task.deadline
        if (deadline != null) {
            val timeUntilDeadline = deadline - currentTime
            val daysUntilDeadline = TimeUnit.MILLISECONDS.toDays(timeUntilDeadline).toInt()

            val (priorityWeight, severityWeight) = when {
                daysUntilDeadline < 0 -> 0.95f to 0.05f  // Overdue: almost all priority
                daysUntilDeadline == 0 -> 0.85f to 0.15f // Due today: mostly priority
                daysUntilDeadline <= 7 -> 0.60f to 0.40f // This week: balanced
                daysUntilDeadline <= 30 -> 0.25f to 0.75f // This month: mostly severity
                else -> 0.15f to 0.85f // Far future: almost all severity
            }

            score += (priorityScore * priorityWeight + severityScore * severityWeight).toInt()

            // 4. Deadline Boosters
            val hoursUntilDeadline = TimeUnit.MILLISECONDS.toHours(timeUntilDeadline).toInt()
            when {
                daysUntilDeadline < 0 -> score += 300 // Overdue
                hoursUntilDeadline < 3 -> score += 250 // Less than 3 hours
                hoursUntilDeadline < 6 -> score += 200 // Less than 6 hours
                hoursUntilDeadline < 12 -> score += 150 // Less than 12 hours
                daysUntilDeadline == 0 -> score += 120 // Due today
                daysUntilDeadline == 1 -> score += 80 // Due tomorrow
                daysUntilDeadline <= 3 -> score += 50 // Due in 2-3 days
                daysUntilDeadline <= 7 -> score += 30 // Due this week
            }
        } else {
            // No deadline: use balanced weighting
            score += (priorityScore * 0.5f + severityScore * 0.5f).toInt()
        }

        // 5. Capacity Check
        val fitsInDay = task.estimatedMinutes <= availableMinutes
        val fitsWithOvertime = task.estimatedMinutes <= (availableMinutes * 1.2).toInt()

        when {
            fitsInDay -> score += 60 // Fits comfortably
            fitsWithOvertime -> score += 30 // Possible with overtime
            else -> score -= 40 // Too big for today
        }

        // 6. Momentum Bonus (already started)
        if (task.actualMinutes > 0) {
            val progressPercent = (task.actualMinutes.toFloat() / task.estimatedMinutes * 100).toInt()
            score += (80 * (progressPercent / 100f)).toInt()
        }

        // 7. Blocker Penalty
        if (task.hasBlocker) {
            score -= if (task.priority == Priority.Immediate || task.priority == Priority.High) {
                50 // Less penalty for urgent tasks
            } else {
                150 // Heavy penalty for non-urgent
            }
        }

        // 8. Moved to Next Day Bonus (procrastination indicator)
        if (task.movedToNextDay > 0) {
            score += task.movedToNextDay * 20
        }

        return score.coerceAtLeast(0)
    }

    /**
     * Get the top priority task (highest score)
     */
    fun getTopPriorityTask(
        tasks: List<Task>,
        availableMinutes: Int
    ): Task? {
        return tasks
            .filter { !it.isDone && !it.hasBlocker }
            .maxByOrNull { task ->
                calculatePriority(task, availableMinutes)
            }
    }

    /**
     * Get all tasks that fit in today's available time (greedy algorithm)
     */
    fun getTasksThatFitToday(
        tasks: List<Task>,
        availableMinutes: Int
    ): List<Task> {
        val sortedTasks = tasks
            .filter { !it.isDone && !it.hasBlocker }
            .sortedByDescending { task ->
                calculatePriority(task, availableMinutes)
            }

        val result = mutableListOf<Task>()
        var remainingMinutes = availableMinutes

        for (task in sortedTasks) {
            if (task.estimatedMinutes <= remainingMinutes) {
                result.add(task)
                remainingMinutes -= task.estimatedMinutes
            }
        }

        return result
    }

    /**
     * Calculate project risk based on time needed vs time available
     */
    fun calculateProjectRisk(
        project: Project,
        tasks: List<Task>,
        user: UserProfile
    ): RiskStatus {
        if (project.isCompleted) {
            return RiskStatus.OnTrack
        }

        val incompleteTasks = tasks.filter { !it.isDone }
        if (incompleteTasks.isEmpty()) {
            return RiskStatus.OnTrack
        }

        val totalMinutesNeeded = incompleteTasks.sumOf { it.estimatedMinutes }
        val minutesAvailable = calculateAvailableTimeUntilDeadline(project, user)

        val utilizationPercent = if (minutesAvailable > 0) {
            (totalMinutesNeeded.toFloat() / minutesAvailable) * 100
        } else {
            Float.MAX_VALUE
        }

        return when {
            utilizationPercent >= 100 -> RiskStatus.AtRisk
            utilizationPercent >= 80 -> RiskStatus.Warning
            else -> RiskStatus.OnTrack
        }
    }

    /**
     * Calculate available work time until project deadline
     */
    private fun calculateAvailableTimeUntilDeadline(
        project: Project,
        user: UserProfile
    ): Int {
        val deadline = project.deadline ?: return Int.MAX_VALUE
        val now = System.currentTimeMillis()

        if (deadline <= now) {
            return 0
        }

        val daysUntilDeadline = TimeUnit.MILLISECONDS.toDays(deadline - now).toInt()
        val hoursPerDay = user.endWorkHour - user.startWorkHour
        val workDaysPerWeek = user.workDays.size

        // Calculate total work days (approximate)
        val totalWorkDays = (daysUntilDeadline * workDaysPerWeek) / 7

        return totalWorkDays * hoursPerDay * 60
    }

    /**
     * Check if a task is critical (urgent and important)
     */
    fun isTaskCritical(task: Task, availableMinutes: Int): Boolean {
        if (task.isDone) return false

        val score = calculatePriority(task, availableMinutes)
        return score >= 400 // High threshold for critical
    }

    /**
     * Get urgency level based on deadline
     */
    fun getUrgency(task: Task): Urgency {
        val deadline = task.deadline ?: return Urgency.Low

        val now = System.currentTimeMillis()
        val timeUntilDeadline = deadline - now
        val hoursUntilDeadline = TimeUnit.MILLISECONDS.toHours(timeUntilDeadline).toInt()
        val daysUntilDeadline = TimeUnit.MILLISECONDS.toDays(timeUntilDeadline).toInt()

        return when {
            timeUntilDeadline < 0 -> Urgency.Immediate // Overdue
            hoursUntilDeadline < 3 -> Urgency.Immediate // Less than 3 hours
            daysUntilDeadline == 0 -> Urgency.Today // Due today
            hoursUntilDeadline < 24 -> Urgency.High // Less than 24 hours
            daysUntilDeadline <= 7 -> Urgency.ThisWeek // This week
            daysUntilDeadline <= 14 -> Urgency.Normal // Next two weeks
            daysUntilDeadline <= 30 -> Urgency.NextWeek // This month
            else -> Urgency.Low // More than a month
        }
    }

    /**
     * Get recommended focus duration for a task
     */
    fun getRecommendedFocusDuration(task: Task, userFocusDuration: Int): Int {
        val remaining = task.remainingMinutes

        return when {
            remaining <= 0 -> userFocusDuration // Default
            remaining < userFocusDuration -> remaining // Finish it
            remaining < userFocusDuration * 2 -> userFocusDuration // One session
            else -> userFocusDuration // Standard session
        }
    }

    /**
     * Suggest best time slots for a task based on calendar
     */
    fun suggestTimeSlots(
        task: Task,
        calendarEvents: List<com.nowwhat.app.data.CalendarEvent>,
        workStartHour: Int,
        workEndHour: Int
    ): List<Pair<Long, Long>> {
        // This is a placeholder for future implementation
        // Would analyze calendar gaps and suggest optimal times
        return emptyList()
    }

    /**
     * Calculate task completion probability
     */
    fun calculateCompletionProbability(
        task: Task,
        availableMinutes: Int
    ): Float {
        if (task.isDone) return 1.0f

        val deadline = task.deadline
        if (deadline == null) return 0.8f // No deadline = decent chance

        val now = System.currentTimeMillis()
        val timeUntilDeadline = deadline - now

        if (timeUntilDeadline < 0) return 0.0f // Already overdue

        val minutesUntilDeadline = TimeUnit.MILLISECONDS.toMinutes(timeUntilDeadline).toInt()
        val timeAvailable = minOf(availableMinutes, minutesUntilDeadline)

        return when {
            task.remainingMinutes <= timeAvailable * 0.5 -> 0.95f // Plenty of time
            task.remainingMinutes <= timeAvailable * 0.8 -> 0.75f // Good chance
            task.remainingMinutes <= timeAvailable -> 0.50f // Tight but possible
            task.remainingMinutes <= timeAvailable * 1.2 -> 0.25f // Risky
            else -> 0.1f // Very unlikely
        }
    }

    /**
     * Generate task recommendations based on context
     */
    fun generateRecommendations(
        tasks: List<Task>,
        availableMinutes: Int,
        currentTime: Long = System.currentTimeMillis()
    ): TaskRecommendations {
        val activeTasks = tasks.filter { !it.isDone }

        val topTask = getTopPriorityTask(activeTasks, availableMinutes)
        val criticalTasks = activeTasks.filter { isTaskCritical(it, availableMinutes) }
        val overdueTasks = activeTasks.filter {
            it.deadline != null && it.deadline!! < currentTime
        }
        val todayTasks = activeTasks.filter {
            it.deadline != null && isSameDay(it.deadline!!, currentTime)
        }
        val blockedTasks = activeTasks.filter { it.hasBlocker }

        return TaskRecommendations(
            topPriority = topTask,
            critical = criticalTasks,
            overdue = overdueTasks,
            dueToday = todayTasks,
            blocked = blockedTasks,
            fitInDay = getTasksThatFitToday(activeTasks, availableMinutes)
        )
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Data class for task recommendations
 */
data class TaskRecommendations(
    val topPriority: Task?,
    val critical: List<Task>,
    val overdue: List<Task>,
    val dueToday: List<Task>,
    val blocked: List<Task>,
    val fitInDay: List<Task>
)