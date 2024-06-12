package com.clevertap.android.sdk

import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONArray
import org.junit.*
import org.junit.Assert.*

class ProfileValueHandlerTest : BaseTestCase() {

    @MockK(relaxed = true)
    private lateinit var validator: Validator

    @MockK(relaxed = true)
    private lateinit var validationResultStack: ValidationResultStack

    private lateinit var profileValueHandler: ProfileValueHandler
    override fun setUp() {
        super.setUp()
        MockKAnnotations.init(this)
        profileValueHandler = ProfileValueHandler(validator, validationResultStack)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_DoubleValue_Increment() {
        val value = 2.5
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_FloatValue_Increment() {
        val value = 2.5f
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_IntValue_Increment() {
        val value = 2
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_DoubleValue_Decrement() {
        val value = 2.5
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == -value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_FloatValue_Decrement() {
        val value = 2.5f
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == -value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValueNull_IntValue_Decrement() {
        val value = 2
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, null)

        assert(result == -value)
        verify { validator wasNot called }
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_DoubleValue_Increment() {

        val existingValue = 3.0
        val value = 2.5
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue + value)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_FloatValue_Increment() {

        val existingValue = 3.0f
        val value = 2.5f
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue + value)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_IntValue_Increment() {

        val existingValue = 3
        val value = 2
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue + value)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_DoubleValue_Decrement() {

        val existingValue = 3.0
        val value = 2.5
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue - value)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_FloatValue_Decrement() {

        val existingValue = 3.0f
        val value = 2.5f
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue - value)
    }

    @Test
    fun testHandleIncrementDecrementValues_ExistingValuePresent_IntValue_Decrement() {
        val existingValue = 3
        val value = 2
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == existingValue - value)
    }

    @Test
    fun testHandleIncrementDecrementValues_IntExistingValue_FloatCurrentValue_Decrement() {
        val existingValue = 3
        val value = 2.5f
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1)
    }

    @Test
    fun testHandleIncrementDecrementValues_IntExistingValue_DoubleCurrentValue_Decrement() {
        val existingValue = 3
        val value = 2.5
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1)
    }

    @Test
    fun testHandleIncrementDecrementValues_FloatExistingValue_IntCurrentValue_Decrement() {
        val existingValue = 3.5f
        val value = 2
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1.5f)
    }

    @Test
    fun testHandleIncrementDecrementValues_FloatExistingValue_DoubleCurrentValue_Decrement() {
        val existingValue = 3.5f
        val value = 2.0
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1.5f)
    }

    @Test
    fun testHandleIncrementDecrementValues_DoubleExistingValue_IntCurrentValue_Decrement() {
        val existingValue = 3.5
        val value = 2
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1.5)
    }

    @Test
    fun testHandleIncrementDecrementValues_DoubleExistingValue_FloatCurrentValue_Decrement() {
        val existingValue = 3.5
        val value = 2f
        val command = Constants.COMMAND_DECREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 1.5)
    }

    @Test
    fun testHandleIncrementDecrementValues_IntExistingValue_FloatCurrentValue_Increment() {
        val existingValue = 3
        val value = 2.5f
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5)
    }

    @Test
    fun testHandleIncrementDecrementValues_IntExistingValue_DoubleCurrentValue_Increment() {
        val existingValue = 3
        val value = 2.5
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5)
    }

    @Test
    fun testHandleIncrementDecrementValues_FloatExistingValue_IntCurrentValue_Increment() {
        val existingValue = 3.5f
        val value = 2
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5.5f)
    }

    @Test
    fun testHandleIncrementDecrementValues_FloatExistingValue_DoubleCurrentValue_Increment() {
        val existingValue = 3.5f
        val value = 2.0
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5.5f)
    }

    @Test
    fun testHandleIncrementDecrementValues_DoubleExistingValue_IntCurrentValue_Increment() {
        val existingValue = 3.5
        val value = 2
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5.5)
    }

    @Test
    fun testHandleIncrementDecrementValues_DoubleExistingValue_FloatCurrentValue_Increment() {
        val existingValue = 3.5
        val value = 2f
        val command = Constants.COMMAND_INCREMENT

        val result = profileValueHandler.handleIncrementDecrementValues(value, command, existingValue)

        assert(result == 5.5)
    }

    @Test
    fun testHandleMultiValues_ExistingValues_ADD() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues = JSONArray().put("existingValue")
        val command = Constants.COMMAND_ADD

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("existingValue").put("value1").put("value2")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("existingValue").put("value1").put("value2").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_ExistingValues_SET() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues = JSONArray().put("existingValue")
        val command = Constants.COMMAND_ADD

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray()
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("existingValue")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("existingValue").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_ExistingValues_REMOVE() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1")
        val existingValues = JSONArray().put("value1").put("value2")
        val command = Constants.COMMAND_REMOVE

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("value2")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("value2").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_ExistingValues_REMOVE_AllValuesRemoved() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues = JSONArray().put("value1").put("value2")
        val command = Constants.COMMAND_REMOVE

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray()
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertNull(result)
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_NoExistingValues_ADD() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues: JSONArray? = null
        val command = Constants.COMMAND_ADD

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }
        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("value1").put("value2")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("value1").put("value2").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_NoExistingValues_SET() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues: JSONArray? = null
        val command = Constants.COMMAND_SET

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }
        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("value1").put("value2")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("value1").put("value2").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_NoExistingValues_REMOVE() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues: JSONArray? = null
        val command = Constants.COMMAND_REMOVE

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertNull(result)
        verify(exactly = 0) { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) }
    }

    @Test
    fun testHandleMultiValues_InvalidValues_ADD() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues: JSONArray? = null
        val command = Constants.COMMAND_ADD

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("a".repeat(525))
                    errorCode = 521
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("a".repeat(Constants.MAX_MULTI_VALUE_LENGTH))
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("a".repeat(Constants.MAX_MULTI_VALUE_LENGTH)).toString(), result.toString())
        verify(exactly = 2) { validationResultStack.pushValidationResult(any()) }
    }


    @Test
    fun testHandleMultiValues_ScalarExistingValues_ADD() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1").put("value2")
        val existingValues = 3
        val command = Constants.COMMAND_ADD

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray().put("value1").put("value2").put("3")
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertEquals(JSONArray().put("value1").put("value2").put("3").toString(), result.toString())
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    @Test
    fun testHandleMultiValues_ScalarExistingValues_REMOVE() {
        // Mocking necessary dependencies
        val key = "testKey"
        val values = JSONArray().put("value1")
        val existingValues = "value1"
        val command = Constants.COMMAND_REMOVE

        every { validator.cleanMultiValuePropertyValue(any()) } returns
                ValidationResult().apply {
                    `object` = values
                    errorCode = 0
                }

        every { validator.mergeMultiValuePropertyForKey(any(), any(), any(), any()) } returns
                ValidationResult().apply {
                    `object` = JSONArray()
                    errorCode = 0
                }

        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Execute the method
        val result = profileValueHandler.handleMultiValues(key, values, command, existingValues)

        // Verify the result
        assertNull(result)
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }
}