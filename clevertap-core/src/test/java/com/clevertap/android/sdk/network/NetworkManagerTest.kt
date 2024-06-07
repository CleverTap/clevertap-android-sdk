package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.LocalDataStoreProvider
import com.clevertap.android.sdk.MockCoreState
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.cryption.CryptHandler.EncryptionAlgorithm.AES
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventGroup.VARIABLES
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApiTestProvider
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.http.MockHttpClient
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NetworkManagerTest : BaseTestCase() {

    private lateinit var networkManager: NetworkManager
    private lateinit var ctApi: CtApi
    private lateinit var mockHttpClient: MockHttpClient
    @Mock private lateinit var ctApiWrapper : CtApiWrapper

    @Before
    fun setUpNetworkManager() {
        MockitoAnnotations.openMocks(this)
        mockHttpClient = MockHttpClient()
        ctApi = CtApiTestProvider.provideTestCtApiForConfig(cleverTapInstanceConfig, mockHttpClient)
        networkManager = provideNetworkManager()
        `when`(ctApiWrapper.ctApi).thenReturn(ctApi)
    }

    @Test
    fun test_initHandshake_noHeaders_callSuccessCallback() {
        val callback = Mockito.mock(Runnable::class.java)
        networkManager.initHandshake(REGULAR, callback)
        Mockito.verify(callback).run()
    }

    @Test
    fun test_initHandshake_muteHeadersTrue_neverCallSuccessCallback() {
        val callback = Mockito.mock(Runnable::class.java)
        mockHttpClient.responseHeaders = mapOf(Constants.HEADER_MUTE to listOf("true"))
        networkManager.initHandshake(REGULAR, callback)
        Mockito.verify(callback, Mockito.never()).run()
    }

    @Test
    fun test_initHandshake_muteHeadersFalse_callSuccessCallback() {
        val callback = Mockito.mock(Runnable::class.java)
        mockHttpClient.responseHeaders = mapOf(Constants.HEADER_MUTE to listOf("false"))
        networkManager.initHandshake(REGULAR, callback)
        Mockito.verify(callback).run()
    }

    @Test
    fun test_initHandshake_changeDomainsHeaders_callSuccessCallbackAndUseDomains() {
        val callback = Mockito.mock(Runnable::class.java)
        val domain = "region.header-domain.com"
        val spikyDomain = "region-spiky.header-domain.com"
        // we only use changed domain when region is not configured
        ctApi.region = null
        mockHttpClient.responseHeaders = mapOf(
            Constants.HEADER_DOMAIN_NAME to listOf(domain),
            Constants.SPIKY_HEADER_DOMAIN_NAME to listOf(spikyDomain)
        )
        networkManager.initHandshake(REGULAR, callback)

        Mockito.verify(callback).run()
        assertEquals(domain, networkManager.getDomain(REGULAR))
        assertEquals(spikyDomain, networkManager.getDomain(PUSH_NOTIFICATION_VIEWED))
    }

    @Test
    fun test_sendQueue_requestFailure_returnFalse() {
        mockHttpClient.alwaysThrowOnExecute = true
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(2), null))
    }

    @Test
    fun test_sendQueue_successResponseEmptyJsonBody_returnTrue() {
        mockHttpClient.responseBody = JSONObject().toString()
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(2), null))
    }

    @Test
    fun test_sendQueue_successResponseNullBody_returnFalse() {
        mockHttpClient.responseBody = null
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(4), null))
    }

    @Test
    fun test_sendVariablesQueue_successResponse_returnTrue() {
        mockHttpClient.responseBody = JSONObject().toString()
        assertTrue(networkManager.sendQueue(appCtx, VARIABLES, getSampleJsonArrayOfJsonObjects(1), null))
    }

    @Test
    fun test_sendVariablesQueue_errorResponses_returnFalse() {
        mockHttpClient.responseCode = 400
        assertFalse(networkManager.sendQueue(appCtx, VARIABLES, getSampleJsonArrayOfJsonObjects(1), null))
        mockHttpClient.responseCode = 401
        assertFalse(networkManager.sendQueue(appCtx, VARIABLES, getSampleJsonArrayOfJsonObjects(1), null))
        mockHttpClient.responseCode = 500
        assertFalse(networkManager.sendQueue(appCtx, VARIABLES, getSampleJsonArrayOfJsonObjects(1), null))
    }

    private fun provideNetworkManager(): NetworkManager {
        val metaData = CoreMetaData()
        val deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, "clevertapId", metaData)
        val coreState = MockCoreState(appCtx, cleverTapInstanceConfig)
        val callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        val lockManager = CTLockManager()
        val dbManager = DBManager(cleverTapInstanceConfig, lockManager)
        val controllerManager =
            ControllerManager(appCtx, cleverTapInstanceConfig, lockManager, callbackManager, deviceInfo, dbManager)
        val cryptHandler = CryptHandler(0, AES, cleverTapInstanceConfig.accountId)
        val localDataStore =
            LocalDataStoreProvider.provideLocalDataStore(appCtx, cleverTapInstanceConfig, cryptHandler, deviceInfo)
        val triggersManager = TriggerManager(appCtx, cleverTapInstanceConfig.accountId, deviceInfo)
        val inAppResponse =
            InAppResponse(
                cleverTapInstanceConfig,
                controllerManager,
                true,
                coreState.storeRegistry,
                triggersManager,
                metaData
            )

        return NetworkManager(
            appCtx,
            cleverTapInstanceConfig,
            deviceInfo,
            metaData,
            ValidationResultStack(),
            controllerManager,
            dbManager,
            callbackManager,
            lockManager,
            Validator(),
            inAppResponse,
            ctApiWrapper
        )
    }
}
