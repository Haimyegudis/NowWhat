// קובץ: app/src/main/java/com/nowwhat/app/model/SubTask.kt
package com.nowwhat.app.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class SubTask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val taskId: Int,
    val title: String,
    val isDone: Boolean = false,
    // שינוי: ברירת מחדל 0 דקות
    val estimatedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // שינוי: ברירת מחדל ללא דד-ליין
    val deadline: Long? = null
)