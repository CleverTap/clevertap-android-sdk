package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.util.UnstableApi
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
    private val isTablet: Boolean,
    private val onActionClick: (() -> Unit)? = null
) : InAppMediaHandler {

    private var handle: InAppVideoPlayerHandle? = null
    private var videoFrameLayout: FrameLayout? = null
    private var fullScreenDialog: ComponentDialog? = null
    private var videoFrameInDialog: FrameLayout? = null
    private var exoPlayerFullscreen = false

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (exoPlayerFullscreen) {
                closeFullscreenDialog()
                isEnabled = false
            }
        }
    }

    init {
         // Reclaim a live player that survived rotation, falling back to a fresh handle.
        handle = InAppVideoPlayerCache.consume(media.mediaUrl)
            ?: if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
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
        if (config.videoFrameId != 0) {
            videoFrameLayout = relativeLayout?.findViewById(config.videoFrameId)
        }
        relativeLayout?.findViewById<ImageView>(config.imageViewId)?.visibility = View.GONE
        prepareMedia()
        playMedia()
        // Restore fullscreen state if this Fragment was recreated after a rotation that
        // happened while the video was in fullscreen mode.
        if (InAppVideoPlayerCache.consumeFullscreen()) {
            onBackPressedCallback.isEnabled = true
            openFullscreenDialog()
        }
        videoFrameLayout?.setContentDescriptionIfNotBlank(media.contentDescription)
    }

    override fun onResume(owner: LifecycleOwner) {
        prepareMedia()
        playMedia()
    }

    override fun onPause(owner: LifecycleOwner) {
        if (fragment.activity?.isChangingConfigurations == true) {
            // Rotation: the Activity (and its dialog) is being destroyed, so we must close the
            // fullscreen dialog explicitly. Save the state first so setup() can restore it.
            val wasFullscreen = exoPlayerFullscreen
            if (exoPlayerFullscreen) {
                closeFullscreenDialog()
                onBackPressedCallback.isEnabled = false
            }
            val h = handle ?: return
            h.detachSurface()
            InAppVideoPlayerCache.store(h, media.mediaUrl, isFullscreen = wasFullscreen)
        } else {
            // Background: leave fullscreen dialog intact — it will still be there on resume.
            handle?.softPause()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (fragment.activity?.isChangingConfigurations == true) return
        handle?.softPause()
    }

    override fun cleanup() {
        handle?.pause()
        InAppVideoPlayerCache.release()
    }

    private fun prepareMedia() {
        val handle = handle ?: return
        handle.initPlayerView(fragment.requireContext(), isTablet)
        handle.setFullscreenClickListener { isFullScreen ->
            if (!exoPlayerFullscreen) {
                onBackPressedCallback.isEnabled = true
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
                onBackPressedCallback.isEnabled = false
            }
        }
        handle.setMuteClickListener()
        onActionClick?.let { callback ->
            handle.setActionClickListener { callback() }
        }
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
        videoSurface.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (videoSurface.parent == null) {
            videoFrameLayout?.addView(videoSurface)
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
            val context = fragment.requireContext()
            val dialog = ComponentDialog(
                context,
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
            )
            this.fullScreenDialog = dialog
            val fullScreenParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val frame = FrameLayout(context)
            this.videoFrameInDialog = frame
            dialog.addContentView(frame, fullScreenParams)

            frame.applyInsetsWithMarginAdjustment { insets, mlp ->
                mlp.leftMargin = insets.left
                mlp.topMargin = insets.top
                mlp.rightMargin = insets.right
                mlp.bottomMargin = insets.bottom
            }

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
