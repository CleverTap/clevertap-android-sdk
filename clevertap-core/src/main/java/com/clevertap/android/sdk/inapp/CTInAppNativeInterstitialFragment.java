package com.clevertap.android.sdk.inapp;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.gif.GifImageView;
import com.clevertap.android.sdk.video.ExoplayerHandle;
import java.util.ArrayList;

public class CTInAppNativeInterstitialFragment extends CTInAppBaseFullNativeFragment {

    private boolean exoPlayerFullscreen = false;

    private Dialog fullScreenDialog;

    private ImageView fullScreenIcon;

    private GifImageView gifImageView;

    private ExoplayerHandle handle = new ExoplayerHandle();

    private RelativeLayout relativeLayout;

    private FrameLayout videoFrameLayout;

    private ViewGroup.LayoutParams imageViewLayoutParams;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ArrayList<Button> inAppButtons = new ArrayList<>();

        View inAppView;
        if (inAppNotification.isTablet() && isTablet()) {
            inAppView = inflater.inflate(R.layout.tab_inapp_interstitial, container, false);
        }
        else {
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
            CTInAppNotificationMedia media = inAppNotification.getMediaList().get(0);
            if (media.isImage()) {
                Bitmap image = resourceProvider().cachedImage(media.getMediaUrl());
                if (image != null) {
                    ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(image);
                }
            } else if (media.isGIF()) {
                byte[] gifByteArray = resourceProvider().cachedGif(media.getMediaUrl());
                if (gifByteArray != null) {
                    gifImageView = relativeLayout.findViewById(R.id.gifImage);
                    gifImageView.setVisibility(View.VISIBLE);
                    gifImageView.setBytes(gifByteArray);
                    gifImageView.startAnimation();
                }
            } else if (media.isVideo()) {
                prepareMedia();
                playMedia();
            } else if (media.isAudio()) {
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

        closeImageView.setOnClickListener(v -> {
            didDismiss(null);
            if (gifImageView != null) {
                gifImageView.clear();
            }
            Activity activity  = getActivity();
            if(activity!=null) activity.finish();
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
            CTInAppNotificationMedia inAppMedia = inAppNotification.getMediaList().get(0);
            gifImageView.setBytes(resourceProvider().cachedGif(inAppMedia.getMediaUrl()));
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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
        View playerView = handle.player();

        handle.switchToFullScreen(false);

        fullScreenIcon.setLayoutParams(imageViewLayoutParams);
        videoFrameLayout.addView(playerView);
        videoFrameLayout.addView(fullScreenIcon);
        exoPlayerFullscreen = false;

        // dismiss full screen dialog
        fullScreenDialog.dismiss();
        fullScreenIcon.setImageDrawable(ContextCompat.getDrawable(this.context, R.drawable.ct_ic_fullscreen_expand));
    }

    private void openFullscreenDialog() {
        View playerView = handle.player();

        imageViewLayoutParams = fullScreenIcon.getLayoutParams();
        handle.switchToFullScreen(true);

        // clear views from container
        videoFrameLayout.removeView(playerView);
        videoFrameLayout.removeView(fullScreenIcon);

        if (fullScreenDialog == null) {
            // create full screen dialog and show
            fullScreenDialog = new Dialog(this.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
                public void onBackPressed() {
                    if (exoPlayerFullscreen) {
                        closeFullscreenDialog();
                    }
                    super.onBackPressed();
                }
            };
        }

        ViewGroup.LayoutParams fullScreenParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        fullScreenDialog.addContentView(playerView, fullScreenParams);
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
        streamContainer();

        handle.initExoplayer(
                context,
                inAppNotification.getMediaList().get(0).getMediaUrl()
        );
    }

    private void streamContainer() {
        // make video container visible
        videoFrameLayout = relativeLayout.findViewById(R.id.video_frame);
        videoFrameLayout.setVisibility(View.VISIBLE);

        // inflate full screen icon
        fullScreenIcon = new ImageView(this.context);
        fullScreenIcon.setImageDrawable(ResourcesCompat.getDrawable(this.context.getResources(), R.drawable.ct_ic_fullscreen_expand, null));
        fullScreenIcon.setOnClickListener(v -> {
            if (!exoPlayerFullscreen) {
                openFullscreenDialog();
            } else {
                closeFullscreenDialog();
            }
        });

        // icon layout params wrt tablet/phone
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (inAppNotification.isTablet() && isTablet()) {
            int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            layoutParams.gravity = Gravity.END;
            int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
            int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics);
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            fullScreenIcon.setLayoutParams(layoutParams);
        } else {
            int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics);
            int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, displayMetrics);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            layoutParams.gravity = Gravity.END;
            int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
            int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics);
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            fullScreenIcon.setLayoutParams(layoutParams);
        }

        // add views to video container
        videoFrameLayout.addView(handle.player());
        videoFrameLayout.addView(fullScreenIcon);
    }
}
