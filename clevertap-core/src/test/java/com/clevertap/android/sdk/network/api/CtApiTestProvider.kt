package com.clevertap.android.sdk.network.api

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.http.CtHttpClient
import com.clevertap.android.sdk.network.http.MockHttpClient
import org.mockito.*

object CtApiTestProvider {

    const val DEFAULT_DOMAIN = "domain.com"
    const val DOMAIN = "new.domain.com"
    const val REGION = "region"
    const val SPIKY_DOMAIN = "new-spiky.domain.com"
    const val PROXY_DOMAIN = "proxy-domain.com"
    const val SPIKY_PROXY_DOMAIN = "proxy-spiky-domain.com"

    const val ACCOUNT_ID = "accountId"
    const val ACCOUNT_TOKEN = "accountToken"
    const val SDK_VERSION = "x.x.x-test"

    fun provideDefaultTestCtApi(): CtApi {

        return CtApi(
            MockHttpClient(),
            DEFAULT_DOMAIN,
            DOMAIN,
            SPIKY_DOMAIN,
            REGION,
            PROXY_DOMAIN,
            SPIKY_PROXY_DOMAIN,
            ACCOUNT_ID,
            ACCOUNT_TOKEN,
            SDK_VERSION,
            Mockito.mock(Logger::class.java),
            "testCtApi"
        )
    }

    fun provideTestCtApiForConfig(
        config: CleverTapInstanceConfig,
        httpClient: CtHttpClient = MockHttpClient()
    ): CtApi {
        return CtApi(
            httpClient,
            Constants.PRIMARY_DOMAIN,
            null,
            null,
            config.accountRegion,
            config.proxyDomain,
            config.spikyProxyDomain,
            config.accountId,
            config.accountToken,
            SDK_VERSION,
            config.logger,
            "testCtApi"
        )
    }
}
