package com.clevertap.android.sdk;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.clevertap.android.sdk.Utils.getSCDomain;
import static com.clevertap.android.sdk.Utils.runOnUiThread;
import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM_LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.LOG_TAG;
import static com.clevertap.android.sdk.pushnotification.PushConstants.FCM;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.location.Location;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;
import com.clevertap.android.sdk.cryption.CryptHandler;
import com.clevertap.android.sdk.displayunits.DisplayUnitListener;
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit;
import com.clevertap.android.sdk.events.EventDetail;
import com.clevertap.android.sdk.events.EventGroup;
import com.clevertap.android.sdk.featureFlags.CTFeatureFlagsController;
import com.clevertap.android.sdk.inapp.CTLocalInApp;
import com.clevertap.android.sdk.inapp.callbacks.FetchInAppsCallback;
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext;
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter;
import com.clevertap.android.sdk.inapp.customtemplates.JsonTemplatesProducer;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter;
import com.clevertap.android.sdk.inapp.customtemplates.TemplateProducer;
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager;
import com.clevertap.android.sdk.inapp.data.CtCacheType;
import com.clevertap.android.sdk.inapp.evaluation.EvaluationManager;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoFactory;
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl;
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore;
import com.clevertap.android.sdk.inapp.store.preference.InAppStore;
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry;
import com.clevertap.android.sdk.inbox.CTInboxActivity;
import com.clevertap.android.sdk.inbox.CTInboxMessage;
import com.clevertap.android.sdk.inbox.CTMessageDAO;
import com.clevertap.android.sdk.interfaces.NotificationHandler;
import com.clevertap.android.sdk.interfaces.NotificationRenderedListener;
import com.clevertap.android.sdk.interfaces.OnInitCleverTapIDListener;
import com.clevertap.android.sdk.interfaces.SCDomainListener;
import com.clevertap.android.sdk.network.NetworkManager;
import com.clevertap.android.sdk.product_config.CTProductConfigController;
import com.clevertap.android.sdk.product_config.CTProductConfigListener;
import com.clevertap.android.sdk.pushnotification.CTPushNotificationListener;
import com.clevertap.android.sdk.pushnotification.CoreNotificationRenderer;
import com.clevertap.android.sdk.pushnotification.INotificationRenderer;
import com.clevertap.android.sdk.pushnotification.NotificationInfo;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushType;
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpListener;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.usereventlogs.UserEventLog;
import com.clevertap.android.sdk.utils.Clock;
import com.clevertap.android.sdk.utils.UriHelper;
import com.clevertap.android.sdk.validation.ManifestValidator;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.variables.CTVariableUtils;
import com.clevertap.android.sdk.variables.Var;
import com.clevertap.android.sdk.variables.callbacks.FetchVariablesCallback;
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.messaging.FirebaseMessaging;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONException;
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

    /**
     * Implement to get called back when the device push token is received
     */
    @RestrictTo(Scope.LIBRARY)
    public interface RequestDevicePushTokenListener {

        /**
         * @param token the device token
         * @param type  the token type com.clevertap.android.sdk.PushType (FCM)
         */
        void onDevicePushToken(String token, PushType type);
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

    private static int debugLevel = LogLevel.INFO.intValue();

    static CleverTapInstanceConfig defaultConfig;

    private static HashMap<String, CleverTapAPI> instances;

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static String sdkVersion;  // For Google Play Store/Android Studio analytics

    private static NotificationHandler sNotificationHandler;

    private static NotificationHandler sSignedCallNotificationHandler;

    private static final HashMap<String,NotificationRenderedListener> sNotificationRenderedListenerMap = new HashMap<>();

    private final Context context;

    private CoreState coreState;

    private static Clock clevertapClock = Clock.SYSTEM;

    private WeakReference<InboxMessageButtonListener> inboxMessageButtonListener;

    private WeakReference<InboxMessageListener> inboxMessageListener;

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
     * Once the SDK is initialized with a default instance, subsequent calls to this method will be ignored.
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
     * This method is used to change the credentials of CleverTap account Id, token, proxyDomain and spikyProxyDomain programmatically
     *
     * @param accountID         CleverTap Account Id
     * @param token             CleverTap Account Token
     * @param proxyDomain       CleverTap Proxy Domain
     * @param spikyProxyDomain  CleverTap Spiky Proxy Domain
     * @noinspection unused
     */
    public static void changeCredentials(
            String accountID,
            String token,
            String proxyDomain,
            String spikyProxyDomain
    ) {
        if (defaultConfig != null) {
            Logger.i("CleverTap SDK already initialized with accountID:" + defaultConfig.getAccountId()
                    + ", token:" + defaultConfig.getAccountToken() + ", proxyDomain: " + defaultConfig.getProxyDomain() +
                    " and spikyDomain: " + defaultConfig.getSpikyProxyDomain() +
                    ". Cannot change credentials to accountID: " + accountID +
                    ", token: " + token + ", proxyDomain: " + proxyDomain + "and spikyProxyDomain: " + spikyProxyDomain);
            return;
        }

        ManifestInfo.changeCredentials(accountID, token, proxyDomain, spikyProxyDomain);
    }

    /**
     * This method is used to change the credentials of CleverTap account Id, token, proxyDomain, spikyProxyDomain programmatically
     *
     * @param accountID         CleverTap Account Id
     * @param token             CleverTap Account Token
     * @param proxyDomain       CleverTap Proxy Domain
     * @param spikyProxyDomain  CleverTap Spiky Proxy Domain
     * @param customHandshakeDomain  Custom handshake Domain
     * @noinspection unused
     */
    public static void changeCredentials(
            String accountID,
            String token,
            String proxyDomain,
            String spikyProxyDomain,
            String customHandshakeDomain
    ) {
        if (defaultConfig != null) {
            Logger.i("CleverTap SDK already initialized with accountID:" + defaultConfig.getAccountId()
                    + ", token:" + defaultConfig.getAccountToken() + ", proxyDomain: " + defaultConfig.getProxyDomain() +
                    ", spikyDomain: " + defaultConfig.getSpikyProxyDomain() + ", handshakeDomain: " + defaultConfig.getCustomHandshakeDomain()
                    + ". Cannot change credentials to accountID: " + accountID +
                    ", token: " + token + ", proxyDomain: " + proxyDomain + ", spikyProxyDomain: " + spikyProxyDomain
                    + "and customHandshakeDomain: " + customHandshakeDomain);
            return;
        }

        ManifestInfo.changeCredentials(accountID, token, proxyDomain, spikyProxyDomain, customHandshakeDomain);
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
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
     * in onMessageReceive() of messaging service.
     * </p>
     *
     * @param context        A reference to an Android context
     * @param extras         The {@link Bundle} object received by the broadcast receiver
     * @param notificationId A custom id to build a notification
     */
    @SuppressWarnings({"WeakerAccess"})
    public static void createNotification(final Context context, final Bundle extras, final int notificationId) {
        CleverTapAPI instance = fromBundle(context, extras);
        if (instance != null) {

            CoreState coreState = instance.coreState;
            CleverTapInstanceConfig config = coreState.getConfig();

            try {
                synchronized (coreState.getPushProviders().getPushRenderingLock()) {
                    coreState.getPushProviders().setPushNotificationRenderer(new CoreNotificationRenderer());
                    coreState.getPushProviders()._createNotification(context, extras, notificationId);
                }
            } catch (Throwable t) {
                config.getLogger().debug(config.getAccountId(), "Failed to process createNotification()", t);
            }
        }
    }

    public static @Nullable
    CleverTapAPI getGlobalInstance(Context context, String _accountId) {
        return fromAccountId(context, _accountId);
    }

    /**
     * Pass Push Notification Payload to CleverTap for smooth functioning of Pull Notifications
     *
     * @param context - Application Context
     * @param extras  - Bundle received via FCM/Pull Notifications
     */
    @SuppressWarnings("unused")
    public static void processPushNotification(Context context, Bundle extras) {
        CleverTapAPI instance = fromBundle(context, extras);
        if (instance != null) {
            instance.coreState.getPushProviders().processCustomPushNotification(extras);
        }
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     * <p/>
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     *
     *  <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: Starting from core v5.1.0, this method runs on the caller's thread. Make sure to call it
     *      in onMessageReceive() of messaging service.
     *</p>
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("creatingNotificationChannel", () -> {
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("createNotificationChannel", () -> {
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("creatingNotificationChannel", () -> {
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("creatingNotificationChannelGroup", () -> {
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("deletingNotificationChannel", () -> {
                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager == null) {
                        return null;
                    }
                    notificationManager.deleteNotificationChannel(channelId);
                    instance.getConfigLogger().info(instance.getAccountId(),
                            "Notification channel " + channelId + " has been deleted");
                    return null;
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
                Task<Void> task = instance.getCoreState().getExecutors().postAsyncSafelyTask();
                task.execute("deletingNotificationChannelGroup", () -> {

                    NotificationManager notificationManager = (NotificationManager) context
                            .getSystemService(NOTIFICATION_SERVICE);
                    if (notificationManager == null) {
                        return null;
                    }
                    notificationManager.deleteNotificationChannelGroup(groupId);
                    instance.getConfigLogger().info(instance.getAccountId(),
                            "Notification channel group " + groupId + " has been deleted");

                    return null;
                });
            }
        } catch (Throwable t) {
            instance.getConfigLogger()
                    .verbose(instance.getAccountId(), "Failure deleting Notification Channel Group", t);
        }
    }

    /*
    This method was generating the token using Custom FCM Sender ID. The token was overriden from onNewToken
    in the presence of Custom FCM Sender ID present in the AndroidManifest.xml.
    Deprecated now as we can directly call tokenRefresh method from onNewToken implementation
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @Deprecated
    public static void fcmTokenRefresh(Context context, String token) {

        for (CleverTapAPI instance : getAvailableInstances(context)) {
            if (instance == null || instance.getCoreState().getConfig().isAnalyticsOnly()) {
                Logger.d("Instance is Analytics Only not processing device token");
                continue;
            }
            instance.getCoreState().getPushProviders().doTokenRefresh(token, PushConstants.FCM);
        }
    }

    /**
     * Returns the log level set for CleverTapAPI
     *
     * @return The {@link LogLevel} int value
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
    public static CleverTapAPI getDefaultInstance(
            Context context,
            String cleverTapID
    ) {
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

    private static CleverTapAPI fromAccountId(final Context context, final String _accountId) {
        if (instances == null) {
            return createInstanceIfAvailable(context, _accountId);
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
                return instance;
            }
        }

        return null;// failed to get instance
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
    @RestrictTo(Scope.LIBRARY)
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
    public static CleverTapAPI instanceWithConfig(Context context, @NonNull CleverTapInstanceConfig config) {
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
    public static CleverTapAPI instanceWithConfig(
            Context context,
            @NonNull CleverTapInstanceConfig config,
            String cleverTapID
    ) {
        //noinspection ConstantValue
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
        } else if (instance.getConfig().getEnableCustomCleverTapId() && Utils
                .validateCTID(cleverTapID) && instance.isErrorDeviceId()) {
            instance.coreState.getLoginController().asyncProfileSwitchUser(null, null, cleverTapID);
        }
        Logger.v(config.getAccountId() + ":async_deviceID", "CleverTapAPI instance = " + instance);
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
            CleverTapAPI.createInstanceIfAvailable(
                    activity.getApplicationContext(),
                    null,
                    cleverTapID
            );
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
            int initialAppEnteredForegroundTime = clevertapClock.currentTimeSecondsInt();
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
    private static CleverTapAPI fromBundle(final Context context, final Bundle extras) {
        String _accountId = extras.getString(Constants.WZRK_ACCT_ID_KEY);
        return fromAccountId(context, _accountId);
    }

    private static boolean isDevelopmentMode(@NonNull Context context) {
        return 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE);
    }

    @RestrictTo(Scope.LIBRARY)
    public static void runJobWork(Context context) {
        if (instances == null) {
            CleverTapAPI instance = CleverTapAPI.getDefaultInstance(context);
            if (instance != null) {
                if (instance.getConfig().isBackgroundSync()) {
                    instance.coreState.getPushProviders().runPushAmpWork(context);
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
            instance.coreState.getPushProviders().runPushAmpWork(context);
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public static void tokenRefresh(Context context, String token, PushType pushType) {
        for (CleverTapAPI instance : getAvailableInstances(context)) {
            instance.coreState.getPushProviders().doTokenRefresh(token, pushType);
        }
    }

    /**
     * Register {@link com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate CustomTemplates} through a
     * {@link TemplateProducer}. See {@link com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.Builder
     * CustomTemplate.Builder}. Templates must be registered before the {@link CleverTapAPI} instance, that would use
     * them, is created. A common place for this initialization is in {@link Application#onCreate()}. If your
     * application uses multiple {@link CleverTapAPI} instances, use the {@link CleverTapInstanceConfig} within the
     * TemplateProducer to differentiate which templates should be registered to which {@link CleverTapAPI}
     * instances.This method can be called multiple times with different TemplateProducers, however all of the
     * produced templates must have unique names.
     * <br/><br/>
     * Example usage:
     * <br/>
     * Java:
     * <pre>
     * CleverTapAPI.registerCustomInAppTemplates(ctConfig -> CustomTemplatesExtKt.templatesSet(
     *                 new CustomTemplate.TemplateBuilder()
     *                         .name("template")
     *                         .presenter()
     *                         .stringArgument("string", "Text")
     *                         .build()));
     * </pre>
     * Kotlin:
     * <pre>
     * CleverTapAPI.registerCustomInAppTemplates {
     *     setOf(
     *         template {
     *             name("template")
     *             presenter()
     *             stringArgument("string", "Text")
     *         })
     * }
     * </pre>
     * @param producer The {@link TemplateProducer} that would create a set of templates
     * @noinspection unused
     */
    public static synchronized void registerCustomInAppTemplates(TemplateProducer producer) {
        TemplatesManager.register(producer);
    }

    /**
     * Register {@link com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate CustomTemplates} through a
     * json definition. Templates must be registered before the {@link CleverTapAPI} instance, that would use
     * them, is created. A common place for this initialization is in {@link Application#onCreate()}. If your
     * application uses multiple {@link CleverTapAPI} instances, extend {@link JsonTemplatesProducer}
     * with definitions for each instance and register using {@link #registerCustomInAppTemplates(TemplateProducer)}.
     * Use the {@link CleverTapInstanceConfig} in {@link JsonTemplatesProducer#defineTemplates} to control
     * for which instance the templates should be registered.
     * </br>
     * This method can be called multiple times with different json definitions and presenters,
     * however all of the templates must have unique names.
     *
     * @param templatesJson A string with the json definitions of templates. See
     *                      {@link JsonTemplatesProducer} for the json format.
     * @param templatesPresenter A presenter for all templates in the json definitions.
     *                           Required if there is at least one template with type "template".
     * @param functionsPresenter A presenter for all functions in the json definitions.
     *                           Required if there is at least one template with type "function".
     */
    public static synchronized void registerCustomInAppTemplates
    (@NonNull String templatesJson,
     @Nullable TemplatePresenter templatesPresenter,
     @Nullable FunctionPresenter functionsPresenter) {
        TemplatesManager.register(new JsonTemplatesProducer(templatesJson, templatesPresenter, functionsPresenter));
    }

    /**
     * Retrieve a {@link CustomTemplateContext} for a template that is currently displaying. If the provided template
     * name is not of a currently active template this method returns <code>null</code>.
     * @noinspection unused
     */
    @Nullable
    public CustomTemplateContext getActiveContextForTemplate(@NonNull String templateName) {
        if (coreState == null || coreState.getTemplatesManager() == null) {
            return null;
        }
        return coreState.getTemplatesManager().getActiveContextForTemplate(templateName);
    }

    /**
     * Sync all currently registered templates (through {@link #registerCustomInAppTemplates(TemplateProducer)} to the
     * backend. Use this method to transfer template definitions from the SDK to the CT dashboard. This method can
     * only be called from a debug build of the application and the current logged-in user must be marked as a
     * test profile through the web dashboard.
     * @noinspection unused
     */
    public void syncRegisteredInAppTemplates() {
        if (!isDevelopmentMode()) {
            getConfigLogger().debug("CustomTemplates",
                    "Your app is NOT in development mode, templates will not be synced");
            return;
        }

        if (coreState == null) {
            getConfigLogger().debug("CustomTemplates", "coreState is null, templates cannot be synced");
            return;
        }

        if (coreState.getNetworkManager() == null) {
            getConfigLogger().debug("CustomTemplates", "networkManager is null, templates cannot be synced");
            return;
        }

        if (coreState.getTemplatesManager() == null) {
            getConfigLogger().debug("CustomTemplates", "templateManager is null, templates cannot be synced");
            return;
        }

        TemplatesManager templatesManager = coreState.getTemplatesManager();
        NetworkManager networkManager = coreState.getNetworkManager();

        getCleverTapID(x -> {
            // getCleverTapID is executed on the main thread
            Task<Void> task = coreState.getExecutors().postAsyncSafelyTask();
            task.execute("DefineTemplates", () -> {
                networkManager.defineTemplates(templatesManager.getAllRegisteredTemplates());
                return null;
            });
        });
    }

    /**
     * Checks whether notification permission is granted or denied for Android 13 and above devices.
     * @return boolean Returns true/false based on whether permission is granted or denied.
     */
    public boolean isPushPermissionGranted() {
        return coreState.getInAppController().isPushPermissionGranted();
    }

    /**
     * Calls the push primer flow for Android 13 and above devices.
     * @param jsonObject JSONObject - Accepts jsonObject created by {@link CTLocalInApp} object
     */
    public void promptPushPrimer(JSONObject jsonObject) {
        coreState.getInAppController().promptPushPrimer(jsonObject);
    }

    /**
     * Calls directly hard permission dialog, if push primer is not required.
     * @param showFallbackSettings - boolean - If `showFallbackSettings` is true then we show a alert
     *                             dialog which routes to app's notification settings page.
     */
    public void promptForPushPermission(boolean showFallbackSettings) {
        coreState.getInAppController().promptPermission(showFallbackSettings);
    }

    // Initialize
    private CleverTapAPI(
            final Context context,
            final CleverTapInstanceConfig config,
            String cleverTapID
    ) {
        this(context, config, CleverTapFactory.getCoreState(context, config, cleverTapID), Clock.SYSTEM);
    }

    // Internal constructor for testing
    CleverTapAPI(
            final Context context,
            final CleverTapInstanceConfig config,
            final CoreState coreState,
            final Clock clock
    ) {
        this.context = context;
        this.coreState = coreState;
        CleverTapAPI.clevertapClock = clock;
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "CoreState is set");
        asyncStartup();
        Logger.i("CleverTap SDK initialized with accountId: " + config.getAccountId() + " accountToken: " + config
                .getAccountToken() + " accountRegion: " + config.getAccountRegion());
    }

    private void asyncStartup() {
        Task<Void> task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("CleverTapAPI#initializeDeviceInfo", () -> {
            if (getConfig().isDefaultInstance()) {
                ManifestValidator.validate(context, coreState.getDeviceInfo(), coreState.getPushProviders());
            }
            return null;
        });

        int now = clevertapClock.currentTimeSecondsInt();
        if (now - CoreMetaData.getInitialAppEnteredForegroundTime() > 5) {
            coreState.getConfig().setCreatedPostAppLaunch();
        }

        task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("setStatesAsync", () -> {
            coreState.getSessionManager().setLastVisitTime();
            coreState.getSessionManager().setUserLastVisitTs();
            coreState.getDeviceInfo().setDeviceNetworkInfoReportingFromStorage();
            coreState.getDeviceInfo().setCurrentUserOptOutStateFromStorage();
            coreState.getDeviceInfo().setSystemEventsAllowedStateFromStorage();
            return null;
        });

        task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("saveConfigtoSharedPrefs", () -> {
            String configJson = getConfig().toJSONString();
            if (configJson == null) {
                Logger.v("Unable to save config to SharedPrefs, config Json is null");
                return null;
            }
            StorageHelper.putString(context, StorageHelper.storageKeyWithSuffix(getConfig(), "instance"), configJson);
            return null;
        });
        task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("recordDeviceIDErrors", () -> {
            if (coreState.getDeviceInfo().getDeviceID() != null) {
                coreState.getLoginController().recordDeviceIDErrors();
            }
            return null;
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
     * Deletes multiple {@link CTInboxMessage} objects for given list of messageIDs
     *
     * @param messageIDs {@link ArrayList} with String values - list of messageIDs of {@link CTInboxMessage} public object of inbox message
     */
    public void deleteInboxMessagesForIDs(final ArrayList<String> messageIDs){
        if (coreState.getControllerManager().getCTInboxController() != null) {
            coreState.getControllerManager().getCTInboxController().deleteInboxMessagesForIDs(messageIDs);
        } else {
            getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
        }
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
     * Suspends the display of InApp Notifications and discards any new InApp Notifications to be shown
     * after this method is called.
     * The InApp Notifications will be displayed only once resumeInAppNotifications() is called.
     * @noinspection unused
     */
    public void discardInAppNotifications() {
        if (!coreState.getConfig().isAnalyticsOnly()) {
            getConfigLogger().debug(getAccountId(), "Discarding InApp Notifications...");
            getConfigLogger().debug(getAccountId(),
                    "Please Note - InApp Notifications will be dropped till resumeInAppNotifications() is not called again");
            coreState.getInAppController().discardInApps();
        } else {
            getConfigLogger().debug(getAccountId(),
                    "CleverTap instance is set for Analytics only! Cannot discard InApp Notifications.");
        }
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
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * @return object of {@link CTFeatureFlagsController}
     * Handler to get the feature flag values
     */
    @Deprecated
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

    /**
     * This method is used to set the SCDomain callback.
     * <p>
     * Register to handle the domain related events from CleverTap
     * This is to be used only by clevertap-signedcall-sdk
     *
     * @param scDomainListener - the {@link SCDomainListener} instance
     * @noinspection unused
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @WorkerThread
    public void setSCDomainListener(SCDomainListener scDomainListener) {
        coreState.getCallbackManager().setSCDomainListener(scDomainListener);

        if (coreState.getNetworkManager() != null) {
            NetworkManager networkManager = coreState.getNetworkManager();
            String domain = networkManager.getDomain(EventGroup.REGULAR);
            if (domain != null) {
                scDomainListener.onSCDomainAvailable(getSCDomain(domain));
            }
        }
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
        Logger.d("CleverTapAPI:getAllInboxMessages: called" );
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

    //Debug

    /**
     * Returns the CTPushAmpListener object
     *
     * @return The {@link CTPushAmpListener} object
     * @noinspection unused
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
     * @noinspection unused
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

    /**
     * Returns a unique CleverTap identifier suitable for use with install attribution providers.
     *
     * @return The attribution identifier currently being used to identify this user.
     *
     * <p><br><span style="background:#ffcc99" >&#9888; this method may take a long time to return,
     * so you should not call it from the application main thread</span></p>
     *
     * <p style="color:red;font-size: 25px;margin-left:10px">&#9760;</p>
     * <b><span style="color:#4d2e00;background:#ffcc99" >Deprecated as of version <code>4.2.0</code> and will be
     * removed in future versions</span>
     * </b><br>
     * <code>Use {@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener)} instead</code>
     */
    @SuppressWarnings("unused")
    @WorkerThread
    @Deprecated
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
     * This method is used to decrement the given value
     * Number should be in positive range
     *
     * @param key   String
     * @param value Number
     */
    @SuppressWarnings("unused")
    public void decrementValue(final String key, final Number value) {
        coreState.getAnalyticsManager().decrementValue(key, value);
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
     *
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserEventLogCount(String)} instead.
     * getUserEventLogCount() provides user-specific event counts.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
    public int getCount(String event) {
        EventDetail eventDetail = coreState.getLocalDataStore().getEventDetail(event);
        if (eventDetail != null) {
            return eventDetail.getCount();
        }

        return -1;
    }

    /**
     * Retrieves the count of logged events for a specific event name associated with the current
     * user/{@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) CleverTap ID}.
     * This operation involves a database query and should be called from a background thread.
     * <br>
     * Example usage:
     * <br>
     * <code>
     * // Call from background thread <br>
     * int itemSelectedCount = getUserEventLogCount("item_selected")
     * </code>
     *
     * @param eventName Name of the event to get the count for (e.g., "navigation_clicked", "item_selected")
     * @return The number of times the specified event has occurred for current user, or -1 if there was an error
     */
    @WorkerThread
    public int getUserEventLogCount(String eventName) {
        if (!getConfig().isPersonalizationEnabled()) {
            return -1;
        }
        return coreState.getLocalDataStore().readUserEventLogCount(eventName);
    }

    /**
     * Returns an EventDetail object for the particular event passed. EventDetail consists of event name, count, first
     * time
     * and last time timestamp of the event.
     *
     * @param event The event name for which you want the Event details
     * @return The {@link EventDetail} object
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserEventLog(String)} instead.
     * getUserEventLog() provides user-specific event log.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
    public EventDetail getDetails(String event) {
        return coreState.getLocalDataStore().getEventDetail(event);
    }

    /**
     * Retrieves user-specific event log associated with the current user/
     * {@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) CleverTap ID}.
     * This operation involves a database query and should be called from a background thread.
     * <br>
     * Example usage:
     * <br>
     * <code>
     * // Call from background thread <br>
     * UserEventLog log = getUserEventLog("navigation_clicked") <br>
     * long firstOccurrence = log.firstTs
     * </code>
     *
     * @param eventName Name of the event to get the log for (e.g., "navigation_clicked", "item_selected")
     * @return {@link UserEventLog} or null if the event log does not exist or there was an error
     */
    @WorkerThread
    public UserEventLog getUserEventLog(String eventName) {
        if (!getConfig().isPersonalizationEnabled()) {
            return null;
        }
        return coreState.getLocalDataStore().readUserEventLog(eventName);
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
     * This method is used to set the RequestDevicePushTokenListener object
     *
     * @param requestTokenListener The {@link RequestDevicePushTokenListener} object
     */
    @SuppressWarnings("unused")
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void setRequestDevicePushTokenListener(RequestDevicePushTokenListener requestTokenListener) {
        try {
            Logger.v(LOG_TAG, FCM_LOG_TAG + "Requesting FCM token using googleservices.json");
            FirebaseMessaging
                    .getInstance()
                    .getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<String> task) {
                            if (!task.isSuccessful()) {
                                Logger.v(LOG_TAG,
                                        FCM_LOG_TAG + "FCM token using googleservices.json failed",
                                        task.getException());
                                requestTokenListener.onDevicePushToken(null, FCM);
                                return;
                            }
                            String token = task.getResult() != null ? task.getResult() : null;
                            Logger.v(LOG_TAG,
                                    FCM_LOG_TAG + "FCM token using googleservices.json - " + token);
                            requestTokenListener.onDevicePushToken(token, FCM);
                        }
                    });

        } catch (Throwable t) {
            Logger.v(LOG_TAG, FCM_LOG_TAG + "Error requesting FCM token", t);
            requestTokenListener.onDevicePushToken(null, FCM);
        }
    }

    //Util

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
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserEventLog(String)} instead.
     * It provides user-specific event log with first occurrence timestamp.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
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
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserEventLogHistory()} instead.
     * getUserEventLogHistory() provides user-specific event logs.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
    public Map<String, EventDetail> getHistory() {
        return coreState.getLocalDataStore().getEventHistory(context);
    }

    /**
     * Retrieves history of all event logs associated with the current user/{@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) CleverTap ID} in the ascending order of lastTs.
     * This operation involves a database query and should be called from a background thread.
     * <br>
     * Example usage:
     * <br>
     * <code>
     * // Call from background thread <br>
     * Map&lt;String, UserEventLog&gt; history = getUserEventLogHistory()
     * </code>
     *
     * @return Map of event name to {@link UserEventLog} for all events by current user, or empty map if there was an error
     */
    @WorkerThread
    public Map<String, UserEventLog> getUserEventLogHistory() {
        Map<String, UserEventLog> history = new LinkedHashMap<>();
        if (!getConfig().isPersonalizationEnabled()) {
            return history;
        }
        List<UserEventLog> logs = coreState.getLocalDataStore().readUserEventLogs();
        for (UserEventLog log : logs) {
            history.put(log.getEventName(), log);
        }
        return history;
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
     * This method unregisters the given instance of the PushPermissionResponseListener if
     * previously registered.
     * <p>
     * Use this method to stop observing the push permission result.
     *
     * @param pushPermissionResponseListener An {@link PushPermissionResponseListener} object
     */
    @SuppressWarnings({"unused"})
    public void unregisterPushPermissionNotificationResponseListener(PushPermissionResponseListener
                                                                           pushPermissionResponseListener) {
        coreState.getCallbackManager().
                unregisterPushPermissionResponseListener(pushPermissionResponseListener);
    }

    /**
     * This method registers the PushPermissionNotificationResponseListener.
     * <p>
     * Call this method only from the onCreate() of the activity/fragment and unregister the
     * listener from the onDestroy() method using the
     * {@link #unregisterPushPermissionNotificationResponseListener(PushPermissionResponseListener)}
     *
     * @param pushPermissionResponseListener An {@link PushPermissionResponseListener} object
     */
    @SuppressWarnings({"unused"})
    public void registerPushPermissionNotificationResponseListener(PushPermissionResponseListener
                                                                          pushPermissionResponseListener) {
        coreState.getCallbackManager().
                registerPushPermissionResponseListener(pushPermissionResponseListener);
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
        Logger.d("CleverTapAPI:getInboxMessageForId() called with: messageId = [" + messageId + "]");
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
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserEventLog(String)} instead.
     * It provides user-specific event log with last occurrence timestamp.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
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
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserLastVisitTs()} instead.
     * getUserLastVisitTs() provides user-specific last visit timestamp.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
    public int getPreviousVisitTime() {
        return coreState.getSessionManager().getLastVisitTime();
    }

    /**
     * Retrieves timestamp of last visit by current user/{@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) CleverTap ID}.
     * <br>
     * Example usage:
     * <br>
     * <code>
     * long lastVisitTs = getUserLastVisitTs()
     * </code>
     *
     * @return Timestamp of last visit by current user, or -1 if there was an error
     */
    public long getUserLastVisitTs() {
        if (!getConfig().isPersonalizationEnabled()) {
            return -1;
        }
        return coreState.getSessionManager().getUserLastVisitTs();
    }

    /**
     * Return the user profile property value for the specified key.
     * Date related property values are returned as number of seconds since January 1, 1970, 00:00:00 GMT
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
     * @noinspection unused
     */
    public String getPushToken(@NonNull PushType pushType) {
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
     * @noinspection unused
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

        int now = clevertapClock.currentTimeSecondsInt();
        return now - currentSession;
    }

    /**
     * Returns the total number of times the app has been launched
     *
     * @return Total number of app launches in int
     * @deprecated since <code>v7.1.0</code>. Use {@link #getUserAppLaunchCount()} instead.
     * getUserAppLaunchCount() provides user-specific app launch count.
     */
    @SuppressWarnings({"unused"})
    @Deprecated(since = "7.1.0")
    public int getTotalVisits() {
        EventDetail ed = coreState.getLocalDataStore().getEventDetail(Constants.APP_LAUNCHED_EVENT);
        if (ed != null) {
            return ed.getCount();
        }

        return 0;
    }

    /**
     * Retrieves number of times app launched by current user/{@link CleverTapAPI#getCleverTapID(OnInitCleverTapIDListener) CleverTap ID}.
     * This operation involves a database query and should be called from a background thread.
     * <br>
     * Example usage:
     * <br>
     * <code>
     * // Call from background thread <br>
     * int launchCount = getUserAppLaunchCount()
     * </code>
     *
     * @return Number of times app launched by current user, or -1 if there was an error
     */
    @WorkerThread
    public int getUserAppLaunchCount() {
        if (!getConfig().isPersonalizationEnabled()) {
            return -1;
        }
        return coreState.getLocalDataStore().readUserEventLogCount(Constants.APP_LAUNCHED_EVENT);
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

    /**
     * Marks multiple {@link CTInboxMessage} objects as read for given list of messageIDs
     *
     * @param messageIDs {@link ArrayList} with String values - list of messageIDs of {@link CTInboxMessage} public object of inbox message
     */
    public void markReadInboxMessagesForIDs(final ArrayList<String> messageIDs){
        if (coreState.getControllerManager().getCTInboxController() != null) {
            coreState.getControllerManager().getCTInboxController().markReadInboxMessagesForIDs(messageIDs);
        } else {
            getConfigLogger().debug(getAccountId(), "Notification Inbox not initialized");
        }
    }

    @Override
    public void messageDidClick(CTInboxActivity ctInboxActivity, int contentPageIndex, CTInboxMessage inboxMessage, Bundle data, HashMap<String, String> keyValue, int buttonIndex) {

        coreState.getAnalyticsManager().pushInboxMessageStateEvent(true, inboxMessage, data);

        Logger.v("clicked inbox notification.");
        //notify the onInboxItemClicked callback if the listener is set.
        if (inboxMessageListener != null && inboxMessageListener.get() != null) {
            inboxMessageListener.get().onInboxItemClicked(inboxMessage, contentPageIndex, buttonIndex);
        }

        if (keyValue != null && !keyValue.isEmpty()) {
            Logger.v("clicked button of an inbox notification.");
            //notify the onInboxButtonClick callback if the listener is set.
            if (inboxMessageButtonListener != null && inboxMessageButtonListener.get() != null) {
                inboxMessageButtonListener.get().onInboxButtonClick(keyValue);
            }
        }
    }

    //Session

    @Override
    public void messageDidShow(CTInboxActivity ctInboxActivity, final CTInboxMessage inboxMessage, final Bundle data) {
        Task<Void> task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("handleMessageDidShow", () -> {
            Logger.d("CleverTapAPI:messageDidShow() called  in async with: messageId = [" + inboxMessage.getMessageId() + "]");

            CTInboxMessage message = getInboxMessageForId(inboxMessage.getMessageId());
            if (!message.isRead()) {
                markReadInboxMessage(inboxMessage);
                coreState.getAnalyticsManager().pushInboxMessageStateEvent(false, inboxMessage, data);
            }
            return null;
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
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * The handle for product config functionalities(fetch/activate etc.)
     *
     * @return - the instance of {@link CTProductConfigController}
     */
    @SuppressWarnings("WeakerAccess")
    @Deprecated
    public CTProductConfigController productConfig() {
        if (getConfig().isAnalyticsOnly()) {
            getConfig().getLogger().debug(getAccountId(),
                    "Product config is not supported with analytics only configuration");
        }
        return coreState.getCtProductConfigController(context);
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
        if (eventName == null || eventName.trim().isEmpty()) {
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
        coreState.getPushProviders().handleToken(fcmId, PushConstants.FCM, register);
    }

    /**
     * Pushes a Signed Call event to CleverTap with a set of attribute pairs.
     *
     * @param eventName    The name of the event
     * @param eventProperties The {@link JSONObject} object that contains the
     *                           event properties regarding Signed Call event
     */
    @SuppressWarnings("unused")
    public Future<?> pushSignedCallEvent(String eventName, JSONObject eventProperties) {
        return coreState.getAnalyticsManager()
                .raiseEventForSignedCall(eventName, eventProperties);
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
     * Pushes the Notification Clicked event for App Inbox to CleverTap.
     *
     * @param messageId String - messageId of {@link CTInboxMessage}
     */
    @SuppressWarnings("unused")
    public void pushInboxNotificationClickedEvent(String messageId) {
        Logger.v( "CleverTapAPI:pushInboxNotificationClickedEvent() called with: messageId = [" +messageId + "]");

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
        Logger.v( "CleverTapAPI:pushInboxNotificationViewedEvent() called with: messageId = [" +messageId + "]");
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
     * Remove the user profile property value specified by key from the user profile. Alternatively this method
     * can also be used to remove PII data (for eg. Email,Name,Phone), locally from database and shared prefs
     *
     * @param key String
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    public void removeValueForKey(final String key) {
        coreState.getAnalyticsManager().removeValueForKey(key);
    }

    /**
     * Resumes display of InApp Notifications.
     * <p>
     * If suspendInAppNotifications() was called previously, calling this method will instantly show
     * all queued InApp Notifications and also resume InApp Notifications on events raised after this
     * method is called.
     * <p>
     * If discardInAppNotifications() was called previously, calling this method will only resume
     * InApp Notifications on events raised after this method is called.
     * @noinspection unused
     */
    public void resumeInAppNotifications() {
        if (!coreState.getConfig().isAnalyticsOnly()) {
            getConfigLogger().debug(getAccountId(), "Resuming InApp Notifications...");
            coreState.getInAppController().resumeInApps();
        } else {
            getConfigLogger().debug(getAccountId(),
                    "CleverTap instance is set for Analytics only! Cannot resume InApp Notifications.");
        }
    }

    public static NotificationHandler getNotificationHandler() {
        return sNotificationHandler;
    }

    public static NotificationHandler getSignedCallNotificationHandler() {
        return sSignedCallNotificationHandler;
    }

    /**
     * This method is used to increment the given value.
     * <p>
     * Number should be in positive range.
     *
     * @param key   String
     * @param value Number
     */
    @SuppressWarnings("unused")
    public void incrementValue(final String key, final Number value) {
        coreState.getAnalyticsManager().incrementValue(key, value);
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     *
     * This method is used to set the CTFeatureFlagsListener
     * Register to receive feature flag callbacks
     *
     * @param featureFlagsListener The {@link CTFeatureFlagsListener} object
     */
    @SuppressWarnings("unused")
    @Deprecated
    public void setCTFeatureFlagsListener(CTFeatureFlagsListener featureFlagsListener) {
        coreState.getCallbackManager().setFeatureFlagListener(featureFlagsListener);
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method has been deprecated since v5.0.0 and will be removed in the future versions of this SDK.
     * </p>
     * This method is used to set the product config listener
     * Register to receive callbacks
     *
     * @param listener The {@link CTProductConfigListener} instance
     */
    @SuppressWarnings("unused")
    @Deprecated
    public void setCTProductConfigListener(CTProductConfigListener listener) {
        coreState.getCallbackManager().setProductConfigListener(listener);
    }

    /**
     * Sets the listener to get the list of currently running Display Campaigns via callback
     *
     * @param listener- {@link DisplayUnitListener}
     */
    public void setDisplayUnitListener(DisplayUnitListener listener) {
        coreState.getCallbackManager().setDisplayUnitListener(listener);
    }

    //Listener

    @SuppressWarnings("unused")
    public void setInAppNotificationButtonListener(InAppNotificationButtonListener listener) {
        coreState.getCallbackManager().setInAppNotificationButtonListener(listener);
    }

    @SuppressWarnings("unused")
    public void setInboxMessageButtonListener(InboxMessageButtonListener listener) {
        this.inboxMessageButtonListener = new WeakReference<>(listener);
    }

    @SuppressWarnings("unused")
    public void setCTInboxMessageListener(InboxMessageListener listener){
        this.inboxMessageListener = new WeakReference<>(listener);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public static void addNotificationRenderedListener(String id, final NotificationRenderedListener notificationRenderedListener) {
       sNotificationRenderedListenerMap.put(id, notificationRenderedListener);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public static NotificationRenderedListener getNotificationRenderedListener(String id) {
       return sNotificationRenderedListenerMap.get(id);
    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public static NotificationRenderedListener removeNotificationRenderedListener(String id) {
       return sNotificationRenderedListenerMap.remove(id);
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
        // this is legacy behavior, on opt out we should not allow system events
        // The second flag is set to support legacy method with correctness out of the box.
        setOptOut(userOptOut, !userOptOut);
    }

    /**
     * Use this method to manage the user's consent for event and profile tracking.
     * You must call this method separately for each active user profile (e.g., when switching user profiles using
     * onUserLogin).
     *
     * This method supports the following consent management scenarios:
     *
     * <ol>
     *   <li><b>Complete Opt-Out (userOptOut = true, allowSystemEvents = false):</b>
     *       No events (neither custom nor system) will be saved remotely or locally for the current user.
     *       This provides the highest level of privacy.</li>
     *   <li><b>Full Opt-In (userOptOut = false, allowSystemEvents = true):</b>
     *       All events (custom and system) will be tracked and saved. This is the default behavior
     *       if no opt-out preferences are set.</li>
     *   <li><b>Partial Opt-In (userOptOut = true, allowSystemEvents = true):</b>
     *       Only system-level CleverTap events (e.g., app launch, notification viewed) will be tracked.
     *       Custom events raised by the client application will be filtered out.</li>
     * </ol>
     *
     * Note: The case where `userOptOut = false` and `allowSystemEvents = false` is invalid.
     * If this combination is provided, the SDK will default to the "Full Opt-In" behavior
     * (userOptOut = false, allowSystemEvents = true).
     *
     * To re-enable full tracking after any form of opt-out, call this method with `userOptOut = false` and `allowSystemEvents = true`.
     *
     * @param userOptOut        boolean: Set to `true` to opt the user out of custom event tracking.
     *                          Set to `false` to opt the user into custom event tracking.
     * @param allowSystemEvents boolean: Set to `true` to allow system-level CleverTap events.
     *                          Set to `false` to disallow system-level CleverTap events (only if `userOptOut` is also `true`).
     */
    public void setOptOut(final boolean userOptOut, final boolean allowSystemEvents) {
        Task<Void> task = coreState.getExecutors().postAsyncSafelyTask();
        task.execute("setOptOut", () -> {
            // Handle case with userOptOut = false, allowSystemEvents = false
            boolean resolvedAllowSystemEvents = !userOptOut || allowSystemEvents;

            // generate the data for a profile push to alert the server to the optOut state change
            HashMap<String, Object> optOutMap = new HashMap<>();
            optOutMap.put(Constants.CLEVERTAP_OPTOUT, userOptOut);
            optOutMap.put(Constants.CLEVERTAP_ALLOW_SYSTEM_EVENTS, resolvedAllowSystemEvents);

            coreState.getCoreMetaData().setCurrentUserOptedOut(false);
            coreState.getAnalyticsManager().pushProfile(optOutMap);
            coreState.getCoreMetaData().setCurrentUserOptedOut(userOptOut);
            coreState.getCoreMetaData().setEnabledSystemEvents(resolvedAllowSystemEvents);
            // persist the new optOut state
            coreState.getDeviceInfo().saveOptOutState(userOptOut);
            coreState.getDeviceInfo().saveAllowedSystemEventsState(resolvedAllowSystemEvents);
            return null;
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
     * Dismisses the App Inbox Activity if already opened
     */
    public void dismissAppInbox() {
        try {
            Activity appInboxActivity = coreState.getCoreMetaData().getAppInboxActivity();
            if (appInboxActivity == null) {
                throw new IllegalStateException("AppInboxActivity reference not found");
            }
            if (!appInboxActivity.isFinishing()) {
                getConfigLogger().verbose(getAccountId(), "Finishing the App Inbox");
                appInboxActivity.finish();
            }
        } catch (Throwable t) {
            getConfigLogger().verbose(getAccountId(), "Can't dismiss AppInbox, please ensure to call this method after the usage of " +
                    "cleverTapApiInstance.showAppInbox(). \n" + t);
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

    /**
     * Suspends display of InApp Notifications.
     * The InApp Notifications are queued once this method is called
     * and will be displayed once resumeInAppNotifications() is called.
     * @noinspection unused
     */
    public void suspendInAppNotifications() {
        if (!coreState.getConfig().isAnalyticsOnly()) {
            getConfigLogger().debug(getAccountId(), "Suspending InApp Notifications...");
            getConfigLogger().debug(getAccountId(),
                    "Please Note - InApp Notifications will be suspended till resumeInAppNotifications() is not called again");
            coreState.getInAppController().suspendInApps();
        } else {
            getConfigLogger().debug(getAccountId(),
                    "CleverTap instance is set for Analytics only! Cannot suspend InApp Notifications.");
        }
    }

    /** @noinspection unused*/
    @RestrictTo(Scope.LIBRARY_GROUP)
    public int getCustomSdkVersion(String customSdkName) {
        return coreState.getCoreMetaData().getCustomSdkVersion(customSdkName);
    }

    /** @noinspection unused*/
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void setCustomSdkVersion(String customSdkName,int customSdkVersion) {
        coreState.getCoreMetaData().setCustomSdkVersion(customSdkName,customSdkVersion);
    }

    //To be called from DeviceInfo AdID GUID generation
    void deviceIDCreated(String deviceId) {

        String accountId = coreState.getConfig().getAccountId();

        if (coreState.getControllerManager() == null) {
            getConfigLogger().verbose(accountId + ":async_deviceID",
                    "ControllerManager not set yet! Returning from deviceIDCreated()");
            return;
        }

        StoreRegistry storeRegistry = coreState.getStoreRegistry();
        CryptHandler cryptHandler = coreState.getCryptHandler();
        StoreProvider storeProvider = StoreProvider.getInstance();
        EvaluationManager evaluationManager = coreState.getEvaluationManager();

        // Inflate the local profile here as deviceId is required
        coreState.getLocalDataStore().inflateLocalProfileAsync(context);

        // must move initStores task to async executor due to addChangeUserCallback synchronization
        Task<Void> task = coreState.getExecutors().ioTask();
        task.execute("initStores", () -> {
            if (storeRegistry.getInAppStore() == null) {
                InAppStore inAppStore = storeProvider.provideInAppStore(context, cryptHandler, deviceId,
                        accountId);
                storeRegistry.setInAppStore(inAppStore);
                evaluationManager.loadSuppressedCSAndEvaluatedSSInAppsIds();
                // can cause ANR if called from main thread
                coreState.getCallbackManager().addChangeUserCallback(inAppStore);
            }
            if (storeRegistry.getImpressionStore() == null) {
                ImpressionStore impStore = storeProvider.provideImpressionStore(context, deviceId,
                        accountId);
                storeRegistry.setImpressionStore(impStore);
                // can cause ANR if called from main thread
                coreState.getCallbackManager().addChangeUserCallback(impStore);
            }
            return null;
        });


        /*
          Reinitialising InAppFCManager with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        if (coreState.getControllerManager().getInAppFCManager() == null) {
            getConfigLogger().verbose(accountId + ":async_deviceID",
                    "Initializing InAppFC after Device ID Created = " + deviceId);
            coreState.getControllerManager()
                    .setInAppFCManager(new InAppFCManager(context, coreState.getConfig(), deviceId,
                            coreState.getStoreRegistry(), coreState.getImpressionManager(),
                            coreState.getExecutors(), clevertapClock));
        }

        //todo : replace with variables
        /*
          Reinitialising product config & Feature Flag controllers with device id, if it's null
          during first initialisation from CleverTapFactory.getCoreState()
         */
        CTFeatureFlagsController ctFeatureFlagsController = coreState.getControllerManager()
                .getCTFeatureFlagsController();

        if (ctFeatureFlagsController != null && TextUtils.isEmpty(ctFeatureFlagsController.getGuid())) {
            getConfigLogger().verbose(accountId + ":async_deviceID",
                    "Initializing Feature Flags after Device ID Created = " + deviceId);
            ctFeatureFlagsController.setGuidAndInit(deviceId);
        }
        //todo: replace with variables
        CTProductConfigController ctProductConfigController = coreState.getControllerManager()
                .getCTProductConfigController();

        if (ctProductConfigController != null && TextUtils
                .isEmpty(ctProductConfigController.getSettings().getGuid())) {
            getConfigLogger().verbose(accountId + ":async_deviceID",
                    "Initializing Product Config after Device ID Created = " + deviceId);
            ctProductConfigController.setGuidAndInit(deviceId);
        }
        getConfigLogger().verbose(accountId + ":async_deviceID",
                "Got device id from DeviceInfo, notifying user profile initialized to SyncListener");
        coreState.getCallbackManager().notifyUserProfileInitialized(deviceId);
        coreState.getCallbackManager().notifyCleverTapIDChanged(deviceId);
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
                                + notification + ", dropping duplicate.");
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
                if (instance != null) {
                    instance.coreState.getActivityLifeCycleManager()
                            .onActivityCreated(notification, deepLink, _accountId);
                }
            }
        } catch (Throwable t) {
            Logger.v("Throwable - " + t.getLocalizedMessage());
        }
    }

    private static CleverTapAPI createInstanceIfAvailable(Context context, String _accountId) {
        return createInstanceIfAvailable(context, _accountId, null);
    }

    private static @Nullable
    CleverTapAPI createInstanceIfAvailable(
            Context context,
            String _accountId,
            String cleverTapID
    ) {
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

    @RestrictTo(Scope.LIBRARY_GROUP)
    public static ArrayList<CleverTapAPI> getAvailableInstances(Context context) {
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
        String accountToken = manifest.getAccountToken();
        String accountRegion = manifest.getAccountRegion();
        String proxyDomain = manifest.getProxyDomain();
        String spikyProxyDomain = manifest.getSpikeyProxyDomain();
        String handshakeDomain = manifest.getHandshakeDomain();
        if (accountId == null || accountToken == null) {
            Logger.i("Account ID or Account token is missing from AndroidManifest.xml, unable to create default instance");
            return null;
        }
        if (accountRegion == null) {
            Logger.i("Account Region not specified in the AndroidManifest - using default region");
        }

        // todo : pass manifest info here
        CleverTapInstanceConfig defaultInstanceConfig = CleverTapInstanceConfig.createDefaultInstance(context, accountId, accountToken, accountRegion);

        // todo : check if these re-assignments are needed, already added in manifest parsing
        if (proxyDomain != null && !proxyDomain.trim().isEmpty()) {
            defaultInstanceConfig.setProxyDomain(proxyDomain);
        }
        if (spikyProxyDomain != null && !spikyProxyDomain.trim().isEmpty()) {
            defaultInstanceConfig.setSpikyProxyDomain(spikyProxyDomain);
        }
        if (handshakeDomain != null && !handshakeDomain.trim().isEmpty()) {
            defaultInstanceConfig.setCustomHandshakeDomain(handshakeDomain);
        }
        return defaultInstanceConfig;
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

    public static void setNotificationHandler(NotificationHandler notificationHandler) {
        sNotificationHandler = notificationHandler;
    }

    /** @noinspection unused*/
    public static void setSignedCallNotificationHandler(NotificationHandler notificationHandler) {
        sSignedCallNotificationHandler = notificationHandler;
    }

    /**
     * Subscribe to receive a unique identifier by which CleverTap identifies this user. The listener is called on the
     * main thread and it is invoked each time the id changes until the listener is removed through
     * {@link #removeCleverTapIDListener(OnInitCleverTapIDListener)}
     *
     * @param onInitCleverTapIDListener non-null callback to retrieve identifier on main thread.
     */
    public void getCleverTapID(@NonNull final OnInitCleverTapIDListener onInitCleverTapIDListener) {
        Task<Void> taskDeviceCachedInfo = coreState.getExecutors().ioTask();
        taskDeviceCachedInfo.execute("getCleverTapID", () -> {
            String deviceID = coreState.getDeviceInfo().getDeviceID();
            if (deviceID != null) {
                runOnUiThread(() -> onInitCleverTapIDListener.onInitCleverTapID(deviceID));
            }
            coreState.getCallbackManager().addOnInitCleverTapIDListener(onInitCleverTapIDListener);
            return null;
        });
    }

    /**
     * Remove a previously subscribed OnInitCleverTapIDListener.
     * See {@link #getCleverTapID(OnInitCleverTapIDListener)}
     *
     * @param onInitCleverTapIDListener The same listener instance passed to
     *                                  {@link #getCleverTapID(OnInitCleverTapIDListener)} that should be removed.
     * @noinspection unused
     */
    public void removeCleverTapIDListener(@NonNull final OnInitCleverTapIDListener onInitCleverTapIDListener) {
        if (coreState != null && coreState.getCallbackManager() != null) {
            coreState.getCallbackManager().removeOnInitCleverTapIDListener(onInitCleverTapIDListener);
        }
    }

    //TODO: start synchronizing entire flow from here
    public Future<?> renderPushNotification(@NonNull INotificationRenderer iNotificationRenderer, Context context,
            Bundle extras) {

        CleverTapInstanceConfig config = coreState.getConfig();
        Future<?> future = null;

        try {
            Task<Void> task = coreState.getExecutors().postAsyncSafelyTask();
            future = task.submit("CleverTapAPI#renderPushNotification",
                    () -> {
                        synchronized (coreState.getPushProviders().getPushRenderingLock()) {
                            coreState.getPushProviders().setPushNotificationRenderer(iNotificationRenderer);

                            if (extras != null && extras.containsKey(Constants.PT_NOTIF_ID)) {
                                coreState.getPushProviders()
                                        ._createNotification(context, extras,
                                                extras.getInt(Constants.PT_NOTIF_ID));
                            } else {
                                coreState.getPushProviders()
                                        ._createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID);
                            }
                        }
                        return null;
                    });
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Failed to process renderPushNotification()", t);
        }

        return future;

    }

    @RestrictTo(Scope.LIBRARY_GROUP)
    public void renderPushNotificationOnCallerThread(@NonNull INotificationRenderer iNotificationRenderer, Context context,
            Bundle extras) {

        CleverTapInstanceConfig config = coreState.getConfig();

        try {
            synchronized (coreState.getPushProviders().getPushRenderingLock()) {
                config.getLogger().verbose(config.getAccountId(),
                        "rendering push on caller thread with id = " + Thread.currentThread().getId());
                coreState.getPushProviders().setPushNotificationRenderer(iNotificationRenderer);

                if (extras != null && extras.containsKey(Constants.PT_NOTIF_ID)) {
                    coreState.getPushProviders()
                            ._createNotification(context, extras,
                                    extras.getInt(Constants.PT_NOTIF_ID));
                } else {
                    coreState.getPushProviders()
                            ._createNotification(context, extras, Constants.EMPTY_NOTIFICATION_ID);
                }
            }
        } catch (Throwable t) {
            config.getLogger().debug(config.getAccountId(), "Failed to process renderPushNotification()", t);
        }

    }

    /**
     * Retrieves a notification bitmap with a specified timeout and size constraint.
     *
     * @param context           The context of the application. Must be non null.
     * @param bundle            The {@link Bundle} object received by the push receiver. Must be non null.
     * @param bitmapSrcUrl      The URL of the bitmap to download.
     * @param fallbackToAppIcon Specifies whether to fallback to the app icon if the bitmap is not available.
     * @param timeoutInMillis   The timeout duration for the bitmap download in milliseconds.  Must be in range of 1 - 20000.
     * @param sizeInBytes       The maximum size of the bitmap in bytes. Must be greater than 0.
     * @return The downloaded bitmap or null if it couldn't be downloaded or doesn't exist.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     *      Note: This method must be called on background thread.
     *</p>
     * @noinspection unused
     */
    public static @Nullable Bitmap getNotificationBitmapWithTimeoutAndSize(
            final Context context, final Bundle bundle, String bitmapSrcUrl,
            boolean fallbackToAppIcon, long timeoutInMillis, int sizeInBytes) {

        if (checkNotificationBitmapRequestInvalid(context, bundle, timeoutInMillis)) return null;

        if (sizeInBytes < 1) {
            Logger.v("Given sizeInBytes is less than 1 bytes. Not downloading bitmap!");
            return null;
        }

        CleverTapAPI cleverTapAPI = fromBundle(context, bundle);
        if (cleverTapAPI == null) {
            Logger.v("cleverTapAPI is null. Not downloading bitmap!");
            return null;
        }

        return Utils.getNotificationBitmapWithTimeoutAndSize(bitmapSrcUrl, fallbackToAppIcon, context, cleverTapAPI.getConfig(),
                timeoutInMillis, sizeInBytes).getBitmap();
    }

    private static boolean checkNotificationBitmapRequestInvalid(Context context, Bundle bundle, long timeoutInMillis) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Logger.v("Notification Bitmap Download is not allowed on main thread");
            return true;
        }
        if (context == null) {
            Logger.v("Given Context is null. Not downloading bitmap!");
            return true;
        }
        if (bundle == null) {
            Logger.v("Given Bundle is null. Not downloading bitmap!");
            return true;
        }
        if (timeoutInMillis < 1) {
            Logger.v("Given timeoutInMillis is less than 1 millis. Not downloading bitmap!");
            return true;
        }
        if (timeoutInMillis > 20000) {
            Logger.v("Given timeoutInMillis exceeds 20 secs limit. Not downloading bitmap!");
            return true;
        }
        return false;
    }

    /**
     * Retrieves a notification bitmap with a specified timeout.
     *
     * @param context           The context of the application. Must be non null.
     * @param bundle            The {@link Bundle} object received by the push receiver. Must be non null.
     * @param bitmapSrcUrl      The URL of the bitmap to download.
     * @param fallbackToAppIcon Specifies whether to fallback to the app icon if the bitmap is not available.
     * @param timeoutInMillis   The timeout duration for the bitmap download in milliseconds.  Must be in range of 1 - 20000.
     * @return The downloaded bitmap or null if it couldn't be downloaded or doesn't exist.
     *
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method must be called on background thread.
     * </p>
     * @noinspection unused
     */
    public static @Nullable Bitmap getNotificationBitmapWithTimeout(
            final Context context, final Bundle bundle, String bitmapSrcUrl,
            boolean fallbackToAppIcon, long timeoutInMillis) {

        if (checkNotificationBitmapRequestInvalid(context, bundle, timeoutInMillis)) return null;

        CleverTapAPI cleverTapAPI = fromBundle(context, bundle);
        if (cleverTapAPI == null) {
            Logger.v("cleverTapAPI is null. Not downloading bitmap!");
            return null;
        }

        return Utils.getNotificationBitmapWithTimeout(bitmapSrcUrl, fallbackToAppIcon, context, cleverTapAPI.getConfig(),
                timeoutInMillis).getBitmap();
    }

    /**
     * Check if your app is in development mode. <br>
     * Some functions (like {@link CleverTapAPI#syncVariables()} and {@link CleverTapAPI#syncRegisteredInAppTemplates()})
     * will only work if the app is in development mode and the current user profile is set as a test profile in CT
     * Dashboard.
     *
     * @return <code>true</code> if the app is in development mode, <code>false</code> otherwise.
     */
    public boolean isDevelopmentMode() {
        return context != null && isDevelopmentMode(context);
    }

    /**
     * Defines a new variable. If the default vale is null it won't resolve the type properly. In
     * that case it is better to use the @Variable annotation instead of this method.
     *
     * @param name Name of the variable.
     * @param defaultValue Default value of variable, used when resolving the underlying value type.
     * @param <T> Type of value.
     * @return Returns the Var instance.
     */
    public <T> Var<T> defineVariable(String name, T defaultValue) {
        return Var.define(name, defaultValue,coreState.getCTVariables());
    }

    /**
     * Defines a new file variable. Disclaimer: cannot be used with @Variable annotation.
     *
     * @param name Name of the variable.
     * @return Returns the Var instance.
     */
    public Var<String> defineFileVariable(String name) {
        return Var.define(name, null , CTVariableUtils.FILE, coreState.getCTVariables());
    }

    /**
     * Parses the @Variable annotated fields from a given instance or multiple instances.
     *
     * @param instances Instance or instances to parse.
     */
    public void parseVariables(Object... instances) {
        coreState.getParser().parseVariables(instances);
    }

    /**
     * Parses the @Variable annotated static fields from a given class or multiple classes.
     *
     * @param classes Class object or objects to parse.
     * @noinspection unused
     */
    public void parseVariablesForClasses(Class<?>... classes) {
        coreState.getParser().parseVariablesForClasses(classes);
    }

    /**
     * Get a copy of the current value of a variable or a group.
     *
     * @param name The name of the variable or the group.
     * @return The value of the variable or the group.
     */
    public Object getVariableValue(String name) {
        if (name == null) {
            return null;
        }
        return coreState.getVarCache().getMergedValue(name);
    }

    /**
     * Get an instance of a variable or a group.
     *
     * @param name The name of the variable or the group.
     * @return The instance of the variable or the group, or null if not created yet.
     */
    public <T> Var<T> getVariable(String name) {
        if (name == null) {
            return null;
        }
        return coreState.getVarCache().getVariable(name);
    }

    /**
     * Fetches variable values from server.
     * @noinspection unused
     */
    public void fetchVariables() {
        fetchVariables(null);
    }

    /**
     * Fetches In Apps from server.
     *
     * @param callback Callback instance {@link FetchInAppsCallback} to be invoked when fetching is done.
     */
    public void fetchInApps(FetchInAppsCallback callback) {
        if (coreState.getConfig().isAnalyticsOnly()) {
            return;
        }
        Logger.v(Constants.LOG_TAG_INAPP + " Fetching In Apps...");

        if (callback != null) {
            coreState.getCallbackManager().setFetchInAppsCallback(callback);
        }

        JSONObject event = getFetchRequestAsJson(Constants.FETCH_TYPE_IN_APPS);
        coreState.getAnalyticsManager().sendFetchEvent(event);
    }

    @NonNull
    JSONObject getFetchRequestAsJson(int fetchType) {
        JSONObject event = new JSONObject();
        JSONObject notif = new JSONObject();
        try {
            notif.put(Constants.KEY_T, fetchType);
            event.put(Constants.KEY_EVT_NAME, Constants.WZRK_FETCH);
            event.put(Constants.KEY_EVT_DATA, notif);
        } catch (JSONException e) {
            Logger.v(Constants.CLEVERTAP_LOG_TAG,
                    "Failed while parsing fetch request as json:", e);
        }
        return event;
    }

    /**
     * Fetches variable values from server.
     * Note that SDK keeps only one registered callback, if you call that method again it would
     * override the callback.
     *
     * @param callback Callback instance to be invoked when fetching is done.
     */
    public void fetchVariables(FetchVariablesCallback callback) {
        if (coreState.getConfig().isAnalyticsOnly()) {
            return;
        }
        Logger.v("variables", "Fetching  variables");
        if (callback != null) {
            coreState.getCallbackManager().setFetchVariablesCallback(callback);
        }

        JSONObject event = getFetchRequestAsJson(Constants.FETCH_TYPE_VARIABLES);
        coreState.getAnalyticsManager().sendFetchEvent(event);
    }

    /**
     * Uploads variables to server.
     */
    public void syncVariables() {
        if (isDevelopmentMode()) {
            Logger.v("variables", "syncVariables: waiting for id to be available");
            getCleverTapID(x -> {
                JSONObject js = coreState.getVarCache().getDefineVarsData();
                Logger.v("variables", "syncVariables: sending following vars to server:" + js);
                coreState.getAnalyticsManager().pushDefineVarsEvent(js);
            });
        } else {
            Logger.v("variables", "Your app is NOT in development mode, variables data will not be sent to server");
        }
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values.
     * Will be called each time new values are fetched.
     *
     * @param callback Callback to register.
     */
    public void addVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().addVariablesChangedCallback(callback);
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values. Will be
     * called only once and then removed.
     *
     * @param callback Callback to register.
     */
    public void addOneTimeVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().addOneTimeVariablesChangedCallback(callback);
    }

    /**
     * Removes previously registered callback.
     *
     * @param callback Callback to remove.
     */
    public void removeVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().removeVariablesChangedCallback(callback);
    }

    /**
     * Removes previously registered callback.
     *
     * @param callback Callback to remove.
     */
    public void removeOneTimeVariablesChangedCallback(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().removeOneTimeVariablesChangedHandler(callback);
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values and file
     * downloads are also completed for file type variables (if any) registered with
     * {@link #defineFileVariable}.
     * Will be called each time new values are fetched.
     *
     * @param callback Callback to register.
     */
    public void onVariablesChangedAndNoDownloadsPending(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().onVariablesChangedAndNoDownloadsPending(callback);
    }

    /**
     * Adds a callback to be invoked when variables are initialised with server values and file
     * downloads are also completed for file type variables (if any) registered with
     * {@link #defineFileVariable}.
     * Will be called only once and then removed.
     *
     * @param callback Callback to register.
     */
    public void onceVariablesChangedAndNoDownloadsPending(@NonNull VariablesChangedCallback callback) {
        coreState.getCTVariables().onceVariablesChangedAndNoDownloadsPending(callback);
    }

    /**
     *  Removes all previously registered callbacks.
     * @noinspection unused
     */
    public void removeAllVariablesChangedCallbacks() {
        coreState.getCTVariables().removeAllVariablesChangedCallbacks();
    }

    /**
     *  Removes all previously registered one time callbacks.
     * @noinspection unused
     */
    public void removeAllOneTimeVariablesChangedCallbacks() {
        coreState.getCTVariables().removeAllOneTimeVariablesChangedCallbacks();
    }

    /**
     * Use this method to set a custom locale for the current CleverTap instance
     *
     * @param locale - The custom locale to be set
     */
    @SuppressWarnings({"unused"})
    public void setLocale(String locale) {
        if(TextUtils.isEmpty(locale)) {
            Logger.i("Empty Locale provided for setLocale, not setting it");
            return;
        }
        coreState.getDeviceInfo().setCustomLocale(locale);
    }

    /**
     * Returns the custom locale set for the current CleverTap instance
     *
     * @return The customLocale string value
     */
    @SuppressWarnings({"unused"})
    public String getLocale() {
        return coreState.getDeviceInfo().getCustomLocale();
    }

    /**
     * Deletes all images and gifs which are preloaded for inapps in cs mode
     *
     * @param expiredOnly to clear only assets which will not be needed further for inapps
     */
    @WorkerThread
    public void clearInAppResources(boolean expiredOnly) {

        Logger logger = getConfigLogger();

        StoreRegistry storeRegistry = coreState.getStoreRegistry();
        if (storeRegistry == null) {
            logger.info(
                    "There was a problem clearing resources because instance is not completely initialised, please try again after some time");
            return;
        }

        FileResourcesRepoImpl impl = FileResourcesRepoFactory.createFileResourcesRepo(context, logger, storeRegistry);

        if (expiredOnly) {
            impl.cleanupExpiredResources(CtCacheType.IMAGE);
        } else {
            impl.cleanupAllResources(CtCacheType.IMAGE);
        }
    }

    /**
     * Deletes all types of files which are preloaded for SDK features like custom in-app templates, app functions and
     * variables etc.
     *
     * @param expiredOnly to clear only files which will not be needed further for SDK features like custom in-app
     *                    templates, app functions and variables etc.
     * @noinspection unused
     */
    @WorkerThread
    public void clearFileResources(boolean expiredOnly) {

        Logger logger = getConfigLogger();

        StoreRegistry storeRegistry = coreState.getStoreRegistry();
        if (storeRegistry == null) {
            logger.info(
                    "There was a problem clearing file resources because instance is not completely initialised, please try again after some time");
            return;
        }

        FileResourcesRepoImpl impl = FileResourcesRepoFactory.createFileResourcesRepo(context, logger, storeRegistry);

        if (expiredOnly) {
            impl.cleanupExpiredResources(CtCacheType.FILES);
        } else {
            impl.cleanupAllResources(CtCacheType.FILES);
        }
    }

    /**
     * Sends push registration token for the given push type
     *
     * @param token    The token
     * @param pushType Push Provider type for which the token is registered
     * @param register Boolean indicating whether to register
     *                 or not for receiving push messages from CleverTap.
     *                 Set this to true to receive push messages from CleverTap,
     *                 and false to not receive any messages from CleverTap.
     */
    @SuppressWarnings("unused")
    public void pushRegistrationToken(String token, PushType pushType, boolean register) {
        coreState.getPushProviders().handleToken(token, pushType, register);
    }
}
