package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.ILogger;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConsoleResponse {

    private final String accountId;

    private final ILogger logger;

    public ConsoleResponse(String accountId, ILogger logger) {
        this.accountId = accountId;
        this.logger = logger;
    }

    public void processResponse(final JSONObject response) {
        // Handle "console" - print them as info to the console
        try {
            /**
             * Console info is no longer used
             * But the feature was to enable logs from LCr
             */
            if (response.has("console")) {
                final JSONArray console = (JSONArray) response.get("console");
                if (console.length() > 0) {
                    for (int i = 0; i < console.length(); i++) {
                        logger.debug(accountId, console.get(i).toString());
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore
        }

        // Handle server set debug level
        try {
            if (response.has("dbg_lvl")) {
                final int debugLevel = response.getInt("dbg_lvl");
                if (debugLevel >= 0) {
                    CleverTapAPI.setDebugLevel(debugLevel);
                    logger.verbose(accountId, "Set debug level to " + debugLevel + " for this session (set by upstream)");
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
    }
}
