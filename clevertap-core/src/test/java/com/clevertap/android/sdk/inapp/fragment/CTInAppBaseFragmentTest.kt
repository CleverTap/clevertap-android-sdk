package com.clevertap.android.sdk.inapp.fragment

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.inapp.CTInAppAction
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.InAppActionType
import com.clevertap.android.sdk.inapp.InAppFixtures
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.utils.configMock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTInAppBaseFragmentTest {

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
    fun `showOnActivity should trigger fragment transaction and set inApp and config fields`() {
        val fragment = createFragmentSpy()
        val mockActivity = mockk<FragmentActivity>()
        val mockFragmentManager = mockk<FragmentManager>()
        val mockFragmentTransaction = mockk<FragmentTransaction>(relaxed = true)
        every { mockFragmentManager.beginTransaction() } returns mockFragmentTransaction
        every { mockActivity.supportFragmentManager } returns mockFragmentManager

        val inApp = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA),
            true
        )
        val mockConfig = configMock()
        assertTrue(
            CTInAppBaseFragment.showOnActivity(
                fragment,
                mockActivity,
                inApp,
                mockConfig,
                "Test"
            )
        )

        verify(exactly = 1) { mockFragmentTransaction.commitNow() }

        val contextMock = mockk<Context>()
        fragment.onAttach(contextMock)
    }

    @Test
    fun `showOnActivity should return false when activity is not FragmentActivity`() {
        val fragment = createFragmentSpy()
        val mockActivity = mockk<Activity>()

        assertFalse(
            CTInAppBaseFragment.showOnActivity(
                fragment,
                mockActivity,
                mockk(relaxed = true),
                mockk(relaxed = true),
                "Test",
            )
        )
    }

    @Test
    fun `showOnActivity should return false when the fragment cannot be displayed`() {
        val fragment = createFragmentSpy()
        val mockActivity = mockk<FragmentActivity>()
        val mockFragmentManager = mockk<FragmentManager>()
        val mockFragmentTransaction = mockk<FragmentTransaction>(relaxed = true)

        every { mockFragmentTransaction.commitNow() } throws IllegalStateException()
        every { mockFragmentManager.beginTransaction() } returns mockFragmentTransaction
        every { mockActivity.supportFragmentManager } returns mockFragmentManager

        val inApp = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA),
            true
        )
        val mockConfig = configMock()
        assertFalse(
            CTInAppBaseFragment.showOnActivity(
                fragment,
                mockActivity,
                inApp,
                mockConfig,
                "Test"
            )
        )
    }

    @Test
    fun `handleButtonClickAtIndex should trigger permission listener for localInApps and dismiss the inApp`() {
        val localInAppJson = CTLocalInApp.Companion.builder()
            .setInAppType(CTLocalInApp.InAppType.ALERT)
            .setTitleText("Title")
            .setMessageText("Message")
            .followDeviceOrientation(false)
            .setPositiveBtnText("Positive Button")
            .setNegativeBtnText("Negative Button")
            .build()
        val inApp = CTInAppNotification(localInAppJson, true)
        val mockConfig = configMock()
        val contextMock =
            mockk<Context>(moreInterfaces = arrayOf(DidClickForHardPermissionListener::class))

        val fragment = createAndAttachFragmentSpy(inApp, mockConfig, contextMock)

        // should trigger permission listener
        fragment.handleButtonClickAtIndex(0)

        verify(exactly = 1) {
            (contextMock as DidClickForHardPermissionListener).didClickForHardPermissionWithFallbackSettings(
                inApp.fallBackToNotificationSettings
            )
        }

        verify(exactly = 1) { fragment.didDismiss(any()) }

        // should not trigger permission listener
        fragment.handleButtonClickAtIndex(1)
        confirmVerified(contextMock)
    }

    @Test
    fun `handleButtonClickAtIndex should trigger permission listener for rfp actions and dismiss the inApp`() {
        val inApp = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL_WITH_BUTTON_ACTION_RFP),
            true
        )
        val mockConfig = configMock()
        val contextMock =
            mockk<Context>(moreInterfaces = arrayOf(DidClickForHardPermissionListener::class))

        val fragment = createAndAttachFragmentSpy(inApp, mockConfig, contextMock)
        fragment.handleButtonClickAtIndex(0)

        verify(exactly = 1) {
            (contextMock as DidClickForHardPermissionListener).didClickForHardPermissionWithFallbackSettings(
                true
            )
        }

        verify(exactly = 1) { fragment.didDismiss(any()) }
    }

    @Test
    fun `triggerAction should parse url parameters as additionalData`() {
        val fragment = createAndAttachFragmentSpy()

        val param1 = "value"
        val param2 = "value 2"
        val param3 = "5"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter("param1", param1)
            .appendQueryParameter("param2", param2)
            .appendQueryParameter("param3", param3)
            .build().toString()

        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
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
        val fragment = createAndAttachFragmentSpy()

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

        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, data)
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
        val fragment = createAndAttachFragmentSpy()

        val callToActionParam = "c2aParam"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, callToActionParam)
            .build().toString()

        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
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
        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), callToActionArgument, null)
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
        val fragment = createAndAttachFragmentSpy()

        val dl = "https://deeplink.com?param1=asd&param2=value2"
        val callToActionParam = "c2aParam"
        val param1 = "value"
        val url = Uri.parse("https://clevertap.com")
            .buildUpon()
            .appendQueryParameter(Constants.KEY_C2A, "${callToActionParam}__dl__$dl")
            .appendQueryParameter("param1", param1)
            .build().toString()

        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), null, null)
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
        fragment.triggerAction(CTInAppAction.CREATOR.createOpenUrlAction(url), callToActionArgument, null)
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

    private fun createFragmentSpy(): CTInAppBaseFragment {
        val fragmentSpy = spyk<CTInAppBaseFragment>()
        every { fragmentSpy.getListener() } returns mockInAppListener
        val mockResources = mockk<Resources>(relaxed = true)
        every { mockResources.configuration } returns mockk(relaxed = true)
        every { fragmentSpy.resources } returns mockResources
        return fragmentSpy
    }

    private fun createAndAttachFragmentSpy(
        inAppNotification: CTInAppNotification = mockk(),
        config: CleverTapInstanceConfig = mockk(),
        context: Context = mockk()
    ): CTInAppBaseFragment {
        val fragmentSpy = createFragmentSpy()
        fragmentSpy.setArguments(inAppNotification, config)
        fragmentSpy.onAttach(context)

        return fragmentSpy
    }
}