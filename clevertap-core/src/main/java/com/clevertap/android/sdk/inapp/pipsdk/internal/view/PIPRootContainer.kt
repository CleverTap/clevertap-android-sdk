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
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimationConfig
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPAnimator
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.PIPPositionResolver
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.dpToPx
import com.clevertap.android.sdk.inapp.pipsdk.internal.engine.percentOf
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

    /** Called when media failed to load during fresh show — PIP was never visible.
     *  Wired to silent cleanup in PIPManager (no animation, no onClose). */
    var onShowFailed: (() -> Unit)? = null

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

        val actionHandler: () -> Unit = {
            s.config.callbacks?.onAction()
            // Dismiss PIP after action (same behavior as regular in-apps)
            onDismissRequested?.invoke()
        }

        // Expanded view — GONE until user expands
        val ev = createExpandedView(s, actionHandler)
        // Shared media view — created but not yet initialized
        val mv = PIPMediaView(context)
        mediaView = mv
        // Compact view — invisible until positioned by onMediaReady
        val cv = createCompactView(s, mv, actionHandler)

        // Wire callbacks BEFORE media init so synchronous signals (cached media) are caught
        mv.onVideoFallback = {
            cv.hideVideoControls()
            ev.hideVideoControls()
        }
        mv.onMediaReady = {
            post {
                if (width == 0 || height == 0) return@post
                cv.bindVideoControls(mv)
                if (isReattach && s.isExpanded) {
                    positionAndShow(s, cv, isReattach = true)
                    expandToFull()
                } else {
                    positionAndShow(s, cv, isReattach)
                }
            }
        }
        mv.onAllMediaFailed = {
            post {
                if (isReattach) {
                    // PIP was visible before rotation — dismiss properly with onClose
                    onDismissRequested?.invoke()
                } else {
                    // PIP was never shown — silent cleanup, no animation, no onClose
                    onShowFailed?.invoke()
                }
            }
        }

        // Start media loading — onMediaReady or onAllMediaFailed will fire
        if (isReattach) {
            mv.rebindSurface(s, resourceProvider!!, mediaExecutor!!)
        } else {
            mv.initialize(s.config, s, resourceProvider!!, mediaExecutor!!)
        }
    }

    private fun createExpandedView(s: PIPSession, actionHandler: () -> Unit): PIPExpandedView {
        val ev = PIPExpandedView(
            context,
            showCloseButton = s.config.showCloseButton,
            hasAction = s.config.action != null,
            showExpandCollapseButton = s.config.showExpandCollapseButton,
            showPlayPauseButton = s.config.showPlayPauseButton,
            showMuteButton = s.config.showMuteButton,
            onCollapse = { collapseToCompact() },
            onClose = { onDismissRequested?.invoke() },
            onAction = actionHandler,
        )
        expandedView = ev
        ev.visibility = View.GONE
        addView(ev, LayoutParams(MATCH_PARENT, MATCH_PARENT))
        return ev
    }

    private fun createCompactView(
        s: PIPSession,
        mv: PIPMediaView,
        actionHandler: () -> Unit,
    ): PIPCompactView {
        val cv = PIPCompactView(
            context, mv, s,
            onExpand = { expandToFull() },
            onClose = { onDismissRequested?.invoke() },
            onAction = actionHandler,
            onSnap = {},   // session.currentPosition already updated inside PIPCompactView
        )
        cv.getSafeInsets = { safeInsets }
        compactView = cv
        cv.visibility = View.INVISIBLE
        addView(cv)
        return cv
    }

    /** Animate the active view out, then call [onDone] for final cleanup. */
    fun dismiss(onDone: () -> Unit) {
        val s = session ?: run { onDone(); return }
        val activeView: View = if (isExpanded) expandedView ?: this else compactView ?: this
        PIPAnimator.animateOut(activeView, effectiveAnimConfig(s), onDone)
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
        s.isExpanded = true
        backCallback?.isEnabled = true
        cv.visibility = View.INVISIBLE

        // Make expanded visible first so its dimensions are valid in bindMedia
        ev.alpha = 0f
        ev.visibility = View.VISIBLE

        // Move shared media view from compact → expanded
        cv.removeView(mv)

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
        s.isExpanded = false
        backCallback?.isEnabled = false

        PIPAnimator.animateCollapse(ev) {
            // Move shared media view back to compact (index 0 = beneath controls overlay)
            ev.mediaContainer.removeView(mv)
            cv.addView(mv, 0, LayoutParams(MATCH_PARENT, MATCH_PARENT))
            // Restart GIF animation after reparenting (no-op for video/image)
            mv.onContainerChanged()
            ev.visibility = View.GONE
            cv.visibility = View.VISIBLE
            cv.syncMuteIcon(mv.isMuted)
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
        var pipH = (pipW * s.config.aspectRatioDenominator /
                s.config.aspectRatioNumerator).toInt().coerceAtLeast(1)

        val hMarginPx = s.config.horizontalEdgeMarginPercent.percentOf(width)
        val vMarginPx = s.config.verticalEdgeMarginPercent.percentOf(height)
        val bottomOffsetPx = PIPDimens.BOTTOM_NAV_OFFSET_DP.dpToPx(context)

        // Clamp height to MAX_HEIGHT_PERCENT of container to prevent overflow in landscape
        // with tall aspect ratios (e.g., 9:16 at 35% width). Using a percentage cap rather
        // than full height minus margins ensures the 9-point snap grid still has meaningful
        // vertical differentiation between TOP/CENTER/BOTTOM positions.
        val maxH = (height * MAX_HEIGHT_PERCENT / 100f).toInt()
        if (pipH > maxH) {
            pipH = maxH.coerceAtLeast(1)
            pipW = (pipH * s.config.aspectRatioNumerator /
                    s.config.aspectRatioDenominator).toInt().coerceAtLeast(1)
        }

        // Add border padding AFTER clamping so media area keeps the correct aspect ratio
        val borderPx = if (s.config.borderEnabled && s.config.borderWidthDp > 0
            && s.config.mediaType != PIPMediaType.VIDEO)
            s.config.borderWidthDp.dpToPx(context) else 0
        if (borderPx > 0) {
            val totalPadding = borderPx * 2
            pipW += totalPadding
            pipH += totalPadding
        }

        if (isReattach) {
            // Skip the layout listener: pipW/pipH are the target sizes and MOVE_IN is never
            // played on reattach, so measured dimensions are not needed.
            cv.layoutParams = LayoutParams(pipW, pipH)
            val anchors = PIPPositionResolver.resolveAnchors(
                width, height, pipW, pipH, hMarginPx, vMarginPx, safeInsets, bottomOffsetPx,
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
                    width, height, cv.width, cv.height, hMarginPx, vMarginPx, safeInsets, bottomOffsetPx,
                )
                val anchor = anchors[s.currentPosition] ?: return
                cv.visibility = View.VISIBLE
                PIPAnimator.animateIn(cv, anchor, effectiveAnimConfig(s), width, height) {
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
        val hMarginPx = s.config.horizontalEdgeMarginPercent.percentOf(width)
        val vMarginPx = s.config.verticalEdgeMarginPercent.percentOf(height)
        val bottomOffsetPx = PIPDimens.BOTTOM_NAV_OFFSET_DP.dpToPx(context)
        val anchors = PIPPositionResolver.resolveAnchors(
            width, height, cv.width, cv.height, hMarginPx, vMarginPx, safeInsets, bottomOffsetPx,
        )
        val anchor = anchors[s.currentPosition] ?: return
        cv.x = anchor.x
        cv.y = anchor.y
    }

    /** Returns the animation config, overriding DISSOLVE → INSTANT for video
     *  (SurfaceView ignores parent alpha, so fade animations don't work). */
    private fun effectiveAnimConfig(s: PIPSession): PIPAnimationConfig {
        val cfg = s.config.animationConfig
        return if (mediaView?.isVideoType == true && cfg.type == PIPAnimation.DISSOLVE) {
            cfg.copy(type = PIPAnimation.INSTANT)
        } else {
            cfg
        }
    }

    private companion object {
        /** Max PIP height as percentage of container height. Prevents overflow in landscape
         *  with tall aspect ratios while leaving room for vertical snap positioning. */
        const val MAX_HEIGHT_PERCENT = 40
    }
}
