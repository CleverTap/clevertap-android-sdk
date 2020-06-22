package com.clevertap.android.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

class DeviceInfo {

    private Context context;
    private CleverTapInstanceConfig config;
    private String library;
    private static final String GUID_PREFIX = "__";
    private final Object deviceIDLock = new Object();
    private final Object adIDLock = new Object();
    private String googleAdID = null;
    private boolean limitAdTracking = false;
    private boolean adIdRun = false;
    private static final String OS_NAME = "Android";
    private DeviceCachedInfo cachedInfo;
    private ArrayList<ValidationResult> validationResults = new ArrayList<>();

    ArrayList<ValidationResult> getValidationResults() {
        // noinspection unchecked
        ArrayList<ValidationResult> tempValidationResults = (ArrayList<ValidationResult>) validationResults.clone();
        validationResults.clear();
        return tempValidationResults;
    }

    DeviceInfo(Context context, CleverTapInstanceConfig config, String cleverTapID) {
        this.context = context;
        this.config = config;
        this.library = null;
        Thread deviceInfoCacheThread = new Thread(new Runnable() {
            @Override
            public void run() {
                getDeviceCachedInfo();
            }
        });
        deviceInfoCacheThread.start();
        initDeviceID(cleverTapID);
    }

    String getLibrary() {
        return library;
    }

    void setLibrary(String library) {
        this.library = library;
    }

    String getGoogleAdID() {
        synchronized (adIDLock) {
            return googleAdID;
        }
    }

    boolean isLimitAdTrackingEnabled() {
        synchronized (adIDLock) {
            return limitAdTracking;
        }
    }

    private DeviceCachedInfo getDeviceCachedInfo() {
        if (cachedInfo == null) {
            cachedInfo = new DeviceCachedInfo();
        }
        return cachedInfo;
    }

    private Logger getConfigLogger(){
        return  this.config.getLogger();
    }

    private void initDeviceID(String cleverTapID) {

        //Show logging as per Manifest flag
        if(config.getEnableCustomCleverTapId()){
            if(cleverTapID == null){
                config.getLogger().info("CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID");
                recordDeviceError("CLEVERTAP_USE_CUSTOM_ID has been specified in the AndroidManifest.xml/Instance Configuration. CleverTap SDK will create a fallback device ID");
            }
        }else{
            if(cleverTapID != null){
                config.getLogger().info("CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used.");
                recordDeviceError("CLEVERTAP_USE_CUSTOM_ID has not been specified in the AndroidManifest.xml. Custom CleverTap ID passed will not be used.");
            }
        }

        String deviceID = _getDeviceID();
        if(deviceID != null && deviceID.trim().length() > 2){
            getConfigLogger().verbose(config.getAccountId(),"CleverTap ID already present for profile");
            if(cleverTapID != null) {
                getConfigLogger().info(config.getAccountId(),"CleverTap ID - "+deviceID+" already exists. Unable to set custom CleverTap ID - " + cleverTapID);
                recordDeviceError("CleverTap ID - "+deviceID+" already exists. Unable to set custom CleverTap ID - " + cleverTapID);
            }
            return;
        }

        if(this.config.getEnableCustomCleverTapId()) {
            forceUpdateCustomCleverTapID(cleverTapID);
            return;
        }

        if(!this.config.isUseGoogleAdId()){
            generateDeviceID();
            return;
        }

        // fetch the googleAdID to generate GUID
        //has to be called on background thread
        Thread generateGUIDFromAdIDThread = new Thread(new Runnable() {
            @Override
            public void run() {
                fetchGoogleAdID();
                generateDeviceID();
                CleverTapAPI.instanceWithConfig(context,config).deviceIDCreated(getDeviceID());
            }
        });
        generateGUIDFromAdIDThread.start();
    }

    void forceUpdateCustomCleverTapID(String cleverTapID){
        if(Utils.validateCTID(cleverTapID)){
            getConfigLogger().info(config.getAccountId(), "Setting CleverTap ID to custom CleverTap ID : " + cleverTapID);
            forceUpdateDeviceId(Constants.CUSTOM_CLEVERTAP_ID_PREFIX+cleverTapID);
        }else {
            setOrGenerateFallbackDeviceID();
            removeDeviceID();
            getConfigLogger().info(config.getAccountId(),"Attempted to set invalid custom CleverTap ID - "+cleverTapID+", falling back to default error CleverTap ID - "+getFallBackDeviceID());
            recordDeviceError("Attempted to set invalid custom CleverTap ID - "+cleverTapID+", falling back to default error CleverTap ID - "+getFallBackDeviceID());
        }
    }

    private void recordDeviceError(String errorDescription){
        ValidationResult validationResult = new ValidationResult();
        validationResult.setErrorCode(514);
        validationResult.setErrorDesc(errorDescription);
        validationResults.add(validationResult);
    }

