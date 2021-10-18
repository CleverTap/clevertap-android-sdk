package com.clevertap.android.sdk.inapp;

import static com.google.android.exoplayer2.ui.PlayerView.SHOW_BUFFERING_WHEN_PLAYING;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.gif.GifImageView;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
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

    private static long mediaPosition = 0;

    private boolean exoPlayerFullscreen = false;

    private Dialog fullScreenDialog;

    private ImageView fullScreenIcon;

    private GifImageView gifImageView;

    private SimpleExoPlayer player;

    private PlayerView playerView;

    private RelativeLayout relativeLayout;

    private FrameLayout videoFrameLayout;

    private ViewGroup.LayoutParams videoFramelayoutParams, playerViewLayoutParams, imageViewLayoutParams;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {

        ArrayList<Button> inAppButtons = new ArrayList<>();

        View inAppView;
        if (inAppNotification.isTablet() && isTablet()) {
            inAppView = inflater.inflate(R.layout.tab_inapp_interstitial, container, false);
        } else {
            inAppView = inflater.inflate(R.layout.inapp_interstitial, container, false);
        }

        final FrameLayout fl = inAppView.findViewById(R.id.inapp_interstitial_frame_layout);

        @SuppressLint("ResourceType") final CloseImageView closeImageView = fl.findViewById(199272);
        relativeLayout = fl.findViewById(R.id.interstitial_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        switch (currentOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                relativeLayout.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                final RelativeLayout relativeLayout1 = fl
                                        .findViewById(R.id.interstitial_relative_layout);
                                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1
                                        .getLayoutParams();
                                if (inAppNotification.isTablet() && isTablet()) {
                                    // tablet layout on tablet
                                    redrawInterstitialTabletInApp(relativeLayout, layoutParams, fl, closeImageView);
                                } else {
                                    // mobile layout
                                    if (isTablet()) {
                                        // mobile layout on tablet
                                        redrawInterstitialMobileInAppOnTablet(relativeLayout, layoutParams, fl,
                                                closeImageView);
                                    } else {
                                        // mobile layout on mobile
                                        redrawInterstitialInApp(relativeLayout1, layoutParams, closeImageView);
                                    }
                                }

                                relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                relativeLayout.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout
                                        .getLayoutParams();
                                if (inAppNotification.isTablet() && isTablet()) {
                                    // tablet layout on tablet
                                    redrawLandscapeInterstitialTabletInApp(relativeLayout, layoutParams, fl,
                                            closeImageView);
                                } else {
                                    // mobile layout
                                    if (isTablet()) {
                                        // mobile layout on tablet
                                        redrawLandscapeInterstitialMobileInAppOnTablet(relativeLayout, layoutParams,
                                                fl, closeImageView);
                                    } else {
                                        // mobile layout on mobile
                                        redrawLandscapeInterstitialInApp(relativeLayout, layoutParams,
                                                closeImageView);
                                    }
                                }

                                relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                break;
        }

        if (!inAppNotification.getMediaList().isEmpty()) {
            if (inAppNotification.getMediaList().get(0).isImage()) {
                Bitmap image = inAppNotification.getImage(inAppNotification.getMediaList().get(0));
                if (image != null) {
                    ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(
                            inAppNotification.getImage(inAppNotification.getMediaList().get(0)));
                }
            } else if (inAppNotification.getMediaList().get(0).isGIF()) {
                if (inAppNotification.getGifByteArray(inAppNotification.getMediaList().get(0)) != null) {
                    gifImageView = relativeLayout.findViewById(R.id.gifImage);
                    gifImageView.setVisibility(View.VISIBLE);
                    gifImageView.setBytes(
                            inAppNotification.getGifByteArray(inAppNotification.getMediaList().get(0)));
                    gifImageView.startAnimation();
                }
            } else if (inAppNotification.getMediaList().get(0).isVideo()) {
                initFullScreenDialog();
                prepareMedia();
                playMedia();
            } else if (inAppNotification.getMediaList().get(0).isAudio()) {
                prepareMedia();
                playMedia();
                disableFullScreenButton();
            }
        }

        LinearLayout linearLayout = relativeLayout.findViewById(R.id.interstitial_linear_layout);
        Button mainButton = linearLayout.findViewById(R.id.interstitial_button1);
        inAppButtons.add(mainButton);
        Button secondaryButton = linearLayout.findViewById(R.id.interstitial_button2);
        inAppButtons.add(secondaryButton);

        TextView textView1 = relativeLayout.findViewById(R.id.interstitial_title);
        textView1.setText(inAppNotification.getTitle());
        textView1.setTextColor(Color.parseColor(inAppNotification.getTitleColor()));

        TextView textView2 = relativeLayout.findViewById(R.id.interstitial_message);
        textView2.setText(inAppNotification.getMessage());
        textView2.setTextColor(Color.parseColor(inAppNotification.getMessageColor()));

        ArrayList<CTInAppNotificationButton> buttons = inAppNotification.getButtons();
        if (buttons.size() == 1) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mainButton.setVisibility(View.GONE);
            } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mainButton.setVisibility(View.INVISIBLE);
            }
            setupInAppButton(secondaryButton, buttons.get(0), 0);
        } else if (!buttons.isEmpty()) {
            for (int i = 0; i < buttons.size(); i++) {
                if (i >= 2) {
                    continue; // only show 2 buttons
                }
                CTInAppNotificationButton inAppNotificationButton = buttons.get(i);
                Button button = inAppButtons.get(i);
                setupInAppButton(button, inAppNotificationButton, i);
            }
        }

        fl.setBackground(new ColorDrawable(0xBB000000));

        closeImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View v) {
                didDismiss(null);
                if (gifImageView != null) {
                    gifImageView.clear();
                }
                getActivity().finish();
            }
        });

        if (!inAppNotification.isHideCloseButton()) {
            closeImageView.setVisibility(View.GONE);
        } else {
            closeImageView.setVisibility(View.VISIBLE);
        }

        return inAppView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (gifImageView != null) {
            gifImageView.setBytes(inAppNotification.getGifByteArray(inAppNotification.getMediaList().get(0)));
            gifImageView.startAnimation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!inAppNotification.getMediaList().isEmpty()) {
            if (player == null && (inAppNotification.getMediaList().get(0).isVideo() || inAppNotification
                    .getMediaList().get(0).isAudio())) {
                prepareMedia();
                playMedia();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (gifImageView != null) {
            gifImageView.clear();
        }
        if (exoPlayerFullscreen) {
            closeFullscreenDialog();
        }
        if (player != null) {
            mediaPosition = player.getCurrentPosition();
            player.stop();
            player.release();
            player = null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gifImageView != null) {
            gifImageView.clear();
        }
        if (player != null) {
            player.stop();
            player.release();
        }

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    void cleanup() {
        super.cleanup();
        if (gifImageView != null) {
            gifImageView.clear();
        }
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
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
        fullScreenIcon.setImageDrawable(
                ContextCompat.getDrawable(this.context, R.drawable.ct_ic_fullscreen_expand));
    }

    private void disableFullScreenButton() {
        fullScreenIcon.setVisibility(View.GONE);
    }

    private void initFullScreenDialog() {
        fullScreenDialog = new Dialog(this.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
            public void onBackPressed() {
                if (exoPlayerFullscreen) {
                    closeFullscreenDialog();
                }
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
        fullScreenDialog.addContentView(playerView,
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        exoPlayerFullscreen = true;
        fullScreenDialog.show();
    }

    private void playMedia() {
        playerView.requestFocus();
        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(player);
        player.setPlayWhenReady(true);
    }

    private void prepareMedia() {
        videoFrameLayout = relativeLayout.findViewById(R.id.video_frame);
        videoFrameLayout.setVisibility(View.VISIBLE);

        playerView = new PlayerView(this.context);
        fullScreenIcon = new ImageView(this.context);
        fullScreenIcon.setImageDrawable(
                ResourcesCompat.getDrawable(this.context.getResources(), R.drawable.ct_ic_fullscreen_expand, null));
        fullScreenIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!exoPlayerFullscreen) {
                    openFullscreenDialog();
                } else {
                    closeFullscreenDialog();
                }
            }
        });
        if (inAppNotification.isTablet() && isTablet()) {

            int playerWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 408, getResources().getDisplayMetrics());
            int playerHeight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 229, getResources().getDisplayMetrics());

            playerView.setLayoutParams(new FrameLayout.LayoutParams(playerWidth, playerHeight));
            int iconWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
            int iconHeight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            layoutParams.gravity = Gravity.END;
            int iconTop = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            int iconRight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            fullScreenIcon.setLayoutParams(layoutParams);
        } else {
            int width = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 240, getResources().getDisplayMetrics());
            int height = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 134, getResources().getDisplayMetrics());

            playerView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
            int iconWidth = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            int iconHeight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            layoutParams.gravity = Gravity.END;
            int iconTop = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
            int iconRight = (int) TypedValue
                    .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            fullScreenIcon.setLayoutParams(layoutParams);
        }
        playerView.setShowBuffering(SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setUseArtwork(true);
        playerView.setControllerAutoShow(false);
        videoFrameLayout.addView(playerView);
        videoFrameLayout.addView(fullScreenIcon);
        Drawable artwork = ResourcesCompat.getDrawable(this.context.getResources(), R.drawable.ct_audio, null);
        playerView.setDefaultArtwork(artwork);

        // 1. Create a default TrackSelector
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(this.context).build();
        ExoTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        TrackSelector trackSelector = new DefaultTrackSelector(this.context,
                videoTrackSelectionFactory);
        // 2. Create the player
        player = new SimpleExoPlayer.Builder(this.context).setTrackSelector(trackSelector).build();
        // 3. Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this.context,
                Util.getUserAgent(this.context, this.context.getApplicationContext().getPackageName()),
                (TransferListener) bandwidthMeter);
        HlsMediaSource hlsMediaSource;
        hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(inAppNotification.getMediaList().get(0).getMediaUrl()));
        // 4. Prepare the player with the source.
        player.prepare(hlsMediaSource);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.seekTo(mediaPosition);
    }
}
