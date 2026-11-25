package com.clevertap.android.sdk.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView

@SuppressLint("AppCompatCustomView")
internal open class HorizontalSquareImageView : ImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = measuredHeight
        @Suppress("SuspiciousNameCombination")
        setMeasuredDimension(height, height)
    }
}
