package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
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

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(appCtx, instanceConfig, "test_db", instanceConfig.logger)
        pushNotificationDAO = PushNotificationDAOImpl(dbHelper, instanceConfig.logger)
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
}
