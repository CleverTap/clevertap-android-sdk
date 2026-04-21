package com.clevertap.android.sdk.network.fetch

import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CallResultTest {

    @Test
    fun `Success holds its data`() {
        val r: CallResult<Int> = CallResult.Success(42)
        assertEquals(42, (r as CallResult.Success).data)
    }

    @Test
    fun `Success of Unit is valid for action-only calls`() {
        val r: CallResult<Unit> = CallResult.Success(Unit)
        assertEquals(Unit, (r as CallResult.Success).data)
    }

    @Test
    fun `Throttled and Disabled are singleton objects`() {
        assertSame(CallResult.Throttled, CallResult.Throttled)
        assertSame(CallResult.Disabled, CallResult.Disabled)
    }

    @Test
    fun `HttpError carries code and body`() {
        val r = CallResult.HttpError(500, "oops")
        assertEquals(500, r.code)
        assertEquals("oops", r.body)
    }

    @Test
    fun `NetworkFailure carries cause`() {
        val cause = IOException("boom")
        val r = CallResult.NetworkFailure(cause)
        assertSame(cause, r.cause)
    }

    @Test
    fun `exhaustive when compiles`() {
        fun label(r: CallResult<Int>): String = when (r) {
            is CallResult.Success -> "ok"
            CallResult.Throttled -> "throttled"
            CallResult.Disabled -> "disabled"
            is CallResult.HttpError -> "http"
            is CallResult.NetworkFailure -> "net"
        }
        assertEquals("ok", label(CallResult.Success(1)))
    }
}
