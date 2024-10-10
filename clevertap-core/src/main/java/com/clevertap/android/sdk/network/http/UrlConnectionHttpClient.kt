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

    companion object {
        const val READ_TIMEOUT = 10000
        const val CONNECT_TIMEOUT = 10000
    }

    private val socketFactory: SSLSocketFactory? by lazy {
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

    private fun createSslContext(): SSLContext? {
        try {

            val sslContext = SSLContext.getInstance("TLS").apply {
                val certificateFactory = CertificateFactory.getInstance("X.509")
                val trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm())
                    .apply {
                        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                            load(null, null) //Use null InputStream & password to create empty key store
                            val inputStream: InputStream =
                                BufferedInputStream(javaClass.classLoader?.getResourceAsStream("com/clevertap/android/sdk/certificates/AmazonRootCA1.cer"))
                            val x509Certificate3 = certificateFactory.generateCertificate(inputStream) as X509Certificate
                            setCertificateEntry("AmazonRootCA1", x509Certificate3)
                        }
                        init(keyStore)
                    }

                init(null, trustManagerFactory.trustManagers, null)
            }
            Logger.d("SSL Context built")
            return sslContext
        } catch (e: Exception) {
            Logger.i("Error building SSL Context", e)
        }
        return null
    }
}
