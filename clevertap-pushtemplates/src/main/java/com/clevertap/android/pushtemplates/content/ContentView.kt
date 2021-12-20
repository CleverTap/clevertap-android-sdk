package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.Utils

open class ContentView(
    internal var context: Context, layoutId: Int,
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
        if (renderer.pt_meta_clr != null && renderer.pt_meta_clr!!.isNotEmpty()) {
            remoteView.setTextColor(
                R.id.app_name,
                Utils.getColour(renderer.pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
            )
            remoteView.setTextColor(
                R.id.timestamp,
                Utils.getColour(renderer.pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
            )
            remoteView.setTextColor(
                R.id.subtitle,
                Utils.getColour(renderer.pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
            )
            setDotSep()
        }
    }

    private fun setDotSep() {
        try {
            renderer.pt_dot = context.resources.getIdentifier(
                PTConstants.PT_DOT_SEP,
                "drawable",
                context.packageName
            )
            renderer.pt_dot_sep = Utils.setBitMapColour(context, renderer.pt_dot, renderer.pt_meta_clr)
        } catch (e: NullPointerException) {
            PTLog.debug("NPE while setting dot sep color")
        }
    }

    fun setCustomContentViewTitle(pt_title: String?) {
        if (pt_title != null && pt_title.isNotEmpty()) {
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
        if (pt_msg != null && pt_msg.isNotEmpty()) {
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

    fun setCustomContentViewCollapsedBackgroundColour(pt_bg: String?) {
        if (pt_bg != null && pt_bg.isNotEmpty()) {
            remoteView.setInt(
                R.id.content_view_small,
                "setBackgroundColor",
                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
            )
        }
    }

    fun setCustomContentViewTitleColour(pt_title_clr: String?) {
        if (pt_title_clr != null && pt_title_clr.isNotEmpty()) {
            remoteView.setTextColor(
                R.id.title,
                Utils.getColour(pt_title_clr, PTConstants.PT_COLOUR_BLACK)
            )
        }
    }

    fun setCustomContentViewMessageColour(pt_msg_clr: String?) {
        if (pt_msg_clr != null && pt_msg_clr.isNotEmpty()) {
            remoteView.setTextColor(
                R.id.msg,
                Utils.getColour(pt_msg_clr, PTConstants.PT_COLOUR_BLACK)
            )
        }
    }

    fun setCustomContentViewSmallIcon() {
        if (renderer.pt_small_icon != null) {
            Utils.loadImageBitmapIntoRemoteView(R.id.small_icon, renderer.pt_small_icon, remoteView)
        } else {
            Utils.loadImageRidIntoRemoteView(R.id.small_icon, renderer.smallIcon, remoteView)
        }
    }

    fun setCustomContentViewDotSep() {
        if (renderer.pt_dot_sep != null) {
            Utils.loadImageBitmapIntoRemoteView(R.id.sep, renderer.pt_dot_sep, remoteView)
            Utils.loadImageBitmapIntoRemoteView(R.id.sep_subtitle, renderer.pt_dot_sep, remoteView)
        }
    }

    fun setCustomContentViewLargeIcon(pt_large_icon: String?) {
        if (pt_large_icon != null && pt_large_icon.isNotEmpty()) {
            Utils.loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, remoteView)
        } else {
            remoteView.setViewVisibility(R.id.large_icon, View.GONE)
        }
    }

    fun setCustomContentViewExpandedBackgroundColour(pt_bg: String?) {
        if (pt_bg != null && pt_bg.isNotEmpty()) {
            remoteView.setInt(
                R.id.content_view_big,
                "setBackgroundColor",
                Utils.getColour(pt_bg, PTConstants.PT_COLOUR_WHITE)
            )
        }
    }
}