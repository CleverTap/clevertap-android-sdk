package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListSizeCheckerTest: BaseTestCase(){

    @Test
    fun test_check_entityListLessThanListSize_ReturnsTrue() {
        //When
        val entity = listOf("xyz","abc","lmn")
        val size = entity.size - 1
        val errorMsg = "Sample error message"
        val listSizeChecker = ListSizeChecker(entity,size,errorMsg)

        //Act
        val result = listSizeChecker.check()

        //Assert
        assertTrue(result)
    }

    @Test
    fun test_check_entityListEqualToListSize_ReturnsTrue() {
        //When
        val entity = listOf("xyz","abc","lmn")
        val size = entity.size
        val errorMsg = "Sample error message"
        val listSizeChecker = ListSizeChecker(entity,size,errorMsg)

        //Act
        val result = listSizeChecker.check()

        //Assert
        assertTrue(result)
    }

    @Test
    fun test_check_entityListGreaterThanListSize_ReturnsFalse() {
        //When
        val entity = listOf("xyz","abc","lmn")
        val size = entity.size + 1
        val errorMsg = "Sample error message"
        val listSizeChecker = ListSizeChecker(entity,size,errorMsg)

        //Act
        val result = listSizeChecker.check()

        //Assert
        assertFalse(result)
    }

    @Test
    fun test_check_entityListIsNull_ReturnsFalse() {
        //When
        val entity = null
        val size = 1
        val errorMsg = "Sample error message"
        val listSizeChecker = ListSizeChecker(entity,size,errorMsg)

        //Act
        val result = listSizeChecker.check()

        //Assert
        assertFalse(result)
    }
}