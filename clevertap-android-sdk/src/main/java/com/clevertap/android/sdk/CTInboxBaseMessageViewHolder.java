package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
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

import com.bumptech.glide.Glide;
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

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

class CTInboxBaseMessageViewHolder extends RecyclerView.ViewHolder {

    private PlayerView videoSurfaceView;
    @SuppressWarnings({"unused", "WeakerAccess"})
    RelativeLayout relativeLayout,clickLayout,bodyRelativeLayout;
    LinearLayout ctaLinearLayout;
    FrameLayout frameLayout;
    Context context;
    ImageView mediaImage,squareImage;

    private Player.EventListener playerListener;

    private WeakReference<CTInboxListViewFragment> parentWeakReference;

    CTInboxListViewFragment getParent() {
        return parentWeakReference.get();
    }

    CTInboxBaseMessageViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    void configureWithMessage(final CTInboxMessage inboxMessage, final CTInboxListViewFragment parent, final int position) {
        context = parent.getContext();
        parentWeakReference = new WeakReference<>(parent);
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

    private void cleanUpVideoView() {
        if (videoSurfaceView != null) {
            Player player = videoSurfaceView.getPlayer();
            if (player != null) {
                if (playerListener != null) {
                    player.removeListener(playerListener);
                    playerListener = null;
                }
                player.release();
                videoSurfaceView.setPlayer(null);
            }
            ViewGroup parent = (ViewGroup) videoSurfaceView.getParent();
            if (parent != null) {
                int index = parent.indexOfChild(videoSurfaceView);
                if (index >= 0) {
                    parent.removeViewAt(index);
                }
            }
            videoSurfaceView = null;
        }
    }

    void removeVideoView() {
        frameLayout.setVisibility(View.GONE);
        cleanUpVideoView();
    }

    int getThumbnailImage(String image){
        if (context != null) {
            return context.getResources().getIdentifier(image,"drawable",context.getPackageName());
        }else{
            return -1;
        }
    }
}
