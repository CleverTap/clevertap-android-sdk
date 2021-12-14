package com.clevertap.android.sdk.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

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
}