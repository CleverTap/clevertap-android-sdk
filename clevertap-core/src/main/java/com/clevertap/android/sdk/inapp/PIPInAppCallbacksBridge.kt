package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks

internal class PIPInAppCallbacksBridge(
    private val inAppNotification: CTInAppNotification,
    private val inAppListener: InAppListener,
    private val logger: Logger
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

    override fun onRedirect(url: String) {
        logger.debug(LOG_TAG, "PIP onRedirect for campaign: ${inAppNotification.campaignId}, url: $url")
        val action = CTInAppAction.createOpenUrlAction(url)
        inAppListener.inAppNotificationActionTriggered(
            inAppNotification, action, url, null, null
        )
    }

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
}
