package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class StringSizeCheckerTest: BaseTestCase(){

    @Test
    fun test_check_entityStringSizeIsLessThanStringLength_ReturnsTrue() {
        //When
        val entity = "abc_xyz"
        val size = entity.trim().length - 1
        val errorMsg = "Sample error message"
        val stringSizeChecker = StringSizeChecker(entity,size,errorMsg)

        //Act
        val result = stringSizeChecker.check()

        //Assert
        assertTrue(result)
    }

    @Test
    fun test_check_entityStringSizeIsEqualToStringLength_ReturnsFalse() {
        //When
        val entity = "abc_xyz"
        val size = entity.trim().length
        val errorMsg = "Sample error message"
        val stringSizeChecker = StringSizeChecker(entity,size,errorMsg)

        //Act
        val result = stringSizeChecker.check()

        //Assert
        assertFalse(result)
    }

    @Test
    fun test_check_entityStringSizeIsGreaterThanStringLength_ReturnsFalse() {
        //When
        val entity = "abc_xyz"
        val size = entity.trim().length + 1
        val errorMsg = "Sample error message"
        val stringSizeChecker = StringSizeChecker(entity,size,errorMsg)

        //Act
        val result = stringSizeChecker.check()

        //Assert
        assertFalse(result)
    }

    @Test
    fun test_check_entityStringIsEmpty_ReturnsFalse() {
        //When
        val entity = ""
        val size = 3
        val errorMsg = "Sample error message"
        val stringSizeChecker = StringSizeChecker(entity,size,errorMsg)

        //Act
        val result = stringSizeChecker.check()

        //Assert
        assertFalse(result)
    }
}