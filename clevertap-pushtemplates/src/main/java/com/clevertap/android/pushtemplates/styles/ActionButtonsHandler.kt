package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateRenderer

/**
 * Handles action buttons for notification styles.
 * This class implements the functionality that was previously in StyleWithActionButtons,
 * but using composition instead of inheritance.
 */
internal class ActionButtonsHandler(private val renderer: TemplateRenderer) {

    /**
     * Adds action buttons to the notification builder
     *
     * @param context The context
     * @param extras The notification extras
     * @param notificationId The notification ID
     * @param builder The notification builder
     * @param isInputBoxStyle Whether this is an InputBoxStyle (needs special handling)
     * @return The updated notification builder
     */
    fun addActionButtons(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        builder: NotificationCompat.Builder,
        isInputBoxStyle: Boolean = false
    ): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || isInputBoxStyle) {
            // Make sure the notification in collapsed state doesn't take up action buttons
            // InputBox Template use Android CTAs for all API levels
            renderer.setActionButtons(context, extras, notificationId, builder, renderer.actions)
        }
        return builder
    }
}
