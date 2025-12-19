package com.nowwhat.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
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

class CalendarRepository(private val context: Context) {

    /**
     * Get all calendar events for a specific date
     */
    fun getEventsForDate(
        date: Calendar,
        user: UserProfile
    ): List<CalendarEvent> {
        // Check permission
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

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
            CalendarContract.Events.DESCRIPTION
        )

        val selection =
            "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(
            startOfDay.timeInMillis.toString(),
            endOfDay.timeInMillis.toString()
        )

        val cursor: Cursor? = context.contentResolver.query(
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
            }
        }

        return events
    }

    /**
     * Calculate available work minutes for today
     */
    fun calculateAvailableMinutesToday(user: UserProfile): Int {
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

        return (totalWorkMinutes - busyMinutes).coerceAtLeast(0)
    }

    /**
     * Check if event overlaps with work hours
     */
    private fun isEventDuringWorkHours(
        startTime: Long,
        endTime: Long,
        user: UserProfile
    ): Boolean {
        val startCal = Calendar.getInstance().apply { timeInMillis = startTime }
        val endCal = Calendar.getInstance().apply { timeInMillis = endTime }

        val startHour = startCal.get(Calendar.HOUR_OF_DAY)
        val endHour = endCal.get(Calendar.HOUR_OF_DAY)

        // Event overlaps with work hours
        return (startHour < user.endWorkHour && endHour > user.startWorkHour)
    }

    /**
     * Get all available calendars
     */
    fun getAvailableCalendars(): List<CalendarInfo> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val calendars = mutableListOf<CalendarInfo>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE
        )

        val cursor: Cursor? = context.contentResolver.query(
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
            val colorIndex = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
            val visibleIndex = it.getColumnIndex(CalendarContract.Calendars.VISIBLE)

            while (it.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = it.getLong(idIndex),
                        displayName = it.getString(nameIndex) ?: "Unknown",
                        accountName = it.getString(accountIndex) ?: "",
                        color = it.getInt(colorIndex),
                        isVisible = it.getInt(visibleIndex) == 1
                    )
                )
            }
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
                context,
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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

        val cursor: Cursor? = context.contentResolver.query(
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
            }
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
    val color: Int,
    val isVisible: Boolean
)
