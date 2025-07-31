package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.MockCoreStateKotlin
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventGroup.VARIABLES
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_ENCRYPTION_ENABLED
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_MUTE
import com.clevertap.android.sdk.network.api.CtApi.Companion.SPIKY_HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApiTestProvider
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.api.EncryptionFailure
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import com.clevertap.android.sdk.network.http.MockHttpClient
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.CleverTapResponse
import com.clevertap.android.sdk.response.ClevertapResponseHandler
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.spyk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
// todo: can verify without manipulation of shared prefs; can improve later.
class NetworkManagerTest : BaseTestCase() {

    private var closeable: AutoCloseable? = null
    private lateinit var networkManager: NetworkManager
    private lateinit var ctApi: CtApi
    private lateinit var mockHttpClient: MockHttpClient
    private lateinit var ctApiWrapper: CtApiWrapper
    private val networkEncryptionManager: NetworkEncryptionManager = mockk(relaxed = true)
    private val networkRepo = mockk<NetworkRepo>(relaxed = true)
    private val clevertapResponseHandler = mockk<ClevertapResponseHandler>(relaxed = true)

    @Before
    fun setUpNetworkManager() {
        ctApiWrapper = mockk()
        mockHttpClient = MockHttpClient()
        ctApi = CtApiTestProvider.provideTestCtApiForConfig(cleverTapInstanceConfig, mockHttpClient)
        networkManager = provideNetworkManager()
        every { ctApiWrapper.ctApi } returns ctApi
    }

    @After
    fun tearDown() {
        closeable?.close()
    }

    @Test
    fun test_initHandshake_noHeaders_callSuccessCallback() {
        val callback = mockk<Runnable>(relaxed = true)
        networkManager.initHandshake(REGULAR, callback)
        verify { callback.run() }
    }

    @Test
    fun test_initHandshake_muteHeadersTrue_neverCallSuccessCallback() {
        val callback = mockk<Runnable>(relaxed = true)
        mockHttpClient.responseHeaders = mapOf(HEADER_MUTE to listOf("true"))
        networkManager.initHandshake(REGULAR, callback)
        verify(exactly = 0) { callback.run() }
    }

    @Test
    fun test_initHandshake_muteHeadersFalse_callSuccessCallback() {
        val callback = mockk<Runnable>(relaxed = true)
        mockHttpClient.responseHeaders = mapOf(HEADER_MUTE to listOf("false"))
        networkManager.initHandshake(REGULAR, callback)
        verify { callback.run() }
    }

    @Test
    fun test_initHandshake_changeDomainsHeaders_callSuccessCallbackAndUseDomains() {
        val callback = mockk<Runnable>(relaxed = true)
        val domain = "region.header-domain.com"
        val spikyDomain = "region-spiky.header-domain.com"
        // we only use changed domain when region is not configured
        ctApi.region = null
        mockHttpClient.responseHeaders = mapOf(
            HEADER_DOMAIN_NAME to listOf(domain),
            SPIKY_HEADER_DOMAIN_NAME to listOf(spikyDomain)
        )
        networkManager.initHandshake(REGULAR, callback)

        verify { callback.run() }
        assertEquals(domain, networkManager.getDomain(REGULAR))
        assertEquals(spikyDomain, networkManager.getDomain(PUSH_NOTIFICATION_VIEWED))
    }

