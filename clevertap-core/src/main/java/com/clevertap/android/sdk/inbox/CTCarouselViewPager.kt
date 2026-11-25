package com.clevertap.android.sdk.inbox

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.viewpager.widget.ViewPager
import com.clevertap.android.sdk.R

internal open class CTCarouselViewPager : ViewPager {

    constructor(context: Context) : super(context) {
        setupAccessibility()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setupAccessibility()
    }

    private fun setupAccessibility() {
        // YES is load-bearing, not defensive. Under the default AUTO,
        // View.includeForAccessibility() keeps a view only if it is actionable, has
        // touch/hover listeners, exposes a node provider, or is a live region. A ViewPager is
        // none of those - it consumes touch inside onTouchEvent() rather than through a
        // listener - and contentDescription is NOT part of that test. So under AUTO the pager
        // is dropped from the tree entirely and TalkBack skips the carousel even when a
        // contentDescription has been set. This line is what fixes that.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES

        // Present the pager as one atomic node instead of letting the reader dive into it.
        // This is the ViewCompat equivalent of the old setFocusable() pair - and unlike them
        // it describes screen-reader intent rather than input focus.
        ViewCompat.setScreenReaderFocusable(this, true)

        // Stable identity only. The part that changes per page (image alt text + "page x of y")
        // is published as stateDescription by setAccessibilityState(), which TalkBack announces
        // on its own - no interruptive announceForAccessibility() needed.
        contentDescription = context.getString(R.string.ct_carousel_label)

        // Makes the node report isClickable() so TalkBack offers "double-tap to activate" for
        // the ACTION_CLICK installed by setAccessibilityClickAction(). This does not change
        // touch behaviour: ViewPager overrides onTouchEvent() without delegating to
        // View.onTouchEvent(), so performClick() is never reached from a real touch.
        isClickable = true
    }

    /**
     * Publishes the currently visible page to screen readers as a state change.
     *
     * Call this on bind and from `OnPageChangeListener.onPageSelected`. Uses
     * stateDescription rather than contentDescription so TalkBack announces the change itself
     * and the carousel is not announced twice.
     *
     * @param pageDescription description of the current page's content
     * @param position        zero-based index of the current page
     * @param total           total number of pages
     */
    fun setAccessibilityState(pageDescription: CharSequence, position: Int, total: Int) {
        ViewCompat.setStateDescription(
            this, context.getString(
                R.string.ct_carousel_position, pageDescription, position + 1, total
            )
        )
    }

    /**
     * Installs an accessibility-only `ACTION_CLICK` so a screen-reader user can open the
     * carousel message.
     *
     * Needed because the adapter hides each page from the accessibility tree, which also hides
     * the per-page `OnClickListener`; without this the carousel can be paged through but
     * never opened. Registered as an accessibility action rather than via
     * `setOnClickListener` because ViewPager consumes the touch stream in
     * `onTouchEvent()` and never calls `performClick()`.
     *
     * @param listener the same listener used for the row body click
     */
    fun setAccessibilityClickAction(listener: View.OnClickListener) {
        ViewCompat.replaceAccessibilityAction(
            this,
            AccessibilityActionCompat.ACTION_CLICK,
            context.getString(R.string.ct_carousel_open_message)
        ) { view, _ ->
            listener.onClick(view)
            true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightSpec = heightMeasureSpec
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val h = child.measuredHeight
            if (h > height) {
                height = h
            }
        }

        if (height != 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        }

        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
