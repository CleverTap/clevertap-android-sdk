package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.google.firebase.messaging.RemoteMessage

/**
 * Class responsible for manipulating the FCM notification bundle.
 *
 * @param messageBundle The bundle containing the FCM notification data.
 */
class FcmNotificationBundleManipulation(private val messageBundle: Bundle) :
    INotificationBundleManipulation<RemoteMessage> {

    /**
     * Adds the priority to the FCM notification bundle if the original priority and the current priority of the message are different .
     *
     * @param message The remote message containing the original priority and updated priority.
     * @return The instance of `INotificationBundleManipulation<RemoteMessage>` for method chaining.
     */
    override fun addPriority(message: RemoteMessage): INotificationBundleManipulation<RemoteMessage> {
        if (message.originalPriority != message.priority) {
            // Map the priority value to a string representation based on its value
            val strPriority = when (message.priority) {
                RemoteMessage.PRIORITY_HIGH -> Constants.PRIORITY_HIGH
                RemoteMessage.PRIORITY_NORMAL -> Constants.PRIORITY_NORMAL
                RemoteMessage.PRIORITY_UNKNOWN -> Constants.PRIORITY_UNKNOWN
                else -> ""
            }
            // Add the priority value to the message bundle using the appropriate constant key
            messageBundle.putString(Constants.WZRK_PN_PRT, strPriority)
        }
        return this
    }

    /**
     * Builds and returns the manipulated FCM notification bundle.
     *
     * @return The manipulated FCM notification bundle. [Bundle] returned is identical to the [Bundle] passed to this
     * class constructor.
     */
    override fun build(): Bundle = messageBundle
}