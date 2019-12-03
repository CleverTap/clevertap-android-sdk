package com.clevertap.android.sdk.ads;

import android.text.TextUtils;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ads.model.CleverTapAdUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Controller class for caching & supplying the Ad Units to the client.
 */
public class AdUnitController {

    private final HashMap<String, CleverTapAdUnit> items = new HashMap<>();

    /**
     * Replaces the old AdUnits with the new ones post transformation of Json objects to AdUnit objects
     *
     * @param messages - json-array of ad items
     * @return ArrayList<CleverTapAdUnit>
     */
    public ArrayList<CleverTapAdUnit> updateAdItems(JSONArray messages) {

        //flush existing ad units before updating with the new ones.
        reset();

        if (messages != null && messages.length() > 0) {
            final ArrayList<CleverTapAdUnit> list = new ArrayList<>();
            try {
                for (int i = 0; i < messages.length(); i++) {
                    //parse each ad Unit
                    CleverTapAdUnit item = CleverTapAdUnit.toAdUnit((JSONObject) messages.get(i));
                    if (TextUtils.isEmpty(item.getError())) {
                        items.put(item.getAdID(), item);
                        list.add(item);
                    } else {
                        Logger.d(Constants.FEATURE_AD_UNIT, "Failed to convert JsonArray item at index:" + i + " to AdUnit");
                    }
                }
            } catch (Exception e) {
                Logger.d(Constants.FEATURE_AD_UNIT, "Failed while parsing Ad Unit:" + e.getLocalizedMessage());
                return null;
            }

            return !list.isEmpty() ? list : null;
        } else {
            Logger.d(Constants.FEATURE_AD_UNIT, "Null json array response can't parse Ad Units ");
            return null;
        }
    }

    /**
     * Getter for retrieving all the running adUnits in the cache.
     *
     * @return ArrayList<CleverTapAdUnit> - Could be null in case no adUnits are there in the cache
     */
    public ArrayList<CleverTapAdUnit> getAllAdUnits() {
        if (!items.isEmpty()) {
            return new ArrayList<>(items.values());
        } else {
            Logger.d(Constants.FEATURE_AD_UNIT, "Failed to return ad Units, nothing found in the cache");
            return null;
        }
    }

    /**
     * Getter to retrieve the AdUnit using the adID
     *
     * @param adID - AdID {@link CleverTapAdUnit#getAdID()}
     * @return CleverTapAdUnit - Could be null in case no adUnits with the ID is found
     */
    public CleverTapAdUnit getAdUnitForID(String adID) {
        if (!TextUtils.isEmpty(adID)) {
            return items.get(adID);
        } else {
            Logger.d(Constants.FEATURE_AD_UNIT, "Can't return Ad Unit, id was null");
            return null;
        }
    }

    /**
     * clears the existing AdUnits
     */
    public void reset() {
        items.clear();
        Logger.d(Constants.FEATURE_AD_UNIT, "Cleared Ad Cache");
    }
}