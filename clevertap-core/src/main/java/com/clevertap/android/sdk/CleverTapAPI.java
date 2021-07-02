package com.clevertap.android.sdk;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.job.JobParameters;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.events.EventDetail;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inbox.CTInboxActivity;
import com.clevertap.android.sdk.inbox.CTInboxMessage;
import com.clevertap.android.sdk.inbox.CTMessageDAO;
import com.clevertap.android.sdk.interfaces.OnInitDeviceIDListener;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.UriHelper;
import com.clevertap.android.sdk.validation.ManifestValidator;
import com.clevertap.android.sdk.validation.ValidationResult;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * <h1>CleverTapAPI</h1>
 * This is the main CleverTapAPI class that manages the SDK instances
 */
public class CleverTapAPI implements CTInboxActivity.InboxActivityListener {

    /**
     * Implement to get called back when the device push token is refreshed
     */
    public interface DevicePushTokenRefreshListener {

        /**
         * @param token the device token
         * @param type  the token type com.clevertap.android.sdk.PushType (FCM)
         */
        void devicePushTokenDidRefresh(String token, PushType type);
    }

    @SuppressWarnings({"unused"})
    public enum LogLevel {
        OFF(-1),
        INFO(0),
        DEBUG(2),
        VERBOSE(3);

        private final int value;

        LogLevel(final int newValue) {
            value = newValue;
        }

        public int intValue() {
            return value;
        }
    }


    @SuppressWarnings("unused")
    public static final String NOTIFICATION_TAG = "wzrk_pn";

    private static int debugLevel = CleverTapAPI.LogLevel.INFO.intValue();

    static CleverTapInstanceConfig defaultConfig;

    private static HashMap<String, CleverTapAPI> instances;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static String sdkVersion;  // For Google Play Store/Android Studio analytics

    private final Context context;

    private WeakReference<InboxMessageButtonListener> inboxMessageButtonListener;

    private CoreState coreState;

    /**
     * This method is used to change the credentials of CleverTap account Id and token programmatically
     *
     * @param accountID CleverTap Account Id
     * @param token     CleverTap Account Token
     */
    @SuppressWarnings("unused")
    public static void changeCredentials(String accountID, String token) {
        changeCredentials(accountID, token, null);
    }

