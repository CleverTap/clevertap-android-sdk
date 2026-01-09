package com.clevertap.android.sdk.validation

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals


@RunWith(RobolectricTestRunner::class)
class ValidationResultFactoryTest : BaseTestCase() {

    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_create_withValidationError_should_returnAppropriateValidationResult() {
        val testCases = getTestCases()

        testCases.forEach { testCase ->
            println("Testing: ${testCase.error.name}")
            val result = ValidationResultFactory.create(testCase.error, *testCase.values)
            
            assertEquals(testCase.expectedCode, result.errorCode, "Error code mismatch for ${testCase.error.name}")
            assertEquals(testCase.expectedMessage, result.errorDesc, "Error message mismatch for ${testCase.error.name}")
            
            println("Result: ${result.errorDesc}\n--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--")
        }
    }

    @Test
    fun test_create_withNoValues_should_returnMessageWithoutFormatting() {
        // Test errors that don't require values
        val noValueErrors = listOf(
            ValidationError.EVENT_NAME_NULL to "Event Name is null/empty",
            ValidationError.CHARGED_EVENT_TOO_MANY_ITEMS to "Charged event contained more than 50 items.",
            ValidationError.PROFILE_IDENTIFIERS_MISMATCH to "Profile Identifiers mismatch with the previously saved ones",
            ValidationError.USE_CUSTOM_ID_FALLBACK to "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID",
            ValidationError.USE_CUSTOM_ID_MISSING_IN_MANIFEST to "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used."
        )

        noValueErrors.forEach { (error, expectedMessage) ->
            val result = ValidationResultFactory.create(error)
            assertEquals(expectedMessage, result.errorDesc, "Message mismatch for ${error.name} without values")
            assertEquals(error.code, result.errorCode, "Error code mismatch for ${error.name}")
        }
    }

