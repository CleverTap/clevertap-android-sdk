package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class CTInboxMessageAdapter extends RecyclerView.Adapter {

    private ArrayList<CTInboxMessage> inboxMessages;
    private SimpleExoPlayer player;
    private Context context;
    private int dotsCount;
    private ImageView[] dots;
    private CTInboxMessage inboxMessage;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages, Activity activity){
        this.inboxMessages = inboxMessages;
        this.context = activity;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        switch (this.inboxMessages.get(i).getType()){
            case SimpleMessage :
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_simple_message_layout,viewGroup,false);
                CTSimpleMessageViewHolder ctSimpleMessageViewHolder = new CTSimpleMessageViewHolder(view);
                return ctSimpleMessageViewHolder;
            case IconMessage:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_icon_message_layout,viewGroup,false);
                CTIconMessageViewHolder ctIconMessageViewHolder = new CTIconMessageViewHolder(view);
                return ctIconMessageViewHolder;
            case CarouselMessage:
            case CarouselImageMessage:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_carousel_layout,viewGroup,false);
                CTCarouselMessageViewHolder ctCarouselMessageViewHolder = new CTCarouselMessageViewHolder(view);
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ctCarouselMessageViewHolder.imageViewPager.getLayoutParams();
                inboxMessage = this.inboxMessages.get(i);
                CTCarouselViewPagerAdapter carouselViewPagerAdapter = new CTCarouselViewPagerAdapter(context,inboxMessage.getCarouselImages(),layoutParams);
                ctCarouselMessageViewHolder.imageViewPager.setAdapter(carouselViewPagerAdapter);
                dotsCount = carouselViewPagerAdapter.getCount();
                dots = new ImageView[dotsCount];
                for(int k=0;k<dotsCount;k++){
                    dots[k] = new ImageView(context);
                    dots[k].setVisibility(View.VISIBLE);
                    dots[k].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(8, 0, 8, 0);
                    params.gravity = Gravity.CENTER;
                    ctCarouselMessageViewHolder.sliderDots.addView(dots[k],params);
                }
                dots[0].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
                CarouselPageChangeListener carouselPageChangeListener = new CarouselPageChangeListener(ctCarouselMessageViewHolder);
                ctCarouselMessageViewHolder.imageViewPager.addOnPageChangeListener(carouselPageChangeListener);
                return ctCarouselMessageViewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final @NonNull RecyclerView.ViewHolder viewHolder, int i) {
        inboxMessage = this.inboxMessages.get(i);
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
                    ((CTIconMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTIconMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat iconSdf = new SimpleDateFormat("dd/MMM");
                    String iconTimestamp = iconSdf.format(new Date(inboxMessage.getDate()));
                    ((CTIconMessageViewHolder)viewHolder).timestamp.setText(iconTimestamp);
                    if(inboxMessage.isRead()){
                        ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    JSONArray iconlinksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(iconlinksArray != null){
                        int size = iconlinksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                            switch (size){

                                case 1:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    hideTwoButtons(((CTIconMessageViewHolder)viewHolder).cta1,((CTIconMessageViewHolder)viewHolder).cta2,((CTIconMessageViewHolder)viewHolder).cta3);
                                    break;
                                case 2:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = iconlinksArray.getJSONObject(1);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    hideOneButton(((CTIconMessageViewHolder)viewHolder).cta1,((CTIconMessageViewHolder)viewHolder).cta2,((CTIconMessageViewHolder)viewHolder).cta3);;
                                    break;
                                case 3:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = iconlinksArray.getJSONObject(1);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    cta3Object = iconlinksArray.getJSONObject(2);
                                    ((CTIconMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                    break;
                            }
                        }catch (JSONException e){
                            //TODO logging
                        }
                    }
                    ((CTIconMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                    Glide.with(((CTIconMessageViewHolder)viewHolder).mediaImage.getContext())
                            .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                            .into(((CTIconMessageViewHolder)viewHolder).mediaImage);
                    ((CTIconMessageViewHolder)viewHolder).iconImage.setVisibility(View.VISIBLE);
                    Glide.with(((CTIconMessageViewHolder)viewHolder).iconImage.getContext())
                            .load(inboxMessage.getInboxMessageContents().get(0).getIcon())
                            .into(((CTIconMessageViewHolder)viewHolder).iconImage);
                    break;
                case CarouselMessage:
                    ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTCarouselMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat carouselSdf = new SimpleDateFormat("dd/MMM");
                    String carouselMessagetimestamp = carouselSdf.format(new Date(inboxMessage.getDate()));
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setText(carouselMessagetimestamp);
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    JSONArray carousellinksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(carousellinksArray != null){
                        int size = carousellinksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                            switch (size){

                                case 1:
                                    cta1Object = carousellinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    hideTwoButtons(((CTCarouselMessageViewHolder)viewHolder).cta1,((CTCarouselMessageViewHolder)viewHolder).cta2,((CTCarouselMessageViewHolder)viewHolder).cta3);
                                    break;
                                case 2:
                                    cta1Object = carousellinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = carousellinksArray.getJSONObject(1);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    hideOneButton(((CTCarouselMessageViewHolder)viewHolder).cta1,((CTCarouselMessageViewHolder)viewHolder).cta2,((CTCarouselMessageViewHolder)viewHolder).cta3);;
                                    break;
                                case 3:
                                    cta1Object = carousellinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = carousellinksArray.getJSONObject(1);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    cta3Object = carousellinksArray.getJSONObject(2);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                    break;
                            }
                        }catch (JSONException e){
                            //TODO logging
                        }
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);

                    break;
                case CarouselImageMessage:

                    ((CTCarouselMessageViewHolder)viewHolder).title.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).message.setVisibility(View.GONE);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat carouselImageSdf = new SimpleDateFormat("dd/MMM");
                    String carouselImageMessagetimestamp = carouselImageSdf.format(new Date(inboxMessage.getDate()));
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setText(carouselImageMessagetimestamp);
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.VISIBLE);
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    JSONArray carouselImagelinksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(carouselImagelinksArray != null){
                        int size = carouselImagelinksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                            switch (size){

                                case 1:
                                    cta1Object = carouselImagelinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    hideTwoButtons(((CTCarouselMessageViewHolder)viewHolder).cta1,((CTCarouselMessageViewHolder)viewHolder).cta2,((CTCarouselMessageViewHolder)viewHolder).cta3);
                                    break;
                                case 2:
                                    cta1Object = carouselImagelinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = carouselImagelinksArray.getJSONObject(1);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    hideOneButton(((CTCarouselMessageViewHolder)viewHolder).cta1,((CTCarouselMessageViewHolder)viewHolder).cta2,((CTCarouselMessageViewHolder)viewHolder).cta3);;
                                    break;
                                case 3:
                                    cta1Object = carouselImagelinksArray.getJSONObject(0);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    cta2Object = carouselImagelinksArray.getJSONObject(1);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    cta3Object = carouselImagelinksArray.getJSONObject(2);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                    ((CTCarouselMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                    break;
                            }
                        }catch (JSONException e){
                            //TODO logging
                        }
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    LinearLayout.LayoutParams layoutImageParams = (LinearLayout.LayoutParams) ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.getLayoutParams();
                    CTCarouselViewPagerAdapter carouselImageViewPagerAdapter = new CTCarouselViewPagerAdapter(context,inboxMessage.getCarouselImages(),layoutImageParams);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.setAdapter(carouselImageViewPagerAdapter);
                    dotsCount = carouselImageViewPagerAdapter.getCount();
                    dots = new ImageView[dotsCount];
                    for(int k=0;k<dotsCount;k++){
                        dots[k] = new ImageView(context);
                        dots[k].setVisibility(View.VISIBLE);
                        dots[k].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(8, 0, 8, 0);
                        params.gravity = Gravity.CENTER;
                        ((CTCarouselMessageViewHolder)viewHolder).sliderDots.addView(dots[k],params);
                    }
                    dots[0].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
                    CarouselPageChangeListener carouselImagePageChangeListener = new CarouselPageChangeListener((CTCarouselMessageViewHolder)viewHolder);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.addOnPageChangeListener(carouselImagePageChangeListener);
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


class CarouselPageChangeListener implements ViewPager.OnPageChangeListener{

        RecyclerView.ViewHolder viewHolder;
        CarouselPageChangeListener(RecyclerView.ViewHolder viewHolder){
            this.viewHolder = viewHolder;
        }
    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int position) {
        for(int i = 0; i< dotsCount; i++){
            dots[i].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
        }
        dots[position].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
        ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(position).getTitle());
        ((CTCarouselMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(position).getMessage());
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}

}
