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
    private lateinit var inboxDeleteCoordinator: InboxDeleteCoordinator
    private val videoSupported = true

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        userId = "user1"
        dbAdapter = mockk(relaxed = true)
        ctLockManager = mockk(relaxed = true)
        callbackManager = mockk(relaxed = true)
        inboxDeleteCoordinator = mockk(relaxed = true)

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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            videoSupported,
            inboxDeleteCoordinator
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
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        val updated = controller.processV2Response(listOf(getCtMsgDao("m1", userId)))

        verify { dbAdapter.upsertMessages(match { it.size == 1 && it[0].id == "m1" }) }
        assertTrue(updated)
    }

    @Test
    fun `processV2Response does not notify callback manager itself`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(listOf(getCtMsgDao("m1", userId)))

        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `processV2Response with empty incoming and empty DB returns false`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
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
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
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
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(emptyList())

        val inMemory = controller.messages
        assertEquals(1, inMemory.size)
        assertEquals("m1", inMemory[0].id)
    }

    @Test
    fun `markReadInboxMessage records a pendingRead for the tapped V2 message`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            controller.markReadInboxMessage(mockk(relaxed = true) { every { messageId } returns "m1" })

            verify(exactly = 1) { dbAdapter.addPendingRead("m1", userId) }
        }
    }

    @Test
    fun `markReadInboxMessagesForIDs records pending reads as a single batch when all are V2`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2),
            getCtMsgDao("m2", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
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
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
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
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
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
    fun `deleteInboxMessage records a pending-delete and fires the coordinator for V2`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val message = mockk<CTInboxMessage>(relaxed = true) { every { messageId } returns "m1" }
            controller.deleteInboxMessage(message)

            verify(exactly = 1) { dbAdapter.addPendingDelete("m1", userId) }
            verify(exactly = 1) { inboxDeleteCoordinator.syncDelete(listOf(message), userId) }
        }
    }

    @Test
    fun `deleteInboxMessagesForIDs records pending-deletes as batch and fires one coordinator call for V2`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2),
            getCtMsgDao("m2", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val ids = arrayListOf("m1", "m2")
            controller.deleteInboxMessagesForIDs(ids)

            verify(exactly = 1) { dbAdapter.addPendingDeletes(ids, userId) }
            verify(exactly = 0) { dbAdapter.addPendingDelete(any(), any()) }
            verify(exactly = 1) {
                inboxDeleteCoordinator.syncDelete(match { it.size == 2 }, userId)
            }
        }
    }

    @Test
    fun `deleteInboxMessage V1 — local only, no pending, no coordinator`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V1)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val message = mockk<CTInboxMessage>(relaxed = true) { every { messageId } returns "m1" }
            controller.deleteInboxMessage(message)

            verify(exactly = 0) { dbAdapter.addPendingDelete(any(), any()) }
            verify(exactly = 0) { inboxDeleteCoordinator.syncDelete(any(), any()) }
            verify { dbAdapter.deleteMessageForId("m1", userId) }
        }
    }

    @Test
    fun `deleteInboxMessagesForIDs mixed — only V2 ids go to pending and coordinator, all locally deleted`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("v1a", userId, source = InboxMessageSource.V1),
            getCtMsgDao("v2a", userId, source = InboxMessageSource.V2),
            getCtMsgDao("v1b", userId, source = InboxMessageSource.V1),
            getCtMsgDao("v2b", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val ids = arrayListOf("v1a", "v2a", "v1b", "v2b")
            controller.deleteInboxMessagesForIDs(ids)

            verify(exactly = 1) {
                dbAdapter.addPendingDeletes(match { it.sorted() == listOf("v2a", "v2b") }, userId)
            }
            verify(exactly = 1) {
                inboxDeleteCoordinator.syncDelete(
                    match { msgs -> msgs.map { it.messageId }.sorted() == listOf("v2a", "v2b") },
                    userId
                )
            }
            verify { dbAdapter.deleteMessagesForIDs(ids, userId) }
        }
    }

    @Test
    fun `markReadInboxMessage V1 — local only, no pending_reads`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V1)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            controller.markReadInboxMessage(mockk(relaxed = true) { every { messageId } returns "m1" })

            verify { dbAdapter.markReadMessageForId("m1", userId) }
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any()) }
        }
    }

    @Test
    fun `markReadInboxMessagesForIDs mixed — only V2 ids in pending_reads`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("v1", userId, source = InboxMessageSource.V1),
            getCtMsgDao("v2", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val ids = arrayListOf("v1", "v2")
            controller.markReadInboxMessagesForIDs(ids)

            verify { dbAdapter.markReadMessagesForIds(ids, userId) }
            verify(exactly = 1) {
                dbAdapter.addPendingReads(match { it == listOf("v2") }, userId)
            }
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any()) }
        }
    }

    @Test
    fun `processV2Response with empty pending-reads skips removePendingReads`() {
        every { dbAdapter.getPendingReads(userId) } returns emptySet()
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(listOf(getCtMsgDao("m1", userId, read = true)))

        verify(exactly = 0) { dbAdapter.removePendingReads(any<List<String>>(), any()) }
    }

    @Test
    fun `updateMessages V1 path tags parsed DAOs as V1`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        val arr = org.json.JSONArray()
        arr.put(JSONObject("""{"_id":"m1","date":1,"wzrk_ttl":${Long.MAX_VALUE / 2},"msg":{}}"""))
        controller.updateMessages(arr)

        verify {
            dbAdapter.upsertMessages(match { list ->
                list.size == 1 && list[0].source == InboxMessageSource.V1
            })
        }
    }

    @Test
    fun `isV2Message returns true for V2 DAO in cache`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        assertTrue(controller.isV2Message("m1"))
    }

    @Test
    fun `isV2Message returns false for V1 DAO in cache`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V1)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        assertFalse(controller.isV2Message("m1"))
    }

    @Test
    fun `isV2Message returns false for unknown id`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        assertFalse(controller.isV2Message("unknown"))
    }

    @Test
    fun `isV2Message returns false for null id`() {
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        assertFalse(controller.isV2Message(null))
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
        wzrkParams: JSONObject = JSONObject(),
        source: InboxMessageSource = InboxMessageSource.V1
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
            it.source = source
        }
    }
}
