package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.inapp.CTLocalInApp.FALLBACK_TO_NOTIFICATION_SETTINGS;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

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
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter;
import com.clevertap.android.sdk.inapp.images.FileResourceProvider;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.inapp.store.preference.FileStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.JsonUtilsKt;
import com.clevertap.android.sdk.variables.JsonUtil;
import com.clevertap.android.sdk.video.VideoLibChecker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;

// inapp db handle // glovbal dn
@RestrictTo(Scope.LIBRARY_GROUP)
public class InAppController implements InAppListener {

    //InApp
    private final class NotificationPrepareRunnable implements Runnable {

        private final WeakReference<InAppController> inAppControllerWeakReference;

        private final JSONObject jsonObject;

        private final boolean videoSupport = VideoLibChecker.haveVideoPlayerSupport;

        NotificationPrepareRunnable(InAppController inAppController, JSONObject jsonObject) {
            this.inAppControllerWeakReference = new WeakReference<>(inAppController);
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            final CTInAppNotification inAppNotification = new CTInAppNotification()
                    .initWithJSON(jsonObject, videoSupport);
            if (inAppNotification.getError() != null) {
                logger
                        .debug(config.getAccountId(),
                                "Unable to parse inapp notification " + inAppNotification.getError());
                return;
            }
            prepareForDisplay(inAppNotification);
        }

        void prepareForDisplay(CTInAppNotification inApp) {

            final Pair<FileStore, InAppAssetsStore> storePair = new Pair<>(storeRegistry.getFilesStore(),
                    storeRegistry.getInAppAssetsStore());

            String templateName = null;
            FileResourceProvider fileResourceProvider = FileResourceProvider.getInstance(context, logger);
            if (CTInAppType.CTInAppTypeCustomCodeTemplate.equals(inApp.getInAppType())) {
                final CustomTemplateInAppData customTemplateData = inApp.getCustomTemplateData();
                final List<String> fileUrls;
                if (customTemplateData != null) {
                    templateName = customTemplateData.getTemplateName();
                    fileUrls = customTemplateData.getFileArgsUrls(templatesManager);
                } else {
                    fileUrls = Collections.emptyList();
                }

                int index = 0;
                while (index < fileUrls.size()) {
                    String url = fileUrls.get(index);
                    byte[] bytes = fileResourceProvider.fetchFile(url);

                    if (bytes != null && bytes.length > 0) {
                        FileResourcesRepoImpl.saveUrlExpiryToStore(new Pair<>(url, CtCacheType.FILES), storePair);
                    } else {
                        // download fail
                        inApp.setError("Error processing the custom code in-app template: file download failed.");
                        break;
                    }
                    index++;
                }
            } else {
                for (CTInAppNotificationMedia media : inApp.getMediaList()) {
                    if (media.isGIF()) {
                        byte[] bytes = fileResourceProvider.fetchInAppGifV1(media.getMediaUrl());
                        if (bytes == null || bytes.length == 0) {
                            inApp.setError("Error processing GIF");
                            break;
                        }
                    } else if (media.isImage()) {

                        Bitmap bitmap = fileResourceProvider.fetchInAppImageV1(media.getMediaUrl());
                        if (bitmap == null) {
                            inApp.setError("Error processing image as bitmap was NULL");
                        }
                    } else if (media.isVideo() || media.isAudio()) {
                        if (!inApp.isVideoSupported()) {
                            inApp.setError("InApp Video/Audio is not supported");
                        }
                    }
                }
            }

            InAppController controller = inAppControllerWeakReference.get();
            if (controller != null) {
                final CustomTemplate template =
                        templateName != null ? templatesManager.getTemplate(templateName) : null;
                controller.notificationReady(inApp, template);
            }
        }
    }

    private enum InAppState {
        DISCARDED(-1),
        SUSPENDED(0),
        RESUMED(1);

        final int state;

        InAppState(final int inAppState) {
            state = inAppState;
        }

        int intValue() {
            return state;
        }
    }

    private static CTInAppNotification currentlyDisplayingInApp = null;

