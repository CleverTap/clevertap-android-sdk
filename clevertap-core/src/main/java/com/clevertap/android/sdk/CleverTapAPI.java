package com.clevertap.android.sdk;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.clevertap.android.sdk.CTJsonConverter.getErrorObject;
import static com.clevertap.android.sdk.CTJsonConverter.getWzrkFields;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.login.IdentityRepo;
import com.clevertap.android.sdk.login.IdentityRepoFactory;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType;
import com.clevertap.android.sdk.pushnotification.amp.CTBackgroundIntentService;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * <h1>CleverTapAPI</h1>
 * This is the main CleverTapAPI class that manages the SDK instances
 */
public class CleverTapAPI implements CleverTapAPIListener {

    //InApp
    private final class NotificationPrepareRunnable implements Runnable {

        private final WeakReference<CleverTapAPI> cleverTapAPIWeakReference;

        private final JSONObject jsonObject;

        private final boolean videoSupport = Utils.haveVideoPlayerSupport;

        NotificationPrepareRunnable(CleverTapAPI cleverTapAPI, JSONObject jsonObject) {
            this.cleverTapAPIWeakReference = new WeakReference<>(cleverTapAPI);
            this.jsonObject = jsonObject;
        }

        @Override
        public void run() {
            final CTInAppNotification inAppNotification = new CTInAppNotification()
                    .initWithJSON(jsonObject, videoSupport);
            if (inAppNotification.getError() != null) {
                getConfigLogger()
                        .debug(getAccountId(), "Unable to parse inapp notification " + inAppNotification.getError());
                return;
            }
            inAppNotification.listener = cleverTapAPIWeakReference.get();
            inAppNotification.prepareForDisplay();
        }
    }

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
        DEBUG(2);

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

    private static CleverTapInstanceConfig defaultConfig;

    private static HashMap<String, CleverTapAPI> instances;

    private static boolean appForeground = false;

    private static final List<CTInAppNotification> pendingNotifications = Collections
            .synchronizedList(new ArrayList<CTInAppNotification>());

    private static CTInAppNotification currentlyDisplayingInApp = null;

    private static WeakReference<Activity> currentActivity;

    private static int initialAppEnteredForegroundTime = 0;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static String sdkVersion;  // For Google Play Store/Android Studio analytics

    private static final boolean isUIEditorEnabled = false;


    private long appLastSeen = 0;

    private String cachedGUID = null;

    private final Context context;

    private final int currentRequestTimestamp = 0;

    private CTExperimentsListener experimentsListener = null;

    private WeakReference<CTFeatureFlagsListener> featureFlagsListener;

    private GeofenceCallback geofenceCallback;

    private WeakReference<InAppNotificationButtonListener> inAppNotificationButtonListener;

    private InAppNotificationListener inAppNotificationListener;

    private HashSet<String> inappActivityExclude = null;

    private WeakReference<InboxMessageButtonListener> inboxMessageButtonListener;

    private final HashMap<String, Integer> installReferrerMap = new HashMap<>(8);

    private int lastVisitTime;

    private CoreState mCoreState;


    private final int mResponseFailureCount = 0;

    private String processingUserLoginIdentifier = null;

    private final Boolean processingUserLoginLock = true;

