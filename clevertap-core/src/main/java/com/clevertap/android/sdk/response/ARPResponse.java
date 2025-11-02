package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.network.ArpRepo;

import org.json.JSONObject;

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
            final ArpRepo arpRepo
    ) {
        // Handle "arp" (additional request parameters)
        try {
            if (response.has("arp")) {
                final JSONObject arp = (JSONObject) response.get("arp");
                if (arp.length() > 0) {
                    arpRepo.handleARPUpdate(context, arp);
                }
            }
        } catch (Throwable t) {
            logger.verbose(accountId, "Failed to process ARP", t);
        }
    }
}
