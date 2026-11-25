package com.clevertap.android.sdk.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R

internal class CTInboxMessageAdapter(
    private val inboxMessages: ArrayList<CTInboxMessage>,
    private val fragment: CTInboxListViewFragment
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        Logger.v("CTInboxMessageAdapter: messages=$inboxMessages")
    }

    override fun getItemCount(): Int {
        return inboxMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        // A null type threw from the Java switch as well.
        return when (inboxMessages[position].type!!) {
            CTInboxMessageType.SimpleMessage -> SIMPLE
            CTInboxMessageType.IconMessage -> ICON
            CTInboxMessageType.CarouselMessage -> CAROUSEL
            CTInboxMessageType.CarouselImageMessage -> IMAGE_CAROUSEL
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, i: Int) {
        val inboxMessage = this.inboxMessages[i]
        val holder = viewHolder as CTInboxBaseMessageViewHolder
        holder.configureWithMessage(inboxMessage, fragment, i)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        when (viewType) {
            SIMPLE -> {
                view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_simple_message_layout, viewGroup, false)
                return CTSimpleMessageViewHolder(view)
            }

            ICON -> {
                view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_icon_message_layout, viewGroup, false)
                return CTIconMessageViewHolder(view)
            }

            CAROUSEL -> {
                view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_carousel_text_layout, viewGroup, false)
                return CTCarouselMessageViewHolder(view)
            }

            IMAGE_CAROUSEL -> {
                view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.inbox_carousel_layout, viewGroup, false)
                return CTCarouselImageViewHolder(view)
            }
        }
        // Unreachable: getItemViewType() only yields the four types above. Java returned null
        // here, which RecyclerView fails on with a NullPointerException.
        throw NullPointerException("CTInboxMessageAdapter: no view holder for viewType $viewType")
    }

    private companion object {

        private const val SIMPLE = 0

        private const val ICON = 1

        private const val CAROUSEL = 2

        private const val IMAGE_CAROUSEL = 3
    }
}
