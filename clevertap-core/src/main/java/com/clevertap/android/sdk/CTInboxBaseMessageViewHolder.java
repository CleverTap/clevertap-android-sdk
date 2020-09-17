package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_NEVER;

class CTInboxBaseMessageViewHolder extends RecyclerView.ViewHolder {
    @SuppressWarnings({"unused"})
    RelativeLayout relativeLayout,clickLayout;
    LinearLayout ctaLinearLayout,bodyRelativeLayout;
    FrameLayout frameLayout;
    Context context;
    ImageView mediaImage,squareImage;
    FrameLayout progressBarFrameLayout;
    private ImageView muteIcon;
    RelativeLayout mediaLayout;

    private WeakReference<CTInboxListViewFragment> parentWeakReference;

    private CTInboxMessage message;
    private CTInboxMessageContent firstContentItem;
    private boolean requiresMediaPlayer;

    CTInboxListViewFragment getParent() {
        return parentWeakReference.get();
    }

    CTInboxBaseMessageViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    void configureWithMessage(final CTInboxMessage inboxMessage, final CTInboxListViewFragment parent, final int position) {
        context = parent.getContext();
        parentWeakReference = new WeakReference<>(parent);
        message = inboxMessage;
        firstContentItem = message.getInboxMessageContents().get(0);
        requiresMediaPlayer = firstContentItem.mediaIsAudio() || firstContentItem.mediaIsVideo();
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
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
            return sdf.format(new Date(time*1000L));
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

    private FrameLayout getLayoutForMediaPlayer() {
        return frameLayout;
    }

    int getImageBackgroundColor() {
        return Color.TRANSPARENT;
    }

    boolean needsMediaPlayer () {
        return requiresMediaPlayer;
    }

    boolean shouldAutoPlay() {
        return firstContentItem.mediaIsVideo();
    }

    void playerReady() {
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        frameLayout.setVisibility(View.VISIBLE);
        if (muteIcon != null) {
            muteIcon.setVisibility(View.VISIBLE);
        }
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.GONE);
        }
    }

    void playerRemoved() {
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.GONE);
        }
        if (muteIcon != null) {
            muteIcon.setVisibility(View.GONE);
        }
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        if (frameLayout != null) {
            frameLayout.removeAllViews();
        }
    }

    void playerBuffering(){
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.VISIBLE);
        }
    }
    boolean addMediaPlayer(PlayerView videoSurfaceView) {
        if (!requiresMediaPlayer) {
            return false;
        }
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        if (frameLayout == null) {
            return false;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE); // Gets set visible in playerReady

        final Resources resources = context.getResources();
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        int width;
        int height;
        if(CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(message.getOrientation().equalsIgnoreCase("l")){
                width = Math.round(this.mediaImage.getMeasuredHeight() * 1.76f);
                height = this.mediaImage.getMeasuredHeight();
            }else{
                height = this.squareImage.getMeasuredHeight();
                //noinspection all
                width = height;
            }
        }else {
            width = resources.getDisplayMetrics().widthPixels;
            height = message.getOrientation().equalsIgnoreCase("l") ? Math.round(width * 0.5625f) : width;
        }

        videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width,height));

        frameLayout.addView(videoSurfaceView);
        frameLayout.setBackgroundColor(Color.parseColor(message.getBgColor()));

        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.VISIBLE);
        }

        final SimpleExoPlayer player = (SimpleExoPlayer) videoSurfaceView.getPlayer();
        float currentVolume = player.getVolume();
        if (firstContentItem.mediaIsVideo()) {
            muteIcon = new ImageView(context);
            muteIcon.setVisibility(View.GONE);
            if (currentVolume > 0) {
                muteIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ct_volume_on));
            } else {
                muteIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.ct_volume_off));
            }

            int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
            int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics);
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            layoutParams.gravity = Gravity.END;
            muteIcon.setLayoutParams(layoutParams);
            muteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    float currentVolume = player.getVolume();
                    if (currentVolume > 0) {
                        player.setVolume(0f);
                        muteIcon.setImageDrawable(resources.getDrawable(R.drawable.ct_volume_off));
                    } else if (currentVolume == 0) {
                        player.setVolume(1);
                        muteIcon.setImageDrawable(resources.getDrawable(R.drawable.ct_volume_on));
                    }
                }
            });
            frameLayout.addView(muteIcon);
        }

        videoSurfaceView.requestFocus();
        videoSurfaceView.setShowBuffering(SHOW_BUFFERING_NEVER);
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter.Builder(context).build();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getPackageName()), defaultBandwidthMeter);
        String uriString = firstContentItem.getMedia();
        if (uriString != null) {
            HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(uriString));
            // Prepare the player with the source.
            player.prepare(hlsMediaSource);
            if(firstContentItem.mediaIsAudio()) {
                videoSurfaceView.showController();//show controller for audio as it is not autoplay
                player.setPlayWhenReady(false);
                player.setVolume(1f);
            }else if(firstContentItem.mediaIsVideo()){
                player.setPlayWhenReady(true);
                player.setVolume(currentVolume);
            }
        }
        return true;
    }
}
