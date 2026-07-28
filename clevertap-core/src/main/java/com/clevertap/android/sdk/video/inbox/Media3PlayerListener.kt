package com.clevertap.android.sdk.video.inbox

import androidx.media3.common.AudioAttributes
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi

/**
 * This class addresses an AbstractMethodError because of the Java 8 feature of default methods in interfaces.
 * Default methods are somewhat not supported if minSDKVersion < 24
 */
open class Media3PlayerListener : Player.Listener {
    override fun onSurfaceSizeChanged(width: Int, height: Int) {}
    override fun onRenderedFirstFrame() {}

    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onCues(cues: MutableList<Cue>) {}
    override fun onCues(cueGroup: CueGroup) {}
    @UnstableApi override fun onMetadata(metadata: Metadata) {}
    override fun onEvents(player: Player, events: Player.Events) {}
    override fun onTimelineChanged(timeline: Timeline, reason: Int) {}
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}
    override fun onTracksChanged(tracks: Tracks) {}
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {}
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {}
    override fun onIsLoadingChanged(isLoading: Boolean) {}
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onLoadingChanged(isLoading: Boolean) {}

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {}
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {}
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}

    override fun onPlaybackStateChanged(playbackState: Int) {}

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {}
    override fun onIsPlayingChanged(isPlaying: Boolean) {}
    override fun onRepeatModeChanged(repeatMode: Int) {}
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
    override fun onPlayerError(error: PlaybackException) {}
    override fun onPlayerErrorChanged(error: PlaybackException?) {}
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onPositionDiscontinuity(reason: Int) {}

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {}
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {}
    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {}
    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {}
    @UnstableApi override fun onAudioSessionIdChanged(audioSessionId: Int) {}
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {}
    override fun onVolumeChanged(volume: Float) {}
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {}
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {}
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {}
    override fun onVideoSizeChanged(videoSize: VideoSize) {}
}
