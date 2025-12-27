package com.nowwhat.app.model

data class UserProfile(
    val name: String,
    val role: String,
    val gender: Gender,
    val language: AppLanguage,
    val startWorkHour: Int,
    val endWorkHour: Int,
    val workDays: Set<Int>,
    val focusDndMinutes: Int = 30,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,

    // Focus Mode settings
    val focusModeDndDuration: Int = 30, // Minutes for DND
    val breakReminder: Boolean = true,  // Show break reminders

    // Calendar Sync
    val calendarId: Long = -1L, // -1 means All Calendars / None selected specific

    // Statistics
    val streak: Int = 0  // Consecutive days of completed tasks
) {
    // How many hours of work per day
    val dailyWorkHours: Int
        get() = endWorkHour - startWorkHour

    // How many minutes of work per day
    val dailyWorkMinutes: Int
        get() = dailyWorkHours * 60

    // Check if given day is a work day
    fun isWorkDay(dayOfWeek: Int): Boolean {
        return workDays.contains(dayOfWeek)
    }
}

enum class Gender {
    Male,
    Female,
    NotSpecified
}

enum class AppLanguage {
    English,
    Hebrew,
    Russian
}