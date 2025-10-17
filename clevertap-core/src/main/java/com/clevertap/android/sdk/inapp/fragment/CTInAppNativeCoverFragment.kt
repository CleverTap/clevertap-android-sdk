package com.clevertap.android.sdk.inapp.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.clevertap.android.sdk.R
import com.clevertap.android.sdk.applyInsetsWithMarginAdjustment
import com.clevertap.android.sdk.customviews.CloseImageView

internal class CTInAppNativeCoverFragment : CTInAppBaseFullNativeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inAppButtons = ArrayList<Button>()
        val inAppView = inflater.inflate(R.layout.inapp_cover, container, false)
        inAppView.applyInsetsWithMarginAdjustment { insets, mlp ->
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            mlp.topMargin = insets.top
            mlp.bottomMargin = insets.bottom
        }

        val fl = inAppView.findViewById<FrameLayout>(R.id.inapp_cover_frame_layout)

        val relativeLayout = fl.findViewById<RelativeLayout>(R.id.cover_relative_layout)
        relativeLayout.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        val linearLayout = relativeLayout.findViewById<LinearLayout>(R.id.cover_linear_layout)
        val mainButton = linearLayout.findViewById<Button>(R.id.cover_button1)
        inAppButtons.add(mainButton)
        val secondaryButton = linearLayout.findViewById<Button>(R.id.cover_button2)
        inAppButtons.add(secondaryButton)
        val imageView = relativeLayout.findViewById<ImageView>(R.id.backgroundImage)

        val mediaForOrientation = inAppNotification.getInAppMediaForOrientation(currentOrientation)
        if (mediaForOrientation != null) {
            if (mediaForOrientation.contentDescription.isNotBlank()) {
                imageView.contentDescription = mediaForOrientation.contentDescription
            }
            val bitmap = resourceProvider().cachedInAppImageV1(mediaForOrientation.mediaUrl)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                imageView.tag = 0
            }
        }

        val textView1 = relativeLayout.findViewById<TextView>(R.id.cover_title)
        textView1.text = inAppNotification.title
        textView1.setTextColor(inAppNotification.titleColor.toColorInt())

        val textView2 = relativeLayout.findViewById<TextView>(R.id.cover_message)
        textView2.text = inAppNotification.message
        textView2.setTextColor(inAppNotification.messageColor.toColorInt())

        val buttons = inAppNotification.buttons
        if (buttons.size == 1) {
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mainButton.visibility = View.GONE
            } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                mainButton.visibility = View.INVISIBLE
            }
            setupInAppButton(secondaryButton, buttons[0], 0)
        } else if (!buttons.isEmpty()) {
            for (i in buttons.indices) {
                if (i >= 2) {
                    continue  // only show 2 buttons
                }
                val inAppNotificationButton = buttons[i]
                val button = inAppButtons[i]
                setupInAppButton(button, inAppNotificationButton, i)
            }
        }

        val closeImageView = fl.findViewById<CloseImageView>(CloseImageView.VIEW_ID)

        closeImageView.setOnClickListener {
            didDismiss(null)
            activity?.finish()
        }

        if (!inAppNotification.isHideCloseButton) {
            closeImageView.setVisibility(View.GONE)
        } else {
            closeImageView.setVisibility(View.VISIBLE)
        }

        return inAppView
    }
}
