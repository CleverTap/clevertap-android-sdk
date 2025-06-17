package com.clevertap.android.sdk.inapp

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeInterstitialImageFragment : CTInAppBaseFullFragment() {

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
        val relativeLayout: RelativeLayout =
            fl.findViewById<RelativeLayout>(R.id.interstitial_image_relative_layout)

        relativeLayout.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        val imageView = relativeLayout.findViewById<ImageView>(R.id.interstitial_image)

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
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

            Configuration.ORIENTATION_LANDSCAPE -> relativeLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
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

        val mediaForOrientation = inAppNotification.getInAppMediaForOrientation(currentOrientation)
        if (mediaForOrientation != null) {
            val bitmap = resourceProvider().cachedInAppImageV1(mediaForOrientation.mediaUrl)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.tag = 0
                imageView.setOnClickListener(CTInAppNativeButtonClickListener())
            }
        }
        closeImageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                didDismiss(null)
                activity?.finish()
            }
        })

        if (!inAppNotification.isHideCloseButton) {
            closeImageView.setVisibility(View.GONE)
        } else {
            closeImageView.setVisibility(View.VISIBLE)
        }

        return inAppView
    }
}
