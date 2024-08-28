package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;

@SuppressLint("ViewConstructor")
class CTInAppWebView extends WebView {

    final Point dim = new Point();

    private int height;

    private int heightPercentage;

    private int width;

    private int widthPercentage;

    @SuppressLint("ResourceType")
    public CTInAppWebView(Context context, int width, int height, int widthPercentage, int heightPercentage) {
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
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        updateDimension();
        setMeasuredDimension(measureWidth(widthMeasureSpec,dim.x), measureHeight(heightMeasureSpec,dim.y));
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

    private int measureHeight(int measureSpec, int wantedHeight) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = wantedHeight;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
    private int measureWidth(int measureSpec, int wantedWidth) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = wantedWidth;
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }
}
