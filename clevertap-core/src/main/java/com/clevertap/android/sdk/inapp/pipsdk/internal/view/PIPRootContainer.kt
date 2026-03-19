package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import android.app.Activity
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPAnimator
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPPositionResolver
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import java.util.concurrent.ExecutorService

/**
 * Full-screen transparent overlay added to [android.R.id.content].
 *
 * Hosts both [PIPCompactView] and [PIPExpandedView]. The shared [PIPMediaView] is
 * physically moved between them during expand/collapse transitions.
 *
 * Touch events pass through to the app in compact mode because [onInterceptTouchEvent]
 * returns false and this ViewGroup's [onTouchEvent] is not overridden (default returns
 * false for ACTION_DOWN when no child consumed it, letting siblings receive the event).
 */
internal class PIPRootContainer(context: Context) : FrameLayout(context) {

    /** Called by internal close/dismiss actions — wired to PIPManager.dismissInternal. */
    var onDismissRequested: (() -> Unit)? = null

    var isExpanded = false
        private set

    private var session: PIPSession? = null
    private var compactView: PIPCompactView? = null
    private var expandedView: PIPExpandedView? = null
    private var mediaView: PIPMediaView? = null
    private var backCallback: OnBackPressedCallback? = null
    private var layoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var safeInsets: Insets = Insets.NONE

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            val newInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            if (newInsets != safeInsets) {
                safeInsets = newInsets
                repositionCompactIfNeeded()
            }
            windowInsets // pass through — do NOT consume
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)
    }

    // ─── Public API called by PIPManager ─────────────────────────────────────────

    fun bindSession(
        s: PIPSession,
        isReattach: Boolean = false,
        resourceProvider: FileResourceProvider? = null,
        mediaExecutor: ExecutorService? = null,
    ) {
        session = s

        val redirectAction: () -> Unit = {
            s.config.redirectUrl?.let { url -> s.config.callbacks?.onRedirect(url) }
        }

        // Expanded view — GONE until user expands
        val ev = PIPExpandedView(
            context,
            showCloseButton = s.config.showCloseButton,
            redirectUrl = s.config.redirectUrl,
            showExpandCollapseButton = s.config.showExpandCollapseButton,
            showPlayPauseButton = s.config.showPlayPauseButton,
            showMuteButton = s.config.showMuteButton,
            onCollapse = { collapseToCompact() },
            onClose = { onDismissRequested?.invoke() },
            onRedirect = redirectAction,
        )
        expandedView = ev
        ev.visibility = View.GONE
        addView(ev, LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Shared media view — initialized or rebound post-rotation
        val mv = PIPMediaView(context)
        mediaView = mv
        if (isReattach) {
            mv.rebindSurface(s, resourceProvider!!, mediaExecutor!!)
        } else {
            mv.initialize(s.config, s, resourceProvider!!, mediaExecutor!!)
        }

        // When video falls back to a static image, hide video-specific controls (mute, play/pause)
        // so the user doesn't see non-functional buttons over a static fallback image.
        mv.onVideoFallback = {
            compactView?.hideVideoControls()
            expandedView?.hideVideoControls()
        }

        // Compact view — invisible until positioned
        val cv = PIPCompactView(
            context, mv, s,
            onExpand = { expandToFull() },
            onClose = { onDismissRequested?.invoke() },
            onRedirect = redirectAction,
            onSnap = {},   // session.currentPosition already updated inside PIPCompactView
        )
        cv.bindVideoControls(mv)
        cv.getSafeInsets = { safeInsets }
        compactView = cv
        cv.visibility = View.INVISIBLE
        addView(cv)

        // Defer positioning until after the container's first layout pass
        post {
            if (width == 0 || height == 0) return@post
            positionAndShow(s, cv, isReattach)
        }
    }

    /** Animate the active view out, then call [onDone] for final cleanup. */
    fun dismiss(onDone: () -> Unit) {
        val s = session ?: run { onDone(); return }
        val activeView: View = if (isExpanded) expandedView ?: this else compactView ?: this
        PIPAnimator.animateOut(activeView, s.config.animation, onDone)
    }

    /**
     * Cancel timers and unregister back-press callback.
     *
     * @param releaseMedia true on final dismiss to release renderer resources (GIF bytes,
     *   image bitmaps). Must be false on rotation — video ExoPlayer lives in PIPSession
     *   and must survive for reattach.
     */
    fun detach(releaseMedia: Boolean = false) {
        // Safety net: remove layout listener if it hasn't fired yet (e.g., rapid dismiss before first layout)
        layoutListener?.let {
            compactView?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            layoutListener = null
        }
        compactView?.detach()
        expandedView?.detach()
        if (releaseMedia) mediaView?.release()
        backCallback?.remove()
    }

    fun setupBackPressCallback(activity: Activity) {
        val componentActivity = activity as? ComponentActivity ?: return
        val cb = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() { collapseToCompact() }
        }
        backCallback = cb
        componentActivity.onBackPressedDispatcher.addCallback(cb)
    }

    // ─── State transitions ────────────────────────────────────────────────────────

    private fun expandToFull() {
        val s = session ?: return
        val cv = compactView ?: return
        val ev = expandedView ?: return
        val mv = mediaView ?: return

        isExpanded = true
        backCallback?.isEnabled = true
        cv.visibility = View.INVISIBLE

        // Make expanded visible first so its dimensions are valid in bindMedia
        ev.alpha = 0f
        ev.visibility = View.VISIBLE

        // Move shared media view from compact → expanded
        (mv.parent as? ViewGroup)?.removeView(mv)

        ev.bindMedia(mv, s) {
            // Restart GIF animation after reparenting (no-op for video/image)
            mv.onContainerChanged()
            // onReady fires from post{} after layout — media container is correctly sized
            PIPAnimator.animateExpand(ev, ev.mediaContainer) {
                s.config.callbacks?.onExpand()
            }
        }
    }

    private fun collapseToCompact() {
        val s = session ?: return
        val cv = compactView ?: return
        val ev = expandedView ?: return
        val mv = mediaView ?: return

        isExpanded = false
        backCallback?.isEnabled = false

        PIPAnimator.animateCollapse(ev) {
            // Move shared media view back to compact (index 0 = beneath controls overlay)
            (mv.parent as? ViewGroup)?.removeView(mv)
            cv.addView(mv, 0, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            // Restart GIF animation after reparenting (no-op for video/image)
            mv.onContainerChanged()
            ev.visibility = View.GONE
            cv.visibility = View.VISIBLE
            s.config.callbacks?.onCollapse()
        }
    }

    // ─── Touch pass-through ───────────────────────────────────────────────────────

    /** Always return false: children handle their own areas; outside them the event
     *  falls through to the app's layout sibling in the content FrameLayout. */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = false

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private fun positionAndShow(s: PIPSession, cv: PIPCompactView, isReattach: Boolean) {
        var pipW = (width * s.config.widthPercent / 100f).toInt().coerceAtLeast(1)
        var pipH = (pipW.toLong() * s.config.aspectRatioDenominator /
                s.config.aspectRatioNumerator).toInt().coerceAtLeast(1)
        val hMarginPx = s.config.horizontalEdgeMarginDp.dpToPx(context)
        val vMarginPx = s.config.verticalEdgeMarginDp.dpToPx(context)

        // Clamp height to MAX_HEIGHT_PERCENT of container to prevent overflow in landscape
        // with tall aspect ratios (e.g., 9:16 at 35% width). Using a percentage cap rather
        // than full height minus margins ensures the 9-point snap grid still has meaningful
        // vertical differentiation between TOP/CENTER/BOTTOM positions.
        val maxH = (height * MAX_HEIGHT_PERCENT / 100f).toInt()
        if (pipH > maxH) {
            pipH = maxH.coerceAtLeast(1)
            pipW = (pipH.toLong() * s.config.aspectRatioNumerator /
                    s.config.aspectRatioDenominator).toInt().coerceAtLeast(1)
        }

        if (isReattach) {
            // Skip the layout listener: pipW/pipH are the target sizes and MOVE_IN is never
            // played on reattach, so measured dimensions are not needed.
            cv.layoutParams = LayoutParams(pipW, pipH)
            val anchors = PIPPositionResolver.resolveAnchors(
                width, height, pipW, pipH, hMarginPx, vMarginPx, safeInsets,
            )
            val anchor = anchors[s.currentPosition] ?: return
            cv.x = anchor.x
            cv.y = anchor.y
            cv.visibility = View.VISIBLE   // show immediately; scrim in PIPMediaView covers black flash
            return
        }

        // Fresh show: wait for layout pass so cv.width/height are valid for MOVE_IN animation.
        cv.layoutParams = LayoutParams(pipW, pipH)
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                cv.viewTreeObserver.removeOnGlobalLayoutListener(this)
                layoutListener = null
                val anchors = PIPPositionResolver.resolveAnchors(
                    width, height, cv.width, cv.height, hMarginPx, vMarginPx, safeInsets,
                )
                val anchor = anchors[s.currentPosition] ?: return
                cv.visibility = View.VISIBLE
                PIPAnimator.animateIn(cv, anchor, s.config.animation, width, height) {
                    s.config.callbacks?.onShow()
                }
            }
        }
        layoutListener = listener
        cv.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun repositionCompactIfNeeded() {
        val s = session ?: return
        val cv = compactView ?: return
        if (cv.visibility != View.VISIBLE) return
        if (width == 0 || height == 0) return
        val hMarginPx = s.config.horizontalEdgeMarginDp.dpToPx(context)
        val vMarginPx = s.config.verticalEdgeMarginDp.dpToPx(context)
        val anchors = PIPPositionResolver.resolveAnchors(
            width, height, cv.width, cv.height, hMarginPx, vMarginPx, safeInsets,
        )
        val anchor = anchors[s.currentPosition] ?: return
        cv.x = anchor.x
        cv.y = anchor.y
    }

    private companion object {
        /** Max PIP height as percentage of container height. Prevents overflow in landscape
         *  with tall aspect ratios while leaving room for vertical snap positioning. */
        const val MAX_HEIGHT_PERCENT = 60
    }
}
