package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.db.dao.PendingDelete
import com.clevertap.android.sdk.db.dao.PendingRead
import com.clevertap.android.sdk.response.InboxV2DeliverySource
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
    fun `processV2Response sweeps expired AWAITING_CONFIRM rows BEFORE reading the pending-delete id set`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(emptyList(), InboxV2DeliverySource.A1)

        io.mockk.verifyOrder {
            dbAdapter.removeExpiredAwaitingConfirm(userId, any())
            dbAdapter.getPendingDeleteIds(userId)
        }
    }

    @Test
    fun `processV2Response sweeps expired pending-reads BEFORE reading the pending-reads set`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(emptyList(), InboxV2DeliverySource.A1)

        io.mockk.verifyOrder {
            dbAdapter.removeExpiredPendingReads(userId, any())
            dbAdapter.getPendingReads(userId)
        }
    }

    @Test
    fun `processV2Response upserts filtered incoming and returns true`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(getCtMsgDao("m1", userId))
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        val updated = controller.processV2Response(listOf(getCtMsgDao("m1", userId)), InboxV2DeliverySource.A1)

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

        controller.processV2Response(listOf(getCtMsgDao("m1", userId)), InboxV2DeliverySource.A1)

        verify(exactly = 0) { callbackManager._notifyInboxMessagesDidUpdate() }
    }

    @Test
    fun `processV2Response with empty incoming and empty DB returns false`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        val updated = controller.processV2Response(emptyList(), InboxV2DeliverySource.A1)

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

        val updated = controller.processV2Response(emptyList(), InboxV2DeliverySource.A1)

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

        controller.processV2Response(emptyList(), InboxV2DeliverySource.A1)

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

            verify(exactly = 1) { dbAdapter.addPendingRead("m1", userId, any()) }
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

            verify(exactly = 1) {
                dbAdapter.addPendingReads(
                    match<List<PendingRead>> { rows -> rows.map { it.messageId }.sorted() == listOf("m1", "m2") },
                    userId
                )
            }
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any(), any()) }
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
        controller.processV2Response(listOf(incomingUnread), InboxV2DeliverySource.A1)

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
            ),
            InboxV2DeliverySource.A1
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

            verify(exactly = 1) { dbAdapter.addPendingDelete("m1", userId, any(), any()) }
            verify(exactly = 1) { inboxDeleteCoordinator.syncDelete(listOf(message), userId) }
        }
    }

    @Test
    fun `deleteInboxMessage forwards the DAO's wzrkParams into the pending row`() {
        val params = JSONObject().put("wzrk_id", "camp-1").put("wzrk_pivot", "default")
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2, wzrkParams = params)
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

            verify(exactly = 1) {
                dbAdapter.addPendingDelete(
                    "m1",
                    userId,
                    match {
                        it != null && it.optString("wzrk_id") == "camp-1" && it.optString("wzrk_pivot") == "default"
                    },
                    any()
                )
            }
        }
    }

    @Test
    fun `deleteInboxMessage with no DAO TTL falls back to now plus 1 day on the pending row`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2, wzrkParams = JSONObject(), expires = 0L)
        )
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(any()) } returns MockCTExecutors(cleverTapInstanceConfig)
            every { ctLockManager.inboxControllerLock } returns Object()

            val before = System.currentTimeMillis() / 1000L
            val message = mockk<CTInboxMessage>(relaxed = true) { every { messageId } returns "m1" }
            controller.deleteInboxMessage(message)
            val after = System.currentTimeMillis() / 1000L
            val oneDay = 24L * 60L * 60L

            verify(exactly = 1) {
                dbAdapter.addPendingDelete(
                    "m1",
                    userId,
                    any(),
                    match { expiresAt -> expiresAt in (before + oneDay)..(after + oneDay) }
                )
            }
        }
    }

    @Test
    fun `deleteInboxMessage forwards a positive DAO TTL onto the pending row`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf(
            getCtMsgDao("m1", userId, source = InboxMessageSource.V2, wzrkParams = JSONObject(), expires = 5_000L)
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

            verify(exactly = 1) { dbAdapter.addPendingDelete("m1", userId, any(), 5_000L) }
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

            verify(exactly = 1) {
                dbAdapter.addPendingDeletes(
                    match<List<PendingDelete>> { rows -> rows.map { it.messageId }.sorted() == listOf("m1", "m2") },
                    userId
                )
            }
            verify(exactly = 0) { dbAdapter.addPendingDelete(any(), any(), any(), any()) }
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

            verify(exactly = 0) { dbAdapter.addPendingDelete(any(), any(), any(), any()) }
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
                dbAdapter.addPendingDeletes(
                    match<List<PendingDelete>> { rows -> rows.map { it.messageId }.sorted() == listOf("v2a", "v2b") },
                    userId
                )
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
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any(), any()) }
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
                dbAdapter.addPendingReads(
                    match<List<PendingRead>> { rows -> rows.map { it.messageId } == listOf("v2") },
                    userId
                )
            }
            verify(exactly = 0) { dbAdapter.addPendingRead(any(), any(), any()) }
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

        controller.processV2Response(listOf(getCtMsgDao("m1", userId, read = true)), InboxV2DeliverySource.A1)

        verify(exactly = 0) { dbAdapter.removePendingReads(any<List<String>>(), any()) }
    }

    // ── FETCH-path sweep tests (T7.3) ───────────────────────────────────────

    @Test
    fun `processV2Response(FETCH) calls markIndexed for every id in the incoming list`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        every { dbAdapter.findSweepableV2Ids(userId, any()) } returns mutableSetOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(
            listOf(getCtMsgDao("m1", userId), getCtMsgDao("m2", userId)),
            InboxV2DeliverySource.FETCH
        )

        verify {
            dbAdapter.markIndexed(
                match { it.containsAll(listOf("m1", "m2")) && it.size == 2 },
                userId
            )
        }
    }

    @Test
    fun `processV2Response(FETCH) skips markIndexed when incoming list is empty`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        every { dbAdapter.findSweepableV2Ids(userId, any()) } returns mutableSetOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(emptyList(), InboxV2DeliverySource.FETCH)

        verify(exactly = 0) { dbAdapter.markIndexed(any(), any()) }
    }

    @Test
    fun `processV2Response(FETCH) sweeps INDEXED V2 ids absent from incoming and returns true`() {
        // m2 is sweepable but absent from this fetch response — must be deleted
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        every { dbAdapter.findSweepableV2Ids(userId, any()) } returns mutableSetOf("m1", "m2")
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        // Only m1 arrives; m2 is absent → should be swept
        val updated = controller.processV2Response(
            listOf(getCtMsgDao("m1", userId)),
            InboxV2DeliverySource.FETCH
        )

        verify { dbAdapter.deleteMessagesForIDs(match { it == listOf("m2") }, userId) }
        assertTrue(updated)
    }

    @Test
    fun `processV2Response(FETCH) does not sweep ids present in incoming`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        // Both m1 and m2 are sweepable but both also appear in the fetch response
        every { dbAdapter.findSweepableV2Ids(userId, any()) } returns mutableSetOf("m1", "m2")
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(
            listOf(getCtMsgDao("m1", userId), getCtMsgDao("m2", userId)),
            InboxV2DeliverySource.FETCH
        )

        // Sweep should produce an empty set — no sweep-triggered delete call for m1 or m2
        verify(exactly = 0) {
            dbAdapter.deleteMessagesForIDs(
                match { it.contains("m1") || it.contains("m2") },
                userId
            )
        }
    }

    @Test
    fun `processV2Response(FETCH) passes graceCutoff = nowSec minus INDEXING_GRACE_SECONDS to findSweepableV2Ids`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        every { dbAdapter.findSweepableV2Ids(userId, any()) } returns mutableSetOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        val beforeSec = System.currentTimeMillis() / 1000L
        controller.processV2Response(emptyList(), InboxV2DeliverySource.FETCH)
        val afterSec = System.currentTimeMillis() / 1000L

        val expectedLow = beforeSec - CTInboxController.INDEXING_GRACE_SECONDS
        val expectedHigh = afterSec - CTInboxController.INDEXING_GRACE_SECONDS
        verify {
            dbAdapter.findSweepableV2Ids(
                userId,
                match { cutoff -> cutoff in expectedLow..expectedHigh }
            )
        }
    }

    @Test
    fun `processV2Response(A1) does not call markIndexed or findSweepableV2Ids`() {
        every { dbAdapter.getMessages(userId) } returns arrayListOf()
        controller = CTInboxController(
            cleverTapInstanceConfig, userId, dbAdapter, ctLockManager, callbackManager, videoSupported,
            inboxDeleteCoordinator
        )

        controller.processV2Response(listOf(getCtMsgDao("m1", userId)), InboxV2DeliverySource.A1)

        verify(exactly = 0) { dbAdapter.markIndexed(any(), any()) }
        verify(exactly = 0) { dbAdapter.findSweepableV2Ids(any(), any()) }
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
