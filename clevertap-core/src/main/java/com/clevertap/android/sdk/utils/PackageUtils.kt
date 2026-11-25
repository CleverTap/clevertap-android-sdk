package com.clevertap.android.sdk.utils

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight

internal object PackageUtils {

    private const val GOOGLE_PLAY_STORE_PACKAGE_OLD = "com.google.market"

    private const val GOOGLE_PLAY_STORE_PACKAGE = "com.android.vending"

    @JvmStatic
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            Class.forName("com.google.android.gms.common.GooglePlayServicesUtil")
            val status = GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context)
            status == ConnectionResult.SUCCESS
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun isGooglePlayStoreAvailable(context: Context): Boolean {
        return isPackageAvailable(context, GOOGLE_PLAY_STORE_PACKAGE) || isPackageAvailable(
            context,
            GOOGLE_PLAY_STORE_PACKAGE_OLD
        )
    }

    /**
     * Checks if a given package is installed on the device.
     *
     * @param context     The application context.
     * @param packageName The name of the package as a string.
     * @return `true` if the given package is installed on the device,
     * otherwise `false`
     */
    private fun isPackageAvailable(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
