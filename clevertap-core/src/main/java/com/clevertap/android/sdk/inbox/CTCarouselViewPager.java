package com.clevertap.android.sdk.inbox;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
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
        setFocusable(true);
        setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        final AccessibilityDelegateCompat originalDelegate = ViewCompat.getAccessibilityDelegate(this);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                    @NonNull AccessibilityNodeInfoCompat info) {
                if (originalDelegate != null) {
                    originalDelegate.onInitializeAccessibilityNodeInfo(host, info);
                } else {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                }
                info.setClassName("android.widget.ScrollView");
                if (getAdapter() != null && getAdapter().getCount() > 1) {
                    info.setScrollable(true);
                    if (getCurrentItem() < getAdapter().getCount() - 1) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD);
                    }
                    if (getCurrentItem() > 0) {
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD);
                    }
                }
            }

            @Override
            public void onInitializeAccessibilityEvent(@NonNull View host,
                    @NonNull AccessibilityEvent event) {
                if (originalDelegate != null) {
                    originalDelegate.onInitializeAccessibilityEvent(host, event);
                } else {
                    super.onInitializeAccessibilityEvent(host, event);
                }
            }

            @Override
            public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) {
                    if (getAdapter() != null && getCurrentItem() < getAdapter().getCount() - 1) {
                        setCurrentItem(getCurrentItem() + 1, true);
                        restoreAccessibilityFocus();
                        return true;
                    }
                } else if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                    if (getCurrentItem() > 0) {
                        setCurrentItem(getCurrentItem() - 1, true);
                        restoreAccessibilityFocus();
                        return true;
                    }
                }
                if (originalDelegate != null) {
                    return originalDelegate.performAccessibilityAction(host, action, args);
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    private void restoreAccessibilityFocus() {
        postDelayed(() -> {
            clearFocus();
            requestFocus();
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
            ViewCompat.performAccessibilityAction(
                    CTCarouselViewPager.this,
                    AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS,
                    null
            );
        }, 300);
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
