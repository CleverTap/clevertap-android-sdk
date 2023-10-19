package com.clevertap.android.sdk.response;

import static com.clevertap.android.sdk.StorageHelper.getPreferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.cryption.CryptHandler;
import com.clevertap.android.sdk.inapp.ImpressionStore;
import com.clevertap.android.sdk.inapp.InAppStore;
import com.clevertap.android.sdk.response.data.CtResponse;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;

import java.util.List;
import java.util.concurrent.Callable;
import org.json.JSONArray;
import org.json.JSONObject;

import kotlin.Pair;

public class InAppResponse extends CleverTapResponseDecorator {

    private CleverTapResponse cleverTapResponse;
    private final CleverTapInstanceConfig config;
    private final ControllerManager controllerManager;
    private final CryptHandler cryptHandler;
    private boolean isSendTest;
    private final Logger logger;
    private final InAppStore inAppStore;
    private final ImpressionStore impressionStore;
    private final DeviceInfo deviceInfo;

    public InAppResponse(
            CleverTapInstanceConfig config,
            ControllerManager controllerManager,
            CryptHandler cryptHandler,
            final boolean isSendTest,
            InAppStore inAppStore,
            ImpressionStore impressionStore,
            DeviceInfo deviceInfo
    ) {
        this.config = config;
        this.cryptHandler = cryptHandler;
        logger = this.config.getLogger();
        this.controllerManager = controllerManager;
        this.isSendTest = isSendTest;
        this.inAppStore = inAppStore;
        this.impressionStore = impressionStore;
        this.deviceInfo = deviceInfo;
    }

    public void setCleverTapResponse(CleverTapResponse cleverTapResponse) {
        this.cleverTapResponse = cleverTapResponse;
    }

    public void setTesting(Boolean isTest) {
        this.isSendTest = isTest;
    }

