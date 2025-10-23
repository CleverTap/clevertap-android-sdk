package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.db.QueueData
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventGroup.VARIABLES
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApi.Companion.HEADER_MUTE
import com.clevertap.android.sdk.network.api.CtApi.Companion.SPIKY_HEADER_DOMAIN_NAME
import com.clevertap.android.sdk.network.api.CtApiTestProvider
import com.clevertap.android.sdk.network.api.CtApiWrapper
import com.clevertap.android.sdk.network.http.MockHttpClient
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
class NetworkManagerTest : BaseTestCase() {

    private var closeable: AutoCloseable? = null
    private lateinit var networkManager: NetworkManager
    private lateinit var ctApi: CtApi
    private lateinit var mockHttpClient: MockHttpClient
    private lateinit var ctApiWrapper: CtApiWrapper
    private lateinit var dbManager: DBManager
    private lateinit var mockCoreContract: CoreContract
    private lateinit var mockDeviceInfo: DeviceInfo
    private lateinit var mockCoreMetaData: CoreMetaData
    private val networkEncryptionManager: NetworkEncryptionManager = mockk(relaxed = true)
    private val networkRepo = mockk<NetworkRepo>(relaxed = true)

    @Before
    fun setUpNetworkManager() {
        ctApiWrapper = mockk()
        mockHttpClient = MockHttpClient()
        ctApi = CtApiTestProvider.provideTestCtApiForConfig(cleverTapInstanceConfig, mockHttpClient)
        
        // Setup mock CoreContract
        mockCoreContract = createMockCoreContract()
        
        networkManager = provideNetworkManager()
        every { ctApiWrapper.ctApi } returns ctApi
    }

    @After
    fun tearDown() {
        closeable?.close()
    }

    // ============ HANDSHAKE TESTS ============

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

    // ============ SEND QUEUE TESTS ============

