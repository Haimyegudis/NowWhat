package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val deadline: Long? = null,
    val priority: Priority = Priority.Medium,
    val risk: RiskStatus = RiskStatus.OnTrack,
    val progress: Int = 0,
    val isCompleted: Boolean = false,
    val markedComplete: Boolean = false,
    val completedAt: Long? = null,
    val isArchived: Boolean = false,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val timeNeededMinutes: Int = 0
)