package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

final class Utils {
    static long getMemoryConsumption() {
        long free = Runtime.getRuntime().freeMemory();
        long total = Runtime.getRuntime().totalMemory();
        return total - free;
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

    static String getCurrentNetworkType(final Context context) {
        try {
            // First attempt to check for WiFi connectivity
            ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connManager == null) {
                return  "Unavailable";
            }
            @SuppressLint("MissingPermission") NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                return "WiFi";
            }

            // Fall back to network type
            TelephonyManager teleMan = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (teleMan == null) {
                return  "Unavailable";
            }
            int networkType = teleMan.getNetworkType();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return "CDMA";
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return "EDGE";
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return "GPRS";
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:
                    return "LTE";
                default:
                    return "Unknown";
            }
        } catch (Throwable t) {
            return "Unavailable";
        }
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
        //noinspection ConstantConditions
        return (ic != null) ? ic : ((fallbackToAppIcon) ? getAppIcon(context) : null);
    }

    private static Bitmap getAppIcon(final Context context) throws NullPointerException {
        // Try to get the app logo first
        try {
            Drawable logo = context.getPackageManager().getApplicationLogo(context.getApplicationInfo());
            if (logo == null)
                throw new Exception("Logo is null");
            return drawableToBitmap(logo);
        } catch (Exception e) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            return drawableToBitmap(context.getPackageManager().getApplicationIcon(context.getApplicationInfo()));
        }
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

    @SuppressWarnings("WeakerAccess")
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

    static byte[] getByteArrayFromImageURL(String srcUrl){
        srcUrl = srcUrl.replace("///", "/");
        srcUrl = srcUrl.replace("//", "/");
        srcUrl = srcUrl.replace("http:/", "http://");
        srcUrl = srcUrl.replace("https:/", "https://");
        HttpsURLConnection connection = null;
        try{
            URL url = new URL(srcUrl);
            connection = (HttpsURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            byte [] buffer = new byte[8192];
            int bytesRead;
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((bytesRead = is.read(buffer)) != -1){
                baos.write(buffer,0,bytesRead);
            }
            return baos.toByteArray();
        }catch (IOException e){
            Logger.v("Error processing image bytes from url: "+ srcUrl);
            return null;
        }finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                Logger.v("Couldn't close connection!", t);
            }
        }
    }

    static int getThumbnailImage(Context context, String image){
        if (context != null) {
            return context.getResources().getIdentifier(image,"drawable",context.getPackageName());
        } else {
            return -1;
        }
    }

    static ArrayList<String> convertJSONArrayToArrayList(JSONArray array){
        ArrayList<String> listdata = new ArrayList<String>();
        if (array != null) {
            for (int i = 0; i< array.length(); i++){
                try {
                    listdata.add(array.getString(i));
                } catch (JSONException e) {
                    Logger.v("Could not convert JSONArray to ArrayList - " + e.getMessage());
                }
            }
        }
        return listdata;
    }

    static boolean validateCTID(String cleverTapID){
        if(cleverTapID == null){
            Logger.i("CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is NULL.");
            return false;
        }
        if(cleverTapID.isEmpty()){
            Logger.i("CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is empty.");
            return false;
        }
        if(cleverTapID.length() > 64){
            Logger.i("Custom CleverTap ID passed is greater than 64 characters. ");
            return false;
        }
        if(!cleverTapID.matches("[a-zA-Z0-9{}:()_!@#$%&-]*")){
            Logger.i("Custom CleverTap ID cannot contain special characters apart from {,},:,(,),_,!,@,#,$,&,% and - ");
            return false;
        }
        return true;
    }
}
