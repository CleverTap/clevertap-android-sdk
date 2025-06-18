package com.clevertap.android.sdk.inapp.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
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

internal class CTInAppNativeFooterFragment : CTInAppBasePartialNativeFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inAppButtons = mutableListOf<Button>()
        val inAppView = inflater.inflate(R.layout.inapp_footer, container, false)
        this.inAppView = inAppView

        val fl = inAppView.findViewById<FrameLayout>(R.id.footer_frame_layout)
        val relativeLayout = fl.findViewById<RelativeLayout>(R.id.footer_relative_layout)
        relativeLayout.setBackgroundColor(inAppNotification.backgroundColor.toColorInt())
        val linearLayout1 = relativeLayout.findViewById<LinearLayout>(R.id.footer_linear_layout_1)
        val linearLayout2 = relativeLayout.findViewById<LinearLayout>(R.id.footer_linear_layout_2)
        val linearLayout3 = relativeLayout.findViewById<LinearLayout>(R.id.footer_linear_layout_3)

        val mainButton = linearLayout3.findViewById<Button>(R.id.footer_button_1)
        inAppButtons.add(mainButton)
        val secondaryButton = linearLayout3.findViewById<Button>(R.id.footer_button_2)
        inAppButtons.add(secondaryButton)

        val imageView = linearLayout1.findViewById<ImageView>(R.id.footer_icon)
        if (!inAppNotification.mediaList.isEmpty()) {
            val image = resourceProvider().cachedInAppImageV1(
                inAppNotification.mediaList[0].mediaUrl
            )
            if (image != null) {
                imageView.setImageBitmap(image)
            } else {
                imageView.setVisibility(View.GONE)
            }
        } else {
            imageView.setVisibility(View.GONE)
        }

        val textView1 = linearLayout2.findViewById<TextView>(R.id.footer_title)
        textView1.text = inAppNotification.title
        textView1.setTextColor(inAppNotification.titleColor.toColorInt())

        val textView2 = linearLayout2.findViewById<TextView>(R.id.footer_message)
        textView2.text = inAppNotification.message
        textView2.setTextColor(inAppNotification.messageColor.toColorInt())

        val buttons = inAppNotification.buttons
        if (buttons != null && !buttons.isEmpty()) {
            for (i in buttons.indices) {
                if (i >= 2) {
                    break  // only show 2 buttons
                }
                val inAppNotificationButton = buttons[i]
                val button = inAppButtons[i]
                setupInAppButton(button, inAppNotificationButton, i)
            }
        }

        if (inAppNotification.buttonCount == 1) {
            hideSecondaryButton(mainButton, secondaryButton)
        }

        inAppView.setOnTouchListener(object : OnTouchListener {
            @SuppressLint("ClickableViewAccessibility")
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                gd.onTouchEvent(event)
                return true
            }
        })
        inAppView.applyInsetsWithMarginAdjustment { insets, mlp ->
            mlp.leftMargin = insets.left
            mlp.rightMargin = insets.right
            mlp.bottomMargin = insets.bottom
        }
        return inAppView
    }
}
