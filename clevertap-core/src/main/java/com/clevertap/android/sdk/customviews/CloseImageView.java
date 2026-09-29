package com.clevertap.android.sdk.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import androidx.appcompat.widget.AppCompatImageView;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;

/**
 * Represents the close button.
 */
public final class CloseImageView extends AppCompatImageView {

    public static final int VIEW_ID = 199272;
    private final int iconSize = getScaledPixels(Constants.INAPP_CLOSE_IV_WIDTH);
    private final int touchTargetSize = getScaledPixels(Constants.INAPP_CLOSE_IV_TOUCH_TARGET_WIDTH);

    @SuppressLint("ResourceType")
    public CloseImageView(Context context) {
        super(context);
        setId(VIEW_ID);
        initAccessibility(context);
    }

    @SuppressLint("ResourceType")
    public CloseImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setId(VIEW_ID);
        initAccessibility(context);
    }

    @SuppressLint("ResourceType")
    public CloseImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setId(VIEW_ID);
        initAccessibility(context);
    }

    private void initAccessibility(Context context) {
        setContentDescription(context.getString(R.string.ct_inapp_close_btn));
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {

            Context context = getContext();
            // Static R reference (instead of getIdentifier) so R8 resource shrinking cannot strip ct_close
            Bitmap closeBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ct_close, null);

            if (closeBitmap != null) {
                Bitmap scaledCloseBitmap = Bitmap.createScaledBitmap(closeBitmap,
                        iconSize, iconSize, true);
                float offset = (touchTargetSize - iconSize) / 2f;
                canvas.drawBitmap(scaledCloseBitmap, offset, offset, new Paint());
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
        setMeasuredDimension(touchTargetSize, touchTargetSize);
    }

    @SuppressWarnings("SameParameterValue")
    private int getScaledPixels(int raw) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                raw, getResources().getDisplayMetrics());
    }
}
