package com.clevertap.android.sdk;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.json.JSONObject;

public class CleverTapInstanceConfig implements Parcelable {

    private String accountId;
    private String accountToken;
    private String accountRegion;
    private String gcmSenderId;
    private boolean analyticsOnly;
    private boolean isDefaultInstance;
    private boolean useGoogleAdId;
    private boolean disableAppLaunchedEvent;
    private boolean personalization;
    private int debugLevel;
    protected Logger logger;
    private boolean createdPostAppLaunch;
    private boolean sslPinning;
    private boolean backgroundSync;
    private boolean raiseNotificationViewed;
    private boolean enableCustomCleverTapId;
    private String fcmSenderId;

    private CleverTapInstanceConfig(Context context, String accountId, String accountToken, String accountRegion, boolean isDefault){
        this.accountId = accountId;
        this.accountToken = accountToken;
        this.accountRegion = accountRegion;
        this.isDefaultInstance = isDefault;
        this.analyticsOnly = false;
        this.personalization = true;
        this.debugLevel = CleverTapAPI.LogLevel.INFO.intValue();
        this.logger = new Logger(this.debugLevel);
        this.createdPostAppLaunch = false;

        ManifestInfo manifest = ManifestInfo.getInstance(context);
        this.useGoogleAdId = manifest.useGoogleAdId();
        this.disableAppLaunchedEvent = manifest.isAppLaunchedDisabled();
        this.gcmSenderId = manifest.getGCMSenderId();
        this.sslPinning = manifest.isSSLPinningEnabled();
        this.backgroundSync = manifest.isBackgroundSync();
        this.raiseNotificationViewed = manifest.raiseNotificationViewed();
        this.fcmSenderId = manifest.getFCMSenderId();
        this.enableCustomCleverTapId = manifest.useCustomId();
    }

    CleverTapInstanceConfig(CleverTapInstanceConfig config){
        this.accountId = config.accountId;
        this.accountToken = config.accountToken;
        this.accountRegion = config.accountRegion;
        this.isDefaultInstance = config.isDefaultInstance;
        this.analyticsOnly = config.analyticsOnly;
        this.personalization = config.personalization;
        this.debugLevel = config.debugLevel;
        this.logger = config.logger;
        this.useGoogleAdId = config.useGoogleAdId;
        this.disableAppLaunchedEvent = config.disableAppLaunchedEvent;
        this.gcmSenderId = config.gcmSenderId;
        this.createdPostAppLaunch = config.createdPostAppLaunch;
        this.sslPinning = config.sslPinning;
        this.backgroundSync = config.backgroundSync;
        this.raiseNotificationViewed = config.raiseNotificationViewed;
        this.enableCustomCleverTapId = config.enableCustomCleverTapId;
        this.fcmSenderId = config.fcmSenderId;
    }

    private CleverTapInstanceConfig(String jsonString) throws Throwable {
        try{
            JSONObject configJsonObject = new JSONObject(jsonString);
            if(configJsonObject.has(Constants.KEY_ACCOUNT_ID))
                this.accountId = configJsonObject.getString(Constants.KEY_ACCOUNT_ID);
            if(configJsonObject.has(Constants.KEY_ACCOUNT_TOKEN))
                this.accountToken = configJsonObject.getString(Constants.KEY_ACCOUNT_TOKEN);
            if(configJsonObject.has(Constants.KEY_ACCOUNT_REGION))
                this.accountRegion = configJsonObject.getString(Constants.KEY_ACCOUNT_REGION);
            if(configJsonObject.has("gcmSenderId"))
                this.gcmSenderId = configJsonObject.getString("gcmSenderId");
            if(configJsonObject.has(Constants.KEY_ANALYTICS_ONLY))
                this.analyticsOnly = configJsonObject.getBoolean(Constants.KEY_ANALYTICS_ONLY);
            if(configJsonObject.has(Constants.KEY_DEFAULT_INSTANCE))
                this.isDefaultInstance = configJsonObject.getBoolean(Constants.KEY_DEFAULT_INSTANCE);
            if(configJsonObject.has(Constants.KEY_USE_GOOGLE_AD_ID))
                this.useGoogleAdId = configJsonObject.getBoolean(Constants.KEY_USE_GOOGLE_AD_ID);
            if(configJsonObject.has(Constants.KEY_DISABLE_APP_LAUNCHED))
                this.disableAppLaunchedEvent = configJsonObject.getBoolean(Constants.KEY_DISABLE_APP_LAUNCHED);
            if(configJsonObject.has(Constants.KEY_PERSONALIZATION))
                this.personalization = configJsonObject.getBoolean(Constants.KEY_PERSONALIZATION);
            if(configJsonObject.has(Constants.KEY_DEBUG_LEVEL)) {
                this.debugLevel = configJsonObject.getInt(Constants.KEY_DEBUG_LEVEL);
                this.logger = new Logger(this.debugLevel);
            }
            if(configJsonObject.has(Constants.KEY_CREATED_POST_APP_LAUNCH))
                this.createdPostAppLaunch = configJsonObject.getBoolean(Constants.KEY_CREATED_POST_APP_LAUNCH);
            if(configJsonObject.has(Constants.KEY_SSL_PINNING))
                this.sslPinning = configJsonObject.getBoolean(Constants.KEY_SSL_PINNING);
            if(configJsonObject.has(Constants.KEY_BACKGROUND_SYNC))
                this.backgroundSync = configJsonObject.getBoolean(Constants.KEY_BACKGROUND_SYNC);
            if(configJsonObject.has(Constants.KEY_RAISE_NOTIFICATION_VIEWED))
                this.raiseNotificationViewed = configJsonObject.getBoolean(Constants.KEY_RAISE_NOTIFICATION_VIEWED);
            if(configJsonObject.has(Constants.KEY_ENABLE_CUSTOM_CT_ID))
                this.enableCustomCleverTapId = configJsonObject.getBoolean(Constants.KEY_ENABLE_CUSTOM_CT_ID);
            if(configJsonObject.has(Constants.KEY_FCM_SENDER_ID))
                this.gcmSenderId = configJsonObject.getString(Constants.KEY_FCM_SENDER_ID);
        } catch (Throwable t){
            Logger.v("Error constructing CleverTapInstanceConfig from JSON: " + jsonString +": ", t.getCause());
            throw(t);
        }
    }