    private static final List<CTInAppNotification> pendingNotifications = Collections
            .synchronizedList(new ArrayList<CTInAppNotification>());

    private final AnalyticsManager analyticsManager;

    private final BaseCallbackManager callbackManager;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final ControllerManager controllerManager;

    private final CoreMetaData coreMetaData;

    private final DeviceInfo deviceInfo;

    private final EvaluationManager evaluationManager;

    private final StoreRegistry storeRegistry;

    private final TemplatesManager templatesManager;

    private InAppState inAppState;

    private HashSet<String> inappActivityExclude = null;

    private final Logger logger;

    private final MainLooperHandler mainLooperHandler;

    private final InAppQueue inAppQueue;

    public final Function0<Unit> onAppLaunchEventSent;

    private final InAppActionHandler inAppActionHandler;

    public final static String LOCAL_INAPP_COUNT = "local_in_app_count";

    public final static String IS_FIRST_TIME_PERMISSION_REQUEST = "firstTimeRequest";

    public InAppController(
            Context context,
            CleverTapInstanceConfig config,
            MainLooperHandler mainLooperHandler,
            ControllerManager controllerManager,
            BaseCallbackManager callbackManager,
            AnalyticsManager analyticsManager,
            CoreMetaData coreMetaData,
            final DeviceInfo deviceInfo,
            InAppQueue inAppQueue,
            final EvaluationManager evaluationManager,
            TemplatesManager templatesManager,
            final StoreRegistry storeRegistry,
            final InAppActionHandler inAppActionHandler) {
        this.context = context;
        this.config = config;
        this.logger = this.config.getLogger();
        this.mainLooperHandler = mainLooperHandler;
        this.controllerManager = controllerManager;
        this.callbackManager = callbackManager;
        this.analyticsManager = analyticsManager;
        this.coreMetaData = coreMetaData;
        this.inAppState = InAppState.RESUMED;
        this.deviceInfo = deviceInfo;
        this.inAppQueue = inAppQueue;
        this.evaluationManager = evaluationManager;
        this.templatesManager = templatesManager;
        this.storeRegistry = storeRegistry;
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
    }


    public void checkPendingInAppNotifications(Activity activity) {
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (mainLooperHandler.getPendingRunnable() != null) {
                logger.verbose(config.getAccountId(), "Found a pending inapp runnable. Scheduling it");
                mainLooperHandler.postDelayed(mainLooperHandler.getPendingRunnable(), 200);
                mainLooperHandler.setPendingRunnable(null);
            } else {
                showNotificationIfAvailable();
            }
        } else {
            Logger.d("In-app notifications will not be shown for this activity ("
                    + (activity != null ? activity.getLocalClassName() : "") + ")");
        }
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

    public void discardInApps() {
        this.inAppState = InAppState.DISCARDED;
        logger.verbose(config.getAccountId(), "InAppState is DISCARDED");
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
            logger.verbose(config.getAccountId(),
                    "InApp Dismissed: " + inAppNotification.getCampaignId() + "  " + templateName);
        } else {
            logger.verbose(config.getAccountId(), "Not calling InApp Dismissed: " + inAppNotification.getCampaignId()
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
            logger.verbose(config.getAccountId(), "Failed to call the in-app notification listener", t);
        }

        // Fire the next one, if any
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
        task.execute("InappController#inAppNotificationDidDismiss", new Callable<Void>() {
            @Override
            public Void call() {
                inAppDidDismiss(context, config, inAppNotification, InAppController.this);
                _showNotificationIfAvailable();
                return null;
            }
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
            Logger.v(config.getAccountId(), "Failed to call the in-app notification listener", t);
        }
    }

    //InApp
    private void notificationReady(final CTInAppNotification inAppNotification,
                                   @Nullable final CustomTemplate template) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainLooperHandler.post(new Runnable() {
                @Override
                public void run() {
                    notificationReady(inAppNotification, template);
                }
            });
            return;
        }

