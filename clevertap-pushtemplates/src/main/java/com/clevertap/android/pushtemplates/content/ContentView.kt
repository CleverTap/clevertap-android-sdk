package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal open class ContentView(
    internal var context: Context,
    layoutId: Int,
    internal var renderer: TemplateRenderer
) {

    internal var remoteView: RemoteViews = RemoteViews(context.packageName, layoutId)

    fun setCustomContentViewBasicKeys() {
        remoteView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
        remoteView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context))
        if (renderer.pt_subtitle != null && renderer.pt_subtitle!!.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.subtitle,
                    Html.fromHtml(renderer.pt_subtitle, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.subtitle, Html.fromHtml(renderer.pt_subtitle))
            }
        } else {
            remoteView.setViewVisibility(R.id.subtitle, View.GONE)
            remoteView.setViewVisibility(R.id.sep_subtitle, View.GONE)
        }

        listOf(R.id.app_name, R.id.timestamp, R.id.subtitle).forEach { resId ->
            setCustomTextColour(renderer.pt_meta_clr, resId)
        }

        setDotSep()
    }

    private fun setDotSep() {
        try {
            renderer.pt_dot = context.resources.getIdentifier(
                PTConstants.PT_DOT_SEP,
                "drawable",
                context.packageName
            )
            renderer.pt_dot_sep = Utils.setBitMapColour(context, renderer.pt_dot, renderer.pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
        } catch (e: NullPointerException) {
            PTLog.debug("NPE while setting dot sep color")
        }
    }

    fun setCustomContentViewTitle(pt_title: String?) {
        if (pt_title.isNotNullAndEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.title,
                    Html.fromHtml(pt_title, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.title, Html.fromHtml(pt_title))
            }
        }
    }

    fun setCustomContentViewMessage(pt_msg: String?) {
        if (pt_msg.isNotNullAndEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.msg,
                    Html.fromHtml(pt_msg, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg))
            }
        }
    }

    fun setCustomContentViewSmallIcon() {
        if (renderer.pt_small_icon != null) {
            Utils.loadImageBitmapIntoRemoteView(R.id.small_icon, renderer.pt_small_icon, remoteView)
        } else {
            Utils.loadImageRidIntoRemoteView(R.id.small_icon, renderer.smallIcon, remoteView)
        }
    }

    fun setCustomContentViewLargeIcon(pt_large_icon: String?) {
        if (pt_large_icon.isNotNullAndEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, remoteView,context)
        } else {
            remoteView.setViewVisibility(R.id.large_icon, View.GONE)
        }
    }

    fun setCustomBackgroundColour(pt_bg: String?, resId: Int) {
        pt_bg?.takeIf { it.isNotEmpty() }?.let {
            Utils.getColourOrNull(it)?.let { color ->
                remoteView.setInt(
                    resId,
                    "setBackgroundColor",
                    color
                )
            }
        }
    }

    fun setCustomTextColour(pt_text_clr: String?, resId: Int) {
        pt_text_clr?.takeIf { it.isNotEmpty() }?.let {
            Utils.getColourOrNull(it)?.let { color ->
                remoteView.setTextColor(
                    resId,
                    color
                )
            }
        }
    }

    fun setCustomContentViewBigImage(pt_big_img: String?, scaleType: PTScaleType) {
        if (pt_big_img.isNotNullAndEmpty()) {
            val imageViewId = when (scaleType) {
                PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
                PTScaleType.CENTER_CROP -> R.id.big_image
            }
            Utils.loadImageURLIntoRemoteView(imageViewId, pt_big_img, remoteView, context)
            if (!Utils.getFallback()) {
                remoteView.setViewVisibility(imageViewId, View.VISIBLE)
            }
        }
    }
}