package com.clevertap.android.sdk.inapp.fragment

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.inapp.media.NoOpMediaHandler
import com.clevertap.android.sdk.inapp.InAppDisplayListener
import java.util.concurrent.atomic.AtomicBoolean
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class CTInAppBasePartialFragment : CTInAppBaseFragment(), InAppDisplayListener {

    private val isCleanedUp = AtomicBoolean()

    override fun onStart() {
        super.onStart()
        if (isOrphan) {
            return
        }
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
        setListener(
            // Application context, not requireContext(): instanceWithConfig may retain the Context
            // for the instance's lifetime, which would leak the Activity.
            CleverTapAPI.instanceWithConfig(requireContext().applicationContext, config)!!.coreState.inAppController
        )
    }

    private fun registerInAppDisplayListener() {
        context?.let {
            CleverTapAPI.instanceWithConfig(it.applicationContext, config)!!
                .coreState.inAppController
                .registerInAppDisplayListener(this)
        }
    }

    private fun unregisterInAppDisplayListener() {
        context?.let {
            CleverTapAPI.instanceWithConfig(it.applicationContext, config)!!
                .coreState.inAppController
                .unregisterInAppDisplayListener()
        }
    }

    override fun hideInApp() {
        didDismiss(null)
    }
}
