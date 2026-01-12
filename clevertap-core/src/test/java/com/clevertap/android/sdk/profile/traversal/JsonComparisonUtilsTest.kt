package com.clevertap.android.sdk.profile.traversal

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class JsonComparisonUtilsTest {

    // areEqual tests - null handling
    @Test
    fun `areEqual returns true when both are null`() {
        assertTrue(JsonComparisonUtils.areEqual(null, null))
    }

    @Test
    fun `areEqual returns false when only first is null`() {
        assertFalse(JsonComparisonUtils.areEqual(null, "value"))
    }

    @Test
    fun `areEqual returns false when only second is null`() {
        assertFalse(JsonComparisonUtils.areEqual("value", null))
    }

    // areEqual tests - reference equality
    @Test
    fun `areEqual returns true for same reference`() {
        val obj = JSONObject().apply { put("key", "value") }
        assertTrue(JsonComparisonUtils.areEqual(obj, obj))
    }

    // areEqual tests - primitive types
    @Test
    fun `areEqual returns true for equal strings`() {
        assertTrue(JsonComparisonUtils.areEqual("test", "test"))
    }

    @Test
    fun `areEqual returns false for different strings`() {
        assertFalse(JsonComparisonUtils.areEqual("test1", "test2"))
    }

    @Test
    fun `areEqual returns true for equal integers`() {
        assertTrue(JsonComparisonUtils.areEqual(42, 42))
    }

    @Test
    fun `areEqual returns false for different integers`() {
        assertFalse(JsonComparisonUtils.areEqual(42, 43))
    }

    @Test
    fun `areEqual returns true for equal longs`() {
        assertTrue(JsonComparisonUtils.areEqual(1000L, 1000L))
    }

    @Test
    fun `areEqual returns true for equal doubles`() {
        assertTrue(JsonComparisonUtils.areEqual(3.14, 3.14))
    }

    @Test
    fun `areEqual returns false for different doubles`() {
        assertFalse(JsonComparisonUtils.areEqual(3.14, 3.15))
    }

    @Test
    fun `areEqual returns true for equal booleans`() {
        assertTrue(JsonComparisonUtils.areEqual(true, true))
        assertTrue(JsonComparisonUtils.areEqual(false, false))
    }

    @Test
    fun `areEqual returns false for different booleans`() {
        assertFalse(JsonComparisonUtils.areEqual(true, false))
    }

    @Test
    fun `areEqual returns false for different types`() {
        assertFalse(JsonComparisonUtils.areEqual(42, "42"))
        assertFalse(JsonComparisonUtils.areEqual(1, 1.0))
        assertFalse(JsonComparisonUtils.areEqual(true, "true"))
    }

    // areEqual tests - empty JSONObject
    @Test
    fun `areEqual returns true for two empty JSONObjects`() {
        val obj1 = JSONObject()
        val obj2 = JSONObject()
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns false for empty and non-empty JSONObject`() {
        val obj1 = JSONObject()
        val obj2 = JSONObject().apply { put("key", "value") }
        assertFalse(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns true for JSONObjects with multiple properties`() {
        val obj1 = JSONObject().apply {
            put("name", "John")
            put("age", 30)
            put("active", true)
        }
        val obj2 = JSONObject().apply {
            put("name", "John")
            put("age", 30)
            put("active", true)
        }
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns true for JSONObjects with properties in different order`() {
        val obj1 = JSONObject().apply {
            put("a", 1)
            put("b", 2)
        }
        val obj2 = JSONObject().apply {
            put("b", 2)
            put("a", 1)
        }
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns false for JSONObjects with different number of keys`() {
        val obj1 = JSONObject().apply {
            put("a", 1)
            put("b", 2)
        }
        val obj2 = JSONObject().apply {
            put("a", 1)
        }
        assertFalse(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    // areEqual tests - nested JSONObject
    @Test
    fun `areEqual returns true for deeply nested equal JSONObjects`() {
        val obj1 = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("level3", "value")
                })
            })
        }
        val obj2 = JSONObject().apply {
            put("level1", JSONObject().apply {
                put("level2", JSONObject().apply {
                    put("level3", "value")
                })
            })
        }
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    // areEqual tests - empty JSONArray
    @Test
    fun `areEqual returns true for two empty JSONArrays`() {
        val arr1 = JSONArray()
        val arr2 = JSONArray()
        assertTrue(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns false for empty and non-empty JSONArray`() {
        val arr1 = JSONArray()
        val arr2 = JSONArray().apply { put("value") }
        assertFalse(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns true for JSONArrays with multiple elements`() {
        val arr1 = JSONArray().apply {
            put("a")
            put("b")
            put("c")
        }
        val arr2 = JSONArray().apply {
            put("a")
            put("b")
            put("c")
        }
        assertTrue(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns false for JSONArrays with different element order`() {
        val arr1 = JSONArray().apply {
            put("a")
            put("b")
        }
        val arr2 = JSONArray().apply {
            put("b")
            put("a")
        }
        assertFalse(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns false for JSONArrays with different lengths`() {
        val arr1 = JSONArray().apply {
            put("a")
            put("b")
        }
        val arr2 = JSONArray().apply {
            put("a")
        }
        assertFalse(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns true for JSONArrays with mixed types`() {
        val arr1 = JSONArray().apply {
            put("string")
            put(42)
            put(true)
            put(3.14)
        }
        val arr2 = JSONArray().apply {
            put("string")
            put(42)
            put(true)
            put(3.14)
        }
        assertTrue(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    // areEqual tests - nested JSONArray
    @Test
    fun `areEqual returns true for equal nested JSONArrays`() {
        val arr1 = JSONArray().apply {
            put(JSONArray().apply {
                put("nested1")
                put("nested2")
            })
        }
        val arr2 = JSONArray().apply {
            put(JSONArray().apply {
                put("nested1")
                put("nested2")
            })
        }
        assertTrue(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns false for different nested JSONArrays`() {
        val arr1 = JSONArray().apply {
            put(JSONArray().apply {
                put("nested1")
            })
        }
        val arr2 = JSONArray().apply {
            put(JSONArray().apply {
                put("nested2")
            })
        }
        assertFalse(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    // areEqual tests - JSONArray containing JSONObject
    @Test
    fun `areEqual returns true for JSONArrays containing equal JSONObjects`() {
        val arr1 = JSONArray().apply {
            put(JSONObject().apply {
                put("key", "value")
            })
        }
        val arr2 = JSONArray().apply {
            put(JSONObject().apply {
                put("key", "value")
            })
        }
        assertTrue(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    @Test
    fun `areEqual returns false for JSONArrays containing different JSONObjects`() {
        val arr1 = JSONArray().apply {
            put(JSONObject().apply {
                put("key", "value1")
            })
        }
        val arr2 = JSONArray().apply {
            put(JSONObject().apply {
                put("key", "value2")
            })
        }
        assertFalse(JsonComparisonUtils.areEqual(arr1, arr2))
    }

    // areEqual tests - JSONObject containing JSONArray
    @Test
    fun `areEqual returns true for JSONObjects containing equal JSONArrays`() {
        val obj1 = JSONObject().apply {
            put("array", JSONArray().apply {
                put("a")
                put("b")
            })
        }
        val obj2 = JSONObject().apply {
            put("array", JSONArray().apply {
                put("a")
                put("b")
            })
        }
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns false for JSONObjects containing different JSONArrays`() {
        val obj1 = JSONObject().apply {
            put("array", JSONArray().apply {
                put("a")
            })
        }
        val obj2 = JSONObject().apply {
            put("array", JSONArray().apply {
                put("b")
            })
        }
        assertFalse(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    // areEqual tests - complex nested structures
    @Test
    fun `areEqual returns true for complex equal nested structures`() {
        val obj1 = JSONObject().apply {
            put("string", "value")
            put("number", 42)
            put("nested", JSONObject().apply {
                put("array", JSONArray().apply {
                    put(JSONObject().apply {
                        put("deep", "value")
                    })
                    put("element")
                })
            })
        }
        val obj2 = JSONObject().apply {
            put("string", "value")
            put("number", 42)
            put("nested", JSONObject().apply {
                put("array", JSONArray().apply {
                    put(JSONObject().apply {
                        put("deep", "value")
                    })
                    put("element")
                })
            })
        }
        assertTrue(JsonComparisonUtils.areEqual(obj1, obj2))
    }

    @Test
    fun `areEqual returns false for complex structures with deep difference`() {
        val obj1 = JSONObject().apply {
            put("nested", JSONObject().apply {
                put("array", JSONArray().apply {
                    put(JSONObject().apply {
                        put("deep", "value1")
                    })
                })
            })
        }
        val obj2 = JSONObject().apply {
            put("nested", JSONObject().apply {
                put("array", JSONArray().apply {
                    put(JSONObject().apply {
                        put("deep", "value2")
                    })
                })
            })
        }
        assertFalse(JsonComparisonUtils.areEqual(obj1, obj2))
    }
}
