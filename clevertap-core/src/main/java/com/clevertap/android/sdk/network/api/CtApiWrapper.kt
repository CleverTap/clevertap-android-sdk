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
internal class CtApiWrapper(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val deviceInfo: DeviceInfo
) {

    @get:WorkerThread
    val ctApi: CtApi by lazy {
        provideDefaultCtApi(
            context = context,
            config = config,
            deviceInfo = deviceInfo
        )
    }

    fun needsHandshake(isViewedEvent: Boolean) : Boolean =
        ctApi.needsHandshake(isViewedEvent = isViewedEvent)

}
