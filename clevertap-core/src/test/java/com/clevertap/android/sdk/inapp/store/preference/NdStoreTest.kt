package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.store.preference.ICTPreference
import com.clevertap.android.sdk.toList
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class NdStoreTest {

    private lateinit var ctPreference: ICTPreference
    private lateinit var ndStore: NdStore

    @Before
    fun setUp() {
        ctPreference = mockk(relaxed = true)
        ndStore = NdStore(ctPreference)
    }

    @Test
    fun `storeServerSideNdMetaData writes JSONArray and read returns it from cache`() {
        val meta = JSONArray("[{\"ti\":70001},{\"ti\":70002}]").toList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs

        ndStore.storeServerSideNdMetaData(meta)

        assertEquals(meta.toString(), ndStore.readServerSideNdMetaData().toString())
        verify { ctPreference.writeString(Constants.PREFS_ND_KEY_SS, JSONArray(meta).toString()) }
    }

    @Test
    fun `readServerSideNdMetaData reads from prefs when no cache`() {
        every { ctPreference.readString(Constants.PREFS_ND_KEY_SS, any()) } returns "[{\"ti\":70003}]"

        val result = ndStore.readServerSideNdMetaData()

        assertEquals(1, result.size)
        assertEquals(70003, result[0].optInt("ti"))
    }

    @Test
    fun `readServerSideNdMetaData returns empty on blank`() {
        every { ctPreference.readString(Constants.PREFS_ND_KEY_SS, any()) } returns ""
        assertEquals(0, ndStore.readServerSideNdMetaData().size)
    }

    @Test
    fun `evaluated nd ids write then read`() {
        every { ctPreference.writeString(any(), any()) } just Runs
        val ids = JSONArray().put(70001L).put(70002L)
        ndStore.storeEvaluatedServerSideNdIds(ids)
        verify { ctPreference.writeString(Constants.PREFS_EVALUATED_ND_KEY_SS, ids.toString()) }

        every { ctPreference.readString(Constants.PREFS_EVALUATED_ND_KEY_SS, any()) } returns ids.toString()
        assertEquals(2, ndStore.readEvaluatedServerSideNdIds().length())
    }

    @Test
    fun `suppressed nd ids write then read`() {
        every { ctPreference.writeString(any(), any()) } just Runs
        val entries = JSONArray().put(JSONObject().put(Constants.NOTIFICATION_ID_TAG, "70004_20260810"))
        ndStore.storeSuppressedNdIds(entries)
        verify { ctPreference.writeString(Constants.PREFS_SUPPRESSED_ND_KEY, entries.toString()) }

        every { ctPreference.readString(Constants.PREFS_SUPPRESSED_ND_KEY, any()) } returns entries.toString()
        assertEquals(1, ndStore.readSuppressedNdIds().length())
    }

    @Test
    fun `removeServerSideNdMetaData clears key`() {
        ndStore.removeServerSideNdMetaData()
        verify { ctPreference.remove(Constants.PREFS_ND_KEY_SS) }
    }

    @Test
    fun `onChangeUser repoints to the ND namespace for the new user`() {
        ndStore.onChangeUser("device_id", "account_id")
        verify { ctPreference.changePreferenceName("${Constants.ND_KEY}:device_id:account_id") }
    }
}
