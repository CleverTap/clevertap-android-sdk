package com.clevertap.android.sdk.pushnotification;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static com.clevertap.android.sdk.BuildConfig.VERSION_CODE;
import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getPushTypes;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobScheduler;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.clevertap.android.sdk.AnalyticsManager;
import com.clevertap.android.sdk.CTXtensions;
import com.clevertap.android.sdk.CleverTapAPI.DevicePushTokenRefreshListener;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.ControllerManager;
import com.clevertap.android.sdk.DeviceInfo;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.ManifestInfo;
import com.clevertap.android.sdk.StorageHelper;
import com.clevertap.android.sdk.Utils;
import com.clevertap.android.sdk.db.BaseDatabaseManager;
import com.clevertap.android.sdk.db.DBAdapter;
import com.clevertap.android.sdk.interfaces.AudibleNotification;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpWorker;
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.PackageUtils;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultFactory;
import com.clevertap.android.sdk.validation.ValidationResultStack;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Single point of contact to load & support all types of Notification messaging services viz. FCM, XPS, HMS etc.
 */

@RestrictTo(Scope.LIBRARY_GROUP)
public class PushProviders implements CTPushProviderListener {

    private static final int DEFAULT_FLEX_INTERVAL = 5;
    private static final int PING_FREQUENCY_VALUE = 240;
    private static final String PF_JOB_ID = "pfjobid";
    private static final String PF_WORK_ID = "pfworkid";
    private static final String PING_FREQUENCY = "pf";
    private static final String inputFormat = "HH:mm";

    private final ArrayList<PushType> allEnabledPushTypes = new ArrayList<>();

    private final ArrayList<PushType> allDisabledPushTypes = new ArrayList<>();

    private final ArrayList<CTPushProvider> availableCTPushProviders = new ArrayList<>();

    private final ArrayList<PushType> customEnabledPushTypes = new ArrayList<>();

    private final AnalyticsManager analyticsManager;

    private final BaseDatabaseManager baseDatabaseManager;

    private final CleverTapInstanceConfig config;

    private final Context context;
    private final CTWorkManager ctWorkManager;

    private INotificationRenderer iNotificationRenderer = new CoreNotificationRenderer();

    private final ValidationResultStack validationResultStack;

    private final Object tokenLock = new Object();

    private final Object pushRenderingLock = new Object();

    private DevicePushTokenRefreshListener tokenRefreshListener;

    /**
     * Factory method to load push providers.
     *
     * @return A PushProviders class with the loaded providers.
     */
    @NonNull
    public static PushProviders load(Context context,
                                     CleverTapInstanceConfig config,
                                     BaseDatabaseManager baseDatabaseManager,
                                     ValidationResultStack validationResultStack,
                                     AnalyticsManager analyticsManager, ControllerManager controllerManager,
                                     CTWorkManager ctWorkManager) {
        PushProviders providers = new PushProviders(context, config, baseDatabaseManager, validationResultStack,
                analyticsManager,ctWorkManager);
        providers.init();
        controllerManager.setPushProviders(providers);
        return providers;
    }

