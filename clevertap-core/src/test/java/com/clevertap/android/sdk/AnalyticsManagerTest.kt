package com.clevertap.android.sdk

import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest : BaseTestCase() {

    private lateinit var analyticsManagerSUT: AnalyticsManager
    private lateinit var corestate: MockCoreState
    private lateinit var validator: Validator
    private lateinit var baseEventQueueManager: BaseEventQueueManager

    @Before
    override fun setUp() {
        super.setUp()
        validator = Mockito.mock(Validator::class.java)
        baseEventQueueManager= Mockito.mock(EventQueueManager::class.java)
        corestate = MockCoreState(application, cleverTapInstanceConfig)
        analyticsManagerSUT = AnalyticsManager(application,cleverTapInstanceConfig,
            baseEventQueueManager,validator,corestate.validationResultStack,
            corestate.coreMetaData, corestate.localDataStore,corestate.deviceInfo,
            corestate.callbackManager,corestate.controllerManager,corestate.ctLockManager)
    }

    @Test
    fun test_incrementValue_intValueIsPassed_incrementIntValue() {
        val validationResult = ValidationResult()
        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 20)
        val updateObj = JSONObject().put("score", commandObj)
        validationResult.`object` = "score"
        validationResult.errorCode = 11

        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        `when`(corestate.localDataStore.getProfileValueForKey("score"))
            .thenReturn(10)
        `when`(validator.cleanObjectKey("score"))
            .thenReturn(validationResult)

        analyticsManagerSUT.incrementValue("score",10)

        verify(corestate.localDataStore).setProfileField("score",20)
        verify(baseEventQueueManager).pushBasicProfile(captor.capture())
        JSONAssert.assertEquals(updateObj, captor.value, true)
    }
}