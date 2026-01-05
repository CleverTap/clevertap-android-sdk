package com.clevertap.android.sdk.response;

import android.content.Context;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.inapp.InAppController;
import com.clevertap.android.sdk.inapp.TriggerManager;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps;
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
        this.logger = this.config.getLogger();
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
        processResponse(response, stringBody, context, false);
    }

    public void processResponse(
            final JSONObject response,
            final String stringBody,
            final Context context,
            final boolean isUserSwitching
    ) {
        try {

            if (config.isAnalyticsOnly()) {
                logger.verbose(config.getAccountId(),
                        "CleverTap instance is configured to analytics only, not processing inapp messages");
                // process metadata response
                return;
            }

            if (response == null || response.length() == 0) {
                logger.verbose(
                        config.getAccountId(),
                        "There is no inapps data to handle"
                );
                return;
            }

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

            String mode = res.getInAppMode();
            if (!mode.isEmpty()) {
                inAppStore.setMode(mode);
            }

            if (isUserSwitching) {
                return;
            }

            // Legacy SS in-apps (inapp_notifs -> NORMAL in-app campaigns WITHOUT advance display rules)
            DurationPartitionedInApps.ImmediateAndDelayed partitionedLegacyInApps = res.getPartitionedLegacyInApps();
            if (partitionedLegacyInApps.hasImmediateInApps()) {
                displayInApp(partitionedLegacyInApps.getImmediateInApps());
            }
            if (partitionedLegacyInApps.hasDelayedInApps()) {
                scheduleDelayedLegacyInApps(partitionedLegacyInApps.getDelayedInApps());
            }

            // Legacy SS in-apps meta (inapp_notifs_meta -> IN-ACTION in-app campaigns WITHOUT advance display rules)
            DurationPartitionedInApps.InActionOnly partitionedLegacyMetaInApps = res.getPartitionedLegacyMetaInApps();
            if (partitionedLegacyMetaInApps.hasInActionInApps()) {
                // Schedule in-action timers
                controllerManager.getInAppController()
                        .scheduleInActionInApps(partitionedLegacyMetaInApps.getInActionInApps());
            }

            // App launch SS in-apps (inapp_notifs_applaunched -> NORMAL in-app campaigns WITH advance display rules on app launched event)
            DurationPartitionedInApps.ImmediateAndDelayed partitionedAppLaunchServerSideInApps = res.getPartitionedAppLaunchServerSideInApps();
            if (partitionedAppLaunchServerSideInApps.hasImmediateInApps()) {
                controllerManager.getInAppController().onAppLaunchServerSideInAppsResponse(
                        partitionedAppLaunchServerSideInApps.getImmediateInApps(),
                        coreMetaData.getLocationFromUser());
            }
            if (partitionedAppLaunchServerSideInApps.hasDelayedInApps()) {
                controllerManager.getInAppController().onAppLaunchServerSideDelayedInAppsResponse(
                        partitionedAppLaunchServerSideInApps.getDelayedInApps(),
                        coreMetaData.getLocationFromUser()
                );
            }

            // App launch SS in-apps meta (inapp_notifs_applaunched_meta -> IN-ACTION in-app campaigns WITH advance display rules on app launched event)
            DurationPartitionedInApps.InActionOnly partitionedAppLaunchServerSideMetaInApps = res.getPartitionedAppLaunchServerSideMetaInApps();
            if (partitionedAppLaunchServerSideMetaInApps.hasInActionInApps()) {
                // Schedule in-action from App Launch SS meta
                controllerManager.getInAppController()
                        .scheduleInActionInApps(partitionedAppLaunchServerSideMetaInApps.getInActionInApps());
            }

            // CS in-apps (inapp_notifs_cs)
            DurationPartitionedInApps.ImmediateAndDelayed partitionedClientSideInApps = res.getPartitionedClientSideInApps();
            if (partitionedClientSideInApps.hasImmediateInApps()) {
                inAppStore.storeClientSideInApps(partitionedClientSideInApps.getImmediateInApps());
            }
            if (partitionedClientSideInApps.hasDelayedInApps()) {
                inAppStore.storeClientSideDelayedInApps(partitionedClientSideInApps.getDelayedInApps());
            }

            // SS in-apps (inapp_notifs_ss -> IN-ACTION + NORMAL in-app campaigns WITH advance display rules )
            DurationPartitionedInApps.UnknownAndInAction partitionedServerSideInAppsMeta = res.getPartitionedServerSideInAppsMeta();
            // delayAfterTrigger only comes within inapp_notifs(Legacy SS, with in-app content)
            if (partitionedServerSideInAppsMeta.hasUnknownDurationInApps())
            {
                inAppStore.storeServerSideInAppsMetaData(partitionedServerSideInAppsMeta.getUnknownDurationInApps());
            }
            if (partitionedServerSideInAppsMeta.hasInActionInApps())
            {
                inAppStore.storeServerSideInActionMetaData(partitionedServerSideInAppsMeta.getInActionInApps());
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

    private void scheduleDelayedLegacyInApps(JSONArray delayedLegacyInApps) {
        InAppController inAppController = controllerManager.getInAppController();

        // Schedule using delay functionality
        inAppController.scheduleDelayedInAppsForAllModes(delayedLegacyInApps);

        logger.verbose(config.getAccountId(),
                "InApp: scheduling " + delayedLegacyInApps.length() +
                        " delayed in-apps. Active delays: " +
                        inAppController.getActiveDelayedInAppsCount());
    }

}
