package com.clevertap.android.sdk.network.api

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.MockHttpClient
import org.mockito.*

internal object CtApiTestProvider {

    const val DEFAULT_DOMAIN = "domain.com"
    const val CACHED_DOMAIN = "new.domain.com"
    const val REGION = "region"
    const val CACHED_SPIKY_DOMAIN = "new-spiky.domain.com"
    const val PROXY_DOMAIN = "proxy-domain.com"
    const val SPIKY_PROXY_DOMAIN = "proxy-spiky-domain.com"
    const val CUSTOM_HANDSHAKE_DOMAIN = "custom-handshake-domain.com"

    const val ACCOUNT_ID = "accountId"
    const val ACCOUNT_TOKEN = "accountToken"
    const val SDK_VERSION = "x.x.x-test"

    fun provideDefaultTestCtApi(): CtApi {

        return CtApi(
            httpClient = MockHttpClient(),
            defaultDomain = DEFAULT_DOMAIN,
            cachedDomain = CACHED_DOMAIN,
            cachedSpikyDomain = CACHED_SPIKY_DOMAIN,
            region = REGION,
            proxyDomain = PROXY_DOMAIN,
            spikyProxyDomain = SPIKY_PROXY_DOMAIN,
            customHandshakeDomain = null,
            accountId = ACCOUNT_ID,
            accountToken = ACCOUNT_TOKEN,
            sdkVersion = SDK_VERSION,
            logger = Mockito.mock(Logger::class.java),
            logTag = "testCtApi"
        )
    }

    fun provideTestCtApiForConfig(
        config: CleverTapInstanceConfig,
        httpClient: CtHttpClient = MockHttpClient()
    ): CtApi {
        return CtApi(
            httpClient = httpClient,
            defaultDomain = Constants.PRIMARY_DOMAIN,
            cachedDomain = null,
            cachedSpikyDomain = null,
            region = config.accountRegion,
            proxyDomain = config.proxyDomain,
            spikyProxyDomain = config.spikyProxyDomain,
            customHandshakeDomain = config.customHandshakeDomain,
            accountId = config.accountId,
            accountToken = config.accountToken,
            sdkVersion = SDK_VERSION,
            logger = config.logger,
            logTag = "testCtApi"
        )
    }
}
