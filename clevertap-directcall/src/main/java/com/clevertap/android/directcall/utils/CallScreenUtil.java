package com.clevertap.android.directcall.utils;

import static com.clevertap.android.directcall.Constants.CALLING_LOG_TAG_SUFFIX;
import static com.clevertap.android.directcall.Constants.KEY_BG_COLOR;
import static com.clevertap.android.directcall.Constants.KEY_BRAND_LOGO;
import static com.clevertap.android.directcall.Constants.KEY_RINGTONE;
import static com.clevertap.android.directcall.Constants.KEY_TEXT_COLOR;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.clevertap.android.directcall.R;
import com.clevertap.android.directcall.StorageHelper;
import com.clevertap.android.directcall.init.DirectCallAPI;

import java.util.Map;
import java.util.Objects;

public class CallScreenUtil {
    private static MediaPlayer mediaPlayer;
    private static CallScreenUtil instance = null;

    public static CallScreenUtil getInstance() {
        if (instance == null) {
            instance = new CallScreenUtil();
        }
        return instance;
    }

    public enum DCCallScreenType {
        INCOMING, OUTGOING, ONGOING;
    }

    private CallScreenUtil() {
    }

    public void playOutgoingRingtone(Context context) {
        try {
            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
            String ringtone = sharedPref.getString(KEY_RINGTONE, null);
            if (ringtone == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.ct_outgoing_tone);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } else {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(ringtone);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while playing the outgoing ringtone: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception e) {
                DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while freeing up the mediaPlayer resources : " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    public void releaseMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while freeing up the mediaPlayer resources : " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public interface BrandingViews {
        String ivLogo = "ivLogo";
        String rootViewBackground = "rootViewBackground";
        String tvContext = "tvContext";
        String tvPoweredBy = "tvPoweredBy";
        String tvCallScreenLabel = "tvCallScreenLabel";

        interface OUTGOING {
            String tvCallStatus = "tvCallStatus";
        }

        interface ONGOING {
            String tvHoldState = "tvHoldState";
            String tvNetworkLatency = "tvNetworkLatency";
            String chTimer = "chTimer";
        }

        interface INCOMING {
            //no views
        }
    }

    public void setBranding(final Context context, final Map<String, View> brandingViewParams, DCCallScreenType callScreenType) {
        try {
            SharedPreferences sharedPref = StorageHelper.getPreferences(context);
            String color = sharedPref.getString(KEY_TEXT_COLOR, null);
            String backgroundColor = sharedPref.getString(KEY_BG_COLOR, null);
            String logoUrl = sharedPref.getString(KEY_BRAND_LOGO, null);

            if(backgroundColor == null || backgroundColor.isEmpty() || backgroundColor.length() < 3 ||
                    color == null || color.isEmpty() || color.length() < 3) {
                backgroundColor = context.getString(R.string.default_background_color);
                color = context.getString(R.string.default_font_color);
            }

            int bgColor = Color.parseColor(backgroundColor);
            if (logoUrl != null && !logoUrl.isEmpty() && validateBrandingParam(brandingViewParams, BrandingViews.ivLogo)) {
                RequestOptions requestOptions = RequestOptions
                        .diskCacheStrategyOf(DiskCacheStrategy.ALL);

                Glide.with(context)
                        .load(logoUrl)
                        .apply(requestOptions)
                        .into(((ImageView) Objects.requireNonNull(brandingViewParams.get(BrandingViews.ivLogo))));
            }

            if (validateBrandingParam(brandingViewParams, BrandingViews.rootViewBackground)) {
                (brandingViewParams.get(BrandingViews.rootViewBackground)).setBackgroundColor(bgColor);
            }

            int fontColor = Color.parseColor(color);
            ((TextView) brandingViewParams.get(BrandingViews.tvContext)).setTextColor(fontColor);
            ((TextView) brandingViewParams.get(BrandingViews.tvPoweredBy)).setTextColor(fontColor);
            ((TextView) brandingViewParams.get(BrandingViews.tvCallScreenLabel)).setTextColor(fontColor);
            switch (callScreenType) {
                case OUTGOING:
                    ((TextView) brandingViewParams.get(BrandingViews.OUTGOING.tvCallStatus)).setTextColor(fontColor);
                    break;
                case ONGOING:
                    ((TextView) brandingViewParams.get(BrandingViews.ONGOING.tvHoldState)).setTextColor(fontColor);
                    ((TextView) brandingViewParams.get(BrandingViews.ONGOING.tvNetworkLatency)).setTextColor(fontColor);
                    ((Chronometer) brandingViewParams.get(BrandingViews.ONGOING.chTimer)).setTextColor(fontColor);
                    break;
                case INCOMING:
                    break;
            }
        } catch (Exception e) {
            DirectCallAPI.getLogger().debug(CALLING_LOG_TAG_SUFFIX, "Exception while setting up the branding on call screens: " + e.getLocalizedMessage());
            e.printStackTrace();
        }

    }

    public boolean validateBrandingParam(Map<String, View> brandingViewParams, String id) {
        return brandingViewParams.containsKey(id) && brandingViewParams.get(id) != null;
    }
}
