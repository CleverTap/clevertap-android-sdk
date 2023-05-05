package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
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
        dbAdapter = mock(DBAdapter::class.java)
        ctLockManager = mock(CTLockManager::class.java)
        callbackManager = mock(CallbackManager::class.java)

        val messageDAOList =
            arrayListOf(getCtMsgDao("msg_1", userId, false), getCtMsgDao("msg_2", userId, false))
        `when`(dbAdapter.getMessages(userId)).thenReturn(messageDAOList)
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
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val spyController = spy(controller)
            messageIDs = arrayListOf("msg_3", "msg_4")
            val lock = Object()
            `when`(ctLockManager.inboxControllerLock).thenReturn(lock)

            // When
            spyController.deleteInboxMessagesForIDs(messageIDs)

            // then
            verify(ctLockManager).inboxControllerLock
            verify(spyController)._deleteMessagesForIds(messageIDs)
            verifyNoInteractions(callbackManager)
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
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val spyController = spy(controller)
            messageIDs = arrayListOf("msg_1", "msg_2")
            val lock = Object()
            `when`(ctLockManager.inboxControllerLock).thenReturn(lock)

            // When
            spyController.deleteInboxMessagesForIDs(messageIDs)

            // Then
            verify(ctLockManager).inboxControllerLock
            verify(spyController)._deleteMessagesForIds(messageIDs)
            verify(callbackManager)._notifyInboxMessagesDidUpdate()
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


        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            // When
            val result = controller._deleteMessagesForIds(messageIDs)

            // Then
            verify(dbAdapter).getMessages(userId)
            verify(dbAdapter).deleteMessagesForIDs(messageIDs, userId)
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


        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )

            // When
            val result = controller._markReadForMessagesWithIds(messageIDs)

            // Then
            verify(dbAdapter).getMessages(userId) //Called when creating an instance of CTInboxController
            verify(dbAdapter).markReadMessagesForIds(messageIDs, userId)
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
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val spyController = spy(controller)
            messageIDs = arrayListOf("msg_3", "msg_4")
            val lock = Object()
            `when`(ctLockManager.inboxControllerLock).thenReturn(lock)

            // When
            spyController.markReadInboxMessagesForIDs(messageIDs)

            // Then
            verify(ctLockManager).inboxControllerLock
            verify(spyController)._markReadForMessagesWithIds(messageIDs)
            verifyNoInteractions(callbackManager)
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
        mockStatic(CTExecutorFactory::class.java).use {
            `when`(CTExecutorFactory.executors(any())).thenReturn(
                MockCTExecutors(
                    cleverTapInstanceConfig
                )
            )
            val spyController = spy(controller)
            messageIDs = arrayListOf("msg_1", "msg_2")
            val lock = Object()
            `when`(ctLockManager.inboxControllerLock).thenReturn(lock)

            // When
            spyController.markReadInboxMessagesForIDs(messageIDs)

            // Then
            verify(ctLockManager).inboxControllerLock
            verify(spyController)._markReadForMessagesWithIds(messageIDs)
            verify(callbackManager, times(2))._notifyInboxMessagesDidUpdate()
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