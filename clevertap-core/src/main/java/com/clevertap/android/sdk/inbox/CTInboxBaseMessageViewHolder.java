package com.clevertap.android.sdk.inbox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.clevertap.android.sdk.R;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;

@RestrictTo(Scope.LIBRARY)
public class CTInboxBaseMessageViewHolder extends RecyclerView.ViewHolder {

    Context context;

    LinearLayout ctaLinearLayout, bodyRelativeLayout;

    FrameLayout frameLayout;

    ImageView mediaImage, squareImage;

    RelativeLayout mediaLayout;

    FrameLayout progressBarFrameLayout;

    RelativeLayout relativeLayout, clickLayout;

    private CTInboxMessageContent firstContentItem;

    private CTInboxMessage message;

    private ImageView muteIcon;

    private WeakReference<CTInboxListViewFragment> parentWeakReference;

    private boolean requiresMediaPlayer;

    protected final ImageView readDot;

    CTInboxBaseMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        readDot = itemView.findViewById(R.id.read_circle);
    }

    public boolean addMediaPlayer(
            float currentVolume,
            Function0<Float> muteClick,
            Function3<String, Boolean, Boolean, Void> playMedia,
            View videoSurfaceView
    ) {
        if (!requiresMediaPlayer) {
            return false;
        }
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        if (frameLayout == null) {
            return false;
        }
        frameLayout.removeAllViews();
        frameLayout.setVisibility(View.GONE); // Gets set visible in playerReady

        final Resources resources = context.getResources();
        final DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        int width;
        int height;
        if (CTInboxActivity.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (message.getOrientation().equalsIgnoreCase("l")) {
                width = Math.round(this.mediaImage.getMeasuredHeight() * 1.76f);
                height = this.mediaImage.getMeasuredHeight();
            } else {
                height = this.squareImage.getMeasuredHeight();
                //noinspection all
                width = height;
            }
        } else {
            width = resources.getDisplayMetrics().widthPixels;
            height = message.getOrientation().equalsIgnoreCase("l") ? Math.round(width * 0.5625f) : width;
        }

        videoSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

        frameLayout.addView(videoSurfaceView);
        frameLayout.setBackgroundColor(Color.parseColor(message.getBgColor()));

        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.VISIBLE);
        }
        if (firstContentItem.mediaIsVideo()) {
            muteIcon = new ImageView(context);
            muteIcon.setVisibility(View.GONE);

            // Initial state setup
            setMuteIconState(muteIcon, context, currentVolume);

            int iconWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            int iconHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, displayMetrics);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(iconWidth, iconHeight);
            int iconTop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, displayMetrics);
            int iconRight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, displayMetrics);
            layoutParams.setMargins(0, iconTop, iconRight, 0);
            layoutParams.gravity = Gravity.END;
            muteIcon.setLayoutParams(layoutParams);

            View.OnClickListener updateMuteIcon = v -> {
                float volume = muteClick.invoke();
                setMuteIconState(muteIcon, context, volume);
            };
            muteIcon.setOnClickListener(updateMuteIcon);
            frameLayout.addView(muteIcon);
        }

        playMedia.invoke(firstContentItem.getMedia(), firstContentItem.mediaIsAudio(), firstContentItem.mediaIsVideo());
        return true;
    }

    private void setMuteIconState(ImageView icon, Context context, float volume) {
        boolean isMuted = volume <= 0;
        int drawableRes = isMuted ? R.drawable.ct_volume_off : R.drawable.ct_volume_on;
        int contentDescRes = isMuted ? R.string.ct_inbox_mute_button_content_description
                : R.string.ct_inbox_unmute_button_content_description;

        icon.setContentDescription(context.getString(contentDescRes));
        icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), drawableRes, null));
    }

    /**
     * Logic for timestamp
     *
     * @param time Epoch date of creation
     * @return String timestamp
     */
    String calculateDisplayTimestamp(long time) {
        long now = System.currentTimeMillis() / 1000;
        long diff = now - time;
        if (diff < 60) {
            return "Just Now";
        } else if (diff > 60 && diff < 59 * 60) {
            return (diff / (60)) + " mins ago";
        } else if (diff > 59 * 60 && diff < 23 * 59 * 60) {
            return diff / (60 * 60) > 1 ? diff / (60 * 60) + " hours ago" : diff / (60 * 60) + " hour ago";
        } else if (diff > 24 * 60 * 60 && diff < 48 * 60 * 60) {
            return "Yesterday";
        } else {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
            return sdf.format(new Date(time * 1000L));
        }
    }

    void configureWithMessage(
            final CTInboxMessage inboxMessage,
            final CTInboxListViewFragment parent,
            final int position
    ) {
        context = parent.getContext();
        parentWeakReference = new WeakReference<>(parent);
        message = inboxMessage;
        firstContentItem = message.getInboxMessageContents().get(0);
        requiresMediaPlayer = firstContentItem.mediaIsStreamable();
    }

    int getImageBackgroundColor() {
        return Color.TRANSPARENT;
    }

    CTInboxListViewFragment getParent() {
        return parentWeakReference.get();
    }

    void hideOneButton(Button mainButton, Button secondaryButton, Button tertiaryButton) {
        tertiaryButton.setVisibility(View.GONE);
        mainButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3));
        secondaryButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3));
        tertiaryButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
    }

    void hideTwoButtons(Button mainButton, Button secondaryButton, Button tertiaryButton) {
        secondaryButton.setVisibility(View.GONE);
        tertiaryButton.setVisibility(View.GONE);
        mainButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 6));
        secondaryButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
        tertiaryButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 0));
    }

    public boolean needsMediaPlayer() {
        return requiresMediaPlayer;
    }

    public void playerBuffering() {
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.VISIBLE);
        }
    }

    public void playerReady() {
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        frameLayout.setVisibility(View.VISIBLE);
        if (muteIcon != null) {
            muteIcon.setVisibility(View.VISIBLE);
        }
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.setVisibility(View.GONE);
        }
    }

    public void playerRemoved() {
        // .post here is imp
        if (progressBarFrameLayout != null) {
            progressBarFrameLayout.post(() -> progressBarFrameLayout.setVisibility(View.GONE));
        }
        if (muteIcon != null) {
            muteIcon.post(() -> muteIcon.setVisibility(View.GONE));
        }
        FrameLayout frameLayout = getLayoutForMediaPlayer();
        if (frameLayout != null) {
            frameLayout.removeAllViews();
        }
    }

    void setDots(ImageView[] dots, int dotsCount, Context appContext, LinearLayout sliderDots) {
        for (int k = 0; k < dotsCount; k++) {
            dots[k] = new ImageView(appContext);
            dots[k].setVisibility(View.VISIBLE);
            dots[k].setImageDrawable(
                    ResourcesCompat.getDrawable(appContext.getResources(), R.drawable.ct_unselected_dot, null));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 6, 4, 6);
            params.gravity = Gravity.CENTER;
            if (sliderDots.getChildCount() < dotsCount) {
                sliderDots.addView(dots[k], params);
            }
        }
    }

    public boolean shouldAutoPlay() {
        return firstContentItem.mediaIsVideo();
    }

    private FrameLayout getLayoutForMediaPlayer() {
        return frameLayout;
    }

    protected void markItemAsRead(final CTInboxMessage inboxMessage,
            final int position) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                final CTInboxListViewFragment parent = getParent();
                if (parent != null) {
                    Activity activity = parent.getActivity();
                    if (activity == null) {
                        return;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (readDot.getVisibility() == View.VISIBLE) {
                                parent.didShow(null, position);
                            }
                            readDot.setVisibility(View.GONE);
                            inboxMessage.setRead(true);
                        }
                    });
                }
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(runnable, 2000);
    }
}
