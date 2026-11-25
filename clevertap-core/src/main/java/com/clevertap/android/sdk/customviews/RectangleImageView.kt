package com.clevertap.android.sdk.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

@Suppress("SuspiciousNameCombination")
@SuppressLint("AppCompatCustomView")
internal open class RectangleImageView : ImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth * 0.5625f
        val finalWidth = Math.round(width)
        setMeasuredDimension(measuredWidth, finalWidth)
    }
}
