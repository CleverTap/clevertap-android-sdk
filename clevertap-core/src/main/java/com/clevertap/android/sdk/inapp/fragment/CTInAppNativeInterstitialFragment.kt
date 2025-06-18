package com.clevertap.android.sdk.inapp.fragment


import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import androidx.core.view.size
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.customviews.CloseImageView
import com.clevertap.android.sdk.gif.GifImageView
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inapps.ExoplayerHandle
import com.clevertap.android.sdk.video.inapps.Media3Handle

@UnstableApi
internal class CTInAppNativeInterstitialFragment : CTInAppBaseFullNativeFragment() {

    private var exoPlayerFullscreen = false
    private var fullScreenDialog: ComponentDialog? = null
    private var fullScreenIcon: ImageView? = null
    private var gifImageView: GifImageView? = null
    private lateinit var handle: InAppVideoPlayerHandle
    private var relativeLayout: RelativeLayout? = null
    private var videoFrameLayout: FrameLayout? = null
    private var videoFrameInDialog: FrameLayout? = null

    private var imageViewLayoutParams: ViewGroup.LayoutParams? = null
    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (exoPlayerFullscreen) {
                closeFullscreenDialog()
                isEnabled = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle = if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
            Media3Handle()
        } else {
            ExoplayerHandle()
        }
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
        videoFrameLayout = relativeLayout?.findViewById(R.id.video_frame)

        // Container backgrounds
        relativeLayout?.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        fl.background = ColorDrawable(0xBB000000.toInt())

        // Container size
        resizeContainer(fl, closeImageView!!)

        // Inapps data binding
        setMediaForInApp()
        setTitleAndMessage()
        setButtons()
        handleCloseButton()

