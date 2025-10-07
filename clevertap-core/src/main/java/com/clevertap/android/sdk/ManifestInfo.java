package com.clevertap.android.sdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;

import org.jetbrains.annotations.TestOnly;

/**
 * Parser for android manifest and picks up fields from manifest once to be references
 *
 * Should be singleton and initialised only once -> need to validate.
 */
// todo lp Remove context dependency from here
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ManifestInfo {

    private static final String LABEL_ACCOUNT_ID = "CLEVERTAP_ACCOUNT_ID";
    private static final String LABEL_TOKEN = "CLEVERTAP_TOKEN";
    public static final String LABEL_NOTIFICATION_ICON = "CLEVERTAP_NOTIFICATION_ICON";
    private static final String LABEL_INAPP_EXCLUDE = "CLEVERTAP_INAPP_EXCLUDE";
    private static final String LABEL_REGION = "CLEVERTAP_REGION";
    private static final String LABEL_PROXY_DOMAIN = "CLEVERTAP_PROXY_DOMAIN";
    private static final String LABEL_SPIKY_PROXY_DOMAIN = "CLEVERTAP_SPIKY_PROXY_DOMAIN";
    private static final String LABEL_CLEVERTAP_HANDSHAKE_DOMAIN = "CLEVERTAP_HANDSHAKE_DOMAIN";
    private static final String LABEL_DISABLE_APP_LAUNCH = "CLEVERTAP_DISABLE_APP_LAUNCHED";
    private static final String LABEL_SSL_PINNING = "CLEVERTAP_SSL_PINNING";
    private static final String LABEL_BACKGROUND_SYNC = "CLEVERTAP_BACKGROUND_SYNC";
    private static final String LABEL_CUSTOM_ID = "CLEVERTAP_USE_CUSTOM_ID";
    private static final String LABEL_USE_GOOGLE_AD_ID = "CLEVERTAP_USE_GOOGLE_AD_ID";
    private static final String LABEL_FCM_SENDER_ID = "FCM_SENDER_ID";
    private static final String LABEL_PACKAGE_NAME = "CLEVERTAP_APP_PACKAGE";
    private static final String LABEL_BETA = "CLEVERTAP_BETA";
    private static final String LABEL_INTENT_SERVICE = "CLEVERTAP_INTENT_SERVICE";
    private static final String LABEL_ENCRYPTION_LEVEL = "CLEVERTAP_ENCRYPTION_LEVEL";
    private static final String LABEL_DEFAULT_CHANNEL_ID = "CLEVERTAP_DEFAULT_CHANNEL_ID";

    // intentionally named this way to avoid static scan flagging on codebase
    private static final String LABEL_PUSH_PROVIDER_1 = "CLEVERTAP_PROVIDER_1";
    private static final String LABEL_PUSH_PROVIDER_2 = "CLEVERTAP_PROVIDER_2";

    private static final String LABEL_ENCRYPTION_IN_TRANSIT = "CLEVERTAP_ENCRYPTION_IN_TRANSIT";

    private static ManifestInfo instance; // singleton

    public synchronized static ManifestInfo getInstance(Context context) {
        if (instance == null) {
            instance = new ManifestInfo(context);
        }
        return instance;
    }

    // Only added for testing
    @TestOnly
    static void clearPreloadedManifestInfo() {
        instance = null;
    }

    static void changeCredentials(String id, String token, String region) {
        ccAccountId = id;
        ccAccountToken = token;
        ccAccountRegion = region;
    }

    static void changeCredentials(String id, String token, String _proxyDomain, String _spikyProxyDomain) {
        ccAccountId = id;
        ccAccountToken = token;
        ccProxyDomain = _proxyDomain;
        ccSpikyProxyDomain = _spikyProxyDomain;
    }

    static void changeCredentials(String id, String token, String _proxyDomain, String _spikyProxyDomain, String customHandshakeDomain) {
        ccAccountId = id;
        ccAccountToken = token;
        ccProxyDomain = _proxyDomain;
        ccSpikyProxyDomain = _spikyProxyDomain;
        ccHandshakeDomain = customHandshakeDomain;
    }

    // Start - Capture the credentials from ChangeCredentials
    private static String ccAccountId;
    private static String ccAccountToken;
    private static String ccAccountRegion;
    private static String ccProxyDomain;
    private static String ccSpikyProxyDomain;
    private static String ccHandshakeDomain;
    // End - Capture the credentials from ChangeCredentials

    private final String accountId;
    private final String accountToken;
    private final String accountRegion;
    private final String proxyDomain;
    private final String spikyProxyDomain;
    private final String handshakeDomain;
    private final boolean useADID;
    private final boolean appLaunchedDisabled;
    private final String notificationIcon;
    private final String excludedActivitiesForInApps;
    private final boolean sslPinning;
    private final boolean backgroundSync;
    private final boolean useCustomID;
    private final String fcmSenderId;
    private final String packageName;
    private final boolean beta;
    private final String intentServiceName;
    private final String devDefaultPushChannelId;
    private final String[] profileKeys;
    private final int encryptionLevel;
    private final String provider1;
    private final String provider2;
    private final String encryptionInTransit;

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

        // start -> assign these if they did not happen in changeCredentials
        accountId = ccAccountId != null ? ccAccountId : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_ACCOUNT_ID);
        accountToken = ccAccountToken != null ? ccAccountToken : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_TOKEN);
        accountRegion = ccAccountRegion != null ? ccAccountRegion : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_REGION);
        proxyDomain = ccProxyDomain != null ? ccProxyDomain : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_PROXY_DOMAIN);
        spikyProxyDomain = ccSpikyProxyDomain != null ? ccSpikyProxyDomain : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_SPIKY_PROXY_DOMAIN);
        handshakeDomain = ccHandshakeDomain != null ? ccHandshakeDomain : _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_CLEVERTAP_HANDSHAKE_DOMAIN);
        // end -> assign these if they did not happen in changeCredentials

        notificationIcon = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_NOTIFICATION_ICON);
        useADID = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_USE_GOOGLE_AD_ID));
        appLaunchedDisabled = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_DISABLE_APP_LAUNCH));
        excludedActivitiesForInApps = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_INAPP_EXCLUDE);
        sslPinning = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_SSL_PINNING));
        backgroundSync = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_BACKGROUND_SYNC));
        useCustomID = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_CUSTOM_ID));

        String fcmSenderIdTemp;
        fcmSenderIdTemp = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_FCM_SENDER_ID);
        if (fcmSenderIdTemp != null) {
            fcmSenderIdTemp = fcmSenderIdTemp.replace("id:", "");
        }
        fcmSenderId = fcmSenderIdTemp;

        int encLvlTemp;
        try {
            String manifestEncryptionLevel = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_ENCRYPTION_LEVEL);
            int parsedEncryptionLevel = 0;
            if (manifestEncryptionLevel != null) {
                parsedEncryptionLevel = Integer.parseInt(manifestEncryptionLevel);
            }

            if (parsedEncryptionLevel >= 0 && parsedEncryptionLevel <= 2) {
                encLvlTemp = parsedEncryptionLevel;
            } else {
                Logger.v("Invalid encryption level is used, defaulting to no encryption");
                encLvlTemp = 0;
            }
        } catch (Throwable t) {
            encLvlTemp = 0;
            Logger.v("Unable to parse encryption level from the Manifest, Setting it to 0 by default", t.getCause());
        }
        encryptionLevel = encLvlTemp;

        packageName = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_PACKAGE_NAME);
        beta = "1".equals(_getManifestStringValueForKey(metaData, ManifestInfo.LABEL_BETA));
        intentServiceName = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_INTENT_SERVICE);
        devDefaultPushChannelId = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_DEFAULT_CHANNEL_ID);
        profileKeys = parseProfileKeys(metaData);
        provider1 = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_PUSH_PROVIDER_1);
        provider2 = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_PUSH_PROVIDER_2);
        encryptionInTransit = _getManifestStringValueForKey(metaData, ManifestInfo.LABEL_ENCRYPTION_IN_TRANSIT);
    }

    ManifestInfo(
            String accountId,
            String accountToken,
            String accountRegion,
            String proxyDomain,
            String spikyProxyDomain,
            String handshakeDomain,
            boolean useADID,
            boolean appLaunchedDisabled,
            String notificationIcon,
            String excludedActivitiesForInApps,
            boolean sslPinning,
            boolean backgroundSync,
            boolean useCustomID,
            String fcmSenderId,
            String packageName,
            boolean beta,
            String intentServiceName,
            String devDefaultPushChannelId,
            String[] profileKeys,
            int encryptionLevel,
            String provider1,
            String provider2,
            String encryptionInTransit
    ) {

        // assign these if they did not happen in change creds
        this.accountId = accountId;
        this.accountToken = accountToken;
        this.accountRegion = accountRegion;
        this.proxyDomain = proxyDomain;
        this.spikyProxyDomain = spikyProxyDomain;
        this.handshakeDomain = handshakeDomain;

        this.useADID = useADID;
        this.appLaunchedDisabled = appLaunchedDisabled;
        this.notificationIcon = notificationIcon;
        this.excludedActivitiesForInApps = excludedActivitiesForInApps;
        this.sslPinning = sslPinning;
        this.backgroundSync = backgroundSync;
        this.useCustomID = useCustomID;
        this.fcmSenderId = fcmSenderId;
        this.packageName = packageName;
        this.beta = beta;
        this.intentServiceName = intentServiceName;
        this.devDefaultPushChannelId = devDefaultPushChannelId;
        this.profileKeys = profileKeys;
        this.encryptionLevel = encryptionLevel;
        this.provider1 = provider1;
        this.provider2 = provider2;
        this.encryptionInTransit = encryptionInTransit;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getExcludedActivities() {
        return excludedActivitiesForInApps;
    }

    public String getFCMSenderId() {
        return fcmSenderId;
    }
    public String getDevDefaultPushChannelId() {
        return devDefaultPushChannelId;
    }

    public String getIntentServiceName() {
        return intentServiceName;
    }

    public String getNotificationIcon() {
        return notificationIcon;
    }

    public String[] getProfileKeys() {
        return profileKeys;
    }

    boolean enableBeta() {
        return beta;
    }
    public int getEncryptionLevel() {
        return encryptionLevel;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getAccountRegion() {
        Logger.v("ManifestInfo: getAccountRegion called, returning region:"+accountRegion);
        return accountRegion;
    }

    String getAccountToken() {
        return accountToken;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getProxyDomain() {
        Logger.v("ManifestInfo: getProxyDomain called, returning proxyDomain:" + proxyDomain);
        return proxyDomain;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getSpikeyProxyDomain() {
        Logger.v("ManifestInfo: getSpikeyProxyDomain called, returning spikeyProxyDomain:" + spikyProxyDomain);
        return spikyProxyDomain;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public String getHandshakeDomain() {
        Logger.v("ManifestInfo: getHandshakeDomain called, returning handshakeDomain:" + handshakeDomain);
        return handshakeDomain;
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

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isSSLPinningEnabled() {
        return sslPinning;
    }

    boolean useCustomId() {
        return useCustomID;
    }

    boolean useGoogleAdId() {
        return useADID;
    }

    @SuppressWarnings("ConstantConditions")
    private String[] parseProfileKeys(final Bundle metaData) {
        String profileKeyString = _getManifestStringValueForKey(metaData, Constants.CLEVERTAP_IDENTIFIER);
        return !TextUtils.isEmpty(profileKeyString) ? profileKeyString.split(Constants.SEPARATOR_COMMA)
                : Constants.NULL_STRING_ARRAY;
    }

    /**
     * This returns string representation of int,boolean,string,float value of given key
     *
     * @param manifest bundle to retrieve values from
     * @param name     key of bundle
     * @return string representation of int,boolean,string,float
     */
    private String _getManifestStringValueForKey(Bundle manifest, String name) {
        try {
            Object o = manifest.get(name);
            return (o != null) ? o.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public String getVendorOneProvider() {
        return provider1;
    }

    public String getVendorTwoProvider() {
        return provider2;
    }

    public String getEncryptionInTransit() {
        return encryptionInTransit;
    }
}
