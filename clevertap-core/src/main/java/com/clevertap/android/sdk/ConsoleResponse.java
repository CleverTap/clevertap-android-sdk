package com.clevertap.android.sdk;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

class ConsoleResponse extends CleverTapResponse {

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;


    private final Logger mLogger;

    ConsoleResponse(CleverTapResponse cleverTapResponse) {
        mCleverTapResponse = cleverTapResponse;
        CoreState coreState = getCoreState();
        mConfig = coreState.getConfig();
        mLogger = mConfig.getLogger();
    }

    @Override
    void processResponse(final JSONObject response, final String stringBody, final Context context) {
        // Handle "console" - print them as info to the console
        try {
            if (response.has("console")) {
                final JSONArray console = (JSONArray) response.get("console");
                if (console.length() > 0) {
                    for (int i = 0; i < console.length(); i++) {
                        mLogger.debug(mConfig.getAccountId(), console.get(i).toString());
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
                    mLogger.verbose(mConfig.getAccountId(),
                            "Set debug level to " + debugLevel + " for this session (set by upstream)");
                }
            }
        } catch (Throwable t) {
            // Ignore
        }

        // process InBox Response
        mCleverTapResponse.processResponse(response, stringBody, context);
    }
}
