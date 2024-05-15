package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.junit.*
import kotlin.test.assertEquals

class JsonUtilsTest {

    @Test
    fun `JSONArray filterObjects should only keep matching objects and ignore other types of elements`() {
        val array = JSONArray("""[{"matching":true}, 5, {"matching":false}, {"number": 5}, false]""")
        val filteredArray = array.filterObjects { element -> element.optBoolean("matching") }

        assertEquals(1, filteredArray.length())
        assertEquals(true, filteredArray.getJSONObject(0).getBoolean("matching"))
    }
}
