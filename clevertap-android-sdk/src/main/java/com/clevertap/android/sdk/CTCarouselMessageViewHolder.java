package com.clevertap.android.sdk;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

class CTCarouselMessageViewHolder extends RecyclerView.ViewHolder {

    CTCarouselViewPager imageViewPager;
    LinearLayout sliderDots;
    TextView title,message,timestamp, carouselTimestamp;
    ImageView readDot,carouselReadDot;
    Button cta1,cta2,cta3;
    RelativeLayout clickLayout;

    CTCarouselMessageViewHolder(@NonNull View itemView) {
        super(itemView);

        imageViewPager = itemView.findViewById(R.id.image_carousel_viewpager);
        sliderDots = itemView.findViewById(R.id.sliderDots);
        title = itemView.findViewById(R.id.messageTitle);
        message = itemView.findViewById(R.id.messageText);
        timestamp = itemView.findViewById(R.id.timestamp);
        carouselTimestamp = itemView.findViewById(R.id.carousel_timestamp);
        readDot = itemView.findViewById(R.id.read_circle);
        carouselReadDot = itemView.findViewById(R.id.carousel_read_circle);
        cta1 = itemView.findViewById(R.id.cta_button_1);
        cta2 = itemView.findViewById(R.id.cta_button_2);
        cta3 = itemView.findViewById(R.id.cta_button_3);
        clickLayout = itemView.findViewById(R.id.body_relative_layout);
    }
}
