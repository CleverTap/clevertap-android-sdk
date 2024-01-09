package com.clevertap.android.sdk

import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest : BaseTestCase() {

    private lateinit var analyticsManagerSUT: AnalyticsManager
    private lateinit var coreState: MockCoreState
    private lateinit var validator: Validator
    private lateinit var baseEventQueueManager: BaseEventQueueManager

    @Before
    override fun setUp() {
        super.setUp()
        validator = mock(Validator::class.java)
        baseEventQueueManager = mock(EventQueueManager::class.java)
        val inAppResponse = mock(InAppResponse::class.java)
        coreState = MockCoreState(application, cleverTapInstanceConfig)
        analyticsManagerSUT = AnalyticsManager(
            application,
            cleverTapInstanceConfig,
            baseEventQueueManager,
            validator,
            coreState.validationResultStack,
            coreState.coreMetaData,
            coreState.localDataStore,
            coreState.deviceInfo,
            coreState.callbackManager,
            coreState.controllerManager,
            coreState.ctLockManager,
            inAppResponse
        )
    }

    @Test
    fun test_incrementValue_emptyKey_throwsEmptyKeyError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 523

        analyticsManagerSUT.incrementValue("",10)

        `when`(validator.cleanObjectKey(""))
            .thenReturn(validationResult)
    }

    @Test
    fun test_decrementValue_negativeValue_throwsMalformedValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = "abc"
        validationResult.errorCode = 512

        analyticsManagerSUT.decrementValue("abc",-10)

        `when`(validator.cleanObjectKey("abc"))
            .thenReturn(validationResult)
    }

    @Test
    fun test_incrementValue_intValueIsPassed_incrementIntValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "int_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10)
        val updateObj = JSONObject().put("int_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("int_score"))
            .thenReturn(10)
        `when`(validator.cleanObjectKey("int_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.incrementValue("int_score",10)

        verify(coreState.localDataStore).setProfileField("int_score",20)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_incrementValue_doubleValueIsPassed_incrementDoubleValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "double_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25)
        val updateObj = JSONObject().put("double_score", commandObj)
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("double_score"))
            .thenReturn(10.25)
        `when`(validator.cleanObjectKey("double_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.incrementValue("double_score",10.25)

        verify(coreState.localDataStore).setProfileField("double_score",20.5)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_incrementValue_floatValueIsPassed_incrementFloatValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "float_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25f)
        val updateObj = JSONObject().put("float_score", commandObj)
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("float_score"))
            .thenReturn(10.25f)
        `when`(validator.cleanObjectKey("float_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.incrementValue("float_score",10.25f)

        verify(coreState.localDataStore).setProfileField("float_score",20.5f)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_intValueIsPassed_decrementIntValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "decr_int_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10)
        val updateObj = JSONObject().put("decr_int_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_int_score"))
            .thenReturn(30)
        `when`(validator.cleanObjectKey("decr_int_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.decrementValue("decr_int_score",10)

        verify(coreState.localDataStore).setProfileField("decr_int_score",20)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_doubleValueIsPassed_decrementDoubleValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "decr_double_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50)
        val updateObj = JSONObject().put("decr_double_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_double_score"))
            .thenReturn(20.25)
        `when`(validator.cleanObjectKey("decr_double_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.decrementValue("decr_double_score",10.50)

        verify(coreState.localDataStore).setProfileField("decr_double_score",9.75)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_decrementValue_floatValueIsPassed_decrementFloatValue() {
        val validationResult = ValidationResult()
        validationResult.`object` = "decr_float_score"
        validationResult.errorCode = 200

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50f)
        val updateObj = JSONObject().put("decr_float_score", commandObj)

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(coreState.localDataStore.getProfileValueForKey("decr_float_score"))
            .thenReturn(20.25f)
        `when`(validator.cleanObjectKey("decr_float_score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.decrementValue("decr_float_score",10.50f)

        verify(coreState.localDataStore).setProfileField("decr_float_score",9.75f)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture(), anyBoolean())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }

    @Test
    fun test_removeValueForKey_when_key_identity(){

        //Act
        analyticsManagerSUT.removeValueForKey("Identity")

        //Assert
        verify(coreState.localDataStore, never()).removeProfileField("Identity")
    }

    @Test
    fun test_removeValueForKey_when_key_identity_is_lowercase(){
        //Act
        analyticsManagerSUT.removeValueForKey("identity")

        //Assert
        verify(coreState.localDataStore, never()).removeProfileField("identity")
    }

}