package com.clevertap.android.sdk.network.http

import com.clevertap.android.sdk.Logger
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

class UrlConnectionHttpClient(
    var isSslPinningEnabled: Boolean,
    private val logger: Logger,
    private val logTag: String
) : CtHttpClient {

    var readTimeout = 10000
    var connectTimeout = 10000

    private val sslSocketFactory: SSLSocketFactory? by lazy {
        try {
            Logger.d("Pinning SSL session to DigiCertGlobalRoot CA certificate")
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
            connection = openHttpsURLConnection(request)

            if (request.body != null) {
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(request.body.toByteArray(Charsets.UTF_8))
                }
            }
            logger.debug(logTag, "Sending request to: ${request.url}")

            // execute request
            val responseCode = connection.responseCode
            val headers = connection.headerFields
            val disconnectConnection = { connection.disconnect() }

            return if (responseCode == HttpURLConnection.HTTP_OK) {
                Response(request, responseCode, headers, connection.inputStream, disconnectConnection)
            } else {
                Response(request, responseCode, headers, connection.errorStream, disconnectConnection)
            }
        } catch (e: Exception) {
            connection?.disconnect()
            throw e
        }
    }

    private fun openHttpsURLConnection(request: Request): HttpsURLConnection {
        val url = URL(request.url.toString())
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout
        for (header in request.headers) {
            connection.setRequestProperty(header.key, header.value)
        }
        connection.instanceFollowRedirects = false
        if (isSslPinningEnabled && sslContext != null) {
            connection.sslSocketFactory = sslSocketFactory
        }
        return connection
    }

    private fun createSslContext(): SSLContext? {
        try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null) //Use null InputStream & password to create empty key store
            val inputStream: InputStream =
                BufferedInputStream(javaClass.classLoader?.getResourceAsStream("com/clevertap/android/sdk/certificates/AmazonRootCA1.cer"))
            val x509Certificate3 = certificateFactory.generateCertificate(inputStream) as X509Certificate
            keyStore.setCertificateEntry("AmazonRootCA1", x509Certificate3)
            trustManagerFactory.init(keyStore)
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, null)
            Logger.d("SSL Context built")
            return sslContext
        } catch (e: Exception) {
            Logger.i("Error building SSL Context", e)
        }
        return null
    }
}
