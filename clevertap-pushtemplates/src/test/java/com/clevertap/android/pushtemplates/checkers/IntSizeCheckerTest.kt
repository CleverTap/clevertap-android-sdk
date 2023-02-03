package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntSizeCheckerTest: BaseTestCase() {

    @Test
    fun test_check_correct_params_returns_true() {
        val entity = 12345
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)
        //when
        val result = intSizeChecker.check()

        //Act
       assertEquals(result,true)
    }

    @Test
    fun test_check_empty_entity_returns_true() {
        val entity = -112345
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)
        //when
        val result = intSizeChecker.check()

        assertEquals(result,false)
    }

    @Test
    fun test_check_entity_is_same_as_int_min_value_returns_false() {
        val entity = Int.MIN_VALUE
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)
        //when
        val result = intSizeChecker.check()

        assertFalse(result)
    }

}