    private fun getTestCases(): List<ValidationTestCase> {
        return listOf(
            // Event name validation errors (510)
            ValidationTestCase(
                ValidationError.EVENT_NAME_NULL,
                510,
                arrayOf(),
                "Event Name is null/empty"
            ),
            ValidationTestCase(
                ValidationError.EVENT_NAME_TOO_LONG,
                510,
                arrayOf("LongEventName", "100"),
                "LongEventName... exceeds the limit of 100 characters. Trimmed"
            ),
            ValidationTestCase(
                ValidationError.EVENT_NAME_INVALID_CHARACTERS,
                510,
                arrayOf("Invalid@Event"),
                "Key 'Invalid@Event' contains invalid characters. Cleaned"
            ),
            
            // Profile validation errors (512)
            ValidationTestCase(
                ValidationError.INVALID_COUNTRY_CODE,
                512,
                arrayOf("+1234567890"),
                "Device country code not available and profile phone: +1234567890 does not appear to start with country code"
            ),
            ValidationTestCase(
                ValidationError.INVALID_PHONE,
                512,
                arrayOf(),
                "Invalid phone number"
            ),
            ValidationTestCase(
                ValidationError.EMPTY_KEY,
                512,
                arrayOf(),
                "Found an empty key. Skipping and continuing"
            ),
            ValidationTestCase(
                ValidationError.EMPTY_KEY_ABORT,
                512,
                arrayOf(),
                "Found an empty key. Aborting the operation"
            ),
            ValidationTestCase(
                ValidationError.PROP_VALUE_NOT_PRIMITIVE,
                512,
                arrayOf("propertyName", "Object"),
                "Property value for property propertyName wasn't a primitive (Object)"
            ),
            ValidationTestCase(
                ValidationError.CHANNEL_ID_MISSING_IN_PAYLOAD,
                512,
                arrayOf("default_channel"),
                "ChannelId is required for API 26+ but not provided in the notification payload. Falling to default channel: default_channel"
            ),
            ValidationTestCase(
                ValidationError.CHANNEL_ID_NOT_REGISTERED,
                512,
                arrayOf("my_channel"),
                "Unable to render notification on channelId: my_channel as it is not registered by the app. Falling to default channel. "
            ),
            ValidationTestCase(
                ValidationError.NOTIFICATION_VIEWED_DISABLED,
                512,
                arrayOf("payload123"),
                "Recording of Notification Viewed is disabled in the CleverTap Dashboard for notification payload: payload123"
            ),
            
            // Event name restrictions (513)
            ValidationTestCase(
                ValidationError.RESTRICTED_EVENT_NAME,
                513,
                arrayOf("Stayed"),
                "Stayed is a restricted event name. Last event aborted."
            ),
            ValidationTestCase(
                ValidationError.DISCARDED_EVENT_NAME,
                513,
                arrayOf("App Launched"),
                "App Launched is a discarded event name. Last event aborted."
            ),
            
            // Custom ID errors (514)
            ValidationTestCase(
                ValidationError.USE_CUSTOM_ID_FALLBACK,
                514,
                arrayOf(),
                "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID"
            ),
            ValidationTestCase(
                ValidationError.USE_CUSTOM_ID_MISSING_IN_MANIFEST,
                514,
                arrayOf(),
                "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used."
            ),
            ValidationTestCase(
                ValidationError.UNABLE_TO_SET_CT_CUSTOM_ID,
                514,
                arrayOf("existing_id", "new_id"),
                "CleverTap ID - existing_id already exists. Unable to set custom CleverTap ID - new_id"
            ),
            ValidationTestCase(
                ValidationError.INVALID_CT_CUSTOM_ID,
                514,
                arrayOf("invalid_id", "fallback_id"),
                "Attempted to set invalid custom CleverTap ID - invalid_id, falling back to default error CleverTap ID - fallback_id"
            ),
            
            // Key validation errors (520)
            ValidationTestCase(
                ValidationError.KEY_INVALID_CHARACTERS,
                520,
                arrayOf("invalid@key"),
                "Key 'invalid@key' contains invalid characters. Cleaned"
            ),
            ValidationTestCase(
                ValidationError.KEY_LENGTH_EXCEEDED,
                520,
                arrayOf("VeryLongKeyName", "50"),
                "Key 'VeryLongKeyName' exceeds the limit of 50 characters. Trimmed"
            ),
            
            // Value validation errors (521)
            ValidationTestCase(
                ValidationError.VALUE_CHARS_LIMIT_EXCEEDED,
                521,
                arrayOf("VeryLongValue", "200"),
                "VeryLongValue... exceeds the limit of 200 characters. Trimmed"
            ),
            ValidationTestCase(
                ValidationError.VALUE_INVALID_CHARACTERS,
                521,
                arrayOf("keyName"),
                "Value for key 'keyName' contains invalid characters. Cleaned"
            ),
            
            // Charged event errors (522)
            ValidationTestCase(
                ValidationError.CHARGED_EVENT_TOO_MANY_ITEMS,
                522,
                arrayOf(),
                "Charged event contained more than 50 items."
            ),
            
            // Multi-value key errors (523)
            ValidationTestCase(
                ValidationError.RESTRICTED_MULTI_VALUE_KEY,
                523,
                arrayOf("restrictedKey"),
                "restrictedKey... is a restricted key for multi-value properties. Dropped."
            ),
            
            // Profile identifier errors (531)
            ValidationTestCase(
                ValidationError.PROFILE_IDENTIFIERS_MISMATCH,
                531,
                arrayOf(),
                "Profile Identifiers mismatch with the previously saved ones"
            ),
            
            // Structural limit errors (540-544)
            ValidationTestCase(
                ValidationError.DEPTH_LIMIT_EXCEEDED,
                540,
                arrayOf("10", "5"),
                "Event data exceeded maximum nesting depth. Depth: 10, Limit: 5"
            ),
            ValidationTestCase(
                ValidationError.ARRAY_KEY_COUNT_LIMIT_EXCEEDED,
                541,
                arrayOf("100", "50"),
                "Event data exceeded maximum array key count. Count: 100, Limit: 50"
            ),
            ValidationTestCase(
                ValidationError.OBJECT_KEY_COUNT_LIMIT_EXCEEDED,
                542,
                arrayOf("75", "50"),
                "Event data exceeded maximum object key count. Count: 75, Limit: 50"
            ),
            ValidationTestCase(
                ValidationError.ARRAY_LENGTH_LIMIT_EXCEEDED,
                543,
                arrayOf("200", "100"),
                "Event data exceeded maximum array length. Length: 200, Limit: 100"
            ),
            ValidationTestCase(
                ValidationError.KV_PAIR_COUNT_LIMIT_EXCEEDED,
                544,
                arrayOf("150", "100"),
                "Event data exceeded maximum key-value pair count. Count: 150, Limit: 100"
            ),
            
            // Data removal errors (545)
            ValidationTestCase(
                ValidationError.NULL_VALUE_REMOVED,
                545,
                arrayOf("nullKey"),
                "Null value for key 'nullKey' was removed"
            ),
            ValidationTestCase(
                ValidationError.EMPTY_VALUE_REMOVED,
                545,
                arrayOf("emptyKey"),
                "Empty value for key 'emptyKey' was removed"
            )
        )
    }

    data class ValidationTestCase(
        val error: ValidationError,
        val expectedCode: Int,
        val values: Array<String>,
        val expectedMessage: String
    )

}


