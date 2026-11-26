package com.clevertap.android.sdk.pushnotification.fcm

import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class FcmNotificationParserTest {

    private val parser = FcmNotificationParser()

    @Test
    fun `toBundle copies every data entry as a string extra`() {
        val message = mockk<RemoteMessage> {
            every { data } returns mapOf("nm" to "hello", "wzrk_pn" to "true")
        }

        val bundle = assertNotNull(parser.toBundle(message))

        assertEquals(setOf("nm", "wzrk_pn"), bundle.keySet())
        assertEquals("hello", bundle.getString("nm"))
        assertEquals("true", bundle.getString("wzrk_pn"))
    }

    @Test
    fun `toBundle returns an empty bundle for a message without data`() {
        val message = mockk<RemoteMessage> {
            every { data } returns emptyMap()
        }

        val bundle = assertNotNull(parser.toBundle(message))

        assertEquals(0, bundle.size())
    }

    @Test
    fun `toBundle returns null when reading the message data throws`() {
        val message = mockk<RemoteMessage> {
            every { data } throws IllegalStateException("broken")
        }

        assertNull(parser.toBundle(message))
    }
}
