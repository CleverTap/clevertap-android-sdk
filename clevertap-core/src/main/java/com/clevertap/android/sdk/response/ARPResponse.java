package com.clevertap.android.sdk.response;

import android.content.Context;
import android.content.SharedPreferences;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.validation.Validator;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ARPResponse extends CleverTapResponseDecorator {

    private final CTProductConfigController ctProductConfigController;

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final Logger logger;

    private final NetworkManager networkManager;

    private final Validator validator;

    public ARPResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config,
            NetworkManager networkManager,
            Validator validator, ControllerManager controllerManager) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        ctProductConfigController = controllerManager.getCTProductConfigController();
        logger = this.config.getLogger();
        this.networkManager = networkManager;
        this.validator = validator;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        // Handle "arp" (additional request parameters)
        try {
            if (response.has("arp")) {
                final JSONObject arp = (JSONObject) response.get("arp");
                if (arp.length() > 0) {
                    if (ctProductConfigController != null) {
                        ctProductConfigController.setArpValue(arp);
                    }
                    //Handle Discarded events in ARP
                    try {
                        processDiscardedEventsList(arp);
                    } catch (Throwable t) {
                        logger
                                .verbose("Error handling discarded events response: " + t.getLocalizedMessage());
                    }
                    handleARPUpdate(context, arp);
                }
            }
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "Failed to process ARP", t);
        }

        // process Console response
        cleverTapResponse.processResponse(response, stringBody, context);
    }

    //Saves ARP directly to new namespace
    private void handleARPUpdate(final Context context, final JSONObject arp) {
        if (arp == null || arp.length() == 0) {
            return;
        }

        final String nameSpaceKey = networkManager.getNewNamespaceARPKey();
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
                        logger.verbose(config.getAccountId(),
                                "ARP update for key " + key + " rejected (string value too long)");
                    }
                } else if (o instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) o);
                } else {
                    logger
                            .verbose(config.getAccountId(),
                                    "ARP update for key " + key + " rejected (invalid data type)");
                }
            } catch (JSONException e) {
                // Ignore
            }
        }
        logger.verbose(config.getAccountId(),
                "Stored ARP for namespace key: " + nameSpaceKey + " values: " + arp.toString());
        StorageHelper.persist(editor);
    }

    /**
     * Dashboard has a feature where marketers can discard event. We get that list in the ARP response,
     * SDK then checks if the event is in the discarded list before sending it to LC
     *
     * @param response response from server
     */
    private void processDiscardedEventsList(JSONObject response) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            logger.verbose(config.getAccountId(), "ARP doesn't contain the Discarded Events key");
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
            if (validator != null) {
                validator.setDiscardedEvents(discardedEventsList);
            } else {
                logger.verbose(config.getAccountId(), "Validator object is NULL");
            }
        } catch (JSONException e) {
            logger
                    .verbose(config.getAccountId(), "Error parsing discarded events list" + e.getLocalizedMessage());
        }
    }
}
