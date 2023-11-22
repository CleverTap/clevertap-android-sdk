package com.clevertap.android.sdk.pushnotification.work

import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.os.BatteryManager
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.flushPushImpressionsOnPostAsyncSafely

class CTFlushPushImpressionsWork(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    val tag = "CTFlushPushImpressionsWork"

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    override fun doWork(): Result {
        Logger.d(
            tag,
            "hello, this is FlushPushImpressionsWork from CleverTap. I am awake now and ready to flush push impressions:-)"
        )
        Logger.d(tag, "initiating push impressions flush...")

        val context = applicationContext
        CleverTapAPI.getAvailableInstances(context)
            .filterNotNull()
            .filter {
                !it.coreState.config.isAnalyticsOnly
            }
            .forEach {
                if (checkIfStopped())
                    return Result.success()

                it.apply {
                    Logger.d(tag, "flushing queue for push impressions on CT instance = $accountId")
                    flushPushImpressionsOnPostAsyncSafely(tag, Constants.D_SRC_PI_WM, context)
                }
            }

        Logger.d(tag, "flush push impressions work is DONE! going to sleep now...ˁ(-.-)ˀzzZZ")
        return Result.success()
    }

    private fun checkIfStopped(): Boolean {
        if (isStopped) {
            Logger.d(tag, "someone told me to stop flushing and go to sleep again! going to sleep now.ˁ(-.-)ˀzzZZ")
        }
        return isStopped
    }
}
