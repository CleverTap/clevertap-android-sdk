package com.clevertap.android.directcall.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.core.content.res.ResourcesCompat;

import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.utils.Utils;

public class DCScratchCard extends View {
    private Drawable mDrawable;
    private float mScratchWidth;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mInnerPaint;
    private Paint mOuterPaint;
    private OnScratchListener mListener;

    public interface OnScratchListener {
        void onScratch(DCScratchCard DCScratchCard, float visiblePercent);
    }

    public DCScratchCard(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        resolveAttr(context, attrs);
    }

    public DCScratchCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        resolveAttr(context, attrs);
    }

    public DCScratchCard(Context context) {
        super(context);
        resolveAttr(context, null);
    }

    private void resolveAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DCScratchCard);
        mDrawable = typedArray.getDrawable(R.styleable.DCScratchCard_scratchDrawable);
        mScratchWidth = typedArray.getDimension(R.styleable.DCScratchCard_scratchWidth, Utils.getInstance().dipToPx(context, 70));
        typedArray.recycle();
    }

    public void setOuterScratchDrawable(@DrawableRes int resId) {
        mDrawable = ResourcesCompat.getDrawable(getResources(), resId, getContext().getTheme());
        invalidate();
    }

    public void setOnScratchListener(OnScratchListener listener) {
        mListener = listener;
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (mBitmap != null)
            mBitmap.recycle();

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        if (mDrawable != null) {
            mDrawable.setBounds(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
            mDrawable.draw(mCanvas);
        } else {
            mCanvas.drawColor(0xFFEC7063);
        }

        if (mPath == null) {
            mPath = new Path();
        }

        if (mInnerPaint == null) {
            mInnerPaint = new Paint();
            mInnerPaint.setAntiAlias(true);
            mInnerPaint.setDither(true);
            mInnerPaint.setStyle(Paint.Style.STROKE);
            mInnerPaint.setFilterBitmap(true);
            mInnerPaint.setStrokeJoin(Paint.Join.ROUND);
            mInnerPaint.setStrokeCap(Paint.Cap.ROUND);
            mInnerPaint.setStrokeWidth(mScratchWidth);
            mInnerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }

        if (mOuterPaint == null) {
            mOuterPaint = new Paint();
        }
    }

    private float mLastTouchX;
    private float mLastTouchY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentTouchX = event.getX();
        float currentTouchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.reset();
                mPath.moveTo(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                try {
                    float dx = Math.abs(currentTouchX - mLastTouchX);
                    float dy = Math.abs(currentTouchY - mLastTouchY);
                    if (dx >= 4 || dy >= 4) {
                        float x1 = mLastTouchX;
                        float y1 = mLastTouchY;
                        float x2 = (currentTouchX + mLastTouchX) / 2;
                        float y2 = (currentTouchY + mLastTouchY) / 2;
                        mPath.quadTo(x1, y1, x2, y2);
                    }

                    if (mListener != null) {
                        int width = mBitmap.getWidth();
                        int height = mBitmap.getHeight();
                        int total = width * height;
                        int count = 0;
                        for (int i = 0; i < width; i += 3) {
                            for (int j = 0; j < height; j += 3) {
                                if (mBitmap.getPixel(i, j) == 0x00000000)
                                    count++;
                            }
                        }
                        mListener.onScratch(this, ((float) count) / total * 9);
                    }
                }
                catch (Exception e){

                }
                break;
            case MotionEvent.ACTION_UP:
               /* mPath.lineTo(currentTouchX, currentTouchY);
                if (mListener != null) {
                    int width = mBitmap.getWidth();
                    int height = mBitmap.getHeight();
                    int total = width * height;
                    int count = 0;
                    for (int i = 0; i < width; i += 3) {
                        for (int j = 0; j < height; j += 3) {
                            if (mBitmap.getPixel(i, j) == 0x00000000)
                                count++;
                        }
                    }
                    mListener.onScratch(this, ((float) count) / total * 9);
                }*/
                break;
        }

        mCanvas.drawPath(mPath, mInnerPaint);
        mLastTouchX = currentTouchX;
        mLastTouchY = currentTouchY;

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mOuterPaint);
        super.onDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
