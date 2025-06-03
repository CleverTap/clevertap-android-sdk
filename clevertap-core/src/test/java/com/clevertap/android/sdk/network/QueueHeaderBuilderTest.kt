package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Test
import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import kotlin.use

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

        assertThat(header).isNotNull()
        assertThat(header!!.optString(Constants.D_SRC)).isEqualTo(caller)
    }

    @Test
    fun `header contains debug true when debug level is 3`() {
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.getDebugLevel()).thenReturn(3)
            val header = builder.buildHeader(null)
            assertThat(header).isNotNull()
            assertThat(header!!.optBoolean("debug")).isTrue()
        }
    }

    @Test
    fun `header object is null when accountId and accountToken are null`() {
        every { config.accountId } returns null
        every { config.accountToken } returns null

        val header = builder.buildHeader("caller")
        assertThat(header).isNull()
    }

    @Test
    fun `install referer data is not appended multiple times`() {
        every { coreMetaData.isInstallReferrerDataSent } returns true

        val header = builder.buildHeader("test_caller")!!
        assertThat(header).isNotNull()
        assertThat(header.optLong("rct")).isEqualTo(0)
        assertThat(header.optLong("ait")).isEqualTo(0)
    }

    @Test
    fun `arp header is dropped when length is 0`() {
        every { arpRepo.getARP(any()) } returns JSONObject()

        val header1 = builder.buildHeader("caller")!!
        assertThat(header1).isNotNull()
        assertThat(header1.optJSONObject("arp")).isNull()

        every { arpRepo.getARP(any()) } returns null

        val header2 = builder.buildHeader("caller")!!
        assertThat(header2).isNotNull()
        assertThat(header2.optJSONObject("arp")).isNull()
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
        every { coreMetaData.wzrkParams } returns JSONObject().apply { put("wzrk_key", "wzrk_value") }
        every { coreMetaData.source } returns "src"
        every { coreMetaData.medium } returns "med"
        every { coreMetaData.campaign } returns "camp"

        // Mock ijRepo
        every { ijRepo.getI(any()) } returns 7
        every { ijRepo.getJ(any()) } returns 8

        val inappsJson = JSONArray().apply {
            JSONObject().apply {
                put("inappId", "inappId")
                put("todayCount", 2)
                put("lifetimeCount", 22)
            }
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

        assertThat(header).isNotNull()
        // Basic fields
        assertThat(header.optString(Constants.D_SRC)).isEqualTo("test_caller")
        assertThat(header.optString("g")).isEqualTo("device_id_123")
        assertThat(header.optString("type")).isEqualTo("meta")
        assertThat(header.optJSONObject("af")?.optString("foo")).isEqualTo("bar")
        assertThat(header.optInt("_i")).isEqualTo(7)
        assertThat(header.optInt("_j")).isEqualTo(8)
        assertThat(header.optString("id")).isEqualTo("test_account_id")
        assertThat(header.optString("tk")).isEqualTo("test_token")
        assertThat(header.optInt("l_ts")).isEqualTo(456)
        assertThat(header.optInt("f_ts")).isEqualTo(123)
        assertThat(header.optString("ct_pi")).contains("Email")
        assertThat(header.optString("ct_pi")).contains("Identity")
        assertThat(header.optBoolean("ddnd")).isFalse()
        assertThat(header.optInt("bk")).isEqualTo(1)
        assertThat(header.optJSONArray("rtl")?.length()).isEqualTo(2)
        assertThat(header.optLong("rct")).isEqualTo(111L)
        assertThat(header.optLong("ait")).isEqualTo(222L)
        assertThat(header.optBoolean("frs")).isTrue()
        assertThat(header.optBoolean("debug")).isFalse()
        assertThat(header.optJSONObject("arp")?.optString("arp_key")).isEqualTo("arp_val")
        val ref = header.optJSONObject("ref")!!
        assertThat(ref.optString("us")).isEqualTo("src")
        assertThat(ref.optString("um")).isEqualTo("med")
        assertThat(ref.optString("uc")).isEqualTo("camp")
        val wzrk = header.optJSONObject("wzrk_ref")!!
        assertThat(wzrk.optString("wzrk_key")).isEqualTo("wzrk_value")
        assertThat(header.optInt("imp")).isEqualTo(5)
        assertThat(header.optJSONArray("tlc")?.toString()).isEqualTo(inappsJson.toString())
    }
}
