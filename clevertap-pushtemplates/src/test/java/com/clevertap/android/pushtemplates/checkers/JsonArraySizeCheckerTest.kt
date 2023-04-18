package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JsonArraySizeCheckerTest: BaseTestCase(){

    @Test
    fun test_check_entityJSONArrayGiven_ReturnsTrue() {
        //When
        val entity = JSONArray().apply {
            put(0,"xyz")
            put(1,"abc")
        }
        val size = 0//Size is not used in this Checker class
        val errorMsg = "Sample error message"
        val jsonArraySizeChecker = JsonArraySizeChecker(entity,size,errorMsg)

        //Act
        val result = jsonArraySizeChecker.check()

        //Assert
        assertTrue(result)
    }

    @Test
    fun test_check_entityJSONArrayNull_ReturnsFalse() {
        //When
        val entity = null
        val size = 0//Size is not used in this Checker class
        val errorMsg = "Sample error message"
        val jsonArraySizeChecker = JsonArraySizeChecker(entity,size,errorMsg)

        //Act
        val result = jsonArraySizeChecker.check()

        //Assert
        assertFalse(result)
    }
}