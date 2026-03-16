package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession

/**
 * Full-screen expanded PIP overlay.
 *
 * Layout (all programmatic):
 * - Dark semi-transparent background (scrim) fills the view
 * - [mediaContainer] is centred with the correct aspect ratio
 * - [controlsOverlay] covers the whole view; has Collapse, Close, Play/Pause, Mute buttons
 *
 * [bindMedia] must be called after the view has been measured (use post/onReady callback).
 */
internal class PIPExpandedView(
    context: Context,
    private val showCloseButton: Boolean,
    private val redirectUrl: String?,
    private val onCollapse: () -> Unit,
    private val onClose: () -> Unit,
    private val onRedirect: () -> Unit,
) : FrameLayout(context) {

    internal val mediaContainer: FrameLayout
    private val controlsOverlay: PIPControlsOverlay
    private var playPauseBtn: ImageView? = null
    private var muteBtn: ImageView? = null

    init {
        setBackgroundColor(Color.BLACK)

        // Media container — fills full expanded view
        mediaContainer = FrameLayout(context)
        addView(mediaContainer, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay covers full view (starts hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f

        val padPx = 16.dpToPx(context)
        val iconSizePx = 58.dpToPx(context)
        val playPauseSizePx = 64.dpToPx(context)

        // Deeplink button — top-left (hidden if redirectUrl is null)
        val deeplinkBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_action)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (redirectUrl != null) View.VISIBLE else View.GONE
            setOnClickListener { onRedirect() }
        }
        controlsOverlay.addView(
            deeplinkBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.TOP or Gravity.START),
        )

        // Close button — top-right (hidden if showCloseButton = false)
        val closeBtn = TextView(context).apply {
            text = "\u2715"     // ✕
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        controlsOverlay.addView(
            closeBtn,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END),
        )

        // Play/Pause button — centre (video only; hidden until bindMedia)
        val ppBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_pause)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
        }
        playPauseBtn = ppBtn
        controlsOverlay.addView(ppBtn, LayoutParams(playPauseSizePx, playPauseSizePx, Gravity.CENTER))

        // Mute button — bottom-left (video only; hidden until bindMedia)
        val mBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_volume_off)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = View.GONE
        }
        muteBtn = mBtn
        controlsOverlay.addView(
            mBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.BOTTOM or Gravity.START),
        )

        // Collapse button — bottom-right
        val collapseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_collapse)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            setOnClickListener { onCollapse() }
        }
        controlsOverlay.addView(
            collapseBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.BOTTOM or Gravity.END),
        )

        addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Tap anywhere on scrim to reveal controls
        setOnClickListener { controlsOverlay.showControls() }
    }

    /**
     * Sizes [mediaContainer], inserts [mv], and wires video controls.
     * [containerWidth]/[containerHeight] are the PIPRootContainer's dimensions (already laid out).
     * [onReady] fires once layout is applied; used to start the entry animation.
     */
    fun bindMedia(
        mv: PIPMediaView,
        session: PIPSession,
        onReady: () -> Unit,
    ) {
        mediaContainer.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        mediaContainer.removeAllViews()
        mediaContainer.addView(mv, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Wire video-only controls
        val isVideo = mv.isVideoType
        playPauseBtn?.visibility = if (isVideo) View.VISIBLE else View.GONE
        muteBtn?.visibility = if (isVideo) View.VISIBLE else View.GONE

        if (isVideo) {
            updatePlayPauseIcon(mv.isPlaying)
            updateMuteIcon(mv.isMuted)

            playPauseBtn?.setOnClickListener {
                mv.togglePlayPause()
                updatePlayPauseIcon(mv.isPlaying)
                controlsOverlay.resetAutoHideTimer()
            }
            muteBtn?.setOnClickListener {
                mv.toggleMute()
                updateMuteIcon(mv.isMuted)
                controlsOverlay.resetAutoHideTimer()
            }
        }

        // Defer onReady until after the layout pass so mediaContainer has real dimensions
        post(onReady)
    }

    fun showControls() = controlsOverlay.showControls()
    fun detach() = controlsOverlay.detach()

    private fun updatePlayPauseIcon(playing: Boolean) {
        playPauseBtn?.setImageResource(
            if (playing) R.drawable.ct_ic_pause else R.drawable.ct_ic_play
        )
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.setImageResource(
            if (muted) R.drawable.ct_ic_volume_off else R.drawable.ct_ic_volume_on
        )
    }
}
