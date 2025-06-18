package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.inapp.CTInAppNotificationButton

internal abstract class CTInAppBaseFullNativeFragment : CTInAppBaseFullFragment() {

    fun setupInAppButton(
        inAppButton: Button,
        inAppNotificationButton: CTInAppNotificationButton?,
        buttonIndex: Int
    ) {
        if (inAppNotificationButton != null) {
            inAppButton.visibility = View.VISIBLE
            inAppButton.tag = buttonIndex
            inAppButton.text = inAppNotificationButton.text
            inAppButton.setTextColor(inAppNotificationButton.textColor.toColorInt())
            inAppButton.setOnClickListener(CTInAppNativeButtonClickListener())

            var borderDrawable: ShapeDrawable? = null
            var shapeDrawable: ShapeDrawable? = null

            if (!inAppNotificationButton.borderRadius.isEmpty()) {
                val value: Float =
                    inAppNotificationButton.borderRadius.toFloat() * (480.0f / getDPI()) * 2
                shapeDrawable = ShapeDrawable(
                    RoundRectShape(
                        floatArrayOf(
                            value, value, value, value,
                            value, value, value, value
                        ), null,
                        floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    )
                )
                shapeDrawable.paint.setColor(inAppNotificationButton.backgroundColor.toColorInt())
                shapeDrawable.paint.style = Paint.Style.FILL
                shapeDrawable.paint.isAntiAlias = true
                borderDrawable = ShapeDrawable(
                    RoundRectShape(
                        floatArrayOf(
                            value, value, value, value,
                            value, value, value, value
                        ), null,
                        floatArrayOf(
                            value, value, value, value,
                            value, value, value, value
                        )
                    )
                )
            }

            if (!inAppNotificationButton.borderColor.isEmpty()) {
                if (borderDrawable != null) {
                    borderDrawable.paint.setColor(inAppNotificationButton.borderColor.toColorInt())
                    borderDrawable.setPadding(1, 1, 1, 1)
                    borderDrawable.paint.style = Paint.Style.FILL
                }
            }

            if (shapeDrawable != null && borderDrawable != null) {
                val drawables: Array<Drawable> = arrayOf<Drawable>(
                    borderDrawable,
                    shapeDrawable
                )

                val layerDrawable = LayerDrawable(drawables)
                inAppButton.background = layerDrawable
            }
        } else {
            inAppButton.visibility = View.GONE
        }
    }

    private fun getDPI(): Int {
        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        if (wm == null) {
            return DisplayMetrics.DENSITY_DEFAULT
        }
        //Returns the dpi using Device Configuration API for API30 above
        val density = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val configuration = requireContext().resources.configuration
            configuration.densityDpi
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getMetrics(dm)
            dm.densityDpi
        }
        return if (density > 0) {
            density
        } else {
            DisplayMetrics.DENSITY_DEFAULT
        }
    }
}
