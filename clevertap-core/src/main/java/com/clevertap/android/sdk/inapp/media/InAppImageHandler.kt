package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

/**
 * Handler for static image media in InApp notifications.
 * Stateless — only [setup] is meaningful; all lifecycle methods are no-ops.
 */
internal class InAppImageHandler(
    private val inAppNotification: CTInAppNotification,
    private val media: CTInAppNotificationMedia,
    private val currentOrientation: Int,
    private val resourceProvider: FileResourceProvider
) : InAppMediaHandler {

    override fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener?
    ) {
        if (config.useOrientationForImage) {
            val mediaForOrientation =
                inAppNotification.getInAppMediaForOrientation(currentOrientation) ?: return
            val imageView = relativeLayout?.findViewById<ImageView>(config.imageViewId)
            imageView?.setContentDescriptionIfNotBlank(mediaForOrientation.contentDescription)
            val bitmap = resourceProvider.cachedInAppImageV1(mediaForOrientation.mediaUrl)
            if (bitmap != null) {
                imageView?.setImageBitmap(bitmap)
                if (config.clickableMedia && clickListener != null) {
                    imageView?.tag = 0
                    imageView?.setOnClickListener(clickListener)
                }
            }
        } else {
            val image = resourceProvider.cachedInAppImageV1(media.mediaUrl) ?: return
            val imageView = relativeLayout?.findViewById<ImageView>(config.imageViewId)
            imageView?.setContentDescriptionIfNotBlank(media.contentDescription)
            imageView?.visibility = View.VISIBLE
            imageView?.setImageBitmap(image)
        }
    }
}
