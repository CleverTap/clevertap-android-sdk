package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession

/**
 * Full-screen expanded PIP overlay.
 *
 * Layout (all programmatic):
 * - Dark background (scrim) fills the view
 * - [mediaContainer] fills the view (video uses FIT mode internally)
 * - [controlsWrapper] is sized to match the video's aspect ratio and centred over the media;
 *   it hosts [controlsOverlay] with close at top-right and a bottom control row
 *
 * [bindMedia] must be called after the view has been measured (use post/onReady callback).
 */
internal class PIPExpandedView(
    context: Context,
    private val showCloseButton: Boolean,
    private val redirectUrl: String?,
    private val showExpandCollapseButton: Boolean = true,
    private val showPlayPauseButton: Boolean = true,
    private val showMuteButton: Boolean = true,
    private val onCollapse: () -> Unit,
    private val onClose: () -> Unit,
    private val onRedirect: () -> Unit,
) : FrameLayout(context) {

    internal val mediaContainer: FrameLayout
    private val controlsWrapper: FrameLayout
    private val controlsOverlay: PIPControlsOverlay
    private var playPauseBtn: ImageView? = null
    private var muteBtn: ImageView? = null
    private lateinit var bottomRow: LinearLayout
    private lateinit var rowSpacer: View

    // Aspect ratio set in bindMedia(); used by layout change listener to size controlsWrapper.
    private var aspectRatioW = 16f
    private var aspectRatioH = 9f

    init {
        setBackgroundColor(Color.BLACK)

        // Media container — fills full expanded view (edge-to-edge).
        // Consume insets here so ExoPlayer's PlayerView doesn't offset exo_content_frame.
        mediaContainer = FrameLayout(context)
        ViewCompat.setOnApplyWindowInsetsListener(mediaContainer) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }
        addView(mediaContainer, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls wrapper — sized to video aspect ratio, centred over media.
        // Starts as MATCH_PARENT; resized by layout change listener once dimensions are known.
        controlsWrapper = FrameLayout(context)
        addView(controlsWrapper, LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER))

        // Controls overlay inside the wrapper (starts hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f
        controlsWrapper.addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val padPx = ICON_PADDING_DP.dpToPx(context)
        val iconSizePx = ICON_SIZE_DP.dpToPx(context)
        val iconMarginPx = ICON_MARGIN_DP.dpToPx(context)
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)
        val iconGapPx = ICON_GAP_DP.dpToPx(context)

        // Close button — top-right of video frame
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_close_pip)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        controlsOverlay.addView(
            closeBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.TOP or Gravity.END).apply {
                setMargins(iconMarginPx, iconMarginPx, iconMarginPx, iconMarginPx)
            },
        )

        // Bottom control row — centred horizontally at bottom of video frame (video),
        // or full-width with space-between for image/GIF.
        bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Deeplink button (hidden if redirectUrl is null)
        val deeplinkBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_deeplink)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (redirectUrl != null) View.VISIBLE else View.GONE
            setOnClickListener { onRedirect() }
        }
        bottomRow.addView(deeplinkBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginEnd = iconGapPx
        })

        // Mute button (video only; hidden until bindMedia)
        val mBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_volume_off_tint)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = View.GONE
        }
        muteBtn = mBtn
        bottomRow.addView(mBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginEnd = iconGapPx
        })

        // Play/Pause button (video only; hidden until bindMedia)
        val ppBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_pause)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = View.GONE
        }
        playPauseBtn = ppBtn
        bottomRow.addView(ppBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginEnd = iconGapPx
        })

        // Spacer — hidden for video (icons grouped center), visible for image/GIF (pushes
        // deeplink to left and collapse to right within the row).
        rowSpacer = View(context)
        rowSpacer.visibility = View.GONE
        bottomRow.addView(rowSpacer, LinearLayout.LayoutParams(0, 0, 1f))

        // Collapse button (hidden if expandCollapse control disabled)
        val collapseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_collapse)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (showExpandCollapseButton) View.VISIBLE else View.GONE
            setOnClickListener { onCollapse() }
        }
        bottomRow.addView(collapseBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx))

        controlsOverlay.addView(
            bottomRow,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                setMargins(rowMarginPx, rowMarginPx, rowMarginPx, rowMarginPx)
            },
        )

        // Tap anywhere on scrim to reveal controls
        setOnClickListener { controlsOverlay.showControls() }

        // Resize controlsWrapper whenever this view's dimensions change (initial layout, rotation).
        addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = right - left
            val h = bottom - top
            if (w > 0 && h > 0) {
                sizeControlsWrapper(w, h)
            }
        }
    }

    /**
     * Inserts [mv] into [mediaContainer] and wires video controls.
     * Updates the aspect ratio used to size [controlsWrapper] over the video bounds.
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

        // Update aspect ratio — layout change listener will resize controlsWrapper
        aspectRatioW = session.config.aspectRatioNumerator.toFloat()
        aspectRatioH = session.config.aspectRatioDenominator.toFloat()

        // Wire video-only controls (respecting server-configured visibility)
        val isVideo = mv.isVideoType
        playPauseBtn?.visibility = if (isVideo && showPlayPauseButton) View.VISIBLE else View.GONE
        muteBtn?.visibility = if (isVideo && showMuteButton) View.VISIBLE else View.GONE
        applyBottomRowLayout(isVideo)

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

    /** Hides video-specific controls (play/pause, mute). Called when video falls back to static image. */
    fun hideVideoControls() {
        playPauseBtn?.visibility = View.GONE
        playPauseBtn?.setOnClickListener(null)
        muteBtn?.visibility = View.GONE
        muteBtn?.setOnClickListener(null)
        applyBottomRowLayout(isVideo = false)
    }

    /**
     * Switches the bottom row between two layouts:
     * - **Video:** WRAP_CONTENT centered (icons grouped in the middle)
     * - **Image/GIF:** MATCH_PARENT with spacer (deeplink left, collapse right)
     */
    private fun applyBottomRowLayout(isVideo: Boolean) {
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)
        if (isVideo) {
            rowSpacer.visibility = View.GONE
            bottomRow.layoutParams = LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { setMargins(rowMarginPx, rowMarginPx, rowMarginPx, rowMarginPx) }
        } else {
            rowSpacer.visibility = View.VISIBLE
            bottomRow.layoutParams = LayoutParams(
                MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM
            ).apply { setMargins(rowMarginPx, rowMarginPx, rowMarginPx, rowMarginPx) }
        }
    }

    /**
     * Sizes [controlsWrapper] to the aspect-ratio-correct area that matches
     * the video's visible bounds within [mediaContainer] (FIT mode).
     */
    private fun sizeControlsWrapper(containerW: Int, containerH: Int) {
        val fitW: Int
        val fitH: Int
        if (containerW.toFloat() / containerH > aspectRatioW / aspectRatioH) {
            // Container is wider than video — height-constrained
            fitH = containerH
            fitW = (fitH * aspectRatioW / aspectRatioH).toInt()
        } else {
            // Container is taller than video — width-constrained
            fitW = containerW
            fitH = (fitW * aspectRatioH / aspectRatioW).toInt()
        }

        val lp = controlsWrapper.layoutParams as? LayoutParams
        if (lp != null && lp.width == fitW && lp.height == fitH) return // no change
        controlsWrapper.layoutParams = LayoutParams(fitW, fitH, Gravity.CENTER)
    }

    private fun updatePlayPauseIcon(playing: Boolean) {
        playPauseBtn?.setImageResource(
            if (playing) R.drawable.ct_ic_pause else R.drawable.ct_ic_play
        )
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.setImageResource(
            if (muted) R.drawable.ct_ic_volume_off_tint else R.drawable.ct_ic_volume_on_tint
        )
    }

    private companion object {
        const val ICON_SIZE_DP = 56
        const val ICON_PADDING_DP = 14
        const val ICON_MARGIN_DP = 8
        const val ROW_MARGIN_DP = 12
        const val ICON_GAP_DP = 12
    }
}
