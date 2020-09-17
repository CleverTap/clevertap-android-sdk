package com.clevertap.android.sdk;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;

public class MediaPlayerRecyclerView extends RecyclerView {

    SimpleExoPlayer player;
    //surface view for playing video
    private PlayerView videoSurfaceView;
    private Context appContext;
    private CTInboxBaseMessageViewHolder playingHolder;

    /**
     * {@inheritDoc}
     *
     * @param context
     */
    public MediaPlayerRecyclerView(Context context) {
        super(context);
        initialize(context);
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param attrs
     */
    public MediaPlayerRecyclerView(Context context,
                                   AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public MediaPlayerRecyclerView(Context context,
                                   AttributeSet attrs,
                                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        appContext = context.getApplicationContext();
        videoSurfaceView = new PlayerView(appContext);
        videoSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        if(CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
        }else{
            videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }
        videoSurfaceView.setUseArtwork(true);
        Drawable artwork = context.getResources().getDrawable(R.drawable.ct_audio);
        videoSurfaceView.setDefaultArtwork(artwork);

        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory();
        TrackSelector trackSelector =
                new DefaultTrackSelector(appContext,videoTrackSelectionFactory);

        player = new SimpleExoPlayer.Builder(context).setTrackSelector(trackSelector).build();
        player.setVolume(0f); // start off muted
        videoSurfaceView.setUseController(true);
        videoSurfaceView.setControllerAutoShow(false);
        videoSurfaceView.setPlayer(player);

        addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    playVideo();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {}
            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                if (playingHolder != null && playingHolder.itemView.equals(view)) {
                    stop();
                }
            }
        });
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        if(playingHolder != null){
                            playingHolder.playerBuffering();
                        }
                        break;
                    case Player.STATE_ENDED:
                        if (player != null) {
                            player.seekTo(0);
                            player.setPlayWhenReady(false);
                            if (videoSurfaceView != null) {
                                videoSurfaceView.showController();
                            }
                        }
                        break;
                    case Player.STATE_IDLE:
                        break;
                    case Player.STATE_READY:
                        if (playingHolder != null) {
                            playingHolder.playerReady();
                        }
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {}
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {}
            @Override
            public void onLoadingChanged(boolean isLoading) {}
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
    }

    private CTInboxBaseMessageViewHolder findBestVisibleMediaHolder() {
        CTInboxBaseMessageViewHolder bestHolder = null;

        //noinspection ConstantConditions
        int startPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

        int bestHeight = 0;
        for (int i=startPosition; i<=endPosition; i++) {
            int pos = i - startPosition;
            View child = getChildAt(pos);
            if (child == null) {
                continue;
            }
            CTInboxBaseMessageViewHolder holder = (CTInboxBaseMessageViewHolder) child.getTag();
            if(holder != null) {
                if (!holder.needsMediaPlayer()) {
                    continue;
                }
                Rect rect = new Rect();
                boolean measured = holder.itemView.getGlobalVisibleRect(rect);
                int height = measured ? rect.height() : 0;
                if (height > bestHeight) {
                    bestHeight = height;
                    bestHolder = holder;
                }
            }
        }
        return bestHolder;
    }
    public void playVideo() {
        if (videoSurfaceView == null) {
            return;
        }
        CTInboxBaseMessageViewHolder targetHolder = findBestVisibleMediaHolder();
        if(targetHolder == null) {
            stop();
            removeVideoView();
            return;
        }

        if (playingHolder != null && playingHolder.itemView.equals(targetHolder.itemView)) {
            Rect rect = new Rect();
            boolean measured = playingHolder.itemView.getGlobalVisibleRect(rect);
            int visibleHeight = measured ? rect.height() : 0;
            if (player != null) {
                boolean play = visibleHeight >= 400;
                if (play) {
                    if (playingHolder.shouldAutoPlay()) {
                        player.setPlayWhenReady(true);
                    }
                } else {
                    player.setPlayWhenReady(false);
                }

            }
            return;
        }

        removeVideoView();
        boolean addedVideo = targetHolder.addMediaPlayer(videoSurfaceView);
        if (addedVideo) {
            playingHolder = targetHolder;
        }
    }

    private void removeVideoView() {
        if (videoSurfaceView == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) videoSurfaceView.getParent();
        if (parent == null) {
            return;
        }
        int index = parent.indexOfChild(videoSurfaceView);
        if (index >= 0) {
            parent.removeViewAt(index);
            if (player != null) {
                player.stop();
            }
            if (playingHolder != null) {
                playingHolder.playerRemoved();
                playingHolder = null;
            }
        }
    }

    public void onPausePlayer() {
        if (player != null){
            player.setPlayWhenReady(false);
        }
    }
    @SuppressWarnings({"unused"})
    public void removePlayer() {
        if (videoSurfaceView != null) {
            removeVideoView();
            videoSurfaceView = null;
        }
    }

    public void stop(){
        if (player != null){
            player.stop();
        }
        playingHolder = null;
    }

    public void onRestartPlayer() {
        if (videoSurfaceView == null) {
            initialize(appContext);
            playVideo();
        }
    }

    public void release() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        playingHolder = null;
        videoSurfaceView = null;
    }
}
