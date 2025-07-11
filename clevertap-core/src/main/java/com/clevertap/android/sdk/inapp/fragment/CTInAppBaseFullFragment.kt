package com.clevertap.android.sdk.inapp.fragment

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.InAppListener

internal abstract class CTInAppBaseFullFragment : CTInAppBaseFragment() {

    fun addCloseImageView(relativeLayout: RelativeLayout, closeImageView: CloseImageView) {
        relativeLayout.post(object : Runnable {
            override fun run() {
                val margin = closeImageView.measuredWidth / 2
                closeImageView.x = (relativeLayout.right - margin).toFloat()
                closeImageView.y = (relativeLayout.top - margin).toFloat()
            }
        })
    }

    override fun cleanup() { /* no-op */
    }

    override fun generateListener() {
        val context = context
        if (context is InAppNotificationActivity) {
            setListener(context as InAppListener)
        }
    }

    /**
     * Checks if a devices is a tablet or a handset based on smallest width qualifier which specifies the smallest of
     * the screen's two sides, regardless of the device's current orientation.<br></br>
     * for example,<br></br> 600dp: a 7” tablet (600x1024 mdpi)
     * <br></br>480dp: a large phone screen ~5" (480x800 mdpi)
     *
     * Adopting this method to determine if a device is tablet over manually calculating diagonal of device due to
     * some OEM issues. [#116](https://github.com/CleverTap/clevertap-android-sdk/issues/116)
     *
     * @return true if device screen's smallest width, independent of orientation is >= 600dp else false
     */
    fun isTablet(): Boolean {
            if (Utils.isActivityDead(activity)) {
                return false
            }

            try {
                return resources.getBoolean(R.bool.ctIsTablet)
            } catch (_: Exception) {
                // resource not found
                Logger.d("Failed to decide whether device is a smart phone or tablet!")
                return false
            }
        }

    fun redrawHalfInterstitialInApp(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        closeImageView: CloseImageView
    ) {
        layoutParams.height = (relativeLayout.measuredWidth * 1.3f).toInt()
        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawHalfInterstitialMobileInAppOnTablet(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        closeImageView: CloseImageView
    ) {
        layoutParams.setMargins(
            getScaledPixels(140),
            getScaledPixels(140),
            getScaledPixels(140),
            getScaledPixels(140)
        )
        layoutParams.width = relativeLayout.measuredWidth - getScaledPixels(210)
        layoutParams.height = (layoutParams.width * 1.3f).toInt()
        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawInterstitialInApp(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        closeImageView: CloseImageView
    ) {
        layoutParams.height = (relativeLayout.measuredWidth * 1.78f).toInt()
        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawInterstitialMobileInAppOnTablet(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        fl: FrameLayout,
        closeImageView: CloseImageView
    ) {
        val aspectHeight = ((relativeLayout.measuredWidth - getScaledPixels(200)) * 1.78f).toInt()
        val requiredHeight = fl.measuredHeight - getScaledPixels(280)

        if (aspectHeight > requiredHeight) {
            layoutParams.height = requiredHeight
            layoutParams.width = (requiredHeight / 1.78f).toInt()
        } else {
            layoutParams.height = aspectHeight
            layoutParams.width = relativeLayout.measuredWidth - getScaledPixels(
                200
            )
        }

        layoutParams.setMargins(
            getScaledPixels(140), getScaledPixels(140), getScaledPixels(140), getScaledPixels(140)
        )

        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawInterstitialTabletInApp(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        fl: FrameLayout,
        closeImageView: CloseImageView
    ) {
        val aspectHeight = (relativeLayout.measuredWidth * 1.78f).toInt()
        val requiredHeight = fl.measuredHeight - getScaledPixels(80)

        if (aspectHeight > requiredHeight) {
            layoutParams.height = requiredHeight
            layoutParams.width = (requiredHeight / 1.78f).toInt()
        } else {
            layoutParams.height = aspectHeight
        }

        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawLandscapeInterstitialInApp(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        closeImageView: CloseImageView
    ) {
        layoutParams.width = (relativeLayout.measuredHeight * 1.78f).toInt()
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawLandscapeInterstitialMobileInAppOnTablet(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        fl: FrameLayout,
        closeImageView: CloseImageView
    ) {
        val aspectWidth = ((relativeLayout.measuredHeight - getScaledPixels(120)) * 1.78f).toInt()
        val requiredWidth = fl.measuredWidth - getScaledPixels(280)

        if (aspectWidth > requiredWidth) {
            layoutParams.width = requiredWidth
            layoutParams.height = (requiredWidth / 1.78f).toInt()
        } else {
            layoutParams.width = aspectWidth
            layoutParams.height = (relativeLayout.measuredHeight - getScaledPixels(120))
        }

        layoutParams.setMargins(
            getScaledPixels(140), getScaledPixels(100), getScaledPixels(140), getScaledPixels(100)
        )
        layoutParams.gravity = Gravity.CENTER
        relativeLayout.setLayoutParams(layoutParams)

        addCloseImageView(relativeLayout, closeImageView)
    }

    fun redrawLandscapeInterstitialTabletInApp(
        relativeLayout: RelativeLayout,
        layoutParams: FrameLayout.LayoutParams,
        fl: FrameLayout,
        closeImageView: CloseImageView
    ) {
        val aspectWidth = (relativeLayout.measuredHeight * 1.78f).toInt()
        val requiredWidth = fl.measuredWidth - getScaledPixels(80)

        if (aspectWidth > requiredWidth) {
            layoutParams.width = requiredWidth
            layoutParams.height = (requiredWidth / 1.78f).toInt()
        } else {
            layoutParams.width = aspectWidth
        }

        layoutParams.gravity = Gravity.CENTER
        relativeLayout.setLayoutParams(layoutParams)
        addCloseImageView(relativeLayout, closeImageView)
    }
}
