package com.clevertap.android.sdk.network.api

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient

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
        CtApiProvider.provideDefaultCtApi(
            context = context,
            config = config,
            deviceInfo = deviceInfo
        )
    }

    fun needsHandshake(
        isViewedEvent: Boolean
    ) : Boolean =
        ctApi.needsHandshake(
            isViewedEvent = isViewedEvent
        )
}

internal object CtApiProvider {

    @WorkerThread
    internal fun provideDefaultCtApi(
        context: Context,
        config: CleverTapInstanceConfig,
        deviceInfo: DeviceInfo
    ): CtApi {
        val httpClient = UrlConnectionHttpClient(
            isSslPinningEnabled = config.isSslPinningEnabled,
            logger = config.logger,
            logTag = config.accountId
        )

        return CtApi(
            httpClient = httpClient,
            defaultDomain = Constants.PRIMARY_DOMAIN,
            cachedDomain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null),
            cachedSpikyDomain = StorageHelper.getStringFromPrefs(context, config, Constants.SPIKY_KEY_DOMAIN_NAME, null),
            region = config.accountRegion,
            proxyDomain = config.proxyDomain,
            spikyProxyDomain = config.spikyProxyDomain,
            customHandshakeDomain = config.customHandshakeDomain,
            accountId = config.accountId,
            accountToken = config.accountToken,
            sdkVersion = deviceInfo.sdkVersion.toString(),
            logger = config.logger,
            logTag = config.accountId
        )
    }
}
