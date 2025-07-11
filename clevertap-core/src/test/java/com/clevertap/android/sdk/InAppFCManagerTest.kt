package com.clevertap.android.sdk

import android.content.SharedPreferences
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.InAppFixtures
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InAppFCManagerTest : BaseTestCase() {

    private data class InAppCount(val todayCount: Int, val lifetimeCount: Int)

    private lateinit var impressionManager: ImpressionManager
    private lateinit var clock: FakeClock

    override fun setUp() {
        super.setUp()
        impressionManager = mockk(relaxed = true)
        clock = FakeClock()
    }

    @Test
    fun `canShow should return false for null in-apps`() {
        val fcManager = createInAppFCManager()
        assertFalse(fcManager.canShow(null) { json, inAppId -> false })
    }

    @Test
    fun `canShow should return true for in-apps without id`() {
        val fcManager = createInAppFCManager()
        val inApp =
            CTInAppNotification(JSONObject(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL), false)

        assertTrue(fcManager.canShow(inApp) { json, inAppId -> false })
    }

    @Test
    fun `canShow should return false if hasInAppFrequencyLimitsMaxedOut returns true`() {
        val fcManager = createInAppFCManager()
        val id = "id"
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id)

        assertFalse(fcManager.canShow(inApp) { json, inAppId ->
            inAppId == id
        })
    }

    @Test
    fun `canShow should return false if hasInAppFrequencyLimitsMaxedOut throws`() {
        val fcManager = createInAppFCManager()
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, "id")

        assertFalse(fcManager.canShow(inApp) { json, inAppId ->
            throw Exception()
        })
    }

    @Test
    fun `canShow should return true if in-app is excluded from frequency caps`() {
        val fcManager = createInAppFCManager()
        val id = "id"
        val inApp =
            createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id, exclude = true)

        assertTrue(fcManager.canShow(inApp) { json, inAppId -> false })
    }

    @Test
    fun `canShow should return false if the in-app is shown more times than max per session`() {
        val fcManager = createInAppFCManager()
        val id = "id"
        val inApp =
            createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id, maxDisplayCount = 1)

        every { impressionManager.perSession(id) } returns 1

        assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })
    }

    @Test
    fun `canShow should return false if in-apps exceed session limit`() {
        val deviceId = "deviceId2"
        val fcManager = createInAppFCManager(deviceId)
        val id = "id"
        val inApp =
            createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id)

        every { impressionManager.perSessionTotal() } returns 10

        mockkStatic(StorageHelper::class) {
            every {
                StorageHelper.getInt(
                    any(),
                    getKeyWithDeviceIdAndAccountId(Constants.INAPP_MAX_PER_SESSION_KEY, deviceId),
                    any()
                )
            } returns 10
            assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })
        }
    }

    @Test
    fun `canShow should return false if an in-app is displayed more times than total lifetime count`() {
        val deviceId = "deviceId"
        val fcManager = createInAppFCManager(deviceId)
        val id = "id"
        val inApp =
            createInApp(
                InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL,
                id,
                totalLifetimeCount = 10
            )
        val displayedLifetimeCount = 11
        mockkStatic(StorageHelper::class) {
            val mockPrefs = mockk<SharedPreferences>()
            every { mockPrefs.getString(id, any()) } returns "0,$displayedLifetimeCount"
            every {
                StorageHelper.getPreferences(
                    any(),
                    getKeyWithDeviceIdAndAccountId(Constants.KEY_COUNTS_PER_INAPP, deviceId),
                )
            } returns mockPrefs
            assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })
        }
    }

    @Test
    fun `canShow should return false if an in-app is displayed more times than total daily count`() {
        val deviceId = "deviceId"
        val fcManager = createInAppFCManager(deviceId)
        val id = "id"
        val inApp =
            createInApp(
                InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL,
                id,
                totalDailyCount = 10
            )
        val totalDailyCount = 10
        mockkStatic(StorageHelper::class) {
            val mockPrefs = mockk<SharedPreferences>()
            every { mockPrefs.getString(id, any()) } returns "$totalDailyCount,0"
            every {
                StorageHelper.getPreferences(
                    any(),
                    getKeyWithDeviceIdAndAccountId(Constants.KEY_COUNTS_PER_INAPP, deviceId),
                )
            } returns mockPrefs
            assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })
        }
    }

    @Test
    fun `canShow should return true if no limits are exceeded`() {
        val fcManager = createInAppFCManager()
        val id = "id"
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id)
        assertTrue(fcManager.canShow(inApp) { json, inAppId -> false })
    }

    @Test
    fun `didShow should increment shownTodayCount`() {
        val fcManager = createInAppFCManager()
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, "id")

        fcManager.didShow(appCtx, inApp)
        assertEquals(1, fcManager.shownTodayCount)
        fcManager.didShow(appCtx, inApp)
        assertEquals(2, fcManager.shownTodayCount)
    }

    @Test
    fun `didShow should increment counts in getInAppsCount`() {
        val fcManager = createInAppFCManager()
        val id1 = "id1"
        val id2 = "id2"
        val inApp1 = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id1)
        val inApp2 = createInApp(InAppFixtures.TYPE_CUSTOM_CODE, id2)

        fcManager.didShow(appCtx, inApp1)
        fcManager.didShow(appCtx, inApp1)
        fcManager.didShow(appCtx, inApp2)

        val countsMap = countsToMap(fcManager.getInAppsCount(appCtx))

        assertEquals(2, countsMap[id1]!!.todayCount)
        assertEquals(2, countsMap[id1]!!.lifetimeCount)
        assertEquals(1, countsMap[id2]!!.todayCount)
        assertEquals(1, countsMap[id2]!!.lifetimeCount)
    }

    @Test
    fun `processResponse should clear stale in-apps`() {
        val fcManager = createInAppFCManager()
        val id1 = "5"
        val id2 = "id2"
        val inApp1 = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id1)
        val inApp2 = createInApp(InAppFixtures.TYPE_CUSTOM_CODE, id2)

        fcManager.didShow(appCtx, inApp1)
        fcManager.didShow(appCtx, inApp2)
        assertEquals(2, fcManager.getInAppsCount(appCtx).length())

        val jsonResponse = JSONObject(
            """
            {"${Constants.INAPP_NOTIFS_STALE_KEY}": [$id1,"$id2"]}
            """
        )

        fcManager.processResponse(appCtx, jsonResponse)
        assertEquals(0, fcManager.getInAppsCount(appCtx).length())
    }

    @Test
    fun `updateLimits should update the limits used in canShow`() {
        val fcManager = createInAppFCManager()
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, "id")

        assertTrue(fcManager.canShow(inApp) { json, inAppId -> false })

        fcManager.updateLimits(appCtx, 0, 1000)
        assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })

        fcManager.updateLimits(appCtx, 1000, 0)
        assertFalse(fcManager.canShow(inApp) { json, inAppId -> false })

        fcManager.updateLimits(appCtx, 1000, 1000)
        assertTrue(fcManager.canShow(inApp) { json, inAppId -> false })
    }

    @Test
    fun `changeUser should switch daily and lifetime in-app counts to a new user`() {
        val fcManager = createInAppFCManager()
        val id1 = "15"
        val id2 = "id"
        val inApp1 = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id1)
        val inApp2 = createInApp(InAppFixtures.TYPE_CUSTOM_CODE, id2)

        fcManager.didShow(appCtx, inApp1)
        fcManager.didShow(appCtx, inApp2)
        assertEquals(2, fcManager.getInAppsCount(appCtx).length())

        fcManager.changeUser("newDeviceId")
        assertEquals(0, fcManager.getInAppsCount(appCtx).length())

        fcManager.didShow(appCtx, inApp1)
        assertEquals(1, fcManager.getInAppsCount(appCtx).length())
    }

    @Test
    fun `constructor should reset daily counts when created after a day`() {
        var fcManager = createInAppFCManager()
        val id = "id"
        val inApp = createInApp(InAppFixtures.TYPE_ADVANCED_BUILDER_INTERSTITIAL, id)

        fcManager.didShow(appCtx, inApp)

        var countsMap = countsToMap(fcManager.getInAppsCount(appCtx))
        assertEquals(1, countsMap[id]!!.todayCount)
        assertEquals(1, countsMap[id]!!.lifetimeCount)

        // not advancing the clock should increment the previous daily counts
        fcManager = createInAppFCManager()
        fcManager.didShow(appCtx, inApp)
        countsMap = countsToMap(fcManager.getInAppsCount(appCtx))
        assertEquals(2, countsMap[id]!!.todayCount)
        assertEquals(2, countsMap[id]!!.lifetimeCount)

        clock.advanceOneDay()
        //advancing the clock should reset the todayCount
        fcManager = createInAppFCManager()
        fcManager.didShow(appCtx, inApp)
        countsMap = countsToMap(fcManager.getInAppsCount(appCtx))
        assertEquals(1, countsMap[id]!!.todayCount)
        assertEquals(3, countsMap[id]!!.lifetimeCount)
    }

    /**
     * Converts the counts array [[targetID, todayCount, lifetime]] returned by InAppFCManager
     * into a map {targetID: InAppCount(todayCount,lifetime)}
     */
    private fun countsToMap(inAppCount: JSONArray): Map<String, InAppCount> {
        //[[targetID, todayCount, lifetime]]
        val countsMap = mutableMapOf<String, InAppCount>()
        for (i in 0..<inAppCount.length()) {
            val count = inAppCount.getJSONArray(i)
            countsMap.put(count.getString(0), InAppCount(count.getInt(1), count.getInt(2)))
        }
        return countsMap
    }


    private fun getKeyWithDeviceIdAndAccountId(key: String, deviceId: String): String {
        return "$key:$deviceId:${cleverTapInstanceConfig.accountId}"
    }

    private fun createInApp(
        inAppJson: String,
        id: String,
        exclude: Boolean = false,
        maxDisplayCount: Int = 1000,
        totalLifetimeCount: Int = -1,
        totalDailyCount: Int = -1
    ): CTInAppNotification {
        val json = JSONObject(inAppJson)
        json.put(Constants.INAPP_ID_IN_PAYLOAD, id)
        json.put(Constants.KEY_EFC, if (exclude) 1 else 0)
        val displayParams = json.optJSONObject(Constants.INAPP_WINDOW)
        if (displayParams != null) {
            displayParams.put(Constants.INAPP_MAX_DISPLAY_COUNT, maxDisplayCount)
            json.put(Constants.INAPP_WINDOW, displayParams)
        } else {
            json.put(Constants.INAPP_MAX_DISPLAY_COUNT, maxDisplayCount)
        }
        json.put(Constants.KEY_TLC, totalLifetimeCount)
        json.put(Constants.KEY_TDC, totalDailyCount)
        return CTInAppNotification(json, true)
    }

    private fun createInAppFCManager(deviceId: String = "deviceId"): InAppFCManager {
        return InAppFCManager(
            appCtx,
            cleverTapInstanceConfig,
            deviceId,
            mockk(relaxed = true),
            impressionManager,
            MockCTExecutors(),
            clock
        )
    }
}