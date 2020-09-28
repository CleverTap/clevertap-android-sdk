package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

@SuppressWarnings("SuspiciousNameCombination")
@SuppressLint("AppCompatCustomView")
public class RectangleImageView extends ImageView {

    public RectangleImageView(Context context) {
        super(context);
    }

    public RectangleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RectangleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        float width = getMeasuredWidth() * 0.5625f;
        int finalWidth = Math.round(width);
        setMeasuredDimension(getMeasuredWidth(), finalWidth);
    }
}
