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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LaunchPendingIntentFactory {

    private const val flagsLaunchPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    /**
     * Returns null on Android 12+ when the app has no launch intent and the payload has no deep link.
     */
    @JvmStatic
    fun getLaunchPendingIntent(extras: Bundle, context: Context): PendingIntent? {
        return if (VERSION.SDK_INT >= VERSION_CODES.S) {
            getActivityIntent(extras, context)
        } else {
            val launchIntent = Intent(context, CTPushNotificationReceiver::class.java)
            // Take all the properties from the notif and add it to the intent
            launchIntent.putExtras(extras)
            launchIntent.removeExtra(Constants.WZRK_ACTIONS)

            PendingIntent.getBroadcast(
                context, Random().nextInt(),
                launchIntent, flagsLaunchPendingIntent
            )
        }
    }

    /**
     * Returns null when the app has no launch intent and the payload has no deep link.
     */
    @JvmStatic
    fun getActivityIntent(extras: Bundle, context: Context): PendingIntent? {
        val launchIntent: Intent
        val deepLink = extras.getString(Constants.DEEP_LINK_KEY)
        if (extras.containsKey(Constants.DEEP_LINK_KEY) && deepLink != null) {
            launchIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
            Utils.setPackageNameFromResolveInfoList(context, launchIntent)
        } else {
            launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: return null
        }

        launchIntent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        // Take all the properties from the notif and add it to the intent
        launchIntent.putExtras(extras)
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)

        return PendingIntent.getActivity(
            context, Random().nextInt(), launchIntent,
            flagsLaunchPendingIntent, null
        )
    }
}
