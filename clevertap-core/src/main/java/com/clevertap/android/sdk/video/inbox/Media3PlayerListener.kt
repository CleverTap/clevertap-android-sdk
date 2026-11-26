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
import androidx.annotation.RestrictTo

/**
 * This class addresses an AbstractMethodError because of the Java 8 feature of default methods in interfaces.
 * Default methods are somewhat not supported if minSDKVersion < 24
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class Media3PlayerListener : Player.Listener {
    override fun onSurfaceSizeChanged(width: Int, height: Int) { /* No-op default (see class KDoc) */ }
    override fun onRenderedFirstFrame() { /* No-op default (see class KDoc) */ }

    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onCues(cues: MutableList<Cue>) { /* No-op default (see class KDoc) */ }
    override fun onCues(cueGroup: CueGroup) { /* No-op default (see class KDoc) */ }
    @UnstableApi override fun onMetadata(metadata: Metadata) { /* No-op default (see class KDoc) */ }
    override fun onEvents(player: Player, events: Player.Events) { /* No-op default (see class KDoc) */ }
    override fun onTimelineChanged(timeline: Timeline, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onTracksChanged(tracks: Tracks) { /* No-op default (see class KDoc) */ }
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { /* No-op default (see class KDoc) */ }
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) { /* No-op default (see class KDoc) */ }
    override fun onIsLoadingChanged(isLoading: Boolean) { /* No-op default (see class KDoc) */ }
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onLoadingChanged(isLoading: Boolean) { /* No-op default (see class KDoc) */ }

    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) { /* No-op default (see class KDoc) */ }
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        // No-op default (see class KDoc)
    }
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        // No-op default (see class KDoc)
    }

    override fun onPlaybackStateChanged(playbackState: Int) { /* No-op default (see class KDoc) */ }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        // No-op default (see class KDoc)
    }
    override fun onIsPlayingChanged(isPlaying: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onRepeatModeChanged(repeatMode: Int) { /* No-op default (see class KDoc) */ }
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onPlayerError(error: PlaybackException) { /* No-op default (see class KDoc) */ }
    override fun onPlayerErrorChanged(error: PlaybackException?) { /* No-op default (see class KDoc) */ }
    @UnstableApi @Deprecated("Deprecated in Java")
    override fun onPositionDiscontinuity(reason: Int) { /* No-op default (see class KDoc) */ }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) { /* No-op default (see class KDoc) */ }
    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        // No-op default (see class KDoc)
    }
    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) { /* No-op default (see class KDoc) */ }
    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) { /* No-op default (see class KDoc) */ }
    override fun onMaxSeekToPreviousPositionChanged(maxSeekToPreviousPositionMs: Long) {
        // No-op default (see class KDoc)
    }
    @UnstableApi override fun onAudioSessionIdChanged(audioSessionId: Int) { /* No-op default (see class KDoc) */ }
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) { /* No-op default (see class KDoc) */ }
    override fun onVolumeChanged(volume: Float) { /* No-op default (see class KDoc) */ }
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) { /* No-op default (see class KDoc) */ }
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onVideoSizeChanged(videoSize: VideoSize) { /* No-op default (see class KDoc) */ }
}
