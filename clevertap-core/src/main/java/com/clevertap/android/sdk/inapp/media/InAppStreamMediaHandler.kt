package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.CTInAppNotificationMedia
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inapps.ExoplayerHandle
import com.clevertap.android.sdk.video.inapps.Media3Handle

/**
 * Handler for video/audio stream media in InApp notifications.
 * Manages ExoPlayer/Media3 player lifecycle, view binding, and fullscreen dialog.
 */
@OptIn(UnstableApi::class)
internal class InAppStreamMediaHandler
    (
    private val fragment: Fragment,
    private val media: CTInAppNotificationMedia,
    private val isTablet: Boolean
) : InAppMediaHandler {

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

    init {
        handle = if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
            Media3Handle()
        } else {
            ExoplayerHandle()
        }
    }

    override fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener?
    ) {
        fillVideoFrame = config.fillVideoFrame
        if (config.videoFrameId != 0) {
            videoFrameLayout = relativeLayout?.findViewById(config.videoFrameId)
        }
        relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility = View.GONE
        prepareMedia()
        playMedia()
        videoFrameLayout?.setContentDescriptionIfNotBlank(media.contentDescription)
    }

    override fun onResume() {
        prepareMedia()
        playMedia()
    }

    override fun onPause() {
        if (exoPlayerFullscreen) {
            closeFullscreenDialog()
            onBackPressedCallback.isEnabled = false
        }
        handle?.savePosition()
        handle?.pause()
    }

    override fun onStop() {
        handle?.pause()
    }

    override fun cleanup() {
        handle?.pause()
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
        handle.initExoplayer(fragment.requireContext(), media.mediaUrl)
    }

    private fun playMedia() {
        handle?.play()
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
            val dialog = ComponentDialog(
                fragment.requireContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
            )
            this.fullScreenDialog = dialog
            val fullScreenParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val frame = FrameLayout(fragment.requireContext())
            this.videoFrameInDialog = frame
            dialog.addContentView(frame, fullScreenParams)

            val activity = fragment.activity
            if (activity != null) {
                dialog.onBackPressedDispatcher.addCallback(activity, onBackPressedCallback)
            }
        }

        videoFrameInDialog?.addView(playerView)
        exoPlayerFullscreen = true
        fullScreenDialog?.show()
    }
}
