package com.clevertap.android.sdk.inapp.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.toColorInt
import androidx.media3.common.util.UnstableApi
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.gif.GifImageView
import com.clevertap.android.sdk.inapp.pip.PiPCornerPosition
import com.clevertap.android.sdk.inapp.pip.PiPEntryAnimation
import com.clevertap.android.sdk.inapp.pip.PiPSizePreset
import com.clevertap.android.sdk.video.InAppVideoPlayerHandle
import com.clevertap.android.sdk.video.VideoLibChecker
import com.clevertap.android.sdk.video.VideoLibraryIntegrated
import com.clevertap.android.sdk.video.inapps.ExoplayerHandle
import com.clevertap.android.sdk.video.inapps.Media3Handle
import kotlin.math.sqrt

@UnstableApi
internal class CTInAppNativePiPFragment : CTInAppBasePartialFragment() {

    // State
    private var isExpanded = false

    // Views - PiP compact
    private var pipContainer: FrameLayout? = null
    private var pipImage: ImageView? = null
    private var pipGif: GifImageView? = null
    private var pipVideoFrame: FrameLayout? = null
    private var pipCloseButton: ImageView? = null
    private var pipExpandButton: ImageView? = null
    private var pipRedirectButton: ImageView? = null

    // Views - Expanded
    private var expandedContainer: FrameLayout? = null
    private var expandedImage: ImageView? = null
    private var expandedGif: GifImageView? = null
    private var expandedVideoFrame: FrameLayout? = null
    private var expandedTitle: TextView? = null
    private var expandedMessage: TextView? = null
    private var expandedCollapseButton: ImageView? = null
    private var expandedButton1: Button? = null
    private var expandedButton2: Button? = null

    // Video
    private var videoHandle: InAppVideoPlayerHandle? = null
    private var hasVideo = false

    // Drag state
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f

    // Config
    private lateinit var sizePreset: PiPSizePreset
    private lateinit var cornerPosition: PiPCornerPosition
    private lateinit var entryAnimation: PiPEntryAnimation
    private var edgePaddingPx = 0
    private var touchSlop = 0

    companion object {
        private const val SNAP_ANIMATION_DURATION_MS = 250L
        private const val EXPAND_ANIMATION_DURATION_MS = 200L
        private const val ENTRY_ANIMATION_DURATION_MS = 400L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.inapp_pip, container, false)

        // Parse PiP config
        sizePreset = PiPSizePreset.fromString(inAppNotification.pipSizePreset)
        cornerPosition = PiPCornerPosition.fromString(inAppNotification.pipCornerPosition)
        entryAnimation = PiPEntryAnimation.fromString(inAppNotification.pipAnimation)
        edgePaddingPx = getScaledPixels(16)
        touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        // Find PiP compact views
        pipContainer = rootView.findViewById(R.id.pip_container)
        pipImage = rootView.findViewById(R.id.pip_image)
        pipGif = rootView.findViewById(R.id.pip_gif)
        pipVideoFrame = rootView.findViewById(R.id.pip_video_frame)
        pipCloseButton = rootView.findViewById(R.id.pip_close)
        pipExpandButton = rootView.findViewById(R.id.pip_expand)
        pipRedirectButton = rootView.findViewById(R.id.pip_redirect)

        // Find expanded views
        expandedContainer = rootView.findViewById(R.id.pip_expanded_container)
        expandedImage = rootView.findViewById(R.id.pip_expanded_image)
        expandedGif = rootView.findViewById(R.id.pip_expanded_gif)
        expandedVideoFrame = rootView.findViewById(R.id.pip_expanded_video_frame)
        expandedTitle = rootView.findViewById(R.id.pip_expanded_title)
        expandedMessage = rootView.findViewById(R.id.pip_expanded_message)
        expandedCollapseButton = rootView.findViewById(R.id.pip_collapse)
        expandedButton1 = rootView.findViewById(R.id.pip_expanded_button1)
        expandedButton2 = rootView.findViewById(R.id.pip_expanded_button2)

        // Initially show PiP, hide expanded
        expandedContainer?.visibility = View.GONE
        pipContainer?.visibility = View.VISIBLE

        // Apply PiP size
        applyPiPSize()

        // Set up content
        setupPiPMedia()
        setupExpandedContent()
        setupControls()
        setupDragListener()

        // Position will be applied after layout
        pipContainer?.post {
            applyInitialPosition()
            applyEntryAnimation()
        }

