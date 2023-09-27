package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.pushnotification.PushNotificationUtil.getAll;
import static com.clevertap.android.sdk.utils.CTJsonConverter.toArray;
import static com.clevertap.android.sdk.utils.CTJsonConverter.toJsonArray;
import static com.clevertap.android.sdk.utils.CTJsonConverter.toList;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

import com.clevertap.android.sdk.Constants.IdentityType;
import com.clevertap.android.sdk.cryption.CryptHandler;
import com.clevertap.android.sdk.login.LoginConstants;

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

    @NonNull
    private ArrayList<String> allowedPushTypes = getAll();

    private boolean analyticsOnly;

    private boolean backgroundSync;

    private boolean beta;

    private boolean createdPostAppLaunch;

    @Deprecated
    private int debugLevel;

    private boolean disableAppLaunchedEvent;

    private boolean enableCustomCleverTapId;

    private String fcmSenderId;

    private boolean isDefaultInstance;

    @Deprecated
    private Logger logger;

    private String packageName;

    private boolean personalization;

    private String[] identityKeys = Constants.NULL_STRING_ARRAY;

    private boolean sslPinning;

    private boolean useGoogleAdId;
    private int encryptionLevel;


    @SuppressWarnings("unused")
    public static CleverTapInstanceConfig createInstance(Context context, @NonNull String accountId,
            @NonNull String accountToken) {

        //noinspection ConstantConditions
        if (accountId == null || accountToken == null) {
            Logger.info("CleverTap accountId and accountToken cannot be null");
            return null;
        }
        return new CleverTapInstanceConfig(context, accountId, accountToken, null, false);
    }

    @SuppressWarnings({"unused"})
    public static CleverTapInstanceConfig createInstance(Context context, @NonNull String accountId,
            @NonNull String accountToken, String accountRegion) {
        //noinspection ConstantConditions
        if (accountId == null || accountToken == null) {
            Logger.info("CleverTap accountId and accountToken cannot be null");
            return null;
        }
        return new CleverTapInstanceConfig(context, accountId, accountToken, accountRegion, false);
    }

    CleverTapInstanceConfig(CleverTapInstanceConfig config) {
        this.accountId = config.accountId;
        this.accountToken = config.accountToken;
        this.accountRegion = config.accountRegion;
        this.isDefaultInstance = config.isDefaultInstance;
        this.analyticsOnly = config.analyticsOnly;
        this.personalization = config.personalization;
        this.debugLevel = config.debugLevel;
        this.useGoogleAdId = config.useGoogleAdId;
        this.disableAppLaunchedEvent = config.disableAppLaunchedEvent;
        this.createdPostAppLaunch = config.createdPostAppLaunch;
        this.sslPinning = config.sslPinning;
        this.backgroundSync = config.backgroundSync;
        this.enableCustomCleverTapId = config.enableCustomCleverTapId;
        this.fcmSenderId = config.fcmSenderId;
        this.packageName = config.packageName;
        this.beta = config.beta;
        this.allowedPushTypes = config.allowedPushTypes;
        this.identityKeys = config.identityKeys;
        this.encryptionLevel = config.encryptionLevel;
    }

    private
    CleverTapInstanceConfig(Context context, String accountId, String accountToken, String accountRegion,
            boolean isDefault) {
        this.accountId = accountId;
        this.accountToken = accountToken;
        this.accountRegion = accountRegion;
        this.isDefaultInstance = isDefault;
        this.analyticsOnly = false;
        this.personalization = true;
        this.createdPostAppLaunch = false;

        ManifestInfo manifest = ManifestInfo.getInstance(context);
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
            Logger.verbose(accountId, LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys from Manifest: " + Arrays
                    .toString(identityKeys));
        } else {
            this.encryptionLevel = 0;
        }
    }

    private CleverTapInstanceConfig(String jsonString) throws Throwable {
        try {
            JSONObject configJsonObject = new JSONObject(jsonString);
            if (configJsonObject.has(Constants.KEY_ACCOUNT_ID)) {
                this.accountId = configJsonObject.getString(Constants.KEY_ACCOUNT_ID);
            }
            if (configJsonObject.has(Constants.KEY_ACCOUNT_TOKEN)) {
                this.accountToken = configJsonObject.getString(Constants.KEY_ACCOUNT_TOKEN);
            }
            if (configJsonObject.has(Constants.KEY_ACCOUNT_REGION)) {
                this.accountRegion = configJsonObject.getString(Constants.KEY_ACCOUNT_REGION);
            }
            if (configJsonObject.has(Constants.KEY_ANALYTICS_ONLY)) {
                this.analyticsOnly = configJsonObject.getBoolean(Constants.KEY_ANALYTICS_ONLY);
            }
            if (configJsonObject.has(Constants.KEY_DEFAULT_INSTANCE)) {
                this.isDefaultInstance = configJsonObject.getBoolean(Constants.KEY_DEFAULT_INSTANCE);
            }
            if (configJsonObject.has(Constants.KEY_USE_GOOGLE_AD_ID)) {
                this.useGoogleAdId = configJsonObject.getBoolean(Constants.KEY_USE_GOOGLE_AD_ID);
            }
            if (configJsonObject.has(Constants.KEY_DISABLE_APP_LAUNCHED)) {
                this.disableAppLaunchedEvent = configJsonObject.getBoolean(Constants.KEY_DISABLE_APP_LAUNCHED);
            }
            if (configJsonObject.has(Constants.KEY_PERSONALIZATION)) {
                this.personalization = configJsonObject.getBoolean(Constants.KEY_PERSONALIZATION);
            }
            if (configJsonObject.has(Constants.KEY_DEBUG_LEVEL)) {
                this.debugLevel = configJsonObject.getInt(Constants.KEY_DEBUG_LEVEL);
            }
            this.logger = new Logger(this.debugLevel);

            if (configJsonObject.has(Constants.KEY_PACKAGE_NAME)) {
                this.packageName = configJsonObject.getString(Constants.KEY_PACKAGE_NAME);
            }
            if (configJsonObject.has(Constants.KEY_CREATED_POST_APP_LAUNCH)) {
                this.createdPostAppLaunch = configJsonObject.getBoolean(Constants.KEY_CREATED_POST_APP_LAUNCH);
            }
            if (configJsonObject.has(Constants.KEY_SSL_PINNING)) {
                this.sslPinning = configJsonObject.getBoolean(Constants.KEY_SSL_PINNING);
            }
            if (configJsonObject.has(Constants.KEY_BACKGROUND_SYNC)) {
                this.backgroundSync = configJsonObject.getBoolean(Constants.KEY_BACKGROUND_SYNC);
            }
            if (configJsonObject.has(Constants.KEY_ENABLE_CUSTOM_CT_ID)) {
                this.enableCustomCleverTapId = configJsonObject.getBoolean(Constants.KEY_ENABLE_CUSTOM_CT_ID);
            }
            if (configJsonObject.has(Constants.KEY_FCM_SENDER_ID)) {
                this.fcmSenderId = configJsonObject.getString(Constants.KEY_FCM_SENDER_ID);
            }
            if (configJsonObject.has(Constants.KEY_BETA)) {
                this.beta = configJsonObject.getBoolean(Constants.KEY_BETA);
            }
            if (configJsonObject.has(Constants.KEY_ALLOWED_PUSH_TYPES)) {
                this.allowedPushTypes = (ArrayList<String>) toList(
                        configJsonObject.getJSONArray(Constants.KEY_ALLOWED_PUSH_TYPES));
            }
            if (configJsonObject.has(Constants.KEY_IDENTITY_TYPES)) {
                this.identityKeys = (String[]) toArray(configJsonObject.getJSONArray(Constants.KEY_IDENTITY_TYPES));
            }
            if(configJsonObject.has(Constants.KEY_ENCRYPTION_LEVEL)){
                this.encryptionLevel = configJsonObject.getInt(Constants.KEY_ENCRYPTION_LEVEL);
            }
        } catch (Throwable t) {
            Logger.verbose("Error constructing CleverTapInstanceConfig from JSON: " + jsonString + ": ", t.getCause());
            throw (t);
        }
    }

    private CleverTapInstanceConfig(Parcel in) {
        accountId = in.readString();
        accountToken = in.readString();
        accountRegion = in.readString();
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
        allowedPushTypes = new ArrayList<>();
        in.readList(allowedPushTypes, String.class.getClassLoader());
        identityKeys = in.createStringArray();
        encryptionLevel = in.readInt();
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
    public ArrayList<String> getAllowedPushTypes() {
        return allowedPushTypes;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Deprecated
    public int getDebugLevel() {
        return debugLevel;
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * Use CleverTapAPI.setDebugLevel() instead. Loglevel is now common for all instances
     * </p>
     */
    @Deprecated
    @SuppressWarnings({"unused"})
    public void setDebugLevel(CleverTapAPI.LogLevel debugLevel) {
        setDebugLevel(debugLevel.intValue());
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * Use CleverTapAPI.setDebugLevel() instead. Log level is now common for all instances
     * </p>
     */
    @Deprecated
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

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @Deprecated
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

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated
    public void log(@NonNull String tag, @NonNull String message) {
        logger.verbose(getDefaultSuffix(tag), message);
    }

    /**
     * <p style="color:#4d2e00;background:#ffcc99;font-weight: bold" >
     * Note: This method has been deprecated since v5.3.0 to make logging static across the SDK.
     * It will be removed in the future versions of this SDK.
     * </p>
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Deprecated
    public void log(@NonNull String tag, @NonNull String message, Throwable throwable) {
        logger.verbose(getDefaultSuffix(tag), message, throwable);
    }

    public void setIdentityKeys(@IdentityType String... identityKeys) {
        if (!isDefaultInstance) {
            this.identityKeys = identityKeys;
            Logger.verbose(accountId, LoginConstants.LOG_TAG_ON_USER_LOGIN, "Setting Profile Keys via setter: " + Arrays
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
        dest.writeList(allowedPushTypes);
        dest.writeStringArray(identityKeys);
        dest.writeInt(encryptionLevel);
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
    public void setEncryptionLevel(CryptHandler.EncryptionLevel encryptionLevel) {
        this.encryptionLevel = encryptionLevel.intValue();
    }
    public int getEncryptionLevel() {
        return encryptionLevel;
    }

    String toJSONString() {
        JSONObject configJsonObject = new JSONObject();
        try {
            configJsonObject.put(Constants.KEY_ACCOUNT_ID, getAccountId());
            configJsonObject.put(Constants.KEY_ACCOUNT_TOKEN, getAccountToken());
            configJsonObject.put(Constants.KEY_ACCOUNT_REGION, getAccountRegion());
            configJsonObject.put(Constants.KEY_FCM_SENDER_ID, getFcmSenderId());
            configJsonObject.put(Constants.KEY_ANALYTICS_ONLY, isAnalyticsOnly());
            configJsonObject.put(Constants.KEY_DEFAULT_INSTANCE, isDefaultInstance());
            configJsonObject.put(Constants.KEY_USE_GOOGLE_AD_ID, isUseGoogleAdId());
            configJsonObject.put(Constants.KEY_DISABLE_APP_LAUNCHED, isDisableAppLaunchedEvent());
            configJsonObject.put(Constants.KEY_PERSONALIZATION, isPersonalizationEnabled());
            configJsonObject.put(Constants.KEY_DEBUG_LEVEL, getDebugLevel());
            configJsonObject.put(Constants.KEY_CREATED_POST_APP_LAUNCH, isCreatedPostAppLaunch());
            configJsonObject.put(Constants.KEY_SSL_PINNING, isSslPinningEnabled());
            configJsonObject.put(Constants.KEY_BACKGROUND_SYNC, isBackgroundSync());
            configJsonObject.put(Constants.KEY_ENABLE_CUSTOM_CT_ID, getEnableCustomCleverTapId());
            configJsonObject.put(Constants.KEY_PACKAGE_NAME, getPackageName());
            configJsonObject.put(Constants.KEY_BETA, isBeta());
            configJsonObject.put(Constants.KEY_ALLOWED_PUSH_TYPES, toJsonArray(allowedPushTypes));
            configJsonObject.put(Constants.KEY_ENCRYPTION_LEVEL , getEncryptionLevel());
            return configJsonObject.toString();
        } catch (Throwable e) {
            Logger.verbose("Unable to convert config to JSON : ", e.getCause());
            return null;
        }
    }

    private String getDefaultSuffix(@NonNull String tag) {
        return "[" + ((!TextUtils.isEmpty(tag) ? ":" + tag : "") + ":" + accountId + "]");
    }

    // convenience to construct the internal only default config
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected static CleverTapInstanceConfig createDefaultInstance(Context context, @NonNull String accountId,
            @NonNull String accountToken, String accountRegion) {
        return new CleverTapInstanceConfig(context, accountId, accountToken, accountRegion, true);
    }

    // for internal use only!
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected static CleverTapInstanceConfig createInstance(@NonNull String jsonString) {
        try {
            return new CleverTapInstanceConfig(jsonString);
        } catch (Throwable t) {
            return null;
        }
    }

}