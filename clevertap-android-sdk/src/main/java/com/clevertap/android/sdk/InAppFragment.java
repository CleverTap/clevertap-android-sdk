package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.net.URLDecoder;

public class InAppFragment extends Fragment implements  View.OnTouchListener, View.OnLongClickListener{

    private CTInAppNotification inAppNotification;
    private InAppWebView webView;
    private final Point dim = new Point();
    private CleverTapInstanceConfig config;
    private CleverTapAPI cleverTapAPI;
    private final GestureDetector gd = new GestureDetector(new GestureListener());
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Bundle bundle = getArguments();
        inAppNotification = bundle.getParcelable("inApp");
        config = bundle.getParcelable("config");
        cleverTapAPI = CleverTapAPI.instanceWithConfig(getActivity().getBaseContext(),config);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = null;
        try {
            FrameLayout fl = null;
            if (inAppNotification.getInAppType().equals(CTInAppType.CTInAppTypeHeaderHTML)) {
                inAppView = inflater.inflate(R.layout.header, container, false);
                fl = inAppView.findViewById(R.id.header_frame_layout);

            }
            else if (inAppNotification.getInAppType().equals(CTInAppType.CTInAppTypeFooterHTML)) {
                inAppView = inflater.inflate(R.layout.footer, container, false);
                fl = inAppView.findViewById(R.id.footer_frame_layout);
            }
            webView = new InAppWebView(getActivity().getBaseContext());
            InAppWebViewClient webViewClient = new InAppWebViewClient();
            webView.setWebViewClient(webViewClient);
            if(fl!=null)
                fl.addView(webView);
        }catch (Throwable t){
            config.getLogger().verbose(config.getAccountId(),"Fragment view not created",t);
            return null;
        }
        return inAppView;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reDrawInApp();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressWarnings("SameParameterValue")
    private int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
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
            setOnTouchListener(InAppFragment.this);
            setOnLongClickListener(InAppFragment.this);
            //noinspection ResourceType
            setId(188293);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            updateDimension();
            setMeasuredDimension(dim.x, dim.y);
        }
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
        removeFragment(formData);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
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
                    removeFragment(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            webView.startAnimation(animSet);
            return true;
        }
    }

    private void removeFragment(Bundle formData){
        getActivity().getFragmentManager()
                .beginTransaction()
                .remove(InAppFragment.this)
                .setCustomAnimations(android.R.animator.fade_in,android.R.animator.fade_out)
                .commit();
        cleverTapAPI.notificationDidDismiss(getActivity().getBaseContext(),inAppNotification, formData);
    }


}
