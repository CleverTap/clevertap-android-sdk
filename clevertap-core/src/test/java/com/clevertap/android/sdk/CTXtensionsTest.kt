package com.clevertap.android.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTXtensionsTest : BaseTestCase() {

    @Test
    fun test_when_sdkInt_and_targetSdkVersion_is_33_and_input_is_32_should_return_true() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertTrue { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_33_and_targetSdkVersion_is_32_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 33)

        application.applicationContext.applicationInfo.targetSdkVersion = 32

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_32_and_targetSdkVersion_is_33_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 32)

        application.applicationContext.applicationInfo.targetSdkVersion = 33

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_when_sdkInt_is_30_and_targetSdkVersion_is_30_and_input_is_32_should_return_false() {
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", 30)

        application.applicationContext.applicationInfo.targetSdkVersion = 30

        assertFalse { application.isPackageAndOsTargetsAbove(32) }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreEnabled_and_channelImportanceIsNone_should_return_false() {

        configureTestNotificationChannel(NotificationManager.IMPORTANCE_NONE, true, 30)
        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreEnabled_and_channelImportanceIsMAX_should_return_true() {

        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 30)
        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertTrue { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreDisabled_and_channelImportanceIsNone_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_NONE, false, 30)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_30_and_notificationsAreDisabled_and_channelImportanceIsMAX_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, false, 30)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_25_and_notificationsAreDisabled_should_return_false() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, false, 25)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertFalse { actual }
    }

    @Test
    fun test_isNotificationChannelEnabled_when_sdkInt_is_25_and_notificationsAreEnabled_should_return_true() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 25)

        val actual = application.isNotificationChannelEnabled("BlockedBRTesting")
        assertTrue { actual }
    }

    //given = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_registered_then_return_its_channelID() {
        configureTestNotificationChannel(NotificationManager.IMPORTANCE_MAX, true, 30)
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val actual = nm.getOrCreateChannel("BlockedBRTesting",application)
        assertEquals("BlockedBRTesting",actual)
    }

    //given = null | manifest = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_is_null_and_manifestChannel_is_registered_then_return_manifestChannel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_MAX, true, 30,
                "ManifestChannelId", "ManifestChannelName"
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals("ManifestChannelId", actual)
        }
    }

    //given = null | manifest = null | default = not registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_null_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = null | default = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_is_null_and_manifestChannel_is_null_and_fallback_channel_exists_then_return_fallback_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = not registered | manifest = null | default = not registered
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_available_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }
    //given = not registered | manifest = not registered | default
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = not registered | manifest = null | default = registered
    @Test
    fun test_getOrCreateChannel_when_channel_not_registered_and_manifestChannel_not_available_and_fallback_channel_exists_then_return_fallback_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn(null)

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel("NonExistentChannel", application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = not registered | default = not registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_not_registered_and_default_not_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("NonRegisteredManifestChannelId")

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }

    //given = null | manifest = not registered | default = registered
    @Test
    fun test_getOrCreateChannel_when_given_channel_null_and_manifestChannel_not_registered_and_default_is_registered_then_return_default_channel() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("NonRegisteredManifestChannelId")

            // Configure the test notification channel with an existing fallback channel
            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_DEFAULT, true, 30,
                channelID = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
            )

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID, actual)
        }
    }


    @Test
    fun test_getOrCreateChannel_when_getNotificationChannel_throws_exception_return_null() {
        mockStatic(ManifestInfo::class.java).use {
            val nm = mock(NotificationManager::class.java)
            val manifestInfo = mock(ManifestInfo::class.java)

            `when`(ManifestInfo.getInstance(application)).thenReturn(manifestInfo)
            `when`(manifestInfo.devDefaultPushChannelId).thenReturn("ManifestChannelId")

            configureTestNotificationChannel(
                NotificationManager.IMPORTANCE_MAX, true, 30,
                "ManifestChannelId", "ManifestChannelName"
            )

            // Throw an exception from the `getNotificationChannel()` method.
            `when`(nm.getNotificationChannel("ManifestChannelId")).thenThrow(RuntimeException())

            val actual = nm.getOrCreateChannel(null, application)
            assertEquals(null, actual)

        }
    }

    @Test
    fun `test isInvalidIndex with null JSONArray`() {
        val jsonArray: JSONArray? = null
        assertTrue(jsonArray.isInvalidIndex(0))
    }

    @Test
    fun `test isInvalidIndex with empty JSONArray`() {
        val jsonArray = JSONArray()
        assertTrue(jsonArray.isInvalidIndex(0))
    }

    @Test
    fun `test isInvalidIndex with valid index`() {
        val jsonArray = JSONArray("[1, 2, 3]")
        assertFalse(jsonArray.isInvalidIndex(0))
        assertFalse(jsonArray.isInvalidIndex(1))
        assertFalse(jsonArray.isInvalidIndex(2))
    }

    @Test
    fun `test isInvalidIndex with invalid index at right and left boundary`() {
        val jsonArray = JSONArray("[1, 2, 3]")
        assertTrue(jsonArray.isInvalidIndex(3))
        assertTrue(jsonArray.isInvalidIndex(-1))
    }

    @Test
    fun `test hasData with empty SharedPreferences`() {
        val sharedPreferences = application.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Ensure the SharedPreferences is empty
        assertTrue(sharedPreferences.all.isEmpty())

        // Test the hasData function
        assertFalse(sharedPreferences.hasData())
    }

    @Test
    fun `test hasData with non-empty SharedPreferences`() {
        val sharedPreferences = application.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        // Add some data to SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("key1", "value1")
        editor.putString("key2", "value2")
        editor.apply()

        // Ensure the SharedPreferences is not empty
        assertTrue(sharedPreferences.all.isNotEmpty())

        // Test the hasData function
        assertTrue(sharedPreferences.hasData())
    }

    @Test
    fun `test orEmptyArray with null JSONArray`() {
        val jsonArray: JSONArray? = null

        // Ensure that the result is a non-null empty JSONArray
        assertNotNull(jsonArray.orEmptyArray())

        // Ensure that the length of the result is 0
        assertEquals(0, jsonArray.orEmptyArray().length())
    }

    @Test
    fun `test orEmptyArray with non-null JSONArray`() {
        val jsonArray = JSONArray("[1, 2, 3]")

        // Ensure that the result is the same reference as the original non-null JSONArray
        assertSame(jsonArray, jsonArray.orEmptyArray())
    }

    @Test
    fun `test toList with empty JSONArray`() {
        val jsonArray = JSONArray()

        val jsonObjectList = jsonArray.toList<JSONObject>()

        // Ensure the resulting list is empty
        assertTrue(jsonObjectList.isEmpty())
    }

    @Test
    fun `test toList with non-empty JSONArray of JSONObject data type`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonObject2 = JSONObject("{\"key2\": \"value2\"}")

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(jsonObject2)

        val jsonObjectList = jsonArray.toList<JSONObject>()

        // Ensure the resulting list has the correct size
        assertEquals(2, jsonObjectList.size)

        // Ensure the resulting list contains the expected JSONObjects
        assertEquals(jsonObject1.toString(), jsonObjectList[0].toString())
        assertEquals(jsonObject2.toString(), jsonObjectList[1].toString())
    }

    @Test
    fun `test toList with non-empty JSONArray of mix data type`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonObject2 = JSONObject("{\"key2\": \"value2\"}")

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(1)
        jsonArray.put(3.14159265359)
        jsonArray.put(true)
        jsonArray.put(JSONArray())
        jsonArray.put(jsonObject2)

        val jsonObjectList = jsonArray.toList<JSONObject>()

        // Ensure the resulting list has the correct size
        assertEquals(2, jsonObjectList.size)

        // Ensure the resulting list contains the expected JSONObjects
        assertEquals(jsonObject1.toString(), jsonObjectList[0].toString())
        assertEquals(jsonObject2.toString(), jsonObjectList[1].toString())

        // double data type
        val doubleList = jsonArray.toList<Double>()

        // Ensure the resulting list has the correct size
        assertEquals(1, doubleList.size)
        assertEquals(jsonArray[2], doubleList[0])

        // int data type
        val intList = jsonArray.toList<Int>()

        // Ensure the resulting list has the correct size
        assertEquals(1, intList.size)
        assertEquals(jsonArray[1], intList[0])

        // boolean data type
        val booleanList = jsonArray.toList<Boolean>()

        // Ensure the resulting list has the correct size
        assertEquals(1, booleanList.size)
        assertTrue(booleanList[0])

        // Any data type
        val anyList = jsonArray.toList<Any>()

        // Ensure the resulting list has the correct size
        assertEquals(jsonArray.length(), anyList.size)
        assertEquals(jsonObject1.toString(), anyList[0].toString())
        assertEquals(jsonObject2.toString(), anyList[5].toString())
        assertEquals(jsonArray[1], anyList[1])
        assertEquals(jsonArray[2], anyList[2])
        assertTrue(anyList[3] as Boolean)
        assertEquals(jsonArray[4], anyList[4])
    }

    @Test
    fun `test toList with non-empty JSONArray of double data type`() {

        val jsonArray = JSONArray()
        jsonArray.put(3.14159265359)
        jsonArray.put(4.14159265359)
        jsonArray.put(5.14159265359)

        val jsonObjectList = jsonArray.toList<JSONObject>()

        // Ensure the resulting list has the correct size
        assertEquals(0, jsonObjectList.size)

        val jsonObjectListDouble = jsonArray.toList<Double>()

        // Ensure the resulting list has the correct size
        assertEquals(3, jsonObjectListDouble.size)
        assertEquals(jsonArray[0], jsonObjectListDouble[0])
        assertEquals(jsonArray[1], jsonObjectListDouble[1])
        assertEquals(jsonArray[2], jsonObjectListDouble[2])
    }

    @Test
    fun `test toList with non-empty JSONArray of Int`() {
        val jsonArray = JSONArray()
        jsonArray.put(1)

        val intList = jsonArray.toList<Int>()

        // Ensure the resulting list has the correct size
        assertEquals(1, intList.size)
        assertEquals(jsonArray[0], intList[0])
    }

    @Test
    fun `test toList with non-empty JSONArray of Boolean`() {
        val jsonArray = JSONArray()
        jsonArray.put(true)

        val booleanList = jsonArray.toList<Boolean>()

        // Ensure the resulting list has the correct size
        assertEquals(1, booleanList.size)
        assertTrue(booleanList[0])
    }

    @Test
    fun `test toList with non-empty JSONArray of Any`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(1)
        jsonArray.put(3.14159265359)
        jsonArray.put(true)

        val anyList = jsonArray.toList<Any>()

        // Ensure the resulting list has the correct size
        assertEquals(jsonArray.length(), anyList.size)
        assertEquals(jsonObject1.toString(), anyList[0].toString())
        assertEquals(jsonArray[1], anyList[1])
        assertEquals(jsonArray[2], anyList[2])
        assertTrue(anyList[3] as Boolean)
    }

    @Test
    fun `test toList with non-empty JSONArray of JSONArray`() {
        val innerArray1 = JSONArray()
        innerArray1.put("value1a")
        innerArray1.put("value1b")

        val innerArray2 = JSONArray()
        innerArray2.put("value2a")
        innerArray2.put("value2b")

        val jsonArray = JSONArray()
        jsonArray.put(innerArray1)
        jsonArray.put(innerArray2)

        val jsonArrayList = jsonArray.toList<JSONArray>()

        // Ensure the resulting list has the correct size
        assertEquals(2, jsonArrayList.size)

        // Ensure the resulting list contains the expected JSONArrays
        assertEquals(innerArray1.toString(), jsonArrayList[0].toString())
        assertEquals(innerArray2.toString(), jsonArrayList[1].toString())
    }

    @Test
    fun `test iterator with empty JSONArray`() {
        val jsonArray = JSONArray()

        val result = mutableListOf<JSONObject>()

        jsonArray.iterator<JSONObject> { jsonObject ->
            result.add(jsonObject)
        }

        assertEquals(
            emptyList(),
            result
        )
    }

    @Test
    fun `test iterator with JSONArray of Strings`() {
        val jsonArray = JSONArray()
        jsonArray.put("value1")
        jsonArray.put("value2")

        val result = mutableListOf<String>()

        jsonArray.iterator<String> { stringValue ->
            result.add(stringValue)
        }

        assertEquals(listOf("value1", "value2"), result)
    }

    @Test
    fun `test iterator with JSONArray of Integers`() {
        val jsonArray = JSONArray()
        jsonArray.put(42)
        jsonArray.put(100)

        val result = mutableListOf<Int>()

        jsonArray.iterator<Int> { intValue ->
            result.add(intValue)
        }

        assertEquals(listOf(42, 100), result)
    }

    @Test
    fun `test iterator with JSONArray of JSONObjects`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonObject2 = JSONObject("{\"key2\": \"value2\"}")

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(jsonObject2)

        val result = mutableListOf<JSONObject>()

        jsonArray.iterator<JSONObject> { jsonObject ->
            result.add(jsonObject)
        }

        assertEquals(listOf(jsonObject1, jsonObject2), result)
    }

    @Test
    fun `test iterator with JSONArray of mixed data types and iterator of JSONObject`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonObject2 = JSONObject("{\"key2\": \"value2\"}")

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(42)
        jsonArray.put(3.14)
        jsonArray.put(true)
        jsonArray.put("string value")
        jsonArray.put(jsonObject2)

        val result = mutableListOf<JSONObject>()

        jsonArray.iterator<JSONObject> { jsonObject ->
            result.add(jsonObject)
        }

        assertEquals(
            listOf(jsonObject1, jsonObject2),
            result
        )
    }

    @Test
    fun `test iterator with generic type Any`() {
        val jsonObject1 = JSONObject("{\"key1\": \"value1\"}")
        val jsonObject2 = JSONObject("{\"key2\": \"value2\"}")

        val innerArray1 = JSONArray()
        innerArray1.put("value1a")
        innerArray1.put("value1b")

        val innerArray2 = JSONArray()
        innerArray2.put("value2a")
        innerArray2.put("value2b")

        val jsonArray = JSONArray()
        jsonArray.put(jsonObject1)
        jsonArray.put(42)
        jsonArray.put(3.14)
        jsonArray.put(true)
        jsonArray.put("string value")
        jsonArray.put(jsonObject2)
        jsonArray.put(innerArray1)
        jsonArray.put(innerArray2)

        val result = mutableListOf<Any>()

        jsonArray.iterator<Any> { element ->
            result.add(element)
        }

        assertEquals(
            listOf(jsonObject1, 42, 3.14, true, "string value", jsonObject2, innerArray1, innerArray2),
            result
        )
    }

    @Test
    fun `safeGetJSONArray returns Pair(true, JSONArray) when key exists and array is not empty`() {
        // Arrange
        val jsonObject = JSONObject()
        val jsonArray = JSONArray().put(1).put(2).put(3)
        jsonObject.put("key", jsonArray)

        // Act
        val result = jsonObject.safeGetJSONArray("key")

        // Assert
        assertTrue(result.first)
        assertEquals(jsonArray.toString(), result.second.toString())
    }

    @Test
    fun `safeGetJSONArray returns Pair(false, null) when key exists but array is empty`() {
        // Arrange
        val jsonObject = JSONObject()
        val jsonArray = JSONArray()
        jsonObject.put("key", jsonArray)

        // Act
        val result = jsonObject.safeGetJSONArray("key")

        // Assert
        assertFalse(result.first)
        assertNull(result.second)
    }

    @Test
    fun `safeGetJSONArray returns Pair(false, null) when key does not exist`() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("anotherKey", "value")

        // Act
        val result = jsonObject.safeGetJSONArray("key")

        // Assert
        assertFalse(result.first)
        assertNull(result.second)
    }

    @Test
    fun `safeGetJSONArray returns Pair(false, null) when key's value is not a JSON array`() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("key", "not_an_array")

        // Act
        val result = jsonObject.safeGetJSONArray("key")

        // Assert
        assertFalse(result.first)
        assertNull(result.second)
    }

    @Test
    fun `copyFrom copies all key-value pairs from another JSONObject`() {
        // Arrange
        val original = JSONObject()
        original.put("key1", "value1")
        original.put("key2", 42)
        original.put("key3", true)

        val jsonObject = JSONObject()

        // Act
        jsonObject.copyFrom(original)

        // Assert
        assertEquals("value1", jsonObject.getString("key1"))
        assertEquals(42, jsonObject.getInt("key2"))
        assertEquals(true, jsonObject.getBoolean("key3"))
    }

    @Test
    fun `copyFrom does not modify destination JSONObject if source is empty`() {
        // Arrange
        val original = JSONObject()
        val jsonObject = JSONObject()
        jsonObject.put("existingKey", "existingValue")

        // Act
        jsonObject.copyFrom(original)

        // Assert
        assertEquals("existingValue", jsonObject.getString("existingKey"))
        assertEquals(1, jsonObject.length()) // Ensure no additional keys are added
    }

    @Test
    fun `copyFrom overrides existing keys in destination JSONObject with values from source`() {
        // Arrange
        val original = JSONObject()
        original.put("key1", "newValue1")
        original.put("key2", 99)

        val jsonObject = JSONObject()
        jsonObject.put("key1", "value1")
        jsonObject.put("key2", 42)

        // Act
        jsonObject.copyFrom(original)

        // Assert
        assertEquals("newValue1", jsonObject.getString("key1"))
        assertEquals(99, jsonObject.getInt("key2"))
    }

    @Test
    fun `copyFrom overrides existing keys in destination JSONObject with values from source, and adds new keys`() {
        // Arrange
        val original = JSONObject()
        original.put("key1", "newValue1")
        original.put("key2", 99)
        original.put("key3", false)

        val jsonObject = JSONObject()
        jsonObject.put("key1", "value1")
        jsonObject.put("key2", 42)
        jsonObject.put("existingKey", "existingValue")

        // Act
        jsonObject.copyFrom(original)

        // Assert
        assertEquals("newValue1", jsonObject.getString("key1"))
        assertEquals(99, jsonObject.getInt("key2"))
        assertEquals(false, jsonObject.getBoolean("key3"))
        assertEquals("existingValue", jsonObject.getString("existingKey"))
        assertEquals(4, jsonObject.length()) // Ensure all keys are present
    }

    @Test
    fun `copyFrom deep copies nested JSON objects`() {
        // Arrange
        val original = JSONObject()
        val nestedOriginal = JSONObject()
        nestedOriginal.put("nestedKey", "nestedValue")
        original.put("key1", nestedOriginal)
        original.put("key2", "value2")

        val jsonObject = JSONObject()
        val nestedJsonObject = JSONObject()
        nestedJsonObject.put("nestedKey", "originalNestedValue")
        jsonObject.put("key1", nestedJsonObject)
        jsonObject.put("key2", "originalValue")

        // Act
        jsonObject.copyFrom(original)

        // Assert
        assertEquals("nestedValue", jsonObject.getJSONObject("key1").getString("nestedKey"))
        assertEquals("value2", jsonObject.getString("key2"))
    }

    @Test
    fun `isNotNullAndEmpty returns true for a non-null and non-empty JSONObject`() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("key", "value")

        // Act
        val result = jsonObject.isNotNullAndEmpty()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `isNotNullAndEmpty returns false for a null JSONObject`() {
        // Arrange
        val jsonObject: JSONObject? = null

        // Act
        val result = jsonObject.isNotNullAndEmpty()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `isNotNullAndEmpty returns false for an empty JSONObject`() {
        // Arrange
        val jsonObject = JSONObject()

        // Act
        val result = jsonObject.isNotNullAndEmpty()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `concatIfNotNull concatenates two non-null strings with a separator`() {
        // Arrange
        val str1 = "Hello"
        val str2 = "World"
        val separator = ", "

        // Act
        val result = str1.concatIfNotNull(str2, separator)

        // Assert
        assertEquals("Hello, World", result)
    }

    @Test
    fun `concatIfNotNull returns the non-null string when the other string is null`() {
        // Arrange
        val str1 = "Hello"
        val str2: String? = null
        val separator = ", "

        // Act
        val result = str1.concatIfNotNull(str2, separator)

        // Assert
        assertEquals("Hello", result)
    }

    @Test
    fun `concatIfNotNull returns the other string when the first string is null`() {
        // Arrange
        val str1: String? = null
        val str2 = "World"
        val separator = ", "

        // Act
        val result = str1.concatIfNotNull(str2, separator)

        // Assert
        assertEquals("World", result)
    }

    @Test
    fun `concatIfNotNull returns null when both strings are null`() {
        // Arrange
        val str1: String? = null
        val str2: String? = null
        val separator = ", "

        // Act
        val result = str1.concatIfNotNull(str2, separator)

        // Assert
        assertEquals(null, result)
    }

    @Test
    fun `concatIfNotNull concatenates two non-null strings without a separator`() {
        // Arrange
        val str1 = "Hello"
        val str2 = "World"

        // Act
        val result = str1.concatIfNotNull(str2)

        // Assert
        assertEquals("HelloWorld", result)
    }

    private fun configureTestNotificationChannel(
        importance: Int, areChannelsEnabled: Boolean, SDK_INT: Int, channelID: String = "BlockedBRTesting",
        channelName: String = "BlockedBRTesting",
    ) {
        val nm = application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(nm)
        shadowNotificationManager.setNotificationsEnabled(areChannelsEnabled)
        val notificationChannel = NotificationChannel(
            channelID,
            channelName,
            importance
        )
        notificationChannel.description = "channelDescription"
        nm.createNotificationChannel(notificationChannel)
        ReflectionHelpers.setStaticField(VERSION::class.java, "SDK_INT", SDK_INT)
    }
}