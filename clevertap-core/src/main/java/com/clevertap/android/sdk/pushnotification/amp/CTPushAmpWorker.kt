package com.clevertap.android.sdk.pushnotification.amp

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Logger

class CTPushAmpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        Logger.v("PushAmpWorker is awake")
        CleverTapAPI.runJobWork(applicationContext)
        return Result.success()
    }
}