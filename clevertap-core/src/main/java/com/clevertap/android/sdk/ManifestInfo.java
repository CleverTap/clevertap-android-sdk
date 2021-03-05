package com.clevertap.android.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.utils.NullConstants;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ManifestInfo {

    private static String accountId;

    private static String accountToken;

    private static String accountRegion;

    private static boolean useADID;

    private static boolean appLaunchedDisabled;

    private static String notificationIcon;

    private static ManifestInfo instance;

    private static String excludedActivities;

    private static boolean sslPinning;

    private static boolean backgroundSync;

    private static boolean useCustomID;

    private static String fcmSenderId;

    private static String packageName;

    private static boolean beta;

    private static String intentServiceName;

    private static String xiaomiAppKey;

    private static String xiaomiAppID;

    private final String[] profileKeys;

    public synchronized static ManifestInfo getInstance(Context context) {
        if (instance == null) {
            instance = new ManifestInfo(context);
        }
        return instance;
    }

    private ManifestInfo(Context context) {
        Bundle metaData = null;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            metaData = ai.metaData;
        } catch (Throwable t) {
            // no-op
        }
        if (metaData == null) {
            metaData = new Bundle();
        }
        if (accountId == null) {
            accountId = metaData.getString(Constants.LABEL_ACCOUNT_ID);
        }
        if (accountToken == null) {
            accountToken = metaData.getString(Constants.LABEL_TOKEN);
        }
        if (accountRegion == null) {
            accountRegion = metaData.getString(Constants.LABEL_REGION);
        }
        notificationIcon = metaData.getString(Constants.LABEL_NOTIFICATION_ICON);
        useADID = "1".equals(metaData.getString(Constants.LABEL_USE_GOOGLE_AD_ID));
        appLaunchedDisabled = "1".equals(metaData.getString(Constants.LABEL_DISABLE_APP_LAUNCH));
        excludedActivities = metaData.getString(Constants.LABEL_INAPP_EXCLUDE);
        sslPinning = "1".equals(metaData.getString(Constants.LABEL_SSL_PINNING));
        backgroundSync = "1".equals(metaData.getString(Constants.LABEL_BACKGROUND_SYNC));
        useCustomID = "1".equals(metaData.getString(Constants.LABEL_CUSTOM_ID));
        fcmSenderId = metaData.getString(Constants.LABEL_FCM_SENDER_ID);
        if (fcmSenderId != null) {
            fcmSenderId = fcmSenderId.replace("id:", "");
        }
        packageName = metaData.getString(Constants.LABEL_PACKAGE_NAME);
        beta = "1".equals(metaData.getString(Constants.LABEL_BETA));
        if (intentServiceName == null) {
            intentServiceName = metaData.getString(Constants.LABEL_INTENT_SERVICE);
        }

        xiaomiAppKey = metaData.getString(Constants.LABEL_XIAOMI_APP_KEY);
        xiaomiAppID = metaData.getString(Constants.LABEL_XIAOMI_APP_ID);

        profileKeys = parseProfileKeys(metaData);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getFCMSenderId() {
        return fcmSenderId;
    }

    public String[] getProfileKeys() {
        return profileKeys;
    }

    public String getXiaomiAppID() {
        return xiaomiAppID;
    }

    public String getXiaomiAppKey() {
        return xiaomiAppKey;
    }

    boolean enableBeta() {
        return beta;
    }

    String getAccountRegion() {
        return accountRegion;
    }

    String getAcountToken() {
        return accountToken;
    }

    public String getExcludedActivities() {
        return excludedActivities;
    }

    public String getIntentServiceName() {
        return intentServiceName;
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    String getPackageName() {
        return packageName;
    }

    boolean isAppLaunchedDisabled() {
        return appLaunchedDisabled;
    }

    boolean isBackgroundSync() {
        return backgroundSync;
    }

    boolean isSSLPinningEnabled() {
        return sslPinning;
    }

    boolean useCustomId() {
        return useCustomID;
    }

    boolean useGoogleAdId() {
        return useADID;
    }

    private String[] parseProfileKeys(final Bundle metaData) {
        String profileKeyString = metaData.getString(Constants.CLEVERTAP_IDENTIFIER);
        return !TextUtils.isEmpty(profileKeyString) ? profileKeyString.split(Constants.SEPARATOR_COMMA)
                : NullConstants.NULL_STRING_ARRAY;
    }

    static void changeCredentials(String id, String token, String region) {
        accountId = id;
        accountToken = token;
        accountRegion = region;
    }

}
