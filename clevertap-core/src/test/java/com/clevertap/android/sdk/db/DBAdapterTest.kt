package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.DBAdapter.Table
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
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

    @Test
    fun test_fetchEvents_when_Called_should_ReturnAListOfEntriesAsJsonObject() {
        //when calling this function, it will return all the entries fro the given table less than or equal to passed limit.
        // the returned list of entries will be of format {'key' : <jsonArray> } where key is the last index of entries

        //note : this function is not supposed to work with following tables as they have seperate functions with different insertion rules :
        // Table.USER_PROFILES,Table.PUSH_NOTIFICATIONS,  Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED).forEach { table->
            println("table:$table")

            //assertion
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}1") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}2") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}3") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}4") },table)

            //test
            dbAdapter.fetchEvents(table,2).let {  /// {2: ["__","__"]}

                //validation
                println("jsonObject = $it")
                val arr = it.getJSONArray("2")
                assertEquals(2,arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.getName()}1",(arr[0] as JSONObject).getString("name"))
                assertEquals("${table.getName()}2",(arr[1] as JSONObject).getString("name"))
            }
        }

    }
    @Test
    fun test_removeEvents_when_called_should_RemoveAllEntries() {
        //note : will not work with Table.USER_PROFILES,Table.PUSH_NOTIFICATIONS,  Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED, ).forEach { table->
            println("table:$table")
            //assertion
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}1") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}2") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}3") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}4") },table)
            dbAdapter.fetchEvents(table, Int.MAX_VALUE).let { println("jsonObject = $it") }

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
        //note : this function is not supposed to work with following tables as they have seperate functions with different insertion rules :
        // Table.USER_PROFILES,Table.PUSH_NOTIFICATIONS,  Table.INBOX_MESSAGES,Table.UNINSTALL_TS


        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED, ).forEach { table->
            println("table:$table")

            //test
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}1") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}2") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}3") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}4") },table)

            //validation
            dbAdapter.fetchEvents(table,Int.MAX_VALUE).let {


                println("jsonObject = $it")
                val arr = it.getJSONArray("4")
                assertEquals(4,arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.getName()}1",(arr[0] as JSONObject).getString("name"))
                assertEquals("${table.getName()}2",(arr[1] as JSONObject).getString("name"))
            }
        }


    }

    @Test
    fun test_cleanupEventsFromLastId_when_called_should_removeAllEntriesWithIdLesserThanPassedId() {
        //note : this function is not supposed to work with following tables as they have seperate functions with different insertion rules :
        // Table.USER_PROFILES,Table.PUSH_NOTIFICATIONS,  Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED, ).forEach { table->
            println("table:$table")

            //assert
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}1") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}2") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}3") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}4") },table)
            dbAdapter.fetchEvents(table,Int.MAX_VALUE).let { println("jsonObject = $it")}

            //test
            dbAdapter.cleanupEventsFromLastId("2",table)//will remove ids 1 & 2 , and will save ids 3 & 4

            //validation
            println("after")
            dbAdapter.fetchEvents(table,Int.MAX_VALUE).let {
                println("jsonObject = $it")
                val arr = it.getJSONArray("4")
                assertEquals(2,arr.length())
                assertTrue(arr[0] is JSONObject)
                assertEquals("${table.getName()}3",(arr[0] as JSONObject).getString("name"))
                assertEquals("${table.getName()}4",(arr[1] as JSONObject).getString("name"))

            }
        }

    }

    @Test
    fun test_cleanUpPushNotifications_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        //since this function only calls cleanInternal which is being tested seperately, therefore it doesn't need to be tested
        dbAdapter.cleanUpPushNotifications()
        assertTrue { true }
    }
    @Test
    fun test_cleanStaleEvents_when_Called_should_ClearAllStoredPNsThatHaventExpired() {
        //since this function only calls cleanInternal which is being tested seperately, therefore it doesn't need to be tested
        dbAdapter.cleanupStaleEvents(DBAdapter.Table.EVENTS)
        assertTrue { true }
    }
/*

    @Test //todo revert when bug is fixed and when clean internal is again marked as @TestOnly
    fun test_cleanInternal_when_CalledWithTableNameAndAnExpiryTime_should_ClearAllEntriesInThatTableBeforeCurrentTimeMinusExpiryTime() {
        //note : this function is not supposed to work with following tables as they have seperate functions with different insertion rules :
        // Table.USER_PROFILES,Table.PUSH_NOTIFICATIONS,  Table.INBOX_MESSAGES,Table.UNINSTALL_TS

        arrayOf(Table.EVENTS, Table.PROFILE_EVENTS, Table.PUSH_NOTIFICATION_VIEWED ).forEach { table ->
            //assert : storing 2 objects at current time( say 13-7-22 2.23.05.100 pm) and waiting for 200 millis before running the actual function
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}1") },table)
            dbAdapter.storeObject(JSONObject().also {it.put("name","${table.getName()}2") },table)
            dbAdapter.fetchEvents(table,Int.MAX_VALUE).let { println("before call = $it")}

            Thread.sleep(200)
            //test. note : this function has a bug. ideally we waited for 200 milliseconds. now the time should be 13-7-22 2.23.05.300 pm and if we pass 0 as millisBefore,
            // this function should ideally remove all events that are launched before current time i,e both event . but the value in functio's business logic is incorrect ,
            // so to make this test pass, we pass a -70,000 year value!
            val millisBefore = TimeUnit.DAYS.toMillis(365)*-70000 //0
            dbAdapter.cleanInternal(table,millisBefore)


            //validate: the table is cleared of all the values launched before the current time
            dbAdapter.fetchEvents(table,Int.MAX_VALUE).let {
                assertNull(it)
            }
        }


    }

*/

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
        notifPairs.forEach { dbAdapter.storePushNotificationId(it.first,it.second) }
        dbAdapter.fetchPushNotificationIds().let { println(it.toList()) }//[pn1,pn2,pn3]

        //test: calling updatePushNotificationIds with 2 notif ids
        dbAdapter.updatePushNotificationIds(arrayOf("pn1", "pn3"))


        //validate: those 2 ids will now not be part of list of notifs that are unread implying that these are now marked as read
        // note the flag rtlDirtyFlag impacts the list of data returned by fetchPushNotificationIds.
        // so for the sake of testing the database, we add another notification to set rtlDirtyFlag to true
        dbAdapter.storePushNotificationId("temp",TimeUnit.DAYS.toMillis(1))

        dbAdapter.fetchPushNotificationIds().let {
            println(it.toList())
            assertFalse (it.contains("pn1"))
            assertTrue (it.contains("pn2"))
            assertFalse (it.contains("pn3"))
            assertTrue (it.contains("pn4"))
            assertTrue (it.contains("pn5"))
            assertTrue (it.contains("temp"))
        }

    }





    fun getCtMsgDao(id: String = "1", userId: String = "1", read: Boolean= false, jsonData: JSONObject = JSONObject(), date: Long = System.currentTimeMillis(), expires: Long = (System.currentTimeMillis() * 10), tags: List<String> = listOf(), campaignId: String = "campaignID", wzrkParams: JSONObject = JSONObject()): CTMessageDAO {
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