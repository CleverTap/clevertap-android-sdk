package com.clevertap.android.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Custom ViewPager Adapter to add views to the Carousel
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class CTCarouselViewPagerAdapter extends PagerAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<String> carouselImages;
    private View view;
    private LinearLayout.LayoutParams layoutParams;
    private CTInboxMessage inboxMessage;
    private int row;

    CTCarouselViewPagerAdapter(Context context, CTInboxMessage inboxMessage, LinearLayout.LayoutParams layoutParams, int row) {
        this.context = context;
        this.carouselImages = inboxMessage.getCarouselImages();
        this.layoutParams = layoutParams;
        this.inboxMessage = inboxMessage;
        this.row = row;
    }

    @Override
    public int getCount() {
        return carouselImages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //noinspection ConstantConditions
        view = layoutInflater.inflate(R.layout.inbox_carousel_image_layout,container,false);
        ImageView imageView = view.findViewById(R.id.imageView);
        imageView.setVisibility(View.VISIBLE);
        Glide.with(imageView.getContext())
                .load(carouselImages.get(position))
                .into(imageView);
        container.addView(view,layoutParams);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((CTInboxActivity)context).handleViewPagerClick(row,position);
            }
        });
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }
}
