package com.clevertap.android.sdk.customviews;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.inbox.CTInboxBaseMessageViewHolder;

import com.clevertap.android.sdk.video.inbox.ExoplayerHandle;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

@RestrictTo(Scope.LIBRARY)
public class MediaPlayerRecyclerView extends RecyclerView {

    private CTInboxBaseMessageViewHolder playingHolder;

    private final ExoplayerHandle handle = new ExoplayerHandle();

    private final Rect rect = new Rect();

    private final OnScrollListener onScrollListener = new OnScrollListener() {
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
    };
    private final OnChildAttachStateChangeListener onChildAttachStateChangeListener = new OnChildAttachStateChangeListener() {
        @Override
        public void onChildViewAttachedToWindow(@NonNull View view) {
        }

        @Override
        public void onChildViewDetachedFromWindow(@NonNull View view) {
            if (playingHolder != null && playingHolder.itemView.equals(view)) {
                stop();
            }
        }
    };;

    /**
     * {@inheritDoc}
     */
    public MediaPlayerRecyclerView(Context context) {
        super(context);
        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public MediaPlayerRecyclerView(
            Context context,
            AttributeSet attrs
    ) {
        super(context, attrs);
        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public MediaPlayerRecyclerView(
            Context context,
            AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public void onPausePlayer() {
        handle.setPlayWhenReady(false);
    }

    public void onRestartPlayer() {
        initialize();
        playVideo();
    }

    public void playVideo() {
        CTInboxBaseMessageViewHolder targetHolder = findBestVisibleMediaHolder();

        // Case 1 : No viewholder has video item in it
        if (targetHolder == null) {
            removeVideoView();
            return;
        }

        // Case 2 : Found viewholder is same with surface and player attached
        if (playingHolder != null && playingHolder.itemView.equals(targetHolder.itemView)) {
            boolean measured = playingHolder.itemView.getGlobalVisibleRect(rect);
            int visibleHeight = measured ? rect.height() : 0;
            boolean play = visibleHeight >= 400;
            if (play && playingHolder.shouldAutoPlay()) {
                handle.setPlayWhenReady(true);
            } else {
                handle.setPlayWhenReady(false);
            }
            return;
        }

        // Case 3: Video has to be played in different view holder so we remove and reattch to correct one
        removeVideoView();
        float currentVolume = handle.playerVolume();
        boolean addedVideo = targetHolder.addMediaPlayer(
                currentVolume,
                new Function0<Float>() {
                    @Override
                    public Float invoke() {
                        handle.handleMute();
                        return handle.playerVolume();
                    }
                },
                new Function3<String, Boolean, Boolean, Void>() {
                    @Override
                    public Void invoke(String uri, Boolean isMediaAudio, Boolean isMediaVideo) {
                        handle.startPlaying(
                                getContext(),
                                uri,
                                isMediaAudio,
                                isMediaVideo
                        );
                        return null;
                    }
                },
                handle.player()
        );
        handle.playMedia();
        if (addedVideo) {
            playingHolder = targetHolder;
        }
    }

    public void stop() {
        /*if (player != null) {
            player.stop();
        }*/
        handle.pause();
        playingHolder = null;
    }

    private CTInboxBaseMessageViewHolder findBestVisibleMediaHolder() {
        CTInboxBaseMessageViewHolder bestHolder = null;

        //noinspection ConstantConditions
        int startPosition = ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();
        int endPosition = ((LinearLayoutManager) getLayoutManager()).findLastVisibleItemPosition();

        int bestHeight = 0;
        for (int i = startPosition; i <= endPosition; i++) {
            int pos = i - startPosition;
            View child = getChildAt(pos);
            if (child == null) {
                continue;
            }
            CTInboxBaseMessageViewHolder holder = (CTInboxBaseMessageViewHolder) child.getTag();
            if (holder != null) {
                if (!holder.needsMediaPlayer()) {
                    continue;
                }
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

    private void initialize() {

        handle.initExoplayer(getContext().getApplicationContext(), bufferingStarted(), playerReady());
        handle.initPlayerView(getContext().getApplicationContext(), artworkAsset());

        recyclerViewListeners();
    }

    private Function0<Void> bufferingStarted() {
        return new Function0<Void>() {
            @Override
            public Void invoke() {
                if (playingHolder != null) {
                    playingHolder.playerBuffering();
                }
                return null;
            }
        };
    }

    private Function0<Void> playerReady() {
        return new Function0<Void>() {
            @Override
            public Void invoke() {
                if (playingHolder != null) {
                    playingHolder.playerReady();
                }
                return null;
            }
        };
    }

    private Function0<Drawable> artworkAsset() {
        return new Function0<Drawable>() {
            @Override
            public Drawable invoke() {
                return ResourcesCompat.getDrawable(getResources(), R.drawable.ct_audio, null);
            }
        };
    }

    private void recyclerViewListeners() {
        removeOnScrollListener(onScrollListener);
        removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
        addOnScrollListener(onScrollListener);
        addOnChildAttachStateChangeListener(onChildAttachStateChangeListener);
    }

    private void removeVideoView() {
        handle.pause();
        if (playingHolder != null) {
            playingHolder.playerRemoved(); // removes all the views from video container
            playingHolder = null;
        }
    }
}