    @Test
    fun test_sendQueue_requestFailure_returnFalse() {
        mockHttpClient.alwaysThrowOnExecute = true
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, getSampleJsonArrayOfJsonObjects(2), null))
        
        // Verify CoreContract was notified of failure
        verify { mockCoreContract.onFlushFailure(appCtx) }
        
        assertFalse(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, getSampleJsonArrayOfJsonObjects(2), null))
        verify(atLeast = 2) { mockCoreContract.onFlushFailure(appCtx) }
    }

    @Test
    fun test_sendQueue_successResponseEmptyJsonBody_returnTrue() {
        mockHttpClient.responseBody = JSONObject().toString()
        val queue = getSampleJsonArrayOfJsonObjects(2)
        
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))
        // Verify CoreContract handled the response
        verify { mockCoreContract.handleSendQueueResponse(any(), any(), any(), any(), any()) }
        
        assertTrue(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null))
        verify { mockCoreContract.handlePushImpressionsResponse(any(), any()) }
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
        // With null body, response handler should still be called but handle empty response
        verify { mockCoreContract.handleSendQueueResponse(any(), any(), any(), any(), any()) }
        
        assertTrue(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null))
        verify { mockCoreContract.handlePushImpressionsResponse(any(), any()) }
    }

    @Test
    fun test_sendVariablesQueue_successResponse_returnTrue() {
        mockHttpClient.responseBody = JSONObject().toString()
        assertTrue(networkManager.sendQueue(appCtx, VARIABLES, getSampleJsonArrayOfJsonObjects(1), null))
        
        // Verify CoreContract handled variables response
        verify { mockCoreContract.handleVariablesResponse(any()) }
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
        verify { mockCoreContract.notifySCDomainAvailable(any()) }

        // Act
        val op2 = networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null)

        // Assert
        assertFalse(op2)
        verify(atLeast = 2) { networkRepo.getDomain() }
        verify(atLeast = 2) { networkRepo.setDomain(newDomain) }
    }

    @Test
    fun test_sendQueue_processingMuteHeaders_returnFalse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("true")
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act & Assert
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, queue, null))
        verify { networkRepo.setMuted(true) }

        // Act & Assert
        assertFalse(networkManager.sendQueue(appCtx, PUSH_NOTIFICATION_VIEWED, queue, null))
        verify(atLeast = 2) { networkRepo.setMuted(true) }
    }

    @Test
    fun test_sendQueue_processingMuteHeaders_returnTrue() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("false")
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act & Assert
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))
        verify { networkRepo.setMuted(false) }

        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("some-garbage")
        )

        // Act & Assert
        assertTrue(networkManager.sendQueue(appCtx, REGULAR, queue, null))
    }

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
        verify(atLeast = 2) { networkRepo.setLastRequestTs(any()) }
        verify(atLeast = 2) { networkRepo.setFirstRequestTs(any()) }
    }

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
        verify(atLeast = 2) { networkRepo.setLastRequestTs(any()) }
        verify(exactly = 0) { networkRepo.setFirstRequestTs(any()) }
    }

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

        val isFullResponseSlot = slot<Boolean>()

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)
        verify { 
            mockCoreContract.handleSendQueueResponse(
                any(), 
                capture(isFullResponseSlot), 
                any(), 
                any(), 
                any()
            ) 
        }
        assertTrue(isFullResponseSlot.captured, "isFullResponse should be true for App Launched event")
    }

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

        val isFullResponseSlot = slot<Boolean>()

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)
        verify { 
            mockCoreContract.handleSendQueueResponse(
                any(), 
                capture(isFullResponseSlot), 
                any(), 
                any(), 
                any()
            ) 
        }
        assertTrue(isFullResponseSlot.captured, "isFullResponse should be true for wzrk_fetch event")
    }

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

        val isFullResponseSlot = slot<Boolean>()

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)
        verify { 
            mockCoreContract.handleSendQueueResponse(
                any(), 
                capture(isFullResponseSlot), 
                any(), 
                any(), 
                any()
            ) 
        }
        assertFalse(isFullResponseSlot.captured, "isFullResponse should be false for regular event")
    }

    @Test
    fun test_sendQueue_notifiesHeadersSent() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()

        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)
        verify { mockCoreContract.notifyHeadersSent(any(), any()) }
    }

    @Test
    fun test_sendQueue_getsHeadersFromCoreContract() {
        // Arrange
        val customHeaders = JSONObject().apply {
            put("X-Custom-Header", "custom-value")
        }
        every { mockCoreContract.getHeadersToAttach(any()) } returns customHeaders

        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()

        val queue = getSampleJsonArrayOfJsonObjects(1)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)
        verify { mockCoreContract.getHeadersToAttach(any()) }
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

    // ============ TEMPLATE TESTS ============

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

    // ============ FLUSH DB QUEUE TESTS ============

    @Test
    fun test_flushDBQueue_emptyQueue_shouldExitImmediately() {
        // Given
        val emptyQueueData = createQueueData(
            eventCount = 0,
            hasMore = false
        )

        every { dbManager.getQueuedEvents(any(), any(), any()) } returns emptyQueueData

        // When
        networkManager.flushDBQueue(appCtx, REGULAR, "test_caller", false)

        // Then
        verify(exactly = 1) { dbManager.getQueuedEvents(appCtx, 50, REGULAR) }
        verify(exactly = 0) { dbManager.cleanupSentEvents(any(), any(), any()) }
        verify(exactly = 0) { mockCoreContract.onNetworkError() }
        verify(exactly = 0) { mockCoreContract.onNetworkSuccess(any(), any()) }
    }

    @Test
    fun test_flushDBQueue_singleBatch_successfulSend() {
        // Given
        val queueData = createQueueData(
            eventCount = 5,
            hasMore = false
        )
        val eventIds = queueData.eventIds.toList()
        val profileEventIds = queueData.profileEventIds.toList()

        every { dbManager.getQueuedEvents(any(), any(), any()) } returns queueData

        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()

        // When
        networkManager.flushDBQueue(appCtx, REGULAR, "test_caller", false)

        // Then
        verify(exactly = 1) { dbManager.getQueuedEvents(appCtx, 50, REGULAR) }
        verify(exactly = 1) { dbManager.cleanupSentEvents(appCtx, eventIds, profileEventIds) }
        verify(exactly = 1) { mockCoreContract.onNetworkSuccess(queueData.data, true) }
        verify(exactly = 0) { mockCoreContract.onNetworkError() }
    }

    @Test
    fun test_flushDBQueue_multipleBatches_shouldProcessAll() {
        // Given
        val batch1 = createQueueData(
            eventCount = 50,
            hasMore = true
        )
        val batch2 = createQueueData(
            eventCount = 50,
            hasMore = true
        )
        val batch3 = createQueueData(
            eventCount = 25,
            hasMore = false
        )

        every { dbManager.getQueuedEvents(any(), any(), any()) } returnsMany listOf(batch1, batch2, batch3)

        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()

        // When
        networkManager.flushDBQueue(appCtx, REGULAR, "test_caller", false)

        // Then
        verify(exactly = 3) { dbManager.getQueuedEvents(appCtx, 50, REGULAR) }
        verify(exactly = 3) { dbManager.cleanupSentEvents(any(), any(), any()) }
        verify(exactly = 3) { mockCoreContract.onNetworkSuccess(any(), true) }
        verify(exactly = 0) { mockCoreContract.onNetworkError() }
    }

    @Test
    fun test_flushDBQueue_networkFailure_shouldStopProcessing() {
        // Given
        val batch1 = createQueueData(
            eventCount = 50,
            hasMore = true
        )

        every { dbManager.getQueuedEvents(any(), any(), any()) } returns batch1

        mockHttpClient.alwaysThrowOnExecute = true

        // When
        networkManager.flushDBQueue(appCtx, REGULAR, "test_caller", false)

        // Then
        verify(exactly = 1) { dbManager.getQueuedEvents(appCtx, 50, REGULAR) }
        verify(exactly = 0) { dbManager.cleanupSentEvents(any(), any(), any()) }
        verify(exactly = 1) { mockCoreContract.onNetworkError() }
        verify(exactly = 1) { mockCoreContract.onNetworkSuccess(batch1.data, false) }
    }

    @Test
    fun test_flushDBQueue_pushNotificationEvents_shouldUseSpecialCleanup() {
        // Given
        val eventIds = listOf("push1", "push2", "push3")
        val pushData = QueueData().apply {
            eventIds.forEach { id ->
                data.put(JSONObject().apply {
                    put("type", "event")
                    put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
                    put("evtData", JSONObject().apply {
                        put(Constants.WZRK_PUSH_ID, "push_$id")
                        put(Constants.WZRK_ACCT_ID_KEY, "account_$id")
                    })
                })
            }
            this.eventIds.addAll(eventIds)
            this.hasMore = false
        }

        every { dbManager.getQueuedEvents(any(), any(), any()) } returns pushData

        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()

        // When
        networkManager.flushDBQueue(appCtx, PUSH_NOTIFICATION_VIEWED, "push_caller", false)

        // Then
        verify(exactly = 1) { dbManager.getQueuedEvents(appCtx, 50, PUSH_NOTIFICATION_VIEWED) }
        verify(exactly = 1) { dbManager.cleanupPushNotificationEvents(appCtx, eventIds) }
        verify(exactly = 0) { dbManager.cleanupSentEvents(any(), any(), any()) }
        verify(exactly = 1) { mockCoreContract.onNetworkSuccess(pushData.data, true) }
    }

    // ============ HELPER METHODS ============

    private fun createQueueData(
        eventCount: Int,
        hasMore: Boolean = false
    ): QueueData {
        val queueData = QueueData()

        repeat(eventCount) { i ->
            val isEvent = i % 2 == 0
            val type = if (isEvent) "event" else "profile"
            val id = "${type}_id_$i"

            queueData.data.put(JSONObject().apply {
                put("type", type)
                put("evtName", "test_${type}_$i")
                put("evtData", JSONObject().apply {
                    put("key", "value_$i")
                })
            })

            if (isEvent) {
                queueData.eventIds.add(id)
            } else {
                queueData.profileEventIds.add(id)
            }
        }

        queueData.hasMore = hasMore
        return queueData
    }

    private fun createMockCoreContract(): CoreContract {
        dbManager = mockk<DBManager>(relaxed = true)
        mockCoreMetaData = CoreMetaData()
        val mockContract = mockk<CoreContract>(relaxed = true)
        
        // Setup basic return values
        every { mockContract.context() } returns appCtx
        every { mockContract.config() } returns cleverTapInstanceConfig
        every { mockContract.logger() } returns TestLogger()
        every { mockContract.database() } returns dbManager
        
        // Mock DeviceInfo
        mockDeviceInfo = MockDeviceInfo(
            context = application,
            config = cleverTapInstanceConfig,
            cleverTapID = "clevertapId",
            coreMetaData = mockCoreMetaData
        )
        every { mockContract.deviceInfo() } returns mockDeviceInfo
        
        // Mock CoreMetaData
        mockCoreMetaData = CoreMetaData()
        every { mockContract.coreMetaData() } returns mockCoreMetaData
        
        // Mock response handling methods
        every { 
            mockContract.handleSendQueueResponse(any(), any(), any(), any(), any()) 
        } just Runs
        
        every { 
            mockContract.handleVariablesResponse(any()) 
        } just Runs
        
        every { 
            mockContract.handlePushImpressionsResponse(any(), any()) 
        } just Runs
        
        every { mockContract.onNetworkError() } just Runs
        every { mockContract.onNetworkSuccess(any(), any()) } just Runs
        every { mockContract.onFlushFailure(any()) } just Runs
        every { mockContract.notifyHeadersSent(any(), any()) } just Runs
        every { mockContract.getHeadersToAttach(any()) } returns null
        every { mockContract.notifySCDomainAvailable(any()) } just Runs
        every { mockContract.notifySCDomainUnavailable() } just Runs
        
        return mockContract
    }

    private fun provideNetworkManager(): NetworkManager {

        val queueHeaderBuilder = mockk<QueueHeaderBuilder>()
        every { queueHeaderBuilder.buildHeader(any()) } returns JSONObject()

        // NetworkManager now only takes CoreContract + network-specific dependencies
        return NetworkManager(
            ctApiWrapper = ctApiWrapper,
            encryptionManager = networkEncryptionManager,
            networkRepo = networkRepo,
            queueHeaderBuilder = queueHeaderBuilder
        ).apply {
            coreContract = mockCoreContract
        }
    }

    private fun getErrorJson(): JSONObject {
        return JSONObject().apply {
            put("error", "Error")
        }
    }
}
