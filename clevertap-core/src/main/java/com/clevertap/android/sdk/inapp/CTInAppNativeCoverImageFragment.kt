package com.clevertap.android.sdk.inapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeCoverImageFragment : CTInAppBaseFullFragment() {

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
        val imageView = relativeLayout.findViewById<ImageView>(R.id.cover_image)

        val mediaForOrientation = inAppNotification.getInAppMediaForOrientation(currentOrientation)
        if (mediaForOrientation != null) {
            val bitmap = resourceProvider().cachedInAppImageV1(mediaForOrientation.mediaUrl)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.tag = 0
                imageView.setOnClickListener(CTInAppNativeButtonClickListener())
            }
        }

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        closeImageView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                didDismiss(null)
                activity?.finish()
            }
        })

        if (!inAppNotification.isHideCloseButton) {
            closeImageView.setVisibility(View.GONE)
        } else {
            closeImageView.setVisibility(View.VISIBLE)
        }

        return inAppView
    }
}
