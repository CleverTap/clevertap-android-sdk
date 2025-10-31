package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.network.ArpRepo;
import com.clevertap.android.sdk.validation.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ARPResponse {

    private final ILogger logger;

    private final String accountId;

    public ARPResponse(String accountId, ILogger logger) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(
            final JSONObject response,
            final Context context,
            final Validator validator,
            final ArpRepo arpRepo
    ) {
        // Handle "arp" (additional request parameters)
        try {
            if (response.has("arp")) {
                final JSONObject arp = (JSONObject) response.get("arp");
                if (arp.length() > 0) {
                    //Handle Discarded events in ARP
                    try {
                        processDiscardedEventsList(arp, validator);
                    } catch (Throwable t) {
                        logger.verbose("Error handling discarded events response: " + t.getLocalizedMessage());
                    }
                    arpRepo.handleARPUpdate(context, arp);
                }
            }
        } catch (Throwable t) {
            logger.verbose(accountId, "Failed to process ARP", t);
        }
    }

    /**
     * Dashboard has a feature where marketers can discard event. We get that list in the ARP response,
     * SDK then checks if the event is in the discarded list before sending it to LC
     */
    private void processDiscardedEventsList(JSONObject response, Validator validator) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            logger.verbose(accountId, "ARP doesn't contain the Discarded Events key");
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
                logger.verbose(accountId, "Validator object is NULL");
            }
        } catch (JSONException e) {
            logger.verbose(accountId, "Error parsing discarded events list" + e.getLocalizedMessage());
        }
    }
}
