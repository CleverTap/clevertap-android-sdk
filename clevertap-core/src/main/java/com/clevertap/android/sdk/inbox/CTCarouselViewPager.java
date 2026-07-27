package com.clevertap.android.sdk.inbox;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.viewpager.widget.ViewPager;

import com.clevertap.android.sdk.R;

@RestrictTo(Scope.LIBRARY)
public class CTCarouselViewPager extends ViewPager {

    public CTCarouselViewPager(Context context) {
        super(context);
        setupAccessibility();
    }

    public CTCarouselViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupAccessibility();
    }

    private void setupAccessibility() {
        // YES is load-bearing, not defensive. Under the default AUTO,
        // View.includeForAccessibility() keeps a view only if it is actionable, has
        // touch/hover listeners, exposes a node provider, or is a live region. A ViewPager is
        // none of those - it consumes touch inside onTouchEvent() rather than through a
        // listener - and contentDescription is NOT part of that test. So under AUTO the pager
        // is dropped from the tree entirely and TalkBack skips the carousel even when a
        // contentDescription has been set. This line is what fixes that.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);

        // Present the pager as one atomic node instead of letting the reader dive into it.
        // This is the ViewCompat equivalent of the old setFocusable() pair - and unlike them
        // it describes screen-reader intent rather than input focus.
        ViewCompat.setScreenReaderFocusable(this, true);

        // Stable identity only. The part that changes per page (image alt text + "page x of y")
        // is published as stateDescription by setAccessibilityState(), which TalkBack announces
        // on its own - no interruptive announceForAccessibility() needed.
        setContentDescription(getContext().getString(R.string.ct_carousel_label));

        // Makes the node report isClickable() so TalkBack offers "double-tap to activate" for
        // the ACTION_CLICK installed by setAccessibilityClickAction(). This does not change
        // touch behaviour: ViewPager overrides onTouchEvent() without delegating to
        // View.onTouchEvent(), so performClick() is never reached from a real touch.
        setClickable(true);
    }

    /**
     * Publishes the currently visible page to screen readers as a state change.
     * <p>
     * Call this on bind and from {@code OnPageChangeListener.onPageSelected}. Uses
     * stateDescription rather than contentDescription so TalkBack announces the change itself
     * and the carousel is not announced twice.
     *
     * @param pageDescription description of the current page's content
     * @param position        zero-based index of the current page
     * @param total           total number of pages
     */
    void setAccessibilityState(@NonNull CharSequence pageDescription, int position, int total) {
        ViewCompat.setStateDescription(this, getContext().getString(
                R.string.ct_carousel_position, pageDescription, position + 1, total));
    }

    /**
     * Installs an accessibility-only {@code ACTION_CLICK} so a screen-reader user can open the
     * carousel message.
     * <p>
     * Needed because the adapter hides each page from the accessibility tree, which also hides
     * the per-page {@code OnClickListener}; without this the carousel can be paged through but
     * never opened. Registered as an accessibility action rather than via
     * {@code setOnClickListener} because ViewPager consumes the touch stream in
     * {@code onTouchEvent()} and never calls {@code performClick()}.
     *
     * @param listener the same listener used for the row body click
     */
    void setAccessibilityClickAction(@NonNull final View.OnClickListener listener) {
        ViewCompat.replaceAccessibilityAction(
                this,
                AccessibilityActionCompat.ACTION_CLICK,
                getContext().getString(R.string.ct_carousel_open_message),
                (view, arguments) -> {
                    listener.onClick(view);
                    return true;
                });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            int h = child.getMeasuredHeight();
            if (h > height) {
                height = h;
            }
        }

        if (height != 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