    /**
     * This method is used to change the credentials of CleverTap account Id, token and region programmatically
     *
     * @param accountID CleverTap Account Id
     * @param token     CleverTap Account Token
     * @param region    Clever Tap Account Region
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void changeCredentials(String accountID, String token, String region) {
        if (defaultConfig != null) {
            Logger.i("CleverTap SDK already initialized with accountID:" + defaultConfig.getAccountId()
                    + " and token:" + defaultConfig.getAccountToken() + ". Cannot change credentials to "
                    + accountID + " and " + token);
            return;
        }

        ManifestInfo.changeCredentials(accountID, token, region);
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p>
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
    @SuppressWarnings({"WeakerAccess"})
    public static void createNotification(final Context context, final Bundle extras, final int notificationId) {
        String _accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        if (instances == null) {
            CleverTapAPI instance = createInstanceIfAvailable(context, _accountId);
            if (instance != null) {
                instance.coreState.getPushProviders()._createNotification(context, extras, notificationId);
            }
            return;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            boolean shouldProcess = false;
            if (instance != null) {
                shouldProcess = (_accountId == null && instance.coreState.getConfig().isDefaultInstance())
                        || instance
                        .getAccountId()
                        .equals(_accountId);
            }
            if (shouldProcess) {
                try {
                    instance.coreState.getPushProviders()._createNotification(context, extras, notificationId);
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
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param extras  The {@link Bundle} object received by the broadcast receiver
     */
    @SuppressWarnings({"WeakerAccess"})
    public static void createNotification(final Context context, final Bundle extras) {
        createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID);
    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context            A reference to an Android context
     * @param channelId          A String for setting the id of the notification channel
     * @param channelName        A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance         An Integer value setting the importance of the notifications sent in this channel
     * @param showBadge          An boolean value as to whether this channel shows a badge
     */
    @SuppressWarnings("unused")
    public static void createNotificationChannel(final Context context, final String channelId,
            final CharSequence channelName, final String channelDescription, final int importance,
            final boolean showBadge) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("createNotificationChannel", new Callable<Void>() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public Void call() {

                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }
                        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                channelName,
                                importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setShowBadge(showBadge);
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel " + channelName.toString() + " has been created");
                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger().verbose(instance.getAccountId(), "Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism and creating
     * notification channel groups. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context            A reference to an Android context
     * @param channelId          A String for setting the id of the notification channel
     * @param channelName        A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance         An Integer value setting the importance of the notifications sent in this
     *                           channel
     * @param groupId            A String for setting the notification channel as a part of a notification
     *                           group
     * @param showBadge          An boolean value as to whether this channel shows a badge
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannel(final Context context, final String channelId,
            final CharSequence channelName, final String channelDescription, final int importance,
            final String groupId, final boolean showBadge) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("creatingNotificationChannel", new Callable<Void>() {
                    @Override
                    public Void call() {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }
                        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                channelName,
                                importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setGroup(groupId);
                        notificationChannel.setShowBadge(showBadge);
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel " + channelName.toString() + " has been created");

                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger().verbose(instance.getAccountId(), "Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context            A reference to an Android context
     * @param channelId          A String for setting the id of the notification channel
     * @param channelName        A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance         An Integer value setting the importance of the notifications sent in this channel
     * @param showBadge          An boolean value as to whether this channel shows a badge
     * @param sound              A String denoting the custom sound raw file for this channel
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannel(final Context context, final String channelId,
            final CharSequence channelName, final String channelDescription, final int importance,
            final boolean showBadge, final String sound) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("createNotificationChannel", new Callable<Void>() {
                    @Override
                    public Void call() {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }

                        String soundfile = "";
                        Uri soundUri = null;

                        if (!sound.isEmpty()) {
                            if (sound.contains(".mp3") || sound.contains(".ogg") || sound.contains(".wav")) {
                                soundfile = sound.substring(0, (sound.length() - 4));
                            } else {
                                instance.getConfigLogger()
                                        .debug(instance.getAccountId(), "Sound file name not supported");
                            }
                            if (!soundfile.isEmpty()) {
                                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context
                                        .getPackageName() + "/raw/" + soundfile);
                            }

                        }

                        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                channelName,
                                importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setShowBadge(showBadge);
                        if (soundUri != null) {
                            notificationChannel.setSound(soundUri,
                                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .build());
                        } else {
                            instance.getConfigLogger().debug(instance.getAccountId(),
                                    "Sound file not found, notification channel will be created without custom sound");
                        }
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel " + channelName.toString() + " has been created");
                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger().verbose(instance.getAccountId(), "Failure creating Notification Channel", t);
        }
    }

    /**
     * Launches an asynchronous task to create the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism and creating
     * notification channel groups. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context            A reference to an Android context
     * @param channelId          A String for setting the id of the notification channel
     * @param channelName        A String for setting the name of the notification channel
     * @param channelDescription A String for setting the description of the notification channel
     * @param importance         An Integer value setting the importance of the notifications sent in this
     *                           channel
     * @param groupId            A String for setting the notification channel as a part of a notification
     *                           group
     * @param showBadge          An boolean value as to whether this channel shows a badge
     * @param sound              A String denoting the custom sound raw file for this channel
     */
    @SuppressWarnings({"unused"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannel(final Context context, final String channelId,
            final CharSequence channelName, final String channelDescription, final int importance,
            final String groupId, final boolean showBadge, final String sound) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificatonChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("creatingNotificationChannel", new Callable<Void>() {
                    @Override
                    public Void call() {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }

                        String soundfile = "";
                        Uri soundUri = null;

                        if (!sound.isEmpty()) {
                            if (sound.contains(".mp3") || sound.contains(".ogg") || sound.contains(".wav")) {
                                soundfile = sound.substring(0, (sound.length() - 4));
                            } else {
                                instance.getConfigLogger()
                                        .debug(instance.getAccountId(), "Sound file name not supported");
                            }
                            if (!soundfile.isEmpty()) {
                                soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context
                                        .getPackageName() + "/raw/" + soundfile);
                            }

                        }
                        NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                channelName,
                                importance);
                        notificationChannel.setDescription(channelDescription);
                        notificationChannel.setGroup(groupId);
                        notificationChannel.setShowBadge(showBadge);
                        if (soundUri != null) {
                            notificationChannel.setSound(soundUri,
                                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                            .build());
                        } else {
                            instance.getConfigLogger().debug(instance.getAccountId(),
                                    "Sound file not found, notification channel will be created without custom sound");
                        }
                        notificationManager.createNotificationChannel(notificationChannel);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel " + channelName.toString() + " has been created");
                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger().verbose(instance.getAccountId(), "Failure creating Notification Channel", t);
        }

    }

    /**
     * Launches an asynchronous task to create the notification channel group from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context   A reference to an Android context
     * @param groupId   A String for setting the id of the notification channel group
     * @param groupName A String for setting the name of the notification channel group
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createNotificationChannelGroup(final Context context, final String groupId,
            final CharSequence groupName) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("creatingNotificationChannelGroup", new Callable<Void>() {
                    @Override
                    public Void call() {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }
                        notificationManager
                                .createNotificationChannelGroup(
                                        new NotificationChannelGroup(groupId, groupName));
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel group " + groupName.toString() + " has been created");
                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger()
                    .verbose(instance.getAccountId(), "Failure creating Notification Channel Group", t);
        }
    }

    /**
     * Launches an asynchronous task to delete the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context   A reference to an Android context
     * @param channelId A String for setting the id of the notification channel
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void deleteNotificationChannel(final Context context, final String channelId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("deletingNotificationChannel", new Callable<Void>() {
                    @Override
                    public Void call() {
                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }
                        notificationManager.deleteNotificationChannel(channelId);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel " + channelId + " has been deleted");
                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger().verbose(instance.getAccountId(), "Failure deleting Notification Channel", t);
        }
    }

    /**
     * Launches an asynchronous task to delete the notification channel from CleverTap
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     * @param context A reference to an Android context
     * @param groupId A String for setting the id of the notification channel group
     */
    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void deleteNotificationChannelGroup(final Context context, final String groupId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
                task.execute("deletingNotificationChannelGroup", new Callable<Void>() {
                    @Override
                    public Void call() {

                        NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(NOTIFICATION_SERVICE);
                        if (notificationManager == null) {
                            return null;
                        }
                        notificationManager.deleteNotificationChannelGroup(groupId);
                        instance.getConfigLogger().info(instance.getAccountId(),
                                "Notification channel group " + groupId + " has been deleted");

                        return null;
                    }
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger()
                    .verbose(instance.getAccountId(), "Failure deleting Notification Channel Group", t);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void fcmTokenRefresh(Context context, String token) {

        for (CleverTapAPI instance : getAvailableInstances(context)) {
            if (instance == null || instance.getCoreState().getConfig().isAnalyticsOnly()) {
                Logger.d("Instance is Analytics Only not processing device token");
                continue;
            }
            //get token from Manifest
            String tokenUsingManifestMetaEntry = Utils
                    .getFcmTokenUsingManifestMetaEntry(context, instance.getCoreState().getConfig());
            if (!TextUtils.isEmpty(tokenUsingManifestMetaEntry)) {
                token = tokenUsingManifestMetaEntry;
            }
            instance.getCoreState().getPushProviders().doTokenRefresh(token, PushType.FCM);
        }
    }

    /**
     * Returns the log level set for CleverTapAPI
     *
     * @return The {@link CleverTapAPI.LogLevel} int value
     */
    @SuppressWarnings("WeakerAccess")
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
     * Debug messages are tagged as CleverTap.
     *
     * @param level Can be one of the following:  -1 (disables all debugging), 0 (default, shows minimal SDK
     *              integration related logging),
     *              1(shows debug output), 3(shows verbose output)
     */
    @SuppressWarnings("WeakerAccess")
    public static void setDebugLevel(int level) {
        debugLevel = level;
    }

    /**
     * Enables or disables debugging. If enabled, see debug messages in Android's logcat utility.
     * Debug messages are tagged as CleverTap.
     *
     * @param level Can be one of the following: LogLevel.OFF (disables all debugging), LogLevel.INFO (default, shows
     *              minimal SDK integration related logging),
     *              LogLevel.DEBUG(shows debug output),LogLevel.VERBOSE(shows verbose output)
     */
    @SuppressWarnings({"unused"})
    public static void setDebugLevel(LogLevel level) {
        debugLevel = level.intValue();
    }

    /**
     * Returns the default shared instance of the CleverTap SDK.
     *
     * @param context     The Android context
     * @param cleverTapID Custom CleverTapID passed by the app
     * @return The {@link CleverTapAPI} object
     */
    @SuppressWarnings("WeakerAccess")
    public static CleverTapAPI getDefaultInstance(Context context, String cleverTapID) {
        // For Google Play Store/Android Studio tracking
        sdkVersion = BuildConfig.SDK_VERSION_STRING;

        if (defaultConfig != null) {
            return instanceWithConfig(context, defaultConfig, cleverTapID);
        } else {
            defaultConfig = getDefaultConfig(context);
            if (defaultConfig != null) {
                return instanceWithConfig(context, defaultConfig, cleverTapID);
            }
        }
        return null;
    }

    /**
     * Returns the default shared instance of the CleverTap SDK.
     *
     * @param context The Android context
     * @return The {@link CleverTapAPI} object
     */
    @SuppressWarnings("WeakerAccess")
    public static @Nullable
    CleverTapAPI getDefaultInstance(Context context) {
        return getDefaultInstance(context, null);
    }

    public static @Nullable
    CleverTapAPI getGlobalInstance(Context context, String _accountId) {
        if (instances == null) {
            return createInstanceIfAvailable(context, _accountId);
        }

        CleverTapAPI instance = null;
        for (String accountId : instances.keySet()) {
            instance = CleverTapAPI.instances.get(accountId);
        }

        return instance;
    }

    public static HashMap<String, CleverTapAPI> getInstances() {
        return instances;
    }

    public static void setInstances(final HashMap<String, CleverTapAPI> instances) {
        CleverTapAPI.instances = instances;
    }

    /**
     * Checks whether this notification is from CleverTap.
     *
     * @param extras The payload from the FCM intent
     * @return See {@link NotificationInfo}
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static NotificationInfo getNotificationInfo(final Bundle extras) {
        if (extras == null) {
            return new NotificationInfo(false, false);
        }

        boolean fromCleverTap = extras.containsKey(Constants.NOTIFICATION_TAG);
        boolean shouldRender = fromCleverTap && extras.containsKey("nm");
        return new NotificationInfo(fromCleverTap, shouldRender);
    }

    // other static handlers
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void handleNotificationClicked(Context context, Bundle notification) {
        if (notification == null) {
            return;
        }

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

        for (String accountId : instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            boolean shouldProcess = false;
            if (instance != null) {
                shouldProcess = (_accountId == null && instance.coreState.getConfig().isDefaultInstance())
                        || instance
                        .getAccountId()
                        .equals(_accountId);
            }
            if (shouldProcess) {
                instance.pushNotificationClickedEvent(notification);
                break;
            }
        }
    }

    /**
     * Returns an instance of the CleverTap SDK using CleverTapInstanceConfig.
     *
     * @param context The Android context
     * @param config  The {@link CleverTapInstanceConfig} object
     * @return The {@link CleverTapAPI} object
     */
    public static CleverTapAPI instanceWithConfig(Context context, CleverTapInstanceConfig config) {
        return instanceWithConfig(context, config, null);
    }

    /**
     * Returns an instance of the CleverTap SDK using CleverTapInstanceConfig.
     *
     * @param context The Android context
     * @param config  The {@link CleverTapInstanceConfig} object
     * @return The {@link CleverTapAPI} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static CleverTapAPI instanceWithConfig(Context context, @NonNull CleverTapInstanceConfig config,
            String cleverTapID) {
        //noinspection Constant Conditions
        if (config == null) {
            Logger.v("CleverTapInstanceConfig cannot be null");
            return null;
        }
        if (instances == null) {
            instances = new HashMap<>();
        }

        CleverTapAPI instance = instances.get(config.getAccountId());
        if (instance == null) {
            instance = new CleverTapAPI(context, config, cleverTapID);
            instances.put(config.getAccountId(), instance);
            final CleverTapAPI finalInstance = instance;
            Task<Void> task = CTExecutorFactory.executors(instance.coreState.getConfig()).postAsyncSafelyTask();
            task.execute("notifyProfileInitialized", new Callable<Void>() {
                @Override
                public Void call() {
                    if (finalInstance.getCleverTapID() != null) {
                        finalInstance.coreState.getCallbackManager().notifyUserProfileInitialized();
                        finalInstance.coreState.getLoginController().recordDeviceIDErrors();
                    }
                    return null;
                }
            });
        } else if (instance.isErrorDeviceId() && instance.getConfig().getEnableCustomCleverTapId() && Utils
                .validateCTID(cleverTapID)) {
            instance.coreState.getLoginController().asyncProfileSwitchUser(null, null, cleverTapID);
        }
        return instance;
    }

    /**
     * Returns whether or not the app is in the foreground.
     *
     * @return The foreground status
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isAppForeground() {
        return CoreMetaData.isAppForeground();
    }

    /**
     * Use this method to notify CleverTap that the app is in foreground
     *
     * @param appForeground boolean true/false
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void setAppForeground(boolean appForeground) {
        CoreMetaData.setAppForeground(appForeground);
    }

    @SuppressWarnings("WeakerAccess")
    public static void onActivityPaused() {
        if (instances == null) {
            return;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            try {
                if (instance != null) {
                    instance.coreState.getActivityLifeCycleManager().activityPaused();
                }
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void onActivityResumed(Activity activity) {
        onActivityResumed(activity, null);
    }

    @SuppressWarnings("WeakerAccess")
    public static void onActivityResumed(Activity activity, String cleverTapID) {
        if (instances == null) {
            CleverTapAPI.createInstanceIfAvailable(activity.getApplicationContext(), null, cleverTapID);
        }

        CoreMetaData.setAppForeground(true);

        if (instances == null) {
            Logger.v("Instances is null in onActivityResumed!");
            return;
        }

        String currentActivityName = CoreMetaData.getCurrentActivityName();
        CoreMetaData.setCurrentActivity(activity);
        if (currentActivityName == null || !currentActivityName.equals(activity.getLocalClassName())) {
            CoreMetaData.incrementActivityCount();
        }

        if (CoreMetaData.getInitialAppEnteredForegroundTime() <= 0) {
            int initialAppEnteredForegroundTime = Utils.getNow();
            CoreMetaData.setInitialAppEnteredForegroundTime(initialAppEnteredForegroundTime);
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            try {
                if (instance != null) {
                    instance.coreState.getActivityLifeCycleManager().activityResumed(activity);
                }
            } catch (Throwable t) {
                Logger.v("Throwable - " + t.getLocalizedMessage());
            }
        }
    }

    /**
     * Pass Push Notification Payload to CleverTap for smooth functioning of Push Amplification
     *
     * @param context - Application Context
     * @param extras  - Bundle received via FCM/Push Amplification
     */
    @SuppressWarnings("unused")
    public static void processPushNotification(Context context, Bundle extras) {
        String _accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        if (instances == null) {
            CleverTapAPI instance = createInstanceIfAvailable(context, _accountId);
            if (instance != null) {
                instance.coreState.getPushProviders().processCustomPushNotification(extras);
            }
            return;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance != null) {
                instance.coreState.getPushProviders().processCustomPushNotification(extras);
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void runBackgroundIntentService(Context context) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                if (instance.getConfig().isBackgroundSync()) {
                    instance.coreState.getPushProviders().runInstanceJobWork(context, null);
                } else {
                    Logger.d("Instance doesn't allow Background sync, not running the Job");
                }
            }
            return;
        }
        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance == null) {
                continue;
            }
            if (instance.getConfig().isAnalyticsOnly()) {
                Logger.d(accountId, "Instance is Analytics Only not processing device token");
                continue;
            }
            if (!instance.getConfig().isBackgroundSync()) {
                Logger.d(accountId, "Instance doesn't allow Background sync, not running the Job");
                continue;
            }
            instance.coreState.getPushProviders().runInstanceJobWork(context, null);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void runJobWork(Context context, JobParameters parameters) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                if (instance.getConfig().isBackgroundSync()) {
                    instance.coreState.getPushProviders().runInstanceJobWork(context, parameters);
                } else {
                    Logger.d("Instance doesn't allow Background sync, not running the Job");
                }
            }
            return;
        }
        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance != null && instance.getConfig().isAnalyticsOnly()) {
                Logger.d(accountId, "Instance is Analytics Only not running the Job");
                continue;
            }
            if (!(instance != null && instance.getConfig().isBackgroundSync())) {
                Logger.d(accountId, "Instance doesn't allow Background sync, not running the Job");
                continue;
            }
            instance.coreState.getPushProviders().runInstanceJobWork(context, parameters);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void tokenRefresh(Context context, String token, PushType pushType) {
        for (CleverTapAPI instance : getAvailableInstances(context)) {
            instance.coreState.getPushProviders().doTokenRefresh(token, pushType);
        }
    }

    // Initialize
    private CleverTapAPI(final Context context, final CleverTapInstanceConfig config, String cleverTapID) {
        this.context = context;

       /* new Thread("SharedPreferences-load") {
            public void run() {
                StorageHelper.getPreferences(context.getApplicationContext());
            }
        }.start();*/

        CoreState coreState = CleverTapFactory
                .getCoreState(context, config, cleverTapID);
        setCoreState(coreState);

        Task<Void> task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("CleverTapAPI#initializeDeviceInfo", new Callable<Void>() {
            @Override
            public Void call() {
                if (config.isDefaultInstance()) {
                    manifestAsyncValidation();
                }
                return null;
            }
        });

        int now = Utils.getNow();
        if (now - CoreMetaData.getInitialAppEnteredForegroundTime() > 5) {
            this.coreState.getConfig().setCreatedPostAppLaunch();
        }

        task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("setStatesAsync", new Callable<Void>() {
            @Override
            public Void call() {
                CleverTapAPI.this.coreState.getSessionManager().setLastVisitTime();
                CleverTapAPI.this.coreState.getDeviceInfo().setDeviceNetworkInfoReportingFromStorage();
                CleverTapAPI.this.coreState.getDeviceInfo().setCurrentUserOptOutStateFromStorage();
                return null;
            }
        });

        task = CTExecutorFactory.executors(config).postAsyncSafelyTask();
        task.execute("saveConfigtoSharedPrefs", new Callable<Void>() {
            @Override
            public Void call() {
                String configJson = config.toJSONString();
                if (configJson == null) {
                    Logger.v("Unable to save config to SharedPrefs, config Json is null");
                    return null;
                }
                StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, "instance"), configJson);
                return null;
            }
        });

        Logger.i("CleverTap SDK initialized with accountId: " + config.getAccountId() + " accountToken: " + config
                .getAccountToken() + " accountRegion: " + config.getAccountRegion());
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void addMultiValueForKey(String key, String value) {
        if (value == null || value.isEmpty()) {
            coreState.getAnalyticsManager()._generateEmptyMultiValueError(key);
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void addMultiValuesForKey(final String key, final ArrayList<String> values) {
        coreState.getAnalyticsManager().addMultiValuesForKey(key, values);
    }

    /**
     * Deletes the given {@link CTInboxMessage} object
     *
     * @param message {@link CTInboxMessage} public object of inbox message
     */
    @SuppressWarnings({"unused"})
    public void deleteInboxMessage(final CTInboxMessage message) {
        if (coreState.getControllerManager().getCTInboxController() != null) {
            coreState.getControllerManager().getCTInboxController().deleteInboxMessage(message);
        } else {
            getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
        }
    }

    /**
     * Deletes the {@link CTInboxMessage} object for given messageId
     *
     * @param messageId String - messageId of {@link CTInboxMessage} public object of inbox message
     */
    @SuppressWarnings("unused")
    public void deleteInboxMessage(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        deleteInboxMessage(message);
    }

    /**
     * Disables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    @SuppressWarnings({"unused"})
    public void disablePersonalization() {
        this.coreState.getConfig().enablePersonalization(false);
    }

    /**
     * Use this method to enable device network-related information tracking, including IP address.
     * This reporting is disabled by default.  To re-disable tracking call this method with enabled set to false.
     *
     * @param value boolean Whether device network info reporting should be enabled/disabled.
     */
    @SuppressWarnings({"unused"})
    public void enableDeviceNetworkInfoReporting(boolean value) {
        coreState.getDeviceInfo().enableDeviceNetworkInfoReporting(value);
    }

    /**
     * Enables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    @SuppressWarnings({"unused"})
    public void enablePersonalization() {
        this.coreState.getConfig().enablePersonalization(true);
    }

    /**
     * @return object of {@link CTFeatureFlagsController}
     * Handler to get the feature flag values
     */
    public CTFeatureFlagsController featureFlag() {
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger()
                    .debug(getAccountId(), "Feature flag is not supported with analytics only configuration");
        }
        return coreState.getControllerManager().getCTFeatureFlagsController();
    }

    /**
     * Sends all the events in the event queue.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void flush() {
        coreState.getBaseEventQueueManager().flush();
    }

    public String getAccountId() {
        return coreState.getConfig().getAccountId();
    }

    /**
     * Getter for retrieving all the Display Units.
     *
     * @return ArrayList<CleverTapDisplayUnit> - could be null, if there is no Display Unit campaigns
     */
    @Nullable
    public ArrayList<CleverTapDisplayUnit> getAllDisplayUnits() {

        if (coreState.getControllerManager().getCTDisplayUnitController() != null) {
            return coreState.getControllerManager().getCTDisplayUnitController().getAllDisplayUnits();
        } else {
            getConfigLogger()
                    .verbose(getAccountId(), Constants.FEATURE_DISPLAY_UNIT + "Failed to get all Display Units");
            return null;
        }
    }

    /**
     * Returns an ArrayList of all {@link CTInboxMessage} objects
     *
     * @return ArrayList of {@link CTInboxMessage} of Inbox Messages
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public ArrayList<CTInboxMessage> getAllInboxMessages() {
        ArrayList<CTInboxMessage> inboxMessageArrayList = new ArrayList<>();
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() != null) {
                ArrayList<CTMessageDAO> messageDAOArrayList =
                        coreState.getControllerManager().getCTInboxController().getMessages();
                for (CTMessageDAO messageDAO : messageDAOArrayList) {
                    Logger.v("CTMessage Dao - " + messageDAO.toJSON().toString());
                    inboxMessageArrayList.add(new CTInboxMessage(messageDAO.toJSON()));
                }
                return inboxMessageArrayList;
            } else {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return inboxMessageArrayList; //return empty list to avoid null pointer exceptions
            }
        }
    }

    //Debug

    /**
     * Returns the CTInboxListener object
     *
     * @return An {@link CTInboxListener} object
     */
    @SuppressWarnings({"unused"})
    public CTInboxListener getCTNotificationInboxListener() {
        return coreState.getCallbackManager().getInboxListener();
    }

    /**
     * This method sets the CTInboxListener
     *
     * @param notificationInboxListener An {@link CTInboxListener} object
     */
    @SuppressWarnings({"unused"})
    public void setCTNotificationInboxListener(CTInboxListener notificationInboxListener) {
        coreState.getCallbackManager().setInboxListener(notificationInboxListener);
    }

    /**
     * Returns the CTPushAmpListener object
     *
     * @return The {@link CTPushAmpListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public CTPushAmpListener getCTPushAmpListener() {
        return coreState.getCallbackManager().getPushAmpListener();
    }

    /**
     * This method is used to set the CTPushAmpListener
     *
     * @param pushAmpListener - The{@link CTPushAmpListener} object
     */
    @SuppressWarnings("unused")
    public void setCTPushAmpListener(CTPushAmpListener pushAmpListener) {
        coreState.getCallbackManager().setPushAmpListener(pushAmpListener);
    }

    /**
     * Returns the CTPushNotificationListener object
     *
     * @return The {@link CTPushNotificationListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public CTPushNotificationListener getCTPushNotificationListener() {
        return coreState.getCallbackManager().getPushNotificationListener();
    }

    /**
     * This method is used to set the CTPushNotificationListener
     *
     * @param pushNotificationListener - The{@link CTPushNotificationListener} object
     */
    @SuppressWarnings("unused")
    public void setCTPushNotificationListener(CTPushNotificationListener pushNotificationListener) {
        coreState.getCallbackManager().setPushNotificationListener(pushNotificationListener);
    }

    //Network Info handling

    /**
     * Returns a unique CleverTap identifier suitable for use with install attribution providers.
     *
     * @return The attribution identifier currently being used to identify this user.
     *
     * <p><br><span style="color:red;background:#ffcc99" >&#9888; this method may take a long time to return,
     * so you should not call it from the application main thread</span></p>
     */
    @SuppressWarnings("unused")
    @WorkerThread
    public String getCleverTapAttributionIdentifier() {
        return coreState.getDeviceInfo().getAttributionID();
    }

    /**
     * Returns a unique identifier by which CleverTap identifies this user.
     *
     * @return The user identifier currently being used to identify this user.
     *
     * <p><br><span style="color:red;background:#ffcc99" >&#9888; this method may take a long time to return,
     * so you should not call it from the application main thread</span></p>
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    @WorkerThread
    public String getCleverTapID() {
        return coreState.getDeviceInfo().getDeviceID();
    }

    /**
     * Returns a unique identifier by which CleverTap identifies this user, on Main thread Callback.
     *
     * @param onInitDeviceIDListener non-null callback to retrieve identifier on main thread.
     */
    public void getCleverTapID(@NonNull OnInitDeviceIDListener onInitDeviceIDListener) {
        Task<Void> taskDeviceCachedInfo = CTExecutorFactory.executors(getConfig()).ioTask();
        taskDeviceCachedInfo.execute("getCleverTapID", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                onInitDeviceIDListener.onInitDeviceID(coreState.getDeviceInfo().getDeviceID());
                return null;
            }
        });
    }

    @RestrictTo(Scope.LIBRARY)
    public CoreState getCoreState() {
        return coreState;
    }

    void setCoreState(final CoreState cleverTapState) {
        coreState = cleverTapState;
    }

    /**
     * Returns the total count of the specified event
     *
     * @param event The event for which you want to get the total count
     * @return Total count in int
     */
    @SuppressWarnings({"unused"})
    public int getCount(String event) {
        EventDetail eventDetail = coreState.getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) {
            return eventDetail.getCount();
        }

        return -1;
    }