        return inAppView
    }

    override fun onStart() {
        super.onStart()
        gifImageView?.let { gifImageView ->

            val inAppMedia = inAppNotification.mediaList.firstOrNull()
            if (inAppMedia == null) {
                return
            }
            gifImageView.setBytes(resourceProvider().cachedInAppGifV1(inAppMedia.mediaUrl))
            gifImageView.startAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (inAppNotification.hasStreamMedia()) {
            prepareMedia()
            playMedia()
        }
    }

    override fun onPause() {
        super.onPause()
        gifImageView?.clear()
        if (exoPlayerFullscreen) {
            closeFullscreenDialog()
            onBackPressedCallback.isEnabled = false
        }
        handle.savePosition()
        handle.pause()
    }

    override fun onStop() {
        super.onStop()
        gifImageView?.clear()
        handle.pause()
    }

    override fun cleanup() {
        super.cleanup()
        gifImageView?.clear()
        handle.pause()
    }

    private fun handleCloseButton() {
        if (!inAppNotification.isHideCloseButton) {
            closeImageView?.setOnClickListener(null)
            closeImageView?.setVisibility(View.GONE)
        } else {
            closeImageView?.setVisibility(View.VISIBLE)
            closeImageView?.setOnClickListener {
                didDismiss(null)
                gifImageView?.clear()
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

    private fun setMediaForInApp() {
        if (inAppNotification.mediaList.isNotEmpty()) {
            val media = inAppNotification.mediaList[0]
            if (media.isImage()) {
                val image = resourceProvider().cachedInAppImageV1(media.mediaUrl)
                if (image != null) {
                    val imageView = relativeLayout?.findViewById<ImageView>(R.id.backgroundImage)
                    imageView?.setVisibility(View.VISIBLE)
                    imageView?.setImageBitmap(image)
                }
            } else if (media.isGIF()) {
                val gifByteArray = resourceProvider().cachedInAppGifV1(media.mediaUrl)
                if (gifByteArray != null) {
                    gifImageView = relativeLayout?.findViewById(R.id.gifImage)
                    gifImageView?.setVisibility(View.VISIBLE)
                    gifImageView?.setBytes(gifByteArray)
                    gifImageView?.startAnimation()
                }
            } else if (media.isVideo()) {
                initFullScreenIconForStream()
                prepareMedia()
                playMedia()
            } else if (media.isAudio()) {
                initFullScreenIconForStream()
                prepareMedia()
                playMedia()
                disableFullScreenButton()
            }
        }
    }

    private fun initFullScreenIconForStream() {
        // inflate full screen icon for video control
        val fullScreenIcon = ImageView(requireContext())
        this.fullScreenIcon = fullScreenIcon
        fullScreenIcon.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.ct_ic_fullscreen_expand, null
            )
        )
        fullScreenIcon.setOnClickListener {
            if (!exoPlayerFullscreen) {
                onBackPressedCallback.isEnabled = true
                openFullscreenDialog()
            } else {
                closeFullscreenDialog()
                onBackPressedCallback.isEnabled = false
            }
        }

        // icon layout params wrt tablet/phone
        val displayMetrics = resources.displayMetrics

        val iconSide = if (inAppNotification.isTablet && isTablet()) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, displayMetrics).toInt()
        } else {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20f, displayMetrics).toInt()
        }
        val iconTop =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
        val iconRight =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, displayMetrics).toInt()
        val layoutParams = FrameLayout.LayoutParams(iconSide, iconSide)
        layoutParams.gravity = Gravity.END
        layoutParams.setMargins(0, iconTop, iconRight, 0)
        fullScreenIcon.setLayoutParams(layoutParams)
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

    private fun disableFullScreenButton() {
        fullScreenIcon?.setVisibility(View.GONE)
    }

    private fun closeFullscreenDialog() {
        val playerView = handle.videoSurface()

        handle.switchToFullScreen(false)

        fullScreenIcon?.setLayoutParams(imageViewLayoutParams)
        videoFrameInDialog?.removeAllViews()
        videoFrameLayout?.addView(playerView)
        videoFrameLayout?.addView(fullScreenIcon)
        exoPlayerFullscreen = false
        // dismiss full screen dialog
        fullScreenDialog?.dismiss()
        fullScreenIcon?.setImageDrawable(
            ContextCompat.getDrawable(requireContext(), R.drawable.ct_ic_fullscreen_expand)
        )
    }

    private fun openFullscreenDialog() {
        val playerView = handle.videoSurface()

        imageViewLayoutParams = fullScreenIcon?.layoutParams
        handle.switchToFullScreen(true)

        // clear views from inapp container
        videoFrameLayout?.removeAllViews()

        if (fullScreenDialog == null) {
            // create only once
            // create full screen dialog and show
            val fullScreenDialog =
                ComponentDialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            this.fullScreenDialog = fullScreenDialog
            val fullScreenParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            )
            val videoFrameInDialog = FrameLayout(requireContext())
            this.videoFrameInDialog = videoFrameInDialog
            fullScreenDialog.addContentView(videoFrameInDialog, fullScreenParams)

            val activity = getActivity()
            if (activity != null) {
                fullScreenDialog.onBackPressedDispatcher.addCallback(
                    activity, onBackPressedCallback
                )
            }
        }

        videoFrameInDialog?.addView(playerView)
        exoPlayerFullscreen = true
        fullScreenDialog?.show()
    }

    private fun playMedia() {
        handle.play()
    }

    private fun prepareMedia() {
        handle.initPlayerView(requireContext(), inAppNotification.isTablet && isTablet())
        addViewsForStreamMedia()

        handle.initExoplayer(requireContext(), inAppNotification.mediaList[0].mediaUrl)
    }

    private fun addViewsForStreamMedia() {
        // make video container visible
        videoFrameLayout?.visibility = View.VISIBLE

        // add views to video container
        val videoSurface = handle.videoSurface()

        if (videoFrameLayout?.size == 0) {
            videoFrameLayout?.addView(videoSurface)
            videoFrameLayout?.addView(fullScreenIcon)
        } else {
            //noop
            Logger.d("Video views and controls are already added, not re-attaching")
        }
    }
}
