package com.clevertap.android.sdk

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.clevertap.android.sdk.Constants.AUTH
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import com.clevertap.android.sdk.utils.Clock
import com.google.firebase.messaging.RemoteMessage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale
import java.util.Scanner
import java.util.regex.Pattern
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Bucket D/E: used by geofence, hms, pushtemplates and the wrapper SDKs (Leanplum, react-native).
 * An `object` whose members are all `@JvmStatic`, so every former `Utils.x(...)` static method keeps
 * its JVM signature and Kotlin call sites still compile to static calls.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public object Utils {
    private const val TAG = "Utils"

    private val normalizedNameExcludePattern: Pattern = Pattern.compile("\\s+")

    @JvmStatic
    public fun containsIgnoreCase(collection: Collection<String?>?, key: String?): Boolean {
        if (collection == null || key == null) {
            return false
        }
        return collection.any { key.equals(it, ignoreCase = true) }
    }

    @JvmStatic
    @Suppress("DEPRECATION")
    public fun convertBundleObjectToHashMap(b: Bundle): HashMap<String, Any> {
        // Bundle values may be null; the Java map accepted them as-is.
        val map = HashMap<String, Any?>()
        for (s in b.keySet()) {
            when (val o = b.get(s)) {
                is Bundle -> map.putAll(convertBundleObjectToHashMap(o))
                else -> map[s] = o
            }
        }
        @Suppress("UNCHECKED_CAST")
        return map as HashMap<String, Any>
    }

    @JvmStatic
    public fun convertJSONArrayOfJSONObjectsToArrayListOfHashMaps(
        jsonArray: JSONArray?
    ): ArrayList<HashMap<String, Any>> {
        val hashMapArrayList = ArrayList<HashMap<String, Any>>()
        if (jsonArray == null) {
            return hashMapArrayList
        }
        for (i in 0 until jsonArray.length()) {
            try {
                hashMapArrayList.add(convertJSONObjectToHashMap(jsonArray.getJSONObject(i)))
            } catch (e: JSONException) {
                Logger.v(TAG, "Could not convert JSONArray of JSONObjects to ArrayList of HashMaps", e)
            }
        }
        return hashMapArrayList
    }

    @JvmStatic
    public fun convertJSONArrayToArrayList(array: JSONArray?): ArrayList<String> {
        val listData = ArrayList<String>()
        if (array == null) {
            return listData
        }
        for (i in 0 until array.length()) {
            try {
                listData.add(array.getString(i))
            } catch (e: JSONException) {
                Logger.v(TAG, "Could not convert JSONArray to ArrayList", e)
            }
        }
        return listData
    }

    @JvmStatic
    public fun convertJSONObjectToHashMap(b: JSONObject): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        for (s in b.keys()) {
            try {
                when (val o = b.get(s)) {
                    is JSONObject -> map.putAll(convertJSONObjectToHashMap(o))
                    else -> map[s] = o
                }
            } catch (ignored: Throwable) {
                // Ignore
            }
        }
        return map
    }

    @JvmStatic
    public fun convertToTitleCase(text: String?): String? {
        if (text.isNullOrEmpty()) {
            return text
        }

        var convertNext = true
        return buildString {
            for (c in text) {
                val ch = when {
                    Character.isSpaceChar(c) -> {
                        convertNext = true
                        c
                    }

                    convertNext -> {
                        convertNext = false
                        Character.toTitleCase(c)
                    }

                    else -> Character.toLowerCase(c)
                }
                append(ch)
            }
        }
    }

    // used by inapp.
    @JvmStatic
    public fun getBitmapFromURL(srcUrl: String): Bitmap? {
        val bitmapDownloadRequest = BitmapDownloadRequest(srcUrl)
        return HttpBitmapLoader.getHttpBitmap(HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP, bitmapDownloadRequest).bitmap
    }

    @JvmStatic
    public fun getNowInMillis(): Long = System.currentTimeMillis()

    @JvmStatic
    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    public fun getDeviceNetworkType(context: Context): String {
        // Fall back to network type
        val teleMan = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            ?: return "Unavailable"

        var networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (hasPermission(context, Manifest.permission.READ_BASIC_PHONE_STATE) || hasPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                )
            ) {
                try {
                    networkType = teleMan.dataNetworkType
                } catch (se: SecurityException) {
                    Logger.d(TAG, "Security Exception caught while fetch network type", se)
                }
            } else {
                Logger.d(
                    TAG,
                    "READ_PHONE_STATE or READ_BASIC_PHONE_STATE permission not asked by the app or not granted by the user"
                )
            }
        } else {
            networkType = teleMan.networkType
        }

        return when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            TelephonyManager.NETWORK_TYPE_CDMA,
            TelephonyManager.NETWORK_TYPE_1xRTT,
            TelephonyManager.NETWORK_TYPE_IDEN -> "2G"

            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_EVDO_0,
            TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_EVDO_B,
            TelephonyManager.NETWORK_TYPE_EHRPD,
            TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"

            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            else -> "Unknown"
        }
    }

    @JvmStatic
    public fun getMemoryConsumption(): Long {
        val free = Runtime.getRuntime().freeMemory()
        val total = Runtime.getRuntime().totalMemory()
        return total - free
    }

    @JvmStatic
    public fun getDownloadedBitmapPostFallbackIconCheck(
        fallbackToAppIcon: Boolean, context: Context?,
        ic: DownloadedBitmap
    ): DownloadedBitmap = when {
        ic.bitmap != null -> ic
        fallbackToAppIcon -> getAppIcon(context)
        else -> ic
    }

    /**
     * get bitmap from url within defined timeoutMillis bound and sizeBytes bound or else return
     * null or app icon based on fallbackToAppIcon param
     */
    @JvmStatic
    @Throws(NullPointerException::class)
    public fun getNotificationBitmapWithTimeoutAndSize(
        icoPath: String?, fallbackToAppIcon: Boolean,
        context: Context?, config: CleverTapInstanceConfig?, timeoutMillis: Long, sizeBytes: Int
    ): DownloadedBitmap {
        val bitmapDownloadRequest = BitmapDownloadRequest(
            icoPath, fallbackToAppIcon,
            context, config, timeoutMillis, sizeBytes
        )
        return HttpBitmapLoader.getHttpBitmap(
            HttpBitmapOperation.DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT,
            bitmapDownloadRequest
        )
    }

    @JvmStatic
    @Throws(NullPointerException::class)
    public fun getNotificationBitmapWithTimeout(
        icoPath: String?, fallbackToAppIcon: Boolean,
        context: Context?, config: CleverTapInstanceConfig?, timeoutMillis: Long
    ): DownloadedBitmap {
        val bitmapDownloadRequest = BitmapDownloadRequest(
            icoPath, fallbackToAppIcon,
            context, config, timeoutMillis
        )
        return HttpBitmapLoader.getHttpBitmap(
            HttpBitmapOperation.DOWNLOAD_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT,
            bitmapDownloadRequest
        )
    }

    /**
     * Downloads an animated GIF from URL and returns the downloaded bytes
     */
    private fun downloadGif(url: String?, context: Context?, config: CleverTapInstanceConfig): DownloadedBitmap? {
        try {
            if (url == null || !url.lowercase(Locale.getDefault()).endsWith(".gif")) {
                return null
            }

            val downloadRequest = BitmapDownloadRequest(
                url,
                false,
                context,
                config,
                Constants.PN_IMAGE_DOWNLOAD_TIMEOUT_IN_MILLIS,  // downloadTimeLimitInMillis
                -1
            )

            val downloadedBitmap = HttpBitmapLoader.getHttpBitmap(
                HttpBitmapOperation.DOWNLOAD_BYTES_WITH_TIME_LIMIT,
                downloadRequest
            )

            config.logger.debug(config.accountId, "Downloaded GIF in : ${downloadedBitmap.downloadTime}")

            return if (downloadedBitmap.status == DownloadedBitmap.Status.SUCCESS && downloadedBitmap.bytes != null) {
                downloadedBitmap
            } else {
                config.logger.debug(config.accountId, "Failed to download gif ${downloadedBitmap.status.statusValue}")
                null
            }
        } catch (e: Exception) {
            config.logger.debug(
                config.accountId,
                "Couldn't download gif for notification: ", e
            )
            return null
        }
    }

    /**
     * Saves GIF bytes to a file and returns a content URI
     */
    @RequiresApi(26)
    private fun saveGifToFileAndGetUri(
        gifBytes: ByteArray?,
        context: Context,
        config: CleverTapInstanceConfig,
        clock: Clock
    ): Uri? {
        try {
            val pushDir = context.getDir(Constants.PUSH_DIRECTORY_NAME, Context.MODE_PRIVATE)
            if (pushDir == null) {
                config.logger.debug(
                    config.accountId,
                    "CleverTap.Push dir not available for gif"
                )
                return null
            }

            val file = File(pushDir, "${clock.currentTimeMillis()}.gif")
            Files.write(file.toPath(), gifBytes)

            // Return content URI using FileProvider
            return FileProvider.getUriForFile(context, "${context.packageName}.clevertap.fileprovider", file)
        } catch (e: Exception) {
            config.logger.debug(
                config.accountId,
                "Failed to write gif to file or create URI: $e"
            )
            return null
        }
    }

    /**
     * Saves an animated image from URL to a file and returns a content URI
     * This is the main function that orchestrates the two operations
     */
    @JvmStatic
    @RequiresApi(26)
    internal fun getNotificationGifURI(
        url: String?,
        context: Context,
        config: CleverTapInstanceConfig,
        clock: Clock
    ): Uri? {
        // Download the GIF
        val downloadedBitmap = downloadGif(url, context, config) ?: return null

        // Save to file and get URI
        return saveGifToFileAndGetUri(downloadedBitmap.bytes, context, config, clock)
    }

    /**
     * Cleans up old GIF image files from the push notification cache directory.
     * Only removes GIF files that are one day (24 hours) or older, based on the timestamp
     * embedded in their filename.
     *
     * This method expects GIF files to be named in the format "timestamp.gif" where
     * timestamp is the creation time in milliseconds (e.g., "1234567890123.gif").
     * Files that don't match this naming convention will be skipped.
     *
     * @param context The application context used to access the cache directory
     * @param config The CleverTap instance configuration containing logger and account information
     */
    @JvmStatic
    internal fun cleanupOldGIFs(context: Context, config: CleverTapInstanceConfig, clock: Clock) {
        val cacheDir: File? = context.getDir(Constants.PUSH_DIRECTORY_NAME, Context.MODE_PRIVATE)
        try {
            if (cacheDir == null || !cacheDir.exists()) {
                return
            }

            val files = cacheDir.listFiles() ?: return

            val currentTimeMillis = clock.currentTimeMillis()
            var deletedCount = 0
            for (file in files) {
                if (!file.isFile || !file.name.endsWith(".gif")) {
                    continue
                }
                try {
                    val fileName = file.name
                    val fileTimestamp = fileName.substring(0, fileName.lastIndexOf(".gif")).toLong()

                    // Check if file is one day or older
                    if (currentTimeMillis - fileTimestamp >= Constants.ONE_DAY_IN_MILLIS) {
                        if (file.delete()) {
                            deletedCount++
                        } else {
                            config.logger.debug(config.accountId, "Failed to delete old GIF file: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    config.logger.debug(config.accountId, "Skipping file with invalid file name format: ${file.name}")
                }
            }
            if (deletedCount > 0) {
                config.logger.debug(
                    config.accountId,
                    "Cleaned up $deletedCount old animated notification files"
                )
            }
        } catch (e: Exception) {
            config.logger.debug(
                config.accountId,
                "Error during animated image cleanup: ", e
            )
        }
    }

    @JvmStatic
    public fun getThumbnailImage(context: Context?, image: String?): Int =
        context?.resources?.getIdentifier(image, "drawable", context.packageName) ?: -1

    /**
     * Checks whether a particular permission is available or not.
     *
     * @param context    The Android [Context]
     * @param permission The fully qualified Android permission name
     */
    @JvmStatic
    public fun hasPermission(context: Context, permission: String): Boolean = try {
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission)
    } catch (t: Throwable) {
        false
    }

    @JvmStatic
    public fun isActivityDead(activity: Activity?): Boolean =
        activity == null || activity.isFinishing || activity.isDestroyed

    @JvmStatic
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun isServiceAvailable(context: Context, clazz: Class<*>?): Boolean {
        if (clazz == null) {
            return false
        }

        try {
            val packageInfo: PackageInfo =
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SERVICES)
            // Java iterated the array without a null check: an app without services still fails here.
            val services = checkNotNull(packageInfo.services) { "package has no services" }
            val serviceInfo = services.firstOrNull { it.name == clazz.name }
            if (serviceInfo != null) {
                Logger.v(TAG, "Service ${serviceInfo.name} found")
                return true
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.d(TAG, "Intent Service name not found exception - ", e)
        }
        return false
    }

    @JvmStatic
    @Throws(JSONException::class)
    public fun optionalStringKey(o: JSONObject, k: String): String? =
        if (o.has(k) && !o.isNull(k)) o.getString(k) else null

    /**
     * Handy method to post any runnable to run on the main thread.
     *
     * @param runnable - task to be run
     */
    @JvmStatic
    public fun runOnUiThread(runnable: Runnable?) {
        runnable ?: return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            //run if already on the UI thread
            runnable.run()
        } else {
            //post on UI thread if called from Non-UI thread.
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }

    @JvmStatic
    public fun setPackageNameFromResolveInfoList(context: Context, launchIntent: Intent) {
        val resolveInfoList: List<android.content.pm.ResolveInfo> =
            context.packageManager.queryIntentActivities(launchIntent, 0) ?: return
        val appPackageName = context.packageName
        if (resolveInfoList.any { appPackageName == it.activityInfo.packageName }) {
            launchIntent.setPackage(appPackageName)
        }
    }

    /**
     * @param content String which contains bundle information
     * @return Bundle to be passed to createNotification(Context context, Bundle extras)
     */
    @JvmStatic
    @Throws(JSONException::class)
    public fun stringToBundle(content: String?): Bundle {
        val bundle = Bundle()
        if (content.isNullOrEmpty()) {
            return bundle
        }
        val jsonObject = JSONObject(content)
        for (key in jsonObject.keys()) {
            bundle.putString(key, jsonObject.getString(key))
        }
        return bundle
    }

    @JvmStatic
    public fun validateCTID(cleverTapID: String?): Boolean {
        val error = when {
            cleverTapID == null ->
                "CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is NULL."

            cleverTapID.isEmpty() ->
                "CLEVERTAP_USE_CUSTOM_ID has been set as 1 in AndroidManifest.xml but custom CleverTap ID passed is empty."

            cleverTapID.length > 64 -> "Custom CleverTap ID passed is greater than 64 characters. "

            !cleverTapID.matches(Regex("[=|<>;+.A-Za-z0-9()!:$@_-]*")) ->
                "Custom CleverTap ID cannot contain special characters apart from : =,(,),_,!,@,$,|<,>,;,+,. and - "

            else -> return true
        }
        Logger.i(TAG, error)
        return false
    }

    @JvmStatic
    @Throws(NullPointerException::class)
    public fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    @Throws(NullPointerException::class)
    private fun getAppIcon(context: Context?): DownloadedBitmap {
        // A null context fails with an NPE, as declared: first caught by the logo fallback, then
        // thrown from the fallback (no error handling there - handled upstream), exactly as in Java.
        fun requireContext(): Context = context ?: throw NullPointerException("context is null")

        // Try to get the app logo first
        return try {
            val ctx = requireContext()
            val logo = ctx.packageManager.getApplicationLogo(ctx.applicationInfo)
                ?: throw Exception("Logo is null")
            DownloadedBitmapFactory.successBitmap(drawableToBitmap(logo), 0, null)
        } catch (e: Exception) {
            Logger.v(TAG, "Failed to get app logo, falling back to app icon", e)
            // Try to get the app icon now
            val ctx = requireContext()
            DownloadedBitmapFactory.successBitmap(
                drawableToBitmap(ctx.packageManager.getApplicationIcon(ctx.applicationInfo)),
                0, null
            )
        }
    }

    @JvmStatic
    public fun getSCDomain(domain: String): String {
        // Pattern#split(input, limit) matches the former String#split(regex, limit).
        val parts = Pattern.compile("\\.").split(domain, 2)
        return "${parts[0]}.$AUTH.${parts[1]}"
    }

    @JvmStatic
    public fun isRenderFallback(remoteMessage: RemoteMessage, context: Context?): Boolean {
        // Boolean.parseBoolean semantics: only a case-insensitive "true" is true.
        val renderRateKillSwitch = "true".equals(remoteMessage.data[Constants.WZRK_TSR_FB], ignoreCase = true) //tsrfb
        val renderRateFallback =
            "true".equals(remoteMessage.data[Constants.NOTIFICATION_RENDER_FALLBACK], ignoreCase = true)

        return !renderRateKillSwitch && renderRateFallback
    }

    @JvmStatic
    public fun navigateToAndroidSettingsForNotifications(context: Context) {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", context.packageName)
                putExtra("app_uid", context.applicationInfo.uid)
            }
        }
        context.startActivity(intent)
    }

    @JvmStatic
    @WorkerThread
    public fun isMainProcess(context: Context, mainProcessName: String?): Boolean = try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        // Java dereferenced both without null checks; that failure is caught below.
        val processInfos = checkNotNull(am?.runningAppProcesses) { "running app processes unavailable" }
        val myPid = Process.myPid()
        processInfos.any { info ->
            info.pid == myPid && checkNotNull(mainProcessName) { "main process name is null" } == info.processName
        }
    } catch (e: Exception) {
        Logger.v(TAG, "Error checking if main process", e)
        false
    }

    @JvmStatic
    public fun toJSONObjectList(jsonArray: JSONArray): MutableList<JSONObject> =
        (0 until jsonArray.length()).mapNotNullTo(ArrayList()) { jsonArray.optJSONObject(it) }

    /**
     * Calculates the haversine distance between two locations on Earth.
     *
     * @param coordinateA The first location.
     * @param coordinateB The second location.
     * @return The haversine distance between the two locations, in kilometers.
     */
    @JvmStatic
    public fun haversineDistance(coordinateA: Location, coordinateB: Location): Double {
        // The Earth radius ranges from a maximum of about 6378 km (equatorial)
        // to a minimum of about 6357 km (polar).
        // A globally-average value is usually considered to be 6371 km (6371e3).
        // This method uses 6378.2 km as the radius since this is the value
        // used by the backend, and calculations should produce the same result.
        val earthDiameter = 2 * 6378.2

        val radConvert = PI / 180
        val phi1 = coordinateA.latitude * radConvert
        val phi2 = coordinateB.latitude * radConvert

        val deltaPhi = (coordinateB.latitude - coordinateA.latitude) * radConvert
        val deltaLambda = (coordinateB.longitude - coordinateA.longitude) * radConvert

        val sinPhi = sin(deltaPhi / 2)
        val sinLambda = sin(deltaLambda / 2)

        val a = sinPhi * sinPhi + cos(phi1) * cos(phi2) * sinLambda * sinLambda
        // Distance in km
        return earthDiameter * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Reads the content of a file from the "assets" folder.
     *
     * @param context  The application context.
     * @param fileName The name of the file to be read from the "assets" folder.
     * @return The content of the file as a String.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun readAssetFile(context: Context, fileName: String): String? {
        val assetManager = context.assets
        assetManager.open(fileName).use { inputStream ->
            // Read the entire content of the file into a String
            return Scanner(inputStream).useDelimiter("\\A").next()
        }
    }

    /**
     * Get the CT normalized version of an event or a property name.
     *
     * @param name The event/property name
     */
    @JvmStatic
    public fun getNormalizedName(name: String?): String? {
        name ?: return null
        // lowercase with English locale for consistent behavior with the backend and across different device locales
        return normalizedNameExcludePattern.matcher(name).replaceAll("").lowercase(Locale.ENGLISH)
    }

    /**
     * Check if two event/property names are equal with applied CT normalization
     *
     * @param name  Event or property name
     * @param other Event or property name to compare to
     * @see getNormalizedName
     */
    @JvmStatic
    public fun areNamesNormalizedEqual(name: String?, other: String?): Boolean =
        getNormalizedName(name) == getNormalizedName(other)
}
