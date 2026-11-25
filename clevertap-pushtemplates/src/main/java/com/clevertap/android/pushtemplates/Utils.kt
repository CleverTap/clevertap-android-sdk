package com.clevertap.android.pushtemplates

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.clevertap.android.pushtemplates.PTConstants.Companion.ALT_TEXT_SUFFIX
import com.clevertap.android.pushtemplates.PTConstants.Companion.COLOR_KEYS
import com.clevertap.android.pushtemplates.PTConstants.Companion.PT_DARK_MODE_SUFFIX
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.task.CTExecutorFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

/*
 * Several helpers below collect Bundle.getString() results into ArrayList<String> / Map<String, String>.
 * The Java version did the same through platform types, so a missing/non-string value travelled as a
 * null element. The lists are built with nullable elements and handed out under the non-null type the
 * Kotlin callers expect, which keeps that runtime behaviour byte-for-byte.
 */
@Suppress("WeakerAccess")
internal object Utils {

    @JvmStatic
    @Throws(NullPointerException::class)
    fun getAppIcon(context: Context?): Bitmap {
        // Try to get the app logo first
        return try {
            val logo = context!!.packageManager.getApplicationLogo(context.applicationInfo)
                ?: throw Exception("Logo is null")
            drawableToBitmap(logo)
        } catch (e: Exception) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            drawableToBitmap(
                context!!.packageManager.getApplicationIcon(context.applicationInfo)
            )
        }
    }

    @JvmStatic
    @Throws(NullPointerException::class)
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    @JvmStatic
    @Suppress("SameParameterValue")
    fun _getManifestStringValueForKey(manifest: Bundle?, name: String?): String? {
        return try {
            @Suppress("DEPRECATION")
            val o = manifest!!.get(name)
            o?.toString()
        } catch (t: Throwable) {
            null
        }
    }

    @JvmStatic
    fun getAppIconAsIntId(context: Context): Int {
        val ai = context.applicationInfo
        return ai.icon
    }

    @JvmStatic
    fun getImageDataListFromExtras(extras: Bundle, defaultAltText: String?): ArrayList<ImageData> {
        val imageList = ArrayList<ImageData>()
        var counter = 1
        for (key in extras.keySet()) {
            if (key.startsWith("pt_img") && key.length > 6 && Character.isDigit(key[6]) && !key.endsWith(
                    ALT_TEXT_SUFFIX
                )
            ) {
                val imageUrl = extras.getString(key)
                val altText = extras.getString(key + ALT_TEXT_SUFFIX, defaultAltText + counter)
                imageList.add(ImageData(imageUrl, altText))
                counter++
            }
        }
        return imageList
    }

    @JvmStatic
    @Suppress("unused")
    fun getCTAListFromExtras(extras: Bundle): ArrayList<String> {
        val ctaList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_cta")) {
                ctaList.add(extras.getString(key))
            }
        }
        return ctaList.asPlatformList()
    }

    @JvmStatic
    fun getDeepLinkListFromExtras(extras: Bundle): ArrayList<String> {
        val dlList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_dl")) {
                dlList.add(extras.getString(key))
            }
        }
        return dlList.asPlatformList()
    }

    @JvmStatic
    fun getBigTextFromExtras(extras: Bundle): ArrayList<String> {
        val btList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_bt")) {
                btList.add(extras.getString(key))
            }
        }
        return btList.asPlatformList()
    }

    @JvmStatic
    fun getSmallTextFromExtras(extras: Bundle): ArrayList<String> {
        val stList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.startsWith("pt_st") && key.length > 5
                && Character.isDigit(key[5])
            ) {
                stList.add(extras.getString(key))
            }
        }
        return stList.asPlatformList()
    }

    @JvmStatic
    fun getPriceFromExtras(extras: Bundle): ArrayList<String> {
        val stList = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_price") && !key.contains("pt_price_list")) {
                stList.add(extras.getString(key))
            }
        }
        return stList.asPlatformList()
    }

    /**
     * Creates a map of colors for the specified display mode (dark/light)
     * @param extras The original extras bundle containing all color values
     * @param isDarkMode Whether to use dark mode colors
     * @return A map containing appropriate colors for the specified mode
     */
    @JvmStatic
    fun createColorMap(extras: Bundle, isDarkMode: Boolean): Map<String, String> {
        val colorMap = HashMap<String, String?>()

        // Process each color key
        for (key in COLOR_KEYS) {
            val color = getDarkModeAdaptiveColor(extras, isDarkMode, key)
            colorMap[key] = color
        }

        @Suppress("UNCHECKED_CAST")
        return colorMap as Map<String, String>
    }

    /**
     * Gets color value based on dark mode preference
     * @param extras The extras bundle containing color values
     * @param isDarkMode Whether to use dark mode colors
     * @param key The color key to retrieve
     * @return The appropriate color for the specified mode
     */
    @JvmStatic
    fun getDarkModeAdaptiveColor(extras: Bundle, isDarkMode: Boolean, key: String): String? {
        val colorDark = extras.getString(key + PT_DARK_MODE_SUFFIX)
        val color = extras.getString(key)

        return if (isDarkMode && colorDark != null) {
            colorDark
        } else {
            color
        }
    }

    @JvmStatic
    fun isDarkMode(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    @JvmStatic
    fun getTimeStamp(context: Context?, timeMillis: Long): String {
        return DateUtils.formatDateTime(
            context, timeMillis,
            DateUtils.FORMAT_SHOW_TIME
        )
    }

    @JvmStatic
    fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            context.getString(stringId)
        }
    }

    @JvmStatic
    fun fromJson(s: JSONObject): Bundle {
        val bundle = Bundle()

        val it = s.keys()
        while (it.hasNext()) {
            val key = it.next()
            val arr: JSONArray? = s.optJSONArray(key)
            val str: String? = s.optString(key)

            if (arr != null && arr.length() <= 0) {
                bundle.putStringArray(key, arrayOf())
            } else if (arr != null && arr.optString(0) != null) {
                val newarr = arrayOfNulls<String>(arr.length())
                for (i in 0 until arr.length()) {
                    newarr[i] = arr.optString(i)
                }
                bundle.putStringArray(key, newarr)
            } else if (str != null) {
                bundle.putString(key, str)
            } else {
                System.err.println("unable to transform json to bundle $key")
            }
        }

        return bundle
    }

    @JvmStatic
    fun cancelNotification(ctx: Context?, notifyId: Int) {
        val nMgr = ctx!!.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nMgr.cancel(notifyId)
    }

    @JvmStatic
    fun getTimerThreshold(extras: Bundle): Int {
        var value: String? = "-1"
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_THRESHOLD)) {
                value = extras.getString(key)
            }
        }
        return Integer.parseInt(value ?: "-1")
    }

    @JvmStatic
    fun setPackageNameFromResolveInfoList(context: Context, launchIntent: Intent) {
        val resolveInfoList = context.packageManager.queryIntentActivities(launchIntent, 0)
        val appPackageName = context.packageName
        for (resolveInfo in resolveInfoList) {
            if (appPackageName == resolveInfo.activityInfo.packageName) {
                launchIntent.setPackage(appPackageName)
                break
            }
        }
    }

    @JvmStatic
    fun raiseCleverTapEvent(context: Context?, config: CleverTapInstanceConfig?, extras: Bundle) {
        val instance: CleverTapAPI? = if (config != null) {
            CleverTapAPI.instanceWithConfig(context, config)
        } else {
            CleverTapAPI.getDefaultInstance(context)
        }

        val eProps: HashMap<String, Any?> = getEventPropertiesFromExtras(extras)

        val eName = getEventNameFromExtras(extras)

        if (eName != null && eName.isNotEmpty()) {
            if (instance != null) {
                instance.pushEvent(eName, eProps)
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event")
            }
        }
    }

    @JvmStatic
    fun raiseCleverTapEvent(context: Context?, config: CleverTapInstanceConfig?, extras: Bundle, key: String?) {
        val instance: CleverTapAPI? = if (config != null) {
            CleverTapAPI.instanceWithConfig(context, config)
        } else {
            CleverTapAPI.getDefaultInstance(context)
        }

        val value = extras.getString(key)

        val eProps: HashMap<String, Any?> = getEventPropertiesFromExtras(extras, key, value)

        val eName = getEventNameFromExtras(extras)

        if (eName != null && eName.isNotEmpty()) {
            if (instance != null) {
                instance.pushEvent(eName, eProps)
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event")
            }
        }
    }

    @JvmStatic
    fun raiseCleverTapEvent(
        context: Context?, config: CleverTapInstanceConfig?, evtName: String?,
        eProps: HashMap<String, *>?
    ) {
        val instance: CleverTapAPI? = if (config != null) {
            CleverTapAPI.instanceWithConfig(context, config)
        } else {
            CleverTapAPI.getDefaultInstance(context)
        }

        if (evtName != null && evtName.isNotEmpty()) {
            if (instance != null) {
                instance.pushEvent(evtName, eProps)
            } else {
                PTLog.debug("CleverTap instance is NULL, not raising the event")
            }
        }
    }

    @JvmStatic
    fun convertRatingBundleObjectToHashMap(b: Bundle): HashMap<String, Any?> {
        b.remove("config")
        val map = HashMap<String, Any?>()
        for (s in b.keySet()) {
            if (s.contains("wzrk_") || s == PTConstants.PT_ID) {
                @Suppress("DEPRECATION")
                val o = b.get(s)
                if (o is Bundle) {
                    map.putAll(convertRatingBundleObjectToHashMap(o))
                } else {
                    @Suppress("DEPRECATION")
                    map[s] = b.get(s)
                }
            }
        }
        return map
    }

    @JvmStatic
    fun getEventNameFromExtras(extras: Bundle): String? {
        var eName: String? = null
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_NAME_KEY)) {
                eName = extras.getString(key)
            }
        }
        return eName
    }

    @JvmStatic
    fun getEventPropertiesFromExtras(extras: Bundle, pkey: String?, value: String?): HashMap<String, Any?> {
        val eProps = HashMap<String, Any?>()

        var eProp: Array<String>
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                val propValue = extras.getString(key)
                if (propValue != null && propValue.isNotEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        eProp = Pattern.compile(PTConstants.PT_EVENT_PROPERTY_SEPERATOR).split(key)
                        if (propValue.equals(pkey, ignoreCase = true)) {
                            eProps[eProp[1]] = value
                            continue
                        }
                        eProps[eProp[1]] = propValue
                    } else {
                        PTLog.verbose("Property $key does not have the separator")
                    }
                } else {
                    PTLog.verbose("Property Key is Empty. Skipping Property: $key")
                }
            }
        }
        return eProps
    }

    @JvmStatic
    fun getEventPropertiesFromExtras(extras: Bundle): HashMap<String, Any?> {
        val eProps = HashMap<String, Any?>()

        var eProp: Array<String>
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                val propValue = extras.getString(key)
                if (propValue != null && propValue.isNotEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        eProp = Pattern.compile(PTConstants.PT_EVENT_PROPERTY_SEPERATOR).split(key)
                        eProps[eProp[1]] = propValue
                    } else {
                        PTLog.verbose("Property $key does not have the separator")
                    }
                } else {
                    PTLog.verbose("Property Key is Empty. Skipping Property: $key")
                }
            }
        }
        return eProps
    }

    @JvmStatic
    fun getTimerEnd(extras: Bundle, currentTs: Long): Int {
        var value: String? = "-1"
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_END)) {
                value = extras.getString(key)
            }
        }
        if (value!!.contains("\$D_")) {
            val temp = Pattern.compile(PTConstants.PT_TIMER_SPLIT).split(value)
            value = temp[1]
        }
        val diff = (java.lang.Long.parseLong(value) - (currentTs / 1000)).toInt()
        if (value == "-1") {
            return Int.MIN_VALUE
        } //For empty check in timer_end
        return diff
    }

    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isNotificationInTray(context: Context, notificationId: Int): Boolean {
        val mNotificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifications = mNotificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == notificationId) {
                return true
            }
        }
        return false
    }

    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getNotificationById(context: Context, notificationId: Int): Notification? {
        val mNotificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifications = mNotificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == notificationId) {
                return notification.notification
            }
        }
        return null
    }

    @JvmStatic
    fun getNotificationIds(context: Context): ArrayList<Int> {
        val ids = ArrayList<Int>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mNotificationManager = context
                .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notifications = mNotificationManager.activeNotifications
            for (notification in notifications) {
                if (notification.packageName.equals(context.packageName, ignoreCase = true)) {
                    ids.add(notification.id)
                }
            }
        }
        return ids
    }

    @JvmStatic
    fun raiseNotificationClicked(context: Context?, extras: Bundle?, config: CleverTapInstanceConfig?) {
        val instance: CleverTapAPI? = if (config != null) {
            CleverTapAPI.instanceWithConfig(context, config)
        } else {
            CleverTapAPI.getDefaultInstance(context)
        }
        instance?.pushNotificationClickedEvent(extras)
    }

    @JvmStatic
    fun getActionKeys(extras: Bundle): JSONArray? {
        var actions: JSONArray? = null

        val actionsString = extras.getString(Constants.WZRK_ACTIONS)
        if (actionsString != null) {
            try {
                actions = JSONArray(actionsString)
            } catch (t: Throwable) {
                PTLog.debug("error parsing notification actions: " + t.localizedMessage)
            }
        }
        return actions
    }

    @JvmStatic
    fun showToast(
        context: Context?, message: String?,
        config: CleverTapInstanceConfig?
    ) {
        if (config != null) {
            val task = CTExecutorFactory.executors(config).mainTask<Void?>()
            task.execute("PushTemplatesUtils#showToast") {
                if (!TextUtils.isEmpty(message)) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }

    @JvmStatic
    fun createSilentNotificationChannel(context: Context) {
        val notificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            ?: return
        val notificationChannel: NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) == null || (
                        notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) != null
                                && !isNotificationChannelEnabled(
                            notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)
                        ))
            ) {
                val soundUri: Uri? = Uri
                    .parse(
                        ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/raw/"
                                + PTConstants.PT_SOUND_FILE_NAME
                    )
                notificationChannel = NotificationChannel(
                    PTConstants.PT_SILENT_CHANNEL_ID,
                    PTConstants.PT_SILENT_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                )
                if (soundUri != null) {
                    notificationChannel.setSound(
                        soundUri,
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                    )
                }
                notificationChannel.description = PTConstants.PT_SILENT_CHANNEL_DESC
                notificationChannel.setShowBadge(false)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }
    }

    @JvmStatic
    fun deleteSilentNotificationChannel(context: Context) {
        val notificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID) != null
                && isNotificationChannelEnabled(
                    notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)
                )
            ) {
                notificationManager.deleteNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)
            }
        }
    }

    @JvmStatic
    fun isNotificationChannelEnabled(channel: NotificationChannel?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel != null) {
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        return false
    }

    @JvmStatic
    fun setBitMapColour(context: Context, resourceID: Int, clr: String?, defaultClr: String?): Bitmap? {
        val color = getColour(clr, defaultClr)

        return try {
            var mDrawable = ContextCompat.getDrawable(context, resourceID) ?: return null

            mDrawable = mDrawable.mutate()
            mDrawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            drawableToBitmap(mDrawable)
        } catch (e: Exception) {
            null
        }
    }

    @JvmStatic
    fun getColour(clr: String?, default_clr: String?): Int {
        return try {
            Color.parseColor(clr)
        } catch (e: Exception) {
            PTLog.debug("Can not parse colour value: $clr Switching to default colour: $default_clr")
            Color.parseColor(default_clr)
        }
    }

    /**
     * Safely parses a color string (e.g., "#RRGGBB" or "#AARRGGBB") into an integer color value.
     *
     * If the input is null, empty, or an invalid color format, this method returns `null` instead of throwing an exception.
     *
     * @param clr the color string to parse (e.g., "#FF0000" for red)
     * @return the parsed color as an [Integer], or `null` if parsing fails
     */
    @JvmStatic
    fun getColourOrNull(clr: String?): Int? {
        return try {
            Color.parseColor(clr)
        } catch (e: Exception) {
            PTLog.debug("Can not parse colour value: $clr")
            null
        }
    }

    @JvmStatic
    fun getFlipInterval(extras: Bundle): Int {
        val interval = extras.getString(PTConstants.PT_FLIP_INTERVAL)
        try {
            if (interval != null) {
                val t = Integer.parseInt(interval)
                return Math.max(t, PTConstants.PT_FLIP_INTERVAL_TIME)
            }
        } catch (e: Exception) {
            PTLog.debug(
                "Flip Interval couldn't be converted to number: " + interval + " - Defaulting to base value: "
                        + PTConstants.PT_FLIP_INTERVAL_TIME
            )
        }
        return PTConstants.PT_FLIP_INTERVAL_TIME
    }

    @JvmStatic
    fun deleteImageFromStorage(context: Context, intent: Intent) {
        val pId = intent.getStringExtra(Constants.WZRK_PUSH_ID)

        val cw = ContextWrapper(context.applicationContext)
        val MyDirectory = cw.getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE)
        val path = MyDirectory.absolutePath
        val fileList = MyDirectory.list()
        var fileToBeDeleted: File?
        if (fileList != null) {
            for (fileName in fileList) {
                if (pId != null && fileName.contains(pId)) {
                    fileToBeDeleted = File("$path/$fileName")
                    val wasDeleted = fileToBeDeleted.delete()
                    if (!wasDeleted) {
                        PTLog.debug("Failed to clean up the following file: $fileName")
                    }
                } else if (pId == null && fileName.contains("null")) {
                    fileToBeDeleted = File("$path/$fileName")
                    val wasDeleted = fileToBeDeleted.delete()
                    if (!wasDeleted) {
                        PTLog.debug("Failed to clean up the following file: $fileName")
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArrayList<String?>.asPlatformList(): ArrayList<String> = this as ArrayList<String>
}
