package com.clevertap.android.sdk.network;

import static com.clevertap.android.sdk.Utils.getSCDomain;
import static com.clevertap.android.sdk.utils.CTJsonConverter.getRenderedTargetList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTLockManager;
import com.clevertap.android.sdk.CTXtensions;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.db.QueueData;
import com.clevertap.android.sdk.events.EventGroup;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate;
import com.clevertap.android.sdk.inapp.evaluation.EventType;
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.network.api.CtApiWrapper;
import com.clevertap.android.sdk.network.api.DefineTemplatesRequestBody;
import com.clevertap.android.sdk.network.api.SendQueueRequestBody;
import com.clevertap.android.sdk.network.http.Response;
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil;
import com.clevertap.android.sdk.response.ARPResponse;
import com.clevertap.android.sdk.response.CleverTapResponse;
import com.clevertap.android.sdk.response.ConsoleResponse;
import com.clevertap.android.sdk.response.DisplayUnitResponse;
import com.clevertap.android.sdk.response.FeatureFlagResponse;
import com.clevertap.android.sdk.response.FetchVariablesResponse;
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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class NetworkManager {

    private final BaseCallbackManager callbackManager;
    private final List<CleverTapResponse> cleverTapResponses = new ArrayList<>();
    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ControllerManager controllerManager;

    private final CoreMetaData coreMetaData;

    private final CtApiWrapper ctApiWrapper;

    private final BaseDatabaseManager databaseManager;

    private final DeviceInfo deviceInfo;

    private final Logger logger;

    private int responseFailureCount = 0;

    private int networkRetryCount = 0;

    private final ValidationResultStack validationResultStack;

    private final Validator validator;

    private int minDelayFrequency = 0;

    private final List<NetworkHeadersListener> mNetworkHeadersListeners = new ArrayList<>();

    public void addNetworkHeadersListener(NetworkHeadersListener listener) {
        mNetworkHeadersListeners.add(listener);
    }

    public void removeNetworkHeadersListener(NetworkHeadersListener listener) {
        mNetworkHeadersListeners.remove(listener);
    }

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
        } catch (Exception ignore) {
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
            InAppResponse inAppResponse,
            final CtApiWrapper ctApiWrapper
    ) {
        this.context = context;
        this.config = config;
        this.deviceInfo = deviceInfo;
        this.callbackManager = callbackManager;
        this.validator = validator;
        logger = this.config.getLogger();

        this.coreMetaData = coreMetaData;
        this.validationResultStack = validationResultStack;
        this.controllerManager = controllerManager;
        databaseManager = baseDatabaseManager;
        this.ctApiWrapper = ctApiWrapper;

        cleverTapResponses.add(inAppResponse);
        cleverTapResponses.add(new MetadataResponse(config, deviceInfo, this));
        cleverTapResponses.add(new ARPResponse(config, this, validator, controllerManager));
        cleverTapResponses.add(new ConsoleResponse(config));
        cleverTapResponses.add(new InboxResponse(config, ctLockManager, callbackManager, controllerManager));
        cleverTapResponses.add(new PushAmpResponse(context, config, baseDatabaseManager, callbackManager, controllerManager));
        cleverTapResponses.add(new FetchVariablesResponse(config,controllerManager,callbackManager));
        cleverTapResponses.add(new DisplayUnitResponse(config, callbackManager, controllerManager));
        cleverTapResponses.add(new FeatureFlagResponse(config, controllerManager));
        cleverTapResponses.add(new ProductConfigResponse(config, coreMetaData, controllerManager));
        cleverTapResponses.add(new GeofenceResponse(config, callbackManager));
    }

    /**
     * Flushes the events queue from the local database to CleverTap servers.
     *
     * @param context    The Context object.
     * @param eventGroup The EventGroup indicating the type of events to be flushed.
     * @param caller     The optional caller identifier.
     */
    public void flushDBQueue(final Context context, final EventGroup eventGroup, @Nullable final String caller) {
        config.getLogger()
                .verbose(config.getAccountId(), "Somebody has invoked me to send the queue to CleverTap servers");

        QueueData cursor;
        QueueData previousCursor = null;
        boolean loadMore = true;

        while (loadMore) {

            // Retrieve queued events from the local database in batch size of 50
            cursor = databaseManager.getQueuedEvents(context, 50, previousCursor, eventGroup);

            if (cursor == null || cursor.isEmpty()) {
                // No events in the queue, log and break
                config.getLogger().verbose(config.getAccountId(), "No events in the queue, failing");

                if (eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED) {
                    // Notify listener for push impression sent to the server
                    if (previousCursor != null && previousCursor.getData() != null) {
                        try {
                            notifyListenersForPushImpressionSentToServer(previousCursor.getData());
                        } catch (Exception e) {
                            config.getLogger().verbose(config.getAccountId(),
                                    "met with exception while notifying listeners for PushImpressionSentToServer event");
                        }
                    }
                }
                break;
            }

            previousCursor = cursor;
            JSONArray queue = cursor.getData();

            if (queue == null || queue.length() <= 0) {
                // No events in the queue, log and break
                config.getLogger().verbose(config.getAccountId(), "No events in the queue, failing");
                break;
            }

            // Send the events queue to CleverTap servers
            loadMore = sendQueue(context, eventGroup, queue, caller);
            if (!loadMore) {
                // network error
                controllerManager.invokeCallbacksForNetworkError();
                controllerManager.invokeBatchListener(queue, false);
            } else {
                // response was successfully received
                controllerManager.invokeBatchListener(queue, true);
            }
        }
    }

    //gives delay frequency based on region
    //randomly adds delay to 1s delay in case of non-EU regions
    public int getDelayFrequency() {

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
            SecureRandom randomGen = new SecureRandom();
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

    @WorkerThread
    public void initHandshake(final EventGroup eventGroup, final Runnable handshakeSuccessCallback) {
        // Always set this to 0 so that the handshake is not performed during a HTTP failure
        responseFailureCount = 0;
        performHandshakeForDomain(context, eventGroup, handshakeSuccessCallback);
    }

    @WorkerThread
    public boolean needsHandshakeForDomain(final EventGroup eventGroup) {

        boolean needsHandshake = ctApiWrapper.needsHandshake(
                eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED
        );
        boolean needHandshakeDueToFailure = responseFailureCount > 5;

        if (needHandshakeDueToFailure) {
            setDomain(context, null);
        }
        return needsHandshake || needHandshakeDueToFailure;
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

    @WorkerThread
    int getCurrentRequestTimestamp() {
        return ctApiWrapper.getCtApi().getCurrentRequestTimestampSeconds();
    }

    @WorkerThread
    public String getDomain(final EventGroup eventGroup) {
        return ctApiWrapper.getCtApi().getActualDomain(eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED);
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

    boolean hasDomainChanged(final String newDomain) {
        final String oldDomain = StorageHelper.getStringFromPrefs(context, config, Constants.KEY_DOMAIN_NAME, null);
        return !newDomain.equals(oldDomain);
    }

    /**
     * Constructs a header {@link JSONObject} to be included as a first element of a sendQueue request
     *
     * @param context The Context object.
     * @param caller  The optional caller identifier.
     */
    private JSONObject getQueueHeader(Context context, @Nullable final String caller) {
        try {
            // Construct the header JSON object
            final JSONObject header = new JSONObject();

            // Add caller if available
            if (caller != null) {
                header.put(Constants.D_SRC, caller);
            }

            // Add device ID
            String deviceId = deviceInfo.getDeviceID();
            if (deviceId != null && !deviceId.equals("")) {
                header.put("g", deviceId);
            } else {
                logger.verbose(config.getAccountId(),
                        "CRITICAL: Couldn't finalise on a device ID! Using error device ID instead!");
            }

            // Add type as "meta"
            header.put("type", "meta");

            // Add app fields
            JSONObject appFields = deviceInfo.getAppLaunchedFields();
            if (coreMetaData.isWebInterfaceInitializedExternally()) {
                appFields.put("wv_init", true);
            }
            header.put("af", appFields);

            // Add _i and _j if available
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
                logger.debug(config.getAccountId(), "Account ID/token not found, unable to configure queue request");
                return null;
            }

            // Add account ID, token, and timestamps
            header.put("id", accountId);
            header.put("tk", token);
            header.put("l_ts", getLastRequestTimestamp());
            header.put("f_ts", getFirstRequestTimestamp());

            // Add ct_pi (identities)
            header.put("ct_pi", IdentityRepoFactory
                    .getRepo(this.context, config, deviceInfo,
                            validationResultStack).getIdentitySet().toString());

            // Add ddnd (Do Not Disturb)
            header.put("ddnd",
                    !(CTXtensions.areAppNotificationsEnabled(this.context)
                            && (controllerManager.getPushProviders() == null
                            || controllerManager.getPushProviders().isNotificationSupported())));

            // Add bk (Background Ping) if required
            if (coreMetaData.isBgPing()) {
                header.put("bk", 1);
                coreMetaData.setBgPing(false);
            }
            // Add rtl (Rendered Target List)
            header.put("rtl", getRenderedTargetList(databaseManager.loadDBAdapter(this.context)));

            // Add rct and ait (Referrer Click Time and App Install Time) if not sent before
            if (!coreMetaData.isInstallReferrerDataSent()) {
                header.put("rct", coreMetaData.getReferrerClickTime());
                header.put("ait", coreMetaData.getAppInstallTime());
            }
            // Add frs (First Request in Session) and update first request flag
            header.put("frs", coreMetaData.isFirstRequestInSession());

            // Add debug flag to show errors and events on the integration-debugger
            if (CleverTapAPI.getDebugLevel() == 3) {
                header.put("debug", true);
            }

            coreMetaData.setFirstRequestInSession(false);

            //Add ARP (Additional Request Parameters)
            try {
                final JSONObject arp = getARP();
                if (arp != null && arp.length() > 0) {
                    header.put("arp", arp);
                }
            } catch (JSONException e) {
                logger.verbose(config.getAccountId(), "Failed to attach ARP", e);
            }

            // Add ref (Referrer Information)
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

            } catch (JSONException e) {
                logger.verbose(config.getAccountId(), "Failed to attach ref", e);
            }

            // Add wzrk_ref (CleverTap-specific Parameters)
            JSONObject wzrkParams = coreMetaData.getWzrkParams();
            if (wzrkParams != null && wzrkParams.length() > 0) {
                header.put("wzrk_ref", wzrkParams);
            }

            // Attach InAppFC to header if available
            if (controllerManager.getInAppFCManager() != null) {
                Logger.v("Attaching InAppFC to Header");
                header.put("imp", controllerManager.getInAppFCManager().getShownTodayCount());
                header.put("tlc", controllerManager.getInAppFCManager().getInAppsCount(context));
            } else {
                logger.verbose(config.getAccountId(),
                        "controllerManager.getInAppFCManager() is NULL, not Attaching InAppFC to Header");
            }

            return header;
        } catch (JSONException e) {
            logger.verbose(config.getAccountId(), "CommsManager: Failed to attach header", e);
            return null;
        }
    }

    @WorkerThread
    private void performHandshakeForDomain(
            final Context context,
            final EventGroup eventGroup,
            final Runnable handshakeSuccessCallback
    ) {

        try (Response response = ctApiWrapper.getCtApi().performHandshakeForDomain(eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED)) {
            if (response.isSuccess()) {
                logger.verbose(config.getAccountId(), "Received success from handshake :)");

                if (processIncomingHeaders(context, response)) {
                    logger.verbose(config.getAccountId(), "We are not muted");
                    // We have a new domain, run the callback
                    handshakeSuccessCallback.run();
                }
            } else {
                logger.verbose(config.getAccountId(),
                        "Invalid HTTP status code received for handshake - " + response.getCode());
            }
        } catch (Exception e) {
            logger.verbose(config.getAccountId(), "Failed to perform handshake!", e);
        }
    }

    /**
     * Processes the incoming response headers for a change in domain and/or mute.
     *
     * @return True to continue sending requests, false otherwise.
     */
    @WorkerThread
    private boolean processIncomingHeaders(final Context context, Response response) {
        final String muteCommand = response.getHeaderValue(Constants.HEADER_MUTE);
        if (muteCommand != null && muteCommand.trim().length() > 0) {
            if (muteCommand.equals("true")) {
                setMuted(context, true);
                return false;
            } else {
                setMuted(context, false);
            }
        }

        final String domainName = response.getHeaderValue(Constants.HEADER_DOMAIN_NAME);
        Logger.v("Getting domain from header - " + domainName);
        if (domainName == null || domainName.trim().length() == 0) {
            return true;
        }

        final String spikyDomainName = response.getHeaderValue(Constants.SPIKY_HEADER_DOMAIN_NAME);
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
     * Sends the queue to the CleverTap server.
     *
     * @param context    The Context object.
     * @param eventGroup The EventGroup representing the type of event queue.
     * @param queue      The JSON array containing the event queue.
     * @param caller     The optional caller identifier.
     * @return True if the queue was sent successfully, false otherwise.
     */
    public boolean sendQueue(final Context context, final EventGroup eventGroup, final JSONArray queue,
            @Nullable final String caller) {
        if (queue == null || queue.length() <= 0) {
            // Empty queue, no need to send
            return false;
        }

        if (deviceInfo.getDeviceID() == null) {
            logger.debug(config.getAccountId(), "CleverTap Id not finalized, unable to send queue");
            return false;
        }

        EndpointId endpointId = EndpointId.fromEventGroup(eventGroup);
        JSONObject queueHeader = getQueueHeader(context, caller);
        applyQueueHeaderListeners(queueHeader, endpointId, queue.optJSONObject(0).has("profile"));

        final SendQueueRequestBody body = new SendQueueRequestBody(queueHeader, queue);
        logger.debug(config.getAccountId(), "Send queue contains " + queue.length() + " items: " + body);

        try (Response response = callApiForEventGroup(eventGroup, body)) {
            networkRetryCount = 0;
            boolean isProcessed;
            if (eventGroup == EventGroup.VARIABLES) {
                isProcessed = handleVariablesResponse(response);
            } else {
                isProcessed = handleSendQueueResponse(response, body, endpointId);
            }

            if (isProcessed) {
                responseFailureCount = 0;
            } else {
                responseFailureCount++;
            }
            return isProcessed;
        } catch (Exception e) {
            networkRetryCount++;
            responseFailureCount++;
            logger.debug(config.getAccountId(), "An exception occurred while sending the queue, will retry: ", e);
            if (callbackManager.getFailureFlushListener() != null) {
                callbackManager.getFailureFlushListener().failureFlush(context);
            }
            return false;
        }
    }

    @WorkerThread
    public boolean defineTemplates(final Context context, Collection<CustomTemplate> templates) {
        final JSONObject header = getQueueHeader(context, null);
        if (header == null) {
            return false;
        }

        final DefineTemplatesRequestBody body = new DefineTemplatesRequestBody(header, templates);
        logger.debug(config.getAccountId(), "Will define templates: " + body);

        try (Response response = ctApiWrapper.getCtApi().defineTemplates(body)) {
            if (response.isSuccess()) {
                handleTemplateResponseSuccess(response);
                return true;
            } else {
                handleVarsOrTemplatesResponseError(response, "CustomTemplates");
                return false;
            }
        } catch (Exception e) {
            logger.debug(config.getAccountId(), "An exception occurred while defining templates.", e);
            return false;
        }
    }

    private void applyQueueHeaderListeners(JSONObject queueHeader, EndpointId endpointId, boolean isProfile) {
        if (queueHeader != null) {
            for (NetworkHeadersListener listener : mNetworkHeadersListeners) {
                final JSONObject headersToAttach = listener.onAttachHeaders(endpointId, EventType.Companion.fromBoolean(isProfile));
                if (headersToAttach != null) {
                    CTXtensions.copyFrom(queueHeader, headersToAttach);
                }
            }
        }
    }

    @WorkerThread
    private Response callApiForEventGroup(EventGroup eventGroup, SendQueueRequestBody body) {
        if (eventGroup == EventGroup.VARIABLES) {
            return ctApiWrapper.getCtApi().defineVars(body);
        } else {
            return ctApiWrapper.getCtApi().sendQueue(eventGroup == EventGroup.PUSH_NOTIFICATION_VIEWED, body);
        }
    }

    private boolean handleVariablesResponse(@NonNull Response response) {
        if (response.isSuccess()) {
            String bodyString = response.readBody();
            JSONObject bodyJson = CTXtensions.toJsonOrNull(bodyString);

            logger.verbose(config.getAccountId(), "Processing variables response : " + bodyJson);

            new ARPResponse(config, this, validator, controllerManager)
                    .processResponse(bodyJson, bodyString, this.context);
            return true;
        } else {
            handleVarsOrTemplatesResponseError(response, "Variables");
            return false;
        }
    }

    private void handleVarsOrTemplatesResponseError(Response response, String logTag) {
        switch (response.getCode()) {
            case 400:
                JSONObject errorStreamJson = CTXtensions.toJsonOrNull(response.readBody());
                if (errorStreamJson != null && !TextUtils.isEmpty(errorStreamJson.optString("error"))) {
                    String errorMessage = errorStreamJson.optString("error");
                    logger.info(logTag, "Error while syncing: " + errorMessage);
                } else {
                    logger.info(logTag, "Error while syncing.");
                }
                return;
            case 401:
                logger.info(logTag, "Unauthorized access from a non-test profile. "
                        + "Please mark this profile as a test profile from the CleverTap dashboard.");
                return;
            default:
                logger.info(logTag, "Response code " + response.getCode() + " while syncing.");
        }
    }

    private void handleTemplateResponseSuccess(Response response) {
        logger.info(config.getAccountId(), "Custom templates defined successfully.");
        JSONObject body = CTXtensions.toJsonOrNull(response.readBody());
        if (body != null) {
            String warnings = body.optString("error");
            if (!TextUtils.isEmpty(warnings)) {
                logger.info(config.getAccountId(), "Custom templates warnings: " + warnings);
            }
        }
    }

    @WorkerThread
    private boolean handleSendQueueResponse(@NonNull Response response, SendQueueRequestBody body,
            EndpointId endpointId) {
        if (!response.isSuccess()) {
            handleSendQueueResponseError(response);
            return false;
        }

        String newDomain = response.getHeaderValue(Constants.HEADER_DOMAIN_NAME);

        if (newDomain != null && !newDomain.trim().isEmpty() && hasDomainChanged(newDomain)) {
            setDomain(context, newDomain);
            logger.debug(config.getAccountId(),
                    "The domain has changed to " + newDomain + ". The request will be retried shortly.");
            return false;
        }

        if (body.getQueueHeader() != null) {
            for (NetworkHeadersListener listener : mNetworkHeadersListeners) {
                boolean isProfile = body.getQueue().optJSONObject(0).has("profile");
                listener.onSentHeaders(body.getQueueHeader(), endpointId, EventType.Companion.fromBoolean(isProfile));
            }
        }

        if (!processIncomingHeaders(context, response)) {
            return false;
        }

        logger.debug(config.getAccountId(), "Queue sent successfully");
        setLastRequestTimestamp(getCurrentRequestTimestamp());
        setFirstRequestTimestampIfNeeded(getCurrentRequestTimestamp());

        String bodyString = response.readBody();
        JSONObject bodyJson = CTXtensions.toJsonOrNull(bodyString);
        logger.verbose(config.getAccountId(), "Processing response : " + bodyJson);

        boolean isFullResponse = doesBodyContainAppLaunchedOrFetchEvents(body);
        for (CleverTapResponse processor : cleverTapResponses) {
            processor.isFullResponse = isFullResponse;
            processor.processResponse(bodyJson, bodyString, context);
        }

        return true;
    }

    private void handleSendQueueResponseError(@NonNull Response response) {
        logger.info("Received error response code: " + response.getCode());
    }

    private boolean doesBodyContainAppLaunchedOrFetchEvents(SendQueueRequestBody body) {
        // check if there is app launched/wzrk_fetch event
        for (int index = 0; index < body.getQueue().length(); index++) {
            try {
                JSONObject event = body.getQueue().getJSONObject(index);
                final String eventType = event.getString("type");
                if ("event".equals(eventType)) {
                    final String evtName = event.getString("evtName");
                    if (Constants.APP_LAUNCHED_EVENT.equals(evtName) || Constants.WZRK_FETCH.equals(evtName)) {
                        return true;
                    }
                }
            } catch (JSONException jsonException) {
                //skip
            }
        }
        return false;
    }

    private void notifyListenersForPushImpressionSentToServer(final JSONArray queue) throws JSONException {

         /* verify whether there is a listener assigned to the push ID for monitoring the 'push impression'
         event.
         */
        for (int i = 0; i < queue.length(); i++) {
            try {
                JSONObject notif = queue.getJSONObject(i).optJSONObject("evtData");
                if (notif != null) {
                    String pushId = notif.optString(Constants.WZRK_PUSH_ID);
                    String pushAccountId = notif.optString(Constants.WZRK_ACCT_ID_KEY);

                    notifyListenerForPushImpressionSentToServer(PushNotificationUtil.
                            buildPushNotificationRenderedListenerKey(pushAccountId,
                                    pushId));

                }
            } catch (JSONException e) {
                logger.verbose(config.getAccountId(),
                        "Encountered an exception while parsing the push notification viewed event queue");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        logger.verbose(config.getAccountId(),
                "push notification viewed event sent successfully");
    }

    private void notifyListenerForPushImpressionSentToServer(@NonNull String listenerKey) {
        NotificationRenderedListener notificationRenderedListener
                = CleverTapAPI.getNotificationRenderedListener(listenerKey);

        if (notificationRenderedListener != null) {
            logger.verbose(config.getAccountId(),
                    "notifying listener " + listenerKey + ", that push impression sent successfully");
            notificationRenderedListener.onNotificationRendered(true);
        }
    }

    @WorkerThread
    private void setDomain(
            final Context context,
            String domainName
    ) {
        logger.verbose(config.getAccountId(), "Setting domain to " + domainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_DOMAIN_NAME), domainName);
        ctApiWrapper.getCtApi().setCachedDomain(domainName);

        if (callbackManager.getSCDomainListener() != null) {
            if (domainName != null) {
                callbackManager.getSCDomainListener().onSCDomainAvailable(getSCDomain(domainName));
            } else {
                callbackManager.getSCDomainListener().onSCDomainUnavailable();
            }
        }
    }

    private void setFirstRequestTimestampIfNeeded(int ts) {
        if (getFirstRequestTimestamp() > 0) {
            return;
        }
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_FIRST_TS), ts);
    }

    @WorkerThread
    private void setSpikyDomain(final Context context, String spikyDomainName) {
        logger.verbose(config.getAccountId(), "Setting spiky domain to " + spikyDomainName);
        StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, Constants.SPIKY_KEY_DOMAIN_NAME), spikyDomainName);
        ctApiWrapper.getCtApi().setCachedSpikyDomain(spikyDomainName);
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
                    "Fetched ARP for namespace key: " + nameSpaceKey + " values: " + all);
            return ret;
        } catch (Exception e) {
            logger.verbose(config.getAccountId(), "Failed to construct ARP object", e);
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
        logger.verbose(config.getAccountId(), "Completed ARP update for namespace key: " + newKey);
        StorageHelper.persist(editor);
        oldPrefs.edit().clear().apply();
        return newPrefs;
    }

    @WorkerThread
    private void setMuted(final Context context, boolean mute) {
        if (mute) {
            final int now = (int) (System.currentTimeMillis() / 1000);
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_MUTED), now);
            setDomain(context, null);

            // Clear all the queues
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
            task.execute("CommsManager#setMuted", () -> {
                databaseManager.clearQueues(context);
                return null;
            });
        } else {
            StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(config, Constants.KEY_MUTED), 0);
        }
    }
}
