package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Utils
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class CTInAppBasePartialFragment : CTInAppBaseFragment() {

    private val isCleanedUp = AtomicBoolean()

    override fun onStart() {
        super.onStart()
        if (isCleanedUp.get()) {
            cleanup()
        }
    }

    override fun cleanup() {
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
            CleverTapAPI.instanceWithConfig(requireContext(), config).coreState.inAppController
        )
    }
}
