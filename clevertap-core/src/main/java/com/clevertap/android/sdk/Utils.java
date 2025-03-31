package com.clevertap.android.sdk;

import static com.clevertap.android.sdk.Constants.AUTH;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest;
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader;
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation;
import com.clevertap.android.sdk.network.DownloadedBitmap;
import com.clevertap.android.sdk.network.DownloadedBitmapFactory;
import com.google.firebase.messaging.RemoteMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class Utils {

    private static final Pattern normalizedNameExcludePattern = Pattern.compile("\\s+");

    public static boolean containsIgnoreCase(Collection<String> collection, String key) {
        if (collection == null || key == null) {
            return false;
        }
        for (String entry : collection) {
            if (key.equalsIgnoreCase(entry)) {
                return true;
            }
        }
        return false;
    }

    public static HashMap<String, Object> convertBundleObjectToHashMap(@NonNull Bundle b) {
        final HashMap<String, Object> map = new HashMap<>();
        for (String s : b.keySet()) {
            final Object o = b.get(s);

            if (o instanceof Bundle) {
                map.putAll(convertBundleObjectToHashMap((Bundle) o));
            } else {
                map.put(s, b.get(s));
            }
        }
        return map;
    }

    public static ArrayList<HashMap<String, Object>> convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(
            JSONArray jsonArray) {
        final ArrayList<HashMap<String, Object>> hashMapArrayList = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    hashMapArrayList.add(convertJSONObjectToHashMap(jsonArray.getJSONObject(i)));
                } catch (JSONException e) {
                    Logger.v("Could not convert JSONArray of JSONObjects to ArrayList of HashMaps - " + e
                            .getMessage());
                }
            }
        }
        return hashMapArrayList;
    }

    public static ArrayList<String> convertJSONArrayToArrayList(JSONArray array) {
        ArrayList<String> listdata = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                try {
                    listdata.add(array.getString(i));
                } catch (JSONException e) {
                    Logger.v("Could not convert JSONArray to ArrayList - " + e.getMessage());
                }
            }
        }
        return listdata;
    }

    public static HashMap<String, Object> convertJSONObjectToHashMap(JSONObject b) {
        final HashMap<String, Object> map = new HashMap<>();
        final Iterator<String> keys = b.keys();

        while (keys.hasNext()) {
            try {
                final String s = keys.next();
                final Object o = b.get(s);
                if (o instanceof JSONObject) {
                    map.putAll(convertJSONObjectToHashMap((JSONObject) o));
                } else {
                    map.put(s, b.get(s));
                }
            } catch (Throwable ignored) {
                // Ignore
            }
        }

        return map;
    }

    public static String convertToTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    // used by inapp.
    public static Bitmap getBitmapFromURL(@NonNull String srcUrl) {
        BitmapDownloadRequest bitmapDownloadRequest = new BitmapDownloadRequest(srcUrl);
        return HttpBitmapLoader.getHttpBitmap(HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP,bitmapDownloadRequest).getBitmap();
    }

    public static long getNowInMillis() {
        return System.currentTimeMillis();
    }

    @SuppressLint("MissingPermission")
    public static String getCurrentNetworkType(final Context context) {
        try {
            // First attempt to check for WiFi connectivity
            ConnectivityManager connManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager == null) {
                return "Unavailable";
            }
            NetworkInfo mWifi = connManager
                    .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi != null && mWifi.isConnected()) {
                return "WiFi";
            }

            return getDeviceNetworkType(context);


        } catch (Throwable t) {
            return "Unavailable";
        }
    }

    @SuppressLint("MissingPermission")
    public static String getDeviceNetworkType(@NonNull final Context context) {
        // Fall back to network type
        TelephonyManager teleMan = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (teleMan == null) {
            return "Unavailable";
        }

        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                try {
                    networkType = teleMan.getDataNetworkType();
                } catch (SecurityException se) {
                    Logger.d("Security Exception caught while fetch network type" + se.getMessage());
                }
            } else {
                Logger.d("READ_PHONE_STATE permission not asked by the app or not granted by the user");
            }
        } else {
            networkType = teleMan.getNetworkType();
        }

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
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "Unknown";
        }
    }

    public static long getMemoryConsumption() {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        return total - free;
    }

    @NonNull
    public static DownloadedBitmap getDownloadedBitmapPostFallbackIconCheck(final boolean fallbackToAppIcon, final Context context,
            @NonNull final DownloadedBitmap ic) {
        return (ic.getBitmap() != null) ? ic : (fallbackToAppIcon ? getAppIcon(context) : ic);
    }

    /**
     * get bitmap from url within defined timeoutMillis bound and sizeBytes bound or else return
     * null or app icon based on fallbackToAppIcon param
     */
    public static @NonNull DownloadedBitmap getNotificationBitmapWithTimeoutAndSize(String icoPath, boolean fallbackToAppIcon,
            final Context context, final CleverTapInstanceConfig config, long timeoutMillis, int sizeBytes)
            throws NullPointerException {
        final BitmapDownloadRequest bitmapDownloadRequest = new BitmapDownloadRequest(icoPath, fallbackToAppIcon,
                context, config, timeoutMillis, sizeBytes);
        return HttpBitmapLoader.getHttpBitmap(HttpBitmapOperation.DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT,bitmapDownloadRequest);
    }
    public static @NonNull DownloadedBitmap getNotificationBitmapWithTimeout(String icoPath, boolean fallbackToAppIcon,
            final Context context, final CleverTapInstanceConfig config, long timeoutMillis)
            throws NullPointerException {
        final BitmapDownloadRequest bitmapDownloadRequest = new BitmapDownloadRequest(icoPath, fallbackToAppIcon,
                context, config, timeoutMillis);
        return HttpBitmapLoader.getHttpBitmap(HttpBitmapOperation.DOWNLOAD_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT,bitmapDownloadRequest);
    }

    public static int getNow() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public static int getThumbnailImage(Context context, String image) {
        if (context != null) {
            return context.getResources().getIdentifier(image, "drawable", context.getPackageName());
        } else {
            return -1;
        }
    }

    /**
     * Checks whether a particular permission is available or not.
     *
     * @param context    The Android {@link Context}
     * @param permission The fully qualified Android permission name
     */
    public static boolean hasPermission(@NonNull final Context context, @NonNull String permission) {
        try {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission);
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isActivityDead(Activity activity) {
        if (activity == null) {
            return true;
        }
        return activity.isFinishing() || activity.isDestroyed();
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static boolean isServiceAvailable(@NonNull Context context, Class clazz) {
        if (clazz == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        String packageName = context.getPackageName();

        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SERVICES);
            ServiceInfo[] services = packageInfo.services;
            for (ServiceInfo serviceInfo : services) {
                if (serviceInfo.name.equals(clazz.getName())) {
                    Logger.v("Service " + serviceInfo.name + " found");
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.d("Intent Service name not found exception - " + e.getLocalizedMessage());
        }
        return false;
    }

    public static String optionalStringKey(JSONObject o, String k)
            throws JSONException {
        if (o.has(k) && !o.isNull(k)) {
            return o.getString(k);
        }

        return null;
    }

    /**
     * Handy method to post any runnable to run on the main thread.
     *
     * @param runnable - task to be run
     */
    public static void runOnUiThread(Runnable runnable) {
        if (runnable != null) {
            //run if already on the UI thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else {
                //post on UI thread if called from Non-UI thread.
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(runnable);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static void setPackageNameFromResolveInfoList(Context context, Intent launchIntent) {
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(launchIntent, 0);
        if (resolveInfoList != null) {
            String appPackageName = context.getPackageName();
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (appPackageName.equals(resolveInfo.activityInfo.packageName)) {
                    launchIntent.setPackage(appPackageName);
                    break;
                }
            }
        }
    }

    /**
     * @param content String which contains bundle information
     * @return Bundle to be passed to createNotification(Context context, Bundle extras)
     */
    @SuppressWarnings("rawtypes")
    public static Bundle stringToBundle(String content) throws JSONException {

        Bundle bundle = new Bundle();

        if (!TextUtils.isEmpty(content)) {
            JSONObject jsonObject = new JSONObject(content);
            Iterator iter = jsonObject.keys();
            while (iter.hasNext()) {
                String key = (String) iter.next();
                String value = jsonObject.getString(key);
                bundle.putString(key, value);
            }
        }

        return bundle;
    }

    public static boolean validateCTID(String cleverTapID) {
        if (cleverTapID == null) {
            Logger.i(
                    "CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is NULL.");
            return false;
        }
        if (cleverTapID.isEmpty()) {
            Logger.i(
                    "CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is empty.");
            return false;
        }
        if (cleverTapID.length() > 64) {
            Logger.i("Custom CleverTap ID passed is greater than 64 characters. ");
            return false;
        }
        if (!cleverTapID.matches("[=|<>;+.A-Za-z0-9()!:$@_-]*")) {
            Logger.i(
                    "Custom CleverTap ID cannot contain special characters apart from : =,(,),_,!,@,$,|<,>,;,+,. and - ");
            return false;
        }
        return true;
    }

    static Bitmap drawableToBitmap(@NonNull Drawable drawable)
            throws NullPointerException {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static @NonNull DownloadedBitmap getAppIcon(final Context context) throws NullPointerException {
        // Try to get the app logo first
        try {
            Drawable logo = context.getPackageManager().getApplicationLogo(context.getApplicationInfo());
            if (logo == null) {
                throw new Exception("Logo is null");
            }
            return DownloadedBitmapFactory.INSTANCE.successBitmap(drawableToBitmap(logo), 0, null);
        } catch (Exception e) {
            e.printStackTrace();
            // Try to get the app icon now
            // No error handling here - handle upstream
            return DownloadedBitmapFactory.INSTANCE.successBitmap(
                    drawableToBitmap(context.getPackageManager().getApplicationIcon(context.getApplicationInfo())),
                    0, null);
        }
    }

    public static String getSCDomain(String domain) {
        String[] parts = domain.split("\\.", 2);
        return parts[0] + "." + AUTH + "." + parts[1];
    }

    public static boolean isRenderFallback(RemoteMessage remoteMessage, Context context) {
        boolean renderRateKillSwitch = Boolean
                .parseBoolean(remoteMessage.getData().get(Constants.WZRK_TSR_FB));//tsrfb
        boolean renderRateFallback = Boolean
                .parseBoolean(remoteMessage.getData().get(Constants.NOTIFICATION_RENDER_FALLBACK));

        return !renderRateKillSwitch && renderRateFallback;

    }

    public static void navigateToAndroidSettingsForNotifications(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        }
        context.startActivity(intent);
    }

    @WorkerThread
    public static boolean isMainProcess(Context context, String mainProcessName) {

        try {
            ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));

            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();

            int myPid = Process.myPid();

            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<JSONObject> toJSONObjectList(JSONArray jsonArray) throws JSONException {
        List<JSONObject> jsonObjectList = new ArrayList<>();
        for (int index = 0; index < jsonArray.length(); index++) {
            jsonObjectList.add(jsonArray.getJSONObject(index));
        }
        return jsonObjectList;
    }

    /**
     * Calculates the haversine distance between two locations on Earth.
     *
     * @param coordinateA The first location.
     * @param coordinateB The second location.
     * @return The haversine distance between the two locations, in kilometers.
     */
    public static double haversineDistance(Location coordinateA, Location coordinateB) {
        // The Earth radius ranges from a maximum of about 6378 km (equatorial)
        // to a minimum of about 6357 km (polar).
        // A globally-average value is usually considered to be 6371 km (6371e3).
        // This method uses 6378.2 km as the radius since this is the value
        // used by the backend, and calculations should produce the same result.
        final double EARTH_DIAMETER = 2 * 6378.2;

        final double RAD_CONVERT = Math.PI / 180;
        double phi1 = coordinateA.getLatitude() * RAD_CONVERT;
        double phi2 = coordinateB.getLatitude() * RAD_CONVERT;

        double deltaPhi = (coordinateB.getLatitude() - coordinateA.getLatitude()) * RAD_CONVERT;
        double deltaLambda = (coordinateB.getLongitude() - coordinateA.getLongitude()) * RAD_CONVERT;

        double sinPhi = Math.sin(deltaPhi / 2);
        double sinLambda = Math.sin(deltaLambda / 2);

        double a = sinPhi * sinPhi + Math.cos(phi1) * Math.cos(phi2) * sinLambda * sinLambda;
        // Distance in km
        return EARTH_DIAMETER * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Reads the content of a file from the "assets" folder.
     *
     * @param context  The application context.
     * @param fileName The name of the file to be read from the "assets" folder.
     * @return The content of the file as a String.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    public static String readAssetFile(Context context, String fileName) throws IOException {
        AssetManager assetManager = context.getAssets();
        try (InputStream inputStream = assetManager.open(fileName)) {
            // Read the entire content of the file into a String
            return new Scanner(inputStream).useDelimiter("\\A").next();
        }
    }

    /**
     * Get the CT normalized version of an event or a property name.
     *
     * @param name The event/property name
     */
    public static String getNormalizedName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        // lowercase with English locale for consistent behavior with the backend and across different device locales
        return normalizedNameExcludePattern.matcher(name).replaceAll("").toLowerCase(Locale.ENGLISH);
    }

    /**
     * Check if two event/property names are equal with applied CT normalization
     *
     * @param name  Event or property name
     * @param other Event or property name to compare to
     * @see #getNormalizedName(String)
     */
    public static boolean areNamesNormalizedEqual(@Nullable String name, @Nullable String other) {
        return Objects.equals(getNormalizedName(name), getNormalizedName(other));
    }
}