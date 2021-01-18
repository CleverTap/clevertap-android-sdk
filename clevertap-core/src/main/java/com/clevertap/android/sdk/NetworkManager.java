package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.IOException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;

class NetworkManager extends BaseNetworkManager {

    private static SSLSocketFactory sslSocketFactory;

    private static SSLContext sslContext;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private int mCurrentRequestTimestamp = 0;

    private final DeviceInfo mDeviceInfo;

    private final Logger mLogger;

    private int mResponseFailureCount = 0;// TODO encapsulate into NetworkState class

    NetworkManager(CoreState coreState) {
        mContext = coreState.getContext();
        mConfig = coreState.getConfig();
        mDeviceInfo = coreState.getDeviceInfo();
        mLogger = mConfig.getLogger();
    }

    HttpsURLConnection buildHttpsURLConnection(final String endpoint)
            throws IOException {
        URL url = new URL(endpoint);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("X-CleverTap-Account-ID", mConfig.getAccountId());
        conn.setRequestProperty("X-CleverTap-Token", mConfig.getAccountToken());
        conn.setInstanceFollowRedirects(false);
        if (mConfig.isSslPinningEnabled()) {
            SSLContext _sslContext = getSSLContext();
            if (_sslContext != null) {
                conn.setSSLSocketFactory(getPinnedCertsSslSocketfactory(_sslContext));
            }
        }
        return conn;
    }

    int getCurrentRequestTimestamp() {
        return mCurrentRequestTimestamp;
    }

    void setCurrentRequestTimestamp(final int currentRequestTimestamp) {
        mCurrentRequestTimestamp = currentRequestTimestamp;
    }

    String getDomain(boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        String domain = getDomainFromPrefsOrMetadata(eventGroup);

        final boolean emptyDomain = domain == null || domain.trim().length() == 0;
        if (emptyDomain && !defaultToHandshakeURL) {
            return null;
        }

        if (emptyDomain) {
            domain = Constants.PRIMARY_DOMAIN + "/hello";
        } else {
            domain += "/a1";
        }

        return domain;
    }

    String getDomainFromPrefsOrMetadata(final EventGroup eventGroup) {

        try {
            final String region = mConfig.getAccountRegion();
            if (region != null && region.trim().length() > 0) {
                // Always set this to 0 so that the handshake is not performed during a HTTP failure
                setResponseFailureCount(0);
                if (eventGroup.equals(EventGroup.PUSH_NOTIFICATION_VIEWED)) {
                    return region.trim().toLowerCase() + eventGroup.httpResource + "." + Constants.PRIMARY_DOMAIN;
                } else {
                    return region.trim().toLowerCase() + "." + Constants.PRIMARY_DOMAIN;
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        if (eventGroup.equals(EventGroup.PUSH_NOTIFICATION_VIEWED)) {
            return StorageHelper.getStringFromPrefs(mContext, mConfig, Constants.SPIKY_KEY_DOMAIN_NAME, null);
        } else {
            return StorageHelper.getStringFromPrefs(mContext, mConfig, Constants.KEY_DOMAIN_NAME, null);
        }

    }

    String getEndpoint(final boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        String domain = getDomain(defaultToHandshakeURL, eventGroup);
        if (domain == null) {
            mLogger.verbose(mConfig.getAccountId(), "Unable to configure endpoint, domain is null");
            return null;
        }

        final String accountId = mConfig.getAccountId();

        if (accountId == null) {
            mLogger.verbose(mConfig.getAccountId(), "Unable to configure endpoint, accountID is null");
            return null;
        }

        String endpoint = "https://" + domain + "?os=Android&t=" + mDeviceInfo.getSdkVersion();
        endpoint += "&z=" + accountId;

        final boolean needsHandshake = needsHandshakeForDomain(eventGroup);
        // Don't attach ts if its handshake
        if (needsHandshake) {
            return endpoint;
        }

        mCurrentRequestTimestamp = (int) (System.currentTimeMillis() / 1000);
        endpoint += "&ts=" + getCurrentRequestTimestamp();

        return endpoint;
    }

    int getFirstRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(mContext, mConfig, Constants.KEY_FIRST_TS, 0);
    }

    int getLastRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(mContext, mConfig, Constants.KEY_LAST_TS, 0);
    }

    void setLastRequestTimestamp(int ts) {
        StorageHelper.putInt(mContext, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_LAST_TS), ts);
    }

    // TODO encapsulate into NetworkState class
    int getResponseFailureCount() {
        return mResponseFailureCount;
    }

    // TODO encapsulate into NetworkState class
    void setResponseFailureCount(final int responseFailureCount) {
        mResponseFailureCount = responseFailureCount;
    }

    boolean hasDomainChanged(final String newDomain) {
        // TODO implementation
        return true;
    }

    @Override
    void initHandshake(final EventGroup eventGroup, final Runnable handshakeSuccessCallback) {
        mResponseFailureCount = 0;
        setDomain(mContext, null);
        performHandshakeForDomain(mContext, eventGroup, handshakeSuccessCallback);
    }

    String insertHeader(Context context, JSONArray arr) {
        // TODO implementation
        return null;
    }

    @Override
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

    void setFirstRequestTimestampIfNeeded(int ts) {
        if (getFirstRequestTimestamp() > 0) {
            return;
        }
        StorageHelper.putInt(mContext, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_FIRST_TS), ts);
    }

    void setSpikyDomain(final Context context, String spikyDomainName) {
        // TODO implementation
    }

    static boolean isNetworkOnline(Context context) {

        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                // lets be optimistic, if we are truly offline we handle the exception
                return true;
            }
            @SuppressLint("MissingPermission") NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        } catch (Throwable ignore) {
            // lets be optimistic, if we are truly offline we handle the exception
            return true;
        }
    }

    private static SSLSocketFactory getPinnedCertsSslSocketfactory(SSLContext sslContext) {
        if (sslContext == null) {
            return null;
        }

        if (sslSocketFactory == null) {
            try {
                sslSocketFactory = sslContext.getSocketFactory();
                Logger.d("Pinning SSL session to DigiCertGlobalRoot CA certificate");
            } catch (Throwable e) {
                Logger.d("Issue in pinning SSL,", e);
            }
        }
        return sslSocketFactory;
    }

    private static synchronized SSLContext getSSLContext() {
        if (sslContext == null) {
            sslContext = new SSLContextBuilder().build();
        }
        return sslContext;
    }
}
