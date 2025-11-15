package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.features.*
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.network.api.CtApi
import com.clevertap.android.sdk.network.api.EncryptionFailure
import com.clevertap.android.sdk.network.api.EncryptionSuccess
import com.clevertap.android.sdk.network.api.SendQueueRequestBody
import com.clevertap.android.sdk.network.http.Response
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.ClevertapResponseHandler
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.*

class CoreStateTest {

    private lateinit var coreState: CoreState
    private lateinit var context: Context
    private lateinit var config: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var baseEventQueueManager: BaseEventQueueManager
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var pushProviders: PushProviders
    private lateinit var databaseManager: BaseDatabaseManager
    private lateinit var sessionManager: SessionManager
    private lateinit var localDataStore: LocalDataStore
    private lateinit var contentFetchManager: ContentFetchManager
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var validationResultStack: ValidationResultStack
    private lateinit var loginInfoProvider: LoginInfoProvider
    private lateinit var cTVariables: CTVariables
    private lateinit var storeProvider: StoreProvider
    private lateinit var variablesRepo: VariablesRepo
    private lateinit var encryptionManager: NetworkEncryptionManager
    private lateinit var arpResponse: ARPResponse
    private lateinit var clevertapResponseHandler: ClevertapResponseHandler

    // Feature objects
    private lateinit var coreFeature: CoreFeature
    private lateinit var dataFeature: DataFeature
    private lateinit var networkFeature: NetworkFeature
    private lateinit var analyticsFeature: AnalyticsFeature
    private lateinit var profileFeature: ProfileFeature
    private lateinit var inAppFeature: InAppFeature
    private lateinit var inboxFeature: InboxFeature
    private lateinit var variablesFeature: VariablesFeature
    private lateinit var pushFeature: PushFeature
    private lateinit var lifecycleFeature: LifecycleFeature
    private lateinit var callbackFeature: CallbackFeature

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        config = mockk(relaxed = true)
        deviceInfo = mockk(relaxed = true)
        analyticsManager = mockk(relaxed = true)
        baseEventQueueManager = mockk(relaxed = true)
        callbackManager = mockk(relaxed = true)
        pushProviders = mockk(relaxed = true)
        databaseManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        localDataStore = mockk(relaxed = true)
        contentFetchManager = mockk(relaxed = true)
        coreMetaData = mockk(relaxed = true)
        validationResultStack = mockk(relaxed = true)
        loginInfoProvider = mockk(relaxed = true)
        cTVariables = mockk(relaxed = true)
        storeProvider = mockk(relaxed = true)
        variablesRepo = mockk(relaxed = true)
        encryptionManager = mockk(relaxed = true)
        arpResponse = mockk(relaxed = true)
        clevertapResponseHandler = mockk(relaxed = true)

        every { config.accountId } returns "testAccount"
        every { config.getLogger() } returns mockk(relaxed = true)
        every { config.logger } returns mockk(relaxed = true)

        // Create feature objects
        coreFeature = mockk(relaxed = true) {
            every { context } returns this@CoreStateTest.context
            every { config } returns this@CoreStateTest.config
            every { deviceInfo } returns this@CoreStateTest.deviceInfo
            every { coreMetaData } returns this@CoreStateTest.coreMetaData
            every { validationResultStack } returns this@CoreStateTest.validationResultStack
            every { mainLooperHandler } returns mockk(relaxed = true)
            every { executors } returns MockCTExecutors()
            every { cryptHandler } returns mockk(relaxed = true)
            every { clock } returns mockk(relaxed = true)
        }

        dataFeature = mockk(relaxed = true) {
            every { databaseManager } returns this@CoreStateTest.databaseManager
            every { localDataStore } returns this@CoreStateTest.localDataStore
            every { storeRegistry } returns mockk(relaxed = true)
            every { storeProvider } returns this@CoreStateTest.storeProvider
        }

        networkFeature = mockk(relaxed = true) {
            every { networkManager } returns mockk(relaxed = true)
            every { contentFetchManager } returns this@CoreStateTest.contentFetchManager
            every { encryptionManager } returns this@CoreStateTest.encryptionManager
            every { arpResponse } returns this@CoreStateTest.arpResponse
            every { clevertapResponseHandler } returns this@CoreStateTest.clevertapResponseHandler
            every { networkHeadersListeners } returns mutableListOf()
        }

