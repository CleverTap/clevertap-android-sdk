package com.clevertap.android.sdk.ads.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ads.CTAdConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This model class holds the data of an individual ad Unit.
 */
public class CTAdUnit implements Parcelable {

    /**
     * Ad unit identifier
     */
    private String adID;

    /**
     * Could be (banner,carousel,custom key value etc.)
     */
    private CTAdConstants.CtAdType adType;

    /**
     * background Color
     */
    private String bgColor;

    /**
     * orientation
     */
    private String orientation;

    /**
     * List of Ad Content Items
     */
    private ArrayList<CTAdUnitContent> adContentItems;

    /**
     * Custom Key Value Pairs
     */
    private HashMap<String, String> customExtras;

    private JSONObject jsonObject;

    private String error;

    //constructors
    private CTAdUnit(JSONObject jsonObject, String adID, CTAdConstants.CtAdType adType,
                     String bgColor, String orientation, ArrayList<CTAdUnitContent> contentArray,
                     JSONObject kvObject, String error) {
        this.jsonObject = jsonObject;
        this.adID = adID;
        this.adType = adType;
        this.bgColor = bgColor;
        this.orientation = orientation;
        this.adContentItems = contentArray;
        this.customExtras = getKeyValues(kvObject);
        this.error = error;
    }

    /**
     * static API to convert json to AdUnit
     *
     * @param jsonObject - Ad Unit Item in Json form
     * @return - CTAdUnit
     */
    public static CTAdUnit toAdUnit(JSONObject jsonObject) {
        //logic to convert jsonobj to item
        try {
            String adID = jsonObject.has("wzrk_id") ? jsonObject.getString("wzrk_id") : "";
            CTAdConstants.CtAdType adType = jsonObject.has(Constants.KEY_TYPE) ? CTAdConstants.CtAdType.type(jsonObject.getString(Constants.KEY_TYPE)) : null;

            String bgColor = jsonObject.has(Constants.KEY_BG) ? jsonObject.getString(Constants.KEY_BG) : "";

            String orientation = jsonObject.has("orientation") ? jsonObject.getString("orientation") : "";

            JSONArray contentArray = jsonObject.has("content") ? jsonObject.getJSONArray("content") : null;
            ArrayList<CTAdUnitContent> contentArrayList = new ArrayList<>();
            if (contentArray != null) {
                for (int i = 0; i < contentArray.length(); i++) {
                    CTAdUnitContent adUnitContent = CTAdUnitContent.toContent(contentArray.getJSONObject(i));
                    if (TextUtils.isEmpty(adUnitContent.getError())) {
                        contentArrayList.add(adUnitContent);
                    }
                }
            }
            JSONObject customKV = null;
            //custom KV can be added to ad unit of any types, so don't
            if (jsonObject.has("custom_kv")) {
                customKV = jsonObject.getJSONObject("custom_kv");
            }
            return new CTAdUnit(jsonObject, adID, adType, bgColor, orientation, contentArrayList, customKV, null);
        } catch (Exception e) {
            return new CTAdUnit(null, "", null, null, null, null, null, "Error Creating AdUnit from JSON : " + e.getLocalizedMessage());
        }
    }

    /**
     * @return the AdId of the adUnit
     */
    public String getAdID() {
        return adID;
    }

    public String getError() {
        return error;
    }

    /**
     * @return the Key Value pair of the adUnit
     */
    @SuppressWarnings("unused")
    public HashMap<String, String> getCustomExtras() {
        return customExtras;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    /**
     * @return the background color of the adUnit
     */
    @SuppressWarnings("unused")
    public String getBgColor() {
        return bgColor;
    }

    /**
     * @return the orientation of the adUnit
     */
    public String getOrientation() {
        return orientation;
    }

    /**
     * @return the AdType of the AdUnit
     */
    @SuppressWarnings("unused")
    public CTAdConstants.CtAdType getAdType() {
        return adType;
    }

    /**
     * @return the list of Content Ad Items.
     */
    @SuppressWarnings("unused")
    public ArrayList<CTAdUnitContent> getAdContentItems() {
        return adContentItems;
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
            this.adType = (CTAdConstants.CtAdType) in.readValue(CTAdConstants.CtAdType.class.getClassLoader());
            this.bgColor = in.readString();
            this.orientation = in.readString();

            if (in.readByte() == 0x01) {
                adContentItems = new ArrayList<>();
                in.readList(adContentItems, CTAdUnitContent.class.getClassLoader());
            } else {
                adContentItems = null;
            }

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
        parcel.writeString(bgColor);
        parcel.writeString(orientation);

        if (adContentItems == null) {
            parcel.writeByte((byte) (0x00));
        } else {
            parcel.writeByte((byte) (0x01));
            parcel.writeList(adContentItems);
        }

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

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append("ADid- ").append(adID);
        stringBuilder.append("Type- " + adType != null ? adType.toString() : null);
        stringBuilder.append("bgColor- ").append(bgColor);
        stringBuilder.append("Orientation- ").append(orientation);
        if (adContentItems != null && adContentItems.isEmpty()) {
            for (int i = 0; i < adContentItems.size(); i++) {
                CTAdUnitContent item = adContentItems.get(i);
                if (item != null) {
                    stringBuilder.append("Content Item:" + i + " " + item.toString());
                    stringBuilder.append("\n");
                }
            }
        }
        if (customExtras != null) {
            stringBuilder.append("Custom KV:").append(customExtras);
        }
        stringBuilder.append("JSON -").append(jsonObject);
        stringBuilder.append("Error-").append(error);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}