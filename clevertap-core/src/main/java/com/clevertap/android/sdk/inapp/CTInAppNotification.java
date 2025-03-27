package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.inapp.CTLocalInApp.FALLBACK_TO_NOTIFICATION_SETTINGS;
import static com.clevertap.android.sdk.inapp.CTLocalInApp.IS_LOCAL_INAPP;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

@RestrictTo(Scope.LIBRARY)
public class CTInAppNotification implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInAppNotification> CREATOR
            = new Parcelable.Creator<CTInAppNotification>() {
        @Override
        public CTInAppNotification createFromParcel(Parcel in) {
            return new CTInAppNotification(in);
        }

        @Override
        public CTInAppNotification[] newArray(int size) {
            return new CTInAppNotification[size];
        }
    };

    private JSONObject actionExtras;

    private String backgroundColor;

    private int buttonCount;

    private ArrayList<CTInAppNotificationButton> buttons = new ArrayList<>();

    private String campaignId;

    private JSONObject customExtras;

    private String customInAppUrl;

    private boolean darkenScreen;

    private String error;

    private boolean excludeFromCaps;

    private int height;

    private int heightPercentage;

    private double aspectRatio = -1;

    private boolean hideCloseButton;

    private String html;

    private String id;

    private CTInAppType inAppType;

    private boolean isLandscape;

    private boolean isPortrait;

    private boolean isTablet;

    private boolean jsEnabled;

    private JSONObject jsonDescription;

    private String landscapeImageUrl;

    private int maxPerSession;

    private ArrayList<CTInAppNotificationMedia> mediaList = new ArrayList<>();

    private String message;

    private String messageColor;

    private char position;

    private boolean showClose;

    private long timeToLive;

    private String title;

    private String titleColor;

    private int totalDailyCount;

    private int totalLifetimeCount;

    private String type;

    private boolean videoSupported;

    private int width;

    private int widthPercentage;

    private boolean isLocalInApp = false;

    private boolean fallBackToNotificationSettings = false;

    private CustomTemplateInAppData customTemplateData;

    CTInAppNotification() {
    }

    private CTInAppNotification(Parcel in) {
        try {
            id = in.readString();
            campaignId = in.readString();
            inAppType = (CTInAppType) in.readValue(CTInAppType.class.getClassLoader());
            html = in.readString();
            excludeFromCaps = in.readByte() != 0x00;
            showClose = in.readByte() != 0x00;
            darkenScreen = in.readByte() != 0x00;
            maxPerSession = in.readInt();
            totalLifetimeCount = in.readInt();
            totalDailyCount = in.readInt();
            position = (char) in.readValue(char.class.getClassLoader());
            height = in.readInt();
            heightPercentage = in.readInt();
            width = in.readInt();
            widthPercentage = in.readInt();
            jsonDescription = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            error = in.readString();
            customExtras = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            actionExtras = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            type = in.readString();
            title = in.readString();
            titleColor = in.readString();
            backgroundColor = in.readString();
            message = in.readString();
            messageColor = in.readString();
            try {
                buttons = in.createTypedArrayList(CTInAppNotificationButton.CREATOR);
            } catch (Throwable t) {
                // no-op
            }
            try {
                mediaList = in.createTypedArrayList(CTInAppNotificationMedia.CREATOR);
            } catch (Throwable t) {
                // no-op
            }
            hideCloseButton = in.readByte() != 0x00;
            buttonCount = in.readInt();
            isTablet = in.readByte() != 0x00;
            customInAppUrl = in.readString();
            jsEnabled = in.readByte() != 0x00;
            isPortrait = in.readByte() != 0x00;
            isLandscape = in.readByte() != 0x00;
            isLocalInApp = in.readByte() != 0x00;
            fallBackToNotificationSettings = in.readByte() != 0x00;
            landscapeImageUrl = in.readString();
            timeToLive = in.readLong();
            customTemplateData = in.readParcelable(CustomTemplateInAppData.class.getClassLoader());
            aspectRatio = in.readDouble();

        } catch (JSONException e) {
            // no-op
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings("unused")
    public JSONObject getActionExtras() {
        return actionExtras;
    }

    public String getId() {
        return id;
    }

    @SuppressWarnings({"WeakerAccess"})
    public CTInAppType getInAppType() {
        return inAppType;
    }

    public long getTimeToLive() {
        return timeToLive;
    }

    public boolean isExcludeFromCaps() {
        return excludeFromCaps;
    }

    public CustomTemplateInAppData getCustomTemplateData() {
        return customTemplateData;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(campaignId);
        dest.writeValue(inAppType);
        dest.writeString(html);
        dest.writeByte((byte) (excludeFromCaps ? 0x01 : 0x00));
        dest.writeByte((byte) (showClose ? 0x01 : 0x00));
        dest.writeByte((byte) (darkenScreen ? 0x01 : 0x00));
        dest.writeInt(maxPerSession);
        dest.writeInt(totalLifetimeCount);
        dest.writeInt(totalDailyCount);
        dest.writeValue(position);
        dest.writeInt(height);
        dest.writeInt(heightPercentage);
        dest.writeInt(width);
        dest.writeInt(widthPercentage);
        if (jsonDescription == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(jsonDescription.toString());
        }
        dest.writeString(error);
        if (customExtras == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(customExtras.toString());
        }
        if (actionExtras == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeString(actionExtras.toString());
        }
        dest.writeString(type);
        dest.writeString(title);
        dest.writeString(titleColor);
        dest.writeString(backgroundColor);
        dest.writeString(message);
        dest.writeString(messageColor);
        dest.writeTypedList(buttons);
        dest.writeTypedList(mediaList);
        dest.writeByte((byte) (hideCloseButton ? 0x01 : 0x00));
        dest.writeInt(buttonCount);
        dest.writeByte((byte) (isTablet ? 0x01 : 0x00));
        dest.writeString(customInAppUrl);
        dest.writeByte((byte) (jsEnabled ? 0x01 : 0x00));
        dest.writeByte((byte) (isPortrait ? 0x01 : 0x00));
        dest.writeByte((byte) (isLandscape ? 0x01 : 0x00));
        dest.writeByte((byte) (isLocalInApp ? 0x01 : 0x00));
        dest.writeByte((byte) (fallBackToNotificationSettings ? 0x01 : 0x00));
        dest.writeString(landscapeImageUrl);
        dest.writeLong(timeToLive);
        dest.writeParcelable(customTemplateData, flags);
        dest.writeDouble(aspectRatio);
    }

    String getBackgroundColor() {
        return backgroundColor;
    }

    int getButtonCount() {
        return buttonCount;
    }

    public ArrayList<CTInAppNotificationButton> getButtons() {
        return buttons;
    }

    public String getCampaignId() {
        return campaignId;
    }

    JSONObject getCustomExtras() {
        return customExtras;
    }

    String getCustomInAppUrl() {
        return customInAppUrl;
    }

    String getError() {
        return error;
    }

    void setError(String error) {
        this.error = error;
    }

    public boolean isLocalInApp() {
        return isLocalInApp;
    }

    public boolean fallBackToNotificationSettings() {
        return fallBackToNotificationSettings;
    }

    int getHeight() {
        return height;
    }

    int getHeightPercentage() {
        return heightPercentage;
    }

    double getAspectRatio() {
        return aspectRatio;
    }

    String getHtml() {
        return html;
    }

    CTInAppNotificationMedia getInAppMediaForOrientation(int orientation) {
        CTInAppNotificationMedia returningMedia = null;
        for (CTInAppNotificationMedia inAppNotificationMedia : this.mediaList) {
            if (orientation == inAppNotificationMedia.getOrientation()) {
                returningMedia = inAppNotificationMedia;
                break;
            }
        }
        return returningMedia;
    }

    public JSONObject getJsonDescription() {
        return jsonDescription;
    }

    public int getMaxPerSession() {
        return maxPerSession;
    }

    ArrayList<CTInAppNotificationMedia> getMediaList() {
        return mediaList;
    }

    public String getMessage() {
        return message;
    }

    String getMessageColor() {
        return messageColor;
    }

    char getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    String getTitleColor() {
        return titleColor;
    }

    public int getTotalDailyCount() {
        return totalDailyCount;
    }

    public int getTotalLifetimeCount() {
        return totalLifetimeCount;
    }

    String getType() {
        return type;
    }

    int getWidth() {
        return width;
    }

    int getWidthPercentage() {
        return widthPercentage;
    }

    CTInAppNotification initWithJSON(JSONObject jsonObject, boolean videoSupported) {
        this.videoSupported = videoSupported;
        this.jsonDescription = jsonObject;
        try {

            this.type = jsonObject.has(Constants.KEY_TYPE) ? jsonObject.getString(Constants.KEY_TYPE) : null;

            if (this.type == null || this.type.equals(Constants.KEY_CUSTOM_HTML)) {
                legacyConfigureWithJson(jsonObject);
            } else {
                configureWithJson(jsonObject);
            }

        } catch (JSONException e) {
            this.error = "Invalid JSON : " + e.getLocalizedMessage();
        }
        return this;

    }

    CTInAppNotification createNotificationForAction(CustomTemplateInAppData actionData) {
        try {
            JSONObject notificationJson = new JSONObject();
            notificationJson.put(Constants.INAPP_ID_IN_PAYLOAD, id);
            notificationJson.put(Constants.NOTIFICATION_ID_TAG, campaignId);
            notificationJson.put(Constants.KEY_TYPE, InAppActionType.CUSTOM_CODE.toString());
            notificationJson.put(Constants.KEY_EFC, 1);
            notificationJson.put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, 1);
            notificationJson.put(Constants.KEY_WZRK_TTL, timeToLive);
            if (jsonDescription.has(Constants.INAPP_WZRK_PIVOT)) {
                notificationJson.put(Constants.INAPP_WZRK_PIVOT, jsonDescription.optString(Constants.INAPP_WZRK_PIVOT));
            }
            if (jsonDescription.has(Constants.INAPP_WZRK_CGID)) {
                notificationJson.put(Constants.INAPP_WZRK_CGID, jsonDescription.optString(Constants.INAPP_WZRK_CGID));
            }
            CTInAppNotification notification = new CTInAppNotification().initWithJSON(notificationJson, videoSupported);
            notification.setCustomTemplateData(actionData);
            return notification;
        } catch (JSONException jsonException) {
            return null;
        }
    }

    void setCustomTemplateData(CustomTemplateInAppData inAppData) {
        customTemplateData = inAppData;
        customTemplateData.writeFieldsToJson(jsonDescription);
    }

    boolean isDarkenScreen() {
        return darkenScreen;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isHideCloseButton() {
        return hideCloseButton;
    }

    boolean isJsEnabled() {
        return jsEnabled;
    }

    public boolean isLandscape() {
        return isLandscape;
    }

    public boolean isPortrait() {
        return isPortrait;
    }

    boolean isShowClose() {
        return showClose;
    }

    boolean isTablet() {
        return isTablet;
    }

    boolean isVideoSupported() {
        return videoSupported;
    }

    public boolean hasStreamMedia() {
        return !getMediaList().isEmpty() && getMediaList().get(0).isMediaStreamable();
    }

    private void configureWithJson(JSONObject jsonObject) {
        try {
            this.id = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD, "");
            this.campaignId = jsonObject.optString(Constants.NOTIFICATION_ID_TAG, "");
            this.type = jsonObject.getString(Constants.KEY_TYPE);// won't be null based on initWithJSON()
            this.isLocalInApp = jsonObject.optBoolean(IS_LOCAL_INAPP, false);
            this.fallBackToNotificationSettings = jsonObject.optBoolean(FALLBACK_TO_NOTIFICATION_SETTINGS, false);
            this.excludeFromCaps = jsonObject.optInt(Constants.KEY_EFC, -1) == 1 || jsonObject.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS, -1) == 1;
            this.totalLifetimeCount = jsonObject.optInt(Constants.KEY_TLC, -1);
            this.totalDailyCount = jsonObject.optInt(Constants.KEY_TDC, -1);
            this.maxPerSession = jsonObject.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1);
            this.inAppType = CTInAppType.fromString(this.type);
            this.isTablet = jsonObject.optBoolean(Constants.KEY_IS_TABLET, false);
            this.backgroundColor = jsonObject.optString(Constants.KEY_BG, Constants.WHITE);
            this.isPortrait = !jsonObject.has(Constants.KEY_PORTRAIT) || jsonObject.getBoolean(Constants.KEY_PORTRAIT);
            this.isLandscape = jsonObject.optBoolean(Constants.KEY_LANDSCAPE, false);
            this.timeToLive = jsonObject.optLong(Constants.WZRK_TIME_TO_LIVE, defaultTtl());

            JSONObject titleObject = jsonObject.optJSONObject(Constants.KEY_TITLE);
            if (titleObject != null) {
                this.title = titleObject.optString(Constants.KEY_TEXT, "");
                this.titleColor = titleObject.optString(Constants.KEY_COLOR, Constants.BLACK);
            }

            JSONObject msgObject = jsonObject.optJSONObject(Constants.KEY_MESSAGE);
            if (msgObject != null) {
                this.message = msgObject.optString(Constants.KEY_TEXT, "");
                this.messageColor = msgObject.optString(Constants.KEY_COLOR, Constants.BLACK);
            }

            this.hideCloseButton = jsonObject.optBoolean(Constants.KEY_HIDE_CLOSE, false);

            JSONObject media = jsonObject.optJSONObject(Constants.KEY_MEDIA);
            if (media != null) {
                CTInAppNotificationMedia portraitMedia = new CTInAppNotificationMedia().initWithJSON(media, Configuration.ORIENTATION_PORTRAIT);
                if (portraitMedia != null) {
                    mediaList.add(portraitMedia);
                }
            }

            JSONObject mediaLandscape = jsonObject.optJSONObject(Constants.KEY_MEDIA_LANDSCAPE);
            if (mediaLandscape != null) {
                CTInAppNotificationMedia landscapeMedia = new CTInAppNotificationMedia().initWithJSON(mediaLandscape, Configuration.ORIENTATION_LANDSCAPE);
                if (landscapeMedia != null) {
                    mediaList.add(landscapeMedia);
                }
            }

            JSONArray buttonArray = jsonObject.optJSONArray(Constants.KEY_BUTTONS);
            if (buttonArray != null) {
                for (int i = 0; i < buttonArray.length(); i++) {
                    JSONObject buttonJson = buttonArray.getJSONObject(i);
                    CTInAppNotificationButton inAppNotificationButton = new CTInAppNotificationButton().initWithJSON(buttonJson);
                    if (inAppNotificationButton.getError() == null) {
                        this.buttons.add(inAppNotificationButton);
                        this.buttonCount++;
                    }
                }
            }
            customTemplateData = CustomTemplateInAppData.createFromJson(jsonObject);

            switch (this.inAppType) {
                case CTInAppTypeFooter:
                case CTInAppTypeHeader:
                case CTInAppTypeCover:
                case CTInAppTypeHalfInterstitial:
                    for (CTInAppNotificationMedia inAppMedia : this.mediaList) {
                        if (inAppMedia.isGIF() || inAppMedia.isAudio() || inAppMedia.isVideo()) {
                            inAppMedia.setMediaUrl(null);
                            Logger.d("Unable to download to media. Wrong media type for template");
                        }
                    }
                    break;
                case CTInAppTypeCoverImageOnly:
                case CTInAppTypeHalfInterstitialImageOnly:
                case CTInAppTypeInterstitialImageOnly:
                    if (!this.mediaList.isEmpty()) {
                        for (CTInAppNotificationMedia inAppMedia : this.mediaList) {
                            if (inAppMedia.isGIF() || inAppMedia.isAudio() || inAppMedia.isVideo() || !inAppMedia.isImage()) {
                                this.error = "Wrong media type for template";
                                break; // Exit the loop early if an error is found
                            }
                        }
                    } else {
                        this.error = "No media type for template";
                    }
                    break;
            }
        } catch (JSONException e) {
            this.error = "Invalid JSON" + e.getLocalizedMessage();
        }
    }

    private boolean isKeyValid(Bundle b, String key, Class<?> type) {
        //noinspection ConstantConditions
        return b.containsKey(key) && b.get(key).getClass().equals(type);
    }

    private void legacyConfigureWithJson(JSONObject jsonObject) {
        Bundle b = getBundleFromJsonObject(jsonObject);
        if (!validateNotifBundle(b)) {
            this.error = "Invalid JSON";
            return;
        }
        try {
            this.id = jsonObject.optString(Constants.INAPP_ID_IN_PAYLOAD, "");
            this.campaignId = jsonObject.optString(Constants.NOTIFICATION_ID_TAG, "");
            this.excludeFromCaps = jsonObject.optInt(Constants.KEY_EFC, -1) == 1 || jsonObject.optInt(Constants.KEY_EXCLUDE_GLOBAL_CAPS, -1) == 1;
            this.totalLifetimeCount = jsonObject.optInt(Constants.KEY_TLC, -1);
            this.totalDailyCount = jsonObject.optInt(Constants.KEY_TDC, -1);
            this.jsEnabled = jsonObject.optBoolean(Constants.INAPP_JS_ENABLED, false);
            this.timeToLive = jsonObject.optLong(Constants.WZRK_TIME_TO_LIVE, defaultTtl());

            JSONObject data = jsonObject.optJSONObject(Constants.INAPP_DATA_TAG);
            if (data != null) {
                this.html = data.getString(Constants.INAPP_HTML_TAG);
                this.customInAppUrl = data.optString(Constants.KEY_URL, "");
                this.customExtras = data.optJSONObject(Constants.KEY_KV) != null ? data.getJSONObject(Constants.KEY_KV) : new JSONObject();

                JSONObject displayParams = jsonObject.optJSONObject(Constants.INAPP_WINDOW);
                if (displayParams != null) {
                    this.darkenScreen = displayParams.getBoolean(Constants.INAPP_NOTIF_DARKEN_SCREEN);
                    this.showClose = displayParams.getBoolean(Constants.INAPP_NOTIF_SHOW_CLOSE);
                    this.position = displayParams.getString(Constants.INAPP_POSITION).charAt(0);
                    this.width = displayParams.optInt(Constants.INAPP_X_DP, 0);
                    this.widthPercentage = displayParams.optInt(Constants.INAPP_X_PERCENT, 0);
                    this.height = displayParams.optInt(Constants.INAPP_Y_DP, 0);
                    this.heightPercentage = displayParams.optInt(Constants.INAPP_Y_PERCENT, 0);
                    this.maxPerSession = displayParams.optInt(Constants.INAPP_MAX_DISPLAY_COUNT, -1);
                    this.aspectRatio = displayParams.optDouble(Constants.INAPP_ASPECT_RATIO, -1);
                }

                if (this.html != null) {
                    switch (this.position) {
                        case 't':
                            if (this.aspectRatio != -1 || (this.widthPercentage == 100 && this.heightPercentage <= 30)) {
                                this.inAppType = CTInAppType.CTInAppTypeHeaderHTML;
                            }
                            break;
                        case 'b':
                            if (this.aspectRatio != -1 || (this.widthPercentage == 100 && this.heightPercentage <= 30)) {
                                this.inAppType = CTInAppType.CTInAppTypeFooterHTML;
                            }
                            break;
                        case 'c':
                            if (this.widthPercentage == 90 && this.heightPercentage == 85) {
                                this.inAppType = CTInAppType.CTInAppTypeInterstitialHTML;
                            } else if (this.widthPercentage == 100 && this.heightPercentage == 100) {
                                this.inAppType = CTInAppType.CTInAppTypeCoverHTML;
                            } else if (this.widthPercentage == 90 && this.heightPercentage == 50) {
                                this.inAppType = CTInAppType.CTInAppTypeHalfInterstitialHTML;
                            }
                            break;
                    }
                }
            }
        } catch (JSONException e) {
            this.error = "Invalid JSON";
        }
    }

    static long defaultTtl() {
        return (System.currentTimeMillis() + 2 * Constants.ONE_DAY_IN_MILLIS) / 1000;
    }

    private boolean validateNotifBundle(Bundle notif) {
        try {
            final Bundle w = notif.getBundle(Constants.INAPP_WINDOW);
            final Bundle d = notif.getBundle("d");
            if (w == null || d == null) {
                return false;
            }

            // Check that either xdp or xp is set
            if (!isKeyValid(w, Constants.INAPP_X_DP, Integer.class)) {
                if (!isKeyValid(w, Constants.INAPP_X_PERCENT, Integer.class)) {
                    return false;
                }
            }

            // Check that either ydp or yp is set
            if (!isKeyValid(w, Constants.INAPP_Y_DP, Integer.class)) {
                if (!isKeyValid(w, Constants.INAPP_Y_PERCENT, Integer.class)) {
                    return false;
                }
            }

            // Check that dk is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_DARKEN_SCREEN, Boolean.class))) {
                return false;
            }

            // Check that sc is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_SHOW_CLOSE, Boolean.class))) {
                return false;
            }

            // Check that html is set
            if (!(isKeyValid(d, Constants.INAPP_HTML_TAG, String.class))) {
                return false;
            }

            // Check that pos contains the right value
            if ((isKeyValid(w, Constants.INAPP_POSITION, String.class))) {
                //noinspection ConstantConditions
                char pos = w.getString(Constants.INAPP_POSITION).charAt(0);
                switch (pos) {
                    case Constants.INAPP_POSITION_TOP:
                        break;
                    case Constants.INAPP_POSITION_RIGHT:
                        break;
                    case Constants.INAPP_POSITION_BOTTOM:
                        break;
                    case Constants.INAPP_POSITION_LEFT:
                        break;
                    case Constants.INAPP_POSITION_CENTER:
                        break;
                    default:
                        return false;
                }
            } else {
                return false;
            }

            return true;
        } catch (Throwable t) {
            Logger.v("Failed to parse in-app notification!", t);
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    private static Bundle getBundleFromJsonObject(JSONObject notif) {
        Bundle b = new Bundle();
        Iterator iterator = notif.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            try {
                Object value = notif.get(key);
                if (value instanceof String) {
                    b.putString(key, (String) value);
                } else if (value instanceof Character) {
                    b.putChar(key, (Character) value);
                } else if (value instanceof Integer) {
                    b.putInt(key, (Integer) value);
                } else if (value instanceof Float) {
                    b.putFloat(key, (Float) value);
                } else if (value instanceof Double) {
                    b.putDouble(key, (Double) value);
                } else if (value instanceof Long) {
                    b.putLong(key, (Long) value);
                } else if (value instanceof Boolean) {
                    b.putBoolean(key, (Boolean) value);
                } else if (value instanceof JSONObject) {
                    b.putBundle(key, getBundleFromJsonObject((JSONObject) value));
                }
            } catch (JSONException e) {
                Logger.v("Key had unknown object. Discarding");
            }
        }
        return b;
    }
}
