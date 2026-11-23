package com.clevertap.android.sdk.pushnotification.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger
import java.util.concurrent.TimeUnit

/**
 * Fires at a broadcast Live Update's `dismissal_date` and expires the run on the owning instance:
 * unsubscribe the topic, remove the notification, raise Ended. A no-op when End or Dismiss already
 * closed the run. Inexact by design (Doze may delay it) — on API 26+ the OS already removed the
 * notification on time via `setTimeoutAfter`; this only has to catch up on topic + event.
 */
class CTLiveUpdateExpiryWork(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val topic = inputData.getString(KEY_TOPIC) ?: return Result.success()
        val accountId = inputData.getString(KEY_ACCOUNT_ID)
        try {
            val instances = CleverTapAPI.getAvailableInstances(applicationContext).filterNotNull()
            val instance = instances.firstOrNull { it.accountId == accountId }
                ?: CleverTapAPI.getDefaultInstance(applicationContext)
                ?: return Result.success()
            instance.coreState.pushProviders.liveUpdateBroadcastManager.expire(topic)
        } catch (t: Throwable) {
            Logger.v(TAG, "Failed to expire Live Update broadcast $topic", t)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "CTLiveUpdateExpiryWork"
        private const val KEY_TOPIC = "topic"
        private const val KEY_ACCOUNT_ID = "accountId"

        @JvmStatic
        fun schedule(context: Context, topic: String, accountId: String?, delayMillis: Long) {
            try {
                val request = OneTimeWorkRequest.Builder(CTLiveUpdateExpiryWork::class.java)
                    .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putString(KEY_TOPIC, topic)
                            .putString(KEY_ACCOUNT_ID, accountId)
                            .build()
                    )
                    .build()
                // Unique per topic: a re-delivered START replaces, not stacks, the expiry.
                CTWorkManagerAccessor.getInstance(context).enqueueUniqueWork(
                    "ct_la_expiry_$topic",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (t: Throwable) {
                Logger.v(TAG, "Failed to schedule Live Update expiry for $topic", t)
            }
        }
    }
}
