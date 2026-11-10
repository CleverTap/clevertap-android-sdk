package com.clevertap.android.sdk.network.http

import com.clevertap.android.sdk.network.http.UrlConnectionHttpClient.Companion.AMAZON_ROOT_CA1_PATH
import io.mockk.mockk
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun `createSslContext builds a TLS context from the bundled certificate`() {
        val sslContext = client.createSslContext()

        assertNotNull(sslContext)
        assertEquals("TLS", sslContext!!.protocol)
    }

    @Test
    fun `bundled certificate is Amazon Root CA 1`() {
        val stream = UrlConnectionHttpClient::class.java.classLoader?.getResourceAsStream(AMAZON_ROOT_CA1_PATH)
        assertNotNull("Certificate missing at $AMAZON_ROOT_CA1_PATH", stream)

        val certificate = stream!!.use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }

        assertEquals("CN=Amazon Root CA 1,O=Amazon,C=US", certificate.subjectX500Principal.name)
    }

    @Test
    fun `KeyStore class loader cannot see the bundled certificate`() {
        // Root cause of the old bug: javaClass inside KeyStore.apply {} resolved to KeyStore's loader
        val stream = KeyStore::class.java.classLoader?.getResourceAsStream(AMAZON_ROOT_CA1_PATH)

        assertNull(stream)
    }
}
