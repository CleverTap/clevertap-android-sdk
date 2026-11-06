package com.clevertap.android.sdk.pushnotification.preview

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Pure JVM: the policy uses java.net.URI only, so no Robolectric is needed.
 * Each rejected case names the attack it stands for.
 */
class PushPreviewUrlPolicyTest {

    private val host = "sk1-eventstore-inapppreviews3bucket-gd8sly0omq1m.s3.ap-northeast-2.amazonaws.com"
    private val policy = PushPreviewUrlPolicy(setOf(host))
    private val goodPath = "/inapp-preview-campaign/1699358338/8bd4c58d-2632-45da-95e6-5ef23d55d6f7"
    private val good = "https://$host$goodPath?X-Amz-Expires=240&X-Amz-Signature=2e5e0145"

    @Test
    fun `real backend url is allowed`() {
        assertNull(policy.reasonToReject(good))
    }

    @Test
    fun `scheme and host are matched case-insensitively`() {
        assertNull(policy.reasonToReject("HTTPS://${host.uppercase()}$goodPath"))
    }

    @Test
    fun `explicit https port is allowed`() {
        assertNull(policy.reasonToReject("https://$host:443$goodPath"))
    }

    @Test
    fun `default host list contains the known preview bucket`() {
        assertNull(PushPreviewUrlPolicy().reasonToReject(good))
    }

    @Test
    fun `http is rejected`() {
        assertEquals("scheme", policy.reasonToReject("http://$host$goodPath"))
    }

    @Test
    fun `non-http schemes are rejected`() {
        for (url in listOf("file:///etc/passwd", "javascript:alert(1)", "content://x/y", "data:text/html,hi")) {
            assertNotNull(policy.reasonToReject(url), "must reject $url")
        }
    }

    @Test
    fun `host suffix tricks are rejected`() {
        assertEquals("host not allowed: $host.evil.com", policy.reasonToReject("https://$host.evil.com$goodPath"))
        assertEquals("host not allowed: evil$host", policy.reasonToReject("https://evil$host$goodPath"))
        assertEquals("host not allowed: evil.com", policy.reasonToReject("https://evil.com/$host$goodPath"))
    }

    @Test
    fun `userinfo is rejected`() {
        assertEquals("userinfo", policy.reasonToReject("https://$host@evil.com$goodPath"))
        assertEquals("userinfo", policy.reasonToReject("https://evil.com@$host$goodPath"))
    }

    @Test
    fun `non-https port is rejected`() {
        assertEquals("port", policy.reasonToReject("https://$host:8443$goodPath"))
    }

    @Test
    fun `fragment is rejected`() {
        assertEquals("fragment", policy.reasonToReject("https://$host$goodPath#x"))
    }

    @Test
    fun `wrong path prefix is rejected`() {
        assertEquals("path prefix", policy.reasonToReject("https://$host/other-folder/x"))
        assertEquals("path prefix", policy.reasonToReject("https://$host/"))
    }

    @Test
    fun `path traversal and encoding tricks are rejected`() {
        assertEquals("path encoding", policy.reasonToReject("https://$host/inapp-preview-campaign/../secrets"))
        assertEquals("path encoding", policy.reasonToReject("https://$host/inapp-preview-campaign//x"))
        assertEquals("path encoding", policy.reasonToReject("https://$host/inapp-preview-campaign/%2e%2e/x"))
    }

    @Test
    fun `control characters, spaces, backslashes and non-ascii are rejected`() {
        assertEquals("illegal character", policy.reasonToReject("https://$host$goodPath\r\nX: y"))
        assertEquals("illegal character", policy.reasonToReject("https://$host$goodPath a"))
        assertEquals("illegal character", policy.reasonToReject("https://$host\\evil.com$goodPath"))
        // Cyrillic "е" in place of the Latin one: a look-alike domain.
        assertEquals("illegal character", policy.reasonToReject("https://sk1-еventstore.amazonaws.com$goodPath"))
    }

    @Test
    fun `ipv6 literal and empty host are rejected`() {
        assertNotNull(policy.reasonToReject("https://[::1]$goodPath"))
        assertNotNull(policy.reasonToReject("https:///$goodPath"))
    }

    @Test
    fun `unparsable and oversized urls are rejected`() {
        assertEquals("unparsable", policy.reasonToReject("https://$host/inapp-preview-campaign/{x}"))
        assertEquals("too long", policy.reasonToReject("https://$host$goodPath?" + "a".repeat(5000)))
    }

    @Test
    fun `rejection reason never echoes the url`() {
        val reason = policy.reasonToReject("https://evil.com$goodPath?secret=1")!!
        assertFalse(reason.contains("secret"))
    }
}
