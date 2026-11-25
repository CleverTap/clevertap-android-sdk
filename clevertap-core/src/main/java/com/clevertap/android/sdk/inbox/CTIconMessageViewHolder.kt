package com.clevertap.android.sdk.inbox

import android.content.res.Configuration
import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.APP_INBOX_CTA1_INDEX
import com.clevertap.android.sdk.Constants.APP_INBOX_CTA2_INDEX
import com.clevertap.android.sdk.Constants.APP_INBOX_CTA3_INDEX
import com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import org.json.JSONException

/**
 * Custom Viewholder for Icon Message type of Inbox template
 */
internal class CTIconMessageViewHolder(itemView: View) : CTInboxBaseMessageViewHolder(itemView) {

    // Java declared these two as private fields shadowing the base holder's clickLayout /
    // ctaLinearLayout (which stay null for this holder); Kotlin cannot shadow, so they are renamed.
    private val iconClickLayout: RelativeLayout?

    private val cta1: Button?

    private val cta2: Button?

    private val cta3: Button?

    private val iconCtaLinearLayout: LinearLayout?

    private val iconImage: ImageView?

    private val title: TextView?

    private val message: TextView?

    private val timestamp: TextView?

    init {
        itemView.tag = this
        title = itemView.findViewById(R.id.messageTitle)
        message = itemView.findViewById(R.id.messageText)
        mediaImage = itemView.findViewById(R.id.media_image)
        iconImage = itemView.findViewById(R.id.image_icon)
        timestamp = itemView.findViewById(R.id.timestamp)
        cta1 = itemView.findViewById(R.id.cta_button_1)
        cta2 = itemView.findViewById(R.id.cta_button_2)
        cta3 = itemView.findViewById(R.id.cta_button_3)
        frameLayout = itemView.findViewById(R.id.icon_message_frame_layout)
        squareImage = itemView.findViewById(R.id.square_media_image)
        defaultImage = itemView.findViewById(R.id.default_media_image)
        iconClickLayout = itemView.findViewById(R.id.click_relative_layout)
        iconCtaLinearLayout = itemView.findViewById(R.id.cta_linear_layout)
        progressBarFrameLayout = itemView.findViewById(R.id.icon_progress_frame_layout)
        mediaLayout = itemView.findViewById(R.id.media_layout)

        ViewCompat.setScreenReaderFocusable(title!!, true)
        ViewCompat.setScreenReaderFocusable(message!!, true)
        ViewCompat.setScreenReaderFocusable(timestamp!!, true)
    }

