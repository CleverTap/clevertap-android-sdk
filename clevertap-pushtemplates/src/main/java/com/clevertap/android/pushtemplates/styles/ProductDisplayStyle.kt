package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.PRODUCT_DISPLAY_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PRODUCT_DISPLAY_DISMISS_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.ProductDisplayLinearBigContentView
import com.clevertap.android.pushtemplates.content.ProductDisplayNonLinearBigContentView
import com.clevertap.android.pushtemplates.content.ProductDisplayNonLinearSmallContentView

class ProductDisplayStyle(private var renderer: TemplateRenderer, private var extras: Bundle) : Style(renderer) {

    override fun makeSmallContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return ProductDisplayNonLinearSmallContentView(context, renderer).remoteView
    }

    override fun makeBigContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return if (renderer.pt_product_display_linear == null || renderer.pt_product_display_linear!!.isEmpty()) {
            ProductDisplayNonLinearBigContentView(context, renderer, extras).remoteView
        } else {
            ProductDisplayLinearBigContentView(context, renderer, extras).remoteView
        }
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            PRODUCT_DISPLAY_CONTENT_PENDING_INTENT, renderer
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, false,
            PRODUCT_DISPLAY_DISMISS_PENDING_INTENT, renderer
        )
    }
}