package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val taskId: Int,

    val title: String,

    // Time
    val estimatedHours: Float = 0.5f,

    // Priorities
    val priority: Priority = Priority.Low,
    val severity: Severity = Severity.Low,

    // Dates
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,

    // Status
    val isDone: Boolean = false
) {
    val estimatedMinutes: Int
        get() = (estimatedHours * 60).toInt()
}
