package com.clevertap.android.sdk.inapp

import android.app.Activity
import android.os.Bundle
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.InAppFCManager
import com.clevertap.android.sdk.InAppNotificationActivity
import com.clevertap.android.sdk.InAppNotificationButtonListener
import com.clevertap.android.sdk.InAppNotificationListener
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.inapp.InAppNotificationInflater.InAppNotificationReadyListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.sdk.utils.configMock
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InAppControllerTest {

    private lateinit var mockControllerManager: ControllerManager
    private lateinit var mockInAppFCManager: InAppFCManager
    private lateinit var mockCallbackManager: CallbackManager
    private lateinit var mockInAppListener: InAppNotificationListener
    private lateinit var mockManifestInfo: ManifestInfo
    private lateinit var mockAnalyticsManager: AnalyticsManager
    private lateinit var mockEvaluationManager: EvaluationManager
    private lateinit var mockTemplatesManager: TemplatesManager
    private lateinit var mockConfig: CleverTapInstanceConfig
    private lateinit var mockInAppActionHandler: InAppActionHandler
    private lateinit var mockInAppInflater: InAppNotificationInflater
    private lateinit var fakeInAppQueue: FakeInAppQueue
    private val fakeClock = FakeClock(timeMillis = 1735686000000) // 01.01.2025

    @Before
    fun setUp() {
        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.isAppForeground() } returns true
        every { CoreMetaData.getCurrentActivity() } returns mockk(relaxed = true)

        mockkStatic(InAppNotificationActivity::class)
        every {
            InAppNotificationActivity.launchForInAppNotification(
                any(),
                any(),
                any()
            )
        } just runs

        mockkObject(CTInAppBaseFragment.Companion)
        every { CTInAppBaseFragment.showOnActivity(any(), any(), any(), any(), any()) } returns true

        mockkStatic(NetworkManager::class)
        mockkObject(NetworkManager)
        every { NetworkManager.isNetworkOnline(any()) } returns true

        mockInAppActionHandler = mockk(relaxed = true)

        mockConfig = configMock()
        every { mockConfig.isAnalyticsOnly } returns false

        mockControllerManager = mockk()
        mockInAppFCManager = mockk()
        every { mockInAppFCManager.canShow(any(), any()) } returns true
        every { mockInAppFCManager.didShow(any(), any()) } just runs
        every { mockControllerManager.inAppFCManager } returns mockInAppFCManager

        mockCallbackManager = mockk()

        mockInAppListener = mockk(relaxed = true)
        every { mockInAppListener.beforeShow(any()) } returns true
        every { mockCallbackManager.inAppNotificationListener } returns mockInAppListener

        mockManifestInfo = mockk()
        every { mockManifestInfo.excludedActivities } returns EXCLUDED_ACTIVITY_NAME

        mockAnalyticsManager = mockk()
        every {
            mockAnalyticsManager.pushInAppNotificationStateEvent(
                any(), any(), any()
            )
        } just runs

        mockEvaluationManager = mockk()
        mockTemplatesManager = mockk(relaxed = true)

        mockInAppInflater = mockk()
        every { mockInAppInflater.inflate(any(), any(), any()) } answers {
            (args[2] as InAppNotificationReadyListener).onNotificationReady(
                CTInAppNotification(args[0] as JSONObject, true)
            )
        }
        fakeInAppQueue = FakeInAppQueue()
    }

    @After
    fun cleanUp() {
        unmockkAll()
        InAppController.clearCurrentlyDisplayingInApp()
    }

    @Test
    fun `promptPushPrimer should launch the inAppActionHandler flow`() {
        val inAppController = createInAppController()
        val primerJson = JSONObject().put(CTLocalInApp.FALLBACK_TO_NOTIFICATION_SETTINGS, true)
        inAppController.promptPushPrimer(primerJson)

        verify(exactly = 1) {
            mockInAppActionHandler.launchPushPermissionPrompt(
                fallbackToSettings = true,
                alwaysRequestIfNotGranted = true,
                any()
            )
        }
    }

    @Test
    fun `permissions methods should call inAppActionHandler methods`() {
        every { mockInAppActionHandler.arePushNotificationsEnabled() } returns true
        every { mockInAppActionHandler.launchPushPermissionPrompt(any()) } returns true

        val inAppController = createInAppController()
        assertTrue(inAppController.isPushPermissionGranted())
        verify(exactly = 1) { mockInAppActionHandler.arePushNotificationsEnabled() }

        inAppController.promptPermission(true)
        verify(exactly = 1) { mockInAppActionHandler.launchPushPermissionPrompt(true) }
    }

    @Test
    fun `inAppActionTriggered should raise notification clicked event`() {
        val inAppController = createInAppController()
        val campaignId = "test-campaign"
        val inApp = CTInAppNotification(
            JSONObject(
                """{
            "${Constants.KEY_TYPE}": "${CTInAppType.CTInAppTypeInterstitial}",
            "${Constants.NOTIFICATION_ID_TAG}": "$campaignId"
            }""".trimIndent()
            ), false
        )
        val callToAction = "test-c2a"
        val additionalDataEntry = Pair("testKey", "testData")
        val additionalData = Bundle().apply {
            putString(additionalDataEntry.first, additionalDataEntry.second)
        }
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), callToAction, additionalData, null
        )

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(
                true, inApp, match { data ->
                    data.getString(Constants.NOTIFICATION_ID_TAG) == campaignId && data.getString(
                        Constants.KEY_C2A
                    ) == callToAction && data.getString(additionalDataEntry.first) == additionalDataEntry.second
                })
        }
    }

    @Test
    fun `triggerInAppAction should present visual function actions`() {
        verifyCustomFunctionActionTriggered(isVisual = true)
    }

    @Test
    fun `triggerInAppAction should present non-visual function actions`() {
        verifyCustomFunctionActionTriggered(isVisual = false)
    }

    @Test
    fun `triggerInAppAction should trigger template close on close action`() {
        val testTemplate = template {
            name("test-template")
            actionArgument("action")
            presenter(mockk())
        }
        every { mockTemplatesManager.getTemplate(testTemplate.name) } returns testTemplate
        every { mockTemplatesManager.isTemplateRegistered(testTemplate.name) } returns true

        val inAppJson = getCustomTemplateInAppJson(testTemplate.name)
        val inApp = CTInAppNotification(JSONObject(inAppJson), false)

        val inAppController = createInAppController()
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), "", null, null
        )

        verify(exactly = 1) { mockTemplatesManager.closeTemplate(inApp) }
    }

    @Test
    fun `triggerInAppAction should delegate open url actions to inAppActionHandler`() {
        val url = "https://clevertap.com"
        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": "$url"
        }
        """.trimIndent()
        val inApp = getInAppWithAction(actionJsonString)

        val inAppController = createInAppController()
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createFromJson(JSONObject(actionJsonString))!!, "", null, null
        )

        verify(exactly = 1) { mockInAppActionHandler.openUrl(url, null) }
    }

    @Test
    fun `triggerInAppAction should trigger inAppButtonListener for KV actions`() {
        val mockInAppButtonListener = mockk<InAppNotificationButtonListener>(relaxed = true)
        every { mockCallbackManager.getInAppNotificationButtonListener() } returns mockInAppButtonListener

        val keyValues = hashMapOf(
            "key1" to "value1",
            "key2" to "value2"
        )
        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.KEY_VALUES}",
            "${Constants.KEY_KV}": ${JSONObject(keyValues)}
        }
        """.trimIndent()
        val inApp = getInAppWithAction(actionJsonString)

        val inAppController = createInAppController()
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createFromJson(JSONObject(actionJsonString))!!, "", null, null
        )

        verify(exactly = 1) { mockInAppButtonListener.onInAppButtonClick(keyValues) }
    }

    @Test
    fun `inAppNotificationDidClick should trigger the InAppButton's action`() {
        val url = "https://clevertap.com"
        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": "$url"
        }
        """.trimIndent()
        val inApp = getInAppWithAction(actionJsonString)
        val inAppController = createInAppController()
        inAppController.inAppNotificationDidClick(inApp, inApp.buttons[0], null)

        verify(exactly = 1) { mockInAppActionHandler.openUrl(url, null) }
    }

    @Test
    fun `inAppNotificationDidDismiss should trigger InAppNotificationListener`() {
        val inApp =
            CTInAppNotification(
                JSONObject(InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV),
                false
            )
        val inAppController = createInAppController()
        inAppController.inAppNotificationDidDismiss(inApp, null)

        verify(exactly = 1) { mockInAppListener.onDismissed(any(), any()) }
    }

    @Test
    fun `inAppNotificationDidShow should track NotificationViewed event and trigger InAppNotificationListener`() {
        val inApp =
            CTInAppNotification(
                JSONObject(InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV),
                false
            )
        val inAppController = createInAppController()
        inAppController.inAppNotificationDidShow(inApp, null)

        verify(exactly = 1) { mockInAppFCManager.didShow(any(), inApp) }
        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(
                false,
                inApp,
                null
            )
        }
        verify(exactly = 1) { mockInAppListener.onShow(inApp) }
    }

    @Test
    fun `addInAppNotificationsToQueue should add to queue and display all provided notifications`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")

        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(inApps)

        var currentInApp = InAppController.currentlyDisplayingInApp!!
        assertEquals(
            CTInAppType.CTInAppTypeHalfInterstitial.toString(),
            currentInApp.type
        )
        inAppController.inAppNotificationDidDismiss(currentInApp, null)

        currentInApp = InAppController.currentlyDisplayingInApp!!
        assertEquals(Constants.KEY_CUSTOM_HTML, currentInApp.type)
        inAppController.inAppNotificationDidDismiss(currentInApp, null)

        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `suspendInApps should pause inapps until resumeInApps is called`() {
        val inAppController = createInAppController()
        inAppController.suspendInApps()

        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        inAppController.addInAppNotificationsToQueue(inApps)
        assertNull(InAppController.currentlyDisplayingInApp)

        inAppController.resumeInApps()

        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `discardInApps should drop all in apps until resumeInApps is called`() {
        val inAppController = createInAppController()
        inAppController.discardInApps()

        // TODO verify when multiple inApps are added to the queue only the first is discarded
//        val inApps =
//            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        val inApps = JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        inAppController.addInAppNotificationsToQueue(inApps)
        assertNull(InAppController.currentlyDisplayingInApp)

        inAppController.resumeInApps()
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `onQueueEvent should evaluate and display all matching client-side in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        every { mockEvaluationManager.evaluateOnEvent(any(), any(), any()) } returns inApps

        val inAppController = createInAppController()
        inAppController.onQueueEvent("event", emptyMap<String, Any>(), mockk())
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `onQueueChargedEvent should evaluate and display all matching client-side in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        every { mockEvaluationManager.evaluateOnChargedEvent(any(), any(), any()) } returns inApps

        val inAppController = createInAppController()
        inAppController.onQueueChargedEvent(
            emptyMap<String, Any>(),
            emptyList<Map<String, Any>>(),
            mockk()
        )
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `onQueueProfileEvent should evaluate and display all matching client-side in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        every {
            mockEvaluationManager.evaluateOnUserAttributeChange(
                any(),
                any(),
                any()
            )
        } returns inApps

        val inAppController = createInAppController()
        inAppController.onQueueProfileEvent(
            emptyMap<String, Map<String, Any>>(),
            mockk()
        )
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `onAppLaunchServerSideInAppsResponse should evaluate and display all matching server-side in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        every {
            mockEvaluationManager.evaluateOnAppLaunchedServerSide(
                any(),
                any(),
                any()
            )
        } returns inApps

        val inAppController = createInAppController()
        inAppController.onAppLaunchServerSideInAppsResponse(inApps, mockk())
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `showNotificationIfAvailable should display the next queued in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        fakeInAppQueue.enqueueAll(inApps)
        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            CTInAppType.CTInAppTypeCover.toString()
        )
    }

    @Test
    fun `showNotificationIfAvailable should not display any in-apps in analytics only mode`() {
        every { mockConfig.isAnalyticsOnly } returns true
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        fakeInAppQueue.enqueueAll(inApps)
        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()

        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `showNotificationIfAvailable should not display in-apps rejected by InAppListener`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        //reject inApps without extras (kv)
        every { mockInAppListener.beforeShow(match { extras -> extras.isEmpty() }) } returns false
        fakeInAppQueue.enqueueAll(inApps)

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()

        //verify only the custom html with kv is displayed
        verifyInAppsDisplayed(inAppController, Constants.KEY_CUSTOM_HTML)
    }

    @Test
    fun `showNotificationIfAvailable should not display in-apps while the app is in background`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        fakeInAppQueue.enqueueAll(inApps)

        every { CoreMetaData.isAppForeground() } returns false

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        assertNull(InAppController.currentlyDisplayingInApp)

        // after becoming foreground again, the in-apps should be displayed
        every { CoreMetaData.isAppForeground() } returns true
        inAppController.showNotificationIfAvailable()
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            Constants.KEY_CUSTOM_HTML
        )
    }

    @Test
    fun `showNotificationIfAvailable should not display in-apps rejected by FCManager`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        fakeInAppQueue.enqueueAll(inApps)

        // reject interstitial type
        every {
            mockInAppFCManager.canShow(
                match { inApp -> inApp.type == CTInAppType.CTInAppTypeInterstitial.toString() },
                any()
            )
        } returns false

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()

        //verify only the custom-html in-app is shown
        verifyInAppsDisplayed(inAppController, Constants.KEY_CUSTOM_HTML)
    }

    @Test
    fun `showNotificationIfAvailable should queue in-apps if there is a currently displayed in-app`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        fakeInAppQueue.enqueueAll(inApps)

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        val currentInApp = InAppController.currentlyDisplayingInApp!!
        assertEquals(CTInAppType.CTInAppTypeInterstitial.toString(), currentInApp.type)

        // show again without dismissing, the current in-app should remain the same
        inAppController.showNotificationIfAvailable()
        assertEquals(CTInAppType.CTInAppTypeInterstitial.toString(), currentInApp.type)

        inAppController.inAppNotificationDidDismiss(currentInApp, null)
        //verify the custom-html in-app is shown
        verifyInAppsDisplayed(inAppController, Constants.KEY_CUSTOM_HTML)
    }

    @Test
    fun `showNotificationIfAvailable should not display in-apps on excluded activities`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        fakeInAppQueue.enqueueAll(inApps)

        val mockActivity = mockk<Activity>()
        every { mockActivity.localClassName } returns EXCLUDED_ACTIVITY_NAME
        every { CoreMetaData.getCurrentActivity() } returns mockActivity

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        assertNull(InAppController.currentlyDisplayingInApp)

        // after the activity changes, the in-apps should be displayed
        every { mockActivity.localClassName } returns "NotExcluded"

        inAppController.showNotificationIfAvailable()
        verifyInAppsDisplayed(
            inAppController,
            CTInAppType.CTInAppTypeInterstitial.toString(),
            Constants.KEY_CUSTOM_HTML
        )
    }

    @Test
    fun `showNotificationIfAvailable should drop expired in-apps`() {
        //TODO verify next in-apps will not be shown after a ttl expired in-app
        val inApps = JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        val ttl = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA),
            false
        ).timeToLive
        // set the clock past the ttl of the in-app. ttl is in seconds
        fakeClock.timeMillis = (ttl + 1) * 1000
        fakeInAppQueue.enqueueAll(inApps)

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `showNotificationIfAvailable should drop html in-apps when there is no internet`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV},${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        fakeInAppQueue.enqueueAll(inApps)
        every { NetworkManager.isNetworkOnline(any()) } returns false

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        // only the interstitial in-app should be displayed
        verifyInAppsDisplayed(inAppController, CTInAppType.CTInAppTypeInterstitial.toString())
    }

    @Test
    fun `showNotificationIfAvailable should drop rfp in-apps when permission is already given`() {
        val htmlTypeRfpInApp = JSONObject(InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV)
        htmlTypeRfpInApp.put(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, true)
        val inApps = JSONArray("[$htmlTypeRfpInApp,${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        fakeInAppQueue.enqueueAll(inApps)

        every { mockInAppActionHandler.arePushNotificationsEnabled() } returns true

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        // only the interstitial in-app should be displayed
        verifyInAppsDisplayed(inAppController, CTInAppType.CTInAppTypeInterstitial.toString())

        verify(exactly = 1) { mockInAppActionHandler.notifyPushPermissionListeners() }
    }

    private fun verifyInAppsDisplayed(inAppController: InAppController, vararg inAppTypes: String) {
        var currentInApp: CTInAppNotification
        for (inAppType in inAppTypes) {
            currentInApp = InAppController.currentlyDisplayingInApp!!
            assertEquals(inAppType, currentInApp.type)
            inAppController.inAppNotificationDidDismiss(currentInApp, null)
        }
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    private fun verifyCustomFunctionActionTriggered(isVisual: Boolean) {
        val testTemplate = function(isVisual) {
            name("test-template")
            presenter(mockk())
        }
        every { mockTemplatesManager.getTemplate(testTemplate.name) } returns testTemplate
        every { mockTemplatesManager.isTemplateRegistered(testTemplate.name) } returns true

        val actionJsonString = getCustomTemplateInAppJson(testTemplate.name)
        val inApp = getInAppWithAction(actionJsonString)

        val inAppController = createInAppController()
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createFromJson(JSONObject(actionJsonString))!!, "", null, null
        )

        verify(exactly = 1) {
            mockTemplatesManager.presentTemplate(match { inApp ->
                inApp.customTemplateData?.templateName == testTemplate.name
            }, any(), any())
        }

        val currentInApp = InAppController.currentlyDisplayingInApp
        if (isVisual) {
            assertEquals(testTemplate.name, currentInApp?.customTemplateData?.templateName)
        } else {
            assertNull(currentInApp)
        }
    }

    private fun getInAppWithAction(actionJsonString: String): CTInAppNotification {
        return CTInAppNotification(
            JSONObject(
                """{
            "${Constants.KEY_TYPE}": "${CTInAppType.CTInAppTypeCover}",
            "${Constants.NOTIFICATION_ID_TAG}": "test-campaign",
            "${Constants.KEY_BUTTONS}": [{
                "${Constants.KEY_TEXT}": "Button",
                "${Constants.KEY_ACTIONS}": $actionJsonString
            }]
            }""".trimIndent()
            ), false
        )
    }

    private fun getCustomTemplateInAppJson(
        templateName: String, varsJsonString: String = "{}"
    ): String {
        return """
        {
            "${Constants.KEY_TYPE}": "${CTInAppType.CTInAppTypeCustomCodeTemplate}",
            "${CustomTemplateInAppData.KEY_TEMPLATE_NAME}": "$templateName",
            "${CustomTemplateInAppData.KEY_VARS}": $varsJsonString
        }
        """.trimIndent()
    }


    private fun createInAppController(): InAppController {
        return InAppController(
            context = mockk(relaxed = true),
            config = mockConfig,
            executors = MockCTExecutors(),
            controllerManager = mockControllerManager,
            callbackManager = mockCallbackManager,
            analyticsManager = mockAnalyticsManager,
            coreMetaData = mockk(relaxed = true),
            manifestInfo = mockManifestInfo,
            deviceInfo = mockk(relaxed = true),
            inAppQueue = fakeInAppQueue,
            evaluationManager = mockEvaluationManager,
            templatesManager = mockTemplatesManager,
            inAppActionHandler = mockInAppActionHandler,
            inAppNotificationInflater = mockInAppInflater,
            clock = fakeClock
        )
    }

    companion object {
        private const val EXCLUDED_ACTIVITY_NAME = "ExcludedActivity"
    }
}
