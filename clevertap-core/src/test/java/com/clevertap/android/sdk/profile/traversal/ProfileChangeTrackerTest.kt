package com.clevertap.android.sdk.profile.traversal

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProfileChangeTrackerTest {

    private val tracker = ProfileChangeTracker()

    @Test
    fun `recordChange records simple value change`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("name", "oldName", "newName", changes)

        assertEquals(1, changes.size)
        assertTrue(changes.containsKey("name"))
        assertEquals("oldName", changes["name"]!!.oldValue)
        assertEquals("newName", changes["name"]!!.newValue)
    }

    @Test
    fun `recordChange records null old value`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("email", null, "[email protected]", changes)

        assertEquals(1, changes.size)
        assertNull(changes["email"]!!.oldValue)
        assertEquals("[email protected]", changes["email"]!!.newValue)
    }

    @Test
    fun `recordChange records null new value`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("phone", "1234567890", null, changes)

        assertEquals(1, changes.size)
        assertEquals("1234567890", changes["phone"]!!.oldValue)
        assertNull(changes["phone"]!!.newValue)
    }

    @Test
    fun `recordChange processes date prefixes in old value`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("created", "\$D_1609459200", "newValue", changes)

        assertEquals(1, changes.size)
        assertEquals(1609459200L, changes["created"]!!.oldValue)
        assertEquals("newValue", changes["created"]!!.newValue)
    }

    @Test
    fun `recordChange processes date prefixes in new value`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("updated", "oldValue", "\$D_1704067200", changes)

        assertEquals(1, changes.size)
        assertEquals("oldValue", changes["updated"]!!.oldValue)
        assertEquals(1704067200L, changes["updated"]!!.newValue)
    }

    @Test
    fun `recordChange handles multiple changes`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("name", "oldName", "newName", changes)
        tracker.recordChange("age", 25, 26, changes)
        tracker.recordChange("email", null, "[email protected]", changes)

        assertEquals(3, changes.size)
        assertTrue(changes.containsKey("name"))
        assertTrue(changes.containsKey("age"))
        assertTrue(changes.containsKey("email"))
    }

    @Test
    fun `recordAddition records simple addition`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordAddition("newField", "newValue", changes)

        assertEquals(1, changes.size)
        assertNull(changes["newField"]!!.oldValue)
        assertEquals("newValue", changes["newField"]!!.newValue)
    }

    @Test
    fun `recordAddition processes date prefixes`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordAddition("timestamp", "\$D_1609459200", changes)

        assertEquals(1, changes.size)
        assertNull(changes["timestamp"]!!.oldValue)
        assertEquals(1609459200L, changes["timestamp"]!!.newValue)
    }

    @Test
    fun `recordAddition with JSONObject records all leaf values`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val newObj = JSONObject().apply {
            put("name", "John")
            put("age", 30)
            put("nested", JSONObject().apply {
                put("city", "NYC")
            })
        }
        
        tracker.recordAddition("user", newObj, changes)

        assertEquals(3, changes.size)
        assertTrue(changes.containsKey("user.name"))
        assertTrue(changes.containsKey("user.age"))
        assertTrue(changes.containsKey("user.nested.city"))
        assertNull(changes["user.name"]!!.oldValue)
        assertEquals("John", changes["user.name"]!!.newValue)
        assertEquals(30, changes["user.age"]!!.newValue)
        assertEquals("NYC", changes["user.nested.city"]!!.newValue)
    }

    @Test
    fun `recordAddition with deeply nested JSONObject`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val newObj = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("level3", "value")
                })
            })
        }
        
        tracker.recordAddition("root", newObj, changes)

        assertEquals(1, changes.size)
        assertTrue(changes.containsKey("root.level1.level2.level3"))
        assertEquals("value", changes["root.level1.level2.level3"]!!.newValue)
    }

    @Test
    fun `recordDeletion records simple deletion`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordDeletion("oldValue", "field", changes)

        assertEquals(1, changes.size)
        assertEquals("oldValue", changes["field"]!!.oldValue)
        assertNull(changes["field"]!!.newValue)
    }

    @Test
    fun `recordDeletion processes date prefixes`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordDeletion("\$D_1609459200", "timestamp", changes)

        assertEquals(1, changes.size)
        assertEquals(1609459200L, changes["timestamp"]!!.oldValue)
        assertNull(changes["timestamp"]!!.newValue)
    }

    @Test
    fun `recordDeletion with JSONObject records all leaf deletions`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val oldObj = JSONObject().apply {
            put("name", "John")
            put("age", 30)
            put("nested", JSONObject().apply {
                put("city", "NYC")
            })
        }
        
        tracker.recordDeletion(oldObj, "user", changes)

        assertEquals(3, changes.size)
        assertTrue(changes.containsKey("user.name"))
        assertTrue(changes.containsKey("user.age"))
        assertTrue(changes.containsKey("user.nested.city"))
        assertEquals("John", changes["user.name"]!!.oldValue)
        assertNull(changes["user.name"]!!.newValue)
        assertEquals(30, changes["user.age"]!!.oldValue)
        assertEquals("NYC", changes["user.nested.city"]!!.oldValue)
    }

    @Test
    fun `recordDeletion with deeply nested JSONObject`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val oldObj = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("level3", "value")
                })
            })
        }
        
        tracker.recordDeletion(oldObj, "root", changes)

        assertEquals(1, changes.size)
        assertTrue(changes.containsKey("root.level1.level2.level3"))
        assertEquals("value", changes["root.level1.level2.level3"]!!.oldValue)
    }

    @Test
    fun `handles JSONArray values`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val oldArray = JSONArray().put("item1")
        val newArray = JSONArray().put("item2")

        tracker.recordChange("items", oldArray, newArray, changes)

        assertEquals(1, changes.size)
        assertEquals(oldArray, changes["items"]!!.oldValue)
        assertEquals(newArray, changes["items"]!!.newValue)
    }

    @Test
    fun `handles numeric values`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("count", 10, 20, changes)
        tracker.recordChange("price", 99.99, 89.99, changes)

        assertEquals(2, changes.size)
        assertEquals(10, changes["count"]!!.oldValue)
        assertEquals(20, changes["count"]!!.newValue)
        assertEquals(99.99, changes["price"]!!.oldValue)
        assertEquals(89.99, changes["price"]!!.newValue)
    }

    @Test
    fun `handles boolean values`() {
        val changes = mutableMapOf<String, ProfileChange>()
        
        tracker.recordChange("isActive", false, true, changes)

        assertEquals(1, changes.size)
        assertEquals(false, changes["isActive"]!!.oldValue)
        assertEquals(true, changes["isActive"]!!.newValue)
    }

    @Test
    fun `recordAddition with empty JSONObject records nothing`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val emptyObj = JSONObject()
        
        tracker.recordAddition("empty", emptyObj, changes)

        assertEquals(0, changes.size)
    }

    @Test
    fun `recordDeletion with empty JSONObject records nothing`() {
        val changes = mutableMapOf<String, ProfileChange>()
        val emptyObj = JSONObject()
        
        tracker.recordDeletion(emptyObj, "empty", changes)

        assertEquals(0, changes.size)
    }
}
