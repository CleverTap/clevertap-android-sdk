package com.clevertap.android.sdk.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import androidx.annotation.RestrictTo

/**
 * Represents the close button.
 */
@SuppressLint("ResourceType")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CloseImageView : AppCompatImageView {

    private val iconSize = scaledPixels(Constants.INAPP_CLOSE_IV_WIDTH)
    private val touchTargetSize = scaledPixels(Constants.INAPP_CLOSE_IV_TOUCH_TARGET_WIDTH)

    // Inflated from XML layouts: keep the framework constructors (the init block runs for each).
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        id = VIEW_ID
        contentDescription = context.getString(R.string.ct_inapp_close_btn)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        try {
            // Static R reference (instead of getIdentifier) so R8 resource shrinking cannot strip ct_close
            val closeBitmap: Bitmap? = BitmapFactory.decodeResource(context.resources, R.drawable.ct_close, null)
            if (closeBitmap == null) {
                Logger.v("Unable to find inapp notif close button image")
                return
            }
            val scaledCloseBitmap = Bitmap.createScaledBitmap(closeBitmap, iconSize, iconSize, true)
            val offset = (touchTargetSize - iconSize) / 2f
            canvas.drawBitmap(scaledCloseBitmap, offset, offset, Paint())
        } catch (t: Throwable) {
            Logger.v("Error displaying the inapp notif close button image:", t)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // The image view is fixed in dip on all devices
        setMeasuredDimension(touchTargetSize, touchTargetSize)
    }

    @Suppress("SameParameterValue")
    private fun scaledPixels(raw: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, raw.toFloat(), resources.displayMetrics).toInt()

    companion object {
        const val VIEW_ID = 199272
    }
}
