package com.clevertap.android.sdk.inbox

import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import androidx.annotation.RestrictTo

/**
 * Custom Viewholder for Icon Message type of Inbox template
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CTIconMessageViewHolder(itemView: View) : CTInboxBaseMessageViewHolder(itemView) {

    private val title: TextView = itemView.findViewById(R.id.messageTitle)

    private val messageText: TextView = itemView.findViewById(R.id.messageText)

    override val mediaImage: ImageView = itemView.findViewById(R.id.media_image)

    private val iconImage: ImageView = itemView.findViewById(R.id.image_icon)

    private val timestamp: TextView = itemView.findViewById(R.id.timestamp)

    private val ctas: Array<Button> = arrayOf(
        itemView.findViewById(R.id.cta_button_1),
        itemView.findViewById(R.id.cta_button_2),
        itemView.findViewById(R.id.cta_button_3)
    )

    private val videoFrame: FrameLayout = itemView.findViewById(R.id.icon_message_frame_layout)

    override val squareImage: ImageView = itemView.findViewById(R.id.square_media_image)

    override val defaultImage: ImageView = itemView.findViewById(R.id.default_media_image)

    private val clickLayout: RelativeLayout = itemView.findViewById(R.id.click_relative_layout)

    private val ctaLayout: LinearLayout = itemView.findViewById(R.id.cta_linear_layout)

    private val progressFrame: FrameLayout = itemView.findViewById(R.id.icon_progress_frame_layout)

    private val mediaLayout: RelativeLayout = itemView.findViewById(R.id.media_layout)

    init {
        itemView.tag = this
        frameLayout = videoFrame
        progressBarFrameLayout = progressFrame

        ViewCompat.setScreenReaderFocusable(title, true)
        ViewCompat.setScreenReaderFocusable(messageText, true)
        ViewCompat.setScreenReaderFocusable(timestamp, true)
    }

    override fun configureWithMessage(
        inboxMessage: CTInboxMessage,
        parent: CTInboxListViewFragment,
        position: Int
    ) {
        super.configureWithMessage(inboxMessage, parent, position)
        val content = inboxMessage.inboxMessageContents[0]

        title.text = content.title
        title.setTextColor(Color.parseColor(content.titleColor))
        messageText.text = content.message
        messageText.setTextColor(Color.parseColor(content.messageColor))
        clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))
        timestamp.text = calculateDisplayTimestamp(inboxMessage.date)
        timestamp.setTextColor(Color.parseColor(content.titleColor))
        showReadState(inboxMessage.isRead)
        videoFrame.visibility = View.GONE
        //Shows the CTA layout only if links are present, also handles the display of the CTAs depending on the number
        bindCtas(content, ctaLayout, ctas, inboxMessage, position, parent)

        iconImage.contentDescription = null
        ViewCompat.setScreenReaderFocusable(iconImage, false)
        bindMedia(
            inboxMessage, content, mediaLayout, mediaImage, squareImage, defaultImage, progressFrame,
            squarePosterLogsGlideNotice = true,
            transparentSquarePlaceholder = true
        )

        //New thread to remove the Read dot, mark message as read and raise Notification Viewed
        markItemAsRead(inboxMessage, position)

        try {
            bindIcon(content)
        } catch (error: NoClassDefFoundError) {
            Logger.d(GLIDE_MISSING_MESSAGE)
        }

        clickLayout.setOnClickListener(
            CTInboxButtonClickListener(
                position = position,
                inboxMessage = inboxMessage,
                fragment = parent,
                buttonIndex = APP_INBOX_ITEM_INDEX
            )
        )
    }

    private fun bindIcon(content: CTInboxMessageContent) {
        // Java dereferenced both unchecked; an icon message's content always has an icon object.
        val icon = checkNotNull(content.icon) { "Icon message content without an icon" }
        if (icon.isEmpty()) {
            iconImage.visibility = View.GONE
            return
        }
        iconImage.visibility = View.VISIBLE
        if (checkNotNull(content.iconContentDescription).isNotEmpty()) {
            iconImage.contentDescription = content.iconContentDescription
            ViewCompat.setScreenReaderFocusable(iconImage, true)
        }
        loadWithPlaceholder(iconImage, icon, Constants.IMAGE_PLACEHOLDER)
    }
}
