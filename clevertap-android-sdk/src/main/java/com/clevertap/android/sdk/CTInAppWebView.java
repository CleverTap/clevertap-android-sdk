package com.clevertap.android.sdk;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;

class CTInAppWebView extends WebView {

    private int width = 0;
    private int height = 0;
    private int widthPercentage = 0;
    private int heightPercentage = 0;
    final Point dim = new Point();

    public CTInAppWebView(Context context, int width , int height, int widthPercentage, int heightPercentage) {
        super(context);
        this.width = width;
        this.height = height;
        this.widthPercentage = widthPercentage;
        this.heightPercentage = heightPercentage;
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setHorizontalFadingEdgeEnabled(false);
        setVerticalFadingEdgeEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);
        setBackgroundColor(0x00000000);
        //noinspection ResourceType
        setId(188293);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDimension();
        setMeasuredDimension(dim.x, dim.y);
    }

    void updateDimension() {
        if (width != 0) {
            dim.x = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    width, getResources().getDisplayMetrics());
        } else {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            dim.x = (int) (metrics.widthPixels * widthPercentage / 100f);
        }
        if (height != 0) {
            dim.y = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    height, getResources().getDisplayMetrics());
        } else {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            dim.y = (int) (metrics.heightPixels * heightPercentage / 100f);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
