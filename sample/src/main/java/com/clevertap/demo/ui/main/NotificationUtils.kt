package com.clevertap.demo.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.clevertap.android.pushtemplates.PTConstants

object NotificationUtils {

    //Require to close notification on action button click
    fun dismissNotification(intent: Intent?, applicationContext: Context){
        intent?.extras?.apply {
            // Only action-button clicks carry an "actionId"; scope the cancel to them. A plain
            // content-intent tap must NOT auto-cancel — every Push-Template content intent carries an
            // int notificationId, so reading it unconditionally would dismiss the (ongoing) pt_progress
            // Live Update on the first tap.
            var autoCancel = false
            var notificationId = -1
            getString("actionId")?.let {
                autoCancel = getBoolean("autoCancel", true)
                notificationId = getInt("notificationId", -1)
            }

            /**
             * If using InputBox template, add ptDismissOnClick flag to not dismiss notification
             * if pt_dismiss_on_click is false in InputBox template payload. Alternatively if normal
             * notification is raised then we dismiss notification.
             */
            val ptDismissOnClick = intent.extras!!.getString(PTConstants.PT_DISMISS_ON_CLICK,"")

            if (autoCancel && notificationId > -1 && ptDismissOnClick.isNullOrEmpty()) {
                val notifyMgr: NotificationManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifyMgr.cancel(notificationId)
            }
        }
    }
}