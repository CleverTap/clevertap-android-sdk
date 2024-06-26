package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.inapp.store.preference.FileStore;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.util.List;
import java.util.concurrent.Callable;
import kotlin.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

public class InAppResponse extends CleverTapResponseDecorator {

    private final CleverTapInstanceConfig config;

    private final ControllerManager controllerManager;

    private final boolean isSendTest;

    private final Logger logger;

    private final StoreRegistry storeRegistry;

    private final TemplatesManager templatesManager;

    private final TriggerManager triggerManager;

    private final CoreMetaData coreMetaData;

    public InAppResponse(
            CleverTapInstanceConfig config,
            ControllerManager controllerManager,
            final boolean isSendTest,
            StoreRegistry storeRegistry,
            TriggerManager triggerManager,
            final TemplatesManager templatesManager,
            CoreMetaData coreMetaData
    ) {
        this.config = config;
        logger = this.config.getLogger();
        this.controllerManager = controllerManager;
        this.isSendTest = isSendTest;
        this.storeRegistry = storeRegistry;
        this.triggerManager = triggerManager;
        this.coreMetaData = coreMetaData;
        this.templatesManager = templatesManager;
    }

    @Override
    public void processResponse(
            final JSONObject response,
            final String stringBody,
            final Context context
    ) {
        try {

            InAppResponseAdapter res = new InAppResponseAdapter(response, templatesManager);
            final ImpressionStore impressionStore = storeRegistry.getImpressionStore();
            final InAppStore inAppStore = storeRegistry.getInAppStore();
            final InAppAssetsStore inAppAssetStore = storeRegistry.getInAppAssetsStore();
            final FileStore fileStore = storeRegistry.getFilesStore();
            final LegacyInAppStore legacyInAppStore = storeRegistry.getLegacyInAppStore();

            if (impressionStore == null || inAppStore == null || inAppAssetStore == null || legacyInAppStore == null || fileStore == null) {
                logger.verbose(config.getAccountId(), "Stores are not initialised, ignoring inapps!!!!");
                return;
            }

            if (config.isAnalyticsOnly()) {
                logger.verbose(config.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                return;
            }

            logger.verbose(config.getAccountId(), "InApp: Processing response");

            int perSession = res.getInAppsPerSession();
            int perDay = res.getInAppsPerDay();

            if (!isSendTest && controllerManager.getInAppFCManager() != null) {
                Logger.v("Updating InAppFC Limits");
                controllerManager.getInAppFCManager().updateLimits(context, perDay, perSession);
                controllerManager.getInAppFCManager().processResponse(context, response);
            } else {
                logger.verbose(config.getAccountId(),
                        "controllerManager.getInAppFCManager() is NULL, not Updating InAppFC Limits");
            }

            Pair<Boolean, JSONArray> inappStaleList = res.getStaleInApps();
            if (inappStaleList.getFirst()) {
                clearStaleInAppCache(inappStaleList.getSecond(), impressionStore, triggerManager);
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

            List<Pair<String, CtCacheType>> preloadAssetsMeta = res.getPreloadAssetsMeta();

            FileResourcesRepoImpl assetRepo = FileResourcesRepoFactory
                    .createFileResourcesRepo(context, logger, storeRegistry);
            if (!preloadAssetsMeta.isEmpty()) {
                assetRepo.preloadFilesAndCache(preloadAssetsMeta);
            }

            if (isFullResponse) {
                logger.verbose(config.getAccountId(), "Handling cache eviction");
                assetRepo.cleanupStaleFiles(res.getPreloadAssets());
            } else {
                logger.verbose(config.getAccountId(), "Ignoring cache eviction");
            }

            String mode = res.getInAppMode();
            if (!mode.isEmpty()) {
                inAppStore.setMode(mode);
            }

        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }
    }

    private void clearStaleInAppCache(JSONArray inappStaleList, ImpressionStore impressionStore,
            final TriggerManager triggerManager) {
        //Stale in-app ids used to remove in-app counts and triggers
        for (int i = 0; i < inappStaleList.length(); i++) {
            String inappStaleId = inappStaleList.optString(i);
            impressionStore.clear(inappStaleId);
            triggerManager.removeTriggers(inappStaleId);
        }
    }

    private void handleAppLaunchServerSide(JSONArray inappNotifsApplaunched) {
        try {
            controllerManager.getInAppController().onAppLaunchServerSideInAppsResponse(inappNotifsApplaunched, coreMetaData.getLocationFromUser());
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
