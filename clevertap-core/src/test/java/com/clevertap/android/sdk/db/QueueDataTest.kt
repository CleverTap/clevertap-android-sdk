package com.clevertap.android.sdk.db

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueDataTest : BaseTestCase() {

    private lateinit var queueData: QueueData

    @Before
    fun setup() {
        queueData = QueueData()
    }

    // ============= Tests for isEmpty property =============

    @Test
    fun test_isEmpty_when_DataIsEmptyJSONArray_should_ReturnTrue() {
        // Test & Validate
        assertTrue(queueData.isEmpty, "QueueData should be empty when data is an empty JSONArray")
    }

    @Test
    fun test_isEmpty_when_DataHasElements_should_ReturnFalse() {
        // Setup
        queueData.data.put(JSONObject().put("key", "value"))

        // Test & Validate
        assertFalse(queueData.isEmpty, "QueueData should not be empty when data has elements")
    }

    // ============= Tests for hasEvents property =============

    @Test
    fun test_hasEvents_when_EventIdsIsEmpty_should_ReturnFalse() {
        // Setup
        queueData.eventIds.clear()
        
        // Test & Validate
        assertFalse(queueData.hasEvents, "hasEvents should be false when eventIds is empty")
    }

    @Test
    fun test_hasEvents_when_EventIdsHasElements_should_ReturnTrue() {
        // Setup
        queueData.eventIds.apply {
            add("1")
            add("2")
            add("3")
        }
        
        // Test & Validate
        assertTrue(queueData.hasEvents, "hasEvents should be true when eventIds has elements")
    }

    // ============= Tests for hasProfileEvents property =============

    @Test
    fun test_hasProfileEvents_when_ProfileEventIdsIsEmpty_should_ReturnFalse() {
        // Test & Validate
        assertFalse(queueData.hasProfileEvents, "hasProfileEvents should be false when profileEventIds is empty")
    }

    @Test
    fun test_hasProfileEvents_when_ProfileEventIdsHasElements_should_ReturnTrue() {
        // Setup
        queueData.profileEventIds.apply {
            add("p1")
            add("p2")
        }
        
        // Test & Validate
        assertTrue(queueData.hasProfileEvents, "hasProfileEvents should be true when profileEventIds has elements")
    }

    // ============= Tests for data and ID lists =============

    @Test
    fun test_QueueData_when_Initialized_should_HaveDefaultValues() {
        // Test
        val newQueueData = QueueData()
        
        // Validate
        assertEquals(JSONArray().toString(), newQueueData.data.toString(), "Empty data by default")
        assertEquals(0, newQueueData.eventIds.size, "EventIds should be empty by default")
        assertEquals(0, newQueueData.profileEventIds.size, "ProfileEventIds should be empty by default")
        assertTrue(newQueueData.isEmpty, "Should be empty by default")
        assertFalse(newQueueData.hasEvents, "Should have no events by default")
        assertFalse(newQueueData.hasProfileEvents, "Should have no profile events by default")
    }

    @Test
    fun test_QueueData_when_FullyPopulated_should_ReflectCorrectState() {
        // Setup
        queueData.data.put(JSONObject().put("name", "event1"))
        queueData.data.put(JSONObject().put("name", "event2"))
        queueData.data.put(JSONObject().put("name", "profile1"))
        
        queueData.eventIds.apply {
            add("1")
            add("2")
        }
        queueData.profileEventIds.add("3")
        
        // Validate
        assertFalse(queueData.isEmpty, "Should not be empty when data exists")
        assertTrue(queueData.hasEvents, "Should have events")
        assertTrue(queueData.hasProfileEvents, "Should have profile events")
        assertEquals(3, queueData.data.length(), "Should have 3 items in data")
        assertEquals(2, queueData.eventIds.size, "Should have 2 event IDs")
        assertEquals(1, queueData.profileEventIds.size, "Should have 1 profile event ID")
    }

    @Test
    fun test_QueueData_when_OnlyEventsNoProfiles_should_ReflectCorrectState() {
        // Setup
        queueData.data.put(JSONObject().put("name", "event1"))
        queueData.data.put(JSONObject().put("name", "event2"))
        
        queueData.eventIds.apply {
            add("1")
            add("2")
        }
        queueData.profileEventIds.clear()
        
        // Validate
        assertFalse(queueData.isEmpty, "Should not be empty")
        assertTrue(queueData.hasEvents, "Should have events")
        assertFalse(queueData.hasProfileEvents, "Should not have profile events")
    }

    @Test
    fun test_QueueData_when_OnlyProfilesNoEvents_should_ReflectCorrectState() {
        // Setup
        queueData.data.put(JSONObject().put("name", "profile1"))
        queueData.data.put(JSONObject().put("name", "profile2"))
        
        queueData.eventIds.clear()
        queueData.profileEventIds.apply {
            add("p1")
            add("p2")
        }
        
        // Validate
        assertFalse(queueData.isEmpty, "Should not be empty")
        assertFalse(queueData.hasEvents, "Should not have events")
        assertTrue(queueData.hasProfileEvents, "Should have profile events")
    }

    // ============= Tests for toString() method =============

    @Test
    fun test_toString_when_Empty_should_ReturnCorrectString() {

        // Test
        val result = queueData.toString()

        // Validate
        assertTrue(result.contains("numItems=0"), "Should show 0 items")
        assertTrue(result.contains("eventIds=0"), "Should show 0 event IDs")
        assertTrue(result.contains("profileEventIds=0"), "Should show 0 profile event IDs")
    }

    @Test
    fun test_toString_when_HasData_should_ReturnCorrectString() {
        // Setup
        queueData.data.put(JSONObject().put("test", "value1"))
        queueData.data.put(JSONObject().put("test", "value2"))

        queueData.eventIds.apply {
            add("1")
            add("2")
            add("3")
        }
        queueData.profileEventIds.add("p1")

        // Test
        val result = queueData.toString()

        // Validate
        assertTrue(result.contains("numItems=2"), "Should show 2 items")
        assertTrue(result.contains("eventIds=3"), "Should show 3 event IDs")
        assertTrue(result.contains("profileEventIds=1"), "Should show 1 profile event ID")
    }

    // ============= Tests for edge cases =============

    @Test
    fun test_QueueData_when_LargeDataSet_should_HandleCorrectly() {

        // Add 100 events
        for (i in 1..100) {
            queueData.data.put(JSONObject().put("id", i))
            if (i <= 80) {
                queueData.eventIds.add("e$i")
            } else {
                queueData.profileEventIds.add("p$i")
            }
        }

        // Validate
        assertEquals(100, queueData.data.length(), "Should have 100 items")
        assertEquals(80, queueData.eventIds.size, "Should have 80 event IDs")
        assertEquals(20, queueData.profileEventIds.size, "Should have 20 profile event IDs")
        assertFalse(queueData.isEmpty, "Should not be empty")
        assertTrue(queueData.hasEvents, "Should have events")
        assertTrue(queueData.hasProfileEvents, "Should have profile events")
    }
}
