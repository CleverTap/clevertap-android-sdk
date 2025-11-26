package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class JsonUtilsTest {

    @Test
    fun prependTest() {
        val expected = JSONArray("[0,1,2,3,4,5]")
        val array = JSONArray("[1,2,3,4,5]")
        array.prepend(0)
        assertEquals(expected.toString(), array.toString())
    }

    @Test
    fun `JSONArray filterObjects should only keep matching objects and ignore other types of elements`() {
        val array = JSONArray("""[{"matching":true}, 5, {"matching":false}, {"number": 5}, false]""")
        val filteredArray = array.filterObjects { element -> element.optBoolean("matching") }

        assertEquals(1, filteredArray.length())
        assertEquals(true, filteredArray.getJSONObject(0).getBoolean("matching"))
    }

    @Test
    fun `JsonUtil-toJson with default Json tokenizer for primitive data`() {
        val list: List<Map<String, Any>> = listOf(
            buildMap {
                put("abTestName", "My Test")
                put("name", "Variant A")
                put("abTestId", 123L)
                put("id", 1234L)
            },
            buildMap {
                put("abTestName", "My Test 2")
                put("name", "Variant C")
                put("abTestId", 100L)
                put("id", 12344L)
            }
        )
        val arrWithJsonTokenizer = JSONArray(list)
        val op = JsonUtil.toJson(list)

        assertEquals(arrWithJsonTokenizer.toString(), op.toString())
    }

    @Test
    fun `JsonUtil-toJson comparision with default Json tokenizer with custom class data`() {
        val list = listOf(SomeJavaClassForTest())
        val arrWithJsonTokenizer = JSONArray(list)
        val op = JsonUtil.toJson(list)

        assertNotEquals(arrWithJsonTokenizer.toString(), op.toString())
    }

    @Test
    fun `JsonUtil-toJson comparision with default Json tokenizer for complex data`() {
        val list: List<Map<String, Any>> = listOf(
            buildMap {
                put("primitiveInt", 1)
                put("primitiveString", "av#3ury43iq---!!@")
                put("primitiveDouble", 3.333)
                put("primitiveLong", 4359L)
                put("primitiveShort", 1.toShort())
                put("primitiveByte", 10.toByte())
                put("primitiveChar", 10.toChar())
                put("max", Long.MAX_VALUE)
                put("map", buildMap {
                    put("a", 1.1)
                    put("b", 2L)
                    put("c", 22)
                    put("d", "abc")
                    put("e", false)
                    put("f", buildMap {
                        put("here", "we go again")
                    })
                    put("g", listOf(1, 3L))
                })
                put("list", listOf<Any>(1, "abc", 5L, 3.33, 4359L, 1.toShort(), 1.toByte(), 12.toChar()))
            }
        )
        val arrWithJsonTokenizer = JSONArray(list)
        val op = JsonUtil.toJson(list)

        assertEquals(arrWithJsonTokenizer.toString(), op.toString())
    }

    @Test
    fun `JsonUtil-listFromJsonFromDefault check for non null return`() {

        // Act
        val op = JsonUtil.listFromJsonFromDefault<String>(null)

        // Assert
        assertEquals(emptyList<String>(), op)
    }
}

class SomeJavaClassForTest{
    val x: String = "abc"
    val y = 1L
    val z = 12
}
