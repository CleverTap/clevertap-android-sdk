package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.utils.Clock
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ImpressionManager(
    private val impressionStore: ImpressionStore,
    private val clock: Clock = Clock.SYSTEM,
    private val locale: Locale = Locale.US,
) {

    // TODO add the offset to shorten the timestamps
    // private val DATE_OFFSET = ..

    private var sessionImpressions: MutableMap<String, MutableList<Long>> = mutableMapOf()
    private var sessionImpressionsTotal = 0

    fun setSessionImpressions(sessionImpressions: MutableMap<String, MutableList<Long>>) {
        this.sessionImpressions.clear()
        this.sessionImpressions.putAll(sessionImpressions)
    }

    fun recordImpression(campaignId: String) {
        sessionImpressionsTotal++
        val now = clock.currentTimeSeconds()
        val records = sessionImpressions.getOrPut(campaignId) { mutableListOf() }
        records.add(now)

        impressionStore.write(campaignId, now)
    }

    fun perSession(campaignId: String): Int {
        return sessionImpressions[campaignId]?.size ?: 0
    }

    fun perSessionTotal(): Int {
        return sessionImpressionsTotal
    }

    fun perSecond(campaignId: String, seconds: Int): Int {
        val now = clock.currentTimeSeconds()
        return getImpressionCount(campaignId, now - seconds)
    }

    fun perMinute(campaignId: String, minutes: Int): Int {
        val now = clock.currentTimeSeconds()
        val offset = TimeUnit.MINUTES.toSeconds(minutes.toLong())
        return getImpressionCount(campaignId, now - offset)
    }

    fun perHour(campaignId: String, hours: Int): Int {
        val now = clock.currentTimeSeconds()
        val offset = TimeUnit.HOURS.toSeconds(hours.toLong())
        return getImpressionCount(campaignId, now - offset)
    }

    fun perDay(campaignId: String, days: Int): Int {
        val calendar = Calendar.getInstance(locale).apply { // TODO reuse instance and just set time?
            val currentDate = Date()
            // Set the calendar's time to the current date and time
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Subtract the specified number of days from the current date
        calendar.add(Calendar.DAY_OF_YEAR, -days)

        // Get the resulting date, which represents the start date for the calculation
        val startOfWeek = calendar.time
        val startingDayTimestamp = TimeUnit.MILLISECONDS.toSeconds(startOfWeek.time)

        return getImpressionCount(campaignId, startingDayTimestamp)
    }

    fun perWeek(
        campaignId: String, weeks: Int
    ): Int {
        // start of week is Monday for some countries and Sunday in others
        val calendar = Calendar.getInstance(locale).apply { // TODO reuse instance and just set time?
            val currentDate = Date()
            // Set the calendar's time to the current date and time
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Get the first weekday based on the user's locale
        val firstWeekday = calendar.firstDayOfWeek

        // Calculate the number of days to subtract to reach the starting day of the week
        val daysToSubtract = (calendar.get(Calendar.DAY_OF_WEEK) - firstWeekday + 7) % 7

        // Move back number of days till start of week
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        // Move back the number of weeks
        if (weeks > 1) {
            calendar.add(Calendar.WEEK_OF_YEAR, -weeks)
        }

        val startingDayTimestamp = TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis)
        return getImpressionCount(campaignId, startingDayTimestamp)
    }

    fun getImpressionCount(campaignId: String): Int {
        return impressionStore.read(campaignId).size
    }

    private fun getImpressionCount(campaignId: String, timestampStart: Long): Int {
        val timestamps = impressionStore.read(campaignId)

        var count = 0
        for (i in (0..timestamps.lastIndex).reversed()) {
            if (timestampStart > timestamps[i]) {
                break
            }
            count++
        }
        return count
    }

    fun clearSessionData() {
        sessionImpressions.clear()
        sessionImpressionsTotal = 0
    }
}
