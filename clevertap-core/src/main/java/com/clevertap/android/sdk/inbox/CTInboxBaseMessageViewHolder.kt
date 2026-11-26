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
import com.bumptech.glide.request.RequestOptions
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class CTInboxBaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    /** The list fragment's context, taken at bind time. */
    var context: Context? = null

    /** Container the shared video player is put in; rows without media have none. */
    var frameLayout: FrameLayout? = null

    var progressBarFrameLayout: FrameLayout? = null

    /** Media views of the rows that can show media; the player is sized after the one in use. */
    protected open val mediaImage: ImageView? get() = null

    protected open val squareImage: ImageView? get() = null

    protected open val defaultImage: ImageView? get() = null

    private var firstContentItem: CTInboxMessageContent? = null

    private var message: CTInboxMessage? = null

    private var muteIcon: ImageView? = null

    private var parentWeakReference = WeakReference<CTInboxListViewFragment>(null)

    private var requiresMediaPlayer = false

    protected val readDot: ImageView? = itemView.findViewById(R.id.read_circle)

    /** The fragment this row was last bound for, while it is alive. */
    protected val parent: CTInboxListViewFragment?
        get() = parentWeakReference.get()

    /** Id of the message this holder currently shows, or null before the first bind. */
    open val messageId: String?
        get() = message?.messageId

    /** Media url of the message this holder currently shows, or null before the first bind. */
    open val mediaUrl: String?
        get() = firstContentItem?.media

    open fun addMediaPlayer(
        currentVolume: Float,
        muteClick: Function0<Float>,
        playMedia: Function3<String, Boolean, Boolean, Void?>,
        videoSurfaceView: View
    ): Boolean {
        if (!requiresMediaPlayer) {
            return false
        }
        val frameLayout = frameLayout ?: return false
        frameLayout.removeAllViews()
        frameLayout.visibility = View.GONE // Gets set visible in playerReady

        // configureWithMessage sets these together with requiresMediaPlayer; the context is the
        // fragment's, which Java dereferenced unchecked as well.
        val context = checkNotNull(context) { "Inbox row bound without a context" }
        val message = checkNotNull(message)
        val firstContentItem = checkNotNull(firstContentItem)
        val displayMetrics = context.resources.displayMetrics

        val (width, height) = if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val orientation = message.requireOrientation()
            when {
                orientation.equals("l", ignoreCase = true) -> {
                    val imageHeight = checkNotNull(mediaImage).measuredHeight
                    (imageHeight * 1.76f).roundToInt() to imageHeight
                }

                orientation.equals("p", ignoreCase = true) -> {
                    val side = checkNotNull(squareImage).measuredHeight
                    side to side
                }

                else -> {
                    val defaultImage = defaultImage
                    val measuredWidth = defaultImage?.measuredWidth ?: 0
                    val measuredHeight = defaultImage?.measuredHeight ?: 0
                    if (defaultImage == null || measuredWidth == 0 || measuredHeight == 0) {
                        val halfWidth = displayMetrics.widthPixels / 2
                        halfWidth to (halfWidth * 0.5625f).roundToInt()
                    } else {
                        measuredWidth to measuredHeight
                    }
                }
            }
        } else {
            val fullWidth = displayMetrics.widthPixels
            val orientation = message.requireOrientation()
            val rowHeight = when {
                orientation.equals("l", ignoreCase = true) -> (fullWidth * 0.5625f).roundToInt()
                orientation.equals("p", ignoreCase = true) -> fullWidth
                else -> defaultImage?.measuredHeight?.takeIf { it != 0 } ?: (fullWidth * 0.5625f).roundToInt()
            }
            fullWidth to rowHeight
        }

        videoSurfaceView.layoutParams = FrameLayout.LayoutParams(width, height)

        // The shared player view may still sit in another row; a view can have only one parent.
        (videoSurfaceView.parent as ViewGroup?)?.removeView(videoSurfaceView)
        frameLayout.addView(videoSurfaceView)
        frameLayout.setBackgroundColor(Color.parseColor(message.bgColor))

        progressBarFrameLayout?.visibility = View.VISIBLE
        if (firstContentItem.mediaIsVideo()) {
            val muteIcon = ImageView(context).also { muteIcon = it }
            muteIcon.visibility = View.GONE

            // Initial state setup
            setMuteIconState(muteIcon, context, currentVolume)

            // 48dp tap target; 9dp padding keeps the visual icon at 30dp
            val iconSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, displayMetrics).toInt()
            val iconPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 9f, displayMetrics).toInt()
            muteIcon.setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            val iconTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, displayMetrics).toInt()
            val iconRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, displayMetrics).toInt()
            muteIcon.layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                setMargins(0, iconTop, iconRight, 0)
                gravity = Gravity.END
            }

            muteIcon.setOnClickListener {
                val volume = muteClick.invoke()
                setMuteIconState(muteIcon, context, volume)
            }
            frameLayout.addView(muteIcon)
        }

        // A streamable content has a media url.
        playMedia.invoke(checkNotNull(firstContentItem.media), firstContentItem.mediaIsAudio(), firstContentItem.mediaIsVideo())
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
        val hours = diff / (60 * 60)
        return when {
            diff < 60 -> "Just Now"
            diff > 60 && diff < 59 * 60 -> "${diff / 60} mins ago"
            diff > 59 * 60 && diff < 23 * 59 * 60 -> if (hours > 1) "$hours hours ago" else "$hours hour ago"
            diff > 24 * 60 * 60 && diff < 48 * 60 * 60 -> "Yesterday"
            else -> {
                @SuppressLint("SimpleDateFormat")
                val sdf = SimpleDateFormat("dd MMM")
                sdf.format(Date(time * 1000L))
            }
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
        firstContentItem = inboxMessage.inboxMessageContents[0].also {
            requiresMediaPlayer = it.mediaIsStreamable()
        }
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
        val icon = if (drawableId != -1) {
            ContextCompat.getDrawable(checkNotNull(context) { "Inbox row bound without a context" }, drawableId)
        } else {
            null
        }
        // Muted grey rather than solid black, matching the iOS inbox placeholder.
        icon?.mutate()?.alpha = VIDEO_PLACEHOLDER_ALPHA
        imageView.setImageDrawable(icon)
    }

    /**
     * Loads [model] into [target], showing the [thumbnail] placeholder while loading and on error.
     * Glide older than 4.9.0 has no [RequestOptions]; it then loads without them, after logging a
     * notice ([logNoticeFirst] logs that notice up front instead, on every load).
     */
    protected fun loadWithPlaceholder(
        target: ImageView,
        model: String?,
        thumbnail: String,
        asGif: Boolean = false,
        logNoticeFirst: Boolean = false
    ) {
        try {
            if (logNoticeFirst) {
                Logger.d(GLIDE_VERSION_MESSAGE)
            }
            val options = RequestOptions()
                .placeholder(Utils.getThumbnailImage(context, thumbnail))
                .error(Utils.getThumbnailImage(context, thumbnail))
            if (asGif) {
                Glide.with(target.context).asGif().load(model).apply(options).into(target)
            } else {
                Glide.with(target.context).load(model).apply(options).into(target)
            }
        } catch (error: NoSuchMethodError) {
            if (!logNoticeFirst) {
                Logger.d(GLIDE_VERSION_MESSAGE)
            }
            if (asGif) {
                Glide.with(target.context).asGif().load(model).into(target)
            } else {
                Glide.with(target.context).load(model).into(target)
            }
        }
    }

    /** Shows the audio thumbnail, when the app has one. */
    protected fun loadAudioThumbnail(target: ImageView) {
        val drawableId = Utils.getThumbnailImage(context, Constants.AUDIO_THUMBNAIL)
        if (drawableId != -1) {
            Glide.with(target.context).load(drawableId).into(target)
        }
    }

    /** Every inbox row layout has the read dot; only rows built in tests may lack it. */
    protected fun showReadState(isRead: Boolean) {
        checkNotNull(readDot) { "Inbox row layout without a read dot" }.visibility =
            if (isRead) View.GONE else View.VISIBLE
    }

    /** Clears what the previous message left on a media view: hidden, [bgColor] background, no description. */
    protected fun resetMediaView(imageView: ImageView, bgColor: String?) {
        imageView.visibility = View.GONE
        imageView.setBackgroundColor(Color.parseColor(bgColor))
        imageView.contentDescription = null
        ViewCompat.setScreenReaderFocusable(imageView, false)
    }

    /** Gives [imageView] the media's alt text, when it has one. */
    protected fun describeMedia(imageView: ImageView, content: CTInboxMessageContent) {
        val description = content.mediaContentDescription
        if (!description.isNullOrEmpty()) {
            imageView.contentDescription = description
            ViewCompat.setScreenReaderFocusable(imageView, true)
        }
    }

    /**
     * Shows the call-to-action buttons for the content's links (one to three of them), or hides
     * [ctaLayout] when the content has no links.
     */
    protected fun bindCtas(
        content: CTInboxMessageContent,
        ctaLayout: LinearLayout,
        ctas: Array<Button>,
        inboxMessage: CTInboxMessage,
        position: Int,
        parent: CTInboxListViewFragment
    ) {
        val links = content.links
        if (links == null) {
            ctaLayout.visibility = View.GONE
            return
        }
        ctaLayout.visibility = View.VISIBLE
        val (cta1, cta2, cta3) = ctas
        try {
            val count = links.length()
            if (count !in 1..ctas.size) {
                return
            }
            val ctaObjects = ArrayList<JSONObject>(count)
            for (i in 0 until count) {
                val ctaObject = links.getJSONObject(i)
                ctaObjects += ctaObject
                ctas[i].visibility = View.VISIBLE
                ctas[i].text = content.getLinkText(ctaObject)
                ctas[i].setTextColor(Color.parseColor(content.getLinkColor(ctaObject)))
                ctas[i].setBackgroundColor(Color.parseColor(content.getLinkBGColor(ctaObject)))
            }
            when (count) {
                1 -> hideTwoButtons(cta1, cta2, cta3)
                2 -> hideOneButton(cta1, cta2, cta3)
                else -> showThreeButtons(cta1, cta2, cta3)
            }
            ctaObjects.forEachIndexed { i, ctaObject ->
                ctas[i].setOnClickListener(
                    CTInboxButtonClickListener(
                        position = position,
                        inboxMessage = inboxMessage,
                        buttonText = ctas[i].text.toString(),
                        buttonObject = ctaObject,
                        fragment = parent,
                        buttonIndex = CTA_INDEXES[i]
                    )
                )
            }
        } catch (e: JSONException) {
            Logger.d("Error parsing CTA JSON - ${e.localizedMessage}")
        }
    }

    /**
     * Shows the first content's media in the view for the message's orientation ("l": [mediaImage],
     * "p": [squareImage], anything else: [defaultImage]) and gives [progressBar] the matching size.
     *
     * [squarePosterLogsGlideNotice] and [transparentSquarePlaceholder] keep two differences the icon
     * row has always had over the simple row: it logs the Glide version notice before every square
     * poster load, and clears the background behind a square video placeholder.
     */
    protected fun bindMedia(
        inboxMessage: CTInboxMessage,
        content: CTInboxMessageContent,
        mediaLayout: RelativeLayout,
        mediaImage: ImageView,
        squareImage: ImageView,
        defaultImage: ImageView,
        progressBar: FrameLayout,
        squarePosterLogsGlideNotice: Boolean = false,
        transparentSquarePlaceholder: Boolean = false
    ) {
        resetMediaView(mediaImage, inboxMessage.bgColor)
        resetMediaView(squareImage, inboxMessage.bgColor)
        resetMediaView(defaultImage, inboxMessage.bgColor)
        mediaLayout.visibility = View.GONE
        progressBar.visibility = View.GONE
        try {
            when (inboxMessage.requireOrientation()) {
                "l" -> showMedia(
                    content, mediaLayout, mediaImage,
                    imageScale = ImageView.ScaleType.CENTER_CROP,
                    posterScale = ImageView.ScaleType.CENTER_CROP,
                    audioScale = ImageView.ScaleType.CENTER_CROP
                )

                "p" -> showMedia(
                    content, mediaLayout, squareImage,
                    imageScale = ImageView.ScaleType.CENTER_CROP,
                    posterScale = if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        ImageView.ScaleType.CENTER_CROP
                    } else {
                        ImageView.ScaleType.FIT_CENTER
                    },
                    audioScale = ImageView.ScaleType.FIT_CENTER,
                    posterLogsGlideNotice = squarePosterLogsGlideNotice,
                    transparentPlaceholder = transparentSquarePlaceholder
                )

                else -> if (!content.media.isNullOrEmpty()) {
                    showMedia(
                        content, mediaLayout, defaultImage,
                        imageScale = ImageView.ScaleType.FIT_CENTER,
                        posterScale = ImageView.ScaleType.FIT_CENTER,
                        audioScale = ImageView.ScaleType.FIT_CENTER
                    )
                }
            }
        } catch (error: NoClassDefFoundError) {
            Logger.d(GLIDE_MISSING_MESSAGE)
        }

        //Set the height and width of Progress Bar Frame to match the thumbnail size
        val displayMetrics = checkNotNull(context) { "Inbox row bound without a context" }.resources.displayMetrics
        val (width, height) = if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            displayMetrics.widthPixels / 2 to displayMetrics.heightPixels / 2
        } else {
            val fullWidth = displayMetrics.widthPixels
            val isSquare = inboxMessage.requireOrientation().equals("p", ignoreCase = true)
            fullWidth to if (isSquare) fullWidth else (fullWidth * 0.5625f).roundToInt()
        }
        progressBar.layoutParams = RelativeLayout.LayoutParams(width, height)
    }

    private fun showMedia(
        content: CTInboxMessageContent,
        mediaLayout: RelativeLayout,
        target: ImageView,
        imageScale: ImageView.ScaleType,
        posterScale: ImageView.ScaleType,
        audioScale: ImageView.ScaleType,
        posterLogsGlideNotice: Boolean = false,
        transparentPlaceholder: Boolean = false
    ) {
        describeMedia(target, content)
        when {
            content.mediaIsImage() -> {
                mediaLayout.visibility = View.VISIBLE
                target.visibility = View.VISIBLE
                target.scaleType = imageScale
                loadWithPlaceholder(target, content.media, Constants.IMAGE_PLACEHOLDER)
            }

            content.mediaIsGIF() -> {
                mediaLayout.visibility = View.VISIBLE
                target.visibility = View.VISIBLE
                target.scaleType = ImageView.ScaleType.FIT_CENTER
                loadWithPlaceholder(target, content.media, Constants.IMAGE_PLACEHOLDER, asGif = true)
            }

            content.mediaIsVideo() -> {
                mediaLayout.visibility = View.VISIBLE
                // A video content always has a poster url (empty when there is none); Java
                // dereferenced it unchecked.
                if (checkNotNull(content.posterUrl).isNotEmpty()) {
                    target.visibility = View.VISIBLE
                    target.scaleType = posterScale
                    loadWithPlaceholder(
                        target, content.posterUrl, Constants.VIDEO_THUMBNAIL,
                        logNoticeFirst = posterLogsGlideNotice
                    )
                } else {
                    target.visibility = View.VISIBLE
                    if (transparentPlaceholder) {
                        target.setBackgroundColor(IMAGE_BACKGROUND_COLOR)
                    }
                    showVideoPlaceholder(target)
                }
            }

            content.mediaIsAudio() -> {
                mediaLayout.visibility = View.VISIBLE
                target.visibility = View.VISIBLE
                target.scaleType = audioScale
                target.setBackgroundColor(IMAGE_BACKGROUND_COLOR)
                loadAudioThumbnail(target)
            }
        }
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

    open fun needsMediaPlayer(): Boolean = requiresMediaPlayer

    open fun playerBuffering() {
        progressBarFrameLayout?.visibility = View.VISIBLE
    }

    open fun playerReady() {
        // Only rows with a player container get a player.
        checkNotNull(frameLayout).visibility = View.VISIBLE
        muteIcon?.visibility = View.VISIBLE
        progressBarFrameLayout?.visibility = View.GONE
    }

    open fun playerRemoved() {
        // .post here is imp
        progressBarFrameLayout?.let { progressBar -> progressBar.post { progressBar.visibility = View.GONE } }
        muteIcon?.let { muteIcon -> muteIcon.post { muteIcon.visibility = View.GONE } }
        frameLayout?.let { frameLayout ->
            frameLayout.removeAllViews()
            // playerReady() left it VISIBLE; nothing else hides it until the next bind.
            frameLayout.visibility = View.GONE
        }
    }

    fun getImageContentDescription(context: Context, inboxMessage: CTInboxMessage, position: Int): String {
        val desc = inboxMessage.inboxMessageContents[position].mediaContentDescription
        return if (desc.isNullOrEmpty()) {
            "${context.getString(R.string.ct_inbox_image_content_description)} ${position + 1}"
        } else {
            desc
        }
    }

    /** Adds [dotsCount] unselected carousel dots to [sliderDots] (unless it already has them) and returns them. */
    fun createDots(dotsCount: Int, appContext: Context, sliderDots: LinearLayout): Array<ImageView> =
        Array(dotsCount) {
            val dot = ImageView(appContext)
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
            dot
        }

    // A streamable row is always bound before the list asks; Java dereferenced it unchecked too.
    open fun shouldAutoPlay(): Boolean = checkNotNull(firstContentItem).mediaIsVideo()

    protected fun markItemAsRead(
        inboxMessage: CTInboxMessage,
        position: Int
    ) {
        @Suppress("DEPRECATION")
        val handler = Handler()
        handler.postDelayed({
            val parent = parent ?: return@postDelayed
            val activity = parent.activity ?: return@postDelayed
            activity.runOnUiThread {
                // The holder may have been rebound to a different message while this
                // timer was pending (list refresh or recycling); the rebind schedules
                // its own timer, so this stale one must do nothing.
                if (message !== inboxMessage) {
                    Logger.v(
                        "markItemAsRead: holder rebound (timer for [${inboxMessage.messageId}], now showing " +
                                "[${message?.messageId}]) — skipping stale mark-read"
                    )
                    return@runOnUiThread
                }
                val readDot = checkNotNull(readDot) { "Inbox row layout without a read dot" }
                if (readDot.visibility == View.VISIBLE) {
                    parent.didShow(null, inboxMessage)
                }
                readDot.visibility = View.GONE
                inboxMessage.setRead(true)
            }
        }, 2000)
    }

    protected companion object {

        /** 40% opacity turns the black ct_video_1 circle into the light grey iOS uses. */
        private const val VIDEO_PLACEHOLDER_ALPHA = 102

        /** Background of the audio thumbnail. */
        const val IMAGE_BACKGROUND_COLOR = Color.TRANSPARENT

        const val GLIDE_VERSION_MESSAGE =
            "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info"

        private val CTA_INDEXES = intArrayOf(
            Constants.APP_INBOX_CTA1_INDEX,
            Constants.APP_INBOX_CTA2_INDEX,
            Constants.APP_INBOX_CTA3_INDEX
        )

        const val GLIDE_MISSING_MESSAGE =
            "CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info"

        /** Java dereferenced the orientation unchecked; only a message without a "msg" object has none. */
        fun CTInboxMessage.requireOrientation(): String =
            checkNotNull(orientation) { "Inbox message $messageId has no orientation" }
    }
}
