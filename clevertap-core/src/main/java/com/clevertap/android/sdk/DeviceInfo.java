package com.clevertap.android.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.core.app.NotificationManagerCompat;
import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.OnSuccessListener;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.CTJsonConverter;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultFactory;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.json.JSONObject;

@RestrictTo(Scope.LIBRARY)
public class DeviceInfo {

    private class DeviceCachedInfo {

        private final String bluetoothVersion;

        private final int build;

        private final String carrier;

        private final String countryCode;

        private final int dpi;

        private final double height;

        private final int heightPixels;

        private final String manufacturer;

        private final String model;

        private final String networkType;

        private final boolean notificationsEnabled;

        private final String osName;

        private final String osVersion;

        private final int sdkVersion;

        private final String versionName;

        private final double width;

        private final int widthPixels;

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

        private String getBluetoothVersion() {
            String bluetoothVersion = "none";
            if (android.os.Build.VERSION.SDK_INT >= 18 &&
                    context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble";
            } else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic";
            }

            return bluetoothVersion;
        }

        private int getBuild() {
            PackageInfo packageInfo;
            try {
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                Logger.d("Unable to get app build");
            }
            return 0;
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

        private int getDPI() {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.densityDpi;
        }

        private double getHeight() {
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

        private int getHeightPixels() {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.heightPixels;
        }

        private String getManufacturer() {
            return Build.MANUFACTURER;
        }

        private String getModel() {
            String model = Build.MODEL;
            model = model.replace(getManufacturer(), "");
            return model;
        }

        @SuppressLint("MissingPermission")
        private String getNetworkType() {
            return Utils.getDeviceNetworkType(context);
        }

        private boolean getNotificationEnabledForUser() {
            boolean isNotificationEnabled = true;
            try {
                isNotificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
            } catch (RuntimeException rte) {
                Logger.d("Runtime exception caused when checking whether notification are enabled or not");
                rte.printStackTrace();
            }
            return isNotificationEnabled;//returns true if any exception is raised.
        }

        private String getOsName() {
            return OS_NAME;
        }

        private String getOsVersion() {
            return Build.VERSION.RELEASE;
        }

        private int getSdkVersion() {
            return BuildConfig.VERSION_CODE;
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

        private double getWidth() {
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

        private int getWidthPixels() {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return 0;
            }
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);
            return dm.widthPixels;
        }

        private double toTwoPlaces(double n) {
            double result = n * 100;
            result = Math.round(result);
            result = result / 100;
            return result;
        }
    }

    /**
     * Type of a device with below possible values<br>
     * <li>{@link DeviceInfo#SMART_PHONE}
     * <li>{@link DeviceInfo#TABLET}
     * <li>{@link DeviceInfo#TV}
     * <li>{@link DeviceInfo#UNKNOWN}
     * <li>{@link DeviceInfo#NULL}
     */
    @IntDef({SMART_PHONE, TABLET, TV, UNKNOWN, NULL})
    @Retention(RetentionPolicy.SOURCE)
    @interface DeviceType {

    }

    private static final String GUID_PREFIX = "__";

    private static final String OS_NAME = "Android";

    /**
     * Device is a smart phone
     */
    static final int SMART_PHONE = 1;

    /**
     * Device is a tablet
     */
    static final int TABLET = 2;

    /**
     * Device is a television
     */
    static final int TV = 3;

    /**
     * Device type is not known
     */
    static final int UNKNOWN = 0;

    /**
     * Initial state of device type before determining
     */
    static final int NULL = -1;

    @DeviceType
    static int sDeviceType = NULL;

    private final Object adIDLock = new Object();

    private boolean adIdRun = false;

    private DeviceCachedInfo cachedInfo;

    private final CleverTapInstanceConfig config;

    private final Context context;

    private final Object deviceIDLock = new Object();

    private String googleAdID = null;

    private String library;

    private boolean limitAdTracking = false;

    private final ArrayList<ValidationResult> validationResults = new ArrayList<>();

    private boolean enableNetworkInfoReporting = false;

    private final CoreMetaData mCoreMetaData;

    DeviceInfo(Context context, CleverTapInstanceConfig config, String cleverTapID,
            CoreMetaData coreMetaData) {
        this.context = context;
        this.config = config;
        this.library = null;
        mCoreMetaData = coreMetaData;
        onInitDeviceInfo(cleverTapID);
        Log.v("iotask", "DeviceInfo() called");
    }

    void onInitDeviceInfo(final String cleverTapID) {
        Task<Void> taskDeviceCachedInfo = CTExecutorFactory.executors(config).ioTask();
        taskDeviceCachedInfo.execute("getDeviceCachedInfo", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getDeviceCachedInfo();
                return null;
            }
        });

