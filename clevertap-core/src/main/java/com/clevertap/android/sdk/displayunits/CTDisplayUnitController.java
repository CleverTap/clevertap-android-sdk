package com.clevertap.android.sdk.displayunits;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Controller class for caching & supplying the Display Units to the client.
 */
public class CTDisplayUnitController {

    private final HashMap<String, CleverTapDisplayUnit> items = new HashMap<>();

    /**
     * Replaces the old Display Units with the new ones, post transformation of Json objects to Display Unit objects
     *
     * @param messages - json-array of Display Unit items
     * @return ArrayList<CleverTapDisplayUnit> - could be null in case of null/empty/invalid json array
     */
    @Nullable
    public ArrayList<CleverTapDisplayUnit> updateDisplayUnits(JSONArray messages) {

        //flush existing display units before updating with the new ones.
        reset();

        if (messages != null && messages.length() > 0) {
            final ArrayList<CleverTapDisplayUnit> list = new ArrayList<>();
            try {
                for (int i = 0; i < messages.length(); i++) {
                    //parse each display Unit
                    CleverTapDisplayUnit item = CleverTapDisplayUnit.toDisplayUnit((JSONObject) messages.get(i));
                    if (TextUtils.isEmpty(item.getError())) {
                        items.put(item.getUnitID(), item);
                        list.add(item);
                    } else {
                        Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Failed to convert JsonArray item at index:" + i + " to Display Unit");
                    }
                }
            } catch (Exception e) {
                Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Failed while parsing Display Unit:" + e.getLocalizedMessage());
                return null;
            }

            return !list.isEmpty() ? list : null;
        } else {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Null json array response can't parse Display Units ");
            return null;
        }
    }

    /**
     * Getter for retrieving all the running Display Units in the cache.
     *
     * @return ArrayList<CleverTapDisplayUnit> - Could be null in case no Display Units are there in the cache
     */
    @Nullable
    public ArrayList<CleverTapDisplayUnit> getAllDisplayUnits() {
        if (!items.isEmpty()) {
            return new ArrayList<>(items.values());
        } else {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Failed to return Display Units, nothing found in the cache");
            return null;
        }
    }

    /**
     * Getter to retrieve the Display Unit using the unitID
     *
     * @param unitId - unitID of the Display Unit {@link CleverTapDisplayUnit#getUnitID()}
     * @return CleverTapDisplayUnit - Could be null in case no Display Units with the ID is found
     */
    @Nullable
    public CleverTapDisplayUnit getDisplayUnitForID(String unitId) {
        if (!TextUtils.isEmpty(unitId)) {
            return items.get(unitId);
        } else {
            Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Can't return Display Unit, id was null");
            return null;
        }
    }

    /**
     * clears the existing Display Units
     */
    public void reset() {
        items.clear();
        Logger.d(Constants.FEATURE_DISPLAY_UNIT, "Cleared Display Units Cache");
    }
}