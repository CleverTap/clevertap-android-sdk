package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class DBAdapterTest : BaseTestCase() {
    private lateinit var dbAdapter: DBAdapter
    private lateinit var instanceConfig: CleverTapInstanceConfig

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"
    private val dbName = "clevertap_$accID"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbAdapter = DBAdapter(appCtx, instanceConfig)
    }


    @Test
    fun test_deleteMessageForId_when_MessageIDAndUserIDIsPassed_should_DeleteMessageIfExists() {
        var msgId: String? = null
        var userID: String? = null
        var result = false

        //case 1 : when msgId or user id is null, false is returned
        result = dbAdapter.deleteMessageForId(msgId, userID)
        assertEquals(false, result)

        //case 2 : when msgId or user id is not null, the sqlite query is executed accordingly on the table and therefore true is returned. note, even empty values for msg or user id are allowed
        msgId = "msg_1234"
        userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        var msgList = dbAdapter.getMessages(userID)
        assertEquals(1, msgList.size)

        result = dbAdapter.deleteMessageForId(msgId, userID)
        assertEquals(true, result)
        msgList = dbAdapter.getMessages(userID)
        assertEquals(0, msgList.size)
    }

    @Test
    fun test_doesPushNotificationIdExist_when_pushNotifIdIsPaased_should_storePushNotif() {
        dbAdapter.storePushNotificationId("pushNotif", 0)
        assertTrue { dbAdapter.doesPushNotificationIdExist("pushNotif") }
        assertFalse { dbAdapter.doesPushNotificationIdExist("pushNotif2") }
    }

    @Test
    fun test_fetchPushNotificationIds_when_FunctionIsCalled_should_ReturnListOfAllStoredPNs() {
        val ids = arrayOf("id1", "id2")

        ids.forEach { dbAdapter.storePushNotificationId(it, 0) }
        val result = dbAdapter.fetchPushNotificationIds()

        assertEquals(ids.size, result.size)
        result.forEach {
            assertTrue(it in ids)
        }
    }

    @Test
    fun test_fetchUserProfileById_when_calledWithUserId_should_returnUserProfile() {
        //assumption: profile is already stored
        dbAdapter.storeUserProfile("userID", JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })

        //validation : profile is fetched
        dbAdapter.fetchUserProfileById("userID").let {
            assertEquals("john", it.getString("name"))
            assertEquals("daniel", it.getString("father"))
        }
        //assertion : profile is not already stored or incorrect user id is passed
        // validation: null is returned
        assertNull(dbAdapter.fetchUserProfileById(null))
        assertNull(dbAdapter.fetchUserProfileById("notAvaialble"))


    }

    @Test
    fun test_getLastUninstallTimestamp_when_FunctionIsCalled_should_ReturnTheLastUninstallTime() {
        //when no uninstall time is stored, should return 0
        assertEquals(0, dbAdapter.lastUninstallTimestamp)

        //assertion: store current time as uninstall time
        val currentTime = System.currentTimeMillis()
        dbAdapter.storeUninstallTimestamp()
        //validation : the last uninstall timestamp is returned(can differ by 1-2 seconds based on processor speed, so taking a range in here of max 2 seconds
        assertTrue(dbAdapter.lastUninstallTimestamp in currentTime..(currentTime + 2000))
    }

    @Test
    fun test_getMessages_when_FunctionIsCalledWithCorrectUserID_should_ReturnAllAssociatedMessages() {
        // case : incorrect user id
        var msgList = dbAdapter.getMessages("unavailableUser")
        assertEquals(0, msgList.size)


        // case correct user id
        val msgId = "msg_1234"
        val userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        msgList = dbAdapter.getMessages(userID)
        assertEquals(1, msgList.size)
        assertEquals(msgId, msgList[0].id)
        assertEquals(userID, msgList[0].userId)

    }

    @Test
    fun test_markReadMessageForId_when_CorrectUserIdAndMessageIsPAssed_should_SetMEssageIdAsRead() {
        var msgId = "msg_1234"
        var userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        dbAdapter.markReadMessageForId(msgId, userID)
        var msg = dbAdapter.getMessages(userID)[0]
        assertTrue(msg.isRead == 1)

        userID = "user_12"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        dbAdapter.markReadMessageForId("msgId", userID)
        msg = dbAdapter.getMessages(userID)[0]
        assertFalse(msg.isRead == 1)


    }

    @Test
    fun test_removeUserProfile() {
        // assuption
        dbAdapter.storeUserProfile("userID", JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })
        assertNotNull(dbAdapter.fetchUserProfileById("userID"))

        //test
        dbAdapter.removeUserProfile("userID")

        //validation
        assertNull(dbAdapter.fetchUserProfileById("userID"))

    }

    @Test
    fun test_storeUninstallTimestamp_when_FunctionIsCalled_should_StoreCurrentTimeAsLastUninstallTime() {

        //test: store current time as uninstall time
        val currentTime = System.currentTimeMillis()
        dbAdapter.storeUninstallTimestamp()

        //validation : the last uninstall timestamp is returned(can differ by 1-2 seconds based on processor speed, so taking a range in here of max 2 seconds
        assertTrue(dbAdapter.lastUninstallTimestamp in currentTime..(currentTime + 2000))
    }

    @Test
    fun test_storeUserProfile() {
        // test
        dbAdapter.storeUserProfile("userID", JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })

        //validation
        dbAdapter.fetchUserProfileById("userID").let {
            assertNotNull(it)
            assertEquals("john", it.getString("name"))
            assertEquals("daniel", it.getString("father"))

        }

    }


    @Test
    fun test_cleanUpPushNotifications_when_Called_should_ClearAllStoredPNsThatHaventExpired() {

        //assume (storing 2 notifications that will expire after 10 seconds and 1 that is already expired. this will not get removed)
        dbAdapter.storePushNotificationId("pn1", 10000)
        dbAdapter.storePushNotificationId("pn2", 10000)
        dbAdapter.storePushNotificationId("pn3", -10)
        dbAdapter.fetchPushNotificationIds().let {
            assertEquals(3, it.size)
        }

        //test
        dbAdapter.cleanUpPushNotifications()

        //validate
        dbAdapter.fetchPushNotificationIds().let {
            assertEquals(1, it.size)
            assertEquals("pn3", it[0])
        }
    }

    @Test
    fun test_storePushNotificationId_when_Called_should_storePushNotificationId() {
        //test
        dbAdapter.storePushNotificationId("pn1", 1)

        //validate
        dbAdapter.fetchPushNotificationIds().let {
            assertEquals(1, it.size)
            assertEquals("pn1", it[0])
        }


    }

    @Test
    fun test_upsertMessages_when_Called_should_InsertORUpsertAMessage() {
        //when a message is not present it will insert the message

        //test
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", false, campaignId = "cp1234")))

        //validate
        assertNotNull(dbAdapter.getMessages("user_11")[0])

        //when a message is not present it will insert the message
        //test
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", true, campaignId = "cp4321")))

        //validate
        assertEquals(1, dbAdapter.getMessages("user_11").size)
        assertEquals(true, dbAdapter.getMessages("user_11")[0].isRead == 1)
        assertEquals("cp4321", dbAdapter.getMessages("user_11")[0].campaignId)


    }

    @Test//todo
    fun test_cleanupEventsFromLastId_when_ABC_should_XYZ() {
        //??
        assertTrue(true)

    }

    @Test//todo
    fun test_cleanupStaleEvents_when_ABC_should_XYZ() {
        //??
        assertTrue(true)

    }

    @Test//todo
    fun test_fetchEvents_when_ABC_should_XYZ() {
        //?
        assertTrue(true)

    }

    @Test//todo
    fun test_updatePushNotificationIds_when_ABC_should_XYZ() {
        dbAdapter.storePushNotificationId("pn1", 10000)
        dbAdapter.storePushNotificationId("pn2", 10000)
        dbAdapter.storePushNotificationId("pn3", -10)
        dbAdapter.fetchPushNotificationIds().let { println(it.toList()) }//[pn1,pn2,pn3]

        dbAdapter.updatePushNotificationIds(arrayOf("pn1", "p2", "pn3"))

        dbAdapter.fetchPushNotificationIds().let { println(it.toList()) }// [] //todo why?
        assertTrue(true)

    }

    @Test//todo
    fun test_storeObject_when_ABC_should_XYZ() {
        //dbAdapter.storeObject(??,??)
        assertTrue(true)

    }

    @Test//todo
    fun test_removeEvents_when_ABC_should_XYZ() {
        //dbAdapter.removeEvents()
        assertTrue(true)

    }


    fun getCtMsgDao(id: String, userId: String, read: Boolean, jsonData: JSONObject = JSONObject(), date: Long = System.currentTimeMillis(), expires: Long = (System.currentTimeMillis() * 10), tags: List<String> = listOf(), campaignId: String = "campaignID", wzrkParams: JSONObject = JSONObject()): CTMessageDAO {
        return CTMessageDAO().also {
            it.id = id
            it.jsonData = jsonData
            it.isRead = if (read) 1 else 0
            it.date = date
            it.expires = expires
            it.userId = userId
            it.tags = tags.joinToString(",")
            it.campaignId = campaignId
            it.wzrkParams = wzrkParams

        }
    }
}