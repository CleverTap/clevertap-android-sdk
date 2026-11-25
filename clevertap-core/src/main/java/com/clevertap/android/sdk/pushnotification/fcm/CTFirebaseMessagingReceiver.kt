package com.clevertap.android.sdk.pushnotification.fcm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.flushPushImpressionsOnPostAsyncSafely
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil.buildPushNotificationRenderedListenerKey
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAccountIdFromNotificationBundle
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getPushIdFromNotificationBundle
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.TimeUnit

/**
 * Receiver to receive firebase messaging broadcast directly from OS.
 * This guarantees OS delivered broadcast reaches to Receiver directly instead of FirebaseMessagingService.
 */
internal open class CTFirebaseMessagingReceiver : BroadcastReceiver(), NotificationRenderedListener {

    private var countDownTimer: CountDownTimer? = null

    private var key = ""

    private var isPRFinished = false

    private var pendingResult: PendingResult? = null

    private var start: Long = 0

    /**
     * Callback when notification is rendered by core sdk.
     *
     * @param isRendered true if rendered successfully
     */
    @SuppressLint("RestrictedApi")
    override fun onNotificationRendered(isRendered: Boolean) {
        Logger.v(
            TAG,
            "push impression sent successfully by core, i should inform OS to kill receiver. my callback key is "
                    + key
        )
        finishReceiverAndCancelTimer("push impression sent successfully by core")
    }

    /**
     * This will finish [PendingResult] to signal OS that we are done with our
     * work and OS can kill App process.
     *
     * @param from name of the caller
     */
    private fun finishReceiverAndCancelTimer(from: String) {
        try {
            Logger.v(TAG, "got a signal to kill receiver and timer because $from")

            if (key.trim().isNotEmpty()) {
                CleverTapAPI.removeNotificationRenderedListener(key)
            }

            val end = System.nanoTime()
            if (pendingResult != null && !isPRFinished) {

                Logger.v(TAG, "informing OS to kill receiver...")

                pendingResult!!.finish()
                isPRFinished = true

                // rendered before timer can finish, so cancel now
                if (countDownTimer != null) {
                    countDownTimer!!.cancel()
                }

                Logger.v(TAG, "informed OS to kill receiver...")
                Logger.v(
                    TAG,
                    "receiver was alive for " + TimeUnit.NANOSECONDS.toSeconds(end - start)
                            + " seconds"
                )
            } else {
                Logger.v(
                    TAG,
                    "have already informed OS to kill receiver, can not inform again else OS will get angry :-O"
                )
            }
        } catch (e: Exception) {
            Logger.d(TAG, "Error finishing receiver and canceling timer", e)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onReceive(context: Context?, intent: Intent?) {
        start = System.nanoTime()

        Logger.d(TAG, "received a message from Firebase")
        if (context == null || intent == null) {
            return
        }

        val remoteMessage = RemoteMessage(intent.extras)
        val messageBundle = FcmNotificationParser().toBundle(remoteMessage) ?: return

        if (remoteMessage.priority != RemoteMessage.PRIORITY_NORMAL) {
            Logger.d(TAG, "returning from CTRM because message priority is not normal")
            return
        }

        /*
          Configurable time, required to render push and send impressions
         */
        val receiverLifeSpan = messageBundle.getString("ctrmt", "4500").toLong()

        pendingResult = goAsync()

        val notificationInfo = CleverTapAPI.getNotificationInfo(messageBundle)

        if (notificationInfo.fromCleverTap) {

            val isRenderFallback = Utils.isRenderFallback(remoteMessage, context)
            if (isRenderFallback) {
                key = buildPushNotificationRenderedListenerKey(
                    getAccountIdFromNotificationBundle(messageBundle),
                    getPushIdFromNotificationBundle(messageBundle)
                )
                CleverTapAPI.addNotificationRenderedListener(key, this)

                countDownTimer = object : CountDownTimer(receiverLifeSpan, 1000) {
                    override fun onFinish() {
                        finishReceiverAndCancelTimer("receiver life time is expired")
                    }

                    override fun onTick(millisUntilFinished: Long) {
                        // NO-OP
                    }
                }

                countDownTimer!!.start()

                Thread {
                    try {
                        val cleverTapAPI = CleverTapAPI.getGlobalInstance(
                            context,
                            getAccountIdFromNotificationBundle(
                                messageBundle
                            )
                        )

                        if (cleverTapAPI != null) {
                            cleverTapAPI.flushPushImpressionsOnPostAsyncSafely(
                                "$TAG#flushQueueSync",
                                Constants.D_SRC_PI_R,
                                context
                            )
                        }
                        //We are done flushing events
                    } catch (e: Exception) {
                        Logger.d(TAG, "Failed executing CTRM flushQueueSync thread.", e)
                    } finally {
                        finishReceiverAndCancelTimer("flush from receiver is done!")
                    }

                }.start()

            } else {
                Logger.v(TAG, "Notification payload does not have a fallback key.")
                finishReceiverAndCancelTimer("isRenderFallback is false")
            }
        } else {
            Logger.v(TAG, "Notification payload is not from CleverTap.")
            finishReceiverAndCancelTimer("push is not from CleverTap.")
        }

    }

    private companion object {

        private const val TAG = "CTRM"
    }
}
