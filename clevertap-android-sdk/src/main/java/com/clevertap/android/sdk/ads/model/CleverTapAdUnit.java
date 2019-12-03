package com.clevertap.android.sdk.ads.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ads.CTAdConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This model class holds the data of an individual ad Unit.
 */
public class CleverTapAdUnit implements Parcelable {

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
     * List of Ad Content Items
     */
    private ArrayList<CleverTapAdUnitContent> adContentItems;

    /**
     * Custom Key Value Pairs
     */
    private HashMap<String, String> customExtras;

    private JSONObject jsonObject;

    private String error;

    //constructors
    private CleverTapAdUnit(JSONObject jsonObject, String adID, CTAdConstants.CtAdType adType,
                            String bgColor, ArrayList<CleverTapAdUnitContent> contentArray,
                            JSONObject kvObject, String error) {
        this.jsonObject = jsonObject;
        this.adID = adID;
        this.adType = adType;
        this.bgColor = bgColor;
        this.adContentItems = contentArray;
        this.customExtras = getKeyValues(kvObject);
        this.error = error;
    }

    /**
     * static API to convert json to AdUnit
     *
     * @param jsonObject - Ad Unit Item in Json form
     * @return - CleverTapAdUnit - always returns non-null instance
     */
    @NonNull
    public static CleverTapAdUnit toAdUnit(JSONObject jsonObject) {
        //logic to convert json obj to item
        try {
            String adID = jsonObject.has(Constants.NOTIFICATION_ID_TAG) ? jsonObject.getString(Constants.NOTIFICATION_ID_TAG) : Constants.TEST_IDENTIFIER;
            CTAdConstants.CtAdType adType = jsonObject.has(Constants.KEY_TYPE) ? CTAdConstants.CtAdType.type(jsonObject.getString(Constants.KEY_TYPE)) : null;

            String bgColor = jsonObject.has(Constants.KEY_BG) ? jsonObject.getString(Constants.KEY_BG) : "";

            JSONArray contentArray = jsonObject.has(Constants.KEY_CONTENT) ? jsonObject.getJSONArray(Constants.KEY_CONTENT) : null;
            ArrayList<CleverTapAdUnitContent> contentArrayList = new ArrayList<>();
            if (contentArray != null) {
                for (int i = 0; i < contentArray.length(); i++) {
                    CleverTapAdUnitContent adUnitContent = CleverTapAdUnitContent.toContent(contentArray.getJSONObject(i));
                    if (TextUtils.isEmpty(adUnitContent.getError())) {
                        contentArrayList.add(adUnitContent);
                    }
                }
            }
            JSONObject customKV = null;
            //custom KV can be added to ad unit of any types, no need to add type check here
            if (jsonObject.has(Constants.KEY_CUSTOM_KV)) {
                customKV = jsonObject.getJSONObject(Constants.KEY_CUSTOM_KV);
            }
            return new CleverTapAdUnit(jsonObject, adID, adType, bgColor, contentArrayList, customKV, null);
        } catch (Exception e) {
            Logger.d(Constants.FEATURE_AD_UNIT, "Unable to init CleverTapAdUnit with JSON - " + e.getLocalizedMessage());
            return new CleverTapAdUnit(null, "", null, null, null, null, "Error Creating AdUnit from JSON : " + e.getLocalizedMessage());
        }
    }

    /**
     * Getter for the AdId of the adUnit
     *
     * @return String
     */
    public String getAdID() {
        return adID;
    }

    public String getError() {
        return error;
    }

    /**
     * Getter for the Key Value pair of the adUnit
     *
     * @return HashMap<String, String>
     */
    @SuppressWarnings("unused")
    public HashMap<String, String> getCustomExtras() {
        return customExtras;
    }

    /**
     * Getter for the JsonObject corresponding to the CleverTapAdUnit object
     *
     * @return JSONObject
     */
    public JSONObject getJsonObject() {
        return jsonObject;
    }

