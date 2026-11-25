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

internal class CTCarouselImageViewHolder(itemView: View) : CTInboxBaseMessageViewHolder(itemView) {

    /**
     * Custom PageChangeListener for Carousel
     */
    inner class CarouselPageChangeListener(
        private val context: Context,
        private val viewHolder: CTCarouselImageViewHolder,
        private val dots: Array<ImageView?>,
        private val inboxMessage: CTInboxMessage
    ) : ViewPager.OnPageChangeListener {

        init {
            this.dots[0]!!.setImageDrawable(
                ResourcesCompat.getDrawable(context.resources, R.drawable.ct_selected_dot, null)
            )
        }

        override fun onPageScrollStateChanged(i: Int) {
        }

        override fun onPageScrolled(i: Int, v: Float, i1: Int) {

        }

        override fun onPageSelected(position: Int) {
            for (dot in this.dots) {
                dot!!.setImageDrawable(
                    ResourcesCompat.getDrawable(context.resources, R.drawable.ct_unselected_dot, null)
                )
            }
            dots[position]!!.setImageDrawable(
                ResourcesCompat.getDrawable(context.resources, R.drawable.ct_selected_dot, null)
            )
            viewHolder.imageViewPager!!.setAccessibilityState(
                getImageContentDescription(context, inboxMessage, position),
                position,
                inboxMessage.inboxMessageContents.size
            )
        }
    }

    private val carouselTimestamp: TextView? = itemView.findViewById(R.id.carousel_timestamp)

    // Java declared this as a private field shadowing the base holder's clickLayout (which stays
    // null for this holder); Kotlin cannot shadow, so it is renamed.
    private val carouselClickLayout: RelativeLayout? = itemView.findViewById(R.id.body_linear_layout)

    private val imageViewPager: CTCarouselViewPager? = itemView.findViewById(R.id.image_carousel_viewpager)

    private val sliderDots: LinearLayout? = itemView.findViewById(R.id.sliderDots)

    private var activePageChangeListener: CarouselPageChangeListener? = null

    override fun configureWithMessage(
        inboxMessage: CTInboxMessage,
        parent: CTInboxListViewFragment,
        position: Int
    ) {
        super.configureWithMessage(inboxMessage, parent, position)
        val parentWeak = getParent()
        // noinspection ConstantConditions
        val appContext = parent.activity!!.applicationContext
        val content = inboxMessage.inboxMessageContents[0]
        val carouselTimestamp = this.carouselTimestamp!!
        val clickLayout = this.carouselClickLayout!!
        val imageViewPager = this.imageViewPager!!
        val sliderDots = this.sliderDots!!
        carouselTimestamp.visibility = View.VISIBLE
        if (inboxMessage.isRead) {
            readDot!!.visibility = View.GONE
        } else {
            readDot!!.visibility = View.VISIBLE
        }
        val carouselImageDisplayTimestamp = calculateDisplayTimestamp(inboxMessage.date)
        carouselTimestamp.text = carouselImageDisplayTimestamp
        carouselTimestamp.setTextColor(Color.parseColor(content.titleColor))

        clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.bgColor))

        //Loads the viewpager
        val layoutParams = imageViewPager.layoutParams as LinearLayout.LayoutParams?
        val carouselViewPagerAdapter = CTCarouselViewPagerAdapter(
            appContext, parent,
            inboxMessage, layoutParams, position
        )
        // Detach the recycled holder's listener BEFORE swapping the adapter. setAdapter() resets
        // the current item to 0, and that dispatch would otherwise reach a listener still holding
        // the previous message's data and dots array.
        val previousListener = activePageChangeListener
        if (previousListener != null) {
            imageViewPager.removeOnPageChangeListener(previousListener)
            activePageChangeListener = null
        }
        imageViewPager.adapter = carouselViewPagerAdapter
        //Adds the dots for the carousel
        val dotsCount = inboxMessage.inboxMessageContents.size
        if (sliderDots.childCount > 0) {
            sliderDots.removeAllViews()
        }
        val dots = arrayOfNulls<ImageView>(dotsCount)
        setDots(dots, dotsCount, appContext, sliderDots)
        dots[0]!!.setImageDrawable(
            ResourcesCompat.getDrawable(appContext.resources, R.drawable.ct_selected_dot, null)
        )
        val pageChangeListener = CarouselPageChangeListener(
            parent.activity!!.applicationContext, this, dots, inboxMessage
        )
        activePageChangeListener = pageChangeListener
        imageViewPager.addOnPageChangeListener(pageChangeListener)
        // onPageSelected() never fires for the initial page - ViewPager only dispatches on a
        // change - so page 0's state must be set explicitly, and last, so nothing triggered by
        // the adapter swap can overwrite it.
        imageViewPager.setAccessibilityState(
            getImageContentDescription(appContext, inboxMessage, 0), 0, dotsCount
        )

        val bodyClickListener = CTInboxButtonClickListener(
            position, inboxMessage, null,
            parentWeak, imageViewPager, true, APP_INBOX_ITEM_INDEX
        )
        clickLayout.setOnClickListener(bodyClickListener)
        // The adapter hides each page from the accessibility tree, which also hides the page's
        // own click listener. Without this a TalkBack user can page through the carousel but
        // has no way to open the message.
        imageViewPager.setAccessibilityClickAction(bodyClickListener)

        markItemAsRead(inboxMessage, position)
    }
}
