package com.clevertap.android.pushtemplates.content

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle

class PendingIntentFactory {

    private fun setPendingIntent(context: Context,notificationId: Int, extras: Bundle, intent: Intent,
                         dl: String): PendingIntent{


    }

    private fun setDismissIntent(context: Context, extras: Bundle,intent: Intent): PendingIntent{

    }

    fun getPendingIntent(context: Context, notificationId: Int, extras: Bundle,
                         isLauncher: Boolean,identifier: String): PendingIntent{

    }

}