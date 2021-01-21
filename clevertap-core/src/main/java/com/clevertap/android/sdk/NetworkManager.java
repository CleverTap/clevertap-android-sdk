package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.CTJsonConverter.getRenderedTargetList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.pushnotification.PushProviders;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONObject;

class NetworkManager extends BaseNetworkManager {

    private static SSLSocketFactory sslSocketFactory;

    private static SSLContext sslContext;

    private boolean enableNetworkInfoReporting = false;

    private final CleverTapInstanceConfig mConfig;

    private final Context mContext;

    private final BaseQueueManager mBaseEventQueueManager;

    private final CTLockManager mCtLockManager;

    private int mCurrentRequestTimestamp = 0;

    private CleverTapResponse mCleverTapResponse;

    private final DeviceInfo mDeviceInfo;

    private final CoreMetaData mCoreMetaData;

    private final Logger mLogger;

    private final BaseDatabaseManager mDatabaseManager;

    private final PostAsyncSafelyHandler mPostAsyncSafelyHandler;

    private int mResponseFailureCount = 0;// TODO encapsulate into NetworkState class

    private final InAppFCManager mInAppFCManager;

    private final PushProviders mPushProvider;

    private final ValidationResultStack mValidationResultStack;

    private int networkRetryCount = 0;// TODO encapsulate into NetworkState class

