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
import java.util.Date
import java.util.Locale
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
    fun `perSecond should return correct impression count for impressions within the last 5 seconds`() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        // Record impressions within the last 5 seconds
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 1)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 2)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 3)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // Act
        val result = impressionManager.perSecond(campaignId, 5)

        // Assert
        assertEquals(3, result)
    }

    @Test
    fun `perSecond should return 0 for impressions outside the last 5 seconds`() {
        // Arrange
        val campaignId = "campaign123"
        val currentTimestamp = System.currentTimeMillis() / 1000

        // Record impressions outside the last 5 seconds
        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 10)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 20)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp - 30)
        impressionManager.recordImpression(campaignId)

        `when`(clock.currentTimeSeconds()).thenReturn(currentTimestamp)

        // Act
        val result = impressionManager.perSecond(campaignId, 5)

        // Assert
        assertEquals(0, result)
    }
}