package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

@SuppressLint("AppCompatCustomView")
public class HorizontalSquareImageView extends ImageView {

    public HorizontalSquareImageView(Context context) {
        super(context);
    }

    public HorizontalSquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalSquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = getMeasuredHeight();
        //noinspection SuspiciousNameCombination
        setMeasuredDimension(height, height);
    }

}
