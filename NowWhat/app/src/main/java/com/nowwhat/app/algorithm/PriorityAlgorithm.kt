package com.nowwhat.app.algorithm

import com.nowwhat.app.model.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Data structure holding a task and its subtasks.
 * Refactored to support "Parent-First" time estimation.
 */
data class TaskWithSubTasks(
    val task: Task,
    val subTasks: List<SubTask>
) {
    // CHANGE: Optimization - If parent has time, ignore subtasks time (Checklist mode).
    // If parent time is 0, sum the subtasks (Micro-management mode).
    val totalEstimatedMinutes: Int
        get() = if (task.estimatedMinutes > 0) task.estimatedMinutes else subTasks.sumOf { it.estimatedMinutes }

    val totalActualMinutes: Int
        get() = task.actualMinutes + subTasks.sumOf { it.actualMinutes }

    val remainingMinutes: Int
        get() = max(0, totalEstimatedMinutes - totalActualMinutes)
}

sealed class ScheduledItem {
    abstract val scheduledDate: Long
    abstract val estimatedMinutes: Int

    data class FullTask(
        val taskWithSubs: TaskWithSubTasks,
        override val scheduledDate: Long
    ) : ScheduledItem() {
        override val estimatedMinutes = taskWithSubs.totalEstimatedMinutes
    }

    data class SplitTask(
        val taskWithSubs: TaskWithSubTasks,
        override val estimatedMinutes: Int,
        override val scheduledDate: Long,
        val partNumber: Int
    ) : ScheduledItem()
}

object PriorityAlgorithm {

    // ============================================================================================
    // SECTION 1: CORE SCORING LOGIC (NORMALIZED 0-100)
    // ============================================================================================

    /**
     * Calculates a normalized urgency score (0-100).
     * Replaces magic numbers with weighted factors.
     */
    fun calculateUrgency(task: Task, availableMinutesToday: Int): Int {
        if (task.isDone) return 0
        if (task.isBlocked) return 0 // Blocked tasks have 0 urgency until unblocked

        var score = 0.0

        // 1. Priority Factor (Max 30 points)
        val priorityScore = when (task.priority) {
            Priority.Critical -> 30
            Priority.Immediate -> 25
            Priority.High -> 20
            Priority.Medium -> 10
            Priority.Low -> 5
        }
        score += priorityScore

        // 2. Severity/Impact Factor (Max 20 points)
        val severityScore = when (task.severity) {
            Severity.Critical -> 20
            Severity.High -> 15
            Severity.Medium -> 10
            Severity.Low -> 5
        }
        score += severityScore

        // 3. Time Pressure Factor (Max 40 points)
        val now = System.currentTimeMillis()
        val deadline = task.deadline

        if (deadline != null) {
            val hoursRemaining = TimeUnit.MILLISECONDS.toHours(deadline - now)

            val timeScore = when {
                hoursRemaining < 0 -> 40.0 // Overdue! Max points
                hoursRemaining < 24 -> 35.0 + (1.0 - (hoursRemaining / 24.0)) * 5 // 35-40 pts
                hoursRemaining < 72 -> 25.0 // 3 days
                hoursRemaining < 168 -> 15.0 // 1 week
                else -> 5.0
            }
            score += timeScore
        } else {
            // No deadline? rely mostly on priority, but give a tiny boost
            score += 5
        }

        // 4. Momentum & Fit Factor (Max 10 points)
        if (task.actualMinutes > 0) {
            score += 5 // Started tasks should be finished
        }

        if (task.estimatedMinutes <= availableMinutesToday) {
            score += 5 // Quick win / Fits in schedule
        }

        // 5. Anti-Procrastination Boost
        // If moved multiple times, slightly increase score to prevent starvation
        if (task.movedToNextDay > 0) {
            score += min(task.movedToNextDay * 2, 10) // Cap at 10 extra points
        }

        return score.toInt().coerceIn(0, 100)
    }

    /**
     * Provides a transparent reason for the score.
     */
    fun calculateUrgencyReason(task: Task): String {
        val now = System.currentTimeMillis()
        val deadline = task.deadline

        if (task.isBlocked) return "Blocked: ${task.waitingFor ?: task.blockerDescription}"
        if (task.isDone) return "Completed"

        if (deadline != null) {
            val hoursLeft = TimeUnit.MILLISECONDS.toHours(deadline - now)
            if (hoursLeft < 0) return "Overdue! Action required"
            if (hoursLeft < 24) return "Due today"
            if (hoursLeft < 48) return "Due tomorrow"
        }

        if (task.priority == Priority.Critical || task.priority == Priority.Immediate) return "High Priority Item"
        if (task.actualMinutes > 0) return "In Progress - Keep going"
        if (task.movedToNextDay > 3) return "Procrastinated often"

        return "Standard Priority"
    }

