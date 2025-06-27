package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CTWebInterface;
import com.clevertap.android.sdk.inapp.CTInAppAction;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton;
import com.clevertap.android.sdk.inapp.CTInAppType;
import com.clevertap.android.sdk.inapp.CTInAppWebView;
import com.clevertap.android.sdk.inapp.InAppActionType;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.utils.UriHelper;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import org.json.JSONException;
import org.json.JSONObject;

// CTInAppHtmlBannerOverlay
// -------------------------------------------------------------------
// Class that renders *custom-html* "header" and "footer" CTInAppNotifications 
// using an overlay window instead of the normal Fragment-based banner.
//
// This allows this type of notification to be used in any Activity, 
// whereas the normal CTInAppHtmlHeaderFragment and CTInAppHtmlFooterFragment 
// require a FragmentActivity.
//
// Unreal’s GameActivity extends NativeActivity, not FragmentActivity, so the
// standard CleverTap banner fragments can’t run. Changing Unreal's GameActivity
// to a FragmentActivity would be a major platform change; this overlay sidesteps
// that requirement.
class CTInAppHtmlBannerOverlay {

    static public boolean canDisplay(@NonNull CTInAppNotification notification) {
        CTInAppType type = notification.getInAppType();
        return type == CTInAppType.CTInAppTypeFooterHTML || type == CTInAppType.CTInAppTypeHeaderHTML;
    }

    static public void display(@NonNull CTInAppNotification notification,
            @NonNull CleverTapInstanceConfig config,
            @NonNull Activity host,
            @NonNull InAppListener listener) {
        if (canDisplay(notification)) {
            new Overlay(notification, config, host, listener).show();
        } else {
            // we shouldn't get to here!
            config.getLogger().debug(config.getAccountId(),
                    "CTInAppHtmlBannerOverlay display() called when canDisplay() returned false!\n");

            // send the onDismissed notification so the caller doesn't wait for an
            // impossible dismissal that is never coming
            listener.inAppNotificationDidDismiss(notification, null);
        }
    }

    // ======================= implementation ===========================
    private static final class Overlay implements View.OnTouchListener, View.OnLongClickListener {
        Overlay(CTInAppNotification n, CleverTapInstanceConfig cfg, Activity a,
                InAppListener l) {
            notification = n;
            activity = a;
            config = cfg;
            listener = l;
            isJsEnabled = notification.isJsEnabled();

            config.getLogger().verbose(config.getAccountId(),
                    "CTInAppHtmlBannerOverlay CTInAppNotification JSON:\n" + n.getJsonDescription());
        }

        void show() {
            ui.post(this::build);
        }

        private final Activity activity;
        private final CleverTapInstanceConfig config;
        private final CTInAppNotification notification;
        private final InAppListener listener;
        private final boolean isJsEnabled;
        private final Handler ui = new Handler(Looper.getMainLooper());
        private final GestureDetector gd = new GestureDetector(new GestureListener(this));

        private WindowManager wm;
        private View overlayRoot;
        private CTInAppWebView webView;
        private boolean animatingDismiss = false;

        @SuppressLint("RestrictedAPI")
        private void build() {
            // this code partially based on CTInAppBasePartialHtmlFragment.displayHTMLView()

            FrameLayout root = new FrameLayout(activity);
            root.setClickable(true);
            root.setFocusable(true);
            overlayRoot = root;

            // ---------- WebView ----------
            webView = new CTInAppWebView(activity,
                    notification.getWidth(),
                    notification.getHeight(),
                    notification.getWidthPercentage(),
                    notification.getHeightPercentage(),
                    notification.getAspectRatio());

            webView.setWebViewClient(new OverlayWebClient(this));
            webView.setOnTouchListener(this);
            webView.setOnLongClickListener(this);

            // Install our custom JavaScript interface
            if (isJsEnabled) {
                CleverTapAPI instance = CleverTapAPI.instanceWithConfig(activity, config);
                webView.setJavaScriptInterface(new OverlayWebInterface(this, instance, config));
            }

            // load the HTML
            String html = notification.getHtml();
            config.getLogger().verbose(config.getAccountId(),
                    "CTInAppHtmlBannerOverlay CTInAppNotification HTML:\n" + html);
            webView.loadDataWithBaseURL(
                    null,
                    html,
                    "text/html", "utf-8", null);

            // add to the layout
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    gravity());
            root.addView(webView, lp);

