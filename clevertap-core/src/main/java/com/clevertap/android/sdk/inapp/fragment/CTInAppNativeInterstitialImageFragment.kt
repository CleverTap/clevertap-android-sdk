package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.media.InAppMediaConfig
import com.clevertap.android.sdk.inapp.media.InAppMediaDelegate
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeInterstitialImageFragment : CTInAppBaseFullFragment() {

    private lateinit var mediaDelegate: InAppMediaDelegate
    private var relativeLayout: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaDelegate = InAppMediaDelegate(
            fragment = this,
            inAppNotification = inAppNotification,
            currentOrientation = currentOrientation,
            isTablet = inAppNotification.isTablet && isTablet(),
            resourceProvider = resourceProvider(),
            supportsStreamMedia = true
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inAppView = if (inAppNotification.isTablet && isTablet()) {
            inflater.inflate(R.layout.tab_inapp_interstitial_image, container, false)
        } else {
            inflater.inflate(R.layout.inapp_interstitial_image, container, false)
        }

        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_interstitial_image_frame_layout)
        fl.background = ColorDrawable(-0x45000000)

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)
        relativeLayout = fl.findViewById(R.id.interstitial_image_relative_layout)

        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
                        val layoutParams = relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet()) {
                            redrawInterstitialTabletInApp(
                                relativeLayout,
                                layoutParams,
                                fl,
                                closeImageView
                            )
                        } else {
                            if (isTablet()) {
                                redrawInterstitialMobileInAppOnTablet(
                                    relativeLayout,
                                    layoutParams,
                                    fl,
                                    closeImageView
                                )
                            } else {
                                redrawInterstitialInApp(
                                    relativeLayout,
                                    layoutParams,
                                    closeImageView
                                )
                            }
                        }
                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })

            Configuration.ORIENTATION_LANDSCAPE -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
                        val layoutParams = relativeLayout
                            .layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet()) {
                            redrawLandscapeInterstitialTabletInApp(
                                relativeLayout, layoutParams, fl,
                                closeImageView
                            )
                        } else {
                            if (isTablet()) {
                                redrawLandscapeInterstitialMobileInAppOnTablet(
                                    relativeLayout,
                                    layoutParams,
                                    fl,
                                    closeImageView
                                )
                            } else {
                                redrawLandscapeInterstitialInApp(
                                    relativeLayout,
                                    layoutParams,
                                    closeImageView
                                )
                            }
                        }

                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })
        }

        mediaDelegate.setMediaForInApp(
            relativeLayout,
            InAppMediaConfig(imageViewId = R.id.interstitial_image, clickableMedia = true, videoFrameId = R.id.video_frame),
            CTInAppNativeButtonClickListener()
        )

        closeImageView.setOnClickListener {
            didDismiss(null)
            mediaDelegate.clear()
            activity?.finish()
        }

        if (!inAppNotification.isHideCloseButton) {
            closeImageView.setVisibility(View.GONE)
        } else {
            closeImageView.setVisibility(View.VISIBLE)
        }

        return inAppView
    }

    override fun onStart() {
        super.onStart()
        mediaDelegate.onStart()
    }

    override fun onResume() {
        super.onResume()
        mediaDelegate.onResume()
    }

    override fun onPause() {
        super.onPause()
        mediaDelegate.onPause()
    }

    override fun onStop() {
        super.onStop()
        mediaDelegate.onStop()
    }

    override fun cleanup() {
        super.cleanup()
        mediaDelegate.cleanup()
    }
}
