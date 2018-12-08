package com.clevertap.android.sdk;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.exoplayer2.ui.PlayerView;

class CTSimpleMessageViewHolder extends RecyclerView.ViewHolder {

    TextView title;
    TextView message;
    TextView timestamp;
    ImageView readDot, mediaImage;
    Button cta1,cta2,cta3;
    RelativeLayout simpleMessageRelativeLayout;
    FrameLayout simpleMessageFrameLayout;
    PlayerView playerView;

    CTSimpleMessageViewHolder(@NonNull View itemView) {
        super(itemView);

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
        playerView = new PlayerView(simpleMessageFrameLayout.getContext());
        playerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT));
        simpleMessageFrameLayout.addView(playerView);
    }
}
