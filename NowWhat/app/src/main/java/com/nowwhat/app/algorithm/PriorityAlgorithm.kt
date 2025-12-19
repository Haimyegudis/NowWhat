package com.nowwhat.app.algorithm

import com.nowwhat.app.model.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object PriorityAlgorithm {

    fun calculateUrgency(task: Task, availableMinutesToday: Int): Int {
        val now = System.currentTimeMillis()
        val deadline = task.deadline ?: (now + TimeUnit.DAYS.toMillis(365))
        val timeLeft = deadline - now

        val hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeft)
        val daysLeft = TimeUnit.MILLISECONDS.toDays(timeLeft)

        // Base scores from priority and severity
        val priorityScore = when (task.priority) {
            Priority.Critical -> 200
            Priority.High -> 150
            Priority.Medium -> 100
            Priority.Low -> 50
        }

        val severityScore = when (task.severity) {
            Severity.Critical -> 150
            Severity.High -> 100
            Severity.Medium -> 50
            Severity.Low -> 25
        }

        // Dynamic weighting based on deadline proximity
        val (priorityWeight, severityWeight) = when {
            timeLeft < 0 -> 0.95 to 0.05  // Overdue: priority dominates
            hoursLeft < 24 -> 0.85 to 0.15  // Due today
            daysLeft < 7 -> 0.60 to 0.40  // This week
            daysLeft < 30 -> 0.25 to 0.75  // This month
            else -> 0.15 to 0.85  // Far future: severity dominates
        }

        var score = (priorityScore * priorityWeight + severityScore * severityWeight).toInt()

        // Time-based boosters
        when {
            timeLeft < 0 -> score += 300  // Overdue
            hoursLeft < 3 -> score += 250  // Less than 3 hours
            hoursLeft < 6 -> score += 200  // Less than 6 hours
            hoursLeft < 24 -> score += 120  // Due today
            daysLeft < 2 -> score += 80  // Due tomorrow
        }

        // Capacity check
        val fitsInDay = task.estimatedMinutes <= availableMinutesToday
        val fitsWithOvertime = task.estimatedMinutes <= (availableMinutesToday * 1.5).toInt()

        when {
            fitsInDay -> score += 60
            fitsWithOvertime -> score += 30
            else -> score -= 40  // Too big for today
        }

        // Momentum bonus for started tasks
        if (task.actualMinutes > 0) {
            val progressRatio = task.actualMinutes.toFloat() / task.estimatedMinutes.coerceAtLeast(1)
            score += (progressRatio * 80).toInt()
        }

        // Blocker penalty
        if (task.isBlocked) {
            score -= if (task.priority == Priority.Critical || task.priority == Priority.High) 50 else 150
        }

        // Procrastination penalty
        score += task.movedToNextDay * 20

        return score.coerceAtLeast(0)
    }

    fun getUrgencyLevel(score: Int): Urgency {
        return when {
            score >= 500 -> Urgency.Critical
            score >= 350 -> Urgency.VeryHigh
            score >= 200 -> Urgency.High
            score >= 100 -> Urgency.Medium
            else -> Urgency.Low
        }
    }

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

    fun getTasksForToday(
        tasks: List<Task>,
        availableMinutes: Int,
        user: UserProfile
    ): List<Task> {
        val undoneTasks = tasks
            .filter { !it.isDone && !it.isBlocked }
            .sortedByDescending { it.urgencyScore }

        val selectedTasks = mutableListOf<Task>()
        var remainingMinutes = availableMinutes

        for (task in undoneTasks) {
            if (task.estimatedMinutes <= remainingMinutes) {
                selectedTasks.add(task)
                remainingMinutes -= task.estimatedMinutes
            }

            if (selectedTasks.size >= 10) break
        }

        return selectedTasks
    }

    fun shouldWorkOnTask(
        task: Task,
        availableMinutes: Int,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        if (task.isDone || task.isBlocked) return false

        val deadline = task.deadline ?: return task.urgencyScore >= 100
        val timeLeft = deadline - currentTime

        if (timeLeft < 0) return true

        val hoursLeft = TimeUnit.MILLISECONDS.toHours(timeLeft)

        if (hoursLeft < 24 && task.estimatedMinutes <= availableMinutes) {
            return true
        }

        return task.urgencyScore >= 200
    }

    fun calculateProjectRisk(
        project: Project,
        tasks: List<Task>,
        user: UserProfile
    ): RiskStatus {
        if (project.deadline == null) return RiskStatus.OnTrack

        val now = System.currentTimeMillis()
        val timeLeft = project.deadline - now

        if (timeLeft < 0) return RiskStatus.Critical

        val undoneTasks = tasks.filter { !it.isDone }
        val totalTimeNeeded = undoneTasks.sumOf { it.estimatedMinutes }

        val daysLeft = TimeUnit.MILLISECONDS.toDays(timeLeft)
        val workDaysLeft = (daysLeft * 5 / 7).coerceAtLeast(1)
        val availableMinutes = workDaysLeft * user.dailyWorkMinutes

        val utilizationRatio = totalTimeNeeded.toFloat() / availableMinutes.coerceAtLeast(1)

        return when {
            utilizationRatio > 1.2 -> RiskStatus.Critical
            utilizationRatio > 0.85 -> RiskStatus.AtRisk
            else -> RiskStatus.OnTrack
        }
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

    fun estimateCompletionDate(
        tasks: List<Task>,
        availableMinutesPerDay: Int
    ): Long {
        val undoneTasks = tasks.filter { !it.isDone && !it.isBlocked }
        val totalMinutes = undoneTasks.sumOf { it.estimatedMinutes }
        val daysNeeded = (totalMinutes.toFloat() / availableMinutesPerDay.coerceAtLeast(1)).toInt()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysNeeded)

        return calendar.timeInMillis
    }

    fun getOptimalTaskOrder(
        tasks: List<Task>,
        availableMinutes: Int
    ): List<Task> {
        val validTasks = tasks.filter { !it.isDone && !it.isBlocked }

        val criticalTasks = validTasks
            .filter { it.urgency == Urgency.Critical || it.urgency == Urgency.VeryHigh }
            .sortedByDescending { it.urgencyScore }

        val urgentTasks = validTasks
            .filter { it.urgency == Urgency.High }
            .sortedByDescending { it.urgencyScore }

        val normalTasks = validTasks
            .filter { it.urgency == Urgency.Medium || it.urgency == Urgency.Low }
            .sortedByDescending { it.urgencyScore }

        return criticalTasks + urgentTasks + normalTasks
    }

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

    fun calculateEfficiency(
        completedTasks: List<Task>
    ): Float {
        if (completedTasks.isEmpty()) return 1.0f

        val accuracyScores = completedTasks.mapNotNull { task ->
            if (task.estimatedMinutes > 0 && task.actualMinutes > 0) {
                val ratio = task.actualMinutes.toFloat() / task.estimatedMinutes
                1.0f - kotlin.math.abs(1.0f - ratio)
            } else null
        }

        return if (accuracyScores.isNotEmpty()) {
            accuracyScores.average().toFloat()
        } else 1.0f
    }

    fun suggestBreak(
        workedMinutes: Int,
        user: UserProfile
    ): Boolean {
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

    fun recommendFocusTask(
        tasks: List<Task>,
        availableMinutes: Int
    ): Task? {
        val validTasks = tasks.filter {
            !it.isDone &&
                    !it.isBlocked &&
                    it.estimatedMinutes <= availableMinutes
        }

        return validTasks.maxByOrNull { it.urgencyScore }
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
                            task.urgencyScore >= 400
                    )
        }.sortedByDescending { it.urgencyScore }
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
}