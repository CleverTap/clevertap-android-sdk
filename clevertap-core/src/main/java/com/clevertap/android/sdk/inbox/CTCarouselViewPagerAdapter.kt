package com.clevertap.android.sdk.inbox

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.Utils
import java.lang.ref.WeakReference

/**
 * Custom ViewPager Adapter to add views to the Carousel
 */
internal open class CTCarouselViewPagerAdapter(
    private val context: Context,
    parent: CTInboxListViewFragment?,
    private val inboxMessage: CTInboxMessage,
    private val layoutParams: LinearLayout.LayoutParams?,
    private val row: Int
) : PagerAdapter() {

    private val carouselImagesData: List<CTInboxImageData> = inboxMessage.carouselImagesData

    private val parentWeakReference: WeakReference<CTInboxListViewFragment> = WeakReference(parent)

    private val parent: CTInboxListViewFragment?
        get() = parentWeakReference.get()

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int = carouselImagesData.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.inbox_carousel_image_layout, container, false)
        try {
            // Java dereferenced the orientation unchecked; only a message without a "msg" object has none.
            val orientation = checkNotNull(inboxMessage.orientation)
            val imageView = when {
                orientation.equals("l", ignoreCase = true) -> view.findViewById<ImageView>(R.id.imageView)
                orientation.equals("p", ignoreCase = true) -> view.findViewById(R.id.squareImageView)
                else -> view.findViewById<ImageView>(R.id.defaultImageView).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }
            addImageAndSetClick(imageView, view, position, container)
        } catch (error: NoClassDefFoundError) {
            Logger.d("CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info")
        }
        return view
    }

    override fun isViewFromObject(view: View, o: Any): Boolean = view === o

    fun addImageAndSetClick(imageView: ImageView, view: View, position: Int, container: ViewGroup) {
        imageView.visibility = View.VISIBLE
        imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        val url = carouselImagesData[position].url
        try {
            Glide.with(imageView.context)
                .load(url)
                .apply(
                    RequestOptions()
                        .placeholder(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                        .error(Utils.getThumbnailImage(context, Constants.IMAGE_PLACEHOLDER))
                )
                .into(imageView)
        } catch (error: NoSuchMethodError) {
            Logger.d(
                "CleverTap SDK requires Glide v4.9.0 or above. Please refer CleverTap Documentation for more info"
            )
            Glide.with(imageView.context)
                .load(url)
                .into(imageView)
        }

        container.addView(view, layoutParams)
        view.setOnClickListener { parent?.handleViewPagerClick(row, position) }
    }
}
