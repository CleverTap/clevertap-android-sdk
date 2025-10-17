package com.clevertap.android.pushtemplates.content

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.isNotNullAndEmpty
import com.clevertap.android.pushtemplates.media.GifResult
import com.clevertap.android.pushtemplates.media.TemplateMediaManager

internal open class ContentView(
    internal var context: Context,
    layoutId: Int,
    internal val templateMediaManager: TemplateMediaManager
) {

    internal var remoteView: RemoteViews = RemoteViews(context.packageName, layoutId)

    fun setCustomContentViewBasicKeys(subtitle : String?, metaColor: String?) {
        remoteView.setTextViewText(R.id.app_name, Utils.getApplicationName(context))
        remoteView.setTextViewText(R.id.timestamp, Utils.getTimeStamp(context, System.currentTimeMillis()))
        if (subtitle != null && subtitle.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.subtitle,
                    Html.fromHtml(subtitle, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.subtitle, Html.fromHtml(subtitle))
            }
        } else {
            remoteView.setViewVisibility(R.id.subtitle, View.GONE)
            remoteView.setViewVisibility(R.id.sep_subtitle, View.GONE)
        }

        listOf(R.id.app_name, R.id.timestamp, R.id.subtitle).forEach { resId ->
            setCustomTextColour(metaColor, resId)
        }

        setDotSep(metaColor)
    }

    private fun setDotSep(metaColor: String?) {
        try {
            Utils.setBitMapColour(context, R.drawable.pt_dot_sep, metaColor, PTConstants.PT_META_CLR_DEFAULTS)
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

    fun setCustomContentViewSmallIcon(smallIconBitmap: Bitmap?, smallIconResourceID: Int) {
        if (smallIconBitmap != null) {
            remoteView.setImageViewBitmap(R.id.small_icon, smallIconBitmap)
        } else {
            remoteView.setImageViewResource(R.id.small_icon, smallIconResourceID)
        }
    }

    fun setCustomContentViewLargeIcon(pt_large_icon: String?) {
        if (pt_large_icon.isNotNullAndEmpty()) {
            loadImageURLIntoRemoteView(R.id.large_icon, pt_large_icon, remoteView)
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
        gifUrl: String?,
        bigImageUrl: String?,
        scaleType: PTScaleType,
        altText: String,
        gifFrames: Int
    ): Boolean {
        val isGifLoaded = setCustomContentViewGIF(
            gifUrl,
            altText,
            scaleType,
            gifFrames,
            layoutId
        )

        return if (isGifLoaded) {
            true
        } else {
            setCustomContentViewBigImage(
                imageUrl = bigImageUrl,
                scaleType = scaleType,
                altText = altText
            )
        }
    }

    fun setCustomContentViewBigImage(
        imageUrl: String?,
        scaleType: PTScaleType,
        altText: String
    ): Boolean {

        if (imageUrl.isNullOrBlank()) return false

        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        val loaded = !loadImageURLIntoRemoteView(imageViewId, imageUrl, remoteView, altText)

        if (loaded) {
            remoteView.setViewVisibility(imageViewId, View.VISIBLE)
            remoteView.setViewVisibility(R.id.big_image_configurable, View.VISIBLE)
        } else {
            remoteView.setViewVisibility(R.id.big_media_configurable, View.GONE)
        }
        return loaded
    }

    fun setCustomContentViewGIF(gifUrl: String?, altText: String, scaleType: PTScaleType, numberOfFrames: Int, layoutId: Int): Boolean {
        val gifResult = templateMediaManager.getGifFrames(gifUrl, numberOfFrames)

        if (gifResult is GifResult.Error) {
            PTLog.debug("${gifResult.reason}. Falling back to static image")
            return false
        }

        val (frames, duration) = gifResult as GifResult.Success

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

    fun loadImageURLIntoRemoteView(
        imageViewID: Int, imageUrl: String?,
        remoteViews: RemoteViews
    ): Boolean {
        return loadImageURLIntoRemoteView(imageViewID, imageUrl, remoteViews, null)
    }

    /**
     * Loads an image URL into a RemoteView.
     * 
     * @param imageViewID The ID of the ImageView in the RemoteView
     * @param imageUrl The URL of the image to load (nullable)
     * @param remoteViews The RemoteViews to load the image into
     * @param altText Alternative text for accessibility (nullable)
     * @return true if fallback is needed (image loading failed), false if image was loaded successfully
     * 
     * INVARIANT: When this method returns false, the imageUrl parameter is guaranteed to be non-null,
     * non-blank, and start with "https". This invariant is enforced by getImageBitmap validation.
     */
    fun loadImageURLIntoRemoteView(
        imageViewID: Int, imageUrl: String?,
        remoteViews: RemoteViews, altText: String?
    ): Boolean {
        val image = templateMediaManager.getImageBitmap(imageUrl)

        if (image != null) {
            remoteViews.setImageViewBitmap(imageViewID, image)
            if (!TextUtils.isEmpty(altText)) {
                remoteViews.setContentDescription(imageViewID, altText)
            }
            return false
        } else {
            PTLog.debug("Image was not perfect. URL:$imageUrl hiding image view")
            return true
        }
    }

   fun setCustomContentViewMessageSummary(pt_msg_summary: String?) {
        if (pt_msg_summary.isNotNullAndEmpty()) {
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                remoteView.setTextViewText(
                    R.id.msg, Html.fromHtml(pt_msg_summary, Html.FROM_HTML_MODE_LEGACY)
                )
            } else {
                remoteView.setTextViewText(R.id.msg, Html.fromHtml(pt_msg_summary))
            }
        }
    }
}