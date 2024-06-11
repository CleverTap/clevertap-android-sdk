import android.content.Context
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.login.LoginController
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.mockito.*

class LoginControllerTest : BaseTestCase() {

    private lateinit var context: Context
    private lateinit var config: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var validationResultStack: ValidationResultStack
    private lateinit var baseEventQueueManager: BaseEventQueueManager
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var controllerManager: ControllerManager
    private lateinit var sessionManager: SessionManager
    private lateinit var localDataStore: LocalDataStore
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var dbManager: DBManager
    private lateinit var ctLockManager: CTLockManager
    private lateinit var cryptHandler: CryptHandler
    private lateinit var pushProviders: PushProviders
    private lateinit var loginController: LoginController

    @Before
    override fun setUp() {
        super.setUp()
        context = mockk(relaxed = true)
        config = mockk(relaxed = true)
        deviceInfo = mockk(relaxed = true)
        validationResultStack = mockk(relaxed = true)
        baseEventQueueManager = mockk(relaxed = true)
        analyticsManager = mockk(relaxed = true)
        coreMetaData = mockk(relaxed = true)
        controllerManager = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)
        localDataStore = mockk(relaxed = true)
        callbackManager = mockk(relaxed = true)
        dbManager = mockk(relaxed = true)
        ctLockManager = mockk(relaxed = true)
        cryptHandler = mockk(relaxed = true)
        pushProviders = mockk(relaxed = true)

        every { controllerManager.getPushProviders() } returns pushProviders

        loginController = LoginController(
            context,
            config,
            deviceInfo,
            validationResultStack,
            baseEventQueueManager,
            analyticsManager,
            coreMetaData,
            controllerManager,
            sessionManager,
            localDataStore,
            callbackManager,
            dbManager,
            ctLockManager,
            cryptHandler
        )
    }

    @Test
    fun `asyncProfileSwitchUser should reset profile and update data`() {
        val profile = mapOf("Name" to "John Doe", "Email" to "john.doe@example.com")
        val cacheGuid = "12345"
        val cleverTapID = "54321"


        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            loginController.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)
        }


        verifyOrder {
            coreMetaData.isCurrentUserOptedOut = false
            pushProviders.forcePushDeviceToken(false)
            baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR)
            baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            dbManager.clearQueues(context)
            CoreMetaData.setActivityCount(1)
            sessionManager.destroySession()
            deviceInfo.forceUpdateDeviceId(cacheGuid)
            callbackManager.notifyUserProfileInitialized(cacheGuid)
            localDataStore.changeUser()
            deviceInfo.setCurrentUserOptOutStateFromStorage()
            analyticsManager.forcePushAppLaunchedEvent()
            analyticsManager.pushProfile(profile)
            pushProviders.forcePushDeviceToken(true)
            controllerManager.inAppFCManager.changeUser(any())
        }
    }

    @Test
    fun `asyncProfileSwitchUser when null cacheGuid`() {
        val profile = mapOf("Name" to "John Doe", "Email" to "john.doe@example.com")
        val cacheGuid = null
        val cleverTapID = "54321"

        every { config.enableCustomCleverTapId } returns false
        every { deviceInfo.forceNewDeviceID() } just Runs
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            loginController.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)
        }

        verifyOrder {
            coreMetaData.isCurrentUserOptedOut = false
            pushProviders.forcePushDeviceToken(false)
            baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR)
            baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            dbManager.clearQueues(context)
            CoreMetaData.setActivityCount(1)
            sessionManager.destroySession()
            deviceInfo.forceNewDeviceID()
            localDataStore.changeUser()
            callbackManager.notifyUserProfileInitialized(any())
            deviceInfo.setCurrentUserOptOutStateFromStorage()
            analyticsManager.forcePushAppLaunchedEvent()
            analyticsManager.pushProfile(profile)
            pushProviders.forcePushDeviceToken(true)
            controllerManager.inAppFCManager.changeUser(any())
        }
    }

    @Test
    fun `asyncProfileSwitchUser when null profile`() {
        val profile: Map<String, Any>? = null
        val cacheGuid = "12345"
        val cleverTapID = "54321"

        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            loginController.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)
        }


        verifyOrder {
            coreMetaData.setCurrentUserOptedOut(false)
            pushProviders.forcePushDeviceToken(false)
            baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR)
            baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            dbManager.clearQueues(context)
            CoreMetaData.setActivityCount(1)
            sessionManager.destroySession()
            deviceInfo.forceUpdateDeviceId(cacheGuid)
            callbackManager.notifyUserProfileInitialized(cacheGuid)
            localDataStore.changeUser()
            deviceInfo.setCurrentUserOptOutStateFromStorage()
            analyticsManager.forcePushAppLaunchedEvent()
            pushProviders.forcePushDeviceToken(true)
            controllerManager.inAppFCManager.changeUser(any())
        }
        verify(exactly = 0) { analyticsManager.pushProfile(any()) }
    }

    @Test
    fun `asyncProfileSwitchUser when null cachedGuid and custom ID is enabled`() {
        val profile = mapOf("Name" to "John Doe", "Email" to "john.doe@example.com")
        val cacheGuid = null
        val cleverTapID = "1234"

        every { config.enableCustomCleverTapId } returns true
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.executors(Mockito.any())).thenReturn(
                MockCTExecutors(cleverTapInstanceConfig)
            )
            loginController.asyncProfileSwitchUser(profile, cacheGuid, cleverTapID)
        }

        verifyOrder {
            coreMetaData.isCurrentUserOptedOut = false
            pushProviders.forcePushDeviceToken(false)
            baseEventQueueManager.flushQueueSync(context, EventGroup.REGULAR)
            baseEventQueueManager.flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED)
            dbManager.clearQueues(context)
            CoreMetaData.setActivityCount(1)
            sessionManager.destroySession()
            deviceInfo.forceUpdateCustomCleverTapID(cleverTapID)
            localDataStore.changeUser()
            callbackManager.notifyUserProfileInitialized(any())
            deviceInfo.setCurrentUserOptOutStateFromStorage()
            analyticsManager.forcePushAppLaunchedEvent()
            analyticsManager.pushProfile(profile)
            pushProviders.forcePushDeviceToken(true)
            controllerManager.inAppFCManager.changeUser(any())
        }
    }
}