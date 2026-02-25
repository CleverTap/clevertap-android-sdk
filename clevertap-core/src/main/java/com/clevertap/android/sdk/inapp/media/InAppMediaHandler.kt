package com.clevertap.android.sdk.inapp.media

import android.view.View
import android.widget.RelativeLayout

/**
 * Unified interface for all media types in InApp notification fragments.
 * Each concrete implementation owns its full lifecycle without routing logic.
 */
internal interface InAppMediaHandler {
    fun setup(
        relativeLayout: RelativeLayout?,
        config: InAppMediaConfig,
        clickListener: View.OnClickListener? = null
    )
    fun onStart() {}
    fun onResume() {}
    fun onPause() {}
    fun onStop() {}
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
