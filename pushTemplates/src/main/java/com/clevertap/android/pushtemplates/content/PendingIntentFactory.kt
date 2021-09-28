package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.sdk.Constants

class PendingIntentFactory {

    internal fun setPendingIntent(context: Context,notificationId: Int, extras: Bundle, launchIntent: Intent,
                         dl: String?): PendingIntent{
        launchIntent.putExtras(extras)
        launchIntent.putExtra(PTConstants.PT_NOTIF_ID, notificationId)
        if (dl != null) {
            launchIntent.putExtra(PTConstants.DEFAULT_DL, true)
            launchIntent.putExtra(Constants.DEEP_LINK_KEY, dl)
        }
        launchIntent.removeExtra(Constants.WZRK_ACTIONS)
        launchIntent.putExtra(Constants.WZRK_FROM_KEY, Constants.WZRK_FROM)
        launchIntent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            launchIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

    }

    private fun setDismissIntent(context: Context, extras: Bundle,intent: Intent): PendingIntent{
        intent.putExtras(extras)
        intent.putExtra(PTConstants.PT_DISMISS_INTENT, true)
        return PendingIntent.getBroadcast(
            context, System.currentTimeMillis().toInt(),
            intent, PendingIntent.FLAG_CANCEL_CURRENT
        )
    }

    fun getPendingIntent(context: Context, notificationId: Int, extras: Bundle,
                         isLauncher: Boolean,identifier: String): PendingIntent{

    }

}