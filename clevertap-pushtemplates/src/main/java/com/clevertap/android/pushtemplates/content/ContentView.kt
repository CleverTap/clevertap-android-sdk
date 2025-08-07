package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateMediaManager
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateRepository
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
        remoteView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context, System.currentTimeMillis()))
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
            renderer.pt_dot = R.drawable.pt_dot_sep
            Utils.setBitMapColour(context, renderer.pt_dot, renderer.pt_meta_clr, PTConstants.PT_META_CLR_DEFAULTS)
        } catch (_: NullPointerException) {
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
            Utils.loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, remoteView, context)
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

    fun setCustomContentViewMedia(
        layoutId: Int,
        gifUrl: String? = renderer.pt_gif,
        bigImageUrl: String? = renderer.pt_big_img,
        scaleType: PTScaleType = renderer.pt_scale_type,
        altText: String = renderer.pt_big_img_collapsed_alt_text,
        gifFrames: Int = renderer.pt_gif_frames
    ) {
        val gifSuccess = setCustomContentViewGIF(
            gifUrl,
            altText,
            scaleType,
            gifFrames,
            layoutId
        )
        if (!gifSuccess) {
            PTLog.debug("Couldn't load GIF. Falling back to static image")
            setCustomContentViewBigImage(
                bigImageUrl,
                scaleType,
                altText
            )
        }
    }

    fun setCustomContentViewBigImage(pt_big_img: String?, scaleType: PTScaleType, altText: String) {
        if (pt_big_img.isNotNullAndEmpty()) {
            val imageViewId = when (scaleType) {
                PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
                PTScaleType.CENTER_CROP -> R.id.big_image
            }
            Utils.loadImageURLIntoRemoteView(imageViewId, pt_big_img, remoteView, context, altText)
            if (!Utils.getFallback()) {
                remoteView.setViewVisibility(imageViewId, View.VISIBLE)
                remoteView.setViewVisibility(R.id.big_image_configurable, View.VISIBLE)
            }
        }
    }

    fun setCustomContentViewGIF(gifUrl: String?, altText: String, scaleType: PTScaleType, numberOfFrames: Int, layoutId: Int): Boolean {
        val gifResult = TemplateMediaManager(TemplateRepository(context, renderer.config)).getGifFrames(gifUrl, numberOfFrames)
        val frames = gifResult.frames
        val duration = gifResult.duration

        if (frames.isNullOrEmpty()) {
            PTLog.debug("No frames extracted from GIF")
            return false
        }

        // Calculate timing for frame flipping
        val extractedFramesSize = frames.size
        val flipInterval = duration / extractedFramesSize
        PTLog.debug("Total duration: " + duration + "ms")
        PTLog.debug("Flip interval: " + flipInterval + "ms")

        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        // Add each frame to the ViewFlipper
        for (frame in frames) {
            val frameRemoteViews = RemoteViews(context.getPackageName(), layoutId)
            frameRemoteViews.setImageViewBitmap(imageViewId, frame)
            frameRemoteViews.setViewVisibility(imageViewId, View.VISIBLE)
            remoteView.addView(R.id.view_flipper, frameRemoteViews)
        }

        if (!TextUtils.isEmpty(altText)) {
            remoteView.setContentDescription(R.id.view_flipper, altText)
        }

        remoteView.setInt(R.id.view_flipper, "setFlipInterval", flipInterval)
        remoteView.setViewVisibility(R.id.view_flipper, View.VISIBLE)

        return true
    }
}