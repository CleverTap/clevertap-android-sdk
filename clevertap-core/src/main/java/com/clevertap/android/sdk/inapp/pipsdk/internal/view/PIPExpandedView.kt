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
 *   inset-based margins so centering stays relative to the full screen
 *
 * Controls layout:
 * - Close button: top-right
 * - Play/pause button: center (video-only)
 * - Bottom-right row: deeplink, mute, collapse
 *
 * [bindMedia] must be called after the view has been measured (use post/onReady callback).
 */
internal class PIPExpandedView(
    context: Context,
    private val showCloseButton: Boolean,
    private val hasAction: Boolean,
    private val showExpandCollapseButton: Boolean = true,
    private val showPlayPauseButton: Boolean = true,
    private val showMuteButton: Boolean = true,
    private val onCollapse: () -> Unit,
    private val onClose: () -> Unit,
    private val onAction: () -> Unit,
) : FrameLayout(context) {

    internal val mediaContainer: FrameLayout
    private val controlsOverlay: PIPControlsOverlay
    private val closeBtn: ImageView
    private var playPauseBtn: ImageView? = null
    private var muteBtn: ImageView? = null
    private val bottomRow: LinearLayout

    /** Current system insets — updated by the insets listener, used for per-button margins. */
    private var currentInsets = Insets.NONE

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

        val iconSizePx = ICON_SIZE_DP.dpToPx(context)
        val centerIconSizePx = CENTER_ICON_SIZE_DP.dpToPx(context)
        val iconMarginPx = ICON_MARGIN_DP.dpToPx(context)
        val rowMarginPx = ROW_MARGIN_DP.dpToPx(context)
        val iconGapPx = ICON_GAP_DP.dpToPx(context)

        // Close button — top-right; inset margins keep it clear of status bar / cutout
        closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_close_pip)
            contentDescription = context.getString(R.string.ct_inapp_close_btn)
            scaleType = ImageView.ScaleType.FIT_CENTER

            visibility = if (showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        controlsOverlay.addView(
            closeBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.TOP or Gravity.END).apply {
                setMargins(iconMarginPx, iconMarginPx, iconMarginPx, iconMarginPx)
            },
        )

        // Play/Pause button — center (video-only, initially hidden)
        val ppBtn = ImageView(context).apply {
            setImageResource(PIPIcons.playPauseIcon(playing = true))
            contentDescription = context.getString(PIPIcons.playPauseContentDescription(playing = true))
            scaleType = ImageView.ScaleType.FIT_CENTER

            visibility = View.GONE
        }
        playPauseBtn = ppBtn
        controlsOverlay.addView(
            ppBtn,
            LayoutParams(centerIconSizePx, centerIconSizePx, Gravity.CENTER),
        )

        // Bottom-right control row: deeplink, mute, collapse
        bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Action button (hidden if no action configured)
        val deeplinkBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_deeplink)
            contentDescription = context.getString(R.string.ct_action_button_content_description)
            scaleType = ImageView.ScaleType.FIT_CENTER

            visibility = if (hasAction) View.VISIBLE else View.GONE
            setOnClickListener { onAction() }
        }
        bottomRow.addView(deeplinkBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx))

        // Mute button (video only; hidden until bindMedia)
        val mBtn = ImageView(context).apply {
            setImageResource(PIPIcons.muteIcon(muted = true))
            contentDescription = context.getString(PIPIcons.muteContentDescription(muted = true))
            scaleType = ImageView.ScaleType.FIT_CENTER

            visibility = View.GONE
        }
        muteBtn = mBtn
        bottomRow.addView(mBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginStart = iconGapPx
        })

        // Collapse button (hidden if expandCollapse control disabled)
        val collapseBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_collapse)
            contentDescription = context.getString(R.string.ct_pip_collapse_button_content_description)
            scaleType = ImageView.ScaleType.FIT_CENTER

            visibility = if (showExpandCollapseButton) View.VISIBLE else View.GONE
            setOnClickListener { onCollapse() }
        }
        bottomRow.addView(collapseBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginStart = iconGapPx
        })

        controlsOverlay.addView(
            bottomRow,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(rowMarginPx, rowMarginPx, iconMarginPx, rowMarginPx)
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

        val isVideo = mv.isVideoType

        // Center play/pause — video-only
        playPauseBtn?.visibility = if (isVideo && showPlayPauseButton) View.VISIBLE else View.GONE
        // Bottom-right mute — video-only
        muteBtn?.visibility = if (isVideo && showMuteButton) View.VISIBLE else View.GONE

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
    }

    /** Syncs play/pause icon with current state — called when ExoPlayer's playing state
     *  changes independently (e.g., buffering → playing after network recovery). */
    fun syncPlayPauseIcon(playing: Boolean) = updatePlayPauseIcon(playing)

    // ─── Inset handling ──────────────────────────────────────────────────────────

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

        // Bottom row: push away from bottom + right edges.
        // Right margin uses iconMarginPx (not rowMarginPx) so the last icon in the row
        // aligns with the close button's right edge.
        (bottomRow.layoutParams as LayoutParams).apply {
            bottomMargin = currentInsets.bottom + rowMarginPx
            rightMargin = currentInsets.right + iconMarginPx
            topMargin = rowMarginPx
            leftMargin = rowMarginPx
        }
        bottomRow.requestLayout()
    }

    // ─── Private helpers ─────────────────────────────────────────────────────────

    private fun updatePlayPauseIcon(playing: Boolean) {
        playPauseBtn?.setImageResource(PIPIcons.playPauseIcon(playing))
        playPauseBtn?.contentDescription =
            playPauseBtn?.context?.getString(PIPIcons.playPauseContentDescription(playing))
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.setImageResource(PIPIcons.muteIcon(muted))
        muteBtn?.contentDescription =
            muteBtn?.context?.getString(PIPIcons.muteContentDescription(muted))
    }

    private companion object {
        const val ICON_SIZE_DP = 48
        const val CENTER_ICON_SIZE_DP = 48
        const val ICON_MARGIN_DP = 8
        const val ROW_MARGIN_DP = 12
        const val ICON_GAP_DP = 12
    }
}