        Task<Void> task = CTExecutorFactory.executors(config).ioTask();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            // callback on main thread
            @Override
            public void onSuccess(final Void aVoid) {
                getConfigLogger().verbose(config.getAccountId(),
                        "DeviceID initialized successfully!" + Thread.currentThread());
                // No need to put getDeviceID() on background thread because prefs already loaded
                CleverTapAPI.instanceWithConfig(context, config).deviceIDCreated(getDeviceID());
            }
        });
        task.execute("initDeviceID", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                initDeviceID(cleverTapID);
                return null;
            }
        });

    }

    public boolean isErrorDeviceId() {
        return getDeviceID() != null && getDeviceID().startsWith(Constants.ERROR_PROFILE_PREFIX);
    }

    public void forceNewDeviceID() {
        String deviceID = generateGUID();
        forceUpdateDeviceId(deviceID);
    }

    public void forceUpdateCustomCleverTapID(String cleverTapID) {
        if (Utils.validateCTID(cleverTapID)) {
            getConfigLogger()
                    .info(config.getAccountId(), "Setting CleverTap ID to custom CleverTap ID : " + cleverTapID);
            forceUpdateDeviceId(Constants.CUSTOM_CLEVERTAP_ID_PREFIX + cleverTapID);
        } else {
            setOrGenerateFallbackDeviceID();
            removeDeviceID();
            String error = recordDeviceError(Constants.INVALID_CT_CUSTOM_ID, cleverTapID, getFallBackDeviceID());
            getConfigLogger().info(config.getAccountId(), error);
        }
    }

    /**
     * Force updates the device ID, with the ID specified.
     * <p>
     * This is used internally by the SDK, there is no need to call this explicitly.
     * </p>
     *
     * @param id The new device ID
     */
    @SuppressLint("CommitPrefEdits")
    public void forceUpdateDeviceId(String id) {
        getConfigLogger().verbose(this.config.getAccountId(), "Force updating the device ID to " + id);
        synchronized (deviceIDLock) {
            StorageHelper.putString(context, getDeviceIdStorageKey(), id);
        }
    }

    String getAttributionID() {
        return getDeviceID();
    }

    /**
     * Determines if a device is tablet, smart phone or TV
     *
     * @param context context
     * @return one of the possible value of {@link DeviceType}
     */
    @DeviceType
    public static int getDeviceType(final Context context) {

        if (sDeviceType == NULL) {

            try {
                UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                    sDeviceType = TV;
                    return sDeviceType;
                }
            } catch (Exception e) {
                //uiModeManager or context is null
                Logger.d("Failed to decide whether device is a TV!");
                e.printStackTrace();
            }

            try {
                sDeviceType = context.getResources().getBoolean(R.bool.ctIsTablet) ? TABLET : SMART_PHONE;
            } catch (Exception e) {
                // resource not found or context is null
                Logger.d("Failed to decide whether device is a smart phone or tablet!");
                e.printStackTrace();
                sDeviceType = UNKNOWN;
            }

        }
        return sDeviceType;
    }

    //Event
    public JSONObject getAppLaunchedFields() {

        try {
            boolean deviceIsMultiUser = false;
            if (getGoogleAdID() != null) {
                deviceIsMultiUser = new LoginInfoProvider(context, config, this).deviceIsMultiUser();
            }
            return CTJsonConverter.from(this, mCoreMetaData.getLocationFromUser(), enableNetworkInfoReporting,
                    deviceIsMultiUser);
        } catch (Throwable t) {
            config.getLogger().verbose(config.getAccountId(), "Failed to construct App Launched event", t);
            return new JSONObject();
        }
    }

    public String getBluetoothVersion() {
        return getDeviceCachedInfo().bluetoothVersion;
    }

    public int getBuild() {
        return getDeviceCachedInfo().build;
    }

    public String getCarrier() {
        return getDeviceCachedInfo().carrier;
    }

    public Context getContext() {
        return context;
    }

    public String getDeviceID() {
        return _getDeviceID() != null ? _getDeviceID() : getFallBackDeviceID();
    }

    public String getCountryCode() {
        return getDeviceCachedInfo().countryCode;
    }

    public int getDPI() {
        return getDeviceCachedInfo().dpi;
    }

    public String getGoogleAdID() {
        synchronized (adIDLock) {
            return googleAdID;
        }
    }

    public double getHeight() {
        return getDeviceCachedInfo().height;
    }

    public String getLibrary() {
        return library;
    }

    void setLibrary(String library) {
        this.library = library;
    }

    public String getManufacturer() {
        return getDeviceCachedInfo().manufacturer;
    }

    public String getModel() {
        return getDeviceCachedInfo().model;
    }

    public String getNetworkType() {
        return getDeviceCachedInfo().networkType;
    }

    public boolean getNotificationsEnabledForUser() {
        return getDeviceCachedInfo().notificationsEnabled;
    }

    public String getOsName() {
        return getDeviceCachedInfo().osName;
    }

    public String getOsVersion() {
        return getDeviceCachedInfo().osVersion;
    }

    public ArrayList<ValidationResult> getValidationResults() {
        // noinspection unchecked
        ArrayList<ValidationResult> tempValidationResults = (ArrayList<ValidationResult>) validationResults.clone();
        validationResults.clear();
        return tempValidationResults;
    }

    public int getSdkVersion() {
        return getDeviceCachedInfo().sdkVersion;
    }

    public String getVersionName() {
        return getDeviceCachedInfo().versionName;
    }

    public double getWidth() {
        return getDeviceCachedInfo().width;
    }

    @SuppressLint("MissingPermission")
    @SuppressWarnings("MissingPermission")
    public Boolean isBluetoothEnabled() {
        Boolean isBluetoothEnabled = null;
        try {
            PackageManager pm = context.getPackageManager();
            int hasBluetoothPermission = pm.checkPermission(Manifest.permission.BLUETOOTH, context.getPackageName());
            if (hasBluetoothPermission == PackageManager.PERMISSION_GRANTED) {
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

    public boolean isLimitAdTrackingEnabled() {
        synchronized (adIDLock) {
            return limitAdTracking;
        }
    }

    public Boolean isWifiConnected() {
        Boolean ret = null;

        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager != null) {
                @SuppressLint("MissingPermission")
                NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
                ret = (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo
                        .isConnected());
            }
        }

        return ret;
    }

    private String _getDeviceID() {
        synchronized (deviceIDLock) {
            if (this.config.isDefaultInstance()) {
                String _new = StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
                return _new != null ? _new : StorageHelper.getString(this.context, Constants.DEVICE_ID_TAG, null);
            } else {
                return StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);
            }
        }
    }

    private synchronized void fetchGoogleAdID() {
        Log.v("initDeviceID()", "fetchGoogleAdID() called!");
        if (getGoogleAdID() == null && !adIdRun) {
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
                    Log.v("initDeviceID()", "limitAdTracking = " + limitAdTracking);
                    if (limitAdTracking) {
                        return;
                    }
                }
                Method getAdId = adInfo.getClass().getMethod("getId");
                advertisingID = (String) getAdId.invoke(adInfo);
            } catch (Throwable t) {
                if (t.getCause() != null) {
                    getConfigLogger().verbose(config.getAccountId(),
                            "Failed to get Advertising ID: " + t.toString() + t.getCause().toString());
                } else {
                    getConfigLogger().verbose(config.getAccountId(), "Failed to get Advertising ID: " + t.toString());
                }
            }
            if (advertisingID != null && advertisingID.trim().length() > 2) {
                synchronized (adIDLock) {
                    googleAdID = advertisingID.replace("-", "");
                }
            }

            Log.v("initDeviceID()", "fetchGoogleAdID() done executing!");
        }
    }

    private synchronized void generateDeviceID() {
        Log.v("initDeviceID()", "generateDeviceID() called!");
        String generatedDeviceID;
        String adId = getGoogleAdID();
        if (adId != null) {
            generatedDeviceID = Constants.GUID_PREFIX_GOOGLE_AD_ID + adId;
        } else {
            synchronized (deviceIDLock) {
                generatedDeviceID = generateGUID();
            }
        }
        forceUpdateDeviceId(generatedDeviceID);
        Log.v("initDeviceID()", "generateDeviceID() done executing!");
    }

    private String generateGUID() {
        return GUID_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    private Logger getConfigLogger() {
        return this.config.getLogger();
    }

    private DeviceCachedInfo getDeviceCachedInfo() {
        if (cachedInfo == null) {
            cachedInfo = new DeviceCachedInfo();
        }
        return cachedInfo;
    }

    private String getDeviceIdStorageKey() {
        return Constants.DEVICE_ID_TAG + ":" + this.config.getAccountId();
    }

    private String getFallBackDeviceID() {
        return StorageHelper.getString(this.context, getFallbackIdStorageKey(), null);
    }

    private String getFallbackIdStorageKey() {
        return Constants.FALLBACK_ID_TAG + ":" + this.config.getAccountId();
    }

    private void initDeviceID(String cleverTapID) {
        Log.v("initDeviceID()", "Called initDeviceID()");
        //Show logging as per Manifest flag
        if (config.getEnableCustomCleverTapId()) {
            if (cleverTapID == null) {
                String error = recordDeviceError(Constants.USE_CUSTOM_ID_FALLBACK);
                config.getLogger().info(error);
            }
        } else {
            if (cleverTapID != null) {
                String error = recordDeviceError(Constants.USE_CUSTOM_ID_MISSING_IN_MANIFEST);
                config.getLogger().info(error);
            }
        }

        Log.v("initDeviceID()", "Calling _getDeviceID");
        String deviceID = _getDeviceID();
        Log.v("initDeviceID()", "Called _getDeviceID");
        if (deviceID != null && deviceID.trim().length() > 2) {
            getConfigLogger().verbose(config.getAccountId(), "CleverTap ID already present for profile");
            if (cleverTapID != null) {
                String error = recordDeviceError(Constants.UNABLE_TO_SET_CT_CUSTOM_ID, deviceID, cleverTapID);
                getConfigLogger().info(config.getAccountId(), error);
            }
            return;
        }

        if (this.config.getEnableCustomCleverTapId()) {
            forceUpdateCustomCleverTapID(cleverTapID);
            return;
        }

        if (!this.config.isUseGoogleAdId()) {
            Log.v("initDeviceID()", "Calling generateDeviceID()");
            generateDeviceID();
            Log.v("initDeviceID()", "Called generateDeviceID()");
            return;
        }

        // fetch the googleAdID to generate GUID
        //has to be called on background thread
        fetchGoogleAdID();
        generateDeviceID();

        Log.v("initDeviceID()", "initDeviceID() done executing!");
    }

    private String recordDeviceError(int messageCode, String... varargs) {
        ValidationResult validationResult = ValidationResultFactory.create(514, messageCode, varargs);
        validationResults.add(validationResult);
        return validationResult.getErrorDesc();
    }

    private void removeDeviceID() {
        StorageHelper.remove(this.context, getDeviceIdStorageKey());
    }

    private synchronized void setOrGenerateFallbackDeviceID() {
        if (getFallBackDeviceID() == null) {
            synchronized (deviceIDLock) {
                String fallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString()
                        .replace("-", "");
                if (fallbackDeviceID.trim().length() > 2) {
                    updateFallbackID(fallbackDeviceID);
                } else {
                    getConfigLogger()
                            .verbose(this.config.getAccountId(), "Unable to generate fallback error device ID");
                }
            }
        }
    }

    private void updateFallbackID(String fallbackId) {
        getConfigLogger().verbose(this.config.getAccountId(), "Updating the fallback id - " + fallbackId);
        StorageHelper.putString(context, getFallbackIdStorageKey(), fallbackId);
    }

    /**
     * Returns the integer identifier for the default app icon.
     *
     * @param context The Android context
     * @return The integer identifier for the image resource
     */
    public static int getAppIconAsIntId(final Context context) {
        ApplicationInfo ai = context.getApplicationInfo();
        return ai.icon;
    }

    int getHeightPixels() {
        return getDeviceCachedInfo().heightPixels;
    }

    void enableDeviceNetworkInfoReporting(boolean value) {
        enableNetworkInfoReporting = value;
        StorageHelper.putBoolean(context, StorageHelper.storageKeyWithSuffix(config, Constants.NETWORK_INFO),
                enableNetworkInfoReporting);
        config.getLogger()
                .verbose(config.getAccountId(),
                        "Device Network Information reporting set to " + enableNetworkInfoReporting);
    }

    void setDeviceNetworkInfoReportingFromStorage() {
        boolean enabled = StorageHelper.getBooleanFromPrefs(context, config, Constants.NETWORK_INFO);
        config.getLogger()
                .verbose(config.getAccountId(),
                        "Setting device network info reporting state from storage to " + enabled);
        enableNetworkInfoReporting = enabled;
    }

    int getWidthPixels() {
        return getDeviceCachedInfo().widthPixels;
    }

    public void setCurrentUserOptOutStateFromStorage() {
        String key = optOutKey();
        if (key == null) {
            config.getLogger().verbose(config.getAccountId(),
                    "Unable to set current user OptOut state from storage: storage key is null");
            return;
        }
        boolean storedOptOut = StorageHelper.getBooleanFromPrefs(context, config, key);
        mCoreMetaData.setCurrentUserOptedOut(storedOptOut);
        config.getLogger().verbose(config.getAccountId(),
                "Set current user OptOut state from storage to: " + storedOptOut + " for key: " + key);
    }

    String optOutKey() {
        String guid = getDeviceID();
        if (guid == null) {
            return null;
        }
        return "OptOut:" + guid;
    }
}
