package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
) {
    /**
     * Total number of tasks in this project (set externally by ViewModel)
     */
    @Ignore
    var totalTasks: Int = 0

    /**
     * Number of completed tasks (set externally by ViewModel)
     */
    @Ignore
    var completedTasks: Int = 0

    /**
     * Project risk status (calculated externally by ViewModel)
     */
    @Ignore
    var risk: RiskStatus = RiskStatus.OnTrack

    /**
     * Time needed to complete all tasks in minutes (set externally by ViewModel)
     */
    @Ignore
    var timeNeededMinutes: Int = 0

    /**
     * Calculate project progress based on completed vs total tasks
     */
    val progress: Int
        get() = if (totalTasks > 0) {
            (completedTasks * 100 / totalTasks)
        } else 0

    /**
     * Time available until deadline in minutes (calculated)
     */
    val timeAvailableMinutes: Int
        get() = if (deadline != null) {
            val now = System.currentTimeMillis()
            val minutesLeft = (deadline - now) / (1000 * 60)
            minutesLeft.toInt().coerceAtLeast(0)
        } else Int.MAX_VALUE
}