package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.text.SimpleDateFormat;
import java.util.Date;

class CTInboxBaseMessageViewHolder extends RecyclerView.ViewHolder {

    private PlayerView videoSurfaceView;
    @SuppressWarnings({"unused", "WeakerAccess"})
    RelativeLayout relativeLayout,clickLayout,bodyRelativeLayout;
    LinearLayout ctaLinearLayout;
    FrameLayout frameLayout;
    private boolean hasVideo;
    private float currentVolume;
    private ImageView muteIcon;
    private Context context;

    CTInboxBaseMessageViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    void configureWithMessage(final CTInboxMessage inboxMessage, final CTInboxListViewFragment parent, final int position) {
        hasVideo = false;
    }

    private void setMute(boolean mute) {
        if (this.videoSurfaceView != null && this.videoSurfaceView.getPlayer() != null) {
            SimpleExoPlayer player = (SimpleExoPlayer) this.videoSurfaceView.getPlayer();
            float currentVolume = player.getVolume();
            boolean currentlyMuted = currentVolume <= 0.0F;
            boolean updateIcon = false;
            if (mute && !currentlyMuted) {
                player.setVolume(0f);
                this.currentVolume = currentVolume;
                updateIcon = true;
            } else if (!mute && currentlyMuted) {
                float volume = this.currentVolume > 0 ? this.currentVolume : 1;
                player.setVolume(volume);
                updateIcon = true;
            }
            if (updateIcon && muteIcon != null) {
                int imageId = mute ? R.drawable.volume_off : R.drawable.volume_on;
                // noinspection ConstantConditions
                muteIcon.setImageDrawable(context.getResources().getDrawable(imageId));
            }
        }
    }

    void play(boolean muted) {
        if (this.videoSurfaceView != null && this.videoSurfaceView.getPlayer() != null) {
            setMute(muted);
            this.videoSurfaceView.getPlayer().setPlayWhenReady(true);
        }
    }

    void pause() {
        if (this.videoSurfaceView != null && this.videoSurfaceView.getPlayer() != null) {
            this.videoSurfaceView.getPlayer().setPlayWhenReady(false);
        }
    }

    boolean shouldAutoPlay() {
        return hasVideo;
    }

    /**
     * Logic for timestamp
     * @param time Epoch date of creation
     * @return String timestamp
     */
    String calculateDisplayTimestamp(long time){
        long now = System.currentTimeMillis()/1000;
        long diff = now-time;
        if(diff < 60){
            return "Just Now";
        }else if(diff > 60 && diff < 59*60){
            return (diff/(60)) + " mins ago";
        }else if(diff > 59*60 && diff < 23*59*60 ){
            return diff/(60*60) > 1 ? diff/(60*60) + " hours ago" : diff/(60*60) + " hour ago";
        }else if(diff > 24*60*60 && diff < 48*60*60){
            return "Yesterday";
        }else {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
            return sdf.format(new Date(time));
        }
    }

    void hideTwoButtons(Button mainButton, Button secondaryButton, Button tertiaryButton){
        secondaryButton.setVisibility(View.GONE);
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,6);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

    void hideOneButton(Button mainButton, Button secondaryButton, Button tertiaryButton){
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

    private SimpleExoPlayer createAndConfigurePlayer(Context context, String mediaUrl, final CTInboxListViewFragment parent) {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        final SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        //noinspection unchecked
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getPackageName()), (TransferListener<? super DataSource>) bandwidthMeter);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(mediaUrl));
        player.prepare(hlsMediaSource);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.setPlayWhenReady(false);

        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}

            @Override
            public void onLoadingChanged(boolean isLoading) {}

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {

                    case Player.STATE_BUFFERING:
                        break;
                    case Player.STATE_ENDED:
                        player.seekTo(0);
                        notifyStopped(parent);
                        break;
                    case Player.STATE_IDLE:
                        notifyStopped(parent);
                        break;
                    case Player.STATE_READY:
                        notifyPlaying(parent);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {}

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {}

            @Override
            public void onPlayerError(ExoPlaybackException error) {}

            @Override
            public void onPositionDiscontinuity(int reason) {}

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {}

            @Override
            public void onSeekProcessed() {}
        });
        return player;
    }

    private void notifyPlaying(CTInboxListViewFragment parent) {
        parent.mediaRecyclerView.holderStartedPlaying(this);
    }

    private void notifyStopped(CTInboxListViewFragment parent) {
        parent.mediaRecyclerView.holderStoppedPlaying(this);
    }

    private void notifyMuteChanged(CTInboxListViewFragment parent, boolean muted) {
        parent.mediaRecyclerView.holderMuteChanged(this, muted);
    }

    void addMediaPlayerView(CTInboxMessage inboxMessage, final CTInboxListViewFragment parent){
        context = parent.getActivity();
        videoSurfaceView = new PlayerView(context);
        videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT));
        videoSurfaceView.setUseArtwork(true);
        videoSurfaceView.setControllerAutoShow(false);
        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        String mediaUrl = inboxMessage.getInboxMessageContents().get(0).getMedia();
        final SimpleExoPlayer player = createAndConfigurePlayer(context, mediaUrl, parent);
        videoSurfaceView.requestFocus();
        videoSurfaceView.setVisibility(View.VISIBLE);
        videoSurfaceView.setPlayer(player);
        videoSurfaceView.setUseArtwork(true);
        // noinspection ConstantConditions
        Drawable artwork = context.getResources().getDrawable(R.drawable.ct_audio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            videoSurfaceView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }else{
            videoSurfaceView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }
        if (inboxMessage.getOrientation().equalsIgnoreCase("l")) {
            int width = context.getResources().getDisplayMetrics().widthPixels;// Get width of the screen
            int height = Math.round(width * 0.5625f);
            videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        } else if (inboxMessage.getOrientation().equalsIgnoreCase("p")) {
            // noinspection ConstantConditions
            int width = context.getResources().getDisplayMetrics().widthPixels;// Get width of the screen
            //noinspection SuspiciousNameCombination
            videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, width));
            videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }

        frameLayout.addView(videoSurfaceView);
        frameLayout.setVisibility(View.VISIBLE);

        CTInboxMessageContent content = inboxMessage.getInboxMessageContents().get(0);
        hasVideo = content.mediaIsVideo();

        if(content.mediaIsVideo()) {
            muteIcon = new ImageView(context);
            muteIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.volume_off));
            int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());
            int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, context.getResources().getDisplayMetrics());
            int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            layoutParams.gravity = Gravity.END;
            muteIcon.setLayoutParams(layoutParams);
            muteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean mute = player.getVolume() > 0;
                    setMute(mute);
                    notifyMuteChanged(parent, mute);
                }
            });
            frameLayout.addView(muteIcon);
        }
    }

    void removeVideoView() {
        frameLayout.setVisibility(View.GONE);
        if (videoSurfaceView != null) {
            ViewGroup parent = (ViewGroup) videoSurfaceView.getParent();
            if (parent != null) {
                int index = parent.indexOfChild(videoSurfaceView);
                if (index >= 0) {
                    parent.removeViewAt(index);
                }
            }
            Player player = videoSurfaceView.getPlayer();
            if (player != null) {
                player.release();
                videoSurfaceView.setPlayer(null);
            }
            videoSurfaceView = null;
        }

    }
}
