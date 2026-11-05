package com.clevertap.android.sdk.pushnotification.preview

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PushPreviewStoreTest : BaseTestCase() {

    private lateinit var dbAdapter: DBAdapter
    private lateinit var databaseManager: BaseDatabaseManager
    private lateinit var clock: TestClock
    private lateinit var store: PushPreviewStore

    private val nowMillis = 1_793_896_379_000L

    @Before
    override fun setUp() {
        super.setUp()
        dbAdapter = mockk(relaxed = true)
        databaseManager = mockk(relaxed = true)
        every { databaseManager.loadDBAdapter(any()) } returns dbAdapter
        clock = TestClock(nowMillis)
        store = PushPreviewStore(
            context = appCtx,
            databaseManager = databaseManager,
            executors = MockCTExecutors(),
            clock = clock,
            logger = TestLogger(),
            accountId = "ACC-1",
        )
    }

    private val customHtml = """{"type":"custom-html"}"""

    private fun sendTest(inApp: String = customHtml): Bundle = Bundle().apply {
        putString(Constants.NOTIFICATION_TAG, "true")
        putString(Constants.NOTIFICATION_ID_TAG, "0_0")
        putString(Constants.WZRK_COLLAPSE, "1699358338_1793896187")
        putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY, "custom-html")
        putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, inApp)
    }

    private fun keyOf(extras: Bundle) = PushPreviewKey.of(PushPreviewPayload.fromBundle(extras)!!)

    // ---------- remember ----------

    @Test
    fun `remember does nothing for a push without preview fields`() {
        val ordinary = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
            putString(Constants.NOTIFICATION_ID_TAG, "campaign-1")
        }

        store.remember(ordinary)

        verify(exactly = 0) { dbAdapter.savePushPreview(any(), any(), any(), any()) }
    }

    @Test
    fun `remember saves the preview fields under the content key with a 24 hour default expiry`() {
        val extras = sendTest()
        val key = slot<String>()
        val data = slot<String>()
        val expires = slot<Long>()
        val collapse = slot<String?>()

        store.remember(extras)

        verify(exactly = 1) {
            dbAdapter.savePushPreview(capture(key), capture(data), capture(expires), captureNullable(collapse))
        }
        assertEquals(keyOf(extras), key.captured)
        val json = JSONObject(data.captured)
        assertEquals("custom-html", json.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY))
        assertEquals(customHtml, json.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY))
        assertEquals(TimeUnit.MILLISECONDS.toSeconds(nowMillis) + PushPreviewStore.MAX_TTL_SECONDS, expires.captured)
        assertEquals("1699358338_1793896187", collapse.captured)
    }

    @Test
    fun `remember honours a shorter wzrk_ttl sent as a string`() {
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis)
        val extras = sendTest().apply { putString(Constants.WZRK_TIME_TO_LIVE, (nowSeconds + 3600).toString()) }
        val expires = slot<Long>()

        store.remember(extras)

        verify { dbAdapter.savePushPreview(any(), any(), capture(expires), any()) }
        assertEquals(nowSeconds + 3600, expires.captured)
    }

    @Test
    fun `remember honours a wzrk_ttl sent as a long by push amplification`() {
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis)
        val extras = sendTest().apply { putLong(Constants.WZRK_TIME_TO_LIVE, nowSeconds + 60) }
        val expires = slot<Long>()

        store.remember(extras)

        verify { dbAdapter.savePushPreview(any(), any(), capture(expires), any()) }
        assertEquals(nowSeconds + 60, expires.captured)
    }

    @Test
    fun `remember captures the fields before returning so later bundle mutation is ignored`() {
        val extras = sendTest()
        val savedData = slot<String>()
        // Executors run inline in this test, so simulate the race by checking the stored copy
        // matches the bundle as it was at call time.
        store.remember(extras)
        extras.putString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY, """{"type":"changed"}""")

        verify { dbAdapter.savePushPreview(any(), capture(savedData), any(), any()) }
        assertEquals(customHtml, JSONObject(savedData.captured).getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY))
    }

    // ---------- the recent-key guard ----------

    @Test
    fun `second sighting of the same push within the window does not touch the database`() {
        val extras = sendTest()

        store.remember(extras)          // _createNotification
        clock.advanceTime(220)
        store.remember(sendTest())      // pushNotificationViewedEvent, same content

        verify(exactly = 1) { dbAdapter.savePushPreview(any(), any(), any(), any()) }
    }

    @Test
    fun `same push seen again after the window is saved again to refresh its expiry`() {
        store.remember(sendTest())
        clock.advanceTime(10_001)
        store.remember(sendTest())

        verify(exactly = 2) { dbAdapter.savePushPreview(any(), any(), any(), any()) }
    }

    @Test
    fun `different previews within the window are all saved`() {
        store.remember(sendTest("""{"type":"a"}"""))
        store.remember(sendTest("""{"type":"b"}"""))

        verify(exactly = 2) { dbAdapter.savePushPreview(any(), any(), any(), any()) }
    }

    @Test
    fun `guard evicts the oldest key once sixteen newer keys were written`() {
        val first = sendTest("""{"type":"first"}""")
        store.remember(first)
        for (i in 1..16) {
            store.remember(sendTest("""{"type":"other-$i"}"""))
        }

        store.remember(first) // evicted from the guard, so it hits the database again

        verify(exactly = 2) { dbAdapter.savePushPreview(keyOf(first), any(), any(), any()) }
    }

    // ---------- find ----------

    @Test
    fun `find returns a bundle built from the stored copy only`() {
        val tapped = sendTest().apply { putString(Constants.WZRK_ACCT_ID_KEY, "ACC-1") }
        val storedJson = PushPreviewPayload.fromBundle(sendTest())!!.toJson().toString()
        val nowSeconds = TimeUnit.MILLISECONDS.toSeconds(nowMillis)
        every { dbAdapter.findPushPreview(keyOf(tapped), nowSeconds) } returns storedJson

        val trusted = store.find(tapped)

        assertNotNull(trusted)
        assertEquals(2, trusted.size())
        assertEquals("custom-html", trusted.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY))
        assertEquals(customHtml, trusted.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY))
    }

    @Test
    fun `find returns null when the store has no row for the key`() {
        every { dbAdapter.findPushPreview(any(), any()) } returns null

        assertNull(store.find(sendTest()))
    }

    @Test
    fun `find returns null when the stored copy is unreadable`() {
        every { dbAdapter.findPushPreview(any(), any()) } returns "not json"

        assertNull(store.find(sendTest()))
    }

    @Test
    fun `find returns null without a database call for a bundle without preview fields`() {
        val ordinary = Bundle().apply { putString(Constants.NOTIFICATION_TAG, "true") }

        assertNull(store.find(ordinary))
        verify(exactly = 0) { dbAdapter.findPushPreview(any(), any()) }
    }

    // ---------- expiry rule ----------

    @Test
    fun `expiry is capped at 24 hours whatever the push says`() {
        val now = 1_000_000L
        val cap = now + PushPreviewStore.MAX_TTL_SECONDS

        assertEquals(cap, PushPreviewStore.expiresAtSeconds(null, now))                 // missing
        assertEquals(cap, PushPreviewStore.expiresAtSeconds(now - 5, now))              // in the past
        assertEquals(cap, PushPreviewStore.expiresAtSeconds(now, now))                  // exactly now
        assertEquals(now + 7200, PushPreviewStore.expiresAtSeconds(now + 7200, now))    // shorter wins
        assertEquals(cap, PushPreviewStore.expiresAtSeconds(cap, now))                  // equal to cap
        assertEquals(cap, PushPreviewStore.expiresAtSeconds(now + 315_360_000L, now))   // ten years
    }
}
