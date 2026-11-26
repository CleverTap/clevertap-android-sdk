package com.clevertap.android.sdk.customviews

import android.os.Bundle
import android.os.Parcel
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InboxVideoStatesTest : BaseTestCase() {

    private val states = InboxVideoStates()

    private fun state(url: String, positionMs: Long, volume: Float = 1f) =
        InboxVideoStates.VideoState(url, positionMs, volume)

    @Test
    fun `a state is found for its message when the media is the same`() {
        states.remember("m1", state("url1", 5_000L))

        val found = states.stateOf("m1", "url1")

        assertEquals(5_000L, found!!.positionMs)
        assertEquals(1f, found.volume, 0f)
    }

    @Test
    fun `a state is ignored when the media of the message changed`() {
        states.remember("m1", state("url1", 5_000L))

        assertNull(states.stateOf("m1", "another-url"))
        assertNull(states.stateOf("m1", null))
    }

    @Test
    fun `there is no state for an unknown message or without a message id`() {
        states.remember("m1", state("url1", 5_000L))

        assertNull(states.stateOf("m2", "url1"))
        assertNull(states.stateOf(null, "url1"))
    }

    @Test
    fun `remembering a message again keeps the latest place`() {
        states.remember("m1", state("url1", 1_000L, 0f))
        states.remember("m1", state("url1", 2_000L, 1f))

        assertEquals(2_000L, states.stateOf("m1", "url1")!!.positionMs)
        assertEquals(1f, states.stateOf("m1", "url1")!!.volume, 0f)
    }

    @Test
    fun `every message keeps its own place and sound`() {
        states.remember("m1", state("url1", 1_000L, 1f))
        states.remember("m2", state("url2", 2_000L, 0f))

        assertEquals(1_000L, states.stateOf("m1", "url1")!!.positionMs)
        assertEquals(2_000L, states.stateOf("m2", "url2")!!.positionMs)
        assertEquals(0f, states.stateOf("m2", "url2")!!.volume, 0f)
    }

    @Test
    fun `saving with nothing remembered gives an empty bundle`() {
        assertTrue(states.save().isEmpty)
    }

    @Test
    fun `saved states can be restored into another instance`() {
        states.remember("m1", state("url1", 5_000L, 1f))
        states.remember("m2", state("url2", 7_000L, 0f))
        val other = InboxVideoStates()

        other.restore(states.save())

        assertEquals(5_000L, other.stateOf("m1", "url1")!!.positionMs)
        assertEquals(1f, other.stateOf("m1", "url1")!!.volume, 0f)
        assertEquals(7_000L, other.stateOf("m2", "url2")!!.positionMs)
        assertEquals(0f, other.stateOf("m2", "url2")!!.volume, 0f)
    }

    @Test
    fun `saved states keep their values through a parcel`() {
        states.remember("m1", state("url1", 5_000L, 1f))
        val parcel = Parcel.obtain()
        val restored: Bundle?
        try {
            parcel.writeBundle(states.save())
            parcel.setDataPosition(0)
            restored = parcel.readBundle(javaClass.classLoader)
        } finally {
            parcel.recycle()
        }
        val other = InboxVideoStates()

        other.restore(restored)

        assertEquals(5_000L, other.stateOf("m1", "url1")!!.positionMs)
    }

    @Test
    fun `restoring nothing changes nothing`() {
        states.remember("m1", state("url1", 5_000L))

        states.restore(null)
        states.restore(Bundle())

        assertEquals(5_000L, states.stateOf("m1", "url1")!!.positionMs)
    }

    @Test
    fun `restoring skips entries that are broken`() {
        val broken = Bundle().apply {
            putString("m1", "not a bundle")
            putBundle("m2", Bundle()) // no media url
            putBundle("m3", Bundle().apply { putString("mediaUrl", "url3"); putLong("positionMs", 3_000L) })
        }

        states.restore(broken)

        assertNull(states.stateOf("m1", "url1"))
        assertNull(states.stateOf("m2", "url2"))
        assertEquals(3_000L, states.stateOf("m3", "url3")!!.positionMs)
    }

    @Test
    fun `restored states replace the ones remembered for the same message`() {
        states.remember("m1", state("url1", 1_000L, 0f))
        val saved = Bundle().apply {
            putBundle("m1", Bundle().apply { putString("mediaUrl", "url1"); putLong("positionMs", 9_000L); putFloat("volume", 1f) })
        }

        states.restore(saved)

        assertEquals(9_000L, states.stateOf("m1", "url1")!!.positionMs)
    }

    @Test
    fun `two instances keep their own states`() {
        val other = InboxVideoStates()
        states.remember("m1", state("url1", 5_000L))

        assertNull(other.stateOf("m1", "url1"))
    }
}
