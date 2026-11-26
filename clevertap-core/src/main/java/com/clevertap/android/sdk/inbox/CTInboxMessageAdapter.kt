package com.clevertap.android.sdk.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CTInboxMessageAdapter(
    private val inboxMessages: List<CTInboxMessage>,
    private val fragment: CTInboxListViewFragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        Logger.v("CTInboxMessageAdapter: messages=$inboxMessages")
    }

    override fun getItemCount(): Int = inboxMessages.size

    override fun getItemViewType(position: Int): Int {
        // A null type threw from the Java switch as well.
        val type = checkNotNull(inboxMessages[position].type) { "Inbox message without a type" }
        return when (type) {
            CTInboxMessageType.SimpleMessage -> SIMPLE
            CTInboxMessageType.IconMessage -> ICON
            CTInboxMessageType.CarouselMessage -> CAROUSEL
            CTInboxMessageType.CarouselImageMessage -> IMAGE_CAROUSEL
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        (viewHolder as CTInboxBaseMessageViewHolder).configureWithMessage(inboxMessages[i], fragment, i)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        fun inflate(layout: Int): View = LayoutInflater.from(viewGroup.context).inflate(layout, viewGroup, false)
        return when (viewType) {
            SIMPLE -> CTSimpleMessageViewHolder(inflate(R.layout.inbox_simple_message_layout))
            ICON -> CTIconMessageViewHolder(inflate(R.layout.inbox_icon_message_layout))
            CAROUSEL -> CTCarouselMessageViewHolder(inflate(R.layout.inbox_carousel_text_layout))
            IMAGE_CAROUSEL -> CTCarouselImageViewHolder(inflate(R.layout.inbox_carousel_layout))
            // Unreachable: getItemViewType() only yields the four types above. Java returned null
            // here, which RecyclerView fails on with a NullPointerException.
            else -> throw NullPointerException("CTInboxMessageAdapter: no view holder for viewType $viewType")
        }
    }

    private companion object {

        private const val SIMPLE = 0

        private const val ICON = 1

        private const val CAROUSEL = 2

        private const val IMAGE_CAROUSEL = 3
    }
}
