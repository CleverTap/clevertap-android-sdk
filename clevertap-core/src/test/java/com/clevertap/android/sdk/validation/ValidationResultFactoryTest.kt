package com.clevertap.android.sdk.validation

import com.clevertap.android.sdk.Constants
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
    fun test_create_whenFunctionIsCalledWithParams_should_returnAppropiateValidationResult() {
        val inputsAndExpectedOutputs = getIOArray()

        inputsAndExpectedOutputs.forEach {
            println("input:  ec : ${it.errorCode}, mc: ${it.messageCode}")
            val result = ValidationResultFactory.create(it.errorCode,it.messageCode, *it.values)
            assertEquals(it.expectedMessage,result.errorDesc)
            assertEquals(it.errorCode,result.errorCode)
            println("result:${result.errorDesc} \n--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--")
        }

        // since this function can also be called without a message code, in which case no message will be generated(except for 522 and 531)
        inputsAndExpectedOutputs.forEach {
            println("input:  ec : ${it.errorCode}, mc:not passing it")
            val result = ValidationResultFactory.create(it.errorCode,)
            assertEquals(it.errorCode,result.errorCode)
            if(it.errorCode in arrayOf(522,531)){
                assertEquals(it.expectedMessage,result.errorDesc)
            }
            else{
                assertEquals("",result.errorDesc)
            }
            println("result:${result.errorDesc} \n--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--\t--")
        }


    }

    private fun getIOArray(): Array<ValidationIO> {
        val key = "key1"
        val key2 = "key2"
        val key3 = "key3"
        val keysArr = arrayOf(key, key2, key3)

        return arrayOf(
            ValidationIO(512, Constants.INVALID_MULTI_VALUE, keysArr, "Invalid multi value for key $key, profile multi value operation aborted."),
            ValidationIO(512, Constants.INVALID_INCREMENT_DECREMENT_VALUE, keysArr, "Increment/Decrement value for profile key $key, cannot be zero or negative"),
            ValidationIO(512, Constants.PUSH_KEY_EMPTY, keysArr, "Profile push key is empty"),
            ValidationIO(512, Constants.OBJECT_VALUE_NOT_PRIMITIVE_PROFILE, keysArr, "Object value wasn't a primitive ($key) for profile field $key2"),
            ValidationIO(512, Constants.INVALID_COUNTRY_CODE, keysArr, "Device country code not available and profile phone: $key does not appear to start with country code"),
            ValidationIO(512, Constants.INVALID_PHONE, keysArr, "Invalid phone number"),
            ValidationIO(512, Constants.KEY_EMPTY, keysArr, "Key is empty, profile removeValueForKey aborted."),
            ValidationIO(512, Constants.PROP_VALUE_NOT_PRIMITIVE, keysArr, "For event \"$key\": Property value for property $key2 wasn't a primitive ($key3)"),
            ValidationIO(512, Constants.CHANNEL_ID_MISSING_IN_PAYLOAD, keysArr, "Unable to render notification, channelId is required but not provided in the notification payload: $key"),
            ValidationIO(512, Constants.CHANNEL_ID_NOT_REGISTERED, keysArr, "Unable to render notification, channelId: $key not registered by the app."),
            ValidationIO(512, Constants.NOTIFICATION_VIEWED_DISABLED, keysArr, "Recording of Notification Viewed is disabled in the CleverTap Dashboard for notification payload: $key"),
            ValidationIO(521, Constants.VALUE_CHARS_LIMIT_EXCEEDED, keysArr, "$key... exceeds the limit of $key2 characters. Trimmed"),
            ValidationIO(521, Constants.MULTI_VALUE_CHARS_LIMIT_EXCEEDED, keysArr, "Multi value property for key $key exceeds the limit of $key2 items. Trimmed"),
            ValidationIO(521, Constants.INVALID_PROFILE_PROP_ARRAY_COUNT, keysArr, "Invalid user profile property array count - $key max is - $key2"),
            ValidationIO(520, Constants.VALUE_CHARS_LIMIT_EXCEEDED, keysArr, "$key... exceeds the limit of $key2 characters. Trimmed"),
            ValidationIO(520, Constants.EVENT_NAME_NULL, keysArr, "Event Name is null"),
            ValidationIO(510, Constants.VALUE_CHARS_LIMIT_EXCEEDED, keysArr, "$key... exceeds the limit of $key2 characters. Trimmed"),
            ValidationIO(510, Constants.EVENT_NAME_NULL, keysArr, "Event Name is null"),
            ValidationIO(511, Constants.PROP_VALUE_NOT_PRIMITIVE, keysArr, "For event $key: Property value for property $key2 wasn't a primitive ($key3)"),
            ValidationIO(511, Constants.OBJECT_VALUE_NOT_PRIMITIVE, keysArr, "An item's object value for key $key wasn't a primitive ($key2)"),
            ValidationIO(513, Constants.RESTRICTED_EVENT_NAME, keysArr, "$key is a restricted event name. Last event aborted."),
            ValidationIO(513, Constants.DISCARDED_EVENT_NAME, keysArr, "$key is a discarded event name. Last event aborted."),
            ValidationIO(514, Constants.USE_CUSTOM_ID_FALLBACK, keysArr, "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID"),
            ValidationIO(514, Constants.USE_CUSTOM_ID_MISSING_IN_MANIFEST, keysArr, "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used."),
            ValidationIO(514, Constants.UNABLE_TO_SET_CT_CUSTOM_ID, keysArr, "CleverTap ID - $key already exists. Unable to set custom CleverTap ID - $key2"),
            ValidationIO(514, Constants.INVALID_CT_CUSTOM_ID, keysArr, "Attempted to set invalid custom CleverTap ID - $key, falling back to default error CleverTap ID - $key2"),
            ValidationIO(522, -1, keysArr, "Charged event contained more than 50 items."),
            ValidationIO(523, Constants.INVALID_MULTI_VALUE_KEY, keysArr, "Invalid multi-value property key $key"),
            ValidationIO(523, Constants.RESTRICTED_MULTI_VALUE_KEY, keysArr, "$key... is a restricted key for multi-value properties. Operation aborted."),
            ValidationIO(531, -1, keysArr, "Profile Identifiers mismatch with the previously saved ones"),
        )
    }


    data class ValidationIO(val errorCode: Int, val messageCode: Int, val values: Array<String> = arrayOf(""),  val expectedMessage: String)

}


