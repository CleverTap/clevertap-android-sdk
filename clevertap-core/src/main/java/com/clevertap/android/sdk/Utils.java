package com.clevertap.android.sdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class Utils {

    public static boolean isActivityDead(Activity activity) {
        if (activity == null) {
            return true;
        }
        boolean isActivityDead = activity.isFinishing();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isActivityDead = isActivityDead || activity.isDestroyed();
        }
        return isActivityDead;
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

    static HashMap<String, Object> convertBundleObjectToHashMap(Bundle b) {
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

    static ArrayList<String> convertJSONArrayToArrayList(JSONArray array) {
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

    static HashMap<String, Object> convertJSONObjectToHashMap(JSONObject b) {
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

    static Bitmap drawableToBitmap(Drawable drawable)
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

    static Bitmap getBitmapFromURL(String srcUrl) {
        // Safe bet, won't have more than three /s
        srcUrl = srcUrl.replace("///", "/");
        srcUrl = srcUrl.replace("//", "/");
        srcUrl = srcUrl.replace("http:/", "http://");
        srcUrl = srcUrl.replace("https:/", "https://");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(srcUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {

            Logger.v("Couldn't download the notification icon. URL was: " + srcUrl);
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                Logger.v("Couldn't close connection!", t);
            }
        }
    }

    static byte[] getByteArrayFromImageURL(String srcUrl) {
        srcUrl = srcUrl.replace("///", "/");
        srcUrl = srcUrl.replace("//", "/");
        srcUrl = srcUrl.replace("http:/", "http://");
        srcUrl = srcUrl.replace("https:/", "https://");
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(srcUrl);
            connection = (HttpsURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            Logger.v("Error processing image bytes from url: " + srcUrl);
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                Logger.v("Couldn't close connection!", t);
            }
        }
    }

    @SuppressLint("MissingPermission")
    static String getCurrentNetworkType(final Context context) {
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
    static String getDeviceNetworkType(final Context context) {
        // Fall back to network type
        TelephonyManager teleMan = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (teleMan == null) {
            return "Unavailable";
        }

        int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

    static long getMemoryConsumption() {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        return total - free;
    }

    static Bitmap getNotificationBitmap(String icoPath, boolean fallbackToAppIcon, final Context context)
            throws NullPointerException {
        // If the icon path is not specified
        if (icoPath == null || icoPath.equals("")) {
            return fallbackToAppIcon ? getAppIcon(context) : null;
        }
        // Simply stream the bitmap
        if (!icoPath.startsWith("http")) {
            icoPath = Constants.ICON_BASE_URL + "/" + icoPath;
        }
        Bitmap ic = getBitmapFromURL(icoPath);
        return (ic != null) ? ic : ((fallbackToAppIcon) ? getAppIcon(context) : null);
    }

    static int getThumbnailImage(Context context, String image) {
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
    static boolean hasPermission(final Context context, String permission) {
        try {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission);
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean validateCTID(String cleverTapID) {
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
        if (!cleverTapID.matches("[A-Za-z0-9()!:$@_-]*")) {
            Logger.i("Custom CleverTap ID cannot contain special characters apart from :,(,),_,!,@,$ and - ");
            return false;
        }
        return true;
    }

    private static Bitmap getAppIcon(final Context context) throws NullPointerException {
        // Try to get the app logo first
        try {
            Drawable logo = context.getPackageManager().getApplicationLogo(context.getApplicationInfo());
            if (logo == null) {
                throw new Exception("Logo is null");
            }
            return drawableToBitmap(logo);
        } catch (Exception e) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            return drawableToBitmap(context.getPackageManager().getApplicationIcon(context.getApplicationInfo()));
        }
    }

}
