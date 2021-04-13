package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class ConsoleResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;


    private final Logger logger;

    public ConsoleResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        logger = this.config.getLogger();
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
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
                        logger.debug(config.getAccountId(), console.get(i).toString());
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
                    logger.verbose(config.getAccountId(),
                            "Set debug level to " + debugLevel + " for this session (set by upstream)");
                }
            }
        } catch (Throwable t) {
            // Ignore
        }

        // process InBox Response
        cleverTapResponse.processResponse(response, stringBody, context);
    }
}