    boolean isErrorDeviceId(){
        return getDeviceID() != null && getDeviceID().startsWith(Constants.ERROR_PROFILE_PREFIX);
    }

    private synchronized void fetchGoogleAdID() {
        if(getGoogleAdID() == null && !adIdRun) {
            String advertisingID = null;
            try {
                adIdRun = true;
                Class adIdClient = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                // noinspection unchecked
                Method getAdInfo = adIdClient.getMethod("getAdvertisingIdInfo", Context.class);
                Object adInfo = getAdInfo.invoke(null, context);
                Method isLimitAdTracking = adInfo.getClass().getMethod("isLimitAdTrackingEnabled");
                Boolean limitedAdTracking = (Boolean) isLimitAdTracking.invoke(adInfo);
                synchronized (adIDLock) {
                    limitAdTracking = limitedAdTracking != null && limitedAdTracking;
                    if(limitAdTracking)
                        return;
                }
                Method getAdId = adInfo.getClass().getMethod("getId");
                advertisingID = (String) getAdId.invoke(adInfo);
            } catch (Throwable t) {
                if(t.getCause() != null) {
                    getConfigLogger().verbose(config.getAccountId(), "Failed to get Advertising ID: " + t.toString() + t.getCause().toString());
                }else{
                    getConfigLogger().verbose(config.getAccountId(), "Failed to get Advertising ID: " + t.toString());
                }
            }
            if (advertisingID != null && advertisingID.trim().length() > 2) {
                synchronized (adIDLock) {
                    googleAdID = advertisingID.replace("-", "");
                }
            }
        }
    }

    private synchronized void setOrGenerateFallbackDeviceID(){
        if(getFallBackDeviceID() == null) {
            synchronized (deviceIDLock) {
                String fallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString().replace("-", "");
                if (fallbackDeviceID.trim().length() > 2) {
                    updateFallbackID(fallbackDeviceID);
                } else {
                    getConfigLogger().verbose(this.config.getAccountId(), "Unable to generate fallback error device ID");
                }
            }
        }
    }

    private synchronized void generateDeviceID() {
        String generatedDeviceID;
        String adId = getGoogleAdID();
        if (adId != null) {
            generatedDeviceID = Constants.GUID_PREFIX_GOOGLE_AD_ID + adId;
        }else{
            synchronized (deviceIDLock) {
                generatedDeviceID = generateGUID();
            }
        }
        forceUpdateDeviceId(generatedDeviceID);
    }

    String getAttributionID() {
        return getDeviceID();
    }

    String getDeviceID() {
        return _getDeviceID() != null ? _getDeviceID() : getFallBackDeviceID();
    }

    private String _getDeviceID(){
        synchronized (deviceIDLock) {
            if (this.config.isDefaultInstance()) {
                String _new = StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
                return _new != null ? _new : StorageHelper.getString(this.context, Constants.DEVICE_ID_TAG, null);
            } else {
                return StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
            }
        }
    }

    private void removeDeviceID(){
        StorageHelper.removeString(this.context, getDeviceIdStorageKey());
    }

    private String getFallBackDeviceID(){
        return StorageHelper.getString(this.context, getFallbackIdStorageKey(), null);
    }

