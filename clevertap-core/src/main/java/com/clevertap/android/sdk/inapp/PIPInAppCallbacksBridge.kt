package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks

internal class PIPInAppCallbacksBridge(
    private val inAppNotification: CTInAppNotification,
    private val inAppListener: InAppListener,
    private val showFailureHandler: PIPShowFailureHandler,
    private val logger: ILogger,
) : PIPCallbacks {

    private companion object {
        const val LOG_TAG = "PIPInAppCallbacksBridge"
    }

    override fun onShow() {
        logger.debug(LOG_TAG, "PIP onShow for campaign: ${inAppNotification.campaignId}")
        inAppListener.inAppNotificationDidShow(inAppNotification, null)
    }

    override fun onClose() {
        logger.debug(LOG_TAG, "PIP onClose for campaign: ${inAppNotification.campaignId}")
        inAppListener.inAppNotificationDidDismiss(inAppNotification, null)
    }

    override fun onAction() {
        val onClick = inAppNotification.pipConfigJson?.optJSONObject("onClick")
        val action = CTInAppAction.createFromJson(onClick) ?: return
        val callToAction = onClick?.optString("c2a", "") ?: ""
        logger.debug(LOG_TAG, "PIP onAction for campaign: ${inAppNotification.campaignId}, type: ${action.type}")
        // Last parameter (activityContext) is null — here's why:
        //
        // Regular in-apps pass Activity context because they live inside
        // InAppNotificationActivity (a transparent Activity). When the user taps a URL:
        //   Task: [MainActivity → InAppNotificationActivity]
        //   → InAppNotificationActivity FINISHES → task is now just [MainActivity]
        //   → Browser opens in a separate task → [Chrome]
        //   → Android might KILL [MainActivity] task (low memory, it's in background)
        //   → Back from Chrome → launcher ❌ (app task was killed)
        // Passing Activity context keeps Chrome in the app's task, avoiding this.
        //
        // PIP doesn't have this problem because PIP is a View overlay, NOT an Activity.
        // When PIP dismisses, it just removes a View — the user's Activity stays running:
        //   Task: [MainActivity]  ← PIP is a View on top, not in the task stack
        //   → PIP dismisses (removes View, Activity stays alive)
        //   → Browser opens in a separate task → [Chrome]
        //   → [MainActivity] task is still alive (it was foreground just before Chrome)
        //   → Back from Chrome → MainActivity ✓
        //
        // No Activity finishes, so the app's task is never at risk of being killed
        // because it was never reduced to an empty/background state.
        inAppListener.inAppNotificationActionTriggered(
            inAppNotification, action, callToAction, null, null
        )
    }

    // The callbacks below are PIP-specific events with no corresponding InAppListener method.
    // They are intentionally log-only for diagnostics. If SDK-level forwarding is needed in the
    // future, add new methods to InAppListener and wire them here.

    override fun onExpand() {
        logger.debug(LOG_TAG, "PIP onExpand for campaign: ${inAppNotification.campaignId}")
    }

    override fun onCollapse() {
        logger.debug(LOG_TAG, "PIP onCollapse for campaign: ${inAppNotification.campaignId}")
    }

    override fun onPlaybackStarted() {
        logger.debug(LOG_TAG, "PIP onPlaybackStarted for campaign: ${inAppNotification.campaignId}")
    }

    override fun onPlaybackPaused() {
        logger.debug(LOG_TAG, "PIP onPlaybackPaused for campaign: ${inAppNotification.campaignId}")
    }

    override fun onMediaError(url: String, error: String) {
        logger.debug(LOG_TAG, "PIP onMediaError for campaign: ${inAppNotification.campaignId}, url: $url, error: $error")
    }

    override fun onShowFailed() {
        logger.debug(LOG_TAG, "PIP onShowFailed for campaign: ${inAppNotification.campaignId}")
        showFailureHandler.onPIPShowFailed(inAppNotification)
    }
}
