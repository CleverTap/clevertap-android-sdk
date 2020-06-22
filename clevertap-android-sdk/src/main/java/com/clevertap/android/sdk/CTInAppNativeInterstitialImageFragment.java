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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

public class CTInAppNativeInterstitialImageFragment extends CTInAppBaseFullFragment {

    private RelativeLayout relativeLayout;
    @SuppressWarnings({"unused"})
    private int layoutHeight = 0;
    private int layoutWidth = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View inAppView;
        if(inAppNotification.isTablet() && isTablet()) {
            inAppView = inflater.inflate(R.layout.tab_inapp_interstitial_image, container, false);
        }else{
            inAppView = inflater.inflate(R.layout.inapp_interstitial_image, container, false);
        }

        final FrameLayout fl  = inAppView.findViewById(R.id.inapp_interstitial_image_frame_layout);
        fl.setBackgroundDrawable(new ColorDrawable(0xBB000000));

        @SuppressLint("ResourceType")
        final CloseImageView closeImageView = fl.findViewById(199272);
        relativeLayout = fl.findViewById(R.id.interstitial_image_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));
        ImageView imageView = relativeLayout.findViewById(R.id.interstitial_image);

        switch (currentOrientation){
            case Configuration.ORIENTATION_PORTRAIT:
                relativeLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final RelativeLayout relativeLayout1 = fl.findViewById(R.id.interstitial_image_relative_layout);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1.getLayoutParams();
                        if (inAppNotification.isTablet() && isTablet()) {
                            int aspectHeight = (int) (relativeLayout1.getMeasuredWidth() * 1.78f);
                            int requiredHeight=fl.getMeasuredHeight()-getScaledPixels(80);

                            if (aspectHeight>requiredHeight)
                            {
                                layoutParams.height=requiredHeight;
                                layoutParams.width= (int) (requiredHeight/1.78f);
                            }
                            else {
                                layoutParams.height=aspectHeight;
                            }

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

                                int aspectHeight = (int) ((relativeLayout1.getMeasuredWidth()-getScaledPixels(200)) * 1.78f);
                                int requiredHeight=fl.getMeasuredHeight()-getScaledPixels(280);

                                if (aspectHeight>requiredHeight)
                                {
                                    layoutParams.height=requiredHeight;
                                    layoutParams.width= (int) (requiredHeight/1.78f);
                                }
                                else {
                                    layoutParams.height=aspectHeight;
                                    layoutParams.width = relativeLayout1.getMeasuredWidth()-getScaledPixels(200);
                                }

                                layoutParams.setMargins(getScaledPixels(140), getScaledPixels(140), getScaledPixels(140), getScaledPixels(140));
                                layoutHeight = layoutParams.height;
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
                                layoutHeight = layoutParams.height = (int) (relativeLayout1.getMeasuredWidth() * 1.78f);
                                relativeLayout1.setLayoutParams(layoutParams);
                                new Handler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int margin = closeImageView.getMeasuredWidth() / 2;
                                        closeImageView.setX(relativeLayout1.getRight() - margin);
                                        closeImageView.setY(relativeLayout1.getTop() - margin);
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
                        final RelativeLayout relativeLayout1 = fl.findViewById(R.id.interstitial_image_relative_layout);
                        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) relativeLayout1.getLayoutParams();
                        if (!inAppNotification.isTablet() || !isTablet()) {
                            if (isTablet()) {

                                int aspectWidth = (int) ((relativeLayout1.getMeasuredHeight()-getScaledPixels(200)) * 1.78f);
                                int requiredWidth=fl.getMeasuredWidth()-getScaledPixels(280);

                                if (aspectWidth>requiredWidth)
                                {
                                    layoutParams.width=requiredWidth;
                                    layoutParams.height= (int) (requiredWidth/1.78f);
                                }
                                else {
                                    layoutParams.width=aspectWidth;
                                    layoutParams.height = relativeLayout1.getMeasuredHeight()-getScaledPixels(200);
                                }

                                layoutParams.setMargins(getScaledPixels(140), getScaledPixels(140), getScaledPixels(140), getScaledPixels(140));
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
                                layoutWidth = layoutParams.width = (int) (relativeLayout1.getMeasuredHeight() * 1.78f);
                                layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
                                relativeLayout1.setLayoutParams(layoutParams);
                                new Handler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        int margin = closeImageView.getMeasuredWidth() / 2;
                                        closeImageView.setX(relativeLayout1.getRight() - margin);
                                        closeImageView.setY(relativeLayout1.getTop() - margin);
                                    }
                                });
                            }
                        } else {

                            int aspectWidth = (int) (relativeLayout1.getMeasuredHeight() * 1.78f);
                            int requiredWidth=fl.getMeasuredWidth()-getScaledPixels(80);

                            if (aspectWidth>requiredWidth)
                            {
                                layoutParams.width=requiredWidth;
                                layoutParams.height= (int) (requiredWidth/1.78f);
                            }
                            else {
                                layoutParams.width=aspectWidth;
                            }

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
                imageView.setImageBitmap(inAppNotification.getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation)));
                imageView.setTag(0);
                imageView.setOnClickListener(new CTInAppNativeButtonClickListener());
            }
        }
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
