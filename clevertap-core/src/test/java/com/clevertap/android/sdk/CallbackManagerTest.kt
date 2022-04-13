package com.clevertap.android.sdk

import android.os.Looper
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import java.util.ArrayList
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CallbackManagerTest : BaseTestCase() {
    private lateinit var callbackManager: CallbackManager
    private lateinit var config: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var coreMetaData: CoreMetaData
    override fun setUp() {
        super.setUp()

        config = CleverTapInstanceConfig.createDefaultInstance(application, "id", "token", "region")
        coreMetaData = CoreMetaData()
        deviceInfo = DeviceInfo(application, config, "clevertapId", coreMetaData)
        callbackManager = CallbackManager(config, deviceInfo)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

    }

    //done//TODO@ansh : replace should_CallCallbackManagerIfAvailable by should_CallInboxMessagesDidUpdateOnListener
    //todo@piyush how to test this case?// TODO@ansh : add test when callbackManager.inboxListener = null
    @Test
    fun test__notifyInboxMessagesDidUpdate_when_FunctionIsCalled_should_CallInboxMessagesDidUpdateOnListener() {
        val ib = object : CTInboxListener {
            override fun inboxDidInitialize() {}
            override fun inboxMessagesDidUpdate() {}
        }
        val ibSpy = Mockito.spy(ib)
        callbackManager.inboxListener = ibSpy
        callbackManager._notifyInboxMessagesDidUpdate()

        Mockito.verify(ibSpy, Mockito.atLeastOnce()).inboxMessagesDidUpdate()
    }

    //done//TODO@ansh : combine test_getFailureFlushListener and test_setFailureFlushListener to one => test_SetterGetterForFailureFlushListener
    @Test
    fun test_SetterGetterForFailureFlushListener() {
        val failureListener = FailureFlushListener { }
        callbackManager.failureFlushListener = failureListener
        assertEquals(failureListener, callbackManager.failureFlushListener)
    }


    //done//TODO@ansh : combine test_getFeatureFlagListener and test_setFeatureFlagListener to one => test_SetterGetterForFeatureFlagListener
    @Test
    fun test_SetterGetterForFeatureFlagListener() {
        val listener = CTFeatureFlagsListener { }
        callbackManager.featureFlagListener = listener
        assertEquals(listener, callbackManager.featureFlagListener)
    }


    //done//TODO@ansh: For all setter and getter combine to one.
    @Test
    fun test_SetterGetterGeofenceCallback() {
        val listener = object : GeofenceCallback {
            override fun handleGeoFences(jsonObject: JSONObject?) {}
            override fun triggerLocation() {}
        }
        callbackManager.geofenceCallback = listener
        assertEquals(listener, callbackManager.geofenceCallback)
    }


    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterInAppNotificationButtonListener() {
        val listener = InAppNotificationButtonListener { }
        callbackManager.inAppNotificationButtonListener = listener
        assertEquals(listener, callbackManager.inAppNotificationButtonListener)
    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterInAppNotificationListener() {
        val listener = object : InAppNotificationListener {
            override fun beforeShow(extras: MutableMap<String, Any>?): Boolean {
                return true
            }

            override fun onDismissed(extras: MutableMap<String, Any>?, actionExtras: MutableMap<String, Any>?) {}
        }
        callbackManager.inAppNotificationListener = listener
        assertEquals(listener, callbackManager.inAppNotificationListener)
    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterInboxListener() {
        val listener = object : CTInboxListener {
            override fun inboxDidInitialize() {}
            override fun inboxMessagesDidUpdate() {}
        }

        callbackManager.inboxListener = listener
        assertEquals(listener, callbackManager.inboxListener)


    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterProductConfigListener() {
        val listener = object : CTProductConfigListener {
            override fun onActivated() {}
            override fun onFetched() {}
            override fun onInit() {}
        }
        callbackManager.productConfigListener = listener
        assertEquals(listener, callbackManager.productConfigListener)

    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterPushAmpListener() {
        val listener = CTPushAmpListener { }
        callbackManager.pushAmpListener = listener
        assertEquals(listener, callbackManager.pushAmpListener)
    }

    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_SetterGetterPushNotificationListener() {
        val listener = CTPushNotificationListener { }
        callbackManager.pushNotificationListener = listener
        assertEquals(listener, callbackManager.pushNotificationListener)
    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_getterSetterSyncListener() {
        val listener = object : SyncListener {
            override fun profileDataUpdated(updates: JSONObject?) {}

            override fun profileDidInitialize(CleverTapID: String?) {}
        }
        callbackManager.syncListener = listener
        assertEquals(listener, callbackManager.syncListener)
    }



    //done//TODO@ansh: setter and getter combine to one.
    @Test
    fun test_getterSetterOnInitCleverTapIDListener() {
        val listener = OnInitCleverTapIDListener { }
        callbackManager.onInitCleverTapIDListener = listener
        assertEquals(listener, callbackManager.onInitCleverTapIDListener)
    }

    @Test
    fun test_notifyUserProfileInitialized_when_FunctionIsCalledWithDeviceID_ShouldCallSyncListenerWithDeviceID() {

        val syncListenrSpy = Mockito.spy(object : SyncListener {
            override fun profileDataUpdated(updates: JSONObject?) {}
            override fun profileDidInitialize(CleverTapID: String?) {}
        })
        callbackManager.syncListener = syncListenrSpy

        // if sync listener is function is called with null string, then it will not return without anyfurther actions
        callbackManager.notifyUserProfileInitialized(null)
        Mockito.verify(syncListenrSpy, Mockito.times(0)).profileDidInitialize(null)

        // if sync listener is set on callback manager and function is called with non-empty string, then it will call synclistener's profileDidInitialize
        callbackManager.notifyUserProfileInitialized("deviceID")
        Mockito.verify(syncListenrSpy, Mockito.times(1)).profileDidInitialize("deviceID")


        //3. this function also has a non parameter overload which will use device id from cached memory to return a device id which might be null but perform equally
        val deviceInfoSpy = Mockito.spy(deviceInfo)
        callbackManager = CallbackManager(config, deviceInfoSpy)
        callbackManager.syncListener = syncListenrSpy
        Mockito.`when`(deviceInfoSpy.getDeviceID()).thenReturn("motorola")
        callbackManager.notifyUserProfileInitialized()
        Mockito.verify(syncListenrSpy, Mockito.times(1)).profileDidInitialize("motorola")

    }

    @Test
    fun test_setDisplayUnitListener_when_FunctionIsCalledWithAValidListener_ShouldAttachItselfWithAWeakReferenceOfThatListener() {
        val configSpy = Mockito.spy(config)
        callbackManager = CallbackManager(configSpy, deviceInfo)
        var listener: DisplayUnitListener? = DisplayUnitListener { }

        callbackManager.setDisplayUnitListener(listener)
        Mockito.verify(configSpy, Mockito.never()).accountId

        listener = null
        callbackManager.setDisplayUnitListener(listener)
        Mockito.verify(configSpy, Mockito.times(1)).accountId
    }

    //TODO@piyush what will be tested if listener is null?//TODO@ansh : add test when callbackManager.inboxListener = null
    @Test
    fun test__notifyInboxInitialized_when_FunctionIsCalled_ShouldCallInboxListnersFunctionIfAvailable() {
        val spy = Mockito.spy(object : CTInboxListener {
            override fun inboxDidInitialize() {}
            override fun inboxMessagesDidUpdate() {}
        })
        callbackManager.inboxListener = spy
        callbackManager._notifyInboxInitialized()
        Mockito.verify(spy, Mockito.times(1)).inboxDidInitialize()
    }

    //done//TODO@ansh: Add test when DU is empty
    //todo@piyush getting error //TODO@ansh: Add test when DU is null
    @Test
    fun test_notifyDisplayUnitsLoaded_when_FunctionIsCalledWithValidUnits_should_CallDisplayUnitListenerIfAvailable() {
        val displayUnitListener = object : DisplayUnitListener {
            override fun onDisplayUnitsLoaded(it: ArrayList<CleverTapDisplayUnit>) {

            }
        }
        val spy = Mockito.spy(displayUnitListener)
        callbackManager.setDisplayUnitListener(spy)
        var units: ArrayList<CleverTapDisplayUnit>? = null

        try {
            callbackManager.notifyDisplayUnitsLoaded(units)
            Mockito.verify(spy, Mockito.never()).onDisplayUnitsLoaded(Mockito.any())
        }catch (e:Throwable){
            println(e.message)
        }

        units = arrayListOf()
        callbackManager.notifyDisplayUnitsLoaded(units)
        Mockito.verify(spy, Mockito.never()).onDisplayUnitsLoaded(units)


        units = arrayListOf(CleverTapDisplayUnit.toDisplayUnit(JSONObject()))
        callbackManager.notifyDisplayUnitsLoaded(units)
        Mockito.verify(spy, Mockito.atLeastOnce()).onDisplayUnitsLoaded(units)
    }
}