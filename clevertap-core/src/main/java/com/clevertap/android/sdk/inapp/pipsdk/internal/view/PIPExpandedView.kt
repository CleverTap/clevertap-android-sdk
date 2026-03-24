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
import androidx.core.graphics.Insets
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
 * - [mediaContainer] fills the view edge-to-edge (video uses FIT mode internally)
 * - [controlsOverlay] fills the view with **no padding**; individual controls use
 *   inset-based margins so centering stays relative to the full screen (matching the
 *   edge-to-edge media) while each button independently avoids unsafe edges
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
    private val controlsOverlay: PIPControlsOverlay
    private lateinit var closeBtn: ImageView
    private var playPauseBtn: ImageView? = null
    private var muteBtn: ImageView? = null
    private lateinit var bottomRow: LinearLayout
    private lateinit var rowSpacer: View

    /** Current system insets — updated by the insets listener, used for per-button margins. */
    private var currentInsets = Insets.NONE

    /** Whether the current media is video (affects bottom row margin strategy). */
    private var isVideoMode = false

    init {
        setBackgroundColor(Color.BLACK)

        // Media container — fills full expanded view (edge-to-edge).
        // Consume insets here so ExoPlayer's PlayerView doesn't offset exo_content_frame.
        mediaContainer = FrameLayout(context)
        ViewCompat.setOnApplyWindowInsetsListener(mediaContainer) { _, _ ->
            WindowInsetsCompat.CONSUMED
        }
        addView(mediaContainer, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay — fills full screen with NO padding.
        // Per-button margins handle insets so Gravity centering stays relative to full screen.
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f
        ViewCompat.setOnApplyWindowInsetsListener(controlsOverlay) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            if (insets != currentInsets) {
                currentInsets = insets
                updateInsetsMargins()
            }
            WindowInsetsCompat.CONSUMED
        }
        addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val padPx = ICON_PADDING_DP.dpToPx(context)
        val iconSizePx = ICON_SIZE_DP.dpToPx(context)
        val iconMarginPx = ICON_MARGIN_DP.dpToPx(context)
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)
        val iconGapPx = ICON_GAP_DP.dpToPx(context)

        // Close button — top-right of screen; inset margins keep it clear of status bar / cutout
        closeBtn = ImageView(context).apply {
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

        // Bottom control row — anchored to screen bottom; inset margins keep it clear of nav bar.
        // Centred horizontally for video, or full-width with spacer for image/GIF.
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
    }

    /**
     * Inserts [mv] into [mediaContainer] and wires video controls.
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
     * Updates inset-based margins on the close button and bottom row.
     * Called when system insets change (initial layout, rotation).
     */
    private fun updateInsetsMargins() {
        val iconMarginPx = ICON_MARGIN_DP.dpToPx(context)
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)

        // Close button: push away from top + right edges
        (closeBtn.layoutParams as LayoutParams).apply {
            topMargin = currentInsets.top + iconMarginPx
            rightMargin = currentInsets.right + iconMarginPx
            leftMargin = iconMarginPx
            bottomMargin = iconMarginPx
        }
        closeBtn.requestLayout()

        // Bottom row: push away from bottom; side insets depend on mode
        applyBottomRowMargins(rowMarginPx)
    }

    /**
     * Switches the bottom row between two layouts:
     * - **Video:** WRAP_CONTENT centered (icons grouped in the middle)
     * - **Image/GIF:** MATCH_PARENT with spacer (deeplink left, collapse right)
     *
     * Also applies inset-based margins via [applyBottomRowMargins].
     */
    private fun applyBottomRowLayout(isVideo: Boolean) {
        isVideoMode = isVideo
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)
        if (isVideo) {
            rowSpacer.visibility = View.GONE
            bottomRow.layoutParams = LayoutParams(
                WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        } else {
            rowSpacer.visibility = View.VISIBLE
            bottomRow.layoutParams = LayoutParams(
                MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM
            )
        }
        applyBottomRowMargins(rowMarginPx)
    }

    /**
     * Sets bottom row margins incorporating current insets.
     * - **Video:** no side insets — centering stays relative to full screen (matching video)
     * - **Image/GIF:** side insets applied (MATCH_PARENT row needs to avoid cutout)
     */
private fun applyBottomRowMargins(rowMarginPx: Int) {
        (bottomRow.layoutParams as LayoutParams).apply {
            bottomMargin = currentInsets.bottom + rowMarginPx
            topMargin = rowMarginPx
            if (isVideoMode) {
                leftMargin = rowMarginPx
                rightMargin = rowMarginPx
            } else {
                leftMargin = currentInsets.left + rowMarginPx
                rightMargin = currentInsets.right + rowMarginPx
            }
        }
        bottomRow.requestLayout()
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