    private CleverTapInstanceConfig(Parcel in){
        accountId = in.readString();
        accountToken = in.readString();
        accountRegion = in.readString();
        gcmSenderId = in.readString();
        analyticsOnly = in.readByte() != 0x00;
        isDefaultInstance = in.readByte() != 0x00;
        useGoogleAdId = in.readByte() != 0x00;
        disableAppLaunchedEvent = in.readByte() != 0x00;
        personalization = in.readByte() != 0x00;
        debugLevel = in.readInt();
        createdPostAppLaunch = in.readByte() != 0x00;
        sslPinning = in.readByte() != 0x00;
        backgroundSync = in.readByte() != 0x00;
        raiseNotificationViewed = in.readByte() != 0x00;
        enableCustomCleverTapId = in.readByte() != 0x00;
        fcmSenderId = in.readString();
    }

    @SuppressWarnings("unused")
    public static CleverTapInstanceConfig createInstance(Context context, @NonNull  String accountId, @NonNull  String accountToken) {
        //noinspection ConstantConditions
        if (accountId == null || accountToken == null) {
            Logger.i("CleverTap accountId and accountToken cannot be null");
            return null;
        }
        return new CleverTapInstanceConfig(context, accountId, accountToken, null,false);
    }

    @SuppressWarnings({"unused"})
    public static CleverTapInstanceConfig createInstance(Context context, @NonNull String accountId, @NonNull String accountToken, String accountRegion) {
        //noinspection ConstantConditions
        if (accountId == null || accountToken == null) {
            Logger.i("CleverTap accountId and accountToken cannot be null");
            return null;
        }
        return new CleverTapInstanceConfig(context, accountId, accountToken, accountRegion,false);
    }

    // for internal use only!
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected static CleverTapInstanceConfig createInstance(@NonNull String jsonString){
        try {
            return new CleverTapInstanceConfig(jsonString);
        } catch (Throwable t) {
            return null;
        }
    }

    // convenience to construct the internal only default config
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected static CleverTapInstanceConfig createDefaultInstance(Context context, @NonNull String accountId, @NonNull String accountToken, String accountRegion) {
        return new CleverTapInstanceConfig(context, accountId, accountToken, accountRegion, true);
    }

