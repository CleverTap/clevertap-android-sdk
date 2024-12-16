package com.clevertap.android.sdk.video.inbox

import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.CueGroup
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.video.VideoSize

/**
 * This class addresses an AbstractMethodError because of the Java 8 feature of default methods in interfaces.
 * Default methods are somewhat not supported if minSDKVersion < 24
 */
open class ExoplayerPlayerListener : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) {}
    override fun onTimelineChanged(timeline: Timeline, reason: Int) {}
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {}
    override fun onTracksChanged(tracks: Tracks) {}
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {}
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {}
    override fun onIsLoadingChanged(isLoading: Boolean) {}
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {}
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {}
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
    override fun onPlaybackStateChanged(playbackState: Int) {}
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {}
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {}
    override fun onIsPlayingChanged(isPlaying: Boolean) {}
    override fun onRepeatModeChanged(repeatMode: Int) {}
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
    override fun onPlayerError(error: PlaybackException) {}
    override fun onPlayerErrorChanged(error: PlaybackException?) {}
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
    override fun onAudioSessionIdChanged(audioSessionId: Int) {}
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) {}
    override fun onVolumeChanged(volume: Float) {}
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {}
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {}
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {}
    override fun onVideoSizeChanged(videoSize: VideoSize) {}
    override fun onSurfaceSizeChanged(width: Int, height: Int) {}
    override fun onRenderedFirstFrame() {}
    override fun onCues(cues: MutableList<Cue>) {}
    override fun onCues(cueGroup: CueGroup) {}
    override fun onMetadata(metadata: Metadata) {}
}
