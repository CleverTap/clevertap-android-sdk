package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.store.preference.ICTPreference
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class NdCountsStoreTest {

    private lateinit var ctPreference: ICTPreference
    private lateinit var store: NdCountsStore

    @Before
    fun setUp() {
        ctPreference = mockk(relaxed = true)
        store = NdCountsStore(ctPreference)
    }

    @Test
    fun `counts parses today,lifetime CSV`() {
        every { ctPreference.readString("70001", "") } returns "3,12"
        assertEquals(NdCountsStore.Counts(3, 12), store.counts("70001"))
    }

    @Test
    fun `counts defaults to zero when absent or malformed`() {
        every { ctPreference.readString("absent", "") } returns ""
        every { ctPreference.readString("bad", "") } returns "x,y"
        every { ctPreference.readString("single", "") } returns "5"
        assertEquals(NdCountsStore.Counts(0, 0), store.counts("absent"))
        assertEquals(NdCountsStore.Counts(0, 0), store.counts("bad"))
        assertEquals(NdCountsStore.Counts(0, 0), store.counts("single"))
    }

    @Test
    fun `increment bumps both today and lifetime`() {
        every { ctPreference.readString("70001", "") } returns "2,9"
        every { ctPreference.writeString(any(), any()) } just Runs
        store.increment("70001")
        verify { ctPreference.writeString("70001", "3,10") }
    }

    @Test
    fun `allTargetCounts excludes global keys and malformed entries`() {
        every { ctPreference.readAll() } returns mapOf(
            "70001" to "1,4",
            "70002" to "0,2",
            Constants.KEY_ND_COUNTS_SHOWN_TODAY to 5,        // global int -> excluded
            Constants.KEY_ND_MAX_PER_DAY to 10,              // global int -> excluded
            Constants.KEY_ND_LAST_RESET_DATE to "25082026",  // global string, not a pair -> excluded
            "malformed" to "oops",
        )

        val result = store.allTargetCounts()

        assertEquals(2, result.size)
        assertEquals(NdCountsStore.Counts(1, 4), result["70001"])
        assertEquals(NdCountsStore.Counts(0, 2), result["70002"])
    }

    @Test
    fun `resetDailyKeepingLifetime zeroes today, keeps lifetime, clears shownToday`() {
        every { ctPreference.readAll() } returns mapOf("70001" to "3,12", "70002" to "1,1")
        every { ctPreference.writeString(any(), any()) } just Runs
        every { ctPreference.writeInt(any(), any()) } just Runs

        store.resetDailyKeepingLifetime()

        verify { ctPreference.writeString("70001", "0,12") }
        verify { ctPreference.writeString("70002", "0,1") }
        verify { ctPreference.writeInt(Constants.KEY_ND_COUNTS_SHOWN_TODAY, 0) }
    }

    @Test
    fun `global ceilings default to 1 until set`() {
        every { ctPreference.readInt(Constants.KEY_ND_MAX_PER_DAY, 1) } returns 1
        every { ctPreference.readInt(Constants.ND_MAX_PER_SESSION_KEY, 1) } returns 1
        assertEquals(1, store.maxPerDay)
        assertEquals(1, store.maxPerSession)
    }

    @Test
    fun `setting ceilings writes them`() {
        every { ctPreference.writeInt(any(), any()) } just Runs
        store.maxPerDay = 10
        store.maxPerSession = 3
        verify { ctPreference.writeInt(Constants.KEY_ND_MAX_PER_DAY, 10) }
        verify { ctPreference.writeInt(Constants.ND_MAX_PER_SESSION_KEY, 3) }
    }

    @Test
    fun `remove deletes a target`() {
        store.remove("70001")
        verify { ctPreference.remove("70001") }
    }

    @Test
    fun `onChangeUser repoints to the counts namespace for the new user`() {
        store.onChangeUser("device_id", "account_id")
        verify { ctPreference.changePreferenceName("${Constants.KEY_ND_COUNTS_PER_TARGET}:device_id:account_id") }
    }
}
