package com.clevertap.android.sdk.network.http

import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient.Companion.CERTIFICATES_DIR
import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient.Companion.PINNED_ROOT_CERTIFICATES
import io.mockk.mockk
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlConnectionHttpClientTest {

    private val client = UrlConnectionHttpClient(
        isSslPinningEnabled = true,
        logger = mockk(relaxed = true),
        logTag = "test"
    )

    @Test
    fun `createSslContext builds a TLS context from the bundled certificates`() {
        val sslContext = client.createSslContext()

        assertNotNull(sslContext)
        assertEquals("TLS", sslContext!!.protocol)
    }

    @Test
    fun `bundled certificates are the five Amazon Trust Services roots`() {
        val subjects = PINNED_ROOT_CERTIFICATES.map { name ->
            val path = "$CERTIFICATES_DIR/$name.cer"
            val stream = UrlConnectionHttpClient::class.java.classLoader?.getResourceAsStream(path)
            assertNotNull("Certificate missing at $path", stream)
            stream!!.use {
                (CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate)
                    .subjectX500Principal.name
            }
        }

        assertEquals(
            listOf(
                "CN=Amazon Root CA 1,O=Amazon,C=US",
                "CN=Amazon Root CA 2,O=Amazon,C=US",
                "CN=Amazon Root CA 3,O=Amazon,C=US",
                "CN=Amazon Root CA 4,O=Amazon,C=US",
                "CN=Starfield Services Root Certificate Authority - G2,O=Starfield Technologies\\, Inc.," +
                    "L=Scottsdale,ST=Arizona,C=US"
            ),
            subjects
        )
    }
}
