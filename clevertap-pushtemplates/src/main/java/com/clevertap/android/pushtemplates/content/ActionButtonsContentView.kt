package com.clevertap.android.pushtemplates.content
import android.app.PendingIntent
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import com.clevertap.android.pushtemplates.ActionButton
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.sdk.Logger

internal open class ActionButtonsContentView(context: Context, layoutId: Int, renderer: TemplateRenderer) :
    ContentView(context, layoutId, renderer) {
    init {
        setActionButtons(renderer.actionButtons, renderer.actionButtonPendingIntents)
    }

    private fun setActionButtons(actionButtons: List<ActionButton>, pendingIntentsMap: Map<String, PendingIntent>) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            // Action Buttons for API 31 and above are set using the OS API and not remote views
            return
        }

        var visibleButtonCount = 0
        actionButtons.take(2).forEach { button ->
            val buttonId = if (visibleButtonCount == 0) {
                R.id.action0
            } else {
                R.id.action1
            }
            if (button.label.isEmpty()) {
                Logger.d("not adding push notification action: action label or id missing")
                return@forEach
            }
            remoteView.setTextViewText(buttonId, button.label)
            remoteView.setViewVisibility(buttonId, View.VISIBLE)

            // Set up the pending intent for this button
            val pendingIntent = pendingIntentsMap[button.id]
            if (pendingIntent != null) {
                remoteView.setOnClickPendingIntent(buttonId, pendingIntent)
            }

            visibleButtonCount++
        }
    }
}