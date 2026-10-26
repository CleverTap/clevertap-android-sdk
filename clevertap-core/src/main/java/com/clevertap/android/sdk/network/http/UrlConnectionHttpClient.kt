package com.clevertap.android.sdk.network.http

import android.net.TrafficStats
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Logger
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

class UrlConnectionHttpClient(
    var isSslPinningEnabled: Boolean,
    private val logger: Logger,
    private val logTag: String
) : CtHttpClient {

    companion object {
        const val READ_TIMEOUT = 10000
        const val CONNECT_TIMEOUT = 10000
        const val NETWORK_TAG_HTTP_REQUESTS = 17
        @VisibleForTesting
        internal const val AMAZON_ROOT_CA1_PATH = "com/clevertap/android/sdk/certificates/AmazonRootCA1.cer"
    }

    private val socketFactory: SSLSocketFactory? by lazy {
        try {
            Logger.d("Pinning SSL session to AmazonRootCA1 certificate")
            sslContext?.socketFactory
        } catch (e: Exception) {
            Logger.d("Issue in pinning SSL,", e)
            null
        }
    }
    private val sslContext: SSLContext? by lazy { createSslContext() }

    override fun execute(request: Request): Response {
        var connection: HttpsURLConnection? = null

        try {
            TrafficStats.setThreadStatsTag(NETWORK_TAG_HTTP_REQUESTS)
            connection = openHttpsURLConnection(request)

            logger.debug(logTag, "Sending request to: ${request.url}")

            // execute request
            val responseCode = connection.responseCode
            val headers = connection.headerFields
            val disconnectConnection = { connection.disconnect() }

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                Response(
                    request = request,
                    code = responseCode,
                    headers = headers,
                    bodyStream = connection.inputStream,
                    closeDelegate = disconnectConnection
                )
            } else {
                Response(
                    request = request,
                    code = responseCode,
                    headers = headers,
                    bodyStream = connection.errorStream,
                    closeDelegate = disconnectConnection
                )
            }
        } catch (e: Exception) {
            connection?.disconnect()
            throw e
        } finally {
            TrafficStats.clearThreadStatsTag()
        }
    }

    private fun openHttpsURLConnection(request: Request): HttpsURLConnection {
        val url = URL(request.url.toString())
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            for (header in request.headers) {
                setRequestProperty(header.key, header.value)
            }
            instanceFollowRedirects = false
            if (isSslPinningEnabled && sslContext != null) {
                sslSocketFactory = socketFactory
            }
            if (request.body != null) {
                doOutput = true
                outputStream.use {
                    it.write(request.body.toByteArray(Charsets.UTF_8))
                }
            }
        }
        return connection
    }

    @VisibleForTesting
    internal fun createSslContext(): SSLContext? = try {
        // Use this class's loader: javaClass inside a scope function can point to a JDK class (e.g. KeyStore)
        val certificate = UrlConnectionHttpClient::class.java.classLoader
            ?.getResourceAsStream(AMAZON_ROOT_CA1_PATH)
            ?.use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
            ?: error("Certificate not found: $AMAZON_ROOT_CA1_PATH")

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null) // Use null InputStream & password to create empty key store
            setCertificateEntry("AmazonRootCA1", certificate)
        }
        val trustManagerFactory = TrustManagerFactory
            .getInstance(TrustManagerFactory.getDefaultAlgorithm())
            .apply { init(keyStore) }

        SSLContext.getInstance("TLS")
            .apply { init(null, trustManagerFactory.trustManagers, null) }
            .also { Logger.d("SSL Context built") }
    } catch (e: Exception) {
        Logger.i("Error building SSL Context", e)
        null
    }
}
