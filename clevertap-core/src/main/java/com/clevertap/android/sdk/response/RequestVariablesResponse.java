package com.clevertap.android.sdk.response;

import android.content.Context;

import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.callbacks.VariableRequestHandledCallback;

import org.json.JSONObject;

public class RequestVariablesResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;


    private final ControllerManager controllerManager;
    private final BaseCallbackManager callbackMgr;

    public RequestVariablesResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config, ControllerManager controllerManager, BaseCallbackManager mgr) {
        this.cleverTapResponse = cleverTapResponse;
        this.config = config;
        this.controllerManager = controllerManager;
        this.callbackMgr = mgr;
    }

    private  void log(String s){
        Logger.v(config.getAccountId(),"ctv_VARIABLE_RESPONSE:"+s);
    }
    private  void log(String s,Throwable t){
        Logger.v(config.getAccountId(),"ctv_VARIABLE_RESPONSE:"+s,t);
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        log( "Processing Variable response...");
        log("processResponse() called with: response = [" + response + "], stringBody = [" + stringBody + "], context = [" + context + "]");

        if (config.isAnalyticsOnly()) {
            log("CleverTap instance is configured to analytics only, not processing Variable response");
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        if (response == null) {
            log("Can't parse Variable Response, JSON response object is null");
            return;
        }

        String varsKey = Constants.REQUEST_VARIABLES_JSON_RESPONSE_KEY;

        if (!response.has(varsKey)) {
            log( "JSON object doesn't contain the Request Variables key");
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        try {
            log( "Processing Request Variables response");

            JSONObject kvJson = response.getJSONObject(varsKey);

            if (controllerManager.getCtVariables() != null) {
                VariableRequestHandledCallback callback = callbackMgr.getVariableRequestHandledCallback();
                controllerManager.getCtVariables().handleVariableResponse(kvJson,callback);
                callbackMgr.setVariableRequestHandledCallback(null);
            }
            else {
                log("Can't parse Variable Response, CTVariables is null");
            }

        } catch (Throwable t) {
            log( "Failed to parse response", t);
        }
        finally {
            cleverTapResponse.processResponse(response, stringBody, context);
        }



    }


}
