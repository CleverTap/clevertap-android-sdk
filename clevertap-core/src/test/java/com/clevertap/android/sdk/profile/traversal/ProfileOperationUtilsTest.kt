package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProfileOperationUtilsTest {

    // isDeleteMarker tests
    @Test
    fun `isDeleteMarker returns true for DELETE_MARKER string`() {
        assertTrue(ProfileOperationUtils.isDeleteMarker(Constants.DELETE_MARKER))
    }

    @Test
    fun `isDeleteMarker returns false for null`() {
        assertFalse(ProfileOperationUtils.isDeleteMarker(null))
    }

    @Test
    fun `isDeleteMarker returns false for different string`() {
        assertFalse(ProfileOperationUtils.isDeleteMarker("some other string"))
    }

    @Test
    fun `isDeleteMarker returns false for non-string types`() {
        assertFalse(ProfileOperationUtils.isDeleteMarker(123))
        assertFalse(ProfileOperationUtils.isDeleteMarker(true))
        assertFalse(ProfileOperationUtils.isDeleteMarker(JSONObject()))
        assertFalse(ProfileOperationUtils.isDeleteMarker(JSONArray()))
    }

    @Test
    fun `isDeleteMarker returns false for empty string`() {
        assertFalse(ProfileOperationUtils.isDeleteMarker(""))
    }

    // processDatePrefixes tests for String values
    @Test
    fun `processDatePrefixes converts valid date prefix string to long`() {
        val input = "${Constants.DATE_PREFIX}1234567890"
        val result = ProfileOperationUtils.processDatePrefixes(input)

        assertTrue(result is Long)
        assertEquals(1234567890L, result)
    }

    @Test
    fun `processDatePrefixes returns original string if no date prefix`() {
        val input = "normalString"
        val result = ProfileOperationUtils.processDatePrefixes(input)

        assertTrue(result is String)
        assertEquals("normalString", result)
    }

    @Test
    fun `processDatePrefixes returns original string if conversion fails`() {
        val input = "${Constants.DATE_PREFIX}notANumber"
        val result = ProfileOperationUtils.processDatePrefixes(input)

        assertTrue(result is String)
        assertEquals(input, result)
    }


    @Test
    fun `processDatePrefixes handles large timestamp`() {
        val input = "${Constants.DATE_PREFIX}9999999999999"
        val result = ProfileOperationUtils.processDatePrefixes(input)

        assertTrue(result is Long)
        assertEquals(9999999999999L, result)
    }

    // processDatePrefixes tests for non-String values
    @Test
    fun `processDatePrefixes returns numbers unchanged`() {
        assertEquals(123, ProfileOperationUtils.processDatePrefixes(123))
        assertEquals(123L, ProfileOperationUtils.processDatePrefixes(123L))
        assertEquals(123.45, ProfileOperationUtils.processDatePrefixes(123.45))
    }

    @Test
    fun `processDatePrefixes returns boolean unchanged`() {
        assertEquals(true, ProfileOperationUtils.processDatePrefixes(true))
        assertEquals(false, ProfileOperationUtils.processDatePrefixes(false))
    }

    // processDatePrefixes tests for JSONArray
    @Test
    fun `processDatePrefixes processes empty array`() {
        val input = JSONArray()
        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONArray

        assertEquals(0, result.length())
    }

    @Test
    fun `processDatePrefixes processes array with mixed types`() {
        val input = JSONArray().apply {
            put("${Constants.DATE_PREFIX}1000")
            put(123)
            put(true)
            put("string")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONArray

        assertEquals(4, result.length())
        assertEquals(1000L, result.get(0))
        assertEquals(123, result.get(1))
        assertEquals(true, result.get(2))
        assertEquals("string", result.get(3))
    }

    @Test
    fun `processDatePrefixes processes nested arrays`() {
        val input = JSONArray().apply {
            put(JSONArray().apply {
                put("${Constants.DATE_PREFIX}1000")
                put("${Constants.DATE_PREFIX}2000")
            })
            put("outer")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONArray
        val nested = result.getJSONArray(0)

        assertEquals(2, nested.length())
        assertEquals(1000L, nested.get(0))
        assertEquals(2000L, nested.get(1))
        assertEquals("outer", result.get(1))
    }

    // processDatePrefixes tests for JSONObject
    @Test
    fun `processDatePrefixes processes empty object`() {
        val input = JSONObject()
        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONObject

        assertEquals(0, result.length())
    }

    @Test
    fun `processDatePrefixes processes object with mixed value types`() {
        val input = JSONObject().apply {
            put("date", "${Constants.DATE_PREFIX}1000")
            put("number", 123)
            put("boolean", true)
            put("string", "value")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONObject

        assertEquals(4, result.length())
        assertEquals(1000L, result.get("date"))
        assertEquals(123, result.get("number"))
        assertEquals(true, result.get("boolean"))
        assertEquals("value", result.get("string"))
    }

    @Test
    fun `processDatePrefixes processes nested objects`() {
        val input = JSONObject().apply {
            put("outer", JSONObject().apply {
                put("innerDate", "${Constants.DATE_PREFIX}1000")
                put("innerString", "value")
            })
            put("date", "${Constants.DATE_PREFIX}2000")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONObject
        val nested = result.getJSONObject("outer")

        assertEquals(1000L, nested.get("innerDate"))
        assertEquals("value", nested.get("innerString"))
        assertEquals(2000L, result.get("date"))
    }

    @Test
    fun `processDatePrefixes processes object with array values`() {
        val input = JSONObject().apply {
            put("dates", JSONArray().apply {
                put("${Constants.DATE_PREFIX}1000")
                put("${Constants.DATE_PREFIX}2000")
            })
            put("normalField", "value")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONObject
        val datesArray = result.getJSONArray("dates")

        assertEquals(1000L, datesArray.get(0))
        assertEquals(2000L, datesArray.get(1))
        assertEquals("value", result.get("normalField"))
    }

    // Complex nested structure tests
    @Test
    fun `processDatePrefixes handles deeply nested structures`() {
        val input = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONArray().apply {
                    put(JSONObject().apply {
                        put("level3Date", "${Constants.DATE_PREFIX}1000")
                        put("level3String", "value")
                    })
                })
            })
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONObject
        val level1 = result.getJSONObject("level1")
        val level2 = level1.getJSONArray("level2")
        val level3 = level2.getJSONObject(0)

        assertEquals(1000L, level3.get("level3Date"))
        assertEquals("value", level3.get("level3String"))
    }

    @Test
    fun `processDatePrefixes with delete marker string`() {
        val input = JSONArray().apply {
            put(Constants.DELETE_MARKER)
            put("${Constants.DATE_PREFIX}1000")
        }

        val result = ProfileOperationUtils.processDatePrefixes(input) as JSONArray

        // DELETE_MARKER should remain as string since it doesn't have date prefix
        assertEquals(Constants.DELETE_MARKER, result.get(0))
        assertEquals(1000L, result.get(1))
    }
}
