package com.clevertap.android.sdk.network;


import com.clevertap.android.sdk.Logger;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

final class SSLContextBuilder {

    SSLContext build() {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);//Use null inputstream & password to create empty key store

            //noinspection ConstantConditions
            InputStream inputStream3 = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("com/clevertap/android/sdk/certificates/AmazonRootCA1.cer"));
            X509Certificate x509Certificate3 = (X509Certificate) certificateFactory.generateCertificate(inputStream3);
            keyStore.setCertificateEntry("AmazonRootCA1", x509Certificate3);

            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            Logger.d("SSL Context built");
            return sslContext;
        } catch (Throwable e) {
            Logger.i("Error building SSL Context", e);
        }
        return null;
    }
}
