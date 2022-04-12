package com.clevertap.android.sdk.validation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.util.*
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class ValidatorTest : BaseTestCase() {
    private lateinit var validator: Validator

    override fun setUp() {
        super.setUp()
        validator = Validator()
    }

    @Test
    fun test_cleanEventName_when_StringIsPassed_should_ReturnAnObjectOfValidationResultWithCleanedString() {
        // when event name does not contain [".", ":", "$", "'", "\"", "\\"] and length less than 512 chars, then only the whitespaces in start and end are removed in the validation result
        var eventName = "     Ideal Name   \n  "
        var result = validator.cleanEventName(eventName)
        assertNull(result.errorDesc)
        assertEquals(0,result.errorCode)
        assertEquals("Ideal Name",result.`object`)


        // when event contains more than 512 chars(excluding whitespaces and illegal chars) then resultant object is trimmed to first 511 chars(after trimming spaces/illegal  chars) and error is received in desc+ code
        eventName = "          "
        repeat(513){ eventName += "H" }
        result = validator.cleanEventName(eventName)
        assertNotNull(result.errorDesc)

        val expectedError = ValidationResultFactory.create(510, Constants.VALUE_CHARS_LIMIT_EXCEEDED,eventName.trim(), Constants.MAX_VALUE_LENGTH.toString() )
        //assertEquals(expectedError.errorDesc,result.errorDesc)
        assertEquals(expectedError.errorCode,result.errorCode)
        assertEquals(511,(result.`object` as String).length)

        // when illegal chars are used, they are removed from the string but no error is raised
        eventName = "APP .:$'\" \\ IS LIVE "
        result = validator.cleanEventName(eventName)
        assertEquals("APP   IS LIVE",result.`object`)
        assertEquals(0,result.errorCode)
        assertEquals(null,result.errorDesc)

    }
    @Test
    fun test_cleanObjectKey_when_StringIsPassed_should_ReturnAnObjectOfValidationResultWithCleanedString() {

        // when key name does not contain [".", ":", "$", "'", "\"", "\\"] and length less than 120 chars, then  the whitespaces in start and end are removed in the validation result AND text is converted to lowercase
        var keyName = "     SoMe Name   \n  "
        var result = validator.cleanObjectKey(keyName)
        assertNull(result.errorDesc)
        assertEquals(0,result.errorCode)
        assertEquals("SoMe Name",result.`object`)


        // when key contains more than 120 chars(excluding whitespaces and illegal chars) then resultant object is trimmed to first 119 chars(after trimming spaces/illegal  chars) and error is received in desc+ code
        keyName = "          "
        repeat(121){ keyName += "H" }
        result = validator.cleanObjectKey(keyName)
        assertNotNull(result.errorDesc)

        val expectedError = ValidationResultFactory.create(520, Constants.VALUE_CHARS_LIMIT_EXCEEDED, keyName.trim(), Constants.MAX_KEY_LENGTH.toString())

        //assertEquals(expectedError.errorDesc,result.errorDesc)
        assertEquals(expectedError.errorCode,result.errorCode)
        assertEquals(119,(result.`object` as String).length)

        // when illegal chars are used, they are removed from the string but no error is raised
        keyName = "APP .:$'\" \\ IS LiVE "
        result = validator.cleanObjectKey(keyName)
        assertEquals("APP   IS LiVE",result.`object`)
        assertEquals(0,result.errorCode)
        assertEquals(null,result.errorDesc)

    }

    @Test
    fun test_cleanMultiValuePropertyKey_when_AKeyNameIsPassed_should_ReturnAppropriateValidationResult() {
        val vSpy = Mockito.spy(validator)

        // this function creates a validation result by first calling validator.cleanObjectKey(...)
        // function on the input string. therefore, to prevent any impact from other function call,
        // we mock the results provided by validator.cleanObjectKey(...)
        var assumedResult = ValidationResult().also { it.`object`="abcd" }
        Mockito.`when`(vSpy.cleanObjectKey(Mockito.anyString())).thenReturn(assumedResult)

        // when keyname is not one of the restricted key names (i.e RestrictedMultiValueFields.Name/Email/Education/Married/DOB/Gender/Phone/Age/FBID/GPID/Birthday),
        // then no validation results are changed
        var result = vSpy.cleanMultiValuePropertyKey("abcd")
        assertEquals(assumedResult,result)
        assertEquals(0,result.errorCode)
        assertEquals(null,result.errorDesc)
        assertEquals("abcd",result.`object`)

        // when keyname is not one of the restricted key names (i.e RestrictedMultiValueFields.Name/Email/Education/Married/DOB/Gender/Phone/Age/FBID/GPID/Birthday),
        // then no validation results are changed

        "Name/Email/Education/Married/DOB/Gender/Phone/Age/FBID/GPID/Birthday".split('/').forEach {forbidden ->
            assumedResult = ValidationResult().also { it.`object`=forbidden }
            Mockito.`when`(vSpy.cleanObjectKey(Mockito.anyString())).thenReturn(assumedResult)
            result = vSpy.cleanMultiValuePropertyKey(forbidden)

            val error = ValidationResultFactory.create(523, Constants.RESTRICTED_MULTI_VALUE_KEY, forbidden)
            assertEquals(error.errorCode,result.errorCode)
            assertEquals(error.errorDesc,result.errorDesc)
            assertEquals(null,result.`object`)

        }
    }

    @Test
    fun test_cleanMultiValuePropertyValue_when_ABC_should_XYZ() {
    }

    @Test
    fun test_mergeMultiValuePropertyForKey_when_ABC_should_XYZ() {
    }


    @Test
    fun test_cleanObjectValue_when_AValueIsPassed_should_ReturnAppropriateValidationResult() {
        // function can take instance of an object and ValidationContext.

        // 1. object must be either of String/Char, Boolean, Long, Integer, Float, Double, or Date, else error occurs
        kotlin.runCatching {
            class ABC
            validator.cleanObjectValue(ABC(), Validator.ValidationContext.Event)
        }.exceptionOrNull().let { t ->
            //t.printStackTrace()
            if(t==null)fail("should have raised exception")
            assertTrue { t is IllegalArgumentException }
            assertEquals("Not a String, Boolean, Long, Integer, Float, Double, or Date", t.message)
        }

        // 2. object can also be a string array or any arrayLIST(not array) of STRING Type but then ValidationContext must be of enum value Profile else error occurs
        kotlin.runCatching {
            validator.cleanObjectValue(arrayOf("acv","aa","ss","ww"), Validator.ValidationContext.Event)
        }.exceptionOrNull().let { t ->
            //t.printStackTrace()
            if(t==null)fail("should have raised exception")
            assertTrue { t is IllegalArgumentException }
            assertEquals("Not a String, Boolean, Long, Integer, Float, Double, or Date", t.message)
        }

        //3. if object is either Integer, Float, Double ,Long or Boolean; no cleaning is required and result is returned as it is.
        arrayOf(1, 0, -1, 2.3f, -2.3f, 0f, 3.31, -3.31, 0.01, 21L, -21L, 0L, true, false).forEach { obj ->
            val result = validator.cleanObjectValue(obj, Validator.ValidationContext.Event)
            assertEquals(null, result.errorDesc)
            assertEquals(0, result.errorCode)
            assertEquals(obj, result.`object`)
        }

        // 4. if object is of type string or character, then final result will have the object set as a string without spaces at start/end and without forbidden characters : '  " and \\
        val testCharsStrings =  mapOf('\'' to "", "\"" to "", '\\' to "", 'a' to "a", ' ' to "", "    spaced string     " to "spaced string", " \t \n \r   spaced string     " to "spaced string")            //each key represents an erroronous char/string while each value represents the expected output
        testCharsStrings.entries.forEach {
            val result = validator.cleanObjectValue(it.key, Validator.ValidationContext.Event)
            assertEquals(null, result.errorDesc)
            assertEquals(0, result.errorCode)
            assertEquals(it.value, result.`object`)
        }
        // 5. for case 4 , length of string must be less than MAX_VALUE_LENGTH(512), else it is trimmed to MAX_VALUE_LENGTH-1 (511) and error value will be set upon it
        var longString = "    \n \r \t"
        repeat(Constants.MAX_VALUE_LENGTH+1) { longString += 'h' }
        var result = validator.cleanObjectValue(longString, Validator.ValidationContext.Event)

        var expectedStr = longString.trim().substring(0,511)
        val expectedError = ValidationResultFactory.create(521, Constants.VALUE_CHARS_LIMIT_EXCEEDED,expectedStr, Constants.MAX_VALUE_LENGTH.toString() + "")
        assertEquals(expectedError.errorDesc, result.errorDesc)
        assertEquals(expectedError.errorCode, result.errorCode)
        assertEquals(expectedStr, result.`object`)

        // 6. if object is of type date, then validator value must be a string of type '$D_xyz'
        val date = Date()
        expectedStr = "\$D_" + (date.time / 1000)
        result = validator.cleanObjectValue(date, Validator.ValidationContext.Event)
        assertEquals(null, result.errorDesc)
        assertEquals(0, result.errorCode)
        assertEquals(expectedStr, result.`object`)

        // 7. if object is of type  [string array]or [ arraylist<string>] AND  ValidationContext is of type Profile then a json of string array is created and object is set as "$set:[<arr>]"

        validator.cleanObjectValue(arrayOf("str", "str", "str", "str"),Validator.ValidationContext.Profile).let {
            assertEquals(null, it.errorDesc)
            assertEquals(0, it.errorCode)
            assertTrue { it.`object` is JSONObject }
            assertEquals("""{""""+'$'+"""set":["str","str","str","str"]}""".trim(), (it.`object` as JSONObject).toString())
        }
        validator.cleanObjectValue(arrayListOf("str", "str", "str", "str"),Validator.ValidationContext.Profile).let {
            assertEquals(null, it.errorDesc)
            assertEquals(0, it.errorCode)
            assertTrue { it.`object` is JSONObject }
            assertEquals("""{""""+'$'+"""set":["str","str","str","str"]}""".trim(), (it.`object` as JSONObject).toString()) //{"$set":["str","str","str","str"]}
        }
        // 8. however, total number of items should be less than 100
        val values = (1..(Constants.MAX_MULTI_VALUE_ARRAY_LENGTH+1)).map { "str" }.toMutableList()
        validator.cleanObjectValue(values,Validator.ValidationContext.Profile).let {
            val error = ValidationResultFactory.create(521, Constants.INVALID_PROFILE_PROP_ARRAY_COUNT, values.size.toString() , Constants.MAX_MULTI_VALUE_ARRAY_LENGTH.toString() )
            assertEquals(error.errorDesc, it.errorDesc)
            assertEquals(error.errorCode, it.errorCode)
            assertEquals(null, it.`object`)
        }

    }

    @Test
    fun test_isRestrictedEventName_when_EventnameIsPassed_should_ReturnAppropriateValidationResult() {
        // when event name is null, should give null error as validation input
        var result = validator.isRestrictedEventName(null)
        var expectedResult = ValidationResultFactory.create(510, Constants.EVENT_NAME_NULL)
        assertNull(result.`object`)
        assertEquals(expectedResult.errorCode, result.errorCode)
        assertEquals(expectedResult.errorDesc, result.errorDesc)

        // when event name is one of the restricted names, should give restricted names error
        val restrictedNames = arrayOf("Stayed", "Notification Clicked",
            "Notification Viewed", "UTM Visited", "Notification Sent", "App Launched", "wzrk_d",
            "App Uninstalled", "Notification Bounced", Constants.GEOFENCE_ENTERED_EVENT_NAME,
            Constants.GEOFENCE_EXITED_EVENT_NAME)

        restrictedNames.forEach { name ->
             result = validator.isRestrictedEventName(name)
             expectedResult = ValidationResultFactory.create(513, Constants.RESTRICTED_EVENT_NAME, name)
            assertNull(result.`object`)
            assertEquals(expectedResult.errorCode, result.errorCode)
            assertEquals(expectedResult.errorDesc, result.errorDesc)
        }

        // when event name is neither null nor one of the restricted names, should return 0 in error code

        result = validator.isRestrictedEventName("abc")
        assertNull(result.`object`)
        assertEquals(0, result.errorCode)
        assertEquals(null, result.errorDesc)

    }



    @Test
    fun test_setDiscardedEvents_when_ListOfEventNamesArePassed_should_SetEventNamesAaDiscarded() {
        // we verify the setting of discarded names by testing with isEventDiscarded() function. if event name returns valid validation results, this means setter is working properly

        validator.setDiscardedEvents(arrayListOf("a","b","c"))

        validator.isEventDiscarded("a").let {result->
            val vr = ValidationResultFactory.create(513, Constants.DISCARDED_EVENT_NAME, "a")
            assertEquals(vr.errorDesc,result.errorDesc)
            assertEquals(vr.errorCode,result.errorCode)
        }

        validator.isEventDiscarded("x").let {result->
            val vr = ValidationResult()
            assertEquals(null,result.errorDesc)
            assertEquals(0,result.errorCode)
        }

    }

    @Test
    fun test_isEventDiscarded_when_EventnameIsPassed_should_ReturnAppropriateValidationResult() {

        // asssumption : event names 'a' ,'b' and 'c' are discarded. event name 'x' is not discarded
        validator.setDiscardedEvents(arrayListOf("a","b","c"))

        validator.isEventDiscarded("a").let {result->
            val vr = ValidationResultFactory.create(513, Constants.DISCARDED_EVENT_NAME, "a")
            assertEquals(vr.errorDesc,result.errorDesc)
            assertEquals(vr.errorCode,result.errorCode)
        }

        validator.isEventDiscarded("x").let {result->
            val vr = ValidationResult()
            assertEquals(null,result.errorDesc)
            assertEquals(0,result.errorCode)
        }

    }
}