package com.clevertap.android.sdk.inapp;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
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

import androidx.activity.ComponentDialog;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.media3.common.util.UnstableApi;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.gif.GifImageView;
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle;
import com.clevertap.android.sdk.video.VideoLibChecker;
import com.clevertap.android.sdk.video.VideoLibraryIntegrated;
import com.clevertap.android.sdk.video.inapps.ExoplayerHandle;
import com.clevertap.android.sdk.video.inapps.Media3Handle;

import java.util.ArrayList;

@UnstableApi public class CTInAppNativeInterstitialFragment extends CTInAppBaseFullNativeFragment {

    private boolean exoPlayerFullscreen = false;

    private ComponentDialog fullScreenDialog;

    private ImageView fullScreenIcon;

    private GifImageView gifImageView;

    private InAppVideoPlayerHandle handle;

    private RelativeLayout relativeLayout;
    private CloseImageView closeImageView;

    private FrameLayout videoFrameLayout;

    private FrameLayout videoFrameInDialog;

    private ViewGroup.LayoutParams imageViewLayoutParams;
    private final OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (exoPlayerFullscreen) {
                closeFullscreenDialog();
                onBackPressedCallback.setEnabled(false);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
            handle = new Media3Handle();
        } else {
            handle = new ExoplayerHandle();
        }
    }

    @Nullable
    @Override
    @SuppressLint("ResourceType")
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View inAppView;
        if (inAppNotification.isTablet() && isTablet()) {
            inAppView = inflater.inflate(R.layout.tab_inapp_interstitial, container, false);
        }
        else {
            inAppView = inflater.inflate(R.layout.inapp_interstitial, container, false);
        }

        // Find views
        FrameLayout fl = inAppView.findViewById(R.id.inapp_interstitial_frame_layout);
        closeImageView = fl.findViewById(199272);
        relativeLayout = fl.findViewById(R.id.interstitial_relative_layout);
        videoFrameLayout = relativeLayout.findViewById(R.id.video_frame);

        // Container backgrounds
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));
        fl.setBackground(new ColorDrawable(0xBB000000));

        // Container size
        resizeContainer(fl, closeImageView);

        // Inapps data binding
        setMediaForInApp();
        setTitleAndMessage();
        setButtons();
        handleCloseButton();

        return inAppView;
    }

    private void handleCloseButton() {
        if (!inAppNotification.isHideCloseButton()) {
            closeImageView.setOnClickListener(null);
            closeImageView.setVisibility(View.GONE);
        } else {
            closeImageView.setVisibility(View.VISIBLE);
            closeImageView.setOnClickListener(v -> {
                didDismiss(null);
                if (gifImageView != null) {
                    gifImageView.clear();
                }
                Activity activity  = getActivity();
                if(activity!=null) activity.finish();
            });
        }
    }

    private void setButtons() {
        ArrayList<Button> buttonViews = new ArrayList<>();
        LinearLayout linearLayout = relativeLayout.findViewById(R.id.interstitial_linear_layout);
        Button mainButton = linearLayout.findViewById(R.id.interstitial_button1);
        buttonViews.add(mainButton);
        Button secondaryButton = linearLayout.findViewById(R.id.interstitial_button2);
        buttonViews.add(secondaryButton);

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
                Button button = buttonViews.get(i);
                setupInAppButton(button, inAppNotificationButton, i);
            }
        }
    }

    private void setTitleAndMessage() {
        TextView textView1 = relativeLayout.findViewById(R.id.interstitial_title);
        textView1.setText(inAppNotification.getTitle());
        textView1.setTextColor(Color.parseColor(inAppNotification.getTitleColor()));

        TextView textView2 = relativeLayout.findViewById(R.id.interstitial_message);
        textView2.setText(inAppNotification.getMessage());
        textView2.setTextColor(Color.parseColor(inAppNotification.getMessageColor()));
    }

    private void setMediaForInApp() {
        if (!inAppNotification.getMediaList().isEmpty()) {
            CTInAppNotificationMedia media = inAppNotification.getMediaList().get(0);
            String contentDescription = media.getContentDescription();
            if (media.isImage()) {
                Bitmap image = resourceProvider().cachedInAppImageV1(media.getMediaUrl());
                if (image != null) {
                    ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                    if (!TextUtils.isEmpty(contentDescription))
                        imageView.setContentDescription(contentDescription);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(image);
                }
            } else if (media.isGIF()) {
                byte[] gifByteArray = resourceProvider().cachedInAppGifV1(media.getMediaUrl());
                if (gifByteArray != null) {
                    gifImageView = relativeLayout.findViewById(R.id.gifImage);
                    if (!TextUtils.isEmpty(contentDescription))
                        gifImageView.setContentDescription(contentDescription);
                    gifImageView.setVisibility(View.VISIBLE);
                    gifImageView.setBytes(gifByteArray);
                    gifImageView.startAnimation();
                }
            } else if (media.isVideo()) {
                initFullScreenIconForStream();
                prepareMedia();
                playMedia();
            } else if (media.isAudio()) {
                initFullScreenIconForStream();
                prepareMedia();
                playMedia();
                disableFullScreenButton();
            }
        }
    }

    private void initFullScreenIconForStream() {
        // inflate full screen icon for video control
        fullScreenIcon = new ImageView(this.context);
        fullScreenIcon.setImageDrawable(ResourcesCompat.getDrawable(this.context.getResources(), R.drawable.ct_ic_fullscreen_expand, null));
        fullScreenIcon.setOnClickListener(v -> {
            if (!exoPlayerFullscreen) {
                onBackPressedCallback.setEnabled(true);
                openFullscreenDialog();
            } else {
                closeFullscreenDialog();
                onBackPressedCallback.setEnabled(false);
            }
        });

        // icon layout params wrt tablet/phone
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int iconSide;
        if (inAppNotification.isTablet() && isTablet()) {
            iconSide = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
        } else {
            iconSide = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics);
        }
        int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
        int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconSide, iconSide);
        layoutParams.gravity = Gravity.END;
        layoutParams.setMargins(0, iconTop, iconRight, 0);
        fullScreenIcon.setLayoutParams(layoutParams);
    }

    private void resizeContainer(
            FrameLayout fl,
            CloseImageView closeImageView
    ) {
        switch (currentOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                relativeLayout.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout
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
                                        redrawInterstitialInApp(relativeLayout, layoutParams, closeImageView);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        if (gifImageView != null) {
            CTInAppNotificationMedia inAppMedia = inAppNotification.getMediaList().get(0);
            gifImageView.setBytes(resourceProvider().cachedInAppGifV1(inAppMedia.getMediaUrl()));
            gifImageView.startAnimation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (inAppNotification.hasStreamMedia()) {
            prepareMedia();
            playMedia();
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
            onBackPressedCallback.setEnabled(false);
        }
        handle.savePosition();
        handle.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gifImageView != null) {
            gifImageView.clear();
        }
        handle.pause();
    }

    @Override
    void cleanup() {
        super.cleanup();
        if (gifImageView != null) {
            gifImageView.clear();
        }
        handle.pause();
    }

    private void disableFullScreenButton() {
        fullScreenIcon.setVisibility(View.GONE);
    }

    private void closeFullscreenDialog() {
        View playerView = handle.videoSurface();

        handle.switchToFullScreen(false);

        fullScreenIcon.setLayoutParams(imageViewLayoutParams);
        videoFrameInDialog.removeAllViews();
        videoFrameLayout.addView(playerView);
        videoFrameLayout.addView(fullScreenIcon);
        exoPlayerFullscreen = false;
        // dismiss full screen dialog
        fullScreenDialog.dismiss();
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(this.context, R.drawable.ct_ic_fullscreen_expand));
    }

    private void openFullscreenDialog() {
        View playerView = handle.videoSurface();

        imageViewLayoutParams = fullScreenIcon.getLayoutParams();
        handle.switchToFullScreen(true);

        // clear views from inapp container
        videoFrameLayout.removeAllViews();

        if (fullScreenDialog == null) {
            // create only once
            // create full screen dialog and show
            fullScreenDialog = new ComponentDialog(this.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            ViewGroup.LayoutParams fullScreenParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            videoFrameInDialog = new FrameLayout(context);
            fullScreenDialog.addContentView(videoFrameInDialog, fullScreenParams);

            FragmentActivity activity = getActivity();
            if (activity != null) {
                fullScreenDialog.getOnBackPressedDispatcher().addCallback(activity,onBackPressedCallback);
            }
        }

        videoFrameInDialog.addView(playerView);
        exoPlayerFullscreen = true;
        fullScreenDialog.show();
    }

    private void playMedia() {
        handle.play();
    }

    private void prepareMedia() {
        handle.initPlayerView(
                context,
                inAppNotification.isTablet() && isTablet()
        );
        addViewsForStreamMedia();

        handle.initExoplayer(
                context,
                inAppNotification.getMediaList().get(0).getMediaUrl()
        );
    }

    private void addViewsForStreamMedia() {
        // make video container visible
        videoFrameLayout.setVisibility(View.VISIBLE);

        // add views to video container
        View videoSurface = handle.videoSurface();

        if (videoFrameLayout.getChildCount() == 0) {
            videoFrameLayout.addView(videoSurface);
            videoFrameLayout.addView(fullScreenIcon);
        } else {
            //noop
            Logger.d("Video views and controls are already added, not re-attaching");
        }
    }
}