    NetworkManager(CoreState coreState) {
        mContext = coreState.getContext();
        mConfig = coreState.getConfig();
        mDeviceInfo = coreState.getDeviceInfo();
        mLogger = mConfig.getLogger();

        mCoreMetaData = coreState.getCoreMetaData();
        mValidationResultStack = coreState.getValidationResultStack();
        mPushProvider = coreState.getPushProviders();
        mInAppFCManager = coreState.getInAppFCManager();
        mDatabaseManager = coreState.getDatabaseManager();
        mCtLockManager = coreState.getCTLockManager();
        mBaseEventQueueManager = coreState.getBaseEventQueueManager();
        mPostAsyncSafelyHandler = coreState.getPostAsyncSafelyHandler();

        // maintain order
        CleverTapResponse cleverTapResponse = new CleverTapResponseHelper(coreState);

        cleverTapResponse = new GeofenceResponse(cleverTapResponse);
        cleverTapResponse = new ProductConfigResponse(cleverTapResponse);
        cleverTapResponse = new FeatureFlagResponse(cleverTapResponse);
        cleverTapResponse = new DisplayUnitResponse(cleverTapResponse);
        cleverTapResponse = new PushAmpResponse(cleverTapResponse);
        cleverTapResponse = new InboxResponse(cleverTapResponse);

        cleverTapResponse = new ConsoleResponse(cleverTapResponse);
        cleverTapResponse = new ARPResponse(cleverTapResponse);
        cleverTapResponse = new MetadataResponse(cleverTapResponse);
        cleverTapResponse = new InAppResponse(cleverTapResponse);

        cleverTapResponse = new BaseResponse(cleverTapResponse);

        setCleverTapResponse(cleverTapResponse);

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

    void performHandshakeForDomain(final Context context, final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback) {
        final String endpoint = getEndpoint(true, eventGroup);
        if (endpoint == null) {
            mLogger.verbose(mConfig.getAccountId(), "Unable to perform handshake, endpoint is null");
        }
        mLogger.verbose(mConfig.getAccountId(), "Performing handshake with " + endpoint);

        HttpsURLConnection conn = null;
        try {
            conn = buildHttpsURLConnection(endpoint);
            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                mLogger
                        .verbose(mConfig.getAccountId(),
                                "Invalid HTTP status code received for handshake - " + responseCode);
                return;
            }

            mLogger.verbose(mConfig.getAccountId(), "Received success from handshake :)");

            if (processIncomingHeaders(context, conn)) {
                mLogger.verbose(mConfig.getAccountId(), "We are not muted");
                // We have a new domain, run the callback
                handshakeSuccessCallback.run();
            }
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "Failed to perform handshake!", t);
        } finally {
            if (conn != null) {
                try {
                    conn.getInputStream().close();
                    conn.disconnect();
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }
    }

    boolean hasDomainChanged(final String newDomain) {
        final String oldDomain = StorageHelper.getStringFromPrefs(mContext, mConfig, Constants.KEY_DOMAIN_NAME, null);
        return !newDomain.equals(oldDomain);
    }

    @Override
    boolean needsHandshakeForDomain(final EventGroup eventGroup) {
        final String domain = getDomainFromPrefsOrMetadata(eventGroup);
        return domain == null || mResponseFailureCount > 5;
    }

    void enableDeviceNetworkInfoReporting(boolean value) {
        enableNetworkInfoReporting = value;
        StorageHelper.putBoolean(mContext, StorageHelper.storageKeyWithSuffix(mConfig, Constants.NETWORK_INFO),
                enableNetworkInfoReporting);
        mLogger
                .verbose(mConfig.getAccountId(),
                        "Device Network Information reporting set to " + enableNetworkInfoReporting);
    }

    CleverTapResponse getCleverTapResponse() {
        return mCleverTapResponse;
    }

    void setCleverTapResponse(final CleverTapResponse cleverTapResponse) {
        mCleverTapResponse = cleverTapResponse;
    }

    /**
     * Processes the incoming response headers for a change in domain and/or mute.
     *
     * @return True to continue sending requests, false otherwise.
     */
    boolean processIncomingHeaders(final Context context, final HttpsURLConnection conn) {
        final String muteCommand = conn.getHeaderField(Constants.HEADER_MUTE);
        if (muteCommand != null && muteCommand.trim().length() > 0) {
            if (muteCommand.equals("true")) {
                setMuted(context, true);
                return false;
            } else {
                setMuted(context, false);
            }
        }

        final String domainName = conn.getHeaderField(Constants.HEADER_DOMAIN_NAME);
        Logger.v("Getting domain from header - " + domainName);
        if (domainName == null || domainName.trim().length() == 0) {
            return true;
        }

        final String spikyDomainName = conn.getHeaderField(Constants.SPIKY_HEADER_DOMAIN_NAME);
        Logger.v("Getting spiky domain from header - " + spikyDomainName);

        setMuted(context, false);
        setDomain(context, domainName);
        Logger.v("Setting spiky domain from header as -" + spikyDomainName);
        if (spikyDomainName == null) {
            setSpikyDomain(context, domainName);
        } else {
            setSpikyDomain(context, spikyDomainName);
        }
        return true;
    }

    void setDomain(final Context context, String domainName) {
        mLogger.verbose(mConfig.getAccountId(), "Setting domain to " + domainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_DOMAIN_NAME),
                domainName);
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

    @Override
    int getDelayFrequency() {

        int minDelayFrequency = 0;

        mLogger.debug(mConfig.getAccountId(), "Network retry #" + networkRetryCount);

        //Retry with delay as 1s for first 10 retries
        if (networkRetryCount < 10) {
            mLogger.debug(mConfig.getAccountId(),
                    "Failure count is " + networkRetryCount + ". Setting delay frequency to 1s");
            minDelayFrequency = Constants.PUSH_DELAY_MS; //reset minimum delay to 1s
            return minDelayFrequency;
        }

        if (mConfig.getAccountRegion() == null) {
            //Retry with delay as 1s if region is null in case of eu1
            mLogger.debug(mConfig.getAccountId(), "Setting delay frequency to 1s");
            return Constants.PUSH_DELAY_MS;
        } else {
            //Retry with delay as minimum delay frequency and add random number of seconds to scatter traffic
            Random randomGen = new Random();
            int randomDelay = (randomGen.nextInt(10) + 1) * 1000;
            minDelayFrequency += randomDelay;
            if (minDelayFrequency < Constants.MAX_DELAY_FREQUENCY) {
                mLogger.debug(mConfig.getAccountId(), "Setting delay frequency to " + minDelayFrequency);
                return minDelayFrequency;
            } else {
                minDelayFrequency = Constants.PUSH_DELAY_MS;
            }
            mLogger.debug(mConfig.getAccountId(), "Setting delay frequency to " + minDelayFrequency);
            return minDelayFrequency;
        }
    }

    void setSpikyDomain(final Context context, String spikyDomainName) {
        mLogger.verbose(mConfig.getAccountId(), "Setting spiky domain to " + spikyDomainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.SPIKY_KEY_DOMAIN_NAME),
                spikyDomainName);
    }

    @Override
    void initHandshake(final EventGroup eventGroup, final Runnable handshakeSuccessCallback) {
        mResponseFailureCount = 0;
        setDomain(mContext, null);
        performHandshakeForDomain(mContext, eventGroup, handshakeSuccessCallback);
    }

    //New namespace for ARP Shared Prefs
    String getNewNamespaceARPKey() {

        final String accountId = mConfig.getAccountId();
        if (accountId == null) {
            return null;
        }

        mLogger.verbose(mConfig.getAccountId(), "New ARP Key = ARP:" + accountId + ":" + mDeviceInfo.getDeviceID());
        return "ARP:" + accountId + ":" + mDeviceInfo.getDeviceID();
    }

