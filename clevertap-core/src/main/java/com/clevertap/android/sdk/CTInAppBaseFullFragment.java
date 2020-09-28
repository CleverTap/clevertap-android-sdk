package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.RelativeLayout;
import androidx.fragment.app.FragmentActivity;

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
        if ((FragmentActivity) context instanceof InAppNotificationActivity) {
            setListener((CTInAppBaseFragment.InAppListener) context);
        }
    }

    boolean isTablet() {
        if (Utils.isActivityDead(getActivity())) {
            return false;
        }
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            Logger.v("Screen size is null ");
            return false;
        }
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        float yInches = dm.heightPixels / dm.ydpi;
        float xInches = dm.widthPixels / dm.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 7) {
            Logger.v("Screen size is : " + diagonalInches);
            return true;
        } else {
            Logger.v("Screen size is : " + diagonalInches);
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

    void redrawLandscapeInterstitialTabletInApp(final RelativeLayout relativeLayout,
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

}
