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

    private val carouselImagesData: ArrayList<CTInboxImageData> = inboxMessage.carouselImagesData

    private var layoutInflater: LayoutInflater? = null

    private val parentWeakReference: WeakReference<CTInboxListViewFragment> = WeakReference(parent)

    private var view: View? = null

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        container.removeView(view)
    }

    override fun getCount(): Int {
        return carouselImagesData.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater?
        this.layoutInflater = layoutInflater
        val view = layoutInflater!!.inflate(R.layout.inbox_carousel_image_layout, container, false)
        this.view = view
        try {
            if (inboxMessage.orientation!!.equals("l", ignoreCase = true)) {
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                addImageAndSetClick(imageView, view, position, container)
            } else if (inboxMessage.orientation!!.equals("p", ignoreCase = true)) {
                val imageView = view.findViewById<ImageView>(R.id.squareImageView)
                addImageAndSetClick(imageView, view, position, container)
            } else {
                val imageView = view.findViewById<ImageView>(R.id.defaultImageView)
                imageView.scaleType = ImageView.ScaleType.FIT_CENTER
                addImageAndSetClick(imageView, view, position, container)
            }
        } catch (error: NoClassDefFoundError) {
            Logger.d("CleverTap SDK requires Glide dependency. Please refer CleverTap Documentation for more info")
        }
        return view
    }

    override fun isViewFromObject(view: View, o: Any): Boolean {
        return view === o
    }

    fun addImageAndSetClick(imageView: ImageView, view: View, position: Int, container: ViewGroup) {
        imageView.visibility = View.VISIBLE
        imageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        try {
            Glide.with(imageView.context)
                .load(carouselImagesData[position].url)
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
                .load(carouselImagesData[position].url)
                .into(imageView)
        }

        container.addView(view, layoutParams)
        view.setOnClickListener {
            val parent = getParent()
            if (parent != null) {
                parent.handleViewPagerClick(row, position)
            }
        }
    }

    fun getParent(): CTInboxListViewFragment? {
        return parentWeakReference.get()
    }
}
