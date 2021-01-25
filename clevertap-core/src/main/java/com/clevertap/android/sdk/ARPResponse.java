package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class ARPResponse extends CleverTapResponseDecorator {

    private final CTProductConfigController mCTProductConfigController;

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final Logger mLogger;

    private final NetworkManager mNetworkManager;

    private final Validator mValidator;

    ARPResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config, NetworkManager networkManager,
            Validator validator, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mCTProductConfigController = controllerManager.getCTProductConfigController();
        mLogger = mConfig.getLogger();
        mNetworkManager = networkManager;
        mValidator = validator;
    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        // Handle "arp" (additional request parameters)
        try {
            if (response.has("arp")) {
                final JSONObject arp = (JSONObject) response.get("arp");
                if (arp.length() > 0) {
                    if (mCTProductConfigController != null) {
                        mCTProductConfigController.setArpValue(arp);
                    }
                    //Handle Discarded events in ARP
                    try {
                        processDiscardedEventsList(arp);
                    } catch (Throwable t) {
                        mLogger
                                .verbose("Error handling discarded events response: " + t.getLocalizedMessage());
                    }
                    handleARPUpdate(context, arp);
                }
            }
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "Failed to process ARP", t);
        }

        // process Console response
        mCleverTapResponse.processResponse(response, stringBody, context);
    }

    //Saves ARP directly to new namespace
    private void handleARPUpdate(final Context context, final JSONObject arp) {
        if (arp == null || arp.length() == 0) {
            return;
        }

        final String nameSpaceKey = mNetworkManager.getNewNamespaceARPKey();
        if (nameSpaceKey == null) {
            return;
        }

        final SharedPreferences prefs = StorageHelper.getPreferences(context, nameSpaceKey);
        final SharedPreferences.Editor editor = prefs.edit();

        final Iterator<String> keys = arp.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            try {
                final Object o = arp.get(key);
                if (o instanceof Number) {
                    final int update = ((Number) o).intValue();
                    editor.putInt(key, update);
                } else if (o instanceof String) {
                    if (((String) o).length() < 100) {
                        editor.putString(key, (String) o);
                    } else {
                        mLogger.verbose(mConfig.getAccountId(),
                                "ARP update for key " + key + " rejected (string value too long)");
                    }
                } else if (o instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) o);
                } else {
                    mLogger
                            .verbose(mConfig.getAccountId(),
                                    "ARP update for key " + key + " rejected (invalid data type)");
                }
            } catch (JSONException e) {
                // Ignore
            }
        }
        mLogger.verbose(mConfig.getAccountId(),
                "Stored ARP for namespace key: " + nameSpaceKey + " values: " + arp.toString());
        StorageHelper.persist(editor);
    }

    private void processDiscardedEventsList(JSONObject response) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            mLogger.verbose(mConfig.getAccountId(), "ARP doesn't contain the Discarded Events key");
            return;
        }

        try {
            ArrayList<String> discardedEventsList = new ArrayList<>();
            JSONArray discardedEventsArray = response.getJSONArray(Constants.DISCARDED_EVENT_JSON_KEY);

            if (discardedEventsArray != null) {
                for (int i = 0; i < discardedEventsArray.length(); i++) {
                    discardedEventsList.add(discardedEventsArray.getString(i));
                }
            }
            if (mValidator != null) {
                mValidator.setDiscardedEvents(discardedEventsList);
            } else {
                mLogger.verbose(mConfig.getAccountId(), "Validator object is NULL");
            }
        } catch (JSONException e) {
            mLogger
                    .verbose(mConfig.getAccountId(), "Error parsing discarded events list" + e.getLocalizedMessage());
        }
    }
}
