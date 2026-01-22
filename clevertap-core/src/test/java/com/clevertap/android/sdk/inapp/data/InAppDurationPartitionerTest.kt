package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_INACTION_DURATION
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class InAppDurationPartitionerTest {

    // ==================== PARTITION LEGACY IN-APPS TESTS ====================

    @Test
    fun `partitionLegacyInApps returns empty when input list is empty`() {
        // Arrange
        val inAppsList = emptyList<JSONObject>()

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertTrue(result.immediateInApps.isEmpty())
        assertTrue(result.delayedInApps.isEmpty())
    }

    @Test
    fun `partitionLegacyInApps places in-app without delay field in immediate list`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
        assertEquals("inapp1", result.immediateInApps[0].getString("id"))
    }

    @Test
    fun `partitionLegacyInApps places in-app with delay 0 in immediate list`() {
        // Arrange
        val inApp = createDelayedInApp("inapp1", delay = 0)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
    }

    @Test
    fun `partitionLegacyInApps places in-app with delay 1 in delayed list`() {
        // Arrange - minimum valid delay
        val inApp = createDelayedInApp("inapp1", delay = 1)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.delayedInApps.size)
        assertEquals("inapp1", result.delayedInApps[0].getString("id"))
    }

    @Test
    fun `partitionLegacyInApps places in-app with delay 1200 in delayed list`() {
        // Arrange - maximum valid delay
        val inApp = createDelayedInApp("inapp1", delay = 1200)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionLegacyInApps places in-app with delay greater than 1200 in immediate list`() {
        // Arrange - exceeds maximum valid delay
        val inApp = createDelayedInApp("inapp1", delay = 1201)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
    }

    @Test
    fun `partitionLegacyInApps places in-app with negative delay in immediate list`() {
        // Arrange
        val inApp = createDelayedInApp("inapp1", delay = -1)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
    }

    @Test
    fun `partitionLegacyInApps correctly partitions mixed immediate and delayed in-apps`() {
        // Arrange
        val immediate1 = createInApp("immediate1")
        val immediate2 = createDelayedInApp("immediate2", delay = 0)
        val immediate3 = createDelayedInApp("immediate3", delay = 1201)
        val delayed1 = createDelayedInApp("delayed1", delay = 10)
        val delayed2 = createDelayedInApp("delayed2", delay = 600)
        val delayed3 = createDelayedInApp("delayed3", delay = 1200)

        val inAppsList = listOf(immediate1, delayed1, immediate2, delayed2, immediate3, delayed3)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(3, result.immediateInApps.size)
        assertEquals(3, result.delayedInApps.size)

        val immediateIds = result.immediateInApps.map { it.getString("id") }
        assertTrue(immediateIds.contains("immediate1"))
        assertTrue(immediateIds.contains("immediate2"))
        assertTrue(immediateIds.contains("immediate3"))

        val delayedIds = result.delayedInApps.map { it.getString("id") }
        assertTrue(delayedIds.contains("delayed1"))
        assertTrue(delayedIds.contains("delayed2"))
        assertTrue(delayedIds.contains("delayed3"))
    }

    @Test
    fun `partitionLegacyInApps preserves order within immediate list`() {
        // Arrange
        val inApp1 = createInApp("first")
        val inApp2 = createInApp("second")
        val inApp3 = createInApp("third")
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertEquals("first", result.immediateInApps[0].getString("id"))
        assertEquals("second", result.immediateInApps[1].getString("id"))
        assertEquals("third", result.immediateInApps[2].getString("id"))
    }

    @Test
    fun `partitionLegacyInApps preserves order within delayed list`() {
        // Arrange
        val inApp1 = createDelayedInApp("first", delay = 10)
        val inApp2 = createDelayedInApp("second", delay = 20)
        val inApp3 = createDelayedInApp("third", delay = 30)
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertEquals("first", result.delayedInApps[0].getString("id"))
        assertEquals("second", result.delayedInApps[1].getString("id"))
        assertEquals("third", result.delayedInApps[2].getString("id"))
    }

    // ==================== PARTITION LEGACY META IN-APPS TESTS ====================

    @Test
    fun `partitionLegacyMetaInApps wraps empty list`() {
        // Arrange
        val inAppsList = emptyList<JSONObject>()

        // Act
        val result = InAppDurationPartitioner.partitionLegacyMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasInActionInApps())
        assertTrue(result.inActionInApps.isEmpty())
    }

    @Test
    fun `partitionLegacyMetaInApps wraps all in-apps as inAction`() {
        // Arrange
        val inApp1 = createInActionInApp("inaction1", inactionDuration = 60)
        val inApp2 = createInActionInApp("inaction2", inactionDuration = 120)
        val inAppsList = listOf(inApp1, inApp2)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasInActionInApps())
        assertEquals(2, result.inActionInApps.size)
        assertEquals("inaction1", result.inActionInApps[0].getString("id"))
        assertEquals("inaction2", result.inActionInApps[1].getString("id"))
    }

    @Test
    fun `partitionLegacyMetaInApps preserves order`() {
        // Arrange
        val inApp1 = createInActionInApp("first", inactionDuration = 10)
        val inApp2 = createInActionInApp("second", inactionDuration = 20)
        val inApp3 = createInActionInApp("third", inactionDuration = 30)
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyMetaInApps(inAppsList)

        // Assert
        assertEquals("first", result.inActionInApps[0].getString("id"))
        assertEquals("second", result.inActionInApps[1].getString("id"))
        assertEquals("third", result.inActionInApps[2].getString("id"))
    }

    // ==================== PARTITION CLIENT-SIDE IN-APPS TESTS ====================

    @Test
    fun `partitionClientSideInApps returns empty when input list is empty`() {
        // Arrange
        val inAppsList = emptyList<JSONObject>()

        // Act
        val result = InAppDurationPartitioner.partitionClientSideInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
    }

    @Test
    fun `partitionClientSideInApps places in-app without delay field in immediate list`() {
        // Arrange
        val inApp = createInApp("cs_inapp1")
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionClientSideInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
    }

    @Test
    fun `partitionClientSideInApps places in-app with valid delay in delayed list`() {
        // Arrange
        val inApp = createDelayedInApp("cs_inapp1", delay = 30)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionClientSideInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionClientSideInApps correctly partitions mixed in-apps`() {
        // Arrange
        val immediate = createInApp("immediate")
        val delayed = createDelayedInApp("delayed", delay = 100)
        val inAppsList = listOf(immediate, delayed)

        // Act
        val result = InAppDurationPartitioner.partitionClientSideInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionClientSideInApps handles boundary delay values`() {
        // Arrange
        val inAppDelay0 = createDelayedInApp("delay0", delay = 0)
        val inAppDelay1 = createDelayedInApp("delay1", delay = 1)
        val inAppDelay1200 = createDelayedInApp("delay1200", delay = 1200)
        val inAppDelay1201 = createDelayedInApp("delay1201", delay = 1201)

        val inAppsList = listOf(inAppDelay0, inAppDelay1, inAppDelay1200, inAppDelay1201)

        // Act
        val result = InAppDurationPartitioner.partitionClientSideInApps(inAppsList)

        // Assert
        assertEquals(2, result.immediateInApps.size) // delay 0 and 1201
        assertEquals(2, result.delayedInApps.size)   // delay 1 and 1200

        val immediateIds = result.immediateInApps.map { it.getString("id") }
        assertTrue(immediateIds.contains("delay0"))
        assertTrue(immediateIds.contains("delay1201"))

        val delayedIds = result.delayedInApps.map { it.getString("id") }
        assertTrue(delayedIds.contains("delay1"))
        assertTrue(delayedIds.contains("delay1200"))
    }

    // ==================== PARTITION SERVER-SIDE META IN-APPS TESTS ====================

    @Test
    fun `partitionServerSideMetaInApps returns empty when input list is empty`() {
        // Arrange
        val inAppsList = emptyList<JSONObject>()

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasUnknownDurationInApps())
        assertFalse(result.hasInActionInApps())
        assertTrue(result.unknownDurationInApps.isEmpty())
        assertTrue(result.inActionInApps.isEmpty())
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app without inactionDuration in unknown list`() {
        // Arrange
        val inApp = createInApp("ss_inapp1")
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasUnknownDurationInApps())
        assertFalse(result.hasInActionInApps())
        assertEquals(1, result.unknownDurationInApps.size)
        assertEquals("ss_inapp1", result.unknownDurationInApps[0].getString("id"))
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app with inactionDuration 0 in unknown list`() {
        // Arrange
        val inApp = createInActionInApp("ss_inapp1", inactionDuration = 0)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasUnknownDurationInApps())
        assertFalse(result.hasInActionInApps())
        assertEquals(1, result.unknownDurationInApps.size)
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app with inactionDuration 1 in inAction list`() {
        // Arrange - minimum valid inaction duration
        val inApp = createInActionInApp("ss_inapp1", inactionDuration = 1)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasUnknownDurationInApps())
        assertTrue(result.hasInActionInApps())
        assertEquals(1, result.inActionInApps.size)
        assertEquals("ss_inapp1", result.inActionInApps[0].getString("id"))
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app with inactionDuration 1200 in inAction list`() {
        // Arrange - maximum valid inaction duration
        val inApp = createInActionInApp("ss_inapp1", inactionDuration = 1200)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasUnknownDurationInApps())
        assertTrue(result.hasInActionInApps())
        assertEquals(1, result.inActionInApps.size)
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app with inactionDuration greater than 1200 in unknown list`() {
        // Arrange - exceeds maximum valid inaction duration
        val inApp = createInActionInApp("ss_inapp1", inactionDuration = 1201)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasUnknownDurationInApps())
        assertFalse(result.hasInActionInApps())
        assertEquals(1, result.unknownDurationInApps.size)
    }

    @Test
    fun `partitionServerSideMetaInApps places in-app with negative inactionDuration in unknown list`() {
        // Arrange
        val inApp = createInActionInApp("ss_inapp1", inactionDuration = -1)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasUnknownDurationInApps())
        assertFalse(result.hasInActionInApps())
        assertEquals(1, result.unknownDurationInApps.size)
    }

    @Test
    fun `partitionServerSideMetaInApps correctly partitions mixed unknown and inAction in-apps`() {
        // Arrange
        val unknown1 = createInApp("unknown1")
        val unknown2 = createInActionInApp("unknown2", inactionDuration = 0)
        val unknown3 = createInActionInApp("unknown3", inactionDuration = 1201)
        val inAction1 = createInActionInApp("inaction1", inactionDuration = 60)
        val inAction2 = createInActionInApp("inaction2", inactionDuration = 600)
        val inAction3 = createInActionInApp("inaction3", inactionDuration = 1200)

        val inAppsList = listOf(unknown1, inAction1, unknown2, inAction2, unknown3, inAction3)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasUnknownDurationInApps())
        assertTrue(result.hasInActionInApps())
        assertEquals(3, result.unknownDurationInApps.size)
        assertEquals(3, result.inActionInApps.size)

        val unknownIds = result.unknownDurationInApps.map { it.getString("id") }
        assertTrue(unknownIds.contains("unknown1"))
        assertTrue(unknownIds.contains("unknown2"))
        assertTrue(unknownIds.contains("unknown3"))

        val inActionIds = result.inActionInApps.map { it.getString("id") }
        assertTrue(inActionIds.contains("inaction1"))
        assertTrue(inActionIds.contains("inaction2"))
        assertTrue(inActionIds.contains("inaction3"))
    }

    @Test
    fun `partitionServerSideMetaInApps preserves order within unknown list`() {
        // Arrange
        val inApp1 = createInApp("first")
        val inApp2 = createInApp("second")
        val inApp3 = createInApp("third")
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertEquals("first", result.unknownDurationInApps[0].getString("id"))
        assertEquals("second", result.unknownDurationInApps[1].getString("id"))
        assertEquals("third", result.unknownDurationInApps[2].getString("id"))
    }

    @Test
    fun `partitionServerSideMetaInApps preserves order within inAction list`() {
        // Arrange
        val inApp1 = createInActionInApp("first", inactionDuration = 10)
        val inApp2 = createInActionInApp("second", inactionDuration = 20)
        val inApp3 = createInActionInApp("third", inactionDuration = 30)
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertEquals("first", result.inActionInApps[0].getString("id"))
        assertEquals("second", result.inActionInApps[1].getString("id"))
        assertEquals("third", result.inActionInApps[2].getString("id"))
    }

    // ==================== PARTITION APP-LAUNCH SERVER-SIDE IN-APPS TESTS ====================

    @Test
    fun `partitionAppLaunchServerSideInApps places in-app without delay field in immediate list`() {
        // Arrange
        val inApp = createInApp("inapp1")
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertFalse(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
    }

    @Test
    fun `partitionAppLaunchServerSideInApps places in-app with valid delay in delayed list`() {
        // Arrange
        val inApp = createDelayedInApp("inapp1", delay = 30)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionAppLaunchServerSideInApps correctly partitions mixed in-apps`() {
        // Arrange
        val immediate = createInApp("immediate")
        val delayed = createDelayedInApp("delayed", delay = 100)
        val inAppsList = listOf(immediate, delayed)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideInApps(inAppsList)

        // Assert
        assertTrue(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.immediateInApps.size)
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionAppLaunchServerSideInApps handles boundary delay values`() {
        // Arrange
        val inAppDelay0 = createDelayedInApp("delay0", delay = 0)
        val inAppDelay1 = createDelayedInApp("delay1", delay = 1)
        val inAppDelay1200 = createDelayedInApp("delay1200", delay = 1200)
        val inAppDelay1201 = createDelayedInApp("delay1201", delay = 1201)

        val inAppsList = listOf(inAppDelay0, inAppDelay1, inAppDelay1200, inAppDelay1201)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideInApps(inAppsList)

        // Assert
        assertEquals(2, result.immediateInApps.size) // delay 0 and 1201
        assertEquals(2, result.delayedInApps.size)   // delay 1 and 1200

        val immediateIds = result.immediateInApps.map { it.getString("id") }
        assertTrue(immediateIds.contains("delay0"))
        assertTrue(immediateIds.contains("delay1201"))

        val delayedIds = result.delayedInApps.map { it.getString("id") }
        assertTrue(delayedIds.contains("delay1"))
        assertTrue(delayedIds.contains("delay1200"))
    }


    // ==================== PARTITION APP-LAUNCH SERVER-SIDE META IN-APPS TESTS ====================

    @Test
    fun `partitionAppLaunchServerSideMetaInApps wraps empty list`() {
        // Arrange
        val inAppsList = emptyList<JSONObject>()

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasInActionInApps())
        assertTrue(result.inActionInApps.isEmpty())
    }

    @Test
    fun `partitionAppLaunchServerSideMetaInApps wraps all in-apps as inAction`() {
        // Arrange
        val inApp1 = createInActionInApp("inaction1", inactionDuration = 60)
        val inApp2 = createInActionInApp("inaction2", inactionDuration = 120)
        val inAppsList = listOf(inApp1, inApp2)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideMetaInApps(inAppsList)

        // Assert
        assertTrue(result.hasInActionInApps())
        assertEquals(2, result.inActionInApps.size)
    }

    @Test
    fun `partitionAppLaunchServerSideMetaInApps preserves order`() {
        // Arrange
        val inApp1 = createInActionInApp("first", inactionDuration = 10)
        val inApp2 = createInActionInApp("second", inactionDuration = 20)
        val inApp3 = createInActionInApp("third", inactionDuration = 30)
        val inAppsList = listOf(inApp1, inApp2, inApp3)

        // Act
        val result = InAppDurationPartitioner.partitionAppLaunchServerSideMetaInApps(inAppsList)

        // Assert
        assertEquals("first", result.inActionInApps[0].getString("id"))
        assertEquals("second", result.inActionInApps[1].getString("id"))
        assertEquals("third", result.inActionInApps[2].getString("id"))
    }

    // ==================== EDGE CASES AND LARGE LISTS TESTS ====================

    @Test
    fun `partitionLegacyInApps in-app with both delay and inaction fields partitions by delay only`() {
        // Arrange - Legacy in-apps only check delay, not inaction
        val inApp = JSONObject()
            .put("id", "mixed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 100)
            .put(INAPP_INACTION_DURATION, 200)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionLegacyInApps(inAppsList)

        // Assert
        assertFalse(result.hasImmediateInApps())
        assertTrue(result.hasDelayedInApps())
        assertEquals(1, result.delayedInApps.size)
    }

    @Test
    fun `partitionServerSideMetaInApps in-app with both delay and inaction fields partitions by inaction only`() {
        // Arrange - Server-side meta in-apps only check inaction, not delay
        val inApp = JSONObject()
            .put("id", "mixed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 100)
            .put(INAPP_INACTION_DURATION, 200)
        val inAppsList = listOf(inApp)

        // Act
        val result = InAppDurationPartitioner.partitionServerSideMetaInApps(inAppsList)

        // Assert
        assertFalse(result.hasUnknownDurationInApps())
        assertTrue(result.hasInActionInApps())
        assertEquals(1, result.inActionInApps.size)
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(id: String): JSONObject {
        return JSONObject().put("id", id)
    }

    private fun createDelayedInApp(id: String, delay: Int): JSONObject {
        return JSONObject()
            .put("id", id)
            .put(INAPP_DELAY_AFTER_TRIGGER, delay)
    }

    private fun createInActionInApp(id: String, inactionDuration: Int): JSONObject {
        return JSONObject()
            .put("id", id)
            .put(INAPP_INACTION_DURATION, inactionDuration)
    }
}