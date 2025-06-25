package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.CTXtensions.applyInsetsWithMarginAdjustment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.R;
import com.clevertap.android.sdk.customviews.CloseImageView;

public class CTInAppNativeCoverImageFragment extends CTInAppBaseFullFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = inflater.inflate(R.layout.inapp_cover_image, container, false);
        applyInsetsWithMarginAdjustment(inAppView, (insets, mlp) -> {
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;
            mlp.topMargin = insets.top;
            mlp.bottomMargin = insets.bottom;
            return null;
        });
        FrameLayout fl = inAppView.findViewById(R.id.inapp_cover_image_frame_layout);
        fl.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));

        RelativeLayout relativeLayout = fl.findViewById(R.id.cover_image_relative_layout);
        ImageView imageView = relativeLayout.findViewById(R.id.cover_image);

        CTInAppNotificationMedia mediaForOrientation = inAppNotification.getInAppMediaForOrientation(currentOrientation);
        if (mediaForOrientation != null) {
            Bitmap bitmap = resourceProvider().cachedInAppImageV1(mediaForOrientation.getMediaUrl());
            String contentDescription = mediaForOrientation.getContentDescription();
            if (!TextUtils.isEmpty(contentDescription))
                imageView.setContentDescription(contentDescription);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
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
