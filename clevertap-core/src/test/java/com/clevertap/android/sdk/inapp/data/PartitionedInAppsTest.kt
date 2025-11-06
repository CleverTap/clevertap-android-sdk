package com.clevertap.android.sdk.inapp.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PartitionedInAppsTest {

    private fun createSampleInApp(id: String, delay: Int? = null): JSONObject {
        return JSONObject().apply {
            put("ti", id)
            put("wzrk_id", "wzrk_$id")
            put("type", "interstitial")
            if (delay != null) {
                put("delayAfterTrigger", delay)
            }
        }
    }

    private fun createInAppsArray(vararg inApps: JSONObject): JSONArray {
        return JSONArray().apply {
            inApps.forEach { put(it) }
        }
    }

    // ============================================
    // CONSTRUCTOR TESTS
    // ============================================

    @Test
    fun `test constructor with empty arrays`() {
        // Given
        val immediateInApps = JSONArray()
        val delayedInApps = JSONArray()

        // When
        val partitioned = PartitionedInApps(immediateInApps, delayedInApps)

        // Then
        assertNotNull(partitioned)
        assertEquals(0, partitioned.immediateInApps.length())
        assertEquals(0, partitioned.delayedInApps.length())
    }

    @Test
    fun `test constructor with populated arrays`() {
        // Given
        val immediateInApps = createInAppsArray(
            createSampleInApp("inapp1"),
            createSampleInApp("inapp2")
        )
        val delayedInApps = createInAppsArray(
            createSampleInApp("inapp3", 300)
        )

        // When
        val partitioned = PartitionedInApps(immediateInApps, delayedInApps)

        // Then
        assertEquals(2, partitioned.immediateInApps.length())
        assertEquals(1, partitioned.delayedInApps.length())
    }

    // ============================================
    // PROPERTY TESTS - hasImmediateInApps
    // ============================================

    @Test
    fun `test hasImmediateInApps returns false when array is empty`() {
        // Given
        val partitioned = PartitionedInApps(JSONArray(), JSONArray())

        // When & Then
        assertFalse(partitioned.hasImmediateInApps)
    }

    @Test
    fun `test hasImmediateInApps returns true when array has one item`() {
        // Given
        val immediateInApps = createInAppsArray(createSampleInApp("inapp1"))
        val partitioned = PartitionedInApps(immediateInApps, JSONArray())

        // When & Then
        assertTrue(partitioned.hasImmediateInApps)
    }

    @Test
    fun `test hasImmediateInApps returns true when array has multiple items`() {
        // Given
        val immediateInApps = createInAppsArray(
            createSampleInApp("inapp1"),
            createSampleInApp("inapp2"),
            createSampleInApp("inapp3")
        )
        val partitioned = PartitionedInApps(immediateInApps, JSONArray())

        // When & Then
        assertTrue(partitioned.hasImmediateInApps)
    }

    // ============================================
    // PROPERTY TESTS - hasDelayedInApps
    // ============================================

    @Test
    fun `test hasDelayedInApps returns false when array is empty`() {
        // Given
        val partitioned = PartitionedInApps(JSONArray(), JSONArray())

        // When & Then
        assertFalse(partitioned.hasDelayedInApps)
    }

    @Test
    fun `test hasDelayedInApps returns true when array has one item`() {
        // Given
        val delayedInApps = createInAppsArray(createSampleInApp("inapp1", 300))
        val partitioned = PartitionedInApps(JSONArray(), delayedInApps)

        // When & Then
        assertTrue(partitioned.hasDelayedInApps)
    }

    @Test
    fun `test hasDelayedInApps returns true when array has multiple items`() {
        // Given
        val delayedInApps = createInAppsArray(
            createSampleInApp("inapp1", 300),
            createSampleInApp("inapp2", 600),
            createSampleInApp("inapp3", 900)
        )
        val partitioned = PartitionedInApps(JSONArray(), delayedInApps)

        // When & Then
        assertTrue(partitioned.hasDelayedInApps)
    }

    // ============================================
    // COMBINATION TESTS
    // ============================================

    @Test
    fun `test both hasImmediateInApps and hasDelayedInApps can be true`() {
        // Given
        val immediateInApps = createInAppsArray(createSampleInApp("inapp1"))
        val delayedInApps = createInAppsArray(createSampleInApp("inapp2", 300))
        val partitioned = PartitionedInApps(immediateInApps, delayedInApps)

        // When & Then
        assertTrue(partitioned.hasImmediateInApps)
        assertTrue(partitioned.hasDelayedInApps)
    }

    @Test
    fun `test both hasImmediateInApps and hasDelayedInApps can be false`() {
        // Given
        val partitioned = PartitionedInApps(JSONArray(), JSONArray())

        // When & Then
        assertFalse(partitioned.hasImmediateInApps)
        assertFalse(partitioned.hasDelayedInApps)
    }

    // ============================================
    // COMPANION OBJECT - empty() TESTS
    // ============================================

    @Test
    fun `test empty factory method creates instance with empty arrays`() {
        // When
        val partitioned = PartitionedInApps.empty()

        // Then
        assertNotNull(partitioned)
        assertEquals(0, partitioned.immediateInApps.length())
        assertEquals(0, partitioned.delayedInApps.length())
    }

    @Test
    fun `test empty factory method sets hasImmediateInApps to false`() {
        // When
        val partitioned = PartitionedInApps.empty()

        // Then
        assertFalse(partitioned.hasImmediateInApps)
    }

    @Test
    fun `test empty factory method sets hasDelayedInApps to false`() {
        // When
        val partitioned = PartitionedInApps.empty()

        // Then
        assertFalse(partitioned.hasDelayedInApps)
    }

    @Test
    fun `test multiple calls to empty create independent instances`() {
        // When
        val partitioned1 = PartitionedInApps.empty()
        val partitioned2 = PartitionedInApps.empty()

        // Then - Different instances
        assertFalse(partitioned1 === partitioned2)
        assertFalse(partitioned1.immediateInApps === partitioned2.immediateInApps)
        assertFalse(partitioned1.delayedInApps === partitioned2.delayedInApps)

        // When - Modify one
        partitioned1.immediateInApps.put(createSampleInApp("inapp1"))

        // Then - Other is unaffected
        assertTrue(partitioned1.hasImmediateInApps)
        assertFalse(partitioned2.hasImmediateInApps)
    }
}