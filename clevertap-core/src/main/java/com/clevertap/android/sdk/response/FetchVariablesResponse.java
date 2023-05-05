package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;

import org.json.JSONObject;

public class FetchVariablesResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;


    private final ControllerManager controllerManager;
    private final BaseCallbackManager callbackMgr;

    public FetchVariablesResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config, ControllerManager controllerManager, BaseCallbackManager mgr) {
        this.cleverTapResponse = cleverTapResponse;
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
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            logI("Can't parse Variable Response, JSON response object is null");
            return;
        }

        String varsKey = Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY;

        if (!response.has(varsKey)) {
            logI("JSON object doesn't contain the " + varsKey + " key");
            cleverTapResponse.processResponse(response, stringBody, context);
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
        finally {
            cleverTapResponse.processResponse(response, stringBody, context);
        }
    }
}