    /**
     * Maps the 0-100 score to an Urgency Enum.
     * Updated thresholds for the new normalized scale.
     */
    fun getUrgencyLevel(score: Int): Urgency {
        return when {
            score >= 85 -> Urgency.Critical
            score >= 70 -> Urgency.VeryHigh
            score >= 50 -> Urgency.High
            score >= 30 -> Urgency.Medium
            else -> Urgency.Low
        }
    }

    // ============================================================================================
    // SECTION 2: DATE & RANGE HELPERS
    // ============================================================================================

    fun getRangeForPeriod(period: String): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Set to end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        return when (period) {
            "Daily" -> now to endOfToday
            "Weekly" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                val endOfWeek = calendar.timeInMillis
                now to endOfWeek
            }
            "Monthly" -> {
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endOfMonth = calendar.timeInMillis
                now to endOfMonth
            }
            else -> now to endOfToday
        }
    }

    // ============================================================================================
    // SECTION 3: WORKLOAD & CAPACITY CALCULATIONS
    // ============================================================================================

    /**
     * Smart Workload Calculation.
     * Includes tasks based on Score, not just hard deadlines.
     */
    fun calculateWorkloadForRange(
        tasks: List<Task>,
        subTasks: List<SubTask>,
        rangeStart: Long,
        rangeEnd: Long
    ): Int {
        val allOpenTasks = tasks.filter { !it.isDone && !it.isBlocked }
        val sortedTasks = allOpenTasks.sortedByDescending { it.urgencyScore }

        val relevantTasks = sortedTasks.filter { task ->
            val deadline = task.deadline ?: Long.MAX_VALUE

            val isDeadlineInScope = deadline <= rangeEnd
            val isOverdue = deadline < rangeStart
            val isInProgress = task.actualMinutes > 0
            // Threshold updated for 0-100 scale (approx 30 is Medium/High boundary)
            val isImportant = task.urgencyScore >= 30

            isDeadlineInScope || isOverdue || isInProgress || isImportant
        }

        val taskMinutes = relevantTasks.sumOf { it.estimatedMinutes }

        // Only sum subtasks if parent estimation is 0 (Logic inside TaskWithSubTasks could handle this,
        // but here we are summing raw entities, so we must be careful).
        // Since we are moving to "Parent Estimation Rules", we check:
        var extraSubTaskMinutes = 0
        val subTasksMap = subTasks.filter { !it.isDone }.groupBy { it.taskId }

        for (task in relevantTasks) {
            if (task.estimatedMinutes == 0) {
                extraSubTaskMinutes += subTasksMap[task.id]?.sumOf { it.estimatedMinutes } ?: 0
            }
        }

        return taskMinutes + extraSubTaskMinutes
    }

    fun getQueueTasks(
        tasks: List<Task>,
        rangeStart: Long,
        rangeEnd: Long
    ): List<Task> {
        return tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }
            .take(5)
    }

    // ============================================================================================
    // SECTION 4: PROJECT RISK & PLANNING
    // ============================================================================================

    private fun getTotalMinutes(task: Task, subTasksMap: Map<Int, List<SubTask>>): Int {
        // Updated logic: if parent has time, use it.
        return if (task.estimatedMinutes > 0) {
            task.estimatedMinutes
        } else {
            val subs = subTasksMap[task.id] ?: emptyList()
            subs.sumOf { it.estimatedMinutes }
        }
    }

    fun calculateProjectRisk(
        project: Project,
        tasks: List<Task>,
        subTasks: List<SubTask>,
        user: UserProfile
    ): RiskStatus {
        if (project.deadline == null) return RiskStatus.OnTrack

        val now = System.currentTimeMillis()
        val timeLeft = project.deadline - now

        if (timeLeft < 0) return RiskStatus.Critical

        val undoneTasks = tasks.filter { !it.isDone }
        val subTasksMap = subTasks.filter { !it.isDone }.groupBy { it.taskId }
        val totalTimeNeeded = undoneTasks.sumOf { getTotalMinutes(it, subTasksMap) }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(timeLeft)
        val workDaysLeft = (daysLeft * 5 / 7).coerceAtLeast(1) // Assume 5 day work week
        val availableMinutes = workDaysLeft * user.dailyWorkMinutes

        val utilizationRatio = totalTimeNeeded.toFloat() / availableMinutes.coerceAtLeast(1)

        return when {
            utilizationRatio > 1.2 -> RiskStatus.Critical
            utilizationRatio > 0.85 -> RiskStatus.AtRisk
            else -> RiskStatus.OnTrack
        }
    }

    fun calculateProjectRisk(project: Project, tasks: List<Task>, user: UserProfile): RiskStatus {
        return calculateProjectRisk(project, tasks, emptyList(), user)
    }

    fun predictProjectCompletion(
        project: Project,
        tasks: List<Task>,
        dailyCapacity: Int
    ): Long {
        val undoneTasks = tasks.filter { !it.isDone && !it.isBlocked }
        val totalMinutesNeeded = undoneTasks.sumOf { it.estimatedMinutes }
        val daysNeeded = (totalMinutesNeeded.toFloat() / dailyCapacity.coerceAtLeast(1)).toInt()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysNeeded)

        return calendar.timeInMillis
    }

    fun willMissDeadline(
        project: Project,
        tasks: List<Task>,
        dailyCapacity: Int
    ): Boolean {
        if (project.deadline == null) return false
        val predictedCompletion = predictProjectCompletion(project, tasks, dailyCapacity)
        return predictedCompletion > project.deadline!!
    }

    // ============================================================================================
    // SECTION 5: RECOMMENDATIONS & WARNINGS
    // ============================================================================================

    fun getRecommendedTasks(
        tasks: List<Task>,
        availableMinutes: Int,
        maxTasks: Int = 5
    ): List<Task> {
        return tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }
            .take(maxTasks)
    }

    fun recommendFocusTask(
        tasks: List<Task>,
        availableMinutes: Int
    ): Task? {
        val validTasks = tasks.filter { !it.isDone && !it.isBlocked }

        // Find highest urgency task that fits time
        val fittingTask = validTasks
            .filter { it.estimatedMinutes <= availableMinutes }
            .maxByOrNull { it.urgencyScore }

        // Fallback to highest urgency overall if nothing fits perfectly
        return fittingTask ?: validTasks.maxByOrNull { it.urgencyScore }
    }

    fun getTasksForToday(
        tasks: List<Task>,
        subTasks: List<SubTask>,
        availableMinutes: Int,
        user: UserProfile
    ): List<Task> {
        val undoneTasks = tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }

        val subTasksMap = subTasks.filter { !it.isDone }.groupBy { it.taskId }
        val selectedTasks = mutableListOf<Task>()
        var remainingMinutes = availableMinutes

        for (task in undoneTasks) {
            val totalMinutes = getTotalMinutes(task, subTasksMap)
            if (totalMinutes <= remainingMinutes) {
                selectedTasks.add(task)
                remainingMinutes -= totalMinutes
            }
            if (selectedTasks.size >= 10) break
        }
        return selectedTasks
    }

    // Legacy overload
    fun getTasksForToday(tasks: List<Task>, availableMinutes: Int, user: UserProfile): List<Task> {
        return getTasksForToday(tasks, emptyList(), availableMinutes, user)
    }

    fun shouldWorkOnTask(
        task: Task,
        availableMinutes: Int,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (task.isDone || task.isBlocked) return false

        val deadline = task.deadline ?: return task.urgencyScore >= 40 // New Threshold
        val timeLeft = deadline - currentTime

        if (timeLeft < 0) return true

        val hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeft)

        if (hoursLeft < 24 && task.estimatedMinutes <= availableMinutes) {
            return true
        }

        return task.urgencyScore >= 60 // New Threshold for High Priority
    }

    fun getWarnings(
        tasks: List<Task>,
        projects: List<Project>,
        availableMinutes: Int,
        user: UserProfile
    ): List<Warning> {
        val warnings = mutableListOf<Warning>()

        val undoneTasks = tasks.filter { !it.isDone && !it.isBlocked }
        val totalTimeNeeded = undoneTasks.sumOf { it.estimatedMinutes }

        if (totalTimeNeeded > availableMinutes * 1.5) {
            warnings.add(Warning.HighWorkload)
        }

        if (availableMinutes < user.dailyWorkMinutes * 0.3) {
            warnings.add(Warning.LowCapacity)
        }

        val now = System.currentTimeMillis()
        val urgentTasks = undoneTasks.filter { task ->
            task.deadline?.let { deadline ->
                val hoursLeft = TimeUnit.MILLISECONDS.toHours(deadline - now)
                hoursLeft in 1..24
            } ?: false
        }

        if (urgentTasks.isNotEmpty()) {
            warnings.add(Warning.DeadlineApproaching)
        }

        val overdueTasks = undoneTasks.filter { task ->
            task.deadline?.let { it < now } ?: false
        }

        if (overdueTasks.isNotEmpty()) {
            warnings.add(Warning.Overdue)
        }

        return warnings
    }

    fun getTasksNeedingAttention(
        tasks: List<Task>,
        projects: List<Project>
    ): List<Task> {
        val now = System.currentTimeMillis()

        return tasks.filter { task ->
            !task.isDone && (
                    task.isBlocked ||
                            (task.deadline != null && task.deadline!! - now < TimeUnit.HOURS.toMillis(24)) ||
                            task.movedToNextDay > 2 ||
                            task.urgencyScore >= 75 // New Threshold
                    )
        }.sortedByDescending { it.urgencyScore }
    }

    // ============================================================================================
    // SECTION 6: SCHEDULING & SCHEDULER
    // ============================================================================================

    fun scheduleTasksForWeek(
        tasks: List<Task>,
        subTasks: List<SubTask>,
        user: UserProfile,
        startDate: Calendar = Calendar.getInstance()
    ): Map<Long, List<ScheduledItem>> {
        val schedule = mutableMapOf<Long, MutableList<ScheduledItem>>()

        val sortedTasks = tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }

        val currentDate = startDate.clone() as Calendar
        currentDate.set(Calendar.HOUR_OF_DAY, 0)
        currentDate.set(Calendar.MINUTE, 0)
        currentDate.set(Calendar.SECOND, 0)

        for (dayIndex in 0..6) {
            val dayKey = currentDate.timeInMillis
            schedule[dayKey] = mutableListOf()

            val dayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK)
            if (!user.isWorkDay(dayOfWeek)) {
                currentDate.add(Calendar.DAY_OF_YEAR, 1)
                continue
            }

            var availableMinutes = user.dailyWorkMinutes

            val tasksToSchedule = sortedTasks.toMutableList()
            val iterator = tasksToSchedule.iterator()

            while (iterator.hasNext() && availableMinutes > 0) {
                val task = iterator.next()
                val taskWithSubs = TaskWithSubTasks(
                    task = task,
                    subTasks = subTasks.filter { it.taskId == task.id && !it.isDone }
                )

                val totalMinutes = taskWithSubs.totalEstimatedMinutes

                when {
                    totalMinutes <= availableMinutes -> {
                        schedule[dayKey]!!.add(ScheduledItem.FullTask(taskWithSubs, dayKey))
                        availableMinutes -= totalMinutes
                        iterator.remove()
                    }
                    availableMinutes >= 60 -> {
                        schedule[dayKey]!!.add(
                            ScheduledItem.SplitTask(
                                taskWithSubs,
                                availableMinutes,
                                dayKey,
                                1
                            )
                        )

                        val tomorrow = (currentDate.clone() as Calendar).apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        val tomorrowKey = tomorrow.timeInMillis

                        if (!schedule.containsKey(tomorrowKey)) {
                            schedule[tomorrowKey] = mutableListOf()
                        }

                        schedule[tomorrowKey]!!.add(
                            ScheduledItem.SplitTask(
                                taskWithSubs,
                                totalMinutes - availableMinutes,
                                tomorrowKey,
                                2
                            )
                        )

                        availableMinutes = 0
                        iterator.remove()
                    }
                    else -> {
                        break
                    }
                }
            }
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return schedule
    }

    fun balanceWorkload(
        tasks: List<Task>,
        dailyCapacity: Int,
        days: Int = 7
    ): Map<Int, List<Task>> {
        val undoneTasks = tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }

        val schedule = mutableMapOf<Int, MutableList<Task>>()
        for (day in 0 until days) {
            schedule[day] = mutableListOf()
        }

        var currentDay = 0
        var remainingCapacity = dailyCapacity

        for (task in undoneTasks) {
            if (task.estimatedMinutes <= remainingCapacity) {
                schedule[currentDay]?.add(task)
                remainingCapacity -= task.estimatedMinutes
            } else {
                currentDay++
                if (currentDay >= days) break
                remainingCapacity = dailyCapacity
                if (task.estimatedMinutes <= remainingCapacity) {
                    schedule[currentDay]?.add(task)
                    remainingCapacity -= task.estimatedMinutes
                }
            }
        }
        return schedule
    }

    fun getOptimalTaskOrder(
        tasks: List<Task>,
        availableMinutes: Int
    ): List<Task> {
        val validTasks = tasks.filter { !it.isDone && !it.isBlocked }
        // Simple sort by new normalized score
        return validTasks.sortedByDescending { it.urgencyScore }
    }

    // Legacy helper functions
    fun canCompleteInTime(
        task: Task,
        availableMinutes: Int,
        deadline: Long = task.deadline ?: Long.MAX_VALUE
    ): Boolean {
        val now = System.currentTimeMillis()
        val timeLeft = deadline - now
        if (timeLeft < 0) return false
        val hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeft)
        val canFinishInOneSession = task.estimatedMinutes <= availableMinutes
        if (hoursLeft < 24) {
            return canFinishInOneSession
        }
        return true
    }

    fun getTasksByDeadline(
        tasks: List<Task>,
        withinHours: Long? = null
    ): List<Task> {
        val now = System.currentTimeMillis()
        val cutoff = withinHours?.let { now + TimeUnit.HOURS.toMillis(it) }

        return tasks
            .filter { !it.isDone && !it.isBlocked }
            .filter { task ->
                task.deadline?.let { deadline ->
                    cutoff?.let { deadline <= it } ?: true
                } ?: false
            }
            .sortedBy { it.deadline }
    }

    fun getBlockedTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { it.isBlocked && !it.isDone }
    }

    fun getStartedTasks(tasks: List<Task>): List<Task> {
        return tasks.filter { !it.isDone && it.actualMinutes > 0 }
    }

    fun getOverdueTasks(tasks: List<Task>): List<Task> {
        val now = System.currentTimeMillis()
        return tasks.filter {
            !it.isDone && it.deadline != null && it.deadline!! < now
        }
    }

    fun calculateEfficiency(completedTasks: List<Task>): Float {
        if (completedTasks.isEmpty()) return 1.0f
        val accuracyScores = completedTasks.mapNotNull { task ->
            if (task.estimatedMinutes > 0 && task.actualMinutes > 0) {
                val ratio = task.actualMinutes.toFloat() / task.estimatedMinutes
                1.0f - kotlin.math.abs(1.0f - ratio)
            } else null
        }
        return if (accuracyScores.isNotEmpty()) accuracyScores.average().toFloat() else 1.0f
    }

    fun suggestBreak(workedMinutes: Int, user: UserProfile): Boolean {
        val breakInterval = 90
        return workedMinutes >= breakInterval
    }

    fun getProductivityScore(
        completedTasks: List<Task>,
        totalAvailableMinutes: Int
    ): Int {
        if (totalAvailableMinutes == 0) return 0
        val totalWorkedMinutes = completedTasks.sumOf { it.actualMinutes }
        val efficiency = calculateEfficiency(completedTasks)
        val utilization = (totalWorkedMinutes.toFloat() / totalAvailableMinutes).coerceAtMost(1.0f)
        return (utilization * efficiency * 100).toInt()
    }

    // Legacy support for older calls (defaults to 3 days lookahead)
    fun calculateWorkload(tasks: List<Task>, daysLookahead: Int): Int {
        val calendar = Calendar.getInstance()
        val effectiveLookahead = if (daysLookahead == 0) 3 else daysLookahead
        val now = System.currentTimeMillis()
        val end = now + TimeUnit.DAYS.toMillis(effectiveLookahead.toLong())
        // We use empty subtasks list here as legacy fallback
        return calculateWorkloadForRange(tasks, emptyList(), now, end)
    }

    fun calculateWorkloadForPeriod(tasks: List<Task>, subTasks: List<SubTask>, daysLookahead: Int): Int {
        val now = System.currentTimeMillis()
        val end = now + TimeUnit.DAYS.toMillis(daysLookahead.toLong())
        return calculateWorkloadForRange(tasks, subTasks, now, end)
    }
}