    override fun configureWithMessage(
        inboxMessage: CTInboxMessage,
        parent: CTInboxListViewFragment,
        position: Int
    ) {
        super.configureWithMessage(inboxMessage, parent, position)
        val parentWeak = getParent()
        val content = inboxMessage.inboxMessageContents[0]
        val title = this.title!!
        val message = this.message!!
        val timestamp = this.timestamp!!
        val clickLayout = this.iconClickLayout!!
        val frameLayout = this.frameLayout!!
        val ctaLinearLayout = this.iconCtaLinearLayout!!
        val mediaImage = this.mediaImage!!
        val squareImage = this.squareImage!!
        val defaultImage = this.defaultImage!!
        val iconImage = this.iconImage!!
        val mediaLayout = this.mediaLayout!!
        val progressBarFrameLayout = this.progressBarFrameLayout!!

        title.setText(content.title)
        title.setTextColor(Color.parseColor(content.titleColor))
        message.setText(content.message)
        message.setTextColor(Color.parseColor(content.messageColor))
        clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))
        val iconDisplayTimestamp = calculateDisplayTimestamp(inboxMessage.date)
        timestamp.setText(iconDisplayTimestamp)
        timestamp.setTextColor(Color.parseColor(content.titleColor))
        if (inboxMessage.isRead) {
            readDot!!.setVisibility(View.GONE)
        } else {
            readDot!!.setVisibility(View.VISIBLE)
        }
        frameLayout.setVisibility(View.GONE)
        //Shows the CTA layout only if links are present, also handles the display of the CTAs depending on the number
        val iconlinksArray = content.links
        if (iconlinksArray != null) {
            ctaLinearLayout.setVisibility(View.VISIBLE)
            val size = iconlinksArray.length()
            val cta1 = this.cta1!!
            val cta2 = this.cta2!!
            val cta3 = this.cta3!!
            try {
                when (size) {
                    1 -> {
                        val cta1Object = iconlinksArray.getJSONObject(0)
                        cta1.setVisibility(View.VISIBLE)
                        cta1.setText(content.getLinkText(cta1Object))
                        cta1.setTextColor(Color.parseColor(content.getLinkColor(cta1Object)))
                        cta1.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta1Object)))
                        hideTwoButtons(cta1, cta2, cta3)
                        if (parentWeak != null) {
                            cta1.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta1.text.toString(), cta1Object, parentWeak, false, APP_INBOX_CTA1_INDEX))
                        }
                    }
                    2 -> {
                        val cta1Object = iconlinksArray.getJSONObject(0)
                        cta1.setVisibility(View.VISIBLE)
                        cta1.setText(content.getLinkText(cta1Object))
                        cta1.setTextColor(Color.parseColor(content.getLinkColor(cta1Object)))
                        cta1.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta1Object)))
                        val cta2Object = iconlinksArray.getJSONObject(1)
                        cta2.setVisibility(View.VISIBLE)
                        cta2.setText(content.getLinkText(cta2Object))
                        cta2.setTextColor(Color.parseColor(content.getLinkColor(cta2Object)))
                        cta2.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta2Object)))
                        hideOneButton(cta1, cta2, cta3)
                        if (parentWeak != null) {
                            cta1.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta1.text.toString(), cta1Object, parentWeak, false, APP_INBOX_CTA1_INDEX))
                            cta2.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta2.text.toString(), cta2Object, parentWeak, false, APP_INBOX_CTA2_INDEX))
                        }
                    }
                    3 -> {
                        val cta1Object = iconlinksArray.getJSONObject(0)
                        cta1.setVisibility(View.VISIBLE)
                        cta1.setText(content.getLinkText(cta1Object))
                        cta1.setTextColor(Color.parseColor(content.getLinkColor(cta1Object)))
                        cta1.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta1Object)))
                        val cta2Object = iconlinksArray.getJSONObject(1)
                        cta2.setVisibility(View.VISIBLE)
                        cta2.setText(content.getLinkText(cta2Object))
                        cta2.setTextColor(Color.parseColor(content.getLinkColor(cta2Object)))
                        cta2.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta2Object)))
                        val cta3Object = iconlinksArray.getJSONObject(2)
                        cta3.setVisibility(View.VISIBLE)
                        cta3.setText(content.getLinkText(cta3Object))
                        cta3.setTextColor(Color.parseColor(content.getLinkColor(cta3Object)))
                        cta3.setBackgroundColor(Color.parseColor(content.getLinkBGColor(cta3Object)))
                        showThreeButtons(cta1, cta2, cta3)
                        if (parentWeak != null) {
                            cta1.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta1.text.toString(), cta1Object, parentWeak, false, APP_INBOX_CTA1_INDEX))
                            cta2.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta2.text.toString(), cta2Object, parentWeak, false, APP_INBOX_CTA2_INDEX))
                            cta3.setOnClickListener(CTInboxButtonClickListener(position, inboxMessage,
                                    cta3.text.toString(), cta3Object, parentWeak, false, APP_INBOX_CTA3_INDEX))
                        }
                    }
                }
            } catch (e: JSONException) {
                Logger.d("Error parsing CTA JSON - " + e.localizedMessage)
            }
        } else {
            ctaLinearLayout.setVisibility(View.GONE)
        }

        mediaImage.setVisibility(View.GONE)
        mediaImage.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))
        mediaImage.setContentDescription(null)
        ViewCompat.setScreenReaderFocusable(mediaImage, false)
        squareImage.setVisibility(View.GONE)
        squareImage.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))
        squareImage.setContentDescription(null)
        ViewCompat.setScreenReaderFocusable(squareImage, false)
        defaultImage.setVisibility(View.GONE)
        defaultImage.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))
        defaultImage.setContentDescription(null)
        ViewCompat.setScreenReaderFocusable(defaultImage, false)
        iconImage.setContentDescription(null)
        ViewCompat.setScreenReaderFocusable(iconImage, false)
        mediaLayout.setVisibility(View.GONE)
        progressBarFrameLayout.setVisibility(View.GONE)
        try {
            when (inboxMessage.orientation!!) {
                "l" -> {
                    if (!TextUtils.isEmpty(content.mediaContentDescription)) {
                        mediaImage.setContentDescription(content.mediaContentDescription)
                        ViewCompat.setScreenReaderFocusable(mediaImage, true)
                    }
                    if (content.mediaIsImage()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        mediaImage.setVisibility(View.VISIBLE)
                        mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP)
                        try {
                            Glide.with(mediaImage.context)
                                    .load(content.media)
                                    .apply(RequestOptions()
                                            .placeholder(
                                                    Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                            .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                    .into(mediaImage)
                        } catch (error: NoSuchMethodError) {
                            Logger.d(
                                    "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                            Glide.with(mediaImage.context)
                                    .load(content.media)
                                    .into(mediaImage)
                        }

                    } else if (content.mediaIsGIF()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        mediaImage.setVisibility(View.VISIBLE)
                        mediaImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                        try {
                            Glide.with(mediaImage.context)
                                    .asGif()
                                    .load(content.media)
                                    .apply(RequestOptions()
                                            .placeholder(
                                                    Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                            .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                    .into(mediaImage)
                        } catch (error: NoSuchMethodError) {
                            Logger.d(
                                    "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                            Glide.with(mediaImage.context)
                                    .asGif()
                                    .load(content.media)
                                    .into(mediaImage)
                        }

                    } else if (content.mediaIsVideo()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        if (!content.posterUrl!!.isEmpty()) {
                            mediaImage.setVisibility(View.VISIBLE)
                            mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP)
                            try {
                                Glide.with(mediaImage.context)
                                        .load(content.posterUrl)
                                        .apply(RequestOptions()
                                                .placeholder(
                                                        Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL))
                                                .error(Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL)))
                                        .into(mediaImage)
                            } catch (error: NoSuchMethodError) {
                                Logger.d(
                                        "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                                Glide.with(mediaImage.context)
                                        .load(content.posterUrl)
                                        .into(mediaImage)
                            }

                        } else {
                            mediaLayout.setVisibility(View.VISIBLE)
                            mediaImage.setVisibility(View.VISIBLE)
                            showVideoPlaceholder(mediaImage)
                        }
                    } else if (content.mediaIsAudio()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        mediaImage.setVisibility(View.VISIBLE)
                        mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP)
                        mediaImage.setBackgroundColor(getImageBackgroundColor())
                        val drawableId = Utils.getThumbnailImage(context, Constants.AUDIO_THUMBNAIL)
                        if (drawableId != -1) {
                            Glide.with(mediaImage.context)
                                    .load(drawableId)
                                    .into(mediaImage)
                        }
                    }
                }
                "p" -> {
                    if (!TextUtils.isEmpty(content.mediaContentDescription)) {
                        squareImage.setContentDescription(content.mediaContentDescription)
                        ViewCompat.setScreenReaderFocusable(squareImage, true)
                    }
                    if (content.mediaIsImage()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        squareImage.setVisibility(View.VISIBLE)
                        squareImage.setScaleType(ImageView.ScaleType.CENTER_CROP)
                        try {
                            Glide.with(squareImage.context)
                                    .load(content.media)
                                    .apply(RequestOptions()
                                            .placeholder(
                                                    Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                            .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                    .into(squareImage)
                        } catch (error: NoSuchMethodError) {
                            Logger.d(
                                    "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                            Glide.with(squareImage.context)
                                    .load(content.media)
                                    .into(squareImage)
                        }

                    } else if (content.mediaIsGIF()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        squareImage.setVisibility(View.VISIBLE)
                        squareImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                        try {
                            Glide.with(squareImage.context)
                                    .asGif()
                                    .load(content.media)
                                    .apply(RequestOptions()
                                            .placeholder(
                                                    Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                            .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                    .into(squareImage)
                        } catch (error: NoSuchMethodError) {
                            Logger.d(
                                    "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                            Glide.with(squareImage.context)
                                    .asGif()
                                    .load(content.media)
                                    .into(squareImage)
                        }

                    } else if (content.mediaIsVideo()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        if (!content.posterUrl!!.isEmpty()) {
                            squareImage.setVisibility(View.VISIBLE)
                            if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                squareImage.setScaleType(ImageView.ScaleType.CENTER_CROP)
                            } else {
                                squareImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                            }
                            try {
                                Logger.d(
                                        "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                                Glide.with(squareImage.context)
                                        .load(content.posterUrl)
                                        .apply(RequestOptions()
                                                .placeholder(
                                                        Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL))
                                                .error(Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL)))
                                        .into(squareImage)
                            } catch (error: NoSuchMethodError) {
                                Glide.with(squareImage.context)
                                        .load(content.posterUrl)
                                        .into(squareImage)
                            }

                        } else {
                            mediaLayout.setVisibility(View.VISIBLE)
                            squareImage.setVisibility(View.VISIBLE)
                            squareImage.setBackgroundColor(getImageBackgroundColor())
                            showVideoPlaceholder(squareImage)
                        }
                    } else if (content.mediaIsAudio()) {
                        mediaLayout.setVisibility(View.VISIBLE)
                        squareImage.setVisibility(View.VISIBLE)
                        squareImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                        squareImage.setBackgroundColor(getImageBackgroundColor())
                        val drawableId = Utils.getThumbnailImage(context, Constants.AUDIO_THUMBNAIL)
                        if (drawableId != -1) {
                            Glide.with(squareImage.context)
                                    .load(drawableId)
                                    .into(squareImage)
                        }
                    }
                }
                else -> {
                    if (!TextUtils.isEmpty(content.media)) {
                        if (!TextUtils.isEmpty(content.mediaContentDescription)) {
                            defaultImage.setContentDescription(content.mediaContentDescription)
                            ViewCompat.setScreenReaderFocusable(defaultImage, true)
                        }
                        if (content.mediaIsImage()) {
                            mediaLayout.setVisibility(View.VISIBLE)
                            defaultImage.setVisibility(View.VISIBLE)
                            defaultImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                            try {
                                Glide.with(defaultImage.context)
                                        .load(content.media)
                                        .apply(RequestOptions()
                                                .placeholder(
                                                        Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                                .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                        .into(defaultImage)
                            } catch (error: NoSuchMethodError) {
                                Logger.d(
                                        "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                                Glide.with(defaultImage.context)
                                        .load(content.media)
                                        .into(defaultImage)
                            }
                        } else if (content.mediaIsGIF()) {
                            mediaLayout.setVisibility(View.VISIBLE)
                            defaultImage.setVisibility(View.VISIBLE)
                            defaultImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                            try {
                                Glide.with(defaultImage.context)
                                        .asGif()
                                        .load(content.media)
                                        .apply(RequestOptions()
                                                .placeholder(
                                                        Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                                .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                                        .into(defaultImage)
                            } catch (error: NoSuchMethodError) {
                                Logger.d(
                                        "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                                Glide.with(defaultImage.context)
                                        .asGif()
                                        .load(content.media)
                                        .into(defaultImage)
                            }
                        } else if (content.mediaIsVideo()) {
                            mediaLayout.setVisibility(View.VISIBLE)
                            if (!content.posterUrl!!.isEmpty()) {
                                defaultImage.setVisibility(View.VISIBLE)
                                defaultImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                                try {
                                    Glide.with(defaultImage.context)
                                            .load(content.posterUrl)
                                            .apply(RequestOptions()
                                                    .placeholder(
                                                            Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL))
                                                    .error(Utils.getThumbnailImage(context, Constants.VIDEO_THUMBNAIL)))
                                            .into(defaultImage)
                                } catch (error: NoSuchMethodError) {
                                    Logger.d(
                                            "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                                    Glide.with(defaultImage.context)
                                            .load(content.posterUrl)
                                            .into(defaultImage)
                                }
                            } else {
                                defaultImage.setVisibility(View.VISIBLE)
                                showVideoPlaceholder(defaultImage)
                            }
                        } else if (content.mediaIsAudio()) {
                            mediaLayout.setVisibility(View.VISIBLE)
                            defaultImage.setVisibility(View.VISIBLE)
                            defaultImage.setScaleType(ImageView.ScaleType.FIT_CENTER)
                            defaultImage.setBackgroundColor(getImageBackgroundColor())
                            val drawableId = Utils.getThumbnailImage(context, Constants.AUDIO_THUMBNAIL)
                            if (drawableId != -1) {
                                Glide.with(defaultImage.context)
                                        .load(drawableId)
                                        .into(defaultImage)
                            }
                        }
                    }
                }
            }
        } catch (error: NoClassDefFoundError) {
            Logger.d("CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info")
        }

        //Set the height and width of Progress Bar Frame to match the thumbnail size
        val resources = context!!.resources
        val width: Int
        val height: Int
        if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            height = resources.displayMetrics.heightPixels / 2
            width = resources.displayMetrics.widthPixels / 2
        } else {
            width = resources.displayMetrics.widthPixels
            if (inboxMessage.orientation!!.equals("l", ignoreCase = true)) {
                height = Math.round(width * 0.5625f)
            } else if (inboxMessage.orientation!!.equals("p", ignoreCase = true)) {
                height = width
            } else {
                height = Math.round(width * 0.5625f)
            }
        }
        progressBarFrameLayout.setLayoutParams(RelativeLayout.LayoutParams(width, height))

        //New thread to remove the Read dot, mark message as read and raise Notification Viewed
        markItemAsRead(inboxMessage, position)

        try {
            if (!content.icon!!.isEmpty()) {
                iconImage.setVisibility(View.VISIBLE)
                if (!content.iconContentDescription!!.isEmpty()) {
                    iconImage.setContentDescription(content.iconContentDescription)
                    ViewCompat.setScreenReaderFocusable(iconImage, true)
                }
                try {
                    Glide.with(iconImage.context)
                            .load(content.icon)
                            .apply(RequestOptions()
                                    .placeholder(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                                    .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                            .into(iconImage)
                } catch (error: NoSuchMethodError) {
                    Logger.d(
                            "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info")
                    Glide.with(iconImage.context)
                            .load(content.icon)
                            .into(iconImage)
                }

            } else {
                iconImage.setVisibility(View.GONE)
            }
        } catch (error: NoClassDefFoundError) {
            Logger.d("CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info")
        }

        if (parentWeak != null) {
            clickLayout.setOnClickListener(
                    CTInboxButtonClickListener(position, inboxMessage, null, null, parentWeak, true, APP_INBOX_ITEM_INDEX))
        }
    }
}
