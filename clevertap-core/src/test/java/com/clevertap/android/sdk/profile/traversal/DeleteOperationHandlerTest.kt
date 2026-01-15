package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DeleteOperationHandlerTest {

    private lateinit var changeTracker: ProfileChangeTracker
    private lateinit var handler: DeleteOperationHandler

    @Before
    fun setup() {
        changeTracker = ProfileChangeTracker()
        handler = DeleteOperationHandler(changeTracker)
    }

    // Simple Deletion Tests
    @Test
    fun `handleDelete removes key with delete marker`() {
        val target = JSONObject().apply {
            put("name", "John")
            put("age", 30)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "name", Constants.DELETE_MARKER, "name",
            changes
        ) { _, _, _, _ -> }

        assertFalse(target.has("name"))
        assertTrue(target.has("age"))
        assertTrue(changes.containsKey("name"))
        assertEquals("John", changes["name"]!!.oldValue)
        assertNull(changes["name"]!!.newValue)
    }

    @Test
    fun `handleDelete with non-existent key does nothing`() {
        val target = JSONObject().apply {
            put("name", "John")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "missing", Constants.DELETE_MARKER, "missing",
            changes
        ) { _, _, _, _ -> }

        assertTrue(target.has("name"))
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `handleDelete with number value`() {
        val target = JSONObject().apply {
            put("count", 42)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "count", Constants.DELETE_MARKER, "count",
            changes
        ) { _, _, _, _ -> }

        assertFalse(target.has("count"))
        assertTrue(changes.containsKey("count"))
        assertEquals(42, changes["count"]!!.oldValue)
    }

    @Test
    fun `handleDelete with boolean value`() {
        val target = JSONObject().apply {
            put("active", true)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "active", Constants.DELETE_MARKER, "active",
            changes
        ) { _, _, _, _ -> }

        assertFalse(target.has("active"))
        assertTrue(changes.containsKey("active"))
        assertEquals(true, changes["active"]!!.oldValue)
    }

    @Test
    fun `handleDelete with date prefix value`() {
        val target = JSONObject().apply {
            put("created", "\$D_1609459200")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "created", Constants.DELETE_MARKER, "created",
            changes
        ) { _, _, _, _ -> }

        assertFalse(target.has("created"))
        assertTrue(changes.containsKey("created"))
        assertEquals(1609459200L, changes["created"]!!.oldValue)
    }

    // Nested Object Deletion Tests
    @Test
    fun `handleDelete recursively deletes nested fields`() {
        val target = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "John")
                put("age", 30)
                put("email", "[email protected]")
            })
        }
        val deleteSpec = JSONObject().apply {
            put("name", Constants.DELETE_MARKER)
            put("age", Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()
        var recursiveCalled = false

        handler.handleDelete(
            target, "user", deleteSpec, "user",
            changes
        ) { targetObj, sourceObj, _, _ ->
            recursiveCalled = true
            // Simulate recursive deletion
            val userObj = targetObj
            userObj.remove("name")
            userObj.remove("age")
        }

        assertTrue(recursiveCalled)
        val userObj = target.getJSONObject("user")
        assertFalse(userObj.has("name"))
        assertFalse(userObj.has("age"))
        assertTrue(userObj.has("email")) // Not deleted
    }

    @Test
    fun `handleDelete removes empty nested objects after deletion`() {
        val target = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "John")
            })
        }
        val deleteSpec = JSONObject().apply {
            put("name", Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "user", deleteSpec, "user",
            changes
        ) { targetObj, _, _, _ ->
            targetObj.remove("name")
        }

        assertFalse(target.has("user")) // Empty object removed
    }

    @Test
    fun `handleDelete does not remove JSONObject with delete marker`() {
        val target = JSONObject().apply {
            put("config", JSONObject().apply {
                put("theme", "dark")
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "config", Constants.DELETE_MARKER, "config",
            changes
        ) { _, _, _, _ -> }

        assertTrue(target.has("config")) // JSONObject not deleted even with marker
        assertTrue(changes.isEmpty())
    }

    // Array Deletion Tests - Delete Elements
    @Test
    fun `handleDelete removes array elements with delete markers`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
                put("tag2")
                put("tag3")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "tags", deleteSpec, "tags",
            changes
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("tags")
        assertEquals(1, resultArray.length())
        assertEquals("tag3", resultArray.getString(0))
        assertTrue(changes.containsKey("tags"))
    }

    @Test
    fun `handleDelete skips JSONObject elements in array deletion`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put("simple")
                put(JSONObject().apply { put("complex", "value") })
                put("another")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("items")
        assertEquals(1, resultArray.length()) // Only JSONObject remains
        assertTrue(resultArray.getJSONObject(0).has("complex"))
    }

    @Test
    fun `handleDelete skips JSONArray elements in array deletion`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put("simple")
                put(JSONArray().apply { put("nested") })
                put("another")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("items")
        assertEquals(1, resultArray.length()) // Only nested JSONArray remains
    }

    @Test
    fun `handleDelete with out of bounds indices skips them`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
            put(Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "tags", deleteSpec, "tags",
            changes
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("tags")
        assertEquals(0, resultArray.length())
    }

    @Test
    fun `handleDelete with no delete markers in array does nothing`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
                put("tag2")
            })
        }
        val deleteSpec = JSONArray().apply {
            put("normalValue")
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "tags", deleteSpec, "tags",
            changes
        ) { _, _, _, _ -> }

        val resultArray = target.getJSONArray("tags")
        assertEquals(2, resultArray.length())
    }

    // Array Deletion Tests - Delete Fields from Elements
    @Test
    fun `handleDelete removes fields from JSONObject array elements`() {
        val target = JSONObject().apply {
            put("users", JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "John")
                    put("age", 30)
                    put("email", "[email protected]")
                })
                put(JSONObject().apply {
                    put("name", "Jane")
                    put("age", 25)
                })
            })
        }
        val deleteSpec = JSONArray().apply {
            put(JSONObject().apply {
                put("name", Constants.DELETE_MARKER)
            })
            put(JSONObject().apply {
                put("age", Constants.DELETE_MARKER)
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "users", deleteSpec, "users",
            changes
        ) { _, _, _, _ -> }

        val users = target.getJSONArray("users")
        assertEquals(2, users.length())
        
        val user1 = users.getJSONObject(0)
        assertFalse(user1.has("name")) // Deleted
        assertTrue(user1.has("age")) // Not deleted
        assertTrue(user1.has("email")) // Not deleted

        val user2 = users.getJSONObject(1)
        assertTrue(user2.has("name")) // Not deleted
        assertFalse(user2.has("age")) // Deleted
    }

    @Test
    fun `handleDelete removes empty objects from array after field deletion`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put(JSONObject().apply {
                    put("temp", "value")
                })
            })
        }
        val deleteSpec = JSONArray().apply {
            put(JSONObject().apply {
                put("temp", Constants.DELETE_MARKER)
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        val items = target.getJSONArray("items")
        assertEquals(0, items.length()) // Empty object removed
    }

    @Test
    fun `handleDelete with mismatched array element types does nothing`() {
        val target = JSONObject().apply {
            put("data", JSONArray().apply {
                put("string")
                put(123)
            })
        }
        val deleteSpec = JSONArray().apply {
            put(JSONObject().apply {
                put("field", Constants.DELETE_MARKER)
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "data", deleteSpec, "data",
            changes
        ) { _, _, _, _ -> }

        val data = target.getJSONArray("data")
        assertEquals(2, data.length()) // Nothing deleted
    }

    @Test
    fun `handleDelete with nested objects in array elements`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put(JSONObject().apply {
                    put("user", JSONObject().apply {
                        put("name", "John")
                        put("age", 30)
                    })
                })
            })
        }
        val deleteSpec = JSONArray().apply {
            put(JSONObject().apply {
                put("user", JSONObject().apply {
                    put("name", Constants.DELETE_MARKER)
                })
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        val items = target.getJSONArray("items")
        val user = items.getJSONObject(0).getJSONObject("user")
        assertFalse(user.has("name"))
        assertTrue(user.has("age"))
    }

    // Edge Cases
    @Test
    fun `handleDelete with empty array does nothing`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
            })
        }
        val deleteSpec = JSONArray()
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "tags", deleteSpec, "tags",
            changes
        ) { _, _, _, _ -> }

        val tags = target.getJSONArray("tags")
        assertEquals(1, tags.length())
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `handleDelete does not remove JSONArray with delete marker`() {
        val target = JSONObject().apply {
            put("list", JSONArray().apply {
                put("item")
            })
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "list", Constants.DELETE_MARKER, "list",
            changes
        ) { _, _, _, _ -> }

        assertTrue(target.has("list")) // JSONArray not deleted even with marker
        assertTrue(changes.isEmpty())
    }

    @Test
    fun `handleDelete with mixed delete strategies prefers element deletion`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put("item1")
                put("item2")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER) // Delete marker
            put(JSONObject()) // Empty object (field deletion spec)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        // Should perform element deletion since it has delete markers
        val items = target.getJSONArray("items")
        assertEquals(1, items.length())
        assertEquals("item2", items.getString(0))
    }

    @Test
    fun `handleDelete records changes for array modifications`() {
        val target = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
                put("tag2")
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "tags", deleteSpec, "tags",
            changes
        ) { _, _, _, _ -> }

        assertTrue(changes.containsKey("tags"))
        val oldArray = changes["tags"]!!.oldValue as JSONArray
        val newArray = changes["tags"]!!.newValue as JSONArray
        assertEquals(2, oldArray.length())
        assertEquals(1, newArray.length())
    }

    @Test
    fun `handleDelete does not record changes when no array elements deleted`() {
        val target = JSONObject().apply {
            put("items", JSONArray().apply {
                put(JSONObject().apply { put("key", "value") })
            })
        }
        val deleteSpec = JSONArray().apply {
            put(Constants.DELETE_MARKER) // Try to delete JSONObject (not allowed)
        }
        val changes = mutableMapOf<String, ProfileChange>()

        handler.handleDelete(
            target, "items", deleteSpec, "items",
            changes
        ) { _, _, _, _ -> }

        assertFalse(changes.containsKey("items"))
    }
}
