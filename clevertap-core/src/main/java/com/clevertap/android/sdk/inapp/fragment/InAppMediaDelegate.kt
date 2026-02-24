package com.clevertap.android.sdk.inapp.fragment

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.gif.GifImageView
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inapps.ExoplayerHandle
import com.clevertap.android.sdk.video.inapps.Media3Handle

internal data class InAppMediaConfig(
    val imageViewId: Int,
    val clickableMedia: Boolean,
    val useOrientationForImage: Boolean = true,
    val hideImageViewForNonImageMedia: Boolean = true,
    val fillVideoFrame: Boolean = true
)

internal fun View.setContentDescriptionIfNotBlank(contentDescription: String) {
    if (contentDescription.isNotBlank()) {
        this.contentDescription = contentDescription
    }
}

@UnstableApi
internal class InAppMediaDelegate(
    private val fragment: Fragment,
    private val inAppNotification: CTInAppNotification,
    private val currentOrientation: Int,
    private val isTablet: Boolean,
    private val resourceProvider: FileResourceProvider
) {

    private var gifImageView: GifImageView? = null
    private var handle: InAppVideoPlayerHandle? = null
    private var videoFrameLayout: FrameLayout? = null
    private var fullScreenDialog: ComponentDialog? = null
    private var videoFrameInDialog: FrameLayout? = null
    private var exoPlayerFullscreen = false
    private var fillVideoFrame = true
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (exoPlayerFullscreen) {
                closeFullscreenDialog()
                isEnabled = false
            }
        }
    }

    fun onCreate() {
        handle = if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
            Media3Handle()
        } else {
            ExoplayerHandle()
        }
    }

    fun onStart() {
        gifImageView?.let { gifImageView ->
            val inAppMedia = inAppNotification.mediaList.firstOrNull() ?: return
            gifImageView.setBytes(resourceProvider.cachedInAppGifV1(inAppMedia.mediaUrl))
            gifImageView.startAnimation()
        }
    }

    fun onResume() {
        if (inAppNotification.hasStreamMedia()) {
            prepareMedia()
            playMedia()
        }
    }

    fun onPause() {
        gifImageView?.clear()
        if (exoPlayerFullscreen) {
            closeFullscreenDialog()
            onBackPressedCallback.isEnabled = false
        }
        handle?.savePosition()
        handle?.pause()
    }

    fun onStop() {
        gifImageView?.clear()
        handle?.pause()
    }

    fun cleanup() {
        gifImageView?.clear()
        handle?.pause()
    }

    fun clearGif() {
        gifImageView?.clear()
    }

    fun bindVideoFrame(videoFrame: FrameLayout?) {
        videoFrameLayout = videoFrame
    }

    fun setMediaForInApp(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener? = null
    ) {
        fillVideoFrame = config.fillVideoFrame
        if (inAppNotification.mediaList.isNotEmpty()) {
            val media = inAppNotification.mediaList[0]
            if (media.isImage()) {
                if (config.useOrientationForImage) {
                    val mediaForOrientation =
                        inAppNotification.getInAppMediaForOrientation(currentOrientation)
                    if (mediaForOrientation != null) {
                        val imageView =
                            relativeLayout?.findViewById<ImageView>(config.imageViewId)
                        imageView?.setContentDescriptionIfNotBlank(mediaForOrientation.contentDescription)
                        val bitmap =
                            resourceProvider.cachedInAppImageV1(mediaForOrientation.mediaUrl)
                        if (bitmap != null) {
                            imageView?.setImageBitmap(bitmap)
                            if (config.clickableMedia && clickListener != null) {
                                imageView?.tag = 0
                                imageView?.setOnClickListener(clickListener)
                            }
                        }
                    }
                } else {
                    val image = resourceProvider.cachedInAppImageV1(media.mediaUrl)
                    if (image != null) {
                        val imageView =
                            relativeLayout?.findViewById<ImageView>(config.imageViewId)
                        imageView?.setContentDescriptionIfNotBlank(media.contentDescription)
                        imageView?.setVisibility(View.VISIBLE)
                        imageView?.setImageBitmap(image)
                    }
                }
            } else if (media.isGIF()) {
                val gifByteArray = resourceProvider.cachedInAppGifV1(media.mediaUrl)
                if (gifByteArray != null) {
                    gifImageView = relativeLayout?.findViewById(R.id.gifImage)
                    gifImageView?.setContentDescriptionIfNotBlank(media.contentDescription)
                    gifImageView?.setVisibility(View.VISIBLE)
                    gifImageView?.setBytes(gifByteArray)
                    gifImageView?.startAnimation()
                    if (config.clickableMedia && clickListener != null) {
                        gifImageView?.tag = 0
                        gifImageView?.setOnClickListener(clickListener)
                    }
                    if (config.hideImageViewForNonImageMedia) {
                        relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility =
                            View.GONE
                    }
                }
            } else if (media.isVideo()) {
                if (config.hideImageViewForNonImageMedia) {
                    relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility =
                        View.GONE
                }
                prepareMedia()
                playMedia()
                videoFrameLayout?.setContentDescriptionIfNotBlank(media.contentDescription)
            } else if (media.isAudio()) {
                if (config.hideImageViewForNonImageMedia) {
                    relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility =
                        View.GONE
                }
                prepareMedia()
                playMedia()
                videoFrameLayout?.setContentDescriptionIfNotBlank(media.contentDescription)
            }
        }
    }

    private fun closeFullscreenDialog() {
        val handle = handle ?: return
        val playerView = handle.videoSurface()

        handle.switchToFullScreen(false)

        videoFrameInDialog?.removeAllViews()
        videoFrameLayout?.addView(playerView)
        exoPlayerFullscreen = false
        fullScreenDialog?.dismiss()
    }

    private fun openFullscreenDialog() {
        val handle = handle ?: return
        val playerView = handle.videoSurface()

        handle.switchToFullScreen(true)

        videoFrameLayout?.removeAllViews()

        if (fullScreenDialog == null) {
            val fullScreenDialog =
                ComponentDialog(
                    fragment.requireContext(),
                    android.R.style.Theme_Black_NoTitleBar_Fullscreen
                )
            this.fullScreenDialog = fullScreenDialog
            val fullScreenParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val videoFrameInDialog = FrameLayout(fragment.requireContext())
            this.videoFrameInDialog = videoFrameInDialog
            fullScreenDialog.addContentView(videoFrameInDialog, fullScreenParams)

            val activity = fragment.activity
            if (activity != null) {
                fullScreenDialog.onBackPressedDispatcher.addCallback(
                    activity, onBackPressedCallback
                )
            }
        }

        videoFrameInDialog?.addView(playerView)
        exoPlayerFullscreen = true
        fullScreenDialog?.show()
    }

    private fun playMedia() {
        handle?.play()
    }

    private fun prepareMedia() {
        val handle = handle ?: return
        handle.initPlayerView(fragment.requireContext(), isTablet)
        handle.setFullscreenClickListener {
            if (!exoPlayerFullscreen) {
                onBackPressedCallback.isEnabled = true
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
                onBackPressedCallback.isEnabled = false
            }
        }
        handle.setMuteClickListener()
        addViewsForStreamMedia()

        handle.initExoplayer(
            fragment.requireContext(),
            inAppNotification.mediaList[0].mediaUrl
        )
    }

    private fun addViewsForStreamMedia() {
        videoFrameLayout?.visibility = View.VISIBLE

        val handle = handle ?: return
        val videoSurface = handle.videoSurface()
        if (fillVideoFrame) {
            videoSurface.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        if (videoFrameLayout?.size == 0) {
            videoFrameLayout?.addView(videoSurface)
        } else {
            Logger.d("Video views and controls are already added, not re-attaching")
        }
    }
}
