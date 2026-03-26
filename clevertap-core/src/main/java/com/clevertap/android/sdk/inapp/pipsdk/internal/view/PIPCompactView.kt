package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.Insets
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPDragHandler
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession

/**
 * Compact draggable PIP window.
 *
 * Contains the shared [mediaView] (moved out during expand), a [PIPControlsOverlay]
 * with Close and Expand buttons, and a [PIPDragHandler] for drag-to-reposition + snap.
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
    private var deeplinkBtn: ImageView? = null
    private var muteBtn: ImageView? = null
    var getSafeInsets: () -> Insets = { Insets.NONE }

    init {
        val cfg = session.config

        // Drop shadow — BOUNDS outline needed because FrameLayout has no background
        outlineProvider = android.view.ViewOutlineProvider.BOUNDS
        elevation = ELEVATION_DP.dpToPx(context).toFloat()
        setBackgroundColor(Color.BLACK)

        // Media fills the view
        addView(mediaView, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay (initially hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f

        val iconSizePx = ICON_SIZE_DP.dpToPx(context)

        // Deeplink button — bottom-left by default (image/GIF); moved to top-left for video
        // in bindVideoControls() to avoid overlap with the mute button.
        val dlBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_deeplink)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.action != null) View.VISIBLE else View.GONE
            setOnClickListener { onAction() }
        }
        deeplinkBtn = dlBtn
        controlsOverlay.addView(
            dlBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.BOTTOM or Gravity.START),
        )

        // Close button — top-right (hidden if showCloseButton = false)
        val closeBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_close_pip)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        controlsOverlay.addView(
            closeBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.TOP or Gravity.END),
        )

        // Mute button — bottom-left (video only; hidden until bindVideoControls)
        val mBtn = ImageView(context).apply {
            setImageResource(PIPIcons.muteIcon(muted = true))
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = View.GONE
        }
        muteBtn = mBtn
        controlsOverlay.addView(
            mBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.BOTTOM or Gravity.START),
        )

        // Expand button — bottom-right (hidden if expandCollapse control disabled)
        val expandBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_expand)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.showExpandCollapseButton) View.VISIBLE else View.GONE
            setOnClickListener { onExpand() }
        }
        controlsOverlay.addView(
            expandBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.BOTTOM or Gravity.END),
        )

        addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        val bottomOffsetPx = BOTTOM_NAV_OFFSET_DP.dpToPx(context)
        dragHandler = PIPDragHandler(
            view = this,
            dragEnabled = cfg.dragEnabled,
            getHorizontalEdgeMarginDp = { session.config.horizontalEdgeMarginDp },
            getVerticalEdgeMarginDp = { session.config.verticalEdgeMarginDp },
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
     * Wires mute button for video media. Call after media is attached.
     */
    fun bindVideoControls(mv: PIPMediaView) {
        if (!mv.isVideoType) return
        // Move deeplink to top-left so it doesn't overlap with the mute button at bottom-left
        deeplinkBtn?.let {
            (it.layoutParams as LayoutParams).gravity = Gravity.TOP or Gravity.START
            it.requestLayout()
        }
        muteBtn?.visibility = if (session.config.showMuteButton) View.VISIBLE else View.GONE
        updateMuteIcon(mv.isMuted)
        muteBtn?.setOnClickListener {
            mv.toggleMute()
            updateMuteIcon(mv.isMuted)
            controlsOverlay.resetAutoHideTimer()
        }
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.setImageResource(PIPIcons.muteIcon(muted))
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

    /** Hides video-specific controls (mute). Called when video falls back to static image. */
    fun hideVideoControls() {
        muteBtn?.visibility = View.GONE
        muteBtn?.setOnClickListener(null)
    }

    fun detach() = controlsOverlay.detach()

    private companion object {
        const val ICON_SIZE_DP = 30
        const val ELEVATION_DP = 6
        /** Extra bottom offset to clear a typical bottom navigation bar (Material 3 = 80dp). */
        const val BOTTOM_NAV_OFFSET_DP = 80
    }
}
