package com.clevertap.android.sdk.response;

import android.content.Context;
import android.content.SharedPreferences;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.InAppFCManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InAppResponse extends CleverTapResponseDecorator {

    private final CleverTapResponse mCleverTapResponse;

    private final CleverTapInstanceConfig mConfig;

    private final ControllerManager mControllerManager;

    private final InAppFCManager mInAppFCManager;

    private final Logger mLogger;


    public InAppResponse(CleverTapResponse cleverTapResponse, CleverTapInstanceConfig config,
            InAppFCManager inAppFCManager, ControllerManager controllerManager) {
        mCleverTapResponse = cleverTapResponse;
        mConfig = config;
        mLogger = mConfig.getLogger();
        mInAppFCManager = inAppFCManager;
        mControllerManager = controllerManager;
    }

    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        try {

            if (mConfig.isAnalyticsOnly()) {
                mLogger.verbose(mConfig.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                mCleverTapResponse.processResponse(response, stringBody, context);
                return;
            }

            mLogger.verbose(mConfig.getAccountId(), "InApp: Processing response");

            if (!response.has("inapp_notifs")) {
                mLogger.verbose(mConfig.getAccountId(),
                        "InApp: Response JSON object doesn't contain the inapp key, failing");
                // process metadata response
                mCleverTapResponse.processResponse(response, stringBody, context);
                return;
            }

            int perSession = 10;
            int perDay = 10;
            if (response.has(Constants.INAPP_MAX_PER_SESSION) && response
                    .get(Constants.INAPP_MAX_PER_SESSION) instanceof Integer) {
                perSession = response.getInt(Constants.INAPP_MAX_PER_SESSION);
            }

            if (response.has("imp") && response.get("imp") instanceof Integer) {
                perDay = response.getInt("imp");
            }

            if (mInAppFCManager != null) {
                Logger.v("Updating InAppFC Limits");
                mInAppFCManager.updateLimits(context, perDay, perSession);
                mInAppFCManager.processResponse(context, response);// Handle stale_inapp
            }

            JSONArray inappNotifs;
            try {
                inappNotifs = response.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY);
            } catch (JSONException e) {
                mLogger.debug(mConfig.getAccountId(), "InApp: In-app key didn't contain a valid JSON array");
                // process metadata response
                mCleverTapResponse.processResponse(response, stringBody, context);
                return;
            }

            // Add all the new notifications to the queue
            SharedPreferences prefs = StorageHelper.getPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONArray inappsFromPrefs = new JSONArray(
                        StorageHelper.getStringFromPrefs(context, mConfig, Constants.INAPP_KEY, "[]"));

                // Now add the rest of them :)
                if (inappNotifs != null && inappNotifs.length() > 0) {
                    for (int i = 0; i < inappNotifs.length(); i++) {
                        try {
                            JSONObject inappNotif = inappNotifs.getJSONObject(i);
                            inappsFromPrefs.put(inappNotif);
                        } catch (JSONException e) {
                            Logger.v("InAppManager: Malformed inapp notification");
                        }
                    }
                }

                // Commit all the changes
                editor.putString(StorageHelper.storageKeyWithSuffix(mConfig, Constants.INAPP_KEY),
                        inappsFromPrefs.toString());
                StorageHelper.persist(editor);
            } catch (Throwable e) {
                mLogger.verbose(mConfig.getAccountId(), "InApp: Failed to parse the in-app notifications properly");
                mLogger.verbose(mConfig.getAccountId(), "InAppManager: Reason: " + e.getMessage(), e);
            }
            // Fire the first notification, if any
            Task<Void> task = CTExecutorFactory.executors(mConfig)
                    .postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InAppResponse#processResponse", new Callable<Void>() {
                @Override
                public Void call() {
                    mControllerManager.getInAppController().showNotificationIfAvailable(context);
                    return null;
                }
            });
        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }

        // process metadata response
        mCleverTapResponse.processResponse(response, stringBody, context);

    }
}
