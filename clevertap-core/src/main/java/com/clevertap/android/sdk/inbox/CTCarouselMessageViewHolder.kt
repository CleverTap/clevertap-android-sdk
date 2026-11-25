package com.clevertap.android.sdk.inbox;

import static com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager.widget.ViewPager;
import com.clevertap.android.sdk.R;

/**
 * Viewholder class for Carousels
 */
class CTCarouselMessageViewHolder extends CTInboxBaseMessageViewHolder {

    /**
     * Custom PageChangeListener for Carousel
     */
    class CarouselPageChangeListener implements ViewPager.OnPageChangeListener {

        private final Context context;

        private final ImageView[] dots;

        private final CTInboxMessage inboxMessage;

        private final CTCarouselMessageViewHolder viewHolder;

        CarouselPageChangeListener(Context context, CTCarouselMessageViewHolder viewHolder, ImageView[] dots,
                CTInboxMessage inboxMessage) {
            this.context = context;
            this.viewHolder = viewHolder;
            this.dots = dots;
            this.inboxMessage = inboxMessage;
            this.dots[0].setImageDrawable(
                    ResourcesCompat.getDrawable(context.getResources(), R.drawable.ct_selected_dot, null));
        }

        @Override
        public void onPageScrollStateChanged(int i) {
        }

        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int position) {
            for (ImageView dot : this.dots) {
                dot.setImageDrawable(
                        ResourcesCompat.getDrawable(context.getResources(), R.drawable.ct_unselected_dot, null));
            }
            dots[position].setImageDrawable(
                    ResourcesCompat.getDrawable(context.getResources(), R.drawable.ct_selected_dot, null));
            viewHolder.title.setText(inboxMessage.getInboxMessageContents().get(position).getTitle());
            viewHolder.title.setTextColor(
                    Color.parseColor(inboxMessage.getInboxMessageContents().get(position).getTitleColor()));
            viewHolder.message.setText(inboxMessage.getInboxMessageContents().get(position).getMessage());
            viewHolder.message.setTextColor(
                    Color.parseColor(inboxMessage.getInboxMessageContents().get(position).getMessageColor()));
            viewHolder.imageViewPager.setAccessibilityState(
                    getImageContentDescription(context, inboxMessage, position),
                    position,
                    inboxMessage.getInboxMessageContents().size());
        }
    }

    private final RelativeLayout clickLayout;

    private final CTCarouselViewPager imageViewPager;


    private final LinearLayout sliderDots;

    private CarouselPageChangeListener activePageChangeListener;

    private final TextView title;

    private final TextView message;

    private final TextView timestamp;

    private TextView carouselTimestamp;

    CTCarouselMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        imageViewPager = itemView.findViewById(R.id.image_carousel_viewpager);
        sliderDots = itemView.findViewById(R.id.sliderDots);
        title = itemView.findViewById(R.id.messageTitle);
        message = itemView.findViewById(R.id.messageText);
        timestamp = itemView.findViewById(R.id.timestamp);
        clickLayout = itemView.findViewById(R.id.body_linear_layout);
    }

    @Override
    void configureWithMessage(final CTInboxMessage inboxMessage, final CTInboxListViewFragment parent,
            final int position) {
        super.configureWithMessage(inboxMessage, parent, position);
        final CTInboxListViewFragment parentWeak = getParent();
        // noinspection ConstantConditions
        final Context appContext = parent.getActivity().getApplicationContext();
        CTInboxMessageContent content = inboxMessage.getInboxMessageContents().get(0);
        this.title.setVisibility(View.VISIBLE);
        this.message.setVisibility(View.VISIBLE);
        this.title.setText(content.getTitle());
        this.title.setTextColor(Color.parseColor(content.getTitleColor()));
        this.message.setText(content.getMessage());
        this.message.setTextColor(Color.parseColor(content.getMessageColor()));
        if (inboxMessage.isRead()) {
            this.readDot.setVisibility(View.GONE);
        } else {
            this.readDot.setVisibility(View.VISIBLE);
        }
        this.timestamp.setVisibility(View.VISIBLE);
        String carouselDisplayTimestamp = calculateDisplayTimestamp(inboxMessage.getDate());
        this.timestamp.setText(carouselDisplayTimestamp);
        this.timestamp.setTextColor(Color.parseColor(content.getTitleColor()));
        this.clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.getBgColor()));

        //Loads the viewpager
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.imageViewPager.getLayoutParams();
        CTCarouselViewPagerAdapter carouselViewPagerAdapter = new CTCarouselViewPagerAdapter(appContext, parent,
                inboxMessage, layoutParams, position);
        // Detach the recycled holder's listener BEFORE swapping the adapter. setAdapter() resets
        // the current item to 0, and that dispatch would otherwise reach a listener still holding
        // the previous message's data and dots array.
        if (activePageChangeListener != null) {
            this.imageViewPager.removeOnPageChangeListener(activePageChangeListener);
            activePageChangeListener = null;
        }
        this.imageViewPager.setAdapter(carouselViewPagerAdapter);
        //Adds the dots for the carousel
        int dotsCount = inboxMessage.getInboxMessageContents().size();
        if (this.sliderDots.getChildCount() > 0) {
            this.sliderDots.removeAllViews();
        }
        ImageView[] dots = new ImageView[dotsCount];
        setDots(dots, dotsCount, appContext, this.sliderDots);
        dots[0].setImageDrawable(
                ResourcesCompat.getDrawable(appContext.getResources(), R.drawable.ct_selected_dot, null));
        activePageChangeListener = new CTCarouselMessageViewHolder.CarouselPageChangeListener(
                parent.getActivity().getApplicationContext(), this, dots, inboxMessage);
        this.imageViewPager.addOnPageChangeListener(activePageChangeListener);
        // onPageSelected() never fires for the initial page - ViewPager only dispatches on a
        // change - so page 0's state must be set explicitly, and last, so nothing triggered by
        // the adapter swap can overwrite it.
        this.imageViewPager.setAccessibilityState(
                getImageContentDescription(appContext, inboxMessage, 0), 0, dotsCount);

        CTInboxButtonClickListener bodyClickListener = new CTInboxButtonClickListener(position, inboxMessage, null,
                parentWeak, this.imageViewPager, true, APP_INBOX_ITEM_INDEX);
        this.clickLayout.setOnClickListener(bodyClickListener);
        // The adapter hides each page from the accessibility tree, which also hides the page's
        // own click listener. Without this a TalkBack user can page through the carousel but
        // has no way to open the message.
        this.imageViewPager.setAccessibilityClickAction(bodyClickListener);

        markItemAsRead(inboxMessage, position);
    }
}
