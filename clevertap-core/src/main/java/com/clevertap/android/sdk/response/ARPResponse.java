package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.CoreContract;
import com.clevertap.android.sdk.network.ArpRepo;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.validation.Validator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ARPResponse extends CleverTapResponseDecorator {

    private final Validator validator;
    private final ArpRepo arpRepo;

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
    private CTProductConfigController ctProductConfigController;

    private CoreContract coreContract;

    public ARPResponse(
            Validator validator,
            ArpRepo arpRepo
    ) {
        this.validator = validator;
        this.arpRepo = arpRepo;
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
                        coreContract.logger().verbose("Error handling discarded events response: " + t.getLocalizedMessage());
                    }
                    arpRepo.handleARPUpdate(context, arp);
                }
            }
        } catch (Throwable t) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Failed to process ARP", t);
        }
    }

    /**
     * Dashboard has a feature where marketers can discard event. We get that list in the ARP response,
     * SDK then checks if the event is in the discarded list before sending it to LC
     *
     * @param response response from server
     */
    private void processDiscardedEventsList(JSONObject response) {
        if (!response.has(Constants.DISCARDED_EVENT_JSON_KEY)) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "ARP doesn't contain the Discarded Events key");
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
                coreContract.logger().verbose(coreContract.config().getAccountId(), "Validator object is NULL");
            }
        } catch (JSONException e) {
            coreContract.logger().verbose(coreContract.config().getAccountId(), "Error parsing discarded events list" + e.getLocalizedMessage());
        }
    }

    public void setCtProductConfigController(CTProductConfigController ctProductConfigController) {
        this.ctProductConfigController = ctProductConfigController;
    }

    public void setCoreContract(CoreContract coreContract) {
        this.coreContract = coreContract;
    }
}
