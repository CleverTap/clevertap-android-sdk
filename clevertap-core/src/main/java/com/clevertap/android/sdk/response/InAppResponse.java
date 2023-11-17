package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderCoroutine;
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider;
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.CTExecutors;
import com.clevertap.android.sdk.task.Task;
import java.util.concurrent.Callable;
import kotlin.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

public class InAppResponse extends CleverTapResponseDecorator {

    private CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    private final boolean isSendTest;

    private final Logger logger;

    private final StoreRegistry storeRegistry;

    public InAppResponse(
            CleverTapInstanceConfig config,
            ControllerManager controllerManager,
            final boolean isSendTest,
            StoreRegistry storeRegistry
    ) {
        this.config = config;
        logger = this.config.getLogger();
        this.controllerManager = controllerManager;
        this.isSendTest = isSendTest;
        this.storeRegistry = storeRegistry;
    }

    public void setCleverTapResponse(CleverTapResponse cleverTapResponse) {
        this.cleverTapResponse = cleverTapResponse;
    }

    @Override
    public void processResponse(
            final JSONObject response,
            final String stringBody,
            final Context context
    ) {
        try {

            InAppResponseAdapter res = new InAppResponseAdapter(response);
            final ImpressionStore impressionStore = storeRegistry.getImpressionStore();
            final InAppStore inAppStore = storeRegistry.getInAppStore();

            if (impressionStore == null || inAppStore == null) {
                return;
            }

            if (config.isAnalyticsOnly()) {
                logger.verbose(config.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                cleverTapResponse.processResponse(response, stringBody, context); // todo this is not needed, no-op
                return;
            }

            logger.verbose(config.getAccountId(), "InApp: Processing response");

            int perSession = res.getInAppsPerSession();
            int perDay = res.getInAppsPerDay();

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

            Pair<Boolean, JSONArray> inappStaleList = res.getStaleInApps();
            if (inappStaleList.getFirst()) {
                clearStaleInAppImpressions(inappStaleList.getSecond(), impressionStore);
            }

            Pair<Boolean, JSONArray> legacyInApps = res.getLegacyInApps();
            if (legacyInApps.getFirst()) {
                displayInApp(legacyInApps.getSecond());
            }

            Pair<Boolean, JSONArray> appLaunchInApps = res.getAppLaunchServerSideInApps();
            if (appLaunchInApps.getFirst()) {
                handleAppLaunchServerSide(appLaunchInApps.getSecond());
            }

            Pair<Boolean, JSONArray> csInApps = res.getClientSideInApps();
            if (csInApps.getFirst()) {
                inAppStore.storeClientSideInApps(csInApps.getSecond());
            }

            Pair<Boolean, JSONArray> ssInApps = res.getServerSideInApps();
            if (ssInApps.getFirst()) {
                inAppStore.storeServerSideInAppsMetaData(ssInApps.getSecond());
            }

            InAppResourceProvider inAppResourceProvider = new InAppResourceProvider(context, logger);

            CTExecutors executor = CTExecutorFactory.executorResourceDownloader();
            InAppImagePreloaderStrategy preloader = new InAppImagePreloaderCoroutine(inAppResourceProvider, logger);
            //InAppImagePreloaderStrategy preloader = new InAppImagePreloaderExecutors(executor, inAppResourceProvider, logger);

            preloader.preloadImages(res.getPreloadImage());

            String inappMode = res.getInAppMode();
            if (!inappMode.isEmpty()) {
                inAppStore.setMode(inappMode);
            }

        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }

        // process metadata response
        cleverTapResponse.processResponse(response, stringBody, context);

    }

    private void clearStaleInAppImpressions(JSONArray inappStaleList, ImpressionStore impressionStore) {
        //Stale in-app ids used to remove in-app counts from impressionStore
        for (int i = 0; i < inappStaleList.length(); i++) {
            String inappStaleId = inappStaleList.optString(i);
            impressionStore.clear(inappStaleId);
        }
    }

    private void handleAppLaunchServerSide(JSONArray inappNotifsApplaunched) {
        try {
            controllerManager.getInAppController().onAppLaunchServerSideInAppsResponse(inappNotifsApplaunched);
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
                controllerManager.getInAppController().addInAppNotificationsToQueue(inappNotifsArray);
                return null;
            }
        });
    }

}
