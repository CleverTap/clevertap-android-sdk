package com.clevertap.android.sdk.inapp.fragment

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeHalfInterstitialFragment : CTInAppBaseFullNativeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val inAppButtons = mutableListOf<Button>()
        val inAppView: View =
            if (inAppNotification.isTablet && isTablet() || inAppNotification.isLocalInApp && isTabletFromDeviceType(
                    inflater.context
                )
            ) {
                inflater.inflate(R.layout.tab_inapp_half_interstitial, container, false)
            } else {
                inflater.inflate(R.layout.inapp_half_interstitial, container, false)
            }

        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_half_interstitial_frame_layout)

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        val relativeLayout = fl.findViewById<RelativeLayout>(R.id.half_interstitial_relative_layout)
        relativeLayout.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val layoutParams = relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet() || inAppNotification.isLocalInApp && isTabletFromDeviceType(
                                inflater.context
                            )
                        ) {
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
                        val layoutParams = relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (!inAppNotification.isTablet || !isTablet()) {
                            if (isTablet()) {
                                layoutParams.setMargins(
                                    getScaledPixels(140),
                                    getScaledPixels(100),
                                    getScaledPixels(140),
                                    getScaledPixels(100)
                                )
                                layoutParams.height =
                                    relativeLayout.measuredHeight - getScaledPixels(130)
                                layoutParams.width = (layoutParams.height * 1.3f).toInt()
                                layoutParams.gravity = Gravity.CENTER
                                relativeLayout.setLayoutParams(layoutParams)

                                relativeLayout.post(object : Runnable {
                                    override fun run() {
                                        val margin = closeImageView.measuredWidth / 2
                                        closeImageView.x = (relativeLayout.right - margin).toFloat()
                                        closeImageView.y = (relativeLayout.top - margin).toFloat()
                                    }
                                })
                            } else {
                                layoutParams.width = (relativeLayout.measuredHeight * 1.3f).toInt()
                                layoutParams.gravity = Gravity.CENTER_HORIZONTAL
                                relativeLayout.setLayoutParams(layoutParams)
                                relativeLayout.post(object : Runnable {
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
                            relativeLayout.post(object : Runnable {
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
                val imageView = relativeLayout.findViewById<ImageView>(R.id.backgroundImage)
                imageView.setImageBitmap(bitmap)
            }
        }

        val linearLayout =
            relativeLayout.findViewById<LinearLayout>(R.id.half_interstitial_linear_layout)
        val mainButton = linearLayout.findViewById<Button>(R.id.half_interstitial_button1)
        inAppButtons.add(mainButton)
        val secondaryButton = linearLayout.findViewById<Button>(R.id.half_interstitial_button2)
        inAppButtons.add(secondaryButton)

        val textView1 = relativeLayout.findViewById<TextView>(R.id.half_interstitial_title)
        textView1.text = inAppNotification.title
        textView1.setTextColor(inAppNotification.titleColor.toColorInt())

        val textView2 = relativeLayout.findViewById<TextView>(R.id.half_interstitial_message)
        textView2.text = inAppNotification.message
        textView2.setTextColor(inAppNotification.messageColor.toColorInt())

        val buttons = inAppNotification.buttons
        if (buttons.size == 1) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mainButton.visibility = View.GONE
            } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mainButton.visibility = View.INVISIBLE
            }
            setupInAppButton(secondaryButton, buttons.get(0), 0)
        } else if (!buttons.isEmpty()) {
            for (i in buttons.indices) {
                if (i >= 2) {
                    continue  // only show 2 buttons
                }
                val inAppNotificationButton = buttons.get(i)
                val button = inAppButtons.get(i)
                setupInAppButton(button, inAppNotificationButton, i)
            }
        }

        fl.background = ColorDrawable(-0x45000000)

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

    fun isTabletFromDeviceType(context: Context?): Boolean {
        return DeviceInfo.getDeviceType(context) == DeviceInfo.TABLET
    }
}
