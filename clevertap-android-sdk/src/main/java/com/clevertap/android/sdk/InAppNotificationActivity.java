package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import java.net.URLDecoder;

public final class InAppNotificationActivity extends Activity implements  View.OnTouchListener, View.OnLongClickListener {

    private CTInAppNotification inAppNotification;
    private InAppWebView webView;
    private CloseImageView closeImageView = null;
    private final Point dim = new Point();
    private final GestureDetector gd = new GestureDetector(new GestureListener());
    private CleverTapAPI cleverTapAPI;
    private boolean isDismissed;

    @Override
    public void setTheme(int resid) {
        super.setTheme(android.R.style.Theme_Translucent_NoTitleBar);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle notif;

        try {
            notif = getIntent().getExtras();
            if (notif == null) {
                throw new IllegalArgumentException();
            }
            inAppNotification = notif.getParcelable("inApp");
            CleverTapInstanceConfig config = notif.getParcelable("config");
            if (config == null) {
                throw new IllegalArgumentException();
            }
            cleverTapAPI = CleverTapAPI.instanceWithConfig(getApplicationContext(), config);
        } catch (Throwable t) {
            Logger.v("Cannot find a valid notification bundle to show!", t);
            return;
        }

        if (!notif.getBoolean("wzrk_animated", false)) {
            // It is not possible to set the pending animation when this activity
            // was originally launched. So, maintain a flag, and restart itself
            // with the pending animation.
            notif.putBoolean("wzrk_animated", true);
            Intent i = new Intent(this, InAppNotificationActivity.class);
            i.putExtras(notif);
            startActivity(i);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }

        // Use a relative layout - very easy to handle all the positions
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams webViewLp = new RelativeLayout
                .LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        webViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);

        // Init the layout params for the notification
        initWebViewLayoutParams(webViewLp);
        webView = new InAppWebView(this);
        InAppWebViewClient webViewClient = new InAppWebViewClient();
        webView.setWebViewClient(webViewClient);

        // For simple notifications, the background isn't partially visible
        // Think of a simple bar notification at the bottom of the screen
        if (isDarkenEnabled())
            rl.setBackgroundDrawable(new ColorDrawable(0xBB000000));
        else
            rl.setBackgroundDrawable(new ColorDrawable(0x00000000));


        rl.addView(webView, webViewLp);

        if (isCloseButtonEnabled()) {
            // Add the close button to the RL
            closeImageView = new CloseImageView(this);
            RelativeLayout.LayoutParams closeIvLp = new RelativeLayout
                    .LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT);
            // Position it at the top right corner
            closeIvLp.addRule(RelativeLayout.ABOVE, webView.getId());
            closeIvLp.addRule(RelativeLayout.RIGHT_OF, webView.getId());


            int sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 2;
            closeIvLp.setMargins(-sub, 0, 0, -sub);

            closeImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    notifyDismissed(null);
                }
            });
            // Add it to the RL
            rl.addView(closeImageView, closeIvLp);
        }
        setContentView(rl);
        rl.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    cleverTapAPI.notificationDidDismiss(getApplicationContext(), inAppNotification, null);
                    return true;
                } else {
                    return false;
                }
            }
        });
        reDrawInApp();
        cleverTapAPI.pushInAppNotificationStateEvent(false, inAppNotification, null);
    }

    private boolean isCloseButtonEnabled() {
        return inAppNotification.isShowClose();
    }

    private boolean isDarkenEnabled() {
        return inAppNotification.isDarkenScreen();
    }

    private void notifyDismissed(Bundle formData) {
        if (isDismissed) {
            return;
        }
        isDismissed = true;
        Logger.v("InAppNotificationActivity notifying dismiss for notification: " + inAppNotification.getCampaignId());
        cleverTapAPI.notificationDidDismiss(getApplicationContext(),inAppNotification, formData);
    }

    private class InAppWebViewClient extends WebViewClient {
        InAppWebViewClient() {
            super();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Bundle formData;
            try {
                formData = UriHelper.getAllKeyValuePairs(url, false);

                if (formData != null && formData.containsKey("wzrk_c2a")) {
                    final String c2a = formData.getString("wzrk_c2a");
                    if (c2a != null) {
                        final String[] parts = c2a.split("__dl__");
                        if (parts.length == 2) {
                            // Decode it here as wzrk_c2a is not decoded by UriHelper
                            formData.putString("wzrk_c2a", URLDecoder.decode(parts[0], "UTF-8"));
                            url = parts[1];
                        }
                    }
                }

                cleverTapAPI.pushInAppNotificationStateEvent(true, inAppNotification, formData);
                Logger.d("Executing call to action for in-app: " + url);
                fireUrlThroughIntent(url,formData);
            } catch (Throwable t) {
                Logger.v("Error parsing the in-app notification action!", t);
            }
            return true;
        }
    }

    private void fireUrlThroughIntent(String url, Bundle formData) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Throwable t) {
            // Ignore
        }
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        notifyDismissed(formData);
    }

    private class InAppWebView extends WebView {

        @SuppressLint("ResourceType")
        public InAppWebView(Context context) {
            super(context);
            setHorizontalScrollBarEnabled(false);
            setVerticalScrollBarEnabled(false);
            setHorizontalFadingEdgeEnabled(false);
            setVerticalFadingEdgeEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
            setBackgroundColor(0x00000000);
            setOnTouchListener(InAppNotificationActivity.this);
            setOnLongClickListener(InAppNotificationActivity.this);
            //noinspection ResourceType
            setId(188293);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            // This should be the last line in this method
            updateDimension();
            setMeasuredDimension(dim.x, dim.y);
        }
    }

    private void updateDimension() {
        // Set the dimensions for this
        // Set X dimension
        if (inAppNotification.getWidth() != 0) {
            // Ignore Constants.INAPP_X_PERCENT
            dim.x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    inAppNotification.getWidth(), getResources().getDisplayMetrics());
        } else {
            // Calculate the size using Constants.INAPP_X_PERCENT
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            dim.x = (int) (metrics.widthPixels * inAppNotification.getWidthPercentage() / 100f);
        }

        // Set Y dimension
        if (inAppNotification.getHeight() != 0) {
            // Ignore Constants.INAPP_X_PERCENT
            dim.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    inAppNotification.getHeight(), getResources().getDisplayMetrics());
        } else {
            // Calculate the size using Constants.INAPP_Y_PERCENT
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            dim.y = (int) (metrics.heightPixels * inAppNotification.getHeightPercentage() / 100f);
        }
    }


    private void initWebViewLayoutParams(RelativeLayout.LayoutParams params) {
        char pos = inAppNotification.getPosition();
        switch (pos) {
            case Constants.INAPP_POSITION_TOP:
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                break;
            case Constants.INAPP_POSITION_LEFT:
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                break;
            case Constants.INAPP_POSITION_BOTTOM:
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            case Constants.INAPP_POSITION_RIGHT:
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            case Constants.INAPP_POSITION_CENTER:
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
                break;
        }
        params.setMargins(0, 0, 0, 0);
    }

    private int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }

    private void reDrawInApp() {
        updateDimension();

        int mHeight = dim.y;
        int mWidth = dim.x;

        float d = getResources().getDisplayMetrics().density;
        mHeight /= d;
        mWidth /= d;

        String html = inAppNotification.getHtml();

        String style = "<style>body{width:" + mWidth + "px; height: " + mHeight + "px}</style>";
        html = html.replaceFirst("<head>", "<head>" + style);
        Logger.v("Density appears to be " + d);

        webView.setInitialScale((int) (d * 100));
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // Right to left
                return remove(e1, e2, false);
            } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                // Left to right
                return remove(e1, e2, true);
            }
            return false;
        }

        @SuppressWarnings("UnusedParameters")
        private boolean remove(MotionEvent e1, MotionEvent e2, boolean ltr) {
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation anim;
            if (ltr)
                anim = new TranslateAnimation(0, getScaledPixels(50), 0, 0);
            else
                anim = new TranslateAnimation(0, -getScaledPixels(50), 0, 0);
            animSet.addAnimation(anim);
            animSet.addAnimation(new AlphaAnimation(1, 0));
            animSet.setDuration(300);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Notification was dismissed
                    finish();
                    if (isDarkenEnabled()) {
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                    notifyDismissed(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            webView.startAnimation(animSet);
            if (closeImageView != null)
                closeImageView.startAnimation(animSet);
            return true;
        }
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reDrawInApp();
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        notifyDismissed(null);
    }
}
