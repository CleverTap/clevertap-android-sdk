package com.clevertap.android.sdk;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

class CTInAppNotification implements Parcelable {

    private String id;
    private String campaignId;
    private CTInAppType inAppType;
    private String html;
    private boolean excludeFromCaps;
    private boolean showClose;
    private boolean darkenScreen;
    private int maxPerSession;
    private int totalLifetimeCount;
    private int totalDailyCount;
    private char position;
    private int height;
    private int heightPercentage;
    private int width;
    private int widthPercentage;
    private JSONObject jsonDescription;
    private String error;
    private JSONObject customExtras;
    private JSONObject actionExtras;
    CTInAppNotificationListener listener;

    CTInAppNotification(){}

    interface CTInAppNotificationListener{
        void notificationReady(CTInAppNotification inAppNotification);
    }

    CTInAppNotification initWithJSON(JSONObject jsonObject){
        this.jsonDescription = jsonObject;
        Bundle b = getBundleFromJsonObject(jsonObject);
        if(!validateNotifBundle(b)){
            this.error = "Invalid JSON";
            return this;
        }
        try {

            this.id = jsonObject.has(Constants.INAPP_ID_IN_PAYLOAD) ? jsonObject.getString(Constants.INAPP_ID_IN_PAYLOAD) : "";
            this.campaignId = jsonObject.has(Constants.NOTIFICATION_ID_TAG) ? jsonObject.getString(Constants.NOTIFICATION_ID_TAG) : "";
            this.excludeFromCaps = jsonObject.has("efc") && jsonObject.getInt("efc") == 1;
            this.totalLifetimeCount = jsonObject.has("tlc") ? jsonObject.getInt("tlc") : -1;
            this.totalDailyCount = jsonObject.has("tdc") ? jsonObject.getInt("tdc") : -1;

            JSONObject data = jsonObject.has("d") ? jsonObject.getJSONObject("d") : null;
            if(data!=null){
                this.html = data.getString(Constants.INAPP_DATA_TAG);

                this.customExtras = data.has("kv") ? data.getJSONObject("kv") : null;
                if(this.customExtras==null)
                    this.customExtras = new JSONObject();

                JSONObject displayParams = jsonObject.getJSONObject(Constants.INAPP_WINDOW);
                if(displayParams!=null){
                    this.darkenScreen = displayParams.getBoolean(Constants.INAPP_NOTIF_DARKEN_SCREEN);
                    this.showClose = displayParams.getBoolean(Constants.INAPP_NOTIF_SHOW_CLOSE);
                    this.position = displayParams.getString(Constants.INAPP_POSITION).charAt(0);
                    this.width = displayParams.has(Constants.INAPP_X_DP) ? displayParams.getInt(Constants.INAPP_X_DP) : 0;
                    this.widthPercentage = displayParams.has(Constants.INAPP_X_PERCENT) ? displayParams.getInt(Constants.INAPP_X_PERCENT) : 0;
                    this.height = displayParams.has(Constants.INAPP_Y_DP) ? displayParams.getInt(Constants.INAPP_Y_DP) : 0;
                    this.heightPercentage = displayParams.has(Constants.INAPP_Y_PERCENT) ? displayParams.getInt(Constants.INAPP_Y_PERCENT) : 0;
                    this.maxPerSession = displayParams.has(Constants.INAPP_MAX_DISPLAY_COUNT) ? displayParams.getInt(Constants.INAPP_MAX_DISPLAY_COUNT) : -1;
                }

                if(this.html!=null){
                    if(this.position == 't' && this.widthPercentage == 100 && this.heightPercentage == 30)
                        this.inAppType = CTInAppType.CTInAppTypeHeaderHTML;
                    else if(this.position == 'b' && this.widthPercentage == 100 && this.heightPercentage == 30)
                        this.inAppType = CTInAppType.CTInAppTypeFooterHTML;
                    else if(this.position == 'c' && this.widthPercentage == 90 && this.heightPercentage == 85)
                        this.inAppType = CTInAppType.CTInAppTypeInterstitialHTML;
                    else if(this.position == 'c' && this.widthPercentage == 100 && this.heightPercentage == 100)
                        this.inAppType = CTInAppType.CTInAppTypeCoverHTML;
                    else if(this.position == 'c' && this.widthPercentage == 90 && this.heightPercentage == 50)
                        this.inAppType = CTInAppType.CTInAppTypeHalfInterstitialHTML;
                }
            }
        }catch (JSONException e){
            this.error = "Invalid JSON";
        }
        return this;

    }

