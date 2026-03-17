package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
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
    private val onRedirect: () -> Unit,
    private val onSnap: (PIPPosition) -> Unit,
) : FrameLayout(context) {

    internal val controlsOverlay: PIPControlsOverlay
    private val dragHandler: PIPDragHandler
    private var muteBtn: ImageView? = null

    init {
        // Rounded card appearance with configurable corner radius and border
        val cfg = session.config
        val bg = GradientDrawable().apply {
            cornerRadius = cfg.cornerRadiusDp.dpToPx(context).toFloat()
            setColor(Color.BLACK)
            if (cfg.border.enabled) {
                setStroke(
                    cfg.border.widthDp.dpToPx(context),
                    Color.parseColor(cfg.border.color),
                )
            }
        }
        background = bg
        clipToOutline = true
        elevation = CARD_ELEVATION_DP.dpToPx(context).toFloat()
        // Inset content so the stroke isn't covered by the media view
        if (cfg.border.enabled) {
            val borderPx = cfg.border.widthDp.dpToPx(context)
            setPadding(borderPx, borderPx, borderPx, borderPx)
        }

        // Media fills the view
        addView(mediaView, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Controls overlay (initially hidden)
        controlsOverlay = PIPControlsOverlay(context)
        controlsOverlay.alpha = 0f

        val padPx = ICON_PADDING_DP.dpToPx(context)
        val iconSizePx = ICON_SIZE_DP.dpToPx(context)

        // Deeplink button — top-left (hidden if redirectUrl is null)
        val deeplinkBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_action)
            scaleType = ImageView.ScaleType.FIT_CENTER
            visibility = if (cfg.redirectUrl != null) View.VISIBLE else View.GONE
            setOnClickListener { onRedirect() }
        }
        controlsOverlay.addView(
            deeplinkBtn,
            LayoutParams(iconSizePx, iconSizePx, Gravity.TOP or Gravity.START),
        )

        // Close button — top-right (hidden if showCloseButton = false)
        val closeBtn = TextView(context).apply {
            text = "\u2715"    // ✕
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(padPx, padPx, padPx, padPx)
            visibility = if (cfg.showCloseButton) View.VISIBLE else View.GONE
            setOnClickListener { onClose() }
        }
        controlsOverlay.addView(
            closeBtn,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.TOP or Gravity.END),
        )

        // Mute button — bottom-left (video only; hidden until bindVideoControls)
        val mBtn = ImageView(context).apply {
            setImageResource(R.drawable.ct_ic_volume_off)
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

        dragHandler = PIPDragHandler(
            view = this,
            dragEnabled = cfg.dragEnabled,
            getHorizontalEdgeMarginDp = { session.config.horizontalEdgeMarginDp },
            getVerticalEdgeMarginDp = { session.config.verticalEdgeMarginDp },
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
        muteBtn?.visibility = if (session.config.showMuteButton) View.VISIBLE else View.GONE
        updateMuteIcon(mv.isMuted)
        muteBtn?.setOnClickListener {
            mv.toggleMute()
            updateMuteIcon(mv.isMuted)
            controlsOverlay.resetAutoHideTimer()
        }
    }

    private fun updateMuteIcon(muted: Boolean) {
        muteBtn?.setImageResource(
            if (muted) R.drawable.ct_ic_volume_off else R.drawable.ct_ic_volume_on
        )
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

    private companion object {
        const val ICON_SIZE_DP = 30
        const val ICON_PADDING_DP = 10
        const val CARD_ELEVATION_DP = 8
    }
}