        return rootView
    }

    override fun onStart() {
        super.onStart()
        // Restart GIF if needed
        val media = inAppNotification.mediaList.firstOrNull() ?: return
        if (media.isGIF()) {
            val gifBytes = resourceProvider().cachedInAppGifV1(media.mediaUrl)
            if (gifBytes != null) {
                if (isExpanded) {
                    expandedGif?.setBytes(gifBytes)
                    expandedGif?.startAnimation()
                } else {
                    pipGif?.setBytes(gifBytes)
                    pipGif?.startAnimation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasVideo && videoHandle != null) {
            prepareAndPlayVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        pipGif?.clear()
        expandedGif?.clear()
        videoHandle?.savePosition()
        videoHandle?.pause()
    }

    override fun onStop() {
        super.onStop()
        pipGif?.clear()
        expandedGif?.clear()
        videoHandle?.pause()
    }

    override fun cleanup() {
        super.cleanup()
        pipGif?.clear()
        expandedGif?.clear()
        videoHandle?.pause()
    }

    // =========================================================================
    // PiP Size
    // =========================================================================

    private fun applyPiPSize() {
        val widthPx = dpToPx(sizePreset.widthDp)
        val heightPx = dpToPx(sizePreset.heightDp)
        pipContainer?.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx)
    }

    // =========================================================================
    // Initial Position
    // =========================================================================

    private fun applyInitialPosition() {
        val container = pipContainer ?: return
        val parent = container.parent as? View ?: return

        val targetX: Float
        val targetY: Float

        when (cornerPosition) {
            PiPCornerPosition.TOP_LEFT -> {
                targetX = edgePaddingPx.toFloat()
                targetY = edgePaddingPx.toFloat()
            }
            PiPCornerPosition.TOP_RIGHT -> {
                targetX = (parent.width - container.width - edgePaddingPx).toFloat()
                targetY = edgePaddingPx.toFloat()
            }
            PiPCornerPosition.BOTTOM_LEFT -> {
                targetX = edgePaddingPx.toFloat()
                targetY = (parent.height - container.height - edgePaddingPx).toFloat()
            }
            PiPCornerPosition.BOTTOM_RIGHT -> {
                targetX = (parent.width - container.width - edgePaddingPx).toFloat()
                targetY = (parent.height - container.height - edgePaddingPx).toFloat()
            }
        }

        container.x = targetX
        container.y = targetY
    }

    // =========================================================================
    // Entry Animation
    // =========================================================================

    private fun applyEntryAnimation() {
        val container = pipContainer ?: return

        when (entryAnimation) {
            PiPEntryAnimation.INSTANT -> {
                // No animation, already visible
            }
            PiPEntryAnimation.DISSOLVE -> {
                container.alpha = 0f
                container.animate()
                    .alpha(1f)
                    .setDuration(ENTRY_ANIMATION_DURATION_MS)
                    .start()
            }
            PiPEntryAnimation.MOVE_IN -> {
                val parent = container.parent as? View ?: return
                val finalX = container.x
                val finalY = container.y

                // Start from outside the nearest screen edge
                val startX = when (cornerPosition) {
                    PiPCornerPosition.TOP_RIGHT, PiPCornerPosition.BOTTOM_RIGHT ->
                        parent.width.toFloat()
                    else -> -container.width.toFloat()
                }

                container.x = startX
                container.animate()
                    .x(finalX)
                    .setDuration(ENTRY_ANIMATION_DURATION_MS)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        }
    }

    // =========================================================================
    // Media Setup
    // =========================================================================

    private fun setupPiPMedia() {
        val media = inAppNotification.mediaList.firstOrNull()
        if (media == null) {
            return
        }

        when {
            media.isImage() -> {
                val image = resourceProvider().cachedInAppImageV1(media.mediaUrl)
                if (image != null) {
                    pipImage?.setImageBitmap(image)
                    pipImage?.visibility = View.VISIBLE
                }
            }
            media.isGIF() -> {
                val gifBytes = resourceProvider().cachedInAppGifV1(media.mediaUrl)
                if (gifBytes != null) {
                    pipGif?.setBytes(gifBytes)
                    pipGif?.startAnimation()
                    pipGif?.visibility = View.VISIBLE
                }
            }
            media.isVideo() -> {
                if (VideoLibChecker.haveVideoPlayerSupport) {
                    hasVideo = true
                    initVideoHandle()
                    prepareAndPlayVideo()
                } else {
                    config.logger.debug("Video not supported for PiP, skipping media")
                }
            }
        }
    }

    private fun setupExpandedMedia() {
        val media = inAppNotification.mediaList.firstOrNull() ?: return

        when {
            media.isImage() -> {
                val image = resourceProvider().cachedInAppImageV1(media.mediaUrl)
                if (image != null) {
                    expandedImage?.setImageBitmap(image)
                    expandedImage?.visibility = View.VISIBLE
                }
            }
            media.isGIF() -> {
                val gifBytes = resourceProvider().cachedInAppGifV1(media.mediaUrl)
                if (gifBytes != null) {
                    expandedGif?.setBytes(gifBytes)
                    expandedGif?.startAnimation()
                    expandedGif?.visibility = View.VISIBLE
                }
            }
            media.isVideo() -> {
                if (hasVideo) {
                    expandedVideoFrame?.visibility = View.VISIBLE
                }
            }
        }
    }

    // =========================================================================
    // Video
    // =========================================================================

    private fun initVideoHandle() {
        videoHandle = if (VideoLibChecker.mediaLibType == VideoLibraryIntegrated.MEDIA3) {
            Media3Handle()
        } else {
            ExoplayerHandle()
        }
    }

    private fun prepareAndPlayVideo() {
        val handle = videoHandle ?: return
        val media = inAppNotification.mediaList.firstOrNull() ?: return
        if (!media.isVideo()) return

        handle.initPlayerView(requireContext(), false)
        val videoFrame = if (isExpanded) expandedVideoFrame else pipVideoFrame
        videoFrame?.visibility = View.VISIBLE

        val surface = handle.videoSurface()
        val currentParent = surface.parent as? ViewGroup
        if (currentParent != videoFrame) {
            currentParent?.removeView(surface)
            videoFrame?.addView(surface)
        }

        handle.initExoplayer(requireContext(), media.mediaUrl)
        handle.play()
    }

    // =========================================================================
    // Expanded Content
    // =========================================================================

    private fun setupExpandedContent() {
        // Title
        val title = inAppNotification.title
        if (!title.isNullOrEmpty()) {
            expandedTitle?.text = title
            expandedTitle?.setTextColor(inAppNotification.titleColor.toColorInt())
            expandedTitle?.visibility = View.VISIBLE
        } else {
            expandedTitle?.visibility = View.GONE
        }

        // Message
        val message = inAppNotification.message
        if (!message.isNullOrEmpty()) {
            expandedMessage?.text = message
            expandedMessage?.setTextColor(inAppNotification.messageColor.toColorInt())
            expandedMessage?.visibility = View.VISIBLE
        } else {
            expandedMessage?.visibility = View.GONE
        }

        // Background - use GradientDrawable to preserve rounded corners
        val expandedContent = expandedContainer?.findViewById<View>(R.id.pip_expanded_content)
        val bgDrawable = GradientDrawable()
        bgDrawable.setColor(inAppNotification.backgroundColor.toColorInt())
        bgDrawable.cornerRadius = dpToPx(12).toFloat()
        expandedContent?.background = bgDrawable

        // Buttons
        setupExpandedButtons()

        // Set up media for expanded view
        setupExpandedMedia()
    }

    private fun setupExpandedButtons() {
        val buttons = inAppNotification.buttons
        if (buttons.isEmpty()) return

        for (i in buttons.indices) {
            if (i >= 2) break
            val btn = buttons[i]
            val buttonView = if (i == 0) expandedButton1 else expandedButton2
            if (buttonView != null) {
                buttonView.visibility = View.VISIBLE
                buttonView.tag = i
                buttonView.text = btn.text
                buttonView.setTextColor(btn.textColor.toColorInt())
                buttonView.setBackgroundColor(btn.backgroundColor.toColorInt())
                buttonView.setOnClickListener(CTInAppNativeButtonClickListener())
            }
        }
    }

    // =========================================================================
    // Controls
    // =========================================================================

    private fun setupControls() {
        // Close button
        pipCloseButton?.setOnClickListener {
            didDismiss(null)
        }

        // Expand button
        pipExpandButton?.setOnClickListener {
            expandToFullScreen()
        }

        // Collapse button
        expandedCollapseButton?.setOnClickListener {
            collapseToMiniPiP()
        }

        // Redirect/CTA button on PiP
        val buttons = inAppNotification.buttons
        if (buttons.isNotEmpty()) {
            pipRedirectButton?.visibility = View.VISIBLE
            pipRedirectButton?.setOnClickListener {
                handleButtonClickAtIndex(0)
            }
        }

        // Close on expanded scrim tap (outside content area)
        expandedContainer?.setOnClickListener {
            collapseToMiniPiP()
        }
        // Prevent clicks on expanded content from closing
        expandedContainer?.findViewById<View>(R.id.pip_expanded_content)?.setOnClickListener {
            // consume click
        }
    }

    // =========================================================================
    // Drag & Snap
    // =========================================================================

    private fun setupDragListener() {
        pipContainer?.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val parentView = view.parent as? View ?: return@setOnTouchListener false
                    val newX = (event.rawX + dX).coerceIn(
                        0f,
                        (parentView.width - view.width).toFloat()
                    )
                    val newY = (event.rawY + dY).coerceIn(
                        0f,
                        (parentView.height - view.height).toFloat()
                    )
                    view.x = newX
                    view.y = newY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    if (distance < touchSlop) {
                        // This was a tap, not a drag - fire body CTA
                        handlePiPBodyTap()
                    } else {
                        snapToNearestCorner()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun handlePiPBodyTap() {
        val buttons = inAppNotification.buttons
        if (buttons.isNotEmpty()) {
            val action = buttons[0].action
            if (action != null) {
                triggerAction(action, buttons[0].text, null)
            }
        }
    }

    private fun snapToNearestCorner() {
        val view = pipContainer ?: return
        val parent = view.parent as? View ?: return

        val centerX = view.x + view.width / 2
        val centerY = view.y + view.height / 2
        val parentCenterX = parent.width / 2f
        val parentCenterY = parent.height / 2f

        val targetX = if (centerX < parentCenterX) {
            edgePaddingPx.toFloat()
        } else {
            (parent.width - view.width - edgePaddingPx).toFloat()
        }
        val targetY = if (centerY < parentCenterY) {
            edgePaddingPx.toFloat()
        } else {
            (parent.height - view.height - edgePaddingPx).toFloat()
        }

        val animX = ObjectAnimator.ofFloat(view, "x", view.x, targetX)
        val animY = ObjectAnimator.ofFloat(view, "y", view.y, targetY)
        val animSet = AnimatorSet()
        animSet.playTogether(animX, animY)
        animSet.duration = SNAP_ANIMATION_DURATION_MS
        animSet.interpolator = DecelerateInterpolator()
        animSet.start()
    }

    // =========================================================================
    // Expand / Collapse
    // =========================================================================

    private fun expandToFullScreen() {
        if (isExpanded) return
        isExpanded = true

        // Transfer video surface from PiP to expanded container
        if (hasVideo && videoHandle != null) {
            val surface = videoHandle!!.videoSurface()
            (surface.parent as? ViewGroup)?.removeView(surface)
            expandedVideoFrame?.visibility = View.VISIBLE
            expandedVideoFrame?.addView(surface)
        }

        // Cross-fade PiP out, expanded in
        pipContainer?.animate()
            ?.alpha(0f)
            ?.setDuration(EXPAND_ANIMATION_DURATION_MS)
            ?.withEndAction {
                pipContainer?.visibility = View.GONE
            }
            ?.start()

        expandedContainer?.alpha = 0f
        expandedContainer?.visibility = View.VISIBLE
        expandedContainer?.animate()
            ?.alpha(1f)
            ?.setDuration(EXPAND_ANIMATION_DURATION_MS)
            ?.start()
    }

    private fun collapseToMiniPiP() {
        if (!isExpanded) return
        isExpanded = false

        // Transfer video surface back to PiP container
        if (hasVideo && videoHandle != null) {
            val surface = videoHandle!!.videoSurface()
            (surface.parent as? ViewGroup)?.removeView(surface)
            pipVideoFrame?.visibility = View.VISIBLE
            pipVideoFrame?.addView(surface)
        }

        expandedContainer?.animate()
            ?.alpha(0f)
            ?.setDuration(EXPAND_ANIMATION_DURATION_MS)
            ?.withEndAction {
                expandedContainer?.visibility = View.GONE
            }
            ?.start()

        pipContainer?.alpha = 0f
        pipContainer?.visibility = View.VISIBLE
        pipContainer?.animate()
            ?.alpha(1f)
            ?.setDuration(EXPAND_ANIMATION_DURATION_MS)
            ?.start()
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }
}
