package com.nowwhat.app.data

import androidx.room.*
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.SubTask
import com.nowwhat.app.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // ========== Projects ==========
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("SELECT * FROM projects WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun getActiveProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedProjects(): Flow<List<Project>>

    // ========== Tasks ==========
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY deadline ASC")
    fun getTasksByProject(projectId: Int): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE isDone = 0 ORDER BY deadline ASC")
    fun getIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDone = 0 AND hasBlocker = 0 ORDER BY deadline ASC")
    fun getActiveTasksWithoutBlockers(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline IS NOT NULL AND deadline < :timestamp AND isDone = 0")
    fun getOverdueTasks(timestamp: Long = System.currentTimeMillis()): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline >= :startOfDay AND deadline < :endOfDay AND isDone = 0")
    fun getTasksDueToday(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    // ========== SubTasks ==========
    @Query("SELECT * FROM subtasks ORDER BY createdAt DESC")
    fun getAllSubTasks(): Flow<List<SubTask>>

    @Query("SELECT * FROM subtasks WHERE id = :subTaskId")
    suspend fun getSubTaskById(subTaskId: Int): SubTask?

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY createdAt ASC")
    fun getSubTasksByTask(taskId: Int): Flow<List<SubTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTask): Long

    @Update
    suspend fun updateSubTask(subTask: SubTask)

    @Delete
    suspend fun deleteSubTask(subTask: SubTask)

    @Query("SELECT * FROM subtasks WHERE isDone = 0 AND taskId = :taskId")
    fun getIncompleteSubTasksByTask(taskId: Int): Flow<List<SubTask>>

    @Query("SELECT * FROM subtasks WHERE isDone = 1 AND taskId = :taskId")
    fun getCompletedSubTasksByTask(taskId: Int): Flow<List<SubTask>>

    // ========== Aggregations ==========
    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId")
    suspend fun getTaskCountForProject(projectId: Int): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE projectId = :projectId AND isDone = 1")
    suspend fun getCompletedTaskCountForProject(projectId: Int): Int

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId")
    suspend fun getSubTaskCountForTask(taskId: Int): Int

    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND isDone = 1")
    suspend fun getCompletedSubTaskCountForTask(taskId: Int): Int

    // ========== Batch Operations ==========
    @Query("DELETE FROM tasks WHERE projectId = :projectId")
    suspend fun deleteAllTasksForProject(projectId: Int)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteAllSubTasksForTask(taskId: Int)

    @Query("UPDATE tasks SET isDone = 1, completedAt = :timestamp WHERE projectId = :projectId")
    suspend fun markAllTasksAsComplete(projectId: Int, timestamp: Long)

    @Query("UPDATE subtasks SET isDone = 1, completedAt = :timestamp WHERE taskId = :taskId")
    suspend fun markAllSubTasksAsComplete(taskId: Int, timestamp: Long)

    // ========== Search ==========
    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchProjects(query: String): Flow<List<Project>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<Task>>

    // ========== Statistics ==========
    @Query("SELECT COUNT(*) FROM tasks WHERE isDone = 1 AND completedAt >= :startTime AND completedAt < :endTime")
    suspend fun getTasksCompletedInRange(startTime: Long, endTime: Long): Int

    @Query("SELECT SUM(actualMinutes) FROM tasks WHERE isDone = 1 AND completedAt >= :startTime AND completedAt < :endTime")
    suspend fun getTotalMinutesWorkedInRange(startTime: Long, endTime: Long): Int?

    @Query("SELECT AVG(actualMinutes * 1.0 / estimatedMinutes) FROM tasks WHERE isDone = 1 AND estimatedMinutes > 0 AND actualMinutes > 0")
    suspend fun getAverageEstimationAccuracy(): Float?
}