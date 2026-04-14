package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.graphics.Insets
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.pipsdk.PIPConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPDragHandler
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession

/**
 * Compact draggable PIP window.
 *
 * Layout:
 * - Close button at top-right
 * - Play/pause button centered (video-only)
 * - Bottom-right row: deeplink, mute, expand
 *
 * Contains the shared [mediaView] (moved out during expand), a [PIPControlsOverlay]
 * with controls, and a [PIPDragHandler] for drag-to-reposition + snap.
 */
internal class PIPCompactView(
    context: Context,
    val mediaView: PIPMediaView,
    private val session: PIPSession,
    private val onExpand: () -> Unit,
    private val onClose: () -> Unit,
    private val onAction: () -> Unit,
    private val onSnap: (PIPPosition) -> Unit,
) : FrameLayout(context) {

    internal val controlsOverlay: PIPControlsOverlay
    private val dragHandler: PIPDragHandler
    private var closeBtn: ImageView? = null
    private var playPauseBtn: ImageView? = null
    private var deeplinkBtn: ImageView? = null
    private var muteBtn: ImageView? = null
    private var expandBtn: ImageView? = null
    var getSafeInsets: () -> Insets = { Insets.NONE }

    init {
        val cfg = session.config

        elevation = ELEVATION_DP.dpToPx(context).toFloat()
        applyBorderStyle(cfg)

        // Media fills the view
        addView(mediaView, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay (initially hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f

        val iconSizePx = MIN_ICON_SIZE_DP.dpToPx(context) // provisional; updated by updateIconSizes()
        val centerIconSizePx = (iconSizePx * CENTER_ICON_SCALE).toInt()
        val iconGapPx = ICON_GAP_DP.dpToPx(context)
        val iconMarginPx = ICON_MARGIN_DP.dpToPx(context)

        // Close button — top-right
        val clsBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_close_pip)
            contentDescription = context.getString(R.string.ct_inapp_close_btn)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        closeBtn = clsBtn
        controlsOverlay.addView(
            clsBtn,
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

        // Bottom-right row: deeplink, mute, expand
        val bottomRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Deeplink button
        val dlBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_deeplink)
            contentDescription = context.getString(R.string.ct_action_button_content_description)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.action != null) View.VISIBLE else View.GONE
            setOnClickListener { onAction() }
        }
        deeplinkBtn = dlBtn
        bottomRow.addView(dlBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx))

        // Mute button (video-only, initially hidden)
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

        // Expand button
        val expBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_expand)
            contentDescription = context.getString(R.string.ct_pip_expand_button_content_description)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.showExpandCollapseButton) View.VISIBLE else View.GONE
            setOnClickListener { onExpand() }
        }
        expandBtn = expBtn
        bottomRow.addView(expBtn, LinearLayout.LayoutParams(iconSizePx, iconSizePx).apply {
            marginStart = iconGapPx
        })

        controlsOverlay.addView(
            bottomRow,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.END).apply {
                setMargins(iconMarginPx, iconMarginPx, iconMarginPx, iconMarginPx)
            },
        )

        addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val bottomOffsetPx = PIPDimens.BOTTOM_NAV_OFFSET_DP.dpToPx(context)
        dragHandler = PIPDragHandler(
            view = this,
            dragEnabled = cfg.dragEnabled,
            getHorizontalEdgeMarginPercent = { session.config.horizontalEdgeMarginPercent },
            getVerticalEdgeMarginPercent = { session.config.verticalEdgeMarginPercent },
            getSafeInsets = { getSafeInsets() },
            getBottomOffsetPx = { bottomOffsetPx },
            onSnapComplete = { newPos ->
                session.currentPosition = newPos
                onSnap(newPos)
            },
            onTap = { controlsOverlay.showControls() },
        )
    }

    /**
     * Wires video-specific controls: play/pause (center) and mute (bottom-right row).
     * Call after media is attached.
     */
    fun bindVideoControls(mv: PIPMediaView) {
        if (!mv.isVideoType) return

        // Show and wire play/pause button (center)
        playPauseBtn?.apply {
            visibility = if (session.config.showPlayPauseButton) View.VISIBLE else View.GONE
            setOnClickListener {
                mv.togglePlayPause()
                updatePlayPauseIcon(mv.isPlaying)
                controlsOverlay.resetAutoHideTimer()
            }
        }
        updatePlayPauseIcon(mv.isPlaying)

        // Show and wire mute button (in bottom-right row)
        muteBtn?.apply {
            visibility = if (session.config.showMuteButton) View.VISIBLE else View.GONE
            setOnClickListener {
                mv.toggleMute()
                updateMuteIcon(mv.isMuted)
                controlsOverlay.resetAutoHideTimer()
            }
        }
        updateMuteIcon(mv.isMuted)
    }

    /** Hides video-specific controls. Called when video falls back to static image. */
    fun hideVideoControls() {
        playPauseBtn?.visibility = View.GONE
        playPauseBtn?.setOnClickListener(null)
        muteBtn?.visibility = View.GONE
        muteBtn?.setOnClickListener(null)
    }

    /** Syncs play/pause icon with current state — called after collapsing or on ExoPlayer state change. */
    fun syncPlayPauseIcon(playing: Boolean) = updatePlayPauseIcon(playing)

    /** Syncs mute icon with current state — called after collapsing from expanded view. */
    fun syncMuteIcon(muted: Boolean) = updateMuteIcon(muted)

    // ─── Icon sizing ────────────────────────────────────────────────────────────

    /**
     * Recomputes icon sizes based on actual PIP width.
     * Called from [PIPRootContainer.positionAndShow] after pipW is finalized
     * (accounts for height clamping and border padding in both portrait and landscape).
     *
     * Width is the constraining dimension because the bottom-right row of icons
     * (deeplink, mute, expand) is laid out horizontally within the PIP width.
     */
    fun updateIconSizes(pipWidthPx: Int) {
        val iconSizePx = resolveIconSize(pipWidthPx)
        listOfNotNull(deeplinkBtn, closeBtn, muteBtn, expandBtn).forEach { btn ->
            btn.layoutParams.width = iconSizePx
            btn.layoutParams.height = iconSizePx
        }
        playPauseBtn?.let {
            val centerSize = (iconSizePx * CENTER_ICON_SCALE).toInt()
            it.layoutParams.width = centerSize
            it.layoutParams.height = centerSize
        }
    }

    // ─── Touch handling ──────────────────────────────────────────────────────────

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Always store starting position; don't intercept yet so buttons can fire
                dragHandler.onInterceptDown(ev)
                false
            }
            MotionEvent.ACTION_MOVE -> dragHandler.shouldIntercept(ev)
            else -> false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> true     // claim gesture stream when no child consumed
            else -> dragHandler.onTouchEvent(event)
        }
    }

    fun detach() = controlsOverlay.detach()

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

    private fun resolveIconSize(pipWidthPx: Int): Int {
        val iconSizePx = (pipWidthPx * ICON_SIZE_FRACTION).toInt()
        return iconSizePx.coerceIn(
            MIN_ICON_SIZE_DP.dpToPx(context),
            MAX_ICON_SIZE_DP.dpToPx(context),
        )
    }

    private fun applyBorderStyle(cfg: PIPConfig) {
        val hasBorderStyle = cfg.mediaType != PIPMediaType.VIDEO &&
                (cfg.cornerRadiusDp > 0 || cfg.borderEnabled)

        if (!hasBorderStyle) {
            outlineProvider = ViewOutlineProvider.BOUNDS
            setBackgroundColor(Color.BLACK)
            return
        }

        val radiusPx = cfg.cornerRadiusDp.dpToPx(context).toFloat()
        val borderPx = if (cfg.borderEnabled && cfg.borderWidthDp > 0)
            cfg.borderWidthDp.dpToPx(context) else 0

        background = GradientDrawable().apply {
            setColor(Color.BLACK)
            cornerRadius = radiusPx
            if (borderPx > 0) {
                setStroke(borderPx, cfg.borderColor)
            }
        }

        if (borderPx > 0) {
            setPadding(borderPx, borderPx, borderPx, borderPx)
        }

        if (radiusPx > 0f) {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                }
            }
        }
    }

    private companion object {
        const val ELEVATION_DP = 6
        const val ICON_GAP_DP = 8
        const val ICON_MARGIN_DP = 4

        /** Icon size as a fraction of the larger PIP dimension (18%). */
        private const val ICON_SIZE_FRACTION = 0.18f
        private const val MIN_ICON_SIZE_DP = 24
        private const val MAX_ICON_SIZE_DP = 40

        /** Center play/pause is proportionally larger than corner icons. */
        private const val CENTER_ICON_SCALE = 1f
    }
}
