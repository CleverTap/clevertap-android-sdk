package com.clevertap.android.sdk.inapp

import android.content.SharedPreferences
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.*
import org.mockito.*
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TriggerManagerTest : BaseTestCase() {

    @Mock
    private lateinit var deviceInfo: DeviceInfo

    private lateinit var triggerManager: TriggerManager

    private lateinit var sharedPreferences: SharedPreferences

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        application = TestApplication.application

        triggerManager = TriggerManager(appCtx, "mockAccountId", deviceInfo)

        sharedPreferences = triggerManager.sharedPrefs()!!
    }

    @Test
    fun testGetTriggers_WhenNoTriggersStored_ShouldReturnZero() {
        val campaignId = "campaign123"

        val result = triggerManager.getTriggers(campaignId)

        assert(result == 0)
    }

    @Test
    fun testGetTriggers_WhenTriggersStored_ShouldReturnStoredValue() {
        val campaignId = "campaign123"

        triggerManager.increment(campaignId)

        assertEquals(1, triggerManager.getTriggers(campaignId))
    }

    @Test
    fun testIncrement_WhenTriggersExist_ShouldIncrementTriggers() {
        val campaignId = "campaign123"

        triggerManager.increment(campaignId)
        assertEquals(1, triggerManager.getTriggers(campaignId))

        triggerManager.increment(campaignId)
        assertEquals(2, triggerManager.getTriggers(campaignId))
    }

    @Test
    fun testIncrement_WhenNoTriggersExist_ShouldSetTriggersTo1() {
        val campaignId = "campaign123"

        assertEquals(0, triggerManager.getTriggers(campaignId))

        triggerManager.increment(campaignId)

        assertEquals(1, triggerManager.getTriggers(campaignId))
    }

    @Test
    fun testRemoveTriggers_WhenTriggersExist_ShouldRemoveTriggers() {
        val campaignId = "campaign123"

        triggerManager.increment(campaignId)
        assertEquals(1, triggerManager.getTriggers(campaignId))

        triggerManager.removeTriggers(campaignId)
        assertEquals(0, triggerManager.getTriggers(campaignId))
    }

    @Test
    fun testRemoveTriggers_WhenCampaignIdDoesNotExist_ShouldReturnZeroTriggers() {
        val campaignId = "campaign123"

        assertEquals(0, triggerManager.getTriggers(campaignId))

        triggerManager.removeTriggers(campaignId)

        assertEquals(0, triggerManager.getTriggers(campaignId))
    }

    @Test
    fun testSharedPrefs_WhenContextRefCleared_ReturnsNull() {
        // Arrange
        triggerManager.contextRef.clear() // Simulate WeakReference being cleared

        // Act
        val result = triggerManager.sharedPrefs()

        // Assert
        assertNull(result)
    }

    @Test
    fun testSharedPrefs_WithNonNullContextRef_ReturnsSharedPreferences() {
        // Act
        val result = triggerManager.sharedPrefs()

        // Assert
        assertNotNull(result)
    }

    @Test
    fun testSharedPrefs_ReturnsNull_WithNullContext() {
        // Arrange
        triggerManager.contextRef = WeakReference(null)

        // Act
        val result = triggerManager.sharedPrefs()

        // Assert
        assertNull(result)
    }

    @Test
    fun testSharedPrefs_ReturnsNull_WithNonNullContext() {
        // Arrange
        triggerManager.contextRef = WeakReference(appCtx)

        // Act
        val result = triggerManager.sharedPrefs()

        // Assert
        assertNotNull(result)
    }

    @Test
    fun getTriggersKey_GeneratesCorrectKey() {
        val campaignId = "campaign123"

        // Act
        val result = triggerManager.getTriggersKey(campaignId)

        // Assert
        val expectedKey = "${TriggerManager.PREF_PREFIX}_$campaignId"
        assertEquals(expectedKey, result, "Generated key does not match the expected format")
    }
}