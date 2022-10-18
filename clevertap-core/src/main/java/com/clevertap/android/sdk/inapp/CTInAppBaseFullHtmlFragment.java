package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.CTWebInterface;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;
import com.clevertap.android.sdk.utils.UriHelper;
import java.net.URLDecoder;

public abstract class CTInAppBaseFullHtmlFragment extends CTInAppBaseFullFragment {

    private class InAppWebViewClient extends WebViewClient {

        InAppWebViewClient() {
            super();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Bundle formData;
            try {
                formData = UriHelper.getAllKeyValuePairs(url, false);

                if (formData.containsKey(Constants.KEY_C2A)) {
                    final String c2a = formData.getString(Constants.KEY_C2A);
                    if (c2a != null) {
                        final String[] parts = c2a.split("__dl__");
                        if (parts.length == 2) {
                            // Decode it here as wzrk_c2a is not decoded by UriHelper
                            formData.putString(Constants.KEY_C2A, URLDecoder.decode(parts[0], "UTF-8"));
                            url = parts[1];
                        }
                    }
                }

                didClick(-1, formData, null);
                Logger.d("Executing call to action for in-app: " + url);
                fireUrlThroughIntent(url, formData);
            } catch (Throwable t) {
                Logger.v("Error parsing the in-app notification action!", t);
            }
            return true;
        }
    }

    protected CTInAppWebView webView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        return displayHTMLView(inflater, container);
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

    protected RelativeLayout.LayoutParams getLayoutParamsForCloseButton() {
        RelativeLayout.LayoutParams closeIvLp = new RelativeLayout
                .LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        // Position it at the top right corner
        closeIvLp.addRule(RelativeLayout.ABOVE, webView.getId());
        closeIvLp.addRule(RelativeLayout.RIGHT_OF, webView.getId());

        int sub = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH) / 2;
        closeIvLp.setMargins(-sub, 0, 0, -sub);
        return closeIvLp;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private View displayHTMLView(LayoutInflater inflater, ViewGroup container) {
        View inAppView;
        try {
            inAppView = inflater.inflate(R.layout.inapp_html_full, container, false);
            RelativeLayout rl = inAppView.findViewById(R.id.inapp_html_full_relative_layout);
            RelativeLayout.LayoutParams webViewLp = new RelativeLayout
                    .LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            webViewLp.addRule(RelativeLayout.CENTER_IN_PARENT);

            initWebViewLayoutParams(webViewLp);
            webView = new CTInAppWebView(this.context, inAppNotification.getWidth(),
                    inAppNotification.getHeight(), inAppNotification.getWidthPercentage(),
                    inAppNotification.getHeightPercentage());
            InAppWebViewClient webViewClient = new InAppWebViewClient();
            webView.setWebViewClient(webViewClient);

            if (inAppNotification.isJsEnabled()) {
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                webView.getSettings().setAllowContentAccess(false);
                webView.getSettings().setAllowFileAccess(false);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    webView.getSettings().setAllowFileAccessFromFileURLs(false);
                }
                webView.addJavascriptInterface(
                        new CTWebInterface(CleverTapAPI.instanceWithConfig(getActivity(), config),
                                this), "CleverTap");
            }

            if (isDarkenEnabled()) {
                rl.setBackground(new ColorDrawable(0xBB000000));
            } else {
                rl.setBackground(new ColorDrawable(0x00000000));
            }

            rl.addView(webView, webViewLp);

            if (isCloseButtonEnabled()) {
                closeImageView = new CloseImageView(this.context);
                RelativeLayout.LayoutParams closeIvLp = getLayoutParamsForCloseButton();

                closeImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        didDismiss(null);
                    }
                });
                rl.addView(closeImageView, closeIvLp);
            }

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Fragment view not created", t);
            return null;
        }
        return inAppView;
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

    private boolean isCloseButtonEnabled() {
        return inAppNotification.isShowClose();
    }

    private boolean isDarkenEnabled() {
        return inAppNotification.isDarkenScreen();
    }

    private void reDrawInApp() {
        webView.updateDimension();

        if (inAppNotification.getCustomInAppUrl().isEmpty()) {
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
        } else {
            String url = inAppNotification.getCustomInAppUrl();
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(url);
        }
    }


}
