package com.clevertap.android.sdk.inapp

import android.app.Activity
import android.app.AlertDialog
import com.clevertap.android.sdk.R

object AlertDialogPromptForSettings {

    interface Callback {
        fun onAccept()
        fun onDecline()
    }

    fun show(
        activity: Activity,
        callback: Callback,
    ) {
        val title = activity.getString(R.string.permission_not_available_title)

        val message = activity.getString(R.string.permission_not_available_message)

        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.permission_not_available_open_settings_option) { dialog, which ->
                callback.onAccept()
            }
            .setNegativeButton(android.R.string.no) { dialog, which ->
                callback.onDecline()
            }
            .show()
    }
}