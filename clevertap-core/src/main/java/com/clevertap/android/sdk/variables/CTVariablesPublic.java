package com.clevertap.android.sdk.variables;

import androidx.annotation.Discouraged;
import androidx.annotation.NonNull;

import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback;
/*
import com.clevertap.android.sdk.feat_variable.CTVariables;
import com.clevertap.android.sdk.feat_variable.Var;
import com.clevertap.android.sdk.feat_variable.callbacks.VariablesChangedCallback;
*/


public class CTVariablesPublic {

    /**
     * Check if your app is in development mode. <br>
     * the following function: {@link #pushVariablesToServer(Runnable)} will only work if the app is in development mode and user is set as test in CT Dashboard
     * @return boolean
     */
    public static boolean isInDevelopmentMode(){
        return CTVariables.isInDevelopmentMode();
    }

    /**
     *  This flag indicates whether or not the SDK is still in process of receiving a response
     *  from the server. <br>
     */
    public static Boolean isVariableResponseReceived(){
        return CTVariables.isVariableResponseReceived();
    }

    /**
     * get current value of a particular variable.
     */
    public static <T> Var<T> getVariable(String name) {
        return CTVariables.getVariable(name);
    }



    /**
     * This api can be used to request variable data from server at any time
     */
    public static void fetchVariables() {
        //todo make same as : CleverTapAPI cleverTapAPI;cleverTapAPI.featureFlag().fetchFeatureFlags();
        // todo test this stmt :setVariableResponseReceived(false) is called, whenever the request for new Variable data is again triggered, i.e in {@link CTVariablesPublic#fetchVariables()} )

        //CTVariables.setVariableResponseReceived(true); // this is ideally correct but need to test in a specific scenario as per TAM
        Logger.v("requesting data from server");
        FakeServer.simulateBERequest(jsonObject -> {
            CTVariables.handleVariableResponse(jsonObject);
            return kotlin.Unit.INSTANCE;
        });

    }


    /**
     * clear current variable data.can be used during profile switch
     */
    public static void clearUserContent() {
        CTVariables.clearUserContent();
    }


    /**
     * This api is used to Force push the variables defined in code to server. This api will only
     * work if App is in development mode and user is marked as test user on the CT Dashboard
     * @param onComplete : a runnable to perform something once the api request is complete
     */
    @Discouraged(message = "Be Very careful when calling this api. This should only be called in debug mode")
    public static void pushVariablesToServer(Runnable onComplete) {
        CTVariables.pushVariablesToServer(onComplete);
    }

    /**
     * Api call to add VariablesChangedHandler
     * @param handler VariablesChangedHandler
     */
    public static void addVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        CTVariables.addVariablesChangedHandler(handler);
    }

    /**
     * Api call to add OneTimeVariablesChangedHandler
     * @param handler VariablesChangedHandler
     */
    public static void addOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        CTVariables.addOneTimeVariablesChangedHandler(handler);
    }

    /**
     * Api call to remove VariablesChangedHandler
     * @param handler VariablesChangedHandler
     */
    public static void removeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
       CTVariables.removeVariablesChangedHandler(handler);
    }

    /**
     * Api call to remove OneTimeVariablesChangedHandler
     * @param handler VariablesChangedHandler
     */
    public static void removeOneTimeVariablesChangedHandler(@NonNull VariablesChangedCallback handler) {
        CTVariables.removeOneTimeVariablesChangedHandler(handler);
    }

    /**
     * Api call to remove all VariablesChangedHandlers
     */
    public static void removeAllVariablesChangedHandler() {
        CTVariables.removeAllVariablesChangedHandler();
    }

    /**
     * Api call to remove all OneTimeVariablesChangedHandlers
     */
    public static void removeAllOneTimeVariablesChangedHandler() {
        CTVariables.removeAllOneTimeVariablesChangedHandler();
    }
}
