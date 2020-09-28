package com.clevertap.android.sdk;


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
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);//Use null inputstream & password to create empty key store

            //noinspection ConstantConditions
            InputStream inputStream1 = new BufferedInputStream(getClass().getClassLoader()
                    .getResourceAsStream("com/clevertap/android/sdk/certificates/DigiCertGlobalRootCA.crt"));
            X509Certificate x509Certificate1 = (X509Certificate) certificateFactory.generateCertificate(inputStream1);
            keyStore.setCertificateEntry("DigiCertGlobalRootCA", x509Certificate1);

            InputStream inputStream2 = new BufferedInputStream(getClass().getClassLoader()
                    .getResourceAsStream("com/clevertap/android/sdk/certificates/DigiCertSHA2SecureServerCA.crt"));
            X509Certificate x509Certificate2 = (X509Certificate) certificateFactory.generateCertificate(inputStream2);
            keyStore.setCertificateEntry("DigiCertSHA2SecureServerCA", x509Certificate2);

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
