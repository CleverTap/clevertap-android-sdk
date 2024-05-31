package com.clevertap.android.sdk

import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.sdk.validation.Validator.ValidationContext.Profile
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest : BaseTestCase() {

    private lateinit var analyticsManagerSUT: AnalyticsManager
    private lateinit var coreState: MockCoreState
    private lateinit var validator: Validator
    private lateinit var validationResultStack: ValidationResultStack
    private lateinit var baseEventQueueManager: BaseEventQueueManager

    @Before
    override fun setUp() {
        super.setUp()
        validator = mock(Validator::class.java)
        validationResultStack = mock(ValidationResultStack::class.java)
        baseEventQueueManager = mock(EventQueueManager::class.java)
        val inAppResponse = mock(InAppResponse::class.java)
        coreState = MockCoreState(application, cleverTapInstanceConfig)
        analyticsManagerSUT = AnalyticsManager(
            application,
            cleverTapInstanceConfig,
            baseEventQueueManager,
            validator,
            validationResultStack,
            coreState.coreMetaData,
            coreState.deviceInfo,
            coreState.callbackManager,
            coreState.controllerManager,
            coreState.ctLockManager,
            inAppResponse
        )
    }

    @Test
    fun test_incrementValue_nullKey_noAction() {
        analyticsManagerSUT.incrementValue(null, 10)

        verifyNoInteractions(validator)
    }

    @Test
    fun test_incrementValue_nullValue_noAction() {
        analyticsManagerSUT.incrementValue("abc", null)

        verifyNoInteractions(validator)
    }

    @Test
    fun test_incrementValue_emptyKey_throwsEmptyKeyError() {
        mockCleanObjectKey("", 0)
        analyticsManagerSUT.incrementValue("", 10)

        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())

        assertEquals(512, captor.value.errorCode)
    }

    @Test
    fun test_decrementValue_negativeValue_throwsMalformedValueError() {
        mockCleanObjectKey("abc", 0)
        analyticsManagerSUT.decrementValue("abc", -10)

        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())

        assertEquals(512, captor.value.errorCode)
    }

    @Test
    fun test_incrementValue_intValueIsPassed_incrementIntValue() {
        mockCleanObjectKey("int_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10)
        val updateObj = JSONObject().put("int_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("int_score"))
            .thenReturn(10)

        analyticsManagerSUT.incrementValue("int_score", 10)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_incrementValue_doubleValueIsPassed_incrementDoubleValue() {
        mockCleanObjectKey("double_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25)
        val updateObj = JSONObject().put("double_score", commandObj)
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("double_score"))
            .thenReturn(10.25)

        analyticsManagerSUT.incrementValue("double_score", 10.25)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_incrementValue_floatValueIsPassed_incrementFloatValue() {
        mockCleanObjectKey("float_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25f)
        val updateObj = JSONObject().put("float_score", commandObj)
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("float_score"))
            .thenReturn(10.25f)

        analyticsManagerSUT.incrementValue("float_score", 10.25f)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_nullValue_noAction() {
        analyticsManagerSUT.decrementValue("abc", null)

        verifyNoInteractions(validator)
    }

    @Test
    fun test_decrementValue_nullKey_noAction() {
        analyticsManagerSUT.decrementValue(null, 10)

        verifyNoInteractions(validator)
    }

    @Test
    fun test_decrementValue_intValueIsPassed_decrementIntValue() {
        mockCleanObjectKey("decr_int_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10)
        val updateObj = JSONObject().put("decr_int_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_int_score"))
            .thenReturn(30)

        analyticsManagerSUT.decrementValue("decr_int_score", 10)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_doubleValueIsPassed_decrementDoubleValue() {
        mockCleanObjectKey("decr_double_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50)
        val updateObj = JSONObject().put("decr_double_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_double_score"))
            .thenReturn(20.25)

        analyticsManagerSUT.decrementValue("decr_double_score", 10.50)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_floatValueIsPassed_decrementFloatValue() {
        mockCleanObjectKey("decr_float_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50f)
        val updateObj = JSONObject().put("decr_float_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_float_score"))
            .thenReturn(20.25f)

        analyticsManagerSUT.decrementValue("decr_float_score", 10.50f)

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_removeValueForKey_when_key_identity() {

        //Act
        analyticsManagerSUT.removeValueForKey("Identity")

        //Assert
        verify(baseEventQueueManager, never()).pushBasicProfile(any(), anyBoolean())
    }

    @Test
    fun test_removeValueForKey_when_key_identity_is_lowercase() {
        //Act
        analyticsManagerSUT.removeValueForKey("identity")

        //Assert
        verify(baseEventQueueManager, never()).pushBasicProfile(any(), anyBoolean())
    }

    @Test
    fun test_removeValueForKey_when_NullKey_pushesEmptyKeyError() {
        mockCleanObjectKey("", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.removeValueForKey(null)
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())
        assertEquals(512, captor.value.errorCode)
    }

    @Test
    fun test_removeValueForKey_when_EmptyKey_pushesEmptyKeyError() {
        mockCleanObjectKey("", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.removeValueForKey("")
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())
        assertEquals(512, captor.value.errorCode)
    }

    @Test
    fun test_removeValueForKey_when_CorrectKey_pushesBasicProfile() {

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DELETE, true)
        val updateObj = JSONObject().put("abc", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        mockCleanObjectKey("abc", 0)

        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.removeValueForKey("abc")
        }

        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_addMultiValuesForKey_when_NullKey_noAction() {
        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey(null, arrayListOf("a"))
        }

        //Assert
        verify(baseEventQueueManager, never()).pushBasicProfile(any(), anyBoolean())
    }

    @Test
    fun test_addMultiValuesForKey_when_NullValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey("abc", null)
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())
        assertEquals(validationResult.errorCode, captor.value.errorCode)
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf())
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())
        assertEquals(validationResult.errorCode, captor.value.errorCode)
    }

    @Test
    fun test_addMultiValuesForKey_when_RestrictedMultiValueKey_restrictedError() {
        val validationResult = ValidationResult()
        validationResult.`object` = null
        validationResult.errorCode = 523
        `when`(validator.cleanMultiValuePropertyKey("Name"))
            .thenReturn(validationResult)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey("Name", arrayListOf("a"))
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack, times(2)).pushValidationResult(captor.capture())
        assertEquals(523, captor.firstValue.errorCode)
        assertEquals(523, captor.secondValue.errorCode)
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyKey_emptyValueError() {
        mockCleanMultiValuePropertyKey("", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey("", arrayListOf("a"))
        }

        //Assert
        val captor = ArgumentCaptor.forClass(ValidationResult::class.java)
        verify(validationResultStack).pushValidationResult(captor.capture())
        assertEquals(523, captor.firstValue.errorCode)
    }

    @Test
    fun test_addMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_ADD, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)

        mockCleanMultiValuePropertyKey("abc", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf("a"))
        }

        //Assert
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(fields, captor.value, true)
    }

    @Test
    fun test_removeMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_REMOVE, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)

        mockCleanMultiValuePropertyKey("abc", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.removeMultiValuesForKey("abc", arrayListOf("a"))
        }

        //Assert
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(fields, captor.value, true)
    }

    @Test
    fun test_setMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {

        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_SET, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)


        mockCleanMultiValuePropertyKey("abc", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.setMultiValuesForKey("abc", arrayListOf("a"))
        }

        //Assert
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(fields, captor.value, true)
    }

    @Test
    fun test_pushProfile_when_nullProfile_noAction() {
        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(null)
        }

        //Assert
        verifyNoInteractions(validator)
    }

    @Test
    fun test_pushProfile_when_emptyProfile_noAction() {
        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(emptyMap())
        }

        //Assert
        verifyNoInteractions(validator)
    }

    @Test
    fun test_pushProfile_when_validPhone_pushesProfile() {
        val validPhone = "+1234"
        val profile = mapOf("Phone" to validPhone)

        mockCleanObjectKey("Phone", 0)
        mockCleanObjectValue(validPhone, 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(profile)
        }

        //Assert
        val basicProfileCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

        verify(baseEventQueueManager).pushBasicProfile(basicProfileCaptor.capture(), anyBoolean())
        JSONAssert.assertEquals(JSONObject().put("Phone", validPhone), basicProfileCaptor.firstValue, true)
    }

    @Test
    fun test_pushProfile_when_invalidPhone_pushesErrorAndPushesProfile() {
        val invalidPhone = "1234"
        val profile = mapOf("Phone" to invalidPhone)

        mockCleanObjectKey("Phone", 0)

        mockCleanObjectValue(invalidPhone, 0)
        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(profile)
        }

        //Assert
        val validationResultCaptor = ArgumentCaptor.forClass(ValidationResult::class.java)
        val basicProfileCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

        verify(validationResultStack).pushValidationResult(validationResultCaptor.capture())
        verify(baseEventQueueManager).pushBasicProfile(basicProfileCaptor.capture(), anyBoolean())
        assertEquals(512, validationResultCaptor.firstValue.errorCode)
        JSONAssert.assertEquals(JSONObject().put("Phone", invalidPhone), basicProfileCaptor.firstValue, true)
    }

    @Test
    fun test_pushProfile_when_invalidKeys_pushesPartialProfile() {
        val profile = mapOf("key1" to "value1", "" to "value2")

        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("", 0)

        mockCleanObjectValue("value1", 0)
        mockCleanObjectValue("value2", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(profile)
        }

        //Assert
        val validationResultCaptor = ArgumentCaptor.forClass(ValidationResult::class.java)
        val basicProfileCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

        verify(validationResultStack).pushValidationResult(validationResultCaptor.capture())
        verify(baseEventQueueManager).pushBasicProfile(basicProfileCaptor.capture(), anyBoolean())
        assertEquals(512, validationResultCaptor.value.errorCode)
        JSONAssert.assertEquals(JSONObject().put("key1", "value1"), basicProfileCaptor.firstValue, true)
    }

    @Test
    fun test_pushProfile_when_nonPrimitiveValue_pushesPartialProfile() {
        val profile = mapOf("key1" to Validator(), "key2" to "value2")

        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("key2", 0)

        `when`(validator.cleanObjectValue(any(Validator::class.java), any()))
            .thenThrow(IllegalArgumentException())
        mockCleanObjectValue("value2", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(profile)
        }

        //Assert
        val validationResultCaptor = ArgumentCaptor.forClass(ValidationResult::class.java)
        val basicProfileCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

        verify(validationResultStack).pushValidationResult(validationResultCaptor.capture())
        verify(baseEventQueueManager).pushBasicProfile(basicProfileCaptor.capture(), anyBoolean())
        assertEquals(512, validationResultCaptor.value.errorCode)
        JSONAssert.assertEquals(JSONObject().put("key2", "value2"), basicProfileCaptor.firstValue, true)
    }

    @Test
    fun test_pushProfile_when_validProfile_pushesCompleteProfile() {
        val profile = mapOf("key1" to "value1", "key2" to "value2")
        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("key2", 0)

        mockCleanObjectValue("value1", 0)
        mockCleanObjectValue("value2", 0)

        //Act
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            analyticsManagerSUT.pushProfile(profile)
        }

        //Assert
        val basicProfileCaptor = ArgumentCaptor.forClass(JSONObject::class.java)

        verify(baseEventQueueManager).pushBasicProfile(basicProfileCaptor.capture(), anyBoolean())
        JSONAssert.assertEquals(
            JSONObject().put("key1", "value1").put("key2", "value2"),
            basicProfileCaptor.firstValue,
            true
        )
    }

    private fun mockCleanObjectKey(key: String?, errCode: Int) {
        `when`(validator.cleanObjectKey(key))
            .thenReturn(ValidationResult().apply {
                `object` = key
                errorCode = errCode
            })
    }

    private fun mockCleanObjectValue(value: String?, errCode: Int) {
        `when`(validator.cleanObjectValue(value, Profile))
            .thenReturn(ValidationResult().apply {
                `object` = value
                errorCode = errCode
            })
    }

    private fun mockCleanMultiValuePropertyKey(key: String?, errCode: Int) {
        `when`(validator.cleanMultiValuePropertyKey(key))
            .thenReturn(ValidationResult().apply {
                `object` = key
                errorCode = errCode
            })
    }
}