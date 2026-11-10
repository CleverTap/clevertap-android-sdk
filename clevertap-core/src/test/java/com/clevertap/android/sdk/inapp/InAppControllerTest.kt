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
import com.clevertap.android.sdk.inapp.CTLocalInApp.InAppType.ALERT
import com.clevertap.android.sdk.inapp.InAppNotificationInflater.InAppNotificationReadyListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.data.EvaluatedInAppsResult
import com.clevertap.android.sdk.inapp.delay.DelayedInAppResult
import com.clevertap.android.sdk.inapp.delay.InActionResult
import com.clevertap.android.sdk.inapp.delay.InAppScheduler
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager
import com.clevertap.android.sdk.inapp.fragment.CTInAppBaseFragment
import com.clevertap.android.sdk.inapp.pipsdk.PIPManager
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.network.NetworkMonitor
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.inapp.store.db.DisplayedInApp
import com.clevertap.android.sdk.inapp.store.db.DisplayedInAppStore
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.toList
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.sdk.utils.configMock
import androidx.fragment.app.FragmentActivity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val SAVED_INAPP_ID = "1742963524_20250326"

@RunWith(RobolectricTestRunner::class)
class InAppControllerTest {

    private lateinit var mockInAppDelayManager: InAppScheduler<DelayedInAppResult>
    private lateinit var mockInAppInActionManager: InAppScheduler<InActionResult>
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

    private lateinit var mockNetworkMonitor: NetworkMonitor
    private lateinit var mockValidationResultStack: ValidationResultStack
    private val fakeClock = FakeClock(timeMillis = 1735686000000) // 01.01.2025

    @Before
    fun setUp() {
        mockkStatic(CoreMetaData::class)
        every { CoreMetaData.isAppForeground() } returns true
        // Default to a FragmentActivity — the normal host for header/footer in-apps.
        every { CoreMetaData.getCurrentActivity() } returns mockk<FragmentActivity>(relaxed = true)

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

        mockNetworkMonitor = mockk(relaxed = true)
        every { mockNetworkMonitor.isNetworkOnline() } returns true

        mockValidationResultStack = mockk(relaxed = true)


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
        every { mockManifestInfo.isFragmentlessInAppBannersEnabled } returns false

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

        mockInAppDelayManager = mockk()
        mockInAppInActionManager = mockk()
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
    fun `inAppActionTriggered should raise notification clicked event for non-local inapp`() {
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
    fun `inAppActionTriggered should not raise notification clicked event`() {
        val inAppController = createInAppController()
        val inApp = CTInAppNotification(
            CTLocalInApp.builder().setInAppType(ALERT).setTitleText("titleText").setMessageText("messageText")
                .followDeviceOrientation(true).setPositiveBtnText("Agree").setNegativeBtnText("Decline")
                .build(), false
        )
        val callToAction = "test-c2a"
        val additionalDataEntry = Pair("testKey", "testData")
        val additionalData = Bundle().apply {
            putString(additionalDataEntry.first, additionalDataEntry.second)
        }
        inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), callToAction, additionalData, null
        )

        verify(exactly = 0) {
            mockAnalyticsManager.pushInAppNotificationStateEvent( any(), any(), any())
        }
    }

