package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.webkit.WebView;

import androidx.annotation.Px;
import androidx.annotation.RequiresApi;

@SuppressLint("ViewConstructor")
class CTInAppWebView extends WebView {

    private static final double DEFAULT_ASPECT_RATIO = -1;

    final Point dim = new Point();
    private final Context context;

    private final int heightDp;

    private final int heightPercentage;

    private final int widthDp;

    private final int widthPercentage;

    private final double aspectRatio;

    @SuppressLint("ResourceType")
    public CTInAppWebView(
            Context context,
            int widthDp,
            int heightDp,
            int widthPercentage,
            int heightPercentage
    ) {
        this(context, widthDp, heightDp, widthPercentage, heightPercentage, DEFAULT_ASPECT_RATIO);
    }

    @SuppressLint("ResourceType")
    public CTInAppWebView(
            Context context,
            int widthDp,
            int heightDp,
            int widthPercentage,
            int heightPercentage,
            double aspectRatio
    ) {
        super(context);
        this.context = context;
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.widthPercentage = widthPercentage;
        this.heightPercentage = heightPercentage;
        this.aspectRatio = aspectRatio;
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setHorizontalFadingEdgeEnabled(false);
        setVerticalFadingEdgeEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setBackgroundColor(0x00000000);
        // set the text zoom in order to ignore device font size changes
        getSettings().setTextZoom(100);
        //noinspection ResourceType
        setId(188293);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDimension();
        setMeasuredDimension(dim.x, dim.y);
    }

    void updateDimension() {

        int width;
        int height;

        if (widthDp > 0) {
            width = dpToPx(widthDp);
        } else {
            width = calculatePercentageWidth();
        }

        if (heightDp > 0) {
            height = dpToPx(heightDp);
        } else if (aspectRatio != -1) {
            height = (int) (width / aspectRatio);
        } else {
            height = calculatePercentageHeight();
        }

        dim.x = width;
        dim.y = height;
    }

    @Px
    private int calculateWidth() {
        if (widthDp > 0) {
            return dpToPx(widthDp);
        }
        return calculatePercentageWidth();
    }

    @Px
    private int calculateHeight() {
        if (heightDp > 0) {
            return dpToPx(heightDp);
        }
        return calculatePercentageHeight();
    }

    @Px
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Px
    private int calculatePercentageWidth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return calculateWidthWithWindowMetrics();
        }
        return calculateWidthWithDisplayMetrics();
    }

    @Px
    private int calculatePercentageHeight() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return calculateHeightWithWindowMetrics();
        }
        return calculateHeightWithDisplayMetrics();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Px
    private int calculateWidthWithWindowMetrics() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return calculateWidthWithDisplayMetrics();

        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() |
                        WindowInsets.Type.displayCutout()
        );

        int availableWidth = metrics.getBounds().width() - insets.left - insets.right;
        return (int) (availableWidth * widthPercentage / 100f);
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Px
    private int calculateHeightWithWindowMetrics() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) return calculateHeightWithDisplayMetrics();

        WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
        Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() |
                        WindowInsets.Type.displayCutout()
        );

        int availableHeight = metrics.getBounds().height() - insets.top - insets.bottom;
        return (int) (availableHeight * heightPercentage / 100f);
    }

    @Px
    private int calculateWidthWithDisplayMetrics() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (metrics.widthPixels * widthPercentage / 100f);
    }

    @Px
    private int calculateHeightWithDisplayMetrics() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return (int) (metrics.heightPixels * heightPercentage / 100f);
    }
}
