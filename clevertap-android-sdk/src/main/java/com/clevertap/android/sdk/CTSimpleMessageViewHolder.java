package com.clevertap.android.sdk;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * Custom ViewHolder for Simple message layout
 */
class CTSimpleMessageViewHolder extends RecyclerView.ViewHolder {

    TextView title;
    TextView message;
    TextView timestamp;
    ImageView readDot, mediaImage, squareImage;
    Button cta1,cta2,cta3;
    @SuppressWarnings({"unused", "WeakerAccess"})
    RelativeLayout simpleMessageRelativeLayout,clickLayout,bodyRelativeLayout;
    LinearLayout ctaLinearLayout;
    FrameLayout simpleMessageFrameLayout;

    CTSimpleMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setTag(this);
        title = itemView.findViewById(R.id.messageTitle);
        message = itemView.findViewById(R.id.messageText);
        timestamp = itemView.findViewById(R.id.timestamp);
        readDot = itemView.findViewById(R.id.read_circle);
        cta1 = itemView.findViewById(R.id.cta_button_1);
        cta2 = itemView.findViewById(R.id.cta_button_2);
        cta3 = itemView.findViewById(R.id.cta_button_3);
        mediaImage = itemView.findViewById(R.id.media_image);
        simpleMessageRelativeLayout = itemView.findViewById(R.id.simple_message_relative_layout);
        simpleMessageFrameLayout = itemView.findViewById(R.id.simple_message_frame_layout);
        squareImage = itemView.findViewById(R.id.square_media_image);
        clickLayout = itemView.findViewById(R.id.click_relative_layout);
        ctaLinearLayout = itemView.findViewById(R.id.cta_linear_layout);
        bodyRelativeLayout = itemView.findViewById(R.id.body_relative_layout);
    }
}
