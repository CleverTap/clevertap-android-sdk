package com.clevertap.android.sdk.utils

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
}
