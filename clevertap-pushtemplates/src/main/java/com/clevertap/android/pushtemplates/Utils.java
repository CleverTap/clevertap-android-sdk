package com.clevertap.android.pushtemplates;

import static android.content.Context.NOTIFICATION_SERVICE;

import static com.clevertap.android.pushtemplates.PTConstants.ALT_TEXT_SUFFIX;
import static com.clevertap.android.pushtemplates.PTConstants.COLOR_KEYS;
import static com.clevertap.android.pushtemplates.PTConstants.PT_DARK_MODE_SUFFIX;

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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.Constants;
import com.clevertap.android.sdk.Logger;
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest;
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader;
import com.clevertap.android.sdk.network.DownloadedBitmap;
import com.clevertap.android.sdk.task.CTExecutorFactory;
import com.clevertap.android.sdk.task.Task;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

        Bitmap ic = getBitmapFromURL(icoPath,context);
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

    private static Bitmap getBitmapFromURL(String srcUrl, @Nullable Context context) {

        BitmapDownloadRequest request = new BitmapDownloadRequest(srcUrl,
                false,
                context,
                null,
                -1,
                -1
        );

        DownloadedBitmap db = HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                request
        );

        if (db.getStatus() == DownloadedBitmap.Status.SUCCESS) {
            return db.getBitmap();
        } else {
            Logger.v("network call for bitmap download failed with url : " + srcUrl + " http status: " + db.getStatus());
            return null;
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

    static ArrayList<ImageData> getImageDataListFromExtras(Bundle extras, String defaultAltText) {
        ArrayList<ImageData> imageList = new ArrayList<>();
        int counter = 1;
        for (String key : extras.keySet()) {
            if (key.contains("pt_img")) {
                String imageUrl = extras.getString(key);
                String altText = extras.getString(key + ALT_TEXT_SUFFIX, defaultAltText + counter);
                imageList.add(new ImageData(imageUrl, altText));
                counter++;
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

    /**
     * Creates a map of colors for the specified display mode (dark/light)
     * @param extras The original extras bundle containing all color values
     * @param isDarkMode Whether to use dark mode colors
     * @return A map containing appropriate colors for the specified mode
     */
    public static Map<String, String> createColorMap(Bundle extras, boolean isDarkMode) {
        Map<String, String> colorMap = new HashMap<>();

        // Process each color key
        for (String key : COLOR_KEYS) {
            String color = getDarkModeAdaptiveColor(extras, isDarkMode, key);
            colorMap.put(key, color);
        }
        
        return colorMap;
    }

    /**
     * Gets color value based on dark mode preference
     * @param extras The extras bundle containing color values
     * @param isDarkMode Whether to use dark mode colors
     * @param key The color key to retrieve
     * @return The appropriate color for the specified mode
     */
    static String getDarkModeAdaptiveColor(Bundle extras, boolean isDarkMode, String key) {
        String colorDark = extras.getString(key + PT_DARK_MODE_SUFFIX);
        String color = extras.getString(key);

        if (isDarkMode && colorDark != null) {
            return colorDark;
        } else {
            return color;
        }
    }



    public static void loadImageBitmapIntoRemoteView(int imageViewID, Bitmap image,
            RemoteViews remoteViews) {
        remoteViews.setImageViewBitmap(imageViewID, image);
    }

    public static void loadImageURLIntoRemoteView(int imageViewID, String imageUrl,
                                                  RemoteViews remoteViews, Context context) {
        loadImageURLIntoRemoteView(imageViewID, imageUrl, remoteViews, context, null);
    }
    public static void loadImageURLIntoRemoteView(int imageViewID, String imageUrl,
                                                  RemoteViews remoteViews, Context context, String altText) {

        long bmpDownloadStartTimeInMillis = System.currentTimeMillis();
        Bitmap image = getBitmapFromURL(imageUrl, context);
        setFallback(false);

        if (image != null) {
            remoteViews.setImageViewBitmap(imageViewID, image);
            if (!TextUtils.isEmpty(altText)) {
                remoteViews.setContentDescription(imageViewID, altText);
            }
            long bmpDownloadEndTimeInMillis = System.currentTimeMillis();
            long pift = bmpDownloadEndTimeInMillis - bmpDownloadStartTimeInMillis;
            PTLog.verbose("Fetched IMAGE " + imageUrl + " in " + pift + " millis");
        } else {
            PTLog.debug("Image was not perfect. URL:" + imageUrl + " hiding image view");
            setFallback(true);
        }
    }

    public static void loadImageRidIntoRemoteView(int imageViewID, int resourceID,
            RemoteViews remoteViews) {
        remoteViews.setImageViewResource(imageViewID, resourceID);
    }

    public static String getTimeStamp(Context context, long timeMillis) {
        return DateUtils.formatDateTime(context, timeMillis,
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

    public static int getTimerEnd(Bundle extras, long currentTs) {
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
        int diff = (int) (Long.parseLong(val) - (currentTs / 1000));
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

    public static Bitmap setBitMapColour(Context context, int resourceID, String clr, String defaultClr) {
        int color = getColour(clr, defaultClr);

        try {
            Drawable mDrawable = ContextCompat.getDrawable(context, resourceID);
            if (mDrawable == null) {
                return null;
            }

            mDrawable = mDrawable.mutate();
            mDrawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            return drawableToBitmap(mDrawable);
        } catch (Exception e) {
            return null;
        }
    }

    public static int getColour(String clr, String default_clr) {
        try {
            return Color.parseColor(clr);
        } catch (Exception e) {
            PTLog.debug("Can not parse colour value: " + clr + " Switching to default colour: " + default_clr);
            return Color.parseColor(default_clr);
        }
    }

    /**
     * Safely parses a color string (e.g., "#RRGGBB" or "#AARRGGBB") into an integer color value.
     * <p>
     * If the input is null, empty, or an invalid color format, this method returns {@code null} instead of throwing an exception.
     * </p>
     *
     * @param clr the color string to parse (e.g., "#FF0000" for red)
     * @return the parsed color as an {@link Integer}, or {@code null} if parsing fails
     */
    @Nullable
    public static Integer getColourOrNull(String clr) {
        try {
            return Color.parseColor(clr);
        } catch (Exception e) {
            PTLog.debug("Can not parse colour value: " + clr);
            return null;
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
