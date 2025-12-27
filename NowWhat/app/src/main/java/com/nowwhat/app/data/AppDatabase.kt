package com.nowwhat.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nowwhat.app.model.Project
import com.nowwhat.app.model.SubTask
import com.nowwhat.app.model.Task

@Database(
    entities = [Project::class, Task::class, SubTask::class],
    version = 4, // ← שנה מ-2 ל-3
    exportSchema = false

)
@TypeConverters(Converters::class)
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
                    .fallbackToDestructiveMigration() // ← זה ימחק את המסד בשינוי גרסה
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}