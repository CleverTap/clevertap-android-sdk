package com.clevertap.android.sdk

import android.os.Looper
import com.clevertap.android.sdk.displayunits.DisplayUnitListener
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener
import com.clevertap.android.sdk.product_config.CTProductConfigListener
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
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

    @Test
    fun test_SetterGetterForFailureFlushListener() {
        val failureListener = FailureFlushListener { }
        callbackManager.failureFlushListener = failureListener
        assertEquals(failureListener, callbackManager.failureFlushListener)
    }


    @Test
    fun test_SetterGetterForFeatureFlagListener() {
        val listener = CTFeatureFlagsListener { }
        callbackManager.featureFlagListener = listener
        assertEquals(listener, callbackManager.featureFlagListener)
    }


    @Test
    fun test_SetterGetterGeofenceCallback() {
        val listener = object : GeofenceCallback {
            override fun handleGeoFences(jsonObject: JSONObject?) {}
            override fun triggerLocation() {}
        }
        callbackManager.geofenceCallback = listener
        assertEquals(listener, callbackManager.geofenceCallback)
    }


    @Test
    fun test_SetterGetterInAppNotificationButtonListener() {
        val listener = InAppNotificationButtonListener { }
        callbackManager.inAppNotificationButtonListener = listener
        assertEquals(listener, callbackManager.inAppNotificationButtonListener)
    }



    @Test
    fun test_SetterGetterInAppNotificationListener() {
        val listener = object : InAppNotificationListener {
            override fun beforeShow(extras: MutableMap<String, Any>?): Boolean {
                return true
            }

            override fun onShow(ctInAppNotification: CTInAppNotification?) {
                TODO("Not yet implemented")
            }

            override fun onDismissed(extras: MutableMap<String, Any>?, actionExtras: MutableMap<String, Any>?) {}
        }
        callbackManager.inAppNotificationListener = listener
        assertEquals(listener, callbackManager.inAppNotificationListener)
    }



    @Test
    fun test_SetterGetterInboxListener() {
        val listener = object : CTInboxListener {
            override fun inboxDidInitialize() {}
            override fun inboxMessagesDidUpdate() {}
        }

        callbackManager.inboxListener = listener
        assertEquals(listener, callbackManager.inboxListener)


    }



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



    @Test
    fun test_SetterGetterPushAmpListener() {
        val listener = CTPushAmpListener { }
        callbackManager.pushAmpListener = listener
        assertEquals(listener, callbackManager.pushAmpListener)
    }

    @Test
    fun test_SetterGetterPushNotificationListener() {
        val listener = CTPushNotificationListener { }
        callbackManager.pushNotificationListener = listener
        assertEquals(listener, callbackManager.pushNotificationListener)
    }



    @Test
    fun test_getterSetterSyncListener() {
        val listener = object : SyncListener {
            override fun profileDataUpdated(updates: JSONObject?) {}

            override fun profileDidInitialize(CleverTapID: String?) {}
        }
        callbackManager.syncListener = listener
        assertEquals(listener, callbackManager.syncListener)
    }



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

    @Test
    fun test_notifyDisplayUnitsLoaded_when_FunctionIsCalledWithValidUnits_should_CallDisplayUnitListenerIfAvailable() {
        val displayUnitListener = object : DisplayUnitListener {
            override fun onDisplayUnitsLoaded(it: ArrayList<CleverTapDisplayUnit>) {

            }
        }
        val spy = Mockito.spy(displayUnitListener)
        callbackManager.setDisplayUnitListener(spy)
        var units: ArrayList<CleverTapDisplayUnit>? = null

        callbackManager.notifyDisplayUnitsLoaded(units)
        Mockito.verify(spy, Mockito.never()).onDisplayUnitsLoaded(Mockito.anyList<CleverTapDisplayUnit>()  as ArrayList<CleverTapDisplayUnit>)

        units = arrayListOf()
        callbackManager.notifyDisplayUnitsLoaded(units)
        Mockito.verify(spy, Mockito.never()).onDisplayUnitsLoaded(units)


        units = arrayListOf(CleverTapDisplayUnit.toDisplayUnit(JSONObject()))
        callbackManager.notifyDisplayUnitsLoaded(units)
        Mockito.verify(spy, Mockito.atLeastOnce()).onDisplayUnitsLoaded(units)
    }
}