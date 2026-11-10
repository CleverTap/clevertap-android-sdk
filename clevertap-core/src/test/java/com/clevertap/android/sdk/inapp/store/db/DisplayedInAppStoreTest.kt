package com.clevertap.android.sdk.inapp.store.db

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.db.dao.DisplayedInAppDAO
import com.clevertap.android.sdk.db.dao.DisplayedInAppData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private lateinit var mockDAO: DisplayedInAppDAO
    private lateinit var mockCryptHandler: CryptHandler

    private val inAppId = "campaign_1"
    private val inApp = JSONObject().put("wzrk_id", inAppId).put("type", "interstitial")
    private val ttl = 1_700_000_000L
    private val savedRow = DisplayedInAppData(inAppId = inAppId, inAppData = "encrypted", timeToLive = ttl)

    @Before
    fun setUp() {
        mockDAO = mockk(relaxed = true)
        mockCryptHandler = mockk(relaxed = true)
        store = DisplayedInAppStore(mockDAO, mockCryptHandler, mockk<ILogger>(relaxed = true), "account_id")
    }

    @Test
    fun `save encrypts the in-app and saves it with its time to live`() {
        every { mockCryptHandler.encrypt(inApp.toString()) } returns "encrypted"
        every { mockDAO.save(any()) } returns true

        assertTrue(store.save(inAppId, inApp, ttl))

        verify(exactly = 1) { mockDAO.save(savedRow) }
    }

    @Test
    fun `save keeps the old in-app and returns false when encryption fails`() {
        every { mockCryptHandler.encrypt(any()) } returns null

        assertFalse(store.save(inAppId, inApp, ttl))

        verify(exactly = 0) { mockDAO.save(any()) }
    }

    @Test
    fun `save returns false when the database save fails`() {
        every { mockCryptHandler.encrypt(any()) } returns "encrypted"
        every { mockDAO.save(any()) } returns false

        assertFalse(store.save(inAppId, inApp, ttl))
    }

    @Test
    fun `get returns the decrypted in-app with its time to live`() {
        every { mockDAO.fetch(inAppId) } returns savedRow
        every { mockCryptHandler.decrypt("encrypted") } returns inApp.toString()

        val displayed = store.get(inAppId)!!

        assertEquals(inApp.toString(), displayed.inApp.toString())
        assertEquals(ttl, displayed.timeToLive)
    }

    @Test
    fun `get returns null when nothing is saved`() {
        every { mockDAO.fetch(inAppId) } returns null

        assertNull(store.get(inAppId))
        verify(exactly = 0) { mockCryptHandler.decrypt(any()) }
    }

    @Test
    fun `get returns null and removes the row when the data cannot be decrypted or parsed`() {
        every { mockDAO.fetch(inAppId) } returns savedRow

        every { mockCryptHandler.decrypt("encrypted") } returns null
        assertNull(store.get(inAppId))

        every { mockCryptHandler.decrypt("encrypted") } returns "not json"
        assertNull(store.get(inAppId))

        verify(exactly = 2) { mockDAO.remove(inAppId) }
    }

    @Test
    fun `get keeps the row when the database read fails`() {
        // The DAO returns null for both "not found" and a read error; neither removes anything.
        every { mockDAO.fetch(inAppId) } returns null

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
