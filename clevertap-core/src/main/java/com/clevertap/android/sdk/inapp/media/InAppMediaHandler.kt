package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.RelativeLayout
import androidx.lifecycle.DefaultLifecycleObserver

/**
 * Unified interface for all media types in InApp notification fragments.
 * Each concrete implementation owns its full lifecycle without routing logic.
 *
 * Extends [DefaultLifecycleObserver] so it can be registered on a fragment's lifecycle
 */
internal interface InAppMediaHandler : DefaultLifecycleObserver {
    fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener? = null
    )
    fun cleanup() {}
    fun clear() {}
}

/**
 * No-op handler for InApp notifications without media.
 */
internal object NoOpMediaHandler : InAppMediaHandler {
    override fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener?
    ) = Unit
}
