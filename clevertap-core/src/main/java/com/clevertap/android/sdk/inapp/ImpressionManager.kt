package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.utils.Clock
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Provides functionality for tracking and managing impressions for various campaigns.
 *
 * The same implementation backs both in-app and Native Display; [impressionStoreProvider] resolves
 * which persistent [ImpressionStore] to use (in-app vs ND). It is a provider (not a direct instance)
 * because the underlying store is created lazily once the device id is available.
 *
 * @property impressionStoreProvider Resolves the [ImpressionStore] to persist/read timestamps.
 * @property clock           An optional Clock implementation for handling time-related operations.
 * @property locale          An optional Locale specifying the locale to use for date and time calculations.
 */
internal class ImpressionManager @JvmOverloads constructor(
    private val impressionStoreProvider: () -> ImpressionStore?,
    private val clock: Clock = Clock.SYSTEM,
    private val locale: Locale = Locale.getDefault(),
) {

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

        impressionStoreProvider()?.write(campaignId, now)
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
     * Counts the impressions for a campaign over the last [days] calendar days, today included.
     *
     * The window always starts at a local midnight. `days = 1` means "today only", so the count
     * resets at the next midnight. `days = 2` means "yesterday and today", and so on. This is the
     * same rule the iOS SDK uses (`CTImpressionManager perDay:days:`) and the same rule the
     * dashboard means by "up to N times in D days".
     *
     * Example: now is 24 Sep 10:00 and one impression was recorded on 23 Sep 15:00.
     * - `days = 1` -> window starts 24 Sep 00:00 -> count is 0
     * - `days = 2` -> window starts 23 Sep 00:00 -> count is 1
     *
     * A [days] value below 1 is not a real window. It is treated as 1 (today only), so a broken
     * payload such as `frequency = 0` can never empty the window and let a capped campaign through.
     *
     * @param campaignId The identifier of the campaign.
     * @param days       The number of calendar days in the window, counting today as the first.
     * @return The count of impressions within the window.
     */
    fun perDay(campaignId: String, days: Int): Int {
        // Guard: a window below 1 day (e.g. frequency = 0 in a bad payload) is treated as "today only".
        val windowDays = days.coerceAtLeast(1)

        val calendar =
            Calendar.getInstance(locale).apply {
                // Set the calendar to "now" via the injected Clock (SYSTEM in prod; testable via FakeClock)
                time = clock.newDate()
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        // Today is the first day of the window, so step back one day fewer than the window size.
        calendar.add(Calendar.DAY_OF_YEAR, -(windowDays - 1))

        val startingDayTimestamp = TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis)
        return getImpressionCount(campaignId, startingDayTimestamp)
    }

    /**
     * Counts the impressions for a campaign over the last [weeks] calendar weeks, this week included.
     *
     * The window always starts at the first day of a week (Monday or Sunday depending on [locale]).
     * `weeks = 1` means "this week only", `weeks = 2` means "last week and this week", and so on.
     * This is the same rule the iOS SDK uses (`CTImpressionManager perWeek:weeks:`).
     *
     * Example: today is Wednesday 23 Sep (week starts Monday 21 Sep), one impression on Friday 11 Sep.
     * - `weeks = 1` -> window starts Mon 21 Sep -> count is 0
     * - `weeks = 2` -> window starts Mon 14 Sep -> count is 0
     * - `weeks = 3` -> window starts Mon 7 Sep  -> count is 1
     *
     * A [weeks] value below 2 (including 0 or negative) keeps the window at "this week only".
     *
     * @param campaignId The identifier of the campaign.
     * @param weeks      The number of calendar weeks in the window, counting this week as the first.
     * @return The count of impressions within the window.
     */
    fun perWeek(
        campaignId: String, weeks: Int
    ): Int {
        // start of week is Monday for some countries and Sunday in others
        val calendar =
            Calendar.getInstance(locale).apply {
                val currentDate = clock.newDate()
                // Set the calendar to "now" via the injected Clock (SYSTEM in prod; testable via FakeClock)
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

        // This week is the first week of the window, so step back one week fewer than the window size.
        if (weeks > 1) {
            calendar.add(Calendar.WEEK_OF_YEAR, -(weeks - 1))
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
    private fun getImpressionCount(campaignId: String): Int {
        return impressionStoreProvider()?.read(campaignId)?.size ?: 0
    }

    /**
     * Retrieves the count of impressions for a campaign within a specific time interval.
     *
     * @param campaignId      The identifier of the campaign.
     * @param timestampStart  The start timestamp of the time interval (in seconds since the Unix epoch).
     * @return The count of impressions within the specified time interval.
     */
    internal fun getImpressionCount(campaignId: String, timestampStart: Long): Int {

        val timestamps = getImpressions(campaignId)

        var low = 0
        var high = timestamps.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = timestamps[mid]

            if (midVal < timestampStart) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return timestamps.size - low
    }

    fun getImpressions(campaignId: String): List<Long> {
        return impressionStoreProvider()?.read(campaignId) ?: emptyList()
    }

    /**
     * Clears session-specific impression data, resetting counts and data.
     */
    fun clearSessionData() {
        sessionImpressions.clear()
        sessionImpressionsTotal = 0
    }

}
