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

    /** Throws a [NullPointerException] for a null [context], for the callers to handle. */
    fun getAppIcon(context: Context?): Bitmap {
        context ?: throw NullPointerException("context")
        // Try to get the app logo first
        return try {
            val logo = context.packageManager.getApplicationLogo(context.applicationInfo)
                ?: throw Exception("Logo is null")
            drawableToBitmap(logo)
        } catch (e: Exception) {
            // Try to get the app icon now
            // No error handling here - handle upstream
            drawableToBitmap(context.packageManager.getApplicationIcon(context.applicationInfo))
        }
    }

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

    @Suppress("DEPRECATION")
    fun getManifestStringValueForKey(manifest: Bundle?, name: String?): String? = try {
        manifest?.get(name)?.toString()
    } catch (t: Throwable) {
        null
    }

    fun getAppIconAsIntId(context: Context): Int = context.applicationInfo.icon

    fun getImageDataListFromExtras(extras: Bundle, defaultAltText: String?): ArrayList<ImageData> {
        val imageList = ArrayList<ImageData>()
        for (key in extras.keySet()) {
            if (key.startsWith("pt_img") && key.length > 6 && key[6].isDigit() && !key.endsWith(ALT_TEXT_SUFFIX)) {
                val imageUrl = extras.getString(key)
                val altText = extras.getString("$key$ALT_TEXT_SUFFIX", "$defaultAltText${imageList.size + 1}")
                imageList.add(ImageData(imageUrl, altText))
            }
        }
        return imageList
    }

    @Suppress("unused")
    fun getCTAListFromExtras(extras: Bundle): ArrayList<String> {
        val values = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_cta")) {
                values.add(extras.getString(key))
            }
        }
        return values.asPlatformList()
    }

    fun getDeepLinkListFromExtras(extras: Bundle): ArrayList<String> {
        val values = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_dl")) {
                values.add(extras.getString(key))
            }
        }
        return values.asPlatformList()
    }

    fun getBigTextFromExtras(extras: Bundle): ArrayList<String> {
        val values = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_bt")) {
                values.add(extras.getString(key))
            }
        }
        return values.asPlatformList()
    }

    fun getSmallTextFromExtras(extras: Bundle): ArrayList<String> {
        val values = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.startsWith("pt_st") && key.length > 5 && key[5].isDigit()) {
                values.add(extras.getString(key))
            }
        }
        return values.asPlatformList()
    }

    fun getPriceFromExtras(extras: Bundle): ArrayList<String> {
        val values = ArrayList<String?>()
        for (key in extras.keySet()) {
            if (key.contains("pt_price") && !key.contains("pt_price_list")) {
                values.add(extras.getString(key))
            }
        }
        return values.asPlatformList()
    }

    /**
     * Creates a map of colors for the specified display mode (dark/light)
     * @param extras The original extras bundle containing all color values
     * @param isDarkMode Whether to use dark mode colors
     * @return A map containing appropriate colors for the specified mode
     */
    fun createColorMap(extras: Bundle, isDarkMode: Boolean): Map<String, String> {
        val colorMap = COLOR_KEYS.associateWithTo(HashMap<String, String?>()) { key ->
            getDarkModeAdaptiveColor(extras, isDarkMode, key)
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
    fun getDarkModeAdaptiveColor(extras: Bundle, isDarkMode: Boolean, key: String): String? {
        val colorDark = extras.getString("$key$PT_DARK_MODE_SUFFIX")
        val color = extras.getString(key)
        return if (isDarkMode && colorDark != null) colorDark else color
    }

    fun isDarkMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun getTimeStamp(context: Context?, timeMillis: Long): String =
        DateUtils.formatDateTime(context, timeMillis, DateUtils.FORMAT_SHOW_TIME)

    fun getApplicationName(context: Context): String {
        val applicationInfo = context.applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            context.getString(stringId)
        }
    }

    fun fromJson(s: JSONObject): Bundle {
        val bundle = Bundle()

        for (key in s.keys()) {
            val arr: JSONArray? = s.optJSONArray(key)
            val str: String? = s.optString(key)

            if (arr != null && arr.length() <= 0) {
                bundle.putStringArray(key, arrayOf())
            } else if (arr != null && arr.optString(0) != null) {
                bundle.putStringArray(key, Array(arr.length()) { i -> arr.optString(i) })
            } else if (str != null) {
                bundle.putString(key, str)
            } else {
                System.err.println("unable to transform json to bundle $key")
            }
        }

        return bundle
    }

    fun cancelNotification(ctx: Context?, notifyId: Int) {
        // Java dereferenced a null context as well (callers log the failure).
        val notificationManager = requireNotNull(ctx) { "cancelNotification needs a context" }
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notifyId)
    }

    /** The value of the last `pt_timer_threshold*` key ("-1" when there is none or it is not a string). */
    fun getTimerThreshold(extras: Bundle): Int {
        var value: String? = "-1"
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_THRESHOLD)) {
                value = extras.getString(key)
            }
        }
        return (value ?: "-1").toInt()
    }

    fun setPackageNameFromResolveInfoList(context: Context, launchIntent: Intent) {
        val appPackageName = context.packageName
        val resolvesToApp = context.packageManager.queryIntentActivities(launchIntent, 0)
            .any { it.activityInfo.packageName == appPackageName }
        if (resolvesToApp) {
            launchIntent.setPackage(appPackageName)
        }
    }

    private fun instanceFor(context: Context?, config: CleverTapInstanceConfig?): CleverTapAPI? =
        if (config != null) {
            CleverTapAPI.instanceWithConfig(context, config)
        } else {
            CleverTapAPI.getDefaultInstance(context)
        }

    fun raiseCleverTapEvent(context: Context?, config: CleverTapInstanceConfig?, extras: Bundle) {
        val instance = instanceFor(context, config)
        val eProps = getEventPropertiesFromExtras(extras)
        pushEventIfNamed(instance, getEventNameFromExtras(extras), eProps)
    }

    fun raiseCleverTapEvent(context: Context?, config: CleverTapInstanceConfig?, extras: Bundle, key: String?) {
        val instance = instanceFor(context, config)
        val eProps = getEventPropertiesFromExtras(extras, key, extras.getString(key))
        pushEventIfNamed(instance, getEventNameFromExtras(extras), eProps)
    }

    fun raiseCleverTapEvent(
        context: Context?, config: CleverTapInstanceConfig?, evtName: String?,
        eProps: Map<String, *>?
    ) {
        pushEventIfNamed(instanceFor(context, config), evtName, eProps)
    }

    private fun pushEventIfNamed(instance: CleverTapAPI?, eventName: String?, eProps: Map<String, *>?) {
        if (eventName.isNullOrEmpty()) {
            return
        }
        if (instance != null) {
            instance.pushEvent(eventName, eProps)
        } else {
            PTLog.debug("CleverTap instance is NULL, not raising the event")
        }
    }

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

    /** The value of the last event name key, as in Java (a later key overrides an earlier one). */
    fun getEventNameFromExtras(extras: Bundle): String? =
        extras.keySet().lastOrNull { it.contains(PTConstants.PT_EVENT_NAME_KEY) }?.let { extras.getString(it) }

    fun getEventPropertiesFromExtras(extras: Bundle, pkey: String?, value: String?): HashMap<String, Any?> {
        val eProps = HashMap<String, Any?>()

        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                val propValue = extras.getString(key)
                if (!propValue.isNullOrEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        val eProp = Pattern.compile(PTConstants.PT_EVENT_PROPERTY_SEPERATOR).split(key)
                        eProps[eProp[1]] = if (propValue.equals(pkey, ignoreCase = true)) value else propValue
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

    fun getEventPropertiesFromExtras(extras: Bundle): HashMap<String, Any?> {
        val eProps = HashMap<String, Any?>()

        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_EVENT_PROPERTY_KEY)) {
                val propValue = extras.getString(key)
                if (!propValue.isNullOrEmpty()) {
                    if (key.contains(PTConstants.PT_EVENT_PROPERTY_SEPERATOR)) {
                        val eProp = Pattern.compile(PTConstants.PT_EVENT_PROPERTY_SEPERATOR).split(key)
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

    fun getTimerEnd(extras: Bundle, currentTs: Long): Int {
        var value: String? = "-1"
        for (key in extras.keySet()) {
            if (key.contains(PTConstants.PT_TIMER_END)) {
                value = extras.getString(key)
            }
        }
        // A non-string timer end threw in Java as well.
        var timerEnd = checkNotNull(value) { "pt_timer_end is not a string" }
        if (timerEnd.contains("\$D_")) {
            timerEnd = Pattern.compile(PTConstants.PT_TIMER_SPLIT).split(timerEnd)[1]
        }
        // Parsed before the "-1" check, as before: a malformed value throws.
        val diff = (timerEnd.toLong() - (currentTs / 1000)).toInt()
        if (timerEnd == "-1") {
            return Int.MIN_VALUE
        } //For empty check in timer_end
        return diff
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun isNotificationInTray(context: Context, notificationId: Int): Boolean =
        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .activeNotifications.any { it.id == notificationId }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getNotificationById(context: Context, notificationId: Int): Notification? =
        (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .activeNotifications.firstOrNull { it.id == notificationId }?.notification

    fun getNotificationIds(context: Context): ArrayList<Int> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return ArrayList()
        }
        return (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .activeNotifications
            .filter { it.packageName.equals(context.packageName, ignoreCase = true) }
            .mapTo(ArrayList()) { it.id }
    }

    fun raiseNotificationClicked(context: Context?, extras: Bundle?, config: CleverTapInstanceConfig?) {
        instanceFor(context, config)?.pushNotificationClickedEvent(extras)
    }

    fun getActionKeys(extras: Bundle): JSONArray? {
        val actionsString = extras.getString(Constants.WZRK_ACTIONS) ?: return null
        return try {
            JSONArray(actionsString)
        } catch (t: Throwable) {
            PTLog.debug("error parsing notification actions: ${t.localizedMessage}")
            null
        }
    }

    fun showToast(
        context: Context?, message: String?,
        config: CleverTapInstanceConfig?
    ) {
        if (config != null) {
            val task = CTExecutorFactory.executors(config).mainTask<Void?>()
            task.execute("PushTemplatesUtils#showToast") {
                if (!message.isNullOrEmpty()) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }

    fun createSilentNotificationChannel(context: Context) {
        val notificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val existingChannel = notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)
        if (existingChannel != null && isNotificationChannelEnabled(existingChannel)) {
            return
        }
        val soundUri: Uri? = Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/${PTConstants.PT_SOUND_FILE_NAME}"
        )
        val notificationChannel = NotificationChannel(
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

    fun deleteSilentNotificationChannel(context: Context) {
        val notificationManager = context
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            isNotificationChannelEnabled(notificationManager.getNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID))
        ) {
            notificationManager.deleteNotificationChannel(PTConstants.PT_SILENT_CHANNEL_ID)
        }
    }

    fun isNotificationChannelEnabled(channel: NotificationChannel?): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channel != null &&
                channel.importance != NotificationManager.IMPORTANCE_NONE

    fun setBitMapColour(context: Context, resourceID: Int, clr: String?, defaultClr: String?): Bitmap? {
        val color = getColour(clr, defaultClr)

        return try {
            val drawable = ContextCompat.getDrawable(context, resourceID)?.mutate() ?: return null
            drawable.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }

    fun getColour(clr: String?, defaultClr: String?): Int = try {
        Color.parseColor(clr)
    } catch (e: Exception) {
        PTLog.debug("Can not parse colour value: $clr Switching to default colour: $defaultClr")
        Color.parseColor(defaultClr)
    }

    /**
     * Safely parses a color string (e.g., "#RRGGBB" or "#AARRGGBB") into an integer color value.
     *
     * If the input is null, empty, or an invalid color format, this method returns `null` instead of throwing an exception.
     *
     * @param clr the color string to parse (e.g., "#FF0000" for red)
     * @return the parsed color as an [Integer], or `null` if parsing fails
     */
    fun getColourOrNull(clr: String?): Int? = try {
        Color.parseColor(clr)
    } catch (e: Exception) {
        PTLog.debug("Can not parse colour value: $clr")
        null
    }

    fun getFlipInterval(extras: Bundle): Int {
        val interval = extras.getString(PTConstants.PT_FLIP_INTERVAL) ?: return PTConstants.PT_FLIP_INTERVAL_TIME
        return try {
            maxOf(interval.toInt(), PTConstants.PT_FLIP_INTERVAL_TIME)
        } catch (e: Exception) {
            PTLog.debug(
                "Flip Interval couldn't be converted to number: $interval - Defaulting to base value: " +
                        "${PTConstants.PT_FLIP_INTERVAL_TIME}"
            )
            PTConstants.PT_FLIP_INTERVAL_TIME
        }
    }

    fun deleteImageFromStorage(context: Context, intent: Intent) {
        val pId = intent.getStringExtra(Constants.WZRK_PUSH_ID)

        val directory = ContextWrapper(context.applicationContext).getDir(PTConstants.PT_DIR, Context.MODE_PRIVATE)
        val path = directory.absolutePath
        // Images are named after their push id; those of a push without one carry "null".
        val fileList = directory.list() ?: return
        for (fileName in fileList) {
            if (fileName.contains(pId ?: "null") && !File("$path/$fileName").delete()) {
                PTLog.debug("Failed to clean up the following file: $fileName")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArrayList<String?>.asPlatformList(): ArrayList<String> = this as ArrayList<String>
}
