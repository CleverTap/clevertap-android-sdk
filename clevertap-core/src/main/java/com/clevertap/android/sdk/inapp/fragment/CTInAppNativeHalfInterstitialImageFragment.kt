package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.media.InAppMediaConfig
import com.clevertap.android.sdk.inapp.media.InAppMediaHandler
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeHalfInterstitialImageFragment : CTInAppBaseFullFragment() {

    private var relativeLayout: RelativeLayout? = null

    override fun createMediaHandler() = InAppMediaHandler.create(
        fragment = this,
        inAppNotification = inAppNotification,
        currentOrientation = currentOrientation,
        isTablet = inAppNotification.isTablet && isTablet(),
        resourceProvider = resourceProvider(),
        supportsStreamMedia = true,
        onActionClick = if (inAppNotification.buttons.isNotEmpty()) {{ handleButtonClickAtIndex(0) }} else null,
        lockedMediaUrl = activeMediaUrl
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val inAppView = if (inAppNotification.isTablet && isTablet()) {
            inflater.inflate(R.layout.tab_inapp_half_interstitial_image, container, false)
        } else {
            inflater.inflate(R.layout.inapp_half_interstitial_image, container, false)
        }

        val fl =
            inAppView.findViewById<FrameLayout>(R.id.inapp_half_interstitial_image_frame_layout)

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        fl.background = ColorDrawable(0xBB000000.toInt())

        relativeLayout = fl.findViewById(R.id.half_interstitial_image_relative_layout)
        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> addHalfInterstitialPortraitLayoutListener(
                relativeLayout, closeImageView
            ) { inAppNotification.isTablet }

            Configuration.ORIENTATION_LANDSCAPE -> addHalfInterstitialLandscapeLayoutListener(
                relativeLayout, closeImageView
            ) { inAppNotification.isTablet }
        }

        mediaHandler.setup(
            relativeLayout,
            InAppMediaConfig(imageViewId = R.id.half_interstitial_image, clickableMedia = true, videoFrameId = R.id.video_frame, gifImageId = R.id.gifImage),
            CTInAppNativeButtonClickListener()
        )

        relativeLayout?.findViewById<View>(R.id.gifImage)?.let { ViewCompat.setScreenReaderFocusable(it, true) }
        relativeLayout?.findViewById<View>(R.id.video_frame)?.let { ViewCompat.setScreenReaderFocusable(it, true) }

        setUpCloseButton(closeImageView)

        return inAppView
    }

}
