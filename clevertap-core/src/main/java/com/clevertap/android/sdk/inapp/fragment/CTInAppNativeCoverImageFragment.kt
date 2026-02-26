package com.clevertap.android.sdk.inapp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.inapp.media.InAppMediaConfig
import com.clevertap.android.sdk.inapp.media.InAppMediaHandler
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeCoverImageFragment : CTInAppBaseFullFragment() {

    private lateinit var mediaHandler: InAppMediaHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaHandler = InAppMediaHandler.create(
            fragment = this,
            inAppNotification = inAppNotification,
            currentOrientation = currentOrientation,
            isTablet = inAppNotification.isTablet && isTablet(),
            resourceProvider = resourceProvider(),
            supportsStreamMedia = true
        )
        lifecycle.addObserver(mediaHandler)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inAppView = inflater.inflate(R.layout.inapp_cover_image, container, false)
        inAppView.applyInsetsWithMarginAdjustment { insets, mlp ->
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            mlp.topMargin = insets.top
            mlp.bottomMargin = insets.bottom
        }

        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_cover_image_frame_layout)
        fl.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())

        val relativeLayout = fl.findViewById<RelativeLayout>(R.id.cover_image_relative_layout)

        mediaHandler.setup(
            relativeLayout,
            InAppMediaConfig(imageViewId = R.id.cover_image, clickableMedia = true, videoFrameId = R.id.video_frame),
            CTInAppNativeButtonClickListener()
        )

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        closeImageView.setOnClickListener {
            didDismiss(null)
            mediaHandler.clear()
            activity?.finish()
        }

        if (!inAppNotification.isHideCloseButton) {
            closeImageView.setVisibility(View.GONE)
        } else {
            closeImageView.setVisibility(View.VISIBLE)
        }

        return inAppView
    }

    override fun cleanup() {
        lifecycle.removeObserver(mediaHandler)
        mediaHandler.cleanup()
        super.cleanup()
    }
}
