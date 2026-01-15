package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ArrayMergeUtilsTest {

    // copyArray tests
    @Test
    fun `copyArray creates deep copy of simple array`() {
        val original = JSONArray().apply {
            put("value1")
            put("value2")
            put(123)
        }

        val copy = ArrayMergeUtils.copyArray(original)

        assertEquals(original.length(), copy.length())
        assertEquals("value1", copy.getString(0))
        assertEquals("value2", copy.getString(1))
        assertEquals(123, copy.getInt(2))
        assertNotSame(original, copy)
    }

    @Test
    fun `copyArray creates deep copy with nested objects`() {
        val original = JSONArray().apply {
            put(JSONObject().apply {
                put("key1", "value1")
                put("key2", 42)
            })
            put("string")
        }

        val copy = ArrayMergeUtils.copyArray(original)

        assertEquals(original.length(), copy.length())
        val originalObj = original.getJSONObject(0)
        val copiedObj = copy.getJSONObject(0)
        assertEquals(originalObj.getString("key1"), copiedObj.getString("key1"))
        assertEquals(originalObj.getInt("key2"), copiedObj.getInt("key2"))
        assertNotSame(originalObj, copiedObj)
    }

    @Test
    fun `copyArray handles empty array`() {
        val original = JSONArray()
        val copy = ArrayMergeUtils.copyArray(original)

        assertEquals(0, copy.length())
        assertNotSame(original, copy)
    }

    @Test
    fun `copyArray handles array with nested arrays`() {
        val original = JSONArray().apply {
            put(JSONArray().apply {
                put("nested1")
                put("nested2")
            })
            put("outer")
        }

        val copy = ArrayMergeUtils.copyArray(original)

        assertEquals(2, copy.length())
        val nestedCopy = copy.getJSONArray(0)
        assertEquals(2, nestedCopy.length())
        assertEquals("nested1", nestedCopy.getString(0))
        assertEquals("nested2", nestedCopy.getString(1))
    }

    // arrayContainsString tests
    @Test
    fun `arrayContainsString returns true when string exists`() {
        val array = JSONArray().apply {
            put("apple")
            put("banana")
            put("cherry")
        }

        assertTrue(ArrayMergeUtils.arrayContainsString(array, "banana"))
    }

    @Test
    fun `arrayContainsString returns false when string does not exist`() {
        val array = JSONArray().apply {
            put("apple")
            put("banana")
        }

        assertFalse(ArrayMergeUtils.arrayContainsString(array, "orange"))
    }

    @Test
    fun `arrayContainsString returns false for empty array`() {
        val array = JSONArray()

        assertFalse(ArrayMergeUtils.arrayContainsString(array, "test"))
    }

    @Test
    fun `arrayContainsString ignores non-string elements`() {
        val array = JSONArray().apply {
            put(123)
            put(true)
            put(JSONObject())
            put("test")
        }

        assertTrue(ArrayMergeUtils.arrayContainsString(array, "test"))
        assertFalse(ArrayMergeUtils.arrayContainsString(array, "123"))
    }

    @Test
    fun `arrayContainsString is case sensitive`() {
        val array = JSONArray().apply {
            put("Test")
        }

        assertTrue(ArrayMergeUtils.arrayContainsString(array, "Test"))
        assertFalse(ArrayMergeUtils.arrayContainsString(array, "test"))
    }

    @Test
    fun `arrayContainsString handles duplicate strings`() {
        val array = JSONArray().apply {
            put("test")
            put("test")
            put("other")
        }

        assertTrue(ArrayMergeUtils.arrayContainsString(array, "test"))
    }

    // hasDeleteMarkerElements tests
    @Test
    fun `hasDeleteMarkerElements returns true when delete marker exists`() {
        val array = JSONArray().apply {
            put("normal")
            put(Constants.DELETE_MARKER)
            put("other")
        }

        assertTrue(ArrayMergeUtils.hasDeleteMarkerElements(array))
    }

    @Test
    fun `hasDeleteMarkerElements returns false when no delete marker exists`() {
        val array = JSONArray().apply {
            put("value1")
            put("value2")
            put(123)
        }

        assertFalse(ArrayMergeUtils.hasDeleteMarkerElements(array))
    }

    @Test
    fun `hasDeleteMarkerElements returns false for empty array`() {
        val array = JSONArray()

        assertFalse(ArrayMergeUtils.hasDeleteMarkerElements(array))
    }

    @Test
    fun `hasDeleteMarkerElements detects delete marker at any position`() {
        // First position
        val array1 = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put("value")
        }
        assertTrue(ArrayMergeUtils.hasDeleteMarkerElements(array1))

        // Last position
        val array2 = JSONArray().apply {
            put("value")
            put(Constants.DELETE_MARKER)
        }
        assertTrue(ArrayMergeUtils.hasDeleteMarkerElements(array2))

        // Middle position
        val array3 = JSONArray().apply {
            put("value1")
            put(Constants.DELETE_MARKER)
            put("value2")
        }
        assertTrue(ArrayMergeUtils.hasDeleteMarkerElements(array3))
    }

    @Test
    fun `hasDeleteMarkerElements ignores non-string delete marker`() {
        val array = JSONArray().apply {
            put(123)
            put(JSONObject().apply {
                put("key", Constants.DELETE_MARKER)
            })
        }

        assertFalse(ArrayMergeUtils.hasDeleteMarkerElements(array))
    }

    @Test
    fun `hasDeleteMarkerElements handles multiple delete markers`() {
        val array = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put("value")
            put(Constants.DELETE_MARKER)
        }

        assertTrue(ArrayMergeUtils.hasDeleteMarkerElements(array))
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

        assertTrue(ArrayMergeUtils.hasJsonObjectElements(array))
    }

    @Test
    fun `hasJsonObjectElements returns false when no JSONObject exists`() {
        val array = JSONArray().apply {
            put("string")
            put(123)
            put(true)
        }

        assertFalse(ArrayMergeUtils.hasJsonObjectElements(array))
    }

    @Test
    fun `hasJsonObjectElements returns false for empty array`() {
        val array = JSONArray()

        assertFalse(ArrayMergeUtils.hasJsonObjectElements(array))
    }

    @Test
    fun `hasJsonObjectElements detects multiple JSONObjects`() {
        val array = JSONArray().apply {
            put(JSONObject().apply { put("key1", "value1") })
            put("string")
            put(JSONObject().apply { put("key2", "value2") })
        }

        assertTrue(ArrayMergeUtils.hasJsonObjectElements(array))
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

        assertTrue(ArrayMergeUtils.hasJsonObjectElements(array))
    }

    @Test
    fun `hasJsonObjectElements ignores JSONArrays`() {
        val array = JSONArray().apply {
            put("string")
            put(JSONArray().apply {
                put("nested")
            })
        }

        assertFalse(ArrayMergeUtils.hasJsonObjectElements(array))
    }
}
