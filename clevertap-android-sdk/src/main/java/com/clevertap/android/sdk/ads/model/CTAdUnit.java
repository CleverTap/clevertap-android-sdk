package com.clevertap.android.sdk.ads.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ads.AdConstants;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This model class holds the data of an individual ad Unit.
 */
public class CTAdUnit implements Parcelable {

    private String adID;//Ad unit identifier
    private AdConstants.CtAdType adType;//can be (banner/image/video/carousel etc.)

    // Custom Key Value pairs
    private HashMap<String, String> customExtras;

    private String error;

    private JSONObject jsonObject;

    //constructors
    private CTAdUnit(JSONObject jsonObject, String adID, AdConstants.CtAdType adType, JSONObject object, String error) {
        this.jsonObject = jsonObject;
        this.adID = adID;
        this.adType = adType;
        this.error = error;
        this.customExtras = getKeyValues(object);
    }

    public String getAdID() {
        return adID;
    }

    public String getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public HashMap<String, String> getCustomExtras() {
        return customExtras;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    /**
     * static API to convert json to AdUnit
     *
     * @param jsonObject - Ad Unit Item in Json form
     * @return - CTAdUnit
     */
    public static CTAdUnit toAdUnit(JSONObject jsonObject) {
        if (jsonObject != null) {
            //logic to convert jsonobj to item
            try {
                String adID = jsonObject.has("wzrk_id") ? jsonObject.getString("wzrk_id") : "";
                AdConstants.CtAdType adType = jsonObject.has(Constants.KEY_TYPE) ? AdConstants.CtAdType.type(jsonObject.getString(Constants.KEY_TYPE)) : null;
                JSONObject customKV = null;
                if (jsonObject.has("custom_kv")) {
                    customKV = jsonObject.getJSONObject("custom_kv");
                }
                return new CTAdUnit(jsonObject, adID, adType, customKV, null);
            } catch (Exception e) {
                return new CTAdUnit(null, null, null, null, "Error Creating AdUnit from JSON : " + e.getLocalizedMessage());

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
            //no op
        }
        return null;
    }

    /**
     * populates the custom key values pairs from json
     *
     * @param kvObj- Custom Key Values
     */
    private HashMap<String, String> getKeyValues(JSONObject kvObj) {
        try {
            if (kvObj != null) {
                Iterator<String> keys = kvObj.keys();
                if (keys != null) {
                    String key, value;
                    HashMap<String, String> hashMap = null;
                    while (keys.hasNext()) {
                        key = keys.next();
                        value = kvObj.getString(key);
                        if (!TextUtils.isEmpty(key)) {
                            if (hashMap == null)
                                hashMap = new HashMap<>();
                            hashMap.put(key, value);
                        }
                    }
                    return hashMap;
                }
            }
        } catch (Exception e) {
            //no op
        }
        return null;
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
    private CTAdUnit(Parcel in) {
        try {
            this.adID = in.readString();
            this.adType = (AdConstants.CtAdType) in.readValue(AdConstants.CtAdType.class.getClassLoader());
            this.customExtras = in.readHashMap(null);
            this.jsonObject = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            this.error = in.readString();
        } catch (Exception e) {
            error = "Error Creating AdUnit from parcel : " + e.getLocalizedMessage();
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(adID);
        parcel.writeValue(adType);
        parcel.writeMap(customExtras);
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
}