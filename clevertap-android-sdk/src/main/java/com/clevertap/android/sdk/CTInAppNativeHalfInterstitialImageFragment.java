package com.clevertap.android.sdk;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class CTInAppNativeHalfInterstitialImageFragment extends CTInAppBaseFullFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inAppView = inflater.inflate(R.layout.inapp_half_interstitial_image, container, false);

        FrameLayout fl  = inAppView.findViewById(R.id.inapp_half_interstitial_image_frame_layout);
        fl.setBackgroundDrawable(new ColorDrawable(0xBB000000));
        RelativeLayout relativeLayout = fl.findViewById(R.id.half_interstitial_image_relative_layout);
        relativeLayout.setBackgroundColor(Color.parseColor(inAppNotification.getBackgroundColor()));
        ImageView imageView = relativeLayout.findViewById(R.id.half_interstitial_image);
        imageView.setImageBitmap(inAppNotification.getImage());
        imageView.setTag(0);
        imageView.setOnClickListener(new CTInAppNativeButtonClickListener());

        CloseImageView closeImageView = fl.findViewById(199272);

        closeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                didDismiss(null);
                getActivity().finish();
            }
        });

        if(!inAppNotification.isHideCloseButton()) {
            closeImageView.setVisibility(View.GONE);
        }
        else {
            closeImageView.setVisibility(View.VISIBLE);
        }

        return inAppView;
    }
}
