package com.clevertap.android.sdk.inapp

import android.app.Activity
import android.app.AlertDialog
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.LOLLIPOP
import com.clevertap.android.sdk.CTStringResources
import com.clevertap.android.sdk.R

/**
 * This class shows an Alert dialog to display a rationale message if notification permission is
 * already denied.
 */
class AlertDialogPromptForSettings private constructor() {

    companion object {

        @JvmStatic
        fun show(
            activity: Activity, onAccept: () -> Unit, onDecline: () -> Unit
        ) {
            val (title, message, positiveButtonText, negativeButtonText) = CTStringResources(
                activity.applicationContext,
                R.string.ct_permission_not_available_title,
                R.string.ct_permission_not_available_message,
                R.string.ct_permission_not_available_open_settings_option,
                R.string.ct_txt_cancel
            )

            val builder = if (SDK_INT >= LOLLIPOP) AlertDialog.Builder(
                activity,
                android.R.style.Theme_Material_Light_Dialog_Alert
            ) else AlertDialog.Builder(activity)

            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { dialog, which ->
                    onAccept()
                }
                .setNegativeButton(negativeButtonText) { dialog, which ->
                    onDecline()
                }
                .show()
        }
    }
}