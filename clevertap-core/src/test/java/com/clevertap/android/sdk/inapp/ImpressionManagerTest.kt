package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.mockito.*
import org.mockito.Mockito.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class ImpressionManagerTest : BaseTestCase() {

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var deviceInfo: DeviceInfo

    private lateinit var impressionStore: ImpressionStore

    private lateinit var impressionManager: ImpressionManager

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        val storeRegistry = StoreRegistry()

        impressionManager = ImpressionManager(
            storeRegistry = storeRegistry,
            clock = clock,
            locale = Locale.US
        )

        `when`(deviceInfo.deviceID).thenReturn("device_id")

        impressionStore = StoreProvider.getInstance().provideImpressionStore(
            appCtx, deviceInfo,
            "account_id"
        )

        storeRegistry.impressionStore = impressionStore
    }

    class FakeClock : Clock {

        override fun currentTimeMillis(): Long {
            val seconds = 1000L
            return seconds * 1000
        }

        override fun newDate(): Date {
            val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return dateFormatter.parse("20230126")!!// January 26, 2023
        }
    }

    @Test
    fun `recordImpression should increase sessionImpressionsTotal`() {
        // Arrange
        val campaignId = "campaign123"

        assertEquals(0, impressionManager.perSessionTotal())

        // Act
        impressionManager.recordImpression(campaignId)

        // Assert
        assertEquals(1, impressionManager.perSessionTotal())
    }

    @Test
    fun `perSession should return correct impression count when campaignId is present`() {
        // Arrange
        val campaignId = "campaign123"

        // Record impressions for the campaign in the session
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)

        // Act
        val result = impressionManager.perSession(campaignId)

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun `perSession should return 0 when campaignId is missing`() {
        // Arrange
        val campaignId = "campaign123"

        // Act
        val result = impressionManager.perSession(campaignId)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun `perSessionTotal should return 0 with initial value`() {
        // Act
        val result = impressionManager.perSessionTotal()

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun `perSessionTotal should return correct count after recording impressions`() {
        // Arrange
        val campaignId = "campaign123"

        // Record some impressions to update the perSessionTotal value
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)

        // Act
        val result = impressionManager.perSessionTotal()

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun testPerSecond() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        val oneSecondAgo = currentTimestamp - 1
        val twoSecondsAgo = currentTimestamp - 2
        val threeSecondsAgo = currentTimestamp - 3
        val elvenSecondsAgo = currentTimestamp - 11

        // IMP: Impression should be recorded in increasing order of timeStamps otherwise unit test will get failed
        recordImpression(elvenSecondsAgo, campaignId)
        recordImpression(threeSecondsAgo, campaignId)
        recordImpression(twoSecondsAgo, campaignId)
        recordImpression(oneSecondAgo, campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // test per 1 second
        assertEquals(1, impressionManager.perSecond(campaignId, 1))

        // test per 10 second
        assertEquals(3, impressionManager.perSecond(campaignId, 10))

        // test per 11 second
        assertEquals(4, impressionManager.perSecond(campaignId, 11))

        // test per 12 second
        assertEquals(4, impressionManager.perSecond(campaignId, 12))
    }

    @Test
    fun testPerMinute() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        val oneMinuteAgo = currentTimestamp - TimeUnit.MINUTES.toSeconds(1)
        val oneMinuteOneSecondAgo = oneMinuteAgo - 1
        val thirtySecondsAgo = currentTimestamp - 30
        val sixtyFiveMinutesAgo = currentTimestamp - TimeUnit.MINUTES.toSeconds(65)

        // IMP: Impression should be recorded in increasing order of timeStamps otherwise unit test will get failed
        recordImpression(sixtyFiveMinutesAgo, campaignId)
        recordImpression(oneMinuteOneSecondAgo, campaignId)
        recordImpression(oneMinuteAgo, campaignId)
        recordImpression(thirtySecondsAgo, campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // test per 1 minute
        assertEquals(2, impressionManager.perMinute(campaignId, 1))

        // test per 2 minutes
        assertEquals(3, impressionManager.perMinute(campaignId, 2))

        // test per 60 minutes
        assertEquals(3, impressionManager.perMinute(campaignId, 60))

        // test per 70 minutes
        assertEquals(4, impressionManager.perMinute(campaignId, 70))
    }

    @Test
    fun testPerHour() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        val thirtyMinutesAgo = currentTimestamp - TimeUnit.MINUTES.toSeconds(30)
        val oneHourAgo = currentTimestamp - TimeUnit.HOURS.toSeconds(1)
        val oneHourOneSecondAgo = oneHourAgo - 1
        val twentyFiveHoursAgo = currentTimestamp - TimeUnit.HOURS.toSeconds(25)
        val twentyFiveHoursOneSecondAgo = twentyFiveHoursAgo - 1

        // IMP: Impression should be recorded in increasing order of timeStamps otherwise unit test will get failed
        recordImpression(twentyFiveHoursOneSecondAgo, campaignId)
        recordImpression(twentyFiveHoursAgo, campaignId)
        recordImpression(oneHourOneSecondAgo, campaignId)
        recordImpression(oneHourAgo, campaignId)
        recordImpression(thirtyMinutesAgo, campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // test per 1 hour
        assertEquals(2, impressionManager.perHour(campaignId, 1))

        // test per 2 hours
        assertEquals(3, impressionManager.perHour(campaignId, 2))

        // test per 25 hours
        assertEquals(4, impressionManager.perHour(campaignId, 25))

        // test per 26 hours
        assertEquals(5, impressionManager.perHour(campaignId, 26))
    }

    @Test
    fun testPerDay() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        val referenceTimestamp = getSecondsSinceLastMidnight()

        val twoDayBeforeMidnightMinus1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(2) - 1
        val twoDaysBeforeMidnightOffset1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(2) + 1
        val oneDayBeforeMidnightMinus1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(1) - 1
        val oneDayBeforeMidnightOffset1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(1) + 1
        val tenHoursBeforeMidnight = referenceTimestamp - TimeUnit.HOURS.toSeconds(10)
        val oneMinuteBeforeMidnight = referenceTimestamp - TimeUnit.MINUTES.toSeconds(1)
        val tenHoursFromMidnight = referenceTimestamp + TimeUnit.HOURS.toSeconds(10)
        val oneSecondAgo = currentTimestamp - 1

        //Arrange
        recordImpression(twoDayBeforeMidnightMinus1s, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(0, impressionManager.perDay(campaignId, 2))
        assertEquals(1, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(twoDaysBeforeMidnightOffset1s, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(1, impressionManager.perDay(campaignId, 2))
        assertEquals(2, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneDayBeforeMidnightMinus1s, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(2, impressionManager.perDay(campaignId, 2))
        assertEquals(3, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneDayBeforeMidnightOffset1s, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(1, impressionManager.perDay(campaignId, 1))
        assertEquals(3, impressionManager.perDay(campaignId, 2))
        assertEquals(4, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(tenHoursBeforeMidnight, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(2, impressionManager.perDay(campaignId, 1))
        assertEquals(4, impressionManager.perDay(campaignId, 2))
        assertEquals(5, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneMinuteBeforeMidnight, campaignId)
        recordImpression(tenHoursFromMidnight, campaignId)
        recordImpression(oneSecondAgo, campaignId)
        //Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(2, impressionManager.perDay(campaignId, 0))
        assertEquals(5, impressionManager.perDay(campaignId, 1))
        assertEquals(7, impressionManager.perDay(campaignId, 2))
        assertEquals(8, impressionManager.perDay(campaignId, 3))
    }

    @Test
    fun testPerWeek() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        val referenceTimestamp = getSecondsSinceFirstDayOfCurrentWeek()

        val twoWeeksBeforeStartOfWeekMinus1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(14) - 1
        val twoWeeksBeforeStartOfWeekOffset1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(14) + 1
        val oneWeekBeforeStartOfWeekMinus1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(7) - 1
        val oneWeekBeforeStartOfWeekOffset1s = referenceTimestamp - TimeUnit.DAYS.toSeconds(7) + 1
        val tenHoursBeforeStartOfWeek = referenceTimestamp - TimeUnit.HOURS.toSeconds(10)
        val oneMinuteBeforeStartOfWeek = referenceTimestamp - TimeUnit.MINUTES.toSeconds(1)
        val tenHoursFromStartOfWeek = referenceTimestamp + TimeUnit.HOURS.toSeconds(10)
        val oneSecondAgo = currentTimestamp - 1

        // Arrange
        recordImpression(twoWeeksBeforeStartOfWeekMinus1s, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(0, impressionManager.perWeek(campaignId, 2))
        assertEquals(1, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(twoWeeksBeforeStartOfWeekOffset1s, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(1, impressionManager.perWeek(campaignId, 2))
        assertEquals(2, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneWeekBeforeStartOfWeekMinus1s, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(2, impressionManager.perWeek(campaignId, 2))
        assertEquals(3, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneWeekBeforeStartOfWeekOffset1s, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(3, impressionManager.perWeek(campaignId, 2))
        assertEquals(4, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(tenHoursBeforeStartOfWeek, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(4, impressionManager.perWeek(campaignId, 2))
        assertEquals(5, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneMinuteBeforeStartOfWeek, campaignId)
        recordImpression(tenHoursFromStartOfWeek, campaignId)
        recordImpression(oneSecondAgo, campaignId)
        // Act and Assert
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)
        assertEquals(2, impressionManager.perWeek(campaignId, 0))
        assertEquals(2, impressionManager.perWeek(campaignId, 1))
        assertEquals(7, impressionManager.perWeek(campaignId, 2))
        assertEquals(8, impressionManager.perWeek(campaignId, 3))
    }

    private fun getSecondsSinceFirstDayOfCurrentWeek(): Long {
        // get today and clear time of day
        val cal = Calendar.getInstance(Locale.US).apply {
            val currentDate = Date()
            // Set the calendar's time to the current date and time
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // get start of this week in milliseconds
        cal[Calendar.DAY_OF_WEEK] = cal.firstDayOfWeek
        return TimeUnit.MILLISECONDS.toSeconds(cal.time.time)
    }

    private fun getSecondsSinceLastMidnight(): Long {
        val timeInMillis =
        Calendar.getInstance(Locale.US).apply {
            val currentDate = Date()
            // Set the calendar's time to the current date and time
            time = currentDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time.time

        return TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
    }

    private fun recordImpression(timestamp: Long, campaignId: String) {
        `when`(clock.currentTimeSeconds()).thenReturn(timestamp)
        impressionManager.recordImpression(campaignId)
    }

}