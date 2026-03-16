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
import android.widget.TextView
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
    private val onSnap: (PIPPosition) -> Unit,
) : FrameLayout(context) {

    internal val controlsOverlay: PIPControlsOverlay
    private val dragHandler: PIPDragHandler

    init {
        // Rounded card appearance with configurable corner radius and border
        val cfg = session.config
        val bg = GradientDrawable().apply {
            cornerRadius = cfg.cornerRadiusDp.dpToPx(context).toFloat()
            setColor(Color.BLACK)// TODO: Check api docs of setColor() for any notes
            if (cfg.border.enabled) {
                setStroke(// TODO: Check api docs of setStroke() for any notes
                    cfg.border.widthDp.dpToPx(context),
                    Color.parseColor(cfg.border.color),
                )
            }
        }
        background = bg
        clipToOutline = true
        elevation = 8.dpToPx(context).toFloat()
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

        val padPx = 10.dpToPx(context)

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
        ) // TODO: can't we move controlsOverlay construction inside it's class itself?

        // Expand button — bottom-right
        val expandBtn = TextView(context).apply {
            text = "\u26F6"    // ⛶ (fullscreen-like square)
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(padPx, padPx, padPx, padPx)
            setOnClickListener { onExpand() }
        }
        controlsOverlay.addView(
            expandBtn,
            LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.END),
        )

        addView(controlsOverlay, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        dragHandler = PIPDragHandler(
            view = this,
            getHorizontalEdgeMarginDp = { session.config.horizontalEdgeMarginDp },
            getVerticalEdgeMarginDp = { session.config.verticalEdgeMarginDp },
            onSnapComplete = { newPos ->
                session.currentPosition = newPos
                onSnap(newPos)
            },
            onTap = { controlsOverlay.showControls() },
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
}
