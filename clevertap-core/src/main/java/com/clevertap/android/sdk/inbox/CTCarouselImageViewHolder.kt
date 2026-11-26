package com.clevertap.android.sdk.inbox

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager.widget.ViewPager
import com.clevertap.android.sdk.Constants.APP_INBOX_ITEM_INDEX
import com.clevertap.android.sdk.R
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CTCarouselImageViewHolder(itemView: View) : CTInboxBaseMessageViewHolder(itemView) {

    /**
     * Custom PageChangeListener for Carousel
     */
    private inner class CarouselPageChangeListener(
        private val context: Context,
        private val dots: Array<ImageView>,
        private val inboxMessage: CTInboxMessage
    ) : ViewPager.SimpleOnPageChangeListener() {

        init {
            dots[0].setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ct_selected_dot, null))
        }

        override fun onPageSelected(position: Int) {
            for (dot in dots) {
                dot.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.ct_unselected_dot, null))
            }
            dots[position].setImageDrawable(
                ResourcesCompat.getDrawable(context.resources, R.drawable.ct_selected_dot, null)
            )
            imageViewPager.setAccessibilityState(
                getImageContentDescription(context, inboxMessage, position),
                position,
                inboxMessage.inboxMessageContents.size
            )
        }
    }

    private val carouselTimestamp: TextView = itemView.findViewById(R.id.carousel_timestamp)

    private val clickLayout: RelativeLayout = itemView.findViewById(R.id.body_linear_layout)

    private val imageViewPager: CTCarouselViewPager = itemView.findViewById(R.id.image_carousel_viewpager)

    private val sliderDots: LinearLayout = itemView.findViewById(R.id.sliderDots)

    private var activePageChangeListener: CarouselPageChangeListener? = null

    override fun configureWithMessage(
        inboxMessage: CTInboxMessage,
        parent: CTInboxListViewFragment,
        position: Int
    ) {
        super.configureWithMessage(inboxMessage, parent, position)
        // Java dereferenced the activity unchecked as well; a bound list always has one.
        val appContext = parent.requireActivity().applicationContext
        val content = inboxMessage.inboxMessageContents[0]
        carouselTimestamp.visibility = View.VISIBLE
        showReadState(inboxMessage.isRead)
        carouselTimestamp.text = calculateDisplayTimestamp(inboxMessage.date)
        carouselTimestamp.setTextColor(Color.parseColor(content.titleColor))

        clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))

        //Loads the viewpager
        val layoutParams = imageViewPager.layoutParams as LinearLayout.LayoutParams?
        val carouselViewPagerAdapter =
            CTCarouselViewPagerAdapter(appContext, parent, inboxMessage, layoutParams, position)
        // Detach the recycled holder's listener BEFORE swapping the adapter. setAdapter() resets
        // the current item to 0, and that dispatch would otherwise reach a listener still holding
        // the previous message's data and dots array.
        activePageChangeListener?.let {
            imageViewPager.removeOnPageChangeListener(it)
            activePageChangeListener = null
        }
        imageViewPager.adapter = carouselViewPagerAdapter
        //Adds the dots for the carousel
        val dotsCount = inboxMessage.inboxMessageContents.size
        if (sliderDots.childCount > 0) {
            sliderDots.removeAllViews()
        }
        val dots = createDots(dotsCount, appContext, sliderDots)
        dots[0].setImageDrawable(ResourcesCompat.getDrawable(appContext.resources, R.drawable.ct_selected_dot, null))
        val pageChangeListener = CarouselPageChangeListener(appContext, dots, inboxMessage)
        activePageChangeListener = pageChangeListener
        imageViewPager.addOnPageChangeListener(pageChangeListener)
        // onPageSelected() never fires for the initial page - ViewPager only dispatches on a
        // change - so page 0's state must be set explicitly, and last, so nothing triggered by
        // the adapter swap can overwrite it.
        imageViewPager.setAccessibilityState(getImageContentDescription(appContext, inboxMessage, 0), 0, dotsCount)

        val bodyClickListener = CTInboxButtonClickListener(
            position = position,
            inboxMessage = inboxMessage,
            fragment = parent,
            viewPager = imageViewPager,
            buttonIndex = APP_INBOX_ITEM_INDEX
        )
        clickLayout.setOnClickListener(bodyClickListener)
        // The adapter hides each page from the accessibility tree, which also hides the page's
        // own click listener. Without this a TalkBack user can page through the carousel but
        // has no way to open the message.
        imageViewPager.setAccessibilityClickAction(bodyClickListener)

        markItemAsRead(inboxMessage, position)
    }
}
