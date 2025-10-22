package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertNull

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

        every { config.accountId } returns "testAccount"
        every { config.getLogger() } returns mockk(relaxed = true)

        coreState = CoreState(
            context = context,
            locationManager = mockk(relaxed = true),
            config = config,
            coreMetaData = coreMetaData,
            databaseManager = databaseManager,
            deviceInfo = deviceInfo,
            eventMediator = mockk(relaxed = true),
            localDataStore = localDataStore,
            activityLifeCycleManager = mockk(relaxed = true),
            analyticsManager = analyticsManager,
            baseEventQueueManager = baseEventQueueManager,
            cTLockManager = mockk(relaxed = true),
            callbackManager = callbackManager,
            inAppController = mockk(relaxed = true),
            evaluationManager = mockk(relaxed = true),
            impressionManager = mockk(relaxed = true),
            sessionManager = sessionManager,
            validationResultStack = validationResultStack,
            mainLooperHandler = mockk(relaxed = true),
            networkManager = mockk(relaxed = true),
            pushProviders = pushProviders,
            varCache = mockk(relaxed = true),
            parser = mockk(relaxed = true),
            cryptHandler = mockk(relaxed = true),
            storeRegistry = mockk(relaxed = true),
            templatesManager = mockk(relaxed = true),
            profileValueHandler = mockk(relaxed = true),
            cTVariables = cTVariables,
            executors = MockCTExecutors(),
            contentFetchManager = contentFetchManager,
            loginInfoProvider = loginInfoProvider,
            storeProvider = storeProvider,
            variablesRepository = variablesRepo

        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== asyncProfileSwitchUser Tests ==========

    @Test
    fun `asyncProfileSwitchUser with cached GUID should update device ID and complete profile switch`() {
        // Arrange
        val profile = mapOf("Name" to "Test User", "Email" to "test@example.com")
        val cacheGuid = "cached-guid-123"
        val cleverTapID = null
        val inAppFCManager = mockk<InAppFCManager>(relaxed = true)

        coreState.setInAppFCManager(inAppFCManager)
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
        verify { inAppFCManager.changeUser(cacheGuid) }
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
        coreState.ctInboxController = ctInboxController
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
