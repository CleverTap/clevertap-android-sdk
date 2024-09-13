package com.clevertap.android.sdk.network.api

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient

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
        cachedHandshakeDomain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_HANDSHAKE_DOMAIN_NAME, null),
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
