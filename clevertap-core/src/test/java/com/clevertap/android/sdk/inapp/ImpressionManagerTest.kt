package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.*
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class ImpressionManagerTest : BaseTestCase() {
    @Mock
    private lateinit var clock: Clock

    private lateinit var impressionManager: ImpressionManager

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        impressionManager = ImpressionManager(
            impressionStore = ImpressionStore(application, "accountId", "testDeviceId"),
            clock = clock,
            locale = Locale.US
        )
    }

    @Test
    fun testRecordImpression() {
        // Arrange
        val campaignId = "campaign123"
        val currentTime = 123456L // Replace with a desired timestamp

        // Mock the clock to return a fixed timestamp
        `when`(clock.currentTimeSeconds()).thenReturn(currentTime)

        // Act
        impressionManager.recordImpression(campaignId)

        // Assert
        assertEquals(1, impressionManager.perSessionTotal()) // Expecting one impression recorded
    }

    @Test
    fun testPerSessionWithCampaignIdPresent() {
        // Arrange
        val campaignId = "campaign123"

        // Create a sessionImpressions map with a campaignId and associated list
        val sessionImpressions = mutableMapOf(
            campaignId to mutableListOf(1L, 2L, 3L)
        )

        // Set the sessionImpressions map in the impressionManager
        impressionManager.setSessionImpressions(sessionImpressions)

        // Act
        val result = impressionManager.perSession(campaignId)

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun testPerSessionWithCampaignIdMissing() {
        // Arrange
        val campaignId = "campaign123"

        // Act
        val result = impressionManager.perSession(campaignId)

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun testPerSessionTotalWithInitialValue() {
        // Act
        val result = impressionManager.perSessionTotal()

        // Assert
        assertEquals(0, result)
    }

    @Test
    fun testPerSessionTotalAfterRecordImpressions() {
        // Arrange
        val campaignId = "campaign123"

        // Record some impressions to update the perSessionTotal value
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)
        impressionManager.recordImpression(campaignId)

        // Act
        val result = impressionManager.perSessionTotal()

        // Assert
        assertEquals(3, result) // Expecting the total number of impressions recorded
    }

    @Test
    fun testPerSecondWithImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimeSeconds = 1000L
        val seconds = 10

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimeSeconds)

        // Record one impression
        impressionManager.recordImpression(campaignId)

        // Act
        val result = impressionManager.perSecond(campaignId, seconds)

        // Assert
        assertEquals(1, result) // Expecting 1 impression within the last 10 seconds
    }

    @Test
    fun testPerSecondWithoutImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimeSeconds = 1000L
        val seconds = 10

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimeSeconds)

        // No impressions recorded, so the result should be 0

        // Act
        val result = impressionManager.perSecond(campaignId, seconds)

        // Assert
        assertEquals(0, result) // Expecting 0 impressions within the last 10 seconds
    }

    @Test
    fun testPerMinuteWithImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = 1000L // Replace with a desired timestamp
        val minutesOffset = 5 // Minutes to subtract

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // Act
        impressionManager.recordImpression(campaignId) // Record an impression
        val result = impressionManager.perMinute(campaignId, minutesOffset)

        // Assert
        assertEquals(1, result) // Expecting 1 impression recorded within the last 5 minutes
    }

    @Test
    fun testPerMinuteWithoutImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = 1000L // Replace with a desired timestamp
        val minutesOffset = 5 // Minutes to subtract

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // No impressions recorded, so the result should be 0

        // Act
        val result = impressionManager.perMinute(campaignId, minutesOffset)

        // Assert
        assertEquals(0, result) // Expecting 0 impressions within the last 5 minutes
    }

    @Test
    fun testPerHourWithImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = 1000L // Replace with a desired timestamp
        val hoursOffset = 2 // Hours to subtract

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // Act
        impressionManager.recordImpression(campaignId) // Record an impression
        val result = impressionManager.perHour(campaignId, hoursOffset)

        // Assert
        assertEquals(1, result) // Expecting 1 impression recorded within the last 2 hours
    }

    @Test
    fun testPerHourWithoutImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = 1000L // Replace with a desired timestamp
        val hoursOffset = 2 // Hours to subtract

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // Act
        val result = impressionManager.perHour(campaignId, hoursOffset)

        // Assert
        assertEquals(0, result) // Expecting 0 impressions within the last 2 hours
    }

    @Test
    fun testPerDayWithImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val daysOffset = 2 // Days to subtract

        val currentDate = Calendar.getInstance()
        //currentDate.set(2023, Calendar.SEPTEMBER, 25) // Set the currentDate to some past date to fail this test

        `when`(clock.currentTimeSeconds()).thenReturn(TimeUnit.MILLISECONDS.toSeconds(currentDate.timeInMillis))

        // Act
        impressionManager.recordImpression(campaignId) // Record an impression
        val result = impressionManager.perDay("campaign123", daysOffset)

        // Assert
        assertEquals(1, result) // Expecting 1 impression recorded within the last 2 days
    }

    @Test
    fun testPerDayWithoutImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val daysOffset = 2 // Days to subtract

        val currentDate = Calendar.getInstance()
        `when`(clock.currentTimeSeconds()).thenReturn(TimeUnit.MILLISECONDS.toSeconds(currentDate.timeInMillis))

        // Act
        val result = impressionManager.perDay(campaignId, daysOffset)

        // Assert
        assertEquals(0, result) // Expecting 0 impressions within the last 2 days
    }

    @Test
    fun testPerWeekWithImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val weeksOffset = 1 // Weeks to subtract
        val currentDate = Calendar.getInstance()
        /*currentDate.set(
            2023, Calendar.SEPTEMBER, 24
        )*/ // Set the currentDate to some past date to fail this test

        `when`(clock.currentTimeSeconds()).thenReturn(TimeUnit.MILLISECONDS.toSeconds(currentDate.timeInMillis))

        // Act
        impressionManager.recordImpression(campaignId) // Record an impression
        val result = impressionManager.perWeek(campaignId, weeksOffset)

        // Assert
        assertEquals(1, result) // Expecting 1 impression recorded within the last 2 weeks
    }

    @Test
    fun testPerWeekWithoutImpressions() {
        // Arrange
        val campaignId = "campaign123"
        val weeksOffset = 2 // Weeks to subtract
        val currentDate = Calendar.getInstance()
        currentDate.set(2023, Calendar.SEPTEMBER, 24)

        `when`(clock.currentTimeSeconds()).thenReturn(TimeUnit.MILLISECONDS.toSeconds(currentDate.timeInMillis))

        // Act
        val result = impressionManager.perWeek(campaignId, weeksOffset)

        // Assert
        assertEquals(0, result) // Expecting 0 impressions within the last 2 weeks
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
}