    @Test
    fun `inAppActionTriggered returns correct data for local inapp`() {
        val inAppController = createInAppController()
        val inApp = CTInAppNotification(
            CTLocalInApp.builder().setInAppType(ALERT).setTitleText("titleText").setMessageText("messageText")
                .followDeviceOrientation(true).setPositiveBtnText("Agree").setNegativeBtnText("Decline")
                .build(), false
        )
        val callToAction = "Allow"
        val additionalData = null

        val result = inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), callToAction, additionalData, null
        )

        assertEquals(callToAction, result.getString(Constants.KEY_C2A))
        assert(result.getString(Constants.NOTIFICATION_ID_TAG)!!.isEmpty())
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
            "${Constants.KEY_KV}": ${JSONObject((keyValues as Map<*, *>?)!!)}
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
    fun `inAppActionTriggered adds url action descriptors to clicked event`() {
        val url = "https://clevertap.com"
        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": "$url"
        }
        """.trimIndent()
        val inApp = getInAppWithAction(actionJsonString)

        createInAppController().inAppNotificationActionTriggered(
            inApp, CTInAppAction.createFromJson(JSONObject(actionJsonString))!!, "cta", null, null
        )

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, match { data ->
                data.getString(Constants.KEY_WZRK_ACTION) == InAppActionType.OPEN_URL.toString() &&
                        data.getString(Constants.KEY_WZRK_DATA) == url
            })
        }
    }

    @Test
    fun `inAppActionTriggered adds close action descriptors to clicked event`() {
        val inApp = getInAppWithAction("""{"${Constants.KEY_TYPE}": "${InAppActionType.CLOSE}"}""")

        createInAppController().inAppNotificationActionTriggered(
            inApp,
            CTInAppAction.createCloseAction(),
            Constants.INAPP_CTA_DISMISS_BUTTON,
            Bundle().apply { putString(Constants.KEY_WZRK_ELEMENT_ID, Constants.INAPP_ELEMENT_ID_CLOSE) },
            null
        )

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, match { data ->
                data.getString(Constants.KEY_WZRK_ACTION) == InAppActionType.CLOSE.toString() &&
                        data.getString(Constants.KEY_WZRK_DATA) == Constants.INAPP_WZRK_DATA_CLOSE &&
                        data.getString(Constants.KEY_WZRK_ELEMENT_ID) == Constants.INAPP_ELEMENT_ID_CLOSE
            })
        }
    }

    @Test
    fun `inAppActionTriggered adds kv action descriptors as a nested payload`() {
        every { mockCallbackManager.getInAppNotificationButtonListener() } returns null
        val keyValues = hashMapOf("key1" to "value1", "key2" to "value2")
        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.KEY_VALUES}",
            "${Constants.KEY_KV}": ${JSONObject((keyValues as Map<*, *>?)!!)}
        }
        """.trimIndent()
        val inApp = getInAppWithAction(actionJsonString)

        createInAppController().inAppNotificationActionTriggered(
            inApp, CTInAppAction.createFromJson(JSONObject(actionJsonString))!!, "cta", null, null
        )

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, match { data ->
                @Suppress("UNCHECKED_CAST")
                val kv = data.getSerializable(Constants.KEY_WZRK_DATA) as? Map<String, String>
                data.getString(Constants.KEY_WZRK_ACTION) == InAppActionType.KEY_VALUES.toString() &&
                        kv?.get("key1") == "value1" && kv?.get("key2") == "value2"
            })
        }
    }

    @Test
    fun `media error on html inapp reports wzrk_error and raises no clicked event`() {
        val inApp = CTInAppNotification(JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_HEADER), true)

        createInAppController().inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), Constants.INAPP_CTA_IMAGE_ERROR_DISMISS, null, null
        )

        verify(exactly = 1) {
            mockValidationResultStack.pushValidationResult(match<ValidationResult> {
                it.errorCode == Constants.INAPP_IMAGE_LOAD_FAILED_ERROR_CODE
            })
        }
        verify(exactly = 0) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(any(), any(), any())
        }
    }

    @Test
    fun `media error c2a on native inapp is treated as a normal click`() {
        val inApp = getInAppWithAction("""{"${Constants.KEY_TYPE}": "${InAppActionType.CLOSE}"}""")

        createInAppController().inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), Constants.INAPP_CTA_IMAGE_ERROR_DISMISS, null, null
        )

        verify(exactly = 0) { mockValidationResultStack.pushValidationResult(any<ValidationResult>()) }
        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, any())
        }
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
        inAppController.inAppNotificationDidClick(inApp, inApp.buttons[0], 0, null)

        verify(exactly = 1) { mockInAppActionHandler.openUrl(url, null) }
    }

    @Test
    fun `inAppNotificationDidClick tags the clicked button with a 1-based element id`() {
        val inApp = getInAppWithAction("""{"${Constants.KEY_TYPE}": "${InAppActionType.CLOSE}"}""")

        createInAppController().inAppNotificationDidClick(inApp, inApp.buttons[0], 0, null)

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, match { data ->
                data.getString(Constants.KEY_WZRK_ELEMENT_ID) == "${Constants.INAPP_ELEMENT_ID_BUTTON_PREFIX}1" &&
                        data.getString(Constants.KEY_WZRK_ACTION) == InAppActionType.CLOSE.toString() &&
                        data.getString(Constants.KEY_WZRK_DATA) == Constants.INAPP_WZRK_DATA_CLOSE
            })
        }
    }

    @Test
    fun `inAppNotificationDidClick uses the passed index for duplicate button payloads`() {
        // Two identical button payloads: CTInAppNotificationButton.equals is value-based, so indexOf
        // would resolve buttons[1] back to slot 0 and mis-tag it button-1.
        val buttonJson = """{
            "${Constants.KEY_TEXT}": "OK",
            "${Constants.KEY_ACTIONS}": {"${Constants.KEY_TYPE}": "${InAppActionType.CLOSE}"}
        }"""
        val inApp = CTInAppNotification(
            JSONObject(
                """{
            "${Constants.KEY_TYPE}": "${CTInAppType.CTInAppTypeCover}",
            "${Constants.NOTIFICATION_ID_TAG}": "test-campaign",
            "${Constants.KEY_BUTTONS}": [$buttonJson, $buttonJson]
            }""".trimIndent()
            ), false
        )
        // Sanity: the two buttons really are equal, so indexOf(buttons[1]) == 0 (the bug).
        assertEquals(inApp.buttons[0], inApp.buttons[1])

        createInAppController().inAppNotificationDidClick(inApp, inApp.buttons[1], 1, null)

        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(true, inApp, match { data ->
                data.getString(Constants.KEY_WZRK_ELEMENT_ID) == "${Constants.INAPP_ELEMENT_ID_BUTTON_PREFIX}2"
            })
        }
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
    fun `getDisplayingInApp returns the in-app on screen when the id matches`() {
        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        val currentInApp = InAppController.currentlyDisplayingInApp!!
        assertEquals("1742963524_20250326", currentInApp.campaignId)
        assertSame(currentInApp, InAppController.getDisplayingInApp("1742963524_20250326"))
    }

    @Test
    fun `getDisplayingInApp returns null when the id does not match`() {
        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        assertNull(InAppController.getDisplayingInApp("another_campaign"))
        assertNull(InAppController.getDisplayingInApp(null))
    }

    @Test
    fun `getDisplayingInApp returns null when no in-app is displayed`() {
        assertNull(InAppController.getDisplayingInApp("1742963524_20250326"))
    }

    @Test
    fun `full screen in-app is launched with the in-app it keeps in memory`() {
        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        val currentInApp = InAppController.currentlyDisplayingInApp!!
        verify(exactly = 1) {
            InAppNotificationActivity.launchForInAppNotification(any(), currentInApp, any())
        }
        assertSame(currentInApp, InAppController.getDisplayingInApp(currentInApp.campaignId))
    }

    @Test
    fun `full screen in-app on screen is saved with its time to live`() {
        val mockStore = displayedInAppStore()
        val savedId = slot<String>()
        val savedInApp = slot<JSONObject>()
        val savedTtl = slot<Long>()
        every { mockStore.save(capture(savedId), capture(savedInApp), capture(savedTtl)) } returns true
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        val currentInApp = InAppController.currentlyDisplayingInApp!!
        assertEquals(currentInApp.campaignId, savedId.captured)
        // The in-app JSON is saved unchanged (its events copy wzrk_ fields from it); the TTL is beside it.
        assertEquals(currentInApp.jsonDescription.toString(), savedInApp.captured.toString())
        assertEquals(currentInApp.timeToLive, savedTtl.captured)
    }

    @Test
    fun `dismissing the in-app removes the saved in-app`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val inApp = InAppController.currentlyDisplayingInApp!!

        inAppController.inAppNotificationDidDismiss(inApp, null)

        assertNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 1) { mockStore.remove(inApp.campaignId!!) }
    }

    @Test
    fun `restoreDisplayedInApp shows the saved in-app again and does not count it again`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60)
        val mockStore = displayedInAppStore(savedInApp)
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var restored: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            restored = it
            true
        }

        assertNotNull(restored)
        assertTrue(restored!!.isRestored)
        assertSame(restored, InAppController.getDisplayingInApp(restored!!.campaignId))

        inAppController.inAppNotificationDidShow(restored!!, null)
        verify(exactly = 0) { mockInAppFCManager.didShow(any(), any()) }
        verify(exactly = 0) { mockAnalyticsManager.pushInAppNotificationStateEvent(any(), any(), any()) }
        // The app still gets onShow, so onShow and onDismissed stay paired in the new process.
        verify(exactly = 1) { mockInAppListener.onShow(restored!!) }
    }

    @Test
    fun `restoreDisplayedInApp gives null when no in-app is saved for the id`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp("another_campaign") {
            result = it
            false
        }

        assertNull(result)
        assertNull(InAppController.currentlyDisplayingInApp)
        // Nothing is removed here: the store itself removes a row it cannot read.
        verify(exactly = 0) { mockStore.remove(any()) }
    }

    @Test
    fun `restoreDisplayedInApp gives null and removes the saved in-app when its time to live has passed`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() - 1)
        val mockStore = displayedInAppStore(savedInApp)
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            result = it
            false
        }

        assertNull(result)
        verify(exactly = 1) { mockStore.remove(SAVED_INAPP_ID) }
    }

    @Test
    fun `restoreDisplayedInApp queues the restored in-app while another in-app is on screen`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60, inAppId = "saved_campaign")
        val mockStore = displayedInAppStore(savedInApp)
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val onScreen = InAppController.currentlyDisplayingInApp

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp("saved_campaign") {
            result = it
            false
        }

        assertNull(result)
        assertSame(onScreen, InAppController.currentlyDisplayingInApp)

        // When the in-app on screen closes, the restored one is shown and still not counted again.
        inAppController.inAppNotificationDidDismiss(onScreen!!, null)
        val shownLater = InAppController.currentlyDisplayingInApp
        assertEquals("saved_campaign", shownLater?.campaignId)
        assertTrue(shownLater!!.isRestored)
        inAppController.inAppNotificationDidShow(shownLater, null)
        verify(exactly = 0) { mockInAppFCManager.didShow(any(), shownLater) }
        verify(exactly = 0) { mockAnalyticsManager.pushInAppNotificationStateEvent(false, shownLater, any()) }
        verify(exactly = 1) { mockInAppListener.onShow(shownLater) }
    }

    @Test
    fun `queued restored in-app is shown even when the frequency cap and beforeShow would refuse it`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60, inAppId = "saved_campaign")
        val mockStore = displayedInAppStore(savedInApp)
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val onScreen = InAppController.currentlyDisplayingInApp!!
        inAppController.restoreDisplayedInApp("saved_campaign") { false }
        // The restored in-app was counted before the process kill, so a "show once" cap now says no,
        // and the app already approved it in beforeShow.
        every { mockInAppFCManager.canShow(any(), any()) } returns false
        every { mockInAppListener.beforeShow(any()) } returns false

        inAppController.inAppNotificationDidDismiss(onScreen, null)

        val shownLater = InAppController.currentlyDisplayingInApp
        assertEquals("saved_campaign", shownLater?.campaignId)
        assertTrue(shownLater!!.isRestored)
        // canShow and beforeShow ran once for the first in-app, never for the restored one.
        verify(exactly = 0) { mockInAppFCManager.canShow(shownLater, any()) }
        verify(exactly = 1) { mockInAppFCManager.canShow(any(), any()) }
        verify(exactly = 1) { mockInAppListener.beforeShow(any()) }
    }

    @Test
    fun `queued restored in-app is shown through the main thread when the in-app on screen closes`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60, inAppId = "saved_campaign")
        val executors = MockCTExecutors()
        val inAppController = createInAppController(
            displayedInAppStore = displayedInAppStore(savedInApp),
            executors = executors
        )
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val onScreen = InAppController.currentlyDisplayingInApp!!
        inAppController.restoreDisplayedInApp("saved_campaign") { false }

        // The dismiss advances the queue on the in-app thread: the restored in-app must not be shown there.
        executors.deferMainTasks = true
        inAppController.inAppNotificationDidDismiss(onScreen, null)
        assertNull(InAppController.currentlyDisplayingInApp)

        executors.runPendingMainTasks()
        val shownLater = InAppController.currentlyDisplayingInApp
        assertEquals("saved_campaign", shownLater?.campaignId)
        assertTrue(shownLater!!.isRestored)
    }

    @Test
    fun `restored in-app that the in-app screen drops frees the in-app slot`() {
        val savedInApp = savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60)
        val mockStore = displayedInAppStore(savedInApp)
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) { false }

        assertNull(InAppController.currentlyDisplayingInApp)
        // The user did not close it, so it stays saved for a recreated in-app screen.
        verify(exactly = 0) { mockStore.remove(any()) }
    }

    @Test
    fun `in-app screen recreated during the restore restores the in-app again`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        // The first screen is destroyed (e.g. rotation) before its result comes back.
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) { false }
        var restored: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            restored = it
            true
        }

        assertNotNull(restored)
        assertSame(restored, InAppController.currentlyDisplayingInApp)
        verify(exactly = 0) { mockStore.remove(any()) }
    }

    @Test
    fun `restore that finishes after a newer restore is dropped and not queued again`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))
        // The inflater does not answer right away, so the two restores can finish out of order.
        val pending = mutableListOf<Pair<JSONObject, InAppNotificationReadyListener>>()
        every { mockInAppInflater.inflate(any(), any(), any()) } answers {
            pending.add(args[0] as JSONObject to args[2] as InAppNotificationReadyListener)
        }
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        // Screen A starts the restore, is recreated (rotation), and screen B starts it again.
        var shownOnA: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) { shownOnA = it; false }
        var shownOnB: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) { shownOnB = it; true }
        assertEquals(2, pending.size)

        // B's inflate finishes first and shows the in-app; A's finishes late.
        pending[1].second.onNotificationReady(CTInAppNotification(pending[1].first, true))
        pending[0].second.onNotificationReady(CTInAppNotification(pending[0].first, true))

        assertNotNull(shownOnB)
        assertSame(shownOnB, InAppController.currentlyDisplayingInApp)
        assertNull(shownOnA)
        // The late result is dropped: nothing waits in the queue to show the same campaign again.
        inAppController.inAppNotificationDidDismiss(shownOnB!!, null)
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `in-app without an id is not saved`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        val noId = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL).apply { remove(Constants.NOTIFICATION_ID_TAG) }

        inAppController.addInAppNotificationsToQueue(listOf(noId))

        assertNotNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 0) { mockStore.save(any(), any(), any()) }
    }

    @Test
    fun `restoreDisplayedInApp gives null when in-apps are suspended or discarded`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))

        for (stopInApps in listOf<(InAppController) -> Unit>({ it.suspendInApps() }, { it.discardInApps(false) })) {
            val inAppController = createInAppController(displayedInAppStore = mockStore)
            stopInApps(inAppController)

            var result: CTInAppNotification? = mockk()
            inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
                result = it
                false
            }

            assertNull(result)
            assertNull(InAppController.currentlyDisplayingInApp)
        }
        verify(exactly = 2) { mockStore.remove(SAVED_INAPP_ID) }
    }

    @Test
    fun `restoreDisplayedInApp gives null for an HTML in-app when there is no internet`() {
        every { mockNetworkMonitor.isNetworkOnline() } returns false
        val htmlInApp = JSONObject(
            """{"ti":1,"wzrk_id":"html_campaign","type":"custom-html",""" +
                """"w":{"dk":true,"sc":true,"pos":"c","xp":90,"yp":85},"d":{"html":"<html></html>"}}"""
        )
        val mockStore = displayedInAppStore(DisplayedInApp(htmlInApp, fakeClock.currentTimeSeconds() + 60))
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp("html_campaign") {
            result = it
            false
        }

        assertNull(result)
        verify(exactly = 1) { mockStore.remove("html_campaign") }
    }

    @Test
    fun `full screen in-app is shown and not saved when there is no database store`() {
        val inAppController = createInAppController(displayedInAppStore = null)

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        assertNotNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `header in-app is not saved because it is not shown in the in-app screen`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]").toList()
        )

        assertNotNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 0) { mockStore.save(any(), any(), any()) }
    }

    @Test
    fun `next full screen in-app replaces the saved one`() {
        val mockStore = displayedInAppStore()
        val savedIds = mutableListOf<String>()
        every { mockStore.save(capture(savedIds), any(), any()) } returns true
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        val second = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL).put(Constants.NOTIFICATION_ID_TAG, "second_campaign")

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        inAppController.inAppNotificationDidDismiss(InAppController.currentlyDisplayingInApp!!, null)
        inAppController.addInAppNotificationsToQueue(listOf(second))

        // save replaces the old row, so only one in-app is ever kept.
        assertEquals(listOf(SAVED_INAPP_ID, "second_campaign"), savedIds)
    }

    @Test
    fun `dismissing an in-app that is not on screen keeps the saved in-app`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val onScreen = InAppController.currentlyDisplayingInApp!!
        val other = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL).put(Constants.NOTIFICATION_ID_TAG, "other_campaign"), true
        )

        inAppController.inAppNotificationDidDismiss(other, null)

        assertSame(onScreen, InAppController.currentlyDisplayingInApp)
        verify(exactly = 0) { mockStore.remove(any()) }
    }

    @Test
    fun `PIP that fails to show removes the saved in-app`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )
        val inApp = InAppController.currentlyDisplayingInApp!!

        inAppController.onPIPShowFailed(inApp, PIPMediaType.IMAGE)

        assertNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 1) { mockStore.remove(inApp.campaignId!!) }
    }

    @Test
    fun `restored in-app closed by the user removes the saved in-app`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        var restored: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            restored = it
            true
        }

        inAppController.inAppNotificationDidDismiss(restored!!, null)

        assertNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 1) { mockStore.remove(SAVED_INAPP_ID) }
    }

    @Test
    fun `restoreDisplayedInApp shows the in-app when its time to live ends this second`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds()))
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var restored: CTInAppNotification? = null
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            restored = it
            true
        }

        // Same check as showInApp: only a time to live in the past stops it.
        assertNotNull(restored)
        verify(exactly = 0) { mockStore.remove(any()) }
    }

    @Test
    fun `restoreDisplayedInApp gives null right away when there is no database store`() {
        val inAppController = createInAppController(displayedInAppStore = null)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            result = it
            false
        }

        assertNull(result)
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `restoreDisplayedInApp gives null when the saved in-app cannot be built`() {
        val mockStore = displayedInAppStore(savedInApp(ttlSeconds = fakeClock.currentTimeSeconds() + 60))
        val broken = mockk<CTInAppNotification>(relaxed = true) {
            every { error } returns "bad in-app"
            every { campaignId } returns SAVED_INAPP_ID
        }
        every { mockInAppInflater.inflate(any(), any(), any()) } answers {
            (args[2] as InAppNotificationReadyListener).onNotificationReady(broken)
        }
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp(SAVED_INAPP_ID) {
            result = it
            false
        }

        assertNull(result)
        assertNull(InAppController.currentlyDisplayingInApp)
        // The screen closes and nothing retries, so the row is removed instead of lingering.
        verify(exactly = 1) { mockStore.remove(SAVED_INAPP_ID) }
    }

    @Test
    fun `user change removes the saved in-app so it is not restored for the new user`() {
        val mockStore = displayedInAppStore()
        val inAppController = createInAppController(displayedInAppStore = mockStore)
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL}]").toList()
        )

        inAppController.onChangeUser("new_device_id", "account_id")

        verify(exactly = 1) { mockStore.clear() }
    }

    @Test
    fun `restoreDisplayedInApp gives null for a push permission in-app when permission is already granted`() {
        every { mockInAppActionHandler.arePushNotificationsEnabled() } returns true
        val pushPermissionInApp = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL)
            .put(Constants.NOTIFICATION_ID_TAG, "push_primer")
            .put(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, true)
        val mockStore = displayedInAppStore(DisplayedInApp(pushPermissionInApp, fakeClock.currentTimeSeconds() + 60))
        val inAppController = createInAppController(displayedInAppStore = mockStore)

        var result: CTInAppNotification? = mockk()
        inAppController.restoreDisplayedInApp("push_primer") {
            result = it
            false
        }

        // Same rule as showInApp: a push permission in-app is not shown once permission is granted,
        // and the app's push permission listeners get the result.
        assertNull(result)
        assertNull(InAppController.currentlyDisplayingInApp)
        verify(exactly = 1) { mockStore.remove("push_primer") }
        verify(exactly = 1) { mockInAppActionHandler.notifyPushPermissionListeners() }
    }

    private fun savedInApp(ttlSeconds: Long, inAppId: String = SAVED_INAPP_ID): DisplayedInApp {
        val inApp = JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL).put(Constants.NOTIFICATION_ID_TAG, inAppId)
        return DisplayedInApp(inApp, ttlSeconds)
    }

    // A store that has only [savedInApp], saved under the id of its in-app.
    private fun displayedInAppStore(savedInApp: DisplayedInApp? = null): DisplayedInAppStore {
        val store = mockk<DisplayedInAppStore>(relaxed = true)
        every { store.get(any()) } returns null
        if (savedInApp != null) {
            val inAppId = savedInApp.inApp.getString(Constants.NOTIFICATION_ID_TAG)
            every { store.get(inAppId) } returns savedInApp
        }
        return store
    }

    @Test
    fun `addInAppNotificationsToQueue should add to queue and display all provided notifications`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_HALF_INTERSTITIAL},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")

        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(inApps.toList())

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
    fun `HTML header in-app uses the fragment path on a FragmentActivity`() {
        every { CoreMetaData.getCurrentActivity() } returns mockk<FragmentActivity>(relaxed = true)
        val inAppController = createInAppController()

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]").toList()
        )

        verify(exactly = 1) {
            CTInAppBaseFragment.showOnActivity(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `HTML header in-app is dropped on a non-FragmentActivity when fragmentless banners are disabled`() {
        // Plain Activity host (not a FragmentActivity); flag defaults to false.
        every { CoreMetaData.getCurrentActivity() } returns mockk<Activity>(relaxed = true)
        val inAppController = createInAppController()

        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]").toList()
        )

        verify(exactly = 0) {
            CTInAppBaseFragment.showOnActivity(any(), any(), any(), any(), any())
        }
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `HTML header in-app shows the overlay on a non-FragmentActivity when fragmentless banners are enabled`() {
        every { CoreMetaData.getCurrentActivity() } returns mockk<Activity>(relaxed = true)
        every { mockManifestInfo.isFragmentlessInAppBannersEnabled } returns true
        mockkConstructor(CTInAppHtmlBannerOverlay::class)
        every { anyConstructed<CTInAppHtmlBannerOverlay>().show() } just runs

        val inAppController = createInAppController()
        inAppController.addInAppNotificationsToQueue(
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]").toList()
        )

        verify(exactly = 1) { anyConstructed<CTInAppHtmlBannerOverlay>().show() }
        verify(exactly = 0) {
            CTInAppBaseFragment.showOnActivity(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `suspendInApps should pause inapps until resumeInApps is called`() {
        val inAppController = createInAppController()
        inAppController.suspendInApps()

        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        inAppController.addInAppNotificationsToQueue(inApps.toList())
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
        inAppController.discardInApps(false)

        // TODO verify when multiple inApps are added to the queue only the first is discarded
//        val inApps =
//            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        val inApps = JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        inAppController.addInAppNotificationsToQueue(inApps.toList())
        assertNull(InAppController.currentlyDisplayingInApp)

        inAppController.resumeInApps()
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `discardInApps should not hideInApp when nothing is displayed`() {
        val mockDisplayListener =  mockk<InAppDisplayListener>(relaxed = true)
        val inAppController = createInAppController()
        inAppController.registerInAppDisplayListener(mockDisplayListener)

        inAppController.discardInApps(true)

        verify(exactly = 0) { mockDisplayListener.hideInApp() }

        inAppController.unregisterInAppDisplayListener()
    }

    @Test
    fun `discardInApps should hideInApp when inapp is displayed`() {
        val mockDisplayListener =  mockk<InAppDisplayListener>(relaxed = true)
        val inAppController = createInAppController()
        inAppController.registerInAppDisplayListener(mockDisplayListener)
        val inApps = JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        inAppController.addInAppNotificationsToQueue(inApps.toList())

        inAppController.discardInApps(true)

        verify { mockDisplayListener.hideInApp() }
    }

    @Test
    fun `discardInApps should not hideInApp when inapp is displayed and listener is not set`() {
        val mockDisplayListener =  mockk<InAppDisplayListener>(relaxed = true)
        val inAppController = createInAppController()
        inAppController.registerInAppDisplayListener(mockDisplayListener)
        val inApps = JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        inAppController.addInAppNotificationsToQueue(inApps.toList())
        inAppController.unregisterInAppDisplayListener()

        inAppController.discardInApps(true)

        verify(exactly = 0) { mockDisplayListener.hideInApp() }
    }

    @Test
    fun `onQueueEvent should evaluate and display all matching client-side in-apps`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_COVER_WITH_FUNCTION_BUTTON_ACTION}]")
        val delayedInApps = JSONArray().toList<JSONObject>()
        val inactionInApps = JSONArray().toList<JSONObject>()
        every { mockEvaluationManager.evaluateOnEvent(any(), any(), any()) } returns EvaluatedInAppsResult(
            inApps.toList(),
            delayedInApps,
            inactionInApps
        )

        val inAppController = createInAppController()
        inAppController.onQueueEvent("event", emptyMap(), mockk())
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
        val delayedInApps = JSONArray().toList<JSONObject>()
        val inactionInApps = JSONArray().toList<JSONObject>()
        every { mockEvaluationManager.evaluateOnChargedEvent(any(), any(), any()) } returns EvaluatedInAppsResult(
            inApps.toList(),
            delayedInApps,
            inactionInApps
        )

        val inAppController = createInAppController()
        inAppController.onQueueChargedEvent(
            emptyMap(),
            emptyList(),
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
        val delayedInApps = JSONArray().toList<JSONObject>()
        val inactionInApps = JSONArray().toList<JSONObject>()
        every {
            mockEvaluationManager.evaluateOnUserAttributeChange(
                any(),
                any(),
                any()
            )
        } returns EvaluatedInAppsResult(inApps.toList(), delayedInApps.toList(),inactionInApps)

        val inAppController = createInAppController()
        inAppController.onQueueProfileEvent(
            emptyMap(),
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
        } returns inApps.toList()

        val inAppController = createInAppController()
        inAppController.onAppLaunchServerSideInAppsResponse(inApps.toList(), emptyList(), mockk())
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
        fakeInAppQueue.enqueueAll(inApps.toList())
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
        fakeInAppQueue.enqueueAll(inApps.toList())
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
        fakeInAppQueue.enqueueAll(inApps.toList())

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()

        //verify only the custom html with kv is displayed
        verifyInAppsDisplayed(inAppController, Constants.KEY_CUSTOM_HTML)
    }

    @Test
    fun `showNotificationIfAvailable should not display in-apps while the app is in background`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA},${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV}]")
        fakeInAppQueue.enqueueAll(inApps.toList())

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
        fakeInAppQueue.enqueueAll(inApps.toList())

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
        fakeInAppQueue.enqueueAll(inApps.toList())

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
        fakeInAppQueue.enqueueAll(inApps.toList())

        val mockActivity = mockk<FragmentActivity>()
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
        fakeInAppQueue.enqueueAll(inApps.toList())

        val inAppController = createInAppController()
        inAppController.showNotificationIfAvailable()
        assertNull(InAppController.currentlyDisplayingInApp)
    }

    @Test
    fun `showNotificationIfAvailable should drop html in-apps when there is no internet`() {
        val inApps =
            JSONArray("[${InAppFixtures.TYPE_CUSTOM_HTML_HEADER_WITH_KV},${InAppFixtures.TYPE_INTERSTITIAL_WITH_MEDIA}]")
        fakeInAppQueue.enqueueAll(inApps.toList())
        every { mockNetworkMonitor.isNetworkOnline() } returns false

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
        fakeInAppQueue.enqueueAll(inApps.toList())

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


    private fun createInAppController(
        pipManager: PIPManager = mockk(relaxed = true),
        displayedInAppStore: DisplayedInAppStore? = null,
        executors: MockCTExecutors = MockCTExecutors()
    ): InAppController {
        return InAppController(
            context = mockk(relaxed = true),
            config = mockConfig,
            executors = executors,
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
            inAppDelayManager = mockInAppDelayManager,
            inAppInActionManager = mockInAppInActionManager,
            networkMonitor = mockNetworkMonitor,
            clock = fakeClock,
            pipManager = pipManager,
            validationResultStack = mockValidationResultStack,
            displayedInAppStoreProvider = { displayedInAppStore },
        )
    }

    @Test
    fun `dismissPipInApp delegates to pipManager dismissFromApi`() {
        val mockPipManager = mockk<PIPManager>(relaxed = true)
        val inAppController = createInAppController(pipManager = mockPipManager)

        inAppController.dismissPipInApp()

        verify(exactly = 1) { mockPipManager.dismissFromApi() }
        verify(exactly = 0) { mockPipManager.dismiss() }
    }

    @Test
    fun `onPIPShowFailed with VIDEO pushes the PIP video error onto the validation stack`() {
        val inAppController = createInAppController()
        val notification = mockk<CTInAppNotification>(relaxed = true) {
            every { campaignId } returns "pip_campaign"
        }

        inAppController.onPIPShowFailed(notification, PIPMediaType.VIDEO)

        verify(exactly = 1) {
            mockValidationResultStack.pushValidationResult(match<ValidationResult> {
                it.errorCode == Constants.INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_CODE &&
                        it.errorDesc == Constants.INAPP_PIP_VIDEO_LOAD_FAILED_ERROR_MSG
            })
        }
    }

    @Test
    fun `onPIPShowFailed with IMAGE pushes the PIP image error onto the validation stack`() {
        val inAppController = createInAppController()
        val notification = mockk<CTInAppNotification>(relaxed = true) {
            every { campaignId } returns "pip_campaign"
        }

        inAppController.onPIPShowFailed(notification, PIPMediaType.IMAGE)

        verify(exactly = 1) {
            mockValidationResultStack.pushValidationResult(match<ValidationResult> {
                it.errorCode == Constants.INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_CODE &&
                        it.errorDesc == Constants.INAPP_PIP_IMAGE_LOAD_FAILED_ERROR_MSG
            })
        }
    }

    @Test
    fun `onPIPShowFailed with GIF pushes the PIP GIF error onto the validation stack`() {
        val inAppController = createInAppController()
        val notification = mockk<CTInAppNotification>(relaxed = true) {
            every { campaignId } returns "pip_campaign"
        }

        inAppController.onPIPShowFailed(notification, PIPMediaType.GIF)

        verify(exactly = 1) {
            mockValidationResultStack.pushValidationResult(match<ValidationResult> {
                it.errorCode == Constants.INAPP_PIP_GIF_LOAD_FAILED_ERROR_CODE &&
                        it.errorDesc == Constants.INAPP_PIP_GIF_LOAD_FAILED_ERROR_MSG
            })
        }
    }

    // Deep Link Attribution Tests

    @Test
    fun `inAppActionTriggered should include wzrk_dl for button with OPEN_URL action`() {
        val inAppController = createInAppController()
        val deepLinkUrl = "https://example.com/deep-link"
        val callToAction = "Click Here"

        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": "$deepLinkUrl"
        }
        """.trimIndent()

        val inApp = getInAppWithAction(actionJsonString)
        val action = CTInAppAction.createFromJson(JSONObject(actionJsonString))!!

        val result = inAppController.inAppNotificationActionTriggered(
            inApp, action, callToAction, null, null
        )

        assertEquals(deepLinkUrl, result.getString(Constants.DEEP_LINK_KEY))
        assertEquals(callToAction, result.getString(Constants.KEY_C2A))
    }

    @Test
    fun `inAppActionTriggered should NOT include wzrk_dl for CLOSE action`() {
        val inAppController = createInAppController()
        val callToAction = "Close"

        val inApp = getInAppWithAction("""{"${Constants.KEY_TYPE}": "close"}""")

        val result = inAppController.inAppNotificationActionTriggered(
            inApp, CTInAppAction.createCloseAction(), callToAction, null, null
        )

        assertNull(result.getString(Constants.DEEP_LINK_KEY))
        assertEquals(callToAction, result.getString(Constants.KEY_C2A))
    }

    @Test
    fun `inAppActionTriggered should NOT include wzrk_dl for KEY_VALUES action`() {
        every { mockCallbackManager.getInAppNotificationButtonListener() } returns mockk(relaxed = true)
        val inAppController = createInAppController()
        val callToAction = "Submit"

        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.KEY_VALUES}",
            "${Constants.KEY_KV}": {"key1": "value1"}
        }
        """.trimIndent()

        val inApp = getInAppWithAction(actionJsonString)
        val action = CTInAppAction.createFromJson(JSONObject(actionJsonString))!!

        val result = inAppController.inAppNotificationActionTriggered(
            inApp, action, callToAction, null, null
        )

        assertNull(result.getString(Constants.DEEP_LINK_KEY))
        assertEquals(callToAction, result.getString(Constants.KEY_C2A))
    }

    @Test
    fun `inAppActionTriggered should NOT include wzrk_dl for empty actionUrl`() {
        val inAppController = createInAppController()
        val callToAction = "Click"

        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": ""
        }
        """.trimIndent()

        val inApp = getInAppWithAction(actionJsonString)
        val action = CTInAppAction.createFromJson(JSONObject(actionJsonString))!!

        val result = inAppController.inAppNotificationActionTriggered(
            inApp, action, callToAction, null, null
        )

        assertNull(result.getString(Constants.DEEP_LINK_KEY))
        assertEquals(callToAction, result.getString(Constants.KEY_C2A))
    }

    @Test
    fun `inAppActionTriggered should verify analytics event includes wzrk_dl`() {
        val inAppController = createInAppController()
        val deepLinkUrl = "https://example.com/deep-link"
        val callToAction = "Click Here"

        val actionJsonString = """
        {
            "${Constants.KEY_TYPE}": "${InAppActionType.OPEN_URL}",
            "${Constants.KEY_ANDROID}": "$deepLinkUrl"
        }
        """.trimIndent()

        val inAppJson = JSONObject("""
        {
            "${Constants.KEY_TYPE}": "${CTInAppType.CTInAppTypeCover}",
            "${Constants.NOTIFICATION_ID_TAG}": "test-campaign-123",
            "${Constants.KEY_BUTTONS}": [{
                "${Constants.KEY_TEXT}": "$callToAction",
                "${Constants.KEY_ACTIONS}": $actionJsonString
            }]
        }
        """.trimIndent())

        val inApp = CTInAppNotification(inAppJson, false)
        val action = CTInAppAction.createFromJson(JSONObject(actionJsonString))!!

        inAppController.inAppNotificationActionTriggered(
            inApp, action, callToAction, null, null
        )

        // Verify analytics manager was called with Bundle containing wzrk_dl
        verify(exactly = 1) {
            mockAnalyticsManager.pushInAppNotificationStateEvent(
                eq(true),
                eq(inApp),
                match { bundle ->
                    bundle.getString(Constants.DEEP_LINK_KEY) == deepLinkUrl &&
                    bundle.getString(Constants.KEY_C2A) == callToAction &&
                    bundle.getString(Constants.NOTIFICATION_ID_TAG) == "test-campaign-123"
                }
            )
        }
    }

    companion object {
        private const val EXCLUDED_ACTIVITY_NAME = "ExcludedActivity"
    }
}