    public String getAccountId() {
        return accountId;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getAccountToken() {
        return accountToken;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public String getAccountRegion() {
        return accountRegion;
    }

    private String getGCMSenderId() {
        return gcmSenderId;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public boolean isAnalyticsOnly() {
        return analyticsOnly;
    }

    @SuppressWarnings({"unused"})
    public void setAnalyticsOnly(boolean analyticsOnly) {
        this.analyticsOnly = analyticsOnly;
    }

    boolean isDefaultInstance() {
        return isDefaultInstance;
    }

    boolean isCreatedPostAppLaunch() {
        return createdPostAppLaunch;
    }

    void setCreatedPostAppLaunch() {
        this.createdPostAppLaunch = true;
    }

    boolean isUseGoogleAdId() {
        return useGoogleAdId;
    }

    @SuppressWarnings({"unused"})
    public void useGoogleAdId(boolean value){
        this.useGoogleAdId = value;
    }

    boolean isDisableAppLaunchedEvent() {
        return disableAppLaunchedEvent;
    }

    public void setDisableAppLaunchedEvent(boolean disableAppLaunchedEvent) {
        this.disableAppLaunchedEvent = disableAppLaunchedEvent;
    }

    boolean isSslPinningEnabled() {
        return sslPinning;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void enablePersonalization(boolean enablePersonalization) {
        this.personalization = enablePersonalization;
    }

    boolean isPersonalizationEnabled() {
        return personalization;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public int getDebugLevel() {
        return debugLevel;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setDebugLevel(CleverTapAPI.LogLevel debugLevel) {
        this.debugLevel = debugLevel.intValue();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setDebugLevel(int debugLevel) {
        this.debugLevel = debugLevel;
    }

    public Logger getLogger() {
        return logger;
    }

    boolean isBackgroundSync() {
        return backgroundSync;
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public void setBackgroundSync(boolean backgroundSync) {
        this.backgroundSync = backgroundSync;
    }

    public void setRaiseNotificationViewed(boolean raiseNotificationViewed) {
        this.raiseNotificationViewed = raiseNotificationViewed;
    }

    boolean getRaiseNotificationViewed() {
        return raiseNotificationViewed;
    }

    boolean getEnableCustomCleverTapId() {
        return enableCustomCleverTapId;
    }

    public void setEnableCustomCleverTapId(boolean enableCustomCleverTapId) {
        this.enableCustomCleverTapId = enableCustomCleverTapId;
    }

    public String getFCMSenderId() {
        return fcmSenderId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(accountId);
        dest.writeString(accountToken);
        dest.writeString(accountRegion);
        dest.writeString(gcmSenderId);
        dest.writeByte((byte) (analyticsOnly ? 0x01 : 0x00));
        dest.writeByte((byte) (isDefaultInstance ? 0x01 : 0x00));
        dest.writeByte((byte) (useGoogleAdId ? 0x01 : 0x00));
        dest.writeByte((byte) (disableAppLaunchedEvent ? 0x01 : 0x00));
        dest.writeByte((byte) (personalization ? 0x01 : 0x00));
        dest.writeInt(debugLevel);
        dest.writeByte((byte) (sslPinning ? 0x01 : 0x00));
        dest.writeByte((byte) (createdPostAppLaunch ? 0x01 : 0x00));
        dest.writeByte((byte) (backgroundSync ? 0x01 : 0x00));
        dest.writeByte((byte) (raiseNotificationViewed ? 0x01 : 0x00));
        dest.writeByte((byte) (enableCustomCleverTapId ? 0x01 : 0x00));
        dest.writeString(fcmSenderId);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<CleverTapInstanceConfig> CREATOR = new Parcelable.Creator<CleverTapInstanceConfig>() {
        @Override
        public CleverTapInstanceConfig createFromParcel(Parcel in) {
            return new CleverTapInstanceConfig(in);
        }

        @Override
        public CleverTapInstanceConfig[] newArray(int size) {
            return new CleverTapInstanceConfig[size];
        }
    };

    String toJSONString() {
        JSONObject configJsonObject = new JSONObject();
        try{
            configJsonObject.put(Constants.KEY_ACCOUNT_ID, getAccountId());
            configJsonObject.put(Constants.KEY_ACCOUNT_TOKEN, getAccountToken());
            configJsonObject.put(Constants.KEY_ACCOUNT_REGION, getAccountRegion());
            configJsonObject.put("gcmSenderId", getGCMSenderId());
            configJsonObject.put(Constants.KEY_ANALYTICS_ONLY, isAnalyticsOnly());
            configJsonObject.put(Constants.KEY_DEFAULT_INSTANCE, isDefaultInstance());
            configJsonObject.put(Constants.KEY_USE_GOOGLE_AD_ID, isUseGoogleAdId());
            configJsonObject.put(Constants.KEY_DISABLE_APP_LAUNCHED, isDisableAppLaunchedEvent());
            configJsonObject.put(Constants.KEY_PERSONALIZATION, isPersonalizationEnabled());
            configJsonObject.put(Constants.KEY_DEBUG_LEVEL, getDebugLevel());
            configJsonObject.put(Constants.KEY_CREATED_POST_APP_LAUNCH, isCreatedPostAppLaunch());
            configJsonObject.put(Constants.KEY_SSL_PINNING, isSslPinningEnabled());
            configJsonObject.put(Constants.KEY_BACKGROUND_SYNC, isBackgroundSync());
            configJsonObject.put(Constants.KEY_RAISE_NOTIFICATION_VIEWED, getRaiseNotificationViewed());
            configJsonObject.put(Constants.KEY_ENABLE_CUSTOM_CT_ID, getEnableCustomCleverTapId());
            configJsonObject.put(Constants.KEY_FCM_SENDER_ID, getFCMSenderId());
            return configJsonObject.toString();
        }catch (Throwable e){
            Logger.v("Unable to convert config to JSON : ",e.getCause());
            return null;
        }
    }
}