    private String generateGUID() {
        return GUID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    void forceNewDeviceID() {
        String deviceID = generateGUID();
        forceUpdateDeviceId(deviceID);
    }

    private String getDeviceIdStorageKey() {
        return Constants.DEVICE_ID_TAG+":"+this.config.getAccountId();
    }

    private String getFallbackIdStorageKey(){
        return Constants.FALLBACK_ID_TAG +":"+this.config.getAccountId();
    }

    private void updateFallbackID(String fallbackId){
        getConfigLogger().verbose(this.config.getAccountId(),"Updating the fallback id - " + fallbackId);
        StorageHelper.putString(context, getFallbackIdStorageKey(), fallbackId);
    }

    /**
     * Force updates the device ID, with the ID specified.
     * <p>
     * This is used internally by the SDK, there is no need to call this explicitly.
     * </p>
     *
     * @param id      The new device ID
     */
    @SuppressLint("CommitPrefEdits")
    void forceUpdateDeviceId(String id) {
        getConfigLogger().verbose(this.config.getAccountId(),"Force updating the device ID to " + id);
        synchronized (deviceIDLock) {
            StorageHelper.putString(context, getDeviceIdStorageKey(), id);
        }
    }
    /**
     * Tests whether a particular permission is available or not.
     *
     * @param context    The Android {@link Context}
     * @param permission The fully qualified Android permission name
     */
    @SuppressWarnings("SameParameterValue")
    boolean testPermission(final Context context, String permission) {
        this.context = context;
        return hasPermission(context, permission);
    }

    @SuppressWarnings("WeakerAccess")
    static boolean hasPermission(final Context context, String permission) {
        try {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the integer identifier for the default app icon.
     *
     * @param context The Android context
     * @return The integer identifier for the image resource
     */
    static int getAppIconAsIntId(final Context context) {
        ApplicationInfo ai = context.getApplicationInfo();
        return ai.icon;
    }

    /**
     * Push Service Detection Handling
     */

    private static ArrayList<PushType> enabledPushTypes = null;
    private static Boolean isFirebasePresent = null;
    private static Boolean isXiaomiPresent = null;
    private static Boolean isBaiduPresent = null;
    private static Boolean isHuaweiPresent = null;
    private static final String FIREBASE_CLASS_NAME = "com.google.firebase.messaging.FirebaseMessaging";
    private static final String XIAOMI_CLASS_NAME = "com.xiaomi.mipush.sdk.MiPushClient";
    private static final String BAIDU_CLASS_NAME = "com.baidu.android.pushservice.PushMessageReceiver";
    private static final String HUAWEI_CLASS_NAME = "com.huawei.hms.push.HmsMessaging";

    private boolean isFCMAvailable() {
        if (isFirebasePresent == null) {
            try {
                Class.forName(FIREBASE_CLASS_NAME);
                isFirebasePresent = true;
                getConfigLogger().verbose("FCM installed");
            } catch (ClassNotFoundException e) {
                isFirebasePresent = false;
                Logger.d("FCM unavailable, will be unable to request FCM token");
            }
        }
        return isFirebasePresent;
    }

    private boolean isXiaomiAvailable(){
        if (isXiaomiPresent == null) {
            try {
                Class.forName(XIAOMI_CLASS_NAME);
                isXiaomiPresent = true;
                getConfigLogger().verbose("Xiaomi Push installed");
            } catch (ClassNotFoundException e) {
                isXiaomiPresent = false;
                Logger.d("Xiaomi Push unavailable, will be unable to request Xiaomi Push token");
            }
        }
        return isXiaomiPresent;
    }

    private boolean isBaiduAvailable(){
        if (isBaiduPresent == null) {
            try {
                Class.forName(BAIDU_CLASS_NAME);
                isBaiduPresent = true;
                getConfigLogger().verbose("Baidu Push installed");
            } catch (ClassNotFoundException e) {
                isBaiduPresent = false;
                Logger.d("Baidu Push unavailable, will be unable to request Baidu Push token");
            }
        }
        return isBaiduPresent;
    }

    private boolean isHuaweiAvailable(){
        if (isHuaweiPresent == null) {
            try {
                Class.forName(HUAWEI_CLASS_NAME);
                isHuaweiPresent = true;
                getConfigLogger().verbose("Huawei Push installed");
            } catch (ClassNotFoundException e) {
                isHuaweiPresent = false;
                Logger.d("Huawei Push unavailable, will be unable to request Huawei Push token");
            }
        }
        return isHuaweiPresent;
    }

    ArrayList<PushType> getEnabledPushTypes() {
        if (enabledPushTypes == null) {
            enabledPushTypes = new ArrayList<>();

            // only return fcm
            boolean fcmAvail = isFCMAvailable();
            if (fcmAvail) {
                enabledPushTypes.add(PushType.FCM);
            }
            boolean xiaomiAvail = isXiaomiAvailable();
            if(xiaomiAvail){
                enabledPushTypes.add(PushType.XPS);
            }
            boolean baiduAvail = isBaiduAvailable();
            if(baiduAvail){
                enabledPushTypes.add(PushType.BPS);
            }
            boolean huaweiAvail = isHuaweiAvailable();
            if(huaweiAvail){
                enabledPushTypes.add(PushType.HPS);
            }

        }
        return enabledPushTypes;
    }

    String getFCMSenderID(){
        return  ManifestInfo.getInstance(this.context).getFCMSenderId();
    }

    Boolean isWifiConnected() {
        Boolean ret = null;

        if (PackageManager.PERMISSION_GRANTED == context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager != null) {
                @SuppressLint("MissingPermission")
                NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
                ret = (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected());
            }
        }

        return ret;
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("MissingPermission")
    Boolean isBluetoothEnabled() {
        Boolean isBluetoothEnabled = null;
        try {
            PackageManager pm = context.getPackageManager();
            int hasBluetoothPermission = pm.checkPermission(Manifest.permission.BLUETOOTH,context.getPackageName());
            if(hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null) {
                    isBluetoothEnabled = bluetoothAdapter.isEnabled();
                }
            }
        } catch (Throwable e) {
            // do nothing since we don't have permissions
        }
        return isBluetoothEnabled;
    }

    String getVersionName(){
        return getDeviceCachedInfo().versionName;
    }

    int getBuild(){
        return getDeviceCachedInfo().build;
    }

    String getOsName(){
        return getDeviceCachedInfo().osName;
    }

    String getOsVersion(){
        return getDeviceCachedInfo().osVersion;
    }

    String getManufacturer(){
        return getDeviceCachedInfo().manufacturer;
    }

    String getModel(){
        return getDeviceCachedInfo().model;
    }

    String getCarrier(){
        return getDeviceCachedInfo().carrier;
    }

    String getNetworkType(){
        return getDeviceCachedInfo().networkType;
    }

    String getBluetoothVersion(){
        return getDeviceCachedInfo().bluetoothVersion;
    }

    String getCountryCode(){
        return getDeviceCachedInfo().countryCode;
    }

    int getSdkVersion(){
        return getDeviceCachedInfo().sdkVersion;
    }

    double getHeight(){
        return getDeviceCachedInfo().height;
    }

    double getWidth(){
        return getDeviceCachedInfo().width;
    }

    int getDPI(){
        return getDeviceCachedInfo().dpi;
    }

    int getHeightPixels(){
        return getDeviceCachedInfo().heightPixels;
    }

    int getWidthPixels(){
        return getDeviceCachedInfo().widthPixels;
    }

    boolean getNotificationsEnabledForUser(){
        return getDeviceCachedInfo().notificationsEnabled;
    }

    private class DeviceCachedInfo {

        private int build;
        private String versionName;
        private String osName;
        private String osVersion;
        private String manufacturer;
        private String model;
        private String carrier;
        private String networkType;
        private String bluetoothVersion;
        private String countryCode;
        private int sdkVersion;
        private double height;
        private int heightPixels;
        private double width;
        private int widthPixels;
        private int dpi;
        private boolean notificationsEnabled;

        DeviceCachedInfo() {
            versionName = getVersionName();
            osName = getOsName();
            osVersion = getOsVersion();
            manufacturer = getManufacturer();
            model = getModel();
            carrier = getCarrier();
            build = getBuild();
            networkType = getNetworkType();
            bluetoothVersion = getBluetoothVersion();
            countryCode = getCountryCode();
            sdkVersion = getSdkVersion();
            height = getHeight();
            heightPixels = getHeightPixels();
            width = getWidth();
            widthPixels = getWidthPixels();
            dpi = getDPI();
            notificationsEnabled = getNotificationEnabledForUser();
        }

        private String getVersionName() {
            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Logger.d("Unable to get app version");
            }
            return null;
        }

        private String getOsName() {
            return OS_NAME;
        }

        private String getOsVersion() {
            return Build.VERSION.RELEASE;
        }

        private String getManufacturer() {
            return Build.MANUFACTURER;
        }

        private String getModel() {
            String model = Build.MODEL;
            model = model.replace(getManufacturer(), "");
            return model;
        }

        private String getCarrier() {
            try {
                TelephonyManager manager = (TelephonyManager) context
                        .getSystemService(Context.TELEPHONY_SERVICE);
                if (manager != null) {
                    return manager.getNetworkOperatorName();
                }

            } catch (Exception e) {
                // Failed to get network operator name from network
            }
            return null;
        }

        private int getBuild(){
            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                Logger.d("Unable to get app build");
            }
            return 0;
        }

        private String getNetworkType() {
            TelephonyManager mTelephonyManager = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyManager == null) {
                return null;
            }
            int networkType = mTelephonyManager.getNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "4G";
                default:
                    return null;
            }
        }

        private String getBluetoothVersion() {
            String bluetoothVersion = "none";
            if(android.os.Build.VERSION.SDK_INT >= 18 &&
                    context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble";
            } else if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic";
            }

            return bluetoothVersion;
        }

        private String getCountryCode() {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    return tm.getSimCountryIso();
                }
            } catch (Throwable ignore) {
                return "";
            }
            return "";
        }

        private int getSdkVersion(){
            return BuildConfig.VERSION_CODE;
        }

        private int getHeightPixels(){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.heightPixels;
        }

        private double getHeight(){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0.0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            // Calculate the height in inches
            double rHeight = dm.heightPixels / dm.ydpi;
            return toTwoPlaces(rHeight);
        }

        private int getWidthPixels(){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.widthPixels;
        }

        private double getWidth(){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0.0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            // Calculate the width in inches
            double rWidth = dm.widthPixels / dm.xdpi;
            return toTwoPlaces(rWidth);

        }

        private int getDPI(){
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.densityDpi;
        }

        private double toTwoPlaces(double n) {
            double result = n * 100;
            result = Math.round(result);
            result = result / 100;
            return result;
        }

        private boolean getNotificationEnabledForUser(){
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
    }
}
