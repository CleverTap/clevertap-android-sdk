package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.plus.model.people.Person;
import com.google.firebase.iid.FirebaseInstanceId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * <h1>CleverTapAPI</h1>
 * This is the main CleverTapAPI class that manages the SDK instances
 */
public class CleverTapAPI implements CTInAppNotification.CTInAppNotificationListener,InAppNotificationActivity.InAppActivityListener, CTInAppBaseFragment.InAppListener, CTNotificationInboxListener {

    public enum LogLevel{
        OFF(-1),
        INFO(0),
        DEBUG(2);

        private final int value;

        LogLevel(final int newValue) {
            value = newValue;
        }

        public int intValue() { return value; }
    }

    /**
     * @deprecated Use {@link #pushChargedEvent(HashMap chargeDetails, ArrayList items)}
     */
    @Deprecated
    public static final String CHARGED_EVENT = "Charged";
    @SuppressWarnings("unused")
    public static final String NOTIFICATION_TAG = "wzrk_pn";

    private static int debugLevel = CleverTapAPI.LogLevel.INFO.intValue();
    private static final Boolean pendingValidationResultsLock = true;
    private static CleverTapInstanceConfig defaultConfig;
    private static HashMap<String, CleverTapAPI> instances;
    private static boolean appForeground = false;
    private static int activityCount = 0;
    private String currentScreenName = "";
    private static ArrayList<CTInAppNotification> pendingNotifications = new ArrayList<>();
    private Runnable pendingInappRunnable = null;
    private static CTInAppNotification currentlyDisplayingInApp = null;

    private static WeakReference<Activity> currentActivity;
    private static int initialAppEnteredForegroundTime = 0;
    private static SSLContext sslContext;
    private static SSLSocketFactory sslSocketFactory;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static String sdkVersion;  // For Google Play Store/Android Studio analytics

    private DBAdapter dbAdapter;
    private Context context;
    private LocalDataStore localDataStore;
    private CleverTapInstanceConfig config;
    private int mResponseFailureCount = 0;
    private int currentRequestTimestamp = 0;
    private Location locationFromUser = null;
    private SyncListener syncListener = null;
    private ArrayList<PushType> enabledPushTypes = null;
    private long appLastSeen = 0;
    private int currentSessionId = 0;
    private boolean firstSession = false;
    private int lastSessionLength = 0;
    private String source = null, medium = null, campaign = null;
    private JSONObject wzrkParams = null;
    private int lastVisitTime;
    private final HashMap<String,Object> notificationIdTagMap = new HashMap<>();
    private final HashMap<String,Object> notificationViewedIdTagMap = new HashMap<>();
    private DeviceInfo deviceInfo;
    private DevicePushTokenRefreshListener tokenRefreshListener;
    private boolean appLaunchPushed = false;
    private final Object appLaunchPushedLock = new Object();
    private Handler handlerUsingMainLooper;
    private ExecutorService es;
    private ExecutorService ns;
    private Runnable commsRunnable = null;
    private Validator validator;
    private final Object optOutFlagLock = new Object();
    private boolean currentUserOptedOut = false;
    private final HashMap<String, Integer> installReferrerMap = new HashMap<>(8);
    private boolean enableNetworkInfoReporting = false;
    private ArrayList<ValidationResult> pendingValidationResults = new ArrayList<>();
    private HashSet<String> inappActivityExclude = null;
    private InAppNotificationListener inAppNotificationListener;
    private InAppFCManager inAppFCManager;
    private int lastLocationPingTime = 0;
    private final Object tokenLock = new Object();
    private final Object notificationMapLock = new Object();
    private boolean havePushedDeviceToken = false;
    private String processingUserLoginIdentifier = null;
    private final Boolean processingUserLoginLock = true;
    private long EXECUTOR_THREAD_ID = 0;
    private long NOTIFICATION_THREAD_ID = 0;
    private final Boolean eventLock = true;
    private boolean offline = false;
    private CTInboxController ctInboxController;
    private final Object inboxControllerLock = new Object();

    @Deprecated
    public final EventHandler event;
    @Deprecated
    public final ProfileHandler profile;
    @Deprecated
    public final DataHandler data;
    @Deprecated
    public final SessionHandler session;


