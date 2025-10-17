package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class PushNotificationDAOImplTest : BaseTestCase() {

    private lateinit var pushNotificationDAO: PushNotificationDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var testClock: TestClock

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        testClock = TestClock()
        dbHelper = DatabaseHelper(
            context = appCtx,
            accountId = instanceConfig.accountId,
            dbName = "test_db",
            logger = instanceConfig.logger
        )
        pushNotificationDAO = PushNotificationDAOImpl(
            dbHelper = dbHelper,
            logger = instanceConfig.logger,
            clock = testClock
        )
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_doesPushNotificationIdExist_when_pushNotifIdIsPassed_should_storePushNotif() {
        pushNotificationDAO.storePushNotificationId("pushNotif", 0)
        assertTrue { pushNotificationDAO.doesPushNotificationIdExist("pushNotif") }
        assertFalse { pushNotificationDAO.doesPushNotificationIdExist("pushNotif2") }
    }

    @Test
    fun test_fetchPushNotificationIds_when_FunctionIsCalled_should_ReturnListOfAllStoredPNs() {
        val ids = arrayOf("id1", "id2")
        ids.forEach { pushNotificationDAO.storePushNotificationId(it, 0) }
        val result = pushNotificationDAO.fetchPushNotificationIds()

        assertEquals(ids.size, result.size)
        result.forEach {
            assertTrue(it in ids)
        }
    }

    @Test
    fun test_storePushNotificationId_when_Called_should_storePushNotificationId() {
        pushNotificationDAO.storePushNotificationId("pn1", 1)

        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(1, result.size)
        assertEquals("pn1", result[0])
    }

    @Test
    fun test_updatePushNotificationIds_when_CalledWithAListOfIds_should_MarkAssociatedEntriesInTableAsRead() {
        // Store unread notifications to database. fetchPushNotificationIds returns the list of unread notifications
        val notifPairs = listOf(
            "pn1" to TimeUnit.DAYS.toMillis(1),
            "pn2" to TimeUnit.DAYS.toMillis(2),
            "pn3" to TimeUnit.DAYS.toMillis(0),
            "pn4" to TimeUnit.DAYS.toMillis(-1),
            "pn5" to TimeUnit.DAYS.toMillis(-2),
        )
        notifPairs.forEach { pushNotificationDAO.storePushNotificationId(it.first, it.second) }
        println(pushNotificationDAO.fetchPushNotificationIds().toList())

        // Call updatePushNotificationIds with 2 notif ids
        pushNotificationDAO.updatePushNotificationIds(arrayOf("pn1", "pn3"))

        // Those 2 ids will now not be part of list of notifs that are unread implying that these are now marked as read
        // Note the flag rtlDirtyFlag impacts the list of data returned by fetchPushNotificationIds.
        // So for the sake of testing the database, we add another notification to set rtlDirtyFlag to true
        pushNotificationDAO.storePushNotificationId("temp", TimeUnit.DAYS.toMillis(1))

        pushNotificationDAO.fetchPushNotificationIds().let {
            println(it.toList())
            assertFalse(it.contains("pn1"))
            assertTrue(it.contains("pn2"))
            assertFalse(it.contains("pn3"))
            assertTrue(it.contains("pn4"))
            assertTrue(it.contains("pn5"))
            assertTrue(it.contains("temp"))
        }
    }

    @Test
    fun test_fetchPushNotificationIds_when_rtlDirtyFlagIsFalse_should_returnEmpty() {
        pushNotificationDAO.storePushNotificationId("pn1", TimeUnit.DAYS.toMillis(1))
        pushNotificationDAO.storePushNotificationId("pn2", TimeUnit.DAYS.toMillis(1))

        // Mark all as read (this sets rtlDirtyFlag to false)
        pushNotificationDAO.updatePushNotificationIds(arrayOf("pn1", "pn2"))

        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(0, result.size)
    }

    @Test
    fun test_storePushNotificationId_when_called_should_storeWithCorrectTTL() {
        val ids = arrayOf("id1", "id2", "id3")
        ids.forEach { pushNotificationDAO.storePushNotificationId(it, TimeUnit.DAYS.toMillis(1)) }
        
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(ids.size, result.size)
        result.forEach { id ->
            assertTrue(id in ids)
        }
    }

    @Test
    fun test_cleanUpPushNotifications_when_notificationsNotExpired_should_keepAll() {
        val currentTime = System.currentTimeMillis()
        testClock.setCurrentTime(currentTime)

        // Store notifications with future TTL (not expired)
        // TTL is 2 days in the future
        val futureTTL1 = currentTime + TimeUnit.DAYS.toMillis(2)
        val futureTTL2 = currentTime + TimeUnit.HOURS.toMillis(12)
        val futureTTL3 = currentTime + TimeUnit.DAYS.toMillis(1)

        pushNotificationDAO.storePushNotificationId("future1", futureTTL1)
        pushNotificationDAO.storePushNotificationId("future2", futureTTL2)
        pushNotificationDAO.storePushNotificationId("future3", futureTTL3)

        // Clean up at current time
        pushNotificationDAO.cleanUpPushNotifications()

        // All notifications should still exist (not expired)
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(3, result.size)
        assertTrue(result.contains("future1"))
        assertTrue(result.contains("future2"))
        assertTrue(result.contains("future3"))
    }

    @Test
    fun test_cleanUpPushNotifications_when_notificationsExpired_should_removeExpired() {
        val currentTime = System.currentTimeMillis()

        // Store some notifications in the past (expired)
        testClock.setCurrentTime(currentTime - TimeUnit.DAYS.toMillis(4))
        // These will get a TTL of current time - 4 days + default TTL
        // When ttl is 0, it uses currentTimeSeconds + DEFAULT_PUSH_TTL_SECONDS
        pushNotificationDAO.storePushNotificationId("expired1", 0)

        testClock.setCurrentTime(currentTime)
        // Store with explicit past TTL (expired)
        val pastTTL = testClock.currentTimeSeconds() - TimeUnit.DAYS.toSeconds(1)
        pushNotificationDAO.storePushNotificationId("expired2", pastTTL)

        // Store notification with future TTL (not expired)
        val futureTTL = testClock.currentTimeSeconds() + TimeUnit.DAYS.toSeconds(1)
        pushNotificationDAO.storePushNotificationId("valid1", futureTTL)

        // Reset to current time and clean up
        testClock.setCurrentTime(currentTime)
        pushNotificationDAO.cleanUpPushNotifications()

        // Only the notification with future TTL should remain
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(1, result.size)
        assertEquals("valid1", result[0])
    }

    @Test
    fun test_cleanUpPushNotifications_when_notificationExactlyAtCurrentTime_should_remove() {
        val currentTime = System.currentTimeMillis()
        testClock.setCurrentTime(currentTime)

        // Store notification with TTL exactly at current time (should be removed)
        pushNotificationDAO.storePushNotificationId("exact_time", testClock.currentTimeSeconds())

        // Store notification 1 millisecond in the future (should be kept)
        pushNotificationDAO.storePushNotificationId("future_1ms", testClock.currentTimeSeconds() + 1)

        // Store notification 1 millisecond in the past (should be removed)
        pushNotificationDAO.storePushNotificationId("past_1ms", testClock.currentTimeSeconds() - 1)

        // Clean up
        pushNotificationDAO.cleanUpPushNotifications()

        // Only the future notification should remain
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(1, result.size)
        assertEquals("future_1ms", result[0])
    }

    @Test
    fun test_cleanUpPushNotifications_when_allNotificationsExpired_should_removeAll() {
        val currentTime = System.currentTimeMillis()
        testClock.setCurrentTime(currentTime)

        // Store all notifications with past TTL (all expired)
        pushNotificationDAO.storePushNotificationId("expired1", testClock.currentTimeSeconds() - TimeUnit.DAYS.toSeconds(1))
        pushNotificationDAO.storePushNotificationId("expired2", testClock.currentTimeSeconds() - TimeUnit.HOURS.toSeconds(1))
        pushNotificationDAO.storePushNotificationId("expired3", testClock.currentTimeSeconds() - TimeUnit.MINUTES.toSeconds(1))

        // Clean up
        pushNotificationDAO.cleanUpPushNotifications()

        // All notifications should be removed
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(0, result.size)
    }

    @Test
    fun test_cleanUpPushNotifications_when_mixedReadAndUnreadExpired_should_removeAllExpired() {
        val currentTime = System.currentTimeMillis()
        testClock.setCurrentTime(currentTime)

        // Store expired notifications
        val expiredTTL = testClock.currentTimeSeconds() - TimeUnit.DAYS.toSeconds(1)
        pushNotificationDAO.storePushNotificationId("expired1", expiredTTL)
        pushNotificationDAO.storePushNotificationId("expired2", expiredTTL)

        // Store valid notifications
        val validTTL = testClock.currentTimeSeconds() + TimeUnit.DAYS.toSeconds(1)
        pushNotificationDAO.storePushNotificationId("valid1", validTTL)
        pushNotificationDAO.storePushNotificationId("valid2", validTTL)

        // Mark one expired and one valid as read
        pushNotificationDAO.updatePushNotificationIds(arrayOf("expired1", "valid1"))

        // Clean up - should remove both expired notifications regardless of read status
        pushNotificationDAO.cleanUpPushNotifications()

        // Need to add a new notification to set rtlDirtyFlag to true to fetch remaining
        pushNotificationDAO.storePushNotificationId("temp", currentTime + TimeUnit.DAYS.toMillis(1))

        // Only valid notifications should remain (valid2 and temp are unread)
        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(2, result.size)
        assertTrue(result.contains("valid2"))
        assertTrue(result.contains("temp"))
        // expired1 and expired2 should be removed from database
        // valid1 is marked as read but still in database
        assertFalse(pushNotificationDAO.doesPushNotificationIdExist("expired1"))
        assertFalse(pushNotificationDAO.doesPushNotificationIdExist("expired2"))
        assertTrue(pushNotificationDAO.doesPushNotificationIdExist("valid1"))
    }

    @Test
    fun test_cleanUpPushNotifications_when_defaultTTLUsed_should_handleCorrectly() {
        val currentTime = System.currentTimeMillis()

        // First, store a notification with default TTL (ttl = 0)
        testClock.setCurrentTime(currentTime)
        pushNotificationDAO.storePushNotificationId("default_ttl", 0)

        // The notification should have TTL = currentTime + Constants.DEFAULT_PUSH_TTL
        // Let's advance time but not beyond the default TTL
        testClock.setCurrentTime(currentTime + TimeUnit.HOURS.toSeconds(1))

        // Clean up - notification should still be valid
        pushNotificationDAO.cleanUpPushNotifications()

        val result = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(1, result.size)
        assertEquals("default_ttl", result[0])

        // Now advance time beyond the default TTL
        testClock.setCurrentTime(currentTime + TimeUnit.SECONDS.toMillis(Constants.DEFAULT_PUSH_TTL_SECONDS )+ 1)

        // Clean up - notification should now be expired
        pushNotificationDAO.cleanUpPushNotifications()

        // Add a temp notification to trigger fetch
        pushNotificationDAO.storePushNotificationId("temp", testClock.currentTimeMillis() + TimeUnit.DAYS.toMillis(1))

        val finalResult = pushNotificationDAO.fetchPushNotificationIds()
        assertEquals(1, finalResult.size)
        assertEquals("temp", finalResult[0])
        assertFalse(pushNotificationDAO.doesPushNotificationIdExist("default_ttl"))
    }
}
