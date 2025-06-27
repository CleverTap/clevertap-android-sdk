package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.inapp.CTLocalInApp.FALLBACK_TO_NOTIFICATION_SETTINGS;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.InAppNotificationActivity;
import com.clevertap.android.sdk.InAppNotificationListener;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter;
import com.clevertap.android.sdk.inapp.images.FileResourceProvider;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.task.CTExecutors;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.Clock;
import com.clevertap.android.sdk.utils.JsonUtilsKt;
import com.clevertap.android.sdk.variables.JsonUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;

@RestrictTo(Scope.LIBRARY_GROUP)
public class InAppController implements InAppListener {

    private enum InAppState {
        DISCARDED,
        SUSPENDED,
        RESUMED
    }

    private static CTInAppNotification currentlyDisplayingInApp = null;

    @VisibleForTesting
    static CTInAppNotification getCurrentlyDisplayingInApp() {
        return currentlyDisplayingInApp;
    }

    @VisibleForTesting
    static void clearCurrentlyDisplayingInApp() {
        currentlyDisplayingInApp = null;
    }

    private static final List<CTInAppNotification> pendingNotifications = Collections
            .synchronizedList(new ArrayList<>());

    private final AnalyticsManager analyticsManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ControllerManager controllerManager;

    private final CoreMetaData coreMetaData;

    private final DeviceInfo deviceInfo;

    private final EvaluationManager evaluationManager;

    private final TemplatesManager templatesManager;

    private InAppState inAppState;

    private final Set<String> inAppExcludedActivityNames;

    private final Logger logger;

    private final String defaultLogTag;

    private final MainLooperHandler mainLooperHandler;

    private final CTExecutors executors;

    private final InAppQueue inAppQueue;

    public final Function0<Unit> onAppLaunchEventSent;

    private final InAppActionHandler inAppActionHandler;

    private final InAppNotificationInflater inAppNotificationInflater;

    private final Clock clock;

    public final static String LOCAL_INAPP_COUNT = "local_in_app_count";

    public final static String IS_FIRST_TIME_PERMISSION_REQUEST = "firstTimeRequest";

    public InAppController(
            Context context,
            CleverTapInstanceConfig config,
            MainLooperHandler mainLooperHandler,
            CTExecutors executors,
            ControllerManager controllerManager,
            BaseCallbackManager callbackManager,
            AnalyticsManager analyticsManager,
            CoreMetaData coreMetaData,
            ManifestInfo manifestInfo,
            final DeviceInfo deviceInfo,
            InAppQueue inAppQueue,
            final EvaluationManager evaluationManager,
            TemplatesManager templatesManager,
            final InAppActionHandler inAppActionHandler,
            final InAppNotificationInflater inAppNotificationInflater,
            final Clock clock) {
        this.context = context;
        this.config = config;
        this.logger = config.getLogger();
        this.defaultLogTag = config.getAccountId();
        this.mainLooperHandler = mainLooperHandler;
        this.executors = executors;
        this.controllerManager = controllerManager;
        this.callbackManager = callbackManager;
        this.analyticsManager = analyticsManager;
        this.coreMetaData = coreMetaData;
        this.inAppState = InAppState.RESUMED;
        this.deviceInfo = deviceInfo;
        this.inAppQueue = inAppQueue;
        this.evaluationManager = evaluationManager;
        this.templatesManager = templatesManager;
        this.onAppLaunchEventSent = () -> {
            final Map<String, Object> appLaunchedProperties = JsonUtil.mapFromJson(
                    deviceInfo.getAppLaunchedFields());
            final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnAppLaunchedClientSide(
                    appLaunchedProperties, coreMetaData.getLocationFromUser());
            if (clientSideInAppsToDisplay.length() > 0) {
                addInAppNotificationsToQueue(clientSideInAppsToDisplay);
            }
            return null;
        };
        this.inAppActionHandler = inAppActionHandler;
        this.inAppNotificationInflater = inAppNotificationInflater;
        this.inAppExcludedActivityNames = getExcludedActivitiesSet(manifestInfo);
        this.clock = clock;
    }

    public void promptPushPrimer(final JSONObject jsonObject) {
        try {
            jsonObject.put(Constants.KEY_REQUEST_FOR_NOTIFICATION_PERMISSION, true);
        } catch (JSONException e) {
            // should not happen, nothing to do
        }
        boolean fallbackToSettings = jsonObject.optBoolean(FALLBACK_TO_NOTIFICATION_SETTINGS, false);
        // always show the primer when fallback to settings is enabled
        inAppActionHandler.launchPushPermissionPrompt(
                fallbackToSettings,
                fallbackToSettings, //alwaysRequestIfNotGranted
                activity -> prepareNotificationForDisplay(jsonObject));

    }

