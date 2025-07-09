package com.clevertap.android.sdk.inapp

import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.configMock
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTInAppHostTest {

    private lateinit var mockInAppListener: InAppListener

    @Before
    fun setUp() {
        mockInAppListener = mockk(relaxed = true)
    }

    @After
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `triggerAction should parse url parameters as additionalData`() {
        val host = createInAppHost()

        val param1 = "value"
        val param2 = "value 2"
        val param3 = "5"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter("param1", param1)
            .appendQueryParameter("param2", param2)
            .appendQueryParameter("param3", param3)
            .build().toString()

        host.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match { action ->
                    action.type == InAppActionType.OPEN_URL
                            && action.actionUrl == url
                },
                callToAction = any(),
                additionalData = match { data ->
                    param1 == data.getString("param1")
                            && param2 == data.getString("param2")
                            && param3 == data.getString("param3")
                },
                activityContext = any()
            )
        }
    }

    @Test
    fun `triggerAction should merge url parameters with provided additionalData `() {
        val host = createInAppHost()

        val urlParam1 = "value"
        val urlParam2 = "value 2"
        val urlParam3 = "5"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter("param1", urlParam1)
            .appendQueryParameter("param2", urlParam2)
            .appendQueryParameter("param3", urlParam3)
            .build().toString()

        val dataParam1 = "dataValue"
        val dataParam2 = "data value 2"
        val data = Bundle().apply {
            putString("param1", dataParam1)
            putString("param2", dataParam2)
        }

        host.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, data)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = any(),
                callToAction = any(),
                additionalData = match { data ->
                    dataParam1 == data.getString("param1")
                            && dataParam2 == data.getString("param2")
                            && urlParam3 == data.getString("param3")
                },
                activityContext = any()
            )
        }
    }

    @Test
    fun `triggerAction should use callToAction argument or c2a url param`() {
        val host = createInAppHost()

        val callToActionParam = "c2aParam"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, callToActionParam)
            .build().toString()

        host.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = any(),
                callToAction = callToActionParam,
                additionalData = any(),
                activityContext = any()
            )
        }

        val callToActionArgument = "argument"
        host.triggerAction(
            CTInAppAction.CREATOR.createOpenUrlAction(url),
            callToActionArgument,
            null
        )
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = any(),
                callToAction = callToActionArgument,
                additionalData = any(),
                activityContext = any()
            )
        }
    }

    @Test
    fun `triggerAction should parse c2a url param with __dl__ data`() {
        val host = createInAppHost()

        val dl = "https://deeplink.com?param1=asd&param2=value2"
        val callToActionParam = "c2aParam"
        val param1 = "value"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, "${callToActionParam}__dl__$dl")
            .appendQueryParameter("param1", param1)
            .build().toString()

        host.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match { action ->
                    // the open-url action should be performed with the url after __dl__
                    dl == action.actionUrl
                },
                callToAction = callToActionParam,
                additionalData = match { data ->
                    // only the params of the original url should be tracked
                    data.size() == 1
                            && param1 == data.getString("param1")
                },
                activityContext = any()
            )
        }

        val callToActionArgument = "argument"
        host.triggerAction(
            CTInAppAction.CREATOR.createOpenUrlAction(url),
            callToActionArgument,
            null
        )
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match { action ->
                    // the open-url action should be performed with the url after __dl__
                    dl == action.actionUrl
                },
                callToAction = callToActionArgument,
                additionalData = match { data ->
                    // only the params of the original url should be tracked
                    data.size() == 1
                            && param1 == data.getString("param1")
                },
                activityContext = any()
            )
        }
    }

    private fun createInAppHost(callbacks: CTInAppHost.Callbacks? = null): CTInAppHost {
        return CTInAppHost(
            inAppListener = mockInAppListener,
            config = configMock(),
            inAppNotification = CTInAppNotification(
                JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL),
                false
            ),
            callbacks = callbacks,
            context = null
        )
    }
}