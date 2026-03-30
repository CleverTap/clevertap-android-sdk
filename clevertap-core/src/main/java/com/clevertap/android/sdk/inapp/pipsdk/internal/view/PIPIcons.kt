package com.clevertap.android.sdk.inapp.pipsdk.internal.view

import com.clevertap.android.sdk.R

/**
 * Shared icon resource and content description mapping for PIP controls.
 * Single source of truth for mute and play/pause icon states + accessibility labels.
 */
internal object PIPIcons {
    fun muteIcon(muted: Boolean): Int =
        if (muted) R.drawable.ct_ic_volume_off_tint else R.drawable.ct_ic_volume_on_tint

    fun muteContentDescription(muted: Boolean): Int =
        if (muted) R.string.ct_unmute_button_content_description else R.string.ct_mute_button_content_description

    fun playPauseIcon(playing: Boolean): Int =
        if (playing) R.drawable.ct_ic_pause else R.drawable.ct_ic_play

    fun playPauseContentDescription(playing: Boolean): Int =
        if (playing) R.string.ct_pip_pause_button_content_description else R.string.ct_pip_play_button_content_description
}
