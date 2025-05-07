package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.CTWebInterface;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Logger;

public abstract class CTInAppBasePartialHtmlFragment extends CTInAppBasePartialFragment
        implements View.OnTouchListener, View.OnLongClickListener {

    private static final String CTA_SWIPE_DISMISS = "swipe-dismiss";

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private final int SWIPE_MIN_DISTANCE = 120;

        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 != null && e2 != null) {
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Right to left
                    return remove(false);
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Left to right
                    return remove(true);
                }
            }
            return false;
        }

        @SuppressWarnings("UnusedParameters")
        private boolean remove(boolean ltr) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation anim;
            if (ltr) {
                anim = new TranslateAnimation(0, getScaledPixels(50), 0, 0);
            } else {
                anim = new TranslateAnimation(0, -getScaledPixels(50), 0, 0);
            }
            animSet.addAnimation(anim);
            animSet.addAnimation(new AlphaAnimation(1, 0));
            animSet.setDuration(300);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    triggerAction(CTInAppAction.createCloseAction(), CTA_SWIPE_DISMISS, null);
                    didDismiss(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
            webView.startAnimation(animSet);
            return true;
        }
    }

    private final GestureDetector gd = new GestureDetector(new GestureListener());

    private CTInAppWebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        return displayHTMLView(inflater, container);
    }

    @Override
    public void onDestroyView() {
        cleanupWebView();
        super.onDestroyView();
    }

    private void cleanupWebView() {
        try {
            if (webView != null) {
                webView.cleanup(inAppNotification.isJsEnabled());
                webView = null;
            }
        } catch (Exception e) {
            config.getLogger().verbose("cleanupWebView -> there was some crash in cleanup", e);
            //no-op; we are anyway destroying everything. This is just for safety.
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reDrawInApp();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reDrawInApp();
    }

    @Override
    public boolean onLongClick(View v) {
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gd.onTouchEvent(event) || (event.getAction() == MotionEvent.ACTION_MOVE);
    }

    abstract ViewGroup getLayout(View view);

    abstract View getView(LayoutInflater inflater, ViewGroup container);

    @SuppressLint("SetJavaScriptEnabled")
    private View displayHTMLView(LayoutInflater inflater, ViewGroup container) {
        View inAppView;
        ViewGroup layout;
        try {
            inAppView = getView(inflater, container);
            layout = getLayout(inAppView);
            webView = new CTInAppWebView(
                    this.context,
                    inAppNotification.getWidth(),
                    inAppNotification.getHeight(),
                    inAppNotification.getWidthPercentage(),
                    inAppNotification.getHeightPercentage(),
                    inAppNotification.getAspectRatio()
            );
            InAppWebViewClient webViewClient = new InAppWebViewClient(this);
            webView.setWebViewClient(webViewClient);
            webView.setOnTouchListener(CTInAppBasePartialHtmlFragment.this);
            webView.setOnLongClickListener(CTInAppBasePartialHtmlFragment.this);

            if (inAppNotification.isJsEnabled()) {
                CleverTapAPI instance = CleverTapAPI.instanceWithConfig(getActivity(), config);
                CTWebInterface ctWebInterface = new CTWebInterface(instance, this);
                webView.setJavaScriptInterface(ctWebInterface);
            }

            if (layout != null) {
                layout.addView(webView);
            }
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Fragment view not created", t);
            return null;
        }
        return inAppView;
    }

    private void reDrawInApp() {
        webView.updateDimension();

        int mHeight = webView.dim.y;
        int mWidth = webView.dim.x;

        float d = getResources().getDisplayMetrics().density;
        mHeight /= d;
        mWidth /= d;

        String html = inAppNotification.getHtml();

        String style = "<style>body{width:" + mWidth + "px; height: " + mHeight
                + "px; margin: 0; padding:0;}</style>";
        html = html.replaceFirst("<head>", "<head>" + style);
        Logger.v("Density appears to be " + d);

        webView.setInitialScale((int) (d * 100));
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }
}
