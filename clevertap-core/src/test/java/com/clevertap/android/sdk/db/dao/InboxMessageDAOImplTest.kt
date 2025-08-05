package com.clevertap.android.sdk.db.dao

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class InboxMessageDAOImplTest : BaseTestCase() {

    private lateinit var inboxMessageDAO: InboxMessageDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbHelper: DatabaseHelper

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(appCtx, instanceConfig.accountId, "test_db", instanceConfig.logger)
        inboxMessageDAO = InboxMessageDAOImpl(dbHelper, instanceConfig.logger)
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_deleteMessage_when_MessageIDAndUserIDIsPassed_should_DeleteMessageIfExists() {
        val msgId = "msg_1234"
        val userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, false)))
        var msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(1, msgList.size)

        val result = inboxMessageDAO.deleteMessage(msgId, userID)
        assertEquals(true, result)
        msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(0, msgList.size)
    }

    @Test
    fun test_deleteMessages_when_MessageIDAndUserIdIsPassed_should_DeleteMessagesIfExists() {
        val userId = "user_11"
        val msgIds = listOf("msg_1", "msg_2", "msg_3")

        // When all msgIds are present in the db all should be deleted
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        var msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(3, msgList.size)

        val result = inboxMessageDAO.deleteMessages(msgIds, userId)
        assertTrue(result)
        msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(0, msgList.size)

        // When some msgId from msgIds is not found, it is skipped and remaining are deleted
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, false),
                getCtMsgDao(msgIds[1], userId, false),
                getCtMsgDao(msgIds[2], userId, false)
            )
        )
        msgList = inboxMessageDAO.getMessages(userId)
        assertEquals(3, msgList.size)

        val updatedMsgIds = msgIds.toMutableList().apply {
            removeAt(lastIndex)
            add("msg_4")
        }
        val result2 = inboxMessageDAO.deleteMessages(updatedMsgIds, userId)
        msgList = inboxMessageDAO.getMessages(userId)
        assertTrue(result2)
        assertEquals(1, msgList.size)
    }

    @Test
    fun test_getMessages_when_FunctionIsCalledWithCorrectUserID_should_ReturnAllAssociatedMessages() {
        // Case: incorrect user id
        var msgList = inboxMessageDAO.getMessages("unavailableUser")
        assertEquals(0, msgList.size)

        // Case: correct user id
        val msgId = "msg_1234"
        val userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        msgList = inboxMessageDAO.getMessages(userID)
        assertEquals(1, msgList.size)
        assertEquals(msgId, msgList[0].id)
        assertEquals(userID, msgList[0].userId)
    }

    @Test
    fun test_markMessageAsRead_when_CorrectUserIdAndMessageIsPassed_should_SetMessageIdAsRead() {
        val msgId = "msg_1234"
        var userID = "user_11"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        inboxMessageDAO.markMessageAsRead(msgId, userID)
        var msg = inboxMessageDAO.getMessages(userID)[0]
        assertTrue(msg.isRead == 1)

        userID = "user_12"
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao(msgId, userID, read = false)))
        inboxMessageDAO.markMessageAsRead("msgId", userID) // Wrong message ID
        msg = inboxMessageDAO.getMessages(userID)[0]
        assertFalse(msg.isRead == 1)
    }

    @Test
    fun test_markMessagesAsRead_when_CorrectUserIdAndMessagesArePassed_should_SetMessageIdsAsRead() {
        val userId = "user_11"
        val msgIds = listOf("msg_1", "msg_2", "msg_3")

        // When all msgIds are present in the db all should be marked as read
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId, read = false),
                getCtMsgDao(msgIds[1], userId, read = false),
                getCtMsgDao(msgIds[2], userId, read = false)
            )
        )
        val result = inboxMessageDAO.markMessagesAsRead(msgIds, userId)
        var msgList = inboxMessageDAO.getMessages(userId)
        assertTrue(result)
        for (msg in msgList)
            assertTrue(msg.isRead == 1)

        // When some msgId from msgIds is not found, it is skipped and remaining are marked as read
        val userId2 = "user_12"
        inboxMessageDAO.upsertMessages(
            arrayListOf(
                getCtMsgDao(msgIds[0], userId2, read = false),
                getCtMsgDao(msgIds[1], userId2, read = false),
                getCtMsgDao(msgIds[2], userId2, read = false)
            )
        )
        val updatedMsgIds = msgIds.toMutableList().apply {
            removeAt(lastIndex)
            add("msg_4")
        }
        val result2 = inboxMessageDAO.markMessagesAsRead(updatedMsgIds, userId2)
        msgList = inboxMessageDAO.getMessages(userId2)
        assertTrue(result2)
        for (msg in msgList) {
            if (msg.id in updatedMsgIds)
                assertTrue(msg.isRead == 1)
            else
                assertFalse(msg.isRead == 1)
        }
    }

    @Test
    fun test_upsertMessages_when_Called_should_InsertORUpsertAMessage() {
        // When a message is not present it will insert the message
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = false, campaignId = "cp1234")))
        assertNotNull(inboxMessageDAO.getMessages("user_11")[0])

        // When a message is present it will update the message
        inboxMessageDAO.upsertMessages(arrayListOf(getCtMsgDao("msg_1234", "user_11", read = true, campaignId = "cp4321")))
        val messages = inboxMessageDAO.getMessages("user_11")
        assertEquals(1, messages.size)
        assertEquals(true, messages[0].isRead == 1)
        assertEquals("cp4321", messages[0].campaignId)
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
