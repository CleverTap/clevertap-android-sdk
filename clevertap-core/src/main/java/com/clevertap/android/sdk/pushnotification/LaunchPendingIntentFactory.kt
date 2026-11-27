package com.clevertap.android.sdk.pushnotification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils
import java.util.Random

/**
 * `@JvmStatic` is kept on purpose: this is a cross-module (`LIBRARY_GROUP`) type, and separately
 * published push templates artifacts built against the former Java class call it statically.
 * An open class with a public constructor (not an `object`), matching the former Java
 * `public class` shape.
 */
@Suppress("UtilityClassWithPublicConstructor") // the former Java class shape (public, non-final, public ctor)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class LaunchPendingIntentFactory {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {

        private const val FLAGS_LAUNCH_PENDING_INTENT = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        /**
         * Returns null on Android 12+ when the app has no launch intent and the payload has no deep link.
         */
        @JvmStatic
        public fun getLaunchPendingIntent(extras: Bundle, context: Context): PendingIntent? {
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                return getActivityIntent(extras, context)
            }
            val launchIntent = Intent(context, CTPushNotificationReceiver::class.java).apply {
                // Take all the properties from the notif and add it to the intent
                putExtras(extras)
                removeExtra(Constants.WZRK_ACTIONS)
            }
            return PendingIntent.getBroadcast(context, Random().nextInt(), launchIntent, FLAGS_LAUNCH_PENDING_INTENT)
        }

        /**
         * Returns null when the app has no launch intent and the payload has no deep link.
         */
        @JvmStatic
        public fun getActivityIntent(extras: Bundle, context: Context): PendingIntent? {
            val deepLink = extras.getString(Constants.DEEP_LINK_KEY)
            val launchIntent = if (extras.containsKey(Constants.DEEP_LINK_KEY) && deepLink != null) {
                Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).also {
                    Utils.setPackageNameFromResolveInfoList(context, it)
                }
            } else {
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
            }

            launchIntent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                // Take all the properties from the notif and add it to the intent
                putExtras(extras)
                removeExtra(Constants.WZRK_ACTIONS)
            }

            return PendingIntent.getActivity(context, Random().nextInt(), launchIntent, FLAGS_LAUNCH_PENDING_INTENT, null)
        }
    }
}