    //Session
    private void clearFirstRequestTimestampIfNeeded(Context context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_FIRST_TS), 0);
    }

    void setFirstRequestTimestampIfNeeded(int ts) {
        if (getFirstRequestTimestamp() > 0) {
            return;
        }
        StorageHelper.putInt(mContext, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_FIRST_TS), ts);
    }

    //gives delay frequency based on region
    //randomly adds delay to 1s delay in case of non-EU regions

    void incrementResponseFailureCount() {
        mResponseFailureCount++;
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

    String insertHeader(Context context, JSONArray arr) {
        try {
            final JSONObject header = new JSONObject();

            String deviceId = mDeviceInfo.getDeviceID();
            if (deviceId != null && !deviceId.equals("")) {
                header.put("g", deviceId);
            } else {
                mLogger.verbose(mConfig.getAccountId(),
                        "CRITICAL: Couldn't finalise on a device ID! Using error device ID instead!");
            }

            header.put("type", "meta");

            JSONObject appFields = getAppLaunchedFields();
            header.put("af", appFields);

            long i = getI();
            if (i > 0) {
                header.put("_i", i);
            }

            long j = getJ();
            if (j > 0) {
                header.put("_j", j);
            }

            String accountId = mConfig.getAccountId();
            String token = mConfig.getAccountToken();

            if (accountId == null || token == null) {
                mLogger
                        .debug(mConfig.getAccountId(),
                                "Account ID/token not found, unable to configure queue request");
                return null;
            }

            header.put("id", accountId);
            header.put("tk", token);
            header.put("l_ts", getLastRequestTimestamp());
            header.put("f_ts", getFirstRequestTimestamp());
            header.put("ct_pi", IdentityRepoFactory
                    .getRepo(mContext, mConfig, mDeviceInfo,
                            mValidationResultStack).getIdentitySet().toString());
            header.put("ddnd",
                    !(mDeviceInfo.getNotificationsEnabledForUser() && (mPushProvider.isNotificationSupported())));
            if (mCoreMetaData.isBgPing()) {
                header.put("bk", 1);
                mCoreMetaData.setBgPing(false);
            }
            header.put("rtl", getRenderedTargetList(mDatabaseManager.loadDBAdapter(mContext)));
            if (!mCoreMetaData.isInstallReferrerDataSent()) {
                header.put("rct", mCoreMetaData.getReferrerClickTime());
                header.put("ait", mCoreMetaData.getAppInstallTime());
            }
            header.put("frs", mCoreMetaData.isFirstRequestInSession());
            mCoreMetaData.setFirstRequestInSession(false);

            // Attach ARP
            try {
                final JSONObject arp = getARP();
                if (arp != null && arp.length() > 0) {
                    header.put("arp", arp);
                }
            } catch (Throwable t) {
                mLogger.verbose(mConfig.getAccountId(), "Failed to attach ARP", t);
            }

            JSONObject ref = new JSONObject();
            try {

                String utmSource = mCoreMetaData.getSource();
                if (utmSource != null) {
                    ref.put("us", utmSource);
                }

                String utmMedium = mCoreMetaData.getMedium();
                if (utmMedium != null) {
                    ref.put("um", utmMedium);
                }

                String utmCampaign = mCoreMetaData.getCampaign();
                if (utmCampaign != null) {
                    ref.put("uc", utmCampaign);
                }

                if (ref.length() > 0) {
                    header.put("ref", ref);
                }

            } catch (Throwable t) {
                mLogger.verbose(mConfig.getAccountId(), "Failed to attach ref", t);
            }

            JSONObject wzrkParams = mCoreMetaData.getWzrkParams();
            if (wzrkParams != null && wzrkParams.length() > 0) {
                header.put("wzrk_ref", wzrkParams);
            }

            if (mInAppFCManager != null) {
                Logger.v("Attaching InAppFC to Header");
                mInAppFCManager.attachToHeader(context, header);
            }

            // Resort to string concat for backward compatibility
            return "[" + header.toString() + ", " + arr.toString().substring(1);
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "CommsManager: Failed to attach header", t);
            return arr.toString();
        }
    }

    /**
     * @return true if the network request succeeded. Anything non 200 results in a false.
     */
    @Override
    boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue) {
        if (queue == null || queue.length() <= 0) {
            return false;
        }

        if (mDeviceInfo.getDeviceID() == null) {
            mLogger.debug(mConfig.getAccountId(), "CleverTap Id not finalized, unable to send queue");
            return false;
        }

        HttpsURLConnection conn = null;
        try {
            final String endpoint = getEndpoint(false, eventGroup);

            // This is just a safety check, which would only arise
            // if upstream didn't adhere to the protocol (sent nothing during the initial handshake)
            if (endpoint == null) {
                mLogger.debug(mConfig.getAccountId(), "Problem configuring queue endpoint, unable to send queue");
                return false;
            }

            conn = buildHttpsURLConnection(endpoint);

            final String body;
            final String req = insertHeader(context, queue);
            if (req == null) {
                mLogger.debug(mConfig.getAccountId(), "Problem configuring queue request, unable to send queue");
                return false;
            }

            mLogger.debug(mConfig.getAccountId(), "Send queue contains " + queue.length() + " items: " + req);
            mLogger.debug(mConfig.getAccountId(), "Sending queue to: " + endpoint);
            conn.setDoOutput(true);
            // noinspection all
            conn.getOutputStream().write(req.getBytes("UTF-8"));

            final int responseCode = conn.getResponseCode();

            // Always check for a 200 OK
            if (responseCode != 200) {
                throw new IOException("Response code is not 200. It is " + responseCode);
            }

            // Check for a change in domain
            final String newDomain = conn.getHeaderField(Constants.HEADER_DOMAIN_NAME);
            if (newDomain != null && newDomain.trim().length() > 0) {
                if (hasDomainChanged(newDomain)) {
                    // The domain has changed. Return a status of -1 so that the caller retries
                    setDomain(context, newDomain);
                    mLogger.debug(mConfig.getAccountId(),
                            "The domain has changed to " + newDomain + ". The request will be retried shortly.");
                    return false;
                }
            }

            if (processIncomingHeaders(context, conn)) {
                // noinspection all
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
                getCleverTapResponse().processResponse(null, body, mContext);
            }

            setLastRequestTimestamp(getCurrentRequestTimestamp());
            setFirstRequestTimestampIfNeeded(getCurrentRequestTimestamp());

            mLogger.debug(mConfig.getAccountId(), "Queue sent successfully");

            mResponseFailureCount = 0;
            networkRetryCount = 0; //reset retry count when queue is sent successfully
            return true;
        } catch (Throwable e) {
            mLogger.debug(mConfig.getAccountId(),
                    "An exception occurred while sending the queue, will retry: " + e.getLocalizedMessage());
            mResponseFailureCount++;
            networkRetryCount++;
            mBaseEventQueueManager.scheduleQueueFlush(context);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.getInputStream().close();
                    conn.disconnect();
                } catch (Throwable t) {
                    // Ignore
                }
            }
        }
    }

    void setDeviceNetworkInfoReportingFromStorage() {
        boolean enabled = StorageHelper.getBooleanFromPrefs(mContext, mConfig, Constants.NETWORK_INFO);
        mLogger
                .verbose(mConfig.getAccountId(),
                        "Setting device network info reporting state from storage to " + enabled);
        enableNetworkInfoReporting = enabled;
    }

    @SuppressLint("CommitPrefEdits")
    void setI(Context context, long i) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_I), i);
        StorageHelper.persist(editor);
    }

    @SuppressLint("CommitPrefEdits")
    void setJ(Context context, long j) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_J), j);
        StorageHelper.persist(editor);
    }

    /**
     * The ARP is additional request parameters, which must be sent once
     * received after any HTTP call. This is sort of a proxy for cookies.
     *
     * @return A JSON object containing the ARP key/values. Can be null.
     */
    private JSONObject getARP() {
        try {
            final String nameSpaceKey = getNewNamespaceARPKey();
            if (nameSpaceKey == null) {
                return null;
            }

            SharedPreferences prefs;

            //checking whether new namespace is empty or not
            //if not empty, using prefs of new namespace to send ARP
            //if empty, checking for old prefs
            if (!StorageHelper.getPreferences(mContext, nameSpaceKey).getAll().isEmpty()) {
                //prefs point to new namespace
                prefs = StorageHelper.getPreferences(mContext, nameSpaceKey);
            } else {
                //prefs point to new namespace migrated from old namespace
                prefs = migrateARPToNewNameSpace(nameSpaceKey, getNamespaceARPKey());
            }

            final Map<String, ?> all = prefs.getAll();
            final Iterator<? extends Entry<String, ?>> iter = all.entrySet().iterator();

            while (iter.hasNext()) {
                final Map.Entry<String, ?> kv = iter.next();
                final Object o = kv.getValue();
                if (o instanceof Number && ((Number) o).intValue() == -1) {
                    iter.remove();
                }
            }
            final JSONObject ret = new JSONObject(all);
            mLogger.verbose(mConfig.getAccountId(),
                    "Fetched ARP for namespace key: " + nameSpaceKey + " values: " + all.toString());
            return ret;
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "Failed to construct ARP object", t);
            return null;
        }
    }

    //Event
    JSONObject getAppLaunchedFields() {

        try {
            boolean deviceIsMultiUser = false;
            if (mDeviceInfo.getGoogleAdID() != null) {
                deviceIsMultiUser = new LoginInfoProvider(mContext, mConfig, mDeviceInfo).deviceIsMultiUser();
            }
            return CTJsonConverter.from(mDeviceInfo, mCoreMetaData.getLocationFromUser(), enableNetworkInfoReporting,
                    deviceIsMultiUser);
        } catch (Throwable t) {
            mLogger.verbose(mConfig.getAccountId(), "Failed to construct App Launched event", t);
            return new JSONObject();
        }
    }

    private long getI() {
        return StorageHelper.getLongFromPrefs(mContext, mConfig, Constants.KEY_I, 0, Constants.NAMESPACE_IJ);
    }

    private long getJ() {
        return StorageHelper.getLongFromPrefs(mContext, mConfig, Constants.KEY_J, 0, Constants.NAMESPACE_IJ);
    }

    //Session
    //Old namespace for ARP Shared Prefs
    private String getNamespaceARPKey() {

        final String accountId = mConfig.getAccountId();
        if (accountId == null) {
            return null;
        }

        mLogger.verbose(mConfig.getAccountId(), "Old ARP Key = ARP:" + accountId);
        return "ARP:" + accountId;
    }

    private SharedPreferences migrateARPToNewNameSpace(String newKey, String oldKey) {
        SharedPreferences oldPrefs = StorageHelper.getPreferences(mContext, oldKey);
        SharedPreferences newPrefs = StorageHelper.getPreferences(mContext, newKey);
        SharedPreferences.Editor editor = newPrefs.edit();
        Map<String, ?> all = oldPrefs.getAll();

        for (Map.Entry<String, ?> kv : all.entrySet()) {
            final Object o = kv.getValue();
            if (o instanceof Number) {
                final int update = ((Number) o).intValue();
                editor.putInt(kv.getKey(), update);
            } else if (o instanceof String) {
                if (((String) o).length() < 100) {
                    editor.putString(kv.getKey(), (String) o);
                } else {
                    mLogger.verbose(mConfig.getAccountId(),
                            "ARP update for key " + kv.getKey() + " rejected (string value too long)");
                }
            } else if (o instanceof Boolean) {
                editor.putBoolean(kv.getKey(), (Boolean) o);
            } else {
                mLogger.verbose(mConfig.getAccountId(),
                        "ARP update for key " + kv.getKey() + " rejected (invalid data type)");
            }
        }
        mLogger.verbose(mConfig.getAccountId(), "Completed ARP update for namespace key: " + newKey + "");
        StorageHelper.persist(editor);
        oldPrefs.edit().clear().apply();
        return newPrefs;
    }

    //Session
    private void clearIJ(Context context) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        StorageHelper.persist(editor);
    }

    //Session
    private void clearLastRequestTimestamp(Context context) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_LAST_TS), 0);
    }

    /**
     * Only call async
     */
    private void clearQueues(final Context context) {
        synchronized (mCtLockManager.getEventLock()) {

            DBAdapter adapter = mDatabaseManager.loadDBAdapter(context);
            DBAdapter.Table tableName = DBAdapter.Table.EVENTS;

            adapter.removeEvents(tableName);
            tableName = DBAdapter.Table.PROFILE_EVENTS;
            adapter.removeEvents(tableName);

            clearUserContext(context);
        }
    }

    //Session
    private void clearUserContext(final Context context) {
        clearIJ(context);
        clearFirstRequestTimestampIfNeeded(context);
        clearLastRequestTimestamp(context);
    }

    private void setMuted(final Context context, boolean mute) {
        if (mute) {
            final int now = (int) (System.currentTimeMillis() / 1000);
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_MUTED), now);
            setDomain(context, null);

            // Clear all the queues
            mPostAsyncSafelyHandler.postAsyncSafely("CommsManager#setMuted", new Runnable() {
                @Override
                public void run() {
                    clearQueues(context);
                }
            });
        } else {
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(mConfig, Constants.KEY_MUTED), 0);
        }
    }

}
