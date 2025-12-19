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
    val priority: Priority = Priority.Medium,
    val severity: Severity = Severity.Medium,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val markedComplete: Boolean = false
) {
    @Ignore
    var totalTasks: Int = 0

    @Ignore
    var completedTasks: Int = 0

    @Ignore
    var risk: RiskStatus = RiskStatus.OnTrack

    @Ignore
    var timeNeededMinutes: Int = 0

    val progress: Int
        get() = if (totalTasks > 0) {
            (completedTasks * 100 / totalTasks)
        } else 0

    val timeAvailableMinutes: Int
        get() = if (deadline != null) {
            val now = System.currentTimeMillis()
            val minutesLeft = (deadline - now) / (1000 * 60)
            minutesLeft.toInt().coerceAtLeast(0)
        } else Int.MAX_VALUE
}