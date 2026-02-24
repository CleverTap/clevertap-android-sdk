package com.clevertap.android.sdk.inapp.fragment


import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView

@UnstableApi
internal class CTInAppNativeInterstitialFragment : CTInAppBaseFullNativeFragment() {

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
        mediaDelegate.initVideoPlayerHandle()
    }

    @SuppressLint("ResourceType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        val inAppView = if (inAppNotification.isTablet && isTablet()) {
            inflater.inflate(R.layout.tab_inapp_interstitial, container, false)
        }
        else {
            inflater.inflate(R.layout.inapp_interstitial, container, false)
        }

        // Find views
        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_interstitial_frame_layout)
        closeImageView = fl.findViewById(CloseImageView.VIEW_ID)
        relativeLayout = fl.findViewById(R.id.interstitial_relative_layout)

        mediaDelegate.bindVideoFrame(relativeLayout?.findViewById(R.id.video_frame))

        // Container backgrounds
        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        fl.background = ColorDrawable(0xBB000000.toInt())

        // Container size
        resizeContainer(fl, closeImageView!!)

        // Inapps data binding
        mediaDelegate.setMediaForInApp(
            relativeLayout,
            InAppMediaConfig(
                imageViewId = R.id.backgroundImage,
                clickableMedia = false,
                useOrientationForImage = false,
                hideImageViewForNonImageMedia = false,
                fillVideoFrame = false
            )
        )
        setTitleAndMessage()
        setButtons()
        handleCloseButton()

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

    private fun handleCloseButton() {
        if (!inAppNotification.isHideCloseButton) {
            closeImageView?.setOnClickListener(null)
            closeImageView?.setVisibility(View.GONE)
        } else {
            closeImageView?.setVisibility(View.VISIBLE)
            closeImageView?.setOnClickListener {
                didDismiss(null)
                mediaDelegate.clearGif()
                activity?.finish()
            }
        }
    }

    private fun setButtons() {
        val buttonViews = mutableListOf<Button>()
        val linearLayout =
            relativeLayout?.findViewById<LinearLayout>(R.id.interstitial_linear_layout)
        val mainButton = linearLayout?.findViewById<Button>(R.id.interstitial_button1)
        mainButton?.let { buttonViews.add(it) }
        val secondaryButton = linearLayout?.findViewById<Button>(R.id.interstitial_button2)
        secondaryButton?.let { buttonViews.add(it) }

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
                    break // only show 2 buttons
                }
                val inAppNotificationButton = buttons[i]
                val button = buttonViews[i]
                setupInAppButton(button, inAppNotificationButton, i)
            }
        }
    }

    private fun setTitleAndMessage() {
        val textView1 = relativeLayout?.findViewById<TextView>(R.id.interstitial_title)
        textView1?.text = inAppNotification.title
        textView1?.setTextColor(inAppNotification.titleColor.toColorInt())

        val textView2 = relativeLayout?.findViewById<TextView>(R.id.interstitial_message)
        textView2?.text = inAppNotification.message
        textView2?.setTextColor(inAppNotification.messageColor.toColorInt())
    }

    private fun resizeContainer(fl: FrameLayout, closeImageView: CloseImageView) {
        when (currentOrientation) {
            Configuration.ORIENTATION_PORTRAIT -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
                        val layoutParams = relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet()) {
                            // tablet layout on tablet
                            redrawInterstitialTabletInApp(
                                relativeLayout, layoutParams, fl, closeImageView
                            )
                        } else {
                            // mobile layout
                            if (isTablet()) {
                                // mobile layout on tablet
                                redrawInterstitialMobileInAppOnTablet(
                                    relativeLayout, layoutParams, fl, closeImageView
                                )
                            } else {
                                // mobile layout on mobile
                                redrawInterstitialInApp(
                                    relativeLayout, layoutParams, closeImageView
                                )
                            }
                        }

                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })

            Configuration.ORIENTATION_LANDSCAPE -> relativeLayout?.getViewTreeObserver()
                ?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        val relativeLayout = relativeLayout ?: return
                        val layoutParams = relativeLayout.layoutParams as FrameLayout.LayoutParams
                        if (inAppNotification.isTablet && isTablet()) {
                            // tablet layout on tablet
                            redrawLandscapeInterstitialTabletInApp(
                                relativeLayout, layoutParams, fl, closeImageView
                            )
                        } else {
                            // mobile layout
                            if (isTablet()) {
                                // mobile layout on tablet
                                redrawLandscapeInterstitialMobileInAppOnTablet(
                                    relativeLayout, layoutParams, fl, closeImageView
                                )
                            } else {
                                // mobile layout on mobile
                                redrawLandscapeInterstitialInApp(
                                    relativeLayout, layoutParams, closeImageView
                                )
                            }
                        }
                        relativeLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                    }
                })
        }
    }
}
