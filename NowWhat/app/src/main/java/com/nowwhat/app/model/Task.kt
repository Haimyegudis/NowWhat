package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(
    tableName = "tasks",
    indices = [Index("projectId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val projectId: Int? = null,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.Medium,
    val severity: Severity = Severity.Medium,
    val estimatedMinutes: Int = 60,
    val actualMinutes: Int = 0,
    val deadline: Long? = null,
    val isDone: Boolean = false,
    val isArchived: Boolean = false, // Added field
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val hasBlocker: Boolean = false,
    val blockerDescription: String = "",
    val movedToNextDay: Int = 0,
    val calendarEventId: String? = null,
    val waitingFor: String? = null,
    val reminderTime: Long? = null,
    val scheduledDate: Long? = null,
    val splitFromTaskId: Int? = null,
    val splitPartIndex: Int = 0,
    val originalEstimatedMinutes: Int? = null
) {
    @Ignore
    var urgencyScore: Int = 0

    @Ignore
    var urgency: Urgency = Urgency.Low

    @Ignore
    var urgencyReason: String = ""

    val isBlocked: Boolean
        get() = hasBlocker || !waitingFor.isNullOrBlank()

    val progress: Int
        get() = if (isDone) {
            100
        } else if (actualMinutes > 0) {
            ((actualMinutes.toFloat() / estimatedMinutes) * 100).toInt().coerceIn(0, 99)
        } else {
            0
        }

    val remainingMinutes: Int
        get() = (estimatedMinutes - actualMinutes).coerceAtLeast(0)

    val isOverdue: Boolean
        get() = deadline != null && deadline!! < System.currentTimeMillis() && !isDone

    val isDueToday: Boolean
        get() {
            if (deadline == null) return false
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val tomorrow = today.clone() as Calendar
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)

            return deadline!! >= today.timeInMillis && deadline!! < tomorrow.timeInMillis
        }

    val daysUntilDeadline: Int?
        get() {
            if (deadline == null) return null
            val diff = deadline!! - System.currentTimeMillis()
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
}