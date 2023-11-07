package com.clevertap.android.sdk.inapp;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.clevertap.android.sdk.R;
import java.util.ArrayList;

public class CTInAppNativeHeaderFragment extends CTInAppBasePartialNativeFragment {

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ArrayList<Button> inAppButtons = new ArrayList<>();
        inAppView = inflater.inflate(R.layout.inapp_header, container, false);

        FrameLayout fl = inAppView.findViewById(R.id.header_frame_layout);

        RelativeLayout relativeLayout = fl.findViewById(R.id.header_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));
        LinearLayout linearLayout1 = relativeLayout.findViewById(R.id.header_linear_layout_1);
        LinearLayout linearLayout2 = relativeLayout.findViewById(R.id.header_linear_layout_2);
        LinearLayout linearLayout3 = relativeLayout.findViewById(R.id.header_linear_layout_3);

        Button mainButton = linearLayout3.findViewById(R.id.header_button_1);
        inAppButtons.add(mainButton);
        Button secondaryButton = linearLayout3.findViewById(R.id.header_button_2);
        inAppButtons.add(secondaryButton);

        ImageView imageView = linearLayout1.findViewById(R.id.header_icon);
        if (!inAppNotification.getMediaList().isEmpty()) {
            Bitmap image = resourceProvider().cachedImage(inAppNotification.getMediaList().get(0).getMediaUrl());
            if (image != null) {
                imageView.setImageBitmap(image);
            } else {
                imageView.setVisibility(View.GONE);
            }
        } else {
            imageView.setVisibility(View.GONE);
        }

        TextView textView1 = linearLayout2.findViewById(R.id.header_title);
        textView1.setText(inAppNotification.getTitle());
        textView1.setTextColor(Color.parseColor(inAppNotification.getTitleColor()));

        TextView textView2 = linearLayout2.findViewById(R.id.header_message);
        textView2.setText(inAppNotification.getMessage());
        textView2.setTextColor(Color.parseColor(inAppNotification.getMessageColor()));

        ArrayList<CTInAppNotificationButton> buttons = inAppNotification.getButtons();
        if (buttons != null && !buttons.isEmpty()) {
            for (int i = 0; i < buttons.size(); i++) {
                if (i >= 2) {
                    continue; // only show 2 buttons
                }
                CTInAppNotificationButton inAppNotificationButton = buttons.get(i);
                Button button = inAppButtons.get(i);
                setupInAppButton(button, inAppNotificationButton, i);
            }
        }

        if (inAppNotification.getButtonCount() == 1) {
            hideSecondaryButton(mainButton, secondaryButton);
        }

        inAppView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gd.onTouchEvent(event);
                return true;
            }
        });

        return inAppView;
    }
}
