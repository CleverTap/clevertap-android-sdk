package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.MockCoreState
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.events.EventGroup.REGULAR
import com.clevertap.android.sdk.events.EventGroup.VARIABLES
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
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
import com.clevertap.android.sdk.response.CleverTapResponse
import com.clevertap.android.sdk.response.InAppResponse
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import io.mockk.every
import org.mockito.MockitoAnnotations
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
    @Mock private lateinit var ctApiWrapper : CtApiWrapper
    private val networkEncryptionManager: NetworkEncryptionManager = mockk(relaxed = true)

    @Before
    fun setup() {
        closeable = MockitoAnnotations.openMocks(this)
        mockHttpClient = MockHttpClient()
        ctApi = CtApiTestProvider.provideTestCtApiForConfig(cleverTapInstanceConfig, mockHttpClient)
        networkManager = provideNetworkManager()
        `when`(ctApiWrapper.ctApi).thenReturn(ctApi)
    }

    @After
    fun tearDown() {
        closeable?.close()
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
        mockHttpClient.responseHeaders = mapOf(HEADER_MUTE to listOf("true"))
        networkManager.initHandshake(REGULAR, callback)
        Mockito.verify(callback, Mockito.never()).run()
    }

    @Test
    fun test_initHandshake_muteHeadersFalse_callSuccessCallback() {
        val callback = Mockito.mock(Runnable::class.java)
        mockHttpClient.responseHeaders = mapOf(HEADER_MUTE to listOf("false"))
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
            HEADER_DOMAIN_NAME to listOf(domain),
            SPIKY_HEADER_DOMAIN_NAME to listOf(spikyDomain)
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

    /**
     * Test that when domain changes in the response, sendQueue returns false
     */
    @Test
    fun test_sendQueue_domainChanges_returnFalse() {
        // Arrange
        val newDomain = "new-domain.clevertap.com"
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_DOMAIN_NAME to listOf(newDomain)
        )

        // Force network manager to recognize domain as changed
        val originalDomain = networkManager.getDomain(REGULAR)
        StorageHelper.putString(
            appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig, Constants.KEY_DOMAIN_NAME),
            "different-domain.com"
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertFalse(result)

        // Check if domain was updated
        val updatedDomain = networkManager.getDomain(REGULAR)
        assert(updatedDomain == newDomain)

        // Reset domain for other tests
        StorageHelper.putString(
            appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig.accountId, Constants.KEY_DOMAIN_NAME),
            originalDomain
        )
    }

    /**
     * Test that when processIncomingHeaders returns false (muted), sendQueue returns false
     */
    @Test
    fun test_sendQueue_processingHeadersFails_returnFalse() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseHeaders = mapOf(
            HEADER_MUTE to listOf("true") // This will cause processIncomingHeaders to return false
        )

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Act & Assert
        assertFalse(networkManager.sendQueue(appCtx, REGULAR, queue, null))

        // Verify we're muted
        val isMuted = StorageHelper.getIntFromPrefs(
            appCtx,
            cleverTapInstanceConfig,
            Constants.KEY_MUTED,
            0
        )
        assert(isMuted > 0)

        // Reset muted status for other tests
        StorageHelper.putInt(
            appCtx,
            StorageHelper.storageKeyWithSuffix(cleverTapInstanceConfig, Constants.KEY_MUTED),
            0
        )
    }

    /**
     * Test that when everything is successful, sendQueue returns true
     */
    @Test
    fun test_sendQueue_success_returnTrue() {
        // Arrange
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = JSONObject().toString()
        mockHttpClient.responseHeaders = emptyMap()

        val queue = getSampleJsonArrayOfJsonObjects(2)

        // Get the current timestamps for verification
        val initialTimestamp = StorageHelper.getIntFromPrefs(
            appCtx,
            cleverTapInstanceConfig,
            Constants.KEY_LAST_TS,
            0
        )

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify timestamps were updated
        val updatedTimestamp = StorageHelper.getIntFromPrefs(
            appCtx,
            cleverTapInstanceConfig,
            Constants.KEY_LAST_TS,
            0
        )
        assert(updatedTimestamp >= initialTimestamp)
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

        // Create mock response processors to verify isFullResponse flag
        val originalProcessors = networkManager.cleverTapResponses
        val mockProcessors = ArrayList<CleverTapResponse>()
        for (processor in originalProcessors) {
            val spyProcessor = spyk(processor)
            mockProcessors.add(spyProcessor)
        }

        // Replace original processors with spies
        networkManager.cleverTapResponses.clear()
        networkManager.cleverTapResponses.addAll(mockProcessors)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to true for all processors
        for (processor in networkManager.cleverTapResponses) {
            assertTrue(processor.isFullResponse)
            verify { processor.processResponse(any(), any(), any()) }
        }
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

        // Create mock response processors to verify isFullResponse flag
        val originalProcessors = networkManager.cleverTapResponses
        val mockProcessors = ArrayList<CleverTapResponse>()
        for (processor in originalProcessors) {
            val spyProcessor = spyk(processor)
            mockProcessors.add(spyProcessor)
        }

        // Replace original processors with spies
        networkManager.cleverTapResponses.clear()
        networkManager.cleverTapResponses.addAll(mockProcessors)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to true for all processors
        for (processor in networkManager.cleverTapResponses) {
            assertTrue(processor.isFullResponse)
            verify { processor.processResponse(any(), any(), any()) }
        }
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

        // Create mock response processors to verify isFullResponse flag
        val originalProcessors = networkManager.cleverTapResponses
        val mockProcessors = ArrayList<CleverTapResponse>()
        for (processor in originalProcessors) {
            val spyProcessor = spyk(processor)
            mockProcessors.add(spyProcessor)
        }

        // Replace original processors with spies
        networkManager.cleverTapResponses.clear()
        networkManager.cleverTapResponses.addAll(mockProcessors)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify that isFullResponse is set to false for all processors
        for (processor in networkManager.cleverTapResponses) {
            assertFalse(processor.isFullResponse)
            verify { processor.processResponse(any(), any(), any()) }
        }
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

        // Create mock response processors to verify decrypted data is passed
        val originalProcessors = networkManager.cleverTapResponses
        val mockProcessors = ArrayList<CleverTapResponse>()
        for (processor in originalProcessors) {
            val spyProcessor = spyk(processor)
            mockProcessors.add(spyProcessor)
        }

        // Replace original processors with spies
        networkManager.cleverTapResponses.clear()
        networkManager.cleverTapResponses.addAll(mockProcessors)

        // Act
        val result = networkManager.sendQueue(appCtx, REGULAR, queue, null)

        // Assert
        assertTrue(result)

        // Verify decryption was called
        verify { networkEncryptionManager.decryptResponse(any<String>()) }

        // Verify that the decrypted data was passed to all processors
        for (processor in networkManager.cleverTapResponses) {
            verify { processor.processResponse(any(), eq("{}"), any()) }
        }
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
    fun `defineTemplates should return false when error response code is received`() {
        mockHttpClient.responseCode = 400
        mockHttpClient.responseBody = getErrorJson().toString()
        assertFalse(networkManager.defineTemplates(appCtx, emptyList()))

        mockHttpClient.responseCode = 401
        assertFalse(networkManager.defineTemplates(appCtx, emptyList()))

        mockHttpClient.responseCode = 500
        assertFalse(networkManager.defineTemplates(appCtx, emptyList()))
    }

    @Test
    fun `defineTemplates should return false when http call results in an exception`() {
        mockHttpClient.alwaysThrowOnExecute = true
        assertFalse(networkManager.defineTemplates(appCtx, emptyList()))
    }

    @Test
    fun `defineTemplates should return true when success response code is received`() {
        mockHttpClient.responseCode = 200
        mockHttpClient.responseBody = getErrorJson().toString()
        assertTrue(networkManager.defineTemplates(appCtx, emptyList()))
    }

    private fun provideNetworkManager(): NetworkManager {
        val metaData = CoreMetaData()
        val deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, "clevertapId", metaData)
        val coreState = MockCoreState(cleverTapInstanceConfig)
        val callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        val lockManager = CTLockManager()
        val dbManager = DBManager(cleverTapInstanceConfig, lockManager)
        val controllerManager =
            ControllerManager(appCtx, cleverTapInstanceConfig, lockManager, callbackManager, deviceInfo, dbManager)
        val triggersManager = TriggerManager(appCtx, cleverTapInstanceConfig.accountId, deviceInfo)
        val inAppResponse =
            InAppResponse(
                cleverTapInstanceConfig,
                controllerManager,
                true,
                coreState.storeRegistry,
                triggersManager,
                mockk<TemplatesManager>(),
                metaData
            )
        val ijRepo = IJRepo(cleverTapInstanceConfig)
        val arpRepo = ArpRepo(
            accountId = cleverTapInstanceConfig.accountId,
            logger = cleverTapInstanceConfig.logger,
            deviceInfo = deviceInfo
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
            ctApiWrapper,
            networkEncryptionManager,
            ijRepo,
            arpRepo
        )
    }

    private fun getErrorJson(): JSONObject {
        return JSONObject().apply {
            put("error", "Error")
        }
    }
}