    @Override
    public void processResponse(
            final JSONObject response,
            final String stringBody,
            final Context context
    ) {
        try {

            CtResponse res = new CtResponse(response);

            if (config.isAnalyticsOnly()) {
                logger.verbose(config.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                cleverTapResponse.processResponse(response, stringBody, context); // todo this is not needed, no-op
                return;
            }

            logger.verbose(config.getAccountId(), "InApp: Processing response");

            int perSession = res.inAppsPerSession();
            int perDay = res.inAppsPerDay();

            if (!isSendTest && controllerManager.getInAppFCManager() != null) {
                Logger.v("Updating InAppFC Limits");
                controllerManager.getInAppFCManager().updateLimits(context, perDay, perSession);
                controllerManager.getInAppFCManager().processResponse(context, response);// Handle stale_inapp
            } else {
                logger.verbose(config.getAccountId(),
                        "controllerManager.getInAppFCManager() is NULL, not Updating InAppFC Limits");
            }

            // TODO get all types of inapps from response - ss, cs, applaunched - DONE
            // TODO store the inapps (get the old code and move it to some ***Store class) - DONE
            // TODO save the SS/CS mode from the json response - DONE
            //      add onChanged for this SS/CS mode to handle case when switching from SS/CS to CS/SS or
            //      from none to CS/SS to clear data. - DONE
            // TODO call EvaluationManager.evaluateOnAppLaunchedServerSide(appLaunchedNotifs) - DONE


            Pair<Boolean, JSONArray> legacyInApps = res.legacyInApps();
            if (legacyInApps.getFirst()) {
                displayInApp(legacyInApps.getSecond());
            }

            Pair<Boolean, JSONArray> appLaunchInApps = res.appLaunchInApps();
            if (appLaunchInApps.getFirst()) {
                handleAppLaunchServerSide(appLaunchInApps.getSecond());
            }

            Pair<Boolean, JSONArray> csInApps = res.clientSideInApps();
            if (csInApps.getFirst()) {
                inAppStore.storeClientSideInApps(csInApps.getSecond());
            }

            Pair<Boolean, JSONArray> ssInApps = res.serverSideInApps();
            if (ssInApps.getFirst()) {
                inAppStore.storeServerSideInApps(ssInApps.getSecond());
            }

            String inappDeliveryMode = response.optString("inapp_delivery_mode", "");
            if (!inappDeliveryMode.isEmpty()) {
                //TODO: Mode will be received with every request but do we need to persist it?
                inAppStore.setMode(inappDeliveryMode);
            }

            JSONArray inappStaleList = response.optJSONArray("inapp_stale");
            if (inappStaleList != null) {
                clearStaleInAppImpressions(inappStaleList, impressionStore);
            }
        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }

        // process metadata response
        cleverTapResponse.processResponse(response, stringBody, context);

    }

    /*public synchronized void updateLimits(final Context context, int perDay, int perSession) {
        StorageHelper.putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_MAX_PER_DAY, deviceId)),
                perDay);
        StorageHelper
                .putInt(context, storageKeyWithSuffix(getKeyWithDeviceId(Constants.INAPP_MAX_PER_SESSION, deviceId)),
                        perSession);
    }

    private String storageKeyWithSuffix(String key) {
        return key + ":" + config.getAccountId();
    }

    private String getKeyWithDeviceId(String key, String deviceId) {
        return key + ":" + deviceId;
    }

    public void processResponse(final Context context, final JSONObject response) {
        try {
            if (!response.has("inapp_stale")) {
                return;
            }

            final JSONArray arr = response.getJSONArray("inapp_stale");

            final SharedPreferences prefs = getPreferences(context,
                    storageKeyWithSuffix(getKeyWithDeviceId(Constants.KEY_COUNTS_PER_INAPP, deviceId)));
            final SharedPreferences.Editor editor = prefs.edit();

            for (int i = 0; i < arr.length(); i++) {
                final Object o = arr.get(i);
                if (o instanceof Integer) {
                    editor.remove("" + o);
                    Logger.d("Purged stale in-app - " + o);
                } else if (o instanceof String) {
                    editor.remove((String) o);
                    Logger.d("Purged stale in-app - " + o);
                }
            }

            StorageHelper.persist(editor);
        } catch (Throwable t) {
            Logger.v("Failed to purge out stale targets", t);
        }
    }*/

    private void clearStaleInAppImpressions(JSONArray inappStaleList, ImpressionStore impressionStore) {
        //Stale in-app ids used to remove in-app counts from impressionStore
        for (int i = 0; i < inappStaleList.length(); i++) {
            String inappStaleId = inappStaleList.optString(i);
            impressionStore.clear(inappStaleId);
        }
    }

    private void handleAppLaunchServerSide(JSONArray inappNotifsApplaunched) {
        try {
            List<JSONObject> inappNotifsApplaunchedList = Utils.toJSONObjectList(inappNotifsApplaunched);
            //TODO: inject EvaluationManager as a dependency?
//            new EvaluationManager(..)
//                    .evaluateOnAppLaunchedServerSide(inappNotifsApplaunchedList);
        } catch (Throwable e) {
            logger.verbose(config.getAccountId(), "InAppManager: Malformed AppLaunched ServerSide inApps");
            logger.verbose(config.getAccountId(), "InAppManager: Reason: " + e.getMessage(), e);
        }
    }

    private void displayInApp(JSONArray inappNotifsArray) {
        // Fire the first notification, if any
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
        task.execute("InAppResponse#processResponse", new Callable<Void>() {
            @Override
            public Void call() {
                //TODO: send inappNotifsArray for display
                controllerManager.getInAppController().addInAppNotificationsToQueue(inappNotifsArray);
                return null;
            }
        });
    }

    /*
    @Override
    public void processResponse(final JSONObject response, final String stringBody, final Context context) {
        try {

            if (config.isAnalyticsOnly()) {
                logger.verbose(config.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                cleverTapResponse.processResponse(response, stringBody, context);
                return;
            }

            logger.verbose(config.getAccountId(), "InApp: Processing response");

            if (!response.has("inapp_notifs")) {
                logger.verbose(config.getAccountId(),
                        "InApp: Response JSON object doesn't contain the inapp key, failing");
                // process metadata response
                cleverTapResponse.processResponse(response, stringBody, context);
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

            if (!isSendTest && controllerManager.getInAppFCManager() != null) {
                Logger.v("Updating InAppFC Limits");
                controllerManager.getInAppFCManager().updateLimits(context, perDay, perSession);
                controllerManager.getInAppFCManager().processResponse(context, response);// Handle stale_inapp
            } else {
                logger.verbose(config.getAccountId(),
                        "controllerManager.getInAppFCManager() is NULL, not Updating InAppFC Limits");

            }

            JSONArray inappNotifs;
            try {
                inappNotifs = response.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY);
            } catch (JSONException e) {
                logger.debug(config.getAccountId(), "InApp: In-app key didn't contain a valid JSON array");
                // process metadata response
                cleverTapResponse.processResponse(response, stringBody, context);
                return;
            }

            // Add all the new notifications to the queue
            SharedPreferences prefs = StorageHelper.getPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONArray inappsFromPrefs = new JSONArray(
                        StorageHelper.getStringFromPrefs(context, config, Constants.INAPP_KEY, "[]"));

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
                editor.putString(StorageHelper.storageKeyWithSuffix(config, Constants.INAPP_KEY),
                        inappsFromPrefs.toString());
                StorageHelper.persist(editor);
            } catch (Throwable e) {
                logger.verbose(config.getAccountId(), "InApp: Failed to parse the in-app notifications properly");
                logger.verbose(config.getAccountId(), "InAppManager: Reason: " + e.getMessage(), e);
            }
            // Fire the first notification, if any
            Task<Void> task = CTExecutorFactory.executors(config)
                    .postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InAppResponse#processResponse", new Callable<Void>() {
                @Override
                public Void call() {
                    controllerManager.getInAppController().showNotificationIfAvailable(context);
                    return null;
                }
            });
        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }

        // process metadata response
        cleverTapResponse.processResponse(response, stringBody, context);

    }


     */
}
