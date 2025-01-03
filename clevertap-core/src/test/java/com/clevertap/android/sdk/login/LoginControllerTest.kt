import android.content.Context
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.SessionManager
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DBManager
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventGroup
import com.clevertap.android.sdk.login.ChangeUserCallback
import com.clevertap.android.sdk.login.LoginController
import com.clevertap.android.sdk.login.LoginInfoProvider
import com.clevertap.android.sdk.pushnotification.PushProviders
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CountDownLatch
import kotlin.test.assertTrue

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
    private lateinit var loginInfoProvider: LoginInfoProvider

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

        every { controllerManager.pushProviders } returns pushProviders

        loginInfoProvider = LoginInfoProvider(
            context, config
        )
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
            loginInfoProvider
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

    @Ignore("Run this test locally")
    @Test
    fun `test notifyChangeUserCallback`() {
        // Arrange
        val cm = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        val expectedChangeUserCallbackList = ArrayList<ChangeUserCallback>()

        (1..10).forEach { _ ->
            expectedChangeUserCallbackList.add(mockk<ChangeUserCallback> {
                every { onChangeUser(any(), any()) } just Runs
            })
        }
        expectedChangeUserCallbackList.forEach {
            cm.addChangeUserCallback(it)
        }
        val loginController = LoginController(
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
            cm,
            dbManager,
            ctLockManager,
            loginInfoProvider
        )
        //Act
        loginController.notifyChangeUserCallback()
        //Assert
        expectedChangeUserCallbackList.forEach {
            verifyOrder {
                it.onChangeUser(any(), any())
            }
        }
    }

    @Ignore("Run this test locally")
    @Test
    fun `test notifyChangeUserCallback thread safety`() {
        // Arrange
        val cm = CallbackManager(cleverTapInstanceConfig, deviceInfo)
        val mockChangeUserCallback = mockk<ChangeUserCallback> {
            every { onChangeUser(any(), any()) } just Runs
        }
        val loginController = LoginController(
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
            cm,
            dbManager,
            ctLockManager,
            loginInfoProvider
        )

        var isPassed = true
        var ex: Exception? = null
        // CountDownLatch to wait for all threads to finish
        val latch = CountDownLatch(50)

        //Act
        (1..50).forEach { _ ->
            Thread {
                try {
                    (1..100000).forEach { _ ->
                        cm.addChangeUserCallback(mockChangeUserCallback)
                    }

                } catch (e: ArrayIndexOutOfBoundsException) {
                    ex = e
                    isPassed = false
                } finally {
                    latch.countDown()
                }
            }.start()
        }
        Thread {
            try {
                loginController.notifyChangeUserCallback()
            } catch (e: ConcurrentModificationException) {
                ex = e
                isPassed = false
            }
        }.start()
        latch.await()

        //Assert
        assertTrue(isPassed, "Exceptions must not be thrown but this is thrown $ex")
    }

}
