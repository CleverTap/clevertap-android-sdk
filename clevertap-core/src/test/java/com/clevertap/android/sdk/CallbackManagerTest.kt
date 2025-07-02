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
import io.mockk.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
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
        val ibSpy = spyk<CTInboxListener>(ib)
        callbackManager.inboxListener = ibSpy
        callbackManager._notifyInboxMessagesDidUpdate()

        verify(atLeast = 1) {
            ibSpy.inboxMessagesDidUpdate()
        }
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
    fun test_OnInitListenersInvokedOnNotify_when_PreviouslySubscribed() {
        val ctId = "newId"
        val listener1 = mockk<OnInitCleverTapIDListener>(relaxed = true)
        val listener2 = mockk<OnInitCleverTapIDListener>(relaxed = true)
        callbackManager.addOnInitCleverTapIDListener(listener1)
        callbackManager.addOnInitCleverTapIDListener(listener2)
        callbackManager.notifyCleverTapIDChanged(ctId)

        ShadowLooper.runUiThreadTasks()
        verify { listener1.onInitCleverTapID(ctId) }
        verify { listener2.onInitCleverTapID(ctId) }

        val ctId2 = "newId2"
        callbackManager.removeOnInitCleverTapIDListener(listener1)
        callbackManager.notifyCleverTapIDChanged(ctId2)

        ShadowLooper.runUiThreadTasks()
        verify(inverse = true) { listener1.onInitCleverTapID(ctId2) }
        verify { listener2.onInitCleverTapID(ctId2) }

        callbackManager.removeOnInitCleverTapIDListener(listener2)
    }
    @Test
    fun test_OnInitListenersThreadSafety_ReproduceCME() {

        val testListener = mockk<OnInitCleverTapIDListener>(relaxed = true)
        var concurrentModificationOccurred = false
        var otherExceptionOccurred = false
        var caughtException: Exception? = null

        // Add initial listeners
        repeat(5) {
            callbackManager.addOnInitCleverTapIDListener(mockk(relaxed = true))
        }

        val barrier = CyclicBarrier(2)

        // Thread 1: Continuously modify the list (add/remove)
        val modificationThread = Thread {
            try {
                barrier.await() // Synchronize start
                repeat(10000) {
                    callbackManager.addOnInitCleverTapIDListener(testListener)
                    callbackManager.removeOnInitCleverTapIDListener(testListener)
                }
            } catch (e: Exception) {
                caughtException = e
                otherExceptionOccurred = true
            }
        }

        // Thread 2: Continuously notify (iterate through list)
        val notificationThread = Thread {
            try {
                barrier.await() // Synchronize start
                repeat(5000) { index ->
                    callbackManager.notifyCleverTapIDChanged("testId_$index")
                }
            } catch (e: ConcurrentModificationException) {
                caughtException = e
                concurrentModificationOccurred = true
            } catch (e: Exception) {
                caughtException = e
                otherExceptionOccurred = true
            }
        }

        // Start both threads
        modificationThread.start()
        notificationThread.start()

        // Wait for completion
        modificationThread.join(30000) // 30 second timeout
        notificationThread.join(30000)

        caughtException?.printStackTrace()

        // Assertions
        assertFalse(
            "ConcurrentModificationException should not occur with thread-safe implementation. " +
                    "Exception: $caughtException",
            concurrentModificationOccurred
        )

        assertFalse(
            "No other exceptions should occur. Exception: $caughtException",
            otherExceptionOccurred
        )

        println("Thread safety test completed successfully - no ConcurrentModificationException occurred")
    }
    @Test
    fun test_notifyUserProfileInitialized_when_FunctionIsCalledWithDeviceID_ShouldCallSyncListenerWithDeviceID() {

        val syncListenrSpy = spyk(object : SyncListener {
            override fun profileDataUpdated(updates: JSONObject?) {}
            override fun profileDidInitialize(cleverTapID: String?) {}
        })
        callbackManager.syncListener = syncListenrSpy

        // if sync listener is function is called with null string, then it will not return without any further actions
        callbackManager.notifyUserProfileInitialized(null)
        verify(exactly = 0) {
            syncListenrSpy.profileDidInitialize(null)
        }

        // if sync listener is set on callback manager and function is called with non-empty string, then it will call synclistener's profileDidInitialize
        callbackManager.notifyUserProfileInitialized("deviceID")
        verify(exactly = 1) {
            syncListenrSpy.profileDidInitialize("deviceID")
        }


        //3. this function also has a non parameter overload which will use device id from cached memory to return a device id which might be null but perform equally
        val deviceInfoSpy = spyk(deviceInfo)
        callbackManager = CallbackManager(config, deviceInfoSpy)
        callbackManager.syncListener = syncListenrSpy
        every { deviceInfoSpy.deviceID } returns "motorola"
        callbackManager.notifyUserProfileInitialized()
        verify(exactly = 1) { syncListenrSpy.profileDidInitialize("motorola") }

    }

    @Test
    fun test_setDisplayUnitListener_when_FunctionIsCalledWithAValidListener_ShouldAttachItselfWithAWeakReferenceOfThatListener() {
        val configSpy = spyk(config)
        callbackManager = CallbackManager(configSpy, deviceInfo)
        var listener: DisplayUnitListener? = DisplayUnitListener { }

        callbackManager.setDisplayUnitListener(listener)
        verify(exactly = 0) { configSpy.accountId }

        listener = null
        callbackManager.setDisplayUnitListener(listener)
        verify(exactly = 1) { configSpy.accountId }
    }

    @Test
    fun test__notifyInboxInitialized_when_FunctionIsCalled_ShouldCallInboxListnersFunctionIfAvailable() {
        val inboxListenerSpy = spyk(object : CTInboxListener {
            override fun inboxDidInitialize() {}
            override fun inboxMessagesDidUpdate() {}
        })
        callbackManager.inboxListener = inboxListenerSpy
        callbackManager._notifyInboxInitialized()
        verify(exactly = 1) { inboxListenerSpy.inboxDidInitialize() }
    }

    @Test
    fun test_notifyDisplayUnitsLoaded_when_FunctionIsCalledWithValidUnits_should_CallDisplayUnitListenerIfAvailable() {
        val displayUnitListener = object : DisplayUnitListener {
            override fun onDisplayUnitsLoaded(it: ArrayList<CleverTapDisplayUnit>) {

            }
        }
        val listenerSpy = spyk(displayUnitListener)
        callbackManager.setDisplayUnitListener(listenerSpy)

        callbackManager.notifyDisplayUnitsLoaded(null)
        verify(exactly = 0) { listenerSpy.onDisplayUnitsLoaded(any()) }

        var units: ArrayList<CleverTapDisplayUnit> = arrayListOf()
        callbackManager.notifyDisplayUnitsLoaded(units)
        verify(exactly = 0) { listenerSpy.onDisplayUnitsLoaded(units) }

        units = arrayListOf(CleverTapDisplayUnit.toDisplayUnit(JSONObject()))
        callbackManager.notifyDisplayUnitsLoaded(units)
        verify { listenerSpy.onDisplayUnitsLoaded(units) }
    }
}
