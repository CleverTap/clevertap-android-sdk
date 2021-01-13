package com.clevertap.android.sdk;

import android.content.Context;
import java.io.IOException;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;

class NetworkManager {

    HttpsURLConnection buildHttpsURLConnection(final String endpoint)
            throws IOException {
        // TODO implementation
        return null;
    }

    String getDomain(boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        // TODO implementation
        return null;
    }

    String getDomainFromPrefsOrMetadata(final EventGroup eventGroup) {
        // TODO implementation
        return null;
    }

    String getEndpoint(final boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        // TODO implementation
        return null;
    }

    boolean hasDomainChanged(final String newDomain) {
        // TODO implementation
        return true;
    }

    String insertHeader(Context context, JSONArray arr) {
        // TODO implementation
        return null;
    }

    boolean isNetworkOnline(Context context) {

        // TODO implementation
        return true;
    }

    boolean needsHandshakeForDomain(final EventGroup eventGroup) {
        // TODO implementation
        return true;
    }

    void performHandshakeForDomain(final Context context, final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback) {
        // TODO implementation
    }

    boolean processIncomingHeaders(final Context context, final HttpsURLConnection conn) {
        // TODO implementation
        return true;
    }

    boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue) {
        // TODO implementation
        return true;
    }

    void setDomain(final Context context, String domainName) {
        // TODO implementation
    }

    void setSpikyDomain(final Context context, String spikyDomainName) {
        // TODO implementation
    }
}
