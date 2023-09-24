package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.utils.Clock
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ImpressionManager(
  private val impressionStore: ImpressionStore,
  private val clock: Clock = Clock.SYSTEM,
  private val locale: Locale = Locale.US,
) {

  // TODO add the offset to shorten the timestamps
//  private val DATE_OFFSET = ..

  private val sessionImpressions: MutableMap<String, MutableList<Long>> = mutableMapOf()
  private var sessionImpressionsTotal = 0

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

  fun perDay(campaignId: String, days: Int): Int { // TODO maybe the perDay could be changed to use the Calendar functionality
    val now = clock.currentTimeSeconds()
    val offset = TimeUnit.DAYS.toSeconds(days.toLong())
    return getImpressionCount(campaignId, now - offset)
  }

  fun perWeek(campaignId: String, weeks: Int): Int { // start of week is Monday for some countries and Sunday in others
    // TODO create calendar and reset time and move day to the Monday or Sunday
    val calendar = Calendar.getInstance(locale).apply { // TODO reuse instance and just set time?
      timeInMillis = clock.currentTimeMillis()
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    // move back number of days till start of week
    val daysFromStartOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - calendar.firstDayOfWeek
    calendar.add(Calendar.DAY_OF_MONTH, -daysFromStartOfWeek)
    // TODO check if dayOfMonth is getting negative - almost certainly the library is taking care of it

    // move back number of weeks
    if (weeks > 1) {
      calendar.add(Calendar.WEEK_OF_YEAR, -(weeks-1))
    }

    val startingDayTimestamp = TimeUnit.MILLISECONDS.toSeconds(calendar.timeInMillis)
    return getImpressionCount(campaignId, startingDayTimestamp)
  }

  fun getImpressionCount(campaignId: String): Int {
    return impressionStore.read(campaignId).size
  }

  fun getImpressionCount(campaignId: String, timestampStart: Long): Int {
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

  fun getImpressions(campaignId: String): List<Long> {
    TODO("Not yet implemented")
  }

}
