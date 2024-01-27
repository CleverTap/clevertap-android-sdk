package com.clevertap.android.sdk.network.api

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient

fun provideDefaultTestCtApi(
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
        domain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null),
        spikyDomain = StorageHelper.getStringFromPrefs(context, config, Constants.SPIKY_KEY_DOMAIN_NAME, null),
        region = config.accountRegion,
        proxyDomain = config.proxyDomain,
        spikyProxyDomain = config.spikyProxyDomain,
        accountId = config.accountId,
        accountToken = config.accountToken,
        sdkVersion = deviceInfo.sdkVersion.toString(),
        logger = config.logger,
        logTag = config.accountId
    )
}
