package com.clevertap.android.sdk.inapp.fragment

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.media.NoOpMediaHandler

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

    companion object {
        private const val HALF_INTERSTITIAL_ASPECT_RATIO = 1.3f

        /**
         * Returns the height the half-interstitial card must be reduced to so that it fits the
         * screen, or null when it fits and keeps its usual height of width x 1.3.
         *
         * The card is centered, so the bars cover its container on both sides and [barHeight] is
         * removed twice. When no height is left, null is returned and the usual size is kept.
         *
         * @param cardWidth width of the card before resizing
         * @param cardHeight height of the card before resizing
         * @param barHeight how much the taller bar covers the container of the card
         */
        internal fun fittedHalfInterstitialHeight(cardWidth: Int, cardHeight: Int, barHeight: Int): Int? {
            val aspectHeight = (cardWidth * HALF_INTERSTITIAL_ASPECT_RATIO).toInt()
            val availableHeight = cardHeight - 2 * barHeight
            return if (availableHeight > 0 && aspectHeight > availableHeight) availableHeight else null
        }

        /**
         * Returns how much the status bar, the navigation bar or the display cutout covers the
         * container of [relativeLayout], or 0 when it is not covered.
         *
         * An app that is drawn behind these bars (edge to edge, forced on Android 15 and above for
         * apps that target API 35 or higher) is covered by them. Otherwise the system already keeps
         * the content clear of the bars and nothing is covered, so the bars must not be removed again.
         */
        private fun systemBarOverlap(relativeLayout: RelativeLayout): Int {
            val bars = ViewCompat.getRootWindowInsets(relativeLayout)?.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val container = relativeLayout.parent as? View
            if (bars == null || container == null) {
                return 0
            }
            val containerTop = IntArray(2).also { container.getLocationInWindow(it) }[1]
            val windowHeight = relativeLayout.rootView.height
            val overlapTop = maxOf(0, bars.top - containerTop)
            val overlapBottom = maxOf(0, containerTop + container.height - (windowHeight - bars.bottom))
            return maxOf(overlapTop, overlapBottom)
        }
    }

    override fun cleanup() {
        if (mediaHandler !is NoOpMediaHandler) {
            lifecycle.removeObserver(mediaHandler)
            mediaHandler.cleanup()
        }
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
        val fittedHeight = fittedHalfInterstitialHeight(
            cardWidth = relativeLayout.measuredWidth,
            cardHeight = relativeLayout.measuredHeight,
            barHeight = systemBarOverlap(relativeLayout)
        )
        if (fittedHeight != null) {
            // The card would be taller than the screen, so fit the height and make it narrower.
            layoutParams.height = fittedHeight
            layoutParams.width = (fittedHeight / HALF_INTERSTITIAL_ASPECT_RATIO).toInt()
        } else {
            layoutParams.height = (relativeLayout.measuredWidth * HALF_INTERSTITIAL_ASPECT_RATIO).toInt()
        }
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