    public void promptPermission(boolean showFallbackSettings) {
        inAppActionHandler.launchPushPermissionPrompt(showFallbackSettings);
    }

    public boolean isPushPermissionGranted() {
        return inAppActionHandler.arePushNotificationsEnabled();
    }

    @Override
    @NonNull
    public Bundle inAppNotificationActionTriggered(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppAction action,
            @NonNull final String callToAction,
            @Nullable final Bundle additionalData,
            @Nullable final Context activityContext) {
        Bundle data;
        if (additionalData != null) {
            data = new Bundle(additionalData);
        } else {
            data = new Bundle();
        }
        data.putString(Constants.NOTIFICATION_ID_TAG, inAppNotification.getCampaignId());
        data.putString(Constants.KEY_C2A, callToAction);

        // send clicked event
        analyticsManager.pushInAppNotificationStateEvent(true, inAppNotification, data);

        InAppActionType type = action.getType();
        if (type == null) {
            logger.debug("Triggered in-app action without type");
            return data;
        }

        switch (type) {
            case CUSTOM_CODE:
                triggerCustomTemplateAction(inAppNotification, action.getCustomTemplateInAppData());
                break;
            case CLOSE:
                if (CTInAppType.CTInAppTypeCustomCodeTemplate == inAppNotification.getInAppType()) {
                    templatesManager.closeTemplate(inAppNotification);
                }
                // SDK In-Apps are dismissed in CTInAppBaseFragment::handleButtonClick or CTInAppNotificationActivity
                break;
            case OPEN_URL:
                String actionUrl = action.getActionUrl();
                if (actionUrl != null) {
                    inAppActionHandler.openUrl(actionUrl, activityContext);
                } else {
                    logger.debug("Cannot trigger open url action without url value");
                }
                break;
            case KEY_VALUES:
                if (action.getKeyValues() != null && !action.getKeyValues().isEmpty()) {
                    if (callbackManager.getInAppNotificationButtonListener() != null) {
                        callbackManager.getInAppNotificationButtonListener().onInAppButtonClick(action.getKeyValues());
                    }
                }
                break;
        }

        return data;
    }

    @Nullable
    @Override
    public Bundle inAppNotificationDidClick(
            @NonNull final CTInAppNotification inAppNotification,
            @NonNull final CTInAppNotificationButton button,
            @Nullable final Context activityContext) {
        if (button.getAction() == null) {
            return null;
        }
        return inAppNotificationActionTriggered(
                inAppNotification,
                button.getAction(),
                button.getText(),
                null,
                activityContext);
    }

    @Override
    public void inAppNotificationDidDismiss(
            @NonNull final CTInAppNotification inAppNotification,
            @Nullable final Bundle formData) {

        if (controllerManager.getInAppFCManager() != null) {
            String templateName = inAppNotification.getCustomTemplateData() != null
                    ? inAppNotification.getCustomTemplateData().getTemplateName() : "";
            logger.verbose(defaultLogTag,
                    "InApp Dismissed: " + inAppNotification.getCampaignId() + "  " + templateName);
        } else {
            logger.verbose(defaultLogTag, "Not calling InApp Dismissed: " + inAppNotification.getCampaignId()
                    + " because InAppFCManager is null");
        }
        try {
            final InAppNotificationListener listener = callbackManager.getInAppNotificationListener();
            if (listener != null) {
                final HashMap<String, Object> notifKVS;

                if (inAppNotification.getCustomExtras() != null) {
                    notifKVS = Utils.convertJSONObjectToHashMap(inAppNotification.getCustomExtras());
                } else {
                    notifKVS = new HashMap<>();
                }

                Logger.v("Calling the in-app listener on behalf of " + coreMetaData.getSource());

                if (formData != null) {
                    listener.onDismissed(notifKVS, Utils.convertBundleObjectToHashMap(formData));
                } else {
                    listener.onDismissed(notifKVS, null);
                }
            }
        } catch (Throwable t) {
            logger.verbose(defaultLogTag, "Failed to call the in-app notification listener", t);
        }

        // Fire the next one, if any
        Task<Void> task = executors.postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
        task.execute("InappController#inAppNotificationDidDismiss", () -> {
            inAppDidDismiss(inAppNotification);
            _showNotificationIfAvailable();
            return null;
        });
    }

