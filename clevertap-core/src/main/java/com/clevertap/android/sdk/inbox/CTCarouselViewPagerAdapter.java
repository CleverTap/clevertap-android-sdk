package com.clevertap.android.sdk.inbox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.Utils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Custom ViewPager Adapter to add views to the Carousel
 */
@SuppressWarnings({"FieldCanBeLocal"})
@RestrictTo(Scope.LIBRARY)
public class CTCarouselViewPagerAdapter extends PagerAdapter {

    private final ArrayList<String> carouselImages;

    private final Context context;

    private final CTInboxMessage inboxMessage;

    private LayoutInflater layoutInflater;

    private final LinearLayout.LayoutParams layoutParams;

    private final WeakReference<CTInboxListViewFragment> parentWeakReference;

    private final int row;

    private View view;

    CTCarouselViewPagerAdapter(Context context, CTInboxListViewFragment parent, CTInboxMessage inboxMessage,
            LinearLayout.LayoutParams layoutParams, int row) {
        this.context = context;
        this.parentWeakReference = new WeakReference<>(parent);
        this.carouselImages = inboxMessage.getCarouselImages();
        this.layoutParams = layoutParams;
        this.inboxMessage = inboxMessage;
        this.row = row;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }

    @Override
    public int getCount() {
        return carouselImages.size();
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //noinspection Constant Conditions
        view = layoutInflater.inflate(R.layout.inbox_carousel_image_layout, container, false);
        try {
            if (inboxMessage.getOrientation().equalsIgnoreCase("l")) {
                ImageView imageView = view.findViewById(R.id.imageView);
                addImageAndSetClick(imageView, view, position, container);
            } else if (inboxMessage.getOrientation().equalsIgnoreCase("p")) {
                ImageView imageView = view.findViewById(R.id.squareImageView);
                addImageAndSetClick(imageView, view, position, container);
            }
        } catch (NoClassDefFoundError error) {
            Logger.d("CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info");
        }
        return view;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    void addImageAndSetClick(ImageView imageView, View view, final int position, ViewGroup container) {
        imageView.setVisibility(View.VISIBLE);
        try {
            Glide.with(imageView.getContext())
                    .load(carouselImages.get(position))
                    .apply(new RequestOptions()
                            .placeholder(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                            .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER)))
                    .into(imageView);
        } catch (NoSuchMethodError error) {
            Logger.d(
                    "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info");
            Glide.with(imageView.getContext())
                    .load(carouselImages.get(position))
                    .into(imageView);
        }

        container.addView(view, layoutParams);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CTInboxListViewFragment parent = getParent();
                if (parent != null) {
                    parent.handleViewPagerClick(row, position);
                }
            }
        });
    }

    CTInboxListViewFragment getParent() {
        return parentWeakReference.get();
    }
}
