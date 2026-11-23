package com.clevertap.android.sdk;

import static android.content.Context.USAGE_STATS_SERVICE;
import static android.view.Display.DEFAULT_DISPLAY;
import static com.clevertap.android.sdk.inapp.InAppController.LOCAL_INAPP_COUNT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.UiModeManager;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.WorkerThread;

import com.clevertap.android.sdk.login.LoginInfoProvider;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import com.clevertap.android.sdk.utils.CTJsonConverter;
import com.clevertap.android.sdk.validation.ValidationError;
import com.clevertap.android.sdk.validation.ValidationResult;
import com.clevertap.android.sdk.validation.ValidationResultFactory;
import com.clevertap.android.sdk.network.NetworkMonitor;


import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;

@RestrictTo(Scope.LIBRARY)
public class DeviceInfo {

    private static final String TAG = "DeviceInfo";

    private class DeviceCachedInfo {


        private final static String STANDBY_BUCKET_ACTIVE = "active";
        private final static String STANDBY_BUCKET_FREQUENT = "frequent";
        private final static String STANDBY_BUCKET_RARE = "rare";
        private final static String STANDBY_BUCKET_RESTRICTED = "restricted";
        private final static String STANDBY_BUCKET_WORKING_SET = "working_set";

        private final String bluetoothVersion;

        private final int build;

        private final String carrier;

        private final String countryCode;


        private final int dpi;

        private final double height;

        private final String manufacturer;

        private final String model;

        private final String osName;

        private final String osVersion;

        private final int sdkVersion;

        private final String versionName;

        private final double width;

        private final String appBucket;

        private int localInAppCount;

        private final String locale;

        DeviceCachedInfo() {
            // One read per system service. They use only DeviceInfo fields, not this object's, so starting them
            // here is safe.
            DeviceRead<PackageValues> packageRead = DeviceStateKt.startExistingRead("package info", this::readPackageValues);
            DeviceRead<TelephonyValues> telephonyRead = DeviceStateKt.startExistingRead("telephony", this::readTelephonyValues);
            DeviceRead<WindowSize> windowRead = DeviceStateKt.startExistingRead("window size", this::getWindowSizeData);
            DeviceRead<String> bucketRead = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? DeviceStateKt.startExistingRead("app standby bucket", this::getAppBucket)
                    : null;

            osName = getOsName();
            osVersion = getOsVersion();
            manufacturer = getManufacturer();
            model = getModel();
            sdkVersion = getSdkVersion();
            localInAppCount = getLocalInAppCountFromPreference();
            locale = getDeviceLocale();

            PackageValues packageValues = packageRead.get();
            versionName = packageValues != null ? packageValues.versionName : null;
            build = packageValues != null ? packageValues.build : 0;
            bluetoothVersion = packageValues != null ? packageValues.bluetoothVersion : null;

            TelephonyValues telephonyValues = telephonyRead.get();
            carrier = telephonyValues != null ? telephonyValues.carrier : null;
            countryCode = telephonyValues != null && telephonyValues.countryCode != null
                    ? telephonyValues.countryCode : "";

            WindowSize windowSize = windowRead.get();
            if (windowSize == null) {
                windowSize = new WindowSize(0, 0, 0);
            }
            width = windowSize.width;
            height = windowSize.height;
            dpi = windowSize.localDpi;

            appBucket = bucketRead != null ? bucketRead.get() : null;
        }

        // One getPackageInfo for both version values, plus the feature lookups, all on PackageManager.
        private PackageValues readPackageValues() {
            PackageInfo packageInfo = DeviceStateKt.catchOrNull("package info", this::getPackageInfo);
            String bluetooth = DeviceStateKt.catchOrNull("bluetooth version", this::getBluetoothVersion);
            return new PackageValues(
                    packageInfo != null ? packageInfo.versionName : null,
                    packageInfo != null ? packageInfo.versionCode : 0,
                    bluetooth
            );
        }

        private TelephonyValues readTelephonyValues() {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (manager == null) {
                return null;
            }
            return new TelephonyValues(
                    DeviceStateKt.catchOrNull("carrier", manager::getNetworkOperatorName),
                    DeviceStateKt.catchOrNull("sim country", manager::getSimCountryIso)
            );
        }

