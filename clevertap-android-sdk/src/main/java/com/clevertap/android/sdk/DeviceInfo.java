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
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

class DeviceInfo {

    private Context context;
    private CleverTapInstanceConfig config;
    private static final String GUID_PREFIX = "__";
    //private String provisionalGUID = null;
    private final Object deviceIDLock = new Object();
    private final Object adIDLock = new Object();
    private String googleAdID = null;
    private boolean limitAdTracking = false;
    private boolean adIdRun = false;
    private static final String OS_NAME = "Android";
    private DeviceCachedInfo cachedInfo;
    private Handler handler;

    DeviceInfo(Context context, CleverTapInstanceConfig config, String cleverTapID) {
        this.context = context;
        this.config = config;
        this.handler = new Handler(Looper.myLooper());
        Runnable deviceCacheInfoRunnable = new Runnable() {
            @Override
            public void run() {
                getDeviceCachedInfo();  // put this here to avoid running on main thread
            }
        };
        handler.post(deviceCacheInfoRunnable);
        initDeviceID(cleverTapID);
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

    @SuppressWarnings({"WeakerAccess"})
    private void initDeviceID(String cleverTapID) {


        String deviceID = getDeviceID();
        if(deviceID != null && deviceID.trim().length() > 2){
            getConfigLogger().info(config.getAccountId(),"CleverTap ID already present for profile");
            if(cleverTapID != null) {
                getConfigLogger().info(config.getAccountId(),"Discarding custom CleverTap ID - " + cleverTapID);
            }
            return;
        }

        //Validate the provided Custom CTID
        boolean isCustomCTIDValid = false;
        if(ManifestInfo.getInstance(context).useCustomId()) {
            isCustomCTIDValid = Utils.validateCTID(cleverTapID);
        }

        //If validation is passed, update the GUID to provided custom CTID
        if(isCustomCTIDValid) {
            getConfigLogger().info(config.getAccountId(), "Updating CleverTap ID to given custom CleverTap ID : " + cleverTapID);
            forceUpdateDeviceId(Constants.CUSTOM_CLEVERTAP_ID_PREFIX+cleverTapID);
        } else {
            Runnable deviceIDRunnable = new Runnable() {
                @Override
                public void run() {
                // grab and cache the googleAdID in any event if available
                cacheGoogleAdID();
                //If Manifest flag is present then dont use Google Ad ID and create fallback ID
                if(ManifestInfo.getInstance(context).useCustomId()){
                    Logger.i("Since passed Custom CleverTap ID is not valid. Falling back to the error device id.");
                    if(getFallBackDeviceID() == null) {
                        Logger.i("CleverTap will create a fallback device ID");
                        generateFallbackDeviceID();
                    }else{
                        Logger.i("Fallback device ID "+ getFallBackDeviceID() +" already exists, will not create a new fallback device ID");
                        updateFallbackID(getFallBackDeviceID());
                    }
                }else {
                    //Generate Device ID
                    generateDeviceID();
                }
                }
            };
            handler.post(deviceIDRunnable);
        }
    }

    private synchronized void cacheGoogleAdID() {

        if(this.config.isUseGoogleAdId() && googleAdID == null && !adIdRun) {
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
                getConfigLogger().verbose(config.getAccountId(), "Failed to get Advertising ID: " + t.toString());
            }
            if (advertisingID != null && advertisingID.trim().length() > 2) {
                synchronized (adIDLock) {
                    googleAdID = advertisingID.replace("-", "");
                }
            }
        }
    }

    synchronized void generateFallbackDeviceID(){
        synchronized (deviceIDLock) {
            String fallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString().replace("-", "");
            if (fallbackDeviceID.trim().length() > 2) {
                updateFallbackID(fallbackDeviceID);
            } else {
                getConfigLogger().verbose(this.config.getAccountId(),"Unable to generate fallback error device ID");
            }
        }
    }

