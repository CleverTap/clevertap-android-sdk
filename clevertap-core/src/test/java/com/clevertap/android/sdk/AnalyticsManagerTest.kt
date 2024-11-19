package com.clevertap.android.sdk

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.fixtures.CleverTapFixtures
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.sdk.validation.Validator.ValidationContext.Profile
import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.clearStaticMockk
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest {

    private lateinit var analyticsManagerSUT: AnalyticsManager
    private lateinit var coreState: MockCoreStateKotlin
    private val cleverTapInstanceConfig: CleverTapInstanceConfig = CleverTapFixtures.provideCleverTapInstanceConfig()

    @MockK(relaxed = true)
    private lateinit var validator: Validator

    @MockK(relaxed = true)
    private lateinit var validationResultStack: ValidationResultStack

    @MockK(relaxed = true)
    private lateinit var eventQueueManager: BaseEventQueueManager

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var inAppResponse: InAppResponse

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(CTExecutorFactory::class)
        every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
        coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        analyticsManagerSUT = AnalyticsManager(
            context,
            cleverTapInstanceConfig,
            eventQueueManager,
            validator,
            validationResultStack,
            coreState.coreMetaData,
            coreState.deviceInfo,
            coreState.callbackManager,
            coreState.controllerManager,
            coreState.ctLockManager,
            inAppResponse
        ) {
            10000
        }
    }

    @After
    fun tearDown() {
        // confirmVerified(validator, validationResultStack, eventQueueManager, context, inAppResponse)
        clearStaticMockk(CTExecutorFactory::class)
        clearAllMocks()
    }

    @Test
    fun `clevertap does not process push notification viewed event if it is not from clevertap`() {
        val bundle = Bundle().apply {
            putString("some", "random")
            putString("non clevertap", "bundle")
        }

        analyticsManagerSUT.pushNotificationViewedEvent(bundle)

        verify {
            eventQueueManager wasNot called
        }
    }

    @Test
    fun `clevertap does not process push notification viewed event if wzrk_id is not present`() {
        val bundle = Bundle().apply {
            putString("some", "random")
            putString("non clevertap", "bundle")
            putString("wzrk_pid", "pid")
        }

        analyticsManagerSUT.pushNotificationViewedEvent(bundle)
        verify {
            eventQueueManager wasNot called
        }
    }

    @Test
    fun `clevertap does not process duplicate PN viewed within 2 seconds`() {

        // send PN first time
        val bundle = Bundle().apply {
            putString("wzrk_pn", "wzrk_pn")
            putString("wzrk_id", "id")
            putString("wzrk_pid", "pid")
            putString("wzrk_someid", "someid")
        }

        val json = JSONObject().apply {
            put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
            put("evtData", CTJsonConverter.getWzrkFields(bundle))
        }

        analyticsManagerSUT.pushNotificationViewedEvent(bundle)

        verify {
            eventQueueManager.queueEvent(
                context,
                withArg {
                    JSONAssert.assertEquals(json, it, true)
                },
                Constants.NV_EVENT
            )
        }

        // Send duplicate PN
        analyticsManagerSUT.pushNotificationViewedEvent(bundle)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                withArg {
                    JSONAssert.assertEquals(json, it, true)
                },
                Constants.NV_EVENT
            )
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun test_incrementValue_nullKey_noAction() {
        analyticsManagerSUT.incrementValue(null, 10)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_incrementValue_nullValue_noAction() {
        analyticsManagerSUT.incrementValue("abc", null)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_incrementValue_emptyKey_throwsEmptyKeyError() {
        mockCleanObjectKey("", 0)
        analyticsManagerSUT.incrementValue("", 10)

        verify {
            validationResultStack.pushValidationResult(withArg { arg ->
                assertEquals(512, arg.errorCode)
            })
        }
    }

    @Test
    fun test_decrementValue_negativeValue_throwsMalformedValueError() {
        mockCleanObjectKey("abc", 0)
        analyticsManagerSUT.decrementValue("abc", -10)

        verify {
            validationResultStack.pushValidationResult(withArg { arg ->
                assertEquals(512, arg.errorCode)
            })
        }
    }

    @Test
    fun test_incrementValue_intValueIsPassed_incrementIntValue() {
        mockCleanObjectKey("int_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10)
        val updateObj = JSONObject().put("int_score", commandObj)

        every { coreState.localDataStore.getProfileProperty("int_score") } returns 10

        analyticsManagerSUT.incrementValue("int_score", 10)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_incrementValue_doubleValueIsPassed_incrementDoubleValue() {
        mockCleanObjectKey("double_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25)
        val updateObj = JSONObject().put("double_score", commandObj)

        every { coreState.localDataStore.getProfileProperty("double_score") } returns (10.25)

        analyticsManagerSUT.incrementValue("double_score", 10.25)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_incrementValue_floatValueIsPassed_incrementFloatValue() {
        mockCleanObjectKey("float_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25f)
        val updateObj = JSONObject().put("float_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("float_score")
        } returns 10.25f

        analyticsManagerSUT.incrementValue("float_score", 10.25f)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_nullValue_noAction() {
        analyticsManagerSUT.decrementValue("abc", null)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_decrementValue_nullKey_noAction() {
        analyticsManagerSUT.decrementValue(null, 10)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_decrementValue_intValueIsPassed_decrementIntValue() {
        mockCleanObjectKey("decr_int_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10)
        val updateObj = JSONObject().put("decr_int_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_int_score")
        } returns 30

        analyticsManagerSUT.decrementValue("decr_int_score", 10)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_doubleValueIsPassed_decrementDoubleValue() {
        mockCleanObjectKey("decr_double_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50)
        val updateObj = JSONObject().put("decr_double_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_double_score")
        } returns 20.25

        analyticsManagerSUT.decrementValue("decr_double_score", 10.50)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_floatValueIsPassed_decrementFloatValue() {
        mockCleanObjectKey("decr_float_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50f)
        val updateObj = JSONObject().put("decr_float_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_float_score")
        } returns 20.25f

        analyticsManagerSUT.decrementValue("decr_float_score", 10.50f)

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_removeValueForKey_when_key_identity() {

        //Act
        val key = "Identity"

        mockCleanObjectKey(key, 0)

        analyticsManagerSUT.removeValueForKey(key)

        // Verify
        verify(exactly = 0) {
            eventQueueManager.pushBasicProfile(any(), any())
        }
    }

    @Test
    fun test_removeValueForKey_when_key_identity_is_lowercase() {

        val key = "identity"

        mockCleanObjectKey(key, 0)

        //Act
        analyticsManagerSUT.removeValueForKey(key)

        // Assert
        // Verify
        verify(exactly = 0) {
            eventQueueManager.pushBasicProfile(any(), any())
        }
    }

    @Test
    fun test_removeValueForKey_when_NullKey_pushesEmptyKeyError() {
        mockCleanObjectKey("", 0)

        analyticsManagerSUT.removeValueForKey(null)

        verify {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(512, it.errorCode)
            })
        }
    }

    @Test
    fun test_removeValueForKey_when_EmptyKey_pushesEmptyKeyError() {
        mockCleanObjectKey("", 0)

            analyticsManagerSUT.removeValueForKey("")

        // Assert
        verify {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(512, it.errorCode)
            })
        }
    }

    @Test
    fun test_removeValueForKey_when_CorrectKey_pushesBasicProfile() {

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DELETE, true)
        val updateObj = JSONObject().put("abc", commandObj)

        mockCleanObjectKey("abc", 0)

        analyticsManagerSUT.removeValueForKey("abc")

        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(updateObj, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_NullKey_noAction() {
        analyticsManagerSUT.addMultiValuesForKey(null, arrayListOf("a"))

        //Assert
        // Verify
        verify {
            eventQueueManager wasNot called
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_NullValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        analyticsManagerSUT.addMultiValuesForKey("abc", null)

        //Assert
        verify {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(validationResult.errorCode, it.errorCode)
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf())

        //Assert
        verify {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(validationResult.errorCode, it.errorCode)
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_RestrictedMultiValueKey_restrictedError() {
        val validationResult = ValidationResult()
        validationResult.`object` = null
        validationResult.errorCode = 523
        every {
            validator.cleanMultiValuePropertyKey("Name")
        } returns validationResult

        // Act
        analyticsManagerSUT.addMultiValuesForKey("Name", arrayListOf("a"))

        // Check
        verify(exactly = 2) {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(523, it.errorCode)
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyKey_emptyValueError() {
        mockCleanMultiValuePropertyKey("", 0)

            analyticsManagerSUT.addMultiValuesForKey("", arrayListOf("a"))

        // Assert
        verify {
            validationResultStack.pushValidationResult(withArg {
                assertEquals(523, it.errorCode)
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject().apply {
            put(Constants.COMMAND_ADD, JSONArray(arrayListOf("a")))
        }
        val fields = JSONObject().apply {
            put("abc", commandObj)
        }

        mockCleanMultiValuePropertyKey("abc", 0)

        analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf("a"))

        // Assert
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(fields, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_removeMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_REMOVE, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)

        mockCleanMultiValuePropertyKey("abc", 0)

        analyticsManagerSUT.removeMultiValuesForKey("abc", arrayListOf("a"))

        // Assert
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(fields, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_setMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {

        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_SET, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)


        mockCleanMultiValuePropertyKey("abc", 0)

        analyticsManagerSUT.setMultiValuesForKey("abc", arrayListOf("a"))

        //Assert
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(fields, it, true) },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_nullProfile_noAction() {
        analyticsManagerSUT.pushProfile(null)

        //Assert
        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_pushProfile_when_emptyProfile_noAction() {
        analyticsManagerSUT.pushProfile(emptyMap())

        //Assert
        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_pushProfile_when_nullDeviceId_noAction() {
        val profile = mapOf("key1" to "value1", "key2" to "value2")
        every {
            coreState.deviceInfo.deviceID
        } returns null

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Verify
        verify {
            validator wasNot called
        }
    }



    @Test
    fun test_pushProfile_when_validPhone_pushesProfile() {
        val validPhone = "+1234"
        val profile = mapOf("Phone" to validPhone)

        every {
            coreState.deviceInfo.deviceID
        } returns "1234"

        mockCleanObjectKey("Phone", 0)
        mockCleanObjectValue(validPhone, 0)

        analyticsManagerSUT.pushProfile(profile)

        // Checks
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(JSONObject().put("Phone", validPhone), it, true) },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_invalidPhone_pushesErrorAndPushesProfile() {
        val invalidPhone = "1234"
        val profile = mapOf("Phone" to invalidPhone)

        mockCleanObjectKey("Phone", 0)

        mockCleanObjectValue(invalidPhone, 0)

        every { coreState.deviceInfo.deviceID } returns "1234"

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Checks
        verify {
            validationResultStack.pushValidationResult(withArg { assertEquals(512, it.errorCode) })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(JSONObject().put("Phone", invalidPhone), it, true) },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_invalidKeys_pushesPartialProfile() {
        val profile = mapOf("key1" to "value1", "" to "value2")

        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("", 0)

        mockCleanObjectValue("value1", 0)
        mockCleanObjectValue("value2", 0)

        every { coreState.deviceInfo.deviceID } returns "1234"

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Checks
        verify {
            validationResultStack.pushValidationResult(withArg { assertEquals(512, it.errorCode) })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(JSONObject().put("key1", "value1"), it, true) },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_nonPrimitiveValue_pushesPartialProfile() {
        val profile = mapOf("key1" to Validator(), "key2" to "value2")

        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("key2", 0)

        every { coreState.deviceInfo.deviceID }  returns "1234"

        every { validator.cleanObjectValue(any(), any()) }throws IllegalArgumentException()
        mockCleanObjectValue("value2", 0)

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Checks
        verify {
            validationResultStack.pushValidationResult(withArg { assertEquals(512, it.errorCode) })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(JSONObject().put("key2", "value2"), it, true) },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_validProfile_pushesCompleteProfile() {
        val profile = mapOf("key1" to "value1", "key2" to "value2")
        mockCleanObjectKey("key1", 0)
        mockCleanObjectKey("key2", 0)

        mockCleanObjectValue("value1", 0)
        mockCleanObjectValue("value2", 0)

        every { coreState.deviceInfo.deviceID } returns "1234"

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Verify
        verify {
            eventQueueManager.pushBasicProfile(
                withArg { JSONAssert.assertEquals(JSONObject().put("key1", "value1").put("key2", "value2"), it, true) },
                any()
            )
        }
    }

    private fun mockCleanObjectKey(key: String?, errCode: Int) {

        every {
            validator.cleanObjectKey(key)
        } returns ValidationResult().apply {
            `object` = key
            errorCode = errCode
        }
    }

    private fun mockCleanObjectValue(value: String?, errCode: Int) {

        every {
            validator.cleanObjectValue(value, Profile)
        } returns ValidationResult().apply {
            `object` = value
            errorCode = errCode
        }
    }

    private fun mockCleanMultiValuePropertyKey(key: String?, errCode: Int) {

        every {
            validator.cleanMultiValuePropertyKey(key)
        } returns ValidationResult().apply {
            `object` = key
            errorCode = errCode
        }
    }
}