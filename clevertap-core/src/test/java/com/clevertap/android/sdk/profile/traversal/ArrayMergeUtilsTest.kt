package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ArrayMergeUtilsTest {

    // deepCopy tests
    @Test
    fun `deepCopy creates deep copy of simple array`() {
        val original = JSONArray().apply {
            put("value1")
            put("value2")
            put(123)
        }

        val copy = original.deepCopy()

        assertEquals(original.length(), copy.length())
        assertEquals("value1", copy.getString(0))
        assertEquals("value2", copy.getString(1))
        assertEquals(123, copy.getInt(2))
        assertNotSame(original, copy)
    }

    @Test
    fun `deepCopy creates deep copy with nested objects`() {
        val original = JSONArray().apply {
            put(JSONObject().apply {
                put("key1", "value1")
                put("key2", 42)
            })
            put("string")
        }

        val copy = original.deepCopy()

        assertEquals(original.length(), copy.length())
        val originalObj = original.getJSONObject(0)
        val copiedObj = copy.getJSONObject(0)
        assertEquals(originalObj.getString("key1"), copiedObj.getString("key1"))
        assertEquals(originalObj.getInt("key2"), copiedObj.getInt("key2"))
        assertNotSame(originalObj, copiedObj)
    }

    @Test
    fun `deepCopy handles empty array`() {
        val original = JSONArray()
        val copy = original.deepCopy()

        assertEquals(0, copy.length())
        assertNotSame(original, copy)
    }

    @Test
    fun `deepCopy handles array with nested arrays`() {
        val original = JSONArray().apply {
            put(JSONArray().apply {
                put("nested1")
                put("nested2")
            })
            put("outer")
        }

        val copy = original.deepCopy()

        assertEquals(2, copy.length())
        val nestedCopy = copy.getJSONArray(0)
        assertEquals(2, nestedCopy.length())
        assertEquals("nested1", nestedCopy.getString(0))
        assertEquals("nested2", nestedCopy.getString(1))
    }

    // containsString tests
    @Test
    fun `containsString returns true when string exists`() {
        val array = JSONArray().apply {
            put("apple")
            put("banana")
            put("cherry")
        }

        assertTrue(array.containsString("banana"))
    }

    @Test
    fun `containsString returns false when string does not exist`() {
        val array = JSONArray().apply {
            put("apple")
            put("banana")
        }

        assertFalse(array.containsString("orange"))
    }

    @Test
    fun `containsString returns false for empty array`() {
        val array = JSONArray()

        assertFalse(array.containsString("test"))
    }

    @Test
    fun `containsString ignores non-string elements`() {
        val array = JSONArray().apply {
            put(123)
            put(true)
            put(JSONObject())
            put("test")
        }

        assertTrue(array.containsString("test"))
        assertFalse(array.containsString("123"))
    }

    @Test
    fun `containsString is case sensitive`() {
        val array = JSONArray().apply {
            put("Test")
        }

        assertTrue(array.containsString("Test"))
        assertFalse(array.containsString("test"))
    }

    @Test
    fun `containsString handles duplicate strings`() {
        val array = JSONArray().apply {
            put("test")
            put("test")
            put("other")
        }

        assertTrue(array.containsString("test"))
    }

    // hasDeleteMarkerElements tests
    @Test
    fun `hasDeleteMarkerElements returns true when delete marker exists`() {
        val array = JSONArray().apply {
            put("normal")
            put(Constants.DELETE_MARKER)
            put("other")
        }

        assertTrue(array.hasDeleteMarkerElements())
    }

    @Test
    fun `hasDeleteMarkerElements returns false when no delete marker exists`() {
        val array = JSONArray().apply {
            put("value1")
            put("value2")
            put(123)
        }

        assertFalse(array.hasDeleteMarkerElements())
    }

    @Test
    fun `hasDeleteMarkerElements returns false for empty array`() {
        val array = JSONArray()

        assertFalse(array.hasDeleteMarkerElements())
    }

    @Test
    fun `hasDeleteMarkerElements detects delete marker at any position`() {
        // First position
        val array1 = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put("value")
        }
        assertTrue(array1.hasDeleteMarkerElements())

        // Last position
        val array2 = JSONArray().apply {
            put("value")
            put(Constants.DELETE_MARKER)
        }
        assertTrue(array2.hasDeleteMarkerElements())

        // Middle position
        val array3 = JSONArray().apply {
            put("value1")
            put(Constants.DELETE_MARKER)
            put("value2")
        }
        assertTrue(array3.hasDeleteMarkerElements())
    }

    @Test
    fun `hasDeleteMarkerElements ignores non-string delete marker`() {
        val array = JSONArray().apply {
            put(123)
            put(JSONObject().apply {
                put("key", Constants.DELETE_MARKER)
            })
        }

        assertFalse(array.hasDeleteMarkerElements())
    }

    @Test
    fun `hasDeleteMarkerElements handles multiple delete markers`() {
        val array = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put("value")
            put(Constants.DELETE_MARKER)
        }

        assertTrue(array.hasDeleteMarkerElements())
    }

    // hasJsonObjectElements tests
    @Test
    fun `hasJsonObjectElements returns true when JSONObject exists`() {
        val array = JSONArray().apply {
            put("string")
            put(JSONObject().apply {
                put("key", "value")
            })
        }

        assertTrue(array.hasJsonObjectElements())
    }

    @Test
    fun `hasJsonObjectElements returns false when no JSONObject exists`() {
        val array = JSONArray().apply {
            put("string")
            put(123)
            put(true)
        }

        assertFalse(array.hasJsonObjectElements())
    }

    @Test
    fun `hasJsonObjectElements returns false for empty array`() {
        val array = JSONArray()

        assertFalse(array.hasJsonObjectElements())
    }

    @Test
    fun `hasJsonObjectElements detects multiple JSONObjects`() {
        val array = JSONArray().apply {
            put(JSONObject().apply { put("key1", "value1") })
            put("string")
            put(JSONObject().apply { put("key2", "value2") })
        }

        assertTrue(array.hasJsonObjectElements())
    }

    @Test
    fun `hasJsonObjectElements handles nested JSONObjects`() {
        val array = JSONArray().apply {
            put(JSONObject().apply {
                put("outer", JSONObject().apply {
                    put("inner", "value")
                })
            })
        }

        assertTrue(array.hasJsonObjectElements())
    }

    @Test
    fun `hasJsonObjectElements ignores JSONArrays`() {
        val array = JSONArray().apply {
            put("string")
            put(JSONArray().apply {
                put("nested")
            })
        }

        assertFalse(array.hasJsonObjectElements())
    }
}
