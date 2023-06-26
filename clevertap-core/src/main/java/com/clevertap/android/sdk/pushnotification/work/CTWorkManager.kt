package com.clevertap.android.sdk.pushnotification.work

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.isPackageAndOsTargetsAbove
import com.clevertap.android.sdk.Constants.FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME
import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Utils

class CTWorkManager(private val context: Context, config: CleverTapInstanceConfig) {

    private val accountId: String = config.accountId
    private val logger: Logger = config.logger

    private fun schedulePushImpressionsFlushWork() {

        logger.verbose(accountId, "scheduling one time work request to flush push impressions...")

        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(true)
                .build()

            val flushPushImpressionsWorkRequest = OneTimeWorkRequest.Builder(CTFlushPushImpressionsWork::class.java)
                .setConstraints(constraints)
                .build()

            // schedule unique work request to avoid duplicates
            WorkManager.getInstance(context).enqueueUniqueWork(
                FLUSH_PUSH_IMPRESSIONS_ONE_TIME_WORKER_NAME,
                ExistingWorkPolicy.KEEP,
                flushPushImpressionsWorkRequest
            )

            logger.verbose(accountId, "Finished scheduling one time work request to flush push impressions...")
        } catch (t: Throwable) {
            logger.verbose(accountId, "Failed to schedule one time work request to flush push impressions.", t)
            t.printStackTrace()
        }
    }

    fun init() {
        /**
         * Due to ANR below O, schedule push impression flush worker for O and above.
         * Also we need worker on main process only.
         */
        if (context.isPackageAndOsTargetsAbove(Build.VERSION_CODES.O) &&
            Utils.isMainProcess(context, context.packageName)
        ) {
            schedulePushImpressionsFlushWork()
        }
    }
}
