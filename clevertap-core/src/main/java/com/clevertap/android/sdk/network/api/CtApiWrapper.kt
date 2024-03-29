package com.clevertap.android.sdk.network.api

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo

class CtApiWrapper internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo
) {

    @get:WorkerThread
    val ctApi: CtApi by lazy {
        provideDefaultTestCtApi(context, config, deviceInfo)
    }
}
