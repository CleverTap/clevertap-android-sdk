package com.clevertap.android.sdk.network;

import static com.clevertap.android.sdk.utils.CTJsonConverter.getRenderedTargetList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.LocalDataStore;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.db.QueueCursor;
import com.clevertap.android.sdk.events.EventGroup;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.response.ARPResponse;
import com.clevertap.android.sdk.response.BaseResponse;
import com.clevertap.android.sdk.response.CleverTapResponse;
import com.clevertap.android.sdk.response.CleverTapResponseHelper;
import com.clevertap.android.sdk.response.ConsoleResponse;
import com.clevertap.android.sdk.response.DisplayUnitResponse;
import com.clevertap.android.sdk.response.FeatureFlagResponse;
import com.clevertap.android.sdk.response.GeofenceResponse;
import com.clevertap.android.sdk.response.InAppResponse;
import com.clevertap.android.sdk.response.InboxResponse;
import com.clevertap.android.sdk.response.MetadataResponse;
import com.clevertap.android.sdk.response.ProductConfigResponse;
import com.clevertap.android.sdk.response.PushAmpResponse;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import com.clevertap.android.sdk.validation.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class NetworkManager extends BaseNetworkManager {

    private static SSLSocketFactory sslSocketFactory;

    private static SSLContext sslContext;

    private final BaseCallbackManager callbackManager;

    private CleverTapResponse cleverTapResponse;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ControllerManager controllerManager;

    private final CoreMetaData coreMetaData;

    private int currentRequestTimestamp = 0;

    private final BaseDatabaseManager databaseManager;

    private final DeviceInfo deviceInfo;

    private final Logger logger;

    private int networkRetryCount = 0;

    private final ValidationResultStack validationResultStack;

    private int responseFailureCount = 0;

    public static boolean isNetworkOnline(Context context) {

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

    public NetworkManager(
            Context context,
            CleverTapInstanceConfig config,
            DeviceInfo deviceInfo,
            CoreMetaData coreMetaData,
            ValidationResultStack validationResultStack,
            ControllerManager controllerManager,
            BaseDatabaseManager baseDatabaseManager,
            final BaseCallbackManager callbackManager,
            CTLockManager ctLockManager,
            Validator validator,
            LocalDataStore localDataStore) {
        this.context = context;
        this.config = config;
        this.deviceInfo = deviceInfo;
        this.callbackManager = callbackManager;
        logger = this.config.getLogger();

        this.coreMetaData = coreMetaData;
        this.validationResultStack = validationResultStack;
        this.controllerManager = controllerManager;
        databaseManager = baseDatabaseManager;
        // maintain order
        CleverTapResponse cleverTapResponse = new CleverTapResponseHelper();

        cleverTapResponse = new GeofenceResponse(cleverTapResponse, config, callbackManager);
        cleverTapResponse = new ProductConfigResponse(cleverTapResponse, config, coreMetaData, controllerManager);
        cleverTapResponse = new FeatureFlagResponse(cleverTapResponse, config, controllerManager);
        cleverTapResponse = new DisplayUnitResponse(cleverTapResponse, config,
                callbackManager, controllerManager);
        cleverTapResponse = new PushAmpResponse(cleverTapResponse, context, config, ctLockManager,
                baseDatabaseManager, callbackManager, controllerManager);
        cleverTapResponse = new InboxResponse(cleverTapResponse, config, ctLockManager,
                callbackManager, controllerManager);

        cleverTapResponse = new ConsoleResponse(cleverTapResponse, config);
        cleverTapResponse = new ARPResponse(cleverTapResponse, config, this, validator, controllerManager);
        cleverTapResponse = new MetadataResponse(cleverTapResponse, config, deviceInfo, this);
        cleverTapResponse = new InAppResponse(cleverTapResponse, config, controllerManager, false);

        cleverTapResponse = new BaseResponse(context, config, deviceInfo, this, localDataStore, cleverTapResponse);

        setCleverTapResponse(cleverTapResponse);

    }

    @Override
    public void flushDBQueue(final Context context, final EventGroup eventGroup) {
        config.getLogger()
                .verbose(config.getAccountId(), "Somebody has invoked me to send the queue to CleverTap servers");

        QueueCursor cursor;
        QueueCursor previousCursor = null;
        boolean loadMore = true;

        while (loadMore) {

            cursor = databaseManager.getQueuedEvents(context, 50, previousCursor, eventGroup);

            if (cursor == null || cursor.isEmpty()) {
                config.getLogger().verbose(config.getAccountId(), "No events in the queue, failing");
                break;
            }

            previousCursor = cursor;
            JSONArray queue = cursor.getData();

            if (queue == null || queue.length() <= 0) {
                config.getLogger().verbose(config.getAccountId(), "No events in the queue, failing");
                break;
            }

            loadMore = sendQueue(context, eventGroup, queue);
        }
    }

    //gives delay frequency based on region
    //randomly adds delay to 1s delay in case of non-EU regions
    @Override
    public int getDelayFrequency() {

        int minDelayFrequency = 0;

        logger.debug(config.getAccountId(), "Network retry #" + networkRetryCount);

        //Retry with delay as 1s for first 10 retries
        if (networkRetryCount < 10) {
            logger.debug(config.getAccountId(),
                    "Failure count is " + networkRetryCount + ". Setting delay frequency to 1s");
            minDelayFrequency = Constants.PUSH_DELAY_MS; //reset minimum delay to 1s
            return minDelayFrequency;
        }

        if (config.getAccountRegion() == null) {
            //Retry with delay as 1s if region is null in case of eu1
            logger.debug(config.getAccountId(), "Setting delay frequency to 1s");
            return Constants.PUSH_DELAY_MS;
        } else {
            //Retry with delay as minimum delay frequency and add random number of seconds to scatter traffic
            Random randomGen = new Random();
            int randomDelay = (randomGen.nextInt(10) + 1) * 1000;
            minDelayFrequency += randomDelay;
            if (minDelayFrequency < Constants.MAX_DELAY_FREQUENCY) {
                logger.debug(config.getAccountId(), "Setting delay frequency to " + minDelayFrequency);
                return minDelayFrequency;
            } else {
                minDelayFrequency = Constants.PUSH_DELAY_MS;
            }
            logger.debug(config.getAccountId(), "Setting delay frequency to " + minDelayFrequency);
            return minDelayFrequency;
        }
    }

    //New namespace for ARP Shared Prefs
    public String getNewNamespaceARPKey() {

        final String accountId = config.getAccountId();
        if (accountId == null) {
            return null;
        }

        logger.verbose(config.getAccountId(), "New ARP Key = ARP:" + accountId + ":" + deviceInfo.getDeviceID());
        return "ARP:" + accountId + ":" + deviceInfo.getDeviceID();
    }

    public void incrementResponseFailureCount() {
        responseFailureCount++;
    }

    @Override
    public void initHandshake(final EventGroup eventGroup, final Runnable handshakeSuccessCallback) {
        responseFailureCount = 0;
        setDomain(context, null);
        performHandshakeForDomain(context, eventGroup, handshakeSuccessCallback);
    }

    @Override
    public boolean needsHandshakeForDomain(final EventGroup eventGroup) {
        final String domain = getDomainFromPrefsOrMetadata(eventGroup);
        return domain == null || responseFailureCount > 5;
    }

    @SuppressLint("CommitPrefEdits")
    public void setI(Context context, long i) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(StorageHelper.storageKeyWithSuffix(config, Constants.KEY_I), i);
        StorageHelper.persist(editor);
    }

    @SuppressLint("CommitPrefEdits")
    public void setJ(Context context, long j) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(StorageHelper.storageKeyWithSuffix(config, Constants.KEY_J), j);
        StorageHelper.persist(editor);
    }

    HttpsURLConnection buildHttpsURLConnection(final String endpoint)
            throws IOException {
        URL url = new URL(endpoint);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("X-CleverTap-Account-ID", config.getAccountId());
        conn.setRequestProperty("X-CleverTap-Token", config.getAccountToken());
        conn.setInstanceFollowRedirects(false);
        if (config.isSslPinningEnabled()) {
            SSLContext _sslContext = getSSLContext();
            if (_sslContext != null) {
                conn.setSSLSocketFactory(getPinnedCertsSslSocketfactory(_sslContext));
            }
        }
        return conn;
    }

    CleverTapResponse getCleverTapResponse() {
        return cleverTapResponse;
    }

    void setCleverTapResponse(final CleverTapResponse cleverTapResponse) {
        this.cleverTapResponse = cleverTapResponse;
    }

    int getCurrentRequestTimestamp() {
        return currentRequestTimestamp;
    }

    void setCurrentRequestTimestamp(final int currentRequestTimestamp) {
        this.currentRequestTimestamp = currentRequestTimestamp;
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
            final String region = config.getAccountRegion();
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
            return StorageHelper.getStringFromPrefs(context, config, Constants.SPIKY_KEY_DOMAIN_NAME, null);
        } else {
            return StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null);
        }

    }

    String getEndpoint(final boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        String domain = getDomain(defaultToHandshakeURL, eventGroup);
        if (domain == null) {
            logger.verbose(config.getAccountId(), "Unable to configure endpoint, domain is null");
            return null;
        }

        final String accountId = config.getAccountId();

        if (accountId == null) {
            logger.verbose(config.getAccountId(), "Unable to configure endpoint, accountID is null");
            return null;
        }

        String endpoint = "https://" + domain + "?os=Android&t=" + deviceInfo.getSdkVersion();
        endpoint += "&z=" + accountId;

        final boolean needsHandshake = needsHandshakeForDomain(eventGroup);
        // Don't attach ts if its handshake
        if (needsHandshake) {
            return endpoint;
        }

        currentRequestTimestamp = (int) (System.currentTimeMillis() / 1000);
        endpoint += "&ts=" + getCurrentRequestTimestamp();

        return endpoint;
    }

    int getFirstRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(context, config, Constants.KEY_FIRST_TS, 0);
    }

    int getLastRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(context, config, Constants.KEY_LAST_TS, 0);
    }

    void setLastRequestTimestamp(int ts) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_LAST_TS), ts);
    }

    int getResponseFailureCount() {
        return responseFailureCount;
    }

    void setResponseFailureCount(final int responseFailureCount) {
        this.responseFailureCount = responseFailureCount;
    }

    //gives delay frequency based on region
    //randomly adds delay to 1s delay in case of non-EU regions

    boolean hasDomainChanged(final String newDomain) {
        final String oldDomain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null);
        return !newDomain.equals(oldDomain);
    }

    String insertHeader(Context context, JSONArray arr) {
        try {
            final JSONObject header = new JSONObject();

            String deviceId = deviceInfo.getDeviceID();
            if (deviceId != null && !deviceId.equals("")) {
                header.put("g", deviceId);
            } else {
                logger.verbose(config.getAccountId(),
                        "CRITICAL: Couldn't finalise on a device ID! Using error device ID instead!");
            }

            header.put("type", "meta");

            JSONObject appFields = deviceInfo.getAppLaunchedFields();
            header.put("af", appFields);

            long i = getI();
            if (i > 0) {
                header.put("_i", i);
            }

            long j = getJ();
            if (j > 0) {
                header.put("_j", j);
            }

            String accountId = config.getAccountId();
            String token = config.getAccountToken();

            if (accountId == null || token == null) {
                logger
                        .debug(config.getAccountId(),
                                "Account ID/token not found, unable to configure queue request");
                return null;
            }

            header.put("id", accountId);
            header.put("tk", token);
            header.put("l_ts", getLastRequestTimestamp());
            header.put("f_ts", getFirstRequestTimestamp());
            header.put("ct_pi", IdentityRepoFactory
                    .getRepo(this.context, config, deviceInfo,
                            validationResultStack).getIdentitySet().toString());
            header.put("ddnd",
                    !(deviceInfo.getNotificationsEnabledForUser() && (controllerManager.getPushProviders()
                            .isNotificationSupported())));
            if (coreMetaData.isBgPing()) {
                header.put("bk", 1);
                coreMetaData.setBgPing(false);
            }
            header.put("rtl", getRenderedTargetList(databaseManager.loadDBAdapter(this.context)));
            if (!coreMetaData.isInstallReferrerDataSent()) {
                header.put("rct", coreMetaData.getReferrerClickTime());
                header.put("ait", coreMetaData.getAppInstallTime());
            }
            header.put("frs", coreMetaData.isFirstRequestInSession());
            coreMetaData.setFirstRequestInSession(false);

            // Attach ARP
            try {
                final JSONObject arp = getARP();
                if (arp != null && arp.length() > 0) {
                    header.put("arp", arp);
                }
            } catch (Throwable t) {
                logger.verbose(config.getAccountId(), "Failed to attach ARP", t);
            }

            JSONObject ref = new JSONObject();
            try {

                String utmSource = coreMetaData.getSource();
                if (utmSource != null) {
                    ref.put("us", utmSource);
                }

                String utmMedium = coreMetaData.getMedium();
                if (utmMedium != null) {
                    ref.put("um", utmMedium);
                }

                String utmCampaign = coreMetaData.getCampaign();
                if (utmCampaign != null) {
                    ref.put("uc", utmCampaign);
                }

                if (ref.length() > 0) {
                    header.put("ref", ref);
                }

            } catch (Throwable t) {
                logger.verbose(config.getAccountId(), "Failed to attach ref", t);
            }

            JSONObject wzrkParams = coreMetaData.getWzrkParams();
            if (wzrkParams != null && wzrkParams.length() > 0) {
                header.put("wzrk_ref", wzrkParams);
            }

            if (controllerManager.getInAppFCManager() != null) {
                Logger.v("Attaching InAppFC to Header");
                controllerManager.getInAppFCManager().attachToHeader(context, header);
            }

            // Resort to string concat for backward compatibility
            return "[" + header.toString() + ", " + arr.toString().substring(1);
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "CommsManager: Failed to attach header", t);
            return arr.toString();
        }
    }

    void performHandshakeForDomain(final Context context, final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback) {
        final String endpoint = getEndpoint(true, eventGroup);
        if (endpoint == null) {
            logger.verbose(config.getAccountId(), "Unable to perform handshake, endpoint is null");
        }
        logger.verbose(config.getAccountId(), "Performing handshake with " + endpoint);

        HttpsURLConnection conn = null;
        try {
            conn = buildHttpsURLConnection(endpoint);
            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger
                        .verbose(config.getAccountId(),
                                "Invalid HTTP status code received for handshake - " + responseCode);
                return;
            }

            logger.verbose(config.getAccountId(), "Received success from handshake :)");

            if (processIncomingHeaders(context, conn)) {
                logger.verbose(config.getAccountId(), "We are not muted");
                // We have a new domain, run the callback
                handshakeSuccessCallback.run();
            }
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "Failed to perform handshake!", t);
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

    /**
     * @return true if the network request succeeded. Anything non 200 results in a false.
     */
    @Override
    boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue) {
        if (queue == null || queue.length() <= 0) {
            return false;
        }

        if (deviceInfo.getDeviceID() == null) {
            logger.debug(config.getAccountId(), "CleverTap Id not finalized, unable to send queue");
            return false;
        }

        HttpsURLConnection conn = null;
        try {
            final String endpoint = getEndpoint(false, eventGroup);

            // This is just a safety check, which would only arise
            // if upstream didn't adhere to the protocol (sent nothing during the initial handshake)
            if (endpoint == null) {
                logger.debug(config.getAccountId(), "Problem configuring queue endpoint, unable to send queue");
                return false;
            }

            conn = buildHttpsURLConnection(endpoint);

            final String body;
            final String req = insertHeader(context, queue);
            if (req == null) {
                logger.debug(config.getAccountId(), "Problem configuring queue request, unable to send queue");
                return false;
            }

            logger.debug(config.getAccountId(), "Send queue contains " + queue.length() + " items: " + req);
            logger.debug(config.getAccountId(), "Sending queue to: " + endpoint);
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
                    logger.debug(config.getAccountId(),
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
                getCleverTapResponse().processResponse(null, body, this.context);
            }

            setLastRequestTimestamp(getCurrentRequestTimestamp());
            setFirstRequestTimestampIfNeeded(getCurrentRequestTimestamp());

            logger.debug(config.getAccountId(), "Queue sent successfully");

            responseFailureCount = 0;
            networkRetryCount = 0; //reset retry count when queue is sent successfully
            return true;
        } catch (Throwable e) {
            logger.debug(config.getAccountId(),
                    "An exception occurred while sending the queue, will retry: ", e);
            responseFailureCount++;
            networkRetryCount++;
            callbackManager.getFailureFlushListener().failureFlush(context);
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

    void setDomain(final Context context, String domainName) {
        logger.verbose(config.getAccountId(), "Setting domain to " + domainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_DOMAIN_NAME),
                domainName);
    }

    void setFirstRequestTimestampIfNeeded(int ts) {
        if (getFirstRequestTimestamp() > 0) {
            return;
        }
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_FIRST_TS), ts);
    }

    void setSpikyDomain(final Context context, String spikyDomainName) {
        logger.verbose(config.getAccountId(), "Setting spiky domain to " + spikyDomainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.SPIKY_KEY_DOMAIN_NAME),
                spikyDomainName);
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
            if (!StorageHelper.getPreferences(context, nameSpaceKey).getAll().isEmpty()) {
                //prefs point to new namespace
                prefs = StorageHelper.getPreferences(context, nameSpaceKey);
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
            logger.verbose(config.getAccountId(),
                    "Fetched ARP for namespace key: " + nameSpaceKey + " values: " + all.toString());
            return ret;
        } catch (Throwable t) {
            logger.verbose(config.getAccountId(), "Failed to construct ARP object", t);
            return null;
        }
    }

    private long getI() {
        return StorageHelper.getLongFromPrefs(context, config, Constants.KEY_I, 0, Constants.NAMESPACE_IJ);
    }

    private long getJ() {
        return StorageHelper.getLongFromPrefs(context, config, Constants.KEY_J, 0, Constants.NAMESPACE_IJ);
    }

    //Session
    //Old namespace for ARP Shared Prefs
    private String getNamespaceARPKey() {

        final String accountId = config.getAccountId();
        if (accountId == null) {
            return null;
        }

        logger.verbose(config.getAccountId(), "Old ARP Key = ARP:" + accountId);
        return "ARP:" + accountId;
    }

    private SharedPreferences migrateARPToNewNameSpace(String newKey, String oldKey) {
        SharedPreferences oldPrefs = StorageHelper.getPreferences(context, oldKey);
        SharedPreferences newPrefs = StorageHelper.getPreferences(context, newKey);
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
                    logger.verbose(config.getAccountId(),
                            "ARP update for key " + kv.getKey() + " rejected (string value too long)");
                }
            } else if (o instanceof Boolean) {
                editor.putBoolean(kv.getKey(), (Boolean) o);
            } else {
                logger.verbose(config.getAccountId(),
                        "ARP update for key " + kv.getKey() + " rejected (invalid data type)");
            }
        }
        logger.verbose(config.getAccountId(), "Completed ARP update for namespace key: " + newKey + "");
        StorageHelper.persist(editor);
        oldPrefs.edit().clear().apply();
        return newPrefs;
    }

    private void setMuted(final Context context, boolean mute) {
        if (mute) {
            final int now = (int) (System.currentTimeMillis() / 1000);
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_MUTED), now);
            setDomain(context, null);

            // Clear all the queues
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
            task.execute("CommsManager#setMuted", new Callable<Void>() {
                @Override
                public Void call() {
                    databaseManager.clearQueues(context);
                    return null;
                }
            });
        } else {
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_MUTED), 0);
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