    private SyncListener syncListener = null;


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
                instance._createNotification(context, extras, notificationId);
            }
            return;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            boolean shouldProcess = false;
            if (instance != null) {
                shouldProcess = (_accountId == null && instance.getCoreState().getConfig().isDefaultInstance())
                        || instance
                        .getAccountId()
                        .equals(_accountId);
            }
            if (shouldProcess) {
                try {
                    instance._createNotification(context, extras, notificationId);
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
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("createNotificationChannel", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
                                }
                                NotificationChannel notificationChannel = new NotificationChannel(channelId,
                                        channelName,
                                        importance);
                                notificationChannel.setDescription(channelDescription);
                                notificationChannel.setShowBadge(showBadge);
                                notificationManager.createNotificationChannel(notificationChannel);
                                instance.getConfigLogger().info(instance.getAccountId(),
                                        "Notification channel " + channelName.toString() + " has been created");

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
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("creatingNotificationChannel", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
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
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("createNotificationChannel", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
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
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("creatingNotificationChannel", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {
                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
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
    public static void createNotificationChannelGroup(final Context context, final String groupId,
            final CharSequence groupName) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#createNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("creatingNotificationChannelGroup", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
                                }
                                notificationManager
                                        .createNotificationChannelGroup(
                                                new NotificationChannelGroup(groupId, groupName));
                                instance.getConfigLogger().info(instance.getAccountId(),
                                        "Notification channel group " + groupName.toString() + " has been created");

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
    public static void deleteNotificationChannel(final Context context, final String channelId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannel");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("deletingNotificationChannel", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
                                }
                                notificationManager.deleteNotificationChannel(channelId);
                                instance.getConfigLogger().info(instance.getAccountId(),
                                        "Notification channel " + channelId + " has been deleted");

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
    public static void deleteNotificationChannelGroup(final Context context, final String groupId) {
        final CleverTapAPI instance = getDefaultInstanceOrFirstOther(context);
        if (instance == null) {
            Logger.v("No CleverTap Instance found in CleverTapAPI#deleteNotificationChannelGroup");
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                instance.getCoreState().getPostAsyncSafelyHandler()
                        .postAsyncSafely("deletingNotificationChannelGroup", new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void run() {

                                NotificationManager notificationManager = (NotificationManager) context
                                        .getSystemService(NOTIFICATION_SERVICE);
                                if (notificationManager == null) {
                                    return;
                                }
                                notificationManager.deleteNotificationChannelGroup(groupId);
                                instance.getConfigLogger().info(instance.getAccountId(),
                                        "Notification channel group " + groupId + " has been deleted");

                            }
                        });
            }
        } catch (Throwable t) {
            instance.getConfigLogger()
                    .verbose(instance.getAccountId(), "Failure deleting Notification Channel Group", t);
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
     * @param level Can be one of the following: LogLevel.OFF (disables all debugging), LogLevel.INFO (default, shows
     *              minimal SDK integration related logging),
     *              LogLevel.DEBUG(shows debug output)
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
                shouldProcess = (_accountId == null && instance.getCoreState().getConfig().isDefaultInstance())
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
        //noinspection ConstantConditions
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
            instance.getCoreState().getPostAsyncSafelyHandler()
                    .postAsyncSafely("notifyProfileInitialized", new Runnable() {
                        @Override
                        public void run() {
                            if (finalInstance.getCleverTapID() != null) {
                                finalInstance.notifyUserProfileInitialized();
                                finalInstance.recordDeviceIDErrors();
                            }
                        }
                    });
        } else if (instance.isErrorDeviceId() && instance.getConfig().getEnableCustomCleverTapId() && Utils
                .validateCTID(cleverTapID)) {
            instance.asyncProfileSwitchUser(null, null, cleverTapID);
        }
        return instance;
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
                    instance.activityPaused();
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

        CleverTapAPI.setAppForeground(true);

        if (instances == null) {
            Logger.v("Instances is null in onActivityResumed!");
            return;
        }

        String currentActivityName = getCurrentActivityName();
        setCurrentActivity(activity);
        if (currentActivityName == null || !currentActivityName.equals(activity.getLocalClassName())) {
            CoreMetaData.incrementActivityCount();
        }

        if (initialAppEnteredForegroundTime <= 0) {
            initialAppEnteredForegroundTime = (int) System.currentTimeMillis() / 1000;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            try {
                if (instance != null) {
                    instance.activityResumed(activity);
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
                instance.processCustomPushNotification(extras);
            }
            return;
        }

        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance != null) {
                instance.processCustomPushNotification(extras);
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void runBackgroundIntentService(Context context) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                if (instance.getConfig().isBackgroundSync()) {
                    instance.runInstanceJobWork(context, null);
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
            instance.runInstanceJobWork(context, null);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void runJobWork(Context context, JobParameters parameters) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                if (instance.getConfig().isBackgroundSync()) {
                    instance.runInstanceJobWork(context, parameters);
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
            instance.runInstanceJobWork(context, parameters);
        }
    }

    //Push
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void tokenRefresh(Context context) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                instance.onTokenRefresh();
            }
            return;
        }
        for (String accountId : CleverTapAPI.instances.keySet()) {
            CleverTapAPI instance = CleverTapAPI.instances.get(accountId);
            if (instance != null && instance.getConfig().isAnalyticsOnly()) {
                Logger.d(accountId, "Instance is Analytics Only not processing device token");
                continue;
            }
            if (instance != null) {
                instance.onTokenRefresh();
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void tokenRefresh(Context context, String token, PushType pushType) {
        for (CleverTapAPI instance : getAvailableInstances(context)) {
            instance.getCoreState().getPushProviders().doTokenRefresh(token, pushType);
        }
    }

    // Initialize
    private CleverTapAPI(final Context context, final CleverTapInstanceConfig config, String cleverTapID) {
        this.context = context;

        initFeatureFlags(false);

        CoreState coreState = CleverTapFactory
                .getCoreState(context, config, cleverTapID);
        setCoreState(coreState);

        coreState.getPostAsyncSafelyHandler().postAsyncSafely("CleverTapAPI#initializeDeviceInfo", new Runnable() {
            @Override
            public void run() {

                if (config.isDefaultInstance()) {
                    manifestAsyncValidation();
                }
            }
        });

        int now = (int) System.currentTimeMillis() / 1000;
        if (now - initialAppEnteredForegroundTime > 5) {
            this.getCoreState().getConfig().setCreatedPostAppLaunch();
        }

        setLastVisitTime();

        // Default (flag is set in the config init) or first non-default instance gets the ABTestController
        if (!config.isDefaultInstance()) {
            if (instances == null || instances.size() <= 0) {
                config.setEnableABTesting(true);
            }
        }

        coreState.getPostAsyncSafelyHandler().postAsyncSafely("setStatesAsync", new Runnable() {
            @Override
            public void run() {
                ((NetworkManager) getCoreState().getNetworkManager()).setDeviceNetworkInfoReportingFromStorage();
                setCurrentUserOptOutStateFromStorage();
            }
        });

        coreState.getPostAsyncSafelyHandler().postAsyncSafely("saveConfigtoSharedPrefs", new Runnable() {
            @Override
            public void run() {
                String configJson = config.toJSONString();
                if (configJson == null) {
                    Logger.v("Unable to save config to SharedPrefs, config Json is null");
                    return;
                }
                StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(config, "instance"), configJson);
            }
        });

        if (this.getCoreState().getConfig().isBackgroundSync() && !this.getCoreState().getConfig()
                .isAnalyticsOnly()) {
            coreState.getPostAsyncSafelyHandler().postAsyncSafely("createOrResetJobScheduler", new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        createOrResetJobScheduler(context);
                    } else {
                        createAlarmScheduler(context);
                    }
                }
            });
        }
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void addMultiValuesForKey(final String key, final ArrayList<String> values) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("addMultiValuesForKey", new Runnable() {
            @Override
            public void run() {
                final String command = (getCoreState().getLocalDataStore().getProfileValueForKey(key) != null)
                        ? Constants.COMMAND_ADD : Constants.COMMAND_SET;
                _handleMultiValues(values, key, command);
            }
        });
    }


    /**
     * Deletes the given {@link CTInboxMessage} object
     *
     * @param message {@link CTInboxMessage} public object of inbox message
     */
    @SuppressWarnings({"unused"})
    public void deleteInboxMessage(final CTInboxMessage message) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("deleteInboxMessage", new Runnable() {
            @Override
            public void run() {
                synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
                    if (mCoreState.getCtInboxController() != null) {
                        boolean update = mCoreState.getCtInboxController()
                                .deleteMessageWithId(message.getMessageId());
                        if (update) {
                            mCoreState.getCallbackManager()._notifyInboxMessagesDidUpdate();
                        }
                    } else {
                        getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                    }
                }
            }
        });
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
        this.getCoreState().getConfig().enablePersonalization(false);
    }

    /**
     * Use this method to enable device network-related information tracking, including IP address.
     * This reporting is disabled by default.  To re-disable tracking call this method with enabled set to false.
     *
     * @param value boolean Whether device network info reporting should be enabled/disabled.
     */
    @SuppressWarnings({"unused"})
    public void enableDeviceNetworkInfoReporting(boolean value) {
        ((NetworkManager) getCoreState().getNetworkManager()).enableDeviceNetworkInfoReporting(value);
    }

    /**
     * Enables the Profile/Events Read and Synchronization API
     * Personalization is enabled by default
     */
    @SuppressWarnings({"unused"})
    public void enablePersonalization() {
        this.getCoreState().getConfig().enablePersonalization(true);
    }

    /**
     * @return object of {@link CTFeatureFlagsController}
     * Handler to get the feature flag values
     */
    public CTFeatureFlagsController featureFlag() {
        return mCoreState.getCtFeatureFlagsController();
    }

    //Push

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method
     */
    @Override
    public void featureFlagsDidUpdate() {
        try {
            if (featureFlagsListener != null && featureFlagsListener.get() != null) {
                featureFlagsListener.get().featureFlagsUpdated();
            }
        } catch (Throwable t) {
            // no-op
        }
    }

    /**
     * This method is internal to the CleverTap SDK.
     * Developers should not use this method manually
     */
    @Override
    public void fetchFeatureFlags() {
        if (getCoreState().getConfig().isAnalyticsOnly()) {
            return;
        }
        JSONObject event = new JSONObject();
        JSONObject notif = new JSONObject();
        try {
            notif.put("t", Constants.FETCH_TYPE_FF);
            event.put("evtName", Constants.WZRK_FETCH);
            event.put("evtData", notif);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.FETCH_EVENT);
    }


    /**
     * Sends all the events in the event queue.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void flush() {
        flushQueueAsync(context, EventGroup.REGULAR);
    }

    public String getAccountId() {
        return getCoreState().getConfig().getAccountId();
    }

    /**
     * Getter for retrieving all the Display Units.
     *
     * @return ArrayList<CleverTapDisplayUnit> - could be null, if there is no Display Unit campaigns
     */
    @Nullable
    public ArrayList<CleverTapDisplayUnit> getAllDisplayUnits() {

        if (mCoreState.getCTDisplayUnitController() != null) {
            return mCoreState.getCTDisplayUnitController().getAllDisplayUnits();
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
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() != null) {
                ArrayList<CTMessageDAO> messageDAOArrayList = mCoreState.getCtInboxController().getMessages();
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

    //Push

    /**
     * Returns the CTExperimentsListener object
     *
     * @return The {@link CTExperimentsListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public CTExperimentsListener getCTExperimentsListener() {
        return experimentsListener;
    }

    /**
     * This method is used to set the CTExperimentsListener
     *
     * @param experimentsListener The {@link CTExperimentsListener} object
     */
    @SuppressWarnings("unused")
    public void setCTExperimentsListener(CTExperimentsListener experimentsListener) {
        this.experimentsListener = experimentsListener;
    }

    /**
     * Returns the CTInboxListener object
     *
     * @return An {@link CTInboxListener} object
     */
    @SuppressWarnings({"unused"})
    public CTInboxListener getCTNotificationInboxListener() {
        return mCoreState.getCallbackManager().getInboxListener();
    }

    /**
     * This method sets the CTInboxListener
     *
     * @param notificationInboxListener An {@link CTInboxListener} object
     */
    @SuppressWarnings({"unused"})
    public void setCTNotificationInboxListener(CTInboxListener notificationInboxListener) {
        mCoreState.getCallbackManager().setInboxListener(notificationInboxListener);
    }

    //Debug

    /**
     * Returns the CTPushAmpListener object
     *
     * @return The {@link CTPushAmpListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public CTPushAmpListener getCTPushAmpListener() {
        return mCoreState.getCallbackManager().getPushAmpListener();
    }

    /**
     * This method is used to set the CTPushAmpListener
     *
     * @param pushAmpListener - The{@link CTPushAmpListener} object
     */
    @SuppressWarnings("unused")
    public void setCTPushAmpListener(CTPushAmpListener pushAmpListener) {
        mCoreState.getCallbackManager().setPushAmpListener(pushAmpListener);
    }

    /**
     * Returns the CTPushNotificationListener object
     *
     * @return The {@link CTPushNotificationListener} object
     */
    @SuppressWarnings("WeakerAccess")
    public CTPushNotificationListener getCTPushNotificationListener() {
        return mCoreState.getCallbackManager().getPushNotificationListener();
    }

    /**
     * This method is used to set the CTPushNotificationListener
     *
     * @param pushNotificationListener - The{@link CTPushNotificationListener} object
     */
    @SuppressWarnings("unused")
    public void setCTPushNotificationListener(CTPushNotificationListener pushNotificationListener) {
        mCoreState.getCallbackManager().setPushNotificationListener(pushNotificationListener);
    }

    /**
     * Returns a unique CleverTap identifier suitable for use with install attribution providers.
     *
     * @return The attribution identifier currently being used to identify this user.
     */
    @SuppressWarnings("unused")
    public String getCleverTapAttributionIdentifier() {
        return getCoreState().getDeviceInfo().getAttributionID();
    }

    /**
     * Returns a unique identifier by which CleverTap identifies this user.
     *
     * @return The user identifier currently being used to identify this user.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getCleverTapID() {
        return getCoreState().getDeviceInfo().getDeviceID();
    }

    //Network Info handling

    @RestrictTo(Scope.LIBRARY)
    public CoreState getCoreState() {
        return mCoreState;
    }

    void setCoreState(final CoreState cleverTapState) {
        mCoreState = cleverTapState;
    }

    /**
     * Returns the total count of the specified event
     *
     * @param event The event for which you want to get the total count
     * @return Total count in int
     */
    @SuppressWarnings({"unused"})
    public int getCount(String event) {
        EventDetail eventDetail = getCoreState().getLocalDataStore().getEventDetail(event);
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
        return getCoreState().getLocalDataStore().getEventDetail(event);
    }

    public Map<String, String> getDeviceInfo() {
        final Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("build", String.valueOf(getCoreState().getDeviceInfo().getBuild()));
        deviceInfo.put("versionName", getCoreState().getDeviceInfo().getVersionName());
        deviceInfo.put("osName", getCoreState().getDeviceInfo().getOsName());
        deviceInfo.put("osVersion", getCoreState().getDeviceInfo().getOsVersion());
        deviceInfo.put("manufacturer", getCoreState().getDeviceInfo().getManufacturer());
        deviceInfo.put("model", getCoreState().getDeviceInfo().getModel());
        deviceInfo.put("sdkVersion", String.valueOf(getCoreState().getDeviceInfo().getSdkVersion()));
        deviceInfo.put("dpi", String.valueOf(getCoreState().getDeviceInfo().getDPI()));
        deviceInfo.put("device_width", String.valueOf(getCoreState().getDeviceInfo().getWidthPixels()));
        deviceInfo.put("device_height", String.valueOf(getCoreState().getDeviceInfo().getHeightPixels()));
        if (getCoreState().getDeviceInfo().getLibrary() != null) {
            deviceInfo.put("library", getCoreState().getDeviceInfo().getLibrary());
        }
        return deviceInfo;
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
        return getCoreState().getPushProviders().getCachedToken(type);
    }

    /**
     * Returns the DevicePushTokenRefreshListener
     *
     * @return The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public DevicePushTokenRefreshListener getDevicePushTokenRefreshListener() {
        return getCoreState().getPushProviders().getDevicePushTokenRefreshListener();
    }

    /**
     * This method is used to set the DevicePushTokenRefreshListener object
     *
     * @param tokenRefreshListener The {@link DevicePushTokenRefreshListener} object
     */
    @SuppressWarnings("unused")
    public void setDevicePushTokenRefreshListener(DevicePushTokenRefreshListener tokenRefreshListener) {
        getCoreState().getPushProviders().setDevicePushTokenRefreshListener(tokenRefreshListener);

    }

    /**
     * Getter for retrieving Display Unit using the unitID
     *
     * @param unitID - unitID of the Display Unit {@link CleverTapDisplayUnit#getUnitID()}
     * @return CleverTapDisplayUnit - could be null, if there is no Display Unit campaign with the identifier
     */
    @Nullable
    public CleverTapDisplayUnit getDisplayUnitForId(String unitID) {
        if (mCoreState.getCTDisplayUnitController() != null) {
            return mCoreState.getCTDisplayUnitController().getDisplayUnitForID(unitID);
        } else {
            getConfigLogger().verbose(getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to get Display Unit for id: " + unitID);
            return null;
        }
    }

    //Util

    /**
     * Returns the timestamp of the first time the given event was raised
     *
     * @param event The event name for which you want the first time timestamp
     * @return The timestamp in int
     */
    @SuppressWarnings({"unused"})
    public int getFirstTime(String event) {
        EventDetail eventDetail = getCoreState().getLocalDataStore().getEventDetail(event);
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
        return this.geofenceCallback;
    }

    /**
     * This method is used to set the geofence callback
     * Register to handle geofence responses from CleverTap
     * This is to be used only by clevertap-geofence-sdk
     *
     * @param geofenceCallback The {@link GeofenceCallback} instance
     */

    @SuppressWarnings("unused")
    public void setGeofenceCallback(GeofenceCallback geofenceCallback) {
        this.geofenceCallback = geofenceCallback;
    }

    /**
     * Returns a Map of event names and corresponding event details of all the events raised
     *
     * @return A Map of Event Name and its corresponding EventDetail object
     */
    @SuppressWarnings({"unused"})
    public Map<String, EventDetail> getHistory() {
        return getCoreState().getLocalDataStore().getEventHistory(context);
    }

    /**
     * Returns the InAppNotificationListener object
     *
     * @return An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public InAppNotificationListener getInAppNotificationListener() {
        return inAppNotificationListener;
    }

    //DeepLink

    /**
     * This method sets the InAppNotificationListener
     *
     * @param inAppNotificationListener An {@link InAppNotificationListener} object
     */
    @SuppressWarnings({"unused"})
    public void setInAppNotificationListener(InAppNotificationListener inAppNotificationListener) {
        this.inAppNotificationListener = inAppNotificationListener;
    }

    /**
     * Returns the count of all inbox messages for the user
     *
     * @return int - count of all inbox messages
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getInboxMessageCount() {
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() != null) {
                return mCoreState.getCtInboxController().count();
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
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() != null) {
                CTMessageDAO message = mCoreState.getCtInboxController().getMessageForId(messageId);
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
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() != null) {
                return mCoreState.getCtInboxController().unreadCount();
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
        EventDetail eventDetail = getCoreState().getLocalDataStore().getEventDetail(event);
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
        mCoreState.getLocationManager()._setLocation(location);
    }

    /**
     * Returns the timestamp of the previous visit
     *
     * @return Timestamp of previous visit in int
     */
    @SuppressWarnings({"unused"})
    public int getPreviousVisitTime() {
        return lastVisitTime;
    }

    /**
     * Return the user profile property value for the specified key
     *
     * @param name String
     * @return {@link JSONArray}, String or null
     */
    @SuppressWarnings({"unused"})
    public Object getProperty(String name) {
        if (!getCoreState().getConfig().isPersonalizationEnabled()) {
            return null;
        }
        return getCoreState().getLocalDataStore().getProfileProperty(name);
    }

    /**
     * Returns the token for a particular push type
     */
    public String getPushToken(@NonNull PushConstants.PushType pushType) {
        return getCoreState().getPushProviders().getCachedToken(pushType);
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
        return syncListener;
    }

    /**
     * This method is used to set the SyncListener
     *
     * @param syncListener The {@link SyncListener} object
     */
    @SuppressWarnings("unused")
    public void setSyncListener(SyncListener syncListener) {
        this.syncListener = syncListener;
    }

    /**
     * Returns the time elapsed by the user on the app
     *
     * @return Time elapsed by user on the app in int
     */
    @SuppressWarnings({"unused"})
    public int getTimeElapsed() {
        int currentSession = getCoreState().getCoreMetaData().getCurrentSession();
        if (currentSession == 0) {
            return -1;
        }

        int now = (int) (System.currentTimeMillis() / 1000);
        return now - currentSession;
    }

    /**
     * Returns the total number of times the app has been launched
     *
     * @return Total number of app launches in int
     */
    @SuppressWarnings({"unused"})
    public int getTotalVisits() {
        EventDetail ed = getCoreState().getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
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
        ud.setSource(getCoreState().getCoreMetaData().getSource());
        ud.setMedium(getCoreState().getCoreMetaData().getMedium());
        ud.setCampaign(getCoreState().getCoreMetaData().getCampaign());
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
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() != null) {
                ArrayList<CTMessageDAO> messageDAOArrayList = mCoreState.getCtInboxController().getUnreadMessages();
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

    @Override
    public void inAppNotificationDidClick(CTInAppNotification inAppNotification, Bundle formData,
            HashMap<String, String> keyValueMap) {
        pushInAppNotificationStateEvent(true, inAppNotification, formData);
        if (keyValueMap != null && !keyValueMap.isEmpty()) {
            if (inAppNotificationButtonListener != null && inAppNotificationButtonListener.get() != null) {
                inAppNotificationButtonListener.get().onInAppButtonClick(keyValueMap);
            }
        }
    }

    @Override
    public void inAppNotificationDidDismiss(final Context context, final CTInAppNotification inAppNotification,
            Bundle formData) {
        inAppNotification.didDismiss();
        if (getCoreState().getInAppFCManager() != null) {
            getCoreState().getInAppFCManager().didDismiss(inAppNotification);
            getConfigLogger().verbose(getAccountId(), "InApp Dismissed: " + inAppNotification.getCampaignId());
        }
        try {
            final InAppNotificationListener listener = getInAppNotificationListener();
            if (listener != null) {
                final HashMap<String, Object> notifKVS;

                if (inAppNotification.getCustomExtras() != null) {
                    notifKVS = Utils.convertJSONObjectToHashMap(inAppNotification.getCustomExtras());
                } else {
                    notifKVS = new HashMap<>();
                }

                Logger.v("Calling the in-app listener on behalf of " + getCoreState().getCoreMetaData().getSource());

                if (formData != null) {
                    listener.onDismissed(notifKVS, Utils.convertBundleObjectToHashMap(formData));
                } else {
                    listener.onDismissed(notifKVS, null);
                }
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to call the in-app notification listener", t);
        }

        // Fire the next one, if any
        runOnNotificationQueue(new Runnable() {
            @Override
            public void run() {
                inAppDidDismiss(context, getConfig(), inAppNotification);
                _showNotificationIfAvailable(context);
            }
        });
    }

    //InApp
    @Override
    public void inAppNotificationDidShow(CTInAppNotification inAppNotification, Bundle formData) {
        pushInAppNotificationStateEvent(false, inAppNotification, formData);
    }

    /**
     * Initializes the inbox controller and sends a callback to the {@link CTInboxListener}
     * This method needs to be called separately for each instance of {@link CleverTapAPI}
     */
    @SuppressWarnings({"unused"})
    public void initializeInbox() {
        mCoreState.initializeInbox();
    }

    /**
     * Marks the given {@link CTInboxMessage} object as read
     *
     * @param message {@link CTInboxMessage} public object of inbox message
     */
    //marks the message as read
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void markReadInboxMessage(final CTInboxMessage message) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("markReadInboxMessage", new Runnable() {
            @Override
            public void run() {
                synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
                    if (mCoreState.getCtInboxController() != null) {
                        boolean read = mCoreState.getCtInboxController()
                                .markReadForMessageWithId(message.getMessageId());
                        if (read) {
                            mCoreState.getCallbackManager()._notifyInboxMessagesDidUpdate();
                        }
                    } else {
                        getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
                    }
                }
            }
        });
    }

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
        pushInboxMessageStateEvent(true, inboxMessage, data);
        if (keyValue != null && !keyValue.isEmpty()) {
            if (inboxMessageButtonListener != null && inboxMessageButtonListener.get() != null) {
                inboxMessageButtonListener.get().onInboxButtonClick(keyValue);
            }
        }
    }

    @Override
    public void messageDidShow(CTInboxActivity ctInboxActivity, final CTInboxMessage inboxMessage,
            final Bundle data) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("handleMessageDidShow", new Runnable() {
            @Override
            public void run() {
                CTInboxMessage message = getInboxMessageForId(inboxMessage.getMessageId());
                if (!message.isRead()) {
                    markReadInboxMessage(inboxMessage);
                    pushInboxMessageStateEvent(false, inboxMessage, data);
                }
            }
        });
    }

    //InApp
    @Override
    public void notificationReady(final CTInAppNotification inAppNotification) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getCoreState().getMainLooperHandler().post(new Runnable() {
                @Override
                public void run() {
                    notificationReady(inAppNotification);
                }
            });
            return;
        }

        if (inAppNotification.getError() != null) {
            getConfigLogger()
                    .debug(getAccountId(), "Unable to process inapp notification " + inAppNotification.getError());
            return;
        }
        getConfigLogger().debug(getAccountId(), "Notification ready: " + inAppNotification.getJsonDescription());
        displayNotification(inAppNotification);
    }

    //Session

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
        if (getConfig().getEnableCustomCleverTapId()) {
            if (cleverTapID == null) {
                Logger.i(
                        "CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml Please call onUserlogin() and pass a custom CleverTap ID");
            }
        } else {
            if (cleverTapID != null) {
                Logger.i(
                        "CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml Please call CleverTapAPI.defaultInstance() without a custom CleverTap ID");
            }
        }
        _onUserLogin(profile, cleverTapID);
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
        return mCoreState.getCtProductConfigController();
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
        getCoreState().getPushProviders().handleToken(regId, PushType.BPS, register);
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

        if (chargeDetails == null || items == null) {
            getConfigLogger().debug(getAccountId(), "Invalid Charged event: details and or items is null");
            return;
        }

        if (items.size() > 50) {
            ValidationResult error = ValidationResultFactory.create(522);
            getConfigLogger().debug(getAccountId(), error.getErrorDesc());
            getCoreState().getValidationResultStack().pushValidationResult(error);
        }

        JSONObject evtData = new JSONObject();
        JSONObject chargedEvent = new JSONObject();
        ValidationResult vr;
        try {
            for (String key : chargeDetails.keySet()) {
                Object value = chargeDetails.get(key);
                vr = getCoreState().getValidator().cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));
                }

                try {
                    vr = getCoreState().getValidator().cleanObjectValue(value, Validator.ValidationContext.Event);
                } catch (IllegalArgumentException e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = ValidationResultFactory.create(511,
                            Constants.PROP_VALUE_NOT_PRIMITIVE, "Charged", key,
                            value != null ? value.toString() : "");
                    getCoreState().getValidationResultStack().pushValidationResult(error);
                    getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                    // Skip this property
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));
                }

                evtData.put(key, value);
            }

            JSONArray jsonItemsArray = new JSONArray();
            for (HashMap<String, Object> map : items) {
                JSONObject itemDetails = new JSONObject();
                for (String key : map.keySet()) {
                    Object value = map.get(key);
                    vr = getCoreState().getValidator().cleanObjectKey(key);
                    key = vr.getObject().toString();
                    // Check for an error
                    if (vr.getErrorCode() != 0) {
                        chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));
                    }

                    try {
                        vr = getCoreState().getValidator().cleanObjectValue(value, Validator.ValidationContext.Event);
                    } catch (IllegalArgumentException e) {
                        // The object was neither a String, Boolean, or any number primitives
                        ValidationResult error = ValidationResultFactory
                                .create(511, Constants.OBJECT_VALUE_NOT_PRIMITIVE, key,
                                        value != null ? value.toString() : "");
                        getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                        getCoreState().getValidationResultStack().pushValidationResult(error);
                        // Skip this property
                        continue;
                    }
                    value = vr.getObject();
                    // Check for an error
                    if (vr.getErrorCode() != 0) {
                        chargedEvent.put(Constants.ERROR_KEY, getErrorObject(vr));
                    }
                    itemDetails.put(key, value);
                }
                jsonItemsArray.put(itemDetails);
            }
            evtData.put("Items", jsonItemsArray);

            chargedEvent.put("evtName", Constants.CHARGED_EVENT);
            chargedEvent.put("evtData", evtData);
            getCoreState().getBaseEventQueueManager().queueEvent(context, chargedEvent, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }

    /**
     * Use this method to pass the deeplink with UTM parameters to track installs
     *
     * @param uri URI of the deeplink
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushDeepLink(Uri uri) {
        pushDeepLink(uri, false);
    }

    /**
     * Raises the Display Unit Clicked event
     *
     * @param unitID - unitID of the Display Unit{@link CleverTapDisplayUnit#getUnitID()}
     */
    @SuppressWarnings("unused")
    public void pushDisplayUnitClickedEventForID(String unitID) {
        JSONObject event = new JSONObject();

        try {
            event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);

            //wzrk fields
            if (mCoreState.getCTDisplayUnitController() != null) {
                CleverTapDisplayUnit displayUnit = mCoreState.getCTDisplayUnitController()
                        .getDisplayUnitForID(unitID);
                if (displayUnit != null) {
                    JSONObject eventExtraData = displayUnit.getWZRKFields();
                    if (eventExtraData != null) {
                        event.put("evtData", eventExtraData);
                        try {
                            getCoreState().getCoreMetaData().setWzrkParams(eventExtraData);
                        } catch (Throwable t) {
                            // no-op
                        }
                    }
                }
            }

            getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
            getConfigLogger().verbose(getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit clicked event" + t);
        }
    }

    /**
     * Raises the Display Unit Viewed event
     *
     * @param unitID - unitID of the Display Unit{@link CleverTapDisplayUnit#getUnitID()}
     */
    @SuppressWarnings("unused")
    public void pushDisplayUnitViewedEventForID(String unitID) {
        JSONObject event = new JSONObject();

        try {
            event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);

            //wzrk fields
            if (mCoreState.getCTDisplayUnitController() != null) {
                CleverTapDisplayUnit displayUnit = mCoreState.getCTDisplayUnitController()
                        .getDisplayUnitForID(unitID);
                if (displayUnit != null) {
                    JSONObject eventExtras = displayUnit.getWZRKFields();
                    if (eventExtras != null) {
                        event.put("evtData", eventExtras);
                    }
                }
            }

            getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
            getConfigLogger().verbose(getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Failed to push Display Unit viewed event" + t);
        }
    }

    /**
     * Internally records an "Error Occurred" event, which can be viewed in the dashboard.
     *
     * @param errorMessage The error message
     * @param errorCode    The error code
     */
    @SuppressWarnings({"unused"})
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

        if (eventName == null || eventName.equals("")) {
            return;
        }

        ValidationResult validationResult = getCoreState().getValidator().isRestrictedEventName(eventName);
        // Check for a restricted event name
        if (validationResult.getErrorCode() > 0) {
            getCoreState().getValidationResultStack().pushValidationResult(validationResult);
            return;
        }

        ValidationResult discardedResult = getCoreState().getValidator().isEventDiscarded(eventName);
        // Check for a discarded event name
        if (discardedResult.getErrorCode() > 0) {
            getCoreState().getValidationResultStack().pushValidationResult(discardedResult);
            return;
        }

        if (eventActions == null) {
            eventActions = new HashMap<>();
        }

        JSONObject event = new JSONObject();
        try {
            // Validate
            ValidationResult vr = getCoreState().getValidator().cleanEventName(eventName);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                event.put(Constants.ERROR_KEY, getErrorObject(vr));
            }

            eventName = vr.getObject().toString();
            JSONObject actions = new JSONObject();
            for (String key : eventActions.keySet()) {
                Object value = eventActions.get(key);
                vr = getCoreState().getValidator().cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                try {
                    vr = getCoreState().getValidator().cleanObjectValue(value, Validator.ValidationContext.Event);
                } catch (IllegalArgumentException e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = ValidationResultFactory
                            .create(512, Constants.PROP_VALUE_NOT_PRIMITIVE, eventName, key,
                                    value != null ? value.toString() : "");
                    getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                    getCoreState().getValidationResultStack().pushValidationResult(error);
                    // Skip this record
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    event.put(Constants.ERROR_KEY, getErrorObject(vr));
                }
                actions.put(key, value);
            }
            event.put("evtName", eventName);
            event.put("evtData", actions);
            getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
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
        getCoreState().getPushProviders().handleToken(fcmId, PushType.FCM, register);
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
        getCoreState().getValidationResultStack().pushValidationResult(validationResult);
    }

    /**
     * Pushes the Geofence Cluster Exited event to CleverTap.
     *
     * @param geoFenceProperties The {@link JSONObject} object that contains the
     *                           event properties regarding GeoFence Cluster Exited event
     */
    @SuppressWarnings("unused")
    public Future<?> pushGeoFenceExitedEvent(JSONObject geoFenceProperties) {
        return raiseEventForGeofences(Constants.GEOFENCE_EXITED_EVENT_NAME, geoFenceProperties);
    }

    /**
     * Pushes the Geofence Cluster Entered event to CleverTap.
     *
     * @param geofenceProperties The {@link JSONObject} object that contains the
     *                           event properties regarding GeoFence Cluster Entered event
     */
    @SuppressWarnings("unused")
    public Future<?> pushGeofenceEnteredEvent(JSONObject geofenceProperties) {
        return raiseEventForGeofences(Constants.GEOFENCE_ENTERED_EVENT_NAME, geofenceProperties);
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
        getCoreState().getPushProviders().handleToken(regId, PushType.HPS, register);
    }

    /**
     * Pushes the Notification Clicked event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of {@link CTInboxMessage}
     */
    @SuppressWarnings("unused")
    public void pushInboxNotificationClickedEvent(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        pushInboxMessageStateEvent(true, message, null);
    }

    /**
     * Pushes the Notification Viewed event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of {@link CTInboxMessage}
     */
    @SuppressWarnings("unused")
    public void pushInboxNotificationViewedEvent(String messageId) {
        CTInboxMessage message = getInboxMessageForId(messageId);
        pushInboxMessageStateEvent(false, message, null);
    }

    /**
     * This method is used to push install referrer via url String
     *
     * @param url A String with the install referrer parameters
     */
    @SuppressWarnings({"unused"})
    public void pushInstallReferrer(String url) {
        try {
            getConfigLogger().verbose(getAccountId(), "Referrer received: " + url);

            if (url == null) {
                return;
            }
            int now = (int) (System.currentTimeMillis() / 1000);

            //noinspection ConstantConditions
            if (installReferrerMap.containsKey(url) && now - installReferrerMap.get(url) < 10) {
                getConfigLogger()
                        .verbose(getAccountId(), "Skipping install referrer due to duplicate within 10 seconds");
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
     *
     * @param source   The UTM source parameter
     * @param medium   The UTM medium parameter
     * @param campaign The UTM campaign parameter
     */
    @SuppressWarnings({"unused"})
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

    /**
     * Pushes the Notification Clicked event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushNotificationClickedEvent(final Bundle extras) {
        mCoreState.getPushProviders().pushNotificationClickedEvent(extras);
    }

    /**
     * Pushes the Notification Viewed event to CleverTap.
     *
     * @param extras The {@link Bundle} object that contains the
     *               notification details
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void pushNotificationViewedEvent(Bundle extras) {
        mCoreState.getPushProviders().pushNotificationViewedEvent(extras);
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
        if (profile == null || profile.isEmpty()) {
            return;
        }

        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("profilePush", new Runnable() {
            @Override
            public void run() {
                _push(profile);
            }
        });
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
        getCoreState().getPushProviders().handleToken(regId, PushType.XPS, register);
    }

    //Session

    /**
     * Record a Screen View event
     *
     * @param screenName String, the name of the screen
     */
    @SuppressWarnings({"unused"})
    public void recordScreen(String screenName) {
        if (screenName == null || (!getCoreState().getCoreMetaData().getScreenName().isEmpty() && getCoreState()
                .getCoreMetaData().getScreenName().equals(screenName))) {
            return;
        }
        getConfigLogger().debug(getAccountId(), "Screen changed to " + screenName);
        getCoreState().getCoreMetaData().setCurrentScreenName(screenName);
        recordPageEventWithExtras(null);
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeMultiValuesForKey(final String key, final ArrayList<String> values) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("removeMultiValuesForKey", new Runnable() {
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
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeValueForKey(final String key) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("removeValueForKey", new Runnable() {
            @Override
            public void run() {
                _removeValueForKey(key);
            }
        });
    }

    /**
     * This method is used to set the CTFeatureFlagsListener
     * Register to receive feature flag callbacks
     *
     * @param featureFlagsListener The {@link CTFeatureFlagsListener} object
     */
    @SuppressWarnings("unused")
    public void setCTFeatureFlagsListener(CTFeatureFlagsListener featureFlagsListener) {
        this.featureFlagsListener = new WeakReference<>(featureFlagsListener);
    }

    /**
     * This method is used to set the product config listener
     * Register to receive callbacks
     *
     * @param listener The {@link CTProductConfigListener} instance
     */
    @SuppressWarnings("unused")
    public void setCTProductConfigListener(CTProductConfigListener listener) {
        mCoreState.getCallbackManager().setProductConfigListener(listener);
    }

    /**
     * Sets the listener to get the list of currently running Display Campaigns via callback
     *
     * @param listener- {@link DisplayUnitListener}
     */
    public void setDisplayUnitListener(DisplayUnitListener listener) {
        mCoreState.getCallbackManager().setDisplayUnitListener(listener);
    }

    public void setInAppNotificationButtonListener(InAppNotificationButtonListener listener) {
        this.inAppNotificationButtonListener = new WeakReference<>(listener);
    }

    public void setInboxMessageButtonListener(InboxMessageButtonListener listener) {
        this.inboxMessageButtonListener = new WeakReference<>(listener);
    }

    //Listener

    /**
     * Not to be used by developers. This is used internally to help CleverTap know which library is wrapping the
     * native SDK
     *
     * @param library {@link String} library name
     */
    public void setLibrary(String library) {
        if (getCoreState().getDeviceInfo() != null) {
            getCoreState().getDeviceInfo().setLibrary(library);
        }
    }

    /**
     * Sets the location in CleverTap to get updated GeoFences
     *
     * @param location android.location.Location
     */
    @SuppressWarnings("unused")
    public Future<?> setLocationForGeofences(Location location, int sdkVersion) {
        getCoreState().getCoreMetaData().setLocationForGeofence(true);
        getCoreState().getCoreMetaData().setGeofenceSDKVersion(sdkVersion);
        return mCoreState.getLocationManager()._setLocation(location);
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
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("setMultiValuesForKey", new Runnable() {
            @Override
            public void run() {
                _handleMultiValues(values, key, Constants.COMMAND_SET);
            }
        });
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
        getCoreState().getCoreMetaData().setOffline(value);
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
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("setOptOut", new Runnable() {
            @Override
            public void run() {
                // generate the data for a profile push to alert the server to the optOut state change
                HashMap<String, Object> optOutMap = new HashMap<>();
                optOutMap.put(Constants.CLEVERTAP_OPTOUT, enable);

                // determine order of operations depending on enabled/disabled
                if (enable) {  // if opting out first push profile event then set the flag
                    pushProfile(optOutMap);
                    getCoreState().getCoreMetaData().setCurrentUserOptedOut(true);
                } else {  // if opting back in first reset the flag to false then push the profile event
                    getCoreState().getCoreMetaData().setCurrentUserOptedOut(false);
                    pushProfile(optOutMap);
                }
                // persist the new optOut state
                String key = optOutKey();
                if (key == null) {
                    getConfigLogger()
                            .verbose(getAccountId(), "Unable to persist user OptOut state, storage key is null");
                    return;
                }
                StorageHelper.putBoolean(context, StorageHelper.storageKeyWithSuffix(getConfig(), key), enable);
                getConfigLogger().verbose(getAccountId(), "Set current user OptOut state to: " + enable);
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
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            if (mCoreState.getCtInboxController() == null) {
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
        Logger.v("Initializing InAppFC after Device ID Created = " + deviceId);
        getCoreState().setInAppFCManager(new InAppFCManager(context, getCoreState().getConfig(), deviceId));
        Logger.v("Initializing ABTesting after Device ID Created = " + deviceId);
        initFeatureFlags(true);
        //reinitialising product config controller with google ad id.
        mCoreState.getCtProductConfigController().setGuidAndInit(deviceId);
        getConfigLogger()
                .verbose("Got device id from DeviceInfo, notifying user profile initialized to SyncListener");
        notifyUserProfileInitialized(deviceId);
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
    void pushInAppNotificationStateEvent(boolean clicked, CTInAppNotification data, Bundle customData) {
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
                    getCoreState().getCoreMetaData().setWzrkParams(notif);
                } catch (Throwable t) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            }

            event.put("evtData", notif);
            getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable ignored) {
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
                    getCoreState().getCoreMetaData().setWzrkParams(notif);
                } catch (Throwable t) {
                    // no-op
                }
                event.put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME);
            } else {
                event.put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME);
            }

            event.put("evtData", notif);
            getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (Throwable ignored) {
            // We won't get here
        }
    }

    private JSONArray _cleanMultiValues(ArrayList<String> values, String key) {

        try {
            if (values == null || key == null) {
                return null;
            }

            JSONArray cleanedValues = new JSONArray();
            ValidationResult vr;

            // loop through and clean the new values
            for (String value : values) {
                value = (value == null) ? "" : value;  // so we will generate a validation error later on

                // validate value
                vr = getCoreState().getValidator().cleanMultiValuePropertyValue(value);

                // Check for an error
                if (vr.getErrorCode() != 0) {
                    getCoreState().getValidationResultStack().pushValidationResult(vr);
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

    private JSONArray _constructExistingMultiValue(String key, String command) {

        boolean remove = command.equals(Constants.COMMAND_REMOVE);
        boolean add = command.equals(Constants.COMMAND_ADD);

        // only relevant for add's and remove's; a set overrides the existing value, so return a new array
        if (!remove && !add) {
            return new JSONArray();
        }

        Object existing = _getProfilePropertyIgnorePersonalizationFlag(key);

        // if there is no existing value
        if (existing == null) {
            // if its a remove then return null to abort operation
            // no point in running remove against a nonexistent value
            if (remove) {
                return null;
            }

            // otherwise return an empty array
            return new JSONArray();
        }

        // value exists

        // the value should only ever be a JSONArray or scalar (String really)

        // if its already a JSONArray return that
        if (existing instanceof JSONArray) {
            return (JSONArray) existing;
        }

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


    private void _generateEmptyMultiValueError(String key) {
        ValidationResult error = ValidationResultFactory.create(512, Constants.INVALID_MULTI_VALUE, key);
        getCoreState().getValidationResultStack().pushValidationResult(error);
        getConfigLogger().debug(getAccountId(), error.getErrorDesc());
    }

    private void _generateInvalidMultiValueKeyError(String key) {
        ValidationResult error = ValidationResultFactory.create(523, Constants.INVALID_MULTI_VALUE_KEY, key);
        getCoreState().getValidationResultStack().pushValidationResult(error);
        getConfigLogger().debug(getAccountId(),
                "Invalid multi-value property key " + key + " profile multi value operation aborted");
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

    // use for internal profile getter doesn't do the personalization check
    private Object _getProfilePropertyIgnorePersonalizationFlag(String key) {
        return getCoreState().getLocalDataStore().getProfileValueForKey(key);
    }

    private void _handleMultiValues(ArrayList<String> values, String key, String command) {
        if (key == null) {
            return;
        }

        if (values == null || values.isEmpty()) {
            _generateEmptyMultiValueError(key);
            return;
        }

        ValidationResult vr;

        // validate the key
        vr = getCoreState().getValidator().cleanMultiValuePropertyKey(key);

        // Check for an error
        if (vr.getErrorCode() != 0) {
            getCoreState().getValidationResultStack().pushValidationResult(vr);
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

    private void _onUserLogin(final Map<String, Object> profile, final String cleverTapID) {
        if (profile == null) {
            return;
        }

        try {
            final String currentGUID = getCleverTapID();
            if (currentGUID == null) {
                return;
            }

            boolean haveIdentifier = false;
            LoginInfoProvider loginInfoProvider = new LoginInfoProvider(mCoreState.getContext(),
                    mCoreState.getConfig(), mCoreState.getDeviceInfo());
            // check for valid identifier keys
            // use the first one we find
            IdentityRepo iProfileHandler = IdentityRepoFactory
                    .getRepo(mCoreState.getContext(), mCoreState.getConfig(), mCoreState.getDeviceInfo(),
                            mCoreState.getValidationResultStack());
            for (String key : profile.keySet()) {
                Object value = profile.get(key);
                boolean isProfileKey = iProfileHandler.hasIdentity(key);
                if (isProfileKey) {
                    try {
                        String identifier = null;
                        if (value != null) {
                            identifier = value.toString();
                        }
                        if (identifier != null && identifier.length() > 0) {
                            haveIdentifier = true;
                            cachedGUID = loginInfoProvider.getGUIDForIdentifier(key, identifier);
                            if (cachedGUID != null) {
                                break;
                            }
                        }
                    } catch (Throwable t) {
                        // no-op
                    }
                }
            }

            // if no valid identifier provided or there are no identified users on the device; just push on the current profile
            if (!isErrorDeviceId()) {
                if (!haveIdentifier || loginInfoProvider.isAnonymousDevice()) {
                    getConfigLogger().debug(getAccountId(),
                            "onUserLogin: no identifier provided or device is anonymous, pushing on current user profile");
                    pushProfile(profile);
                    return;
                }
            }

            // if identifier maps to current guid, push on current profile
            if (cachedGUID != null && cachedGUID.equals(currentGUID)) {
                getConfigLogger().debug(getAccountId(),
                        "onUserLogin: " + profile.toString() + " maps to current device id " + currentGUID
                                + " pushing on current profile");
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

            asyncProfileSwitchUser(profile, cachedGUID, cleverTapID);

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "onUserLogin failed", t);
        }
    }

    //Session

    private void _push(Map<String, Object> profile) {
        if (profile == null || profile.isEmpty()) {
            return;
        }

        try {
            ValidationResult vr;
            JSONObject customProfile = new JSONObject();
            JSONObject fieldsToUpdateLocally = new JSONObject();
            for (String key : profile.keySet()) {
                Object value = profile.get(key);

                vr = getCoreState().getValidator().cleanObjectKey(key);
                key = vr.getObject().toString();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    getCoreState().getValidationResultStack().pushValidationResult(vr);
                }

                if (key.isEmpty()) {
                    ValidationResult keyError = ValidationResultFactory.create(512, Constants.PUSH_KEY_EMPTY);
                    getCoreState().getValidationResultStack().pushValidationResult(keyError);
                    getConfigLogger().debug(getAccountId(), keyError.getErrorDesc());
                    // Skip this property
                    continue;
                }

                try {
                    vr = getCoreState().getValidator().cleanObjectValue(value, Validator.ValidationContext.Profile);
                } catch (Throwable e) {
                    // The object was neither a String, Boolean, or any number primitives
                    ValidationResult error = ValidationResultFactory.create(512,
                            Constants.OBJECT_VALUE_NOT_PRIMITIVE_PROFILE,
                            value != null ? value.toString() : "", key);
                    getCoreState().getValidationResultStack().pushValidationResult(error);
                    getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                    // Skip this property
                    continue;
                }
                value = vr.getObject();
                // Check for an error
                if (vr.getErrorCode() != 0) {
                    getCoreState().getValidationResultStack().pushValidationResult(vr);
                }

                // test Phone:  if no device country code, test if phone starts with +, log but always send
                if (key.equalsIgnoreCase("Phone")) {
                    try {
                        value = value.toString();
                        String countryCode = getCoreState().getDeviceInfo().getCountryCode();
                        if (countryCode == null || countryCode.isEmpty()) {
                            String _value = (String) value;
                            if (!_value.startsWith("+")) {
                                ValidationResult error = ValidationResultFactory
                                        .create(512, Constants.INVALID_COUNTRY_CODE, _value);
                                getCoreState().getValidationResultStack().pushValidationResult(error);
                                getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                            }
                        }
                        getConfigLogger().verbose(getAccountId(),
                                "Profile phone is: " + value + " device country code is: " + ((countryCode != null)
                                        ? countryCode : "null"));
                    } catch (Exception e) {
                        getCoreState().getValidationResultStack()
                                .pushValidationResult(ValidationResultFactory.create(512, Constants.INVALID_PHONE));
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
                getCoreState().getLocalDataStore().setProfileFields(fieldsToUpdateLocally);
            }

            getCoreState().getBaseEventQueueManager().pushBasicProfile(customProfile);

        } catch (Throwable t) {
            // Will not happen
            getConfigLogger().verbose(getAccountId(), "Failed to push profile", t);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void _pushFacebookUser(JSONObject graphUser) {
        try {
            if (graphUser == null) {
                return;
            }
            // Note: No validations are required here, as everything is controlled
            String name = getGraphUserPropertySafely(graphUser, "name", "");
            try {
                // Certain users have nasty looking names - unicode chars, validate for any
                // not allowed chars
                ValidationResult vr = getCoreState().getValidator()
                        .cleanObjectValue(name, Validator.ValidationContext.Profile);
                name = vr.getObject().toString();

                if (vr.getErrorCode() != 0) {
                    getCoreState().getValidationResultStack().pushValidationResult(vr);
                }
            } catch (IllegalArgumentException e) {
                // Weird name, wasn't a string, or any number
                // This would never happen with FB
                name = "";
            }

            String gender = getGraphUserPropertySafely(graphUser, "gender", null);
            // Convert to WR format
            if (gender != null) {
                if (gender.toLowerCase().startsWith("m")) {
                    gender = "M";
                } else if (gender.toLowerCase().startsWith("f")) {
                    gender = "F";
                } else {
                    gender = "";
                }
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
                if (fbEdu.toLowerCase().contains("high school")) {
                    education = "School";
                } else if (fbEdu.toLowerCase().contains("college")) {
                    education = "College";
                } else if (fbEdu.toLowerCase().contains("graduate school")) {
                    education = "Graduate";
                } else {
                    education = "";
                }
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
            if (id != null && id.length() > 3) {
                profile.put("FBID", id);
            }
            if (name != null && name.length() > 3) {
                profile.put("Name", name);
            }
            if (email != null && email.length() > 3) {
                profile.put("Email", email);
            }
            if (gender != null && !gender.trim().equals("")) {
                profile.put("Gender", gender);
            }
            if (education != null && !education.trim().equals("")) {
                profile.put("Education", education);
            }
            if (work != null && !work.trim().equals("")) {
                profile.put("Employed", work);
            }
            if (birthday != null && birthday.length() > 3) {
                profile.put("DOB", birthday);
            }
            if (married != null && !married.trim().equals("")) {
                profile.put("Married", married);
            }

            getCoreState().getBaseEventQueueManager().pushBasicProfile(profile);
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to parse graph user object successfully", t);
        }
    }

    private void _removeValueForKey(String key) {
        try {
            key = (key == null) ? "" : key; // so we will generate a validation error later on

            // validate the key
            ValidationResult vr;

            vr = getCoreState().getValidator().cleanObjectKey(key);
            key = vr.getObject().toString();

            if (key.isEmpty()) {
                ValidationResult error = ValidationResultFactory.create(512, Constants.KEY_EMPTY);
                getCoreState().getValidationResultStack().pushValidationResult(error);
                getConfigLogger().debug(getAccountId(), error.getErrorDesc());
                // Abort
                return;
            }
            // Check for an error
            if (vr.getErrorCode() != 0) {
                getCoreState().getValidationResultStack().pushValidationResult(vr);
            }

            // remove from the local profile
            getCoreState().getLocalDataStore().removeProfileField(key);

            // send the delete command
            JSONObject command = new JSONObject().put(Constants.COMMAND_DELETE, true);
            JSONObject update = new JSONObject().put(key, command);
            getCoreState().getBaseEventQueueManager().pushBasicProfile(update);

            getConfigLogger().verbose(getAccountId(), "removing value for key " + key + " from user profile");

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to remove profile value for key " + key, t);
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

            checkPendingNotifications(context,
                    getCoreState().getConfig());  // see if we have any pending notifications

            JSONArray inapps = new JSONArray(
                    StorageHelper.getStringFromPrefs(context, getCoreState().getConfig(), Constants.INAPP_KEY, "[]"));
            if (inapps.length() < 1) {
                return;
            }

            JSONObject inapp = inapps.getJSONObject(0);
            prepareNotificationForDisplay(inapp);

            // JSON array doesn't have the feature to remove a single element,
            // so we have to copy over the entire array, but the first element
            JSONArray inappsUpdated = new JSONArray();
            for (int i = 0; i < inapps.length(); i++) {
                if (i == 0) {
                    continue;
                }
                inappsUpdated.put(inapps.get(i));
            }
            SharedPreferences.Editor editor = prefs.edit()
                    .putString(StorageHelper.storageKeyWithSuffix(getConfig(), Constants.INAPP_KEY),
                            inappsUpdated.toString());
            StorageHelper.persist(editor);
        } catch (Throwable t) {
            // We won't get here
            getConfigLogger().verbose(getAccountId(), "InApp: Couldn't parse JSON array string from prefs", t);
        }
    }

    //Profile

    private String _stringifyAndCleanScalarProfilePropValue(Object value) {
        String val = CTJsonConverter.toJsonString(value);

        if (val != null) {
            ValidationResult vr = getCoreState().getValidator().cleanMultiValuePropertyValue(val);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                getCoreState().getValidationResultStack().pushValidationResult(vr);
            }

            Object _value = vr.getObject();
            val = (_value != null) ? vr.getObject().toString() : null;
        }

        return val;
    }

    private void _validateAndPushMultiValue(JSONArray currentValues, JSONArray newValues,
            ArrayList<String> originalValues, String key, String command) {

        try {

            // if any of these are null, indicates some problem along the way so abort operation
            if (currentValues == null || newValues == null || originalValues == null || key == null
                    || command == null) {
                return;
            }

            String mergeOperation = command.equals(Constants.COMMAND_REMOVE) ? Validator.REMOVE_VALUES_OPERATION
                    : Validator.ADD_VALUES_OPERATION;

            // merge currentValues and newValues
            ValidationResult vr = getCoreState().getValidator()
                    .mergeMultiValuePropertyForKey(currentValues, newValues, mergeOperation, key);

            // Check for an error
            if (vr.getErrorCode() != 0) {
                getCoreState().getValidationResultStack().pushValidationResult(vr);
            }

            // set the merged local values array
            JSONArray localValues = (JSONArray) vr.getObject();

            // update local profile
            // remove an empty array
            if (localValues == null || localValues.length() <= 0) {
                getCoreState().getLocalDataStore().removeProfileField(key);
            } else {
                // not empty so save to local profile
                getCoreState().getLocalDataStore().setProfileField(key, localValues);
            }

            // push to server
            JSONObject commandObj = new JSONObject();
            commandObj.put(command, new JSONArray(originalValues));

            JSONObject fields = new JSONObject();
            fields.put(key, commandObj);

            getCoreState().getBaseEventQueueManager().pushBasicProfile(fields);

            getConfigLogger().verbose(getAccountId(), "Constructed multi-value profile push: " + fields.toString());

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Error pushing multiValue for key " + key, t);
        }
    }

    //Lifecycle
    private void activityPaused() {
        setAppForeground(false);
        appLastSeen = System.currentTimeMillis();
        getConfigLogger().verbose(getAccountId(), "App in background");
        final int now = (int) (System.currentTimeMillis() / 1000);
        if (getCoreState().getCoreMetaData().inCurrentSession()) {
            try {
                StorageHelper
                        .putInt(context,
                                StorageHelper.storageKeyWithSuffix(getConfig(), Constants.LAST_SESSION_EPOCH),
                                now);
                getConfigLogger().verbose(getAccountId(), "Updated session time: " + now);
            } catch (Throwable t) {
                getConfigLogger().verbose(getAccountId(), "Failed to update session time time: " + t.getMessage());
            }
        }
    }

    //Lifecycle
    private void activityResumed(Activity activity) {
        getConfigLogger().verbose(getAccountId(), "App in foreground");
        checkTimeoutSession();
        //Anything in this If block will run once per App Launch.
        //Will not run for Apps which disable App Launched event
        if (!getCoreState().getCoreMetaData().isAppLaunchPushed()) {
            pushAppLaunchedEvent();
            fetchFeatureFlags();
            onTokenRefresh();
            getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("HandlingInstallReferrer", new Runnable() {
                @Override
                public void run() {
                    if (!mCoreState.getCoreMetaData().isInstallReferrerDataSent() && getCoreState().getCoreMetaData()
                            .isFirstSession()) {
                        handleInstallReferrerOnFirstInstall();
                    }
                }
            });

            try {
                if (geofenceCallback != null) {
                    geofenceCallback.triggerLocation();
                }
            } catch (IllegalStateException e) {
                getConfigLogger().verbose(getAccountId(), e.getLocalizedMessage());
            } catch (Exception e) {
                getConfigLogger().verbose(getAccountId(), "Failed to trigger location");
            }
        }
        if (!getCoreState().getCoreMetaData().inCurrentSession()) {
            getCoreState().getBaseEventQueueManager().pushInitialEventsAsync();
        }
        checkExistingInAppNotifications(activity);
        checkPendingInAppNotifications(activity);
    }

    private void asyncProfileSwitchUser(final Map<String, Object> profile, final String cacheGuid,
            final String cleverTapID) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("resetProfile", new Runnable() {
            @Override
            public void run() {
                try {
                    getConfigLogger().verbose(getAccountId(), "asyncProfileSwitchUser:[profile " + profile
                            + " with Cached GUID " + ((cacheGuid != null) ? cachedGUID
                            : "NULL" + " and cleverTapID " + cleverTapID));
                    //set optOut to false on the current user to unregister the device token
                    getCoreState().getCoreMetaData().setCurrentUserOptedOut(false);
                    // unregister the device token on the current user
                    getCoreState().getPushProviders().forcePushDeviceToken(false);

                    // try and flush and then reset the queues
                    flushQueueSync(context, EventGroup.REGULAR);
                    flushQueueSync(context, EventGroup.PUSH_NOTIFICATION_VIEWED);
                    clearQueues(context);

                    // clear out the old data
                    getCoreState().getLocalDataStore().changeUser();
                    CoreMetaData.setActivityCount(1);
                    getCoreState().getSessionManager().destroySession();

                    // either force restore the cached GUID or generate a new one
                    if (cacheGuid != null) {
                        getCoreState().getDeviceInfo().forceUpdateDeviceId(cacheGuid);
                        notifyUserProfileInitialized(cacheGuid);
                    } else if (getConfig().getEnableCustomCleverTapId()) {
                        getCoreState().getDeviceInfo().forceUpdateCustomCleverTapID(cleverTapID);
                    } else {
                        getCoreState().getDeviceInfo().forceNewDeviceID();
                    }
                    notifyUserProfileInitialized(getCleverTapID());
                    setCurrentUserOptOutStateFromStorage(); // be sure to call this after the guid is updated
                    forcePushAppLaunchedEvent();
                    if (profile != null) {
                        pushProfile(profile);
                    }
                    getCoreState().getPushProviders().forcePushDeviceToken(true);
                    synchronized (processingUserLoginLock) {
                        processingUserLoginIdentifier = null;
                    }
                    resetInbox();
                    resetFeatureFlags();
                    resetProductConfigs();
                    recordDeviceIDErrors();
                    resetDisplayUnits();
                    getCoreState().getInAppFCManager().changeUser(getCleverTapID());
                } catch (Throwable t) {
                    getConfigLogger().verbose(getAccountId(), "Reset Profile error", t);
                }
            }
        });
    }


    private HttpsURLConnection buildHttpsURLConnection(final String endpoint) {
        // TODO dummy method, remove after complete development
        return null;
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

    // private multi-value handlers and helpers

    private void checkExistingInAppNotifications(Activity activity) {
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (currentlyDisplayingInApp != null && ((System.currentTimeMillis() / 1000) < currentlyDisplayingInApp
                    .getTimeToLive())) {
                Fragment inAppFragment = ((FragmentActivity) activity).getSupportFragmentManager()
                        .getFragment(new Bundle(), currentlyDisplayingInApp.getType());
                if (getCurrentActivity() != null && inAppFragment != null) {
                    FragmentTransaction fragmentTransaction = ((FragmentActivity) activity)
                            .getSupportFragmentManager()
                            .beginTransaction();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("inApp", currentlyDisplayingInApp);
                    bundle.putParcelable("config", getConfig());
                    inAppFragment.setArguments(bundle);
                    fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                    fragmentTransaction.add(android.R.id.content, inAppFragment, currentlyDisplayingInApp.getType());
                    Logger.v(getConfig().getAccountId(),
                            "calling InAppFragment " + currentlyDisplayingInApp.getCampaignId());
                    fragmentTransaction.commit();
                }
            }
        }
    }

    private void checkPendingInAppNotifications(Activity activity) {
        final boolean canShow = canShowInAppOnActivity();
        if (canShow) {
            if (pendingInappRunnable != null) {
                getConfigLogger().verbose(getAccountId(), "Found a pending inapp runnable. Scheduling it");
                getCoreState().getMainLooperHandler().postDelayed(pendingInappRunnable, 200);
                pendingInappRunnable = null;
            } else {
                showNotificationIfAvailable(context);
            }
        } else {
            Logger.d("In-app notifications will not be shown for this activity ("
                    + (activity != null ? activity.getLocalClassName() : "") + ")");
        }
    }

    // SessionManager/session management
    private void checkTimeoutSession() {
        if (appLastSeen <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - appLastSeen) > Constants.SESSION_LENGTH_MINS * 60 * 1000) {
            getConfigLogger().verbose(getAccountId(), "Session Timed Out");
            getCoreState().getSessionManager().destroySession();
            setCurrentActivity(null);
        }
    }

    //InApp
    private void displayNotification(final CTInAppNotification inAppNotification) {

        if (Looper.myLooper() != Looper.getMainLooper()) {
            getCoreState().getMainLooperHandler().post(new Runnable() {
                @Override
                public void run() {
                    displayNotification(inAppNotification);
                }
            });
            return;
        }

        if (getCoreState().getInAppFCManager() != null) {
            if (!getCoreState().getInAppFCManager().canShow(inAppNotification)) {
                getConfigLogger().verbose(getAccountId(),
                        "InApp has been rejected by FC, not showing " + inAppNotification.getCampaignId());
                showInAppNotificationIfAny();
                return;
            }

            getCoreState().getInAppFCManager().didShow(context, inAppNotification);
        } else {
            getConfigLogger().verbose(getAccountId(),
                    "getCoreState().getInAppFCManager() is NULL, not showing " + inAppNotification.getCampaignId());
            return;
        }

        final InAppNotificationListener listener = getInAppNotificationListener();

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
            getConfigLogger().verbose(getAccountId(),
                    "Application has decided to not show this in-app notification: " + inAppNotification
                            .getCampaignId());
            showInAppNotificationIfAny();
            return;
        }
        showInApp(context, inAppNotification, getCoreState().getConfig());

    }

    //Event

    private void flushQueueAsync(final Context context, final EventGroup pushNotificationViewed) {

        // TODO dummy method, remove after complete development
    }

    //Profile

    private void flushQueueSync(final Context context, final EventGroup regular) {
        // TODO dummy method, remove after complete development
    }

    //Event
    private void forcePushAppLaunchedEvent() {
        getCoreState().getCoreMetaData().setAppLaunchPushed(false);
        pushAppLaunchedEvent();
    }

    //Push

    private CleverTapInstanceConfig getConfig() {
        return getCoreState().getConfig();
    }

    private Logger getConfigLogger() {
        return getConfig().getLogger();
    }

    //gives delay frequency based on region
    //randomly adds delay to 1s delay in case of non-EU regions
    private int getDelayFrequency() {
        // TODO dummy method, remove after complete development
        return 0;
    }

    private String getEndpoint(final boolean defaultToHandshakeURL, final EventGroup eventGroup) {
        // TODO dummy method, remove after complete development
        return null;
    }

    private int getFirstRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(context, getCoreState().getConfig(), Constants.KEY_FIRST_TS, 0);
    }

    private String getGraphUserPropertySafely(JSONObject graphUser, String key, String def) {
        try {
            String prop = (String) graphUser.get(key);
            if (prop != null) {
                return prop;
            } else {
                return def;
            }
        } catch (Throwable t) {
            return def;
        }
    }


    private int getLastRequestTimestamp() {
        return StorageHelper.getIntFromPrefs(context, getCoreState().getConfig(), Constants.KEY_LAST_TS, 0);
    }


    private void handleInstallReferrerOnFirstInstall() {
        getConfigLogger().verbose(getAccountId(), "Starting to handle install referrer");
        try {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(context).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerServiceDisconnected() {
                    if (!mCoreState.getCoreMetaData().isInstallReferrerDataSent()) {
                        handleInstallReferrerOnFirstInstall();
                    }
                }

                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            // Connection established.
                            ReferrerDetails response = null;
                            try {
                                response = referrerClient.getInstallReferrer();
                                String referrerUrl = response.getInstallReferrer();
                                mCoreState.getCoreMetaData()
                                        .setReferrerClickTime(response.getReferrerClickTimestampSeconds());
                                mCoreState.getCoreMetaData()
                                        .setAppInstallTime(response.getInstallBeginTimestampSeconds());
                                pushInstallReferrer(referrerUrl);
                                mCoreState.getCoreMetaData().setInstallReferrerDataSent(true);
                                getConfigLogger().debug(getAccountId(),
                                        "Install Referrer data set [Referrer URL-" + referrerUrl + "]");
                            } catch (RemoteException e) {
                                getConfigLogger().debug(getAccountId(),
                                        "Remote exception caused by Google Play Install Referrer library - " + e
                                                .getMessage());
                                referrerClient.endConnection();
                                mCoreState.getCoreMetaData().setInstallReferrerDataSent(false);
                            }
                            referrerClient.endConnection();
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            // API not available on the current Play Store app.
                            getConfigLogger().debug(getAccountId(),
                                    "Install Referrer data not set, API not supported by Play Store on device");
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            // Connection couldn't be established.
                            getConfigLogger().debug(getAccountId(),
                                    "Install Referrer data not set, connection to Play Store unavailable");
                            break;
                    }
                }
            });
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(),
                    "Google Play Install Referrer's InstallReferrerClient Class not found - " + t
                            .getLocalizedMessage()
                            + " \n Please add implementation 'com.android.installreferrer:installreferrer:2.1' to your build.gradle");
        }
    }


    /**
     * This method handles send Test flow for Display Units
     *
     * @param extras - bundled data of notification payload
     */
    private void handleSendTestForDisplayUnits(Bundle extras) {
        try {
            String pushJsonPayload = extras.getString(Constants.DISPLAY_UNIT_PREVIEW_PUSH_PAYLOAD_KEY);
            Logger.v("Received Display Unit via push payload: " + pushJsonPayload);
            JSONObject r = new JSONObject();
            JSONArray displayUnits = new JSONArray();
            r.put(Constants.DISPLAY_UNIT_JSON_RESPONSE_KEY, displayUnits);
            JSONObject testPushObject = new JSONObject(pushJsonPayload);
            displayUnits.put(testPushObject);
            processDisplayUnitsResponse(r);
        } catch (Throwable t) {
            Logger.v("Failed to process Display Unit from push notification payload", t);
        }
    }

    private void initFeatureFlags(boolean fromPlayServices) {
        Logger.v("Initializing Feature Flags with device Id = " + getCleverTapID());

        if (getConfig().isAnalyticsOnly()) {
            getConfigLogger().debug(getConfig().getAccountId(), "Feature Flag is not enabled for this instance");
            return;
        }

        if (mCoreState.getCtFeatureFlagsController() == null) {
            mCoreState.setCtFeatureFlagsController(new CTFeatureFlagsController(context, getCleverTapID(),
                    getCoreState().getConfig(), this));
            getConfigLogger().verbose(getConfig().getAccountId(), "Feature Flags initialized");
        }

        if (fromPlayServices && !mCoreState.getCtFeatureFlagsController().isInitialized()) {
            mCoreState.getCtFeatureFlagsController().setGuidAndInit(getCleverTapID());
        }
    }


    //Networking
    private String insertHeader(Context context, JSONArray arr) {

    }

    private boolean isAppLaunchReportingDisabled() {
        return getCoreState().getConfig().isDisableAppLaunchedEvent();
    }

    private boolean isErrorDeviceId() {
        return getCoreState().getDeviceInfo().isErrorDeviceId();
    }

    //Session

    private boolean isProcessUserLoginWithIdentifier(String identifier) {
        synchronized (processingUserLoginLock) {
            return processingUserLoginIdentifier != null && processingUserLoginIdentifier.equals(identifier);
        }
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

    //Util

    //Run manifest validation in async
    private void manifestAsyncValidation() {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("Manifest Validation", new Runnable() {
            @Override
            public void run() {
                ManifestValidator
                        .validate(context, getCoreState().getDeviceInfo(), getCoreState().getPushProviders());
            }
        });
    }


    //Profile
    private void notifyUserProfileInitialized(String deviceID) {
        deviceID = (deviceID != null) ? deviceID : getCleverTapID();

        if (deviceID == null) {
            return;
        }

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

    private void notifyUserProfileInitialized() {
        notifyUserProfileInitialized(getCoreState().getDeviceInfo().getDeviceID());
    }


    //Push
    private void onTokenRefresh() {
        getCoreState().getPushProviders().refreshAllTokens();
    }

    private String optOutKey() {
        String guid = getCleverTapID();
        if (guid == null) {
            return null;
        }
        return "OptOut:" + guid;
    }


    private Date parseTimeToDate(String time) {

        final String inputFormat = "HH:mm";
        SimpleDateFormat inputParser = new SimpleDateFormat(inputFormat, Locale.US);
        try {
            return inputParser.parse(time);
        } catch (java.text.ParseException e) {
            return new Date(0);
        }
    }

    //InApp
    private void prepareNotificationForDisplay(final JSONObject jsonObject) {
        getConfigLogger().debug(getAccountId(), "Preparing In-App for display: " + jsonObject.toString());
        runOnNotificationQueue(new NotificationPrepareRunnable(this, jsonObject));
    }

    /**
     * Stores silent push notification in DB for smooth working of Push Amplification
     * Background Job Service and also stores wzrk_pid to the DB to avoid duplication of Push
     * Notifications from Push Amplification.
     *
     * @param extras - Bundle
     */
    private void processCustomPushNotification(final Bundle extras) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("customHandlePushAmplification", new Runnable() {
            @Override
            public void run() {
                String notifMessage = extras.getString(Constants.NOTIF_MSG);
                notifMessage = (notifMessage != null) ? notifMessage : "";
                if (notifMessage.isEmpty()) {
                    //silent notification
                    getConfigLogger().verbose(getAccountId(), "Push notification message is empty, not rendering");
                    getCoreState().getDatabaseManager().loadDBAdapter(context).storeUninstallTimestamp();
                    String pingFreq = extras.getString("pf", "");
                    if (!TextUtils.isEmpty(pingFreq)) {
                        updatePingFrequencyIfNeeded(context, Integer.parseInt(pingFreq));
                    }
                } else {
                    String wzrk_pid = extras.getString(Constants.WZRK_PUSH_ID);
                    String ttl = extras.getString(Constants.WZRK_TIME_TO_LIVE,
                            (System.currentTimeMillis() + Constants.DEFAULT_PUSH_TTL) / 1000 + "");
                    long wzrk_ttl = Long.parseLong(ttl);
                    DBAdapter dbAdapter = getCoreState().getDatabaseManager().loadDBAdapter(context);
                    getConfigLogger().verbose("Storing Push Notification..." + wzrk_pid + " - with ttl - " + ttl);
                    dbAdapter.storePushNotificationId(wzrk_pid, wzrk_ttl);
                }
            }
        });
    }

    private void pushAmazonRegistrationId(String token, boolean register) {
        getCoreState().getPushProviders().handleToken(token, PushType.ADM, register);
    }

    //Event

    //Event
    private void pushAppLaunchedEvent() {
        if (isAppLaunchReportingDisabled()) {
            getCoreState().getCoreMetaData().setAppLaunchPushed(true);
            getConfigLogger().debug(getAccountId(), "App Launched Events disabled in the Android Manifest file");
            return;
        }
        if (getCoreState().getCoreMetaData().isAppLaunchPushed()) {
            getConfigLogger()
                    .verbose(getAccountId(), "App Launched has already been triggered. Will not trigger it ");
            return;
        } else {
            getConfigLogger().verbose(getAccountId(), "Firing App Launched event");
        }
        getCoreState().getCoreMetaData().setAppLaunchPushed(true);
        JSONObject event = new JSONObject();
        try {
            event.put("evtName", Constants.APP_LAUNCHED_EVENT);
            event.put("evtData", getAppLaunchedFields());
        } catch (Throwable t) {
            // We won't get here
        }
        getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
    }

    private synchronized void pushDeepLink(Uri uri, boolean install) {
        if (uri == null) {
            return;
        }

        try {
            JSONObject referrer = UriHelper.getUrchinFromUri(uri);
            if (referrer.has("us")) {
                getCoreState().getCoreMetaData().setSource(referrer.get("us").toString());
            }
            if (referrer.has("um")) {
                getCoreState().getCoreMetaData().setMedium(referrer.get("um").toString());
            }
            if (referrer.has("uc")) {
                getCoreState().getCoreMetaData().setCampaign(referrer.get("uc").toString());
            }

            referrer.put("referrer", uri.toString());
            if (install) {
                referrer.put("install", true);
            }
            recordPageEventWithExtras(referrer);

        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Failed to push deep link", t);
        }
    }


    private Future<?> raiseEventForGeofences(String eventName, JSONObject geofenceProperties) {

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

            getCoreState().getCoreMetaData().setLocationFromUser(location);

            future = getCoreState().getBaseEventQueueManager().queueEvent(context, event, Constants.RAISED_EVENT);
        } catch (JSONException e) {
            getConfigLogger().debug(getAccountId(), Constants.LOG_TAG_GEOFENCES +
                    "JSON Exception when raising GeoFence event "
                    + eventName + " - " + e.getLocalizedMessage());
        }

        return future;
    }

    private void recordDeviceIDErrors() {
        for (ValidationResult validationResult : getCoreState().getDeviceInfo().getValidationResults()) {
            getCoreState().getValidationResultStack().pushValidationResult(validationResult);
        }
    }

    //Event
    private void recordPageEventWithExtras(JSONObject extras) {
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
            getCoreState().getBaseEventQueueManager().queueEvent(context, jsonObject, Constants.PAGE_EVENT);
        } catch (Throwable t) {
            // We won't get here
        }
    }


    /**
     * Resets the Display Units in the cache
     */
    private void resetDisplayUnits() {
        if (mCoreState.getCTDisplayUnitController() != null) {
            mCoreState.getCTDisplayUnitController().reset();
        } else {
            getConfigLogger().verbose(getAccountId(),
                    Constants.FEATURE_DISPLAY_UNIT + "Can't reset Display Units, DisplayUnitcontroller is null");
        }
    }

    private void resetFeatureFlags() {
        if (mCoreState.getCtFeatureFlagsController() != null && mCoreState.getCtFeatureFlagsController()
                .isInitialized()) {
            mCoreState.getCtFeatureFlagsController().resetWithGuid(getCleverTapID());
            mCoreState.getCtFeatureFlagsController().fetchFeatureFlags();
        }
    }

    // always call async
    private void resetInbox() {
        synchronized (mCoreState.getCTLockManager().getInboxControllerLock()) {
            mCoreState.setCtInboxController(null);
        }
        mCoreState.initializeInbox();
    }

    private void resetProductConfigs() {
        if (getCoreState().getConfig().isAnalyticsOnly()) {
            getConfigLogger().debug(getConfig().getAccountId(), "Product Config is not enabled for this instance");
            return;
        }
        if (mCoreState.getCtProductConfigController() != null) {
            mCoreState.getCtProductConfigController().resetSettings();
        }
        mCoreState.setCtProductConfigController(new CTProductConfigController(context, getCleverTapID(),
                getCoreState().getConfig(), mCoreState.getBaseEventQueueManager(), mCoreState.getCoreMetaData(),
                mCoreState.getCallbackManager()));
        getConfigLogger().verbose(getConfig().getAccountId(), "Product Config reset");
    }

    private void runInstanceJobWork(final Context context, final JobParameters parameters) {
        getCoreState().getPostAsyncSafelyHandler().postAsyncSafely("runningJobService", new Runnable() {
            @Override
            public void run() {
                if (getCoreState().getPushProviders().isNotificationSupported()) {
                    Logger.v(getAccountId(), "Token is not present, not running the Job");
                    return;
                }

                Calendar now = Calendar.getInstance();

                int hour = now.get(Calendar.HOUR_OF_DAY); // Get hour in 24 hour format
                int minute = now.get(Calendar.MINUTE);

                Date currentTime = parseTimeToDate(hour + ":" + minute);
                Date startTime = parseTimeToDate(Constants.DND_START);
                Date endTime = parseTimeToDate(Constants.DND_STOP);

                if (isTimeBetweenDNDTime(startTime, endTime, currentTime)) {
                    Logger.v(getAccountId(), "Job Service won't run in default DND hours");
                    return;
                }

                long lastTS = getCoreState().getDatabaseManager().loadDBAdapter(context).getLastUninstallTimestamp();

                if (lastTS == 0 || lastTS > System.currentTimeMillis() - 24 * 60 * 60 * 1000) {
                    try {
                        JSONObject eventObject = new JSONObject();
                        eventObject.put("bk", 1);
                        getCoreState().getBaseEventQueueManager()
                                .queueEvent(context, eventObject, Constants.PING_EVENT);

                        if (parameters == null) {
                            int pingFrequency = getPingFrequency(context);
                            AlarmManager alarmManager = (AlarmManager) context
                                    .getSystemService(Context.ALARM_SERVICE);
                            Intent cancelIntent = new Intent(CTBackgroundIntentService.MAIN_ACTION);
                            cancelIntent.setPackage(context.getPackageName());
                            PendingIntent alarmPendingIntent = PendingIntent
                                    .getService(context, getAccountId().hashCode(), cancelIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT);
                            if (alarmManager != null) {
                                alarmManager.cancel(alarmPendingIntent);
                            }
                            Intent alarmIntent = new Intent(CTBackgroundIntentService.MAIN_ACTION);
                            alarmIntent.setPackage(context.getPackageName());
                            PendingIntent alarmServicePendingIntent = PendingIntent
                                    .getService(context, getAccountId().hashCode(), alarmIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT);
                            if (alarmManager != null) {
                                if (pingFrequency != -1) {
                                    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                                            SystemClock.elapsedRealtime() + (pingFrequency
                                                    * Constants.ONE_MIN_IN_MILLIS),
                                            Constants.ONE_MIN_IN_MILLIS * pingFrequency, alarmServicePendingIntent);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Logger.v("Unable to raise background Ping event");
                    }

                }
            }
        });
    }

    // -----------------------------------------------------------------------//
    // ********                        Display Unit LOGIC                *****//
    // -----------------------------------------------------------------------//

    private void setCurrentUserOptOutStateFromStorage() {
        String key = optOutKey();
        if (key == null) {
            getConfigLogger().verbose(getAccountId(),
                    "Unable to set current user OptOut state from storage: storage key is null");
            return;
        }
        boolean storedOptOut = StorageHelper.getBooleanFromPrefs(context, getCoreState().getConfig(), key);
        getCoreState().getCoreMetaData().setCurrentUserOptedOut(storedOptOut);
        getConfigLogger().verbose(getAccountId(),
                "Set current user OptOut state from storage to: " + storedOptOut + " for key: " + key);
    }

    private void setFirstRequestTimestampIfNeeded(Context context, int ts) {
        if (getFirstRequestTimestamp() > 0) {
            return;
        }
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(getConfig(), Constants.KEY_FIRST_TS), ts);
    }

    //Session
    private void setLastRequestTimestamp(Context context, int ts) {
        StorageHelper.putInt(context, StorageHelper.storageKeyWithSuffix(getConfig(), Constants.KEY_LAST_TS), ts);
    }

    //Session
    private void setLastVisitTime() {
        EventDetail ed = getCoreState().getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed == null) {
            lastVisitTime = -1;
        } else {
            lastVisitTime = ed.getLastTime();
        }
    }

    //Util

    // -----------------------------------------------------------------------//
    // ********                        Feature Flags Logic               *****//
    // -----------------------------------------------------------------------//

    // ********                       Feature Flags Public API           *****//

    // ********                        Feature Flags Internal methods        *****//


    private void showInAppNotificationIfAny() {
        if (!getCoreState().getConfig().isAnalyticsOnly()) {
            runOnNotificationQueue(new Runnable() {
                @Override
                public void run() {
                    _showNotificationIfAvailable(context);
                }
            });
        }
    }

    //InApp
    @SuppressWarnings({"unused"})
    private void showNotificationIfAvailable(final Context context) {
        if (!getCoreState().getConfig().isAnalyticsOnly()) {
            runOnNotificationQueue(new Runnable() {
                @Override
                public void run() {
                    _showNotificationIfAvailable(context);
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
            getConfigLogger().debug(getAccountId(),
                    "In-app notifications will not be shown on " + Arrays.toString(inappActivityExclude.toArray()));
        }
    }
    // -----------------------------------------------------------------------//
    // ********                        PRODUCT CONFIG Logic              *****//
    // -----------------------------------------------------------------------//

    // ********                       PRODUCT CONFIG Public API           *****//

    // ********                       PRODUCT CONFIG Internal API           *****//


    /**
     * Returns whether or not the app is in the foreground.
     *
     * @return The foreground status
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isAppForeground() {
        return appForeground;
    }

    /**
     * Use this method to notify CleverTap that the app is in foreground
     *
     * @param appForeground boolean true/false
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public static void setAppForeground(boolean appForeground) {
        CleverTapAPI.appForeground = appForeground;
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

        try {
            for (String accountId : CleverTapAPI.instances.keySet()) {
                CleverTapAPI instance = CleverTapAPI.instances.get(accountId);

                boolean shouldProcess = false;
                if (instance != null) {
                    shouldProcess = (_accountId == null && instance.getCoreState().getConfig().isDefaultInstance())
                            || instance
                            .getAccountId().equals(_accountId);
                }

                if (shouldProcess) {
                    if (notification != null && !notification.isEmpty() && notification
                            .containsKey(Constants.NOTIFICATION_TAG)) {
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
        } catch (Throwable t) {
            Logger.v("Throwable - " + t.getLocalizedMessage());
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
                        showInApp(context, notification, config);
                    }
                });
            } catch (Throwable t) {
                // no-op
            }
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
                    return (instance != null && instance.getCoreState().getConfig().getAccountId().equals(_accountId))
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

    //GEOFENCE APIs

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


    //InApp
    private static void inAppDidDismiss(Context context, CleverTapInstanceConfig config,
            CTInAppNotification inAppNotification) {
        Logger.v(config.getAccountId(), "Running inAppDidDismiss");
        if (currentlyDisplayingInApp != null && (currentlyDisplayingInApp.getCampaignId()
                .equals(inAppNotification.getCampaignId()))) {
            currentlyDisplayingInApp = null;
            checkPendingNotifications(context, config);
        }
    }

    //InApp
    private static void showInApp(Context context, final CTInAppNotification inAppNotification,
            CleverTapInstanceConfig config) {

        Logger.v(config.getAccountId(), "Attempting to show next In-App");

        if (!appForeground) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(), "Not in foreground, queueing this In App");
            return;
        }

        if (currentlyDisplayingInApp != null) {
            pendingNotifications.add(inAppNotification);
            Logger.v(config.getAccountId(), "In App already displaying, queueing this In App");
            return;
        }

        if ((System.currentTimeMillis() / 1000) > inAppNotification.getTimeToLive()) {
            Logger.d("InApp has elapsed its time to live, not showing the InApp");
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
                intent.putExtra("inApp", inAppNotification);
                Bundle configBundle = new Bundle();
                configBundle.putParcelable("config", config);
                intent.putExtra("configBundle", configBundle);
                try {
                    Activity currentActivity = getCurrentActivity();
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
                //noinspection ConstantConditions
                FragmentTransaction fragmentTransaction = ((FragmentActivity) getCurrentActivity())
                        .getSupportFragmentManager()
                        .beginTransaction();
                Bundle bundle = new Bundle();
                bundle.putParcelable("inApp", inAppNotification);
                bundle.putParcelable("config", config);
                inAppFragment.setArguments(bundle);
                fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
                fragmentTransaction.add(android.R.id.content, inAppFragment, inAppNotification.getType());
                Logger.v(config.getAccountId(), "calling InAppFragment " + inAppNotification.getCampaignId());
                fragmentTransaction.commit();

            } catch (ClassCastException e) {
                Logger.v(config.getAccountId(),
                        "Fragment not able to render, please ensure your Activity is an instance of AppCompatActivity"
                                + e.getMessage());
            } catch (Throwable t) {
                Logger.v(config.getAccountId(), "Fragment not able to render", t);
            }
        }
    }


}