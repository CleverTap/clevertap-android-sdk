package com.clevertap.android.sdk.ads;

import android.text.TextUtils;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ads.model.CTAdUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Controller class for caching & supplying the Ad Units to the client.
 */
public class AdUnitController {

    private final HashMap<String, CTAdUnit> items = new HashMap<>();

    /**
     * Replaces the old AdUnits with the new ones post transformation of Json objects to AdUnit objects
     *
     * @param messages
     * @return
     */
    public ArrayList<CTAdUnit> updateAdItems(JSONArray messages) {
        items.clear();

        if (messages != null && messages.length() > 0) {
            final ArrayList<CTAdUnit> list = new ArrayList<>();
            try {
                for (int i = 0; i < messages.length(); i++) {
                    //parse each ad Unit
                    CTAdUnit item = CTAdUnit.toAdUnit((JSONObject) messages.get(i));
                    if (TextUtils.isEmpty(item.getError())) {
                        items.put(item.getAdID(), item);
                        list.add(item);
                    } else {
                        Logger.v("Failed to convert JsonArray item at index:" + i + " to AdUnit");
                    }
                }
            } catch (Exception e) {
                Logger.v("Failed while parsing Ad Unit:" + e.getLocalizedMessage());
                return null;
            }

            return !list.isEmpty() ? list : null;
        }

        return null;
    }

    /**
     * API to get the AdUnit using the adID
     *
     * @param adID - Ad ID
     * @return CTAdUnit
     */
    public CTAdUnit getAdUnitForID(String adID) {
        if (!TextUtils.isEmpty(adID)) {
            return items.get(adID);
        } else {
            Logger.d("Can't return Ad Unit, id was null");
        }
        return null;
    }
}