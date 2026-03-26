package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import com.clevertap.android.sdk.R

/**
 * Shared icon resource mapping for PIP controls.
 * Single source of truth for mute and play/pause icon states.
 */
internal object PIPIcons {
    fun muteIcon(muted: Boolean): Int =
        if (muted) R.drawable.ct_ic_volume_off_tint else R.drawable.ct_ic_volume_on_tint

    fun playPauseIcon(playing: Boolean): Int =
        if (playing) R.drawable.ct_ic_pause else R.drawable.ct_ic_play
}