    //InApp
    @Override
    public void inAppNotificationDidShow(@NonNull CTInAppNotification inAppNotification, @Nullable Bundle formData) {
        controllerManager.getInAppFCManager().didShow(context, inAppNotification);
        analyticsManager.pushInAppNotificationStateEvent(false, inAppNotification, formData);

        //Fire onShow() callback when InApp is shown.
        try {
            final InAppNotificationListener listener = callbackManager.getInAppNotificationListener();
            if (listener != null) {
                listener.onShow(inAppNotification);
            }
        } catch (Throwable t) {
            Logger.v(defaultLogTag, "Failed to call the in-app notification listener", t);
        }
    }

    public void discardInApps() {
        this.inAppState = InAppState.DISCARDED;
        logger.verbose(defaultLogTag, "InAppState is DISCARDED");
    }

    public void resumeInApps() {
        this.inAppState = InAppState.RESUMED;
        logger.verbose(defaultLogTag, "InAppState is RESUMED");
        logger.verbose(defaultLogTag, "Resuming InApps by calling showInAppNotificationIfAny()");
        showNotificationIfAvailable();
    }

    public void suspendInApps() {
        this.inAppState = InAppState.SUSPENDED;
        logger.verbose(defaultLogTag, "InAppState is SUSPENDED");
    }

    @WorkerThread
    public void addInAppNotificationsToQueue(JSONArray inappNotifs) {
        try {
            JSONArray filteredNotifs = filterNonRegisteredCustomTemplates(inappNotifs);
            inAppQueue.enqueueAll(filteredNotifs);

            // Fire the first notification, if any
            showNotificationIfAvailable();
        } catch (Exception e) {
            logger.debug(defaultLogTag, "InAppController: : InApp notification handling error: " + e.getMessage());
        }
    }


    @WorkerThread
    public void onQueueEvent(final String eventName, Map<String, Object> eventProperties, Location userLocation) {
        final Map<String, Object> appFieldsWithEventProperties = JsonUtil.mapFromJson(deviceInfo.getAppLaunchedFields());
        appFieldsWithEventProperties.putAll(eventProperties);
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnEvent(eventName, appFieldsWithEventProperties, userLocation);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    @WorkerThread
    public void onQueueChargedEvent(Map<String, Object> chargeDetails, List<Map<String, Object>> items, Location userLocation) {
        final Map<String, Object> appFieldsWithChargedEventProperties = JsonUtil.mapFromJson(deviceInfo.getAppLaunchedFields());
        appFieldsWithChargedEventProperties.putAll(chargeDetails);
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnChargedEvent(appFieldsWithChargedEventProperties, items, userLocation);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    @WorkerThread
    public void onQueueProfileEvent(final Map<String, Map<String, Object>> userAttributeChangedProperties, Location location) {
        final Map<String, Object> appFields = JsonUtil.mapFromJson(deviceInfo.getAppLaunchedFields());
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnUserAttributeChange(userAttributeChangedProperties, location, appFields);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    public void onAppLaunchServerSideInAppsResponse(
            @NonNull JSONArray appLaunchServerSideInApps,
            Location userLocation
    ) throws JSONException {
        final Map<String, Object> appLaunchedProperties = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        List<JSONObject> appLaunchSsInAppList = Utils.toJSONObjectList(appLaunchServerSideInApps);
        final JSONArray serverSideInAppsToDisplay = evaluationManager.evaluateOnAppLaunchedServerSide(
                appLaunchSsInAppList, appLaunchedProperties, userLocation);

        if (serverSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(serverSideInAppsToDisplay);
        }
    }

    public void showNotificationIfAvailable() {
        if (!config.isAnalyticsOnly()) {
            Task<Void> task = executors.postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InappController#showNotificationIfAvailable", () -> {
                _showNotificationIfAvailable();
                return null;
            });
        }
    }

    private void _showNotificationIfAvailable() {
        try {
            if (!canShowInAppOnCurrentActivity()) {
                Logger.v("Not showing notification on blacklisted activity");
                return;
            }

            if (this.inAppState == InAppState.SUSPENDED) {
                logger.debug(defaultLogTag,
                        "InApp Notifications are set to be suspended, not showing the InApp Notification");
                return;
            }

            // see if we have any pending notifications
            if (checkPendingNotifications()) {
                return;
            }

            JSONObject inapp = inAppQueue.dequeue();
            if (inapp == null) {
                return;
            }

            if (this.inAppState != InAppState.DISCARDED) {
                prepareNotificationForDisplay(inapp);
            } else {
                logger.debug(defaultLogTag,
                        "InApp Notifications are set to be discarded, dropping the InApp Notification");
            }
        } catch (Throwable t) {
            // We won't get here
            logger.verbose(defaultLogTag, "InApp: Couldn't parse JSON array string from prefs", t);
        }
    }

    private void addInAppNotificationInFrontOfQueue(JSONObject inApp) {
        if (isNonRegisteredCustomTemplate(inApp)) {
            return;
        }
        inAppQueue.insertInFront(inApp);
        showNotificationIfAvailable();
    }

    private boolean canShowInAppOnActivity(Activity activity) {
        if (activity == null) {
            return true;
        }
        String activityName = activity.getLocalClassName();

        for (String blacklistedActivity : inAppExcludedActivityNames) {
            if (activityName.contains(blacklistedActivity)) {
                return false;
            }
        }

        return true;
    }

    private boolean canShowInAppOnCurrentActivity() {
        return canShowInAppOnActivity(CoreMetaData.getCurrentActivity());
    }

    private void displayNotification(final CTInAppNotification inAppNotification) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainLooperHandler.post(() -> displayNotification(inAppNotification));
            return;
        }

