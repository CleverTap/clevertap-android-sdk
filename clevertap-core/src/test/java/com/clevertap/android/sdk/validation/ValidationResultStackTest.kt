package com.clevertap.android.sdk.validation

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
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
}