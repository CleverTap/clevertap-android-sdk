package com.clevertap.android.sdk.inapp.store.db

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.DelayedLegacyInAppDAO
import com.clevertap.android.sdk.db.DelayedLegacyInAppData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayedInAppStoreTest {

    private lateinit var store: DisplayedInAppStore
    private lateinit var mockDAO: DelayedLegacyInAppDAO
    private lateinit var mockCryptHandler: CryptHandler

    private val inAppId = "campaign_1"
    private val data = JSONObject().put("inApp", JSONObject().put("wzrk_id", inAppId)).put("ttl", 100L)

    @Before
    fun setUp() {
        mockDAO = mockk(relaxed = true)
        mockCryptHandler = mockk(relaxed = true)
        store = DisplayedInAppStore(mockDAO, mockCryptHandler, mockk<ILogger>(relaxed = true), "account_id")
    }

    @Test
    fun `save encrypts the data and replaces any saved in-app`() {
        every { mockCryptHandler.encrypt(data.toString()) } returns "encrypted"
        every { mockDAO.insertBatch(any()) } returns true

        assertTrue(store.save(inAppId, data))

        verifyOrder {
            mockDAO.clearAll()
            mockDAO.insertBatch(listOf(DelayedLegacyInAppData(inAppId = inAppId, delay = 0, inAppData = "encrypted")))
        }
    }

    @Test
    fun `save keeps the old in-app and returns false when encryption fails`() {
        every { mockCryptHandler.encrypt(any()) } returns null

        assertFalse(store.save(inAppId, data))

        verify(exactly = 0) { mockDAO.clearAll() }
        verify(exactly = 0) { mockDAO.insertBatch(any()) }
    }

    @Test
    fun `save returns false when the database insert fails`() {
        every { mockCryptHandler.encrypt(any()) } returns "encrypted"
        every { mockDAO.insertBatch(any()) } returns false

        assertFalse(store.save(inAppId, data))
    }

    @Test
    fun `get returns the decrypted data`() {
        every { mockDAO.fetchSingleInApp(inAppId) } returns "encrypted"
        every { mockCryptHandler.decrypt("encrypted") } returns data.toString()

        assertEquals(data.toString(), store.get(inAppId).toString())
    }

    @Test
    fun `get returns null when nothing is saved`() {
        every { mockDAO.fetchSingleInApp(inAppId) } returns null

        assertNull(store.get(inAppId))
        verify(exactly = 0) { mockCryptHandler.decrypt(any()) }
    }

    @Test
    fun `get returns null and removes the row when the data cannot be decrypted or parsed`() {
        every { mockDAO.fetchSingleInApp(inAppId) } returns "encrypted"

        every { mockCryptHandler.decrypt("encrypted") } returns null
        assertNull(store.get(inAppId))

        every { mockCryptHandler.decrypt("encrypted") } returns "not json"
        assertNull(store.get(inAppId))

        verify(exactly = 2) { mockDAO.remove(inAppId) }
    }

    @Test
    fun `get keeps the row when the database read fails`() {
        // The DAO returns null for both "not found" and a read error; neither removes anything.
        every { mockDAO.fetchSingleInApp(inAppId) } returns null

        assertNull(store.get(inAppId))
        verify(exactly = 0) { mockDAO.remove(any()) }
    }

    @Test
    fun `remove removes the in-app by id`() {
        every { mockDAO.remove(inAppId) } returns true

        assertTrue(store.remove(inAppId))
        verify(exactly = 1) { mockDAO.remove(inAppId) }
    }

    @Test
    fun `clear removes any saved in-app`() {
        every { mockDAO.clearAll() } returns true

        assertTrue(store.clear())
        verify(exactly = 1) { mockDAO.clearAll() }
    }
}