    // Initialize
    private CleverTapAPI(final Context context, final CleverTapInstanceConfig config){
        this.config = new CleverTapInstanceConfig(config);
        this.context = context;
        this.handlerUsingMainLooper = new Handler(Looper.getMainLooper());
        this.es = Executors.newFixedThreadPool(1);
        this.ns = Executors.newFixedThreadPool(1);
        this.localDataStore = new LocalDataStore(context, config);
        this.deviceInfo = DeviceInfo.initWithConfig(context, config);
        this.validator = new Validator();
        this.inAppFCManager = new InAppFCManager(context, config);
        postAsyncSafely("CleverTapAPI#initializeDeviceInfo", new Runnable() {
            @Override
            public void run() {
                initializeDeviceInfo();
                if (config.isDefaultInstance()) {
                    manifestAsyncValidation();
                }
            }
        });

        int now = (int) System.currentTimeMillis()/1000;
        if(now - initialAppEnteredForegroundTime > 5){
            this.config.setCreatedPostAppLaunch();
        }

        this.event = new EventHandler(this);
        this.profile = new ProfileHandler(this);
        this.data = new DataHandler(this);
        this.session = new SessionHandler(this);

        setLastVisitTime();
        postAsyncSafely("setStatesAsync", new Runnable() {
            @Override
            public void run() {
                setDeviceNetworkInfoReportingFromStorage();
                setCurrentUserOptOutStateFromStorage();
            }
        });

        postAsyncSafely("saveConfigtoSharedPrefs", new Runnable() {
            @Override
            public void run() {
                String configJson = config.toJSONString();
                if(configJson == null) {
                    Logger.v("Unable to save config to SharedPrefs, config Json is null");
                    return;
                }
                StorageHelper.putString(context,storageKeyWithSuffix("instance"), configJson);
            }
        });
        Logger.i("CleverTap SDK initialized with accountId: "+ config.getAccountId() + " accountToken: " + config.getAccountId() + " accountRegion: " + config.getAccountRegion());

        //TODO Remove after testing
        postAsyncSafely("stub", new Runnable() {
            @Override
            public void run() {
                try {
                    manualInboxUpdate();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // only call async
    private void initializeDeviceInfo() {
        deviceInfo.initDeviceID();
    }

    private LocalDataStore getLocalDataStore() {
        return localDataStore;
    }

    private CleverTapInstanceConfig getConfig() {
        return config;
    }

    private Logger getConfigLogger(){
        return getConfig().getLogger();
    }

    private String getAccountId(){
        return config.getAccountId();
    }

    // static lifecycle callbacks
    static void onActivityCreated(Activity activity) {
        // make sure we have at least the default instance created here.
        if (instances == null) {
            CleverTapAPI.createInstanceIfAvailable(activity, null);
        }
        CleverTapAPI.setAppForeground(true);

        if (instances == null) {
            Logger.v("Instances is null in onActivityCreated!");
            return;
        }

        boolean alreadyProcessedByCleverTap = false;
        Bundle notification = null;
        Uri deepLink = null;
        String _accountId = null;

        // check for launch deep link
        try {
            Intent intent = activity.getIntent();
            deepLink = intent.getData();
            if (deepLink != null) {
                Bundle queryArgs = UriHelper.getAllKeyValuePairs(deepLink.toString(), true);
                _accountId = queryArgs.getString(Constants.WZRK_ACCT_ID_KEY);
            }
        } catch (Throwable t) {
            // Ignore
        }

        // check for launch via notification click
        try {
            notification = activity.getIntent().getExtras();
            if (notification != null && !notification.isEmpty()) {
                try {
                    alreadyProcessedByCleverTap = (notification.containsKey(Constants.WZRK_FROM_KEY) && Constants.WZRK_FROM.equals(notification.get(Constants.WZRK_FROM_KEY)));
                    if (alreadyProcessedByCleverTap){
                        Logger.v("ActivityLifecycleCallback: Notification Clicked already processed for "+ notification.toString() +", dropping duplicate.");
                    }
                    if (notification.containsKey(Constants.WZRK_ACCT_ID_KEY)) {
                        _accountId = (String) notification.get(Constants.WZRK_ACCT_ID_KEY);
                    }
                } catch (Throwable t) {
                    // no-op
                }
            }
        } catch (Throwable t) {
            // Ignore
        }

        if (alreadyProcessedByCleverTap && deepLink == null) return;

        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);

            boolean shouldProcess = (_accountId == null && instance.config.isDefaultInstance()) || instance.getAccountId().equals(_accountId);

            if (shouldProcess) {
                if (notification != null && !notification.isEmpty() && notification.containsKey(Constants.NOTIFICATION_TAG)) {
                    instance.pushNotificationClickedEvent(notification);
                }

                if (deepLink != null) {
                    try {
                        instance.pushDeepLink(deepLink);
                    } catch (Throwable t) {
                        // no-op
                    }
                }
                break;
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void onActivityResumed(Activity activity) {
        if (instances == null) {
            CleverTapAPI.createInstanceIfAvailable(activity, null);
        }

        CleverTapAPI.setAppForeground(true);

        if (instances == null) {
            Logger.v("Instances is null in onActivityResumed!");
            return;
        }

        String currentActivityName = getCurrentActivityName();
        setCurrentActivity(activity);
        if (currentActivityName == null || !currentActivityName.equals(activity.getLocalClassName())) {
            activityCount++;
        }

        if (initialAppEnteredForegroundTime <= 0){
            initialAppEnteredForegroundTime = (int)System.currentTimeMillis()/1000;
        }

        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            try {
                instance.activityResumed(activity);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void onActivityPaused() {
        if (instances == null) return;

        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            try {
                instance.activityPaused();
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    // other static handlers

    static void handleNotificationClicked(Context context,Bundle notification) {
        if (notification == null) return;

        String _accountId = null;
        try {
            _accountId = notification.getString(Constants.WZRK_ACCT_ID_KEY);
        } catch (Throwable t) {
            // no-op
        }

        if (instances == null) {
            CleverTapAPI instance = createInstanceIfAvailable(context, _accountId);
            if (instance != null) {
                instance.pushNotificationClickedEvent(notification);
            }
            return;
        }

        for (String accountId: instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            boolean shouldProcess = (_accountId == null && instance.config.isDefaultInstance()) || instance.getAccountId().equals(_accountId);
            if (shouldProcess) {
                instance.pushNotificationClickedEvent(notification);
                break;
            }
        }
    }

    /**
     * Returns an instance of the CleverTap SDK.
     *
     * @param context The Android context
     * @return The {@link CleverTapAPI} object
     * @deprecated use {@link CleverTapAPI#getDefaultInstance(Context context)}
     */
    public static @Nullable CleverTapAPI getInstance(Context context) throws CleverTapMetaDataNotFoundException, CleverTapPermissionsNotSatisfied {
        // For Google Play Store/Android Studio tracking
        sdkVersion = BuildConfig.SDK_VERSION_STRING;
        return getDefaultInstance(context);
    }

    /**
     * Returns the default shared instance of the CleverTap SDK.
     *
     * @param context The Android context
     * @return The {@link CleverTapAPI} object
     */
    @SuppressWarnings("WeakerAccess")
    public static @Nullable CleverTapAPI getDefaultInstance(Context context) {
        // For Google Play Store/Android Studio tracking
        sdkVersion = BuildConfig.SDK_VERSION_STRING;
        if (defaultConfig == null) {
            ManifestInfo manifest = ManifestInfo.getInstance(context);
            String accountId = manifest.getAccountId();
            String accountToken = manifest.getAcountToken();
            String accountRegion = manifest.getAccountRegion();
            if(accountId == null || accountToken == null) {
                Logger.i("Account ID or Account token is missing from AndroidManifest.xml, unable to create default instance");
                return null;
            }
            if (accountRegion == null) {
                Logger.i("Account Region not specified in the AndroidManifest - using default region");
            }
            defaultConfig = CleverTapInstanceConfig.createDefaultInstance(context, accountId, accountToken, accountRegion);
            defaultConfig.setDebugLevel(getDebugLevel());
        }
        return instanceWithConfig(context, defaultConfig);
    }

    /**
     * Returns an instance of the CleverTap SDK using CleverTapInstanceConfig.
     *
     * @param context The Android context
     * @param config The {@link CleverTapInstanceConfig} object
     * @return The {@link CleverTapAPI} object
     */
    public static CleverTapAPI instanceWithConfig(Context context, @NonNull CleverTapInstanceConfig config){
        //noinspection ConstantConditions
        if (config == null) {
            Logger.v("CleverTapInstanceConfig cannot be null");
            return null;
        }
        if (instances == null) {
            instances = new HashMap<>();
        }
        CleverTapAPI instance = instances.get(config.getAccountId());
        if (instance == null){
            instance = new CleverTapAPI(context, config);
            instances.put(config.getAccountId(), instance);
        }
        return instance;
    }

    //Lifecycle
    private void activityPaused() {
        setAppForeground(false);
        appLastSeen = System.currentTimeMillis();
        getConfigLogger().verbose(getAccountId(), "App in background");
        final int now = (int) (System.currentTimeMillis() / 1000);
        if (inCurrentSession()) {
            try {
                StorageHelper.putInt(context, storageKeyWithSuffix(Constants.LAST_SESSION_EPOCH), now);
                getConfigLogger().verbose(getAccountId(),"Updated session time: "+now);
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(),"Failed to update session time time: " + t.getMessage());
            }
        }
    }

    // SessionManager/session management
    private void checkTimeoutSession() {
        if(appLastSeen <= 0) return;
        long now = System.currentTimeMillis();
        if ((now - appLastSeen) > Constants.SESSION_LENGTH_MINS * 60 * 1000) {
            getConfigLogger().verbose(getAccountId(), "Session Timed Out");
            destroySession();
            setCurrentActivity(null);
        }
    }

    /**
     * Destroys the current session
     */
    private void destroySession() {
        currentSessionId = 0;
        setAppLaunchPushed(false);
        getConfigLogger().verbose(getAccountId(),"Session destroyed; Session ID is now 0");
        clearSource();
        clearMedium();
        clearCampaign();
        clearWzrkParams();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean inCurrentSession(){
        return currentSessionId > 0;
    }

    //Lifecycle
    private void activityResumed(Activity activity) {
        getConfigLogger().verbose(getAccountId(), "App in foreground");
        checkTimeoutSession();
        if (!inCurrentSession()) {
            onTokenRefresh();
            pushAppLaunchedEvent();
            pushInitialEventsAsync();
        }
        checkPendingInAppNotifications(activity);
    }

    //Event
    private void pushAppLaunchedEvent() {
        if (isAppLaunchReportingDisabled()) {
            setAppLaunchPushed(true);
            getConfigLogger().debug(getAccountId(), "App Launched Events disabled in the Android Manifest file");
            return;
        }
        if (isAppLaunchPushed()) {
            getConfigLogger().verbose(getAccountId(), "App Launched has already been triggered. Will not trigger it ");
            return;
        } else {
            getConfigLogger().verbose(getAccountId(), "Firing App Launched event");
        }
        setAppLaunchPushed(true);
        JSONObject event = new JSONObject();
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT);
            event.put("evtData", getAppLaunchedFields());
        } catch (Throwable t) {
            // We won't get here
        }
        queueEvent(context, event, Constants.RAISED_EVENT);
    }

    private void setAppLaunchPushed(boolean pushed) {
        synchronized (appLaunchPushedLock) {
            appLaunchPushed = pushed;
        }
    }

    private boolean isAppLaunchPushed() {
        synchronized (appLaunchPushedLock) {
            return appLaunchPushed;
        }
    }

    private boolean isAppLaunchReportingDisabled() {
        return this.config.isDisableAppLaunchedEvent();
    }

    private static Activity getCurrentActivity() {
        return (currentActivity == null) ? null : currentActivity.get();
    }

    private static void setCurrentActivity(@Nullable Activity activity) {
        if (activity == null) {
            currentActivity = null;
            return;
        }
        if (!activity.getLocalClassName().contains("InAppNotificationActivity")) {
            currentActivity = new WeakReference<>(activity);
        }
    }

    private static String getCurrentActivityName() {
        Activity current = getCurrentActivity();
        return (current != null) ? current.getLocalClassName() : null;
    }

    private void checkPendingInAppNotifications(Activity activity){
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (pendingInappRunnable != null) {
                getConfigLogger().verbose(getAccountId(), "Found a pending inapp runnable. Scheduling it");
                getHandlerUsingMainLooper().postDelayed(pendingInappRunnable, 200);
                pendingInappRunnable = null;
            } else {
                showNotificationIfAvailable(context);
            }
        } else {
            Logger.d("In-app notifications will not be shown for this activity ("
                    + (activity != null ? activity.getLocalClassName() : "") + ")");
        }
    }

    //Event
    private void pushInitialEventsAsync() {
        postAsyncSafely("CleverTapAPI#pushInitialEventsAsync", new Runnable() {
            @Override
            public void run() {
                try {
                    getConfigLogger().verbose(getAccountId(), "Queuing daily events");
                    pushBasicProfile(null);
                } catch (Throwable t) {
                    getConfigLogger().verbose(getAccountId(), "Daily profile sync failed", t);
                }
            }
        });
    }

    //Push
    static void tokenRefresh(Context context){
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if(instance != null) {
                instance.onTokenRefresh();
            }
            return;
        }
        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance.getConfig().isAnalyticsOnly()) {
                Logger.d(accountId, "Instance is Analytics Only not processing device token");
                continue;
            }
            instance.onTokenRefresh();
        }
    }

    //Push
    private void onTokenRefresh() {
        if (enabledPushTypes == null) {
            enabledPushTypes = this.deviceInfo.getEnabledPushTypes();
        }
        if (enabledPushTypes == null) return;
        for (PushType pushType : enabledPushTypes) {
            switch (pushType) {
                case GCM:
                    doGCMRefresh();
                    break;
                case FCM:
                    doFCMRefresh();
                    break;
                default:
                    //no-op
                    break;
            }
        }
    }

    //Push
    private void doFCMRefresh() {
        postAsyncSafely("FcmManager#doFCMRefresh", new Runnable() {
            @Override
            public void run() {
                try {
                    if(getConfig().isAnalyticsOnly()){
                        getConfigLogger().debug(getAccountId(),"Instance is set for Analytics only, not refreshing token");
                        return;
                    }

                    String freshToken = FCMGetFreshToken();
                    if (freshToken == null) return;

                    cacheFCMToken(freshToken);

                    // better safe to always force a push from here
                    pushFCMDeviceToken(freshToken, true, true);

                    try {
                        deviceTokenDidRefresh(freshToken, PushType.FCM);
                    } catch (Throwable t) {
                        //no-op
                    }
                } catch (Throwable t) {
                    getConfigLogger().verbose(getAccountId(), "FcmManager: FCM Token error", t);
                }
            }
        });
    }

    //Push
    private void doGCMRefresh() {
        final DeviceInfo _deviceInfo = this.deviceInfo;
        postAsyncSafely("GcmManager#doGCMRefresh", new Runnable() {
            @Override
            public void run() {
                try {
                    if(getConfig().isAnalyticsOnly()){
                        getConfigLogger().debug(getAccountId(),"Instance is set for Analytics only, will not request push token");
                        return;
                    }
                    String freshToken = GCMGetFreshToken(_deviceInfo.getGCMSenderID());
                    if (freshToken == null) return;

                    cacheGCMToken(freshToken);

                    // better safe to always force a push from here
                    pushGCMDeviceToken(freshToken, true, true);

                    try {
                        deviceTokenDidRefresh(freshToken, PushType.GCM);
                    } catch (Throwable t) {
                        //no-op
                    }
                } catch (Throwable t) {
                    getConfigLogger().verbose(getAccountId(),"GcmManager: GCM Token error", t);
                }
            }
        });
    }

    //Push
    /**
     * request token from FCM
     */
    private String FCMGetFreshToken() {
        getConfigLogger().verbose(getAccountId(), "FcmManager: Requesting a FCM token");
        String token = null;
        try {
            token = FirebaseInstanceId.getInstance().getToken();
            getConfigLogger().info(getAccountId(),"FCM token: "+token);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "FcmManager: Error requesting FCM token", t);
        }
        return token;
    }

    //Push
    /**
     * request token from GCM
     */
    private String GCMGetFreshToken(final String senderID) {
        getConfigLogger().verbose(getAccountId(), "GcmManager: Requesting a GCM token for Sender ID - " + senderID);
        String token = null;
        try {
            token = InstanceID.getInstance(context)
                    .getToken(senderID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            getConfigLogger().info(getAccountId(), "GCM token : " + token);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "GcmManager: Error requesting GCM token", t);
        }
        return token;
    }

    //Push
    private void cacheFCMToken(String token) {
        try {
            if (token == null || alreadyHaveFCMToken(token)) return;

            final SharedPreferences prefs = getPreferences();
            if (prefs == null) return;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(storageKeyWithSuffix(Constants.FCM_PROPERTY_REG_ID), token);
            StorageHelper.persist(editor);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "FcmManager: Unable to cache FCM Token", t);
        }
    }

    //Push
    private void cacheGCMToken(String token) {
        try {
            if (token == null || alreadyHaveGCMToken(token)) return;

            final SharedPreferences prefs = getPreferences();
            if (prefs == null) return;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(storageKeyWithSuffix(Constants.GCM_PROPERTY_REG_ID), token);
            StorageHelper.persist(editor);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "GcmManager: Unable to cache GCM Token", t);
        }
    }

    //Push
    private boolean alreadyHaveFCMToken(final String newToken) {
        if (newToken == null) return false;
        String cachedToken = getCachedFCMToken();
        return (cachedToken != null && cachedToken.equals(newToken));
    }

    //Push
    private boolean alreadyHaveGCMToken(final String newToken) {
        if (newToken == null) return false;
        String cachedToken = getCachedGCMToken();
        return (cachedToken != null && cachedToken.equals(newToken));
    }

    //Push
    private String getCachedFCMToken() {
        SharedPreferences prefs = getPreferences();
        return (prefs == null) ? null : getStringFromPrefs(Constants.FCM_PROPERTY_REG_ID, null);
    }

    //Push
    private String getCachedGCMToken() {
        SharedPreferences prefs = getPreferences();
        return (prefs == null) ? null : getStringFromPrefs(Constants.GCM_PROPERTY_REG_ID, null);
    }

    //Preferences
    private String storageKeyWithSuffix(String key){
        return key+":"+getConfig().getAccountId();
    }

    private SharedPreferences getPreferences() {
        try {
            return (context == null) ? null : StorageHelper.getPreferences(context);
        } catch (Throwable t) {
            return null;
        }
    }

    private String getStringFromPrefs(String rawKey, String defaultValue){
        if (this.config.isDefaultInstance()) {
            String _new = StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
            //noinspection ConstantConditions
            return _new != null ? _new : StorageHelper.getString(this.context, rawKey, defaultValue);
        } else {
            return StorageHelper.getString(this.context, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private int getIntFromPrefs(String rawKey, int defaultValue){
        if (this.config.isDefaultInstance()) {
            int dummy = -1000;
            int _new = StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), dummy);
            return _new != dummy ? _new : StorageHelper.getInt(this.context, rawKey, defaultValue);
        } else {
            return StorageHelper.getInt(this.context, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    private boolean getBooleanFromPrefs(String rawKey){
        if(this.config.isDefaultInstance()){
            boolean _new = StorageHelper.getBoolean(this.context,storageKeyWithSuffix(rawKey),false);
            //noinspection ConstantConditions
            return !_new ? StorageHelper.getBoolean(this.context,rawKey,false) : _new;
        }
        else{
            return StorageHelper.getBoolean(this.context, storageKeyWithSuffix(rawKey), false);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private long getLongFromPrefs(String rawKey, int defaultValue, String nameSpace){
        if (this.config.isDefaultInstance()) {
            long dummy = -1000;
            long _new = StorageHelper.getLong(this.context, nameSpace, storageKeyWithSuffix(rawKey), dummy);
            return _new != dummy ? _new : StorageHelper.getLong(this.context, nameSpace, rawKey, defaultValue);
        } else {
            return StorageHelper.getLong(this.context, nameSpace, storageKeyWithSuffix(rawKey), defaultValue);
        }
    }

    //Push
    private void pushGCMDeviceToken(String token, final boolean register, final boolean forceUpdate) {
        synchronized (tokenLock) {
            if (havePushedDeviceToken && !forceUpdate) {
                getConfigLogger().debug(getAccountId(), "GcmManager: skipping device token push - already sent.");
                return;
            }

            try {
                token = (token != null) ? token : getCachedGCMToken();
                if (token == null) return;
                pushDeviceToken(context, token, register, PushType.GCM);
                havePushedDeviceToken = true;
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "GcmManager: pushing device token failed", t);
            }
        }
    }

    //Push
    private void pushFCMDeviceToken(String token, final boolean register, final boolean forceUpdate) {
        synchronized (tokenLock) {
            if (havePushedDeviceToken && !forceUpdate) {
                getConfigLogger().verbose(getAccountId(), "FcmManager: skipping device token push - already sent.");
                return;
            }

            try {
                token = (token != null) ? token : getCachedFCMToken();
                if (token == null) return;
                pushDeviceToken(context, token, register, PushType.FCM);
                havePushedDeviceToken = true;
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "FcmManager: pushing device token failed", t);
            }
        }
    }

    //Push
    private void deviceTokenDidRefresh(String token, PushType type) {
        if (tokenRefreshListener != null) {
            getConfigLogger().debug(getAccountId(), "Notifying devicePushTokenDidRefresh: " + token);
            tokenRefreshListener.devicePushTokenDidRefresh(token, type);
        }
    }

    //Push
    /**
     * Implement to get called back when the device push token is refreshed
     */
    public interface DevicePushTokenRefreshListener {
        /**
         * @param token the device token
         * @param type  the token type com.clevertap.android.sdk.PushType (FCM or GCM)
         */
        void devicePushTokenDidRefresh(String token, PushType type);
    }


    //Debug
    /**
     * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
     * Debug messages are tagged as CleverTap.
     *
     * @param level Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK integration related logging),
     *              1(shows debug output)
     */
    @SuppressWarnings("WeakerAccess")
    public static void setDebugLevel(int level) {
        debugLevel = level;
    }

    /**
     * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
     * Debug messages are tagged as CleverTap.
     *
     * @param level Can be one of the following: LogLevel.OFF (disables all debugging), LogLevel.INFO (default, shows minimal SDK integration related logging),
     *              LogLevel.DEBUG(shows debug output)
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void setDebugLevel(LogLevel level) {
        debugLevel = level.intValue();
    }

    /**
     * Returns the log level set for CleverTapAPI
     * @return The {@link CleverTapAPI.LogLevel} int value
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    //Validation
    private void pushValidationResult(ValidationResult vr) {
        synchronized (pendingValidationResultsLock) {
            try {
                int len = pendingValidationResults.size();
                if (len > 50) {
                    ArrayList<ValidationResult> trimmed = new ArrayList<>();
                    // Trim down the list to 40, so that this loop needn't run for the next 10 events
                    // Hence, skip the first 10 elements
                    for (int i = 10; i < len; i++)
                        trimmed.add(pendingValidationResults.get(i));
                    trimmed.add(vr);
                    pendingValidationResults = trimmed;
                } else {
                    pendingValidationResults.add(vr);
                }
            } catch (Exception e) {
                // no-op
            }
        }
    }

    /**
     * If you want to stop recorded events from being sent to the server, use this method to set the SDK instance to offline.
     * Once offline, events will be recorded and queued locally but will not be sent to the server until offline is disabled.
     * Calling this method again with offline set to false will allow events to be sent to server and the SDK instance will immediately attempt to send events that have been queued while offline.
     *
     * @param value boolean, true sets the sdk offline, false sets the sdk back online
     *
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setOffline(boolean value){
        offline = value;
        if (offline) {
            getConfigLogger().debug(getAccountId(), "CleverTap Instance has been set to offline, won't send events queue");
        } else {
            getConfigLogger().debug(getAccountId(), "CleverTap Instance has been set to online, sending events queue");
            flush();
        }
    }

    private boolean isOffline(){
        return offline;
    }

    //Network Info handling
    /**
     * Use this method to enable device network-related information tracking, including IP address.
     * This reporting is disabled by default.  To re-disable tracking call this method with enabled set to false.
     *
     @param value  boolean Whether device network info reporting should be enabled/disabled.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void enableDeviceNetworkInfoReporting(boolean value){
        enableNetworkInfoReporting = value;
        StorageHelper.putBoolean(context,storageKeyWithSuffix(Constants.NETWORK_INFO),enableNetworkInfoReporting);
        getConfigLogger().verbose(getAccountId(), "Device Network Information reporting set to " + enableNetworkInfoReporting);
    }

    private void setDeviceNetworkInfoReportingFromStorage(){
        boolean enabled = getBooleanFromPrefs(Constants.NETWORK_INFO);
        getConfigLogger().verbose(getAccountId(), "Setting device network info reporting state from storage to " + enabled);
        enableNetworkInfoReporting = enabled;
    }

    //Run manifest validation in async
    private void manifestAsyncValidation(){
        postAsyncSafely("Manifest Validation", new Runnable() {
            @Override
            public void run() {
                ManifestValidator.validate(context, deviceInfo);
            }
        });
    }

    // OptOut handling

    private boolean isCurrentUserOptedOut() {
        synchronized (optOutFlagLock) {
            return currentUserOptedOut;
        }
    }

    private void setCurrentUserOptedOut(boolean enable) {
        synchronized (optOutFlagLock) {
            currentUserOptedOut = enable;
        }
    }

    /**
     * Use this method to opt the current user out of all event/profile tracking.
     * You must call this method separately for each active user profile (e.g. when switching user profiles using onUserLogin).
     * Once enabled, no events will be saved remotely or locally for the current user. To re-enable tracking call this method with enabled set to false.
     * @param userOptOut boolean Whether tracking opt out should be enabled/disabled.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setOptOut(boolean userOptOut){
        final boolean enable = userOptOut;
        postAsyncSafely("setOptOut", new Runnable() {
            @Override
            public void run() {
                // generate the data for a profile push to alert the server to the optOut state change
                HashMap<String, Object> optOutMap = new HashMap<>();
                optOutMap.put(Constants.CLEVERTAP_OPTOUT, enable);

                // determine order of operations depending on enabled/disabled
                if (enable) {  // if opting out first push profile event then set the flag
                    pushProfile(optOutMap);
                    setCurrentUserOptedOut(true);
                } else {  // if opting back in first reset the flag to false then push the profile event
                    setCurrentUserOptedOut(false);
                    pushProfile(optOutMap);
                }
                // persist the new optOut state
                String key = optOutKey();
                if (key == null) {
                    getConfigLogger().verbose(getAccountId(), "Unable to persist user OptOut state, storage key is null");
                    return;
                }
                StorageHelper.putBoolean(context, storageKeyWithSuffix(key), enable);
                getConfigLogger().verbose(getAccountId(), "Set current user OptOut state to: " + enable);
            }
        });
    }

    private String optOutKey() {
        String guid = getCleverTapID();
        if (guid == null) {
            return null;
        }
        return "OptOut:"+guid;
    }

    private void setCurrentUserOptOutStateFromStorage() {
        String key = optOutKey();
        if (key == null) {
            getConfigLogger().verbose(getAccountId(), "Unable to set current user OptOut state from storage: storage key is null");
            return;
        }
        boolean storedOptOut = getBooleanFromPrefs(key);
        setCurrentUserOptedOut(storedOptOut);
        getConfigLogger().verbose(getAccountId(), "Set current user OptOut state from storage to: " + storedOptOut + " for key: " + key);
    }

    //Util
    /**
     * Returns a unique identifier by which CleverTap identifies this user.
     *
     * @return The user identifier currently being used to identify this user.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getCleverTapID() {
        return this.deviceInfo.getDeviceID();
    }

    /**
     * Returns a unique CleverTap identifier suitable for use with install attribution providers.
     *
     * @return The attribution identifier currently being used to identify this user.
     */
    @SuppressWarnings("unused")
    public String getCleverTapAttributionIdentifier() {
        return this.deviceInfo.getAttributionID();
    }

    /**
     * Returns the device push token or null
     *
     * @param type com.clevertap.android.sdk.PushType (FCM or GCM)
     * @return String device token or null
     * NOTE: on initial install calling getDevicePushToken may return null, as the device token is
     * not yet available
     * Implement CleverTapAPI.DevicePushTokenRefreshListener to get a callback once the token is
     * available
     */
    @SuppressWarnings("unused")
    public String getDevicePushToken(final PushType type) {
        switch (type) {
            case GCM:
                return getCachedGCMToken();
            case FCM:
                return getCachedFCMToken();
            default:
                return null;
        }
    }

    //Util
    /**
     * Returns whether or not the app is in the foreground.
     *
     * @return The foreground status
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isAppForeground() {
        return appForeground;
    }

    //Util

    /**
     * Use this method to notify CleverTap that the app is in foreground
     * @param appForeground boolean true/false
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void setAppForeground(boolean appForeground) {
        CleverTapAPI.appForeground = appForeground;
    }

    //DeepLink
    /**
     * Use this method to pass the deeplink with UTM parameters to track installs
     * @param uri URI of the deeplink
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushDeepLink(Uri uri)  {
        pushDeepLink(uri, false);
    }

    private synchronized void pushDeepLink(Uri uri, boolean install) {
        if (uri == null)
            return;

        try {
            JSONObject referrer = UriHelper.getUrchinFromUri(uri);
            if(referrer.has("us"))
                setSource(referrer.get("us").toString());
            if(referrer.has("um"))
                setMedium(referrer.get("um").toString());
            if(referrer.has("uc"))
                setCampaign(referrer.get("uc").toString());

            referrer.put("referrer", uri.toString());
            if (install) {
                referrer.put("install", true);
            }
            recordPageEventWithExtras(referrer);

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to push deep link", t);
        }
    }

    //Event
    private void recordPageEventWithExtras(JSONObject extras) {
        try{
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
            queueEvent(context, jsonObject, Constants.PAGE_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    private boolean shouldDeferProcessingEvent(int eventType){
        //noinspection SimplifiableIfStatement
        if (getConfig().isCreatedPostAppLaunch()){
            return false;
        }
        return (eventType == Constants.RAISED_EVENT && !isAppLaunchPushed());
    }

    //Event
    private void queueEvent(final Context context, final JSONObject event, final int eventType) {
        postAsyncSafely("queueEvent", new Runnable() {
            @Override
            public void run() {
                if (isCurrentUserOptedOut()) {
                    String eventString = event == null ? "null" : event.toString();
                    getConfigLogger().debug(getAccountId(), "Current user is opted out dropping event: " + eventString);
                    return;
                }
                if(shouldDeferProcessingEvent(eventType)){
                    getConfigLogger().debug(getAccountId(),"App Launched not yet processed, re-queuing event "+ event);
                    getHandlerUsingMainLooper().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            queueEvent(context,event,eventType);
                        }
                    },300);
                    return;
                }
                lazyCreateSession(context);
                addToQueue(context, event, eventType);
            }
        });
    }

    //Session
    private void lazyCreateSession(Context context) {
        if (!inCurrentSession()) {
            pushAppLaunchedEvent();
            createSession(context);
            pushInitialEventsAsync();
        }
    }

    private void createSession(final Context context) {
        currentSessionId = (int) (System.currentTimeMillis() / 1000);

        getConfigLogger().verbose(getAccountId(), "Session created with ID: " + currentSessionId);

        SharedPreferences prefs = StorageHelper.getPreferences(context);

        final int lastSessionID = getIntFromPrefs(Constants.SESSION_ID_LAST, 0);
        final int lastSessionTime = getIntFromPrefs(Constants.LAST_SESSION_EPOCH, 0);
        if (lastSessionTime > 0) {
            lastSessionLength = lastSessionTime - lastSessionID;
        }

        getConfigLogger().verbose(getAccountId(), "Last session length: " + lastSessionLength + " seconds");

        if (lastSessionID == 0) {
            firstSession = true;
        }

        final SharedPreferences.Editor editor = prefs.edit().putInt(storageKeyWithSuffix(Constants.SESSION_ID_LAST), currentSessionId);
        StorageHelper.persist(editor);
    }

    private int getCurrentSession() {
        return currentSessionId;
    }

    //Event
    /**
     * Adds a new event to the queue, to be sent later.
     *
     * @param context   The Android context
     * @param event     The event to be queued
     * @param eventType The type of event to be queued
     */

    // only call async
    private void addToQueue(final Context context, final JSONObject event, final int eventType) {
        if (isMuted()) {
            return;
        }
        processEvent(context, event, eventType);
    }

    //Util
    /**
     * @return true if the mute command was sent anytime between now and now - 24 hours.
     */
    private boolean isMuted() {
        final int now = (int) (System.currentTimeMillis() / 1000);
        final int muteTS = getIntFromPrefs(Constants.KEY_MUTED, 0);

        return now - muteTS < 24 * 60 * 60;
    }

    //Event
    private void processEvent(final Context context, final JSONObject event, final int eventType) {
        synchronized (eventLock) {
            try {
                activityCount = activityCount == 0 ? 1 : activityCount;
                String type;
                if (eventType == Constants.PAGE_EVENT) {
                    type = "page";
                } else if (eventType == Constants.PING_EVENT) {
                    type = "ping";
                    attachMeta(event, context);
                } else if (eventType == Constants.PROFILE_EVENT) {
                    type = "profile";
                } else if (eventType == Constants.DATA_EVENT) {
                    type = "data";
                } else {
                    type = "event";
                }

                // Complete the received event with the other params

                String currentActivityName = getScreenName();
                if (currentActivityName != null) {
                    event.put("n", currentActivityName);
                }

                int session = getCurrentSession();
                event.put("s", session);
                event.put("pg", activityCount);
                event.put("type", type);
                event.put("ep", System.currentTimeMillis() / 1000);
                event.put("f", isFirstSession());
                event.put("lsl", getLastSessionLength());
                attachPackageNameIfRequired(context, event);

                // Report any pending validation error
                ValidationResult vr = popValidationResult();
                if (vr != null) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                getLocalDataStore().setDataSyncFlag(event);
                queueEventToDB(context, event, eventType);
                updateLocalStore(context, event, eventType);
                scheduleQueueFlush(context);

            } catch (Throwable e) {
                getConfigLogger().verbose(getAccountId(), "Failed to queue event: " + event.toString(), e);
            }
        }
    }

    //Session
    private int getLastSessionLength() {
        return lastSessionLength;
    }

    //Session
    private boolean isFirstSession() {
        return firstSession;
    }

    //Session
    private void attachPackageNameIfRequired(final Context context, final JSONObject event) {
        try {
            final String type = event.getString("type");
            // Send it only for app launched events
            if ("event".equals(type) && Constants.APP_LAUNCHED_EVENT.equals(event.getString("evtName"))) {
                event.put("pai", context.getPackageName());
            }
        } catch (Throwable t) {
            // Ignore
        }
    }

    //Util
    /**
     * Attaches meta info about the current state of the device to an event.
     * Typically, this meta is added only to the ping event.
     */
    private void attachMeta(final JSONObject o, final Context context) {
        // Memory consumption
        try {
            o.put("mc", Utils.getMemoryConsumption());
        } catch (Throwable t) {
            // Ignore
        }

        // Attach the network type
        try {
            o.put("nt", Utils.getCurrentNetworkType(context));
        } catch (Throwable t) {
            // Ignore
        }
    }

    /**
     * Record a Screen View event
     * @param screenName String, the name of the screen
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void recordScreen(String screenName){
        if(screenName == null || (!currentScreenName.isEmpty() && currentScreenName.equals(screenName))) return;
        getConfigLogger().debug(getAccountId(), "Screen changed to " + screenName);
        currentScreenName = screenName;
        recordPageEventWithExtras(null);
    }

    private String getScreenName(){
        return currentScreenName.equals("") ? null : currentScreenName;
    }

    //Validation
    private ValidationResult popValidationResult() {
        // really a shift
        ValidationResult vr = null;

        synchronized (pendingValidationResultsLock) {
            try {
                if (!pendingValidationResults.isEmpty()) {
                    vr = pendingValidationResults.remove(0);
                }
            } catch (Exception e) {
                // no-op
            }
        }
        return vr;
    }

    //Validation
    private JSONObject getErrorObject(ValidationResult vr) {
        JSONObject error = new JSONObject();
        try {
            error.put("c", vr.getErrorCode());
            error.put("d", vr.getErrorDesc());
        } catch (JSONException e) {
            // Won't reach here
        }
        return error;
    }

    /**
     * Enables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void enablePersonalization() {
        this.config.enablePersonalization(true);
    }

    /**
     * Disables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void disablePersonalization() {
        this.config.enablePersonalization(false);
    }

    //Event
    private void queueEventToDB(final Context context, final JSONObject event, final int type) {
        synchronized (eventLock) {
            DBAdapter adapter = loadDBAdapter(context);
            DBAdapter.Table table = (type == Constants.PROFILE_EVENT) ? DBAdapter.Table.PROFILE_EVENTS : DBAdapter.Table.EVENTS;

            int returnCode = adapter.storeObject(event, table);

            if (returnCode > 0) {
                getConfigLogger().debug(getAccountId(),"Queued event: " + event.toString());
                getConfigLogger().verbose(getAccountId(), "Queued event to DB table " + table + ": " + event.toString());
            }
        }
    }

    //Util
    private DBAdapter loadDBAdapter(Context context) {
        if (dbAdapter == null) {
            dbAdapter = new DBAdapter(context,this.config);
            dbAdapter.cleanupStaleEvents(DBAdapter.Table.EVENTS);
            dbAdapter.cleanupStaleEvents(DBAdapter.Table.PROFILE_EVENTS);
        }
        return dbAdapter;
    }

    //Util
    // only call async
    private void updateLocalStore(final Context context, final JSONObject event, final int type) {
        if (type == Constants.RAISED_EVENT) {
            getLocalDataStore().persistEvent(context, event, type);
        }
    }

    //Event
    private void scheduleQueueFlush(final Context context) {
        if (commsRunnable == null)
            commsRunnable = new Runnable() {
                @Override
                public void run() {
                    flushQueueAsync(context);
                }
            };
        // Cancel any outstanding send runnables, and issue a new delayed one
        getHandlerUsingMainLooper().removeCallbacks(commsRunnable);
        getHandlerUsingMainLooper().postDelayed(commsRunnable, Constants.PUSH_DELAY_MS);

        getConfigLogger().verbose(getAccountId(), "Scheduling delayed queue flush on main event loop");
    }

    private void flushQueueAsync(final Context context) {
        postAsyncSafely("CommsManager#flushQueueAsync", new Runnable() {
            @Override
            public void run() {
                flushQueueSync(context);
            }
        });
    }

    private void flushQueueSync(final Context context) {
        if (!isNetworkOnline(context)) {
            getConfigLogger().verbose(getAccountId(), "Network connectivity unavailable. Will retry later");
            return;
        }

        if (isOffline()){
            getConfigLogger().debug(getAccountId(), "CleverTap Instance has been set to offline, won't send events queue");
            return;
        }

        if (needsHandshakeForDomain()) {
            mResponseFailureCount = 0;
            setDomain(context, null);
            performHandshakeForDomain(context, new Runnable() {
                @Override
                public void run() {
                    flushDBQueue(context);
                }
            });
        } else {
            flushDBQueue(context);
        }
    }

    //Util
    private boolean isNetworkOnline(Context context) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null ) {
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

    //Networking
    private boolean needsHandshakeForDomain() {
        final String domain = getDomainFromPrefsOrMetadata();
        return domain == null || mResponseFailureCount > 5;
    }

    private String getDomainFromPrefsOrMetadata() {
        try {
            final String region = this.config.getAccountRegion();
            if (region != null && region.trim().length() > 0) {
                // Always set this to 0 so that the handshake is not performed during a HTTP failure
                mResponseFailureCount = 0;
                return region.trim().toLowerCase() + "." + Constants.PRIMARY_DOMAIN;
            }
        } catch (Throwable t) {
            // Ignore
        }
        return getStringFromPrefs(Constants.KEY_DOMAIN_NAME, null);
    }

    private void setDomain(final Context context, String domainName) {
        getConfigLogger().verbose(getAccountId(), "Setting domain to " + domainName);
        StorageHelper.putString(context, storageKeyWithSuffix(Constants.KEY_DOMAIN_NAME), domainName);
    }

    private void performHandshakeForDomain(final Context context, final Runnable handshakeSuccessCallback) {
        if (isMuted()) {
            return;
        }

        final String endpoint = getEndpoint(true);
        if (endpoint == null) {
            getConfigLogger().verbose(getAccountId(), "Unable to perform handshake, endpoint is null");
        }
        getConfigLogger().verbose(getAccountId(), "Performing handshake with " + endpoint);

        HttpsURLConnection conn = null;
        try {
            conn = buildHttpsURLConnection(endpoint);
            final int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                getConfigLogger().verbose(getAccountId(), "Invalid HTTP status code received for handshake - " + responseCode);
                return;
            }

            getConfigLogger().verbose(getAccountId(), "Received success from handshake :)");

            if (processIncomingHeaders(context, conn)) {
                getConfigLogger().verbose(getAccountId(), "We are not muted");
                // We have a new domain, run the callback
                handshakeSuccessCallback.run();
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to perform handshake!", t);
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

    private String getEndpoint(final boolean defaultToHandshakeURL) {
        String domain = getDomain(defaultToHandshakeURL);
        if (domain == null) {
            getConfigLogger().verbose(getAccountId(), "Unable to configure endpoint, domain is null");
            return null;
        }

        final String accountId = getAccountId();
        if (accountId == null) {
            getConfigLogger().verbose(getAccountId(), "Unable to configure endpoint, accountID is null");
            return null;
        }

        String endpoint = "https://" + domain + "?os=Android&t=" + this.deviceInfo.getSdkVersion();
        endpoint += "&z=" + accountId;

        final boolean needsHandshake = needsHandshakeForDomain();
        // Don't attach ts if its handshake
        if (needsHandshake) {
            return endpoint;
        }

        currentRequestTimestamp = (int) (System.currentTimeMillis() / 1000);
        endpoint += "&ts=" + currentRequestTimestamp;

        return endpoint;
    }

    private HttpsURLConnection buildHttpsURLConnection(final String endpoint)
            throws IOException {

        URL url = new URL(endpoint);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("X-CleverTap-Account-ID", getAccountId());
        conn.setRequestProperty("X-CleverTap-Token", this.config.getAccountToken());
        conn.setInstanceFollowRedirects(false);
        if(this.config.isSslPinningEnabled()){
            SSLContext _sslContext = getSSLContext();
            if(_sslContext!=null)
                conn.setSSLSocketFactory(getPinnedCertsSslSocketfactory(_sslContext));
        }
        return conn;
    }

    private static synchronized SSLContext getSSLContext(){
        if(sslContext == null) {
            sslContext = new SSLContextBuilder().build();
        }
        return sslContext;
    }

    private static SSLSocketFactory getPinnedCertsSslSocketfactory(SSLContext sslContext){
        if(sslContext == null) return null;

        if(sslSocketFactory == null){
            try{
                sslSocketFactory = sslContext.getSocketFactory();
                Logger.d("Pinning SSL session to DigiCertGlobalRoot CA certificate");
            }catch (Throwable e){
                Logger.d("Issue in pinning SSL,", e);
            }
        }
        return  sslSocketFactory;
    }

    private String getDomain(boolean defaultToHandshakeURL) {
        String domain = getDomainFromPrefsOrMetadata();

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

    /**
     * Processes the incoming response headers for a change in domain and/or mute.
     *
     * @return True to continue sending requests, false otherwise.
     */
    private boolean processIncomingHeaders(final Context context,final HttpsURLConnection conn) {

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
        if (domainName == null || domainName.trim().length() == 0) {
            return true;
        }

        setMuted(context, false);
        setDomain(context, domainName);
        return true;
    }

    //Util
    private void setMuted(final Context context, boolean mute) {
        if (mute) {
            final int now = (int) (System.currentTimeMillis() / 1000);
            StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_MUTED), now);
            setDomain(context, null);

            // Clear all the queues
            postAsyncSafely("CommsManager#setMuted", new Runnable() {
                @Override
                public void run() {
                    clearQueues(context);
                }
            });
        } else {
            StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_MUTED), 0);
        }
    }

    //Session
    /**
     * Only call async
     */
    private void clearQueues(final Context context) {
        synchronized (eventLock) {
            DBAdapter adapter = loadDBAdapter(context);
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
        _clearARP(context);
        clearFirstRequestTimestampIfNeeded(context);
        clearLastRequestTimestamp(context);
    }

    //Session
    private void clearIJ(Context context) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        StorageHelper.persist(editor);
    }

    //Session
    private void _clearARP(Context context) {
        final String nameSpaceKey = getNamespaceARPKey();
        if (nameSpaceKey == null) return;

        final SharedPreferences prefs = StorageHelper.getPreferences(context, nameSpaceKey);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        StorageHelper.persist(editor);
    }

    //Session
    private String getNamespaceARPKey() {

        final String accountId = getAccountId();
        if (accountId == null) return null;

        return "ARP:"+ accountId;
    }

    //Session
    private void clearFirstRequestTimestampIfNeeded(Context context) {
        StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_FIRST_TS), 0);
    }

    //Session
    private void clearLastRequestTimestamp(Context context) {
        StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_LAST_TS), 0);
    }

    //Event
    private void flushDBQueue(final Context context) {
        getConfigLogger().verbose(getAccountId(), "Somebody has invoked me to send the queue to CleverTap servers");

        QueueCursor cursor;
        QueueCursor previousCursor = null;
        boolean loadMore = true;
        while (loadMore) {
            cursor = getQueuedEvents(context, 50, previousCursor);

            if (cursor == null || cursor.isEmpty()) {
                getConfigLogger().verbose(getAccountId(), "No events in the queue, bailing");
                break;
            }

            previousCursor = cursor;
            JSONArray queue = cursor.getData();

            if (queue == null || queue.length() <= 0) {
                getConfigLogger().verbose(getAccountId(), "No events in the queue, bailing");
                break;
            }

            loadMore = sendQueue(context, queue);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private QueueCursor getQueuedEvents(final Context context, final int batchSize, final QueueCursor previousCursor) {
        return getQueuedDBEvents(context, batchSize, previousCursor);
    }

    private QueueCursor getQueuedDBEvents(final Context context, final int batchSize, final QueueCursor previousCursor) {

        synchronized (eventLock) {
            DBAdapter adapter = loadDBAdapter(context);
            DBAdapter.Table tableName = (previousCursor != null) ? previousCursor.getTableName() : DBAdapter.Table.EVENTS;

            // if previousCursor that means the batch represented by the previous cursor was processed so remove those from the db
            if (previousCursor != null) {
                adapter.cleanupEventsFromLastId(previousCursor.getLastId(), previousCursor.getTableName());
            }

            // grab the new batch
            QueueCursor newCursor = new QueueCursor();
            newCursor.setTableName(tableName);
            JSONObject queuedDBEvents = adapter.fetchEvents(tableName, batchSize);
            newCursor = updateCursorForDBObject(queuedDBEvents, newCursor);

            // if we have no events then try and fetch profile events
            if (newCursor.isEmpty() && tableName.equals(DBAdapter.Table.EVENTS)) {
                tableName = DBAdapter.Table.PROFILE_EVENTS;
                newCursor.setTableName(tableName);
                queuedDBEvents = adapter.fetchEvents(tableName, batchSize);
                newCursor = updateCursorForDBObject(queuedDBEvents, newCursor);
            }

            return newCursor.isEmpty() ? null : newCursor;
        }
    }

    // helper extracts the cursor data from the db object
    private  QueueCursor updateCursorForDBObject(JSONObject dbObject, QueueCursor cursor) {

        if (dbObject == null) return cursor;

        Iterator<String> keys = dbObject.keys();
        if (keys.hasNext()) {
            String key = keys.next();
            cursor.setLastId(key);
            try {
                cursor.setData(dbObject.getJSONArray(key));
            } catch (JSONException e) {
                cursor.setLastId(null);
                cursor.setData(null);
            }
        }

        return cursor;
    }

    /**
     * @return true if the network request succeeded. Anything non 200 results in a false.
     */
    private boolean sendQueue(final Context context, final JSONArray queue) {

        if (queue == null || queue.length() <= 0) return false;

        if(getCleverTapID()==null){
            getConfigLogger().debug(getAccountId(),"CleverTap Id not finalized, unable to send queue");
            return false;
        }

        HttpsURLConnection conn = null;
        try {
            final String endpoint = getEndpoint(false);

            // This is just a safety check, which would only arise
            // if upstream didn't adhere to the protocol (sent nothing during the initial handshake)
            if (endpoint == null) {
                getConfigLogger().debug(getAccountId(), "Problem configuring queue endpoint, unable to send queue");
                return false;
            }

            conn = buildHttpsURLConnection(endpoint);

            final String body;

            synchronized (this) {

                final String req = insertHeader(context, queue);
                if (req == null) {
                    getConfigLogger().debug(getAccountId(), "Problem configuring queue request, unable to send queue");
                    return false;
                }

                getConfigLogger().debug(getAccountId(), "Send queue contains " + queue.length() + " items: " + req);
                getConfigLogger().debug(getAccountId(), "Sending queue to: " + endpoint);
                conn.setDoOutput(true);
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
                        getConfigLogger().debug(getAccountId(), "The domain has changed to " + newDomain + ". The request will be retried shortly.");
                        return false;
                    }
                }

                if (processIncomingHeaders(context, conn)) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));

                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    body = sb.toString();
                    processResponse(context, body);
                }

                setLastRequestTimestamp(context, currentRequestTimestamp);
                setFirstRequestTimestampIfNeeded(context, currentRequestTimestamp);

                getConfigLogger().debug(getAccountId(), "Queue sent successfully");
            }
            mResponseFailureCount = 0;
            return true;
        } catch(Throwable e) {
            getConfigLogger().debug(getAccountId(), "An exception occurred while sending the queue, will retry: " + e.getLocalizedMessage());
            mResponseFailureCount++;
            scheduleQueueFlush(context);
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

    //Networking
    private String insertHeader(Context context, JSONArray arr) {
        try {
            final JSONObject header = new JSONObject();

            String deviceId = getCleverTapID();
            if (deviceId != null && !deviceId.equals("")) {
                header.put("g", deviceId);
            } else {
                getConfigLogger().verbose(getAccountId(), "CRITICAL: Couldn't finalise on a device ID!");
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

            String accountId = getAccountId();
            String token = this.config.getAccountToken();

            if (accountId == null || token == null) {
                getConfigLogger().debug(getAccountId(), "Account ID/token not found, unable to configure queue request");
                return null;
            }

            header.put("id", accountId);
            header.put("tk", token);
            header.put("l_ts", getLastRequestTimestamp());
            header.put("f_ts", getFirstRequestTimestamp());

            // Attach ARP
            try {
                final JSONObject arp = getARP(context);
                if (arp != null && arp.length() > 0) {
                    header.put("arp", arp);
                }
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(),"Failed to attach ARP", t);
            }

            JSONObject ref = new JSONObject();
            try {

                String utmSource = getSource();
                if (utmSource != null) {
                    ref.put("us", utmSource);
                }

                String utmMedium = getMedium();
                if (utmMedium != null) {
                    ref.put("um", utmMedium);
                }

                String utmCampaign = getCampaign();
                if (utmCampaign != null) {
                    ref.put("uc", utmCampaign);
                }

                if (ref.length() > 0) {
                    header.put("ref", ref);
                }

            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to attach ref", t);
            }

            JSONObject wzrkParams = getWzrkParams();
            if (wzrkParams != null && wzrkParams.length() > 0) {
                header.put("wzrk_ref", wzrkParams);
            }

            inAppFCManager.attachToHeader(context, header);

            // Resort to string concat for backward compatibility
            return "[" + header.toString() + ", " + arr.toString().substring(1);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "CommsManager: Failed to attach header", t);
            return arr.toString();
        }
    }

    private boolean hasDomainChanged(final String newDomain) {
        final String oldDomain = getStringFromPrefs(Constants.KEY_DOMAIN_NAME, null);
        return !newDomain.equals(oldDomain);
    }

    //Event
    private void processResponse(final Context context, final String responseStr) {
        if (responseStr == null) {
            getConfigLogger().verbose(getAccountId(), "Problem processing queue response, response is null");
            return;
        }

        try {
            getConfigLogger().verbose(getAccountId(), "Trying to process response: " + responseStr);
            JSONObject response = new JSONObject(responseStr);
            try {
                if(!this.config.isAnalyticsOnly())
                    processInAppResponse(response, context);
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to process in-app notifications from the response!", t);
            }

            // Always look for a GUID in the response, and if present, then perform a force update
            try {
                if (response.has("g")) {
                    final String deviceID = response.getString("g");
                    this.deviceInfo.forceUpdateDeviceId(deviceID);
                    getConfigLogger().verbose(getAccountId(), "Got a new device ID: " + deviceID);
                }
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to update device ID!", t);
            }

            try {
                getLocalDataStore().syncWithUpstream(context, response);
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to sync local cache with upstream", t);
            }

            // Handle "arp" (additional request parameters)
            try {
                if (response.has("arp")) {
                    final JSONObject arp = (JSONObject) response.get("arp");
                    if (arp.length() > 0) {
                        handleARPUpdate(context, arp);
                    }
                }
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to process ARP", t);
            }

            // Handle i
            try {
                if (response.has("_i")) {
                    final long i = response.getLong("_i");
                    setI(context, i);
                }
            } catch (Throwable t) {
                // Ignore
            }

            // Handle j
            try {
                if (response.has("_j")) {
                    final long j = response.getLong("_j");
                    setJ(context, j);
                }
            } catch (Throwable t) {
                // Ignore
            }

            // Handle "console" - print them as info to the console
            try {
                if (response.has("console")) {
                    final JSONArray console = (JSONArray) response.get("console");
                    if (console.length() > 0) {
                        for (int i = 0; i < console.length(); i++) {
                            getConfigLogger().debug(getAccountId(), console.get(i).toString());
                        }
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }

            // Handle server set debug level
            try {
                if (response.has("dbg_lvl")) {
                    final int debugLevel = response.getInt("dbg_lvl");
                    if (debugLevel >= 0) {
                        CleverTapAPI.setDebugLevel(debugLevel);
                        getConfigLogger().verbose(getAccountId(), "Set debug level to " + debugLevel + " for this session (set by upstream)");
                    }
                }
            } catch (Throwable t) {
                // Ignore
            }

            // Handle stale_inapp
            try {
                inAppFCManager.processResponse(context, response);
            } catch (Throwable t) {
                // Ignore
            }

            //TODO
            //Handle notification inbox
            try{
                getConfigLogger().verbose("Processing inbox messages...");
                processInboxResponse(response,context);
            }catch (Throwable t){
                getConfigLogger().verbose("Notification inbox exception: "+ t.getLocalizedMessage());
            }

        } catch (Throwable t) {
            mResponseFailureCount++;
            getConfigLogger().verbose(getAccountId(), "Problem process send queue response", t);
        }
    }

    //NotificationInbox
    private void processInboxResponse(final JSONObject response, final Context context){
        try{
            getConfigLogger().verbose(getAccountId(),"Inbox: Processing response");
            if (!response.has("inbox_notifs")) {
                getConfigLogger().verbose(getAccountId(),"Inbox: Response JSON object doesn't contain the inbox key, bailing");
                return;
            }

            if(getConfig().isAnalyticsOnly()){
                getConfigLogger().verbose(getAccountId(),"CleverTap instance is configured to analytics only, not processing inbox messages");
                return;
            }
            synchronized (inboxControllerLock) {
                if (this.ctInboxController == null) {
                    this.ctInboxController = CTInboxController.initWithAccountId(getAccountId(), getCleverTapID(), loadDBAdapter(context));
                    if (this.ctInboxController != null && ctInboxController.isInitialized()) {
                        if (this.ctInboxController.listener == null) {
                            this.ctInboxController.listener = new WeakReference<>(this).get();
                        }
                        this.ctInboxController.notifyInitialized();
                        JSONArray inboxMessages = response.getJSONArray("inbox_notifs");
                        this.ctInboxController.updateMessages(inboxMessages);
                    }
                }
            }

        }catch (Throwable t){
            getConfigLogger().verbose(getAccountId(),"InboxResponse: Failed to parse response", t);
        }
    }

    @Override
    public void inboxMessagesDidUpdate() {
        //TODO
        getConfigLogger().debug(getAccountId(),"Notification Inbox updated");
    }

    @Override
    public void inboxDidInitialize() {
        //TODO
        getConfigLogger().debug(getAccountId(),"Notification Inbox initialized");
    }

    /**
     * This method sets the CTNotificationInboxListener
     * @param notificationInboxListener An {@link CTNotificationInboxListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setCTNotificationInboxListener(CTNotificationInboxListener notificationInboxListener) {
        synchronized (inboxControllerLock) {
            if (this.ctInboxController != null) {
                this.ctInboxController.listener = notificationInboxListener;
            }
        }
    }

    /**
     * Returns the CTNotificationInboxListener object
     * @return An {@link CTNotificationInboxListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public CTNotificationInboxListener getCTNotificationInboxListener() {
        synchronized (inboxControllerLock) {
            if (this.ctInboxController != null) {
                return this.ctInboxController.listener;
            } else {
                return null;
            }
        }
    }

    //TODO Remove after testing
    public void manualInboxUpdate() throws JSONException {
        synchronized (inboxControllerLock) {
            if (this.ctInboxController == null) {
                this.ctInboxController = CTInboxController.initWithAccountId(getAccountId(), getCleverTapID(), loadDBAdapter(context));
                if (this.ctInboxController != null && ctInboxController.isInitialized()) {
                    if (this.ctInboxController.listener == null) {
                        this.ctInboxController.listener = new WeakReference<>(this).get();
                    }
                    this.ctInboxController.notifyInitialized();
                    JSONObject msg1 = new JSONObject();
                    msg1.put("id", "1");
                    msg1.put("date", 12);
                    msg1.put("ttl", 1);
                    JSONObject msg2 = new JSONObject();
                    msg2.put("id", "2");
                    msg2.put("date", 12);
                    msg2.put("ttl", 1);
                    JSONArray inboxMessages = new JSONArray();
                    inboxMessages.put(msg2);
                    inboxMessages.put(msg1);
                    this.ctInboxController.updateMessages(inboxMessages);
                }
            }
        }
    }


    //InApp
    private void processInAppResponse(final JSONObject response, final Context context) {
        try {
            getConfigLogger().verbose(getAccountId(),"InApp: Processing response");

            if (!response.has("inapp_notifs")) {
                getConfigLogger().verbose(getAccountId(),"InApp: Response JSON object doesn't contain the inapp key, bailing");
                return;
            }

            int perSession = 10;
            int perDay = 10;
            if (response.has(Constants.INAPP_MAX_PER_SESSION) && response.get(Constants.INAPP_MAX_PER_SESSION) instanceof Integer) {
                perSession = response.getInt(Constants.INAPP_MAX_PER_SESSION);
            }

            if (response.has("imp") && response.get("imp") instanceof Integer) {
                perDay = response.getInt("imp");
            }

            inAppFCManager.updateLimits(context, perDay, perSession);

            JSONArray inappNotifs;
            try {
                inappNotifs = response.getJSONArray(Constants.INAPP_JSON_RESPONSE_KEY);
            } catch (JSONException e) {
                getConfigLogger().debug(getAccountId(),"InApp: In-app key didn't contain a valid JSON array");
                return;
            }

            // Add all the new notifications to the queue
            SharedPreferences prefs = StorageHelper.getPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            try {
                JSONArray inappsFromPrefs = new JSONArray(getStringFromPrefs(Constants.PREFS_INAPP_KEY, "[]"));

                // Now add the rest of them :)
                if (inappNotifs != null && inappNotifs.length() > 0) {
                    for (int i = 0; i < inappNotifs.length(); i++) {
                        try {
                            JSONObject inappNotif = inappNotifs.getJSONObject(i);
                            inappsFromPrefs.put(inappNotif);
                        } catch (JSONException e) {
                            Logger.v("InAppManager: Malformed inapp notification");
                        }
                    }
                }

                // Commit all the changes
                editor.putString(storageKeyWithSuffix(Constants.PREFS_INAPP_KEY), inappsFromPrefs.toString());
                StorageHelper.persist(editor);
            } catch (Throwable e) {
                getConfigLogger().verbose(getAccountId(),"InApp: Failed to parse the in-app notifications properly");
                getConfigLogger().verbose(getAccountId(),"InAppManager: Reason: " + e.getMessage(), e);
            }
            // Fire the first notification, if any
            runOnNotificationQueue(new Runnable() {
                @Override
                public void run() {
                    _showNotificationIfAvailable(context);
                }
            });
        } catch (Throwable t) {
            Logger.v("InAppManager: Failed to parse response", t);
        }
    }

    //InApp
    @SuppressWarnings({"unused", "WeakerAccess"})
    private void showNotificationIfAvailable(final Context context){
        if(!this.config.isAnalyticsOnly()) {
            runOnNotificationQueue(new Runnable() {
                @Override
                public void run() {
                    _showNotificationIfAvailable(context);
                }
            });
        }
    }

    //InApp
    private void _showNotificationIfAvailable(Context context) {
        SharedPreferences prefs = StorageHelper.getPreferences(context);
        try {
            if (!canShowInAppOnActivity()) {
                Logger.v("Not showing notification on blacklisted activity");
                return;
            }

            checkPendingNotifications(context, config);  // see if we have any pending notifications

            JSONArray inapps = new JSONArray(getStringFromPrefs(Constants.PREFS_INAPP_KEY, "[]"));
            if (inapps.length() < 1) {
                return;
            }

            JSONObject inapp = inapps.getJSONObject(0);
            prepareNotificationForDisplay(inapp);

            // JSON array doesn't have the feature to remove a single element,
            // so we have to copy over the entire array, but the first element
            JSONArray inappsUpdated = new JSONArray();
            for (int i = 0; i < inapps.length(); i++) {
                if (i==0) continue;
                inappsUpdated.put(inapps.get(i));
            }
            SharedPreferences.Editor editor = prefs.edit().putString(storageKeyWithSuffix(Constants.PREFS_INAPP_KEY), inappsUpdated.toString());
            StorageHelper.persist(editor);
        } catch (Throwable t) {
            // We won't get here
            getConfigLogger().verbose(getAccountId(),"InApp: Couldn't parse JSON array string from prefs", t);
        }
    }


    //InApp
    private void prepareNotificationForDisplay(final JSONObject jsonObject){
        getConfigLogger().debug(getAccountId(),"Preparing In-App for display: "+jsonObject.toString());
        runOnNotificationQueue(new NotificationPrepareRunnable(this, jsonObject));
    }

    //InApp
    private final class NotificationPrepareRunnable implements Runnable{
        private final WeakReference<CleverTapAPI> cleverTapAPIWeakReference;
        private final JSONObject jsonObject;

        NotificationPrepareRunnable (CleverTapAPI cleverTapAPI, JSONObject jsonObject){
            this.cleverTapAPIWeakReference = new WeakReference<>(cleverTapAPI);
            this.jsonObject = jsonObject;
        }

        @Override
        public void run(){
            final CTInAppNotification inAppNotification = new CTInAppNotification().initWithJSON(jsonObject);
            if(inAppNotification.getError() != null){
                getConfigLogger().debug(getAccountId(),"Unable to parse inapp notification "+ inAppNotification.getError());
                return;
            }
            inAppNotification.listener = cleverTapAPIWeakReference.get();
            inAppNotification.prepareForDisplay();
        }
    }

    //InApp
    @Override
    public void notificationReady(final CTInAppNotification inAppNotification){
        if(Looper.myLooper() != Looper.getMainLooper()){
            getHandlerUsingMainLooper().post(new Runnable() {
                @Override
                public void run() {
                    notificationReady(inAppNotification);
                }
            });
            return;
        }

        if(inAppNotification.getError() != null){
            getConfigLogger().debug(getAccountId(),"Unable to process inapp notification " + inAppNotification.getError());
            return;
        }
        getConfigLogger().debug(getAccountId(),"Notification ready: "+inAppNotification.getJsonDescription());
        displayNotification(inAppNotification);
    }

    //InApp
    private void displayNotification(final CTInAppNotification inAppNotification){

        if(Looper.myLooper() != Looper.getMainLooper()){
            getHandlerUsingMainLooper().post(new Runnable() {
                @Override
                public void run() {
                    displayNotification(inAppNotification);
                }
            });
            return;
        }

        if (!inAppFCManager.canShow(inAppNotification)) {
            getConfigLogger().verbose(getAccountId(),"InApp has been rejected by FC, not showing " + inAppNotification.getCampaignId());
            showInAppNotificationIfAny();
            return;
        }

        inAppFCManager.didShow(context, inAppNotification);

        final InAppNotificationListener listener = getInAppNotificationListener();

        final boolean goFromListener;

        if (listener != null) {
            final HashMap<String, Object> kvs;

            if(inAppNotification.getCustomExtras()!=null) {
                kvs = Utils.convertJSONObjectToHashMap(inAppNotification.getCustomExtras());
            }else{
                kvs = new HashMap<>();
            }

            goFromListener = listener.beforeShow(kvs);
        } else {
            goFromListener = true;
        }

        if (!goFromListener) {
            getConfigLogger().verbose(getAccountId(),"Application has decided to not show this in-app notification: " + inAppNotification.getCampaignId());
            showInAppNotificationIfAny();
            return;
        }
        showInApp(context,inAppNotification,config);

    }

    //InApp
    private static void inAppDidDismiss(Context context, CleverTapInstanceConfig config, CTInAppNotification inAppNotification){
        Logger.v(config.getAccountId(), "Running inAppDidDismiss");
        if(currentlyDisplayingInApp != null && (currentlyDisplayingInApp.getCampaignId().equals(inAppNotification.getCampaignId()))) {
            currentlyDisplayingInApp = null;
            checkPendingNotifications(context, config);
        }
    }

    private static void checkPendingNotifications(final Context context, final CleverTapInstanceConfig config) {
        Logger.v(config.getAccountId(), "checking Pending Notifications");
        if (pendingNotifications != null && !pendingNotifications.isEmpty()) {
            try {
                final CTInAppNotification notification = pendingNotifications.get(0);
                pendingNotifications.remove(0);
                Handler mainHandler = new Handler(context.getMainLooper());
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        showInApp(context, notification , config);
                    }
                });
            } catch (Throwable t) {
                // no-op
            }
        }
    }

    //InApp
    private static void showInApp(Context context, final CTInAppNotification inAppNotification, CleverTapInstanceConfig config){

        Logger.v(config.getAccountId(), "Attempting to show next In-App");

        if (!appForeground) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(),"Not in foreground, queueing this In App");
            return;
        }

        if(currentlyDisplayingInApp != null){
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(),"In App already displaying, queueing this In App");
            return;
        }

        currentlyDisplayingInApp = inAppNotification;

        CTInAppBaseFragment inAppFragment = null;
        CTInAppType type = inAppNotification.getInAppType();
        switch(type){
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

                Intent intent = new Intent(context,InAppNotificationActivity.class);
                intent.putExtra("inApp",inAppNotification);
                intent.putExtra("config",config);
                try {
                    Activity currentActivity = getCurrentActivity();
                    if (currentActivity == null) {
                        throw new IllegalStateException("Current activity reference not found");
                    }
                    config.getLogger().verbose(config.getAccountId(),"calling InAppActivity for notification: " + inAppNotification.getJsonDescription());
                    currentActivity.startActivity(intent);
                    Logger.d("Displaying In-App: "+inAppNotification.getJsonDescription());

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
                Logger.d(config.getAccountId(),"Unknown InApp Type found: " + type);
                currentlyDisplayingInApp = null;
                return;
        }

        if (inAppFragment != null) {
            Logger.d("Displaying In-App: "+inAppNotification.getJsonDescription());
            try {
                FragmentTransaction fragmentTransaction = getCurrentActivity().getFragmentManager().beginTransaction();
                Bundle bundle = new Bundle();
                bundle.putParcelable("inApp",inAppNotification);
                bundle.putParcelable("config",config);
                inAppFragment.setArguments(bundle);
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in,android.R.animator.fade_out);
                fragmentTransaction.add(android.R.id.content,inAppFragment);
                Logger.v(config.getAccountId(),"calling InAppFragment " + inAppNotification.getCampaignId());
                fragmentTransaction.commit();

            } catch (Throwable t) {
                Logger.v(config.getAccountId(),"Fragment not able to render", t);
            }
        }
    }

    private void showInAppNotificationIfAny(){
        if(!this.config.isAnalyticsOnly()){
            runOnNotificationQueue(new Runnable() {
                @Override
                public void run() {
                    _showNotificationIfAvailable(context);
                }
            });
        }
    }

    /**
     * This method sets the InAppNotificationListener
     * @param inAppNotificationListener An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setInAppNotificationListener(InAppNotificationListener inAppNotificationListener) {
        this.inAppNotificationListener = inAppNotificationListener;
    }

    /**
     * Returns the InAppNotificationListener object
     * @return An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public InAppNotificationListener getInAppNotificationListener() {
        return inAppNotificationListener;
    }

    private boolean canShowInAppOnActivity() {
        updateBlacklistedActivitySet();

        for (String blacklistedActivity : inappActivityExclude) {
            String currentActivityName = getCurrentActivityName();
            if (currentActivityName != null && currentActivityName.contains(blacklistedActivity)) {
                return false;
            }
        }

        return true;
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
            getConfigLogger().debug(getAccountId(),"In-app notifications will not be shown on " + Arrays.toString(inappActivityExclude.toArray()));
        }
    }

    //Session
    private void setLastRequestTimestamp(Context context, int ts) {
        StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_LAST_TS), ts);
    }

    private void setFirstRequestTimestampIfNeeded(Context context, int ts) {
        if (getFirstRequestTimestamp() > 0) return;
        StorageHelper.putInt(context, storageKeyWithSuffix(Constants.KEY_FIRST_TS), ts);
    }

    private int getFirstRequestTimestamp() {
        return getIntFromPrefs(Constants.KEY_FIRST_TS, 0);
    }

    private int getLastRequestTimestamp() {
        return getIntFromPrefs(Constants.KEY_LAST_TS, 0);
    }

    //Event
    private JSONObject getAppLaunchedFields() {
        try {
            final JSONObject evtData = new JSONObject();
            evtData.put("Build", this.deviceInfo.getBuild() + "");
            evtData.put("Version", this.deviceInfo.getVersionName());
            evtData.put("OS Version", this.deviceInfo.getOsVersion());
            evtData.put("SDK Version", this.deviceInfo.getSdkVersion());

            if (locationFromUser != null) {
                evtData.put("Latitude", locationFromUser.getLatitude());
                evtData.put("Longitude", locationFromUser.getLongitude());
            }

            // send up googleAdID
            if (this.deviceInfo.getGoogleAdID() != null) {
                String baseAdIDKey = "GoogleAdID";
                String adIDKey = deviceIsMultiUser() ? Constants.MULTI_USER_PREFIX + baseAdIDKey : baseAdIDKey;
                evtData.put(adIDKey, this.deviceInfo.getGoogleAdID());
                evtData.put("GoogleAdIDLimit", this.deviceInfo.isLimitAdTrackingEnabled());
            }

            try {
                // Device data
                evtData.put("Make", this.deviceInfo.getManufacturer());
                evtData.put("Model", this.deviceInfo.getModel());
                evtData.put("Carrier", this.deviceInfo.getCarrier());
                evtData.put("useIP",enableNetworkInfoReporting);
                evtData.put("OS", this.deviceInfo.getOsName());
                evtData.put("wdt", this.deviceInfo.getWidth());
                evtData.put("hgt", this.deviceInfo.getHeight());
                evtData.put("dpi", this.deviceInfo.getDPI());

                String cc = this.deviceInfo.getCountryCode();
                if (cc != null && !cc.equals(""))
                    evtData.put("cc", cc);

                if(enableNetworkInfoReporting) {
                    final Boolean isWifi = this.deviceInfo.isWifiConnected();
                    if (isWifi != null) {
                        evtData.put("wifi", isWifi);
                    }

                    final Boolean isBluetoothEnabled = this.deviceInfo.isBluetoothEnabled();
                    if (isBluetoothEnabled != null) {
                        evtData.put("BluetoothEnabled", isBluetoothEnabled);
                    }

                    final String bluetoothVersion = this.deviceInfo.getBluetoothVersion();
                    if (bluetoothVersion != null) {
                        evtData.put("BluetoothVersion", bluetoothVersion);
                    }

                    final String radio = this.deviceInfo.getNetworkType();
                    if (radio != null) {
                        evtData.put("Radio", radio);
                    }
                }

            } catch (Throwable t) {
                // Ignore
            }

            return evtData;
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to construct App Launched event", t);
            return new JSONObject();
        }
    }

    //Session
    /**
     * The ARP is additional request parameters, which must be sent once
     * received after any HTTP call. This is sort of a proxy for cookies.
     *
     * @return A JSON object containing the ARP key/values. Can be null.
     */
    private JSONObject getARP(final Context context) {
        try {
            final String nameSpaceKey = getNamespaceARPKey();
            if (nameSpaceKey == null) return null;

            final SharedPreferences prefs = StorageHelper.getPreferences(context, nameSpaceKey);
            final Map<String, ?> all = prefs.getAll();
            final Iterator<? extends Map.Entry<String, ?>> iter = all.entrySet().iterator();

            while (iter.hasNext()) {
                final Map.Entry<String, ?> kv = iter.next();
                final Object o = kv.getValue();
                if (o instanceof Number && ((Number) o).intValue() == -1) {
                    iter.remove();
                }
            }
            final JSONObject ret = new JSONObject(all);
            getConfigLogger().verbose(getAccountId(), "Fetched ARP for namespace key: " + nameSpaceKey + " values: " + all.toString());
            return ret;
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to construct ARP object", t);
            return null;
        }
    }

    private long getI() {
        return getLongFromPrefs(Constants.KEY_I, 0,Constants.NAMESPACE_IJ);
    }

    private long getJ() {
        return getLongFromPrefs(Constants.KEY_J, 0, Constants.NAMESPACE_IJ);
    }

    @SuppressLint("CommitPrefEdits")
    private void setJ(Context context, long j) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(storageKeyWithSuffix(Constants.KEY_J), j);
        StorageHelper.persist(editor);
    }

    @SuppressLint("CommitPrefEdits")
    private void setI(Context context, long i) {
        final SharedPreferences prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(storageKeyWithSuffix(Constants.KEY_I), i);
        StorageHelper.persist(editor);
    }

    private void handleARPUpdate(final Context context, final JSONObject arp) {
        if (arp == null || arp.length() == 0) return;

        final String nameSpaceKey = getNamespaceARPKey();
        if (nameSpaceKey == null) return;

        final SharedPreferences prefs = StorageHelper.getPreferences(context, nameSpaceKey);
        final SharedPreferences.Editor editor = prefs.edit();

        final Iterator<String> keys = arp.keys();
        while (keys.hasNext()) {
            final String key = keys.next();
            try {
                final Object o = arp.get(key);
                if (o instanceof Number) {
                    final int update = ((Number) o).intValue();
                    editor.putInt(key, update);
                } else if (o instanceof String) {
                    if (((String) o).length() < 100) {
                        editor.putString(key, (String) o);
                    } else {
                        getConfigLogger().verbose(getAccountId(), "ARP update for key " + key + " rejected (string value too long)");
                    }
                } else if (o instanceof Boolean) {
                    editor.putBoolean(key, (Boolean) o);
                } else {
                    getConfigLogger().verbose(getAccountId(), "ARP update for key " + key + " rejected (invalid data type)");
                }
            } catch (JSONException e) {
                // Ignore
            }
        }
        getConfigLogger().verbose(getAccountId(), "Completed ARP update for namespace key: " + nameSpaceKey + "");
        StorageHelper.persist(editor);
    }

    //util
    private boolean deviceIsMultiUser() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        return cachedGUIDs.length() > 1;
    }

    //Profile
    private JSONObject getCachedGUIDs() {
        JSONObject cache = null;
        String json = getStringFromPrefs(Constants.CACHED_GUIDS_KEY, null);
        if (json != null) {
            try {
                cache = new JSONObject(json);
            } catch (Throwable t) {
                // no-op
                getConfigLogger().verbose(getAccountId(), "Error reading guid cache: " + t.toString());
            }
        }

        return (cache != null) ? cache : new JSONObject();
    }

    //Listener

    /**
     * Returns the SyncListener object
     * @return The {@link SyncListener} object
     */
    public SyncListener getSyncListener() {
        return syncListener;
    }

    /**
     * This method is used to set the SyncListener
     * @param syncListener The {@link SyncListener} object
     */
    @SuppressWarnings("unused")
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    /**
     * Returns the DevicePushTokenRefreshListener
     * @return The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public DevicePushTokenRefreshListener getDevicePushTokenRefreshListener(){
        return tokenRefreshListener;
    }

    /**
     * This method is used to set the DevicePushTokenRefreshListener object
     * @param tokenRefreshListener The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public void setDevicePushTokenRefreshListener(DevicePushTokenRefreshListener tokenRefreshListener){
        this.tokenRefreshListener = tokenRefreshListener;
    }
    //Profile
    private void pushBasicProfile(JSONObject baseProfile) {
        try {
            String guid = getCleverTapID();

            JSONObject profileEvent = new JSONObject();

            if (baseProfile != null && baseProfile.length() > 0) {
                Iterator i = baseProfile.keys();
                while (i.hasNext()) {
                    String next = i.next().toString();

                    // need to handle command-based JSONObject props here now
                    Object value = null;
                    try {
                        value = baseProfile.getJSONObject(next);
                    } catch (Throwable t) {
                        try {
                            value = baseProfile.get(next);
                        } catch (JSONException e) {
                            //no-op
                        }
                    }

                    if (value != null) {
                        profileEvent.put(next, value);

                        // cache the valid identifier: guid pairs
                        if (Constants.PROFILE_IDENTIFIER_KEYS.contains(next)) {
                            try {
                                cacheGUIDForIdentifier(guid, next, value.toString());
                            } catch (Throwable t) {
                                // no-op
                            }
                        }
                    }
                }
            }

            try {
                String carrier = this.deviceInfo.getCarrier();
                if (carrier != null && !carrier.equals("")) {
                    profileEvent.put("Carrier", carrier);
                }

                String cc = this.deviceInfo.getCountryCode();
                if (cc != null && !cc.equals("")) {
                    profileEvent.put("cc", cc);
                }

                profileEvent.put("tz", TimeZone.getDefault().getID());

                JSONObject event = new JSONObject();
                event.put("profile", profileEvent);
                queueEvent(context, event, Constants.PROFILE_EVENT);
            } catch (JSONException e) {
                getConfigLogger().verbose(getAccountId(), "FATAL: Creating basic profile update event failed!");
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Basic profile sync", t);
        }
    }

    private void cacheGUIDForIdentifier(String guid, String key, String identifier) {
        if (guid == null || key == null || identifier == null) return;

        String cacheKey = key + "_" + identifier;
        JSONObject cache = getCachedGUIDs();
        try {
            cache.put(cacheKey, guid);
            setCachedGUIDs(cache);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error caching guid: " + t.toString());
        }
    }

    private void setCachedGUIDs(JSONObject cachedGUIDs) {
        if (cachedGUIDs == null) return;
        try {
            StorageHelper.putString(context, storageKeyWithSuffix(Constants.CACHED_GUIDS_KEY), cachedGUIDs.toString());
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error persisting guid cache: " + t.toString());
        }
    }


    /**
     * Push a profile update.
     *
     * @param profile A {@link Map}, with keys as strings, and values as {@link String},
     *                {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                {@link java.util.Date}, or {@link Character}
     */
    public void pushProfile(final Map<String, Object> profile) {
        if (profile == null || profile.isEmpty())
            return;

        postAsyncSafely("profilePush", new Runnable() {
            @Override
            public void run() {
                _push(profile);
            }
        });
    }

    private void _push(Map<String, Object> profile) {
        if (profile == null || profile.isEmpty())
            return;

        try {
            ValidationResult vr;
            JSONObject customProfile = new JSONObject();
            JSONObject fieldsToUpdateLocally = new JSONObject();
            for (String key : profile.keySet()) {
                Object value = profile.get(key);

                vr = validator.cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    pushValidationResult(vr);
                }

                if (key == null || key.isEmpty()) {
                    ValidationResult keyError = new ValidationResult();
                    keyError.setErrorCode(512);
                    final String keyErr = "Profile push key is empty";
                    keyError.setErrorDesc(keyErr);
                    pushValidationResult(keyError);
                    getConfigLogger().debug(getAccountId(),keyErr);
                    // Skip this property
                    continue;
                }

                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Profile);
                } catch (Throwable e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = new ValidationResult();
                    error.setErrorCode(512);
                    final String err = "Object value wasn't a primitive (" + value + ") for profile field " + key;
                    error.setErrorDesc(err);
                    pushValidationResult(error);
                    getConfigLogger().debug(getAccountId(), err);
                    // Skip this property
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    pushValidationResult(vr);
                }

                // test Phone:  if no device country code, test if phone starts with +, log but always send
                if (key.equalsIgnoreCase("Phone")) {
                    try {
                        value = value.toString();
                        String countryCode = this.deviceInfo.getCountryCode();
                        if (countryCode == null || countryCode.isEmpty()) {
                            String _value = (String) value;
                            if (!_value.startsWith("+")) {
                                ValidationResult error = new ValidationResult();
                                error.setErrorCode(512);
                                final String err = "Device country code not available and profile phone: " + value + " does not appear to start with country code";
                                error.setErrorDesc(err);
                                pushValidationResult(error);
                                getConfigLogger().debug(getAccountId(), err);
                            }
                        }
                        getConfigLogger().verbose(getAccountId(), "Profile phone is: " + value + " device country code is: " + ((countryCode != null) ? countryCode : "null"));
                    } catch (Exception e) {
                        pushValidationResult(new ValidationResult(512, "Invalid phone number"));
                        getConfigLogger().debug(getAccountId(), "Invalid phone number: " + e.getLocalizedMessage());
                        continue;
                    }
                }

                // add to the local profile update object
                fieldsToUpdateLocally.put(key, value);
                customProfile.put(key, value);
            }

            getConfigLogger().verbose(getAccountId(), "Constructed custom profile: " + customProfile.toString());

            // update local profile values
            if (fieldsToUpdateLocally.length() > 0) {
                getLocalDataStore().setProfileFields(fieldsToUpdateLocally);
            }

            pushBasicProfile(customProfile);

        } catch (Throwable t) {
            // Will not happen
            getConfigLogger().verbose(getAccountId(), "Failed to push profile", t);
        }
    }

    //UTM
    // only set if not already set during the session
    private synchronized void setSource(String source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    // only set if not already set during the session
    private synchronized void setMedium(String medium) {
        if (this.medium == null) {
            this.medium = medium;
        }
    }

    private synchronized void setCampaign(String campaign) {
        if (this.campaign == null) {
            this.campaign = campaign;
        }
    }

    private synchronized void setWzrkParams(JSONObject wzrkParams) {
        if (this.wzrkParams == null) {
            this.wzrkParams = wzrkParams;
        }
    }

    private synchronized void clearSource() {
        source = null;
    }

    private synchronized void clearMedium() {
        medium = null;
    }

    private synchronized void clearCampaign() {
        campaign = null;
    }

    private synchronized void clearWzrkParams() {
        wzrkParams = null;
    }

    private synchronized String getSource() {
        return source;
    }

    private synchronized String getMedium() {
        return medium;
    }

    private synchronized String getCampaign() {
        return campaign;
    }

    private synchronized JSONObject getWzrkParams() {
        return wzrkParams;
    }

    //Session
    private void setLastVisitTime() {
        EventDetail ed = getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed == null) {
            lastVisitTime = -1;
        } else {
            lastVisitTime = ed.getLastTime();
        }
    }

    //Session

    /**
     * Returns the total number of times the app has been launched
     * @return Total number of app launches in int
     */
    public int getTotalVisits() {
        EventDetail ed = getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed != null) return ed.getCount();

        return 0;
    }

    /**
     * Returns the number of screens which have been displayed by the app
     * @return Total number of screens which have been displayed by the app
     */
    public int getScreenCount() {
        return CleverTapAPI.activityCount;
    }

    /**
     * Returns the time elapsed by the user on the app
     * @return Time elapsed by user on the app in int
     */
    public int getTimeElapsed() {
        int currentSession = getCurrentSession();
        if (currentSession == 0) return -1;

        int now = (int) (System.currentTimeMillis() / 1000);
        return now - currentSession;
    }

    /**
     * Returns the timestamp of the previous visit
     * @return Timestamp of previous visit in int
     */
    public int getPreviousVisitTime() {
        return lastVisitTime;
    }

    /**
     * Returns a UTMDetail object which consists of UTM parameters like source, medium & campaign
     * @return The {@link UTMDetail} object
     */
    public UTMDetail getUTMDetails() {
        UTMDetail ud = new UTMDetail();
        ud.setSource(source);
        ud.setMedium(medium);
        ud.setCampaign(campaign);
        return ud;
    }

    //Profile
    /**
     * Set a collection of unique values as a multi-value user profile property, any existing value will be overwritten.
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     */
    public void setMultiValuesForKey(final String key, final ArrayList<String> values) {
        postAsyncSafely("setMultiValuesForKey", new Runnable() {
            @Override
            public void run() {
                _handleMultiValues(values, key, Constants.COMMAND_SET);
            }
        });
    }

    /**
     * Add a unique value to a multi-value user profile property
     * If the property does not exist it will be created
     * <p/>
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     * <p/>
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key   String
     * @param value String
     */
    public void addMultiValueForKey(String key, String value) {

        //noinspection ConstantConditions
        if (value == null || value.isEmpty()) {
            _generateEmptyMultiValueError(key);
            return;
        }

        addMultiValuesForKey(key, new ArrayList<>(Collections.singletonList(value)));
    }

    /**
     * Add a collection of unique values to a multi-value user profile property
     * If the property does not exist it will be created
     * <p/>
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     * <p/>
     * If the key currently contains a scalar value, the key will be promoted to a multi-value property
     * with the current value cast to a string and the new value(s) added
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     */
    public void addMultiValuesForKey(final String key, final ArrayList<String> values) {
        postAsyncSafely("addMultiValuesForKey", new Runnable() {
            @Override
            public void run() {
                final String command = (getLocalDataStore().getProfileValueForKey(key) != null) ? Constants.COMMAND_ADD : Constants.COMMAND_SET;
                _handleMultiValues(values, key, command);
            }
        });
    }

    /**
     * Remove a unique value from a multi-value user profile property
     * <p/>
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key   String
     * @param value String
     */
    public void removeMultiValueForKey(String key, String value) {

        //noinspection ConstantConditions
        if (value == null || value.isEmpty()) {
            _generateEmptyMultiValueError(key);
            return;
        }

        removeMultiValuesForKey(key, new ArrayList<>(Collections.singletonList(value)));
    }

    /**
     * Remove a collection of unique values from a multi-value user profile property
     * <p/>
     * If the key currently contains a scalar value, prior to performing the remove operation
     * the key will be promoted to a multi-value property with the current value cast to a string.
     * <p/>
     * If the multi-value property is empty after the remove operation, the key will be removed.
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     */
    public void removeMultiValuesForKey(final String key, final ArrayList<String> values) {
        postAsyncSafely("removeMultiValuesForKey", new Runnable() {
            @Override
            public void run() {
                _handleMultiValues(values, key, Constants.COMMAND_REMOVE);
            }
        });
    }

    /**
     * Remove the user profile property value specified by key from the user profile
     *
     * @param key String
     */
    public void removeValueForKey(final String key) {
        postAsyncSafely("removeValueForKey", new Runnable() {
            @Override
            public void run() {
                _removeValueForKey(key);
            }
        });
    }

    private void _removeValueForKey(String key) {
        try {
            key = (key == null) ? "" : key; // so we will generate a validation error later on

            // validate the key
            ValidationResult vr;

            vr = validator.cleanObjectKey(key);
            key = vr.getObject().toString();

            if (key.isEmpty()) {
                ValidationResult error = new ValidationResult();
                error.setErrorCode(512);
                error.setErrorDesc("Key is empty, profile removeValueForKey aborted.");
                pushValidationResult(error);
                getConfigLogger().debug(getAccountId(),"Key is empty, profile removeValueForKey aborted");
                // Abort
                return;
            }
            // Check for an error
            if (vr.getErrorCode() != 0) {
                pushValidationResult(vr);
            }

            // remove from the local profile
            getLocalDataStore().removeProfileField(key);

            // send the delete command
            JSONObject command = new JSONObject().put(Constants.COMMAND_DELETE, true);
            JSONObject update = new JSONObject().put(key, command);
            pushBasicProfile(update);

            getConfigLogger().verbose(getAccountId(), "removing value for key " + key + " from user profile");

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to remove profile value for key " + key, t);
        }
    }

    private String getGraphUserPropertySafely(JSONObject graphUser, String key, String def) {
        try {
            String prop = (String) graphUser.get(key);
            if (prop != null)
                return prop;
            else
                return def;
        } catch (Throwable t) {
            return def;
        }
    }

    /**
     * Pushes everything available in the JSON object returned by the Facebook GraphRequest
     *
     * @param graphUser The object returned from Facebook
     */
    public void pushFacebookUser(final JSONObject graphUser) {
        postAsyncSafely("pushFacebookUser", new Runnable() {
            @Override
            public void run() {
                _pushFacebookUser(graphUser);
            }
        });
    }

    private void _pushFacebookUser(JSONObject graphUser) {
        try {
            if (graphUser == null)
                return;
            // Note: No validations are required here, as everything is controlled
            String name = getGraphUserPropertySafely(graphUser, "name", "");
            try {
                // Certain users have nasty looking names - unicode chars, validate for any
                // not allowed chars
                ValidationResult vr = validator.cleanObjectValue(name,Validator.ValidationContext.Profile);
                name = vr.getObject().toString();

                if (vr.getErrorCode() != 0) {
                    pushValidationResult(vr);
                }
            } catch (IllegalArgumentException e) {
                // Weird name, wasn't a string, or any number
                // This would never happen with FB
                name = "";
            }

            String gender = getGraphUserPropertySafely(graphUser, "gender", null);
            // Convert to WR format
            if (gender != null) {
                if (gender.toLowerCase().startsWith("m"))
                    gender = "M";
                else if (gender.toLowerCase().startsWith("f"))
                    gender = "F";
                else
                    gender = "";
            } else {
                gender = null;
            }
            String email = getGraphUserPropertySafely(graphUser, "email", "");

            String birthday = getGraphUserPropertySafely(graphUser, "birthday", null);
            if (birthday != null) {
                // Some users don't have the year of birth mentioned
                // FB returns only MM/dd in those cases
                if (birthday.matches("^../..")) {
                    // This means that the year is not available(~30% of the times)
                    // Ignore
                    birthday = "";
                } else {
                    try {
                        Date date = Constants.FB_DOB_DATE_FORMAT.parse(birthday);
                        birthday = "$D_" + (int) (date.getTime() / 1000);
                    } catch (ParseException e) {
                        // Differs from the specs
                        birthday = "";
                    }
                }
            }

            String work;
            try {
                JSONArray workArray = graphUser.getJSONArray("work");
                work = (workArray.length() > 0) ? "Y" : "N";
            } catch (Throwable t) {
                work = null;
            }

            String education;
            try {
                JSONArray eduArray = graphUser.getJSONArray("education");
                // FB returns the education levels in a descending order - highest = last entry
                String fbEdu = eduArray.getJSONObject(eduArray.length() - 1).getString("type");
                if (fbEdu.toLowerCase().contains("high school"))
                    education = "School";
                else if (fbEdu.toLowerCase().contains("college"))
                    education = "College";
                else if (fbEdu.toLowerCase().contains("graduate school"))
                    education = "Graduate";
                else
                    education = "";
            } catch (Throwable t) {
                // No education info available
                education = null;
            }

            String id = getGraphUserPropertySafely(graphUser, "id", "");

            String married = getGraphUserPropertySafely(graphUser, "relationship_status", null);
            if (married != null) {
                if (married.equalsIgnoreCase("married")) {
                    married = "Y";
                } else {
                    married = "N";
                }
            }

            final JSONObject profile = new JSONObject();
            if (id != null && id.length() > 3) profile.put("FBID", id);
            if (name != null && name.length() > 3) profile.put("Name", name);
            if (email != null && email.length() > 3) profile.put("Email", email);
            if (gender != null && !gender.trim().equals("")) profile.put("Gender", gender);
            if (education != null && !education.trim().equals("")) profile.put("Education", education);
            if (work != null && !work.trim().equals("")) profile.put("Employed", work);
            if (birthday != null && birthday.length() > 3) profile.put("DOB", birthday);
            if (married != null && !married.trim().equals("")) profile.put("Married", married);

            pushBasicProfile(profile);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to parse graph user object successfully", t);
        }
    }

    /**
     * Pushes everything useful within the Google Plus
     * {@link com.google.android.gms.plus.model.people.Person} object.
     *
     * @param person The {@link com.google.android.gms.plus.model.people.Person} object
     * @see com.google.android.gms.plus.model.people.Person
     */
    public void pushGooglePlusPerson(final com.google.android.gms.plus.model.people.Person person) {
        postAsyncSafely("pushGooglePlusPerson", new Runnable() {
            @Override
            public void run() {
                _pushGooglePlusPerson(person);
            }
        });
    }

    private void _pushGooglePlusPerson(com.google.android.gms.plus.model.people.Person person) {
        if (person == null) {
            return;
        }
        try {
            // Note: No validations are required here, as everything is controlled
            String name = "";
            if (person.hasDisplayName()) {
                try {
                    // Certain users have nasty looking names - unicode chars, validate for any
                    // not allowed chars
                    name = person.getDisplayName();
                    ValidationResult vr = validator.cleanObjectValue(name, Validator.ValidationContext.Profile);
                    name = vr.getObject().toString();

                    if (vr.getErrorCode() != 0) {
                        pushValidationResult(vr);
                    }
                } catch (Throwable t) {
                    // Weird name, wasn't a string, or any number
                    // This would never happen with G+
                    name = "";
                }
            }

            String gender = null;
            if (person.hasGender()) {
                if (person.getGender() == com.google.android.gms.plus.model.people.Person.Gender.MALE) {
                    gender = "M";
                } else if (person.getGender() == com.google.android.gms.plus.model.people.Person.Gender.FEMALE) {
                    gender = "F";
                }
            }

            String birthday = null;

            if (person.hasBirthday()) {
                // We have the string as YYYY-MM-DD
                try {
                    Date date = Constants.GP_DOB_DATE_FORMAT.parse(person.getBirthday());
                    birthday = "$D_" + (int) (date.getTime() / 1000);
                } catch (Throwable t) {
                    // Differs from the specs
                    birthday = null;
                }
            }

            String work = null;
            if (person.hasOrganizations()) {
                List<Person.Organizations> organizations = person.getOrganizations();
                for (com.google.android.gms.plus.model.people.Person.Organizations o : organizations) {
                    if (o.getType() == com.google.android.gms.plus.model.people.Person.Organizations.Type.WORK) {
                        work = "Y";
                        break;
                    }
                }
            }

            String id = "";
            if (person.hasId()) {
                id = person.getId();
            }

            String married = null;
            if (person.hasRelationshipStatus()) {
                if (person.getRelationshipStatus() == com.google.android.gms.plus.model.people.Person.RelationshipStatus.MARRIED) {
                    married = "Y";
                } else {
                    married = "N";
                }
            }

            // Construct json object from the data
            final JSONObject profile = new JSONObject();
            if (id != null && id.trim().length() > 0) profile.put("GPID", id);
            if (name != null && name.trim().length() > 0) profile.put("Name", name);
            if (gender != null && gender.trim().length() > 0) profile.put("Gender", gender);
            if (work != null && work.trim().length() > 0) profile.put("Employed", work);
            if (birthday != null && birthday.trim().length() > 4) profile.put("DOB", birthday);
            if (married != null && married.trim().length() > 0) profile.put("Married", married);

            pushBasicProfile(profile);
        } catch (Throwable t) {
            // We won't get here
            getConfigLogger().verbose(getAccountId(), "FATAL: Creating G+ profile update event failed!");
        }
    }

    /**
     * Return the user profile property value for the specified key
     *
     * @param name String
     * @return {@link JSONArray}, String or null
     */
    public Object getProperty(String name) {
        if (!this.config.isPersonalizationEnabled()) return null;
        return getLocalDataStore().getProfileProperty(name);
    }

    // use for internal profile getter doesn't do the personalization check
    private Object _getProfilePropertyIgnorePersonalizationFlag(String key) {
        return getLocalDataStore().getProfileValueForKey(key);
    }

    // private multi-value handlers and helpers

    private void _handleMultiValues(ArrayList<String> values, String key, String command) {
        if (key == null) return;

        if (values == null || values.isEmpty()) {
            _generateEmptyMultiValueError(key);
            return;
        }

        ValidationResult vr;

        // validate the key
        vr = validator.cleanMultiValuePropertyKey(key);

        // Check for an error
        if (vr.getErrorCode() != 0) {
            pushValidationResult(vr);
        }

        // reset the key
        Object _key = vr.getObject();
        String cleanKey = (_key != null) ? vr.getObject().toString() : null;

        // if key is empty generate an error and return
        if (cleanKey == null || cleanKey.isEmpty()) {
            _generateInvalidMultiValueKeyError(key);
            return;
        }

        key = cleanKey;

        try {
            JSONArray currentValues = _constructExistingMultiValue(key, command);
            JSONArray newValues = _cleanMultiValues(values, key);
            _validateAndPushMultiValue(currentValues, newValues, values, key, command);

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error handling multi value operation for key " + key, t);
        }
    }

    private JSONArray _constructExistingMultiValue(String key, String command) {

        Boolean remove = command.equals(Constants.COMMAND_REMOVE);
        Boolean add = command.equals(Constants.COMMAND_ADD);

        // only relevant for add's and remove's; a set overrides the existing value, so return a new array
        if (!remove && !add) return new JSONArray();

        Object existing = _getProfilePropertyIgnorePersonalizationFlag(key);

        // if there is no existing value
        if (existing == null) {
            // if its a remove then return null to abort operation
            // no point in running remove against a nonexistent value
            if (remove) return null;

            // otherwise return an empty array
            return new JSONArray();
        }

        // value exists

        // the value should only ever be a JSONArray or scalar (String really)

        // if its already a JSONArray return that
        if (existing instanceof JSONArray) return (JSONArray) existing;

        // handle a scalar value as the existing value
        /*
            if its an add, our rule is to promote the scalar value to multi value and include the cleaned stringified
            scalar value as the first element of the resulting array

            NOTE: the existing scalar value is currently limited to 120 bytes; when adding it to a multi value
            it is subject to the current 40 byte limit

            if its a remove, our rule is to delete the key from the local copy
            if the cleaned stringified existing value is equal to any of the cleaned values passed to the remove method

            if its an add, return an empty array as the default,
            in the event the existing scalar value fails stringifying/cleaning

            returning null will signal that a remove operation should be aborted,
            as there is no valid promoted multi value to remove against
         */

        JSONArray _default = (add) ? new JSONArray() : null;

        String stringified = _stringifyAndCleanScalarProfilePropValue(existing);

        return (stringified != null) ? new JSONArray().put(stringified) : _default;
    }

    private String _stringifyScalarProfilePropValue(Object value) {
        String val = null;

        try {
            val = value.toString();
        } catch (Exception e) {
            // no-op
        }

        return val;
    }

    private String _stringifyAndCleanScalarProfilePropValue(Object value) {
        String val = _stringifyScalarProfilePropValue(value);

        if (val != null) {
            ValidationResult vr = validator.cleanMultiValuePropertyValue(val);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                pushValidationResult(vr);
            }

            Object _value = vr.getObject();
            val = (_value != null) ? vr.getObject().toString() : null;
        }

        return val;
    }

    private JSONArray _cleanMultiValues(ArrayList<String> values, String key) {

        try {
            if (values == null || key == null) return null;

            JSONArray cleanedValues = new JSONArray();
            ValidationResult vr;

            // loop through and clean the new values
            for (String value : values) {
                value = (value == null) ? "" : value;  // so we will generate a validation error later on

                // validate value
                vr = validator.cleanMultiValuePropertyValue(value);

                // Check for an error
                if (vr.getErrorCode() != 0) {
                    pushValidationResult(vr);
                }

                // reset the value
                Object _value = vr.getObject();
                value = (_value != null) ? vr.getObject().toString() : null;

                // if value is empty generate an error and return
                if (value == null || value.isEmpty()) {
                    _generateEmptyMultiValueError(key);
                    // Abort
                    return null;
                }
                // add to the newValues to be merged
                cleanedValues.put(value);
            }

            return cleanedValues;

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error cleaning multi values for key " + key, t);
            _generateEmptyMultiValueError(key);
            return null;
        }
    }

    private void _validateAndPushMultiValue(JSONArray currentValues, JSONArray newValues, ArrayList<String> originalValues, String key, String command) {

        try {

            // if any of these are null, indicates some problem along the way so abort operation
            if (currentValues == null || newValues == null || originalValues == null || key == null || command == null)
                return;

            String mergeOperation = command.equals(Constants.COMMAND_REMOVE) ? Validator.REMOVE_VALUES_OPERATION : Validator.ADD_VALUES_OPERATION;

            // merge currentValues and newValues
            ValidationResult vr = validator.mergeMultiValuePropertyForKey(currentValues, newValues, mergeOperation, key);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                pushValidationResult(vr);
            }

            // set the merged local values array
            JSONArray localValues = (JSONArray) vr.getObject();

            // update local profile
            // remove an empty array
            if (localValues == null || localValues.length() <= 0) {
                getLocalDataStore().removeProfileField(key);
            } else {
                // not empty so save to local profile
                getLocalDataStore().setProfileField(key, localValues);
            }

            // push to server
            JSONObject commandObj = new JSONObject();
            commandObj.put(command, new JSONArray(originalValues));

            JSONObject fields = new JSONObject();
            fields.put(key, commandObj);

            pushBasicProfile(fields);

            getConfigLogger().verbose(getAccountId(), "Constructed multi-value profile push: " + fields.toString());

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error pushing multiValue for key " + key, t);
        }
    }

    private void _generateEmptyMultiValueError(String key) {
        ValidationResult error = new ValidationResult();
        String msg = "Invalid multi value for key " + key + ", profile multi value operation aborted.";
        error.setErrorCode(512);
        error.setErrorDesc(msg);
        pushValidationResult(error);
        getConfigLogger().debug(getAccountId(), msg);
    }

    private void _generateInvalidMultiValueKeyError(String key) {
        ValidationResult error = new ValidationResult();
        error.setErrorCode(523);
        error.setErrorDesc("Invalid multi-value property key " + key);
        pushValidationResult(error);
        getConfigLogger().debug(getAccountId(), "Invalid multi-value property key " + key + " profile multi value operation aborted");
    }

    //Event
    /**
     * Pushes the notification details to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     * @deprecated use pushNotificationClickedEvent(extras) instead
     */
    @Deprecated
    public void pushNotificationEvent(final Bundle extras) {
        pushNotificationClickedEvent(extras);
    }


    /**
     * Pushes the Notification Clicked event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    public void pushNotificationClickedEvent(final Bundle extras) {

        if (this.config.isAnalyticsOnly()) {
            getConfigLogger().debug(getAccountId(), "is Analytics Only - will not process Notification Clicked event.");
            return;
        }

        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            getConfigLogger().debug(getAccountId(), "Push notification: " + (extras == null ? "NULL" : extras.toString()) + " not from CleverTap - will not process Notification Clicked event.");
            return;
        }

        String accountId = null;
        try {
            accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        } catch (Throwable t) {
            // no-op
        }

        boolean shouldProcess = (accountId == null && config.isDefaultInstance()) || getAccountId().equals(accountId);

        if (!shouldProcess) {
            getConfigLogger().debug(getAccountId(), "Push notification not targeted at this instance, not processing Notification Clicked Event");
            return;
        }

        if (extras.containsKey(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)) {
            pendingInappRunnable  = new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.v("Received in-app via push payload: " + extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY));
                        JSONObject r = new JSONObject();
                        JSONArray inappNotifs = new JSONArray();
                        r.put(Constants.INAPP_JSON_RESPONSE_KEY, inappNotifs);
                        inappNotifs.put(new JSONObject(extras.getString(Constants.INAPP_PREVIEW_PUSH_PAYLOAD_KEY)));
                        processInAppResponse(r, context);
                    } catch (Throwable t) {
                        Logger.v("Failed to display inapp notification from push notification payload", t);
                    }
                }
            };
            return;
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            getConfigLogger().debug(getAccountId(), "Push notification ID Tag is null, not processing Notification Clicked event for:  " + extras.toString());
            return;
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (5 secs) don't process
        boolean isDuplicate = checkDuplicateNotificationIds(extras, notificationIdTagMap, Constants.NOTIFICATION_ID_TAG_INTERVAL);
        if (isDuplicate) {
            getConfigLogger().debug(getAccountId(), "Already processed Notification Clicked event for " + extras.toString() + ", dropping duplicate.");
            return;
        }

        JSONObject event = new JSONObject();
        JSONObject notif = new JSONObject();
        try {
            for (String x : extras.keySet()) {
                if (!x.startsWith(Constants.WZRK_PREFIX))
                    continue;
                Object value = extras.get(x);
                notif.put(x, value);
            }

            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            event.put("evtData", notif);
            queueEvent(context, event, Constants.RAISED_EVENT);

            try {
                setWzrkParams(getWzrkFields(extras));
            } catch (Throwable t) {
                // no-op
            }
        } catch (Throwable t) {
            // We won't get here
        }
    }

    private boolean checkDuplicateNotificationIds(Bundle extras, HashMap<String,Object> notificationTagMap, int interval){
        synchronized (notificationMapLock) {
            // default to false; only return true if we are sure we've seen this one before
            boolean isDupe = false;
            try {
                String notificationIdTag = extras.getString(Constants.NOTIFICATION_ID_TAG);
                long now = System.currentTimeMillis();
                if (notificationTagMap.containsKey(notificationIdTag)) {
                    long timestamp = (Long) notificationTagMap.get(notificationIdTag);
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

    private JSONObject getWzrkFields(Bundle root) throws JSONException {
        final JSONObject fields = new JSONObject();
        for (String s : root.keySet()) {
            final Object o = root.get(s);
            if (o instanceof Bundle) {
                final JSONObject wzrkFields = getWzrkFields((Bundle) o);
                final Iterator<String> keys = wzrkFields.keys();
                while (keys.hasNext()) {
                    final String k = keys.next();
                    fields.put(k, wzrkFields.get(k));
                }
            } else if (s.startsWith(Constants.WZRK_PREFIX)) {
                fields.put(s, root.get(s));
            }
        }

        return fields;
    }

    private JSONObject getWzrkFields(CTInAppNotification root) throws JSONException {
        final JSONObject fields = new JSONObject();
        JSONObject jsonObject = root.getJsonDescription();
        Iterator<String> iterator = jsonObject.keys();

        while(iterator.hasNext()){
            String keyName = iterator.next();
            if(keyName.startsWith(Constants.WZRK_PREFIX))
                fields.put(keyName,jsonObject.get(keyName));
        }

        return fields;
    }

    /**
     * Push Charged event, which describes a purchase made.
     *
     * @param chargeDetails A {@link HashMap}, with keys as strings, and values as {@link String},
     *                      {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                      {@link java.util.Date}, or {@link Character}
     * @param items         An {@link ArrayList} which contains up to 15 {@link HashMap} objects,
     *                      where each HashMap object describes a particular item purchased
     */
    public void pushChargedEvent(HashMap<String, Object> chargeDetails,
                     ArrayList<HashMap<String, Object>> items) {

        if (chargeDetails == null || items == null) {
            getConfigLogger().debug(getAccountId(), "Invalid Charged event: details and or items is null");
            return;
        }

        if (items.size() > 50) {
            ValidationResult error = new ValidationResult();
            error.setErrorCode(522);
            error.setErrorDesc("Charged event contained more than 50 items.");
            getConfigLogger().debug(getAccountId(),"Charged event contained more than 50 items.");
            pushValidationResult(error);
        }

        JSONObject evtData = new JSONObject();
        JSONObject chargedEvent = new JSONObject();
        ValidationResult vr;
        try {
            for (String key : chargeDetails.keySet()) {
                Object value = chargeDetails.get(key);
                vr = validator.cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0)
                    chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));

                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event);
                } catch (IllegalArgumentException e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = new ValidationResult();
                    error.setErrorCode(511);
                    final String err = "For event Charged: Property value for property " + key + " wasn't a primitive (" + value + ")";
                    error.setErrorDesc(err);
                    pushValidationResult(error);
                    getConfigLogger().debug(getAccountId(), err);
                    // Skip this property
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0)
                    chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));

                evtData.put(key, value);
            }

            JSONArray jsonItemsArray = new JSONArray();
            for (HashMap<String, Object> map : items) {
                JSONObject itemDetails = new JSONObject();
                for (String key : map.keySet()) {
                    Object value = map.get(key);
                    vr = validator.cleanObjectKey(key);
                    key = vr.getObject().toString();
                    // Check for an error
                    if (vr.getErrorCode() != 0)
                        chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));

                    try {
                        vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event);
                    } catch (IllegalArgumentException e) {
                        // The object was neither a String, Boolean, or any number primitives
                        ValidationResult error = new ValidationResult();
                        error.setErrorCode(511);
                        final String err = "An item's object value for key " + key + " wasn't a primitive (" + value + ")";
                        error.setErrorDesc(err);
                        getConfigLogger().debug(getAccountId(), err);

                        pushValidationResult(error);
                        // Skip this property
                        continue;
                    }
                    value = vr.getObject();
                    // Check for an error
                    if (vr.getErrorCode() != 0)
                        chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));
                    itemDetails.put(key, value);
                }
                jsonItemsArray.put(itemDetails);
            }
            evtData.put("Items", jsonItemsArray);

            chargedEvent.put("evtName", Constants.CHARGED_EVENT);
            chargedEvent.put("evtData", evtData);
            queueEvent(context, chargedEvent, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    /**
     * Push an event with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventActions A {@link HashMap}, with keys as strings, and values as {@link String},
     *                     {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                     {@link java.util.Date}, or {@link Character}
     */
    public void pushEvent(String eventName, Map<String, Object> eventActions) {

        if (eventName == null || eventName.equals(""))
            return;

        ValidationResult validationResult = validator.isRestrictedEventName(eventName);
        // Check for a restricted event name
        if (validationResult.getErrorCode()>0) {
            pushValidationResult(validationResult);
            return;
        }

        if (eventActions == null) {
            eventActions = new HashMap<>();
        }

        JSONObject event = new JSONObject();
        try {
            // Validate
            ValidationResult vr = validator.cleanEventName(eventName);

            // Check for an error
            if (vr.getErrorCode() != 0)
                event.put(Constants.ERROR_KEY, getErrorObject(vr));

            eventName = vr.getObject().toString();
            JSONObject actions = new JSONObject();
            for (String key : eventActions.keySet()) {
                Object value = eventActions.get(key);
                vr = validator.cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0)
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                try {
                    vr = validator.cleanObjectValue(value, Validator.ValidationContext.Event);
                } catch (IllegalArgumentException e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = new ValidationResult();
                    error.setErrorCode(512);
                    final String err = "For event \"" + eventName + "\": Property value for property " + key + " wasn't a primitive (" + value + ")";
                    error.setErrorDesc(err);
                    getConfigLogger().debug(getAccountId(), err);
                    pushValidationResult(error);
                    // Skip this record
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0)
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                actions.put(key, value);
            }
            event.put("evtName", eventName);
            event.put("evtData", actions);
            queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    /**
     * Pushes a basic event.
     *
     * @param eventName The name of the event
     */
    public void pushEvent(String eventName) {
        if (eventName == null || eventName.trim().equals(""))
            return;

        pushEvent(eventName, null);
    }


    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    public void pushNotificationViewedEvent(Bundle extras){

        if (extras == null || extras.isEmpty() || extras.get(Constants.NOTIFICATION_TAG) == null) {
            getConfigLogger().debug(getAccountId(), "Push notification: " + (extras == null ? "NULL" : extras.toString()) + " not from CleverTap - will not process Notification Viewed event.");
            return;
        }

        if (!extras.containsKey(Constants.NOTIFICATION_ID_TAG) || (extras.getString(Constants.NOTIFICATION_ID_TAG) == null)) {
            getConfigLogger().debug(getAccountId(), "Push notification ID Tag is null, not processing Notification Viewed event for:  " + extras.toString());
            return;
        }

        // Check for dupe notification views; if same notficationdId within specified time interval (2 secs) don't process
        boolean isDuplicate = checkDuplicateNotificationIds(extras, notificationViewedIdTagMap, Constants.NOTIFICATION_VIEWED_ID_TAG_INTERVAL);
        if (isDuplicate) {
            getConfigLogger().debug(getAccountId(), "Already processed Notification Viewed event for " + extras.toString() + ", dropping duplicate.");
            return;
        }

        JSONObject event = new JSONObject();
        try {
            JSONObject notif = getWzrkFields(extras);
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            event.put("evtData", notif);
        } catch (Throwable ignored) {
            //no-op
        }
        queueEvent(context, event, Constants.RAISED_EVENT);
    }

    /**
     * Raises the Notification Clicked event, if {@param clicked} is true,
     * otherwise the Notification Viewed event, if {@param clicked} is false.
     *
     * @param clicked    Whether or not this notification was clicked
     * @param data       The data to be attached as the event data
     * @param customData Additional data such as form input to to be added to the event data
     */
    void pushInAppNotificationStateEvent(boolean clicked, CTInAppNotification data, Bundle customData) {
        JSONObject event = new JSONObject();
        try {
            JSONObject notif = getWzrkFields(data);

            if (customData != null) {
                for (String x : customData.keySet()) {

                    Object value = customData.get(x);
                    if (value != null) notif.put(x, value);
                }
            }

            if (clicked) {
                try {
                    setWzrkParams(notif);
                } catch (Throwable t) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            }

            event.put("evtData", notif);
            queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable ignored) {
            // We won't get here
        }
    }

    //Session

    /**
     * Returns an EventDetail object for the particular event passed. EventDetail consists of event name, count, first time
     * and last time timestamp of the event.
     *
     * @param event The event name for which you want the Event details
     * @return The {@link EventDetail} object
     */
    public EventDetail getDetails(String event) {
        return getLocalDataStore().getEventDetail(event);
    }

    /**
     * Returns a Map of event names and corresponding event details of all the events raised
     * @return A Map of Event Name and its corresponding EventDetail object
     */
    public Map<String, EventDetail> getHistory() {
        return getLocalDataStore().getEventHistory(context);
    }

    /**
     * Returns the timestamp of the first time the given event was raised
     * @param event The event name for which you want the first time timestamp
     * @return The timestamp in int
     */
    public int getFirstTime(String event) {
        EventDetail eventDetail = getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) return eventDetail.getFirstTime();

        return -1;
    }

    /**
     * Returns the timestamp of the last time the given event was raised
     * @param event The event name for which you want the last time timestamp
     * @return The timestamp in int
     */
    public int getLastTime(String event) {
        EventDetail eventDetail = getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) return eventDetail.getLastTime();

        return -1;
    }

    /**
     * Returns the total count of the specified event
     * @param event The event for which you want to get the total count
     * @return Total count in int
     */
    public int getCount(String event) {
        EventDetail eventDetail = getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) return eventDetail.getCount();

        return -1;
    }

    //Event
    /**
     * Internally records an "Error Occurred" event, which can be viewed in the dashboard.
     *
     * @param errorMessage The error message
     * @param errorCode    The error code
     */
    public void pushError(final String errorMessage, final int errorCode) {
        final HashMap<String, Object> props = new HashMap<>();
        props.put("Error Message", errorMessage);
        props.put("Error Code", errorCode);

        try {
            final String activityName = getCurrentActivityName();
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

    //Profile
    /**
     * Creates a separate and distinct user profile identified by one or more of Identity,
     * Email, FBID or GPID values,
     * and populated with the key-values included in the profile map argument.
     * <p>
     * If your app is used by multiple users, you can use this method to assign them each a
     * unique profile to track them separately.
     * <p>
     * If instead you wish to assign multiple Identity, Email, FBID and/or GPID values to the same
     * user profile,
     * use profile.push rather than this method.
     * <p>
     * If none of Identity, Email, FBID or GPID is included in the profile map,
     * all profile map values will be associated with the current user profile.
     * <p>
     * When initially installed on this device, your app is assigned an "anonymous" profile.
     * The first time you identify a user on this device (whether via onUserLogin or profilePush),
     * the "anonymous" history on the device will be associated with the newly identified user.
     * <p>
     * Then, use this method to switch between subsequent separate identified users.
     * <p>
     * Please note that switching from one identified user to another is a costly operation
     * in that the current session for the previous user is automatically closed
     * and data relating to the old user removed, and a new session is started
     * for the new user and data for that user refreshed via a network call to CleverTap.
     * In addition, any global frequency caps are reset as part of the switch.
     *
     * @param profile The map keyed by the type of identity, with the value as the identity
     */
    @SuppressWarnings("unused")
    public void onUserLogin(final Map<String, Object> profile) {
        if (profile == null) return;

        try {
            final String currentGUID = getCleverTapID();
            if (currentGUID == null) return;

            boolean haveIdentifier = false;
            String cachedGUID = null;

            // check for valid identifier keys
            // use the first one we find
            for (String key : profile.keySet()) {
                Object value = profile.get(key);
                if (Constants.PROFILE_IDENTIFIER_KEYS.contains(key)) {
                    try {
                        String identifier = value.toString();
                        if (identifier != null && identifier.length() > 0) {
                            haveIdentifier = true;
                            cachedGUID = getGUIDForIdentifier(key, identifier);
                            if (cachedGUID != null) break;
                        }
                    } catch (Throwable t) {
                        // no-op
                    }
                }
            }

            // if no valid identifier provided or there are no identified users on the device; just push on the current profile
            if (!haveIdentifier || isAnonymousDevice()) {
                getConfigLogger().debug(getAccountId(), "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile");
                pushProfile(profile);
                return;
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID.equals(currentGUID)) {
                getConfigLogger().debug(getAccountId(), "onUserLogin: " + profile.toString() + " maps to current device id " + currentGUID + " pushing on current profile");
                pushProfile(profile);
                return;
            }

            // stringify profile to use as dupe blocker
            String profileToString = profile.toString();

            // as processing happens async block concurrent onUserLogin requests with the same profile, as our cache is set async
            if (isProcessUserLoginWithIdentifier(profileToString)) {
                getConfigLogger().debug(getAccountId(), "Already processing onUserLogin for " + profileToString);
                return;
            }

            // create new guid if necessary and reset
            // block any concurrent onUserLogin call for the same profile
            synchronized (processingUserLoginLock) {
                processingUserLoginIdentifier = profileToString;
            }

            getConfigLogger().verbose(getAccountId(), "onUserLogin: queuing reset profile for " + profileToString
                    + " with Cached GUID " + ((cachedGUID != null) ? cachedGUID : "NULL"));


            final String guid = cachedGUID;

            final DeviceInfo _deviceInfo = this.deviceInfo;

            postAsyncSafely("resetProfile", new Runnable() {
                @Override
                public void run() {
                    try {
                        //set optOut to false on the current user to unregister the device token
                        setCurrentUserOptedOut(false);
                        // unregister the device token on the current user
                        forcePushDeviceToken(false);

                        // try and flush and then reset the queues
                        flushQueueSync(context);
                        clearQueues(context);

                        // clear out the old data
                        getLocalDataStore().changeUser();
                        inAppFCManager.changeUser(context);
                        activityCount = 1;
                        destroySession();

                        // either force restore the cached GUID or generate a new one
                        if (guid != null) {
                            _deviceInfo.forceUpdateDeviceId(guid);
                            notifyUserProfileInitialized(guid);
                        } else {
                            String g = _deviceInfo.forceNewDeviceID();
                            notifyUserProfileInitialized(g);
                        }
                        setCurrentUserOptOutStateFromStorage(); // be sure to call this after the guid is updated
                        forcePushAppLaunchedEvent();
                        pushProfile(profile);
                        forcePushDeviceToken(true);
                        synchronized (processingUserLoginLock) {
                            processingUserLoginIdentifier = null;
                        }
                        resetInbox();
                    } catch (Throwable t) {
                        getConfigLogger().verbose(getAccountId(), "Reset Profile error", t);
                    }
                }
            });

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "onUserLogin failed", t);
        }
    }

    private String getGUIDForIdentifier(String key, String identifier) {
        if (key == null || identifier == null) return null;

        String cacheKey = key + "_" + identifier;
        JSONObject cache = getCachedGUIDs();
        try {
            return cache.getString(cacheKey);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(),"Error reading guid cache: " + t.toString());
            return null;
        }
    }

    private boolean isAnonymousDevice() {
        JSONObject cachedGUIDs = getCachedGUIDs();
        return cachedGUIDs.length() <= 0;
    }

    private boolean isProcessUserLoginWithIdentifier(String identifier) {
        synchronized (processingUserLoginLock) {
            return processingUserLoginIdentifier != null && processingUserLoginIdentifier.equals(identifier);
        }
    }

    //Push
    /**
     * push the device token outside of the normal course
     */
    private void forcePushDeviceToken(final boolean register) {
        pushDeviceToken(register, true);
    }

    @SuppressWarnings("SameParameterValue")
    private void pushDeviceToken(final boolean register, final boolean force) {
        if (enabledPushTypes == null) return;
        for (PushType pushType : enabledPushTypes) {
            switch (pushType) {
                case GCM:
                    pushGCMDeviceToken(null, register, force);
                    break;
                case FCM:
                    pushFCMDeviceToken(null, register, force);
                    break;
                default:
                    //no-op
                    break;
            }
        }
    }

    //Event
    private void forcePushAppLaunchedEvent() {
        setAppLaunchPushed(false);
        pushAppLaunchedEvent();
    }

    /**
     * Sends all the events in the event queue.
     */
    @SuppressWarnings("unused")
    public void flush() {
        flushQueueAsync(context);
    }

    //Push

    /**
     * Sends the GCM registration ID to CleverTap.
     *
     * @param gcmId    The GCM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    public void pushGcmRegistrationId(String gcmId, boolean register) {
        pushDeviceToken(gcmId, register, PushType.GCM);
    }

    /**
     * Sends the FCM registration ID to CleverTap.
     *
     * @param fcmId    The FCM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    public void pushFcmRegistrationId(String fcmId, boolean register) {
        pushDeviceToken(fcmId, register, PushType.FCM);
    }

    /**
     * For internal use, don't call the public API internally
     *
     */
    private void pushDeviceToken(final String token, final boolean register, final PushType type) {
        pushDeviceToken(this.context, token, register, type);
    }

    private void pushDeviceToken(final Context context, final String token, final boolean register, final PushType type) {
        if (token == null || type == null ) return;

        JSONObject event = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            String action = register ? "register" : "unregister";
            data.put("action", action);
            data.put("id", token);
            data.put("type", type.toString());
            event.put("data", data);
            getConfigLogger().verbose(getAccountId(), "DataHandler: pushing device token with action " + action + " and type " + type.toString());
            queueEvent(context, event, Constants.DATA_EVENT);
        } catch (JSONException e) {
            // we won't get here
        }
    }

    //Util
    /**
     * Returns the generic handler object which is used to post
     * runnables. The returned value will never be null.
     *
     * @return The generic handler
     * @see Handler
     */
    private Handler getHandlerUsingMainLooper() {
        return handlerUsingMainLooper;
    }

    //Util
    /**
     * Use this to safely post a runnable to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     */
    @SuppressWarnings("UnusedParameters")
    private void postAsyncSafely(final String name, final Runnable runnable) {
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            getConfigLogger().verbose(getAccountId(), "Executor service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to submit task to the executor service", t);
        }
    }

    //InApp
    @Override
    public void inAppNotificationDidShow(Context context, CTInAppNotification inAppNotification, Bundle formData) {
        pushInAppNotificationStateEvent(false, inAppNotification, formData);
    }

    @Override
    public void inAppNotificationDidClick(Context context, CTInAppNotification inAppNotification, Bundle formData) {
        pushInAppNotificationStateEvent(true, inAppNotification, formData);
    }

    @Override
    public void inAppNotificationDidDismiss(final Context context, final CTInAppNotification inAppNotification, Bundle formData){
        inAppNotification.didDismiss();
        inAppFCManager.didDismiss(inAppNotification);
        getConfigLogger().verbose(getAccountId(),"InApp Dismissed: " +inAppNotification.getCampaignId());
        try {
            final InAppNotificationListener listener = getInAppNotificationListener();
            if (listener != null) {
                final HashMap<String, Object> notifKVS;

                if (inAppNotification.getCustomExtras() != null) {
                    //noinspection ConstantConditions
                    notifKVS = Utils.convertJSONObjectToHashMap(inAppNotification.getCustomExtras());
                } else {
                    notifKVS = new HashMap<>();
                }

                Logger.v("Calling the in-app listener on behalf of " + source);

                if (formData != null) {
                    listener.onDismissed(notifKVS, Utils.convertBundleObjectToHashMap(formData));
                } else {
                    listener.onDismissed(notifKVS, null);
                }
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(),"Failed to call the in-app notification listener", t);
        }

        // Fire the next one, if any
        runOnNotificationQueue(new Runnable() {
            @Override
            public void run() {
                inAppDidDismiss(context,getConfig(),inAppNotification);
                _showNotificationIfAvailable(context);
            }
        });
    }

    //InApp
    private void runOnNotificationQueue(final Runnable runnable){
        try {
            final boolean executeSync = Thread.currentThread().getId() == NOTIFICATION_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                ns.submit(new Runnable() {
                    @Override
                    public void run() {
                        NOTIFICATION_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            getConfigLogger().verbose(getAccountId(), "Notification executor service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to submit task to the notification executor service", t);
        }
    }

    //Profile
    private void notifyUserProfileInitialized(String deviceID) {
        deviceID = (deviceID != null) ? deviceID : getCleverTapID();

        if (deviceID == null) return;

        final SyncListener sl;
        try {
            sl = getSyncListener();
            if (sl != null) {
                sl.profileDidInitialize(deviceID);
            }
        } catch (Throwable t) {
            // Ignore
        }
    }

    private static @Nullable CleverTapAPI createInstanceIfAvailable(Context context, String _accountId){
        try {
            if (_accountId == null) {
                try {
                    return CleverTapAPI.getDefaultInstance(context);
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
            String configJson = StorageHelper.getString(context,"instance:"+_accountId,"");
            if(!configJson.isEmpty()) {
                CleverTapInstanceConfig config = CleverTapInstanceConfig.createInstance(configJson);
                Logger.v("Inflated Instance Config: " +configJson);
                return config != null ? CleverTapAPI.instanceWithConfig(context, config) : null;
            } else {
                try {
                    CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
                    return (instance != null && instance.config.getAccountId().equals(_accountId)) ? instance : null;
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static @Nullable CleverTapAPI getDefaultInstanceOrFirstOther(Context context) {
        CleverTapAPI instance = getDefaultInstance(context);
        if (instance == null && instances != null && !instances.isEmpty()) {
            for (String accountId: CleverTapAPI.instances.keySet()) {
                instance = CleverTapAPI.instances.get(accountId);
                if (instance != null) {
                    break;
                }
            }
        }
        return instance;
    }

    //PN
    /**
     * Checks whether this notification is from CleverTap.
     *
     * @param extras The payload from the GCM intent
     * @return See {@link NotificationInfo}
     */
    public static NotificationInfo getNotificationInfo(final Bundle extras) {
        if (extras == null) return new NotificationInfo(false, false);

        boolean fromCleverTap = extras.containsKey(Constants.NOTIFICATION_TAG);
        boolean shouldRender = fromCleverTap && extras.containsKey("nm");
        return new NotificationInfo(fromCleverTap, shouldRender);
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param extras  The {@link Bundle} object received by the broadcast receiver
     */
    public static void createNotification(final Context context, final Bundle extras) {
        createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID);
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p>
     * If your app is using CleverTap SDK's built in FCM/GCM message handling,
     * this method does not need to be called explicitly.
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param extras  The {@link Bundle} object received by the broadcast receiver
     * @param notificationId A custom id to build a notification
     */
    @SuppressWarnings({"WeakerAccess"})
    public static void createNotification(final Context context, final Bundle extras, final int notificationId) {
        String _accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        if (instances == null) {
            CleverTapAPI instance = createInstanceIfAvailable(context,_accountId);
            if(instance != null)
                instance._createNotification(context,extras,notificationId);
            return;
        }

        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            boolean shouldProcess = (_accountId == null && instance.config.isDefaultInstance()) || instance.getAccountId().equals(_accountId);
            if (shouldProcess) {
                try {
                    instance._createNotification(context,extras, notificationId);
                } catch (Throwable t) {
                    // no-op
                }
                break;
            }
        }
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p/>
     * If your app is using CleverTap SDK's built in FCM/GCM message handling,
     * this method does not need to be called explicitly.
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param extras  The {@link Bundle} object received by the broadcast receiver
     * @param notificationId A custom id to build a notification
     */
    private void _createNotification(final Context context, final Bundle extras, final int notificationId) {
        //noinspection ConstantConditions
        if (extras == null || extras.get(Constants.NOTIFICATION_TAG) == null) {
            return;
        }

        if (config.isAnalyticsOnly()) {
            getConfigLogger().debug(getAccountId(), "Instance is set for Analytics only, cannot create notification");
            return;
        }

        try {
            postAsyncSafely("CleverTapAPI#_createNotification", new Runnable() {
                @Override
                public void run() {
                    try {
                        getConfigLogger().debug(getAccountId(), "Handling notification: " + extras.toString());
                        // Check if this is a test notification
                        if (extras.containsKey(Constants.DEBUG_KEY)
                                && "y".equals(extras.getString(Constants.DEBUG_KEY))) {
                            int r = (int) (Math.random() * 10);
                            if (r != 8) {
                                // Discard acknowledging this notif
                                return;
                            }
                            JSONObject event = new JSONObject();
                            try {
                                JSONObject actions = new JSONObject();
                                for (String x : extras.keySet()) {
                                    Object value = extras.get(x);
                                    actions.put(x, value);
                                }
                                event.put("evtName", "wzrk_d");
                                event.put("evtData", actions);
                                queueEvent(context, event, Constants.RAISED_EVENT);
                            } catch (JSONException ignored) {
                                // Won't happen
                            }
                            // Drop further processing
                            return;
                        }
                        String notifMessage = extras.getString("nm");
                        notifMessage = (notifMessage != null) ? notifMessage : "";
                        if (notifMessage.isEmpty()) {
                            getConfigLogger().verbose(getAccountId(),"Push notification message is empty, not rendering");
                            return;
                        }
                        String notifTitle = extras.getString("nt", "");
                        notifTitle = notifTitle.isEmpty() ? context.getApplicationInfo().name : notifTitle;
                        triggerNotification(context, extras, notifMessage, notifTitle, notificationId);
                    } catch (Throwable t) {
                        // Occurs if the notification image was null
                        // Let's return, as we couldn't get a handle on the app's icon
                        // Some devices throw a PackageManager* exception too
                        getConfigLogger().debug(getAccountId(),"Couldn't render notification: ", t);
                    }
                }
            });
        } catch (Throwable t) {
            getConfigLogger().debug(getAccountId(), "Failed to process push notification", t);
        }
    }

    private void triggerNotification(Context context, Bundle extras, String notifMessage, String notifTitle, int notificationId){
        String icoPath = extras.getString("ico");
        Intent launchIntent = new Intent(context, CTPushNotificationReceiver.class);

        PendingIntent pIntent;

        // Take all the properties from the notif and add it to the intent
        launchIntent.putExtras(extras);
        launchIntent.removeExtra("wzrk_acts");
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(),
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Style style;
        String bigPictureUrl = extras.getString("wzrk_bp");
        if (bigPictureUrl != null && bigPictureUrl.startsWith("http")) {
            try {
                Bitmap bpMap = Utils.getNotificationBitmap(bigPictureUrl, false, context);

                //noinspection ConstantConditions
                if (bpMap == null)
                    throw new Exception("Failed to fetch big picture!");

                style = new NotificationCompat.BigPictureStyle()
                        .setSummaryText(notifMessage)
                        .bigPicture(bpMap);
            } catch (Throwable t) {
                style = new NotificationCompat.BigTextStyle()
                        .bigText(notifMessage);
                getConfigLogger().verbose(getAccountId(), "Falling back to big text notification, couldn't fetch big picture", t);
            }
        } else {
            style = new NotificationCompat.BigTextStyle()
                    .bigText(notifMessage);
        }

        int smallIcon;
        try {
            String x = ManifestInfo.getInstance(context).getNotificationIcon();
            if (x == null) throw new IllegalArgumentException();
            smallIcon = context.getResources().getIdentifier(x, "drawable", context.getPackageName());
            if (smallIcon == 0) throw new IllegalArgumentException();
        } catch (Throwable t) {
            smallIcon = DeviceInfo.getAppIconAsIntId(context);
        }

        int priorityInt = NotificationCompat.PRIORITY_DEFAULT;
        String priority = extras.getString("pr");
        if (priority != null) {
            if (priority.equals("high")) {
                priorityInt = NotificationCompat.PRIORITY_HIGH;
            }
            if (priority.equals("max")) {
                priorityInt = NotificationCompat.PRIORITY_MAX;
            }
        }

        // if we have not user set notificationID then try collapse key
        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
            try {
                Object collapse_key = extras.get("wzrk_ck");
                if(collapse_key != null) {
                    if (collapse_key instanceof Number) {
                        notificationId = ((Number) collapse_key).intValue();
                    } else if (collapse_key instanceof String) {
                        try {
                            notificationId = Integer.parseInt(collapse_key.toString());
                            getConfigLogger().debug(getAccountId(), "Converting collapse_key: " + collapse_key + " to notificationId int: " + notificationId);
                        } catch (NumberFormatException e) {
                            notificationId = (collapse_key.toString().hashCode());
                            getConfigLogger().debug(getAccountId(), "Converting collapse_key: " + collapse_key + " to notificationId int: " + notificationId);
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // no-op
            }
        } else {
            getConfigLogger().debug(getAccountId(), "Have user provided notificationId: " + notificationId + " won't use collapse_key (if any) as basis for notificationId");
        }

        // if after trying collapse_key notification is still empty set to random int
        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
            notificationId = (int) (Math.random() * 100);
            getConfigLogger().debug(getAccountId(), "Setting random notificationId: " + notificationId);
        }

        NotificationCompat.Builder nb;

        if (context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O){
            String channelId = extras.getString("wzrk_cid", "");
            if (channelId.isEmpty()) {
                getConfigLogger().debug(getAccountId(), "ChannelId is empty for notification: " + extras.toString());
            }
            nb = new NotificationCompat.Builder(context,channelId);

            // choices here are Notification.BADGE_ICON_NONE = 0, Notification.BADGE_ICON_SMALL = 1, Notification.BADGE_ICON_LARGE = 2.  Default is  Notification.BADGE_ICON_LARGE
            String badgeIconParam = extras.getString("wzrk_bi", null);
            if (badgeIconParam != null) {
                try {
                    int badgeIconType = Integer.parseInt(badgeIconParam);
                    if (badgeIconType >=0) {
                        nb.setBadgeIconType(badgeIconType);
                    }
                } catch (Throwable t) {
                    // no-op
                }
            }

            String badgeCountParam = extras.getString("wzrk_bc", null);
            if (badgeCountParam != null) {
                try {
                    int badgeCount = Integer.parseInt(badgeCountParam);
                    if (badgeCount >= 0) {
                        nb.setNumber(badgeCount);
                    }
                } catch (Throwable t) {
                    // no-op
                }
            }
        } else {
            nb = new NotificationCompat.Builder(context);
        }

        nb.setContentTitle(notifTitle)
                .setContentText(notifMessage)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setStyle(style)
                .setPriority(priorityInt)
                .setSmallIcon(smallIcon);

        nb.setLargeIcon(Utils.getNotificationBitmap(icoPath, true, context));

        try {
            if (extras.containsKey("wzrk_sound")) {
                Uri soundUri = null;

                Object o = extras.get("wzrk_sound");

                if ((o instanceof Boolean && (Boolean) o)) {
                    soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                } else if (o instanceof String) {
                    String s = (String) o;
                    if (s.equals("true")) {
                        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    } else if (!s.isEmpty()) {
                        if (s.contains(".mp3") || s.contains(".ogg") || s.contains(".wav")) {
                            s = s.substring(0, (s.length() - 4));
                        }
                        soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/" + s);

                    }
                }

                if (soundUri != null) {
                    nb.setSound(soundUri);
                }
            }
        } catch (Throwable t) {
            getConfigLogger().debug(getAccountId(), "Could not process sound parameter", t);
        }

        // add actions if any
        JSONArray actions = null;
        String actionsString = extras.getString("wzrk_acts");
        if (actionsString != null) {
            try {
                actions = new JSONArray(actionsString);
            } catch (Throwable t) {
                getConfigLogger().debug(getAccountId(), "error parsing notification actions: " + t.getLocalizedMessage());
            }
        }

        boolean isCTIntentServiceAvailable = isServiceAvailable(context,
                CTNotificationIntentService.MAIN_ACTION, CTNotificationIntentService.class);

        if (actions != null && actions.length() > 0) {
            for (int i = 0; i < actions.length(); i++) {
                try {
                    JSONObject action = actions.getJSONObject(i);
                    String label = action.optString("l");
                    String dl = action.optString("dl");
                    String ico = action.optString("ico");
                    String id = action.optString("id");
                    boolean autoCancel = action.optBoolean("ac", true);
                    if (label.isEmpty() || id.isEmpty()) {
                        getConfigLogger().debug(getAccountId(), "not adding push notification action: action label or id missing");
                        continue;
                    }
                    int icon = 0;
                    if (!ico.isEmpty()) {
                        try {
                            icon = context.getResources().getIdentifier(ico, "drawable", context.getPackageName());
                        } catch (Throwable t) {
                            getConfigLogger().debug(getAccountId(), "unable to add notification action icon: " + t.getLocalizedMessage());
                        }
                    }

                    boolean sendToCTIntentService = (autoCancel && isCTIntentServiceAvailable);

                    Intent actionLaunchIntent;
                    if (sendToCTIntentService) {
                        actionLaunchIntent = new Intent(CTNotificationIntentService.MAIN_ACTION);
                        actionLaunchIntent.putExtra("ct_type", CTNotificationIntentService.TYPE_BUTTON_CLICK);
                        if (!dl.isEmpty()) {
                            actionLaunchIntent.putExtra("dl", dl);
                        }
                    } else {
                        if (!dl.isEmpty()) {
                            actionLaunchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(dl));
                        } else {
                            actionLaunchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                        }
                    }

                    if (actionLaunchIntent != null) {
                        actionLaunchIntent.putExtras(extras);
                        actionLaunchIntent.removeExtra("wzrk_acts");
                        actionLaunchIntent.putExtra("actionId", id);
                        actionLaunchIntent.putExtra("autoCancel", autoCancel);
                        actionLaunchIntent.putExtra("wzrk_c2a", id);
                        actionLaunchIntent.putExtra("notificationId", notificationId);

                        actionLaunchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    }

                    PendingIntent actionIntent;
                    int requestCode = ((int) System.currentTimeMillis()) + i;
                    if (sendToCTIntentService) {
                        actionIntent = PendingIntent.getService(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    } else {
                        actionIntent = PendingIntent.getActivity(context, requestCode,
                                actionLaunchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    }
                    nb.addAction(icon, label, actionIntent);

                } catch (Throwable t) {
                    getConfigLogger().debug(getAccountId(), "error adding notification action : " + t.getLocalizedMessage());
                }
            }
        }

        Notification n = nb.build();

        getConfigLogger().debug(getAccountId(), "Building notification: " + n.toString() + ", with notificationId: " + String.valueOf(notificationId));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, n);
        }

    }

    private static boolean isServiceAvailable(Context context, String action, Class clazz) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List resolveInfo =
                packageManager.queryIntentServices(intent, 0);
        if (resolveInfo.size() > 0) {
            Logger.v("" + clazz.getName() + " is available");
            return true;
        }
        Logger.v("" + clazz.getName() + " is NOT available");
        return false;
    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     * @param channelName  A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance An Integer value setting the importance of the notifications sent in this channel
     * @param showBadge An boolean value as to whether this channel shows a badge
     *
     */
    @SuppressWarnings("unused")
    public static void createNotificationChannel(final Context context, final String channelId, final CharSequence channelName, final String channelDescription, final int importance, final boolean showBadge) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("createNotificationChannel", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;
                        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setShowBadge(showBadge);
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel " + channelName.toString() + " has been created");

                    }
                });
            }
        } catch (Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism and creating
     * notification channel groups. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     * @param channelName  A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance An Integer value setting the importance of the notifications sent in this
     *                   channel
     * @param groupId A String for setting the notification channel as a part of a notification
     *                group
     * @param showBadge An boolean value as to whether this channel shows a badge
     */
    @SuppressWarnings("unused")
    public static void createNotificationChannel(final Context context, final String channelId, final CharSequence channelName, final String channelDescription, final int importance, final String groupId, final boolean showBadge) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("creatingNotificationChannel", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;
                        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setGroup(groupId);
                        notificationChannel.setShowBadge(showBadge);
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel " + channelName.toString() + " has been created");

                    }
                });
            }
        } catch (Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     * @param channelName  A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance An Integer value setting the importance of the notifications sent in this channel
     * @param showBadge An boolean value as to whether this channel shows a badge
     * @param sound A String denoting the custom sound raw file for this channel
     */
    @SuppressWarnings("unused")
    public static void createNotificationChannel(final Context context, final String channelId, final CharSequence channelName, final String channelDescription, final int importance, final boolean showBadge, final String sound) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("createNotificationChannel", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;

                        String soundfile="";
                        Uri soundUri = null;

                        if (!sound.isEmpty()) {
                            if (sound.contains(".mp3") || sound.contains(".ogg") || sound.contains(".wav")) {
                                soundfile = sound.substring(0, (sound.length() - 4));
                            }else{
                                instance.getConfigLogger().debug(instance.getAccountId(),"Sound file name not supported");
                            }
                            if(!soundfile.isEmpty())
                                soundUri  = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/" + soundfile);

                        }

                        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setShowBadge(showBadge);
                        if(soundUri!=null) {
                            notificationChannel.setSound(soundUri, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                        }
                        else{
                            instance.getConfigLogger().debug(instance.getAccountId(),"Sound file not found, notification channel will be created without custom sound");
                        }
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel " + channelName.toString() + " has been created");

                    }
                });
            }
        } catch (Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure creating Notification Channel",t);
        }
    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism and creating
     * notification channel groups. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     * @param channelName  A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance An Integer value setting the importance of the notifications sent in this
     *                   channel
     * @param groupId A String for setting the notification channel as a part of a notification
     *                group
     * @param showBadge An boolean value as to whether this channel shows a badge
     * @param sound A String denoting the custom sound raw file for this channel
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void createNotificationChannel(final Context context, final String channelId, final CharSequence channelName, final String channelDescription, final int importance, final String groupId, final boolean showBadge, final String sound) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("creatingNotificationChannel", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {
                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;

                        String soundfile="";
                        Uri soundUri = null;

                        if (!sound.isEmpty()) {
                            if (sound.contains(".mp3") || sound.contains(".ogg") || sound.contains(".wav")) {
                                soundfile = sound.substring(0, (sound.length() - 4));
                            }else{
                                instance.getConfigLogger().debug(instance.getAccountId(),"Sound file name not supported");
                            }
                            if(!soundfile.isEmpty())
                                soundUri  = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/" + soundfile);

                        }
                        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setGroup(groupId);
                        notificationChannel.setShowBadge(showBadge);
                        if(soundUri!=null) {
                            notificationChannel.setSound(soundUri, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                        }
                        else{
                            instance.getConfigLogger().debug(instance.getAccountId(),"Sound file not found, notification channel will be created without custom sound");
                        }
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel " + channelName.toString() + " has been created");

                    }
                });
            }
        }catch(Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel group from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param groupId A String for setting the id of the notification channel group
     * @param groupName  A String for setting the name of the notification channel group
     */
    @SuppressWarnings("unused")
    public static void createNotificationChannelGroup(final Context context, final String groupId, final CharSequence groupName) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("creatingNotificationChannelGroup", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;
                        notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(groupId, groupName));
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel group " + groupName.toString() + " has been created");

                    }
                });
            }
        } catch (Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure creating Notification Channel Group", t);
        }
    }

    /**
     * Launches an asynchronous task to delete the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     */
    @SuppressWarnings("unused")
    public static void deleteNotificationChannel(final Context context, final String channelId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("deletingNotificationChannel", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;
                        notificationManager.deleteNotificationChannel(channelId);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel " + channelId + " has been deleted");

                    }
                });
            }
        } catch (Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure deleting Notification Channel", t);
        }
    }

    /**
     * Launches an asynchronous task to delete the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM/GCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param groupId A String for setting the id of the notification channel group
     */
    @SuppressWarnings("unused")
    public static void deleteNotificationChannelGroup(final Context context, final String groupId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.postAsyncSafely("deletingNotificationChannelGroup", new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void run() {

                        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) return;
                        notificationManager.deleteNotificationChannelGroup(groupId);
                        instance.getConfigLogger().info(instance.getAccountId(),"Notification channel group " + groupId + " has been deleted");

                    }
                });
            }
        }catch(Throwable t){
            instance.getConfigLogger().verbose(instance.getAccountId(),"Failure deleting Notification Channel Group", t);
        }
    }

    static void handleInstallReferrer(Context context, Intent intent){
        if (instances == null) {
            Logger.v("No CleverTap Instance found");
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                instance.pushInstallReferrer(intent);
            }
            return;
        }

        for (String accountId: CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            instance.pushInstallReferrer(intent);
        }

    }

    /**
     * This method is used to push install referrer via Intent
     * @param intent An Intent with the install referrer parameters
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushInstallReferrer(Intent intent) {
        try {
            final Bundle extras = intent.getExtras();
            // Preliminary checks
            if (extras == null || !extras.containsKey("referrer")) {
                return;
            }
            final String url;
            try {
                url = URLDecoder.decode(extras.getString("referrer"), "UTF-8");

                getConfigLogger().verbose(getAccountId(), "Referrer received: " + url);
            } catch (Throwable e) {
                // Could not decode
                return;
            }
            if (url == null) {
                return;
            }
            int now = (int) (System.currentTimeMillis() / 1000);

            if (installReferrerMap.containsKey(url) && now - installReferrerMap.get(url) < 10) {
                getConfigLogger().verbose(getAccountId(),"Skipping install referrer due to duplicate within 10 seconds");
                return;
            }

            installReferrerMap.put(url, now);

            Uri uri = Uri.parse("wzrk://track?install=true&" + url);

            pushDeepLink(uri, true);
        } catch (Throwable t) {
            // no-op
        }
    }

    /**
     * This method is used to push install referrer via UTM source, medium & campaign parameters
     * @param source The UTM source parameter
     * @param medium The UTM medium parameter
     * @param campaign The UTM campaign parameter
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public synchronized void pushInstallReferrer(String source, String medium, String campaign) {
        if (source == null && medium == null && campaign == null) return;
        try {
            // If already pushed, don't send it again
            int status = StorageHelper.getInt(context, "app_install_status", 0);
            if (status != 0) {
                Logger.d("Install referrer has already been set. Will not override it");
                return;
            }
            StorageHelper.putInt(context, "app_install_status", 1);

            if (source != null) source = Uri.encode(source);
            if (medium != null) medium = Uri.encode(medium);
            if (campaign != null) campaign = Uri.encode(campaign);

            String uriStr = "wzrk://track?install=true";
            if (source != null) uriStr += "&utm_source=" + source;
            if (medium != null) uriStr += "&utm_medium=" + medium;
            if (campaign != null) uriStr += "&utm_campaign=" + campaign;

            Uri uri = Uri.parse(uriStr);
            pushDeepLink(uri, true);
        } catch (Throwable t) {
            Logger.v("Failed to push install referrer", t);
        }
    }

    /**
     * This method is used to change the credentials of CleverTap account Id and token programmatically
     * @param accountID CleverTap Account Id
     * @param token CleverTap Account Token
     */
    @SuppressWarnings("unused")
    public static void changeCredentials(String accountID, String token) {
        changeCredentials(accountID, token, null);
    }

    /**
     * This method is used to change the credentials of CleverTap account Id, token and region programmatically
     * @param accountID CleverTap Account Id
     * @param token CleverTap Account Token
     * @param region Clever Tap Account Region
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void changeCredentials(String accountID, String token, String region) {
        if(defaultConfig != null){
            Logger.i("CleverTap SDK already initialized with accountID:"+defaultConfig.getAccountId()
                    +" and token:"+defaultConfig.getAccountToken()+". Cannot change credentials to "
                    + accountID + " and " + token);
            return;
        }

        ManifestInfo.changeCredentials(accountID,token,region);
    }

    /**
     * get the current device location
     * requires Location Permission in AndroidManifest e.g. "android.permission.ACCESS_COARSE_LOCATION"
     * You can then use the returned Location value to update the user profile location in CleverTap via {@link #setLocation(Location)}
     *
     * @return android.location.Location
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public Location getLocation() {
        return _getLocation();
    }

    /**
     * set the user profile location in CleverTap
     * location can then be used for geo-segmentation etc.
     *
     * @param location android.location.Location
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setLocation(Location location) {
        _setLocation(location);
    }

    /**
     * @deprecated use {@link #setLocation(Location)} ()} instead.
     */
    @Deprecated
    public void updateLocation(Location location) {
        _setLocation(location);
    }

    private void _setLocation(Location location) {
        if (location == null) return;

        locationFromUser = location;
        Logger.v("Location updated (" + location.getLatitude() + ", " + location.getLongitude() + ")");

        // only queue the location ping if we are in the foreground
        if (!isAppForeground()) return;

        // Queue the ping event to transmit location update to server
        // min 10 second interval between location pings
        final int now = (int) (System.currentTimeMillis() / 1000);
        if (now > (lastLocationPingTime + Constants.LOCATION_PING_INTERVAL_IN_SECONDS)) {
            queueEvent(context, new JSONObject(), Constants.PING_EVENT);
            lastLocationPingTime = now;
            Logger.v("Queuing location ping event for location (" + location.getLatitude() + ", " + location.getLongitude() + ")");
        }
    }

    @SuppressLint("MissingPermission")
    private Location _getLocation() {
        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) {
                Logger.d("Location Manager is null.");
                return null;
            }
            List<String> providers = lm.getProviders(true);
            Location bestLocation = null;
            Location l = null;
            for (String provider : providers) {
                try {
                    l = lm.getLastKnownLocation(provider);
                } catch (SecurityException e) {
                    //no-op
                    Logger.v("Location security exception", e);
                }

                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    bestLocation = l;
                }
            }

            return bestLocation;
        } catch (Throwable t) {
            Logger.v("Couldn't get user's location", t);
            return null;
        }
    }

    //Notification Inbox public APIs
    public void initializeInbox(){
        postAsyncSafely("initializeInbox", new Runnable() {
            @Override
            public void run() {
                if(getConfig().isAnalyticsOnly()){
                    getConfigLogger().debug(getAccountId(),"Instance is analytics only, not initializing Notification Inbox");
                }
                synchronized (inboxControllerLock) {
                    if (ctInboxController != null) {
                        getConfigLogger().debug(getAccountId(), "Notification Inbox initialized");
                    } else {
                        ctInboxController = CTInboxController.initWithAccountId(getAccountId(), getAccountId(), loadDBAdapter(context));
                    }
                }
            }
        });

    }

    public int getInboxMessageCount(){
       if(isInboxInitialized()) {
           synchronized (inboxControllerLock) {
               return ctInboxController.count();
           }
       }else{
           getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
           return -1;
       }

    }

    public int getInboxMessageUnreadCount(){
        if(isInboxInitialized()) {
            synchronized (inboxControllerLock) {
                return ctInboxController.unreadCount();
            }
        }else{
            getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
            return -1;
        }

    }

    public CTInboxMessage getInboxMessageForId(String messageId){
        if(isInboxInitialized()) {
            synchronized (inboxControllerLock) {
                JSONObject messageJson = ctInboxController.getMessageForId(messageId);
                return new CTInboxMessage().initWithJSON(messageJson);
            }
        }else{
            getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
            return null;
        }
    }

    public void deleteInboxMessage(final CTInboxMessage message){
        postAsyncSafely("deleteInboxMessage", new Runnable() {
            @Override
            public void run() {
                if (isInboxInitialized()) {
                    synchronized (inboxControllerLock) {
                        ctInboxController.deleteMessageWithId(message.getMessageId());
                    }
                } else {
                    getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                }
            }});
    }

    public void markReadInboxMessage(final CTInboxMessage message){
        postAsyncSafely("markReadInboxMessage", new Runnable() {
            @Override
            public void run() {
                if(isInboxInitialized()){
                    synchronized (inboxControllerLock) {
                        ctInboxController.markReadForMessageWithId(message.getMessageId());
                    }
                }else{
                    getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
                }
            }
        });
    }

    public ArrayList<CTInboxMessage> getUnreadInboxMessages(){
        ArrayList<CTInboxMessage> inboxMessageArrayList = new ArrayList<>();
        if(isInboxInitialized()){
            synchronized (inboxControllerLock) {
                ArrayList<CTMessageDAO> messageDAOArrayList = ctInboxController.getUnreadMessages();
                for (CTMessageDAO messageDAO : messageDAOArrayList) {
                    inboxMessageArrayList.add(new CTInboxMessage().initWithJSON(messageDAO.toJSON()));
                }
                return inboxMessageArrayList;
            }
        }else{
            getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
            return null;
        }

    }

    public ArrayList<CTInboxMessage> getAllInboxMessages(){
        ArrayList<CTInboxMessage> inboxMessageArrayList = new ArrayList<>();
        if(isInboxInitialized()){
            synchronized (inboxControllerLock) {
                ArrayList<CTMessageDAO> messageDAOArrayList = ctInboxController.getMessages();
                for (CTMessageDAO messageDAO : messageDAOArrayList) {
                    inboxMessageArrayList.add(new CTInboxMessage().initWithJSON(messageDAO.toJSON()));
                }
                return inboxMessageArrayList;
            }
        }else{
            getConfigLogger().debug(getAccountId(),"Notification Inbox not initialized");
            return null;
        }

    }

    public void createNotificationInboxActivity(CTInboxStyleConfig styleConfig){
        Intent intent = new Intent(context,CTNotificationInboxActivity.class);
        intent.putExtra("styleConfig",styleConfig);
        intent.putExtra("config",config);
        try {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                throw new IllegalStateException("Current activity reference not found");
            }
            currentActivity.startActivity(intent);
            Logger.d("Displaying Notification Inbox");

        } catch (Throwable t) {
            Logger.v("Please verify the integration of your app." +
                    " It is not setup to support Notification Inbox yet.", t);
        }
    }

    public void createNotificationInboxActivity(){
        CTInboxStyleConfig styleConfig = new CTInboxStyleConfig();
        styleConfig.setTitleColor(Integer.toString(Color.BLACK));
        styleConfig.setBodyColor(Integer.toString(Color.BLACK));
        styleConfig.setLayoutColor(Integer.toString(Color.WHITE));
        styleConfig.setCtaColor(Integer.toString(Color.BLUE));
        ArrayList<CTInboxMessage> inboxMessageArrayList = getAllInboxMessages();
        Intent intent = new Intent(context,CTNotificationInboxActivity.class);
        intent.putExtra("styleConfig",styleConfig);
        intent.putExtra("config",config);
        intent.putExtra("messageList",inboxMessageArrayList);
        try {
            Activity currentActivity = getCurrentActivity();
            if (currentActivity == null) {
                throw new IllegalStateException("Current activity reference not found");
            }
            currentActivity.startActivity(intent);
            currentActivity.overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            Logger.d("Displaying Notification Inbox");

        } catch (Throwable t) {
            Logger.v("Please verify the integration of your app." +
                    " It is not setup to support Notification Inbox yet.", t);
        }
    }

    //Notification Inbox Private APIs
    private void resetInbox(){
        synchronized (inboxControllerLock) {
            this.ctInboxController = CTInboxController.initWithAccountId(getAccountId(), getCleverTapID(), loadDBAdapter(context));
            if (this.ctInboxController != null && ctInboxController.isInitialized()) {
                this.ctInboxController.listener = new WeakReference<>(this).get();
            }
        }
    }

    private boolean isInboxInitialized(){
        if(getConfig().isAnalyticsOnly()){
            getConfigLogger().debug(getAccountId(),"Instance is analytics only, not initializing Notification Inbox");
            return false;
        }
        synchronized (inboxControllerLock) {
            if (this.ctInboxController != null) {
                return this.ctInboxController.isInitialized();
            } else {
                return false;
            }
        }
    }

}
