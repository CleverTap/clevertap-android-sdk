package com.clevertap.android.sdk;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

public class CTInAppNativeInterstitialFragment extends CTInAppBaseFullNativeFragment {

    private GifImageView gifImageView;
    private PlayerView playerView;
    private SimpleExoPlayer player;
    private Dialog fullScreenDialog;
    private ImageView fullScreenIcon;
    private boolean exoPlayerFullscreen = false;
    private RelativeLayout relativeLayout;
    private ViewGroup.LayoutParams videoFramelayoutParams,playerViewLayoutParams,imageViewLayoutParams;
    private static long mediaPosition = 0;
    private FrameLayout videoFrameLayout;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {


        ArrayList<Button> inAppButtons = new ArrayList<>();
        View inAppView = inflater.inflate(R.layout.inapp_interstitial, container, false);

        FrameLayout fl  = inAppView.findViewById(R.id.inapp_interstitial_frame_layout);

        relativeLayout = fl.findViewById(R.id.interstitial_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        LinearLayout linearLayout = relativeLayout.findViewById(R.id.interstitial_linear_layout);
        Button mainButton = linearLayout.findViewById(R.id.interstitial_button1);
        inAppButtons.add(mainButton);
        Button secondaryButton = linearLayout.findViewById(R.id.interstitial_button2);
        inAppButtons.add(secondaryButton);

        if (inAppNotification.mediaIsImage()) {
            Bitmap image = inAppNotification.getImage();
            if (image != null) {
                ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(inAppNotification.getImage());
            }
        }
        else if (inAppNotification.mediaIsGIF()) {
            if (inAppNotification.getGifByteArray() != null) {
                gifImageView = relativeLayout.findViewById(R.id.gifImage);
                gifImageView.setVisibility(View.VISIBLE);
                gifImageView.setBytes(inAppNotification.getGifByteArray());
                gifImageView.startAnimation();
            }
        }
        else if (inAppNotification.mediaIsVideo()) {
            initFullScreenDialog();
            prepareMedia();
            playMedia();
            fullScreenIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!exoPlayerFullscreen)
                        openFullscreenDialog();
                    else
                        closeFullscreenDialog();
                }
            });
        }
        else if (inAppNotification.mediaIsAudio()) {
            prepareMedia();
            playMedia();
            disableFullScreenButton();
        }


        TextView textView1 = relativeLayout.findViewById(R.id.interstitial_title);
        textView1.setText(inAppNotification.getTitle());
        textView1.setTextColor(Color.parseColor(inAppNotification.getTitleColor()));

        TextView textView2 = relativeLayout.findViewById(R.id.interstitial_message);
        textView2.setText(inAppNotification.getMessage());
        textView2.setTextColor(Color.parseColor(inAppNotification.getMessageColor()));

        ArrayList<CTInAppNotificationButton> buttons = inAppNotification.getButtons();
        if(buttons.size() ==1){
            mainButton.setVisibility(View.INVISIBLE);
            setupInAppButton(secondaryButton,buttons.get(0),inAppNotification,0);
        }
        else if (buttons != null && !buttons.isEmpty()) {
            for(int i=0; i < buttons.size(); i++) {
                if (i >= 2) continue; // only show 2 buttons
                CTInAppNotificationButton inAppNotificationButton = buttons.get(i);
                Button button = inAppButtons.get(i);
                setupInAppButton(button,inAppNotificationButton,inAppNotification,i);
            }
        }

        fl.setBackgroundDrawable(new ColorDrawable(0xBB000000));

       CloseImageView closeImageView = fl.findViewById(199272);

       closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                didDismiss(null);
                if(gifImageView != null)
                    gifImageView.clear();
                getActivity().finish();
            }
        });

       if(!inAppNotification.isHideCloseButton()) {
           closeImageView.setVisibility(View.GONE);
       }
       else {
           closeImageView.setVisibility(View.VISIBLE);
       }

        return inAppView;
    }

    private void playMedia(){
        playerView.requestFocus();
        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);
    }

    private void prepareMedia(){
        videoFrameLayout = relativeLayout.findViewById(R.id.video_frame);
        videoFrameLayout.setVisibility(View.VISIBLE);
        playerView = videoFrameLayout.findViewById(R.id.videoPlayer);
        playerView.setShowBuffering(true);
        playerView.setUseArtwork(true);
        playerView.setControllerAutoShow(false);
        Drawable artwork = getActivity().getBaseContext().getResources().getDrawable(R.drawable.ct_headphones);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            playerView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }else{
            playerView.setDefaultArtwork(Utils.drawableToBitmap(artwork));
        }
        fullScreenIcon = videoFrameLayout.findViewById(R.id.fullScreen);

        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(getActivity().getBaseContext(), trackSelector);
        // 3. Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getActivity().getBaseContext(),
                Util.getUserAgent(getActivity().getBaseContext(), getActivity().getApplication().getPackageName()), (TransferListener<? super DataSource>) bandwidthMeter);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(inAppNotification.getMediaUrl()));
        // 4. Prepare the player with the source.
        player.prepare(hlsMediaSource);
        player.setRepeatMode(1);
        player.seekTo(mediaPosition);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(gifImageView != null){
            gifImageView.setBytes(inAppNotification.getGifByteArray());
            gifImageView.startAnimation();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(gifImageView != null) {
            gifImageView.clear();
        }
        if(player != null){
            player.stop();
            player.release();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if(gifImageView != null) {
            gifImageView.clear();
        }
        if (player != null) {
            mediaPosition = player.getCurrentPosition();
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(player == null && (inAppNotification.mediaIsVideo() || inAppNotification.mediaIsAudio())){
            prepareMedia();
            playMedia();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    void cleanup() {
        super.cleanup();
        if(gifImageView != null) {
            gifImageView.clear();
        }
        if(player != null){
            player.stop();
            player.release();
            player = null;
        }
    }

    private void initFullScreenDialog() {
        fullScreenDialog = new Dialog(getActivity(), android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (exoPlayerFullscreen)
                    closeFullscreenDialog();
                super.onBackPressed();
            }
        };
    }

    private void openFullscreenDialog() {
        imageViewLayoutParams = fullScreenIcon.getLayoutParams();
        playerViewLayoutParams = playerView.getLayoutParams();
        videoFramelayoutParams = videoFrameLayout.getLayoutParams();
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        ((ViewGroup) fullScreenIcon.getParent()).removeView(fullScreenIcon);
        ((ViewGroup) videoFrameLayout.getParent()).removeView(videoFrameLayout);
        fullScreenDialog.addContentView(playerView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        exoPlayerFullscreen = true;
        fullScreenDialog.show();
    }

    private void closeFullscreenDialog() {
        ((ViewGroup) playerView.getParent()).removeView(playerView);
        playerView.setLayoutParams(playerViewLayoutParams);
        ((FrameLayout) videoFrameLayout.findViewById(R.id.video_frame)).addView(playerView);
        fullScreenIcon.setLayoutParams(imageViewLayoutParams);
        ((FrameLayout) videoFrameLayout.findViewById(R.id.video_frame)).addView(fullScreenIcon);
        videoFrameLayout.setLayoutParams(videoFramelayoutParams);
        ((RelativeLayout) relativeLayout.findViewById(R.id.interstitial_relative_layout)).addView(videoFrameLayout);
        exoPlayerFullscreen = false;
        fullScreenDialog.dismiss();
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_fullscreen_expand));
    }

    private void disableFullScreenButton(){
        fullScreenIcon.setVisibility(View.GONE);
    }
}
