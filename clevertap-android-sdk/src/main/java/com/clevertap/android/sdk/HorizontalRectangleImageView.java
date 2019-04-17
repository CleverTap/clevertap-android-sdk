package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

@SuppressLint("AppCompatCustomView")
public class HorizontalRectangleImageView extends ImageView {

    public HorizontalRectangleImageView(Context context) {
        super(context);
    }

    public HorizontalRectangleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalRectangleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

//        float width = getMeasuredWidth()*0.5625f;
//        int finalWidth = Math.round(width);
//        //noinspection SuspiciousNameCombination
//        setMeasuredDimension(getMeasuredWidth(), finalWidth);

        float width = getMeasuredHeight() * 1.76f;
        int finalWidth = Math.round(width);
        setMeasuredDimension(finalWidth,getMeasuredHeight());
    }
}
