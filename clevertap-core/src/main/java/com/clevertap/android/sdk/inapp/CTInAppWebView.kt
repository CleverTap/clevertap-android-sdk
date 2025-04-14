package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebView
import androidx.annotation.Px
import androidx.annotation.RequiresApi

@SuppressLint("ViewConstructor")
internal class CTInAppWebView @SuppressLint("ResourceType") constructor(
    private val context: Context,
    private val widthDp: Int,
    private val heightDp: Int,
    private val widthPercentage: Int,
    private val heightPercentage: Int,
    private val aspectRatio: Double
) : WebView(context) {

    companion object {
        private const val DEFAULT_ASPECT_RATIO = -1.0
    }

    @JvmField
    val dim: Point = Point()

    @SuppressLint("ResourceType")
    constructor(
        context: Context,
        widthDp: Int,
        heightDp: Int,
        widthPercentage: Int,
        heightPercentage: Int
    ) : this(context, widthDp, heightDp, widthPercentage, heightPercentage, DEFAULT_ASPECT_RATIO)

    init {
        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = false
        isVerticalFadingEdgeEnabled = false
        overScrollMode = OVER_SCROLL_NEVER
        setBackgroundColor(0x00000000)
        // set the text zoom in order to ignore device font size changes
        settings.textZoom = 100
        id = 188293
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        updateDimension()
        setMeasuredDimension(dim.x, dim.y)
    }

    fun updateDimension() {

        val width = if (widthDp > 0) {
            dpToPx(widthDp)
        } else {
            calculatePercentageWidth()
        }

        val height = if (heightDp > 0) {
            dpToPx(heightDp)
        } else if (aspectRatio != -1.0 && aspectRatio > 0.0f) {
            (width / aspectRatio).toInt()
        } else {
            calculatePercentageHeight()
        }

        dim.x = width
        dim.y = height
    }

    @Px
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    @Px
    private fun calculatePercentageWidth(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return calculateWidthWithWindowMetrics()
        }
        return calculateWidthWithDisplayMetrics()
    }

    @Px
    private fun calculatePercentageHeight(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return calculateHeightWithWindowMetrics()
        }
        return calculateHeightWithDisplayMetrics()
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Px
    private fun calculateWidthWithWindowMetrics(): Int {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return calculateWidthWithDisplayMetrics()

        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or
                    WindowInsets.Type.displayCutout()
        )

        val availableWidth = metrics.bounds.width() - insets.left - insets.right
        return (availableWidth * widthPercentage / 100f).toInt()
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Px
    private fun calculateHeightWithWindowMetrics(): Int {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                ?: return calculateHeightWithDisplayMetrics()

        val metrics = windowManager.currentWindowMetrics
        val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.systemBars() or
                    WindowInsets.Type.displayCutout()
        )

        val availableHeight = metrics.bounds.height() - insets.top - insets.bottom
        return (availableHeight * heightPercentage / 100f).toInt()
    }

    @Px
    private fun calculateWidthWithDisplayMetrics(): Int {
        val metrics = resources.displayMetrics
        return (metrics.widthPixels * widthPercentage / 100f).toInt()
    }

    @Px
    private fun calculateHeightWithDisplayMetrics(): Int {
        val metrics = resources.displayMetrics
        return (metrics.heightPixels * heightPercentage / 100f).toInt()
    }
}
