package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class JsonFlattenerTest {

    @Test
    fun `flatten returns empty map for empty json`() {
        val json = JSONObject()

        val result = JsonFlattener.flatten(json)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `flatten returns single key for flat json`() {
        val json = JSONObject().apply {
            put("name", "John")
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        assertEquals("John", result["name"])
    }

    @Test
    fun `flatten returns multiple keys for flat json`() {
        val json = JSONObject().apply {
            put("name", "John")
            put("age", 30)
            put("city", "NYC")
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(3, result.size)
        assertEquals("John", result["name"])
        assertEquals(30, result["age"])
        assertEquals("NYC", result["city"])
    }

    @Test
    fun `flatten uses dot notation for nested objects`() {
        val json = JSONObject().apply {
            put("user", JSONObject().apply {
                put("name", "John")
                put("age", 30)
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertEquals("John", result["user.name"])
        assertEquals(30, result["user.age"])
    }

    @Test
    fun `flatten handles deeply nested objects`() {
        val json = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("level3", "value")
                })
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        assertEquals("value", result["level1.level2.level3"])
    }

    @Test
    fun `flatten keeps arrays as-is`() {
        val json = JSONObject().apply {
            put("tags", JSONArray().apply {
                put("tag1")
                put("tag2")
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        assertTrue(result["tags"] is JSONArray)
        val array = result["tags"] as JSONArray
        assertEquals(2, array.length())
        assertEquals("tag1", array.getString(0))
        assertEquals("tag2", array.getString(1))
    }

    @Test
    fun `flatten handles mixed nested and flat data`() {
        val json = JSONObject().apply {
            put("name", "John")
            put("address", JSONObject().apply {
                put("city", "NYC")
                put("zip", "10001")
            })
            put("age", 30)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(4, result.size)
        assertEquals("John", result["name"])
        assertEquals("NYC", result["address.city"])
        assertEquals("10001", result["address.zip"])
        assertEquals(30, result["age"])
    }

    @Test
    fun `flatten skips null values`() {
        val json = JSONObject().apply {
            put("name", "John")
            put("middle", JSONObject.NULL)
            put("age", 30)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertEquals("John", result["name"])
        assertEquals(30, result["age"])
        assertFalse(result.containsKey("middle"))
    }

    @Test
    fun `flatten handles boolean values`() {
        val json = JSONObject().apply {
            put("active", true)
            put("verified", false)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertEquals(true, result["active"])
        assertEquals(false, result["verified"])
    }

    @Test
    fun `flatten handles numeric values`() {
        val json = JSONObject().apply {
            put("intValue", 42)
            put("doubleValue", 3.14)
            put("longValue", 1234567890L)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(3, result.size)
        assertEquals(42, result["intValue"])
        assertEquals(3.14, result["doubleValue"])
        assertEquals(1234567890L, result["longValue"])
    }

    @Test
    fun `flatten handles nested array`() {
        val json = JSONObject().apply {
            put("user", JSONObject().apply {
                put("tags", JSONArray().apply {
                    put("a")
                    put("b")
                })
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        assertTrue(result["user.tags"] is JSONArray)
    }

    @Test
    fun `flatten handles multiple nested levels with arrays`() {
        val json = JSONObject().apply {
            put("data", JSONObject().apply {
                put("items", JSONArray().apply {
                    put(1)
                    put(2)
                })
                put("meta", JSONObject().apply {
                    put("count", 2)
                })
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertTrue(result["data.items"] is JSONArray)
        assertEquals(2, result["data.meta.count"])
    }

    @Test
    fun `flatten handles empty nested objects`() {
        val json = JSONObject().apply {
            put("name", "John")
            put("empty", JSONObject())
            put("age", 30)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertEquals("John", result["name"])
        assertEquals(30, result["age"])
    }

    @Test
    fun `flatten handles string with dots`() {
        val json = JSONObject().apply {
            put("domain", "example.com")
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        assertEquals("example.com", result["domain"])
    }


    @Test
    fun `flatten handles complex real-world structure`() {
        val json = JSONObject().apply {
            put("id", "123")
            put("date", "\$D_123")
            put("user", JSONObject().apply {
                put("name", "John Doe")
                put("email", "john@example.com")
                put("address", JSONObject().apply {
                    put("street", "123 Main St")
                    put("city", "NYC")
                    put("date", "\$D_123")
                })
            })
            put("tags", JSONArray().apply {
                put("premium")
                put("verified")
                put("\$D_123")
            })
            put("active", true)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(9, result.size)
        assertEquals("123", result["id"])
        assertEquals(123L, result["date"])
        assertEquals("John Doe", result["user.name"])
        assertEquals("john@example.com", result["user.email"])
        assertEquals("123 Main St", result["user.address.street"])
        assertEquals("NYC", result["user.address.city"])
        assertEquals(123L, result["user.address.date"])
        assertTrue(result["tags"] is JSONArray)
        assertEquals((result["tags"] as JSONArray).get(2), 123L)
        assertEquals(true, result["active"])
    }

    @Test
    fun `flatten preserves array order`() {
        val json = JSONObject().apply {
            put("items", JSONArray().apply {
                put("first")
                put("second")
                put("third")
            })
        }

        val result = JsonFlattener.flatten(json)

        val array = result["items"] as JSONArray
        assertEquals("first", array.getString(0))
        assertEquals("second", array.getString(1))
        assertEquals("third", array.getString(2))
    }

    @Test
    fun `flatten handles nested objects with same key names at different levels`() {
        val json = JSONObject().apply {
            put("name", "Root")
            put("child", JSONObject().apply {
                put("name", "Child")
                put("grandchild", JSONObject().apply {
                    put("name", "Grandchild")
                })
            })
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(3, result.size)
        assertEquals("Root", result["name"])
        assertEquals("Child", result["child.name"])
        assertEquals("Grandchild", result["child.grandchild.name"])
    }

    @Test
    fun `flatten handles empty arrays`() {
        val json = JSONObject().apply {
            put("items", JSONArray())
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(1, result.size)
        val array = result["items"] as JSONArray
        assertEquals(0, array.length())
    }

    @Test
    fun `flatten handles zero and negative numbers`() {
        val json = JSONObject().apply {
            put("zero", 0)
            put("negative", -42)
            put("negativeDouble", -3.14)
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(3, result.size)
        assertEquals(0, result["zero"])
        assertEquals(-42, result["negative"])
        assertEquals(-3.14, result["negativeDouble"])
    }

    @Test
    fun `flatten handles empty string values`() {
        val json = JSONObject().apply {
            put("empty", "")
            put("name", "John")
        }

        val result = JsonFlattener.flatten(json)

        assertEquals(2, result.size)
        assertEquals("", result["empty"])
        assertEquals("John", result["name"])
    }
}
