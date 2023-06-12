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
        config.getLogger().verbose(accountId,
                "scheduling one time work request to flush push impressions...");

        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(true)
                    .build();

            OneTimeWorkRequest flushPushImpressionsWorkRequest = new OneTimeWorkRequest
                    .Builder(CTFlushPushImpressionsWork.class)
                    .setConstraints(constraints)
                    .build();

            // schedule unique work request to avoid duplicates
            WorkManager.getInstance(context).enqueueUniqueWork(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME,
                    ExistingWorkPolicy.KEEP, flushPushImpressionsWorkRequest);

            config.getLogger().verbose(accountId,
                    "Finished scheduling one time work request to flush push impressions...");

        } catch (Throwable t) {
            config.getLogger().verbose(accountId,
                    "Failed to schedule one time work request to flush push impressions.", t);
            t.printStackTrace();
        }
    }

    public void cancelPushImpressionsFlushWork() {
        try {
            config.getLogger().verbose(config.getAccountId(),
                    "cancelling already scheduled one time work request to flush push impressions...");

            WorkManager.getInstance(context).cancelUniqueWork(FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME);

            config.getLogger().verbose(config.getAccountId(),
                    "Finished cancelling one time work request to flush push impressions...");
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(),
                    "Failed to cancel one time work request to flush push impressions.", t);
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

            /*
                 +------+  +------+  +------+
                 |  C1  |  |  C2  |  |  C3  |
                 +------+  +------+  +------+
                    |         |         |
                    v         v         v
                 +------+  +------+  +------+
                 |  P1  |  |  P2  |  |  P3  |
                 +------+  +------+  +------+
                    |         |          |
                    +----X----+----------+
                              |
                       +-------------+
                       |     N/w     |
                       +-------------+

                       +---------------+
                       |     Worker    | [Shared among C1,C2,C3]
                       +---------------+

               Worker is only one among all CT instance with name flushPushImpressionsOneTime. Let's say
               All C1, C2, C3 try to flush push impression through n/w in order C1 -> C2 -> C3. All instance will
               try to set listener to take decision if worker need to be cancel. Let's say C1 sets listener and n/w
               fails for C1, in this case worker is needed for C1 to flush it's push impression. Now let's C2 and C3
               also sets listener but n/w successfully sends PI for C2 and C3, in this case they will cancel
               Worker. But since worker is shared among all CT instance, Worker needed for C1 will get cancel and
               PI flush will not happen since C3/C2 canceled it. hence we should use this cancel approach only when
               there is only one instance of CT. in case it is multi, let the worker run irrespective of PI sent or
               not during rendering. we can not solve for multi instance because then we will need flags for each C1,C2
               C3 and need to persist it because process may get killed any time.
                Canceling worker is slight optimization which is enough for single instance.

            */
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