    void prepareForDisplay(){
        //if InAppType is HTML
        listener.notificationReady(this);
    }

    private boolean validateNotifBundle(Bundle notif) {
        try {
            final Bundle w = notif.getBundle(Constants.INAPP_WINDOW);
            final Bundle d = notif.getBundle("d");
            if (w == null || d == null) return false;

            // Check that either xdp or xp is set
            if (!isKeyValid(w, Constants.INAPP_X_DP, Integer.class))
                if (!isKeyValid(w, Constants.INAPP_X_PERCENT, Integer.class))
                    return false;

            // Check that either ydp or yp is set
            if (!isKeyValid(w, Constants.INAPP_Y_DP, Integer.class))
                if (!isKeyValid(w, Constants.INAPP_Y_PERCENT, Integer.class))
                    return false;

            // Check that dk is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_DARKEN_SCREEN, Boolean.class)))
                return false;

            // Check that sc is set
            if (!(isKeyValid(w, Constants.INAPP_NOTIF_SHOW_CLOSE, Boolean.class)))
                return false;

            // Check that html is set
            if (!(isKeyValid(d, Constants.INAPP_DATA_TAG, String.class)))
                return false;

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
            } else
                return false;

            return true;
        } catch (Throwable t) {
            Logger.v("Failed to parse in-app notification!", t);
            return false;
        }
    }

    private boolean isKeyValid(Bundle b, String key, Class<?> type) {
        //noinspection ConstantConditions
        return b.containsKey(key) && b.get(key).getClass().equals(type);
    }

    private static Bundle getBundleFromJsonObject(JSONObject notif) {
        Bundle b = new Bundle();
        Iterator iterator = notif.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            try {
                Object value = notif.get(key);
                if (value instanceof String)
                    b.putString(key, (String) value);
                else if (value instanceof Character)
                    b.putChar(key, (Character) value);
                else if (value instanceof Integer)
                    b.putInt(key, (Integer) value);
                else if (value instanceof Float)
                    b.putFloat(key, (Float) value);
                else if (value instanceof Double)
                    b.putDouble(key, (Double) value);
                else if (value instanceof Long)
                    b.putLong(key, (Long) value);
                else if (value instanceof Boolean)
                    b.putBoolean(key, (Boolean) value);
                else if (value instanceof JSONObject)
                    b.putBundle(key, getBundleFromJsonObject((JSONObject) value));
            } catch (JSONException e) {
                Logger.v("Key had unknown object. Discarding");
            }
        }
        return b;
    }

    public String getId() {
        return id;
    }

    public String getCampaignId() {
        return campaignId;
    }

    public CTInAppType getInAppType() {
        return inAppType;
    }

    public String getHtml() {
        return html;
    }

    public boolean isExcludeFromCaps() {
        return excludeFromCaps;
    }

    public boolean isShowClose() {
        return showClose;
    }

    public boolean isDarkenScreen() {
        return darkenScreen;
    }

    public int getMaxPerSession() {
        return maxPerSession;
    }

    public int getTotalLifetimeCount() {
        return totalLifetimeCount;
    }

    public int getTotalDailyCount() {
        return totalDailyCount;
    }

    public char getPosition() {
        return position;
    }

    public int getHeight() {
        return height;
    }

    public int getHeightPercentage() {
        return heightPercentage;
    }

    public int getWidth() {
        return width;
    }

    public int getWidthPercentage() {
        return widthPercentage;
    }

    public JSONObject getJsonDescription() {
        return jsonDescription;
    }

    public String getError() {
        return error;
    }

    public JSONObject getCustomExtras() {
        return customExtras;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public JSONObject getActionExtras() {
        return actionExtras;
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
        }catch (JSONException e){
            // no-op
        }
    }

    @Override
    public int describeContents() {
        return 0;
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
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CTInAppNotification> CREATOR = new Parcelable.Creator<CTInAppNotification>() {
        @Override
        public CTInAppNotification createFromParcel(Parcel in) {
            return new CTInAppNotification(in);
        }

        @Override
        public CTInAppNotification[] newArray(int size) {
            return new CTInAppNotification[size];
        }
    };
}
