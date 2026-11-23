package com.clevertap.android.sdk.inapp.fragment

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inapp.media.NoOpMediaHandler
import com.clevertap.android.sdk.inapp.InAppDisplayListener
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class CTInAppBasePartialFragment : CTInAppBaseFragment(), InAppDisplayListener {

    private val isCleanedUp = AtomicBoolean()

    override fun onStart() {
        super.onStart()
        if (isOrphan) return
        if (isCleanedUp.get()) {
            cleanup()
        }
        registerInAppDisplayListener()
    }

    override fun onStop() {
        super.onStop()
        unregisterInAppDisplayListener()
    }

    override fun cleanup() {
        if (mediaHandler !is NoOpMediaHandler) {
            lifecycle.removeObserver(mediaHandler)
            mediaHandler.cleanup()
        }
        val activity = getActivity()
        if (activity != null && !Utils.isActivityDead(activity)
            && isCleanedUp.compareAndSet(false, true)
        ) {
            val fragmentManager = activity.supportFragmentManager
            val transaction = fragmentManager.beginTransaction()
            try {
                transaction.remove(this).commit()
            } catch (_: IllegalStateException) {
                fragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
            }
        }
    }


    override fun generateListener() {
        // Resolve the live instance by accountId rather than rebuilding from the passed config
        // snapshot (SDK-6254). If it can't be resolved, skip instead of recreating a stale instance.
        // Use context?. (not requireContext()) so a detached fragment is a no-op, not a crash.
        context?.let { ctx ->
            CleverTapAPI.getGlobalInstance(ctx.applicationContext, config.accountId)
                ?.coreState?.inAppController
                ?.let { setListener(it) }
        }
    }

    private fun registerInAppDisplayListener() {
        context?.let { ctx ->
            CleverTapAPI.getGlobalInstance(ctx.applicationContext, config.accountId)
                ?.coreState?.inAppController
                ?.registerInAppDisplayListener(this)
        }
    }

    private fun unregisterInAppDisplayListener() {
        context?.let { ctx ->
            CleverTapAPI.getGlobalInstance(ctx.applicationContext, config.accountId)
                ?.coreState?.inAppController
                ?.unregisterInAppDisplayListener()
        }
    }

    override fun hideInApp() {
        didDismiss(null)
    }
}