    /**
     * Returns an EventDetail object for the particular event passed. EventDetail consists of event name, count, first
     * time
     * and last time timestamp of the event.
     *
     * @param event The event name for which you want the Event details
     * @return The {@link EventDetail} object
     */
    @SuppressWarnings({"unused"})
    public EventDetail getDetails(String event) {
        return coreState.getLocalDataStore().getEventDetail(event);
    }

    /**
     * Returns the device push token or null
     *
     * @param type com.clevertap.android.sdk.PushType (FCM)
     * @return String device token or null
     * NOTE: on initial install calling getDevicePushToken may return null, as the device token is
     * not yet available
     * Implement CleverTapAPI.DevicePushTokenRefreshListener to get a callback once the token is
     * available
     */
    @SuppressWarnings("unused")
    public String getDevicePushToken(final PushType type) {
        return coreState.getPushProviders().getCachedToken(type);
    }

    //Util

    /**
     * Returns the DevicePushTokenRefreshListener
     *
     * @return The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public DevicePushTokenRefreshListener getDevicePushTokenRefreshListener() {
        return coreState.getPushProviders().getDevicePushTokenRefreshListener();
    }

    /**
     * This method is used to set the DevicePushTokenRefreshListener object
     *
     * @param tokenRefreshListener The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public void setDevicePushTokenRefreshListener(DevicePushTokenRefreshListener tokenRefreshListener) {
        coreState.getPushProviders().setDevicePushTokenRefreshListener(tokenRefreshListener);

    }

    /**
     * Getter for retrieving Display Unit using the unitID
     *
     * @param unitID - unitID of the Display Unit {@link CleverTapDisplayUnit#getUnitID()}
     * @return CleverTapDisplayUnit - could be null, if there is no Display Unit campaign with the identifier
     */
    @Nullable
    public CleverTapDisplayUnit getDisplayUnitForId(String unitID) {
        if (coreState.getControllerManager().getCTDisplayUnitController() != null) {
            return coreState.getControllerManager().getCTDisplayUnitController().getDisplayUnitForID(unitID);
        } else {
            getConfigLogger().verbose(getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to get Display Unit for id: " + unitID);
            return null;
        }
    }

    /**
     * Returns the timestamp of the first time the given event was raised
     *
     * @param event The event name for which you want the first time timestamp
     * @return The timestamp in int
     */
    @SuppressWarnings({"unused"})
    public int getFirstTime(String event) {
        EventDetail eventDetail = coreState.getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) {
            return eventDetail.getFirstTime();
        }

        return -1;
    }

