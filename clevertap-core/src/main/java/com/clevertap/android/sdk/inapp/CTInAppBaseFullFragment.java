package com.clevertap.android.sdk.inapp;

import android.os.Handler;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import com.clevertap.android.sdk.InAppNotificationActivity;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.utils.Utils;

public abstract class CTInAppBaseFullFragment extends CTInAppBaseFragment {

    void addCloseImageView(final RelativeLayout relativeLayout, final CloseImageView closeImageView) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int margin = closeImageView.getMeasuredWidth() / 2;
                closeImageView.setX(relativeLayout.getRight() - margin);
                closeImageView.setY(relativeLayout.getTop() - margin);
            }
        });
    }

    @Override
    void cleanup() {/* no-op */}

    @Override
    void generateListener() {
        if (context instanceof InAppNotificationActivity) {
            setListener((InAppListener) context);
        }
    }

    /**
     * Checks if a devices is a tablet or a handset based on smallest width qualifier which specifies the smallest of
     * the screen's two sides, regardless of the device's current orientation.<br>
     * for example,<br> 600dp: a 7” tablet (600x1024 mdpi)
     * <br>480dp: a large phone screen ~5" (480x800 mdpi)
     *
     * Adopting this method to determine if a device is tablet over manually calculating diagonal of device due to
     * some OEM issues. <a href="https://github.com/CleverTap/clevertap-android-sdk/issues/116">#116</a>
     *
     * @return true if device screen's smallest width, independent of orientation is >= 600dp else false
     */
    boolean isTablet() {
        if (Utils.isActivityDead(getActivity())) {
            return false;
        }

        try {
            return getResources().getBoolean(R.bool.ctIsTablet);
        } catch (Exception e) {
            // resource not found
            Logger.d("Failed to decide whether device is a smart phone or tablet!");
            e.printStackTrace();
            return false;
        }
    }

    void redrawHalfInterstitialInApp(
            final RelativeLayout relativeLayout, LayoutParams layoutParams, CloseImageView closeImageView) {
        layoutParams.height = (int) (relativeLayout.getMeasuredWidth()
                * 1.3f);
        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawHalfInterstitialMobileInAppOnTablet(RelativeLayout relativeLayout, LayoutParams layoutParams,
            CloseImageView closeImageView) {
        layoutParams.setMargins(getScaledPixels(140), getScaledPixels(140),
                getScaledPixels(140), getScaledPixels(140));
        layoutParams.width = relativeLayout.getMeasuredWidth() - getScaledPixels(
                210);
        layoutParams.height = (int) (layoutParams.width * 1.3f);
        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawInterstitialInApp(final RelativeLayout relativeLayout, LayoutParams layoutParams,
            CloseImageView closeImageView) {
        layoutParams.height = (int) (relativeLayout.getMeasuredWidth()
                * 1.78f);
        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawInterstitialMobileInAppOnTablet(final RelativeLayout relativeLayout,
            LayoutParams layoutParams, FrameLayout fl, CloseImageView closeImageView) {
        int aspectHeight = (int) (
                (relativeLayout.getMeasuredWidth() - getScaledPixels(200)) * 1.78f);
        int requiredHeight = fl.getMeasuredHeight() - getScaledPixels(280);

        if (aspectHeight > requiredHeight) {
            layoutParams.height = requiredHeight;
            layoutParams.width = (int) (requiredHeight / 1.78f);
        } else {
            layoutParams.height = aspectHeight;
            layoutParams.width = relativeLayout.getMeasuredWidth() - getScaledPixels(
                    200);
        }

        layoutParams.setMargins(getScaledPixels(140), getScaledPixels(140),
                getScaledPixels(140), getScaledPixels(140));

        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawInterstitialTabletInApp(final RelativeLayout relativeLayout,
            LayoutParams layoutParams, FrameLayout fl, CloseImageView closeImageView) {
        int aspectHeight = (int) (relativeLayout.getMeasuredWidth() * 1.78f);
        int requiredHeight = fl.getMeasuredHeight() - getScaledPixels(80);

        if (aspectHeight > requiredHeight) {
            layoutParams.height = requiredHeight;
            layoutParams.width = (int) (requiredHeight / 1.78f);
        } else {
            layoutParams.height = aspectHeight;
        }

        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawLandscapeInterstitialInApp(final RelativeLayout relativeLayout, LayoutParams layoutParams,
            CloseImageView closeImageView) {
        layoutParams.width = (int) (relativeLayout.getMeasuredHeight()
                * 1.78f);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawLandscapeInterstitialMobileInAppOnTablet(final RelativeLayout relativeLayout,
            LayoutParams layoutParams, FrameLayout fl, CloseImageView closeImageView) {
        int aspectWidth = (int) (
                (relativeLayout.getMeasuredHeight() - getScaledPixels(120)) * 1.78f);
        int requiredWidth = fl.getMeasuredWidth() - getScaledPixels(280);

        if (aspectWidth > requiredWidth) {
            layoutParams.width = requiredWidth;
            layoutParams.height = (int) (requiredWidth / 1.78f);
        } else {
            layoutParams.width = aspectWidth;
            layoutParams.height = relativeLayout.getMeasuredHeight()
                    - getScaledPixels(120);
        }

        layoutParams.setMargins(getScaledPixels(140), getScaledPixels(100),
                getScaledPixels(140), getScaledPixels(100));
        layoutParams.gravity = Gravity.CENTER;
        relativeLayout.setLayoutParams(layoutParams);

        addCloseImageView(relativeLayout, closeImageView);
    }

    void redrawLandscapeInterstitialTabletInApp(final RelativeLayout relativeLayout,
            LayoutParams layoutParams, FrameLayout fl, CloseImageView closeImageView) {
        int aspectWidth = (int) (relativeLayout.getMeasuredHeight() * 1.78f);
        int requiredWidth = fl.getMeasuredWidth() - getScaledPixels(80);

        if (aspectWidth > requiredWidth) {
            layoutParams.width = requiredWidth;
            layoutParams.height = (int) (requiredWidth / 1.78f);
        } else {
            layoutParams.width = aspectWidth;
        }

        layoutParams.gravity = Gravity.CENTER;
        relativeLayout.setLayoutParams(layoutParams);
        addCloseImageView(relativeLayout, closeImageView);
    }

}
