package com.clevertap.android.sdk

import android.content.Context
import android.content.Intent
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppFixtures
import com.clevertap.android.sdk.utils.configMock
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class InAppNotificationActivityTest {

    @Test
    fun `launchForInAppNotification sends only the in-app id and not the in-app object`() {
        val context = mockk<Context>(relaxed = true)
        every { context.packageName } returns "com.clevertap.test"
        val intent = slot<Intent>()
        every { context.startActivity(capture(intent)) } just runs
        val inApp = CTInAppNotification(JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL), true)

        InAppNotificationActivity.launchForInAppNotification(context, inApp, configMock())

        val extras = intent.captured.extras!!
        assertEquals("1742963524_20250326", extras.getString(Constants.INAPP_ID_KEY))
        // The whole in-app must never go through Binder again (SDK-6299).
        assertFalse(extras.containsKey(Constants.INAPP_KEY))
    }
}