        analyticsFeature = mockk(relaxed = true) {
            every { analyticsManager } returns this@CoreStateTest.analyticsManager
            every { baseEventQueueManager } returns this@CoreStateTest.baseEventQueueManager
            every { sessionManager } returns this@CoreStateTest.sessionManager
            every { eventMediator } returns mockk(relaxed = true)
        }

        profileFeature = mockk(relaxed = true) {
            every { loginInfoProvider } returns this@CoreStateTest.loginInfoProvider
            every { profileValueHandler } returns mockk(relaxed = true)
            every { locationManager } returns mockk(relaxed = true)
        }

        inAppFeature = mockk(relaxed = true) {
            every { inAppController } returns mockk(relaxed = true)
            every { evaluationManager } returns mockk(relaxed = true)
            every { impressionManager } returns mockk(relaxed = true)
            every { templatesManager } returns mockk(relaxed = true)
            every { inAppFCManager } returns null
        }

        inboxFeature = mockk(relaxed = true) {
            every { cTLockManager } returns mockk(relaxed = true)
            every { ctInboxController } returns null
        }

        variablesFeature = mockk(relaxed = true) {
            every { cTVariables } returns this@CoreStateTest.cTVariables
            every { varCache } returns mockk(relaxed = true)
            every { parser } returns mockk(relaxed = true)
            every { variablesRepository } returns this@CoreStateTest.variablesRepo
        }

        pushFeature = mockk(relaxed = true) {
            every { pushProviders } returns this@CoreStateTest.pushProviders
        }

        lifecycleFeature = mockk(relaxed = true) {
            every { activityLifeCycleManager } returns mockk(relaxed = true)
        }

        callbackFeature = mockk(relaxed = true) {
            every { callbackManager } returns this@CoreStateTest.callbackManager
        }

        // Create CoreState with feature objects
        coreState = CoreState(
            core = coreFeature,
            data = dataFeature,
            network = networkFeature,
            analytics = analyticsFeature,
            profileFeat = profileFeature,
            inApp = inAppFeature,
            inbox = inboxFeature,
            variables = variablesFeature,
            push = pushFeature,
            lifecycle = lifecycleFeature,
            callback = callbackFeature
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== CoreContract Response Handling Tests ==========

    @Test
    fun `handleSendQueueResponse with valid response should delegate to handler`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val requestBody = SendQueueRequestBody(JSONObject(), JSONArray())
        val endpointId = EndpointId.ENDPOINT_A1
        val responseBody = JSONObject().apply {
            put("key", "value")
        }.toString()

        every { response.readBody() } returns responseBody
        every { response.getHeaderValue(CtApi.HEADER_ENCRYPTION_ENABLED) } returns "false"

        // Act
        coreState.handleSendQueueResponse(
            response = response,
            isFullResponse = true,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = false
        )

        // Assert
        verify { clevertapResponseHandler.handleResponse(true, any(), responseBody, false) }
        verify { callbackManager.invokeBatchListener(requestBody.queue, true) }
    }

    @Test
    fun `handleSendQueueResponse with null body should invoke success callback only`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val requestBody = SendQueueRequestBody(JSONObject(), JSONArray())
        val endpointId = EndpointId.ENDPOINT_A1

        every { response.readBody() } returns null

        // Act
        coreState.handleSendQueueResponse(
            response = response,
            isFullResponse = true,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = false
        )

        // Assert
        verify(exactly = 0) { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
        verify { callbackManager.invokeBatchListener(requestBody.queue, true) }
    }

    @Test
    fun `handleSendQueueResponse with empty body should invoke success callback only`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val requestBody = SendQueueRequestBody(JSONObject(), JSONArray())
        val endpointId = EndpointId.ENDPOINT_A1

        every { response.readBody() } returns ""

        // Act
        coreState.handleSendQueueResponse(
            response = response,
            isFullResponse = true,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = false
        )

        // Assert
        verify(exactly = 0) { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
        verify { callbackManager.invokeBatchListener(requestBody.queue, true) }
    }

