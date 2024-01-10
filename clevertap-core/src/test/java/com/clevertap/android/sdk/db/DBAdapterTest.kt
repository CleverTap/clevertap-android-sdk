package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.Test
import org.junit.jupiter.api.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class DBAdapterTest : BaseTestCase() {

    private lateinit var dbAdapter: DBAdapter
    private lateinit var instanceConfig: CleverTapInstanceConfig

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbAdapter = DBAdapter(appCtx, instanceConfig)
    }

    @After
    fun deleteDB() {
        dbAdapter.deleteDB()
    }

    @Test
    fun test_deleteMessageForId_when_MessageIDAndUserIDIsPassed_should_DeleteMessageIfExists() {
        //case 1 : when msgId or user id is null, false is returned
        var result = dbAdapter.deleteMessageForId(messageId = null, userId = null)
        assertEquals(false, result)

        //case 2 : when msgId or user id is not null, the sqlite query is executed accordingly on the table and therefore true is returned. note, even empty values for msg or user id are allowed
        val msgId = "msg_1234"
        val userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        var msgList = dbAdapter.getMessages(userID)
        assertEquals(1, msgList.size)

        result = dbAdapter.deleteMessageForId(msgId, userID)
        assertEquals(true, result)
        msgList = dbAdapter.getMessages(userID)
        assertEquals(0, msgList.size)
    }

    @Test
    fun test_deleteMessagesForIds_when_MessageIDAndUserIdIsPassed_should_DeleteMessagesIfExists() {
        // case 1:when msdIds and userId is null, false should be returned
        var result = dbAdapter.deleteMessagesForIDs(messageIDs = null, userId = null)
        assertFalse(result)

        var userId = "user_11"

        //case 2: when msgIds and userId is non-null, false should be returned
        result = dbAdapter.deleteMessagesForIDs(messageIDs = null, userId)
        assertFalse(result)

        val msgIds = mutableListOf<String>()
        //case 3: when msgIds is non-null and userId is null, false should be returned
        result = dbAdapter.deleteMessagesForIDs(msgIds, userId = null)
        assertFalse(result)

        //case 4: when msgIds and userId is non-null, sqlite query is run and true is returned
        userId = "user_11"
        msgIds.add("msg_1")
        msgIds.add("msg_2")
        msgIds.add("msg_3")

        //When all msgIds are present in the db all should be deleted
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        var msgList = dbAdapter.getMessages(userId)
        assertEquals(3, msgList.size)

        result = dbAdapter.deleteMessagesForIDs(msgIds, userId)
        assertTrue(result)
        msgList = dbAdapter.getMessages(userId)
        assertEquals(0, msgList.size)

        // When some msgId from msgIds is not found, it is skipped and remaining are deleted
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        msgList = dbAdapter.getMessages(userId)
        assertEquals(3, msgList.size)

        msgIds.removeLast()
        msgIds.add("msg_4")
        result = dbAdapter.deleteMessagesForIDs(msgIds, userId)
        msgList = dbAdapter.getMessages(userId)
        assertTrue(result)
        assertEquals(1, msgList.size)
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
        dbAdapter.storeUserProfile(
            "userID",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })

        //validation : profile is fetched
        dbAdapter.fetchUserProfileById("userID")!!.let {
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
        assertEquals(0, dbAdapter.getLastUninstallTimestamp())

        //assertion: store current time as uninstall time
        val currentTime = System.currentTimeMillis()
        dbAdapter.storeUninstallTimestamp()
        //TODO: This could be tested in a better way when DBAdapter can be provided with a test clock
        //validation : the last uninstall timestamp is returned(can differ by 1-2 seconds based on processor speed, so taking a range in here of max 2 seconds
        assertTrue(dbAdapter.getLastUninstallTimestamp() in currentTime..(currentTime + 2000))
    }

    @Test
    fun test_getMessages_when_FunctionIsCalledWithCorrectUserID_should_ReturnAllAssociatedMessages() {
        // case : incorrect user id
        var msgList = dbAdapter.getMessages("unavailableUser")
        assertEquals(0, msgList.size)

        // case correct user id
        val msgId = "msg_1234"
        val userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        msgList = dbAdapter.getMessages(userID)
        assertEquals(1, msgList.size)
        assertEquals(msgId, msgList[0].id)
        assertEquals(userID, msgList[0].userId)
    }

    @Test
    fun test_markReadMessageForId_when_CorrectUserIdAndMessageIsPassed_should_SetMessageIdAsRead() {
        val msgId = "msg_1234"
        var userID = "user_11"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        dbAdapter.markReadMessageForId(msgId, userID)
        var msg = dbAdapter.getMessages(userID)[0]
        assertTrue(msg.isRead == 1)

        userID = "user_12"
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        dbAdapter.markReadMessageForId("msgId", userID)
        msg = dbAdapter.getMessages(userID)[0]
        assertFalse(msg.isRead == 1)
    }

    @Test
    fun test_markReadMessagesForIds_when_CorrectUserIdAndMessagesArePassed_should_SetMessageIdsAsRead() {
        // case 1:when msdIds and userId is null, false should be returned
        var result: Boolean = dbAdapter.markReadMessagesForIds(messageIDs = null, userId = null)
        assertFalse(result)

        var userId = "user_11"
        //case 2: when msgIds is null and userId is non-null, false should be returned
        result = dbAdapter.markReadMessagesForIds(messageIDs = null, userId)
        assertFalse(result)

        val msgIds = mutableListOf<String>()
        //case 3: when msgIds is non-null and userId is null, false should be returned
        result = dbAdapter.markReadMessagesForIds(msgIds, userId = null)
        assertFalse(result)

        userId = "user_11"
        msgIds.add("msg_1")
        msgIds.add("msg_2")
        msgIds.add("msg_3")

        //case 4: when msgIds and userId is non-null, sqlite query is run and true is returned

        //When all msgIds are present in the db all should be marked as read
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        result = dbAdapter.markReadMessagesForIds(msgIds, userId)
        var msgList = dbAdapter.getMessages(userId)
        assertTrue(result)
        for (msg in msgList)
            assertTrue(msg.isRead == 1)

        // When some msgId from msgIds is not found, it is skipped and remaining are marked as read
        userId = "user_12"
        dbAdapter.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        msgIds.removeLast()
        msgIds.add("msg_4")
        result = dbAdapter.markReadMessagesForIds(msgIds, userId)
        msgList = dbAdapter.getMessages(userId)
        assertTrue(result)
        for (msg in msgList) {
            if (msg.id in msgIds)
                assertTrue(msg.isRead == 1)
            else
                assertFalse(msg.isRead == 1)
        }
    }

    @Test
    fun test_removeUserProfile() {
        // assumption
        dbAdapter.storeUserProfile(
            "userID",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })
        assertNotNull(dbAdapter.fetchUserProfileById("userID"))

        //test
        dbAdapter.removeUserProfile("userID")

        //validation
        assertNull(dbAdapter.fetchUserProfileById("userID"))
    }

    @Test
    fun test_storeUserProfile() {
        // test
        dbAdapter.storeUserProfile(
            "userID",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") })

        //validation
        dbAdapter.fetchUserProfileById("userID").let {
            assertNotNull(it)
            assertEquals("john", it.getString("name"))
            assertEquals("daniel", it.getString("father"))

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
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = false, campaignId = "cp1234")))

        //validate
        assertNotNull(dbAdapter.getMessages("user_11")[0])

        //when a message is not present it will insert the message
        //test
        dbAdapter.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = true, campaignId = "cp4321")))

        //validate
        assertEquals(1, dbAdapter.getMessages("user_11").size)
        assertEquals(true, dbAdapter.getMessages("user_11")[0].isRead == 1)
        assertEquals("cp4321", dbAdapter.getMessages("user_11")[0].campaignId)
    }

    @Test
    fun test_fetchEvents_when_Called_should_ReturnAListOfEntriesAsJsonObject() {
        //when calling this function, it will return all the entries fro the given table less than or equal to passed limit.
        // the returned list of entries will be of format {'key' : <jsonArray> } where key is the last index of entries

        //note : this function is not supposed to work with following tables as they have separate functions with different insertion rules :
        // Table.USER_PROFILES, Table.PUSH_NOTIFICATIONS, Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            println("table:$table")

            //assertion
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}4") }, table)

            //test
            dbAdapter.fetchEvents(table, 2).let {  /// {2: ["__","__"]}

                //validation
                println("jsonObject = $it")
                val arr = it!!.getJSONArray("2")
                assertEquals(2, arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.tableName}1", (arr[0] as JSONObject).getString("name"))
                assertEquals("${table.tableName}2", (arr[1] as JSONObject).getString("name"))
            }
        }
    }

    @Test
    fun test_removeEvents_when_called_should_RemoveAllEntries() {
        //note : will not work with Table.USER_PROFILES, Table.PUSH_NOTIFICATIONS, Table.INBOX_MESSAGES, Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            println("table:$table")
            //assertion
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            println("jsonObject = ${dbAdapter.fetchEvents(table, Int.MAX_VALUE)}")

            //test
            dbAdapter.removeEvents(table)

            //validation
            dbAdapter.fetchEvents(table, Int.MAX_VALUE).let {
                println("jsonObject = $it")
                assertNull(it)
            }
        }
    }

    @Test
    fun test_storeObject_when_called_should_storeTheObjectInGivenTable() {
        //when calling this function, it will store all the entries in the given table
        //note : this function is not supposed to work with following tables as they have separate functions with different insertion rules :
        // Table.USER_PROFILES, Table.PUSH_NOTIFICATIONS, Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            println("table:$table")

            //test
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}4") }, table)

            //validation
            dbAdapter.fetchEvents(table, Int.MAX_VALUE).let {

                println("jsonObject = $it")
                val arr = it!!.getJSONArray("4")
                assertEquals(4, arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.tableName}1", (arr[0] as JSONObject).getString("name"))
                assertEquals("${table.tableName}2", (arr[1] as JSONObject).getString("name"))
            }
        }
    }

    @Test
    fun test_cleanupEventsFromLastId_when_called_should_removeAllEntriesWithIdLesserThanPassedId() {
        //note : this function is not supposed to work with following tables as they have separate functions with different insertion rules :
        // Table.USER_PROFILES, Table.PUSH_NOTIFICATIONS, Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table ->
            println("table:$table")

            //assert
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}1") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}2") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}3") }, table)
            dbAdapter.storeObject(JSONObject().also { it.put("name", "${table.tableName}4") }, table)
            println("jsonObject = ${dbAdapter.fetchEvents(table, Int.MAX_VALUE)}")

            //test
            dbAdapter.cleanupEventsFromLastId("2", table)//will remove ids 1 & 2 , and will save ids 3 & 4

            //validation
            println("after")
            dbAdapter.fetchEvents(table, Int.MAX_VALUE).let {
                println("jsonObject = $it")
                val arr = it!!.getJSONArray("4")
                assertEquals(2, arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.tableName}3", (arr[0] as JSONObject).getString("name"))
                assertEquals("${table.tableName}4", (arr[1] as JSONObject).getString("name"))

            }
        }
    }

    @Test
    @Ignore("This could be tested when DBAdapter can be provided with a test clock")
    fun test_cleanUpPushNotifications_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        dbAdapter.cleanUpPushNotifications()
    }

    @Test
    @Ignore("This could be tested when DBAdapter can be provided with a test clock")
    fun test_cleanStaleEvents_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        dbAdapter.cleanupStaleEvents(Table.EVENTS)
    }

    @Test
    fun test_updatePushNotificationIds_when_CalledWithAListOfIds_should_MarkAssociatedEntriesInTableAsRead() {

        //assert: adding unread notifications to database. fetchPushNotificationIds returns the list of unread notifications
        val notifPairs = listOf(
            "pn1" to TimeUnit.DAYS.toMillis(1),
            "pn2" to TimeUnit.DAYS.toMillis(2),
            "pn3" to TimeUnit.DAYS.toMillis(0),
            "pn4" to TimeUnit.DAYS.toMillis(-1),
            "pn5" to TimeUnit.DAYS.toMillis(-2),
        )
        notifPairs.forEach { dbAdapter.storePushNotificationId(it.first, it.second) }
        println(dbAdapter.fetchPushNotificationIds().toList())//[pn1,pn2,pn3]

        //test: calling updatePushNotificationIds with 2 notif ids
        dbAdapter.updatePushNotificationIds(arrayOf("pn1", "pn3"))

        //validate: those 2 ids will now not be part of list of notifs that are unread implying that these are now marked as read
        // note the flag rtlDirtyFlag impacts the list of data returned by fetchPushNotificationIds.
        // so for the sake of testing the database, we add another notification to set rtlDirtyFlag to true
        dbAdapter.storePushNotificationId("temp", TimeUnit.DAYS.toMillis(1))

        dbAdapter.fetchPushNotificationIds().let {
            println(it.toList())
            assertFalse(it.contains("pn1"))
            assertTrue(it.contains("pn2"))
            assertFalse(it.contains("pn3"))
            assertTrue(it.contains("pn4"))
            assertTrue(it.contains("pn5"))
            assertTrue(it.contains("temp"))
        }
    }

    private fun getCtMsgDao(
        id: String = "1",
        userId: String = "1",
        read: Boolean = false,
        jsonData: JSONObject = JSONObject(),
        date: Long = System.currentTimeMillis(),
        expires: Long = (System.currentTimeMillis() * 10),
        tags: List<String> = listOf(),
        campaignId: String = "campaignID",
        wzrkParams: JSONObject = JSONObject()
    ): CTMessageDAO {
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
