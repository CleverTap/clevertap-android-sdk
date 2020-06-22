package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class CTInAppNativeHalfInterstitialFragment extends CTInAppBaseFullNativeFragment {

    private RelativeLayout relativeLayout;
    @SuppressWarnings({"unused"})
    private int layoutHeight = 0;
    private int layoutWidth = 0;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ArrayList<Button> inAppButtons = new ArrayList<>();
        View inAppView;
        if(inAppNotification.isTablet() && isTablet()) {
            inAppView = inflater.inflate(R.layout.tab_inapp_half_interstitial, container, false);
        }else{
            inAppView = inflater.inflate(R.layout.inapp_half_interstitial, container, false);
        }

        final FrameLayout fl  = inAppView.findViewById(R.id.inapp_half_interstitial_frame_layout);

        @SuppressLint("ResourceType") final CloseImageView closeImageView = fl.findViewById(199272);

        relativeLayout = fl.findViewById(R.id.half_interstitial_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        switch (currentOrientation){
            case Configuration.ORIENTATION_PORTRAIT:
                relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        RelativeLayout relativeLayout1 = fl.findViewById(R.id.half_interstitial_relative_layout);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1.getLayoutParams();
                        if (inAppNotification.isTablet() && isTablet()) {
                            layoutHeight = layoutParams.height = (int) (relativeLayout1.getMeasuredWidth() * 1.3f);
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
                            if (isTablet()) {
                                layoutParams.setMargins(getScaledPixels(140), getScaledPixels(140), getScaledPixels(140), getScaledPixels(140));
                                layoutParams.width = relativeLayout1.getMeasuredWidth()-getScaledPixels(210);
                                layoutHeight = layoutParams.height = (int) (layoutParams.width * 1.3f);
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
                                layoutHeight = layoutParams.height = (int) (relativeLayout1.getMeasuredWidth() * 1.3f);
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
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            relativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        RelativeLayout relativeLayout1 = fl.findViewById(R.id.half_interstitial_relative_layout);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1.getLayoutParams();
                        if (!inAppNotification.isTablet() || !isTablet()) {
                            if (isTablet()) {
                                layoutParams.setMargins(getScaledPixels(140), getScaledPixels(100), getScaledPixels(140), getScaledPixels(100));
                                layoutParams.height = relativeLayout1.getMeasuredHeight()-getScaledPixels(130);
                                layoutParams.width = (int) (layoutParams.height *1.3f);
                                layoutParams.gravity=Gravity.CENTER;
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
                                layoutWidth = layoutParams.width = (int) (relativeLayout1.getMeasuredHeight() * 1.3f);
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

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        } else {
                            relativeLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        }
                    }
                });
                break;

        }

        if(inAppNotification.getInAppMediaForOrientation(currentOrientation) != null) {
            if (inAppNotification.getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation)) != null) {
                ImageView imageView = relativeLayout.findViewById(R.id.backgroundImage);
                imageView.setImageBitmap(inAppNotification.getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation)));
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
        if(buttons.size() ==1){
            if(currentOrientation == Configuration.ORIENTATION_LANDSCAPE){
                mainButton.setVisibility(View.GONE);
            }else if(currentOrientation == Configuration.ORIENTATION_PORTRAIT){
                mainButton.setVisibility(View.INVISIBLE);
            }
            setupInAppButton(secondaryButton,buttons.get(0),0);
        }
        else if (!buttons.isEmpty()) {
            for(int i=0; i < buttons.size(); i++) {
                if (i >= 2) continue; // only show 2 buttons
                CTInAppNotificationButton inAppNotificationButton = buttons.get(i);
                Button button = inAppButtons.get(i);
                setupInAppButton(button,inAppNotificationButton,i);
            }
        }

        fl.setBackgroundDrawable(new ColorDrawable(0xBB000000));

        closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                didDismiss(null);
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
}
