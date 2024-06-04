package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider;
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy;
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategyExecutors;
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderExecutors;
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.inapp.store.preference.FileStore;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
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

    private final TriggerManager triggerManager;

    private final CoreMetaData coreMetaData;

    public InAppResponse(
            CleverTapInstanceConfig config,
            ControllerManager controllerManager,
            final boolean isSendTest,
            StoreRegistry storeRegistry,
            TriggerManager triggerManager,
            CoreMetaData coreMetaData
    ) {
        this.config = config;
        logger = this.config.getLogger();
        this.controllerManager = controllerManager;
        this.isSendTest = isSendTest;
        this.storeRegistry = storeRegistry;
        this.triggerManager = triggerManager;
        this.coreMetaData = coreMetaData;
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
            final InAppAssetsStore inAppAssetStore = storeRegistry.getInAppAssetsStore();
            final FileStore fileStore = storeRegistry.getFilesStore();
            final LegacyInAppStore legacyInAppStore = storeRegistry.getLegacyInAppStore();

            if (impressionStore == null || inAppStore == null || inAppAssetStore == null || legacyInAppStore == null) {
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

            InAppResourceProvider inAppResourceProvider = new InAppResourceProvider(context, logger);

            //InAppImagePreloaderStrategy preloadStrategy = new InAppImagePreloaderCoroutine(inAppResourceProvider, logger);
            //InAppCleanupStrategy cleanupStrategy = new InAppCleanupStrategyCoroutine(inAppResourceProvider);

            FilePreloaderStrategy preloadStrategy = new FilePreloaderExecutors(inAppResourceProvider, logger);
            FileCleanupStrategy cleanupStrategy = new FileCleanupStrategyExecutors(inAppResourceProvider);

            FileResourcesRepoImpl assetRepo = new FileResourcesRepoImpl(cleanupStrategy, preloadStrategy, inAppAssetStore, fileStore, legacyInAppStore);
            assetRepo.fetchAllImages(res.getPreloadImages());
            assetRepo.fetchAllGifs(res.getPreloadGifs());
            // TODO CustomTemplates download all file arguments before presenting replace image fetching will general file handling (including custom template files)

            if (isFullResponse) {
                logger.verbose(config.getAccountId(), "Handling cache eviction");
                assetRepo.cleanupStaleImages(res.getPreloadAssets());
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
