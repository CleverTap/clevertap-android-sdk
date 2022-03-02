package com.clevertap.android.pushtemplates;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.widget.RemoteViews;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("WeakerAccess")
public class Utils {

    @SuppressWarnings("unused")
    public static Bitmap getNotificationBitmap(String icoPath, boolean fallbackToAppIcon,
            final Context context)
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

    private static Bitmap getAppIcon(final Context context) throws NullPointerException {
        // Try to get the app logo first
        try {
            Drawable logo =
                    context.getPackageManager().getApplicationLogo(context.getApplicationInfo());
            if (logo == null) {
                throw new Exception("Logo is null");
            }
            return drawableToBitmap(logo);
        } catch (Exception e) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            return drawableToBitmap(
                    context.getPackageManager().getApplicationIcon(context.getApplicationInfo()));
        }
    }

    static Bitmap drawableToBitmap(Drawable drawable)
            throws NullPointerException {

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static Bitmap getBitmapFromURL(String srcUrl) {
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
            connection.setUseCaches(true);
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("Accept-Encoding", "gzip, deflate");
            connection.setConnectTimeout(PTConstants.PT_CONNECTION_TIMEOUT);
            connection.connect();
            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                PTLog.debug("File not loaded completely not going forward. URL was: " + srcUrl);
                return null;
            }

            // might be -1: server did not report the length
            long fileLength = connection.getContentLength();
            boolean isGZipEncoded = (connection.getContentEncoding() != null &&
                    connection.getContentEncoding().contains("gzip"));

            // download the file
            InputStream input = connection.getInputStream();

            byte[] data = new byte[16384];
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                total += count;
                buffer.write(data, 0, count);
            }

            byte[] tmpByteArray = new byte[16384];
            long totalDownloaded = total;

            if (isGZipEncoded) {
                InputStream is = new ByteArrayInputStream(buffer.toByteArray());
                ByteArrayOutputStream decompressedFile = new ByteArrayOutputStream();
                GZIPInputStream gzipInputStream = new GZIPInputStream(is);
                total = 0;
                int counter;
                while ((counter = gzipInputStream.read(tmpByteArray)) != -1) {
                    total += counter;
                    decompressedFile.write(tmpByteArray, 0, counter);
                }
                if (fileLength != -1 && fileLength != totalDownloaded) {
                    PTLog.debug("File not loaded completely not going forward. URL was: " + srcUrl);
                    return null;
                }
                return BitmapFactory.decodeByteArray(decompressedFile.toByteArray(), 0, (int) total);
            }

            if (fileLength != -1 && fileLength != totalDownloaded) {
                PTLog.debug("File not loaded completely not going forward. URL was: " + srcUrl);
                return null;
            }
            return BitmapFactory.decodeByteArray(buffer.toByteArray(), 0, (int) totalDownloaded);
        } catch (IOException e) {
            PTLog.verbose("Couldn't download the file. URL was: " + srcUrl);
            return null;
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Throwable t) {
                PTLog.verbose("Couldn't close connection!", t);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    static String _getManifestStringValueForKey(Bundle manifest, String name) {
        try {
            Object o = manifest.get(name);
            return (o != null) ? o.toString() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    static int getAppIconAsIntId(final Context context) {
        ApplicationInfo ai = context.getApplicationInfo();
        return ai.icon;
    }

    static ArrayList<String> getImageListFromExtras(Bundle extras) {
        ArrayList<String> imageList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_img")) {
                imageList.add(extras.getString(key));
            }
        }
        return imageList;
    }

    @SuppressWarnings("unused")
    static ArrayList<String> getCTAListFromExtras(Bundle extras) {
        ArrayList<String> ctaList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_cta")) {
                ctaList.add(extras.getString(key));
            }
        }
        return ctaList;
    }

    static ArrayList<String> getDeepLinkListFromExtras(Bundle extras) {
        ArrayList<String> dlList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_dl")) {
                dlList.add(extras.getString(key));
            }
        }
        return dlList;
    }

    static ArrayList<String> getBigTextFromExtras(Bundle extras) {
        ArrayList<String> btList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_bt")) {
                btList.add(extras.getString(key));
            }
        }
        return btList;
    }

    static ArrayList<String> getSmallTextFromExtras(Bundle extras) {
        ArrayList<String> stList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_st")) {
                stList.add(extras.getString(key));
            }
        }
        return stList;
    }

    static ArrayList<String> getPriceFromExtras(Bundle extras) {
        ArrayList<String> stList = new ArrayList<>();
        for (String key : extras.keySet()) {
            if (key.contains("pt_price") && !key.contains("pt_price_list")) {
                stList.add(extras.getString(key));
            }
        }
        return stList;
    }

    public static void loadImageBitmapIntoRemoteView(int imageViewID, Bitmap image,
            RemoteViews remoteViews) {
        remoteViews.setImageViewBitmap(imageViewID, image);
    }

    public static void loadImageURLIntoRemoteView(int imageViewID, String imageUrl,
            RemoteViews remoteViews) {

        Bitmap image = getBitmapFromURL(imageUrl);
        setFallback(false);

        if (image != null) {
            remoteViews.setImageViewBitmap(imageViewID, image);
        } else {
            PTLog.debug("Image was not perfect. URL:" + imageUrl + " hiding image view");
            setFallback(true);
        }

    }

    public static void loadImageURLIntoRemoteView(int imageViewID, String imageUrl,
            RemoteViews remoteViews, Context context) {
        Bitmap image = getBitmapFromURL(imageUrl);
        setFallback(false);
        if (image != null) {
            remoteViews.setImageViewBitmap(imageViewID, image);
        } else {
            PTLog.debug("Image was not perfect. URL:" + imageUrl + " hiding image view");
            setFallback(true);
        }

    }

    public static void loadImageRidIntoRemoteView(int imageViewID, int resourceID,
            RemoteViews remoteViews) {
        remoteViews.setImageViewResource(imageViewID, resourceID);
    }

    public static String getTimeStamp(Context context) {
        return DateUtils.formatDateTime(context, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME);
    }

    public static String getApplicationName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString()
                : context.getString(stringId);
    }

    @SuppressWarnings("ConstantConditions")
    static Bundle fromJson(JSONObject s) {
        Bundle bundle = new Bundle();

        for (Iterator<String> it = s.keys(); it.hasNext(); ) {
            String key = it.next();
            JSONArray arr = s.optJSONArray(key);
            String str = s.optString(key);

            if (arr != null && arr.length() <= 0) {
                bundle.putStringArray(key, new String[]{});
            } else if (arr != null && arr.optString(0) != null) {
                String[] newarr = new String[arr.length()];
                for (int i = 0; i < arr.length(); i++) {
                    newarr[i] = arr.optString(i);
                }
                bundle.putStringArray(key, newarr);
            } else if (str != null) {
                bundle.putString(key, str);
            } else {
                System.err.println("unable to transform json to bundle " + key);
            }
        }

        return bundle;
    }

    static void cancelNotification(Context ctx, int notifyId) {
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(notifyId);
    }

    static int getTimerThreshold(Bundle extras) {
        String val = "-1";
        for (String key : extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_THRESHOLD)) {
                val = extras.getString(key);
            }
        }
        return Integer.parseInt(val != null ? val : "-1");
    }

    static void setPackageNameFromResolveInfoList(Context context, Intent launchIntent) {
        List<ResolveInfo> resolveInfoList = context.getPackageManager().queryIntentActivities(launchIntent, 0);
        String appPackageName = context.getPackageName();
        for (ResolveInfo resolveInfo : resolveInfoList) {
            if (appPackageName.equals(resolveInfo.activityInfo.packageName)) {
                launchIntent.setPackage(appPackageName);
                break;
            }
        }
    }

    static void raiseCleverTapEvent(Context context, CleverTapInstanceConfig config, Bundle extras) {

        CleverTapAPI instance;
        if (config != null) {
            instance = CleverTapAPI.instanceWithConfig(context, config);
        } else {
            instance = CleverTapAPI.getDefaultInstance(context);
        }

        HashMap<String, Object> eProps;
        eProps = getEventPropertiesFromExtras(extras);

        String eName = getEventNameFromExtras(extras);

        if (eName != null && !eName.isEmpty()) {
            if (instance != null) {
                instance.pushEvent(eName, eProps);
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event");
            }
        }

    }

    static void raiseCleverTapEvent(Context context, CleverTapInstanceConfig config, Bundle extras, String key) {

        CleverTapAPI instance;
        if (config != null) {
            instance = CleverTapAPI.instanceWithConfig(context, config);
        } else {
            instance = CleverTapAPI.getDefaultInstance(context);
        }

        HashMap<String, Object> eProps;
        String value = extras.getString(key);

        eProps = getEventPropertiesFromExtras(extras, key, value);

        String eName = getEventNameFromExtras(extras);

        if (eName != null && !eName.isEmpty()) {
            if (instance != null) {
                instance.pushEvent(eName, eProps);
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event");
            }
        }

    }

    static void raiseCleverTapEvent(Context context, CleverTapInstanceConfig config, String evtName,
            HashMap<String, Object> eProps) {

        CleverTapAPI instance;
        if (config != null) {
            instance = CleverTapAPI.instanceWithConfig(context, config);
        } else {
            instance = CleverTapAPI.getDefaultInstance(context);
        }

        if (evtName != null && !evtName.isEmpty()) {
            if (instance != null) {
                instance.pushEvent(evtName, eProps);
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event");
            }
        }

    }

    static HashMap<String, Object> convertRatingBundleObjectToHashMap(Bundle b) {
        b.remove("config");
        final HashMap<String, Object> map = new HashMap<>();
        for (String s : b.keySet()) {
            if (s.contains("wzrk_") || s.equals(PTConstants.PT_ID)) {
                final Object o = b.get(s);
                if (o instanceof Bundle) {
                    map.putAll(convertRatingBundleObjectToHashMap((Bundle) o));
                } else {
                    map.put(s, b.get(s));
                }
            }
        }
        return map;
    }

    static String getEventNameFromExtras(Bundle extras) {
        String eName = null;
        for (String key : extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_NAME_KEY)) {
                eName = extras.getString(key);
            }
        }
        return eName;
    }

    static HashMap<String, Object> getEventPropertiesFromExtras(Bundle extras, String pkey, String value) {
        HashMap<String, Object> eProps = new HashMap<>();

        String[] eProp;
        for (String key : extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                if (extras.getString(key) != null && !extras.getString(key).isEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        eProp = key.split(PTConstants.PT_EVENT_PROPERTY_SEPERATOR);
                        if (extras.getString(key).equalsIgnoreCase(pkey)) {
                            eProps.put(eProp[1], value);
                            continue;
                        }
                        eProps.put(eProp[1], extras.getString(key));
                    } else {
                        PTLog.verbose("Property " + key + " does not have the separator");
                    }

                } else {
                    PTLog.verbose("Property Key is Empty. Skipping Property: " + key);
                }

            }
        }
        return eProps;
    }


    static HashMap<String, Object> getEventPropertiesFromExtras(Bundle extras) {
        HashMap<String, Object> eProps = new HashMap<>();

        String[] eProp;
        for (String key : extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                if (extras.getString(key) != null && !extras.getString(key).isEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        eProp = key.split(PTConstants.PT_EVENT_PROPERTY_SEPERATOR);
                        eProps.put(eProp[1], extras.getString(key));
                    } else {
                        PTLog.verbose("Property " + key + " does not have the separator");
                    }

                } else {
                    PTLog.verbose("Property Key is Empty. Skipping Property: " + key);
                }

            }
        }
        return eProps;
    }

    public static int getTimerEnd(Bundle extras) {
        String val = "-1";
        for (String key : extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_END)) {
                val = extras.getString(key);
            }
        }
        if (val.contains("$D_")) {
            String[] temp = val.split(PTConstants.PT_TIMER_SPLIT);
            val = temp[1];
        }
        long currentts = System.currentTimeMillis();
        int diff = (int) (Long.parseLong(val) - (currentts / 1000));
        if (val.equals("-1")){
            return Integer.MIN_VALUE;
        }//For empty check in timer_end
        return diff;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isNotificationInTray(Context context, int notificationId) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notificationId) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static Notification getNotificationById(Context context, int notificationId) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(NOTIFICATION_SERVICE);
        StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getId() == notificationId) {
                return notification.getNotification();
            }
        }
        return null;
    }

    public static ArrayList<Integer> getNotificationIds(Context context) {
        ArrayList<Integer> ids = new ArrayList<Integer>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NotificationManager mNotificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);
            StatusBarNotification[] notifications = mNotificationManager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getPackageName().equalsIgnoreCase(context.getPackageName())) {
                    ids.add(notification.getId());
                }
            }
        }
        return ids;
    }

    static void raiseNotificationClicked(Context context, Bundle extras, CleverTapInstanceConfig config) {
        CleverTapAPI instance;
        if (config != null) {
            instance = CleverTapAPI.instanceWithConfig(context, config);
        } else {
            instance = CleverTapAPI.getDefaultInstance(context);
        }
        if (instance != null) {
            instance.pushNotificationClickedEvent(extras);
        }

    }

    static JSONArray getActionKeys(Bundle extras) {
        JSONArray actions = null;

        String actionsString = extras.getString(Constants.WZRK_ACTIONS);
        if (actionsString != null) {
            try {
                actions = new JSONArray(actionsString);
            } catch (Throwable t) {
                PTLog.debug("error parsing notification actions: " + t.getLocalizedMessage());
            }
        }
        return actions;
    }

    static void showToast(final Context context, final String message,
            final CleverTapInstanceConfig config) {
        if (config != null) {
            Task<Void> task = CTExecutorFactory.executors(config).mainTask();
            task.execute("PushTemplatesUtils#showToast", new Callable<Void>() {
                @Override
                public Void call() {
                    if (!TextUtils.isEmpty(message)) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                    return null;
                }
            });
        }
    }

    static void createSilentNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel notificationChannel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) == null || (
                    notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) != null
                            && !isNotificationChannelEnabled(
                            notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)))) {
                Uri soundUri = Uri
                        .parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/raw/"
                                + PTConstants.PT_SOUND_FILE_NAME);
                notificationChannel = new NotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID,
                        PTConstants.PT_SILENT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                if (soundUri != null) {
                    notificationChannel.setSound(soundUri,
                            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
                }
                notificationChannel.setDescription(PTConstants.PT_SILENT_CHANNEL_DESC);
                notificationChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

    }

    static void deleteSilentNotificationChannel(Context context) {
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) != null
                    && isNotificationChannelEnabled(
                    notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID))) {
                notificationManager.deleteNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID);
            }
        }

    }

    static boolean isNotificationChannelEnabled(NotificationChannel channel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel != null) {
            return channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return false;
    }

    public static Bitmap setBitMapColour(Context context, int resourceID, String clr)
            throws NullPointerException {
        if (clr != null && !clr.isEmpty()) {
            int color = getColour(clr, PTConstants.PT_COLOUR_GREY);

            Drawable mDrawable = Objects.requireNonNull(ContextCompat.getDrawable(context, resourceID)).mutate();
            mDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            return Utils.drawableToBitmap(mDrawable);
        }
        return null;
    }

    public static int getColour(String clr, String default_clr) {
        try {
            return Color.parseColor(clr);
        } catch (Exception e) {
            PTLog.debug("Can not parse colour value: " + clr + " Switching to default colour: " + default_clr);
            return Color.parseColor(default_clr);
        }
    }

    static void setFallback(Boolean val) {
        PTConstants.PT_FALLBACK = val;
    }

    public static boolean getFallback() {
        return PTConstants.PT_FALLBACK;
    }

    public static int getFlipInterval(Bundle extras) {
        String interval = extras.getString(PTConstants.PT_FLIP_INTERVAL);
        try {
            int t = 0;
            if (interval != null) {
                t = Integer.parseInt(interval);
                return Math.max(t, PTConstants.PT_FLIP_INTERVAL_TIME);
            }
        } catch (Exception e) {
            PTLog.debug("Flip Interval couldn't be converted to number: " + interval + " - Defaulting to base value: "
                    + PTConstants.PT_FLIP_INTERVAL_TIME);
        }
        return PTConstants.PT_FLIP_INTERVAL_TIME;
    }

    static void deleteImageFromStorage(Context context, Intent intent) {
        String pId = intent.getStringExtra(Constants.WZRK_PUSH_ID);

        ContextWrapper cw = new ContextWrapper(context.getApplicationContext());
        File MyDirectory = cw.getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE);
        String path = MyDirectory.getAbsolutePath();
        String[] fileList = MyDirectory.list();
        File fileToBeDeleted = null;
        if (fileList != null) {
            for (String fileName : fileList) {
                if (pId != null && fileName.contains(pId)) {
                    fileToBeDeleted = new File(path + "/" + fileName);
                    boolean wasDeleted = fileToBeDeleted.delete();
                    if (!wasDeleted) {
                        PTLog.debug("Failed to clean up the following file: " + fileName);
                    }
                } else if (pId == null && fileName.contains("null")) {
                    fileToBeDeleted = new File(path + "/" + fileName);
                    boolean wasDeleted = fileToBeDeleted.delete();
                    if (!wasDeleted) {
                        PTLog.debug("Failed to clean up the following file: " + fileName);
                    }
                }
            }
        }
    }

}
