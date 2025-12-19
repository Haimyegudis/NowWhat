package com.nowwhat.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.nowwhat.app.model.UserProfile
import java.util.*

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val allDay: Boolean,
    val isDuringWorkHours: Boolean,
    val calendarId: Long = 0,
    val eventColor: Int? = null,
    val location: String = "",
    val description: String = ""
)

class CalendarRepository(context: Context) {

    // Use Application Context to prevent memory leaks
    private val appContext: Context = context.applicationContext

    companion object {
        private const val TAG = "CalendarRepository"
    }

    /**
     * Get all calendar events for a specific date from all calendars
     */
    fun getEventsForDate(
        date: Calendar,
        user: UserProfile
    ): List<CalendarEvent> {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Calendar permission not granted")
            return emptyList()
        }

        return try {
            getEventsForDateInternal(date, user)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calendar events", e)
            emptyList()
        }
    }

    private fun getEventsForDateInternal(
        date: Calendar,
        user: UserProfile
    ): List<CalendarEvent> {
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = date.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)
        endOfDay.set(Calendar.MILLISECOND, 999)

        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_COLOR,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        // Query all events within the day range
        val selection =
            "((${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?) OR " +
                    "(${CalendarContract.Events.DTEND} >= ? AND ${CalendarContract.Events.DTEND} <= ?) OR " +
                    "(${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DTEND} >= ?))"

        val selectionArgs = arrayOf(
            startOfDay.timeInMillis.toString(),
            endOfDay.timeInMillis.toString(),
            startOfDay.timeInMillis.toString(),
            endOfDay.timeInMillis.toString(),
            startOfDay.timeInMillis.toString(),
            endOfDay.timeInMillis.toString()
        )

        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIndex = it.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val calendarIdIndex = it.getColumnIndex(CalendarContract.Events.CALENDAR_ID)
                val colorIndex = it.getColumnIndex(CalendarContract.Events.EVENT_COLOR)
                val locationIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val descriptionIndex = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)

                while (it.moveToNext()) {
                    try {
                        val id = it.getLong(idIndex)
                        val title = it.getString(titleIndex) ?: "Untitled"
                        val startTime = it.getLong(startIndex)
                        val endTime = it.getLong(endIndex)
                        val allDay = it.getInt(allDayIndex) == 1
                        val calendarId = it.getLong(calendarIdIndex)
                        val eventColor = if (!it.isNull(colorIndex)) it.getInt(colorIndex) else null
                        val location = it.getString(locationIndex) ?: ""
                        val description = it.getString(descriptionIndex) ?: ""

                        val isDuringWorkHours = isEventDuringWorkHours(
                            startTime = startTime,
                            endTime = endTime,
                            user = user
                        )

                        events.add(
                            CalendarEvent(
                                id = id,
                                title = title,
                                startTime = startTime,
                                endTime = endTime,
                                allDay = allDay,
                                isDuringWorkHours = isDuringWorkHours,
                                calendarId = calendarId,
                                eventColor = eventColor,
                                location = location,
                                description = description
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing calendar event", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar", e)
        } finally {
            cursor?.close()
        }

        return events
    }

    /**
     * Calculate available work minutes for today
     */
    fun calculateAvailableMinutesToday(user: UserProfile): Int {
        return try {
            val today = Calendar.getInstance()
            val dayOfWeek = today.get(Calendar.DAY_OF_WEEK)

            // Check if today is a work day
            if (dayOfWeek !in user.workDays) {
                return 0
            }

            // Total work minutes
            val totalWorkMinutes = (user.endWorkHour - user.startWorkHour) * 60

            // Get today's events
            val events = getEventsForDate(today, user)

            // Calculate busy minutes (only events during work hours)
            val busyMinutes = events
                .filter { it.isDuringWorkHours && !it.allDay }
                .sumOf {
                    val duration = (it.endTime - it.startTime) / (1000 * 60)
                    duration.toInt()
                }

            (totalWorkMinutes - busyMinutes).coerceAtLeast(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating available minutes", e)
            user.dailyWorkMinutes // Return default work minutes on error
        }
    }

    /**
     * Check if event overlaps with work hours
     */
    private fun isEventDuringWorkHours(
        startTime: Long,
        endTime: Long,
        user: UserProfile
    ): Boolean {
        return try {
            val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
            val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

            val startHour = startCal.get(Calendar.HOUR_OF_DAY)
            val startMinute = startCal.get(Calendar.MINUTE)
            val endHour = endCal.get(Calendar.HOUR_OF_DAY)
            val endMinute = endCal.get(Calendar.MINUTE)

            val eventStartMinutes = startHour * 60 + startMinute
            val eventEndMinutes = endHour * 60 + endMinute
            val workStartMinutes = user.startWorkHour * 60
            val workEndMinutes = user.endWorkHour * 60

            // Event overlaps with work hours
            (eventStartMinutes < workEndMinutes && eventEndMinutes > workStartMinutes)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking work hours", e)
            false
        }
    }

    /**
     * Get all available calendars (Google, Outlook, Samsung, etc.)
     */
    fun getAvailableCalendars(): List<CalendarInfo> {
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Calendar permission not granted for getAvailableCalendars")
            return emptyList()
        }

        val calendars = mutableListOf<CalendarInfo>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE
        )

        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIndex = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accountTypeIndex = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val colorIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleIndex = it.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                while (it.moveToNext()) {
                    try {
                        val accountType = it.getString(accountTypeIndex) ?: ""

                        calendars.add(
                            CalendarInfo(
                                id = it.getLong(idIndex),
                                displayName = it.getString(nameIndex) ?: "Unknown",
                                accountName = it.getString(accountIndex) ?: "",
                                accountType = accountType,
                                color = it.getInt(colorIndex),
                                isVisible = it.getInt(visibleIndex) == 1
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing calendar info", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available calendars", e)
        } finally {
            cursor?.close()
        }

        return calendars
    }

    /**
     * Get events for a date range
     */
    fun getEventsForDateRange(
        startDate: Calendar,
        endDate: Calendar,
        user: UserProfile
    ): List<CalendarEvent> {
        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Calendar permission not granted for getEventsForDateRange")
            return emptyList()
        }

        val events = mutableListOf<CalendarEvent>()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_COLOR
        )

        val selection =
            "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(
            startDate.timeInMillis.toString(),
            endDate.timeInMillis.toString()
        )

        var cursor: Cursor? = null
        try {
            cursor = appContext.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CalendarContract.Events._ID)
                val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIndex = it.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIndex = it.getColumnIndex(CalendarContract.Events.ALL_DAY)

                while (it.moveToNext()) {
                    try {
                        val startTime = it.getLong(startIndex)
                        val endTime = it.getLong(endIndex)

                        events.add(
                            CalendarEvent(
                                id = it.getLong(idIndex),
                                title = it.getString(titleIndex) ?: "Untitled",
                                startTime = startTime,
                                endTime = endTime,
                                allDay = it.getInt(allDayIndex) == 1,
                                isDuringWorkHours = isEventDuringWorkHours(startTime, endTime, user)
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing event in date range", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting events for date range", e)
        } finally {
            cursor?.close()
        }

        return events
    }
}

/**
 * Calendar information
 */
data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isVisible: Boolean
)