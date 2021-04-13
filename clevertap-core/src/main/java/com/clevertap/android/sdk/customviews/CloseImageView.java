package com.clevertap.android.sdk.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import androidx.appcompat.widget.AppCompatImageView;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;

/**
 * Represents the close button.
 */
public final class CloseImageView extends AppCompatImageView {

    private final int canvasSize = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH);

    @SuppressLint("ResourceType")
    public CloseImageView(Context context) {
        super(context);
        setId(199272);
    }

    @SuppressLint("ResourceType")
    public CloseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setId(199272);
    }

    @SuppressLint("ResourceType")
    public CloseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setId(199272);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {

            Context context = getContext();
            int resourceID = context.getResources().getIdentifier("ct_close", "drawable", context.getPackageName());
            Bitmap closeBitmap = BitmapFactory.decodeResource(context.getResources(), resourceID, null);

            if (closeBitmap != null) {
                Bitmap scaledCloseBitmap = Bitmap.createScaledBitmap(closeBitmap,
                        canvasSize, canvasSize, true);
                canvas.drawBitmap(scaledCloseBitmap, 0, 0, new Paint());
            } else {
                Logger.v("Unable to find inapp notif close button image");
            }
        } catch (Throwable t) {
            Logger.v("Error displaying the inapp notif close button image:", t);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The image view is fixed in dip on all devices
        setMeasuredDimension(canvasSize, canvasSize);
    }

    @SuppressWarnings("SameParameterValue")
    private int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }
}
