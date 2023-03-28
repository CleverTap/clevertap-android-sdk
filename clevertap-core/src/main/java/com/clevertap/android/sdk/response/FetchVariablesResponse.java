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
        try {
            //todo remove this code block as it replaces server response with fake respnse
            response.put(varsKey,getJson(callbackMgr.getFetchVariablesCallback()!=null));
        }
        catch (Throwable t){t.printStackTrace();}

        if (!response.has(varsKey)) {
            log( "JSON object doesn't contain the Request Variables key");
            cleverTapResponse.processResponse(response, stringBody, context);
            return;
        }

        try {
            log( "Processing Request Variables response");

            JSONObject kvJson = response.getJSONObject(varsKey);

            if (controllerManager.getCtVariables() != null) {
                FetchVariablesCallback callback = callbackMgr.getFetchVariablesCallback();
                controllerManager.getCtVariables().handleVariableResponse(kvJson,callback);
                callbackMgr.setFetchVariablesCallback(null);
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
            "{\"pDb1\":8.88,\"pStr1\":\"server1\",\"pBool1\":true,\"pIn1\":8,\"dDb1\":8.88,\"dIn1\":8,\"dBool1\":true,\"dStr1\":\"server1\",\"pDb1Inst\":8.88,\"pStr1Inst\":\"server1\",\"pBool1Inst\":true,\"pIn1Inst\":8,\"pDb2\":8.88,\"pStr2\":\"server1\",\"pBool2\":true,\"pIn2\":8,\"dDb2\":8.88,\"dIn2\":8,\"dBool2\":true,\"dStr2\":\"server1\",\"pDb2Inst\":8.88,\"pStr2Inst\":\"server1\",\"pBool2Inst\":true,\"pIn2Inst\":8}",
            "{\"pDb1\":7.77,\"pStr1\":\"server2\",\"pBool1\":false,\"pIn1\":7,\"dDb1\":7.77,\"dIn1\":7,\"dBool1\":false,\"dStr1\":\"server2\",\"pDb1Inst\":7.77,\"pStr1Inst\":\"server2\",\"pBool1Inst\":false,\"pIn1Inst\":7,\"pDb2\":7.77,\"pStr2\":\"server2\",\"pBool2\":false,\"pIn2\":7,\"dDb2\":7.77,\"dIn2\":7,\"dBool2\":false,\"dStr2\":\"server2\",\"pDb2Inst\":7.77,\"pStr2Inst\":\"server2\",\"pBool2Inst\":false,\"pIn2Inst\":7}",
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
