package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getDefaultPushTypes;
import static com.clevertap.android.sdk.utils.CTJsonConverter.toArray;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.Constants.IdentityType;
import com.clevertap.android.sdk.cryption.EncryptionLevel;
import com.clevertap.android.sdk.login.LoginConstants;
import com.clevertap.android.sdk.pushnotification.PushConstants;
import com.clevertap.android.sdk.pushnotification.PushType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class CleverTapInstanceConfig implements Parcelable {

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CleverTapInstanceConfig> CREATOR
            = new Parcelable.Creator<CleverTapInstanceConfig>() {
        @Override
        public CleverTapInstanceConfig createFromParcel(Parcel in) {
            return new CleverTapInstanceConfig(in);
        }

        @Override
        public CleverTapInstanceConfig[] newArray(int size) {
            return new CleverTapInstanceConfig[size];
        }
    };

    private String accountId;
    private String accountRegion;
    private String accountToken;
    private String proxyDomain;
    private String spikyProxyDomain;
    private String customHandshakeDomain;
    @NonNull private final ArrayList<PushType> pushTypes = getDefaultPushTypes();
    private boolean analyticsOnly;
    private boolean backgroundSync;
    private boolean beta;
    private boolean createdPostAppLaunch;
    private int debugLevel;
    private boolean disableAppLaunchedEvent;
    private boolean enableCustomCleverTapId;
    private String fcmSenderId;
    private boolean isDefaultInstance;
    private Logger logger;
    private String packageName;
    private boolean personalization;
    private String[] identityKeys = Constants.NULL_STRING_ARRAY;
    private boolean sslPinning;
    private boolean useGoogleAdId;
    private int encryptionLevel;

    /**
     * Creates a CleverTapInstanceConfig with meta data from manifest file
     * @param context Application context
     * @return CleverTapInstanceConfig
     */
    @SuppressWarnings("unused")
    public static CleverTapInstanceConfig getDefaultInstance(Context context) {
        ManifestInfo info = ManifestInfo.getInstance(context);
        return CleverTapInstanceConfig.createInstanceWithManifest(
                info,
                info.getAccountId(),
                info.getAccountToken(),
                info.getAccountRegion(),
                true
        );
    }

    @SuppressWarnings("unused")
    public static CleverTapInstanceConfig createInstance(
            Context context,
            @NonNull String accountId,
            @NonNull String accountToken
    ) {
        return CleverTapInstanceConfig.createInstance(context, accountId, accountToken, null);
    }

    @SuppressWarnings({"unused"})
    public static CleverTapInstanceConfig createInstance(
            @NonNull Context context,
            @NonNull String accountId,
            @NonNull String accountToken,
            @Nullable String accountRegion
    ) {
        //noinspection ConstantConditions
        if (accountId == null || accountToken == null) {
            Logger.i("CleverTap accountId and accountToken cannot be null");
            return null;
        }
        ManifestInfo manifestInfo = ManifestInfo.getInstance(context);
        return CleverTapInstanceConfig.createInstanceWithManifest(manifestInfo, accountId, accountToken, accountRegion, false);
    }


    // convenience to construct the internal only default config
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected static CleverTapInstanceConfig createDefaultInstance(
            @NonNull Context context,
            @NonNull String accountId,
            @NonNull String accountToken,
            @Nullable String accountRegion
    ) {
        ManifestInfo manifestInfo = ManifestInfo.getInstance(context);
        return CleverTapInstanceConfig.createInstanceWithManifest(manifestInfo, accountId, accountToken, accountRegion, true);
    }

    static CleverTapInstanceConfig createInstanceWithManifest(
            @NonNull ManifestInfo manifest,
            @NonNull String accountId,
            @NonNull String accountToken,
            @Nullable String accountRegion,
            boolean isDefaultInstance
    ) {
        return new CleverTapInstanceConfig(manifest, accountId, accountToken, accountRegion, isDefaultInstance);
    }

    // for internal use only!
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Nullable
    protected static CleverTapInstanceConfig createInstance(@NonNull String jsonString) {
        try {
            return new CleverTapInstanceConfig(jsonString);
        } catch (Throwable t) {
            return null;
        }
    }

    CleverTapInstanceConfig(CleverTapInstanceConfig config) {
        this.accountId = config.accountId;
        this.accountToken = config.accountToken;
        this.accountRegion = config.accountRegion;
        this.proxyDomain = config.proxyDomain;
        this.spikyProxyDomain = config.spikyProxyDomain;
        this.customHandshakeDomain = config.customHandshakeDomain;
        this.isDefaultInstance = config.isDefaultInstance;
        this.analyticsOnly = config.analyticsOnly;
        this.personalization = config.personalization;
        this.debugLevel = config.debugLevel;
        this.logger = config.logger;
        this.useGoogleAdId = config.useGoogleAdId;
        this.disableAppLaunchedEvent = config.disableAppLaunchedEvent;
        this.createdPostAppLaunch = config.createdPostAppLaunch;
        this.sslPinning = config.sslPinning;
        this.backgroundSync = config.backgroundSync;
        this.enableCustomCleverTapId = config.enableCustomCleverTapId;
        this.fcmSenderId = config.fcmSenderId;
        this.packageName = config.packageName;
        this.beta = config.beta;
        this.identityKeys = config.identityKeys;
        this.encryptionLevel = config.encryptionLevel;
        for (PushType pushType: config.pushTypes) {
            addPushType(pushType);
        }
    }

    private CleverTapInstanceConfig(
            ManifestInfo manifest,
            String accountId,
            String accountToken,
            String accountRegion,
            boolean isDefault
    ) {
        this.accountId = accountId;
        this.accountToken = accountToken;
        this.accountRegion = accountRegion;
        this.isDefaultInstance = isDefault;
        this.analyticsOnly = false;
        this.personalization = true;
        this.debugLevel = CleverTapAPI.LogLevel.INFO.intValue();
        this.logger = new Logger(this.debugLevel);
        this.createdPostAppLaunch = false;

        this.useGoogleAdId = manifest.useGoogleAdId();
        this.disableAppLaunchedEvent = manifest.isAppLaunchedDisabled();
        this.sslPinning = manifest.isSSLPinningEnabled();
        this.backgroundSync = manifest.isBackgroundSync();
        this.fcmSenderId = manifest.getFCMSenderId();
        this.packageName = manifest.getPackageName();
        this.enableCustomCleverTapId = manifest.useCustomId();
        this.beta = manifest.enableBeta();
        /*
         * For default instance, use manifest meta, otherwise use from setter field
         */
        if (isDefaultInstance) {
            this.encryptionLevel = manifest.getEncryptionLevel();
            identityKeys = manifest.getProfileKeys();
            log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys from Manifest: " + Arrays
                    .toString(identityKeys));
        } else {
            this.encryptionLevel = 0;
        }
        buildPushProvidersFromManifest(manifest);
    }

    private void buildPushProvidersFromManifest(ManifestInfo manifest) {
        try {
            String provider1 = manifest.getVendorOneProvider();
            if (provider1 != null) {
                String[] splits = provider1.split(",");
                if (splits != null && splits.length == 5) {
                    PushType pushType = new PushType(splits[0], splits[1], splits[2], splits[3], splits[4]);
                    addPushType(pushType);
                }
            }
            String provider2 = manifest.getVendorTwoProvider();
            if (provider2 != null) {
                String[] splits = provider2.split(",");
                if (splits != null && splits.length == 5) {
                    PushType pushType = new PushType(splits[0], splits[1], splits[2], splits[3], splits[4]);
                    addPushType(pushType);
                }
            }
        } catch (Exception e) {
            Logger.v("There was some problem in loading push providers from manifest");
        }
    }

    private CleverTapInstanceConfig(String jsonString) throws Throwable {
        try {
            JSONObject configJsonObject = new JSONObject(jsonString);
            if (configJsonObject.has(KEY_ACCOUNT_ID)) {
                this.accountId = configJsonObject.getString(KEY_ACCOUNT_ID);
            }
            if (configJsonObject.has(KEY_ACCOUNT_TOKEN)) {
                this.accountToken = configJsonObject.getString(KEY_ACCOUNT_TOKEN);
            }
            if (configJsonObject.has(KEY_PROXY_DOMAIN)) {
                this.proxyDomain = configJsonObject.getString(KEY_PROXY_DOMAIN);
            }
            if (configJsonObject.has(KEY_SPIKY_PROXY_DOMAIN)) {
                this.spikyProxyDomain = configJsonObject.getString(KEY_SPIKY_PROXY_DOMAIN);
            }
            if (configJsonObject.has(KEY_CUSTOM_HANDSHAKE_DOMAIN)) {
                this.customHandshakeDomain = configJsonObject.optString(KEY_CUSTOM_HANDSHAKE_DOMAIN, null);
            }
            if (configJsonObject.has(KEY_ACCOUNT_REGION)) {
                this.accountRegion = configJsonObject.getString(KEY_ACCOUNT_REGION);
            }
            if (configJsonObject.has(KEY_ANALYTICS_ONLY)) {
                this.analyticsOnly = configJsonObject.getBoolean(KEY_ANALYTICS_ONLY);
            }
            if (configJsonObject.has(KEY_DEFAULT_INSTANCE)) {
                this.isDefaultInstance = configJsonObject.getBoolean(KEY_DEFAULT_INSTANCE);
            }
            if (configJsonObject.has(KEY_USE_GOOGLE_AD_ID)) {
                this.useGoogleAdId = configJsonObject.getBoolean(KEY_USE_GOOGLE_AD_ID);
            }
            if (configJsonObject.has(KEY_DISABLE_APP_LAUNCHED)) {
                this.disableAppLaunchedEvent = configJsonObject.getBoolean(KEY_DISABLE_APP_LAUNCHED);
            }
            if (configJsonObject.has(KEY_PERSONALIZATION)) {
                this.personalization = configJsonObject.getBoolean(KEY_PERSONALIZATION);
            }
            if (configJsonObject.has(KEY_DEBUG_LEVEL)) {
                this.debugLevel = configJsonObject.getInt(KEY_DEBUG_LEVEL);
            }
            this.logger = new Logger(this.debugLevel);

            if (configJsonObject.has(KEY_PACKAGE_NAME)) {
                this.packageName = configJsonObject.getString(KEY_PACKAGE_NAME);
            }
            if (configJsonObject.has(KEY_CREATED_POST_APP_LAUNCH)) {
                this.createdPostAppLaunch = configJsonObject.getBoolean(KEY_CREATED_POST_APP_LAUNCH);
            }
            if (configJsonObject.has(KEY_SSL_PINNING)) {
                this.sslPinning = configJsonObject.getBoolean(KEY_SSL_PINNING);
            }
            if (configJsonObject.has(KEY_BACKGROUND_SYNC)) {
                this.backgroundSync = configJsonObject.getBoolean(KEY_BACKGROUND_SYNC);
            }
            if (configJsonObject.has(KEY_ENABLE_CUSTOM_CT_ID)) {
                this.enableCustomCleverTapId = configJsonObject.getBoolean(KEY_ENABLE_CUSTOM_CT_ID);
            }
            if (configJsonObject.has(KEY_FCM_SENDER_ID)) {
                this.fcmSenderId = configJsonObject.getString(KEY_FCM_SENDER_ID);
            }
            if (configJsonObject.has(KEY_BETA)) {
                this.beta = configJsonObject.getBoolean(KEY_BETA);
            }
            if (configJsonObject.has(KEY_IDENTITY_TYPES)) {
                this.identityKeys = (String[]) toArray(configJsonObject.getJSONArray(KEY_IDENTITY_TYPES));
            }
            if (configJsonObject.has(KEY_ENCRYPTION_LEVEL)){
                this.encryptionLevel = configJsonObject.getInt(KEY_ENCRYPTION_LEVEL);
            }
            if (configJsonObject.has(KEY_PUSH_TYPES)) {
                JSONArray pushTypesArray = configJsonObject.getJSONArray(KEY_PUSH_TYPES);
                for (int i = 0; i < pushTypesArray.length(); i++) {
                    JSONObject pushTypeJo = pushTypesArray.getJSONObject(i);
                    PushType pushType = PushType.fromJSONObject(pushTypeJo);
                    if (pushType != null) {
                        addPushType(pushType);
                    }
                }
            }
        } catch (Throwable t) {
            Logger.v("Error constructing CleverTapInstanceConfig from JSON: " + jsonString + ": ", t.getCause());
            throw (t);
        }
    }

    private CleverTapInstanceConfig(Parcel in) {
        accountId = in.readString();
        accountToken = in.readString();
        accountRegion = in.readString();
        proxyDomain = in.readString();
        spikyProxyDomain = in.readString();
        customHandshakeDomain = in.readString();
        analyticsOnly = in.readByte() != 0x00;
        isDefaultInstance = in.readByte() != 0x00;
        useGoogleAdId = in.readByte() != 0x00;
        disableAppLaunchedEvent = in.readByte() != 0x00;
        personalization = in.readByte() != 0x00;
        debugLevel = in.readInt();
        createdPostAppLaunch = in.readByte() != 0x00;
        sslPinning = in.readByte() != 0x00;
        backgroundSync = in.readByte() != 0x00;
        enableCustomCleverTapId = in.readByte() != 0x00;
        fcmSenderId = in.readString();
        packageName = in.readString();
        logger = new Logger(debugLevel);
        beta = in.readByte() != 0x00;
        identityKeys = in.createStringArray();
        encryptionLevel = in.readInt();
        try {
            JSONArray allowedTypesJsonArray = new JSONArray(in.readString());
            for (int i = 0; i < allowedTypesJsonArray.length(); i++) {
                PushType pushType = PushType.fromJSONObject(allowedTypesJsonArray.getJSONObject(i));
                if (pushType != null) {
                    addPushType(pushType);
                }
            }
        } catch (JSONException e) {
            Logger.v("Error in loading push providers from parcel, using firebase");
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void enablePersonalization(boolean enablePersonalization) {
        this.personalization = enablePersonalization;
    }

    public String getAccountId() {
        return accountId;
    }

    @SuppressWarnings({"unused"})
    public String getAccountRegion() {
        return accountRegion;
    }

    @SuppressWarnings({"unused"})
    public String getAccountToken() {
        return accountToken;
    }

    @NonNull
    public ArrayList<PushType> getPushTypes() {
        return pushTypes;
    }

    public void addPushType(@NonNull PushType allowedPushType) {
        if (!pushTypes.contains(allowedPushType)) {
            this.pushTypes.add(allowedPushType);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getDebugLevel() {
        return debugLevel;
    }

    public String getProxyDomain() {
        return proxyDomain;
    }

    public void setProxyDomain(String proxyDomain) {
        this.proxyDomain = proxyDomain;
    }

    public String getSpikyProxyDomain() {
        return spikyProxyDomain;
    }

    public void setSpikyProxyDomain(String spikyProxyDomain) {
        this.spikyProxyDomain = spikyProxyDomain;
    }

    public String getCustomHandshakeDomain() {
        return customHandshakeDomain;
    }

    public void setCustomHandshakeDomain(String handshakeDomain) {
        this.customHandshakeDomain = handshakeDomain;
    }

    @SuppressWarnings({"unused"})
    public void setDebugLevel(CleverTapAPI.LogLevel debugLevel) {
        setDebugLevel(debugLevel.intValue());
    }

    @SuppressWarnings({"unused"})
    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
        if (logger != null) {
            logger.setDebugLevel(debugLevel);
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getFcmSenderId() {
        return fcmSenderId;
    }

    public Logger getLogger() {
        if (logger == null) {
            logger = new Logger(this.debugLevel);
        }
        return logger;
    }

    public String getPackageName() {
        return packageName;
    }

    public String[] getIdentityKeys() {
        return identityKeys;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public boolean isAnalyticsOnly() {
        return analyticsOnly;
    }

    @SuppressWarnings({"unused"})
    public void setAnalyticsOnly(boolean analyticsOnly) {
        this.analyticsOnly = analyticsOnly;
    }

    public boolean isBeta() {
        return beta;
    }

    public boolean isDefaultInstance() {
        return isDefaultInstance;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void log(@NonNull String tag, @NonNull String message) {
        logger.verbose(getDefaultSuffix(tag), message);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void log(@NonNull String tag, @NonNull String message, Throwable throwable) {
        logger.verbose(getDefaultSuffix(tag), message, throwable);
    }

    public void setIdentityKeys(@IdentityType String... identityKeys) {
        if (!isDefaultInstance) {
            this.identityKeys = identityKeys;
            log(LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys via setter: " + Arrays
                    .toString(this.identityKeys));
        }
    }

    @SuppressWarnings({"unused"})
    public void useGoogleAdId(boolean value) {
        this.useGoogleAdId = value;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(accountId);
        dest.writeString(accountToken);
        dest.writeString(accountRegion);
        dest.writeString(proxyDomain);
        dest.writeString(spikyProxyDomain);
        dest.writeString(customHandshakeDomain);
        dest.writeByte((byte) (analyticsOnly ? 0x01 : 0x00));
        dest.writeByte((byte) (isDefaultInstance ? 0x01 : 0x00));
        dest.writeByte((byte) (useGoogleAdId ? 0x01 : 0x00));
        dest.writeByte((byte) (disableAppLaunchedEvent ? 0x01 : 0x00));
        dest.writeByte((byte) (personalization ? 0x01 : 0x00));
        dest.writeInt(debugLevel);
        dest.writeByte((byte) (createdPostAppLaunch ? 0x01 : 0x00));
        dest.writeByte((byte) (sslPinning ? 0x01 : 0x00));
        dest.writeByte((byte) (backgroundSync ? 0x01 : 0x00));
        dest.writeByte((byte) (enableCustomCleverTapId ? 0x01 : 0x00));
        dest.writeString(fcmSenderId);
        dest.writeString(packageName);
        dest.writeByte((byte) (beta ? 0x01 : 0x00));
        dest.writeStringArray(identityKeys);
        dest.writeInt(encryptionLevel);
        String allowTypesString = getPushTypesArray().toString();
        dest.writeString(allowTypesString);
    }

    public boolean getEnableCustomCleverTapId() {
        return enableCustomCleverTapId;
    }

    @SuppressWarnings({"unused"})
    public void setEnableCustomCleverTapId(boolean enableCustomCleverTapId) {
        this.enableCustomCleverTapId = enableCustomCleverTapId;
    }

    @RestrictTo(Scope.LIBRARY)
    public boolean isBackgroundSync() {
        return backgroundSync;
    }

    @SuppressWarnings({"unused"})
    public void setBackgroundSync(boolean backgroundSync) {
        this.backgroundSync = backgroundSync;
    }

    public boolean isCreatedPostAppLaunch() {
        return createdPostAppLaunch;
    }

    boolean isDisableAppLaunchedEvent() {
        return disableAppLaunchedEvent;
    }

    @SuppressWarnings({"unused"})
    public void setDisableAppLaunchedEvent(boolean disableAppLaunchedEvent) {
        this.disableAppLaunchedEvent = disableAppLaunchedEvent;
    }

    boolean isPersonalizationEnabled() {
        return personalization;
    }

    public boolean isSslPinningEnabled() {
        return sslPinning;
    }

    boolean isUseGoogleAdId() {
        return useGoogleAdId;
    }

    void setCreatedPostAppLaunch() {
        this.createdPostAppLaunch = true;
    }
    public void setEncryptionLevel(EncryptionLevel encryptionLevel) {
        this.encryptionLevel = encryptionLevel.intValue();
    }
    public int getEncryptionLevel() {
        return encryptionLevel;
    }

    //Keys used by the SDK
    private static final String KEY_ACCOUNT_ID = "accountId";
    private static final String KEY_ACCOUNT_TOKEN = "accountToken";
    private static final String KEY_ACCOUNT_REGION = "accountRegion";
    private static final String KEY_PROXY_DOMAIN = "proxyDomain";
    private static final String KEY_SPIKY_PROXY_DOMAIN = "spikyProxyDomain";
    private static final String KEY_CUSTOM_HANDSHAKE_DOMAIN = "customHandshakeDomain";
    private static final String KEY_FCM_SENDER_ID = "fcmSenderId";
    private static final String KEY_ANALYTICS_ONLY = "analyticsOnly";
    private static final String KEY_DEFAULT_INSTANCE = "isDefaultInstance";
    private static final String KEY_USE_GOOGLE_AD_ID = "useGoogleAdId";
    private static final String KEY_DISABLE_APP_LAUNCHED = "disableAppLaunchedEvent";
    private static final String KEY_PERSONALIZATION = "personalization";
    private static final String KEY_DEBUG_LEVEL = "debugLevel";
    private static final String KEY_CREATED_POST_APP_LAUNCH = "createdPostAppLaunch";
    private static final String KEY_SSL_PINNING = "sslPinning";
    private static final String KEY_BACKGROUND_SYNC = "backgroundSync";
    private static final String KEY_ENABLE_CUSTOM_CT_ID = "getEnableCustomCleverTapId";
    private static final String KEY_BETA = "beta";
    private static final String KEY_IDENTITY_TYPES = "identityTypes";
    private static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_ENCRYPTION_LEVEL = "encryptionLevel";
    private static final String KEY_PUSH_TYPES = "allowedPushTypes";

    String toJSONString() {
        JSONObject configJsonObject = new JSONObject();
        try {
            configJsonObject.put(KEY_ACCOUNT_ID, getAccountId());
            configJsonObject.put(KEY_ACCOUNT_TOKEN, getAccountToken());
            configJsonObject.put(KEY_ACCOUNT_REGION, getAccountRegion());
            configJsonObject.put(KEY_PROXY_DOMAIN, getProxyDomain());
            configJsonObject.put(KEY_SPIKY_PROXY_DOMAIN, getSpikyProxyDomain());
            configJsonObject.put(KEY_CUSTOM_HANDSHAKE_DOMAIN, getCustomHandshakeDomain());
            configJsonObject.put(KEY_FCM_SENDER_ID, getFcmSenderId());
            configJsonObject.put(KEY_ANALYTICS_ONLY, isAnalyticsOnly());
            configJsonObject.put(KEY_DEFAULT_INSTANCE, isDefaultInstance());
            configJsonObject.put(KEY_USE_GOOGLE_AD_ID, isUseGoogleAdId());
            configJsonObject.put(KEY_DISABLE_APP_LAUNCHED, isDisableAppLaunchedEvent());
            configJsonObject.put(KEY_PERSONALIZATION, isPersonalizationEnabled());
            configJsonObject.put(KEY_DEBUG_LEVEL, getDebugLevel());
            configJsonObject.put(KEY_CREATED_POST_APP_LAUNCH, isCreatedPostAppLaunch());
            configJsonObject.put(KEY_SSL_PINNING, isSslPinningEnabled());
            configJsonObject.put(KEY_BACKGROUND_SYNC, isBackgroundSync());
            configJsonObject.put(KEY_ENABLE_CUSTOM_CT_ID, getEnableCustomCleverTapId());
            configJsonObject.put(KEY_PACKAGE_NAME, getPackageName());
            configJsonObject.put(KEY_BETA, isBeta());
            configJsonObject.put(KEY_ENCRYPTION_LEVEL , getEncryptionLevel());
            JSONArray pushTypesArray = getPushTypesArray();
            configJsonObject.put(KEY_PUSH_TYPES, pushTypesArray);

            return configJsonObject.toString();
        } catch (Throwable e) {
            Logger.v("Unable to convert config to JSON : ", e.getCause());
            return null;
        }
    }

    @NonNull
    private JSONArray getPushTypesArray() {
        JSONArray pushTypesArray = new JSONArray();
        for (PushType pushType : getPushTypes()) {
            // fcm is always loaded by default.
            if (pushType != PushConstants.FCM) {
                pushTypesArray.put(pushType.toJSONObject());
            }
        }
        return pushTypesArray;
    }

    private String getDefaultSuffix(@NonNull String tag) {
        return "[" + ((!TextUtils.isEmpty(tag) ? ":" + tag : "") + ":" + accountId + "]");
    }
}