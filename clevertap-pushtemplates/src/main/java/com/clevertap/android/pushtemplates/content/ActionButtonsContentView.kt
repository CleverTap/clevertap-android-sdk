package com.clevertap.android.pushtemplates.content
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.pushnotification.ActionButton

open class ActionButtonsContentView(context: Context, layoutId: Int, renderer: TemplateRenderer) :
    ContentView(context, layoutId, renderer) {
    init {
        setActionButtons(renderer.actionButtons)
    }

    private fun setActionButtons(actionButtons: List<ActionButton>) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            // Action Buttons for API 31 and above are set using the OS API and not remote views
            return
        }

        var visibleButtonCount = 0
        actionButtons.forEach { button ->
            val buttonId = when (visibleButtonCount) {
                0 -> R.id.action0
                1 -> R.id.action1
                else -> return@forEach // Skip if we already have 2 buttons
            }
            if (button.label.isEmpty()) {
                Logger.d("not adding push notification action: action label or id missing")
                return@forEach
            }
            remoteView.setTextViewText(buttonId, button.label)
            remoteView.setViewVisibility(buttonId, View.VISIBLE)

            // Set up the pending intent for this button
            remoteView.setOnClickPendingIntent(buttonId, button.pendingIntent)

            visibleButtonCount++
        }
    }
}