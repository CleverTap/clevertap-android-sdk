package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.areAppNotificationsEnabled
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueHeaderBuilderTest {
    private val context = mockk<Context>(relaxed = true)
    private val config = mockk<CleverTapInstanceConfig>(relaxed = true)
    private val coreMetaData = mockk<CoreMetaData>(relaxed = true)
    private val controllerManager = mockk<ControllerManager>(relaxed = true)
    private val deviceInfo = mockk<DeviceInfo>(relaxed = true)
    private val arpRepo = mockk<ArpRepo>(relaxed = true)
    private val ijRepo = mockk<IJRepo>(relaxed = true)
    private val databaseManager = mockk<BaseDatabaseManager>(relaxed = true)
    private val validationResultStack = mockk<ValidationResultStack>(relaxed = true)
    private val firstRequestTs = { 123 }
    private val lastRequestTs = { 456 }
    private val logger = mockk<ILogger>(relaxed = true)

    private val builder = QueueHeaderBuilder(
        context = context,
        config = config,
        coreMetaData = coreMetaData,
        controllerManager = controllerManager,
        deviceInfo = deviceInfo,
        arpRepo = arpRepo,
        ijRepo = ijRepo,
        databaseManager = databaseManager,
        validationResultStack = validationResultStack,
        firstRequestTs = firstRequestTs,
        lastRequestTs = lastRequestTs,
        logger = logger
    )

    @Test
    fun `buildHeader should include caller when provided`() {

        val caller = "test_caller"
        val header = builder.buildHeader(caller)

        assertNotNull(header)
        assertEquals(caller, header!!.optString(Constants.D_SRC))
    }

    @Test
    fun `header contains debug true when debug level is 3`() {
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.getDebugLevel() } returns 3
            val header = builder.buildHeader(null)
            assertNotNull(header)
            assertTrue(header!!.optBoolean("debug"))
        }
    }

    @Test
    fun `header object is null when accountId and accountToken are null`() {
        every { config.accountId } returns null
        every { config.accountToken } returns null

        val header = builder.buildHeader("caller")
        assertNull(header)
    }

    @Test
    fun `install referer data is not appended multiple times`() {
        every { coreMetaData.isInstallReferrerDataSent } returns true

        val header = builder.buildHeader("test_caller")!!
        assertNotNull(header)
        assertEquals(0, header.optLong("rct"))
        assertEquals(0, header.optLong("ait"))
    }

    @Test
    fun `arp header is dropped when length is 0`() {
        every { arpRepo.getARP(any()) } returns JSONObject()

        val header1 = builder.buildHeader("caller")!!
        assertNotNull(header1)
        assertNull(header1.optJSONObject("arp"))

        every { arpRepo.getARP(any()) } returns null

        val header2 = builder.buildHeader("caller")!!
        assertNotNull(header2)
        assertNull(header2.optJSONObject("arp"))
    }

    @Test
    fun `buildHeader should include all expected fields`() {
        // Mock config fields
        every { config.accountId } returns "test_account_id"
        every { config.accountToken } returns "test_token"

        // Mock deviceInfo
        every { deviceInfo.deviceID } returns "device_id_123"
        val appFields = JSONObject().apply { put("foo", "bar") }
        every { deviceInfo.appLaunchedFields } returns appFields

        // Mock coreMetaData
        every { coreMetaData.isWebInterfaceInitializedExternally } returns true
        every { coreMetaData.isBgPing } returns true
        every { coreMetaData.isInstallReferrerDataSent } returns false
        every { coreMetaData.referrerClickTime } returns 111L
        every { coreMetaData.appInstallTime } returns 222L
        every { coreMetaData.isFirstRequestInSession } returns true
        every { coreMetaData.wzrkParams } returns JSONObject().apply {
            put(
                "wzrk_key",
                "wzrk_value"
            )
        }
        every { coreMetaData.source } returns "src"
        every { coreMetaData.medium } returns "med"
        every { coreMetaData.campaign } returns "camp"

        // Mock ijRepo
        every { ijRepo.getI(any()) } returns 7
        every { ijRepo.getJ(any()) } returns 8

        val inappsJson = JSONArray().apply {
            put(JSONObject().apply { // Added put to actually add the JSONObject to JSONArray
                put("inappId", "inappId")
                put("todayCount", 2)
                put("lifetimeCount", 22)
            })
        }
        // Mock controllerManager
        every { controllerManager.pushProviders } returns null
        every { controllerManager.inAppFCManager } returns mockk {
            every { shownTodayCount } returns 5
            every { getInAppsCount(any()) } returns inappsJson
        }

        // Mock context
        every { context.areAppNotificationsEnabled() } returns true

        // Mock databaseManager
        val dbAdapter = mockk<DBAdapter>(relaxed = true)
        every { dbAdapter.fetchPushNotificationIds() } returns listOf("p1", "p2").toTypedArray()
        every { databaseManager.loadDBAdapter(any()) } returns dbAdapter

        // Mock arpRepo
        every { arpRepo.getARP(any()) } returns JSONObject().apply { put("arp_key", "arp_val") }

        val header = builder.buildHeader("test_caller")!!

        assertNotNull(header)
        // Basic fields
        assertEquals("test_caller", header.optString(Constants.D_SRC))
        assertEquals("device_id_123", header.optString("g"))
        assertEquals("meta", header.optString("type"))
        assertEquals("bar", header.optJSONObject("af")?.optString("foo"))
        assertEquals(7, header.optInt("_i"))
        assertEquals(8, header.optInt("_j"))
        assertEquals("test_account_id", header.optString("id"))
        assertEquals("test_token", header.optString("tk"))
        assertEquals(456, header.optInt("l_ts"))
        assertEquals(123, header.optInt("f_ts"))
        assertTrue(header.optString("ct_pi").contains("Email"))
        assertTrue(header.optString("ct_pi").contains("Identity"))
        assertFalse(header.optBoolean("ddnd"))
        assertEquals(1, header.optInt("bk"))
        assertEquals(2, header.optJSONArray("rtl")?.length())
        assertEquals(111L, header.optLong("rct"))
        assertEquals(222L, header.optLong("ait"))
        assertTrue(header.optBoolean("frs"))
        assertFalse(header.optBoolean("debug"))
        assertEquals("arp_val", header.optJSONObject("arp")?.optString("arp_key"))
        val ref = header.optJSONObject("ref")!!
        assertEquals("src", ref.optString("us"))
        assertEquals("med", ref.optString("um"))
        assertEquals("camp", ref.optString("uc"))
        val wzrk = header.optJSONObject("wzrk_ref")!!
        assertEquals("wzrk_value", wzrk.optString("wzrk_key"))
        assertEquals(5, header.optInt("imp"))
        assertEquals(inappsJson.toString(), header.optJSONArray("tlc")?.toString())
    }
}