package com.clevertap.android.sdk.inapp.pipsdk

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.pipsdk.internal.lifecycle.PIPLifecycleObserver
import com.clevertap.android.sdk.inapp.pipsdk.internal.session.PIPSession
import com.clevertap.android.sdk.inapp.pipsdk.internal.view.PIPRootContainer
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Singleton entry point for the PIP SDK.
 *
 * All public methods are safe to call from any thread — they dispatch to the main thread
 * internally. Only one PIP session can be active at a time; calling [show] while a session
 * is already visible replaces it.
 *
 * **Setup (must be called once before [show]):**
 * ```kotlin
 * PIPManager.init(fileResourceProvider)
 * ```
 *
 * **Typical usage (Kotlin):**
 * ```kotlin
 * PIPManager.show(activity, PIPConfig.builder(url, PIPMediaType.IMAGE).build())
 * ```
 *
 * **Typical usage (Java):**
 * ```java
 * PIPManager.show(activity, PIPConfig.builder(url, PIPMediaType.IMAGE).build());
 * ```
 */
internal object PIPManager {

    @Volatile private var session: PIPSession? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var callbacksRegistered = false
    private var applicationRef: Application? = null

    // Rotation re-attach state
    @Volatile private var pendingRotationReattach = false
    @Volatile private var pendingReattachClassName: String? = null

    // Media dependencies — set via init()
    private var resourceProvider: FileResourceProvider? = null
    private var mediaExecutor: ExecutorService? = null

    // ─── Public API ───────────────────────────────────────────────────────────────

    /**
     * Initializes the PIP SDK with required media dependencies.
     * Must be called once before [show].
     *
     * @param resourceProvider The [FileResourceProvider] for media caching/fetching.
     */
    @JvmStatic
    fun init(resourceProvider: FileResourceProvider) {
        this.resourceProvider = resourceProvider
    }


