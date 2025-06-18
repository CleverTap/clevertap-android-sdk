package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
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

internal class CTInAppNativeHalfInterstitialImageFragment : CTInAppBaseFullFragment() {

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

        fl.background = ColorDrawable(-0x45000000)

        val relativeLayout =
            fl.findViewById<RelativeLayout>(R.id.half_interstitial_image_relative_layout)
        relativeLayout.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        val imageView = relativeLayout.findViewById<ImageView>(R.id.half_interstitial_image)
        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val layoutParams =
                            relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet()) {
                            redrawHalfInterstitialInApp(
                                relativeLayout, layoutParams, closeImageView
                            )
                        } else {
                            if (isTablet()) {
                                redrawHalfInterstitialMobileInAppOnTablet(
                                    relativeLayout, layoutParams, closeImageView
                                )
                            } else {
                                redrawHalfInterstitialInApp(
                                    relativeLayout, layoutParams, closeImageView
                                )
                            }
                        }

                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })

            Configuration.ORIENTATION_LANDSCAPE -> relativeLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val layoutParams =
                            relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (!inAppNotification.isTablet || !isTablet()) {
                            if (isTablet()) {
                                layoutParams.setMargins(
                                    getScaledPixels(140),
                                    getScaledPixels(100),
                                    getScaledPixels(140),
                                    getScaledPixels(100)
                                )
                                layoutParams.height =
                                    relativeLayout.measuredHeight - getScaledPixels(
                                        130
                                    )
                                layoutParams.width = (layoutParams.height * 1.3f).toInt()
                                layoutParams.gravity = Gravity.CENTER
                                relativeLayout.setLayoutParams(layoutParams)

                                Handler().post(object : Runnable {
                                    override fun run() {
                                        val margin = closeImageView.measuredWidth / 2
                                        closeImageView.x = (relativeLayout.right - margin).toFloat()
                                        closeImageView.y = (relativeLayout.top - margin).toFloat()
                                    }
                                })
                            } else {
                                layoutParams.width =
                                    (relativeLayout.measuredHeight * 1.3f).toInt()
                                layoutParams.gravity = Gravity.CENTER_HORIZONTAL
                                relativeLayout.setLayoutParams(layoutParams)
                                Handler().post(object : Runnable {
                                    override fun run() {
                                        val margin = closeImageView.measuredWidth / 2
                                        closeImageView.x = (relativeLayout.right - margin).toFloat()
                                        closeImageView.y = (relativeLayout.top - margin).toFloat()
                                    }
                                })
                            }
                        } else {
                            layoutParams.width = (relativeLayout.measuredHeight * 1.3f).toInt()
                            layoutParams.gravity = Gravity.CENTER
                            relativeLayout.setLayoutParams(layoutParams)
                            Handler().post(object : Runnable {
                                override fun run() {
                                    val margin = closeImageView.measuredWidth / 2
                                    closeImageView.x = (relativeLayout.right - margin).toFloat()
                                    closeImageView.y = (relativeLayout.top - margin).toFloat()
                                }
                            })
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
