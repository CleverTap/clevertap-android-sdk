package com.clevertap.android.sdk.inapp

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants

/**
 * Adapts [CTInAppHtmlBannerOverlay]'s UI callbacks and its [CTInAppWebView] interactions to the
 * SDK's [InAppListener], mirroring how [PIPInAppCallbacksBridge] adapts the PIP renderer's
 * callbacks. This keeps the overlay renderer free of analytics/action semantics.
 *
 * The action-triggered result (the dismiss data) is remembered in [pendingDismissData] and
 * reported via [InAppListener.inAppNotificationDidDismiss] only once the overlay is actually
 * removed, matching the fragment flow where dismissal follows the action.
 */
internal class CTHtmlBannerCallbacksBridge(
    private val notification: CTInAppNotification,
    private val config: CleverTapInstanceConfig,
    private val inAppListener: InAppListener
) : CTInAppHtmlBannerOverlay.Callbacks, InAppDisplayListener {

    /** Set by the caller immediately after constructing the overlay. */
    var overlay: CTInAppHtmlBannerOverlay? = null

    private var pendingDismissData: Bundle? = null

    // ----- InAppDisplayListener (external hide, e.g. discardInApps/suspend) -----

    override fun hideInApp() {
        didDismiss(null)
    }

    // ----- CTInAppHtmlBannerOverlay.Callbacks (banner lifecycle) -----

    override fun onBannerShown() {
        inAppListener.inAppNotificationDidShow(notification, null)
    }

    override fun onBannerSwipeDismissed() {
        triggerAction(CTInAppAction.createCloseAction(), Constants.INAPP_CTA_SWIPE_DISMISS, null)
    }

    override fun onBannerRemoved() {
        inAppListener.inAppNotificationDidDismiss(notification, pendingDismissData)
        pendingDismissData = null
    }

    // ----- InAppWebInteraction (WebView url loads + JS bridge) -----

    override fun openActionUrl(url: String) {
        triggerAction(CTInAppAction.createOpenUrlAction(url), null, null)
    }

    override fun triggerAction(
        action: CTInAppAction, callToAction: String?, additionalData: Bundle?
    ) {
        val parsed = InAppActionParser.parse(action, callToAction, additionalData, config)
        pendingDismissData = inAppListener.inAppNotificationActionTriggered(
            notification,
            parsed.action,
            parsed.callToAction ?: "",
            parsed.additionalData,
            overlay?.activity
        )
        overlay?.dismiss()
    }

    override fun didDismiss(data: Bundle?) {
        pendingDismissData = data
        overlay?.dismiss()
    }
}
