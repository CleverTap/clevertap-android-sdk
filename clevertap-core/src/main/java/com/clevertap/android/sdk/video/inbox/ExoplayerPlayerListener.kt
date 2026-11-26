package com.clevertap.android.sdk.video.inbox

import com.google.android.exoplayer2.DeviceInfo
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.CueGroup
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.video.VideoSize
import androidx.annotation.RestrictTo

/**
 * This class addresses an AbstractMethodError because of the Java 8 feature of default methods in interfaces.
 * Default methods are somewhat not supported if minSDKVersion < 24
 *
 * @deprecated ExoPlayer (com.google.android.exoplayer2) support in CleverTap is deprecated and will be removed
 * in v9.0.0. Migrate to Media3 (androidx.media3) and use [Media3PlayerListener] instead.
 */
@Deprecated(
    message = "ExoPlayer (com.google.android.exoplayer2) support in CleverTap is deprecated and will be removed in " +
        "v9.0.0. Migrate to Media3 (androidx.media3) and use Media3PlayerListener instead.",
    replaceWith = ReplaceWith("Media3PlayerListener()")
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class ExoplayerPlayerListener : Player.Listener {
    override fun onEvents(player: Player, events: Player.Events) { /* No-op default (see class KDoc) */ }
    override fun onTimelineChanged(timeline: Timeline, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onTracksChanged(tracks: Tracks) { /* No-op default (see class KDoc) */ }
    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { /* No-op default (see class KDoc) */ }
    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) { /* No-op default (see class KDoc) */ }
    override fun onIsLoadingChanged(isLoading: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onAvailableCommandsChanged(availableCommands: Player.Commands) { /* No-op default (see class KDoc) */ }
    override fun onTrackSelectionParametersChanged(parameters: TrackSelectionParameters) {
        // No-op default (see class KDoc)
    }
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        // No-op default (see class KDoc)
    }
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { /* No-op default (see class KDoc) */ }
    override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
        // No-op default (see class KDoc)
    }
    override fun onIsPlayingChanged(isPlaying: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onRepeatModeChanged(repeatMode: Int) { /* No-op default (see class KDoc) */ }
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onPlayerError(error: PlaybackException) { /* No-op default (see class KDoc) */ }
    override fun onPlayerErrorChanged(error: PlaybackException?) { /* No-op default (see class KDoc) */ }
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
    override fun onAudioSessionIdChanged(audioSessionId: Int) { /* No-op default (see class KDoc) */ }
    override fun onAudioAttributesChanged(audioAttributes: AudioAttributes) { /* No-op default (see class KDoc) */ }
    override fun onVolumeChanged(volume: Float) { /* No-op default (see class KDoc) */ }
    override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) { /* No-op default (see class KDoc) */ }
    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) { /* No-op default (see class KDoc) */ }
    override fun onVideoSizeChanged(videoSize: VideoSize) { /* No-op default (see class KDoc) */ }
    override fun onSurfaceSizeChanged(width: Int, height: Int) { /* No-op default (see class KDoc) */ }
    override fun onRenderedFirstFrame() { /* No-op default (see class KDoc) */ }
    override fun onCues(cues: MutableList<Cue>) { /* No-op default (see class KDoc) */ }
    override fun onCues(cueGroup: CueGroup) { /* No-op default (see class KDoc) */ }
    override fun onMetadata(metadata: Metadata) { /* No-op default (see class KDoc) */ }
}
