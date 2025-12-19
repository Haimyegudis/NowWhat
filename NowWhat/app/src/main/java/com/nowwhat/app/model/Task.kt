package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.Medium,
    val severity: Severity = Severity.Medium,
    val estimatedMinutes: Int = 60,
    val actualMinutes: Int = 0,
    val deadline: Long? = null,
    val isDone: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val hasBlocker: Boolean = false,
    val blockerDescription: String = "",
    val movedToNextDay: Int = 0,
    val calendarEventId: String? = null
) {
    /**
     * Urgency score (calculated externally)
     */
    @Ignore
    var urgencyScore: Int = 0

    /**
     * Urgency level (calculated externally)
     */
    @Ignore
    var urgency: Urgency = Urgency.Low

    /**
     * Calculate progress percentage
     */
    val progress: Int
        get() = if (isDone) {
            100
        } else if (actualMinutes > 0) {
            ((actualMinutes.toFloat() / estimatedMinutes) * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }

    /**
     * Calculate remaining minutes
     */
    val remainingMinutes: Int
        get() = (estimatedMinutes - actualMinutes).coerceAtLeast(0)

    /**
     * Check if task is overdue
     */
    val isOverdue: Boolean
        get() = deadline != null && deadline!! < System.currentTimeMillis() && !isDone

    /**
     * Check if task is due today
     */
    val isDueToday: Boolean
        get() {
            if (deadline == null) return false
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val tomorrow = today.clone() as java.util.Calendar
            tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1)

            return deadline!! >= today.timeInMillis && deadline!! < tomorrow.timeInMillis
        }

    /**
     * Get days until deadline (can be negative if overdue)
     */
    val daysUntilDeadline: Int?
        get() {
            if (deadline == null) return null
            val diff = deadline!! - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
}