    @Test
    fun test_sendQueue_requestFailure_returnFalse() {
        mockHttpClient.alwaysThrowOnExecute = true
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(2), null))
        assertFalse(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, getSampleJsonArrayOfJsonObjects(2), null))
    }

    @Test
    fun test_sendQueue_successResponseEmptyJsonBody_returnTrue() {
        mockHttpClient.responseBody = JSONObject().toString()
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(2), null))
        assertTrue(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, getSampleJsonArrayOfJsonObjects(2), null))
    }

    @Test
    fun test_sendQueue_successResponseNullBody_returnTrue() {
        mockHttpClient.responseBody = null
        mockHttpClient.responseCode = 200
        val queue = getSampleJsonArrayOfJsonObjects(
            totalJsonObjects = 4,
            printGenArray = true
        )
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))
        assertTrue(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null))

        // verify we do not process body
        verify(exactly = 0) { networkEncryptionManager.decryptResponse(any()) }
        verify(exactly = 0) { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
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

    /**
     * Test that when domain changes in the response, sendQueue returns false
     */
    @Test
    fun test_sendQueue_domainChanges_returnFalse() {
        // Arrange
        val newDomain = "new-domain.clevertap.com"
        val originalDomain = "original-domain.clevertap.com"

        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_DOMAIN_NAME to listOf(newDomain)
        )

        every { networkRepo.getDomain() } returns originalDomain
        every { networkRepo.setDomain(any()) } returns Unit

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act
        val op1 = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertFalse(op1)

        verify { networkRepo.getDomain() }
        verify { networkRepo.setDomain(newDomain) }

        // Act
        val op2 = networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null)

        // Assert
        assertFalse(op2)

        verify { networkRepo.getDomain() }
        verify { networkRepo.setDomain(newDomain) }
    }

    /**
     * Test that when processIncomingHeaders returns false (muted), sendQueue returns false
     */
    @Test
    fun test_sendQueue_processingMuteHeaders_returnFalse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("true") // This will cause processIncomingHeaders to return false
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act & Assert
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, queue, null))

        verify { networkRepo.setMuted(true) }

        // Act & Assert
        assertFalse(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null))

        verify { networkRepo.setMuted(true) }
    }

    @Test
    fun test_sendQueue_processingMuteHeaders_returnTrue() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("false") // This will cause processIncomingHeaders to return false
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act & Assert
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))

        verify { networkRepo.setMuted(false) }

        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("some-garbage") // This will cause processIncomingHeaders to return false
        )

        // Act & Assert
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))
    }

    /**
     * Test that when everything is successful, sendQueue saves request timestamps
     */
    @Test
    fun test_sendQueue_success_firstTime_savesRequestTs() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        val queue = getSampleJsonArrayOfJsonObjects(2)

        every { networkRepo.getFirstRequestTs() } returns 0
        // Act
        val op1 = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(op1)

        verify { networkRepo.setLastRequestTs(any()) }
        verify { networkRepo.setFirstRequestTs(any()) }

        // Act
        val op2 = networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null)

        // Assert
        assertTrue(op2)

        verify { networkRepo.setLastRequestTs(any()) }
        verify { networkRepo.setFirstRequestTs(any()) }
    }

    /**
     * Test that when everything is successful (not first time), sendQueue saves request last timestamp
     */
    @Test
    fun test_sendQueue_success_savesLastRequestTs() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        val queue = getSampleJsonArrayOfJsonObjects(2)

        every { networkRepo.getFirstRequestTs() } returns 12334
        // Act
        val op1 = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(op1)

        verify { networkRepo.setLastRequestTs(any()) }
        verify(exactly = 0) { networkRepo.setFirstRequestTs(any()) }

        // Act
        val op2 = networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null)

        // Assert
        assertTrue(op2)

        verify { networkRepo.setLastRequestTs(any()) }
        verify(exactly = 0) { networkRepo.setFirstRequestTs(any()) }
    }

    /**
     * Test that when body contains app launched event, processors receive full response flag
     */
    @Test
    fun test_sendQueue_withAppLaunchedEvent_processesWithFullResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        // Create queue with App Launched event
        val queue = JSONArray()
        val header = JSONObject()
        header.put("type", "meta")
        queue.put(header)

        val event = JSONObject()
        event.put("type", "event")
        event.put("evtName", Constants.APP_LAUNCHED_EVENT)
        queue.put(event)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to true for all processors
        verify { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    /**
     * Test that when body contains wzrk_fetch event, processors receive full response flag
     */
    @Test
    fun test_sendQueue_withWzrkFetchEvent_processesWithFullResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        // Create queue with wzrk_fetch event
        val queue = JSONArray()
        val header = JSONObject()
        header.put("type", "meta")
        queue.put(header)

        val event = JSONObject()
        event.put("type", "event")
        event.put("evtName", Constants.WZRK_FETCH)
        queue.put(event)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to true for all processors
        verify { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    /**
     * Test that when body does not contain app launched or wzrk_fetch events,
     * processors receive non-full response flag
     */
    @Test
    fun test_sendQueue_withoutSpecialEvents_processesWithNonFullResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        // Create queue with regular event
        val queue = JSONArray()
        val header = JSONObject()
        header.put("type", "meta")
        queue.put(header)

        val event = JSONObject()
        event.put("type", "event")
        event.put("evtName", "Regular Event")
        queue.put(event)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to false for all processors
        verify { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
    }

    /**
     * Test that when encryption is enabled in the response header, the response gets decrypted
     * before being passed to response processors
     */
    @Test
    fun test_sendQueue_whenEncryptionEnabledHeaderTrue_decryptsResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        // We'll use a simple JSON string for testing
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = mapOf(
            HEADER_ENCRYPTION_ENABLED to listOf("true")
        )

        // Setup the encryption manager to handle decryption
        every { networkEncryptionManager.decryptResponse(any()) } returns
            EncryptionSuccess("{}", "iv")

        // Create a simple queue with a regular event
        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify decryption was called
        verify { networkEncryptionManager.decryptResponse(any<String>()) }

        // Verify that the decrypted data was passed to all processors
        verify { clevertapResponseHandler.handleResponse(eq(false), any(), eq("{}"), any()) }
    }

    /**
     * Test that when encryption is enabled but decryption fails, sendQueue returns false
     */
    @Test
    fun test_sendQueue_whenEncryptionEnabledButDecryptionFails_returnsFalse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = mapOf(
            HEADER_ENCRYPTION_ENABLED to listOf("true")
        )

        // Setup the encryption manager to simulate decryption failure
        every { networkEncryptionManager.decryptResponse(any()) } returns EncryptionFailure

        // Create a simple queue
        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertFalse(result)

        // Verify decryption was attempted
        verify { networkEncryptionManager.decryptResponse(any<String>()) }
    }

    /**
     * Test that when encryption header is not present, the response is not decrypted
     */
    @Test
    fun test_sendQueue_whenEncryptionEnabledHeaderNotPresent_doesNotDecryptResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        // Create a simple queue
        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify decryption was not called
        verify(exactly = 0) { networkEncryptionManager.decryptResponse(any()) }
    }

    /**
     * Test that when encryption header is false, the response is not decrypted
     */
    @Test
    fun test_sendQueue_whenEncryptionEnabledHeaderFalse_doesNotDecryptResponse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = mapOf(
            HEADER_ENCRYPTION_ENABLED to listOf("false")
        )

        // Create a simple queue
        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify decryption was not called
        verify(exactly = 0) { networkEncryptionManager.decryptResponse(any()) }
    }

    @Test
    fun test_sendQueue_error402_returnFalse() {
        // Given - HTTP 402 Payment Required error
        mockHttpClient.responseCode = 402
        mockHttpClient.responseBody = getErrorJson().toString()
        val queue = getSampleJsonArrayOfJsonObjects(2)

        // When
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Then
        assertFalse(result)
    }

    @Test
    fun test_sendQueue_error419_returnFalse() {
        // Given - HTTP 419 Authentication Timeout error
        mockHttpClient.responseCode = 419
        mockHttpClient.responseBody = getErrorJson().toString()
        val queue = getSampleJsonArrayOfJsonObjects(2)

        // When
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Then
        assertFalse(result)
    }

    @Test
    fun `defineTemplates should return false when error response code is received`() {
        mockHttpClient.responseCode = 400
        mockHttpClient.responseBody = getErrorJson().toString()
        assertFalse(networkManager.defineTemplates(emptyList()))

        mockHttpClient.responseCode = 401
        assertFalse(networkManager.defineTemplates(emptyList()))

        mockHttpClient.responseCode = 500
        assertFalse(networkManager.defineTemplates(emptyList()))
    }

    @Test
    fun `defineTemplates should return false when http call results in an exception`() {
        mockHttpClient.alwaysThrowOnExecute = true
        assertFalse(networkManager.defineTemplates(emptyList()))
    }

    @Test
    fun `defineTemplates should return true when success response code is received`() {
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = getErrorJson().toString()
        assertTrue(networkManager.defineTemplates(emptyList()))
    }

    private fun provideNetworkManager(): NetworkManager {
        val metaData = CoreMetaData()
        val deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, "clevertapId", metaData)
        val coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        val callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        val lockManager = CTLockManager()
        val dbManager = DBManager(
            accountId = cleverTapInstanceConfig.accountId,
            logger = cleverTapInstanceConfig.logger,
            databaseName = DBAdapter.getDatabaseName(cleverTapInstanceConfig),
            ctLockManager = lockManager,
            ijRepo = IJRepo(cleverTapInstanceConfig),
        )
        val controllerManager =
            ControllerManager(appCtx, cleverTapInstanceConfig, lockManager, callbackManager, deviceInfo, dbManager)
        val queueHeaderBuilder = mockk<QueueHeaderBuilder>()
        every { queueHeaderBuilder.buildHeader(any()) } returns JSONObject()

        // Create 10 spy processors for response handling tests
        val responses = ArrayList<CleverTapResponse>()
        for (i in 1..10) {
            val spyProcessor = spyk<CleverTapResponse>()
            responses.add(spyProcessor)
        }

        // Pass the mock processors directly to the NetworkManager constructor
        return NetworkManager(
            context = appCtx,
            config = cleverTapInstanceConfig,
            deviceInfo = deviceInfo,
            coreMetaData = metaData,
            controllerManager = controllerManager,
            databaseManager = dbManager,
            callbackManager = callbackManager,
            ctApiWrapper = ctApiWrapper,
            encryptionManager = networkEncryptionManager,
            arpResponse = mockk<ARPResponse>(relaxed =  true),
            networkRepo = networkRepo,
            queueHeaderBuilder = queueHeaderBuilder,
            cleverTapResponseHandler = clevertapResponseHandler,
            logger = TestLogger()
        )
    }

    private fun getErrorJson(): JSONObject {
        return JSONObject().apply {
            put("error", "Error")
        }
    }
}