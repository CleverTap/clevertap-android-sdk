package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;

import org.json.JSONObject;

public class FetchVariablesResponse {

    private final CleverTapInstanceConfig config;
    private final CTVariables ctVariables;
    private FetchVariablesCallback fetchVariablesCallback;

    public FetchVariablesResponse(
            CleverTapInstanceConfig config,
            CTVariables ctVariables
    ) {
        this.config = config;
        this.ctVariables = ctVariables;
    }

    private  void logD(String m){
        Logger.d("variables", m);
    }
    private  void logI(String m){
        Logger.d("variables", m);
    }
    private  void logI(String m,Throwable t){
        Logger.i("variables", m, t);
    }

    public void processResponse(final JSONObject response) {
        logI("Processing Variable response...");
        logD("processResponse() called with: response = " + response);

        if (config.isAnalyticsOnly()) {
            logI("CleverTap instance is configured to analytics only, not processing Variable response");
            return;
        }

        if (response == null) {
            logI("Can't parse Variable Response, JSON response object is null");
            return;
        }

        String varsKey = Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY;

        if (!response.has(varsKey)) {
            logI("JSON object doesn't contain the " + varsKey + " key");
            return;
        }

        try {
            logI("Processing Request Variables response");

            JSONObject kvJson = response.getJSONObject(varsKey);
            ctVariables.handleVariableResponse(kvJson, fetchVariablesCallback);
            fetchVariablesCallback = null;

        } catch (Throwable t) {
            logI("Failed to parse response", t);
        }
    }
}
