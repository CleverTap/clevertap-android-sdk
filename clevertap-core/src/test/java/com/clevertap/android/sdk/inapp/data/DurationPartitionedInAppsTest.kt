package com.clevertap.android.sdk.inapp.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DurationPartitionedInAppsTest {

    // ==================== IMMEDIATE AND DELAYED TESTS ====================

    @Test
    fun `ImmediateAndDelayed hasImmediateInApps returns true when immediate list is not empty`() {
        // Arrange
        val immediateInApps = listOf(createInApp("immediate1"), createInApp("immediate2"))
        val delayedInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertTrue(partitioned.hasImmediateInApps())
    }

    @Test
    fun `ImmediateAndDelayed hasImmediateInApps returns false when immediate list is empty`() {
        // Arrange
        val immediateInApps = emptyList<JSONObject>()
        val delayedInApps = listOf(createInApp("delayed1"))

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertFalse(partitioned.hasImmediateInApps())
    }

    @Test
    fun `ImmediateAndDelayed hasDelayedInApps returns true when delayed list is not empty`() {
        // Arrange
        val immediateInApps = emptyList<JSONObject>()
        val delayedInApps = listOf(createInApp("delayed1"), createInApp("delayed2"))

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertTrue(partitioned.hasDelayedInApps())
    }

    @Test
    fun `ImmediateAndDelayed hasDelayedInApps returns false when delayed list is empty`() {
        // Arrange
        val immediateInApps = listOf(createInApp("immediate1"))
        val delayedInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertFalse(partitioned.hasDelayedInApps())
    }

    @Test
    fun `ImmediateAndDelayed both lists can be non-empty simultaneously`() {
        // Arrange
        val immediateInApps = listOf(createInApp("immediate1"))
        val delayedInApps = listOf(createInApp("delayed1"))

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertTrue(partitioned.hasImmediateInApps())
        assertTrue(partitioned.hasDelayedInApps())
        assertEquals(1, partitioned.immediateInApps.size)
        assertEquals(1, partitioned.delayedInApps.size)
    }

    @Test
    fun `ImmediateAndDelayed both lists can be empty simultaneously`() {
        // Arrange
        val immediateInApps = emptyList<JSONObject>()
        val delayedInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertFalse(partitioned.hasImmediateInApps())
        assertFalse(partitioned.hasDelayedInApps())
    }

    @Test
    fun `ImmediateAndDelayed empty companion function returns instance with empty lists`() {
        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed.empty()

        // Assert
        assertFalse(partitioned.hasImmediateInApps())
        assertFalse(partitioned.hasDelayedInApps())
        assertTrue(partitioned.immediateInApps.isEmpty())
        assertTrue(partitioned.delayedInApps.isEmpty())
    }

    @Test
    fun `ImmediateAndDelayed preserves order of in-apps in lists`() {
        // Arrange
        val immediate1 = createInApp("immediate1")
        val immediate2 = createInApp("immediate2")
        val immediate3 = createInApp("immediate3")
        val immediateInApps = listOf(immediate1, immediate2, immediate3)

        val delayed1 = createInApp("delayed1")
        val delayed2 = createInApp("delayed2")
        val delayedInApps = listOf(delayed1, delayed2)

        // Act
        val partitioned = DurationPartitionedInApps.ImmediateAndDelayed(immediateInApps, delayedInApps)

        // Assert
        assertEquals("immediate1", partitioned.immediateInApps[0].getString("id"))
        assertEquals("immediate2", partitioned.immediateInApps[1].getString("id"))
        assertEquals("immediate3", partitioned.immediateInApps[2].getString("id"))
        assertEquals("delayed1", partitioned.delayedInApps[0].getString("id"))
        assertEquals("delayed2", partitioned.delayedInApps[1].getString("id"))
    }

    // ==================== UNKNOWN AND INACTION TESTS ====================

    @Test
    fun `UnknownAndInAction hasUnknownDurationInApps returns true when unknown list is not empty`() {
        // Arrange
        val unknownInApps = listOf(createInApp("unknown1"), createInApp("unknown2"))
        val inActionInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertTrue(partitioned.hasUnknownDurationInApps())
    }

    @Test
    fun `UnknownAndInAction hasUnknownDurationInApps returns false when unknown list is empty`() {
        // Arrange
        val unknownInApps = emptyList<JSONObject>()
        val inActionInApps = listOf(createInApp("inaction1"))

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertFalse(partitioned.hasUnknownDurationInApps())
    }

    @Test
    fun `UnknownAndInAction hasInActionInApps returns true when inAction list is not empty`() {
        // Arrange
        val unknownInApps = emptyList<JSONObject>()
        val inActionInApps = listOf(createInApp("inaction1"), createInApp("inaction2"))

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertTrue(partitioned.hasInActionInApps())
    }

    @Test
    fun `UnknownAndInAction hasInActionInApps returns false when inAction list is empty`() {
        // Arrange
        val unknownInApps = listOf(createInApp("unknown1"))
        val inActionInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertFalse(partitioned.hasInActionInApps())
    }

    @Test
    fun `UnknownAndInAction both lists can be non-empty simultaneously`() {
        // Arrange
        val unknownInApps = listOf(createInApp("unknown1"))
        val inActionInApps = listOf(createInApp("inaction1"))

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertTrue(partitioned.hasUnknownDurationInApps())
        assertTrue(partitioned.hasInActionInApps())
        assertEquals(1, partitioned.unknownDurationInApps.size)
        assertEquals(1, partitioned.inActionInApps.size)
    }

    @Test
    fun `UnknownAndInAction both lists can be empty simultaneously`() {
        // Arrange
        val unknownInApps = emptyList<JSONObject>()
        val inActionInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertFalse(partitioned.hasUnknownDurationInApps())
        assertFalse(partitioned.hasInActionInApps())
    }

    @Test
    fun `UnknownAndInAction empty companion function returns instance with empty lists`() {
        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction.empty()

        // Assert
        assertFalse(partitioned.hasUnknownDurationInApps())
        assertFalse(partitioned.hasInActionInApps())
        assertTrue(partitioned.unknownDurationInApps.isEmpty())
        assertTrue(partitioned.inActionInApps.isEmpty())
    }

    @Test
    fun `UnknownAndInAction preserves order of in-apps in lists`() {
        // Arrange
        val unknown1 = createInApp("unknown1")
        val unknown2 = createInApp("unknown2")
        val unknownInApps = listOf(unknown1, unknown2)

        val inAction1 = createInApp("inaction1")
        val inAction2 = createInApp("inaction2")
        val inAction3 = createInApp("inaction3")
        val inActionInApps = listOf(inAction1, inAction2, inAction3)

        // Act
        val partitioned = DurationPartitionedInApps.UnknownAndInAction(unknownInApps, inActionInApps)

        // Assert
        assertEquals("unknown1", partitioned.unknownDurationInApps[0].getString("id"))
        assertEquals("unknown2", partitioned.unknownDurationInApps[1].getString("id"))
        assertEquals("inaction1", partitioned.inActionInApps[0].getString("id"))
        assertEquals("inaction2", partitioned.inActionInApps[1].getString("id"))
        assertEquals("inaction3", partitioned.inActionInApps[2].getString("id"))
    }


    // ==================== INACTION ONLY TESTS ====================

    @Test
    fun `InActionOnly hasInActionInApps returns true when inAction list is not empty`() {
        // Arrange
        val inActionInApps = listOf(createInApp("inaction1"), createInApp("inaction2"))

        // Act
        val partitioned = DurationPartitionedInApps.InActionOnly(inActionInApps)

        // Assert
        assertTrue(partitioned.hasInActionInApps())
    }

    @Test
    fun `InActionOnly hasInActionInApps returns false when inAction list is empty`() {
        // Arrange
        val inActionInApps = emptyList<JSONObject>()

        // Act
        val partitioned = DurationPartitionedInApps.InActionOnly(inActionInApps)

        // Assert
        assertFalse(partitioned.hasInActionInApps())
    }

    @Test
    fun `InActionOnly empty companion function returns instance with empty list`() {
        // Act
        val partitioned = DurationPartitionedInApps.InActionOnly.empty()

        // Assert
        assertFalse(partitioned.hasInActionInApps())
        assertTrue(partitioned.inActionInApps.isEmpty())
    }

    @Test
    fun `InActionOnly preserves order of in-apps in list`() {
        // Arrange
        val inAction1 = createInApp("inaction1")
        val inAction2 = createInApp("inaction2")
        val inAction3 = createInApp("inaction3")
        val inActionInApps = listOf(inAction1, inAction2, inAction3)

        // Act
        val partitioned = DurationPartitionedInApps.InActionOnly(inActionInApps)

        // Assert
        assertEquals(3, partitioned.inActionInApps.size)
        assertEquals("inaction1", partitioned.inActionInApps[0].getString("id"))
        assertEquals("inaction2", partitioned.inActionInApps[1].getString("id"))
        assertEquals("inaction3", partitioned.inActionInApps[2].getString("id"))
    }


    // ==================== SEALED CLASS HIERARCHY TESTS ====================

    @Test
    fun `all subclasses can be handled via when expression`() {
        // Arrange
        val partitions: List<DurationPartitionedInApps> = listOf(
            DurationPartitionedInApps.ImmediateAndDelayed.empty(),
            DurationPartitionedInApps.UnknownAndInAction.empty(),
            DurationPartitionedInApps.InActionOnly.empty()
        )

        // Act & Assert
        partitions.forEach { partition ->
            val result = when (partition) {
                is DurationPartitionedInApps.ImmediateAndDelayed -> "ImmediateAndDelayed"
                is DurationPartitionedInApps.UnknownAndInAction -> "UnknownAndInAction"
                is DurationPartitionedInApps.InActionOnly -> "InActionOnly"
            }
            assertTrue(result.isNotEmpty())
        }
    }

    // ==================== HELPER METHODS ====================

    private fun createInApp(id: String): JSONObject {
        return JSONObject().put("id", id)
    }
}