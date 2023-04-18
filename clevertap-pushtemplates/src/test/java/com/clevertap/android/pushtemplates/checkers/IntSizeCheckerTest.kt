package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntSizeCheckerTest: BaseTestCase() {

    @Test
    fun test_check_correctParamsGiven_ReturnsTrue() {
        //When
        val entity = 12345
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)

        //Act
        val result = intSizeChecker.check()

        //Assert
       assertTrue(result)
    }

    @Test
    fun test_check_entityIsLessThanGivenSize_ReturnsFalse() {
        //When
        val entity = -112345
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)

        //Act
        val result = intSizeChecker.check()

        //Assert
        assertFalse(result)
    }

    @Test
    fun test_check_entitySameAsIntMinValue_ReturnsFalse() {
        //When
        val entity = Int.MIN_VALUE
        val size = 1
        val errorMsg = "Sample error message"
        val intSizeChecker = IntSizeChecker(entity,size,errorMsg)

        //Act
        val result = intSizeChecker.check()

        //Assert
        assertFalse(result)
    }

}