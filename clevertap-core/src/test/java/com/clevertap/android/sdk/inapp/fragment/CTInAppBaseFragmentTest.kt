package com.clevertap.android.sdk.inapp.fragment

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DidClickForHardPermissionListener
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTLocalInApp
import com.clevertap.android.sdk.inapp.InAppFixtures
import com.clevertap.android.sdk.utils.configMock
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTInAppBaseFragmentTest {

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

    private fun createFragmentSpy(): CTInAppBaseFragment {
        val fragmentSpy = spyk<CTInAppBaseFragment>()
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