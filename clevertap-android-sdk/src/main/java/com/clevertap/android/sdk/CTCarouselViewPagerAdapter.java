package com.clevertap.android.sdk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class CTCarouselViewPagerAdapter extends PagerAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private ArrayList<String> carouselImages;
    private View view;
    private LinearLayout.LayoutParams layoutParams;

    CTCarouselViewPagerAdapter(Context context, ArrayList<String> carouselImages, LinearLayout.LayoutParams layoutParams) {
        this.context = context;
        this.carouselImages = carouselImages;
        this.layoutParams = layoutParams;
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
    public Object instantiateItem(@NonNull ViewGroup container, int position) {

        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.inbox_carousel_image_layout,container,false);
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        imageView.setVisibility(View.VISIBLE);
        Glide.with(imageView.getContext())
                .load(carouselImages.get(position))
                .into(imageView);
        container.addView(view,layoutParams);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }
}
