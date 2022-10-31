package com.clevertap.android.sdk.inbox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import java.util.ArrayList;
import java.util.Date;


@SuppressWarnings("rawtypes")
class CTInboxMessageAdapter extends RecyclerView.Adapter {

    private static final int SIMPLE = 0;

    private static final int ICON = 1;

    private static final int CAROUSEL = 2;

    private static final int IMAGE_CAROUSEL = 3;

    private CTInboxListViewFragment fragment;

    private ArrayList<CTInboxMessage> inboxMessages;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages, CTInboxListViewFragment fragment) {
        Logger.v("CTInboxMessageAdapter: messages="+inboxMessages);
        this.inboxMessages = inboxMessages;
        this.fragment = fragment;
    }


    @Override
    public int getItemCount() {
        return inboxMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (inboxMessages.get(position).getType()) {
            case SimpleMessage:
                return SIMPLE;
            case IconMessage:
                return ICON;
            case CarouselMessage:
                return CAROUSEL;
            case CarouselImageMessage:
                return IMAGE_CAROUSEL;
            default:
                return -1;
        }
    }

    @Override
    public void onBindViewHolder(final @NonNull RecyclerView.ViewHolder viewHolder, int i) {
        CTInboxMessage inboxMessage = this.inboxMessages.get(i);
        final CTInboxBaseMessageViewHolder _viewHolder = (CTInboxBaseMessageViewHolder) viewHolder;
        _viewHolder.configureWithMessage(inboxMessage, fragment, i);
    }


    @SuppressWarnings({"ConstantConditions", "NullableProblems"})
    @Override
    public CTInboxBaseMessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case SIMPLE:
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.inbox_simple_message_layout, viewGroup, false);
                return new CTSimpleMessageViewHolder(view);
            case ICON:
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.inbox_icon_message_layout, viewGroup, false);
                return new CTIconMessageViewHolder(view);
            case CAROUSEL:
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.inbox_carousel_text_layout, viewGroup, false);
                return new CTCarouselMessageViewHolder(view);
            case IMAGE_CAROUSEL:
                view = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.inbox_carousel_layout, viewGroup, false);
                return new CTCarouselImageViewHolder(view);
        }
        return null;
    }
}
