package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.media.InAppMediaConfig
import com.clevertap.android.sdk.inapp.media.InAppMediaHandler
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeHalfInterstitialFragment : CTInAppBaseFullNativeFragment() {

    private var relativeLayout: RelativeLayout? = null

    override fun createMediaHandler() = InAppMediaHandler.create(
        fragment = this,
        inAppNotification = inAppNotification,
        currentOrientation = currentOrientation,
        isTablet = inAppNotification.isTablet && isTablet(),
        resourceProvider = resourceProvider(),
        lockedMediaUrl = activeMediaUrl
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val inAppButtons = mutableListOf<Button>()
        val inAppView: View =
            if ((inAppNotification.isTablet || inAppNotification.isLocalInApp) && isTablet()) {
                inflater.inflate(R.layout.tab_inapp_half_interstitial, container, false)
            } else {
                inflater.inflate(R.layout.inapp_half_interstitial, container, false)
            }

        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_half_interstitial_frame_layout)

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        relativeLayout = fl.findViewById(R.id.half_interstitial_relative_layout)
        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> addHalfInterstitialPortraitLayoutListener(
                relativeLayout, closeImageView
            ) { inAppNotification.isTablet || inAppNotification.isLocalInApp }

            Configuration.ORIENTATION_LANDSCAPE -> addHalfInterstitialLandscapeLayoutListener(
                relativeLayout, closeImageView
            ) { inAppNotification.isTablet }
        }

        mediaHandler.setup(
            relativeLayout,
            InAppMediaConfig(imageViewId = R.id.backgroundImage, clickableMedia = false, gifImageId = R.id.gifImage)
        )

        relativeLayout?.findViewById<View>(R.id.backgroundImage)?.let { ViewCompat.setScreenReaderFocusable(it, true) }
        relativeLayout?.findViewById<View>(R.id.gifImage)?.let { ViewCompat.setScreenReaderFocusable(it, true) }

        val linearLayout =
            relativeLayout?.findViewById<LinearLayout>(R.id.half_interstitial_linear_layout)
        val mainButton = linearLayout?.findViewById<Button>(R.id.half_interstitial_button1)
        mainButton?.let { inAppButtons.add(it) }
        val secondaryButton = linearLayout?.findViewById<Button>(R.id.half_interstitial_button2)
        secondaryButton?.let { inAppButtons.add(it) }

        val textView1 = relativeLayout?.findViewById<TextView>(R.id.half_interstitial_title)
        textView1?.text = inAppNotification.title
        textView1?.setTextColor(inAppNotification.titleColor.toColorInt())
        textView1?.let { ViewCompat.setScreenReaderFocusable(it, true) }

        val textView2 = relativeLayout?.findViewById<TextView>(R.id.half_interstitial_message)
        textView2?.text = inAppNotification.message
        textView2?.setTextColor(inAppNotification.messageColor.toColorInt())
        textView2?.let { ViewCompat.setScreenReaderFocusable(it, true) }

        val buttons = inAppNotification.buttons
        if (buttons.size == 1) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mainButton?.visibility = View.GONE
            } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mainButton?.visibility = View.INVISIBLE
            }
            secondaryButton?.let { setupInAppButton(it, buttons[0], 0) }
        } else if (!buttons.isEmpty()) {
            for (i in buttons.indices) {
                if (i >= 2) {
                    continue  // only show 2 buttons
                }
                val inAppNotificationButton = buttons[i]
                val button = inAppButtons[i]
                setupInAppButton(button, inAppNotificationButton, i)
            }
        }

        fl.background = ColorDrawable(0xBB000000.toInt())

        setUpCloseButton(closeImageView)

        return inAppView
    }
}
