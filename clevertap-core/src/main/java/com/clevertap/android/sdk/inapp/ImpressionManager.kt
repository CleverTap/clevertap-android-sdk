package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.utils.Clock
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Provides functionality for tracking and managing impressions for various campaigns.
 *
 * @property impressionStore A storage manager responsible for storing and retrieving impression data.
 * @property clock           An optional Clock implementation for handling time-related operations.
 * @property locale          An optional Locale specifying the locale to use for date and time calculations.
 */
class ImpressionManager @JvmOverloads constructor(
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

    /**
     * Records an impression for a campaign.
     *
     * @param campaignId The identifier of the campaign for which the impression is recorded.
     */
    fun recordImpression(campaignId: String) {
        sessionImpressionsTotal++
        val now = clock.currentTimeSeconds()
        val records = sessionImpressions.getOrPut(campaignId) { mutableListOf() }
        records.add(now)

        impressionStore.write(campaignId, now)
    }

    /**
     * Counts the impressions for a specific campaign in the current session.
     *
     * @param campaignId The identifier of the campaign.
     * @return The count of impressions recorded in the current session.
     */
    fun perSession(campaignId: String): Int {
        return sessionImpressions[campaignId]?.size ?: 0
    }

    /**
     * Retrieves the total count of impressions recorded in the current session.
     *
     * @return The total count of impressions recorded in the current session.
     */
    fun perSessionTotal(): Int {
        return sessionImpressionsTotal
    }

    /**
     * Counts the impressions for a campaign within the last N seconds.
     *
     * @param campaignId The identifier of the campaign.
     * @param seconds    The time interval in seconds.
     * @return The count of impressions within the specified time interval.
     */
    fun perSecond(campaignId: String, seconds: Int): Int {
        val now = clock.currentTimeSeconds()
        return getImpressionCount(campaignId, now - seconds)
    }

    /**
     * Counts the impressions for a campaign within the last N minutes.
     *
     * @param campaignId The identifier of the campaign.
     * @param minutes    The time interval in minutes.
     * @return The count of impressions within the specified time interval.
     */
    fun perMinute(campaignId: String, minutes: Int): Int {
        val now = clock.currentTimeSeconds()
        val offset = TimeUnit.MINUTES.toSeconds(minutes.toLong())
        return getImpressionCount(campaignId, now - offset)
    }

    /**
     * Counts the impressions for a campaign within the last N hours.
     *
     * @param campaignId The identifier of the campaign.
     * @param hours      The time interval in hours.
     * @return The count of impressions within the specified time interval.
     */
    fun perHour(campaignId: String, hours: Int): Int {
        val now = clock.currentTimeSeconds()
        val offset = TimeUnit.HOURS.toSeconds(hours.toLong())
        return getImpressionCount(campaignId, now - offset)
    }

    /**
     * Counts the impressions for a campaign within the last N days.
     *
     * @param campaignId The identifier of the campaign.
     * @param days       The time interval in days.
     * @return The count of impressions within the specified time interval.
     */
    fun perDay(campaignId: String, days: Int): Int {
        val calendar =
            Calendar.getInstance(locale).apply { // TODO reuse instance and just set time?
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

    /**
     * Counts the impressions for a campaign within the last N weeks.
     *
     * @param campaignId The identifier of the campaign.
     * @param weeks      The time interval in weeks.
     * @return The count of impressions within the specified time interval.
     */
    fun perWeek(
        campaignId: String, weeks: Int
    ): Int {
        // start of week is Monday for some countries and Sunday in others
        val calendar =
            Calendar.getInstance(locale).apply { // TODO reuse instance and just set time?
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

    /**
     * Retrieves the total count of impressions for a campaign.
     *
     * @param campaignId The identifier of the campaign.
     * @return The total number of impressions recorded for the campaign.
     */
    fun getImpressionCount(campaignId: String): Int {
        return impressionStore.read(campaignId).size
    }

    /**
     * Retrieves the count of impressions for a campaign within a specific time interval.
     *
     * @param campaignId      The identifier of the campaign.
     * @param timestampStart  The start timestamp of the time interval (in seconds since the Unix epoch).
     * @return The count of impressions within the specified time interval.
     */
    fun getImpressionCount(campaignId: String, timestampStart: Long): Int {
        val timestamps = getImpressions(campaignId)

        var count = 0
        for (i in (0..timestamps.lastIndex).reversed()) {
            if (timestampStart > timestamps[i]) {
                break
            }
            count++
        }
        return count
    }

    fun getImpressions(campaignId: String): List<Long> {
        return impressionStore.read(campaignId)
    }

    /**
     * Clears session-specific impression data, resetting counts and data.
     */
    fun clearSessionData() {
        sessionImpressions.clear()
        sessionImpressionsTotal = 0
    }

}
