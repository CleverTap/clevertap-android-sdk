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
import android.util.TypedValue
import androidx.annotation.RequiresApi
import com.clevertap.android.pushtemplates.ImageBorderData
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.PTScaleType
import com.clevertap.android.pushtemplates.R
import com.clevertap.android.pushtemplates.Utils
import com.clevertap.android.pushtemplates.bakedInto
import com.clevertap.android.pushtemplates.useNativeImageStyling
import com.clevertap.android.pushtemplates.usesNativeImageStyling
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

        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        val loaded = !loadImageURLIntoRemoteView(
            imageViewId, imageUrl, remoteView, altText, imageBorderData.bakedInto(scaleType)
        )

        if (loaded) {
            remoteView.setViewVisibility(imageViewId, View.VISIBLE)
            applyNativeImageStyling(remoteView, imageViewId, imageBorderData, scaleType)
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

        val imageViewId = when (scaleType) {
            PTScaleType.FIT_CENTER -> R.id.big_image_fitCenter
            PTScaleType.CENTER_CROP -> R.id.big_image
        }

        // A FIT_CENTER GIF is styled by baking it into every frame, as its static image is.
        // Frames are pre-extracted stills, so baking costs nothing during playback and adds no
        // bitmaps to the RemoteViews parcel.
        val border = imageBorderData.bakedInto(scaleType)?.takeIf { it.isActive }

        for (frame in frames) {
            // GIF frames are decoded fresh on every call, so recycling the pre-styling frame is
            // safe here. Static images come from TemplateMediaManager's cache and must not be.
            // applyRoundedBorderToBitmap returns its own argument when there is nothing to draw,
            // so only recycle a frame that was genuinely replaced.
            val processedFrame = if (border != null) {
                NotificationBitmapUtils.applyRoundedBorderToBitmap(frame, border)
                    .also { if (it !== frame) frame.recycle() }
            } else frame
            val frameRemoteViews = RemoteViews(context.getPackageName(), layoutId)
            frameRemoteViews.setImageViewBitmap(imageViewId, processedFrame)
            frameRemoteViews.setViewVisibility(imageViewId, View.VISIBLE)
            applyNativeImageStyling(frameRemoteViews, imageViewId, imageBorderData, scaleType)
            remoteView.addView(R.id.view_flipper, frameRemoteViews)
        }

        if (!TextUtils.isEmpty(altText)) {
            remoteView.setContentDescription(R.id.view_flipper, altText)
        }

        remoteView.setInt(R.id.view_flipper, "setFlipInterval", flipInterval)
        remoteView.setViewVisibility(R.id.view_flipper, View.VISIBLE)

        return true
    }

    /**
     * The height this template's image area is laid out at, which styling percentages are resolved
     * against. See `R.dimen.pt_image_style_reference` for why a value known up front is needed at
     * all, and why it cannot be the same for every template.
     *
     * Templates that give their image more or less room than the standard area override this.
     */
    protected open val imageStyleReferenceDimen: Int
        get() = R.dimen.pt_image_style_reference

    /**
     * On API 31+ draws [border] with the views instead of baking it into the bitmap: the image
     * view clips its own corners, and when a border is wanted `big_image_frame` is painted in the
     * border colour, clipped to the outer radius, and the image is inset by the border width so
     * the frame shows through as a ring.
     *
     * Because nothing is baked, the tray can crop the bitmap to fill and the styling stays on the
     * visible edge. Used only for CENTER_CROP, where the image fills the view; a FIT_CENTER image
     * is styled by baking instead, see [usesNativeImageStyling]. No-op otherwise and when there is
     * nothing to draw.
     *
     * A CENTER_CROP image fills the area, so its height is the area's height, and both sizes are
     * resolved against [R.dimen.pt_image_style_reference] accordingly.
     */
    fun applyNativeImageStyling(
        remoteViews: RemoteViews,
        imageViewId: Int,
        border: ImageBorderData?,
        scaleType: PTScaleType,
        frameId: Int = R.id.big_image_frame
    ) {
        if (border == null || !border.isActive || !scaleType.usesNativeImageStyling()) return
        applyNativeImageStylingS(remoteViews, imageViewId, border, frameId)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyNativeImageStylingS(
        remoteViews: RemoteViews,
        imageViewId: Int,
        border: ImageBorderData,
        frameId: Int
    ) {
        val referencePx = context.resources.getDimension(imageStyleReferenceDimen).toInt()
        val radiusPx = NotificationBitmapUtils.resolveCornerRadiusPx(
            referencePx, border.cornerRadiusPercent
        )
        val borderPx =
            if (border.hasBorder) NotificationBitmapUtils.resolveBorderWidthPx(
                referencePx, border.borderWidthValue
            ) else 0f
        val borderColor = border.borderColor

        PTLog.debug(
            "Native image styling: corner radius ${border.cornerRadiusPercent}% -> ${radiusPx}px, " +
                    "border width ${border.borderWidthValue} -> ${borderPx}px"
        )

        if (borderPx > 0f && borderColor != null) {
            remoteViews.setInt(frameId, "setBackgroundColor", borderColor)
            remoteViews.setViewOutlinePreferredRadius(frameId, radiusPx, TypedValue.COMPLEX_UNIT_PX)
            for (side in intArrayOf(
                RemoteViews.MARGIN_LEFT, RemoteViews.MARGIN_TOP,
                RemoteViews.MARGIN_RIGHT, RemoteViews.MARGIN_BOTTOM
            )) {
                remoteViews.setViewLayoutMargin(imageViewId, side, borderPx, TypedValue.COMPLEX_UNIT_PX)
            }
        }
        // The image sits inside the ring, so its own corner follows the inner edge.
        val innerRadius = (radiusPx - borderPx).coerceAtLeast(0f)
        remoteViews.setViewOutlinePreferredRadius(imageViewId, innerRadius, TypedValue.COMPLEX_UNIT_PX)
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
        // Both the raw and the styled bitmap are owned by TemplateMediaManager's caches, so
        // neither is ever recycled here.
        val image = templateMediaManager.getStyledImageBitmap(imageUrl, imageBorderData)
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