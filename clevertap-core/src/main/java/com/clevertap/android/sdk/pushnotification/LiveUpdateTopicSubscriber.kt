package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.ILogger
import com.google.firebase.messaging.FirebaseMessaging

/** Subscribes/unsubscribes this install to an FCM topic. Seam for tests. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface LiveUpdateTopicSubscriber {
    fun subscribe(topic: String)
    fun unsubscribe(topic: String)
}

/**
 * FCM-backed [LiveUpdateTopicSubscriber]. Firebase Messaging is a `compileOnly` dependency of core,
 * so every call is guarded: an app without FCM simply never receives broadcast updates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class FcmLiveUpdateTopicSubscriber(
    private val accountId: String,
    private val logger: ILogger
) : LiveUpdateTopicSubscriber {

    override fun subscribe(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    logger.debug(accountId, "Live Update broadcast: subscribe($topic) success=${task.isSuccessful}")
                }
        } catch (t: Throwable) {
            logger.debug(accountId, "Live Update broadcast: subscribe($topic) failed", t)
        }
    }

    override fun unsubscribe(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    logger.debug(accountId, "Live Update broadcast: unsubscribe($topic) success=${task.isSuccessful}")
                }
        } catch (t: Throwable) {
            logger.debug(accountId, "Live Update broadcast: unsubscribe($topic) failed", t)
        }
    }
}
