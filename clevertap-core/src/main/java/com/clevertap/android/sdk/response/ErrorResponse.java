package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Logger;
import org.json.JSONObject;

class ErrorResponse extends CleverTapResponseDecorator { // TODO Should we remove this implementation because error message is handled from the HttpURLConnection error stream?

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final Logger logger;

    public ErrorResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        logger = this.config.getLogger();
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        logger.verbose(config.getAccountId(), "Processing Variables error...");

        try {
            if (response != null && response.has("error")) {
                logger.info(config.getAccountId(), response.getString("error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Process further
        cleverTapResponse.processResponse(response, stringBody, context);
    }
}
