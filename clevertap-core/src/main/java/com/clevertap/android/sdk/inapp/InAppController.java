package com.clevertap.android.sdk.inapp;

import static com.clevertap.android.sdk.PushPermissionManager.ANDROID_PERMISSION_STRING;
import static com.clevertap.android.sdk.inapp.CTLocalInApp.FALLBACK_TO_NOTIFICATION_SETTINGS;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.BaseCallbackManager;
import com.clevertap.android.sdk.CTPreferenceCache;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.CoreMetaData;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.InAppNotificationActivity;
import com.clevertap.android.sdk.InAppNotificationListener;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.PushPermissionResponseListener;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.inapp.data.InAppResponseAdapter;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.evaluation.LimitAdapter;
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.MainLooperHandler;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.variables.JsonUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// inapp db handle // glovbal dn
@RestrictTo(Scope.LIBRARY_GROUP)
public class InAppController implements CTInAppNotification.CTInAppNotificationListener, InAppListener,
        InAppNotificationActivity.PushPermissionResultCallback {

    //InApp
    private final class NotificationPrepareRunnable implements Runnable {

        private final WeakReference<InAppController> inAppControllerWeakReference;

        private JSONObject jsonObject;

        private final boolean videoSupport = Utils.haveVideoPlayerSupport;

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
            inAppNotification.listener = inAppControllerWeakReference.get();
            inAppNotification.prepareForDisplay(resourceProvider);
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

    private InAppState inAppState;

    private HashSet<String> inappActivityExclude = null;

    private final Logger logger;

    private InAppResourceProvider resourceProvider;

    private final MainLooperHandler mainLooperHandler;

    private final InAppQueue inAppQueue;

    public final Function0<Unit> onAppLaunchEventSent;

    public final static String LOCAL_INAPP_COUNT = "local_in_app_count";

    public final static String IS_HARD_PERMISSION_REQUEST = "isHardPermissionRequest";

    public final static String IS_FIRST_TIME_PERMISSION_REQUEST = "firstTimeRequest";

    public final static String DISPLAY_HARD_PERMISSION_BUNDLE_KEY = "displayHardPermissionDialog";

    public final static String SHOW_FALLBACK_SETTINGS_BUNDLE_KEY = "shouldShowFallbackSettings";

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
            InAppResourceProvider resourceProvider
    ) {
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
        this.resourceProvider = resourceProvider;
        this.inAppQueue = inAppQueue;
        this.evaluationManager = evaluationManager;
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
    }

    public void checkExistingInAppNotifications(Activity activity) {
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (currentlyDisplayingInApp != null && ((System.currentTimeMillis() / 1000) < currentlyDisplayingInApp
                    .getTimeToLive())) {
                Fragment inAppFragment = ((FragmentActivity) activity).getSupportFragmentManager()
                        .getFragment(new Bundle(), currentlyDisplayingInApp.getType());
                if (CoreMetaData.getCurrentActivity() != null && inAppFragment != null) {
                    FragmentTransaction fragmentTransaction = ((FragmentActivity) activity)
                            .getSupportFragmentManager()
                            .beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("inApp", currentlyDisplayingInApp);
                    bundle.putParcelable("config", config);
                    inAppFragment.setArguments(bundle);
                    fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    fragmentTransaction.add(android.R.id.content, inAppFragment, currentlyDisplayingInApp.getType());
                    Logger.v(config.getAccountId(),
                            "calling InAppFragment " + currentlyDisplayingInApp.getCampaignId());
                    fragmentTransaction.commitNow();
                }
            }
        }
    }

    public void checkPendingInAppNotifications(Activity activity) {
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (mainLooperHandler.getPendingRunnable() != null) {
                logger.verbose(config.getAccountId(), "Found a pending inapp runnable. Scheduling it");
                mainLooperHandler.postDelayed(mainLooperHandler.getPendingRunnable(), 200);
                mainLooperHandler.setPendingRunnable(null);
            } else {
                showNotificationIfAvailable(context);
            }
        } else {
            Logger.d("In-app notifications will not be shown for this activity ("
                    + (activity != null ? activity.getLocalClassName() : "") + ")");
        }
    }

    @RequiresApi(api = 33)
    public void promptPushPrimer(JSONObject jsonObject){
        int permissionStatus = ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS);

        if (permissionStatus == PackageManager.PERMISSION_DENIED){
            //Checks whether permission request is asked for the first time.
            boolean isFirstTimeRequest = CTPreferenceCache.getInstance(context, config).isFirstTimeRequest();

            Activity currentActivity = CoreMetaData.getCurrentActivity();
            if (currentActivity == null) {
                Logger.d("CurrentActivity reference is null. SDK can't process the promptPushPrimer(jsonObject) method! Ensure the following things:\n" +
                        "1. Calling ActivityLifecycleCallback.register(this) in your custom application class before super.onCreate().\n" +
                        "   Alternatively, register CleverTap SDK's Application class in the manifest using com.clevertap.android.sdk.Application.\n" +
                        "2. Ensure that the promptPushPrimer() API is called from the onResume() lifecycle method, not onCreate().");
                return;
            }
            boolean shouldShowRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    currentActivity,
                    ANDROID_PERMISSION_STRING);

            if (!isFirstTimeRequest && shouldShowRequestPermissionRationale){
                if (!jsonObject.optBoolean(FALLBACK_TO_NOTIFICATION_SETTINGS, false)) {
                    Logger.v("Notification permission is denied. Please grant notification permission access" +
                            " in your app's settings to send notifications");
                    notifyPushPermissionResult(false);
                } else {
                    showSoftOrHardPrompt(jsonObject);
                }
                return;
            }
            showSoftOrHardPrompt(jsonObject);
        } else {
            notifyPushPermissionResult(true);
        }
    }

    @RequiresApi(api = 33)
    public void promptPermission(boolean showFallbackSettings) {
        JSONObject object = new JSONObject();
        try {
            object.put(FALLBACK_TO_NOTIFICATION_SETTINGS, showFallbackSettings);
            object.put(IS_HARD_PERMISSION_REQUEST, true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        promptPushPrimer(object);
    }

    /**
     * Shows either push primer or directly calls hard permission dialog flow based on whether
     * `isFromPromptPermission` is true.
     * @param jsonObject InApp object
     */
    private void showSoftOrHardPrompt(final JSONObject jsonObject) {
        if (jsonObject.optBoolean(IS_HARD_PERMISSION_REQUEST, false)) {
            startPrompt(Objects.requireNonNull(CoreMetaData.getCurrentActivity()),
                    config, jsonObject.optBoolean(FALLBACK_TO_NOTIFICATION_SETTINGS, false));
        } else {
            prepareNotificationForDisplay(jsonObject);
        }
    }

    public static void startPrompt(Activity activity, CleverTapInstanceConfig config,
                                   boolean showFallbackSettings){
        if (!activity.getClass().equals(InAppNotificationActivity.class)) {
            Intent intent = new Intent(activity, InAppNotificationActivity.class);
            Bundle configBundle = new Bundle();
            configBundle.putParcelable("config", config);
            intent.putExtra("configBundle", configBundle);
            intent.putExtra(Constants.INAPP_KEY, currentlyDisplayingInApp);
            intent.putExtra(DISPLAY_HARD_PERMISSION_BUNDLE_KEY, true);
            intent.putExtra(SHOW_FALLBACK_SETTINGS_BUNDLE_KEY, showFallbackSettings);
            activity.startActivity(intent);
        }
    }

    @RequiresApi(api = 33)
    public boolean isPushPermissionGranted(){
        int permissionStatus = ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS);
        return permissionStatus == PackageManager.PERMISSION_GRANTED;
    }

    public void discardInApps() {
        this.inAppState = InAppState.DISCARDED;
        logger.verbose(config.getAccountId(), "InAppState is DISCARDED");
    }

    @Override
    public void inAppNotificationDidClick(CTInAppNotification inAppNotification, Bundle formData,
            HashMap<String, String> keyValueMap) {
        analyticsManager.pushInAppNotificationStateEvent(true, inAppNotification, formData);
        if (keyValueMap != null && !keyValueMap.isEmpty()) {
            if (callbackManager.getInAppNotificationButtonListener() != null) {
                callbackManager.getInAppNotificationButtonListener().onInAppButtonClick(keyValueMap);
            }
        }
    }

    @Override
    public void inAppNotificationDidDismiss(final Context context, final CTInAppNotification inAppNotification,
            final Bundle formData) {
        inAppNotification.didDismiss(resourceProvider);

        if (controllerManager.getInAppFCManager() != null) {
            controllerManager.getInAppFCManager().didDismiss(inAppNotification);
            logger.verbose(config.getAccountId(), "InApp Dismissed: " + inAppNotification.getCampaignId());
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
                _showNotificationIfAvailable(context);
                return null;
            }
        });
    }

    //InApp
    @Override
    public void inAppNotificationDidShow(CTInAppNotification inAppNotification, Bundle formData) {
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
    @Override
    public void notificationReady(final CTInAppNotification inAppNotification) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainLooperHandler.post(new Runnable() {
                @Override
                public void run() {
                    notificationReady(inAppNotification);
                }
            });
            return;
        }

        if (inAppNotification.getError() != null) {
            logger
                    .debug(config.getAccountId(),
                            "Unable to process inapp notification " + inAppNotification.getError());
            return;
        }
        logger.debug(config.getAccountId(), "Notification ready: " + inAppNotification.getJsonDescription());
        displayNotification(inAppNotification);
    }

    @Override
    public void onPushPermissionAccept() {
        notifyPushPermissionResult(true);
    }

    @Override
    public void onPushPermissionDeny() {
        notifyPushPermissionResult(false);
    }

    //iterates over the PushPermissionResponseListenerList to notify the result
    public void notifyPushPermissionResult(boolean result) {
        for (final PushPermissionResponseListener listener: callbackManager.getPushPermissionResponseListenerList()) {
            if (listener != null){
                listener.onPushPermissionResponse(result);
            }
        }
    }

    public void resumeInApps() {
        this.inAppState = InAppState.RESUMED;
        logger.verbose(config.getAccountId(), "InAppState is RESUMED");
        logger.verbose(config.getAccountId(), "Resuming InApps by calling showInAppNotificationIfAny()");
        showInAppNotificationIfAny();
    }

    public void addInAppNotificationsToQueue(JSONArray inappNotifs) {
        try {
            inAppQueue.enqueueAll(inappNotifs);

            // Fire the first notification, if any
            showNotificationIfAvailable(context);
        } catch (Exception e) {
            logger.debug(config.getAccountId(), "InAppController: : InApp notification handling error: " + e.getMessage());
        }
    }


    //InApp
    public void showNotificationIfAvailable(final Context context) {
        if (!config.isAnalyticsOnly()) {
            Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask(Constants.TAG_FEATURE_IN_APPS);
            task.execute("InappController#showNotificationIfAvailable", new Callable<Void>() {
                @Override
                public Void call() {
                    _showNotificationIfAvailable(context);
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
    private void _showNotificationIfAvailable(Context context) {
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

        if (controllerManager.getInAppFCManager() != null) {

            final Function2<JSONObject, String, Boolean> hasInAppFrequencyLimitsMaxedOut = (inAppJSON, inAppId) -> {
                final List<LimitAdapter> listOfWhenLimits = InAppResponseAdapter.getListOfWhenLimits(inAppJSON);
                return !evaluationManager.matchWhenLimitsBeforeDisplay(listOfWhenLimits, inAppId);
            };

            if (!controllerManager.getInAppFCManager().canShow(inAppNotification, hasInAppFrequencyLimitsMaxedOut)) {
                logger.verbose(config.getAccountId(),
                        "InApp has been rejected by FC, not showing " + inAppNotification.getCampaignId());
                showInAppNotificationIfAny();
                return;
            }
        } else {
            logger.verbose(config.getAccountId(),
                    "getCoreState().getInAppFCManager() is NULL, not showing " + inAppNotification.getCampaignId());
            return;
        }

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

        if (!goFromListener) {
            logger.verbose(config.getAccountId(),
                    "Application has decided to not show this in-app notification: " + inAppNotification
                            .getCampaignId());
            showInAppNotificationIfAny();
            return;
        }
        showInApp(context, inAppNotification, config, this);
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
                    _showNotificationIfAvailable(context);
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

    private static void checkPendingNotifications(final Context context, final CleverTapInstanceConfig config,
            final InAppController inAppController) {
        Logger.v(config.getAccountId(), "checking Pending Notifications");
        if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
            try {
                final CTInAppNotification notification = pendingNotifications.get(0);
                pendingNotifications.remove(0);
                MainLooperHandler mainHandler = new MainLooperHandler();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showInApp(context, notification, config, inAppController);
                    }
                });
            } catch (Throwable t) {
                // no-op
            }
        }
    }

    //InApp
    private static void inAppDidDismiss(Context context, CleverTapInstanceConfig config,
            CTInAppNotification inAppNotification, InAppController inAppController) {
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

    //InApp
    private static void showInApp(Context context, final CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config, InAppController inAppController) {

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

        boolean isHtmlType = inAppNotification.getType().equals(Constants.KEY_CUSTOM_HTML);
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

                Intent intent = new Intent(context, InAppNotificationActivity.class);
                intent.putExtra(Constants.INAPP_KEY, inAppNotification);
                Bundle configBundle = new Bundle();
                configBundle.putParcelable("config", config);
                intent.putExtra("configBundle", configBundle);
                try {
                    Activity currentActivity = CoreMetaData.getCurrentActivity();
                    if (currentActivity == null) {
                        throw new IllegalStateException("Current activity reference not found");
                    }
                    config.getLogger().verbose(config.getAccountId(),
                            "calling InAppActivity for notification: " + inAppNotification.getJsonDescription());
                    currentActivity.startActivity(intent);
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
    public void onQueueChargedEvent(Map<String, Object> chargeDetails,
            List<Map<String, Object>> items, Location userLocation) {
        final Map<String, Object> appFieldsWithChargedEventProperties = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        appFieldsWithChargedEventProperties.putAll(chargeDetails);
        final JSONArray clientSideInAppsToDisplay = evaluationManager.evaluateOnChargedEvent(
                appFieldsWithChargedEventProperties, items, userLocation);
        if (clientSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(clientSideInAppsToDisplay);
        }
    }

    public void onAppLaunchServerSideInAppsResponse(@NonNull JSONArray appLaunchServerSideInApps,
            Location userLocation)
            throws JSONException {
        final Map<String, Object> appLaunchedProperties = JsonUtil.mapFromJson(
                deviceInfo.getAppLaunchedFields());
        List<JSONObject> appLaunchSsInAppList = Utils.toJSONObjectList(appLaunchServerSideInApps);
        final JSONArray serverSideInAppsToDisplay = evaluationManager.evaluateOnAppLaunchedServerSide(
                appLaunchSsInAppList, appLaunchedProperties, userLocation);

        if (serverSideInAppsToDisplay.length() > 0) {
            addInAppNotificationsToQueue(serverSideInAppsToDisplay);
        }
    }
}