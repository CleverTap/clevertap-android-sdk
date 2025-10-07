package com.clevertap.android.sdk

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.clevertap.android.sdk.AnalyticsManagerBundler.notificationClickedJson
import com.clevertap.android.sdk.AnalyticsManagerBundler.notificationViewedJson
import com.clevertap.android.sdk.displayunits.CTDisplayUnitController
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppPreviewHandler
import com.clevertap.android.sdk.inbox.CTInboxController
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.utils.UriHelper
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.Validator
import com.clevertap.android.sdk.validation.Validator.ValidationContext.Profile
import io.mockk.MockKAnnotations
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONCompare
import org.skyscreamer.jsonassert.JSONCompareMode
import java.util.concurrent.Future
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerTest {

    private lateinit var analyticsManagerSUT: AnalyticsManager
    private lateinit var coreState: MockCoreStateKotlin
    private val cleverTapInstanceConfig = CleverTapFixtures.provideCleverTapInstanceConfig()

    @MockK(relaxed = true)
    private lateinit var validator: Validator

    @MockK(relaxed = true)
    private lateinit var validationResultStack: ValidationResultStack

    @MockK(relaxed = true)
    private lateinit var eventQueueManager: BaseEventQueueManager

    @MockK(relaxed = true)
    private lateinit var context: Context

    @MockK(relaxed = true)
    private lateinit var timeProvider: Clock

    @MockK(relaxed = true)
    private lateinit var inAppPreviewHandler: InAppPreviewHandler

    private val bundleIdCheck = Bundle().apply {
        putString("wzrk_pn", "wzrk_pn")
        putString("wzrk_id", "duplicate-id")
        putString("wzrk_pid", "pid")
        putString("wzrk_someid", "someid")
    }

    private val bundlePidCheck = Bundle().apply {
        putString("wzrk_pn", "wzrk_pn")
        putString("wzrk_id", "duplicate-id")
        putString("wzrk_pid", "pid")
        putString("wzrk_someid", "someid")
        putBoolean("wzrk_dd", true)
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        analyticsManagerSUT = AnalyticsManager(
            context,
            cleverTapInstanceConfig,
            eventQueueManager,
            validator,
            validationResultStack,
            coreState.coreMetaData,
            coreState.deviceInfo,
            coreState.callbackManager,
            coreState.controllerManager,
            coreState.cTLockManager,
            timeProvider,
            MockCTExecutors(),
            inAppPreviewHandler
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `clevertap does not process push notification viewed or clicked event if it is not from clevertap`() {
        val bundle = Bundle().apply {
            putString("some", "random")
            putString("non clevertap", "bundle")
        }

        analyticsManagerSUT.pushNotificationViewedEvent(bundle)
        analyticsManagerSUT.pushNotificationClickedEvent(bundle)

        verify {
            eventQueueManager wasNot called
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap does not process push notification viewed event if wzrk_id is not present`() {
        val bundle = Bundle().apply {
            putString("some", "random")
            putString("non clevertap", "bundle")
            putString("wzrk_pid", "pid")
        }

        analyticsManagerSUT.pushNotificationViewedEvent(bundle)
        analyticsManagerSUT.pushNotificationClickedEvent(bundle)
        verify {
            eventQueueManager wasNot called
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap does not process duplicate PN viewed within 2 seconds - case 2nd notif in 200ms`() {
        val json = notificationViewedJson(bundleIdCheck)

        every { timeProvider.currentTimeMillis() } returns 10000

        // send PN first time
        analyticsManagerSUT.pushNotificationViewedEvent(bundleIdCheck)

        verify {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.NV_EVENT
            )
        }

        // setup again, 200 ms has passed
        every { timeProvider.currentTimeMillis() } returns 10200

        // Send duplicate PN
        analyticsManagerSUT.pushNotificationViewedEvent(bundleIdCheck)

        // verify it was not called again, one time was from before
        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.NV_EVENT
            )
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap processes PN viewed for same wzrk_id if separated by a span of greater than 2 seconds`() {

        val json = notificationViewedJson(bundleIdCheck)

        every { timeProvider.currentTimeMillis() } returns 10000

        // send PN first time
        analyticsManagerSUT.pushNotificationViewedEvent(bundleIdCheck)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.NV_EVENT
            )
        }

        // setup again, 10000 ms has passed
        every { timeProvider.currentTimeMillis() } returns 20000

        // Send duplicate PN
        analyticsManagerSUT.pushNotificationViewedEvent(bundleIdCheck)

        // verify queue event called again
        verify(exactly = 2) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.NV_EVENT
            )
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap does not process PN Clicked if SDK is set to analytics only`() {
        cleverTapInstanceConfig.isAnalyticsOnly = true

        // send PN first time
        analyticsManagerSUT.pushNotificationClickedEvent(bundleIdCheck)

        verify {
            eventQueueManager wasNot called
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap does not process duplicate (same wzrk_id) PN clicked within 2 seconds - case 2nd click happens in 200ms`() {

        val json = notificationClickedJson(bundleIdCheck)
        every { timeProvider.currentTimeMillis() } returns 0

        // send PN first time
        analyticsManagerSUT.pushNotificationClickedEvent(bundleIdCheck)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.RAISED_EVENT
            )
        }

        // setup again, 2000 ms has passed
        every { timeProvider.currentTimeMillis() } returns 200

        // Send duplicate PN
        analyticsManagerSUT.pushNotificationClickedEvent(bundleIdCheck)

        // verify it was not called again, one time was from before
        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.RAISED_EVENT
            )
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `clevertap processes PN clicked for same wzrk_id if separated by a span of greater than 5 seconds`() {

        val json = notificationClickedJson(bundleIdCheck)
        every { timeProvider.currentTimeMillis() } returns 10000

        // send PN first time
        analyticsManagerSUT.pushNotificationClickedEvent(bundleIdCheck)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.RAISED_EVENT
            )
        }

        every { timeProvider.currentTimeMillis() } returns 15001

        // Send duplicate PN
        analyticsManagerSUT.pushNotificationClickedEvent(bundleIdCheck)

        // verify queue event called again
        verify(exactly = 2) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json, it, JSONCompareMode.STRICT).passed()
                },
                Constants.RAISED_EVENT
            )
        }
        confirmVerified(eventQueueManager)
    }

    @Test
    fun `dedupeCheckKey used wzrk_id incase wzrk_dd key is false or not present`() {
        val key1 = analyticsManagerSUT.dedupeCheckKey(bundleIdCheck)
        assertEquals("duplicate-id", key1)

        val bundleIdCheckKeyFalse = Bundle().apply {
            putString("wzrk_pn", "wzrk_pn")
            putString("wzrk_id", "duplicate-id")
            putString("wzrk_pid", "pid")
            putString("wzrk_someid", "someid")
            putString("wzrk_dd", "false")
        }

        val key2 = analyticsManagerSUT.dedupeCheckKey(bundleIdCheckKeyFalse)
        assertEquals("duplicate-id", key2)
    }

    @Test
    fun `dedupeCheckKey used wzrk_pid incase wzrk_dd key is true string or boolean`() {

        val key1 = analyticsManagerSUT.dedupeCheckKey(bundlePidCheck)
        assertEquals("pid", key1)

        val bundlePidCheckString = Bundle().apply {
            putString("wzrk_pn", "wzrk_pn")
            putString("wzrk_id", "duplicate-id")
            putString("wzrk_pid", "pid")
            putString("wzrk_someid", "someid")
            putString("wzrk_dd", "TRUE")
        }

        val key2 = analyticsManagerSUT.dedupeCheckKey(bundlePidCheckString)
        assertEquals("pid", key2)
    }

    @Test
    fun `clevertap dedupe check is based on wzrk_pid only if flag (wzrk_dd) is enabled`() {

        // Setup
        val notif1 = Bundle().apply {
            putString("wzrk_pn", "wzrk_pn")
            putString("wzrk_id", "wzrk_id_1111")
            putString("wzrk_someid", "someid1111")
            putString("wzrk_dd", "true")

            putString("wzrk_pid", "same_pid")
        }

        val notif2 = Bundle().apply {
            putString("wzrk_pn", "wzrk_pn")
            putString("wzrk_id", "wzrk_id_2222")
            putString("wzrk_someid", "someid2222")
            putString("wzrk_dd", "true")

            putString("wzrk_pid", "same_pid")
        }

        val json1 = notificationClickedJson(notif1)

        every { timeProvider.currentTimeMillis() } returns 0

        // ACT : send PN first time
        analyticsManagerSUT.pushNotificationClickedEvent(notif1)

        // Validate
        verify(exactly = 1) {
            eventQueueManager.queueEvent(
                context,
                match {
                    JSONCompare.compareJSON(json1, it, JSONCompareMode.STRICT).passed()
                },
                Constants.RAISED_EVENT
            )
        }

        // More setup, 100ms passed
        every { timeProvider.currentTimeMillis() } returns 100

        // ACT : send PN second time
        analyticsManagerSUT.pushNotificationClickedEvent(notif2)

        // Validate
        confirmVerified(eventQueueManager)
    }

    @Test
    fun test_incrementValue_nullKey_noAction() {
        analyticsManagerSUT.incrementValue(null, 10)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_incrementValue_nullValue_noAction() {
        analyticsManagerSUT.incrementValue("abc", null)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_incrementValue_emptyKey_throwsEmptyKeyError() {
        mockCleanObjectKey("")
        analyticsManagerSUT.incrementValue("", 10)

        verify {
            validationResultStack.pushValidationResult(match { arg ->
                arg.errorCode == 512
            })
        }
    }

    @Test
    fun test_decrementValue_negativeValue_throwsMalformedValueError() {
        mockCleanObjectKey("abc", 0)
        analyticsManagerSUT.decrementValue("abc", -10)

        verify {
            validationResultStack.pushValidationResult(match { arg ->
                arg.errorCode == 512
            })
        }
    }

    @Test
    fun test_incrementValue_intValueIsPassed_incrementIntValue() {
        mockCleanObjectKey("int_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10)
        val updateObj = JSONObject().put("int_score", commandObj)

        every { coreState.localDataStore.getProfileProperty("int_score") } returns 10

        analyticsManagerSUT.incrementValue("int_score", 10)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_incrementValue_doubleValueIsPassed_incrementDoubleValue() {
        mockCleanObjectKey("double_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25)
        val updateObj = JSONObject().put("double_score", commandObj)

        every { coreState.localDataStore.getProfileProperty("double_score") } returns (10.25)

        analyticsManagerSUT.incrementValue("double_score", 10.25)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_incrementValue_floatValueIsPassed_incrementFloatValue() {
        mockCleanObjectKey("float_score")

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_INCREMENT, 10.25f)
        val updateObj = JSONObject().put("float_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("float_score")
        } returns 10.25f

        analyticsManagerSUT.incrementValue("float_score", 10.25f)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_nullValue_noAction() {
        analyticsManagerSUT.decrementValue("abc", null)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_decrementValue_nullKey_noAction() {
        analyticsManagerSUT.decrementValue(null, 10)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_decrementValue_intValueIsPassed_decrementIntValue() {
        mockCleanObjectKey("decr_int_score")

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10)
        val updateObj = JSONObject().put("decr_int_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_int_score")
        } returns 30

        analyticsManagerSUT.decrementValue("decr_int_score", 10)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_doubleValueIsPassed_decrementDoubleValue() {
        mockCleanObjectKey("decr_double_score")

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50)
        val updateObj = JSONObject().put("decr_double_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_double_score")
        } returns 20.25

        analyticsManagerSUT.decrementValue("decr_double_score", 10.50)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_decrementValue_floatValueIsPassed_decrementFloatValue() {
        mockCleanObjectKey("decr_float_score", 0)

        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DECREMENT, 10.50f)
        val updateObj = JSONObject().put("decr_float_score", commandObj)

        every {
            coreState.localDataStore.getProfileProperty("decr_float_score")
        } returns 20.25f

        analyticsManagerSUT.decrementValue("decr_float_score", 10.50f)

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_removeValueForKey_when_key_identity() {
        val key = "Identity"
        mockCleanObjectKey(key)

        analyticsManagerSUT.removeValueForKey(key)

        verify(exactly = 0) {
            eventQueueManager.pushBasicProfile(any(), any())
        }
    }

    @Test
    fun test_removeValueForKey_when_key_identity_is_lowercase() {
        val key = "identity"
        mockCleanObjectKey(key)

        analyticsManagerSUT.removeValueForKey(key)
        verify(exactly = 0) {
            eventQueueManager.pushBasicProfile(any(), any())
        }
    }

    @Test
    fun test_removeValueForKey_when_NullKey_pushesEmptyKeyError() {
        mockCleanObjectKey("")

        analyticsManagerSUT.removeValueForKey(null)

        verify {
            validationResultStack.pushValidationResult(match {
                it.errorCode == 512
            })
        }
    }

    @Test
    fun test_removeValueForKey_when_EmptyKey_pushesEmptyKeyError() {
        mockCleanObjectKey("")

        analyticsManagerSUT.removeValueForKey("")
        verify {
            validationResultStack.pushValidationResult(match {
                it.errorCode == 512
            })
        }
    }

    @Test
    fun test_removeValueForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj: JSONObject = JSONObject().put(Constants.COMMAND_DELETE, true)
        val updateObj = JSONObject().put("abc", commandObj)

        mockCleanObjectKey("abc")

        analyticsManagerSUT.removeValueForKey("abc")

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(updateObj, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_NullKey_noAction() {
        analyticsManagerSUT.addMultiValuesForKey(null, arrayListOf("a"))

        verify {
            eventQueueManager wasNot called
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_NullValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        analyticsManagerSUT.addMultiValuesForKey("abc", null)

        //Assert
        verify {
            validationResultStack.pushValidationResult(match {
                validationResult.errorCode == it.errorCode
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyValue_emptyValueError() {
        val validationResult = ValidationResult()
        validationResult.`object` = ""
        validationResult.errorCode = 512

        analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf())

        //Assert
        verify {
            validationResultStack.pushValidationResult(match {
                validationResult.errorCode == it.errorCode
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_RestrictedMultiValueKey_restrictedError() {
        val validationResult = ValidationResult()
        validationResult.`object` = null
        validationResult.errorCode = 523
        every {
            validator.cleanMultiValuePropertyKey("Name")
        } returns validationResult

        // Act
        analyticsManagerSUT.addMultiValuesForKey("Name", arrayListOf("a"))

        // Check
        verify(exactly = 2) {
            validationResultStack.pushValidationResult(match {
                validationResult.errorCode == it.errorCode
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_EmptyKey_emptyValueError() {
        mockCleanMultiValuePropertyKey("")

        analyticsManagerSUT.addMultiValuesForKey("", arrayListOf("a"))

        // Assert
        verify {
            validationResultStack.pushValidationResult(match {
                it.errorCode == 523
            })
        }
    }

    @Test
    fun test_addMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject().apply {
            put(Constants.COMMAND_ADD, JSONArray(arrayListOf("a")))
        }
        val fields = JSONObject().apply {
            put("abc", commandObj)
        }

        mockCleanMultiValuePropertyKey("abc")

        analyticsManagerSUT.addMultiValuesForKey("abc", arrayListOf("a"))

        // Assert
        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(fields, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_removeMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_REMOVE, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)
        mockCleanMultiValuePropertyKey("abc")

        analyticsManagerSUT.removeMultiValuesForKey("abc", arrayListOf("a"))

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(fields, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_setMultiValuesForKey_when_CorrectKey_pushesBasicProfile() {
        val commandObj = JSONObject()
        commandObj.put(Constants.COMMAND_SET, JSONArray(arrayListOf("a")))
        val fields = JSONObject()
        fields.put("abc", commandObj)

        mockCleanMultiValuePropertyKey("abc")

        analyticsManagerSUT.setMultiValuesForKey("abc", arrayListOf("a"))

        verify {
            eventQueueManager.pushBasicProfile(
                match { JSONCompare.compareJSON(fields, it, JSONCompareMode.STRICT).passed() },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_nullProfile_noAction() {
        analyticsManagerSUT.pushProfile(null)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_pushProfile_when_emptyProfile_noAction() {
        analyticsManagerSUT.pushProfile(emptyMap())

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_pushProfile_when_nullDeviceId_noAction() {
        val profile = mapOf("key1" to "value1", "key2" to "value2")
        every {
            coreState.deviceInfo.deviceID
        } returns null

        analyticsManagerSUT.pushProfile(profile)

        verify {
            validator wasNot called
        }
    }

    @Test
    fun test_pushProfile_when_validPhone_pushesProfile() {
        val validPhone = "+1234"
        val profile = mapOf("Phone" to validPhone)

        every {
            coreState.deviceInfo.deviceID
        } returns "1234"

        mockCleanObjectKey("Phone")
        mockCleanObjectValue(validPhone, Profile)

        analyticsManagerSUT.pushProfile(profile)

        val expectedJson = JSONObject().put("Phone", validPhone)
        verify {
            eventQueueManager.pushBasicProfile(
                match {
                    JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
                },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_invalidPhone_pushesErrorAndPushesProfile() {
        val invalidPhone = "1234"
        val profile = mapOf("Phone" to invalidPhone)

        mockCleanObjectKey("Phone")
        mockCleanObjectValue(invalidPhone, Profile)

        every { coreState.deviceInfo.deviceID } returns "1234"

        analyticsManagerSUT.pushProfile(profile)

        val expectedJson = JSONObject().put("Phone", invalidPhone)
        verify {
            validationResultStack.pushValidationResult(match { it.errorCode == 512 })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                match {
                    JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
                },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_invalidKeys_pushesPartialProfile() {
        val profile = mapOf("key1" to "value1", "" to "value2")

        mockCleanObjectKey("key1")
        mockCleanObjectKey("")

        mockCleanObjectValue("value1", Profile)
        mockCleanObjectValue("value2", Profile)

        every { coreState.deviceInfo.deviceID } returns "1234"

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Checks
        val expectedJson = JSONObject().put("key1", "value1")
        verify {
            validationResultStack.pushValidationResult(match { it.errorCode == 512 })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                match {
                    JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
                },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_nonPrimitiveValue_pushesPartialProfile() {
        val nonPrimitiveValue = Any()
        val profile = mapOf("key1" to nonPrimitiveValue, "key2" to "value2")

        mockCleanObjectKey("key1")
        mockCleanObjectKey("key2")
        mockCleanObjectValue("value2", Profile)

        every { coreState.deviceInfo.deviceID } returns "1234"

        every {
            validator.cleanObjectValue(
                nonPrimitiveValue,
                any()
            )
        } throws IllegalArgumentException()

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Checks
        val expectedJson = JSONObject().put("key2", "value2")
        verify {
            validationResultStack.pushValidationResult(match { it.errorCode == 512 })
        }
        verify {
            eventQueueManager.pushBasicProfile(
                match {
                    JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
                },
                any()
            )
        }
    }

    @Test
    fun test_pushProfile_when_validProfile_pushesCompleteProfile() {
        val profile = mapOf("key1" to "value1", "key2" to "value2")
        mockCleanObjectKey("key1")
        mockCleanObjectKey("key2")

        mockCleanObjectValue("value1", Profile)
        mockCleanObjectValue("value2", Profile)

        every { coreState.deviceInfo.deviceID } returns "1234"

        // Act
        analyticsManagerSUT.pushProfile(profile)

        // Verify
        val expectedJson = JSONObject()
            .put("key1", "value1")
            .put("key2", "value2")
        verify {
            eventQueueManager.pushBasicProfile(
                match {
                    JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
                },
                any()
            )
        }
    }

    @Test
    fun `pushAppLaunchedEvent should not queue event when app launched event is disabled`() {
        cleverTapInstanceConfig.isDisableAppLaunchedEvent = true
        analyticsManagerSUT.pushAppLaunchedEvent()
        assertTrue(coreState.coreMetaData.isAppLaunchPushed)
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushAppLaunchedEvent should not queue event when app launched event is already pushed`() {
        coreState.coreMetaData.isAppLaunchPushed = true
        analyticsManagerSUT.pushAppLaunchedEvent()
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushAppLaunchedEvent should queue app launched event if it was not already pushed`() {
        coreState.coreMetaData.isAppLaunchPushed = false
        analyticsManagerSUT.pushAppLaunchedEvent()
        verify(exactly = 1) {
            eventQueueManager.queueEvent(any(), match { event ->
                event.getString(Constants.KEY_EVT_NAME) == Constants.APP_LAUNCHED_EVENT
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `forcePushAppLaunchedEvent should queue app launched event even if it was already pushed`() {
        coreState.coreMetaData.isAppLaunchPushed = true
        analyticsManagerSUT.forcePushAppLaunchedEvent()
        verify(exactly = 1) {
            eventQueueManager.queueEvent(any(), match { event ->
                event.getString(Constants.KEY_EVT_NAME) == Constants.APP_LAUNCHED_EVENT
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushDefineVarsEvent should queue define vars event`() {
        val json = JSONObject()
        analyticsManagerSUT.pushDefineVarsEvent(json)
        verify(exactly = 1) {
            eventQueueManager.queueEvent(any(), match { data ->
                json.toString() == data.toString()
            }, Constants.DEFINE_VARS_EVENT)
        }
    }

    @Test
    fun `pushDisplayUnitClickedEventForID should queue notification clicked event`() {
        verifyDisplayUnitEventForId(isClicked = true)
    }

    @Test
    fun `pushDisplayUnitViewedEventForID should queue notification viewed event`() {
        verifyDisplayUnitEventForId(isClicked = false)
    }

    private fun verifyDisplayUnitEventForId(isClicked: Boolean) {
        val displayController = mockk<CTDisplayUnitController>()
        val displayUnitJson = JSONObject()
        val displayUnit = CleverTapDisplayUnit.toDisplayUnit(displayUnitJson)
        every { displayController.getDisplayUnitForID(any()) } returns displayUnit
        every { coreState.controllerManager.ctDisplayUnitController } returns displayController

        val eventName: String
        if (isClicked) {
            eventName = Constants.NOTIFICATION_CLICKED_EVENT_NAME
            mockCleanEventName(eventName)
            analyticsManagerSUT.pushDisplayUnitClickedEventForID("id")
        } else {
            eventName = Constants.NOTIFICATION_VIEWED_EVENT_NAME
            mockCleanEventName(eventName)
            analyticsManagerSUT.pushDisplayUnitViewedEventForID("id")
        }

        verify(exactly = 1) {
            eventQueueManager.queueEvent(any(), match { event ->
                event.getString(Constants.KEY_EVT_NAME) == eventName
                        && event.getJSONObject(Constants.KEY_EVT_DATA)
                    .toString() == displayUnitJson.toString()
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushError should queue error event with activity name`() {
        mockkStatic(CoreMetaData::class) {
            val eventName = "Error Occurred"
            mockCleanEventName(eventName)
            val locationKey = "Location"
            mockCleanObjectKey(locationKey)
            val messageKey = "Error Message"
            mockCleanObjectKey(messageKey)
            val codeKey = "Error Code"
            mockCleanObjectKey(codeKey)
            val activityName = "activity"
            mockCleanObjectValue(activityName, Validator.ValidationContext.Event)
            every { CoreMetaData.getCurrentActivityName() } returns activityName

            val errorMessage = "message"
            mockCleanObjectValue(errorMessage, Validator.ValidationContext.Event)
            val errorCode = 10
            mockCleanObjectValue(errorCode, Validator.ValidationContext.Event)
            analyticsManagerSUT.pushError(errorMessage, errorCode)

            verify(exactly = 1) {
                eventQueueManager.queueEvent(context, match { event ->
                    val data = event.getJSONObject(Constants.KEY_EVT_DATA)
                    event.getString(Constants.KEY_EVT_NAME) == eventName
                            && data.getString(locationKey) == activityName
                            && data.getString(messageKey) == errorMessage
                            && data.getInt(codeKey) == errorCode
                }, Constants.RAISED_EVENT)
            }
        }

    }

    @Test
    fun `fetchFeatureFlags should do nothing when analytics is disabled`() {
        cleverTapInstanceConfig.isAnalyticsOnly = true
        analyticsManagerSUT.fetchFeatureFlags()
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `fetchFeatureFlags should queue fetch event when analytics is enabled`() {
        cleverTapInstanceConfig.isAnalyticsOnly = false
        val expectedJson = JSONObject()
        val notif = JSONObject()
        notif.put(Constants.KEY_T, Constants.FETCH_TYPE_FF)
        expectedJson.put(Constants.KEY_EVT_NAME, Constants.WZRK_FETCH)
        expectedJson.put(Constants.KEY_EVT_DATA, notif)

        analyticsManagerSUT.fetchFeatureFlags()

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
            }, Constants.FETCH_EVENT)
        }
    }

    @Test
    fun `sendPingEvent should queue ping event`() {
        val event = JSONObject().apply { put("test", "test") }

        analyticsManagerSUT.sendPingEvent(event)
        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, event, Constants.PING_EVENT)
        }
    }

    @Test
    fun `sendDataEvent should queue data event`() {
        val event = JSONObject().apply { put("test", "test") }
        analyticsManagerSUT.sendDataEvent(event)
        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, event, Constants.DATA_EVENT)
        }
    }

    @Test
    fun `pushEvent should not queue event when event name is null or empty`() {
        analyticsManagerSUT.pushEvent(null, emptyMap())
        analyticsManagerSUT.pushEvent("", emptyMap())
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushEvent should not queue event when event name is restricted`() {
        val eventName = "restricted"
        every { validator.isRestrictedEventName(eventName) } returns ValidationResult().apply {
            errorCode = 1
            errorDesc = "Restricted event name"
        }
        analyticsManagerSUT.pushEvent(eventName, emptyMap())
        verify { validationResultStack.pushValidationResult(any()) }
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushEvent should not queue event when event is discarded`() {
        val eventName = "discarded"
        every { validator.isEventDiscarded(eventName) } returns ValidationResult().apply {
            errorCode = 1
            errorDesc = "Discarded event"
        }
        analyticsManagerSUT.pushEvent(eventName, emptyMap())
        verify { validationResultStack.pushValidationResult(any()) }
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushEvent should queue event with empty actions when eventActions is null`() {
        val eventName = "Test Event"
        mockCleanEventName(eventName)

        analyticsManagerSUT.pushEvent(eventName, null)

        val expectedJson = JSONObject()
        expectedJson.put(Constants.KEY_EVT_NAME, eventName)
        expectedJson.put(Constants.KEY_EVT_DATA, JSONObject())

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushEvent should queue event with valid name and actions`() {
        val eventName = "Test Event"
        val prop1 = "prop1"
        val value1 = "value1"
        val prop2 = "prop2"
        val value2 = 123
        val eventActions = mapOf(prop1 to value1, prop2 to value2)
        mockCleanEventName(eventName)
        mockCleanObjectKey(prop1)
        mockCleanObjectKey(prop2)
        mockCleanObjectValue(value1, Validator.ValidationContext.Event)
        mockCleanObjectValue(value2, Validator.ValidationContext.Event)

        analyticsManagerSUT.pushEvent(eventName, eventActions)

        val expectedJson = JSONObject()
        expectedJson.put(Constants.KEY_EVT_NAME, eventName)
        val expectedActions = JSONObject()
            .put(prop1, value1)
            .put(prop2, value2)
        expectedJson.put(Constants.KEY_EVT_DATA, expectedActions)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedJson, it, JSONCompareMode.STRICT).passed()
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushEvent should queue event with error when action key is invalid`() {
        val eventName = "Test Event"
        val actionKey = "action"
        val actionValue = "value"
        val eventActions = mapOf(actionKey to actionValue)
        mockCleanEventName(eventName)
        mockCleanObjectKey(actionKey, 512)
        mockCleanObjectValue(actionValue, Validator.ValidationContext.Event)

        analyticsManagerSUT.pushEvent(eventName, eventActions)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                it.has(Constants.ERROR_KEY)
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushEvent should queue event with error when action value is invalid`() {
        val eventName = "Test Event"
        val actionKey = "action"
        val actionValue = "value"
        val eventActions = mapOf(actionKey to actionValue)
        mockCleanEventName(eventName)
        mockCleanObjectKey(actionKey)
        mockCleanObjectValue(actionValue, Validator.ValidationContext.Event, 512)

        analyticsManagerSUT.pushEvent(eventName, eventActions)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                it.has(Constants.ERROR_KEY)
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushChargedEvent should not queue event when details are null`() {
        analyticsManagerSUT.pushChargedEvent(null, arrayListOf())
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushChargedEvent should not queue event when items are null`() {
        analyticsManagerSUT.pushChargedEvent(hashMapOf(), null)
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushChargedEvent should push validation error when items size is greater than 50`() {
        val items = ArrayList<HashMap<String, Any>>(51)
        val map = hashMapOf<String, Any>()
        repeat(51) {
            items.add(map)
        }
        analyticsManagerSUT.pushChargedEvent(hashMapOf(), items)
        verify {
            validationResultStack.pushValidationResult(match {
                it.errorCode == 522
            })
        }
    }

    @Test
    fun `pushChargedEvent should queue charged event`() {
        val amountKey = "Amount"
        val amountValue = 300
        val nameKey = "Name"
        val name1 = "name1"
        val name2 = "name2"
        val chargeDetails = hashMapOf<String, Any>(amountKey to amountValue)
        val item1 = hashMapOf<String, Any>(nameKey to name1)
        val item2 = hashMapOf<String, Any>(nameKey to name2)
        val items = arrayListOf(item1, item2)

        mockCleanObjectKey(amountKey)
        mockCleanObjectKey(nameKey)
        mockCleanObjectValue(amountValue, Validator.ValidationContext.Event)
        mockCleanObjectValue(amountValue, Validator.ValidationContext.Event)
        mockCleanObjectValue(name1, Validator.ValidationContext.Event)
        mockCleanObjectValue(name2, Validator.ValidationContext.Event)

        analyticsManagerSUT.pushChargedEvent(chargeDetails, items)

        val expectedEvtData = JSONObject()
            .put(amountKey, amountValue)
        val expectedItems = JSONArray()
            .put(JSONObject().put(nameKey, name1))
            .put(JSONObject().put(nameKey, name2))
        expectedEvtData.put(Constants.KEY_ITEMS, expectedItems)

        val expectedEvent = JSONObject()
            .put(Constants.KEY_EVT_NAME, Constants.CHARGED_EVENT)
            .put(Constants.KEY_EVT_DATA, expectedEvtData)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedEvent, it, JSONCompareMode.STRICT).passed()
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `pushInAppNotificationStateEvent for click should queue notification clicked event`() {
        verifyPushInAppNotificationStateEvent(true)
    }

    @Test
    fun `pushInAppNotificationStateEvent for view should queue notification viewed event`() {
        verifyPushInAppNotificationStateEvent(false)
    }

    private fun verifyPushInAppNotificationStateEvent(isClicked: Boolean) {
        mockkStatic(CTJsonConverter::class) {
            val inAppNotification = mockk<CTInAppNotification>(relaxed = true)
            val wzrkFields = JSONObject().apply { put("test", "test") }
            every { CTJsonConverter.getWzrkFields(inAppNotification) } returns wzrkFields

            val customData = Bundle().apply { putString("key", "value") }

            analyticsManagerSUT.pushInAppNotificationStateEvent(
                isClicked,
                inAppNotification,
                customData
            )

            val expectedEvtData = JSONObject()
            expectedEvtData.put("test", "test")
            expectedEvtData.put("key", "value")

            val expectedEvent = JSONObject()
            val eventName = if (isClicked) {
                Constants.NOTIFICATION_CLICKED_EVENT_NAME
            } else {
                Constants.NOTIFICATION_VIEWED_EVENT_NAME
            }
            expectedEvent.put(Constants.KEY_EVT_NAME, eventName)
            expectedEvent.put(Constants.KEY_EVT_DATA, expectedEvtData)

            verify(exactly = 1) {
                eventQueueManager.queueEvent(context, match {
                    JSONCompare.compareJSON(expectedEvent, it, JSONCompareMode.STRICT).passed()
                }, Constants.RAISED_EVENT)
            }
            if (isClicked) {
                assertEquals(wzrkFields, coreState.coreMetaData.wzrkParams)
            }
        }
    }

    @Test
    fun `pushInboxMessageStateEvent for click should queue notification clicked event`() {
        verifyPushInboxMessageStateEvent(true)
    }

    @Test
    fun `pushInboxMessageStateEvent for view should queue notification viewed event`() {
        verifyPushInboxMessageStateEvent(false)
    }

    private fun verifyPushInboxMessageStateEvent(isClicked: Boolean) {
        mockkStatic(CTJsonConverter::class) {
            val inboxMessage = mockk<CTInboxMessage>(relaxed = true)
            val wzrkFields = JSONObject().apply { put("test", "test") }
            every { CTJsonConverter.getWzrkFields(inboxMessage) } returns wzrkFields

            val customData = Bundle().apply { putString("key", "value") }

            analyticsManagerSUT.pushInboxMessageStateEvent(isClicked, inboxMessage, customData)

            val expectedEvtData = JSONObject()
            expectedEvtData.put("test", "test")
            expectedEvtData.put("key", "value")

            val expectedEvent = JSONObject()
            val eventName = if (isClicked) {
                Constants.NOTIFICATION_CLICKED_EVENT_NAME
            } else {
                Constants.NOTIFICATION_VIEWED_EVENT_NAME
            }
            expectedEvent.put(Constants.KEY_EVT_NAME, eventName)
            expectedEvent.put(Constants.KEY_EVT_DATA, expectedEvtData)

            verify(exactly = 1) {
                eventQueueManager.queueEvent(context, match {
                    JSONCompare.compareJSON(expectedEvent, it, JSONCompareMode.STRICT).passed()
                }, Constants.RAISED_EVENT)
            }
            if (isClicked) {
                assertEquals(wzrkFields, coreState.coreMetaData.wzrkParams)
            }
        }
    }

    @Test
    fun `raiseEventForGeofences should queue event and set location`() {
        val eventName = "geofence_event"
        val lat = 34.05
        val lng = -118.25
        val propKey = "prop"
        val propValue = "value"
        val geofenceProperties = JSONObject().apply {
            put("triggered_lat", lat)
            put("triggered_lng", lng)
            put(propKey, propValue)
        }

        val future = mockk<Future<*>>()
        every { eventQueueManager.queueEvent(any(), any(), any()) } returns future

        val result = analyticsManagerSUT.raiseEventForGeofences(eventName, geofenceProperties)

        assertEquals(future, result)

        val expectedEventData = JSONObject().apply {
            put(propKey, propValue)
        }
        val expectedEvent = JSONObject().apply {
            put(Constants.KEY_EVT_NAME, eventName)
            put(Constants.KEY_EVT_DATA, expectedEventData)
        }

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedEvent, it, JSONCompareMode.STRICT).passed()
            }, Constants.RAISED_EVENT)
        }

        val location = coreState.coreMetaData.locationFromUser
        assertEquals(lat, location.latitude)
        assertEquals(lng, location.longitude)
    }

    @Test
    fun `raiseEventForSignedCall should queue event`() {
        val eventName = "signed_call_event"
        val eventProperties = JSONObject().apply {
            put("prop", "value")
        }

        val future = mockk<Future<*>>()
        every { eventQueueManager.queueEvent(any(), any(), any()) } returns future

        val result = analyticsManagerSUT.raiseEventForSignedCall(eventName, eventProperties)

        assertEquals(future, result)

        val expectedEvent = JSONObject().apply {
            put(Constants.KEY_EVT_NAME, eventName)
            put(Constants.KEY_EVT_DATA, eventProperties)
        }

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(expectedEvent, it, JSONCompareMode.STRICT).passed()
            }, Constants.RAISED_EVENT)
        }
    }

    @Test
    fun `recordPageEventWithExtras should queue page event`() {
        val extras = JSONObject().apply {
            put("key1", "value1")
            put("key2", "value2")
        }

        analyticsManagerSUT.recordPageEventWithExtras(extras)

        verify(exactly = 1) {
            eventQueueManager.queueEvent(context, match {
                JSONCompare.compareJSON(extras, it, JSONCompareMode.STRICT).passed()
            }, Constants.PAGE_EVENT)
        }
    }

    @Test
    fun `pushDeepLink should queue page event with referrer data`() {
        mockkStatic(UriHelper::class) {
            val uri = mockk<Uri>()
            val referrer = JSONObject().apply {
                put("us", "source")
                put("um", "medium")
                put("uc", "campaign")
            }
            every { UriHelper.getUrchinFromUri(uri) } returns referrer
            every { uri.toString() } returns "wzrk://test"

            analyticsManagerSUT.pushDeepLink(uri, true)

            val expectedExtras = JSONObject().apply {
                put("us", "source")
                put("um", "medium")
                put("uc", "campaign")
                put("referrer", "wzrk://test")
                put("install", "true")
            }

            verify(exactly = 1) {
                eventQueueManager.queueEvent(context, match {
                    JSONCompare.compareJSON(expectedExtras, it, JSONCompareMode.STRICT).passed()
                }, Constants.PAGE_EVENT)
            }

            assertEquals("source", coreState.coreMetaData.source)
            assertEquals("medium", coreState.coreMetaData.medium)
            assertEquals("campaign", coreState.coreMetaData.campaign)
        }
    }

    @Test
    fun `pushInstallReferrer with url should not queue event if url is null`() {
        analyticsManagerSUT.pushInstallReferrer(null)
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushInstallReferrer with url should not queue event if called twice within 10 seconds`() {
        mockkStatic(UriHelper::class, Uri::class) {
            every { Uri.parse(any()) } returns mockk()
            every { UriHelper.getUrchinFromUri(any()) } returns JSONObject()

            analyticsManagerSUT.pushInstallReferrer("http://test.com")
            analyticsManagerSUT.pushInstallReferrer("http://test.com")

            verify(exactly = 1) { eventQueueManager.queueEvent(any(), any(), any()) }
        }
    }

    @Test
    fun `pushInstallReferrer with url should queue event`() {
        mockkStatic(UriHelper::class, Uri::class) {
            val uri = mockk<Uri>()
            every { Uri.parse("wzrk://track?install=true&http://test.com") } returns uri
            every { UriHelper.getUrchinFromUri(uri) } returns JSONObject()
            every { uri.toString() } returns "wzrk://track?install=true&http://test.com"

            analyticsManagerSUT.pushInstallReferrer("http://test.com")

            verify(exactly = 1) { eventQueueManager.queueEvent(any(), any(), any()) }
        }
    }

    @Test
    fun `pushInstallReferrer with params should not queue event if all params are null`() {
        analyticsManagerSUT.pushInstallReferrer(null, null, null)
        verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
    }

    @Test
    fun `pushInstallReferrer with params should not queue event if already pushed`() {
        mockkStatic(StorageHelper::class) {
            every { StorageHelper.getInt(context, "app_install_status", 0) } returns 1

            analyticsManagerSUT.pushInstallReferrer("source", "medium", "campaign")

            verify(exactly = 0) { eventQueueManager.queueEvent(any(), any(), any()) }
        }
    }

    @Test
    fun `pushInstallReferrer with params should queue event`() {
        mockkStatic(StorageHelper::class, UriHelper::class, Uri::class) {
            every { StorageHelper.getInt(context, "app_install_status", 0) } returns 0
            every { StorageHelper.putInt(context, "app_install_status", 1) } returns Unit

            val uri = mockk<Uri>()
            every { Uri.parse("wzrk://track?install=true&utm_source=source&utm_medium=medium&utm_campaign=campaign") } returns uri
            every { UriHelper.getUrchinFromUri(uri) } returns JSONObject()
            every { uri.toString() } returns "wzrk://track?install=true&utm_source=source&utm_medium=medium&utm_campaign=campaign"

            analyticsManagerSUT.pushInstallReferrer("source", "medium", "campaign")

            verify(exactly = 1) { eventQueueManager.queueEvent(any(), any(), any()) }
        }
    }

    @Test
    fun `pushNotificationClickedEvent should handle in-app preview`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "test")
            putString(Constants.WZRK_ACCT_ID_KEY, cleverTapInstanceConfig.accountId)
            putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, "{}")
        }
        analyticsManagerSUT.pushNotificationClickedEvent(extras)
        verify { inAppPreviewHandler.handleInAppPreview(extras) }
    }

    @Test
    fun `pushNotificationClickedEvent should handle inbox preview`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "test")
            putString(Constants.WZRK_ACCT_ID_KEY, cleverTapInstanceConfig.accountId)
            putString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY, "{\"msg\":[]}")
        }
        val inboxController = mockk<CTInboxController>(relaxed = true)
        every { coreState.controllerManager.ctInboxController } returns inboxController

        analyticsManagerSUT.pushNotificationClickedEvent(extras)

        verify { inboxController.updateMessages(any()) }
    }


    private fun mockCleanObjectKey(key: String?, errCode: Int = 0) {
        every {
            validator.cleanObjectKey(key)
        } returns ValidationResult().apply {
            `object` = key
            errorCode = errCode
        }
    }

    private fun mockCleanEventName(name: String?, errCode: Int = 0) {
        every {
            validator.cleanEventName(name)
        } returns ValidationResult().apply {
            `object` = name
            errorCode = errCode
        }
    }

    private fun mockCleanObjectValue(
        value: Any?,
        context: Validator.ValidationContext,
        errCode: Int = 0
    ) {
        every {
            validator.cleanObjectValue(value, context)
        } returns ValidationResult().apply {
            `object` = value
            errorCode = errCode
        }
    }

    private fun mockCleanMultiValuePropertyKey(key: String?, errCode: Int = 0) {
        every {
            validator.cleanMultiValuePropertyKey(key)
        } returns ValidationResult().apply {
            `object` = key
            errorCode = errCode
        }
    }
}