    /**
     * Shows PIP. Replaces any existing PIP session. Safe to call from any thread.
     *
     * @param activity The host Activity. Should be an [androidx.appcompat.app.AppCompatActivity]
     *   for back-press handling to work correctly.
     * @param config   Immutable session configuration built via [PIPConfig.Builder].
     * @param lifecycleOwner Pass [androidx.fragment.app.Fragment.viewLifecycleOwner] for single-
     *   activity apps to auto-dismiss when the Fragment's view is destroyed. Pass null to manage
     *   lifetime via Activity lifecycle (dismiss on Activity stop).
     * @throws IllegalStateException if [init] has not been called.
     */
    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity, config: PIPConfig, lifecycleOwner: LifecycleOwner? = null) {
        runOnMain { showInternal(activity, config, lifecycleOwner) }
    }

    /** Dismisses PIP with the configured exit animation. No-op if not visible. */
    @JvmStatic
    fun dismiss() = runOnMain { dismissInternal() }

    /**
     * Returns true if PIP is currently visible (compact or expanded).
     *
     * Safe to call from any thread. Note: this is a point-in-time snapshot —
     * the session may be dismissed between the check and a subsequent operation.
     */
    @JvmStatic
    fun isVisible(): Boolean = session != null

    /** The last snapped [PIPPosition]. Null when PIP is hidden. Persists across rotation. */
    @JvmStatic
    val currentPosition: PIPPosition? get() = session?.currentPosition

    // ─── Internal implementation ──────────────────────────────────────────────────

    private fun showInternal(activity: Activity, config: PIPConfig, lifecycleOwner: LifecycleOwner?) {
        if (resourceProvider == null)
            throw IllegalStateException("PIPManager.init(resourceProvider) must be called before show()")

        // Silently replace any existing session
        dismissInternal(notifyCallback = false)

        // Clear stale rotation state from a previous session. Scenario: PIP is showing,
        // user rotates (pendingRotationReattach = true), then PIP is dismissed before the
        // new Activity starts. Without this reset, the stale flag would cause a spurious
        // reattach when the next Activity of the same class starts.
        pendingRotationReattach = false
        pendingReattachClassName = null

        val newSession = PIPSession(
            config = config,
            initialPosition = config.initialPosition,
            activity = activity,
        )
        session = newSession

        // Register ActivityLifecycleCallbacks once on the Application process
        if (!callbacksRegistered) {
            activity.application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
            applicationRef = activity.application
            callbacksRegistered = true
        }

        // SAA: observe Fragment's viewLifecycleOwner to auto-dismiss on view destruction.
        // Note: ON_STOP fires both on fragment view destroy and app background. For SAA,
        // dismissing on background is acceptable since the fragment view may not survive.
        if (lifecycleOwner != null) {
            val observer = PIPLifecycleObserver { runOnMain { cleanupSession() } }
            newSession.lifecycleObserver = observer
            lifecycleOwner.lifecycle.addObserver(observer)
        }

        // Attach PIPRootContainer to the Activity's content view
        val container = PIPRootContainer(activity)
        container.onDismissRequested = { runOnMain { dismissInternal() } }
        newSession.pipRootContainer = container
        container.setupBackPressCallback(activity)

        activity.contentView.addView(
            container,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        container.bindSession(
            newSession,
            isReattach = false,
            resourceProvider = resourceProvider,
            mediaExecutor = ensureMediaExecutor(),
        )
    }

    /**
     * Dismisses the active PIP session and releases all resources.
     *
     * @param notifyCallback false when replacing an existing session so that [PIPCallbacks.onClose]
     *   is not fired spuriously during the replace-show flow.
     */
    internal fun dismissInternal(notifyCallback: Boolean = true) {
        val s = session ?: return
        session = null      // Mark not visible immediately; prevents re-entry

        val container = s.pipRootContainer
        val cleanup: () -> Unit = {
            container?.let { c ->
                c.detach(releaseMedia = true)
                (c.parent as? ViewGroup)?.removeView(c)
            }
            s.videoPlayerWrapper?.release()
            s.videoPlayerWrapper = null
            if (notifyCallback) s.config.callbacks?.onClose()
        }

        if (container != null) {
            container.dismiss(cleanup)
        } else {
            cleanup()
        }

        shutdownMediaExecutor()
        unregisterCallbacks()
    }

    // ─── ActivityLifecycleCallbacks ───────────────────────────────────────────────

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity) {
            val s = session ?: return
            if (pendingRotationReattach && activity.javaClass.name == pendingReattachClassName) {
                pendingRotationReattach = false
                pendingReattachClassName = null
                reattachTo(activity, s)
            }
            // Resume video if we auto-paused it on background
            if (s.activityRef.get() == activity && s.pausedByBackground) {
                s.videoPlayerWrapper?.play()
                s.pausedByBackground = false
            }
        }

        override fun onActivityStopped(activity: Activity) {
            val s = session ?: return
            if (s.activityRef.get() != activity) return
            if (activity.isChangingConfigurations) return
            // Pause video playback while backgrounded (saves battery, stops audio).
            // PIP view stays attached — consistent with how regular in-apps survive background.
            if (s.isPlaying && s.videoPlayerWrapper != null) {
                s.videoPlayerWrapper?.softPause()
                s.pausedByBackground = true
            }
        }

        override fun onActivityDestroyed(activity: Activity) {
            val s = session ?: return
            if (s.activityRef.get() != activity) return
            if (activity.isChangingConfigurations) {
                detachForRotation(s, activity)
            } else {
                // Activity is being destroyed (finish() or system kill) — clean up PIP
                if (session != null) cleanupSession()
            }
        }

        override fun onActivityCreated(a: Activity, b: Bundle?) {}
        override fun onActivityResumed(a: Activity) {}
        override fun onActivityPaused(a: Activity) {}
        override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    }

    // ─── Rotation support ─────────────────────────────────────────────────────────

    private fun detachForRotation(s: PIPSession, activity: Activity) {
        // Save playback state; detach Surface (keeps decode buffer for seamless resume)
        s.videoPlayerWrapper?.detachSurface()
        // Remove view hierarchy; video player wrapper stays alive in session
        s.pipRootContainer?.let { container ->
            container.detach()
            (container.parent as? ViewGroup)?.removeView(container)
        }
        s.pipRootContainer = null

        // Schedule re-attach when the next Activity instance of the same class starts
        pendingRotationReattach = true
        pendingReattachClassName = activity.javaClass.name
    }

    private fun reattachTo(activity: Activity, s: PIPSession) {
        s.activityRef = WeakReference(activity)
        val container = PIPRootContainer(activity)
        container.onDismissRequested = { runOnMain { dismissInternal() } }
        container.setupBackPressCallback(activity)
        s.pipRootContainer = container
        activity.contentView.addView(
            container,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        container.bindSession(
            s,
            isReattach = true,
            resourceProvider = resourceProvider,
            mediaExecutor = ensureMediaExecutor(),
        )
    }

    private fun cleanupSession() {
        val s = session ?: return
        session = null
        s.videoPlayerWrapper?.release()
        s.videoPlayerWrapper = null
        s.pipRootContainer?.let { container ->
            container.detach(releaseMedia = true)
            (container.parent as? ViewGroup)?.removeView(container)
        }
        shutdownMediaExecutor()
        unregisterCallbacks()
    }

    private fun unregisterCallbacks() {
        applicationRef?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        applicationRef = null
        callbacksRegistered = false
    }

    // ─── Threading ────────────────────────────────────────────────────────────────

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post(block)
    }

    private fun ensureMediaExecutor(): ExecutorService {
        return mediaExecutor ?: Executors.newSingleThreadExecutor().also { mediaExecutor = it }
    }

    private fun shutdownMediaExecutor() {
        mediaExecutor?.shutdown()
        mediaExecutor = null
    }
}

// ─── Extension ────────────────────────────────────────────────────────────────────

private val Activity.contentView: FrameLayout
    get() = findViewById(android.R.id.content)
