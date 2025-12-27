// NowWhat/app/src/main/java/com/nowwhat/app/data/CalendarRepository.kt
package com.nowwhat.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import com.nowwhat.app.model.UserProfile
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val ownerName: String,
    val color: Int,
    val isVisible: Boolean
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val color: Int,
    val allDay: Boolean,
    val calendarId: Long
) {
    val isDuringWorkHours: Boolean
        get() {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startTime
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            return hour in 9..17 // Default fallback
        }
}

class CalendarRepository(private val context: Context) {

    fun getAvailableCalendars(): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val ownerIdx = cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                while (cursor.moveToNext()) {
                    calendars.add(
                        CalendarInfo(
                            id = cursor.getLong(idIdx),
                            displayName = cursor.getString(nameIdx) ?: "Unknown",
                            accountName = cursor.getString(accountIdx) ?: "",
                            ownerName = cursor.getString(ownerIdx) ?: "",
                            color = cursor.getInt(colorIdx),
                            isVisible = cursor.getInt(visibleIdx) == 1
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return calendars
    }

    fun getEventsForDate(date: Calendar, user: UserProfile): List<CalendarEvent> {
        // reuse the logic for a specific range of 1 day
        val startOfDay = date.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDay = date.clone() as Calendar
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        endOfDay.set(Calendar.SECOND, 59)

        return getEventsForRange(startOfDay.timeInMillis, endOfDay.timeInMillis, user)
    }

    // Helper to fetch events for any range
    private fun getEventsForRange(startMillis: Long, endMillis: Long, user: UserProfile): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val calendarSelection = if (user.calendarId != -1L) "${CalendarContract.Instances.CALENDAR_ID} = ?" else null
        val selectionArgs = if (user.calendarId != -1L) arrayOf(user.calendarId.toString()) else null

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_ID
        )

        try {
            context.contentResolver.query(
                builder.build(),
                projection,
                calendarSelection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.DISPLAY_COLOR)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)

                while (cursor.moveToNext()) {
                    events.add(
                        CalendarEvent(
                            id = cursor.getLong(idIdx),
                            title = cursor.getString(titleIdx) ?: "No Title",
                            description = cursor.getString(descIdx),
                            startTime = cursor.getLong(beginIdx),
                            endTime = cursor.getLong(endIdx),
                            location = cursor.getString(locIdx),
                            color = cursor.getInt(colorIdx),
                            allDay = cursor.getInt(allDayIdx) == 1,
                            calendarId = cursor.getLong(calIdIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return events
    }

    fun calculateAvailableMinutesToday(user: UserProfile): Int {
        val today = Calendar.getInstance()
        return calculateNetWorkMinutesForDay(today, user)
    }

    /**
     * פונקציה חדשה הסורקת טווח תאריכים ומחשבת קיבולת אמיתית (Capacity)
     * בהתחשב בימי עבודה, שעות עבודה, ואירועים ביומן החופפים לשעות אלו.
     */
    fun calculateAvailableMinutesForRange(startMillis: Long, endMillis: Long, user: UserProfile): Int {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        // איפוס לשעה 00:00 כדי להתחיל ספירה מלאה של ימים מההתחלה
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        var totalAvailableMinutes = 0
        val tempCal = startCal.clone() as Calendar

        // לולאה על כל הימים בטווח
        while (tempCal.before(endCal) || isSameDay(tempCal, endCal)) {
            // אם היום בטווח הוא היום הנוכחי (האמיתי), אנו רוצים לחשב רק מהשעה הנוכחית והלאה?
            // או שזה Capacity כללי? בדרך כלל Capacity הוא פוטנציאל ליום שלם,
            // אבל ליתר דיוק נחשב יום שלם לפי הכללים.

            // חישוב ליום ספציפי זה
            val minutesForDay = calculateNetWorkMinutesForDay(tempCal, user)
            totalAvailableMinutes += minutesForDay

            // מעבר ליום הבא
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return totalAvailableMinutes
    }

    private fun calculateNetWorkMinutesForDay(date: Calendar, user: UserProfile): Int {
        val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)

        // 1. בדיקה אם זה יום עבודה
        if (!user.isWorkDay(dayOfWeek)) {
            return 0
        }

        // 2. הגדרת גבולות יום העבודה
        val workStart = date.clone() as Calendar
        workStart.set(Calendar.HOUR_OF_DAY, user.startWorkHour)
        workStart.set(Calendar.MINUTE, 0)
        workStart.set(Calendar.SECOND, 0)

        val workEnd = date.clone() as Calendar
        workEnd.set(Calendar.HOUR_OF_DAY, user.endWorkHour)
        workEnd.set(Calendar.MINUTE, 0)
        workEnd.set(Calendar.SECOND, 0)

        // סה"כ דקות עבודה תיאורטיות
        val totalWorkMinutes = (user.endWorkHour - user.startWorkHour) * 60
        if (totalWorkMinutes <= 0) return 0

        // 3. שליפת אירועים ליום זה
        val events = getEventsForDate(date, user)
        var busyMinutes = 0

        events.forEach { event ->
            if (!event.allDay) {
                val eventStart = Calendar.getInstance().apply { timeInMillis = event.startTime }
                val eventEnd = Calendar.getInstance().apply { timeInMillis = event.endTime }

                // 4. בדיקת חפיפה: האם האירוע נופל בתוך שעות העבודה?
                // התנאי: האירוע מתחיל לפני שיום העבודה נגמר, ונגמר אחרי שיום העבודה התחיל
                if (eventStart.before(workEnd) && eventEnd.after(workStart)) {

                    // "חיתוך" האירוע לגבולות יום העבודה
                    // אם האירוע התחיל ב-8 (לפני העבודה), נחשיב אותו מ-9 (תחילת העבודה)
                    val actualStart = if (eventStart.before(workStart)) workStart else eventStart
                    // אם האירוע נגמר ב-18 (אחרי העבודה), נחשיב אותו עד 16 (סוף העבודה)
                    val actualEnd = if (eventEnd.after(workEnd)) workEnd else eventEnd

                    val diff = actualEnd.timeInMillis - actualStart.timeInMillis
                    if (diff > 0) {
                        busyMinutes += TimeUnit.MILLISECONDS.toMinutes(diff).toInt()
                    }
                }
            }
        }

        return (totalWorkMinutes - busyMinutes).coerceAtLeast(0)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}