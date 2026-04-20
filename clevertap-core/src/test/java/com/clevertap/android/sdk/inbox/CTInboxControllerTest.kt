package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CTInboxControllerTest : BaseTestCase() {
    private lateinit var messageIDs: ArrayList<String>
    private lateinit var userId: String
    private lateinit var dbAdapter: DBAdapter
    private lateinit var ctLockManager: CTLockManager
    private lateinit var callbackManager: CallbackManager
    private lateinit var controller: CTInboxController
    private val videoSupported = true

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        userId = "user1"
        dbAdapter = mockk(relaxed = true)
        ctLockManager = mockk(relaxed = true)
        callbackManager = mockk(relaxed = true)

        val messageDAOList =
            arrayListOf(getCtMsgDao("msg_1", userId, false), getCtMsgDao("msg_2", userId, false))
        every { dbAdapter.getMessages(userId) } returns messageDAOList
    }

    @Test
    fun `deleteInboxMessagesForIDs should call _deleteMessagesForIDs and not notify callback manager when all messages are invalid`() {
        // given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            val spyController = spyk(controller)
            messageIDs = arrayListOf("msg_3", "msg_4")
            val lock = Object()
            every { ctLockManager.inboxControllerLock } returns lock

            // When
            spyController.deleteInboxMessagesForIDs(messageIDs)

            // then
            verify { ctLockManager.inboxControllerLock }
            verify { spyController._deleteMessagesForIds(messageIDs) }
            verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
        }
    }

    @Test
    fun `deleteInboxMessagesForIDs should call _deleteMessagesForIds and notify callback manager when some messages are valid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            val spyController = spyk(controller)
            messageIDs = arrayListOf("msg_1", "msg_2")
            val lock = Object()
            every { ctLockManager.inboxControllerLock } returns lock

            // When
            spyController.deleteInboxMessagesForIDs(messageIDs)

            // Then
            verify { ctLockManager.inboxControllerLock }
            verify { spyController._deleteMessagesForIds(messageIDs) }
            verify { callbackManager._notifyInboxMessagesDidUpdate() }
        }
    }

    @Test
    fun `_deleteMessagesForIDs should return false when all messageIDs are invalid`() {
        //Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        messageIDs = arrayListOf("msg_3", "msg_4")

        //When
        val result = controller._deleteMessagesForIds(messageIDs)

        //Then
        assertFalse(result)
    }

    @Test
    fun `_deleteMessagesForIDs should return true when some messageIDs are valid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        messageIDs = arrayListOf("msg_1", "msg_2")


        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            // When
            val result = controller._deleteMessagesForIds(messageIDs)

            // Then
            verify { dbAdapter.getMessages(userId) }
            verify { dbAdapter.deleteMessagesForIDs(messageIDs, userId) }
            assertTrue(result)
            assertEquals(0, controller.messages.size)
        }
    }

    @Test
    fun `_markReadMessagesForIDs should return false when all messageIDs are invalid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        messageIDs = arrayListOf("msg_3", "msg_4")

        // When
        val result = controller._markReadForMessagesWithIds(messageIDs)

        // Then
        assertFalse(result)
    }

    @Test
    fun `_markReadForMessagesWithIDs should return true when some messageIDs are valid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        messageIDs = arrayListOf("msg_1", "msg_2")


        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            // When
            val result = controller._markReadForMessagesWithIds(messageIDs)

            // Then
            verify { dbAdapter.getMessages(userId) } //Called when creating an instance of CTInboxController
            verify { dbAdapter.markReadMessagesForIds(messageIDs, userId) }
            assertTrue(result)
            assertEquals(1, controller.messages[0].isRead)
            assertEquals(1, controller.messages[1].isRead)
        }
    }

    @Test
    fun `markReadInboxMessagesForIDs should call _markReadForMessagesWithIds and not notify callback manager when all messages are invalid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            val spyController = spyk(controller)
            messageIDs = arrayListOf("msg_3", "msg_4")
            val lock = Object()
            every { ctLockManager.inboxControllerLock } returns lock

            // When
            spyController.markReadInboxMessagesForIDs(messageIDs)

            // Then
            verify { ctLockManager.inboxControllerLock }
            verify { spyController._markReadForMessagesWithIds(messageIDs) }
            verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
        }
    }

    @Test
    fun `markReadInboxMessagesForIDs should call _markReadForMessagesWithIds and notify callback manager when some messages are valid`() {
        // Given
        controller = CTInboxController(
            cleverTapInstanceConfig,
            userId,
            dbAdapter,
            ctLockManager,
            callbackManager,
            videoSupported
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )

            val spyController = spyk(controller)
            messageIDs = arrayListOf("msg_1", "msg_2")
            val lock = Object()
            every { ctLockManager.inboxControllerLock } returns lock

            // When
            spyController.markReadInboxMessagesForIDs(messageIDs)

            // Then
            verify { ctLockManager.inboxControllerLock }
            verify { spyController._markReadForMessagesWithIds(messageIDs) }
            verify(exactly = 2) { callbackManager._notifyInboxMessagesDidUpdate() }
        }
    }

    @Test
    fun `processV2Response upserts filtered incoming and returns true`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(getCtMsgDao("m1", userId))
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        val updated = controller.processV2Response(listOf(getCtMsgDao("m1", userId)))

        verify { dbAdapter.upsertMessages(match { it.size == 1 && it[0].id == "m1" }) }
        assertTrue(updated)
    }

    @Test
    fun `processV2Response does not notify callback manager itself`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        controller.processV2Response(listOf(getCtMsgDao("m1", userId)))

        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `processV2Response with empty incoming and empty DB returns false`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        val updated = controller.processV2Response(emptyList())

        verify(exactly = 0) { dbAdapter.upsertMessages(any()) }
        verify(exactly = 0) { dbAdapter.deleteMessagesForIDs(any(), any()) }
        assertFalse(updated)
    }

    @Test
    fun `processV2Response removes expired rows found during re-read`() {
        val expired = getCtMsgDao("m1", userId, expires = 1L)
        every { dbAdapter.getMessages(userId) } returns arrayListOf(expired)
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        val updated = controller.processV2Response(emptyList())

        verify { dbAdapter.deleteMessagesForIDs(match { it.contains("m1") }, userId) }
        assertTrue(updated)
    }

    @Test
    fun `processV2Response replaces in-memory list with cleanup finalList`() {
        val survivor = getCtMsgDao("m1", userId)
        every { dbAdapter.getMessages(userId) } returns arrayListOf(survivor)
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        controller.processV2Response(emptyList())

        val inMemory = controller.messages
        assertEquals(1, inMemory.size)
        assertEquals("m1", inMemory[0].id)
    }

    @Test
    fun `markReadInboxMessage records a pendingRead for the tapped message`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(getCtMsgDao("m1", userId))
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            controller.markReadInboxMessage(mockk(relaxed = true) { every { messageId } returns "m1" })

            verify(exactly = 1) { dbAdapter.addPendingRead("m1", userId) }
        }
    }

    @Test
    fun `markReadInboxMessagesForIDs records pending reads as a single batch`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId), getCtMsgDao("m2", userId)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val ids = arrayListOf("m1", "m2")
            controller.markReadInboxMessagesForIDs(ids)

            verify(exactly = 1) { dbAdapter.addPendingReads(ids, userId) }
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any()) }
        }
    }

    @Test
    fun `processV2Response with pending-read applies isRead=1 override on upsert`() {
        every { dbAdapter.getPendingReads(userId) } returns setOf("m1")
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        val incomingUnread = getCtMsgDao("m1", userId, read = false)
        controller.processV2Response(listOf(incomingUnread))

        verify { dbAdapter.upsertMessages(match { it.size == 1 && it[0].id == "m1" && it[0].isRead == 1 }) }
    }

    @Test
    fun `processV2Response clears pending-read when server returns isRead=true for the same id`() {
        every { dbAdapter.getPendingReads(userId) } returns setOf("m1", "m2")
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        controller.processV2Response(
            listOf(
                getCtMsgDao("m1", userId, read = true),
                getCtMsgDao("m2", userId, read = false)
            )
        )

        verify { dbAdapter.removePendingReads(listOf("m1"), userId) }
    }

    @Test
    fun `processV2Response with empty pending-reads skips removePendingReads`() {
        every { dbAdapter.getPendingReads(userId) } returns emptySet()
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported
        )

        controller.processV2Response(listOf(getCtMsgDao("m1", userId, read = true)))

        verify(exactly = 0) { dbAdapter.removePendingReads(any<List<String>>(), any()) }
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