    /**
     * Getter for the hex-value background color of the adUnit e.g. #000000
     *
     * @return String
     */
    @SuppressWarnings("unused")
    public String getBgColor() {
        return bgColor;
    }

    /**
     * Getter for the AdType of the AdUnit, Refer{@link CTAdConstants.CtAdType}
     *
     * @return CTAdConstants.CtAdType
     */
    @SuppressWarnings("unused")
    public CTAdConstants.CtAdType getAdType() {
        return adType;
    }

    /**
     * Getter for the list of Content Ad Items.
     *
     * @return ArrayList<CleverTapAdUnitContent>
     */
    @SuppressWarnings("unused")
    public ArrayList<CleverTapAdUnitContent> getAdContentItems() {
        return adContentItems;
    }

    /**
     * Getter for the WiZRK fields obj to be passed in the data for recording event.
     *
     * @return JSONObject
     */
    public JSONObject getWZRKFields() {
        try {
            if (jsonObject != null) {
                Iterator<String> iterator = jsonObject.keys();
                JSONObject object = new JSONObject();
                while (iterator.hasNext()) {
                    String keyName = iterator.next();
                    if (keyName.startsWith(Constants.WZRK_PREFIX)) {
                        object.put(keyName, jsonObject.get(keyName));
                    }
                }
                return object;
            }
        } catch (Exception e) {
            //no op
            Logger.d(Constants.FEATURE_AD_UNIT, "Error in getting WiZRK fields " + e.getLocalizedMessage());
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
            Logger.d(Constants.FEATURE_AD_UNIT, "Error in getting Key Value Pairs " + e.getLocalizedMessage());
        }
        return null;
    }

    public static final Creator<CleverTapAdUnit> CREATOR = new Creator<CleverTapAdUnit>() {
        @Override
        public CleverTapAdUnit createFromParcel(Parcel in) {
            return new CleverTapAdUnit(in);
        }

        @Override
        public CleverTapAdUnit[] newArray(int size) {
            return new CleverTapAdUnit[size];
        }
    };

    @SuppressWarnings("unchecked")
    private CleverTapAdUnit(Parcel in) {
        try {
            this.adID = in.readString();
            this.adType = (CTAdConstants.CtAdType) in.readValue(CTAdConstants.CtAdType.class.getClassLoader());
            this.bgColor = in.readString();

            if (in.readByte() == 0x01) {
                adContentItems = new ArrayList<>();
                in.readList(adContentItems, CleverTapAdUnitContent.class.getClassLoader());
            } else {
                adContentItems = null;
            }

            this.customExtras = in.readHashMap(null);
            this.jsonObject = in.readByte() == 0x00 ? null : new JSONObject(in.readString());
            this.error = in.readString();
        } catch (Exception e) {
            error = "Error Creating AdUnit from parcel : " + e.getLocalizedMessage();
            Logger.d(Constants.FEATURE_AD_UNIT, error);
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(adID);
        parcel.writeValue(adType);
        parcel.writeString(bgColor);

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
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(" ADid- ").append(adID);
            stringBuilder.append(", Type- ").append((adType != null ? adType.toString() : null));
            stringBuilder.append(", bgColor- ").append(bgColor);
            if (adContentItems != null && !adContentItems.isEmpty()) {
                for (int i = 0; i < adContentItems.size(); i++) {
                    CleverTapAdUnitContent item = adContentItems.get(i);
                    if (item != null) {
                        stringBuilder.append(", Content Item:").append(i).append(" ").append(item.toString());
                        stringBuilder.append("\n");
                    }
                }
            }
            if (customExtras != null) {
                stringBuilder.append(", Custom KV:").append(customExtras);
            }
            stringBuilder.append(", JSON -").append(jsonObject);
            stringBuilder.append(", Error-").append(error);
            stringBuilder.append(" ]");
            return stringBuilder.toString();
        } catch (Exception e) {
            Logger.d(Constants.FEATURE_AD_UNIT, "Exception in toString:" + e);
        }
        return super.toString();
    }
}