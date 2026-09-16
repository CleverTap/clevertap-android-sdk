package com.clevertap.android.sdk.pushnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric (for real org.json) tests for flattening the Live Update `data` object. */
@RunWith(RobolectricTestRunner::class)
class LiveActivityPayloadSurfacerTest {

    @Test
    fun `null or empty data yields empty`() {
        assertTrue(LiveActivityPayloadSurfacer.flatten(null, emptySet()).isEmpty())
        assertTrue(LiveActivityPayloadSurfacer.flatten("", emptySet()).isEmpty())
    }

    @Test
    fun `malformed data yields empty and does not throw`() {
        assertTrue(LiveActivityPayloadSurfacer.flatten("{not json", emptySet()).isEmpty())
    }

    @Test
    fun `primitives are coerced to strings`() {
        val out = LiveActivityPayloadSurfacer.flatten(
            """{"pt_progress":456,"pt_promote":true,"nt":"Pizza"}""", emptySet()
        )
        assertEquals("456", out["pt_progress"])
        assertEquals("true", out["pt_promote"])
        assertEquals("Pizza", out["nt"])
    }

    @Test
    fun `nested array and object are kept as compact json strings`() {
        val out = LiveActivityPayloadSurfacer.flatten(
            """{"pt_progress_segments":[{"length":1}],"obj":{"a":1}}""", emptySet()
        )
        assertEquals("""[{"length":1}]""", out["pt_progress_segments"])
        assertEquals("""{"a":1}""", out["obj"])
    }

    @Test
    fun `root wins - a key already present at top level is not surfaced`() {
        val out = LiveActivityPayloadSurfacer.flatten(
            """{"wzrk_pid":"INNER","nt":"Pizza"}""", setOf("wzrk_pid")
        )
        assertFalse(out.containsKey("wzrk_pid"))
        assertEquals("Pizza", out["nt"])
    }

    @Test
    fun `json null is skipped, not written as the literal null`() {
        val out = LiveActivityPayloadSurfacer.flatten("""{"x":null,"y":"1"}""", emptySet())
        assertFalse(out.containsKey("x"))
        assertEquals("1", out["y"])
    }
}
