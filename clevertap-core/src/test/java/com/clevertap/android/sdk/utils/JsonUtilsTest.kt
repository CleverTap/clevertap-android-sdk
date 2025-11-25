package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.junit.*
import kotlin.test.assertEquals

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
                "abTestName" to "My Test"
                "name" to "Variant A"
                "abTestId" to 123L
                "id" to 1234L
            },
            buildMap {
                "abTestName" to "My Test 2"
                "name" to "Variant C"
                "abTestId" to 100L
                "id" to 12344L
            }
        )
        val arrWithJsonTokenizer = JSONArray(list)
        val op = JsonUtil.toJson(list)

        assertEquals(arrWithJsonTokenizer.toString(), op.toString())
    }

    @Test
    fun `JsonUtil-toJson with default Json tokenizer for complex data`() {
        val list: List<Map<String, Any>> = listOf(
            buildMap {
                "primitive" to 1
                "class" to SomeJavaClassForTest()
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
