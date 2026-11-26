package com.clevertap.android.sdk.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import kotlin.math.roundToInt
import androidx.annotation.RestrictTo

@Suppress("SuspiciousNameCombination")
@SuppressLint("AppCompatCustomView")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class RectangleImageView : ImageView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        setMeasuredDimension(measuredWidth, (measuredWidth * 0.5625f).roundToInt())
    }
}
