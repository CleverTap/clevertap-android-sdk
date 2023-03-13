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
                kvJson = getJson(callback!=null) ; //todo remove this line as it replaces server response with fake respnse
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

    private static final String[] jsons = new String[]{
            "{\"welcomeMsg\":\"Hey{mateeee}\",\"isOptedForOffers\":false,\"initialCoins\":\"100\",\"correctGuessPercentage\":\"80\",\"userConfigurableProps.numberOfGuesses\":5,\"userConfigurableProps.difficultyLevel\":3.3,\"userConfigurableProps.ai_Gender\":\"F\",\"userConfigurableProps.watchAddForAnotherGuess\":true,\"android.samsung.s22\":64999.99,\"android.samsung.s23\":\"Announced\",\"android.nokia.6a\":5400.50,\"android.nokia.12\":\"Announced\",\"apple.iphone15\":\"Announced\",\"javaIStr\":\"server1\",\"javaIBool\":true,\"javaIInt\":2,\"javaIDouble\":2.42,\"definedVar\":\"server1\"}",
            "{\"welcomeMsg\":\"Hey from server\",\"isOptedForOffers\":true,\"initialCoins\":\"80\",\"correctGuessPercentage\":\"90\",\"userConfigurableProps.difficultyLevel\":6.6,\"userConfigurableProps.ai_Gender\":\"X\",\"userConfigurableProps.numberOfGuesses\":25,\"userConfigurableProps.watchAddForAnotherGuess\":true,\"android.samsung.s22\":34999.99,\"android.samsung.s23\":\"Unlisted\",\"android.nokia.6a\":8000,\"android.nokia.12\":\"Unlisted\",\"apple.iphone15\":\"Unlisted\",\"javaIStr\":\"server2\",\"javaIBool\":false,\"javaIInt\":3,\"javaIDouble\":3.42,\"definedVar\":\"server2\"}"
    };

    private static int toggler = 0;

    private static JSONObject getJson(boolean useToggler) {
        JSONObject obj;
        try {
            if (!useToggler) obj =  new JSONObject(jsons[0]);
            else {
                toggler = toggler + 1;
                if (toggler % 2 == 0)  obj =  new JSONObject(jsons[0]);
                else  obj =  new JSONObject(jsons[1]);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            obj =  null;
        }

        Logger.v("CleverTap","ctv_VARIABLE_RESPONSE:getJson called with useToggler = "+useToggler+" and returned following json object:"+obj);

        return  obj;
    }


}
