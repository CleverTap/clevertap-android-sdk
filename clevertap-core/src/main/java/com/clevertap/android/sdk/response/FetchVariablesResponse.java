package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.CTVariables;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;

import org.json.JSONArray;
import org.json.JSONObject;

public class FetchVariablesResponse extends CleverTapResponseDecorator {

    private final CleverTapInstanceConfig config;
    private final ControllerManager controllerManager;
    private final BaseCallbackManager callbackMgr;

    public FetchVariablesResponse(
            CleverTapInstanceConfig config,
            ControllerManager controllerManager,
            BaseCallbackManager mgr
    ) {
        this.config = config;
        this.controllerManager = controllerManager;
        this.callbackMgr = mgr;
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

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        logI("Processing Variable response...");
        logD("processResponse() called with: response = [" + response + "], stringBody = [" + stringBody + "], context = [" + context + "]");

        if (config.isAnalyticsOnly()) {
            logI("CleverTap instance is configured to analytics only, not processing Variable response");
            return;
        }

        if (response == null) {
            logI("Can't parse Variable Response, JSON response object is null");
            return;
        }

        extractAbVariantsFromServer(response);
        extractVarsFromServer(response);
    }

    private void extractVarsFromServer(JSONObject response) {
        String varsKey = Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY;

        if (!response.has(varsKey)) {
            logI("JSON object doesn't contain the " + varsKey + " key");
            return;
        }

        try {
            logI("Processing Request Variables response");

            JSONObject kvJson = response.getJSONObject(varsKey);

            if (controllerManager.getCtVariables() != null) {
                FetchVariablesCallback callback = callbackMgr.getFetchVariablesCallback();
                controllerManager.getCtVariables().handleVariableResponse(kvJson,callback);
                callbackMgr.setFetchVariablesCallback(null);
            }
            else {
                logI("Can't parse Variable Response, CTVariables is null");
            }

        } catch (Throwable t) {
            logI("Failed to parse response", t);
        }
    }

    private void extractAbVariantsFromServer(JSONObject response) {
        String variantsKey = Constants.REQUEST_VARIANTS_JSON_RESPONSE_KEY;

        if (!response.has(variantsKey)) {
            logI("JSON object doesn't contain the " + variantsKey + " key");
            return;
        }

        try {
            logI("Processing Variants response");

            JSONArray abVariantsJson = response.optJSONArray(variantsKey);

            CTVariables ctVariables = controllerManager.getCtVariables();
            if (ctVariables != null) {
                ctVariables.handleAbVariantsResponse(abVariantsJson);
            } else {
                logI("Can't parse Variant Response, CTVariables is null");
            }

        } catch (Throwable t) {
            logI("Failed to parse variants response", t);
        }
    }

}