    private synchronized void generateDeviceID() {
        String generatedDeviceID;

        // try google ad id first
        // if no ad id then make provisional guid permanent
        if (googleAdID != null && ManifestInfo.getInstance(context).useGoogleAdId()) {
            synchronized (adIDLock) {
                generatedDeviceID = Constants.GUID_PREFIX_GOOGLE_AD_ID + googleAdID;
            }
        } else {
            synchronized (deviceIDLock) {
                generatedDeviceID = generateGUID();
                getConfigLogger().verbose(this.config.getAccountId(),"Made provisional ID permanent");
            }
        }

        if (generatedDeviceID.trim().length() > 2) {
            forceUpdateDeviceId(generatedDeviceID);
        } else {
            getConfigLogger().verbose(this.config.getAccountId(),"Unable to generate device ID");
        }
    }

    String getAttributionID() {
        String deviceID = getDeviceID();
        synchronized (deviceIDLock) {
            return (deviceID != null && deviceID.trim().length() > 2) ? deviceID : null;
        }
    }

    String getDeviceID() {
        synchronized (deviceIDLock) {
            if (this.config.isDefaultInstance()) {
                String _new = StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
                return _new != null ? _new : StorageHelper.getString(this.context, Constants.DEVICE_ID_TAG, null);
            } else {
                return StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
            }
        }
    }

    void removeDeviceID(){
        StorageHelper.removeString(this.context, getDeviceIdStorageKey());
    }

    String getFallBackDeviceID(){
        return StorageHelper.getString(this.context, getFallbackIdStorageKey(), null);
    }

    private String generateGUID() {
        return GUID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }


    String forceNewDeviceID() {
        String deviceID = generateGUID();
        return forceUpdateDeviceId(deviceID);
    }

    private String getDeviceIdStorageKey() {
        return Constants.DEVICE_ID_TAG+":"+this.config.getAccountId();
    }

    private String getFallbackIdStorageKey(){
        return Constants.FALLBACK_ID_TAG +":"+this.config.getAccountId();
    }

    String updateFallbackID(String fallbackId){
        getConfigLogger().verbose(this.config.getAccountId(),"Updating the fallback id - " + fallbackId);
        StorageHelper.putString(context, getFallbackIdStorageKey(), fallbackId);
        return fallbackId;
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
    String forceUpdateDeviceId(String id) {
        getConfigLogger().verbose(this.config.getAccountId(),"Force updating the device ID to " + id);
        synchronized (deviceIDLock) {
            StorageHelper.putString(context, getDeviceIdStorageKey(), id);
        }
        return id;
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
    private static Boolean areGoogleServicesAvailable = null;
    private static final String FIREBASE_CLASS_NAME = "com.google.firebase.messaging.FirebaseMessaging";

    private boolean isGCMAvailable() {
        if (getGCMSenderID() == null) {
           Logger.d("GCM Sender ID unknown, will be unable to request GCM token");
           return false;
        }
        if (!isGooglePlayServicesAvailable()) {
            Logger.d("Google Play Services unavailable, will be unable to request GCM token");
            return false;
        }
        return true;
    }

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

    ArrayList<PushType> getEnabledPushTypes() {
        if (enabledPushTypes == null) {
            enabledPushTypes = new ArrayList<>();

            // only return one of fcm and gcm , preferring fcm
            boolean fcmAvail = isFCMAvailable();
            if (fcmAvail) {
                enabledPushTypes.add(PushType.FCM);
            }

            if (!fcmAvail && isGCMAvailable()) {
                enabledPushTypes.add(PushType.GCM);
            }

        }
        return enabledPushTypes;
    }

    private Boolean isGooglePlayServicesAvailable() {
        if (areGoogleServicesAvailable == null) {
            try {
                Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
                areGoogleServicesAvailable = true;
                Logger.d("Google Play services available");
            } catch (Throwable t) {
                Logger.v("Error checking for Google Play Services: " + t.getMessage());
                areGoogleServicesAvailable = false;
            }
        }
        return areGoogleServicesAvailable;
    }


    String getGCMSenderID() {
        return ManifestInfo.getInstance(this.context).getGCMSenderId();
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

    boolean getNotificationsEnabledForUser(){
        return getDeviceCachedInfo().notificationsEnabled;
    }

    private class DeviceCachedInfo{

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
        private double width;
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
            width = getWidth();
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
