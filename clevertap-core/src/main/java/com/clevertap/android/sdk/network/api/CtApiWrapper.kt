package com.clevertap.android.sdk.network.api

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo

/**
 * Wrapper class for accessing the [CtApi] instance.
 *
 * This class provides lazy initialization of the [CtApi] instance, ensuring that it is only
 * initialized when accessed.
 *
 * @param context The application context.
 * @param config The configuration for the CleverTap instance.
 * @param deviceInfo The device information required for CleverTap initialization.
 */
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