            // add the overlay to the activity's window manager
            WindowManager.LayoutParams wmlp = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, // fit the webview
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL, // sit above with own input stream
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            wmlp.gravity = gravity(); // TOP or BOTTOM
            wmlp.token = activity.getWindow().getDecorView().getWindowToken(); // tie to this activity
            wm = activity.getWindowManager();
            wm.addView(root, wmlp);

            // callback into the InAppController
            listener.inAppNotificationDidShow(notification, null);
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

        private void dismiss(@Nullable Bundle additionalData) {
            if (overlayRoot == null || wm == null) {
                config.getLogger().debug(config.getAccountId(),
                        "CTInAppHtmlBannerOverlay.dismiss() - Missing overlay or window manager");
                return;
            }
            if (!animatingDismiss) {
                animatingDismiss = true;
                overlayRoot.animate()
                        .alpha(0f)
                        .setDuration(250)
                        .withEndAction(() -> finishDismiss(additionalData))
                        .start();
            } else {
                finishDismiss(additionalData);
            }
        }

        private void finishDismiss(@Nullable Bundle additionalData) {
            try {
                wm.removeViewImmediate(overlayRoot);
                overlayRoot = null;
                cleanupWebView();

                // callback into the InAppController
                listener.inAppNotificationDidDismiss(notification, additionalData);
            } catch (Exception exception) {
                config.getLogger().debug(config.getAccountId(),
                        "CTInAppHtmlBannerOverlay: Removing failed! " + exception);
            }
        }

        private void cleanupWebView() {
            try {
                if (webView != null) {
                    webView.cleanup(isJsEnabled);
                    webView = null;
                }
            } catch (Exception e) {
                config.getLogger().debug("cleanupWebView -> there was some crash in cleanup", e);
                // no-op; we are anyway destroying everything. This is just for safety.
            }
        }

        public void triggerAction(
                @NonNull CTInAppAction action,
                @Nullable String callToAction,
                @Nullable Bundle additionalData) {
            // this function based on CTInAppBaseFragment.triggerAction()

            if (action.getType() == InAppActionType.OPEN_URL) {
                // All URL parameters should be tracked as additional data
                final Bundle urlActionData = UriHelper.getAllKeyValuePairs(action.getActionUrl(), false);

                // callToAction is handled as a parameter
                String callToActionUrlParam = urlActionData.getString(Constants.KEY_C2A);
                // no need to keep it in the data bundle
                urlActionData.remove(Constants.KEY_C2A);

                // add all additional params, overriding the url params if there is a collision
                if (additionalData != null) {
                    urlActionData.putAll(additionalData);
                }
                // Use the merged data for the action
                additionalData = urlActionData;
                if (callToActionUrlParam != null) {
                    // check if there is a deeplink within the callToAction param
                    final String[] parts = callToActionUrlParam.split(Constants.URL_PARAM_DL_SEPARATOR);
                    if (parts.length == 2) {
                        // Decode it here as it is not decoded by UriHelper
                        try {
                            // Extract the actual callToAction value
                            callToActionUrlParam = URLDecoder.decode(parts[0], "UTF-8");
                        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                            config.getLogger().debug("Error parsing c2a param", e);
                        }
                        // use the url from the callToAction param
                        action = CTInAppAction.createOpenUrlAction(parts[1]);
                    }
                }
                if (callToAction == null) {
                    // Use the url param value only if no other value is passed
                    callToAction = callToActionUrlParam;
                }
            }
            if (callToAction == null) {
                callToAction = "";
            }

            // callback into the InAppController
            Bundle actionData = listener.inAppNotificationActionTriggered(notification,
                    action, callToAction, additionalData,
                    activity);
            dismiss(actionData);
        }

        // ---------------- helpers ----------------
        private int gravity() {
            CTInAppType type = notification.getInAppType();
            switch (type) {
                default:
                case CTInAppTypeHeaderHTML:
                    return Gravity.TOP;
                case CTInAppTypeFooterHTML:
                    return Gravity.BOTTOM;
            }
        }

