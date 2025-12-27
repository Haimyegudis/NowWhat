package com.nowwhat.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.nowwhat.app.model.AppLanguage
import com.nowwhat.app.model.Gender
import com.nowwhat.app.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val NAME = stringPreferencesKey("name")
        private val ROLE = stringPreferencesKey("role")
        private val GENDER = stringPreferencesKey("gender")
        private val LANGUAGE = stringPreferencesKey("language")
        private val START_WORK_HOUR = intPreferencesKey("start_work_hour")
        private val END_WORK_HOUR = intPreferencesKey("end_work_hour")
        private val WORK_DAYS = stringPreferencesKey("work_days")
        private val FOCUS_DND_MINUTES = intPreferencesKey("focus_dnd_minutes")
        private val CURRENT_STREAK = intPreferencesKey("current_streak")
        private val LONGEST_STREAK = intPreferencesKey("longest_streak")
        private val CALENDAR_ID = longPreferencesKey("calendar_id")
    }

    val userProfileFlow: Flow<UserProfile?> = context.dataStore.data.map { preferences ->
        val name = preferences[NAME]
        if (name == null) {
            null
        } else {
            UserProfile(
                name = name,
                role = preferences[ROLE] ?: "",
                gender = Gender.valueOf(preferences[GENDER] ?: Gender.NotSpecified.name),
                language = AppLanguage.valueOf(preferences[LANGUAGE] ?: AppLanguage.English.name),
                startWorkHour = preferences[START_WORK_HOUR] ?: 9,
                endWorkHour = preferences[END_WORK_HOUR] ?: 18,
                workDays = preferences[WORK_DAYS]?.split(",")?.map { it.toInt() }?.toSet() ?: setOf(1, 2, 3, 4, 5),
                focusDndMinutes = preferences[FOCUS_DND_MINUTES] ?: 30,
                currentStreak = preferences[CURRENT_STREAK] ?: 0,
                longestStreak = preferences[LONGEST_STREAK] ?: 0,
                calendarId = preferences[CALENDAR_ID] ?: -1L
            )
        }
    }

    suspend fun saveUserProfile(user: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[NAME] = user.name
            preferences[ROLE] = user.role
            preferences[GENDER] = user.gender.name
            preferences[LANGUAGE] = user.language.name
            preferences[START_WORK_HOUR] = user.startWorkHour
            preferences[END_WORK_HOUR] = user.endWorkHour
            preferences[WORK_DAYS] = user.workDays.joinToString(",")
            preferences[FOCUS_DND_MINUTES] = user.focusDndMinutes
            preferences[CURRENT_STREAK] = user.currentStreak
            preferences[LONGEST_STREAK] = user.longestStreak
            preferences[CALENDAR_ID] = user.calendarId
        }
    }

    suspend fun updateStreak(newStreak: Int) {
        context.dataStore.edit { preferences ->
            val currentLongest = preferences[LONGEST_STREAK] ?: 0
            preferences[CURRENT_STREAK] = newStreak
            if (newStreak > currentLongest) {
                preferences[LONGEST_STREAK] = newStreak
            }
        }
    }

    suspend fun resetStreak() {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_STREAK] = 0
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}