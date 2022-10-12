package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
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

import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import java.util.ArrayList;

public class CTInAppNativeHalfInterstitialFragment extends CTInAppBaseFullNativeFragment {

    private RelativeLayout relativeLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {

        ArrayList<Button> inAppButtons = new ArrayList<>();
        View inAppView;
        if (inAppNotification.isTablet() && isTablet() || inAppNotification.isLocalInApp()
                && isTabletFromDeviceType(inflater.getContext())) {
            inAppView = inflater.inflate(R.layout.tab_inapp_half_interstitial, container, false);
        } else {
            inAppView = inflater.inflate(R.layout.inapp_half_interstitial, container, false);
        }

        final FrameLayout fl = inAppView.findViewById(R.id.inapp_half_interstitial_frame_layout);

        @SuppressLint("ResourceType") final CloseImageView closeImageView = fl.findViewById(199272);

        relativeLayout = fl.findViewById(R.id.half_interstitial_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        switch (currentOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                relativeLayout.getViewTreeObserver()
                        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout
                                        .getLayoutParams();
                                if (inAppNotification.isTablet() && isTablet()
                                        || inAppNotification.isLocalInApp() &&
                                            isTabletFromDeviceType(inflater.getContext())) {
                                    redrawHalfInterstitialInApp(relativeLayout, layoutParams, closeImageView);
                                } else {
                                    if (isTablet()) {
                                        redrawHalfInterstitialMobileInAppOnTablet(relativeLayout, layoutParams,
                                                closeImageView);
                                    } else {
                                        redrawHalfInterstitialInApp(relativeLayout, layoutParams, closeImageView);
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
                                RelativeLayout relativeLayout1 = fl
                                        .findViewById(R.id.half_interstitial_relative_layout);
                                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1
                                        .getLayoutParams();
                                if (!inAppNotification.isTablet() || !isTablet()) {
                                    if (isTablet()) {
                                        layoutParams.setMargins(getScaledPixels(140), getScaledPixels(100),
                                                getScaledPixels(140), getScaledPixels(100));
                                        layoutParams.height = relativeLayout1.getMeasuredHeight() - getScaledPixels(
                                                130);
                                        layoutParams.width = (int) (layoutParams.height * 1.3f);
                                        layoutParams.gravity = Gravity.CENTER;
                                        relativeLayout1.setLayoutParams(layoutParams);

                                        new Handler().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                int margin = closeImageView.getMeasuredWidth() / 2;
                                                closeImageView.setX(relativeLayout.getRight() - margin);
                                                closeImageView.setY(relativeLayout.getTop() - margin);
                                            }
                                        });
                                    } else {
                                        layoutParams.width = (int) (relativeLayout1.getMeasuredHeight()
                                                * 1.3f);
                                        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                                        relativeLayout1.setLayoutParams(layoutParams);
                                        new Handler().post(new Runnable() {
                                            @Override
                                            public void run() {
                                                int margin = closeImageView.getMeasuredWidth() / 2;
                                                closeImageView.setX(relativeLayout.getRight() - margin);
                                                closeImageView.setY(relativeLayout.getTop() - margin);
                                            }
                                        });
                                    }
                                } else {
                                    layoutParams.width = (int) (relativeLayout1.getMeasuredHeight() * 1.3f);
                                    layoutParams.gravity = Gravity.CENTER;
                                    relativeLayout1.setLayoutParams(layoutParams);
                                    new Handler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            int margin = closeImageView.getMeasuredWidth() / 2;
                                            closeImageView.setX(relativeLayout.getRight() - margin);
                                            closeImageView.setY(relativeLayout.getTop() - margin);
                                        }
                                    });
                                }

                                relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            }
                        });
                break;

        }

        if (inAppNotification.getInAppMediaForOrientation(currentOrientation) != null) {
            if (inAppNotification.getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation))
                    != null) {
                ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                imageView.setImageBitmap(inAppNotification
                        .getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation)));
            }
        }

        LinearLayout linearLayout = relativeLayout.findViewById(R.id.half_interstitial_linear_layout);
        Button mainButton = linearLayout.findViewById(R.id.half_interstitial_button1);
        inAppButtons.add(mainButton);
        Button secondaryButton = linearLayout.findViewById(R.id.half_interstitial_button2);
        inAppButtons.add(secondaryButton);

        TextView textView1 = relativeLayout.findViewById(R.id.half_interstitial_title);
        textView1.setText(inAppNotification.getTitle());
        textView1.setTextColor(Color.parseColor(inAppNotification.getTitleColor()));

        TextView textView2 = relativeLayout.findViewById(R.id.half_interstitial_message);
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

    boolean isTabletFromDeviceType(Context context){
        return DeviceInfo.getDeviceType(context) == DeviceInfo.TABLET;
    }
}
