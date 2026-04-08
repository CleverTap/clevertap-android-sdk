package com.clevertap.android.sdk.inapp.pipsdk.internal.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Fires [onStop] when the observed [LifecycleOwner] reaches the STOPPED state.
 * Used for SAA support — pass `fragment.viewLifecycleOwner` to [com.clevertap.android.sdk.inapp.pipsdk.PIPManager.show].
 */
internal class PIPLifecycleObserver(
    private val onStop: () -> Unit,
) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_STOP) onStop()
    }
}
