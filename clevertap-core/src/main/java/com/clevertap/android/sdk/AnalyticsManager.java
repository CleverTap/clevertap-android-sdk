package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.AnalyticsManagerBundler.wzrkBundleToJson;
import static com.clevertap.android.sdk.utils.CTJsonConverter.getWzrkFields;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.events.BaseEventQueueManager;
import com.clevertap.android.sdk.events.FlattenedEventData;
import com.clevertap.android.sdk.inapp.CTInAppNotification;
import com.clevertap.android.sdk.inapp.InAppPreviewHandler;
import com.clevertap.android.sdk.inbox.CTInboxMessage;
import com.clevertap.android.sdk.profile.ProfileCommand;
import com.clevertap.android.sdk.profile.traversal.ProfileOperation;
import com.clevertap.android.sdk.profile.traversal.ProfileChange;
import com.clevertap.android.sdk.response.CleverTapResponse;
import com.clevertap.android.sdk.response.DisplayUnitResponse;
import com.clevertap.android.sdk.response.InboxResponse;
import com.clevertap.android.sdk.task.CTExecutors;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.CTJsonConverter;
import com.clevertap.android.sdk.utils.Clock;
import com.clevertap.android.sdk.utils.JsonFlattener;
import com.clevertap.android.sdk.utils.UriHelper;
import com.clevertap.android.sdk.validation.pipeline.ValidationPipelineProvider;
import com.clevertap.android.sdk.validation.pipeline.EventDataValidationResult;
import com.clevertap.android.sdk.validation.pipeline.EventNameValidationResult;
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyValidationResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnalyticsManager extends BaseAnalyticsManager {

    private final CTLockManager ctLockManager;
    private final HashMap<String, Integer> installReferrerMap = new HashMap<>(8);
    private final BaseEventQueueManager baseEventQueueManager;
    private final BaseCallbackManager callbackManager;
    private final CleverTapInstanceConfig config;
    private final Context context;
    private final ControllerManager controllerManager;
    private final CoreMetaData coreMetaData;
    private final DeviceInfo deviceInfo;
    private final ValidationPipelineProvider validationPipelineProvider;
    private final Clock currentTimeProvider;
    private final CTExecutors executors;
    private final Object notificationMapLock = new Object();
    private final LocalDataStore localDataStore;
    private final InAppPreviewHandler inAppPreviewHandler;

    private final HashMap<String, Long> notificationIdTagMap = new HashMap<>();
    private final HashMap<String, Long> notificationViewedIdTagMap = new HashMap<>();

    AnalyticsManager(
            Context context,
            CleverTapInstanceConfig config,
            BaseEventQueueManager baseEventQueueManager,
            ValidationPipelineProvider validationPipelineProvider,
            CoreMetaData coreMetaData,
            DeviceInfo deviceInfo,
            BaseCallbackManager callbackManager, ControllerManager controllerManager,
            final CTLockManager ctLockManager,
            Clock currentTimeProvider,
            CTExecutors executors,
            LocalDataStore localDataStore,
            InAppPreviewHandler inAppPreviewHandler
    ) {
        this.context = context;
        this.config = config;
        this.baseEventQueueManager = baseEventQueueManager;
        this.validationPipelineProvider = validationPipelineProvider;
        this.coreMetaData = coreMetaData;
        this.deviceInfo = deviceInfo;
        this.callbackManager = callbackManager;
        this.ctLockManager = ctLockManager;
        this.controllerManager = controllerManager;
        this.currentTimeProvider = currentTimeProvider;
        this.executors = executors;
        this.localDataStore = localDataStore;
        this.inAppPreviewHandler = inAppPreviewHandler;
    }

    @Override
    public void addMultiValuesForKey(final String key, final ArrayList<String> values) {
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("addMultiValuesForKey", () -> {
            _handleMultiValues(values, key, ProfileCommand.ADD);
            return null;
        });
    }

    @Override
    public void incrementValue(String key, Number value) {
        _constructIncrementDecrementValues(value,key, ProfileCommand.INCREMENT);
    }

    @Override
    public void decrementValue(String key, Number value) {
        _constructIncrementDecrementValues(value, key, ProfileCommand.DECREMENT);
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method manually
     */
    @Override
    public void fetchFeatureFlags() {
        if (config.isAnalyticsOnly()) {
            return;
        }
        JSONObject event = new JSONObject();
        JSONObject notif = new JSONObject();
        try {
            notif.put("t", Constants.FETCH_TYPE_FF);
            event.put("evtName", Constants.WZRK_FETCH);
            event.put("evtData", notif);
        } catch (JSONException e) {
            // should not happen
        }
        sendFetchEvent(event);
    }

    //Event
    @Override
    public void forcePushAppLaunchedEvent() {
        coreMetaData.setAppLaunchPushed(false);
        pushAppLaunchedEvent();
    }

    @Override
    public void pushAppLaunchedEvent() {
        //Will not run for Apps which disable App Launched event
        if (config.isDisableAppLaunchedEvent()) {
            coreMetaData.setAppLaunchPushed(true);
            config.getLogger()
                    .debug(config.getAccountId(), "App Launched Events disabled in the Android Manifest file");
            return;
        }
        if (coreMetaData.isAppLaunchPushed()) {
            config.getLogger()
                    .verbose(config.getAccountId(), "App Launched has already been triggered. Will not trigger it ");
            return;
        } else {
            config.getLogger().verbose(config.getAccountId(), "Firing App Launched event");
        }
        coreMetaData.setAppLaunchPushed(true);
        JSONObject event = new JSONObject();
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT);

            event.put("evtData", deviceInfo.getAppLaunchedFields());
        } catch (Throwable t) {
            // We won't get here
        }

        // Flattened data is not needed since AppLaunched fields are attached separately before inapp evaluation. AppLaunched Fields never have nesting
        baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT);
    }

    @Override
    public void pushDefineVarsEvent(JSONObject data){
        baseEventQueueManager.queueEvent(context, data, Constants.DEFINE_VARS_EVENT);
    }

    @Override
    public void pushDisplayUnitClickedEventForID(String unitID) {
        JSONObject event = new JSONObject();

        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);

            //wzrk fields
            if (controllerManager.getCTDisplayUnitController() != null) {
                CleverTapDisplayUnit displayUnit = controllerManager.getCTDisplayUnitController()
                        .getDisplayUnitForID(unitID);
                if (displayUnit != null) {
                    JSONObject eventExtraData = displayUnit.getWZRKFields();
                    if (eventExtraData != null) {
                        event.put("evtData", eventExtraData);
                        try {
                            coreMetaData.setWzrkParams(eventExtraData);
                        } catch (Throwable t) {
                            // no-op
                        }
                        baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(eventExtraData));
                    }
                }
            }

        } catch (Throwable t) {
            // We won't get here
            config.getLogger().verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit clicked event" + t);
        }
    }

    @Override
    public void pushDisplayUnitViewedEventForID(String unitID) {
        JSONObject event = new JSONObject();

        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);

            //wzrk fields
            if (controllerManager.getCTDisplayUnitController() != null) {
                CleverTapDisplayUnit displayUnit = controllerManager.getCTDisplayUnitController()
                        .getDisplayUnitForID(unitID);
                if (displayUnit != null) {
                    JSONObject eventExtras = displayUnit.getWZRKFields();
                    if (eventExtras != null) {
                        event.put("evtData", eventExtras);
                        baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(eventExtras));
                    }
                }
            }
        } catch (Throwable t) {
            // We won't get here
            config.getLogger().verbose(config.getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit viewed event" + t);
        }
    }

    @Override
    @SuppressWarnings({"unused"})
    public void pushError(final String errorMessage, final int errorCode) {
        final HashMap<String, Object> props = new HashMap<>();
        props.put("Error Message", errorMessage);
        props.put("Error Code", errorCode);

        try {
            final String activityName = CoreMetaData.getCurrentActivityName();
            if (activityName != null) {
                props.put("Location", activityName);
            } else {
                props.put("Location", "Unknown");
            }
        } catch (Throwable t) {
            // Ignore
            props.put("Location", "Unknown");
        }

        pushEvent("Error Occurred", props);
    }

    @Override
    public void pushEvent(String eventName, Map<String, Object> eventActions) {
        JSONObject event = new JSONObject();
        try {
            // Validate
            EventNameValidationResult nameValidationResult = validationPipelineProvider.getEventNamePipeline().execute(eventName);

            // Check for an error
            if (nameValidationResult.shouldDrop()) {
                return;
            }

            EventDataValidationResult dataValidationResult = validationPipelineProvider.getEventDataPipeline().execute(eventActions);

            if (dataValidationResult.shouldDrop()) {
                return;
            }

            JSONObject eventData = dataValidationResult.getCleanedData();
            FlattenedEventData flattenedData = getFlattenedEventProperties(eventData);
            event.put("evtName", nameValidationResult.getCleanedName());
            event.put("evtData", eventData);

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, flattenedData);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    /**
     * Raises the Notification Clicked event, if {@param clicked} is true,
     * otherwise the Notification Viewed event, if {@param clicked} is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    @Override
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushInAppNotificationStateEvent(boolean clicked, CTInAppNotification data, Bundle customData) {
        JSONObject event = new JSONObject();
        try {
            JSONObject notif = getWzrkFields(data);

            if (customData != null) {
                for (String x : customData.keySet()) {

                    Object value = customData.get(x);
                    if (value != null) {
                        notif.put(x, value);
                    }
                }
            }

            if (clicked) {
                try {
                    coreMetaData.setWzrkParams(notif);
                } catch (Throwable t) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            }

            event.put("evtData", notif);
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif));
        } catch (Throwable ignored) {
            // We won't get here
        }
    }

    @Override
    public void pushInstallReferrer(String url) {
        try {
            config.getLogger().verbose(config.getAccountId(), "Referrer received: " + url);

            if (url == null) {
                return;
            }
            int now = (int) (System.currentTimeMillis() / 1000);

            //noinspection Constant Conditions
            if (installReferrerMap.containsKey(url) && now - installReferrerMap.get(url) < 10) {
                config.getLogger()
                        .verbose(config.getAccountId(),
                                "Skipping install referrer due to duplicate within 10 seconds");
                return;
            }

            installReferrerMap.put(url, now);

            Uri uri = Uri.parse("wzrk://track?install=true&" + url);

            pushDeepLink(uri, true);
        } catch (Throwable t) {
            // no-op
        }
    }

    @Override
    public synchronized void pushInstallReferrer(String source, String medium, String campaign) {
        if (source == null && medium == null && campaign == null) {
            return;
        }
        try {
            // If already pushed, don't send it again
            int status = StorageHelper.getInt(context, "app_install_status", 0);
            if (status != 0) {
                Logger.d("Install referrer has already been set. Will not override it");
                return;
            }
            StorageHelper.putInt(context, "app_install_status", 1);

            if (source != null) {
                source = Uri.encode(source);
            }
            if (medium != null) {
                medium = Uri.encode(medium);
            }
            if (campaign != null) {
                campaign = Uri.encode(campaign);
            }

            String uriStr = "wzrk://track?install=true";
            if (source != null) {
                uriStr += "&utm_source=" + source;
            }
            if (medium != null) {
                uriStr += "&utm_medium=" + medium;
            }
            if (campaign != null) {
                uriStr += "&utm_campaign=" + campaign;
            }

            Uri uri = Uri.parse(uriStr);
            pushDeepLink(uri, true);
        } catch (Throwable t) {
            Logger.v("Failed to push install referrer", t);
        }
    }

    @Override
    public void pushNotificationClickedEvent(final Bundle extras) {

        if (config.isAnalyticsOnly()) {
            config.getLogger()
                    .debug(config.getAccountId(),
                            "is Analytics Only - will not process Notification Clicked event.");
            return;
        }

        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.getLogger().debug(config.getAccountId(),
                    "Push notification not from CleverTap - will not process Notification Clicked event.");
            return;
        }

        String accountId = null;
        try {
            accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        } catch (Throwable t) {
            // no-op
        }

        boolean shouldProcess = (accountId == null && config.isDefaultInstance())
                || config.getAccountId().equals(accountId);

        if (!shouldProcess) {
            config.getLogger().debug(config.getAccountId(),
                    "Push notification not targeted at this instance, not processing Notification Clicked Event");
            return;
        }

        if (extras.containsKey(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_TYPE_KEY)) {
            inAppPreviewHandler.handleInAppPreview(extras);
            return;
        }

        if (extras.containsKey(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY)) {
            handleInboxPreview(extras);
            return;
        }

        if (extras.containsKey(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY)) {
            handleSendTestForDisplayUnits(extras);
            return;
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            config.getLogger().debug(config.getAccountId(),
                    "Push notification ID Tag is null, not processing Notification Clicked event for:  " + extras);
            return;
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (5 secs) don't process
        boolean isDuplicate = checkDuplicateNotificationIds(
                dedupeCheckKey(extras),
                notificationIdTagMap,
                Constants.NOTIFICATION_ID_TAG_INTERVAL
        );
        if (isDuplicate) {
            config.getLogger().debug(config.getAccountId(),
                    "Already processed Notification Clicked event for " + extras
                            + ", dropping duplicate.");
            return;
        }

        try {
            // convert bundle to json
            JSONObject notif = wzrkBundleToJson(extras);
            JSONObject event = AnalyticsManagerBundler.notificationClickedJson(notif);

            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif));
            coreMetaData.setWzrkParams(notif);
        } catch (Throwable t) {
            // We won't get here
        }
        if (callbackManager.getPushNotificationListener() != null) {
            callbackManager.getPushNotificationListener()
                    .onNotificationClickedPayloadReceived(Utils.convertBundleObjectToHashMap(extras));
        } else {
            Logger.d("CTPushNotificationListener is not set");
        }
    }

    private void handleInboxPreview(Bundle extras) {
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("testInboxNotification", () -> {
            try {
                Logger.v("Received inbox via push payload: " + extras
                        .getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY));
                JSONObject r = new JSONObject();
                JSONArray inboxNotifs = new JSONArray();
                r.put(Constants.INBOX_JSON_RESPONSE_KEY, inboxNotifs);
                JSONObject testPushObject = new JSONObject(
                        extras.getString(Constants.INBOX_PREVIEW_PUSH_PAYLOAD_KEY));
                testPushObject.put("_id", String.valueOf(System.currentTimeMillis() / 1000));
                inboxNotifs.put(testPushObject);

                CleverTapResponse cleverTapResponse = new InboxResponse(config, ctLockManager, callbackManager, controllerManager);
                cleverTapResponse.processResponse(r, null, context);
            } catch (Throwable t) {
                Logger.v("Failed to process inbox message from push notification payload", t);
            }
            return null;
        });
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    @Override
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushNotificationViewedEvent(Bundle extras) {

        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            config.getLogger().debug(config.getAccountId(),
                    "Push notification: " + (extras == null ? "NULL" : extras.toString())
                            + " not from CleverTap - will not process Notification Viewed event.");
            return;
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG)
                || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            config.getLogger().debug(config.getAccountId(),
                    "Push notification ID Tag is null, not processing Notification Viewed event for:  " + extras);
            return;
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (2 secs) don't process
        boolean isDuplicate = checkDuplicateNotificationIds(
                dedupeCheckKey(extras),
                notificationViewedIdTagMap,
                Constants.NOTIFICATION_VIEWED_ID_TAG_INTERVAL
        );
        if (isDuplicate) {
            config.getLogger().debug(config.getAccountId(),
                    "Already processed Notification Viewed event for " + extras + ", dropping duplicate.");
            return;
        }

        config.getLogger().debug("Recording Notification Viewed event for notification:  " + extras);

        try {
            JSONObject notif = wzrkBundleToJson(extras);
            JSONObject event = AnalyticsManagerBundler.notificationViewedJson(notif);
            baseEventQueueManager.queueEvent(context, event, Constants.NV_EVENT, getFlattenedEventProperties(notif));

        } catch (JSONException e) {
            config.getLogger().debug("Failed to recording Notification Viewed " + e);
        }
    }

    @Override
    public void pushProfile(final Map<String, Object> profile) {
        if (profile == null || profile.isEmpty() || deviceInfo.getDeviceID() == null) {
            return;
        }
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("profilePush", () -> {
            _push(profile);
            return null;
        });
    }

    @Override
    public void removeMultiValuesForKey(final String key, final ArrayList<String> values) {
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("removeMultiValuesForKey", () -> {
            _handleMultiValues(values, key, ProfileCommand.REMOVE);
            return null;
        });
    }

    @Override
    public void removeValueForKey(final String key) {
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("removeValueForKey", () -> {
            _removeValueForKey(key);
            return null;
        });
    }

    @Override
    public void sendDataEvent(final JSONObject event) {
        baseEventQueueManager.queueEvent(context, event, Constants.DATA_EVENT);
    }

    void pushChargedEvent(HashMap<String, Object> chargeDetails, ArrayList<HashMap<String, Object>> items) {

        if (chargeDetails == null || items == null) {
            config.getLogger().debug(config.getAccountId(), "Invalid Charged event: details and or items is null");
            return;
        }

        validationPipelineProvider.getChargedEventItemsValidationPipeline().execute(items);

        JSONObject chargedEvent = new JSONObject();
        try {
            // Validate charged event details
            EventDataValidationResult detailsResult = validationPipelineProvider.getEventDataPipeline().execute(chargeDetails);

            if (detailsResult.shouldDrop()) {
                return;
            }

            JSONObject evtData = detailsResult.getCleanedData();

            // Validate each item
            JSONArray jsonItemsArray = new JSONArray();
            for (HashMap<String, Object> map : items) {
                EventDataValidationResult itemResult = validationPipelineProvider.getEventDataPipeline().execute(map);

                if (!itemResult.shouldDrop()) {
                    JSONObject itemDetails = itemResult.getCleanedData();
                    jsonItemsArray.put(itemDetails);
                }
            }
            evtData.put("Items", jsonItemsArray);

            chargedEvent.put("evtName", Constants.CHARGED_EVENT);
            chargedEvent.put("evtData", evtData);

            FlattenedEventData flattenedData = getFlattenedEventProperties(evtData);
            baseEventQueueManager.queueEvent(context, chargedEvent, Constants.RAISED_EVENT, flattenedData);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    synchronized void pushDeepLink(Uri uri, boolean install) {
        if (uri == null) {
            return;
        }

        try {
            JSONObject referrer = UriHelper.getUrchinFromUri(uri);
            if (referrer.has("us")) {
                coreMetaData.setSource(referrer.get("us").toString());
            }
            if (referrer.has("um")) {
                coreMetaData.setMedium(referrer.get("um").toString());
            }
            if (referrer.has("uc")) {
                coreMetaData.setCampaign(referrer.get("uc").toString());
            }

            referrer.put("referrer", uri.toString());
            if (install) {
                referrer.put("install", true);
            }
            recordPageEventWithExtras(referrer);

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Failed to push deep link", t);
        }
    }

    @Deprecated
    Future<?> raiseEventForSignedCall(String eventName, JSONObject dcEventProperties) {

        Future<?> future = null;

        JSONObject event = new JSONObject();
        try {
            event.put("evtName", eventName);
            event.put("evtData", dcEventProperties);

            future = baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (JSONException e) {
            config.getLogger().debug(config.getAccountId(), Constants.LOG_TAG_SIGNED_CALL +
                    "JSON Exception when raising Signed Call event "
                    + eventName + " - " + e.getLocalizedMessage());
        }

        return future;
    }

    Future<?> raiseEventForGeofences(String eventName, JSONObject geofenceProperties) {

        Future<?> future = null;

        JSONObject event = new JSONObject();
        try {
            event.put("evtName", eventName);
            event.put("evtData", geofenceProperties);

            Location location = new Location("");
            location.setLatitude(geofenceProperties.getDouble("triggered_lat"));
            location.setLongitude(geofenceProperties.getDouble("triggered_lng"));

            geofenceProperties.remove("triggered_lat");
            geofenceProperties.remove("triggered_lng");

            coreMetaData.setLocationFromUser(location);

            future = baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(geofenceProperties));
        } catch (JSONException e) {
            config.getLogger().debug(config.getAccountId(), Constants.LOG_TAG_GEOFENCES +
                    "JSON Exception when raising GeoFence event "
                    + eventName + " - " + e.getLocalizedMessage());
        }

        return future;
    }

    void recordPageEventWithExtras(JSONObject extras) {
        try {
            JSONObject jsonObject = new JSONObject();
            // Add the extras
            if (extras != null && extras.length() > 0) {
                Iterator keys = extras.keys();
                while (keys.hasNext()) {
                    try {
                        String key = (String) keys.next();
                        jsonObject.put(key, extras.getString(key));
                    } catch (ClassCastException ignore) {
                        // Really won't get here
                    }
                }
            }
            baseEventQueueManager.queueEvent(context, jsonObject, Constants.PAGE_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    void setMultiValuesForKey(final String key, final ArrayList<String> values) {
        Task<Void> task = executors.postAsyncSafelyTask();
        task.execute("setMultiValuesForKey", () -> {
            _handleMultiValues(values, key, ProfileCommand.SET);
            return null;
        });
    }


    private void _handleMultiValues(ArrayList<String> values, String key, ProfileCommand command) {
        if (key == null) {
            return;
        }

        Map<String, ArrayList<String>> eventData = Map.of(key, values);
        EventDataValidationResult eventDataValidationResult = validationPipelineProvider.getMultiValueDataPipeline().execute(eventData);

        if (eventDataValidationResult.shouldDrop()) {
            return;
        }

        try {
            _pushMultiValue(eventDataValidationResult.getCleanedData().getJSONArray(key), key, command);
        } catch (JSONException e) {
            config.getLogger().verbose(config.getAccountId(), "Failed to handle Multi Values for key" + key, e);
        }
    }

    private void _constructIncrementDecrementValues(Number value, String key, ProfileCommand command) {
        if (value == null) {
            return;
        }
        try {
            PropertyKeyValidationResult keyResult = validationPipelineProvider.getPropertyKeyPipeline().execute(key);

            if (keyResult.shouldDrop()) {
                return;
            }

            key = keyResult.getCleanedKey();

            JSONObject profileCommand = new JSONObject().put(command.getCommandString(), value);
            JSONObject profileUpdate = new JSONObject().put(key, profileCommand);

            ProfileOperation operation = command.getOperation();

            baseEventQueueManager.pushBasicProfile(profileUpdate, false, getFlattenedProfileChanges(key, value, operation));
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Failed to update profile value for key " + key, t);
        }
    }

    private void _push(Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return;
        }

        try {
            // Validate profile data
            EventDataValidationResult profileResult = validationPipelineProvider.getEventDataPipeline().execute(profile);

            if (profileResult.shouldDrop()) {
                return;
            }

            JSONObject cleanedProfile = profileResult.getCleanedData();
            config.getLogger()
                    .verbose(config.getAccountId(), "Constructed custom profile: " + cleanedProfile);

            baseEventQueueManager.pushBasicProfile(cleanedProfile, false, getFlattenedProfileChanges(cleanedProfile, ProfileOperation.UPDATE));

        } catch (Throwable t) {
            // Will not happen
            config.getLogger().verbose(config.getAccountId(), "Failed to push profile", t);
        }
    }

    private void _removeValueForKey(String key) {
        try {
            PropertyKeyValidationResult keyValidationResult = validationPipelineProvider.getPropertyKeyPipeline().execute(key);

            if (keyValidationResult.shouldDrop()) {
                return;
            }

            key = keyValidationResult.getCleanedKey();

            // If key contains "Identity" then do not remove from SQLDb and shared prefs
            if (key.toLowerCase().contains("identity")) {
                config.getLogger().verbose(config.getAccountId(),
                    "Cannot remove value for key " + key + " from user profile");
                return;
            }

            // Send the delete command
            ProfileCommand command = ProfileCommand.DELETE;
            JSONObject profileCommand = new JSONObject().put(command.getCommandString(), true);
            JSONObject profileUpdate = new JSONObject().put(key, profileCommand);

            // Set removeFromSharedPrefs to true to remove PII keys from shared prefs
            baseEventQueueManager.pushBasicProfile(profileUpdate, true,
                getFlattenedProfileChanges(key, Constants.DELETE_MARKER, command.getOperation()));

            config.getLogger().verbose(config.getAccountId(),
                "removing value for key " + key + " from user profile");

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(),
                "Failed to remove profile value for key " + key, t);
        }
    }

    private void _pushMultiValue(JSONArray originalValues, String key, ProfileCommand command) {
        try {
            JSONObject profileCommand = new JSONObject();
            profileCommand.put(command.getCommandString(), originalValues);

            JSONObject profileUpdate = new JSONObject();
            profileUpdate.put(key, profileCommand);

            ProfileOperation operation = command.getOperation();
            baseEventQueueManager.pushBasicProfile(profileUpdate, false,
                getFlattenedProfileChanges(key, originalValues, operation));

            config.getLogger().verbose(config.getAccountId(),
                "Constructed multi-value profile push: " + profileUpdate);

        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(),
                "Error pushing multiValue for key " + key, t);
        }
    }

    String dedupeCheckKey(Bundle extras) {
        // This flag is used so that we can release in phased manner, eventually the check has to go away.
        Object doDedupeCheck = extras.get(Constants.WZRK_DEDUPE);

        boolean check = false;
        if (doDedupeCheck != null) {
            if (doDedupeCheck instanceof String) {
                check = "true".equalsIgnoreCase((String) doDedupeCheck);
            }
            if (doDedupeCheck instanceof Boolean) {
                check = (Boolean) doDedupeCheck;
            }
        }

        String notificationIdTag;
        if (check) {
            notificationIdTag = extras.getString(Constants.WZRK_PUSH_ID);
        } else {
            notificationIdTag = extras.getString(Constants.NOTIFICATION_ID_TAG);
        }
        return notificationIdTag;
    }

    private boolean checkDuplicateNotificationIds(
            String notificationIdTag,
            HashMap<String, Long> notificationTagMap,
            int interval
    ) {
        synchronized (notificationMapLock) {
            // default to false; only return true if we are sure we've seen this one before
            boolean isDupe = false;
            try {
                long now = currentTimeProvider.currentTimeMillis();
                if (notificationTagMap.containsKey(notificationIdTag)) {
                    long timestamp;
                    // noinspection ConstantConditions
                    timestamp = notificationTagMap.get(notificationIdTag);
                    // same notificationId within time internal treat as dupe
                    if (now - timestamp < interval) {
                        isDupe = true;
                    }
                }
                notificationTagMap.put(notificationIdTag, now);
            } catch (Throwable ignored) {
                // no-op
            }
            return isDupe;
        }
    }

    public void sendPingEvent(final JSONObject eventObject) {
        baseEventQueueManager
                .queueEvent(context, eventObject, Constants.PING_EVENT);
    }

    public void sendFetchEvent(final JSONObject eventObject) {
        baseEventQueueManager
                .queueEvent(context, eventObject, Constants.FETCH_EVENT);
    }

    /**
     * This method handles send Test flow for Display Units
     *
     * @param extras - bundled data of notification payload
     */
    private void handleSendTestForDisplayUnits(Bundle extras) {
        try {
            JSONObject r = CTJsonConverter.displayUnitFromExtras(extras);

            CleverTapResponse cleverTapResponse = new DisplayUnitResponse(config, callbackManager, controllerManager);
            cleverTapResponse.processResponse(r, null, context);

        } catch (Throwable t) {
            Logger.v("Failed to process Display Unit from push notification payload", t);
        }
    }

    /**
     * Raises the Notification Clicked event, if {@param clicked} is true,
     * otherwise the Notification Viewed event, if {@param clicked} is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    void pushInboxMessageStateEvent(boolean clicked, CTInboxMessage data, Bundle customData) {
        JSONObject event = new JSONObject();
        try {
            JSONObject notif = getWzrkFields(data);

            if (customData != null) {
                for (String x : customData.keySet()) {

                    Object value = customData.get(x);
                    if (value != null) {
                        notif.put(x, value);
                    }
                }
            }

            if (clicked) {
                try {
                    coreMetaData.setWzrkParams(notif);
                } catch (Throwable t) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            }

            event.put("evtData", notif);
            baseEventQueueManager.queueEvent(context, event, Constants.RAISED_EVENT, getFlattenedEventProperties(notif));
        } catch (Throwable ignored) {
            // We won't get here
        }
    }

    private FlattenedEventData getFlattenedEventProperties(JSONObject properties) {
        return new FlattenedEventData.EventProperties(JsonFlattener.flatten(properties));
    }

    private FlattenedEventData.ProfileChanges getFlattenedProfileChanges(String key, Object originalValues, ProfileOperation operation) throws JSONException {
        Map<String, ProfileChange> profileChanges = localDataStore.processProfileTree(key, originalValues, operation);
        return new FlattenedEventData.ProfileChanges(profileChanges);
    }

    private FlattenedEventData.ProfileChanges getFlattenedProfileChanges(JSONObject originalValues, ProfileOperation operation) throws JSONException {
        Map<String, ProfileChange> profileChanges = localDataStore.processProfileTree(originalValues, operation);
        return new FlattenedEventData.ProfileChanges(profileChanges);
    }
}