        if (inAppNotification.getError() != null) {
            logger.debug(config.getAccountId(),
                    "Unable to process inapp notification " + inAppNotification.getError());
            return;
        }
        logger.debug(config.getAccountId(), "Notification ready: " + inAppNotification.getJsonDescription());
        if (template != null && !template.isVisual()) {
            presentTemplate(inAppNotification);
        } else {
            displayNotification(inAppNotification);
        }
    }

    public void resumeInApps() {
        this.inAppState = InAppState.RESUMED;
        logger.verbose(config.getAccountId(), "InAppState is RESUMED");
        logger.verbose(config.getAccountId(), "Resuming InApps by calling showInAppNotificationIfAny()");
        showInAppNotificationIfAny();
    }

    @WorkerThread
    public void addInAppNotificationsToQueue(JSONArray inappNotifs) {
        try {
            JSONArray filteredNotifs = filterNonRegisteredCustomTemplates(inappNotifs);
            inAppQueue.enqueueAll(filteredNotifs);

            // Fire the first notification, if any
            showNotificationIfAvailable();
        } catch (Exception e) {
            logger.debug(config.getAccountId(), "InAppController: : InApp notification handling error: " + e.getMessage());
        }
    }


    //InApp
    public void showNotificationIfAvailable() {
        if (!config.isAnalyticsOnly()) {
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InappController#showNotificationIfAvailable", new Callable<Void>() {
                @Override
                public Void call() {
                    _showNotificationIfAvailable();
                    return null;
                }
            });
        }
    }

    public void suspendInApps() {
        this.inAppState = InAppState.SUSPENDED;
        logger.verbose(config.getAccountId(), "InAppState is SUSPENDED");
    }

    //InApp
    private void _showNotificationIfAvailable() {
        try {
            if (!canShowInAppOnActivity()) {
                Logger.v("Not showing notification on blacklisted activity");
                return;
            }

            if (this.inAppState == InAppState.SUSPENDED) {
                logger.debug(config.getAccountId(),
                        "InApp Notifications are set to be suspended, not showing the InApp Notification");
                return;
            }

            checkPendingNotifications(context, config, this);  // see if we have any pending notifications

            JSONObject inapp = inAppQueue.dequeue();
            if (inapp == null) {
                return;
            }

            if (this.inAppState != InAppState.DISCARDED) {
                prepareNotificationForDisplay(inapp);
            } else {
                logger.debug(config.getAccountId(),
                        "InApp Notifications are set to be discarded, dropping the InApp Notification");
            }
        } catch (Throwable t) {
            // We won't get here
            logger.verbose(config.getAccountId(), "InApp: Couldn't parse JSON array string from prefs", t);
        }
    }

    private void addInAppNotificationInFrontOfQueue(JSONObject inApp) {
        if (isNonRegisteredCustomTemplate(inApp)) {
            return;
        }
        inAppQueue.insertInFront(inApp);
        showNotificationIfAvailable();
    }

    private boolean canShowInAppOnActivity() {
        updateBlacklistedActivitySet();

        for (String blacklistedActivity : inappActivityExclude) {
            String currentActivityName = CoreMetaData.getCurrentActivityName();
            if (currentActivityName != null && currentActivityName.contains(blacklistedActivity)) {
                return false;
            }
        }

        return true;
    }

    //InApp
    private void displayNotification(final CTInAppNotification inAppNotification) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainLooperHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayNotification(inAppNotification);
                }
            });
            return;
        }

        if (inAppNotification.isRequestForPushPermission() && inAppActionHandler.arePushNotificationsEnabled()) {
            logger.verbose(config.getAccountId(),
                    "Not showing push permission request, permission is already granted");
            inAppActionHandler.notifyPushPermissionListeners();
            showInAppNotificationIfAny();
            return;
        }

        checkLimitsBeforeShowing(context, inAppNotification, config, this);
        incrementLocalInAppCountInPersistentStore(context, inAppNotification);
    }

    //InApp
    private void prepareNotificationForDisplay(final JSONObject jsonObject) {
        logger.debug(config.getAccountId(), "Preparing In-App for display: " + jsonObject.toString());
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
        task.execute("InappController#prepareNotificationForDisplay", new Callable<Void>() {
            @Override
            public Void call() {
                new NotificationPrepareRunnable(InAppController.this, jsonObject).run();
                return null;
            }
        });
    }
    private void showInAppNotificationIfAny() {
        if (!config.isAnalyticsOnly()) {
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InAppController#showInAppNotificationIfAny", new Callable<Void>() {
                @Override
                public Void call() {
                    _showNotificationIfAvailable();
                    return null;
                }
            });
        }
    }

    private void updateBlacklistedActivitySet() {
        if (inappActivityExclude == null) {
            inappActivityExclude = new HashSet<>();
            try {
                String activities = ManifestInfo.getInstance(context).getExcludedActivities();
                if (activities != null) {
                    String[] split = activities.split(",");
                    for (String a : split) {
                        inappActivityExclude.add(a.trim());
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }
            logger.debug(config.getAccountId(),
                    "In-app notifications will not be shown on " + Arrays.toString(inappActivityExclude.toArray()));
        }
    }

    private void checkPendingNotifications(
            @NonNull final Context context,
            final CleverTapInstanceConfig config,
            final InAppController inAppController
    ) {
        Logger.v(config.getAccountId(), "checking Pending Notifications");
        if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
            try {
                final CTInAppNotification notification = pendingNotifications.get(0);
                pendingNotifications.remove(0);
                checkLimitsBeforeShowing(context, notification, config, inAppController);
            } catch (Throwable t) {
                // no-op
            }
        }
    }

    //InApp
    private void inAppDidDismiss(
            @NonNull Context context,
            CleverTapInstanceConfig config,
            CTInAppNotification inAppNotification,
            InAppController inAppController
    ) {
        Logger.v(config.getAccountId(), "Running inAppDidDismiss");
        if (currentlyDisplayingInApp != null && (currentlyDisplayingInApp.getCampaignId()
                .equals(inAppNotification.getCampaignId()))) {
            currentlyDisplayingInApp = null;
            checkPendingNotifications(context, config, inAppController);
        }
    }

    private void incrementLocalInAppCountInPersistentStore(Context context, CTInAppNotification inAppNotification) {
        if (inAppNotification.isLocalInApp()) {
            deviceInfo.incrementLocalInAppCount();//update cache
            Task<Void> task = CTExecutorFactory.executors(config).ioTask();
            task.execute("InAppController#incrementLocalInAppCountInPersistentStore", new Callable<Void>() {
                @Override
                public Void call() {
                    StorageHelper.putIntImmediate(context, LOCAL_INAPP_COUNT,
                            deviceInfo.getLocalInAppCount());// update disk with cache
                    return null;
                }
            });
        }
    }

    private void checkLimitsBeforeShowing(
            @NonNull Context context,
            final CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config,
            InAppController inAppController) {
        Task<Boolean> task = CTExecutorFactory.executors(config).ioTask();
        task.addOnSuccessListener(canShow -> {
            if(canShow) {
                showInApp(context, inAppNotification, config, inAppController);
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
                    logger.verbose(config.getAccountId(),
                            "InApp has been rejected by FC, not showing " + inAppNotification.getCampaignId());
                    return false;
                }
            } else {
                logger.verbose(config.getAccountId(),
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
    //InApp
    private void showInApp(
            @NonNull Context context,
            final CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config,
            InAppController inAppController
    ) {

        boolean goFromListener = checkBeforeShowApprovalBeforeDisplay(inAppNotification);
        if (!goFromListener) {
            logger.verbose(config.getAccountId(),
                    "Application has decided to not show this in-app notification: " + inAppNotification
                            .getCampaignId());
            showInAppNotificationIfAny();
            return;
        }

        Logger.v(config.getAccountId(), "Attempting to show next In-App");

        if (!CoreMetaData.isAppForeground()) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(), "Not in foreground, queueing this In App");
            return;
        }

        if (currentlyDisplayingInApp != null) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(), "In App already displaying, queueing this In App");
            return;
        }

        if(!inAppController.canShowInAppOnActivity()) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(), "Not showing In App on blacklisted activity, queuing this In App");
            return;
        }

        if ((System.currentTimeMillis() / 1000) > inAppNotification.getTimeToLive()) {
            Logger.d("InApp has elapsed its time to live, not showing the InApp");
            return;
        }

        String inAppNotificationType = inAppNotification.getType();
        boolean isHtmlType = inAppNotificationType != null && inAppNotificationType.equals(Constants.KEY_CUSTOM_HTML);

        if (isHtmlType && !NetworkManager.isNetworkOnline(context)) {
            Logger.d(config.getAccountId(),
                    "Not showing HTML InApp due to no internet. An active internet connection is required to display the HTML InApp");
            inAppController.showInAppNotificationIfAny();
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
                    Activity currentActivity = CoreMetaData.getCurrentActivity();
                    if (currentActivity == null) {
                        throw new IllegalStateException("Current activity reference not found");
                    }
                    config.getLogger().verbose(config.getAccountId(),
                            "calling InAppActivity for notification: " + inAppNotification.getJsonDescription());
                    InAppNotificationActivity.launchForInAppNotification(currentActivity, inAppNotification, config);
                    Logger.d("Displaying In-App: " + inAppNotification.getJsonDescription());

                } catch (Throwable t) {
                    Logger.v("Please verify the integration of your app." +
                            " It is not setup to support in-app notifications yet.", t);
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
                inAppController.presentTemplate(inAppNotification);
                return;
            default:
                Logger.d(config.getAccountId(), "Unknown InApp Type found: " + type);
                currentlyDisplayingInApp = null;
                return;
        }

        if (inAppFragment != null) {
            Logger.d("Displaying In-App: " + inAppNotification.getJsonDescription());
            try {
                //noinspection Constant Conditions
                FragmentTransaction fragmentTransaction = ((FragmentActivity) CoreMetaData.getCurrentActivity())
                        .getSupportFragmentManager()
                        .beginTransaction();
                Bundle bundle = new Bundle();
                bundle.putParcelable("inApp", inAppNotification);
                bundle.putParcelable("config", config);
                inAppFragment.setArguments(bundle);
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                fragmentTransaction.add(android.R.id.content, inAppFragment, inAppNotification.getType());
                Logger.v(config.getAccountId(), "calling InAppFragment " + inAppNotification.getCampaignId());
                fragmentTransaction.commitNow();

            } catch (ClassCastException e) {
                Logger.v(config.getAccountId(),
                        "Fragment not able to render, please ensure your Activity is an instance of AppCompatActivity"
                                + e.getMessage());
                currentlyDisplayingInApp = null;
            } catch (Throwable t) {
                Logger.v(config.getAccountId(), "Fragment not able to render", t);
                currentlyDisplayingInApp = null;
            }
        }
    }

    @WorkerThread
    public void onQueueEvent(final String eventName, Map<String, Object> eventProperties, Location userLocation) {
        final Map<String, Object> appFieldsWithEventProperties = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        appFieldsWithEventProperties.putAll(eventProperties);
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnEvent(eventName,
                appFieldsWithEventProperties,
                userLocation);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    @WorkerThread
    public void onQueueChargedEvent(
            Map<String, Object> chargeDetails,
            List<Map<String, Object>> items,
            Location userLocation
    ) {
        final Map<String, Object> appFieldsWithChargedEventProperties = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        appFieldsWithChargedEventProperties.putAll(chargeDetails);
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnChargedEvent(
                appFieldsWithChargedEventProperties, items, userLocation);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    @WorkerThread
    public void onQueueProfileEvent(
            final Map<String, Map<String, Object>> userAttributeChangedProperties,
            Location location
    ) {
        final Map<String, Object> appFields = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnUserAttributeChange(
                userAttributeChangedProperties, location, appFields);
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

    public TemplatesManager getTemplatesManager() {
        return templatesManager;
    }
    public StoreRegistry getStoreRegistry() {
        return storeRegistry;
    }
}
