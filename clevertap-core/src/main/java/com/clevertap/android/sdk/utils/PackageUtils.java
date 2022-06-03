package com.clevertap.android.sdk.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;
import java.lang.reflect.Method;

public class PackageUtils {

    private static final String GOOGLE_PLAY_STORE_PACKAGE_OLD = "com.google.market";

    private static final String GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending";

    public static boolean isGooglePlayServicesAvailable(@NonNull Context context) {
        try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
            int status = GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context);
            return status == ConnectionResult.SUCCESS;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isGooglePlayStoreAvailable(@NonNull Context context) {
        return isPackageAvailable(context, GOOGLE_PLAY_STORE_PACKAGE) || isPackageAvailable(context,
                GOOGLE_PLAY_STORE_PACKAGE_OLD);
    }

    /**
     * Checks if a given package is installed on the device.
     *
     * @param context     The application context.
     * @param packageName The name of the package as a string.
     * @return <code>true</code> if the given package is installed on the device,
     * otherwise <code>false</code>
     */
    private static boolean isPackageAvailable(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean isIntentResolved(Context context, Intent intent) {
        return (intent != null
                && context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null);
    }

    /**
     * Check if device is xiaomi, running MIUI OS
     * @param context application context
     * @return true if device is xiaomi, running MIUI OS or false
     */
    public static boolean isXiaomiDeviceRunningMiui(Context context) {
        try {
            String manufacturer = "xiaomi";
            if (!manufacturer.equalsIgnoreCase(android.os.Build.MANUFACTURER)) {
                return false;
            }

            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            String miui = (String) get.invoke(c, "ro.miui.ui.version.code");
            if (miui!=null && !TextUtils.isEmpty(miui.trim()))
            {
                return true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return isIntentResolved(context,
                new Intent("miui.intent.action.OP_AUTO_START").addCategory(Intent.CATEGORY_DEFAULT))
                || isIntentResolved(context, new Intent().setComponent(new ComponentName("com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity")))
                || isIntentResolved(context,
                new Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").addCategory(Intent.CATEGORY_DEFAULT))
                || isIntentResolved(context, new Intent()
                .setComponent(new ComponentName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")));
    }
}