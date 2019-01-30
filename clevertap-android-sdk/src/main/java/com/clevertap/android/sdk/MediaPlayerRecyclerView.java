package com.clevertap.android.sdk;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
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
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

public class MediaPlayerRecyclerView extends RecyclerView {

    private int videoSurfaceDefaultHeight = 0;
    private int screenDefaultHeight = 0;
    SimpleExoPlayer player;
    //surface view for playing video
    private PlayerView videoSurfaceView;
    private Context appContext;
    int targetPosition;

    /**
     * the position of playing video
     */
    private int playPosition = -1;

    private boolean addedVideo = false;
    private View rowParent;

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

    /**
     * prepare for video play
     */
    //remove the player from the row
    private void removeVideoView(PlayerView videoView) {
        ViewGroup parent = (ViewGroup) videoView.getParent();
        if (parent == null) {
            return;
        }
        int index = parent.indexOfChild(videoView);
        if (index >= 0) {
            parent.removeViewAt(index);
            addedVideo = false;
        }
    }

    public void playVideo() {
        //noinspection ConstantConditions
        int startPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

        if (endPosition - startPosition > 1) {
            endPosition = startPosition + 1;
        }
        if (startPosition < 0 || endPosition < 0) {
            return;
        }

        if (startPosition != endPosition) {
            int startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition);
            int endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition);
            targetPosition = startPositionVideoHeight > endPositionVideoHeight ? startPosition : endPosition;
        } else {
            targetPosition = startPosition;
        }

        //noinspection ConstantConditions
        if (targetPosition < 0 || targetPosition == playPosition) {
            return;
        }
        playPosition = targetPosition;
        if (videoSurfaceView == null) {
            return;
        }

        removeVideoView(videoSurfaceView);

        // get target View targetPosition in RecyclerView
        int at = targetPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();

        View child = getChildAt(at);
        if (child == null) {
            return;
        }

        CTInboxBaseMessageViewHolder holder = (CTInboxBaseMessageViewHolder) child.getTag();

        if (holder == null) {
            playPosition = -1;
            return;
        }

        if (!holder.needsMediaPlayer()) {
            return;
        }

        addedVideo = holder.addMediaPlayer(videoSurfaceView);
        if (addedVideo) {
            rowParent = holder.itemView;
        }
    }

    private int getVisibleVideoSurfaceHeight(int playPosition) {
        //noinspection ConstantConditions
        int at = playPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        View child = getChildAt(at);
        if (child == null) {
            return 0;
        }
        int[] location01 = new int[2];
        child.getLocationInWindow(location01);
        if (location01[1] < 0) {
            return location01[1] + videoSurfaceDefaultHeight;
        } else {
            return screenDefaultHeight - location01[1];
        }
    }

    private void initialize(Context context) {
        appContext = context.getApplicationContext();
        //noinspection ConstantConditions
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        //noinspection SuspiciousNameCombination
        videoSurfaceDefaultHeight = point.x;

        screenDefaultHeight = point.y;
        videoSurfaceView = new PlayerView(appContext);
        videoSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);

        videoSurfaceView.setUseArtwork(true);
        Drawable artwork = context.getResources().getDrawable(R.drawable.ct_audio);
        videoSurfaceView.setDefaultArtwork(Utils.drawableToBitmap(artwork));

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(appContext, trackSelector);
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
                if (addedVideo && rowParent != null && rowParent.equals(view)) {
                    stop();
                }
            }
        });
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {

                    case Player.STATE_BUFFERING:
                        break;
                    case Player.STATE_ENDED:
                        player.seekTo(0);
                        break;
                    case Player.STATE_IDLE:
                        break;
                    case Player.STATE_READY:
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

    public void onPausePlayer() {
        if (videoSurfaceView != null) {
            stop();
            videoSurfaceView = null;
        }
    }

    public void stop(){
        if (player != null){
            player.stop();
            playPosition = -1;
        }
    }

    public void onRestartPlayer() {
        if (videoSurfaceView == null) {
            initialize(appContext);
            playPosition = -1;
            playVideo();
        }
    }

    public void release() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        rowParent = null;
    }
}
