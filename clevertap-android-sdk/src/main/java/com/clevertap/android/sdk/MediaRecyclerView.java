package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;

@SuppressLint("ViewConstructor")
public class MediaRecyclerView extends RecyclerView {
    private int videoSurfaceDefaultHeight = 0;
    private int screenDefaultHeight = 0;
    int targetPosition;
    private boolean muted = true;
    private CTInboxBaseMessageViewHolder currentlyPlayingHolder;

    // Note only inflate programmatically!

    MediaRecyclerView(Context context) {
        super(context);
        initialize();
    }

    private void initialize() {
        // noinspection ConstantConditions
        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        //noinspection SuspiciousNameCombination
        videoSurfaceDefaultHeight = point.x;
        screenDefaultHeight = point.y;

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
    }

    void holderStartedPlaying(CTInboxBaseMessageViewHolder holder) {
        if (currentlyPlayingHolder != null && holder == currentlyPlayingHolder) return;

        if (currentlyPlayingHolder != null) {
            currentlyPlayingHolder.pause();
        }
        currentlyPlayingHolder = holder;
    }

    void holderStoppedPlaying(CTInboxBaseMessageViewHolder holder) {
        if (currentlyPlayingHolder != null && holder == currentlyPlayingHolder)  {
            currentlyPlayingHolder = null;
        }
    }

    @SuppressWarnings({"UnusedParameters"})
    void holderMuteChanged(CTInboxBaseMessageViewHolder holder, boolean muted) {
        this.muted = muted;
    }
    void stop() {
        if (currentlyPlayingHolder != null) {
            currentlyPlayingHolder.pause();
        }
    }
    void playVideo() {
        // noinspection ConstantConditions
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

        // get target View targetPosition in RecyclerView
        int at = targetPosition - ((LinearLayoutManager) getLayoutManager()).findFirstVisibleItemPosition();

        View child = getChildAt(at);
        if (child == null) {
            return;
        }

        CTInboxBaseMessageViewHolder holder = (CTInboxBaseMessageViewHolder) child.getTag();

        if (holder == null) {
            return;
        }

        if (holder == currentlyPlayingHolder) return;

        if (currentlyPlayingHolder != null) {
            currentlyPlayingHolder.pause();
        }
        if (holder.shouldAutoPlay()) {
            holder.play(this.muted);
            currentlyPlayingHolder = holder;
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
}
