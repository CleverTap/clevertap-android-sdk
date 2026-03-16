package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.TextView
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
    private val onCollapse: () -> Unit,
    private val onClose: () -> Unit,
) : FrameLayout(context) {

    internal val mediaContainer: FrameLayout
    private val controlsOverlay: PIPControlsOverlay
    private var playPauseBtn: TextView? = null
    private var muteBtn: TextView? = null

    init {
        setBackgroundColor(Color.BLACK)

        // Media container — fills full expanded view
        mediaContainer = FrameLayout(context)
        addView(mediaContainer, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay covers full view (starts hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f

        val padPx = 16.dpToPx(context)

        // Collapse button — top-left
        val collapseBtn = TextView(context).apply {
            text = "\u2190"     // ← left arrow
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(padPx, padPx, padPx, padPx)
            setOnClickListener { onCollapse() }
        }
        controlsOverlay.addView(
            collapseBtn,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.START),
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
        val ppBtn = TextView(context).apply {
            text = "\u23F8"     // ⏸
            textSize = 36f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        playPauseBtn = ppBtn
        controlsOverlay.addView(ppBtn, LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))

        // Mute button — bottom-right (video only; hidden until bindMedia)
        val mBtn = TextView(context).apply {
            text = "\uD83D\uDD07"   // 🔇
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = View.GONE
        }
        muteBtn = mBtn
        controlsOverlay.addView(
            mBtn,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.END),
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
        playPauseBtn?.text = if (playing) "\u23F8" else "\u25B6"    // ⏸ or ▶
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.text = if (muted) "\uD83D\uDD07" else "\uD83D\uDD0A"  // 🔇 or 🔊
    }
}
