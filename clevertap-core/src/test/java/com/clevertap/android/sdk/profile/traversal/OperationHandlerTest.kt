package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OperationHandlerTest {

    private lateinit var changeTracker: ProfileChangeTracker
    private lateinit var arrayHandler: ArrayOperationHandler
    private lateinit var handler: OperationHandler

    @Before
    fun setup() {
        changeTracker = ProfileChangeTracker()
        arrayHandler = ArrayOperationHandler(changeTracker)
        handler = OperationHandler(changeTracker, arrayHandler)
    }

    // UPDATE Operation Tests
    @Test
    fun `handleOperation UPDATE updates simple value`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "name", "Jane", "name",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertEquals("Jane", target.getString("name"))
        assertTrue(changes.containsKey("name"))
        assertEquals("John", changes["name"]!!.oldValue)
        assertEquals("Jane", changes["name"]!!.newValue)
    }

    @Test
    fun `handleOperation UPDATE with identical value does nothing`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "name", "John", "name",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertEquals("John", target.getString("name"))
        assertFalse(changes.containsKey("name"))
    }

    @Test
    fun `handleOperation UPDATE with number value`() {
        val target = JSONObject().apply {
            put("count", 10)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 20, "count",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertEquals(20, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
    }

    @Test
    fun `handleOperation UPDATE with boolean value`() {
        val target = JSONObject().apply {
            put("active", false)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "active", true, "active",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertEquals(true, target.getBoolean("active"))
        assertTrue(changes.containsKey("active"))
    }

    @Test
    fun `handleOperation UPDATE with date prefix processes value`() {
        val target = JSONObject().apply {
            put("timestamp", 0)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "timestamp", "\$D_1609459200", "timestamp",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertEquals("\$D_1609459200", target.getString("timestamp"))
        assertTrue(changes.containsKey("timestamp"))
        assertEquals(1609459200L, changes["timestamp"]!!.newValue)
    }

    // Nested Object Tests
    @Test
    fun `handleOperation UPDATE with nested objects calls recursive apply`() {
        val target = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "John")
            })
        }
        val newValue = JSONObject().apply {
            put("name", "Jane")
        }
        val changes = mutableMapOf<String, ProfileChange>()
        var recursiveCalled = false
        var capturedOldValue: JSONObject? = null
        var capturedNewValue: JSONObject? = null
        var capturedPath = ""

        handler.handleOperation(
            target, "user", newValue, "user",
            changes, ProfileOperation.UPDATE
        ) { oldObj, newObj, path, _ ->
            recursiveCalled = true
            capturedOldValue = oldObj
            capturedNewValue = newObj
            capturedPath = path
        }

        assertTrue(recursiveCalled)
        assertNotNull(capturedOldValue)
        assertNotNull(capturedNewValue)
        assertEquals("user", capturedPath)
    }

    // Array Tests
    @Test
    fun `handleOperation UPDATE with arrays delegates to arrayHandler`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
            })
        }
        val newValue = JSONArray().apply {
            put("tag2")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "tags", newValue, "tags",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        // ArrayHandler should have replaced the array
        val resultArray = target.getJSONArray("tags")
        assertEquals(1, resultArray.length())
        assertEquals("tag2", resultArray.getString(0))
    }

    // INCREMENT Operation Tests
    @Test
    fun `handleOperation INCREMENT adds numbers`() {
        val target = JSONObject().apply {
            put("count", 10)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 5, "count",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(15, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
        assertEquals(10, changes["count"]!!.oldValue)
        assertEquals(15, changes["count"]!!.newValue)
    }

    @Test
    fun `handleOperation INCREMENT with floating point numbers`() {
        val target = JSONObject().apply {
            put("balance", 10.5)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "balance", 5.25, "balance",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(15.75, target.getDouble("balance"), 0.01)
        assertTrue(changes.containsKey("balance"))
    }

    @Test
    fun `handleOperation INCREMENT with zero does nothing`() {
        val target = JSONObject().apply {
            put("count", 10)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 0, "count",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(10, target.getInt("count"))
        assertFalse(changes.containsKey("count"))
    }

    @Test
    fun `handleOperation INCREMENT with non-number value does nothing`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "name", 5, "name",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals("John", target.getString("name"))
        assertFalse(changes.containsKey("name"))
    }

    @Test
    fun `handleOperation INCREMENT when oldValue is not number does nothing`() {
        val target = JSONObject().apply {
            put("value", "text")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "value", 10, "value",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals("text", target.getString("value"))
        assertFalse(changes.containsKey("value"))
    }

    // DECREMENT Operation Tests
    @Test
    fun `handleOperation DECREMENT subtracts numbers`() {
        val target = JSONObject().apply {
            put("count", 20)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 5, "count",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(15, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
        assertEquals(20, changes["count"]!!.oldValue)
        assertEquals(15, changes["count"]!!.newValue)
    }

    @Test
    fun `handleOperation DECREMENT with floating point numbers`() {
        val target = JSONObject().apply {
            put("balance", 20.75)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "balance", 5.25, "balance",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(15.5, target.getDouble("balance"), 0.01)
        assertTrue(changes.containsKey("balance"))
    }

    @Test
    fun `handleOperation DECREMENT can result in negative`() {
        val target = JSONObject().apply {
            put("count", 5)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 10, "count",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(-5, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
    }

    @Test
    fun `handleOperation DECREMENT with non-number value does nothing`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "name", 5, "name",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals("John", target.getString("name"))
        assertFalse(changes.containsKey("name"))
    }

    // GET Operation Tests
    @Test
    fun `handleOperation GET reports current value without modification`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "name", "__CLEVERTAP_GET__", "name",
            changes, ProfileOperation.GET
        ) { _, _, _, _ -> }

        assertEquals("John", target.getString("name")) // Not modified
        assertTrue(changes.containsKey("name"))
        assertEquals("John", changes["name"]!!.oldValue)
        assertEquals(Constants.GET_MARKER, changes["name"]!!.newValue)
    }

    @Test
    fun `handleOperation GET with number value`() {
        val target = JSONObject().apply {
            put("count", 42)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", "__CLEVERTAP_GET__", "count",
            changes, ProfileOperation.GET
        ) { _, _, _, _ -> }

        assertEquals(42, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
        assertEquals(42, changes["count"]!!.oldValue)
        assertEquals(Constants.GET_MARKER, changes["count"]!!.newValue)
    }

    @Test
    fun `handleOperation GET with nested objects calls recursive apply`() {
        val target = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "John")
            })
        }
        val getSpec = JSONObject().apply {
            put("name", "__CLEVERTAP_GET__")
        }
        val changes = mutableMapOf<String, ProfileChange>()
        var recursiveCalled = false

        handler.handleOperation(
            target, "user", getSpec, "user",
            changes, ProfileOperation.GET
        ) { _, _, _, _ ->
            recursiveCalled = true
        }

        assertTrue(recursiveCalled)
    }

    // Missing Key Tests
    @Test
    fun `handleOperation UPDATE with missing key adds new value`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "newKey", "newValue", "newKey",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertTrue(target.has("newKey"))
        assertEquals("newValue", target.getString("newKey"))
        assertTrue(changes.containsKey("newKey"))
        assertNull(changes["newKey"]!!.oldValue)
        assertEquals("newValue", changes["newKey"]!!.newValue)
    }

    @Test
    fun `handleOperation INCREMENT with missing key adds value as-is`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 10, "count",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertTrue(target.has("count"))
        assertEquals(10, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
        assertNull(changes["count"]!!.oldValue)
        assertEquals(10, changes["count"]!!.newValue)
    }

    @Test
    fun `handleOperation DECREMENT with missing key adds negated value`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "count", 10, "count",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertTrue(target.has("count"))
        assertEquals(-10, target.getInt("count"))
        assertTrue(changes.containsKey("count"))
        assertNull(changes["count"]!!.oldValue)
        assertEquals(-10, changes["count"]!!.newValue)
    }

    @Test
    fun `handleOperation DECREMENT with missing key and non-number does nothing`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "value", "text", "value",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertFalse(target.has("value"))
        assertFalse(changes.containsKey("value"))
    }

    @Test
    fun `handleOperation INCREMENT with missing key and non-number does nothing`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "value", "text", "value",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertFalse(target.has("value"))
        assertFalse(changes.containsKey("value"))
    }

    @Test
    fun `handleOperation GET with missing key does nothing`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "missing", "__CLEVERTAP_GET__", "missing",
            changes, ProfileOperation.GET
        ) { _, _, _, _ -> }

        assertFalse(target.has("missing"))
        assertFalse(changes.containsKey("missing"))
    }

    @Test
    fun `handleOperation ARRAY_REMOVE with missing key does nothing`() {
        val target = JSONObject()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "tags", JSONArray().apply { put("item") }, "tags",
            changes, ProfileOperation.ARRAY_REMOVE
        ) { _, _, _, _ -> }

        assertFalse(target.has("tags"))
        assertFalse(changes.containsKey("tags"))
    }

    // Edge Cases
    @Test
    fun `handleOperation with ARRAY_ADD delegates to arrayHandler`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
            })
        }
        val newValue = JSONArray().apply {
            put("tag2")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "tags", newValue, "tags",
            changes, ProfileOperation.ARRAY_ADD
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("tags")
        assertEquals(2, resultArray.length())
        assertEquals("tag1", resultArray.getString(0))
        assertEquals("tag2", resultArray.getString(1))
    }

    @Test
    fun `handleOperation INCREMENT with mixed integer and double`() {
        val target = JSONObject().apply {
            put("value", 10)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "value", 5.5, "value",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(15.5, target.getDouble("value"), 0.01)
        assertTrue(changes.containsKey("value"))
    }

    @Test
    fun `handleOperation DECREMENT with mixed double and integer`() {
        val target = JSONObject().apply {
            put("value", 10.5)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "value", 5, "value",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(5.5, target.getDouble("value"), 0.01)
        assertTrue(changes.containsKey("value"))
    }


    @Test
    fun `handleOperation DECREMENT with floating point precision`() {
        val target = JSONObject().apply {
            put("balance", 100.01)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleOperation(
            target, "balance", 50.005, "balance",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(50.005, target.getDouble("balance"), 0.0001)
        assertTrue(changes.containsKey("balance"))
    }
}
