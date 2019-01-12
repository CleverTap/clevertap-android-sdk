package com.clevertap.android.sdk;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class ExoPlayerRecyclerView extends RecyclerView {

    private List<CTInboxMessage> videoInfoList = new ArrayList<>();
    private int videoSurfaceDefaultHeight = 0;
    private int screenDefaultHeight = 0;
    static SimpleExoPlayer player;
    //surface view for playing video
    private PlayerView videoSurfaceView;
    //private ImageView mCoverImage;
    private Context appContext;
    int targetPosition;
    ImageView muteIcon,muteIcon2;
    Fragment fragment;
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
    public ExoPlayerRecyclerView(Context context) {
        super(context);
        initialize(context);
    }

    /**
     * {@inheritDoc}
     *
     * @param context
     * @param attrs
     */
    public ExoPlayerRecyclerView(Context context,
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
    public ExoPlayerRecyclerView(Context context,
                                 AttributeSet attrs,
                                 int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public void setVideoInfoList(List<CTInboxMessage> videoInfoList) {
        this.videoInfoList = videoInfoList;

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

    //play the video in the row
    public void playVideo() {

        int startPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

        if (endPosition - startPosition > 1) {
            endPosition = startPosition + 1;
        }

        if (startPosition < 0 || endPosition < 0) {
            return;
        }

        //int targetPosition;
        if (startPosition != endPosition) {
            int startPositionVideoHeight = getVisibleVideoSurfaceHeight(startPosition);
            int endPositionVideoHeight = getVisibleVideoSurfaceHeight(endPosition);
            targetPosition = startPositionVideoHeight > endPositionVideoHeight ? startPosition : endPosition;
        } else {
            targetPosition = startPosition;
        }

        if (targetPosition < 0 || targetPosition == playPosition) {
            return;
        }
        playPosition = targetPosition;
        if (videoSurfaceView == null) {
            return;
        }
        //videoSurfaceView.setVisibility(INVISIBLE);
        removeVideoView(videoSurfaceView);

        // get target View targetPosition in RecyclerView
        int at = targetPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();

        View child = getChildAt(at);
        if (child == null) {
            return;
        }

        ViewHolder holder = null;

        switch (videoInfoList.get(targetPosition).getType()){
            case IconMessage:
                holder = (CTIconMessageViewHolder) child.getTag();
                break;
            case SimpleMessage:
                holder = (CTSimpleMessageViewHolder) child.getTag();
                break;
        }

        if (holder == null) {
            playPosition = -1;
            return;
        }

        FrameLayout frameLayout = null;
        switch (videoInfoList.get(targetPosition).getType()){
            case IconMessage:
                frameLayout = holder.itemView.findViewById(R.id.icon_message_frame_layout);
                break;
            case SimpleMessage:
                frameLayout = holder.itemView.findViewById(R.id.simple_message_frame_layout);
                break;
        }

        if (videoInfoList.get(targetPosition).getOrientation().equalsIgnoreCase("l")) {
            int width = getResources().getDisplayMetrics().widthPixels;// Get width of the screen
            int height = Math.round(width * 0.5625f);
            videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        } else if (videoInfoList.get(targetPosition).getOrientation().equalsIgnoreCase("p")) {
            int width = getResources().getDisplayMetrics().widthPixels;// Get width of the screen
            videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, width));
            videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }

        if (frameLayout != null) {
            if(videoInfoList.get(targetPosition).getInboxMessageContents().get(0).mediaIsVideo() || videoInfoList.get(targetPosition).getInboxMessageContents().get(0).mediaIsAudio()) {
                frameLayout.addView(videoSurfaceView);
                frameLayout.setVisibility(VISIBLE);
                if(videoInfoList.get(targetPosition).getInboxMessageContents().get(0).mediaIsVideo()) {
                    muteIcon = new ImageView(appContext);
                    muteIcon.setImageDrawable(appContext.getResources().getDrawable(R.drawable.volume_off));//Volume off icon here by default
                    int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                    int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
                    int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                    int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
                    layoutParams.setMargins(0, iconTop, iconRight, 0);
                    layoutParams.gravity = Gravity.END;
                    muteIcon.setLayoutParams(layoutParams);
                    muteIcon.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            float currentVolume = player.getVolume();
                            if (currentVolume > 0) {
                                player.setVolume(0f);
                                muteIcon.setImageDrawable(appContext.getResources().getDrawable(R.drawable.volume_off));//change to volume off icon
                            } else if (currentVolume == 0) {
                                player.setVolume(1);
                                muteIcon.setImageDrawable(appContext.getResources().getDrawable(R.drawable.volume_on));//change to volume on icon
                            }
                        }
                    });
                    frameLayout.addView(muteIcon);
                }
            }
        }
        addedVideo = true;
        rowParent = holder.itemView;
        videoSurfaceView.requestFocus();
        // Bind the player to the view.
        videoSurfaceView.setPlayer(player);

        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.

        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(appContext,
                Util.getUserAgent(appContext, "android_wave_list"), defaultBandwidthMeter);
        // This is the MediaSource representing the media to be played.
        String uriString = videoInfoList.get(targetPosition).getInboxMessageContents().get(0).getMedia();
        if (uriString != null) {
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uriString));
            // Prepare the player with the source.
            player.prepare(hlsMediaSource);
            if(videoInfoList.get(targetPosition).getInboxMessageContents().get(0).mediaIsAudio()) {
                player.setPlayWhenReady(false);
                player.setVolume(1f);
            }else if(videoInfoList.get(targetPosition).getInboxMessageContents().get(0).mediaIsVideo()){
                player.setPlayWhenReady(true);
                player.setVolume(0f);
            }
        }

    }

    private int getVisibleVideoSurfaceHeight(int playPosition) {
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
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        videoSurfaceDefaultHeight = point.x;

        screenDefaultHeight = point.y;
        videoSurfaceView = new PlayerView(appContext);
        videoSurfaceView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(videoTrackSelectionFactory);

        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(appContext, trackSelector);
        // Bind the player to the view.
        videoSurfaceView.setUseController(true);
        //videoSurfaceView.setShowBuffering(true);
        videoSurfaceView.setUseArtwork(true);
        videoSurfaceView.setControllerAutoShow(false);
        videoSurfaceView.setPlayer(player);
        Drawable artwork = context.getResources().getDrawable(R.drawable.ct_audio);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            videoSurfaceView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }else{
            videoSurfaceView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }

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

        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {

            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
                Logger.d("On Detached");
                if (addedVideo && rowParent != null && rowParent.equals(view)) {
                    //removeVideoView(videoSurfaceView);
                    //player.stop(true);
                    //playPosition = -1;
                    //videoSurfaceView.setVisibility(INVISIBLE);
                    stop();
                }

            }
        });
        player.addListener(new Player.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {

                    case Player.STATE_BUFFERING:
                        //   videoSurfaceView.setAlpha(0.5f);
                        break;
                    case Player.STATE_ENDED:
                        player.seekTo(0);
                        break;
                    case Player.STATE_IDLE:

                        break;
                    case Player.STATE_READY:
                        if(videoSurfaceView!=null) {
                            videoSurfaceView.setVisibility(VISIBLE);
                            videoSurfaceView.setAlpha(1);
                        }
                        //mCoverImage.setVisibility(GONE);

                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }

    public void onPausePlayer() {
        if (videoSurfaceView != null) {
            //removeVideoView(videoSurfaceView);
            if(player!=null) {
                player.release();
                player = null;
            }
            videoSurfaceView = null;
        }
    }

    public void onRestartPlayer() {
        if (videoSurfaceView == null) {
            playPosition = -1;
            playVideo();
        }
    }

    /**
     * release content
     */
    public void release() {

        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        rowParent = null;
    }

    public void stop(){
        if(player!=null){
            player.stop();
            playPosition = -1;
        }
    }


}
