package com.clevertap.android.sdk;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


class CTInboxMessageAdapter extends RecyclerView.Adapter {

    private ArrayList<CTInboxMessage> inboxMessages;
    private CTInboxListViewFragment fragment;
    private static final int SIMPLE = 0;
    private static final int ICON = 1;
    private static final int CAROUSEL = 2;
    private static final int IMAGE_CAROUSEL = 3;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages, CTInboxListViewFragment fragment){
        this.inboxMessages = inboxMessages;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public CTInboxBaseMessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType){
            case SIMPLE :
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_simple_message_layout,viewGroup,false);
                return new CTSimpleMessageViewHolder(view);
            case ICON:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_icon_message_layout,viewGroup,false);
                return new CTIconMessageViewHolder(view);
            case CAROUSEL:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_carousel_text_layout,viewGroup,false);
                return new CTCarouselMessageViewHolder(view);
            case IMAGE_CAROUSEL:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_carousel_layout,viewGroup,false);
                return new CTCarouselImageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final @NonNull RecyclerView.ViewHolder viewHolder, int i) {
        CTInboxMessage inboxMessage = this.inboxMessages.get(i);
        final CTInboxBaseMessageViewHolder _viewHolder = (CTInboxBaseMessageViewHolder) viewHolder;
        _viewHolder.configureWithMessage(inboxMessage, fragment, i);
    }

    @Override
    public int getItemCount() {
        return inboxMessages.size();
    }


    @Override
    public int getItemViewType(int position) {
        switch (inboxMessages.get(position).getType()){
            case SimpleMessage: return SIMPLE;
            case IconMessage: return ICON;
            case CarouselMessage: return CAROUSEL;
            case CarouselImageMessage: return IMAGE_CAROUSEL;
            default: return -1;
        }
    }
}
