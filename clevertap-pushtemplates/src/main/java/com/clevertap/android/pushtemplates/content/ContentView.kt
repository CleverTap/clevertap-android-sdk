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
import com.clevertap.android.pushtemplates.ImageBorderData
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

    fun setCustomContentViewLargeIcon(ptLargeIcon: String?) {
        val shouldHide = ptLargeIcon
            ?.takeIf { it.isNotBlank() }
            ?.let { loadImageURLIntoRemoteView(R.id.large_icon, it, remoteView) }
            ?: true

        remoteView.setViewVisibility(
            R.id.large_icon,
            if (shouldHide) View.GONE else View.VISIBLE
        )
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
        gifFrames: Int,
        imageBorderData: ImageBorderData? = null
    ): Boolean {
        val isGifLoaded = setCustomContentViewGIF(
            gifUrl, altText, scaleType, gifFrames, layoutId, imageBorderData
        )
        return if (isGifLoaded) true
        else setCustomContentViewBigImage(bigImageUrl, scaleType, altText, imageBorderData)
    }

    fun setCustomContentViewBigImage(
        imageUrl: String?,
        scaleType: PTScaleType,
        altText: String,
        imageBorderData: ImageBorderData? = null
    ): Boolean {
        if (imageUrl.isNullOrBlank()) return false

        // When border/radius is active, force fit_center so rounded corners are fully visible
        val effectiveScaleType = if (imageBorderData?.isActive == true) PTScaleType.FIT_CENTER else scaleType
        val imageViewId = when (effectiveScaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        val loaded = !loadImageURLIntoRemoteView(imageViewId, imageUrl, remoteView, altText, imageBorderData)

        if (loaded) {
            remoteView.setViewVisibility(imageViewId, View.VISIBLE)
            remoteView.setViewVisibility(R.id.big_image_configurable, View.VISIBLE)
        } else {
            remoteView.setViewVisibility(R.id.big_media_configurable, View.GONE)
        }
        return loaded
    }

    fun setCustomContentViewGIF(
        gifUrl: String?,
        altText: String,
        scaleType: PTScaleType,
        numberOfFrames: Int,
        layoutId: Int,
        imageBorderData: ImageBorderData? = null
    ): Boolean {
        val gifResult = templateMediaManager.getGifFrames(gifUrl, numberOfFrames)

        if (gifResult is GifResult.Error) {
            PTLog.debug("${gifResult.reason}. Falling back to static image")
            return false
        }

        val (frames, duration) = gifResult as GifResult.Success

        val extractedFramesSize = frames.size
        val flipInterval = duration / extractedFramesSize
        PTLog.debug("Total duration: " + duration + "ms")
        PTLog.debug("Flip interval: " + flipInterval + "ms")

        // When border/radius is active, force fit_center so rounded corners are fully visible
        val effectiveScaleType = if (imageBorderData?.isActive == true) PTScaleType.FIT_CENTER else scaleType
        val imageViewId = when (effectiveScaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        val applyBorder = imageBorderData?.isActive == true
        val borderColor = if (applyBorder) imageBorderData?.borderColor?.let { Utils.getColourOrNull(it) } else null

        for (frame in frames) {
            val processedFrame = if (applyBorder) {
                NotificationBitmapUtils.applyRoundedBorderToBitmap(
                    frame, imageBorderData!!.cornerRadius, borderColor, imageBorderData.borderWidth
                ).also { frame.recycle() }
            } else frame
            val frameRemoteViews = RemoteViews(context.getPackageName(), layoutId)
            frameRemoteViews.setImageViewBitmap(imageViewId, processedFrame)
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
    ): Boolean = loadImageURLIntoRemoteView(imageViewID, imageUrl, remoteViews, null, null)

    /**
     * Loads an image URL into a RemoteView.
     *
     * INVARIANT: When this method returns false, the imageUrl parameter is guaranteed to be non-null,
     * non-blank, and start with "https". This invariant is enforced by getImageBitmap validation.
     */
    fun loadImageURLIntoRemoteView(
        imageViewID: Int, imageUrl: String?,
        remoteViews: RemoteViews, altText: String?
    ): Boolean = loadImageURLIntoRemoteView(imageViewID, imageUrl, remoteViews, altText, null)

    fun loadImageURLIntoRemoteView(
        imageViewID: Int,
        imageUrl: String?,
        remoteViews: RemoteViews,
        altText: String?,
        imageBorderData: ImageBorderData?
    ): Boolean {
        val rawImage = templateMediaManager.getImageBitmap(imageUrl)
        if (rawImage != null) {
            val image = if (imageBorderData?.isActive == true) {
                val borderColor = imageBorderData.borderColor?.let { Utils.getColourOrNull(it) }
                NotificationBitmapUtils.applyRoundedBorderToBitmap(
                    rawImage, imageBorderData.cornerRadius, borderColor, imageBorderData.borderWidth
                )
            } else rawImage
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