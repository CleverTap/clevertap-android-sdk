package com.clevertap.android.sdk.ads;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.clevertap.android.sdk.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This model class holds the data of an individual ad Unit.
 */
public class CTAdUnit implements Parcelable {

    private String adID;//Ad unit identifier
    private String adType;//can be (banner/image/video/carousel etc.)
    private ArrayList<CTAdUnitMediaItem> mediaList;

    // Custom Key Value pairs
    private HashMap<String, String> customExtras;

    private String error;

    private JSONObject jsonObject;

    //constructors
    public CTAdUnit() {

    }

    public static final Creator<CTAdUnit> CREATOR = new Creator<CTAdUnit>() {
        @Override
        public CTAdUnit createFromParcel(Parcel in) {
            return new CTAdUnit(in);
        }

        @Override
        public CTAdUnit[] newArray(int size) {
            return new CTAdUnit[size];
        }
    };

    @SuppressWarnings("unchecked")
    public CTAdUnit(Parcel in) {
        try {
            adID = in.readString();
            adType = in.readString();
            customExtras = in.readHashMap(null);
            mediaList = in.createTypedArrayList(CTAdUnitMediaItem.CREATOR);
            jsonObject = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            error = in.readString();
        } catch (Exception e) {
            error = "Error Creating AdUnit from parcel : " + e.getLocalizedMessage();
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(adID);
        parcel.writeString(adType);
        parcel.writeMap(customExtras);
        parcel.writeTypedList(mediaList);
        if (jsonObject == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeString(jsonObject.toString());
        }
        parcel.writeString(error);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    String getAdID() {
        return adID;
    }

    public String getError() {
        return error;
    }

    public HashMap<String, String> getCustomExtras() {
        return customExtras;
    }

    /**
     * static API to convert json to AdUnit
     *
     * @param object
     * @return
     */
    static CTAdUnit toAdUnit(JSONObject object) {
        if (object != null) {
            Iterator<?> keys = object.keys();
            //logic to convert jsonobj to item
            CTAdUnit item = new CTAdUnit();
            try {
                if (keys != null && keys.hasNext()) {
                    String key = (String) keys.next();
                    if (object.get(key) instanceof JSONObject) {
                        item.jsonObject = (JSONObject) object.get(key);
                        item.adID = item.jsonObject.has("wzrk_id") ? item.jsonObject.getString("wzrk_id") : "";
                        item.adType = item.jsonObject.has("ct_type") ? item.jsonObject.getString("ct_type") : "";

                        if (item.jsonObject.has("kv")) {
                            item.populateKeyValues(item.jsonObject.getJSONObject("kv"));
                        }
                    }
                }
            } catch (Exception e) {
                item.error = "Error Creating AdUnit from JSON : " + e.getLocalizedMessage();
            }

        }
        return null;
    }

    /**
     * get the wzrk fields obj to be passed in the data for recording event.
     */
    public JSONObject getWzrkFields() {
        try {
            if (jsonObject != null) {
                Iterator<String> iterator = jsonObject.keys();
                JSONObject wzrkFieldsObj = new JSONObject();
                while (iterator.hasNext()) {
                    String keyName = iterator.next();
                    if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                        wzrkFieldsObj.put(keyName, jsonObject.get(keyName));
                    }
                }
                return wzrkFieldsObj;
            }
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * populates the custom key values pairs from json
     *
     * @param kvObj
     * @throws JSONException
     */
    private void populateKeyValues(JSONObject kvObj) throws JSONException {
        if (kvObj != null) {
            Iterator<String> keys = kvObj.keys();
            if (keys != null) {
                String key, value;
                while (keys.hasNext()) {
                    key = keys.next();
                    value = kvObj.getString(key);
                    if (!TextUtils.isEmpty(key)) {
                        if (this.customExtras == null)
                            this.customExtras = new HashMap<>();
                        this.customExtras.put(key, value);
                    }
                }
            }
        }
    }

    /**
     * Media meta holder for Ad Unit
     */
    static class CTAdUnitMediaItem implements Parcelable {
        private String mediaUrl;
        private String contentType;
        private String cacheKey;
        private int orientation;
        private String error;

        CTAdUnitMediaItem(Parcel in) {
            try {
                mediaUrl = in.readString();
                contentType = in.readString();
                cacheKey = in.readString();
                orientation = in.readInt();
            } catch (Exception e) {
                error = "Error Creating AdUnit Media Item from Parcel : " + e.getLocalizedMessage();
            }
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mediaUrl);
            dest.writeString(contentType);
            dest.writeString(cacheKey);
            dest.writeInt(orientation);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<CTAdUnitMediaItem> CREATOR = new Creator<CTAdUnitMediaItem>() {
            @Override
            public CTAdUnitMediaItem createFromParcel(Parcel in) {
                return new CTAdUnitMediaItem(in);
            }

            @Override
            public CTAdUnitMediaItem[] newArray(int size) {
                return new CTAdUnitMediaItem[size];
            }
        };
    }
}