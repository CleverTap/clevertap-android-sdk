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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
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
    private ExoPlayerRecyclerView recyclerView;
    PlayerView playerView;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages, Activity activity, ExoPlayerRecyclerView recyclerView){
        this.inboxMessages = inboxMessages;
        this.context = activity;
        this.recyclerView = recyclerView;
//        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
//            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) this.recyclerView.getLayoutManager();
//
//            this.recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
//                @Override
//                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
//                    super.onScrollStateChanged(recyclerView, newState);
//                }
//
//                @Override
//                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
//                    super.onScrolled(recyclerView, dx, dy);
//                    int position = -1;
//                    if (linearLayoutManager != null) {
//                        position = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
//                    }
//                    if(player!=null){
//                        playerView.getTag()
//                        player.stop();
//                    }
//                }
//            });
//        }
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
                    String displayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTSimpleMessageViewHolder)viewHolder).timestamp.setText(displayTimestamp);
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
                            Logger.d("Error parsing CTA JSON - "+e.getLocalizedMessage());
                        }
                    }
                    if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                        ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                        Glide.with(((CTSimpleMessageViewHolder)viewHolder).mediaImage.getContext())
                                .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                .into(((CTSimpleMessageViewHolder)viewHolder).mediaImage);
                    } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                        ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                        Glide.with(((CTSimpleMessageViewHolder)viewHolder).mediaImage.getContext())
                                .asGif()
                                .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                .into(((CTSimpleMessageViewHolder)viewHolder).mediaImage);
                    }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                        //The below method adds videos to the respective cells but autoplay/pause on scroll needs to be added
                        addVideoView(inboxMessage.getType(),viewHolder, context,i);
                    }
                    break;
                case IconMessage:
                    ((CTIconMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTIconMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    String iconDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTIconMessageViewHolder)viewHolder).timestamp.setText(iconDisplayTimestamp);
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
                            Logger.d("Error parsing CTA JSON - "+e.getLocalizedMessage());
                        }
                    }
                    if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                        ((CTIconMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                        Glide.with(((CTIconMessageViewHolder)viewHolder).mediaImage.getContext())
                                .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                .into(((CTIconMessageViewHolder)viewHolder).mediaImage);
                    } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                        ((CTIconMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                        Glide.with(((CTIconMessageViewHolder)viewHolder).mediaImage.getContext())
                                .asGif()
                                .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                .into(((CTIconMessageViewHolder)viewHolder).mediaImage);
                    }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                        //The below method adds videos to the respective cells but autoplay/pause on scroll needs to be added
                        addVideoView(inboxMessage.getType(),viewHolder, context,i);
                    }
                    break;
                case CarouselMessage:
                    ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTCarouselMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    String carouselDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setText(carouselDisplayTimestamp);
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);

                    break;
                case CarouselImageMessage:

                    ((CTCarouselMessageViewHolder)viewHolder).title.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).message.setVisibility(View.GONE);
                    String carouselImageDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setText(carouselImageDisplayTimestamp);
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.VISIBLE);
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
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

    private String calculateDisplayTimestamp(int time){
        int now = (int)System.currentTimeMillis();
        int diff = now-time;
        if(diff < 60*1000){
            return "Just Now";
        }else if(diff > 60*1000 && diff < 59*60*1000){
            return (diff/(60*1000)) + "mins ago";
        }else if(diff > 59*60*1000 && diff < 23*59*60*1000 ){
            return diff/(60*60*1000) + "hours ago";
        }else if(diff > 24*60*60*1000 && diff < 48*60*60*1000){
            return "Yesterday";
        }else {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
            return sdf.format(new Date(time));
        }
    }

    private void addVideoView(CTInboxMessageType inboxMessageType, RecyclerView.ViewHolder viewHolder, Context context, int pos){
        playerView = new PlayerView(context);
        playerView.setTag(pos);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT));
        playerView.setShowBuffering(true);
        playerView.setUseArtwork(true);
        playerView.setControllerAutoShow(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        // 3. Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getPackageName()), (TransferListener<? super DataSource>) bandwidthMeter);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(inboxMessage.getInboxMessageContents().get(0).getMedia()));
        // 4. Prepare the player with the source.
        player.prepare(hlsMediaSource);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.seekTo(1);
        playerView.requestFocus();
        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(player);
        player.setPlayWhenReady(false);

        switch (inboxMessageType){
            case IconMessage:
                CTIconMessageViewHolder iconMessageViewHolder = (CTIconMessageViewHolder) viewHolder;

                iconMessageViewHolder.iconMessageFrameLayout.addView(playerView);
                iconMessageViewHolder.iconMessageFrameLayout.setVisibility(View.VISIBLE);
            case SimpleMessage:
                CTSimpleMessageViewHolder simpleMessageViewHolder = (CTSimpleMessageViewHolder) viewHolder;

                simpleMessageViewHolder.simpleMessageFrameLayout.addView(playerView);
                simpleMessageViewHolder.simpleMessageFrameLayout.setVisibility(View.VISIBLE);
                break;
        }


    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

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
