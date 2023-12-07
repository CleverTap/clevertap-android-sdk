package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter.Companion
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.mockito.*
import org.mockito.Mockito.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InAppResponseAdapterTest {

    @Test
    fun testLegacyInApps() {
        val responseJson = JSONObject()

        val legacyInAppArray = JSONArray().put(JSONObject().apply {
            put("someKey", JSONObject())
        })

        responseJson.put(Constants.INAPP_JSON_RESPONSE_KEY, legacyInAppArray)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        val result = inAppResponseAdapter.legacyInApps
        assertEquals(true, result.first)
        assertNotNull(result.second)
    }

    @Test
    fun testClientSideInApps() {
        val responseJson = JSONObject()

        val clientSideInAppArray = JSONArray().put(JSONObject().apply {
            put("someKey", JSONObject())
        })

        responseJson.put(Constants.INAPP_NOTIFS_KEY_CS, clientSideInAppArray)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        val result = inAppResponseAdapter.clientSideInApps
        assertEquals(true, result.first)
        assertNotNull(result.second)
    }

    @Test
    fun testServerSideInApps() {
        val responseJson = JSONObject()

        val serverSideInAppArray = JSONArray().put(JSONObject().apply {
            put("someKey", JSONObject())
        })

        responseJson.put(Constants.INAPP_NOTIFS_KEY_SS, serverSideInAppArray)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        val result = inAppResponseAdapter.serverSideInApps
        assertEquals(true, result.first)
        assertNotNull(result.second)
    }

    @Test
    fun testStaleInApps() {
        val responseJson = JSONObject()

        val staleInAppArray = JSONArray().put(JSONObject().apply {
            put("someKey", JSONObject())
        })

        responseJson.put(Constants.INAPP_NOTIFS_STALE_KEY, staleInAppArray)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        val result = inAppResponseAdapter.staleInApps
        assertEquals(true, result.first)
        assertNotNull(result.second)
    }

    private fun testAppLaunchServerSideInApps() {
        val responseJson = JSONObject()
        val appLaunchServerSideInAppsArray = JSONArray().put(JSONObject().apply {
            put("someKey", JSONObject())
        })
        responseJson.put(Constants.INAPP_NOTIFS_APP_LAUNCHED_KEY, appLaunchServerSideInAppsArray)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        val result = inAppResponseAdapter.appLaunchServerSideInApps
        assertEquals(true, result.first)
        assertNotNull(result.second)
    }

    @Test
    fun testInAppsPerSession() {
        val responseJson = JSONObject()
        responseJson.put(InAppResponseAdapter.IN_APP_SESSION_KEY, 20)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        assertEquals(20, inAppResponseAdapter.inAppsPerSession)
    }

    @Test
    fun testInAppsPerDay() {
        val responseJson = JSONObject()
        responseJson.put(InAppResponseAdapter.IN_APP_DAILY_KEY, 15)

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        assertEquals(15, inAppResponseAdapter.inAppsPerDay)
    }

    @Test
    fun testInAppMode() {
        val responseJson = JSONObject()
        responseJson.put(Constants.INAPP_DELIVERY_MODE_KEY, "some_mode")

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        assertEquals("some_mode", inAppResponseAdapter.inAppMode)
    }

    @Test
    fun testDefaultValues() {
        val responseJson = JSONObject()

        val inAppResponseAdapter = InAppResponseAdapter(responseJson)

        // Test default values
        assertEquals(InAppResponseAdapter.IN_APP_DEFAULT_SESSION, inAppResponseAdapter.inAppsPerSession)
        assertEquals(InAppResponseAdapter.IN_APP_DEFAULT_DAILY, inAppResponseAdapter.inAppsPerDay)
        assertEquals("", inAppResponseAdapter.inAppMode)

        val defaultLegacyInApps = inAppResponseAdapter.legacyInApps
        assertEquals(false, defaultLegacyInApps.first)
        assertNull(defaultLegacyInApps.second)

        val defaultClientSideInApps = inAppResponseAdapter.clientSideInApps
        assertEquals(false, defaultClientSideInApps.first)
        assertNull(defaultClientSideInApps.second)

        val defaultServerSideInApps = inAppResponseAdapter.serverSideInApps
        assertEquals(false, defaultServerSideInApps.first)
        assertNull(defaultServerSideInApps.second)

        assertEquals(emptyList(), inAppResponseAdapter.preloadImage)

        val defaultStaleInApps = inAppResponseAdapter.staleInApps
        assertEquals(false, defaultStaleInApps.first)
        assertNull(defaultStaleInApps.second)

        val defaultAppLaunchServerSideInApps = inAppResponseAdapter.appLaunchServerSideInApps
        assertEquals(false, defaultAppLaunchServerSideInApps.first)
        assertNull(defaultAppLaunchServerSideInApps.second)
    }
}