        private int getScaledPixels(int raw) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    raw,
                    activity.getResources().getDisplayMetrics());
        }
    }

    // Customized CTWebInterface that overrides the fragment-dependent methods
    // with fragment-free implementations adjusted for the context of the Overlay
    private static final class OverlayWebInterface extends CTWebInterface {
        private final WeakReference<Overlay> overlayRef;
        private final CleverTapInstanceConfig config;

        public OverlayWebInterface(Overlay overlay, CleverTapAPI instance, CleverTapInstanceConfig cfg) {
            super(instance);
            overlayRef = new WeakReference<Overlay>(overlay);
            config = cfg;
        }

        @JavascriptInterface
        @Override
        public void dismissInAppNotification() {
            Overlay overlay = overlayRef.get();
            if (overlay != null) {
                overlay.ui.post(() -> {
                    overlay.dismiss(null);
                });
            } else {
                config.getLogger().debug("OverlayWebInterface: Missing overlay instance");
            }
        }

        @JavascriptInterface
        @Override
        public void triggerInAppAction(String actionJson, String callToAction, String buttonId) {
            if (actionJson == null) {
                config.getLogger().debug(config.getAccountId(),
                        "OverlayWebInterface: CTWebInterface action JSON is null");
                return;
            }
            try {
                CTInAppAction action = CTInAppAction.createFromJson(new JSONObject(actionJson));
                if (action == null) {
                    config.getLogger().debug(config.getAccountId(),
                            "OverlayWebInterface: CTWebInterface invalid action JSON: " + actionJson);
                    return;
                }
                Bundle actionData = new Bundle();
                if (buttonId != null) {
                    actionData.putString("button_id", buttonId);
                }
                Overlay overlay = overlayRef.get();
                if (overlay != null) {
                    overlay.ui.post(() -> {
                        overlay.triggerAction(action, callToAction, actionData);
                    });
                } else {
                    config.getLogger().debug(config.getAccountId(), "OverlayWebInterface: Missing overlay instance");
                }

            } catch (JSONException je) {
                config.getLogger().debug(config.getAccountId(),
                        "OverlayWebInterface: CTWebInterface invalid action JSON: " + actionJson);
            }
        }
    };

    // Customized WebViewClient; only needs to funnel OpenUrl actions back to the overlay
    private static final class OverlayWebClient extends WebViewClient {
        private final WeakReference<Overlay> overlayRef;

        OverlayWebClient(Overlay o) {
            overlayRef = new WeakReference<>(o);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
            return handle(v, r.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView v, String url) {
            return handle(v, Uri.parse(url));
        }

        private boolean handle(WebView v, Uri uri) {
            Overlay overlay = overlayRef.get();
            if (overlay != null) {
                overlay.triggerAction(CTInAppAction.createOpenUrlAction(uri.toString()), null, null);
                return true;
            }
            return false;
        }
    }

    // GestureListener for detecting dismissal swipes;
    // based on the version in CTInAppBasePartialHtmlFragment
    private static final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final WeakReference<Overlay> overlayRef;
        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;
        private static final String CTA_SWIPE_DISMISS = "swipe-dismiss";

        GestureListener(Overlay overlay) {
            overlayRef = new WeakReference<Overlay>(overlay);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 != null && e2 != null) {
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Right to left
                    return remove(false);
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    // Left to right
                    return remove(true);
                }
            }
            return false;
        }

        private boolean remove(boolean ltr) {
            Overlay overlay = overlayRef.get();
            AnimationSet animSet = new AnimationSet(true);
            TranslateAnimation anim;
            if (ltr) {
                anim = new TranslateAnimation(0, overlay.getScaledPixels(50), 0, 0);
            } else {
                anim = new TranslateAnimation(0, -overlay.getScaledPixels(50), 0, 0);
            }
            animSet.addAnimation(anim);
            animSet.addAnimation(new AlphaAnimation(1, 0));
            animSet.setDuration(300);
            animSet.setFillAfter(true);
            animSet.setFillEnabled(true);
            animSet.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    overlay.triggerAction(CTInAppAction.createCloseAction(), CTA_SWIPE_DISMISS, null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });

            overlay.webView.startAnimation(animSet);
            return true;
        }
    }

}
