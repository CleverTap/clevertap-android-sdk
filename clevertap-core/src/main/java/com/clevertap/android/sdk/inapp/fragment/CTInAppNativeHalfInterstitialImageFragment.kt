package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView

@UnstableApi
internal class CTInAppNativeHalfInterstitialImageFragment : CTInAppBaseFullFragment() {

    private lateinit var mediaDelegate: InAppMediaDelegate
    private var relativeLayout: RelativeLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaDelegate = InAppMediaDelegate(
            fragment = this,
            inAppNotification = inAppNotification,
            currentOrientation = currentOrientation,
            isTablet = inAppNotification.isTablet && isTablet(),
            resourceProvider = resourceProvider()
        )
        mediaDelegate.onCreate()
    }

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

        relativeLayout = fl.findViewById(R.id.half_interstitial_image_relative_layout)
        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        mediaDelegate.bindVideoFrame(relativeLayout?.findViewById(R.id.video_frame))

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
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

            Configuration.ORIENTATION_LANDSCAPE -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
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

                                relativeLayout.post {
                                    val margin = closeImageView.measuredWidth / 2
                                    closeImageView.x = (relativeLayout.right - margin).toFloat()
                                    closeImageView.y = (relativeLayout.top - margin).toFloat()
                                }
                            } else {
                                layoutParams.width =
                                    (relativeLayout.measuredHeight * 1.3f).toInt()
                                layoutParams.gravity = Gravity.CENTER_HORIZONTAL
                                relativeLayout.setLayoutParams(layoutParams)
                                relativeLayout.post {
                                    val margin = closeImageView.measuredWidth / 2
                                    closeImageView.x = (relativeLayout.right - margin).toFloat()
                                    closeImageView.y = (relativeLayout.top - margin).toFloat()
                                }
                            }
                        } else {
                            layoutParams.width = (relativeLayout.measuredHeight * 1.3f).toInt()
                            layoutParams.gravity = Gravity.CENTER
                            relativeLayout.setLayoutParams(layoutParams)
                            relativeLayout.post {
                                val margin = closeImageView.measuredWidth / 2
                                closeImageView.x = (relativeLayout.right - margin).toFloat()
                                closeImageView.y = (relativeLayout.top - margin).toFloat()
                            }
                        }

                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })
        }

        mediaDelegate.setMediaForInApp(
            relativeLayout,
            InAppMediaConfig(imageViewId = R.id.half_interstitial_image, clickableMedia = true),
            CTInAppNativeButtonClickListener()
        )

        closeImageView.setOnClickListener {
            didDismiss(null)
            mediaDelegate.clearGif()
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
