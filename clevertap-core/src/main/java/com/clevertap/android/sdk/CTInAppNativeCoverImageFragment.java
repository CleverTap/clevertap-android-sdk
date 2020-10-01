package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;

public class CTInAppNativeCoverImageFragment extends CTInAppBaseFullFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = inflater.inflate(R.layout.inapp_cover_image, container, false);

        FrameLayout fl = inAppView.findViewById(R.id.inapp_cover_image_frame_layout);
        fl.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        RelativeLayout relativeLayout = fl.findViewById(R.id.cover_image_relative_layout);
        ImageView imageView = relativeLayout.findViewById(R.id.cover_image);

        if (inAppNotification.getInAppMediaForOrientation(currentOrientation) != null) {
            if (inAppNotification.getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation))
                    != null) {
                imageView.setImageBitmap(inAppNotification
                        .getImage(inAppNotification.getInAppMediaForOrientation(currentOrientation)));
                imageView.setTag(0);
                imageView.setOnClickListener(new CTInAppNativeButtonClickListener());
            }
        }

        @SuppressLint("ResourceType")
        CloseImageView closeImageView = fl.findViewById(199272);

        closeImageView.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View v) {
                didDismiss(null);
                getActivity().finish();
            }
        });

        if (!inAppNotification.isHideCloseButton()) {
            closeImageView.setVisibility(View.GONE);
        } else {
            closeImageView.setVisibility(View.VISIBLE);
        }

        return inAppView;
    }
}