    /**
     * Returns the GeofenceCallback object
     *
     * @return The {@link GeofenceCallback} object
     */
    @SuppressWarnings("unused")
    public GeofenceCallback getGeofenceCallback() {
        return coreState.getCallbackManager().getGeofenceCallback();
    }

    //DeepLink

    /**
     * This method is used to set the geofence callback
     * Register to handle geofence responses from CleverTap
     * This is to be used only by clevertap-geofence-sdk
     *
     * @param geofenceCallback The {@link GeofenceCallback} instance
     */

    @SuppressWarnings("unused")
    public void setGeofenceCallback(GeofenceCallback geofenceCallback) {
        coreState.getCallbackManager().setGeofenceCallback(geofenceCallback);
    }

    /**
     * Returns a Map of event names and corresponding event details of all the events raised
     *
     * @return A Map of Event Name and its corresponding EventDetail object
     */
    @SuppressWarnings({"unused"})
    public Map<String, EventDetail> getHistory() {
        return coreState.getLocalDataStore().getEventHistory(context);
    }

    /**
     * Returns the InAppNotificationListener object
     *
     * @return An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public InAppNotificationListener getInAppNotificationListener() {
        return coreState.getCallbackManager().getInAppNotificationListener();
    }

    /**
     * This method sets the InAppNotificationListener
     *
     * @param inAppNotificationListener An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused"})
    public void setInAppNotificationListener(InAppNotificationListener inAppNotificationListener) {
        coreState.getCallbackManager().setInAppNotificationListener(inAppNotificationListener);
    }

    /**
     * Returns the count of all inbox messages for the user
     *
     * @return int - count of all inbox messages
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getInboxMessageCount() {
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() != null) {
                return coreState.getControllerManager().getCTInboxController().count();
            } else {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return -1;
            }
        }
    }

    /**
     * Returns the {@link CTInboxMessage} object that belongs to the given message id
     *
     * @param messageId String - unique id of the inbox message
     * @return {@link CTInboxMessage} public object of inbox message
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public CTInboxMessage getInboxMessageForId(String messageId) {
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() != null) {
                CTMessageDAO message = coreState.getControllerManager().getCTInboxController()
                        .getMessageForId(messageId);
                return (message != null) ? new CTInboxMessage(message.toJSON()) : null;
            } else {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return null;
            }
        }
    }

    /**
     * Returns the count of total number of unread inbox messages for the user
     *
     * @return int - count of all unread messages
     */
    @SuppressWarnings({"unused"})
    public int getInboxMessageUnreadCount() {
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() != null) {
                return coreState.getControllerManager().getCTInboxController().unreadCount();
            } else {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return -1;
            }
        }
    }

    /**
     * Returns the timestamp of the last time the given event was raised
     *
     * @param event The event name for which you want the last time timestamp
     * @return The timestamp in int
     */
    @SuppressWarnings({"unused"})
    public int getLastTime(String event) {
        EventDetail eventDetail = coreState.getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) {
            return eventDetail.getLastTime();
        }

        return -1;
    }

    /**
     * get the current device location
     * requires Location Permission in AndroidManifest e.g. "android.permission.ACCESS_COARSE_LOCATION"
     * You can then use the returned Location value to update the user profile location in CleverTap via {@link
     * #setLocation(Location)}
     *
     * @return android.location.Location
     */
    @SuppressWarnings({"unused"})
    public Location getLocation() {
        return coreState.getLocationManager()._getLocation();
    }

    /**
     * set the user profile location in CleverTap
     * location can then be used for geo-segmentation etc.
     *
     * @param location android.location.Location
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setLocation(Location location) {
        coreState.getLocationManager()._setLocation(location);
    }

    /**
     * Returns the timestamp of the previous visit
     *
     * @return Timestamp of previous visit in int
     */
    @SuppressWarnings({"unused"})
    public int getPreviousVisitTime() {
        return coreState.getSessionManager().getLastVisitTime();
    }

    /**
     * Return the user profile property value for the specified key
     *
     * @param name String
     * @return {@link JSONArray}, String or null
     */
    @SuppressWarnings({"unused"})
    public Object getProperty(String name) {
        if (!coreState.getConfig().isPersonalizationEnabled()) {
            return null;
        }
        return coreState.getLocalDataStore().getProfileProperty(name);
    }

    /**
     * Returns the token for a particular push type
     */
    public String getPushToken(@NonNull PushConstants.PushType pushType) {
        return coreState.getPushProviders().getCachedToken(pushType);
    }

    /**
     * Returns the number of screens which have been displayed by the app
     *
     * @return Total number of screens which have been displayed by the app
     */
    @SuppressWarnings({"unused"})
    public int getScreenCount() {
        return CoreMetaData.getActivityCount();
    }

    /**
     * Returns the SyncListener object
     *
     * @return The {@link SyncListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public SyncListener getSyncListener() {
        return coreState.getCallbackManager().getSyncListener();
    }

    /**
     * This method is used to set the SyncListener
     *
     * @param syncListener The {@link SyncListener} object
     */
    @SuppressWarnings("unused")
    public void setSyncListener(SyncListener syncListener) {
        coreState.getCallbackManager().setSyncListener(syncListener);
    }

    /**
     * Returns the time elapsed by the user on the app
     *
     * @return Time elapsed by user on the app in int
     */
    @SuppressWarnings({"unused"})
    public int getTimeElapsed() {
        int currentSession = coreState.getCoreMetaData().getCurrentSessionId();
        if (currentSession == 0) {
            return -1;
        }

        int now = Utils.getNow();
        return now - currentSession;
    }

    /**
     * Returns the total number of times the app has been launched
     *
     * @return Total number of app launches in int
     */
    @SuppressWarnings({"unused"})
    public int getTotalVisits() {
        EventDetail ed = coreState.getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed != null) {
            return ed.getCount();
        }

        return 0;
    }

    /**
     * Returns a UTMDetail object which consists of UTM parameters like source, medium & campaign
     *
     * @return The {@link UTMDetail} object
     */
    @SuppressWarnings({"unused"})
    public UTMDetail getUTMDetails() {
        UTMDetail ud = new UTMDetail();
        ud.setSource(coreState.getCoreMetaData().getSource());
        ud.setMedium(coreState.getCoreMetaData().getMedium());
        ud.setCampaign(coreState.getCoreMetaData().getCampaign());
        return ud;
    }

    /**
     * Returns an ArrayList of unread {@link CTInboxMessage} objects
     *
     * @return ArrayList of {@link CTInboxMessage} of unread Inbox Messages
     */
    @SuppressWarnings({"unused"})
    public ArrayList<CTInboxMessage> getUnreadInboxMessages() {
        ArrayList<CTInboxMessage> inboxMessageArrayList = new ArrayList<>();
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() != null) {
                ArrayList<CTMessageDAO> messageDAOArrayList =
                        coreState.getControllerManager().getCTInboxController().getUnreadMessages();
                for (CTMessageDAO messageDAO : messageDAOArrayList) {
                    inboxMessageArrayList.add(new CTInboxMessage(messageDAO.toJSON()));
                }
                return inboxMessageArrayList;
            } else {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return inboxMessageArrayList; //return empty list to avoid null pointer exceptions
            }
        }
    }

    /**
     * Initializes the inbox controller and sends a callback to the {@link CTInboxListener}
     * This method needs to be called separately for each instance of {@link CleverTapAPI}
     */
    @SuppressWarnings({"unused"})
    public void initializeInbox() {
        coreState.getControllerManager().initializeInbox();
    }

    /**
     * Marks the given {@link CTInboxMessage} object as read
     *
     * @param message {@link CTInboxMessage} public object of inbox message
     */
    //marks the message as read
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void markReadInboxMessage(final CTInboxMessage message) {
        if (coreState.getControllerManager().getCTInboxController() != null) {
            coreState.getControllerManager().getCTInboxController().markReadInboxMessage(message);
        } else {
            getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
        }
    }

    //Session

    /**
     * Marks the given messageId of {@link CTInboxMessage} object as read
     *
     * @param messageId String - messageId of {@link CTInboxMessage} public object of inbox message
     */
    @SuppressWarnings("unused")
    public void markReadInboxMessage(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        markReadInboxMessage(message);
    }

    @Override
    public void messageDidClick(CTInboxActivity ctInboxActivity, CTInboxMessage inboxMessage, Bundle data,
            HashMap<String, String> keyValue) {
        coreState.getAnalyticsManager().pushInboxMessageStateEvent(true, inboxMessage, data);
        if (keyValue != null && !keyValue.isEmpty()) {
            if (inboxMessageButtonListener != null && inboxMessageButtonListener.get() != null) {
                inboxMessageButtonListener.get().onInboxButtonClick(keyValue);
            }
        }
    }

    @Override
    public void messageDidShow(CTInboxActivity ctInboxActivity, final CTInboxMessage inboxMessage,
            final Bundle data) {
        Task<Void> task = CTExecutorFactory.executors(coreState.getConfig()).postAsyncSafelyTask();
        task.execute("handleMessageDidShow", new Callable<Void>() {
            @Override
            public Void call() {
                CTInboxMessage message = getInboxMessageForId(inboxMessage.getMessageId());
                if (!message.isRead()) {
                    markReadInboxMessage(inboxMessage);
                    coreState.getAnalyticsManager().pushInboxMessageStateEvent(false, inboxMessage, data);
                }
                return null;
            }
        });
    }

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
     * @param profile     The map keyed by the type of identity, with the value as the identity
     * @param cleverTapID Custom CleverTap ID passed by the App
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        coreState.getLoginController().onUserLogin(profile, cleverTapID);
    }

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
        onUserLogin(profile, null);
    }

    /**
     * The handle for product config functionalities(fetch/activate etc.)
     *
     * @return - the instance of {@link CTProductConfigController}
     */
    @SuppressWarnings("WeakerAccess")
    public CTProductConfigController productConfig() {
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger().debug(getAccountId(),
                    "Product config is not supported with analytics only configuration");
        }
        return coreState.getCtProductConfigController();
    }

    /**
     * Sends the Baidu registration ID to CleverTap.
     *
     * @param regId    The Baidu registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    @SuppressWarnings("unused")
    public void pushBaiduRegistrationId(String regId, boolean register) {
        coreState.getPushProviders().handleToken(regId, PushType.BPS, register);
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
    @SuppressWarnings({"unused"})
    public void pushChargedEvent(HashMap<String, Object> chargeDetails,
            ArrayList<HashMap<String, Object>> items) {
        coreState.getAnalyticsManager().pushChargedEvent(chargeDetails, items);
    }

    /**
     * Use this method to pass the deeplink with UTM parameters to track installs
     *
     * @param uri URI of the deeplink
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushDeepLink(Uri uri) {
        coreState.getAnalyticsManager().pushDeepLink(uri, false);
    }

    /**
     * Raises the Display Unit Clicked event
     *
     * @param unitID - unitID of the Display Unit{@link CleverTapDisplayUnit#getUnitID()}
     */
    @SuppressWarnings("unused")
    public void pushDisplayUnitClickedEventForID(String unitID) {
        coreState.getAnalyticsManager().pushDisplayUnitClickedEventForID(unitID);
    }

    /**
     * Raises the Display Unit Viewed event
     *
     * @param unitID - unitID of the Display Unit{@link CleverTapDisplayUnit#getUnitID()}
     */
    @SuppressWarnings("unused")
    public void pushDisplayUnitViewedEventForID(String unitID) {
        coreState.getAnalyticsManager().pushDisplayUnitViewedEventForID(unitID);
    }

    /**
     * Internally records an "Error Occurred" event, which can be viewed in the dashboard.
     *
     * @param errorMessage The error message
     * @param errorCode    The error code
     */
    @SuppressWarnings({"unused"})
    public void pushError(final String errorMessage, final int errorCode) {
        coreState.getAnalyticsManager().pushError(errorMessage, errorCode);
    }

    /**
     * Pushes a basic event.
     *
     * @param eventName The name of the event
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushEvent(String eventName) {
        if (eventName == null || eventName.trim().equals("")) {
            return;
        }
        pushEvent(eventName, null);
    }

    /**
     * Push an event with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventActions A {@link HashMap}, with keys as strings, and values as {@link String},
     *                     {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                     {@link java.util.Date}, or {@link Character}
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushEvent(String eventName, Map<String, Object> eventActions) {
        coreState.getAnalyticsManager().pushEvent(eventName, eventActions);
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
    @SuppressWarnings("unused")
    public void pushFcmRegistrationId(String fcmId, boolean register) {
        coreState.getPushProviders().handleToken(fcmId, PushType.FCM, register);
    }

    /**
     * Used to record errors of the Geofence module
     *
     * @param errorCode    - int - predefined error code for geofences
     * @param errorMessage - String - error message
     */
    @SuppressWarnings("unused")
    public void pushGeoFenceError(int errorCode, String errorMessage) {
        ValidationResult validationResult = new ValidationResult(errorCode, errorMessage);
        coreState.getValidationResultStack().pushValidationResult(validationResult);
    }

    /**
     * Pushes the Geofence Cluster Exited event to CleverTap.
     *
     * @param geoFenceProperties The {@link JSONObject} object that contains the
     *                           event properties regarding GeoFence Cluster Exited event
     */
    @SuppressWarnings("unused")
    public Future<?> pushGeoFenceExitedEvent(JSONObject geoFenceProperties) {
        return coreState.getAnalyticsManager()
                .raiseEventForGeofences(Constants.GEOFENCE_EXITED_EVENT_NAME, geoFenceProperties);
    }

    /**
     * Pushes the Geofence Cluster Entered event to CleverTap.
     *
     * @param geofenceProperties The {@link JSONObject} object that contains the
     *                           event properties regarding GeoFence Cluster Entered event
     */
    @SuppressWarnings("unused")
    public Future<?> pushGeofenceEnteredEvent(JSONObject geofenceProperties) {
        return coreState.getAnalyticsManager()
                .raiseEventForGeofences(Constants.GEOFENCE_ENTERED_EVENT_NAME, geofenceProperties);
    }

    /**
     * Sends the Huawei registration ID to CleverTap.
     *
     * @param regId    The Huawei registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    @SuppressWarnings("unused")
    public void pushHuaweiRegistrationId(String regId, boolean register) {
        coreState.getPushProviders().handleToken(regId, PushType.HPS, register);
    }

    /**
     * Pushes the Notification Clicked event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of {@link CTInboxMessage}
     */
    @SuppressWarnings("unused")
    public void pushInboxNotificationClickedEvent(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        coreState.getAnalyticsManager().pushInboxMessageStateEvent(true, message, null);
    }

    /**
     * Pushes the Notification Viewed event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of {@link CTInboxMessage}
     */
    @SuppressWarnings("unused")
    public void pushInboxNotificationViewedEvent(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        coreState.getAnalyticsManager().pushInboxMessageStateEvent(false, message, null);
    }

    /**
     * This method is used to push install referrer via url String
     *
     * @param url A String with the install referrer parameters
     */
    @SuppressWarnings({"unused"})
    public void pushInstallReferrer(String url) {
        coreState.getAnalyticsManager().pushInstallReferrer(url);
    }

    /**
     * This method is used to push install referrer via UTM source, medium & campaign parameters
     *
     * @param source   The UTM source parameter
     * @param medium   The UTM medium parameter
     * @param campaign The UTM campaign parameter
     */
    @SuppressWarnings({"unused"})
    public synchronized void pushInstallReferrer(String source, String medium, String campaign) {
        coreState.getAnalyticsManager().pushInstallReferrer(source, medium, campaign);
    }

    /**
     * Pushes the Notification Clicked event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushNotificationClickedEvent(final Bundle extras) {
        coreState.getAnalyticsManager().pushNotificationClickedEvent(extras);
    }

    //Session

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushNotificationViewedEvent(Bundle extras) {
        coreState.getAnalyticsManager().pushNotificationViewedEvent(extras);
    }

    /**
     * Push a profile update.
     *
     * @param profile A {@link Map}, with keys as strings, and values as {@link String},
     *                {@link Integer}, {@link Long}, {@link Boolean}, {@link Float}, {@link Double},
     *                {@link java.util.Date}, or {@link Character}
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushProfile(final Map<String, Object> profile) {
        coreState.getAnalyticsManager().pushProfile(profile);
    }

    /**
     * Sends the Xiaomi registration ID to CleverTap.
     *
     * @param regId    The Xiaomi registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    @SuppressWarnings("unused")
    public void pushXiaomiRegistrationId(String regId, boolean register) {
        coreState.getPushProviders().handleToken(regId, PushType.XPS, register);
    }

    /**
     * Record a Screen View event
     *
     * @param screenName String, the name of the screen
     */
    @SuppressWarnings({"unused"})
    public void recordScreen(String screenName) {
        String currentScreenName = coreState.getCoreMetaData().getScreenName();
        if (screenName == null || (currentScreenName != null && !currentScreenName.isEmpty() && currentScreenName
                .equals(screenName))) {
            return;
        }
        getConfigLogger().debug(getAccountId(), "Screen changed to " + screenName);
        coreState.getCoreMetaData().setCurrentScreenName(screenName);
        coreState.getAnalyticsManager().recordPageEventWithExtras(null);
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeMultiValueForKey(String key, String value) {
        if (value == null || value.isEmpty()) {
            coreState.getAnalyticsManager()._generateEmptyMultiValueError(key);
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeMultiValuesForKey(final String key, final ArrayList<String> values) {
        coreState.getAnalyticsManager().removeMultiValuesForKey(key, values);
    }

    /**
     * Remove the user profile property value specified by key from the user profile
     *
     * @param key String
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeValueForKey(final String key) {
        coreState.getAnalyticsManager().removeValueForKey(key);
    }

    /**
     * This method is used to set the CTFeatureFlagsListener
     * Register to receive feature flag callbacks
     *
     * @param featureFlagsListener The {@link CTFeatureFlagsListener} object
     */
    @SuppressWarnings("unused")
    public void setCTFeatureFlagsListener(CTFeatureFlagsListener featureFlagsListener) {
        coreState.getCallbackManager().setFeatureFlagListener(featureFlagsListener);
    }

    /**
     * This method is used to set the product config listener
     * Register to receive callbacks
     *
     * @param listener The {@link CTProductConfigListener} instance
     */
    @SuppressWarnings("unused")
    public void setCTProductConfigListener(CTProductConfigListener listener) {
        coreState.getCallbackManager().setProductConfigListener(listener);
    }

    //Listener

    /**
     * Sets the listener to get the list of currently running Display Campaigns via callback
     *
     * @param listener- {@link DisplayUnitListener}
     */
    public void setDisplayUnitListener(DisplayUnitListener listener) {
        coreState.getCallbackManager().setDisplayUnitListener(listener);
    }

    @SuppressWarnings("unused")
    public void setInAppNotificationButtonListener(InAppNotificationButtonListener listener) {
        coreState.getCallbackManager().setInAppNotificationButtonListener(listener);
    }

    @SuppressWarnings("unused")
    public void setInboxMessageButtonListener(InboxMessageButtonListener listener) {
        this.inboxMessageButtonListener = new WeakReference<>(listener);
    }

    /**
     * Not to be used by developers. This is used internally to help CleverTap know which library is wrapping the
     * native SDK
     *
     * @param library {@link String} library name
     */
    public void setLibrary(String library) {
        if (coreState.getDeviceInfo() != null) {
            coreState.getDeviceInfo().setLibrary(library);
        }
    }

    /**
     * Sets the location in CleverTap to get updated GeoFences
     *
     * @param location android.location.Location
     */
    @SuppressWarnings("unused")
    public Future<?> setLocationForGeofences(Location location, int sdkVersion) {
        coreState.getCoreMetaData().setLocationForGeofence(true);
        coreState.getCoreMetaData().setGeofenceSDKVersion(sdkVersion);
        return coreState.getLocationManager()._setLocation(location);
    }

    /**
     * Set a collection of unique values as a multi-value user profile property, any existing value will be
     * overwritten.
     * Max 100 values, on reaching 100 cap, oldest value(s) will be removed.
     * Values must be Strings and are limited to 512 characters.
     *
     * @param key    String
     * @param values {@link ArrayList} with String values
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setMultiValuesForKey(final String key, final ArrayList<String> values) {
        coreState.getAnalyticsManager().setMultiValuesForKey(key, values);
    }

    /**
     * If you want to stop recorded events from being sent to the server, use this method to set the SDK instance to
     * offline.
     * Once offline, events will be recorded and queued locally but will not be sent to the server until offline is
     * disabled.
     * Calling this method again with offline set to false will allow events to be sent to server and the SDK instance
     * will immediately attempt to send events that have been queued while offline.
     *
     * @param value boolean, true sets the sdk offline, false sets the sdk back online
     */
    @SuppressWarnings({"unused"})
    public void setOffline(boolean value) {
        coreState.getCoreMetaData().setOffline(value);
        if (value) {
            getConfigLogger()
                    .debug(getAccountId(), "CleverTap Instance has been set to offline, won't send events queue");
        } else {
            getConfigLogger()
                    .debug(getAccountId(), "CleverTap Instance has been set to online, sending events queue");
            flush();
        }
    }

    /**
     * Use this method to opt the current user out of all event/profile tracking.
     * You must call this method separately for each active user profile (e.g. when switching user profiles using
     * onUserLogin).
     * Once enabled, no events will be saved remotely or locally for the current user. To re-enable tracking call this
     * method with enabled set to false.
     *
     * @param userOptOut boolean Whether tracking opt out should be enabled/disabled.
     */
    @SuppressWarnings({"unused"})
    public void setOptOut(boolean userOptOut) {
        final boolean enable = userOptOut;
        Task<Void> task = CTExecutorFactory.executors(coreState.getConfig()).postAsyncSafelyTask();
        task.execute("setOptOut", new Callable<Void>() {
            @Override
            public Void call() {
                // generate the data for a profile push to alert the server to the optOut state change
                HashMap<String, Object> optOutMap = new HashMap<>();
                optOutMap.put(Constants.CLEVERTAP_OPTOUT, enable);

                // determine order of operations depending on enabled/disabled
                if (enable) {  // if opting out first push profile event then set the flag
                    pushProfile(optOutMap);
                    coreState.getCoreMetaData().setCurrentUserOptedOut(true);
                } else {  // if opting back in first reset the flag to false then push the profile event
                    coreState.getCoreMetaData().setCurrentUserOptedOut(false);
                    pushProfile(optOutMap);
                }
                // persist the new optOut state
                String key = coreState.getDeviceInfo().optOutKey();
                if (key == null) {
                    getConfigLogger()
                            .verbose(getAccountId(), "Unable to persist user OptOut state, storage key is null");
                    return null;
                }
                StorageHelper.putBoolean(context, StorageHelper.storageKeyWithSuffix(getConfig(), key), enable);
                getConfigLogger().verbose(getAccountId(), "Set current user OptOut state to: " + enable);
                return null;
            }
        });
    }

    /**
     * Opens {@link CTInboxActivity} to display Inbox Messages
     *
     * @param styleConfig {@link CTInboxStyleConfig} configuration of various style parameters for the {@link
     *                    CTInboxActivity}
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void showAppInbox(CTInboxStyleConfig styleConfig) {
        synchronized (coreState.getCTLockManager().getInboxControllerLock()) {
            if (coreState.getControllerManager().getCTInboxController() == null) {
                getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                return;
            }
        }

        // make styleConfig immutable
        final CTInboxStyleConfig _styleConfig = new CTInboxStyleConfig(styleConfig);

        Intent intent = new Intent(context, CTInboxActivity.class);
        intent.putExtra("styleConfig", _styleConfig);
        Bundle configBundle = new Bundle();
        configBundle.putParcelable("config", getConfig());
        intent.putExtra("configBundle", configBundle);
        try {
            Activity currentActivity = CoreMetaData.getCurrentActivity();
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

    /**
     * Opens {@link CTInboxActivity} to display Inbox Messages with default {@link CTInboxStyleConfig} object
     */
    @SuppressWarnings({"unused"})
    public void showAppInbox() {
        CTInboxStyleConfig styleConfig = new CTInboxStyleConfig();
        showAppInbox(styleConfig);
    }

    //To be called from DeviceInfo AdID GUID generation
    void deviceIDCreated(String deviceId) {

        String accountId = coreState.getConfig().getAccountId();

        if (coreState.getControllerManager() == null) {
            Logger.v(accountId, "ControllerManager not set yet! Returning from deviceIDCreated()");
            return;
        }

        if (coreState.getControllerManager().getInAppFCManager() == null) {
            Logger.v(accountId, "Initializing InAppFC after Device ID Created = " + deviceId);
            coreState.getControllerManager()
                    .setInAppFCManager(new InAppFCManager(context, coreState.getConfig(), deviceId));
        }

        /*
           Reinitialising product config & Feature Flag controllers with google ad id.
        */
        CTFeatureFlagsController ctFeatureFlagsController = coreState.getControllerManager()
                .getCTFeatureFlagsController();

        if (ctFeatureFlagsController != null) {
            if (!ctFeatureFlagsController.isInitialized()) {
                Logger.v(accountId,
                        "Initializing Feature Flags after Device ID Created = " + deviceId);
            }
            ctFeatureFlagsController.setGuidAndInit(deviceId);
        }
        CTProductConfigController ctProductConfigController = coreState.getControllerManager()
                .getCTProductConfigController();

        if (ctProductConfigController != null) {
            if (!ctProductConfigController.isInitialized()) {
                Logger.v(accountId,
                        "Initializing Product Config after Device ID Created = " + deviceId);
            }
            ctProductConfigController.setGuidAndInit(deviceId);
        }
        Logger.v(accountId, "Got device id from DeviceInfo, notifying user profile initialized to SyncListener");
        coreState.getCallbackManager().notifyUserProfileInitialized(deviceId);
    }

    private CleverTapInstanceConfig getConfig() {
        return coreState.getConfig();
    }

    private Logger getConfigLogger() {
        return getConfig().getLogger();
    }

    private boolean isErrorDeviceId() {
        return coreState.getDeviceInfo().isErrorDeviceId();
    }

    //Run manifest validation in async
    private void manifestAsyncValidation() {
        Task<Void> task = CTExecutorFactory.executors(coreState.getConfig()).postAsyncSafelyTask();
        task.execute("Manifest Validation", new Callable<Void>() {
            @Override
            public Void call() {
                ManifestValidator
                        .validate(context, coreState.getDeviceInfo(), coreState.getPushProviders());
                return null;
            }
        });
    }

    /**
     * Sends the ADM registration ID to CleverTap.
     *
     * @param token    The ADM registration ID
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    @SuppressWarnings("unused")
    private void pushAmazonRegistrationId(String token, boolean register) {
        coreState.getPushProviders().handleToken(token, PushType.ADM, register);
    }

    static void onActivityCreated(Activity activity) {
        onActivityCreated(activity, null);
    }

    // static lifecycle callbacks
    static void onActivityCreated(Activity activity, String cleverTapID) {
        // make sure we have at least the default instance created here.
        if (instances == null) {
            CleverTapAPI.createInstanceIfAvailable(activity.getApplicationContext(), null, cleverTapID);
        }

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
                    alreadyProcessedByCleverTap = (notification.containsKey(Constants.WZRK_FROM_KEY)
                            && Constants.WZRK_FROM.equals(notification.get(Constants.WZRK_FROM_KEY)));
                    if (alreadyProcessedByCleverTap) {
                        Logger.v("ActivityLifecycleCallback: Notification Clicked already processed for "
                                + notification.toString() + ", dropping duplicate.");
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

        if (alreadyProcessedByCleverTap && deepLink == null) {
            return;
        }

        CleverTapAPI instance = CleverTapAPI.instances.get(_accountId);
        if (instance != null) {
            instance.coreState.getActivityLifeCycleManager().onActivityCreated(notification, deepLink);
        }
    }

    private static CleverTapAPI createInstanceIfAvailable(Context context, String _accountId) {
        return createInstanceIfAvailable(context, _accountId, null);
    }

    private static @Nullable
    CleverTapAPI createInstanceIfAvailable(Context context, String _accountId, String cleverTapID) {
        try {
            if (_accountId == null) {
                try {
                    return CleverTapAPI.getDefaultInstance(context, cleverTapID);
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
            String configJson = StorageHelper.getString(context, "instance:" + _accountId, "");
            if (!configJson.isEmpty()) {
                CleverTapInstanceConfig config = CleverTapInstanceConfig.createInstance(configJson);
                Logger.v("Inflated Instance Config: " + configJson);
                return config != null ? CleverTapAPI.instanceWithConfig(context, config, cleverTapID) : null;
            } else {
                try {
                    CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
                    return (instance != null && instance.coreState.getConfig().getAccountId().equals(_accountId))
                            ? instance : null;
                } catch (Throwable t) {
                    Logger.v("Error creating shared Instance: ", t.getCause());
                    return null;
                }
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static ArrayList<CleverTapAPI> getAvailableInstances(Context context) {
        ArrayList<CleverTapAPI> apiArrayList = new ArrayList<>();
        if (instances == null || instances.isEmpty()) {
            CleverTapAPI cleverTapAPI = CleverTapAPI.getDefaultInstance(context);
            if (cleverTapAPI != null) {
                apiArrayList.add(cleverTapAPI);
            }
        } else {
            apiArrayList.addAll(CleverTapAPI.instances.values());
        }
        return apiArrayList;
    }

    /**
     * Creates default {@link CleverTapInstanceConfig} object and returns it
     *
     * @param context The Android context
     * @return The {@link CleverTapInstanceConfig} object
     */
    private static CleverTapInstanceConfig getDefaultConfig(Context context) {
        ManifestInfo manifest = ManifestInfo.getInstance(context);
        String accountId = manifest.getAccountId();
        String accountToken = manifest.getAcountToken();
        String accountRegion = manifest.getAccountRegion();
        if (accountId == null || accountToken == null) {
            Logger.i(
                    "Account ID or Account token is missing from AndroidManifest.xml, unable to create default instance");
            return null;
        }
        if (accountRegion == null) {
            Logger.i("Account Region not specified in the AndroidManifest - using default region");
        }

        return CleverTapInstanceConfig.createDefaultInstance(context, accountId, accountToken, accountRegion);

    }

    private static @Nullable
    CleverTapAPI getDefaultInstanceOrFirstOther(Context context) {
        CleverTapAPI instance = getDefaultInstance(context);
        if (instance == null && instances != null && !instances.isEmpty()) {
            for (String accountId : CleverTapAPI.instances.keySet()) {
                instance = CleverTapAPI.instances.get(accountId);
                if (instance != null) {
                    break;
                }
            }
        }
        return instance;
    }
}