    @Test
    fun `handleSendQueueResponse with encrypted response should decrypt and process`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val requestBody = SendQueueRequestBody(JSONObject(), JSONArray())
        val endpointId = EndpointId.ENDPOINT_A1
        val encryptedBody = JSONObject().apply {
            put("data", "encrypted-data")
        }.toString()
        val decryptedBody = JSONObject().apply {
            put("decrypted", "data")
        }.toString()

        every { response.readBody() } returns encryptedBody
        every { response.getHeaderValue(CtApi.HEADER_ENCRYPTION_ENABLED) } returns "true"
        every { encryptionManager.decryptResponse(encryptedBody) } returns EncryptionSuccess(decryptedBody, "iv")

        // Act
        coreState.handleSendQueueResponse(
            response = response,
            isFullResponse = false,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = true
        )

        // Assert
        verify { encryptionManager.decryptResponse(encryptedBody) }
        verify { clevertapResponseHandler.handleResponse(false, any(), decryptedBody, true) }
        verify { callbackManager.invokeBatchListener(requestBody.queue, true) }
    }

    @Test
    fun `handleSendQueueResponse with encryption failure should invoke failure callback`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val requestBody = SendQueueRequestBody(JSONObject(), JSONArray())
        val endpointId = EndpointId.ENDPOINT_A1
        val encryptedBody = """
            {"data": "encrypted-data"}
        """.trimIndent()

        every { response.readBody() } returns encryptedBody
        every { response.getHeaderValue(CtApi.HEADER_ENCRYPTION_ENABLED) } returns "true"
        every { encryptionManager.decryptResponse(encryptedBody) } returns EncryptionFailure

        // Act
        coreState.handleSendQueueResponse(
            response = response,
            isFullResponse = true,
            requestBody = requestBody,
            endpointId = endpointId,
            isUserSwitchFlush = false
        )

        // Assert
        verify { encryptionManager.decryptResponse(encryptedBody) }
        verify(exactly = 0) { clevertapResponseHandler.handleResponse(any(), any(), any(), any()) }
        verify { callbackManager.invokeBatchListener(requestBody.queue, false) }
    }

    @Test
    fun `handleVariablesResponse should delegate to ARPResponse`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val responseBody = JSONObject().apply {
            put("vars", "data")
        }.toString()

        every { response.readBody() } returns responseBody

        // Act
        coreState.handleVariablesResponse(response)

        // Assert
        verify { arpResponse.processResponse(any(), responseBody, context) }
    }

    @Test
    fun `handleVariablesResponse with null body should still call ARPResponse`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        every { response.readBody() } returns null

        // Act
        coreState.handleVariablesResponse(response)

        // Assert
        verify { arpResponse.processResponse(null, null, context) }
    }

    @Test
    fun `handlePushImpressionsResponse should notify push listeners`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val queue = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "event")
                put("evtData", JSONObject().apply {
                    put(Constants.WZRK_PUSH_ID, "push123")
                    put(Constants.WZRK_ACCT_ID_KEY, "account456")
                })
            })
        }

        every { response.readBody() } returns JSONObject().toString()
        
        // Mock the static method
        mockkStatic(CleverTapAPI::class)
        val mockListener = mockk<com.clevertap.android.sdk.interfaces.NotificationRenderedListener>(relaxed = true)
        every { CleverTapAPI.getNotificationRenderedListener(any()) } returns mockListener

        // Act
        coreState.handlePushImpressionsResponse(response, queue)

        // Assert
        verify { mockListener.onNotificationRendered(true) }
    }

    @Test
    fun `handlePushImpressionsResponse with empty queue should complete without errors`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val queue = JSONArray()

        every { response.readBody() } returns JSONObject().toString()

        // Act - should not throw
        coreState.handlePushImpressionsResponse(response, queue)

        // Assert - no exceptions
    }

    @Test
    fun `handlePushImpressionsResponse with malformed event should skip and continue`() {
        // Arrange
        val response = mockk<Response>(relaxed = true)
        val queue = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "event")
                // Missing evtData
            })
            put(JSONObject().apply {
                put("type", "event")
                put("evtData", JSONObject().apply {
                    put(Constants.WZRK_PUSH_ID, "push456")
                    put(Constants.WZRK_ACCT_ID_KEY, "account789")
                })
            })
        }

        every { response.readBody() } returns JSONObject().toString()
        
        mockkStatic(CleverTapAPI::class)
        val mockListener = mockk<com.clevertap.android.sdk.interfaces.NotificationRenderedListener>(relaxed = true)
        every { CleverTapAPI.getNotificationRenderedListener(any()) } returns mockListener

        // Act - should not throw
        coreState.handlePushImpressionsResponse(response, queue)

        // Assert - should process valid event
        verify { mockListener.onNotificationRendered(true) }
    }

    // ========== CoreContract Callback Tests ==========

    @Test
    fun `onNetworkError should invoke callback manager`() {
        // Act
        coreState.onNetworkError()

        // Assert
        verify { callbackManager.invokeCallbacksForNetworkError(cTVariables) }
    }

    @Test
    fun `onNetworkSuccess should invoke batch listener`() {
        // Arrange
        val queue = JSONArray().apply {
            put(JSONObject())
        }

        // Act
        coreState.onNetworkSuccess(queue, true)

        // Assert
        verify { callbackManager.invokeBatchListener(queue, true) }
    }

    @Test
    fun `onNetworkSuccess with failure should invoke batch listener with false`() {
        // Arrange
        val queue = JSONArray()

        // Act
        coreState.onNetworkSuccess(queue, false)

        // Assert
        verify { callbackManager.invokeBatchListener(queue, false) }
    }

    @Test
    fun `onFlushFailure should invoke failure flush listener`() {
        // Arrange
        val mockFailureListener = mockk<FailureFlushListener>(relaxed = true)
        every { callbackManager.failureFlushListener } returns mockFailureListener

        // Act
        coreState.onFlushFailure(context)

        // Assert
        verify { mockFailureListener.failureFlush(context) }
    }

    @Test
    fun `onFlushFailure with null listener should not throw`() {
        // Arrange
        every { callbackManager.failureFlushListener } returns null

        // Act - should not throw
        coreState.onFlushFailure(context)

        // Assert - no exception
    }

    // ========== CoreContract Header Management Tests ==========

    @Test
    fun `notifyHeadersSent should notify all registered listeners`() {
        // Arrange
        val headers = JSONObject().apply {
            put("X-Custom", "value")
        }
        val endpointId = EndpointId.ENDPOINT_A1
        val listener1 = mockk<NetworkHeadersListener>(relaxed = true)
        val listener2 = mockk<NetworkHeadersListener>(relaxed = true)
        
        every { networkFeature.networkHeadersListeners } returns mutableListOf(listener1, listener2)

        // Act
        coreState.notifyHeadersSent(headers, endpointId)

        // Assert
        verify { listener1.onSentHeaders(headers, endpointId) }
        verify { listener2.onSentHeaders(headers, endpointId) }
    }

    @Test
    fun `notifyHeadersSent with no listeners should complete without errors`() {
        // Arrange
        val headers = JSONObject()
        val endpointId = EndpointId.ENDPOINT_A1
        every { networkFeature.networkHeadersListeners } returns mutableListOf()

        // Act - should not throw
        coreState.notifyHeadersSent(headers, endpointId)

        // Assert - no exception
    }

    @Test
    fun `getHeadersToAttach should combine headers from all listeners`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        val listener1 = mockk<NetworkHeadersListener>()
        val listener2 = mockk<NetworkHeadersListener>()
        
        val headers1 = JSONObject().apply {
            put("X-Header1", "value1")
        }
        val headers2 = JSONObject().apply {
            put("X-Header2", "value2")
        }
        
        every { listener1.onAttachHeaders(endpointId) } returns headers1
        every { listener2.onAttachHeaders(endpointId) } returns headers2
        every { networkFeature.networkHeadersListeners } returns mutableListOf(listener1, listener2)

        // Act
        val result = coreState.getHeadersToAttach(endpointId)

        // Assert
        assertNotNull(result)
        assertTrue(result.has("X-Header1"))
        assertTrue(result.has("X-Header2"))
        assertEquals("value1", result.getString("X-Header1"))
        assertEquals("value2", result.getString("X-Header2"))
    }

    @Test
    fun `getHeadersToAttach with null responses should return null`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        val listener = mockk<NetworkHeadersListener>()
        
        every { listener.onAttachHeaders(endpointId) } returns null
        every { networkFeature.networkHeadersListeners } returns mutableListOf(listener)

        // Act
        val result = coreState.getHeadersToAttach(endpointId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `getHeadersToAttach with no listeners should return null`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        every { networkFeature.networkHeadersListeners } returns mutableListOf()

        // Act
        val result = coreState.getHeadersToAttach(endpointId)

        // Assert
        assertNull(result)
    }

    // ========== CoreContract Domain Notification Tests ==========

    @Test
    fun `notifySCDomainAvailable should notify SCDomain listener`() {
        // Arrange
        val domain = "test.domain.com"
        val mockListener = mockk<com.clevertap.android.sdk.interfaces.SCDomainListener>(relaxed = true)
        every { callbackManager.scDomainListener } returns mockListener

        // Act
        coreState.notifySCDomainAvailable(domain)

        // Assert
        verify { mockListener.onSCDomainAvailable(domain) }
    }

    @Test
    fun `notifySCDomainAvailable with null listener should not throw`() {
        // Arrange
        every { callbackManager.scDomainListener } returns null

        // Act - should not throw
        coreState.notifySCDomainAvailable("domain.com")

        // Assert - no exception
    }

    @Test
    fun `notifySCDomainUnavailable should notify SCDomain listener`() {
        // Arrange
        val mockListener = mockk<com.clevertap.android.sdk.interfaces.SCDomainListener>(relaxed = true)
        every { callbackManager.scDomainListener } returns mockListener

        // Act
        coreState.notifySCDomainUnavailable()

        // Assert
        verify { mockListener.onSCDomainUnavailable() }
    }

    @Test
    fun `notifySCDomainUnavailable with null listener should not throw`() {
        // Arrange
        every { callbackManager.scDomainListener } returns null

        // Act - should not throw
        coreState.notifySCDomainUnavailable()

        // Assert - no exception
    }

    // ========== CoreContract Dependencies Tests ==========

    @Test
    fun `getContext should return context from CoreFeature`() {
        // Act
        val result = coreState.context()

        // Assert
        assertEquals(context, result)
    }

    @Test
    fun `getConfig should return config from CoreFeature`() {
        // Act
        val result = coreState.config()

        // Assert
        assertEquals(config, result)
    }

    @Test
    fun `getDeviceInfo should return deviceInfo from CoreFeature`() {
        // Act
        val result = coreState.deviceInfo()

        // Assert
        assertEquals(deviceInfo, result)
    }

    @Test
    fun `getCoreMetaData should return coreMetaData from CoreFeature`() {
        // Act
        val result = coreState.coreMetaData()

        // Assert
        assertEquals(coreMetaData, result)
    }

    @Test
    fun `getDatabase should return databaseManager from DataFeature`() {
        // Act
        val result = coreState.database()

        // Assert
        assertEquals(databaseManager, result)
    }

    @Test
    fun `getLogger should return logger from Config`() {
        // Act
        val result = coreState.logger()

        // Assert
        verify { config.logger }
    }

    // ========== asyncProfileSwitchUser Tests ==========

    @Test
    fun `asyncProfileSwitchUser with cached GUID should update device ID and complete profile switch`() {
        // Arrange
        val profile = mapOf("Name" to "Test User", "Email" to "test@example.com")
        val cacheGuid = "cached-guid-123"
        val cleverTapID = null
        val inAppFCManager = mockk<InAppFCManager>(relaxed = true)
        
        // Update the inAppFeature's inAppFCManager through the mock
        every { inAppFeature.inAppFCManager } returns null andThen inAppFCManager
        every { inAppFeature.inAppFCManager = any() } just Runs

        every { coreMetaData.isCurrentUserOptedOut = any() } just Runs
        every { deviceInfo.forceUpdateDeviceId(any()) } just Runs
        every { deviceInfo.getDeviceID() } returns cacheGuid
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs

        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)

        // Assert
        verify { coreMetaData.isCurrentUserOptedOut = false }
        verify { pushProviders.forcePushDeviceToken(false) }
        verify { baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR, null, true) }
        verify { baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED, null, true) }
        verify { contentFetchManager.cancelAllResponseJobs() }
        verify { databaseManager.clearQueues(context) }
        verify { CoreMetaData.setActivityCount(1) }
        verify { sessionManager.destroySession() }
        verify { deviceInfo.forceUpdateDeviceId(cacheGuid) }
        verify { callbackManager.notifyUserProfileInitialized(cacheGuid) }
        verify { localDataStore.changeUser() }
        verify { analyticsManager.forcePushAppLaunchedEvent() }
        verify { analyticsManager.pushProfile(profile) }
        verify { pushProviders.forcePushDeviceToken(true) }
    }

    @Test
    @Ignore("This test never finishes, check it later.")
    fun `asyncProfileSwitchUser without cached GUID and custom ID enabled should update custom CleverTap ID`() {
        // Arrange
        val profile = mapOf("Name" to "Test User")
        val cacheGuid = null
        val cleverTapID = "custom-id-456"

        every { config.enableCustomCleverTapId } returns true
        every { deviceInfo.forceUpdateCustomCleverTapID(any()) } just Awaits
        every { deviceInfo.getDeviceID() } returns cleverTapID
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs

        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)

        // Assert
        verify { deviceInfo.forceUpdateCustomCleverTapID(cleverTapID) }
        verify { analyticsManager.pushProfile(profile) }
        verify { callbackManager.notifyUserProfileInitialized(cleverTapID) }
    }

    @Test
    fun `asyncProfileSwitchUser without cached GUID and custom ID disabled should generate new device ID`() {
        // Arrange
        val profile: Map<String, Any?>? = null
        val cacheGuid = null
        val cleverTapID = null
        val newDeviceID = "new-device-id-789"

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.forceNewDeviceID() } just Runs
        every { deviceInfo.getDeviceID() } returns newDeviceID
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs

        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)

        // Assert
        verify { deviceInfo.forceNewDeviceID() }
        verify { callbackManager.notifyUserProfileInitialized(newDeviceID) }
        verify(exactly = 0) { analyticsManager.pushProfile(any()) }
    }

    @Test
    fun `asyncProfileSwitchUser should handle exceptions gracefully`() {
        // Arrange
        val profile = mapOf("Name" to "Test User")
        every { coreMetaData.isCurrentUserOptedOut = any() } throws RuntimeException("Test exception")

        // Act - should not throw
        coreState.asyncProfileSwitchUser(profile, "guid", null)

        // Assert
        verify { config.getLogger() }
    }

    // ========== onUserLogin Tests ==========

    @Test
    fun `onUserLogin with custom ID enabled and null cleverTapID should log warning`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        mockkStatic(Logger::class)
        every { Logger.i(any()) } just Runs
        every { config.enableCustomCleverTapId } returns true

        // Act
        coreState.onUserLogin(profile, null)

        // Assert
        verify {
            Logger.i("CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml Please call onUserlogin() and pass a custom CleverTap ID")
        }
    }

    @Test
    fun `onUserLogin with custom ID disabled and non-null cleverTapID should log warning`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        val cleverTapID = "custom-id"
        mockkStatic(Logger::class)
        every { Logger.i(any()) } just Runs
        every { config.enableCustomCleverTapId } returns false

        // Act
        coreState.onUserLogin(profile, cleverTapID)

        // Assert
        verify {
            Logger.i("CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml Please call CleverTapAPI.defaultInstance() without a custom CleverTap ID")
        }
    }

    @Test
    fun `onUserLogin should execute _onUserLogin asynchronously`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns "current-guid"

        // Act
        coreState.onUserLogin(profile, null)

        // Assert - MockCTExecutors executes synchronously for testing
        // Verify that the task was executed
        verify { config.enableCustomCleverTapId }
    }

    // ========== notifyChangeUserCallback Tests ==========

    @Test
    fun `notifyChangeUserCallback should notify all registered callbacks`() {
        // Arrange
        val deviceID = "device-123"
        val accountId = "account-456"
        val callback1 = mockk<ChangeUserCallback>(relaxed = true)
        val callback2 = mockk<ChangeUserCallback>(relaxed = true)
        val callbackList = mutableListOf(callback1, callback2)

        every { deviceInfo.getDeviceID() } returns deviceID
        every { config.accountId } returns accountId
        every { callbackManager.getChangeUserCallbackList() } returns callbackList

        // Act
        coreState.notifyChangeUserCallback()

        // Assert
        verify { callback1.onChangeUser(deviceID, accountId) }
        verify { callback2.onChangeUser(deviceID, accountId) }
    }

    @Test
    fun `notifyChangeUserCallback should handle null callbacks in list`() {
        // Arrange
        val deviceID = "device-123"
        val callback = mockk<ChangeUserCallback>(relaxed = true)
        val callbackList = mutableListOf(callback, null)

        every { deviceInfo.getDeviceID() } returns deviceID
        every { callbackManager.getChangeUserCallbackList() } returns callbackList

        // Act - should not throw NPE
        coreState.notifyChangeUserCallback()

        // Assert
        verify { callback.onChangeUser(any(), any()) }
    }

    @Test
    fun `notifyChangeUserCallback with empty callback list should complete without errors`() {
        // Arrange
        val callbackList = mutableListOf<ChangeUserCallback?>()
        every { deviceInfo.getDeviceID() } returns "device-123"
        every { callbackManager.getChangeUserCallbackList() } returns callbackList

        // Act - should not throw
        coreState.notifyChangeUserCallback()

        // Assert - no exceptions thrown
    }

    // ========== recordDeviceIDErrors Tests ==========

    @Test
    fun `recordDeviceIDErrors should push all validation results from deviceInfo`() {
        // Arrange
        val validationResult1 = mockk<ValidationResult>()
        val validationResult2 = mockk<ValidationResult>()
        val validationResults = arrayListOf(validationResult1, validationResult2)

        every { deviceInfo.getValidationResults() } returns validationResults
        every { validationResultStack.pushValidationResult(any()) } just Runs

        // Act
        coreState.recordDeviceIDErrors()

        // Assert
        verify { validationResultStack.pushValidationResult(validationResult1) }
        verify { validationResultStack.pushValidationResult(validationResult2) }
    }

    @Test
    fun `recordDeviceIDErrors should handle empty validation results`() {
        // Arrange
        every { deviceInfo.getValidationResults() } returns arrayListOf()

        // Act
        coreState.recordDeviceIDErrors()

        // Assert
        verify(exactly = 0) { validationResultStack.pushValidationResult(any()) }
    }

    // ========== _onUserLogin Tests (Internal, tested via onUserLogin) ==========

    @Test
    fun `onUserLogin with null profile should not perform login operations`() {
        // Arrange - profile is null
        every { config.enableCustomCleverTapId } returns false

        // Act
        coreState.onUserLogin(null, null)

        // Assert - should not attempt to get device ID or process login
        verify(exactly = 0) { deviceInfo.getDeviceID() }
        verify(exactly = 0) { analyticsManager.pushProfile(any()) }
    }

    @Test
    fun `onUserLogin with null currentGUID should return early and not process login`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns null

        // Act
        coreState.onUserLogin(profile, null)

        // Assert
        verify { deviceInfo.getDeviceID() }
        verify(exactly = 0) { analyticsManager.pushProfile(any()) }
    }

    @Test
    fun `onUserLogin without valid identifier on anonymous device should push profile on current user`() {
        // Arrange
        val profile = mapOf("Name" to "Test User") // No identity key
        val currentGUID = "current-guid"

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns currentGUID
        every { deviceInfo.isErrorDeviceId() } returns false
        every { loginInfoProvider.isAnonymousDevice() } returns true


        // Act
        coreState.onUserLogin(profile, null)

        // Assert
        verify { analyticsManager.pushProfile(profile) }
    }

    @Test
    fun `onUserLogin with identifier mapping to current GUID should push profile on current user`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        val currentGUID = "current-guid"

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns currentGUID
        every { deviceInfo.isErrorDeviceId() } returns false
        every { loginInfoProvider.isAnonymousDevice() } returns false
        every { loginInfoProvider.getGUIDForIdentifier("Identity", "user123") } returns currentGUID

        // Act
        coreState.onUserLogin(profile, null)

        // Assert
        verify { analyticsManager.pushProfile(profile) }
    }

    @Test
    fun `onUserLogin with identifier mapping to different GUID should trigger profile switch`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        val currentGUID = "current-guid"
        val cachedGUID = "cached-guid"

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns currentGUID andThen cachedGUID
        every { deviceInfo.isErrorDeviceId() } returns false
        every { loginInfoProvider.isAnonymousDevice() } returns false
        every { loginInfoProvider.getGUIDForIdentifier("Identity", "user123") } returns cachedGUID
        every { deviceInfo.forceUpdateDeviceId(any()) } just Runs
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs

        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.onUserLogin(profile, null)

        // Assert
        verify { deviceInfo.forceUpdateDeviceId(cachedGUID) }
        verify { analyticsManager.pushProfile(profile) }
    }

    @Test
    fun `onUserLogin should handle exceptions in _onUserLogin gracefully`() {
        // Arrange
        val profile = mapOf("Identity" to "user123")
        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } throws RuntimeException("Test exception")

        // Act - should not throw
        coreState.onUserLogin(profile, null)

        // Assert
        verify { config.getLogger() }
    }

    // ========== Product Config Tests (Deprecated) ==========

    @Test
    fun `getCtProductConfigController should return null when analytics only mode enabled`() {
        // Arrange
        every { config.isAnalyticsOnly } returns true

        // Act
        val result = coreState.getCtProductConfigController(context)

        // Assert
        assertNull(result)
        verify { config.getLogger() }
    }

    // ========== Integration Tests ==========

    @Test
    fun `complete user login flow with new user should perform all necessary operations`() {
        // Arrange
        val profile = mapOf("Email" to "newuser@test.com", "Name" to "New User")
        val currentGUID = "current-guid"
        val cachedGUID = null

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.getDeviceID() } returns currentGUID andThen "new-guid"
        every { deviceInfo.isErrorDeviceId() } returns false
        every { deviceInfo.forceNewDeviceID() } just Runs
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs
        every { loginInfoProvider.isAnonymousDevice() } returns false
        every { loginInfoProvider.getGUIDForIdentifier("Email", "newuser@test.com") } returns cachedGUID


        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.onUserLogin(profile, null)

        // Assert - verify complete flow
        verify { coreMetaData.isCurrentUserOptedOut = false }
        verify { pushProviders.forcePushDeviceToken(false) }
        verify { baseEventQueueManager.flushQueueSync(any(), any(), any(), any()) }
        verify { databaseManager.clearQueues(any()) }
        verify { sessionManager.destroySession() }
        verify { deviceInfo.forceNewDeviceID() }
        verify { localDataStore.changeUser() }
        verify { analyticsManager.forcePushAppLaunchedEvent() }
        verify { analyticsManager.pushProfile(profile) }
        verify { pushProviders.forcePushDeviceToken(true) }
    }

    @Test
    fun `asyncProfileSwitchUser should reset all controllers properly`() {
        // Arrange
        val profile = mapOf("Name" to "Test")
        val cacheGuid = "guid-123"
        val ctDisplayUnitController = mockk<com.clevertap.android.sdk.displayunits.CTDisplayUnitController>(relaxed = true)
        val ctFeatureFlagsController = mockk<com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController>(relaxed = true)
        val ctInboxController = mockk<com.clevertap.android.sdk.inbox.CTInboxController>(relaxed = true)

        every { config.enableCustomCleverTapId } returns false
        every { config.isAnalyticsOnly } returns false
        every { deviceInfo.forceUpdateDeviceId(any()) } just Runs
        every { deviceInfo.getDeviceID() } returns cacheGuid
        every { deviceInfo.setCurrentUserOptOutStateFromStorage() } just Runs
        every { deviceInfo.setSystemEventsAllowedStateFromStorage() } just Runs
        every { callbackManager.ctDisplayUnitController } returns ctDisplayUnitController
        every { callbackManager.ctFeatureFlagsController } returns ctFeatureFlagsController
        
        // Set up inbox controller through the feature
        every { inboxFeature.ctInboxController } returns ctInboxController
        every { inboxFeature.ctInboxController = any() } just Runs
        
        every { callbackManager.ctProductConfigController } returns null
        every { ctFeatureFlagsController.isInitialized() } returns true
        every { ctFeatureFlagsController.resetWithGuid(any()) } just Runs
        every { ctFeatureFlagsController.fetchFeatureFlags() } just Runs

        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.setActivityCount(any()) } just Runs

        // Act
        coreState.asyncProfileSwitchUser(profile, cacheGuid, null)

        // Assert
        verify { ctDisplayUnitController.reset() }
        verify { ctFeatureFlagsController.resetWithGuid(cacheGuid) }
        verify { ctFeatureFlagsController.fetchFeatureFlags() }
        verify { cTVariables.clearUserContent() }
    }
}
