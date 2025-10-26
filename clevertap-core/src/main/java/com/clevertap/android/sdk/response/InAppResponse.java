package com.clevertap.android.sdk.response;

import com.clevertap.android.sdk.ILogger;
import com.clevertap.android.sdk.InAppFCManager;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.features.InAppFeatureMethods;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import java.util.List;
import kotlin.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

public class InAppResponse {

    private final String accountId;
    private final boolean isSendTest;

    private final ILogger logger;

    private final TemplatesManager templatesManager;

    public InAppResponse(
            String accountId,
            ILogger logger,
            final boolean isSendTest,
            final TemplatesManager templatesManager
    ) {
        this.accountId = accountId;
        this.logger = logger;
        this.isSendTest = isSendTest;
        this.templatesManager = templatesManager;
    }

    public void processResponse(
            final JSONObject response,
            final boolean isFullResponse,
            final boolean isUserSwitching,
            InAppFCManager inAppFCManager,
            InAppFeatureMethods inAppFeatureMethods
    ) {
        try {

            logger.verbose(accountId, "InApp: Processing response");
            InAppResponseAdapter res = new InAppResponseAdapter(response, templatesManager);
            if (!isSendTest &&  inAppFCManager != null) {
                Logger.v("Updating InAppFC Limits");
                int perSession = res.getInAppsPerSession();
                int perDay = res.getInAppsPerDay();
                inAppFeatureMethods.updateInAppFcManager(perDay, perSession, response);
            } else {
                logger.verbose(accountId, "inAppFCManager is NULL, not Updating InAppFC Limits");
            }

            Pair<Boolean, JSONArray> inappStaleList = res.getStaleInApps();
            if (inappStaleList.getFirst()) {
                inAppFeatureMethods.clearStaleInAppCache(inappStaleList.getSecond());
            }

            String mode = res.getInAppMode();
            if (!mode.isEmpty()) {
                inAppFeatureMethods.setMode(mode);
            }

            if (isUserSwitching) {
                return;
            }

            Pair<Boolean, JSONArray> legacyInApps = res.getLegacyInApps();
            if (legacyInApps.getFirst()) {
                inAppFeatureMethods.displayInApp(legacyInApps.getSecond());
            }

            Pair<Boolean, JSONArray> appLaunchInApps = res.getAppLaunchServerSideInApps();
            if (appLaunchInApps.getFirst()) {
                inAppFeatureMethods.handleAppLaunchServerSide(appLaunchInApps.getSecond());
            }

            Pair<Boolean, JSONArray> csInApps = res.getClientSideInApps();
            if (csInApps.getFirst()) {
                inAppFeatureMethods.storeClientSideInApps(csInApps.getSecond());
            }

            Pair<Boolean, JSONArray> ssInApps = res.getServerSideInApps();
            if (ssInApps.getFirst()) {
                inAppFeatureMethods.storeServerSideInApps(ssInApps.getSecond());
            }

            List<Pair<String, CtCacheType>> preloadAssetsMeta = res.getPreloadAssetsMeta();

            if (!preloadAssetsMeta.isEmpty()) {
                inAppFeatureMethods.preloadFilesAndCache(preloadAssetsMeta);
            }

            if (isFullResponse) {
                logger.verbose(accountId, "Handling cache eviction");
                List<String> preloadAssets = res.getPreloadAssets();
                inAppFeatureMethods.cleanupStaleFiles(preloadAssets);
            } else {
                logger.verbose(accountId, "Ignoring cache eviction");
            }

        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }
    }
}
