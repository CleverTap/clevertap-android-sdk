package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ListEqualSizeCheckerTest: BaseTestCase(){

    @Test
    fun test_check_entityListMatchesListSize_ReturnsTrue() {
        //When
        val entity = listOf("xyz","abc","lmn")
        val size = entity.size
        val errorMsg = "Sample error message"
        val listEqualSizeChecker = ListEqualSizeChecker(entity,size,errorMsg)

        //Act
        val result = listEqualSizeChecker.check()

        //Assert
        assertTrue(result)
    }

    @Test
    fun test_check_entityListNotMatchesListSize_ReturnsFalse() {
        //When
        val entity = listOf("xyz","abc","lmn")
        val size = entity.size + 1 //Different size
        val errorMsg = "Sample error message"
        val listEqualSizeChecker = ListEqualSizeChecker(entity,size,errorMsg)

        //Act
        val result = listEqualSizeChecker.check()

        //Assert
        assertFalse(result)
    }

    @Test
    fun test_check_entityListIsNull_ReturnsFalse() {
        //When
        val entity = null
        val size = 4 //Random size
        val errorMsg = "Sample error message"
        val listEqualSizeChecker = ListEqualSizeChecker(entity,size,errorMsg)

        //Act
        val result = listEqualSizeChecker.check()

        //Assert
        assertFalse(result)
    }
}