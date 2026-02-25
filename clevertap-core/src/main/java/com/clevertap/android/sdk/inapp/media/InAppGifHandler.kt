package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.gif.GifImageView
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal class InAppGifHandler(
    private val media: CTInAppNotificationMedia,
    private val resourceProvider: FileResourceProvider
) : InAppMediaHandler {

    private var gifImageView: GifImageView? = null

    override fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener?
    ) {
        val gifByteArray = resourceProvider.cachedInAppGifV1(media.mediaUrl) ?: return
        gifImageView = relativeLayout?.findViewById(R.id.gifImage)
        gifImageView?.setContentDescriptionIfNotBlank(media.contentDescription)
        gifImageView?.visibility = View.VISIBLE
        gifImageView?.setBytes(gifByteArray)
        gifImageView?.startAnimation()
        if (config.clickableMedia && clickListener != null) {
            gifImageView?.tag = CLICKABLE_MEDIA_TAG
            gifImageView?.setOnClickListener(clickListener)
        }
        relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility = View.GONE
    }

    override fun onStart() {
        gifImageView?.let { view ->
            view.setBytes(resourceProvider.cachedInAppGifV1(media.mediaUrl))
            view.startAnimation()
        }
    }

    override fun onPause() {
        clearGif()
    }

    override fun onStop() {
        clearGif()
    }

    override fun cleanup() {
        clearGif()
    }

    override fun clear() {
        clearGif()
    }

    private fun clearGif() {
        gifImageView?.clear()
    }
}
