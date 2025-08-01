package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StoreProvider
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.junit.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class ImpressionManagerTest : BaseTestCase() {

    private lateinit var clock: Clock

    private lateinit var deviceInfo: DeviceInfo

    private lateinit var impressionStore: ImpressionStore

    private lateinit var impressionManager: ImpressionManager

    override fun setUp() {
        super.setUp()
        clock = mockk(relaxed = true)
        deviceInfo = mockk(relaxed = true)
        val mockLegacyInAppStore: LegacyInAppStore = mockk()
        val mockInAppAssetsStore: InAppAssetsStore = mockk()
        val mockFileStore: FileStore = mockk()
        val storeRegistry = StoreRegistry(
            legacyInAppStore = mockLegacyInAppStore,
            inAppAssetsStore = mockInAppAssetsStore,
            filesStore = mockFileStore
        )

        impressionManager = ImpressionManager(
            storeRegistry = storeRegistry, clock = clock, locale = Locale.getDefault()
        )

        every { deviceInfo.deviceID } returns "device_id"

        impressionStore = StoreProvider.getInstance().provideImpressionStore(
            appCtx, deviceInfo.deviceID, "account_id"
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

        every { clock.currentTimeSeconds() } returns currentTimestamp

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

        every { clock.currentTimeSeconds() } returns currentTimestamp

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

        every { clock.currentTimeSeconds() } returns currentTimestamp

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
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(0, impressionManager.perDay(campaignId, 2))
        assertEquals(1, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(twoDaysBeforeMidnightOffset1s, campaignId)
        //Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(1, impressionManager.perDay(campaignId, 2))
        assertEquals(2, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneDayBeforeMidnightMinus1s, campaignId)
        //Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(0, impressionManager.perDay(campaignId, 1))
        assertEquals(2, impressionManager.perDay(campaignId, 2))
        assertEquals(3, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneDayBeforeMidnightOffset1s, campaignId)
        //Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(1, impressionManager.perDay(campaignId, 1))
        assertEquals(3, impressionManager.perDay(campaignId, 2))
        assertEquals(4, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(tenHoursBeforeMidnight, campaignId)
        //Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perDay(campaignId, 0))
        assertEquals(2, impressionManager.perDay(campaignId, 1))
        assertEquals(4, impressionManager.perDay(campaignId, 2))
        assertEquals(5, impressionManager.perDay(campaignId, 3))

        //Arrange
        recordImpression(oneMinuteBeforeMidnight, campaignId)
        recordImpression(tenHoursFromMidnight, campaignId)
        recordImpression(oneSecondAgo, campaignId)
        //Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
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
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(0, impressionManager.perWeek(campaignId, 2))
        assertEquals(1, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(twoWeeksBeforeStartOfWeekOffset1s, campaignId)
        // Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(1, impressionManager.perWeek(campaignId, 2))
        assertEquals(2, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneWeekBeforeStartOfWeekMinus1s, campaignId)
        // Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(2, impressionManager.perWeek(campaignId, 2))
        assertEquals(3, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneWeekBeforeStartOfWeekOffset1s, campaignId)
        // Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(3, impressionManager.perWeek(campaignId, 2))
        assertEquals(4, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(tenHoursBeforeStartOfWeek, campaignId)
        // Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
        assertEquals(0, impressionManager.perWeek(campaignId, 0))
        assertEquals(0, impressionManager.perWeek(campaignId, 1))
        assertEquals(4, impressionManager.perWeek(campaignId, 2))
        assertEquals(5, impressionManager.perWeek(campaignId, 3))

        // Arrange
        recordImpression(oneMinuteBeforeStartOfWeek, campaignId)
        recordImpression(tenHoursFromStartOfWeek, campaignId)
        recordImpression(oneSecondAgo, campaignId)
        // Act and Assert
        every { clock.currentTimeSeconds() } returns currentTimestamp
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

    @Test
    fun testClearSessionData() {
        // Arrange
        val campaignId = "campaign123"

        // Record an impression to ensure sessionImpressions is not empty
        impressionManager.recordImpression(campaignId)

        // Verify that sessionImpressions is not empty initially
        val initialSessionImpressionsSize = impressionManager.perSessionTotal()
        assert(initialSessionImpressionsSize > 0)

        // Act
        impressionManager.clearSessionData()

        // Assert
        val clearedSessionImpressionsSize = impressionManager.perSessionTotal()
        assertEquals(0, clearedSessionImpressionsSize) // Expecting sessionImpressions to be cleared
    }

    @Test
    fun getImpressionCountEmptyArray() {
        val campaignId = "campaign123"
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 5))
    }

    @Test
    fun getImpressionCountTargetEqualToFirstElement() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetEqualToLastElement() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(1, impressionManager.getImpressionCount(campaignId, 9))
    }

    @Test
    fun getImpressionCountTargetEqualToFirstElementWithLeftDuplicates() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(9, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetEqualToLastElementWithRightDuplicates() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 9, 9, 9, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 9))
    }

    @Test
    fun getImpressionCountTargetGreaterThanAllElements() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 10))
    }

    @Test
    fun getImpressionCountTargetLessThanAllElements() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 0))
    }

    @Test
    fun getImpressionCountTargetEqualToExistingElement() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(3, impressionManager.getImpressionCount(campaignId, 5))
    }

    @Test
    fun getImpressionCountTargetWithDuplicates() {
        val campaignId = "campaign123"
        listOf(1, 1, 3, 5, 7, 9).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(4, impressionManager.getImpressionCount(campaignId, 3))
    }

    @Test
    fun `getImpressionCount with single element greater than target`() {
        val campaignId = "campaign123"
        listOf(5).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(1, impressionManager.getImpressionCount(campaignId, 4))
    }

    @Test
    fun `getImpressionCount with single element equals to target`() {
        val campaignId = "campaign123"
        listOf(5).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(1, impressionManager.getImpressionCount(campaignId, 5))
    }

    @Test
    fun `getImpressionCount with single element less than target`() {
        val campaignId = "campaign123"
        listOf(5).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 6))
    }

    @Test
    fun `getImpressionCount with all equal elements and target greater`() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 2))
    }

    @Test
    fun `getImpressionCount with duplicates`() {
        val campaignId = "campaign123"
        listOf(1, 1, 2, 2, 3, 3).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(4, impressionManager.getImpressionCount(campaignId, 2))
    }

    @Test
    fun `getImpressionCount with all equal elements and target equal`() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetWithNegativeNumbers() {
        val campaignId = "campaign123"
        listOf(-5, -3, -1, 1, 3).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(2, impressionManager.getImpressionCount(campaignId, 0))
    }

    @Test
    fun getImpressionCountTargetEqualToFirstElementWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(6, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetEqualToLastElementWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(1, impressionManager.getImpressionCount(campaignId, 11))
    }

    @Test
    fun getImpressionCountTargetEqualToFirstElementWithLeftDuplicatesWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(10, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetEqualToLastElementWithRightDuplicatesWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11, 11, 11, 11, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 11))
    }

    @Test
    fun getImpressionCountTargetGreaterThanAllElementsWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 12))
    }

    @Test
    fun getImpressionCountTargetLessThanAllElementsWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(6, impressionManager.getImpressionCount(campaignId, 0))
    }

    @Test
    fun getImpressionCountTargetEqualToExistingElementWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(3, impressionManager.getImpressionCount(campaignId, 7))
    }

    @Test
    fun getImpressionCountTargetWithDuplicatesWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(1, 1, 3, 5, 7, 9, 11).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(5, impressionManager.getImpressionCount(campaignId, 3))
    }

    @Test
    fun `getImpressionCount with all equal elements and target greater with even list size`() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1, 1).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(0, impressionManager.getImpressionCount(campaignId, 2))
    }

    @Test
    fun `getImpressionCount with duplicates with even list size`() {
        val campaignId = "campaign123"
        listOf(1, 1, 2, 2, 3, 3, 4, 4).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(4, impressionManager.getImpressionCount(campaignId, 3))
    }

    @Test
    fun `getImpressionCount with all equal elements and target equal with even list size`() {
        val campaignId = "campaign123"
        listOf(1, 1, 1, 1, 1, 1).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(6, impressionManager.getImpressionCount(campaignId, 1))
    }

    @Test
    fun getImpressionCountTargetWithNegativeNumbersWithEvenListSize() {
        val campaignId = "campaign123"
        listOf(-5, -3, -1, 1, 3, 5).forEach { recordImpression(it.toLong(), campaignId) }
        assertEquals(3, impressionManager.getImpressionCount(campaignId, 0))
    }

    private fun getSecondsSinceLastMidnight(): Long {
        val timeInMillis = Calendar.getInstance(Locale.US).apply {
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
        every { clock.currentTimeSeconds() } returns timestamp
        impressionManager.recordImpression(campaignId)
    }
}