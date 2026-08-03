package com.clevertap.android.sdk.inapp

import android.os.Bundle

/**
 * Thin contract for a surface that hosts an in-app [CTInAppWebView] and reacts to its interactions
 * (url loads, JS-driven actions and dismissals).
 *
 * Implemented by:
 *  - [com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment] for fragment-hosted in-apps, and
 *  - [CTHtmlBannerCallbacksBridge] for the non-fragment [CTInAppHtmlBannerOverlay] path.
 *
 * It lets [com.clevertap.android.sdk.CTWebInterface] and [InAppWebViewClient] drive either host
 * without knowing which one is backing the WebView.
 */
internal interface InAppWebInteraction {

    /** Triggered when the WebView attempts to load an action url (see [InAppWebViewClient]). */
    fun openActionUrl(url: String)

    /** Triggered from the JS bridge ([com.clevertap.android.sdk.CTWebInterface.triggerInAppAction]). */
    fun triggerAction(action: CTInAppAction, callToAction: String?, additionalData: Bundle?)

    /** Triggered from the JS bridge ([com.clevertap.android.sdk.CTWebInterface.dismissInAppNotification]). */
    fun didDismiss(data: Bundle?)
}
