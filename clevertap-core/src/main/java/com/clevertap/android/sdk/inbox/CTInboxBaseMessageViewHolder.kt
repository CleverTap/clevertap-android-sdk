package com.clevertap.android.sdk.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date

internal open class CTInboxBaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var context: Context? = null

    var ctaLinearLayout: LinearLayout? = null

    var bodyRelativeLayout: LinearLayout? = null

    var frameLayout: FrameLayout? = null

    var mediaImage: ImageView? = null

    var squareImage: ImageView? = null

    var defaultImage: ImageView? = null

    var mediaLayout: RelativeLayout? = null

    var progressBarFrameLayout: FrameLayout? = null

    var relativeLayout: RelativeLayout? = null

    var clickLayout: RelativeLayout? = null

    private var firstContentItem: CTInboxMessageContent? = null

    private var message: CTInboxMessage? = null

    private var muteIcon: ImageView? = null

    private var parentWeakReference: WeakReference<CTInboxListViewFragment>? = null

    private var requiresMediaPlayer = false

    protected val readDot: ImageView? = itemView.findViewById(R.id.read_circle)

    open fun addMediaPlayer(
        currentVolume: Float,
        muteClick: Function0<Float>,
        playMedia: Function3<String, Boolean, Boolean, Void?>,
        videoSurfaceView: View
    ): Boolean {
        if (!requiresMediaPlayer) {
            return false
        }
        val frameLayout = getLayoutForMediaPlayer() ?: return false
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE // Gets set visible in playerReady

        val context = context!!
        val message = message!!
        val firstContentItem = firstContentItem!!
        val resources = context.resources
        val displayMetrics = resources.displayMetrics

        var width = 0
        var height = 0
        if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (message.orientation!!.equals("l", ignoreCase = true)) {
                width = Math.round(this.mediaImage!!.measuredHeight * 1.76f)
                height = this.mediaImage!!.measuredHeight
            } else if (message.orientation!!.equals("p", ignoreCase = true)) {
                height = this.squareImage!!.measuredHeight
                width = height
            } else {
                if (this.defaultImage != null) {
                    width = this.defaultImage!!.measuredWidth
                    height = this.defaultImage!!.measuredHeight
                }
                if (this.defaultImage == null || width == 0 || height == 0) {
                    width = resources.displayMetrics.widthPixels / 2
                    height = Math.round(width * 0.5625f)
                }
            }
        } else {
            width = resources.displayMetrics.widthPixels
            if (message.orientation!!.equals("l", ignoreCase = true)) {
                height = Math.round(width * 0.5625f)
            } else if (message.orientation!!.equals("p", ignoreCase = true)) {
                height = width
            } else {
                if (this.defaultImage != null) {
                    height = this.defaultImage!!.measuredHeight
                }
                if (this.defaultImage == null || height == 0) {
                    height = Math.round(width * 0.5625f)
                }
            }
        }

        videoSurfaceView.layoutParams = FrameLayout.LayoutParams(width, height)

        // The shared player view may still sit in another row; a view can have only one parent.
        val previousParent = videoSurfaceView.parent as ViewGroup?
        previousParent?.removeView(videoSurfaceView)
        frameLayout.addView(videoSurfaceView)
        frameLayout.setBackgroundColor(Color.parseColor(message.bgColor))

        if (progressBarFrameLayout != null) {
            progressBarFrameLayout!!.visibility = View.VISIBLE
        }
        if (firstContentItem.mediaIsVideo()) {
            val muteIcon = ImageView(context)
            this.muteIcon = muteIcon
            muteIcon.visibility = View.GONE

            // Initial state setup
            setMuteIconState(muteIcon, context, currentVolume)

            // 48dp tap target; 9dp padding keeps the visual icon at 30dp
            val iconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, displayMetrics).toInt()
            val iconPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9f, displayMetrics).toInt()
            muteIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            val layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
            val iconTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
            val iconRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, displayMetrics).toInt()
            layoutParams.setMargins(0, iconTop, iconRight, 0)
            layoutParams.gravity = Gravity.END
            muteIcon.layoutParams = layoutParams

            val updateMuteIcon = View.OnClickListener {
                val volume = muteClick.invoke()
                setMuteIconState(this.muteIcon!!, this.context!!, volume)
            }
            muteIcon.setOnClickListener(updateMuteIcon)
            frameLayout.addView(muteIcon)
        }

        playMedia.invoke(firstContentItem.media!!, firstContentItem.mediaIsAudio(), firstContentItem.mediaIsVideo())
        return true
    }

    private fun setMuteIconState(icon: ImageView, context: Context, volume: Float) {
        val isMuted = volume <= 0
        val drawableRes = if (isMuted) R.drawable.ct_volume_off else R.drawable.ct_volume_on
        // When muted, the action available is "Unmute"; when audible, the action is "Mute"
        val contentDescRes = if (isMuted) {
            R.string.ct_unmute_button_content_description
        } else {
            R.string.ct_mute_button_content_description
        }

        icon.contentDescription = context.getString(contentDescRes)
        icon.setImageDrawable(ResourcesCompat.getDrawable(context.resources, drawableRes, null))
    }

    /**
     * Logic for timestamp
     *
     * @param time Epoch date of creation
     * @return String timestamp
     */
    fun calculateDisplayTimestamp(time: Long): String {
        val now = System.currentTimeMillis() / 1000
        val diff = now - time
        return if (diff < 60) {
            "Just Now"
        } else if (diff > 60 && diff < 59 * 60) {
            (diff / 60).toString() + " mins ago"
        } else if (diff > 59 * 60 && diff < 23 * 59 * 60) {
            if (diff / (60 * 60) > 1) (diff / (60 * 60)).toString() + " hours ago"
            else (diff / (60 * 60)).toString() + " hour ago"
        } else if (diff > 24 * 60 * 60 && diff < 48 * 60 * 60) {
            "Yesterday"
        } else {
            @SuppressLint("SimpleDateFormat")
            val sdf = SimpleDateFormat("dd MMM")
            sdf.format(Date(time * 1000L))
        }
    }

    open fun configureWithMessage(
        inboxMessage: CTInboxMessage,
        parent: CTInboxListViewFragment,
        position: Int
    ) {
        context = parent.context
        parentWeakReference = WeakReference(parent)
        message = inboxMessage
        val firstContentItem = inboxMessage.inboxMessageContents[0]
        this.firstContentItem = firstContentItem
        requiresMediaPlayer = firstContentItem.mediaIsStreamable()
    }

    open fun getImageBackgroundColor(): Int {
        return Color.TRANSPARENT
    }

    /**
     * Shows the video thumbnail for a message without a poster, fitted inside the media area.
     * Set directly rather than through Glide, which rasterises the vector at its 32x18dp intrinsic
     * size and lets the scale type blow that bitmap up into a blurry circle; set as a drawable,
     * the vector redraws sharp at the view's size. The clear() cancels any request still pending
     * from this recycled view's previous message.
     */
    fun showVideoPlaceholder(imageView: ImageView) {
        Glide.with(imageView.context).clear(imageView)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val drawableId = Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL)
        val icon = if (drawableId != -1) ContextCompat.getDrawable(context!!, drawableId) else null
        // Muted grey rather than solid black, matching the iOS inbox placeholder.
        icon?.mutate()?.alpha = VIDEO_PLACEHOLDER_ALPHA
        imageView.setImageDrawable(icon)
    }

    fun getParent(): CTInboxListViewFragment? {
        return parentWeakReference!!.get()
    }

    fun hideOneButton(mainButton: Button, secondaryButton: Button, tertiaryButton: Button) {
        tertiaryButton.visibility = View.GONE
        mainButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f)
        secondaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f)
        tertiaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f)
    }

    fun showThreeButtons(mainButton: Button, secondaryButton: Button, tertiaryButton: Button) {
        mainButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2f)
        secondaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2f)
        tertiaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2f)
    }

    fun hideTwoButtons(mainButton: Button, secondaryButton: Button, tertiaryButton: Button) {
        secondaryButton.visibility = View.GONE
        tertiaryButton.visibility = View.GONE
        mainButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 6f)
        secondaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f)
        tertiaryButton.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0f)
    }

    /** Id of the message this holder currently shows, or null before the first bind. */
    open fun getMessageId(): String? {
        return message?.messageId
    }

    /** Kotlin property syntax, equivalent to [getMessageId]. */
    val messageId: String?
        @JvmSynthetic @JvmName("ktGetMessageId") inline get() = getMessageId()

    /** Media url of the message this holder currently shows, or null before the first bind. */
    open fun getMediaUrl(): String? {
        return firstContentItem?.media
    }

    /** Kotlin property syntax, equivalent to [getMediaUrl]. */
    val mediaUrl: String?
        @JvmSynthetic @JvmName("ktGetMediaUrl") inline get() = getMediaUrl()

    open fun needsMediaPlayer(): Boolean {
        return requiresMediaPlayer
    }

    open fun playerBuffering() {
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout!!.visibility = View.VISIBLE
        }
    }

    open fun playerReady() {
        val frameLayout = getLayoutForMediaPlayer()
        frameLayout!!.visibility = View.VISIBLE
        if (muteIcon != null) {
            muteIcon!!.visibility = View.VISIBLE
        }
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout!!.visibility = View.GONE
        }
    }

    open fun playerRemoved() {
        // .post here is imp
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout!!.post { progressBarFrameLayout!!.visibility = View.GONE }
        }
        if (muteIcon != null) {
            muteIcon!!.post { muteIcon!!.visibility = View.GONE }
        }
        val frameLayout = getLayoutForMediaPlayer()
        if (frameLayout != null) {
            frameLayout.removeAllViews()
            // playerReady() left it VISIBLE; nothing else hides it until the next bind.
            frameLayout.visibility = View.GONE
        }
    }

    fun getImageContentDescription(context: Context, inboxMessage: CTInboxMessage, position: Int): String {
        var desc = inboxMessage.inboxMessageContents[position].mediaContentDescription
        if (desc.isNullOrEmpty()) {
            desc = context.getString(R.string.ct_inbox_image_content_description) + " " + (position + 1)
        }
        return desc
    }

    fun setDots(dots: Array<ImageView?>, dotsCount: Int, appContext: Context, sliderDots: LinearLayout) {
        for (k in 0 until dotsCount) {
            val dot = ImageView(appContext)
            dots[k] = dot
            dot.visibility = View.VISIBLE
            dot.setImageDrawable(
                ResourcesCompat.getDrawable(appContext.resources, R.drawable.ct_unselected_dot, null)
            )
            ViewCompat.setImportantForAccessibility(dot, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(8, 6, 4, 6)
            params.gravity = Gravity.CENTER
            if (sliderDots.childCount < dotsCount) {
                sliderDots.addView(dot, params)
            }
        }
    }

    open fun shouldAutoPlay(): Boolean {
        return firstContentItem!!.mediaIsVideo()
    }

    private fun getLayoutForMediaPlayer(): FrameLayout? {
        return frameLayout
    }

    protected fun markItemAsRead(
        inboxMessage: CTInboxMessage,
        position: Int
    ) {
        val runnable = Runnable {
            val parent = getParent()
            if (parent != null) {
                val activity = parent.activity ?: return@Runnable
                activity.runOnUiThread(Runnable {
                    // The holder may have been rebound to a different message while this
                    // timer was pending (list refresh or recycling); the rebind schedules
                    // its own timer, so this stale one must do nothing.
                    if (message !== inboxMessage) {
                        Logger.v(
                            "markItemAsRead: holder rebound (timer for [" + inboxMessage.messageId +
                                    "], now showing [" + (if (message != null) message!!.messageId else "null") +
                                    "]) — skipping stale mark-read"
                        )
                        return@Runnable
                    }
                    if (readDot!!.visibility == View.VISIBLE) {
                        parent.didShow(null, inboxMessage)
                    }
                    readDot.visibility = View.GONE
                    inboxMessage.setRead(true)
                })
            }
        }
        @Suppress("DEPRECATION")
        val handler = Handler()
        handler.postDelayed(runnable, 2000)
    }

    private companion object {

        /** 40% opacity turns the black ct_video_1 circle into the light grey iOS uses. */
        private const val VIDEO_PLACEHOLDER_ALPHA = 102
    }
}
