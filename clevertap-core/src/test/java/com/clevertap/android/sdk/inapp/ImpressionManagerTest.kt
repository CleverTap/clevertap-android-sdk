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
    private lateinit var clock: FakeClock

    @Mock
    private lateinit var deviceInfo: DeviceInfo

    private lateinit var impressionManager: ImpressionManager

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        val storeRegistry = StoreRegistry()
        clock = FakeClock()

        impressionManager = ImpressionManager(
            storeRegistry = storeRegistry,
            clock = clock,
            locale = Locale.US
        )

        `when`(deviceInfo.deviceID).thenReturn("device_id")

        val impStore: ImpressionStore = StoreProvider.getInstance().provideImpressionStore(
            appCtx, deviceInfo,
            "account_id"
        )

        storeRegistry.impressionStore = impStore
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
//        val currentTime = 123456L // Replace with a desired timestamp
//
//        // Mock the clock to return a fixed timestamp
//        `when`(clock.currentTimeSeconds()).thenReturn(currentTime)

        assertEquals(0, impressionManager.perSessionTotal())

        // Act
        impressionManager.recordImpression(campaignId)

        // Assert
        assertEquals(1, impressionManager.perSessionTotal()) // Expecting one impression recorded
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
        assertEquals(3, result) // Expecting the total number of impressions recorded
    }
}