        @NonNull
        private WindowSize getWindowSizeData() {

            WindowManager wm = getWindowManager();

            if (wm == null) {
                getConfigLogger().verbose(config.getAccountId(),
                        "WindowManager is null, returning zero dimension for width/height");
                return new WindowSize(0, 0, 0);
            }

            int localWidth;
            int localHeight;
            float localXDpi;
            float localYDpi;
            int localDpi;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
                Configuration configuration = context.getResources().getConfiguration();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemGestures());

                localWidth = windowMetrics.getBounds().width() -
                        insets.right - insets.left;
                localHeight = windowMetrics.getBounds().height() -
                        insets.top - insets.bottom;

                localXDpi = configuration.densityDpi;
                localYDpi = configuration.densityDpi;
                localDpi = configuration.densityDpi;

            } else {
                DisplayMetrics dm = new DisplayMetrics();
                wm.getDefaultDisplay().getMetrics(dm);

                localWidth = dm.widthPixels;
                localHeight = dm.heightPixels;
                localXDpi = dm.xdpi;
                localYDpi = dm.ydpi;
                localDpi = dm.densityDpi;
            }

            // legacy code -> conversion to double is copied
            double doubleWidth = localWidth / localXDpi;
            double doubleHeight = localHeight / localYDpi;
            return new WindowSize(
                    localDpi,
                    toTwoPlaces(doubleWidth),
                    toTwoPlaces(doubleHeight)
            );
        }

        private String getBluetoothVersion() {
            String bluetoothVersion = "none";
            if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                bluetoothVersion = "ble";
            } else if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
                bluetoothVersion = "classic";
            }

            return bluetoothVersion;
        }

        private PackageInfo getPackageInfo() {
            try {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                getConfigLogger().debug(config.getAccountId(), "Unable to get app version and build", e);
                return null;
            }
        }

        private String getManufacturer() {
            return Build.MANUFACTURER;
        }

        /**
         * This method is used for devices above API 28
         * This method gets the standby values for app.Standby buckets are divided into the following:-
         * STANDBY_BUCKET_ACTIVE - The app was used very recently, currently in use or likely to be used very soon.
         * STANDBY_BUCKET_FREQUENT - The app was used in the last few days and/or likely to be used in the next few days.
         * STANDBY_BUCKET_RARE - The app has not be used for several days and/or is unlikely to be used for several days.
         * STANDBY_BUCKET_RESTRICTED - The app has not be used for several days, is unlikely to be used for several days, and has
         * been misbehaving in some manner.
         * STANDBY_BUCKET_WORKING_SET - The app was used recently and/or likely to be used in the next few hours.
         *
         * @return one of the possible String value of AppStandbyBucket(). If no AppBucket info is found,
         * returns empty String
         */
        @RequiresApi(api = Build.VERSION_CODES.P)
        private String getAppBucket() {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);
            switch (usm.getAppStandbyBucket()) {
                case UsageStatsManager.STANDBY_BUCKET_ACTIVE:
                    return STANDBY_BUCKET_ACTIVE;
                case UsageStatsManager.STANDBY_BUCKET_FREQUENT:
                    return STANDBY_BUCKET_FREQUENT;
                case UsageStatsManager.STANDBY_BUCKET_RARE:
                    return STANDBY_BUCKET_RARE;
                case UsageStatsManager.STANDBY_BUCKET_RESTRICTED:
                    return STANDBY_BUCKET_RESTRICTED;
                case UsageStatsManager.STANDBY_BUCKET_WORKING_SET:
                    return STANDBY_BUCKET_WORKING_SET;
                default:
                    return "";
            }
        }

        private String getModel() {
            String model = Build.MODEL;
            model = model.replace(getManufacturer(), "");
            return model;
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

        private String getDeviceLocale() {
            String language = Locale.getDefault().getLanguage();
            if ("".equals(language)) {
                language = "xx";
            }
            String country = Locale.getDefault().getCountry();
            if ("".equals(country)) {
                country = "XX";
            }
            return language + "_" + country;
        }

        private double toTwoPlaces(double n) {
            double result = n * 100;
            result = Math.round(result);
            result = result / 100;
            return result;
        }
    }

    /**
     * A simple class representing the size of a window.
     * It stores the local DPI (dots per inch) along with the window's width and height.
     */
    private static class WindowSize {
        public final int localDpi;
        public final double width;
        public final double height;

        public WindowSize(int localDpi, double width, double height) {
            this.localDpi = localDpi;
            this.width = width;
            this.height = height;
        }
    }

    private static class PackageValues {
        final String versionName;
        final int build;
        final String bluetoothVersion;

        PackageValues(String versionName, int build, String bluetoothVersion) {
            this.versionName = versionName;
            this.build = build;
            this.bluetoothVersion = bluetoothVersion;
        }
    }

    private static class TelephonyValues {
        final String carrier;
        final String countryCode;

        TelephonyValues(String carrier, String countryCode) {
            this.carrier = carrier;
            this.countryCode = countryCode;
        }
    }

    private WindowManager getWindowManager() {
        if (context == null) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                DisplayManager displayManager = context.getSystemService(DisplayManager.class);
                if (displayManager != null) {
                    Display primaryDisplay = displayManager.getDisplay(DEFAULT_DISPLAY);
                    if (primaryDisplay != null) {
                        Context windowContext = context.createDisplayContext(primaryDisplay)
                                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION, null);
                        return windowContext.getSystemService(WindowManager.class);
                    }
                }
            } catch (Exception e) {
                getConfigLogger().verbose(config.getAccountId(), "Window context creation failed", e);
            }
        }

        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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
    public static final int SMART_PHONE = 1;

    /**
     * Device is a tablet
     */
    public static final int TABLET = 2;

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

    private boolean enableNetworkInfoReporting = false;

    private String googleAdID = null;

    private String library;

    private boolean limitAdTracking = false;

    private final CoreMetaData mCoreMetaData;

    private final ArrayList<ValidationResult> validationResults = new ArrayList<>();

    private String customLocale;

    private final  NetworkMonitor networkMonitor;

    // Read at init and on each resume, push status per request header.
    private volatile DeviceState deviceState;

    // Guards deviceState. Each refresh takes a generation so only the newest result is stored.
    private final Object deviceStateLock = new Object();

    private long deviceStateGeneration = 0;

    // Bumped on each push only refresh, guarded by deviceStateLock. A full read that overlaps one keeps its value.
    private long pushStatusGeneration = 0;

    // Last push only read, guarded by deviceStateLock. Kept even before the first full read is stored.
    private Integer latestPushStatus;

    // Last resumed activity's configuration, guarded by deviceStateLock. Its display values win.
    private Configuration activityConfiguration;

    // Can't change while the app runs, so read once off the main thread at init.
    private volatile DeviceSpecs deviceSpecs;

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

    /**
     * Determines if a device is tablet, smart phone or TV
     *
     * @param context context
     * @return one of the possible value of {@link DeviceType}
     */
    @DeviceType
    public static int getDeviceType(final Context context) {

        if (sDeviceType == NULL) {

            Boolean isTv = DeviceStateKt.existingReadOrNull("ui mode", () -> {
                UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
            });
            if (Boolean.TRUE.equals(isTv)) {
                sDeviceType = TV;
                return sDeviceType;
            }

            @DeviceType int type;
            try {
                type = context.getResources().getBoolean(R.bool.ctIsTablet) ? TABLET : SMART_PHONE;
            } catch (Exception e) {
                // resource not found or context is null
                Logger.d(TAG, "Failed to decide whether device is a smart phone or tablet!", e);
                type = UNKNOWN;
            }
            // Not cached when the TV check failed or timed out, so a TV isn't reported as a phone for the session.
            if (isTv == null) {
                return type;
            }
            sDeviceType = type;
        }
        return sDeviceType;
    }

    DeviceInfo(Context context, CleverTapInstanceConfig config, String cleverTapID,
               CoreMetaData coreMetaData, NetworkMonitor networkMonitor) {
        this.context = context;
        this.config = config;
        this.library = null;
        this.customLocale = null;
        this.networkMonitor = networkMonitor;
        mCoreMetaData = coreMetaData;

    }

    public void forceNewDeviceID() {
        String deviceID = generateGUID();
        forceUpdateDeviceId(deviceID);
    }

    public String forceUpdateCustomCleverTapID(String cleverTapID) {
        if (Utils.validateCTID(cleverTapID)) {
            getConfigLogger()
                    .info(config.getAccountId(), "Setting CleverTap ID to custom CleverTap ID : " + cleverTapID);
            String id = Constants.CUSTOM_CLEVERTAP_ID_PREFIX + cleverTapID;
            forceUpdateDeviceId(id);
            return id;
        } else {
            String fallbackId = generateFallbackDeviceID();
            removeDeviceID();
            String error = recordDeviceError(ValidationError.INVALID_CT_CUSTOM_ID, cleverTapID, getFallBackDeviceID());
            getConfigLogger().info(config.getAccountId(), error);
            return fallbackId;
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

    //Event
    public JSONObject getAppLaunchedFields() {

        try {
            boolean deviceIsMultiUser = false;
            if (getGoogleAdID() != null) {
                deviceIsMultiUser = new LoginInfoProvider(context, config).deviceIsMultiUser();
            }
            return CTJsonConverter.from(this, mCoreMetaData, enableNetworkInfoReporting,
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

    public String getCountryCode() {
        return getDeviceCachedInfo().countryCode;
    }

    public int getDPI() {
        return getDeviceCachedInfo().dpi;
    }

    public String getDeviceID() {
        String currentId = _getDeviceID();
        return currentId != null ? currentId : getFallBackDeviceID();
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

    public String getAppBucket() {
        return getDeviceCachedInfo().appBucket;
    }

    public String getModel() {
        return getDeviceCachedInfo().model;
    }

    public String getNetworkType() {
        return DeviceStateKt.existingReadOrNull("radio", this::readNetworkType);
    }

    private String readNetworkType() {
        if (networkMonitor == null) return null;
        return networkMonitor.getNetworkTypeString();
    }

    /**
     * Wifi, radio, bluetooth, metered and data saver state, read live for each af build. One read for all,
     * so the values can't disagree. Null if it didn't finish.
     */
    @Nullable
    public LiveNetworkInfo getLiveNetworkInfo() {
        return DeviceStateKt.existingReadOrNull("network info", () -> new LiveNetworkInfo(
                DeviceStateKt.catchOrNull("wifi", this::readWifiConnected),
                DeviceStateKt.catchOrNull("radio", this::readNetworkType),
                DeviceStateKt.catchOrNull("bluetooth state", this::readBluetoothEnabled),
                DeviceStateKt.catchOrNull("metered network", () -> DeviceState.readMetered(context)),
                DeviceStateKt.catchOrNull("data saver", () -> DeviceState.readDataSaver(context))
        ));
    }

    public static class LiveNetworkInfo {
        @Nullable
        public final Boolean wifi;
        @Nullable
        public final String radio;
        @Nullable
        public final Boolean bluetoothEnabled;
        @Nullable
        public final Boolean metered;
        @Nullable
        public final Boolean lowData;

        LiveNetworkInfo(@Nullable Boolean wifi, @Nullable String radio, @Nullable Boolean bluetoothEnabled,
                @Nullable Boolean metered, @Nullable Boolean lowData) {
            this.wifi = wifi;
            this.radio = radio;
            this.bluetoothEnabled = bluetoothEnabled;
            this.metered = metered;
            this.lowData = lowData;
        }
    }

    public String getOsName() {
        return getDeviceCachedInfo().osName;
    }

    public String getOsVersion() {
        return getDeviceCachedInfo().osVersion;
    }

    public int getSdkVersion() {
        return getDeviceCachedInfo().sdkVersion;
    }

    public int getLocalInAppCount() {
        return getDeviceCachedInfo().localInAppCount;
    }

    public void incrementLocalInAppCount() {
        getDeviceCachedInfo().localInAppCount++;
    }

    public String getDeviceLocale() {
        return getDeviceCachedInfo().locale;
    }

    public void setCustomLocale(String customLocale) {
        this.customLocale = customLocale;
    }

    public String getCustomLocale() {
        return customLocale;
    }

    public String getLocale() {
        // If locale is set by the client then use that, otherwise fetch it from the device
        return TextUtils.isEmpty(getCustomLocale()) ? getDeviceLocale() : getCustomLocale();
    }

    public ArrayList<ValidationResult> getValidationResults() {
        // noinspection unchecked
        ArrayList<ValidationResult> tempValidationResults = (ArrayList<ValidationResult>) validationResults.clone();
        validationResults.clear();
        return tempValidationResults;
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
        return DeviceStateKt.existingReadOrNull("bluetooth state", this::readBluetoothEnabled);
    }

    @SuppressLint("MissingPermission")
    private Boolean readBluetoothEnabled() {
        PackageManager pm = context.getPackageManager();
        int hasBluetoothPermission = pm.checkPermission(Manifest.permission.BLUETOOTH, context.getPackageName());
        if (hasBluetoothPermission != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null ? bluetoothAdapter.isEnabled() : null;
    }

    public boolean isErrorDeviceId() {
        final String deviceID = getDeviceID();
        return deviceID != null && deviceID.startsWith(Constants.ERROR_PROFILE_PREFIX);
    }

    public boolean isLimitAdTrackingEnabled() {
        synchronized (adIDLock) {
            return limitAdTracking;
        }
    }

    public Boolean isWifiConnected() {
        return DeviceStateKt.existingReadOrNull("wifi", this::readWifiConnected);
    }

    private Boolean readWifiConnected() {
        if (PackageManager.PERMISSION_GRANTED != context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            return null;
        }
        if (networkMonitor == null) {
            return null;
        }
        if (networkMonitor.getNetworkType() == NetworkMonitor.NetworkType.UNDETECTED) {
            return null;
        }
        return networkMonitor.isWifiConnected();
    }



    @Nullable
    public Boolean isLowPowerMode() {
        return getDeviceState().getLowPower();
    }

    @Nullable
    public Integer getThermalLevel() {
        return getDeviceState().getThermal();
    }

    @Nullable
    public Boolean isDarkMode() {
        return getDeviceState().getDarkMode();
    }

    @Nullable
    public Double getTextSize() {
        return getDeviceState().getTextSize();
    }

    @Nullable
    public Boolean isReduceMotion() {
        return getDeviceState().getReduceMotion();
    }

    @Nullable
    public Integer getPushStatus() {
        return getDeviceState().getPushStatus();
    }

    @Nullable
    public Integer getBatteryLevel() {
        return getDeviceState().getBatt();
    }

    @Nullable
    public Boolean isCharging() {
        return getDeviceState().getChg();
    }

    @Nullable
    public Boolean isBoldText() {
        return getDeviceState().getBoldText();
    }

    @Nullable
    public Integer getRamTier() {
        return getDeviceSpecs().getRam();
    }

    @Nullable
    public Integer getCpuCores() {
        return getDeviceSpecs().getCpu();
    }

    @Nullable
    public String getInstallSource() {
        return getDeviceSpecs().getInstSrc();
    }

    /** ApplicationExitInfo reason of this process's last death, API 30 and up. Null when there is no record. */
    @Nullable
    public Integer getLastExitReason() {
        DeviceSpecs.LastExit lastExit = getDeviceSpecs().getLastExit();
        return lastExit != null ? lastExit.getReason() : null;
    }

    @Nullable
    public Integer getLastExitStatus() {
        DeviceSpecs.LastExit lastExit = getDeviceSpecs().getLastExit();
        return lastExit != null ? lastExit.getStatus() : null;
    }

    @Nullable
    public Integer getLastExitImportance() {
        DeviceSpecs.LastExit lastExit = getDeviceSpecs().getLastExit();
        return lastExit != null ? lastExit.getImportance() : null;
    }

    /** AnrTypes code on the last exit record, API 37 and up. The system can attach it to any exit reason. */
    @Nullable
    public Integer getLastAnrType() {
        DeviceSpecs.Anr anr = getLastAnr();
        return anr != null ? anr.getType() : null;
    }

    /** How long the system waited before declaring the ANR, in milliseconds. */
    @Nullable
    public Long getLastAnrTimeoutMs() {
        DeviceSpecs.Anr anr = getLastAnr();
        return anr != null ? anr.getTimeoutMs() : null;
    }

    @Nullable
    public Boolean isLastAnrUserPerceptible() {
        DeviceSpecs.Anr anr = getLastAnr();
        return anr != null ? anr.getUserPerceptible() : null;
    }

    private DeviceSpecs.Anr getLastAnr() {
        DeviceSpecs.LastExit lastExit = getDeviceSpecs().getLastExit();
        return lastExit != null ? lastExit.getAnr() : null;
    }

    private DeviceSpecs getDeviceSpecs() {
        DeviceSpecs specs = deviceSpecs;
        if (specs == null) {
            // Not read yet. No system calls on the main thread, so leave the values out until the init read lands.
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return DeviceSpecs.UNKNOWN;
            }
            // Off the main thread, reading twice is harmless.
            specs = DeviceSpecs.read(context);
            deviceSpecs = specs;
        }
        return specs;
    }

    /**
     * Returns a task that re-reads the device state, to run off the main thread.
     * Only the newest prepared task's result is stored.
     */
    Callable<Void> prepareDeviceStateRefresh() {
        final long generation;
        synchronized (deviceStateLock) {
            generation = ++deviceStateGeneration;
        }
        return () -> {
            refreshDeviceState(generation);
            return null;
        };
    }

    // Dropped if a newer refresh was prepared.
    @WorkerThread
    private void refreshDeviceState(long generation) {
        final long pushGeneration;
        synchronized (deviceStateLock) {
            pushGeneration = pushStatusGeneration;
        }
        DeviceState state = DeviceState.read(context, context.getResources().getConfiguration());
        synchronized (deviceStateLock) {
            if (generation == deviceStateGeneration) {
                deviceState = withActivityDisplay(withLatestPushStatus(state, pushGeneration));
            }
        }
    }

    // Callers hold deviceStateLock. A push only refresh ran during this read, so keep what the last header sent.
    private DeviceState withLatestPushStatus(DeviceState state, long pushGeneration) {
        return pushGeneration == pushStatusGeneration ? state : state.withPushStatus(latestPushStatus);
    }

    /** Stores a resumed activity's display values. No system calls, so safe on the main thread. */
    void updateDisplayState(Configuration activityConfig) {
        Configuration copy = new Configuration(activityConfig);
        synchronized (deviceStateLock) {
            activityConfiguration = copy;
            if (deviceState != null) {
                deviceState = deviceState.withDisplay(copy);
            }
        }
    }

    /** Re-reads only push permission, so pushStatus matches ddnd in the same header. */
    @WorkerThread
    public void refreshPushStatus() {
        Integer pushStatus = DeviceState.readPushStatus(context);
        synchronized (deviceStateLock) {
            latestPushStatus = pushStatus;
            pushStatusGeneration++;
            if (deviceState != null) {
                deviceState = deviceState.withPushStatus(pushStatus);
            }
        }
    }

    // Callers hold deviceStateLock.
    private DeviceState withActivityDisplay(DeviceState state) {
        return activityConfiguration == null ? state : state.withDisplay(activityConfiguration);
    }

    private DeviceState getDeviceState() {
        DeviceState state = deviceState;
        if (state != null) {
            return state;
        }
        // Not read yet. On the main thread, send only display values and a push status already read, no system call.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            synchronized (deviceStateLock) {
                DeviceState displayOnly = DeviceState.displayOnly(context.getResources().getConfiguration());
                return withActivityDisplay(displayOnly.withPushStatus(latestPushStatus));
            }
        }
        final long pushGeneration;
        synchronized (deviceStateLock) {
            pushGeneration = pushStatusGeneration;
        }
        state = DeviceState.read(context, context.getResources().getConfiguration());
        synchronized (deviceStateLock) {
            // Keep a value a refresh stored meanwhile.
            if (deviceState == null) {
                deviceState = withActivityDisplay(withLatestPushStatus(state, pushGeneration));
            }
            return deviceState;
        }
    }

    void enableDeviceNetworkInfoReporting(boolean value) {
        enableNetworkInfoReporting = value;
        StorageHelper.putBoolean(context, config.getAccountId(), Constants.NETWORK_INFO, enableNetworkInfoReporting);
        config.getLogger()
                .verbose(config.getAccountId(),
                        "Device Network Information reporting set to " + enableNetworkInfoReporting);
    }

    String getAttributionID() {
        return getDeviceID();
    }

    @WorkerThread
    private int getLocalInAppCountFromPreference() {
        return StorageHelper.getInt(context, LOCAL_INAPP_COUNT, 0);
    }

    // Caches immutable device properties (model, screen size, carrier, …) off the caller thread.
    // Device-id resolution itself now happens in CleverTapFactory's device-id barrier via
    // initDeviceID(), not here (SDK-6254).
    void cacheDeviceInfoAsync() {
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "DeviceInfo() called");
        Task<Void> taskDeviceCachedInfo = CTExecutorFactory.executors(config).ioTask();
        taskDeviceCachedInfo.execute("getDeviceCachedInfo", new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                getDeviceCachedInfo();
                return null;
            }
        });

        // Own task, so it doesn't wait behind the snapshot above or get skipped if that throws.
        Task<Void> taskDeviceState = CTExecutorFactory.executors(config).ioTask();
        taskDeviceState.execute("initDeviceState", prepareDeviceStateRefresh());

        Task<Void> taskDeviceSpecs = CTExecutorFactory.executors(config).ioTask();
        taskDeviceSpecs.execute("initDeviceSpecs", () -> {
            getDeviceSpecs();
            return null;
        });

        Task<String> task = CTExecutorFactory.executors(config).ioTask();
        // callback on main thread
        task.addOnSuccessListener(deviceId -> {
            getConfigLogger().verbose(config.getAccountId() + ":async_deviceID",
                    "DeviceID initialized successfully!" + Thread.currentThread());
            // No need to put getDeviceID() on background thread because prefs already loaded
            CleverTapAPI.instanceWithConfig(context, config).deviceIDCreated(deviceId);
        });
        task.execute("initDeviceID", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return initDeviceID(cleverTapID);
            }
        });
    }

    public void setCurrentUserOptOutStateFromStorage() {
        String key = optOutKey();
        if (key == null) {
            config.getLogger().verbose(config.getAccountId(),
                    "Unable to set current user OptOut state from storage: storage key is null");
            return;
        }
        boolean storedOptOut = StorageHelper.getBooleanFromPrefs(context, config.getAccountId(), key);
        mCoreMetaData.setCurrentUserOptedOut(storedOptOut);
        config.getLogger().verbose(config.getAccountId(),
                "Set current user OptOut state from storage to: " + storedOptOut + " for key: " + key);
    }

    void saveOptOutState(boolean userOptOut) {
        // persist the new optOut state
        String key = optOutKey();
        if (key == null) {
            getConfigLogger()
                    .verbose(config.getAccountId(), "Unable to persist user OptOut state, storage key is null");
            return;
        }
        StorageHelper.putBoolean(context, config.getAccountId(), key, userOptOut);
        getConfigLogger().verbose(config.getAccountId(), "Set current user OptOut state to: " + userOptOut);
    }

    public void setSystemEventsAllowedStateFromStorage() {
        String key = allowedSystemEventsKey();
        if (key == null) {
            config.getLogger().verbose(config.getAccountId(),
                    "Unable to set current user allowed system events and communications flag from storage: storage key is null");
            return;
        }
        boolean storedAllowedSystemEvents = StorageHelper.getBooleanFromPrefs(context, config.getAccountId(), key);
        mCoreMetaData.setEnabledSystemEvents(storedAllowedSystemEvents);
        config.getLogger().verbose(config.getAccountId(),
                "Set current user allowed system events and communications flag state from storage to: " + storedAllowedSystemEvents + " for key: " + key);
    }

    void saveAllowedSystemEventsState(boolean allowedSystemEvents) {
        String key = allowedSystemEventsKey();
        if (key == null) {
            getConfigLogger()
                    .verbose(config.getAccountId(), "Unable to persist user allowed system events and communications flag state, storage key is null");
            return;
        }
        StorageHelper.putBoolean(context, config.getAccountId(), key, allowedSystemEvents);
        getConfigLogger().verbose(config.getAccountId(), "Set current user allowed system events and communications flag state to: " + allowedSystemEvents);
    }

    private String optOutKey() {
        String guid = getDeviceID();
        if (guid == null) {
            return null;
        }
        return "OptOut:" + guid;
    }

    private String allowedSystemEventsKey() {
        String guid = getDeviceID();
        if (guid == null) {
            return null;
        }
        return "allowSystemEvents:" + guid;
    }

    void setDeviceNetworkInfoReportingFromStorage() {
        boolean enabled = StorageHelper.getBooleanFromPrefs(context, config.getAccountId(), Constants.NETWORK_INFO);
        config.getLogger()
                .verbose(config.getAccountId(),
                        "Setting device network info reporting state from storage to " + enabled);
        enableNetworkInfoReporting = enabled;
    }

    private String _getDeviceID() {
        String _new = StorageHelper.getString(this.context, getDeviceIdStorageKey(), null);

        if (this.config.isDefaultInstance()) {
            return _new != null ? _new : StorageHelper.getString(this.context, Constants.DEVICE_ID_TAG, null);
        } else {
            return _new;
        }
    }

    private synchronized void fetchGoogleAdID() {
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "fetchGoogleAdID() called!");
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
                    getConfigLogger().verbose(config.getAccountId() + ":async_deviceID",
                            "limitAdTracking = " + limitAdTracking);
                    if (limitAdTracking) {
                        getConfigLogger().debug(config.getAccountId(),
                                "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation");
                        return;
                    }
                }
                Method getAdId = adInfo.getClass().getMethod("getId");
                advertisingID = (String) getAdId.invoke(adInfo);
            } catch (Throwable t) {
                if (t.getCause() != null) {
                    getConfigLogger().verbose(config.getAccountId(),
                            "Failed to get Advertising ID: " + t + t.getCause().toString());
                } else {
                    getConfigLogger().verbose(config.getAccountId(), "Failed to get Advertising ID: " + t);
                }
            }
            if (advertisingID != null && advertisingID.trim().length() > 2) {
                synchronized (adIDLock) {
                    if (advertisingID.contains("00000000")) {
                        //Device has opted out of sharing Google Advertising ID
                        getConfigLogger().debug(config.getAccountId(),
                                "Device user has opted out of sharing Advertising ID, falling back to random UUID for CleverTap ID generation");
                        return;
                    }
                    googleAdID = advertisingID.replace("-", "");
                }
            }

            getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "fetchGoogleAdID() done executing!");
        }
    }

    private synchronized String generateDeviceID() {
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "generateDeviceID() called!");
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
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "generateDeviceID() done executing!");
        return generatedDeviceID;
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

    String initDeviceID(String cleverTapID) {
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "Called initDeviceID()");
        //Show logging as per Manifest flag
        if (config.getEnableCustomCleverTapId()) {
            if (cleverTapID == null) {
                String error = recordDeviceError(ValidationError.USE_CUSTOM_ID_FALLBACK);
                config.getLogger().info(error);
            }
        } else {
            if (cleverTapID != null) {
                String error = recordDeviceError(ValidationError.USE_CUSTOM_ID_MISSING_IN_MANIFEST);
                config.getLogger().info(error);
            }
        }

        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "Calling _getDeviceID");
        String deviceID = _getDeviceID();
        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "Called _getDeviceID");
        if (deviceID != null && deviceID.trim().length() > 2) {
            getConfigLogger().verbose(config.getAccountId(), "CleverTap ID already present for profile");
            if (cleverTapID != null) {
                String error = recordDeviceError(ValidationError.UNABLE_TO_SET_CT_CUSTOM_ID, deviceID, cleverTapID);
                getConfigLogger().info(config.getAccountId(), error);
            }
            return deviceID;
        }

        if (this.config.getEnableCustomCleverTapId()) {
            return forceUpdateCustomCleverTapID(cleverTapID);
        }

        if (!this.config.isUseGoogleAdId()) {
            getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "Calling generateDeviceID()");
            String genId = generateDeviceID();
            getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "Called generateDeviceID()");
            return genId;
        }

        // fetch the googleAdID to generate GUID
        //has to be called on background thread
        fetchGoogleAdID();
        String genId = generateDeviceID();

        getConfigLogger().verbose(config.getAccountId() + ":async_deviceID", "initDeviceID() done executing!");
        return genId;
    }

    private String recordDeviceError(ValidationError error, String... varargs) {
        ValidationResult validationResult = ValidationResultFactory.create(error, varargs);
        validationResults.add(validationResult);
        return validationResult.getErrorDesc();
    }

    private void removeDeviceID() {
        StorageHelper.remove(this.context, getDeviceIdStorageKey());
    }

    private synchronized String generateFallbackDeviceID() {
        String existingId = getFallBackDeviceID();

        if (existingId != null) {
            return existingId;
        }

        synchronized (deviceIDLock) {
            String fallbackDeviceID = Constants.ERROR_PROFILE_PREFIX + UUID.randomUUID().toString()
                    .replace("-", "");
            updateFallbackID(fallbackDeviceID);
            return fallbackDeviceID;
        }
    }

    private void updateFallbackID(String fallbackId) {
        getConfigLogger().verbose(this.config.getAccountId(), "Updating the fallback id - " + fallbackId);
        StorageHelper.putString(context, getFallbackIdStorageKey(), fallbackId);
    }
}
