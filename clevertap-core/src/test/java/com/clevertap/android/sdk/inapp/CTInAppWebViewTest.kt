package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.CTWebInterface
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The in-app WebView must never read the phone's files or content providers. These
 * settings are locked on construction, so they hold for JavaScript-off campaigns too,
 * and they stay locked when JavaScript is switched on for a campaign that needs it.
 */
@RunWith(RobolectricTestRunner::class)
class CTInAppWebViewTest : BaseTestCase() {

    private fun newWebView() = CTInAppWebView(appCtx, 0, 0, 100, 100)

    @Test
    fun `local access is off on construction, before any javascript decision`() {
        val settings = newWebView().settings

        assertFalse(settings.javaScriptEnabled)
        assertFalse(settings.allowContentAccess)
        assertFalse(settings.allowFileAccess)
        @Suppress("DEPRECATION")
        assertFalse(settings.allowFileAccessFromFileURLs)
        assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
    }

    @Test
    fun `enabling javascript keeps local access off`() {
        val webView = newWebView()

        webView.setJavaScriptInterface(mockk<CTWebInterface>(relaxed = true))

        val settings = webView.settings
        assertTrue(settings.javaScriptEnabled)
        assertFalse(settings.allowContentAccess)
        assertFalse(settings.allowFileAccess)
        @Suppress("DEPRECATION")
        assertFalse(settings.allowFileAccessFromFileURLs)
        assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
    }
}