    private PushProviders(
            Context context,
            CleverTapInstanceConfig config,
            BaseDatabaseManager baseDatabaseManager,
            ValidationResultStack validationResultStack,
            AnalyticsManager analyticsManager, CTWorkManager ctWorkManager) {
        this.context = context;
        this.config = config;
        this.baseDatabaseManager = baseDatabaseManager;
        this.validationResultStack = validationResultStack;
        this.analyticsManager = analyticsManager;
        this.ctWorkManager = ctWorkManager;
        initPushAmp();
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p/>
     * If your app is using CleverTap SDK's built in FCM message handling,
     * this method does not need to be called explicitly.
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context        A reference to an Android context
     * @param extras         The {@link Bundle} object received by the broadcast receiver
     * @param notificationId A custom id to build a notification
     */
    public void _createNotification(final Context context, final Bundle extras, final int notificationId) {
        if (extras == null || extras.get(Constants.NOTIFICATION_TAG) == null) {
            return;
        }

        if (config.isAnalyticsOnly()) {
            config.getLogger()
                    .debug(config.getAccountId(),
                            "Instance is set for Analytics only, cannot create notification");
            return;
        }

        try {
            boolean isSilent = extras.getString(Constants.WZRK_PUSH_SILENT, "").equalsIgnoreCase("true");
            if (isSilent) {
                analyticsManager.pushNotificationViewedEvent(extras);
                return;
            }
            String extrasFrom = extras.getString(Constants.EXTRAS_FROM);
            if (extrasFrom == null || !extrasFrom.equals("PTReceiver")) {
                config.getLogger()
                        .debug(config.getAccountId(),
                                "Handling notification: " + extras);

                if (extras.getString(Constants.WZRK_PUSH_ID) != null) {
                    if (baseDatabaseManager.loadDBAdapter(context)
                            .doesPushNotificationIdExist(
                                    extras.getString(Constants.WZRK_PUSH_ID))) {
                        config.getLogger().debug(config.getAccountId(),
                                "Push Notification already rendered, not showing again");
                        return;
                    }
                }
                String notifMessage = iNotificationRenderer.getMessage(extras);
                notifMessage = (notifMessage != null) ? notifMessage : "";
                if (notifMessage.isEmpty()) {
                    //silent notification
                    config.getLogger()
                            .verbose(config.getAccountId(),
                                    "Push notification message is empty, not rendering");
                    baseDatabaseManager.loadDBAdapter(context)
                            .storeUninstallTimestamp();
                    String pingFreq = extras.getString("pf", "");
                    if (!TextUtils.isEmpty(pingFreq)) {
                        updatePingFrequencyIfNeeded(context, Integer.parseInt(pingFreq));
                    }
                    return;
                }
            }

            String notifTitle = iNotificationRenderer.getTitle(extras,
                    context);//extras.getString(Constants.NOTIF_TITLE, "");// uncommon - getTitle()
            notifTitle = notifTitle.isEmpty() ? context.getApplicationInfo().name
                    : notifTitle;//common
            triggerNotification(context, extras, notificationId);
        } catch (Throwable t) {
            // Occurs if the notification image was null
            // Let's return, as we couldn't get a handle on the app's icon
            // Some devices throw a PackageManager* exception too
            config.getLogger()
                    .debug(config.getAccountId(), "Couldn't render notification: ", t);
        }
    }

    /**
     * Saves token for a push type into shared pref
     *
     * @param token    - Messaging token
     * @param pushType - Pushtype, Ref{@link PushType}
     */
    public void cacheToken(final String token, final PushType pushType) {
        if (TextUtils.isEmpty(token) || pushType == null) {
            return;
        }
//
        try {
            Task<Void> task = CTExecutorFactory.executors(config).ioTask();
            task.execute("PushProviders#cacheToken", new Callable<Void>() {
                @Override
                public Void call() {
                    if (alreadyHaveToken(token, pushType)) {
                        return null;
                    }
                    @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
                    if (TextUtils.isEmpty(key)) {
                        return null;
                    }
                    StorageHelper
                            .putStringImmediate(context, StorageHelper.storageKeyWithSuffix(config, key), token);
                    config.log(PushConstants.LOG_TAG, pushType + "Cached New Token successfully " + token);
                    return null;
                }
            });

        } catch (Throwable t) {
            config.log(PushConstants.LOG_TAG, pushType + "Unable to cache token " + token, t);
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public void doTokenRefresh(String token, PushType pushType) {
        if (TextUtils.isEmpty(token) || pushType == null) {
            return;
        }
        switch (pushType) {
            case FCM:
                handleToken(token, PushType.FCM, true);
                break;
            case XPS:
                handleToken(token, PushType.XPS, true);
                break;
            case HPS:
                handleToken(token, PushType.HPS, true);
                break;
            case BPS:
                handleToken(token, PushType.BPS, true);
                break;
            case ADM:
                handleToken(token, PushType.ADM, true);
                break;
        }
    }

    /**
     * push the device token outside of the normal course
     */
    @RestrictTo(Scope.LIBRARY)
    public void forcePushDeviceToken(final boolean register) {

        for (PushType pushType : allEnabledPushTypes) {
            pushDeviceTokenEvent(null, register, pushType);
        }
    }

    /**
     * @return list of all available push types, contains ( Clevertap's plugin + Custom supported Push Types)
     */
    @NonNull
    public ArrayList<PushType> getAvailablePushTypes() {
        ArrayList<PushType> pushTypes = new ArrayList<>();
        for (CTPushProvider pushProvider : availableCTPushProviders) {
            pushTypes.add(pushProvider.getPushType());
        }
        return pushTypes;
    }

    /**
     * @param pushType - Pushtype {@link PushType}
     * @return Messaging token for a particular push type
     */
    public String getCachedToken(PushType pushType) {
        if (pushType != null) {
            @PushConstants.RegKeyType String key = pushType.getTokenPrefKey();
            if (!TextUtils.isEmpty(key)) {
                String cachedToken = StorageHelper.getStringFromPrefs(context, config, key, null);
                config.log(PushConstants.LOG_TAG, pushType + "getting Cached Token - " + cachedToken);
                return cachedToken;
            }
        }
        if (pushType != null) {
            config.log(PushConstants.LOG_TAG, pushType + " Unable to find cached Token for type ");
        }
        return null;
    }

    public DevicePushTokenRefreshListener getDevicePushTokenRefreshListener() {
        return tokenRefreshListener;
    }

    public void setDevicePushTokenRefreshListener(final DevicePushTokenRefreshListener tokenRefreshListener) {
        this.tokenRefreshListener = tokenRefreshListener;
    }

    /**
     * Direct Method to send tokens to Clevertap's server
     * Call this method when Clients are handling the Messaging services on their own
     *
     * @param token    - Messaging token
     * @param pushType - Pushtype, Ref:{@link PushType}
     * @param register - true if we want to register the token to CT server
     *                 false if we want to unregister the token from CT server
     */
    public void handleToken(String token, PushType pushType, boolean register) {
        if (register) {
            registerToken(token, pushType);
        } else {
            unregisterToken(token, pushType);
        }
    }

    /**
     * @return true if we are able to reach the device via any of the messaging service
     */
    public boolean isNotificationSupported() {
        for (PushType pushType : getAvailablePushTypes()) {
            if (getCachedToken(pushType) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNewToken(String freshToken, PushType pushType) {
        if (!TextUtils.isEmpty(freshToken)) {
            doTokenRefresh(freshToken, pushType);
            deviceTokenDidRefresh(freshToken, pushType);
        }
    }

    //Push
    public void onTokenRefresh() {
        refreshAllTokens();
    }

    /**
     * Stores silent push notification in DB for smooth working of Pull Notifications
     * Background Job Service and also stores wzrk_pid to the DB to avoid duplication of Push
     * Notifications from Pull Notifications.
     *
     * @param extras - Bundle
     */
    public void processCustomPushNotification(final Bundle extras) {
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("customHandlePushAmplification", new Callable<Void>() {
            @Override
            public Void call() {
                String notifMessage = extras.getString(Constants.NOTIF_MSG);
                notifMessage = (notifMessage != null) ? notifMessage : "";
                if (notifMessage.isEmpty()) {
                    //silent notification
                    config.getLogger()
                            .verbose(config.getAccountId(), "Push notification message is empty, not rendering");
                    baseDatabaseManager.loadDBAdapter(context).storeUninstallTimestamp();
                    String pingFreq = extras.getString("pf", "");
                    if (!TextUtils.isEmpty(pingFreq)) {
                        updatePingFrequencyIfNeeded(context, Integer.parseInt(pingFreq));
                    }
                } else {
                    String wzrk_pid = extras.getString(Constants.WZRK_PUSH_ID);
                    String ttl = extras.getString(Constants.WZRK_TIME_TO_LIVE,
                            (System.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL) / 1000 + "");
                    long wzrk_ttl = Long.parseLong(ttl);
                    DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
                    config.getLogger().verbose("Storing Push Notification..." + wzrk_pid + " - with ttl - " + ttl);
                    dbAdapter.storePushNotificationId(wzrk_pid, wzrk_ttl);
                }
                return null;
            }
        });
    }

    /**
     * Unregister the token for a push type from Clevertap's server.
     * Devices with unregistered token wont be reachable.
     *
     * @param token    - Messaging token
     * @param pushType - pushtype Ref:{@link PushType}
     */
    public void unregisterToken(String token, PushType pushType) {
        pushDeviceTokenEvent(token, false, pushType);
    }

    /**
     * updates the ping frequency if there is a change & reschedules existing ping tasks.
     */
    public void updatePingFrequencyIfNeeded(final Context context, int frequency) {
        config.getLogger().verbose("Ping frequency received - " + frequency);
        config.getLogger().verbose("Stored Ping Frequency - " + getPingFrequency(context));
        if (frequency != getPingFrequency(context)) {
            setPingFrequency(context, frequency);
            if (config.isBackgroundSync() && !config.isAnalyticsOnly()) {
                Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
                task.execute("createOrResetWorker", new Callable<Void>() {
                    @Override
                    public Void call() {
                        createOrResetWorker(true);
                        return null;
                    }
                });
            }
        }
    }

    private boolean alreadyHaveToken(String newToken, PushType pushType) {
        boolean alreadyAvailable = !TextUtils.isEmpty(newToken) && pushType != null && newToken
                .equalsIgnoreCase(getCachedToken(pushType));
        if (pushType != null) {
            config.log(PushConstants.LOG_TAG, pushType + "Token Already available value: " + alreadyAvailable);
        }
        return alreadyAvailable;
    }

    public void runPushAmpWork(final Context context) {
        Logger.v(config.getAccountId(), "Pushamp - Running work request");
        if (!isNotificationSupported()) {
            Logger.v(config.getAccountId(), "Pushamp - Token is not present, not running the work request");
            return;
        }

        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY); // Get hour in 24 hour format
        int minute = now.get(Calendar.MINUTE);

        final SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat, Locale.US);

        Date currentTime = parseTimeToDate(hour + ":" + minute, inputParser);
        Date startTime = parseTimeToDate(Constants.DND_START, inputParser);
        Date endTime = parseTimeToDate(Constants.DND_STOP, inputParser);

        if (isTimeBetweenDNDTime(startTime, endTime, currentTime)) {
            Logger.v(config.getAccountId(), "Pushamp won't run in default DND hours");
            return;
        }

        long lastTS = baseDatabaseManager.loadDBAdapter(context).getLastUninstallTimestamp();

        if (lastTS == 0 || lastTS > System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
            try {
                JSONObject eventObject = new JSONObject();
                eventObject.put("bk", 1);
                analyticsManager.sendPingEvent(eventObject);
                Logger.v(config.getAccountId(), "Pushamp - Successfully completed work request");
            } catch (JSONException e) {
                Logger.v("Pushamp - Unable to complete work request");
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private void stopJobScheduler(Context context) {
        int existingJobId = StorageHelper.getInt(context, PF_JOB_ID, -1);
        if (existingJobId != -1) {
//          Cancel already running job. Possibly unnecessary
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(existingJobId);
            StorageHelper.remove(context, PF_JOB_ID);
        }
    }

    private void createOrResetWorker(boolean isPingFrequencyUpdated) {
        //Disable push amp for devices below Api 26
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            config.getLogger().debug(config.getAccountId(), "Pushamp feature is not supported below Oreo");
            return;
        }

        String existingWorkName = StorageHelper.getString(context, PF_WORK_ID, "");
        int pingFrequency = getPingFrequency(context);

        // no running work and nothing to create
        if (existingWorkName.equals("") && pingFrequency <= 0) {
            config.getLogger()
                    .debug(config.getAccountId(), "Pushamp - There is no running work and nothing to create");
            return;
        }

        // Running work exists but hard cancel
        if (pingFrequency <= 0) {
            config.getLogger()
                    .debug(config.getAccountId(), "Pushamp - Cancelling worker as pingFrequency <=0 ");
            stopWorker();
            return;
        }

        try {
            WorkManager workManager = WorkManager.getInstance(context);

            // Create a work request only when it doesn't exist already or the ping frequency is updated
            if (existingWorkName.equals("") || isPingFrequencyUpdated) {
                Constraints constraints = new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresCharging(false)
                        .setRequiresBatteryNotLow(true)
                        .build();

                PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(CTPushAmpWorker.class, pingFrequency,
                        TimeUnit.MINUTES, DEFAULT_FLEX_INTERVAL, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build();

                String workName = existingWorkName.equals("") ? config.getAccountId() : existingWorkName;

                workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.REPLACE, request);
                StorageHelper.putString(context, PF_WORK_ID, workName);
                config.getLogger().debug(config.getAccountId(),
                        "Pushamp - Finished scheduling periodic work request - " + workName + " with repeatInterval- "
                                + pingFrequency + " minutes");
            }
        } catch (Exception e) {
            config.getLogger().debug(config.getAccountId(),
                    "Pushamp - Failed scheduling/cancelling periodic work request" + e);
        }
    }

    private void stopWorker() {
        String existingWorkName = StorageHelper.getString(context, PF_WORK_ID, "");
        if (!existingWorkName.equals("")) {
            try {
                WorkManager workManager = WorkManager.getInstance(context);
                workManager.cancelUniqueWork(existingWorkName);
                StorageHelper.putString(context, PF_WORK_ID, "");
                config.getLogger().debug(config.getAccountId(),
                        "Pushamp - Successfully cancelled work");
            } catch (Exception e) {
                config.getLogger().debug(config.getAccountId(),
                        "Pushamp - Failure while cancelling work");
            }
        }
    }

    /**
     * Creates the list of push providers.
     *
     * @return The list of push providers.
     */
    @NonNull
    private List<CTPushProvider> createProviders() {
        List<CTPushProvider> providers = new ArrayList<>();

        for (PushType pushType : allEnabledPushTypes) {
            CTPushProvider pushProvider = getCTPushProviderFromPushType(pushType, true);

            if (pushProvider == null) {
                continue;
            }

            providers.add(pushProvider);
        }

        for (PushType pushType : allDisabledPushTypes) {
            // only for XPS, if for disabled push cached token already exists then unregister xiaomi push
            // for case like user enables xps on all devices first then in next app version disables on all devices

            if (pushType == PushType.XPS) {
                String cachedTokenXps = getCachedToken(PushType.XPS);
                if (!TextUtils.isEmpty(cachedTokenXps)) {
                    CTPushProvider pushProvider = getCTPushProviderFromPushType(pushType, false);

                    if (pushProvider instanceof UnregistrableCTPushProvider) {
                        ((UnregistrableCTPushProvider) pushProvider).unregisterPush(context);
                        config.log(PushConstants.LOG_TAG, "unregistering existing token for disabled " + pushType);
                    }
                }
            }

        }

        return providers;
    }

    /**
     * This code can be moved to {@link PushType} but this is creating new instance of CTPushProvider for each
     * execution,
     * and to prevent multiple instance of same CTPushProvider not moving this to {@link PushType}
     */
    @Nullable
    private CTPushProvider getCTPushProviderFromPushType(final PushType pushType, final boolean isInit) {
        String className = pushType.getCtProviderClassName();
        CTPushProvider pushProvider = null;
        try {
            Class<?> providerClass = Class.forName(className);

            if (isInit) {
                Constructor<?> constructor = providerClass
                        .getConstructor(CTPushProviderListener.class, Context.class, CleverTapInstanceConfig.class);
                pushProvider = (CTPushProvider) constructor.newInstance(this, context, config);

            } else {
                Constructor<?> constructor = providerClass
                        .getConstructor(CTPushProviderListener.class, Context.class, CleverTapInstanceConfig.class,
                                Boolean.class);
                pushProvider = (CTPushProvider) constructor.newInstance(this, context, config, false);
            }
            config.log(PushConstants.LOG_TAG, "Found provider:" + className);
        } catch (InstantiationException e) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider InstantiationException" + className);
        } catch (IllegalAccessException e) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider IllegalAccessException" + className);
        } catch (ClassNotFoundException e) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider ClassNotFoundException" + className);
        } catch (Exception e) {
            config.log(PushConstants.LOG_TAG,
                    "Unable to create provider " + className + " Exception:" + e.getClass().getName());
        }
        return pushProvider;
    }

    //Push
    @SuppressWarnings("SameParameterValue")
    private void deviceTokenDidRefresh(String token, PushType type) {
        if (tokenRefreshListener != null) {
            config.getLogger().debug(config.getAccountId(), "Notifying devicePushTokenDidRefresh: " + token);
            tokenRefreshListener.devicePushTokenDidRefresh(token, type);
        }
    }

    private void findCTPushProviders(List<CTPushProvider> providers) {
        if (providers.isEmpty()) {
            config.log(PushConstants.LOG_TAG,
                    "No push providers found!. Make sure to install at least one push provider");
            return;
        }

        for (CTPushProvider provider : providers) {
            if (!isValid(provider)) {
                config.log(PushConstants.LOG_TAG, "Invalid Provider: " + provider.getClass());
                continue;
            }

            if (!provider.isSupported()) {
                config.log(PushConstants.LOG_TAG, "Unsupported Provider: " + provider.getClass());
                continue;
            }

            if (provider.isAvailable()) {
                config.log(PushConstants.LOG_TAG, "Available Provider: " + provider.getClass());
                availableCTPushProviders.add(provider);
            } else {
                config.log(PushConstants.LOG_TAG, "Unavailable Provider: " + provider.getClass());
            }
        }
    }

    private void findCustomEnabledPushTypes() {
        customEnabledPushTypes.addAll(allEnabledPushTypes);
        for (final CTPushProvider pushProvider : availableCTPushProviders) {
            customEnabledPushTypes.remove(pushProvider.getPushType());
        }
    }

    //Session

    private void findEnabledPushTypes() {
        for (PushType pushType : getPushTypes(config.getAllowedPushTypes())) {
            String className = pushType.getMessagingSDKClassName();
            try {
                Class.forName(className);
                allEnabledPushTypes.add(pushType);
                config.log(PushConstants.LOG_TAG, "SDK Class Available :" + className);

                // if push is off on all devices then remove xps
                if (pushType.getRunningDevices() == PushConstants.NO_DEVICES) {
                    allEnabledPushTypes.remove(pushType);
                    allDisabledPushTypes.add(pushType);
                    config.log(PushConstants.LOG_TAG,
                            "disabling " + pushType + " due to flag set as PushConstants.NO_DEVICES");
                }
                // if push is off for non-xiaomi devices then remove xps
                if (pushType.getRunningDevices() == PushConstants.XIAOMI_MIUI_DEVICES) {
                    if (!PackageUtils.isXiaomiDeviceRunningMiui(context)) {
                        allEnabledPushTypes.remove(pushType);
                        allDisabledPushTypes.add(pushType);
                        config.log(PushConstants.LOG_TAG,
                                "disabling " + pushType + " due to flag set as PushConstants.XIAOMI_MIUI_DEVICES");
                    }
                }

            } catch (Exception e) {
                config.log(PushConstants.LOG_TAG,
                        "SDK class Not available " + className + " Exception:" + e.getClass().getName());
            }
        }
    }

    private int getPingFrequency(Context context) {
        return StorageHelper.getInt(context, PING_FREQUENCY,
                PING_FREQUENCY_VALUE);
    }

    /**
     * Loads all the plugins that are currently supported by the device.
     */
    private void init() {
        findEnabledPushTypes();
        List<CTPushProvider> providers = createProviders();
        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();

        task.addOnSuccessListener(unused -> findCustomEnabledPushTypes());
        task.execute("asyncFindCTPushProviders", () -> {
            findCTPushProviders(providers);
            return null;
        });

    }

    private void initPushAmp() {
        // Prevent PushAmp initialisation on xiaomi's thread
        if(!Utils.isMainProcess(context, context.getPackageName()))
            return;

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("createOrResetWorker", new Callable<Void>() {
            @Override
            public Void call() {
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    stopJobScheduler(context);
                }
                if (config.isBackgroundSync() && !config.isAnalyticsOnly()) {
                    createOrResetWorker(false);
                } else {
                    config.getLogger()
                            .debug(config.getAccountId(), "Pushamp - Cancelling worker as background sync is disabled or config is analytics only");
                    stopWorker();
                }
                return null;
            }
        });
    }

    private boolean isTimeBetweenDNDTime(Date startTime, Date stopTime, Date currentTime) {
        //Start Time
        Calendar startTimeCalendar = Calendar.getInstance();
        startTimeCalendar.setTime(startTime);
        //Current Time
        Calendar currentTimeCalendar = Calendar.getInstance();
        currentTimeCalendar.setTime(currentTime);
        //Stop Time
        Calendar stopTimeCalendar = Calendar.getInstance();
        stopTimeCalendar.setTime(stopTime);

        if (stopTime.compareTo(startTime) < 0) {
            if (currentTimeCalendar.compareTo(stopTimeCalendar) < 0) {
                currentTimeCalendar.add(Calendar.DATE, 1);
            }
            stopTimeCalendar.add(Calendar.DATE, 1);
        }
        return currentTimeCalendar.compareTo(startTimeCalendar) >= 0
                && currentTimeCalendar.compareTo(stopTimeCalendar) < 0;
    }

    private boolean isValid(CTPushProvider provider) {

        if (VERSION_CODE < provider.minSDKSupportVersionCode()) {
            config.log(PushConstants.LOG_TAG,
                    "Provider: %s version %s does not match the SDK version %s. Make sure all CleverTap dependencies are the same version.");
            return false;
        }
        switch (provider.getPushType()) {
            case FCM:
            case HPS:
            case XPS:
            case BPS:
                if (provider.getPlatform() != PushConstants.ANDROID_PLATFORM) {
                    config.log(PushConstants.LOG_TAG, "Invalid Provider: " + provider.getClass() +
                            " delivery is only available for Android platforms." + provider.getPushType());
                    return false;
                }
                break;
            case ADM:
                if (provider.getPlatform() != PushConstants.AMAZON_PLATFORM) {
                    config.log(PushConstants.LOG_TAG, "Invalid Provider: " +
                            provider.getClass() +
                            " ADM delivery is only available for Amazon platforms." + provider.getPushType());
                    return false;
                }
                break;
        }

        return true;
    }

    private Date parseTimeToDate(String time, SimpleDateFormat inputParser) {
        try {
            return inputParser.parse(time);
        } catch (java.text.ParseException e) {
            return new Date(0);
        }
    }

    private void pushDeviceTokenEvent(String token, boolean register, PushType pushType) {
        if (pushType == null) {
            return;
        }
        token = !TextUtils.isEmpty(token) ? token : getCachedToken(pushType);
        if (TextUtils.isEmpty(token)) {
            return;
        }
        synchronized (tokenLock) {
            JSONObject event = new JSONObject();
            JSONObject data = new JSONObject();
            String action = register ? "register" : "unregister";
            try {
                data.put("action", action);
                data.put("id", token);
                data.put("type", pushType.getType());
                if (pushType == PushType.XPS) {
                    config.getLogger().verbose("PushProviders: pushDeviceTokenEvent requesting device region");
                    data.put("region", pushType.getServerRegion());
                }
                event.put("data", data);
                config.getLogger().verbose(config.getAccountId(), pushType + action + " device token " + token);
                analyticsManager.sendDataEvent(event);
            } catch (Throwable t) {
                // we won't get here
                config.getLogger().verbose(config.getAccountId(), pushType + action + " device token failed", t);
            }
        }
    }

    /**
     * Fetches latest tokens from various providers and send to Clevertap's server
     */
    private void refreshAllTokens() {
        Task<Void> task = CTExecutorFactory.executors(config).ioTask();
        task.execute("PushProviders#refreshAllTokens", new Callable<Void>() {
            @Override
            public Void call() {
                // refresh tokens of Push Providers
                refreshCTProviderTokens();

                // refresh tokens of custom Providers
                refreshCustomProviderTokens();
                return null;
            }
        });
    }

    private void refreshCTProviderTokens() {
        for (final CTPushProvider pushProvider : availableCTPushProviders) {
            try {
                pushProvider.requestToken();
            } catch (Throwable t) {
                //no-op
                config.log(PushConstants.LOG_TAG, "Token Refresh error " + pushProvider, t);
            }
        }
    }

    private void refreshCustomProviderTokens() {
        for (PushType pushType : customEnabledPushTypes) {
            try {
                pushDeviceTokenEvent(getCachedToken(pushType), true, pushType);
            } catch (Throwable t) {
                config.log(PushConstants.LOG_TAG, "Token Refresh error " + pushType, t);
            }
        }
    }

    private void registerToken(String token, PushType pushType) {
        pushDeviceTokenEvent(token, true, pushType);
        cacheToken(token, pushType);
    }

    private void setPingFrequency(Context context, int pingFrequency) {
        StorageHelper.putInt(context, PING_FREQUENCY, pingFrequency);
    }

    @RestrictTo(Scope.LIBRARY)
    public @NonNull
    INotificationRenderer getPushNotificationRenderer() {
        return iNotificationRenderer;
    }

    @RestrictTo(Scope.LIBRARY)
    public @NonNull
    Object getPushRenderingLock() {
        return pushRenderingLock;
    }

    @RestrictTo(Scope.LIBRARY)
    public void setPushNotificationRenderer(@NonNull INotificationRenderer iNotificationRenderer) {
        this.iNotificationRenderer = iNotificationRenderer;
    }

    private void triggerNotification(Context context, Bundle extras, int notificationId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            String notificationManagerError = "Unable to render notification, Notification Manager is null.";
            config.getLogger().debug(config.getAccountId(), notificationManagerError);
            return;
        }

        String channelId = extras.getString(Constants.WZRK_CHANNEL_ID, "");
        String updatedChannelId = null;
        final boolean requiresChannelId = VERSION.SDK_INT >= VERSION_CODES.O;

        if (requiresChannelId) {
            int messageCode = -1;
            String value = "";

            if (channelId.isEmpty()) {
                messageCode = Constants.CHANNEL_ID_MISSING_IN_PAYLOAD;
                value = extras.toString();
            } else if (notificationManager.getNotificationChannel(channelId) == null) {
                messageCode = Constants.CHANNEL_ID_NOT_REGISTERED;
                value = channelId;
            }
            if (messageCode != -1) {
                ValidationResult channelIdError = ValidationResultFactory.create(512, messageCode, value);
                config.getLogger().debug(config.getAccountId(), channelIdError.getErrorDesc());
                validationResultStack.pushValidationResult(channelIdError);
            }

            // get channel using channel id from push payload. If channel id is null or empty then create default
            updatedChannelId = CTXtensions.getOrCreateChannel(notificationManager, channelId, context);

            // if no channel gets created then do not render push
            if (updatedChannelId == null || updatedChannelId.trim().isEmpty()) {
                config.getLogger()
                        .debug(config.getAccountId(), "Not rendering Push since channel id is null or blank.");
                return;
            }

            // if channel is blocked by user then do not render push
            if (!CTXtensions.isNotificationChannelEnabled(context,updatedChannelId)) {
                config.getLogger()
                        .verbose(config.getAccountId(),
                                "Not rendering push notification as channel = " + updatedChannelId + " is blocked by user");
                return;
            }

            config.getLogger().debug(config.getAccountId(), "Rendering Push on channel = " + updatedChannelId);
        }

        int smallIcon;
        try {
            String x = ManifestInfo.getInstance(context).getNotificationIcon();
            if (x == null) {
                throw new IllegalArgumentException();
            }
            smallIcon = context.getResources().getIdentifier(x, "drawable", context.getPackageName());
            if (smallIcon == 0) {
                throw new IllegalArgumentException();
            }
        } catch (Throwable t) {
            smallIcon = DeviceInfo.getAppIconAsIntId(context);
        }

        iNotificationRenderer.setSmallIcon(smallIcon, context);

        int priorityInt = NotificationCompat.PRIORITY_DEFAULT;
        String priority = extras.getString(Constants.NOTIF_PRIORITY);
        if (priority != null) {
            if (priority.equals(Constants.PRIORITY_HIGH)) {
                priorityInt = NotificationCompat.PRIORITY_HIGH;
            }
            if (priority.equals(Constants.PRIORITY_MAX)) {
                priorityInt = NotificationCompat.PRIORITY_MAX;
            }
        }

        // if we have no user set notificationID then try collapse key
        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
            try {
                Object collapse_key = iNotificationRenderer
                        .getCollapseKey(extras);
                if (collapse_key != null) {
                    if (collapse_key instanceof Number) {
                        notificationId = ((Number) collapse_key).intValue();
                    } else if (collapse_key instanceof String) {
                        try {
                            notificationId = Integer.parseInt(collapse_key.toString());
                            config.getLogger().verbose(config.getAccountId(),
                                    "Converting collapse_key: " + collapse_key + " to notificationId int: "
                                            + notificationId);
                        } catch (NumberFormatException e) {
                            notificationId = (collapse_key.toString().hashCode());
                            config.getLogger().verbose(config.getAccountId(),
                                    "Converting collapse_key: " + collapse_key + " to notificationId int: "
                                            + notificationId);
                        }
                    }
                    notificationId = Math.abs(notificationId); //Notification Id always needs to be positive
                    config.getLogger().debug(config.getAccountId(),
                            "Creating the notification id: " + notificationId + " from collapse_key: "
                                    + collapse_key);
                }
            } catch (NumberFormatException e) {
                // no-op
            }
        } else {
            config.getLogger().debug(config.getAccountId(), "Have user provided notificationId: " + notificationId
                    + " won't use collapse_key (if any) as basis for notificationId");
        }

        // if after trying collapse_key notification is still empty set to random int
        if (notificationId == Constants.EMPTY_NOTIFICATION_ID) {
            notificationId = (int) (Math.random() * 100);
            config.getLogger().debug(config.getAccountId(), "Setting random notificationId: " + notificationId);
        }

        NotificationCompat.Builder nb;
        if (requiresChannelId) {
            nb = new NotificationCompat.Builder(context, updatedChannelId);

            // choices here are Notification.BADGE_ICON_NONE = 0, Notification.BADGE_ICON_SMALL = 1, Notification.BADGE_ICON_LARGE = 2.  Default is  Notification.BADGE_ICON_LARGE
            String badgeIconParam = extras
                    .getString(Constants.WZRK_BADGE_ICON, null);
            if (badgeIconParam != null) {
                try {
                    int badgeIconType = Integer.parseInt(badgeIconParam);
                    if (badgeIconType >= 0) {
                        nb.setBadgeIconType(badgeIconType);
                    }
                } catch (Throwable t) {
                    // no-op
                }
            }

            String badgeCountParam = extras.getString(Constants.WZRK_BADGE_COUNT, null);//cbi
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
            // noinspection all
            nb = new NotificationCompat.Builder(context);
        }

        nb.setPriority(priorityInt);

        //remove sound for fallback notif

        if (iNotificationRenderer instanceof AudibleNotification) {
            nb = ((AudibleNotification) iNotificationRenderer).setSound(context, extras, nb, config);
        }

        nb = iNotificationRenderer.renderNotification(extras, context, nb, config, notificationId);
        if (nb == null) {// template renderer can return null if template type is null
            return;
        }

        Notification n = nb.build();
        notificationManager.notify(notificationId, n);
        config.getLogger().debug(config.getAccountId(), "Rendered notification: " + n.toString());//cb

        String extrasFrom = extras.getString(Constants.EXTRAS_FROM);
        if (extrasFrom == null || !extrasFrom.equals("PTReceiver")) {
            String ttl = extras.getString(Constants.WZRK_TIME_TO_LIVE,
                    (System.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL) / 1000 + "");
            long wzrk_ttl = Long.parseLong(ttl);
            String wzrk_pid = extras.getString(Constants.WZRK_PUSH_ID);
            DBAdapter dbAdapter = baseDatabaseManager.loadDBAdapter(context);
            config.getLogger().verbose("Storing Push Notification..." + wzrk_pid + " - with ttl - " + ttl);
            dbAdapter.storePushNotificationId(wzrk_pid, wzrk_ttl);

            boolean notificationViewedEnabled = "true".equals(extras.getString(Constants.WZRK_RNV, ""));
            if (!notificationViewedEnabled) {
                ValidationResult notificationViewedError = ValidationResultFactory
                        .create(512, Constants.NOTIFICATION_VIEWED_DISABLED, extras.toString());
                config.getLogger().debug(notificationViewedError.getErrorDesc());
                validationResultStack.pushValidationResult(notificationViewedError);
                return;
            }

            long omrStart = extras.getLong(Constants.OMR_INVOKE_TIME_IN_MILLIS, -1);
            if (omrStart >= 0) {
                long prt = System.currentTimeMillis() - omrStart;
                config.getLogger()
                        .verbose("Rendered Push Notification in " + prt + " millis");
            }

            ctWorkManager.init();
            analyticsManager.pushNotificationViewedEvent(extras);

        }
    }
}