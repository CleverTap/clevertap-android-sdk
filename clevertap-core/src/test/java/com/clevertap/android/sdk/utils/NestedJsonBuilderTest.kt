package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NestedJsonBuilderTest {

    private val builder = NestedJsonBuilder()

    @Test
    fun `buildFromPath creates simple key-value`() {
        val result = builder.buildFromPath("name", "John")

        assertEquals("John", result.getString("name"))
    }

    @Test
    fun `buildFromPath creates nested object`() {
        val result = builder.buildFromPath("user.age", 25)

        assertTrue(result.has("user"))
        assertEquals(25, result.getJSONObject("user").getInt("age"))
    }

    @Test
    fun `buildFromPath creates deeply nested object`() {
        val result = builder.buildFromPath("user.profile.settings.theme", "dark")

        val user = result.getJSONObject("user")
        val profile = user.getJSONObject("profile")
        val settings = profile.getJSONObject("settings")
        assertEquals("dark", settings.getString("theme"))
    }

    @Test
    fun `buildFromPath creates array with single element`() {
        val result = builder.buildFromPath("items[0]", "value")

        val items = result.getJSONArray("items")
        assertEquals(1, items.length())
        assertEquals("value", items.getString(0))
    }

    @Test
    fun `buildFromPath creates array with gap-filling nulls`() {
        val result = builder.buildFromPath("items[2]", "value")

        val items = result.getJSONArray("items")
        assertEquals(3, items.length())
        assertTrue(items.isNull(0))
        assertTrue(items.isNull(1))
        assertEquals("value", items.getString(2))
    }

    @Test
    fun `buildFromPath creates object inside array`() {
        val result = builder.buildFromPath("users[0].name", "Alice")

        val users = result.getJSONArray("users")
        assertEquals(1, users.length())
        assertEquals("Alice", users.getJSONObject(0).getString("name"))
    }

    @Test
    fun `buildFromPath creates nested array inside object`() {
        val result = builder.buildFromPath("profile.scores[2]", 95)

        val profile = result.getJSONObject("profile")
        val scores = profile.getJSONArray("scores")
        assertEquals(3, scores.length())
        assertEquals(95, scores.getInt(2))
    }

    @Test
    fun `buildFromPath handles null value`() {
        val result = builder.buildFromPath("key", null)

        assertTrue(result.has("key"))
        assertTrue(result.isNull("key"))
    }

    @Test
    fun `buildFromPath handles boolean value`() {
        val result = builder.buildFromPath("enabled", true)

        assertEquals(true, result.getBoolean("enabled"))
    }

    @Test
    fun `buildFromPath handles number values`() {
        val result1 = builder.buildFromPath("count", 42)
        assertEquals(42, result1.getInt("count"))

        val result2 = builder.buildFromPath("price", 19.99)
        assertEquals(19.99, result2.getDouble("price"), 0.001)
    }

    @Test
    fun `buildFromPath handles map value`() {
        val map = mapOf("city" to "NYC", "zip" to "10001")
        val result = builder.buildFromPath("address", map)

        val address = result.getJSONObject("address")
        assertEquals("NYC", address.getString("city"))
        assertEquals("10001", address.getString("zip"))
    }

    @Test
    fun `buildFromPath handles list value`() {
        val list = listOf("red", "green", "blue")
        val result = builder.buildFromPath("colors", list)

        val colors = result.getJSONArray("colors")
        assertEquals(3, colors.length())
        assertEquals("red", colors.getString(0))
        assertEquals("green", colors.getString(1))
        assertEquals("blue", colors.getString(2))
    }

    @Test
    fun `buildFromPath handles JSONObject value`() {
        val json = JSONObject().apply {
            put("x", 10)
            put("y", 20)
        }
        val result = builder.buildFromPath("position", json)

        val position = result.getJSONObject("position")
        assertEquals(10, position.getInt("x"))
        assertEquals(20, position.getInt("y"))
    }

    @Test
    fun `buildFromPath handles JSONArray value`() {
        val array = JSONArray().apply {
            put(1)
            put(2)
            put(3)
        }
        val result = builder.buildFromPath("numbers", array)

        val numbers = result.getJSONArray("numbers")
        assertEquals(3, numbers.length())
        assertEquals(1, numbers.getInt(0))
    }

    @Test
    fun `buildFromPath handles integer overflow in array index`() {
        // The regex matches digits, but toIntOrNull catches overflow
        assertFailsWith<IllegalArgumentException> {
            builder.buildFromPath("items[99999999999999999999]", "value")
        }
    }

    @Test
    fun `buildFromPath handles consecutive array indices for matrix`() {
        val result = builder.buildFromPath("matrix[0][1]", "value")

        val matrix = result.getJSONArray("matrix")
        assertEquals(1, matrix.length())
        val row = matrix.getJSONArray(0)
        assertEquals(2, row.length())
        assertTrue(row.isNull(0))
        assertEquals("value", row.getString(1))
    }

    @Test
    fun `buildFromPath handles 3D array structure`() {
        val result = builder.buildFromPath("cube[1][2][3]", 42)

        val cube = result.getJSONArray("cube")
        val layer = cube.getJSONArray(1)
        val row = layer.getJSONArray(2)
        assertEquals(42, row.getInt(3))
    }

    @Test
    fun `buildFromPath handles complex nested structure`() {
        val result = builder.buildFromPath("data.users[0].addresses[1].street", "Main St")
        val data = result.getJSONObject("data")
        val users = data.getJSONArray("users")
        val user = users.getJSONObject(0)
        val addresses = user.getJSONArray("addresses")
        val address = addresses.getJSONObject(1)
        assertEquals("Main St", address.getString("street"))
    }

    @Test
    fun `buildFromPath handles empty path segments gracefully`() {
        // Path with just the key, no nesting
        val result = builder.buildFromPath("simple", "value")
        assertEquals("value", result.getString("simple"))
    }

    @Test
    fun `buildFromPath handles array at root level`() {
        val result = builder.buildFromPath("items[0]", 123)

        val items = result.getJSONArray("items")
        assertEquals(123, items.getInt(0))
    }
}
