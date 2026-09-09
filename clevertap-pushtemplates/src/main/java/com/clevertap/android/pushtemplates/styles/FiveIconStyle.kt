package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.*
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants

internal class FiveIconStyle(private val data: FiveIconsTemplateData, renderer: TemplateRenderer, private var extras: Bundle) : Style(data.baseContent, renderer) {

    lateinit var fiveIconSmallContentView: FiveIconContentView
    lateinit var fiveIconBigContentView: FiveIconContentView

    /**
     * The pt_* text only, not baseContent's title, which falls back to nt. This is what the system
     * shows for the notification once it stacks it into a group summary, where the layouts are not
     * rendered: pt_msg stands in when a campaign sets no pt_title, so a message only campaign is
     * not a blank row there, and an icon only campaign stays blank rather than surfacing nt.
     * baseContent keeps the fallback for the basic template fallback path.
     */
    override val builderTitle: String?
        get() = data.iconTextData.title ?: data.iconTextData.message

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconSmallContentView = FiveIconSmallContentView(context, renderer, data, extras)
        return fiveIconSmallContentView.remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconBigContentView = FiveIconBigContentView(context, renderer, data, extras)
        return fiveIconBigContentView.remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            FIVE_ICON_CONTENT_PENDING_INTENT, extras.getString(Constants.DEEP_LINK_KEY)
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }
}
