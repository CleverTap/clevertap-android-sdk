package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class CTInboxMessageAdapter extends RecyclerView.Adapter {

    private ArrayList<CTInboxMessage> inboxMessages;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages){
        this.inboxMessages = inboxMessages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        switch (this.inboxMessages.get(i).getType()){
            case SimpleMessage :
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_simple_message_layout,viewGroup,false);
                return new CTSimpleMessageViewHolder(view);
            case IconMessage:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_icon_message_layout,viewGroup,false);
                return new CTIconMessageViewHolder(view);
            case CarouselMessage:
            case CarouselImageMessage:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_carousel_layout,viewGroup,false);
                return new CTCarouselMessageViewHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        CTInboxMessage inboxMessage = this.inboxMessages.get(i);
        if(inboxMessage != null){
            switch (inboxMessage.getType()){
                case SimpleMessage:
                    ((CTSimpleMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTSimpleMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM");
                    String timestamp = sdf.format(new Date(inboxMessage.getDate()));
                    ((CTSimpleMessageViewHolder)viewHolder).timestamp.setText(timestamp);
                    if(inboxMessage.isRead()){
                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    JSONArray linksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(linksArray != null){
                        int size = linksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                        switch (size){

                            case 1:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                hideTwoButtons(((CTSimpleMessageViewHolder)viewHolder).cta1,((CTSimpleMessageViewHolder)viewHolder).cta2,((CTSimpleMessageViewHolder)viewHolder).cta3);
                                break;
                            case 2:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                cta2Object = linksArray.getJSONObject(1);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                hideOneButton(((CTSimpleMessageViewHolder)viewHolder).cta1,((CTSimpleMessageViewHolder)viewHolder).cta2,((CTSimpleMessageViewHolder)viewHolder).cta3);;
                                break;
                            case 3:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                cta2Object = linksArray.getJSONObject(1);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                cta3Object = linksArray.getJSONObject(2);
                                ((CTSimpleMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                break;
                            }
                        }catch (JSONException e){
                            //TODO logging
                        }
                    }
                    ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                    Glide.with(((CTSimpleMessageViewHolder)viewHolder).mediaImage.getContext())
                            .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                            .into(((CTSimpleMessageViewHolder)viewHolder).mediaImage);
                    break;
                case IconMessage:
                    ((CTIconMessageViewHolder)viewHolder).title.setText(inboxMessage.getTitle());
                    break;
                case CarouselMessage:
                case CarouselImageMessage:
                    ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getTitle());
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return inboxMessages.size();
    }

    private void hideTwoButtons(Button mainButton, Button secondaryButton, Button tertiaryButton){
        secondaryButton.setVisibility(View.GONE);
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,6);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

    private void hideOneButton(Button mainButton, Button secondaryButton, Button tertiaryButton){
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

}