        if (inAppNotification.isRequestForPushPermission() && inAppActionHandler.arePushNotificationsEnabled()) {
            logger.verbose(defaultLogTag,
                    "Not showing push permission request, permission is already granted");
            inAppActionHandler.notifyPushPermissionListeners();
            showNotificationIfAvailable();
            return;
        }

        checkLimitsBeforeShowing(inAppNotification);
        incrementLocalInAppCountInPersistentStore(context, inAppNotification);
    }

    private void notificationReady(final CTInAppNotification inAppNotification) {
        if (inAppNotification.getError() != null) {
            logger.debug(defaultLogTag, "Unable to process inapp notification " + inAppNotification.getError());
            return;
        }
        final CustomTemplateInAppData templateData = inAppNotification.getCustomTemplateData();
        CustomTemplate template = null;
        if (templateData != null && templateData.getTemplateName() != null) {
            template = templatesManager.getTemplate(templateData.getTemplateName());
        }
        logger.debug(defaultLogTag, "Notification ready: " + inAppNotification.getJsonDescription());
        if (template != null && !template.isVisual()) {
            presentTemplate(inAppNotification);
        } else {
            displayNotification(inAppNotification);
        }
    }
    private void prepareNotificationForDisplay(final JSONObject jsonObject) {
        logger.debug(defaultLogTag, "Preparing In-App for display: " + jsonObject);
        inAppNotificationInflater.inflate(jsonObject,
                "InappController#prepareNotificationForDisplay",
                this::notificationReady);
    }

    private Set<String> getExcludedActivitiesSet(ManifestInfo manifestInfo) {
        Set<String> inAppActivityExclude = new HashSet<>();
            try {
                String activities = manifestInfo.getExcludedActivities();
                if (activities != null) {
                    String[] split = activities.split(",");
                    for (String a : split) {
                        inAppActivityExclude.add(a.trim());
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }
        logger.debug(defaultLogTag,
                "In-app notifications will not be shown on " + Arrays.toString(inAppActivityExclude.toArray()));
        return inAppActivityExclude;
    }

    private boolean checkPendingNotifications() {
        Logger.v(defaultLogTag, "checking Pending Notifications");
        if (pendingNotifications.isEmpty()) {
            return false;
        } else {
            final CTInAppNotification notification = pendingNotifications.get(0);
            pendingNotifications.remove(0);
            checkLimitsBeforeShowing(notification);
            return true;
        }
    }

    private void inAppDidDismiss(CTInAppNotification inAppNotification) {
        Logger.v(defaultLogTag, "Running inAppDidDismiss");
        if (currentlyDisplayingInApp != null && (currentlyDisplayingInApp.getCampaignId()
                .equals(inAppNotification.getCampaignId()))) {
            currentlyDisplayingInApp = null;
            checkPendingNotifications();
        }
    }

    private void incrementLocalInAppCountInPersistentStore(Context context, CTInAppNotification inAppNotification) {
        if (inAppNotification.isLocalInApp()) {
            deviceInfo.incrementLocalInAppCount();//update cache
            Task<Void> task = executors.ioTask();
            task.execute("InAppController#incrementLocalInAppCountInPersistentStore", () -> {
                StorageHelper.putIntImmediate(context, LOCAL_INAPP_COUNT,
                        deviceInfo.getLocalInAppCount());// update disk with cache
                return null;
            });
        }
    }

    private void checkLimitsBeforeShowing(final CTInAppNotification inAppNotification) {
        Task<Boolean> task = executors.ioTask();
        task.addOnSuccessListener(canShow -> {
            if(canShow) {
                showInApp(inAppNotification);
            } else {
                showNotificationIfAvailable();
            }
        });
        task.execute("checkLimitsBeforeShowing", () -> {
            if (controllerManager.getInAppFCManager() != null) {
                final Function2<JSONObject, String, Boolean> hasInAppFrequencyLimitsMaxedOut = (inAppJSON, inAppId) -> {
                    final List<LimitAdapter> listOfWhenLimits = InAppResponseAdapter.getListOfWhenLimits(inAppJSON);
                    return !evaluationManager.matchWhenLimitsBeforeDisplay(listOfWhenLimits, inAppId);
                };

                if (!controllerManager.getInAppFCManager().canShow(inAppNotification, hasInAppFrequencyLimitsMaxedOut)) {
                    logger.verbose(defaultLogTag,
                            "InApp has been rejected by FC, not showing " + inAppNotification.getCampaignId());
                    return false;
                }
            } else {
                logger.verbose(defaultLogTag,
                        "getCoreState().getInAppFCManager() is NULL, not showing " + inAppNotification.getCampaignId());
                return false;
            }
            return true;
        });
    }

    private boolean checkBeforeShowApprovalBeforeDisplay(final CTInAppNotification inAppNotification) {
        final InAppNotificationListener listener = callbackManager.getInAppNotificationListener();

        final boolean goFromListener;

        if (listener != null) {
            final HashMap<String, Object> kvs;

            if (inAppNotification.getCustomExtras() != null) {
                kvs = Utils.convertJSONObjectToHashMap(inAppNotification.getCustomExtras());
            } else {
                kvs = new HashMap<>();
            }

            goFromListener = listener.beforeShow(kvs);
        } else {
            goFromListener = true;
        }

        return goFromListener;
    }

    private void showInApp(final CTInAppNotification inAppNotification) {
        Activity activity = CoreMetaData.getCurrentActivity();
        boolean goFromListener = checkBeforeShowApprovalBeforeDisplay(inAppNotification);
        if (!goFromListener) {
            logger.verbose(defaultLogTag,
                    "Application has decided to not show this in-app notification: " + inAppNotification
                            .getCampaignId());
            showNotificationIfAvailable();
            return;
        }

        Logger.v(defaultLogTag, "Attempting to show next In-App");

        if (!CoreMetaData.isAppForeground()) {
            pendingNotifications.add(inAppNotification);
            Logger.v(defaultLogTag, "Not in foreground, queueing this In App");
            return;
        }

        if (currentlyDisplayingInApp != null) {
            pendingNotifications.add(inAppNotification);
            Logger.v(defaultLogTag, "In App already displaying, queueing this In App");
            return;
        }

        if (!canShowInAppOnActivity(activity)) {
            pendingNotifications.add(inAppNotification);
            Logger.v(defaultLogTag, "Not showing In App on blacklisted activity, queuing this In App");
            return;
        }

        if ((clock.currentTimeMillis() / 1000) > inAppNotification.getTimeToLive()) {
            Logger.d("InApp has elapsed its time to live, not showing the InApp");
            return;
        }

        String inAppNotificationType = inAppNotification.getType();
        boolean isHtmlType = inAppNotificationType != null && inAppNotificationType.equals(Constants.KEY_CUSTOM_HTML);

        if (isHtmlType && !NetworkManager.isNetworkOnline(context)) {
            Logger.d(defaultLogTag,
                    "Not showing HTML InApp due to no internet. An active internet connection is required to display the HTML InApp");
            showNotificationIfAvailable();
            return;
        }

        currentlyDisplayingInApp = inAppNotification;

        CTInAppBaseFragment inAppFragment = null;
        CTInAppType type = inAppNotification.getInAppType();
        switch (type) {
            case CTInAppTypeCoverHTML:
            case CTInAppTypeInterstitialHTML:
            case CTInAppTypeHalfInterstitialHTML:
            case CTInAppTypeCover:
            case CTInAppTypeHalfInterstitial:
            case CTInAppTypeInterstitial:
            case CTInAppTypeAlert:
            case CTInAppTypeInterstitialImageOnly:
            case CTInAppTypeHalfInterstitialImageOnly:
            case CTInAppTypeCoverImageOnly:

                try {
                    if (activity == null) {
                        throw new IllegalStateException("Current activity reference not found");
                    }
                    Logger.d("Displaying In-App: " + inAppNotification.getJsonDescription());
                    InAppNotificationActivity.launchForInAppNotification(activity, inAppNotification, config);
                } catch (Throwable t) {
                    Logger.v("Please verify the integration of your app." +
                            " It is not setup to support in-app notifications yet.", t);
                    currentlyDisplayingInApp = null;
                    return;
                }
                break;
            case CTInAppTypeFooterHTML:
                inAppFragment = new CTInAppHtmlFooterFragment();
                break;
            case CTInAppTypeHeaderHTML:
                inAppFragment = new CTInAppHtmlHeaderFragment();
                break;
            case CTInAppTypeFooter:
                inAppFragment = new CTInAppNativeFooterFragment();
                break;
            case CTInAppTypeHeader:
                inAppFragment = new CTInAppNativeHeaderFragment();
                break;
            case CTInAppTypeCustomCodeTemplate:
                presentTemplate(inAppNotification);
                return;
            default:
                Logger.d(defaultLogTag, "Unknown InApp Type found: " + type);
                currentlyDisplayingInApp = null;
                return;
        }

        if (inAppFragment != null) {
            Logger.d("Displaying In-App: " + inAppNotification.getJsonDescription());
            boolean showFragmentSuccess = CTInAppBaseFragment.showOnActivity(inAppFragment, activity, inAppNotification, config, defaultLogTag);
            if (!showFragmentSuccess) {
                currentlyDisplayingInApp = null;
            }
        }
    }

    private void presentTemplate(final CTInAppNotification inAppNotification) {
        templatesManager.presentTemplate(inAppNotification, this, FileResourceProvider.getInstance(context, logger));
    }

    private JSONArray filterNonRegisteredCustomTemplates(JSONArray inAppNotifications) {
        return JsonUtilsKt.filterObjects(inAppNotifications, jsonObject -> !isNonRegisteredCustomTemplate(jsonObject));
    }

    private boolean isNonRegisteredCustomTemplate(JSONObject inApp) {
        CustomTemplateInAppData customTemplateData = CustomTemplateInAppData.createFromJson(inApp);
        boolean isNonRegistered = customTemplateData != null && customTemplateData.getTemplateName() != null
                && !templatesManager.isTemplateRegistered(customTemplateData.getTemplateName());

        if (isNonRegistered) {
            logger.info("CustomTemplates",
                    "Template with name \"" + customTemplateData.getTemplateName() +
                            "\" is not registered and cannot be presented");
        }

        return isNonRegistered;
    }

    private void triggerCustomTemplateAction(
            CTInAppNotification notification,
            CustomTemplateInAppData templateInAppData
    ) {
        if (templateInAppData != null && templateInAppData.getTemplateName() != null) {
            CustomTemplate template = templatesManager.getTemplate(templateInAppData.getTemplateName());
            if (template != null) {
                // When a custom in-app template is triggered as an action we need to present it.
                // Since all related methods operate with either CTInAppNotification or its json representation, here
                // we create a new notification from the one that initiated the triggering and add the action as its
                // template data.
                CustomTemplateInAppData actionTemplateData = templateInAppData.copy();
                actionTemplateData.setAction(true);
                CTInAppNotification notificationFromAction = notification.createNotificationForAction(actionTemplateData);
                if (notificationFromAction == null) {
                    logger.debug("Failed to present custom template with name: "
                            + templateInAppData.getTemplateName());
                    return;
                }
                if (template.isVisual()) {
                    addInAppNotificationInFrontOfQueue(notificationFromAction.getJsonDescription());
                } else {
                    prepareNotificationForDisplay(notificationFromAction.getJsonDescription());
                }
            } else {
                logger.debug("Cannot present non-registered template with name: "
                        + templateInAppData.getTemplateName());
            }
        } else {
            logger.debug("Cannot present template without name.");
        }
    }
}
