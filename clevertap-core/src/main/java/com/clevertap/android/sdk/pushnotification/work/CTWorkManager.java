package com.clevertap.android.sdk.pushnotification.work;

import static com.clevertap.android.sdk.CTXtensions.isPackageAndOsTargetsAbove;
import static com.clevertap.android.sdk.Constants.FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME;

import android.content.Context;
import android.os.Build;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Utils;

public class CTWorkManager {

    private final Context context;
    private final CleverTapInstanceConfig config;

    public CTWorkManager(Context context, CleverTapInstanceConfig config){
        this.context = context;
        this.config = config;
    }
    public void schedulePushImpressionsFlushWork() {
        String accountId = config.getAccountId();
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .build();

            OneTimeWorkRequest flushPushImpressionsWorkRequest = new OneTimeWorkRequest
                    .Builder(FlushPushImpressionsWork.class)
                    .setConstraints(constraints)
                    .build();

            // schedule unique work request to avoid duplicates
            WorkManager.getInstance(context).enqueueUniqueWork(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME,
                    ExistingWorkPolicy.KEEP, flushPushImpressionsWorkRequest);

            config.getLogger().debug(accountId,
                    "Finished scheduling one time work request for push impressions flush...");

        } catch (Throwable t) {
            config.getLogger().debug(accountId,
                    "Failed to schedule one time work request for push impressions flush.", t);
            t.printStackTrace();
        }
    }

    public void cancelPushImpressionsFlushWork() {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME);
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(),
                    "Failed to cancel one time work request for push impressions flush.", t);
            t.printStackTrace();
        }
    }

    public void init() {
        /**
         * Due to ANR below O, schedule push impression flush worker for O and above.
         * Also we need worker on main process only.
         */
        if (isPackageAndOsTargetsAbove(context, Build.VERSION_CODES.O) &&
                Utils.isMainProcess(context, context.getPackageName())) {
            schedulePushImpressionsFlushWork();

            if (CleverTapAPI.getInstances().size() < 2) {
                CleverTapAPI.addNotificationRenderedListener(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME, (isSuccess) -> {
                    /**
                     * For single CT instance, When push impression event table is empty in DB,
                     * we don't need worker anymore. So cancel it.
                     */
                    cancelPushImpressionsFlushWork();
                    CleverTapAPI.removeNotificationRenderedListener(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME);
                });
            }
        }
    }
}
