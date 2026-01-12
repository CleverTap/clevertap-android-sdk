package com.clevertap.android.sdk.validation

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValidationResultStackTest : BaseTestCase() {
    private lateinit var vrStack: ValidationResultStack
    override fun setUp() {
        super.setUp()
        vrStack = ValidationResultStack()
    }

    @Test
    fun test_pushValidationResult_when_functionIsCalledWithValidationResult_should_AddElementAtLast() {
        //input params >>  vr:ValidationResult, cannot be null

        // if current vrStack size is <=50, calling pushValidationResult will simply add another vr in stack.
        repeat(51){index->
            val vr = ValidationResult(index,"validationResult$index")
            vrStack.pushValidationResult(vr)
            val topElement = vrStack.pendingValidationResults.last()

            assertEquals(vr.errorCode,topElement.errorCode)
            assertEquals(vr.errorDesc,topElement.errorDesc)
            assertEquals(vr,topElement)
            assertEquals(index+1,vrStack.pendingValidationResults.size)
        }

        //if current size of the internal stack is greater than 50(i.e 51 or more) it will remove first 10 elements and then add the new pushed element
        val current11thElement = vrStack.pendingValidationResults[10]
        val vr = ValidationResult(-1,"validationResult-1")
        vrStack.pushValidationResult(vr)
        val topElement = vrStack.pendingValidationResults.last()
        assertEquals(vr.errorCode,topElement.errorCode)
        assertEquals(vr.errorDesc,topElement.errorDesc)
        assertEquals(vr,topElement)

        assertEquals(42,vrStack.pendingValidationResults.size)
        assertEquals(current11thElement.errorDesc,vrStack.pendingValidationResults.first().errorDesc)
        assertEquals(current11thElement.errorCode,vrStack.pendingValidationResults.first().errorCode)
    }

    @Test
    fun test_popValidationResult_when_functionIsCalledWith_should_ReturnFirstElementInserted() {
        //if internal stack is empty, then it is going to remove the first element from the start otherwise its going to return null
        assertNull(vrStack.popValidationResult())

        val vr1 = ValidationResult(1,"error1")
        val vr2 = ValidationResult(2,"error2")
        vrStack.pushValidationResult(vr1)
        vrStack.pushValidationResult(vr2)
        val poppedElement = vrStack.popValidationResult()
        assertEquals(vr1.errorCode,poppedElement.errorCode)
        assertEquals(vr1.errorDesc,poppedElement.errorDesc)


    }

    @Test
    fun test_pushValidationResultList_when_functionIsCalledWithNullList_should_NotAddAnyElements() {
        // Arrange
        val initialSize = vrStack.pendingValidationResults.size

        // Act
        vrStack.pushValidationResult(null as List<ValidationResult>?)

        // Assert
        assertEquals(initialSize, vrStack.pendingValidationResults.size)
    }

    @Test
    fun test_pushValidationResultList_when_functionIsCalledWithEmptyList_should_NotAddAnyElements() {
        // Arrange
        val initialSize = vrStack.pendingValidationResults.size

        // Act
        vrStack.pushValidationResult(emptyList())

        // Assert
        assertEquals(initialSize, vrStack.pendingValidationResults.size)
    }

    @Test
    fun test_pushValidationResultList_when_stackSizeUnder50_should_AddAllElementsAtEnd() {
        // Add a few elements to start with
        val vr1 = ValidationResult(1, "error1")
        val vr2 = ValidationResult(2, "error2")
        vrStack.pushValidationResult(vr1)
        vrStack.pushValidationResult(vr2)

        // Create a list of validation results
        val vrList = listOf(
            ValidationResult(10, "error10"),
            ValidationResult(11, "error11"),
            ValidationResult(12, "error12")
        )

        // Act
        vrStack.pushValidationResult(vrList)

        // Assert
        assertEquals(5, vrStack.pendingValidationResults.size)
        assertEquals(vr1, vrStack.pendingValidationResults[0])
        assertEquals(vr2, vrStack.pendingValidationResults[1])
        assertEquals(vrList[0], vrStack.pendingValidationResults[2])
        assertEquals(vrList[1], vrStack.pendingValidationResults[3])
        assertEquals(vrList[2], vrStack.pendingValidationResults[4])
    }


    @Test
    fun test_pushValidationResultList_when_addingListCausesOver50_should_TrimAndKeepLast40() {
        // Fill stack with 48 elements
        repeat(48) { index ->
            vrStack.pushValidationResult(ValidationResult(index, "error$index"))
        }

        // Add a list of 5 elements (total would be 53)
        val vrList = listOf(
            ValidationResult(100, "error100"),
            ValidationResult(101, "error101"),
            ValidationResult(102, "error102"),
            ValidationResult(103, "error103"),
            ValidationResult(104, "error104")
        )

        vrStack.pushValidationResult(vrList)

        // Assert - should have exactly 40 elements
        assertEquals(40, vrStack.pendingValidationResults.size)

        // First element should be from original stack (13 were removed: 53 - 40 = 13)
        assertEquals(13, vrStack.pendingValidationResults.first().errorCode)

        // Last 5 elements should be from the new list
        assertEquals(100, vrStack.pendingValidationResults[35].errorCode)
        assertEquals(101, vrStack.pendingValidationResults[36].errorCode)
        assertEquals(102, vrStack.pendingValidationResults[37].errorCode)
        assertEquals(103, vrStack.pendingValidationResults[38].errorCode)
        assertEquals(104, vrStack.pendingValidationResults[39].errorCode)
    }

    @Test
    fun test_pushValidationResultList_when_stackAt51AndAddingList_should_TrimToLast40() {
        // Fill stack to exactly 51 elements
        repeat(51) { index ->
            vrStack.pushValidationResult(ValidationResult(index, "error$index"))
        }

        // Add a list of 3 elements
        val vrList = listOf(
            ValidationResult(200, "error200"),
            ValidationResult(201, "error201"),
            ValidationResult(202, "error202")
        )

        vrStack.pushValidationResult(vrList)

        // Assert - should have exactly 40 elements
        assertEquals(40, vrStack.pendingValidationResults.size)

        // First element should be index 14 (54 - 40 = 14 elements skipped)
        assertEquals(14, vrStack.pendingValidationResults.first().errorCode)

        // Last 3 elements should be from the new list
        assertEquals(200, vrStack.pendingValidationResults[37].errorCode)
        assertEquals(201, vrStack.pendingValidationResults[38].errorCode)
        assertEquals(202, vrStack.pendingValidationResults[39].errorCode)
    }

    @Test
    fun test_pushValidationResultList_when_listAloneExceeds40_should_KeepOnlyLast40FromList() {
        // Add 10 existing elements
        repeat(10) { index ->
            vrStack.pushValidationResult(ValidationResult(index, "error$index"))
        }

        // Create a list with 50 elements (total would be 60)
        val vrList = (100 until 150).map { ValidationResult(it, "error$it") }

        vrStack.pushValidationResult(vrList)

        // Assert - should have exactly 40 elements
        assertEquals(40, vrStack.pendingValidationResults.size)

        // All elements should be from the new list (last 40 of it)
        // Skip count = 60 - 40 = 20
        // Since skipCount (20) >= currentSize (10), we take last 40 from new list
        assertEquals(110, vrStack.pendingValidationResults.first().errorCode)
        assertEquals(149, vrStack.pendingValidationResults.last().errorCode)
    }

    @Test
    fun test_pushValidationResultList_when_listExactly40AndStackEmpty_should_AddAll40() {
        // Create a list with exactly 40 elements
        val vrList = (0 until 40).map { ValidationResult(it, "error$it") }

        vrStack.pushValidationResult(vrList)

        // Assert
        assertEquals(40, vrStack.pendingValidationResults.size)
        assertEquals(0, vrStack.pendingValidationResults.first().errorCode)
        assertEquals(39, vrStack.pendingValidationResults.last().errorCode)
    }


    @Test
    fun test_pushValidationResultList_when_singleElementList_should_BehaveLikeSinglePush() {
        // Add some existing elements
        repeat(48) { index ->
            vrStack.pushValidationResult(ValidationResult(index, "error$index"))
        }

        // Add single element list
        val vrList = listOf(ValidationResult(100, "error100"))
        vrStack.pushValidationResult(vrList)

        // Assert - should just add the element
        assertEquals(49, vrStack.pendingValidationResults.size)
        assertEquals(100, vrStack.pendingValidationResults.last().errorCode)
    }

    @Test
    fun test_pushValidationResultList_when_addingToStackAt50_should_NotTriggerTrim() {
        // Fill stack to exactly 50 elements
        repeat(50) { index ->
            vrStack.pushValidationResult(ValidationResult(index, "error$index"))
        }

        // Verify stack is at 50
        assertEquals(50, vrStack.pendingValidationResults.size)

        // Add a single element list (total would be 51, which triggers trim)
        val vrList = listOf(ValidationResult(100, "error100"))
        vrStack.pushValidationResult(vrList)

        // Assert - should have trimmed to 40
        assertEquals(40, vrStack.pendingValidationResults.size)
        assertEquals(11, vrStack.pendingValidationResults.first().errorCode)
        assertEquals(100, vrStack.pendingValidationResults.last().errorCode)
    }
}