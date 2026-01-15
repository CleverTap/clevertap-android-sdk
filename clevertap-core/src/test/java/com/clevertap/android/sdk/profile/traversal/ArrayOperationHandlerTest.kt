package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArrayOperationHandlerTest {

    private lateinit var changeTracker: ProfileChangeTracker
    private lateinit var handler: ArrayOperationHandler

    @Before
    fun setup() {
        changeTracker = ProfileChangeTracker()
        handler = ArrayOperationHandler(changeTracker)
    }

    // ARRAY_ADD Tests
    @Test
    fun `handleArrayOperation ARRAY_ADD adds string values`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put("item2")
        }
        val newArray = JSONArray().apply {
            put("item3")
            put("item4")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_ADD
        ) { _, _, _, _ -> }

        assertEquals(4, oldArray.length())
        assertEquals("item1", oldArray.getString(0))
        assertEquals("item2", oldArray.getString(1))
        assertEquals("item3", oldArray.getString(2))
        assertEquals("item4", oldArray.getString(3))
        assertTrue(changes.containsKey("tags"))
    }

    @Test
    fun `handleArrayOperation ARRAY_ADD ignores non-string values`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
        }
        val newArray = JSONArray().apply {
            put(123)
            put(true)
            put(JSONObject())
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_ADD
        ) { _, _, _, _ -> }

        assertEquals(1, oldArray.length())
        assertEquals("item1", oldArray.getString(0))
        assertFalse(changes.containsKey("tags"))
    }

    @Test
    fun `handleArrayOperation ARRAY_ADD with empty array does nothing`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply { put("item1") }
        val newArray = JSONArray()
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_ADD
        ) { _, _, _, _ -> }

        assertEquals(1, oldArray.length())
        assertFalse(changes.containsKey("tags"))
    }

    @Test
    fun `handleArrayOperation ARRAY_ADD allows duplicates`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
        }
        val newArray = JSONArray().apply {
            put("item1")
            put("item1")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_ADD
        ) { _, _, _, _ -> }

        assertEquals(3, oldArray.length())
        assertEquals("item1", oldArray.getString(0))
        assertEquals("item1", oldArray.getString(1))
        assertEquals("item1", oldArray.getString(2))
    }

    // ARRAY_REMOVE Tests
    @Test
    fun `handleArrayOperation ARRAY_REMOVE removes string values`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put("item2")
            put("item3")
        }
        val newArray = JSONArray().apply {
            put("item2")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_REMOVE
        ) { _, _, _, _ -> }

        val resultArray = parent.getJSONArray("tags")
        assertEquals(2, resultArray.length())
        assertEquals("item1", resultArray.getString(0))
        assertEquals("item3", resultArray.getString(1))
        assertTrue(changes.containsKey("tags"))
    }

    @Test
    fun `handleArrayOperation ARRAY_REMOVE preserves non-string values`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put(123)
            put("item2")
        }
        val newArray = JSONArray().apply {
            put("item1")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_REMOVE
        ) { _, _, _, _ -> }

        val resultArray = parent.getJSONArray("tags")
        assertEquals(2, resultArray.length())
        assertEquals(123, resultArray.getInt(0))
        assertEquals("item2", resultArray.getString(1))
    }

    @Test
    fun `handleArrayOperation ARRAY_REMOVE with no matches does nothing`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put("item2")
        }
        val newArray = JSONArray().apply {
            put("item3")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.ARRAY_REMOVE
        ) { _, _, _, _ -> }

        val resultArray = parent.getJSONArray("tags")
        assertEquals(2, resultArray.length())
        assertFalse(changes.containsKey("tags"))
    }

    // UPDATE Tests
    @Test
    fun `handleArrayOperation UPDATE replaces entire array`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("old1")
            put("old2")
        }
        val newArray = JSONArray().apply {
            put("new1")
            put("new2")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        val resultArray = parent.getJSONArray("tags")
        assertEquals(2, resultArray.length())
        assertEquals("new1", resultArray.getString(0))
        assertEquals("new2", resultArray.getString(1))
        assertTrue(changes.containsKey("tags"))
    }

    @Test
    fun `handleArrayOperation UPDATE with identical arrays does nothing`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put("item2")
        }
        val newArray = JSONArray().apply {
            put("item1")
            put("item2")
        }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.UPDATE
        ) { _, _, _, _ -> }

        assertFalse(changes.containsKey("tags"))
    }

    // INCREMENT Tests
    @Test
    fun `handleArrayOperation INCREMENT adds numbers`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(10)
            put(20)
            put(30)
        }
        val newArray = JSONArray().apply {
            put(5)
            put(10)
        }
        parent.put("scores", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "scores", oldArray, newArray, "scores",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(15, oldArray.getInt(0))
        assertEquals(30, oldArray.getInt(1))
        assertEquals(30, oldArray.getInt(2)) // Unchanged
        assertTrue(changes.containsKey("scores"))
    }

    @Test
    fun `handleArrayOperation INCREMENT with JSONObject calls recursive traversal`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(JSONObject().apply {
                put("count", 10)
            })
        }
        val newArray = JSONArray().apply {
            put(JSONObject().apply {
                put("count", 5)
            })
        }
        parent.put("items", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()
        var recursiveCalled = false

        handler.handleArrayOperation(
            parent, "items", oldArray, newArray, "items",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ ->
            recursiveCalled = true
        }

        assertTrue(recursiveCalled)
    }

    @Test
    fun `handleArrayOperation INCREMENT beyond array length does not extend`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(10)
        }
        val newArray = JSONArray().apply {
            put(5)
            put(10)
            put(15)
        }
        parent.put("scores", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "scores", oldArray, newArray, "scores",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(1, oldArray.length())
        assertEquals(15, oldArray.getInt(0))
    }

    // DECREMENT Tests
    @Test
    fun `handleArrayOperation DECREMENT subtracts numbers`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(100)
            put(50)
        }
        val newArray = JSONArray().apply {
            put(30)
            put(10)
        }
        parent.put("scores", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "scores", oldArray, newArray, "scores",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(70, oldArray.getInt(0))
        assertEquals(40, oldArray.getInt(1))
        assertTrue(changes.containsKey("scores"))
    }

    @Test
    fun `handleArrayOperation INCREMENT with no changes does not record`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(10)
        }
        val newArray = JSONArray().apply {
            put(0)
        }
        parent.put("scores", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "scores", oldArray, newArray, "scores",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(10, oldArray.getInt(0))
        assertFalse(changes.containsKey("scores"))
    }

    // GET Tests
    @Test
    fun `handleArrayOperation GET reports primitive elements`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
            put(123)
            put(true)
        }
        val newArray = JSONArray().apply {
            put("__CLEVERTAP_GET__")
            put("__CLEVERTAP_GET__")
            put("__CLEVERTAP_GET__")
        }
        parent.put("data", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "data", oldArray, newArray, "data",
            changes, ProfileOperation.GET
        ) { _, _, _, _ -> }

        assertEquals(3, changes.size)
        assertTrue(changes.containsKey("data[0]"))
        assertTrue(changes.containsKey("data[1]"))
        assertTrue(changes.containsKey("data[2]"))
        assertEquals("item1", changes["data[0]"]!!.oldValue)
        assertEquals(Constants.GET_MARKER, changes["data[0]"]!!.newValue)
        assertEquals(123, changes["data[1]"]!!.oldValue)
        assertEquals(true, changes["data[2]"]!!.oldValue)
    }

    @Test
    fun `handleArrayOperation GET with JSONObject calls recursive traversal`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "John")
            })
        }
        val newArray = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "__CLEVERTAP_GET__")
            })
        }
        parent.put("users", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()
        var recursiveCalled = false
        var capturedPath = ""

        handler.handleArrayOperation(
            parent, "users", oldArray, newArray, "users",
            changes, ProfileOperation.GET
        ) { _, _, path, _ ->
            recursiveCalled = true
            capturedPath = path
        }

        assertTrue(recursiveCalled)
        assertEquals("users[0]", capturedPath)
    }

    @Test
    fun `handleArrayOperation GET beyond array length skips elements`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put("item1")
        }
        val newArray = JSONArray().apply {
            put("__CLEVERTAP_GET__")
            put("__CLEVERTAP_GET__")
            put("__CLEVERTAP_GET__")
        }
        parent.put("data", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "data", oldArray, newArray, "data",
            changes, ProfileOperation.GET
        ) { _, _, _, _ -> }

        assertEquals(1, changes.size)
        assertTrue(changes.containsKey("data[0]"))
    }

    // Edge Cases
    @Test
    fun `handleArrayOperation with unsupported operation does nothing`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply { put("item1") }
        val newArray = JSONArray().apply { put("item2") }
        parent.put("tags", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "tags", oldArray, newArray, "tags",
            changes, ProfileOperation.DELETE
        ) { _, _, _, _ -> }

        assertTrue(changes.isEmpty())
    }

    @Test
    fun `handleArrayOperation INCREMENT with mixed types only processes numbers`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(10)
            put("text")
            put(20)
        }
        val newArray = JSONArray().apply {
            put(5)
            put(100)
            put(10)
        }
        parent.put("mixed", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "mixed", oldArray, newArray, "mixed",
            changes, ProfileOperation.INCREMENT
        ) { _, _, _, _ -> }

        assertEquals(15, oldArray.getInt(0))
        assertEquals("text", oldArray.getString(1)) // Unchanged
        assertEquals(30, oldArray.getInt(2))
    }

    @Test
    fun `handleArrayOperation DECREMENT with floating point numbers`() {
        val parent = JSONObject()
        val oldArray = JSONArray().apply {
            put(10.5)
            put(20.7)
        }
        val newArray = JSONArray().apply {
            put(5.2)
            put(10.3)
        }
        parent.put("scores", oldArray)
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleArrayOperation(
            parent, "scores", oldArray, newArray, "scores",
            changes, ProfileOperation.DECREMENT
        ) { _, _, _, _ -> }

        assertEquals(5.3, oldArray.getDouble(0), 0.01)
        assertEquals(10.4, oldArray.getDouble(1), 0.01)
        assertTrue(changes.containsKey("scores"))
    }
}
