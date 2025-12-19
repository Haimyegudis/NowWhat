package com.nowwhat.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.Task
import com.nowwhat.app.model.SubTask

@Database(
    entities = [
        Project::class,
        Task::class,
        SubTask::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nowwhat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearDatabase(context: Context) {
            INSTANCE?.close()
            context.deleteDatabase("nowwhat_database")
            INSTANCE = null
        }
    }
}