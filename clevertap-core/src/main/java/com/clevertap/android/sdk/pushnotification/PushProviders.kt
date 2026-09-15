package com.clevertap.android.sdk.pushnotification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.TextUtils
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.BuildConfig
import com.clevertap.android.sdk.getOrCreateChannel
import com.clevertap.android.sdk.isNotificationChannelEnabled
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapAPI.DevicePushTokenRefreshListener
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.interfaces.AudibleNotification
import com.clevertap.android.sdk.pushnotification.amp.CTPushAmpWorker
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.Task
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PushProviders internal constructor(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val baseDatabaseManager: BaseDatabaseManager,
    private val validationResultStack: ValidationResultStack,
    private val analyticsManager: AnalyticsManager,
    private val ctWorkManager: CTWorkManager,
    private val clock: Clock
) : CTPushProviderListener {

    private val allEnabledPushTypes = ArrayList<PushType>()

    private val availableCTPushProviders = ArrayList<CTPushProvider>()

    private val nonEnabledPushTypes = ArrayList<PushType>()

    private var iNotificationRenderer: INotificationRenderer = CoreNotificationRenderer()

    private val tokenLock = Any()

    private val pushRenderingLock = Any()

    private var tokenRefreshListener: DevicePushTokenRefreshListener? = null

    init {
        initPushAmp()
    }

    /**
     * Launches an asynchronous task to download the notification icon from CleverTap,
     * and create the Android notification.
     *
     * If your app is using CleverTap SDK's built in FCM message handling,
     * this method does not need to be called explicitly.
     *
     * Use this method when implementing your own FCM handling mechanism. Refer to the
     * SDK documentation for usage scenarios and examples.
     */
    fun _createNotification(context: Context, extras: Bundle?, notificationId: Int) {
        if (extras == null || extras.get(Constants.NOTIFICATION_TAG) == null) {
            return
        }

        if (config.isAnalyticsOnly) {
            config.logger.debug(
                config.accountId,
                "Instance is set for Analytics only, cannot create notification"
            )
            return
        }

        try {
            // May be overridden below for Live Update Mode B so successive updates land in place.
            var liveActivityNotificationId = notificationId

            // Live Update: flatten the nested `data` (pt_id + client render keys) to the top level so
            // the factory routing below can read it. Idempotent (root-wins), so it is a no-op when the
            // SDK's FCM handler path already surfaced it — this is what lets a Mode A client-factory
            // Live Update work on the CleverTapAPI.createNotification entry point too.
            if ("true".equals(extras.getString(Constants.WZRK_LIVE_ACTIVITY, ""), ignoreCase = true)) {
                PushNotificationHandler.surfaceLiveActivityPayload(extras)
            }

            val isSilent = extras.getString(Constants.WZRK_PUSH_SILENT, "").equals("true", ignoreCase = true)
            if (isSilent) {
                analyticsManager.pushNotificationViewedEvent(extras)
                return
            }
            config.logger.debug(config.accountId, "Handling notification: $extras")

            val existingPushId = extras.getString(Constants.WZRK_PUSH_ID)
            if (existingPushId != null) {
                if (baseDatabaseManager.loadDBAdapter(context).doesPushNotificationIdExist(existingPushId)) {
                    config.logger.debug(config.accountId, "Push Notification already rendered, not showing again")
                    return
                }
            }

            // Live Activity (live update) pushes are rendered by a client-supplied factory.
            // Mirrors the iOS wzrk_la marker; the SDK owns the notification id + lifecycle events.
            val isLiveActivity = extras.getString(Constants.WZRK_LIVE_ACTIVITY, "")
                .equals("true", ignoreCase = true)

            if (isLiveActivity) {
                // Mode is decided by whether the payload carries a pt_id (surfaced from `data` at
                // the gate): pt_id present => Mode B (SDK/Push Template renders, and it takes
                // precedence over a client factory); pt_id absent => Mode A (client factory renders
                // the `data` custom content). Lifecycle events are raised in postNotificationRendered
                // for both modes.
                val isPtMode = PushNotificationHandler.isForPushTemplates(extras)
                val customFactory = CleverTapAPI.getNotificationFactory()

                if (LiveActivityRouter.mode(isPtMode, customFactory != null) == LiveActivityMode.FACTORY) {
                    // Mode A — client factory renders the Notification.
                    triggerLiveActivityNotification(context, extras, customFactory)
                    return
                }
                // Mode B — SDK renders with the SDK-owned in-place id (derived from wzrk_activityId)
                // so successive updates for the same activity replace the same notification.
                val inPlaceId = LiveActivityRouter.stableId(extras.getString(Constants.WZRK_LIVE_ACTIVITY_ID))
                if (inPlaceId != null) {
                    liveActivityNotificationId = inPlaceId
                }
                // fall through to normal rendering
            }

            val notifMessage = iNotificationRenderer.getMessage(extras) ?: ""
            // A Live Update rendered by a self-validating template renderer (pt_progress, whose
            // validator makes the message optional) may legitimately have an empty message, so it
            // must NOT be treated as a silent push. The core renderer can't validate that: it would
            // post a blank/app-name notification AND raise a false Started/Viewed impression — e.g. a
            // Mode A payload delivered to a build with no factory registered (SDK_RENDER fall-through),
            // or a Mode B pt_progress on the createNotification entry that never reached the template
            // renderer. Keep treating an empty message there as a silent push (pre-Live-Update behavior).
            val liveActivityTemplateRender =
                isLiveActivity && iNotificationRenderer !is CoreNotificationRenderer
            if (notifMessage.isEmpty() && !liveActivityTemplateRender) {
                //silent notification
                config.logger.verbose(config.accountId, "Push notification message is empty, not rendering")
                baseDatabaseManager.loadDBAdapter(context).storeUninstallTimestamp()
                val pingFreq = extras.getString("pf", "")
                if (!TextUtils.isEmpty(pingFreq)) {
                    updatePingFrequencyIfNeeded(context, pingFreq!!.toInt())
                }
                return
            }

            // Resolve the title for parity/side-effects; the renderer applies the app-name fallback
            // itself when it builds the notification in triggerNotification.
            iNotificationRenderer.getTitle(extras, context)
            triggerNotification(context, extras, liveActivityNotificationId)
        } catch (t: Throwable) {
            // Occurs if the notification image was null
            // Let's return, as we couldn't get a handle on the app's icon
            // Some devices throw a PackageManager* exception too
            config.logger.debug(config.accountId, "Couldn't render notification: ", t)
        }
    }

    /**
     * Saves token for a push type into shared pref
     */
    fun cacheToken(token: String?, pushType: PushType?) {
        if (TextUtils.isEmpty(token) || pushType == null) {
            return
        }
        try {
            val task = CTExecutorFactory.executors(config).ioTask<Unit>()
            task.execute("PushProviders#cacheToken") {
                if (!alreadyHaveToken(token, pushType)) {
                    val key = pushType.tokenPrefKey
                    if (!TextUtils.isEmpty(key)) {
                        StorageHelper.putStringImmediate(context, config.accountId, key, token)
                        config.log(PushConstants.LOG_TAG, "$pushType Cached New Token successfully $token")
                    }
                }
            }
        } catch (t: Throwable) {
            config.log(PushConstants.LOG_TAG, "$pushType Unable to cache token $token", t)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun doTokenRefresh(token: String?, pushType: PushType?) {
        if (TextUtils.isEmpty(token) || pushType == null) {
            return
        }
        handleToken(token, pushType, true)
    }

    /**
     * push the device token outside of the normal course
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun forcePushDeviceToken(register: Boolean) {
        for (pushType in allEnabledPushTypes) {
            pushDeviceTokenEvent(null, register, pushType)
        }
    }

    /**
     * @return list of all available push types, contains ( Clevertap's plugin + Custom supported Push Types)
     */
    fun getAvailablePushTypes(): ArrayList<PushType> {
        val pushTypes = ArrayList<PushType>()
        for (pushProvider in availableCTPushProviders) {
            pushTypes.add(pushProvider.pushType)
        }
        return pushTypes
    }

    /**
     * @param pushType - Pushtype [PushType]
     * @return Messaging token for a particular push type
     */
    fun getCachedToken(pushType: PushType?): String? {
        if (pushType != null) {
            val key = pushType.tokenPrefKey
            if (!TextUtils.isEmpty(key)) {
                val cachedToken = StorageHelper.getStringFromPrefs(context, config.accountId, key, null)
                config.log(PushConstants.LOG_TAG, "$pushType getting Cached Token - $cachedToken")
                return cachedToken
            }
        }
        if (pushType != null) {
            config.log(PushConstants.LOG_TAG, "$pushType Unable to find cached Token for type ")
        }
        return null
    }

    fun getDevicePushTokenRefreshListener(): DevicePushTokenRefreshListener? {
        return tokenRefreshListener
    }

    fun setDevicePushTokenRefreshListener(tokenRefreshListener: DevicePushTokenRefreshListener?) {
        this.tokenRefreshListener = tokenRefreshListener
    }

    /**
     * Direct Method to send tokens to Clevertap's server
     * Call this method when Clients are handling the Messaging services on their own
     */
    fun handleToken(token: String?, pushType: PushType?, register: Boolean) {
        if (register) {
            registerToken(token, pushType)
        } else {
            unregisterToken(token, pushType)
        }
    }

    /**
     * true if we are able to reach the device via any of the messaging service
     */
    val isNotificationSupported: Boolean
        get() {
            for (pushType in getAvailablePushTypes()) {
                if (getCachedToken(pushType) != null) {
                    return true
                }
            }
            return false
        }

    override fun onNewToken(freshToken: String?, pushType: PushType?) {
        if (!TextUtils.isEmpty(freshToken)) {
            doTokenRefresh(freshToken, pushType)
            deviceTokenDidRefresh(freshToken, pushType)
        }
    }

    //Push
    fun onTokenRefresh() {
        refreshAllTokens()
    }

    /**
     * Stores silent push notification in DB for smooth working of Pull Notifications
     * Background Job Service and also stores wzrk_pid to the DB to avoid duplication of Push
     * Notifications from Pull Notifications.
     */
    fun processCustomPushNotification(extras: Bundle) {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>()
        task.execute("customHandlePushAmplification") {
            val notifMessage = extras.getString(Constants.NOTIF_MSG) ?: ""
            if (notifMessage.isEmpty()) {
                //silent notification
                config.logger.verbose(config.accountId, "Push notification message is empty, not rendering")
                baseDatabaseManager.loadDBAdapter(context).storeUninstallTimestamp()
                val pingFreq = extras.getString("pf", "")
                if (!TextUtils.isEmpty(pingFreq)) {
                    updatePingFrequencyIfNeeded(context, pingFreq!!.toInt())
                }
            } else {
                val wzrkPid = extras.getString(Constants.WZRK_PUSH_ID)
                val ttl = extras.getString(Constants.WZRK_TIME_TO_LIVE)
                var wzrkTtl = clock.currentTimeSeconds() + Constants.DEFAULT_PUSH_TTL_SECONDS
                if (ttl != null) {
                    wzrkTtl = ttl.toLong()
                }
                if (wzrkPid != null) {
                    config.logger.verbose("Storing Push Notification...$wzrkPid - with ttl - $ttl")
                    baseDatabaseManager.loadDBAdapter(context).storePushNotificationId(wzrkPid, wzrkTtl)
                } else {
                    config.logger.verbose(
                        "Will not save Push Notification in DB due to invalid id, processCustomPushNotification"
                    )
                }
            }
        }
    }

    /**
     * Unregister the token for a push type from Clevertap's server.
     * Devices with unregistered token wont be reachable.
     */
    fun unregisterToken(token: String?, pushType: PushType?) {
        pushDeviceTokenEvent(token, false, pushType)
    }

    /**
     * updates the ping frequency if there is a change & reschedules existing ping tasks.
     */
    fun updatePingFrequencyIfNeeded(context: Context, frequency: Int) {
        config.logger.verbose("Ping frequency received - $frequency")
        config.logger.verbose("Stored Ping Frequency - " + getPingFrequency(context))
        if (frequency != getPingFrequency(context)) {
            setPingFrequency(context, frequency)
            if (config.isBackgroundSync && !config.isAnalyticsOnly) {
                val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>(TAG)
                task.execute("createOrResetWorker") {
                    createOrResetWorker(true)
                }
            }
        }
    }

    private fun alreadyHaveToken(newToken: String?, pushType: PushType?): Boolean {
        val alreadyAvailable = !TextUtils.isEmpty(newToken) && pushType != null &&
            newToken.equals(getCachedToken(pushType), ignoreCase = true)
        if (pushType != null) {
            config.log(PushConstants.LOG_TAG, "$pushType Token Already available value: $alreadyAvailable")
        }
        return alreadyAvailable
    }

    fun runPushAmpWork(context: Context) {
        Logger.v(config.accountId, "Pushamp - Running work request")
        if (!isNotificationSupported) {
            Logger.v(config.accountId, "Pushamp - Token is not present, not running the work request")
            return
        }

        val now = Calendar.getInstance()

        val hour = now.get(Calendar.HOUR_OF_DAY) // Get hour in 24 hour format
        val minute = now.get(Calendar.MINUTE)

        val inputParser = SimpleDateFormat(inputFormat, Locale.US)

        val currentTime = parseTimeToDate("$hour:$minute", inputParser)
        val startTime = parseTimeToDate(Constants.DND_START, inputParser)
        val endTime = parseTimeToDate(Constants.DND_STOP, inputParser)

        if (isTimeBetweenDNDTime(startTime, endTime, currentTime)) {
            Logger.v(config.accountId, "Pushamp won't run in default DND hours")
            return
        }

        val lastTS = baseDatabaseManager.loadDBAdapter(context).getLastUninstallTimestamp()

        if (lastTS == 0L || lastTS > clock.currentTimeMillis() - 24 * 60 * 60 * 1000) {
            try {
                val eventObject = JSONObject()
                eventObject.put("bk", 1)
                analyticsManager.sendPingEvent(eventObject)
                Logger.v(config.accountId, "Pushamp - Successfully completed work request")
            } catch (e: JSONException) {
                Logger.v("Pushamp - Unable to complete work request")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopJobScheduler(context: Context) {
        val existingJobId = StorageHelper.getInt(context, PF_JOB_ID, -1)
        if (existingJobId != -1) {
            // Cancel already running job. Possibly unnecessary
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(existingJobId)
            StorageHelper.remove(context, PF_JOB_ID)
        }
    }

    private fun createOrResetWorker(isPingFrequencyUpdated: Boolean) {
        //Disable push amp for devices below Api 26
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            config.logger.debug(config.accountId, "Pushamp feature is not supported below Oreo")
            return
        }

        val existingWorkName = StorageHelper.getString(context, PF_WORK_ID, "") ?: ""
        val pingFrequency = getPingFrequency(context)

        // no running work and nothing to create
        if (existingWorkName == "" && pingFrequency <= 0) {
            config.logger.debug(config.accountId, "Pushamp - There is no running work and nothing to create")
            return
        }

        // Running work exists but hard cancel
        if (pingFrequency <= 0) {
            config.logger.debug(config.accountId, "Pushamp - Cancelling worker as pingFrequency <=0 ")
            stopWorker()
            return
        }

        try {
            val workManager = WorkManager.getInstance(context)

            // Create a work request only when it doesn't exist already or the ping frequency is updated
            if (existingWorkName == "" || isPingFrequencyUpdated) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequest.Builder(
                    CTPushAmpWorker::class.java, pingFrequency.toLong(),
                    TimeUnit.MINUTES, DEFAULT_FLEX_INTERVAL.toLong(), TimeUnit.MINUTES
                )
                    .setConstraints(constraints)
                    .build()

                val workName = if (existingWorkName == "") config.accountId else existingWorkName

                workManager.enqueueUniquePeriodicWork(workName, ExistingPeriodicWorkPolicy.UPDATE, request)
                StorageHelper.putString(context, PF_WORK_ID, workName)
                config.logger.debug(
                    config.accountId,
                    "Pushamp - Finished scheduling periodic work request - $workName with repeatInterval- " +
                        "$pingFrequency minutes"
                )
            }
        } catch (e: Exception) {
            config.logger.debug(
                config.accountId,
                "Pushamp - Failed scheduling/cancelling periodic work request$e"
            )
        }
    }

    private fun stopWorker() {
        val existingWorkName = StorageHelper.getString(context, PF_WORK_ID, "") ?: ""
        if (existingWorkName != "") {
            try {
                val workManager = WorkManager.getInstance(context)
                workManager.cancelUniqueWork(existingWorkName)
                StorageHelper.putString(context, PF_WORK_ID, "")
                config.logger.debug(config.accountId, "Pushamp - Successfully cancelled work")
            } catch (e: Exception) {
                config.logger.debug(config.accountId, "Pushamp - Failure while cancelling work")
            }
        }
    }

    /**
     * Creates the list of push providers.
     */
    private fun createProviders(): List<CTPushProvider> {
        val providers: MutableList<CTPushProvider> = ArrayList()

        for (pushType in allEnabledPushTypes) {
            val pushProvider = getCTPushProviderFromPushType(pushType) ?: continue
            providers.add(pushProvider)
        }

        return providers
    }

    /**
     * This code can be moved to [PushType] but this is creating new instance of CTPushProvider for each
     * execution, and to prevent multiple instance of same CTPushProvider not moving this to [PushType]
     */
    private fun getCTPushProviderFromPushType(pushType: PushType): CTPushProvider? {
        val className = pushType.ctProviderClassName
        var pushProvider: CTPushProvider? = null
        try {
            val providerClass = Class.forName(className)
            val constructor = providerClass.getConstructor(
                CTPushProviderListener::class.java, Context::class.java, CleverTapInstanceConfig::class.java
            )
            pushProvider = constructor.newInstance(this, context, config) as CTPushProvider

            config.log(PushConstants.LOG_TAG, "Found provider:$className")
        } catch (e: InstantiationException) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider InstantiationException$className")
        } catch (e: IllegalAccessException) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider IllegalAccessException$className")
        } catch (e: ClassNotFoundException) {
            config.log(PushConstants.LOG_TAG, "Unable to create provider ClassNotFoundException$className")
        } catch (e: Exception) {
            config.log(
                PushConstants.LOG_TAG,
                "Unable to create provider $className Exception:" + e.javaClass.name
            )
        }
        return pushProvider
    }

    //Push
    private fun deviceTokenDidRefresh(token: String?, type: PushType?) {
        tokenRefreshListener?.let {
            config.logger.debug(config.accountId, "Notifying devicePushTokenDidRefresh: $token")
            it.devicePushTokenDidRefresh(token, type)
        }
    }

    private fun findAvailableCTPushProviders() {
        val providers = createProviders()

        if (providers.isEmpty()) {
            config.log(
                PushConstants.LOG_TAG,
                "No push providers found!. Make sure to install at least one push provider"
            )
            return
        }

        for (provider in providers) {
            if (!isValid(provider)) {
                config.log(PushConstants.LOG_TAG, "Invalid Provider: " + provider.javaClass)
                continue
            }

            if (!provider.isSupported) {
                config.log(PushConstants.LOG_TAG, "Unsupported Provider: " + provider.javaClass)
                continue
            }

            if (provider.isAvailable) {
                config.log(PushConstants.LOG_TAG, "Available Provider: " + provider.javaClass)
                availableCTPushProviders.add(provider)
            } else {
                config.log(PushConstants.LOG_TAG, "Unavailable Provider: " + provider.javaClass)
            }
        }
    }

    private fun findNonEnabledPushTypes() {
        nonEnabledPushTypes.addAll(allEnabledPushTypes)
        for (pushProvider in availableCTPushProviders) {
            nonEnabledPushTypes.remove(pushProvider.pushType)
        }
    }

    //Session
    private fun configurePushTypes() {
        val allowedPushTypes = config.pushTypes ?: return
        for (pushType in allowedPushTypes) {
            val className = pushType.messagingSDKClassName
            try {
                Class.forName(className)
                allEnabledPushTypes.add(pushType)
                config.log(PushConstants.LOG_TAG, "SDK Class Available :$className")
            } catch (e: Exception) {
                config.log(
                    PushConstants.LOG_TAG,
                    "SDK class Not available $className Exception:" + e.javaClass.name
                )
            }
        }
    }

    /**
     * Adds new push provider similar to FCM
     */
    fun addPushService(pushType: PushType) {
        allEnabledPushTypes.add(pushType)
    }

    private fun getPingFrequency(context: Context): Int {
        return StorageHelper.getInt(context, PING_FREQUENCY, PING_FREQUENCY_VALUE)
    }

    /**
     * Loads all the plugins that are currently supported by the device.
     */
    private fun init() {
        configurePushTypes()
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>(TAG)
        task.execute("asyncFindAvailableCTPushProviders") {
            findAvailableCTPushProviders()
            findNonEnabledPushTypes()
        }
    }

    private fun initPushAmp() {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>(TAG)
        task.execute("createOrResetWorker") {
            stopJobScheduler(context)
            if (config.isBackgroundSync && !config.isAnalyticsOnly) {
                createOrResetWorker(false)
            } else {
                config.logger.debug(
                    config.accountId,
                    "Pushamp - Cancelling worker as background sync is disabled or config is analytics only"
                )
                stopWorker()
            }
        }
    }

    @androidx.annotation.VisibleForTesting
    internal fun isTimeBetweenDNDTime(startTime: Date, stopTime: Date, currentTime: Date): Boolean {
        //Start Time
        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.time = startTime
        //Current Time
        val currentTimeCalendar = Calendar.getInstance()
        currentTimeCalendar.time = currentTime
        //Stop Time
        val stopTimeCalendar = Calendar.getInstance()
        stopTimeCalendar.time = stopTime

        if (stopTime.compareTo(startTime) < 0) {
            if (currentTimeCalendar.compareTo(stopTimeCalendar) < 0) {
                currentTimeCalendar.add(Calendar.DATE, 1)
            }
            stopTimeCalendar.add(Calendar.DATE, 1)
        }
        return currentTimeCalendar.compareTo(startTimeCalendar) >= 0 &&
            currentTimeCalendar.compareTo(stopTimeCalendar) < 0
    }

    private fun isValid(provider: CTPushProvider): Boolean {
        if (BuildConfig.VERSION_CODE < provider.minSDKSupportVersionCode()) {
            config.log(
                PushConstants.LOG_TAG,
                "Provider: %s version %s does not match the SDK version %s. Make sure all CleverTap " +
                    "dependencies are the same version."
            )
            return false
        }
        return true
    }

    private fun parseTimeToDate(time: String, inputParser: SimpleDateFormat): Date {
        return try {
            inputParser.parse(time) ?: Date(0)
        } catch (e: ParseException) {
            Date(0)
        }
    }

    private fun pushDeviceTokenEvent(token: String?, register: Boolean, pushType: PushType?) {
        if (pushType == null) {
            return
        }
        val resolvedToken = if (!TextUtils.isEmpty(token)) token else getCachedToken(pushType)
        if (TextUtils.isEmpty(resolvedToken)) {
            return
        }
        synchronized(tokenLock) {
            val event = JSONObject()
            val data = JSONObject()
            val action = if (register) "register" else "unregister"
            try {
                data.put("action", action)
                data.put("id", resolvedToken)
                data.put("type", pushType.type)
                event.put("data", data)
                config.logger.verbose(config.accountId, "$pushType$action device token $resolvedToken")
                analyticsManager.sendDataEvent(event)
            } catch (t: Throwable) {
                // we won't get here
                config.logger.verbose(config.accountId, "$pushType$action device token failed", t)
            }
        }
    }

    /**
     * Fetches latest tokens from various providers and send to Clevertap's server
     */
    private fun refreshAllTokens() {
        val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Unit>(TAG)
        task.execute("PushProviders#refreshAllTokens") {
            // refresh tokens of Push Providers
            refreshAvailableCTProviderTokens()
            // refresh tokens of custom Providers
            refreshNonEnabledProviderTokens()
        }
    }

    private fun refreshAvailableCTProviderTokens() {
        for (pushProvider in availableCTPushProviders) {
            try {
                pushProvider.requestToken()
            } catch (t: Throwable) {
                //no-op
                config.log(PushConstants.LOG_TAG, "Token Refresh error $pushProvider", t)
            }
        }
    }

    private fun refreshNonEnabledProviderTokens() {
        for (pushType in nonEnabledPushTypes) {
            try {
                pushDeviceTokenEvent(getCachedToken(pushType), true, pushType)
            } catch (t: Throwable) {
                config.log(PushConstants.LOG_TAG, "Token Refresh error $pushType", t)
            }
        }
    }

    private fun registerToken(token: String?, pushType: PushType?) {
        pushDeviceTokenEvent(token, true, pushType)
        cacheToken(token, pushType)
    }

    private fun setPingFrequency(context: Context, pingFrequency: Int) {
        StorageHelper.putInt(context, PING_FREQUENCY, pingFrequency)
    }

    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY)
    var pushNotificationRenderer: INotificationRenderer
        get() = iNotificationRenderer
        set(value) {
            iNotificationRenderer = value
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun getPushRenderingLock(): Any {
        return pushRenderingLock
    }

    private fun triggerNotification(context: Context, extras: Bundle, notificationId: Int) {
        var notifId = notificationId
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager == null) {
            val notificationManagerError = "Unable to render notification, Notification Manager is null."
            config.logger.debug(config.accountId, notificationManagerError)
            return
        }

        val channelId = extras.getString(Constants.WZRK_CHANNEL_ID, "")
        var updatedChannelId: String? = null
        val requiresChannelId = VERSION.SDK_INT >= VERSION_CODES.O

        if (requiresChannelId) {
            var error: ValidationError? = null
            var value = ""

            if (channelId.isEmpty()) {
                error = ValidationError.CHANNEL_ID_MISSING_IN_PAYLOAD
                value = extras.toString()
            } else if (notificationManager.getNotificationChannel(channelId) == null) {
                error = ValidationError.CHANNEL_ID_NOT_REGISTERED
                value = channelId
            }
            if (error != null) {
                val channelIdError = ValidationResultFactory.create(error, value)
                config.logger.debug(config.accountId, channelIdError.errorDesc)
                validationResultStack.pushValidationResult(channelIdError)
            }

            val silenceInForeground =
                "true".equals(extras.getString(Constants.WZRK_SILENCE_IN_FOREGROUND), ignoreCase = true)
            val hideHeadsUp = CoreMetaData.isAppForeground() && silenceInForeground

            // get channel using channel id from push payload. If channel id is null or empty then create default
            updatedChannelId = notificationManager.getOrCreateChannel(channelId, context, hideHeadsUp)

            // if no channel gets created then do not render push
            if (updatedChannelId == null || updatedChannelId.trim().isEmpty()) {
                config.logger.debug(config.accountId, "Not rendering Push since channel id is null or blank.")
                return
            }

            // if channel is blocked by user then do not render push
            if (!context.isNotificationChannelEnabled(updatedChannelId)) {
                config.logger.verbose(
                    config.accountId,
                    "Not rendering push notification as channel = $updatedChannelId is blocked by user"
                )
                return
            }

            config.logger.debug(config.accountId, "Rendering Push on channel = $updatedChannelId")
        }

        var smallIcon: Int
        try {
            val x = ManifestInfo.getInstance(context).notificationIcon ?: throw IllegalArgumentException()
            smallIcon = context.resources.getIdentifier(x, "drawable", context.packageName)
            if (smallIcon == 0) {
                throw IllegalArgumentException()
            }
        } catch (t: Throwable) {
            smallIcon = DeviceInfo.getAppIconAsIntId(context)
        }

        iNotificationRenderer.setSmallIcon(smallIcon, context)

        var priorityInt = NotificationCompat.PRIORITY_DEFAULT
        val priority = extras.getString(Constants.NOTIF_PRIORITY)
        if (priority != null) {
            if (priority == Constants.PRIORITY_HIGH) {
                priorityInt = NotificationCompat.PRIORITY_HIGH
            }
            if (priority == Constants.PRIORITY_MAX) {
                priorityInt = NotificationCompat.PRIORITY_MAX
            }
        }

        // if we have no user set notificationID then try collapse key
        if (notifId == Constants.EMPTY_NOTIFICATION_ID) {
            try {
                val collapseKey = iNotificationRenderer.getCollapseKey(extras)
                if (collapseKey != null) {
                    if (collapseKey is Number) {
                        notifId = collapseKey.toInt()
                    } else if (collapseKey is String) {
                        try {
                            notifId = collapseKey.toString().toInt()
                            config.logger.verbose(
                                config.accountId,
                                "Converting collapse_key: $collapseKey to notificationId int: $notifId"
                            )
                        } catch (e: NumberFormatException) {
                            notifId = collapseKey.toString().hashCode()
                            config.logger.verbose(
                                config.accountId,
                                "Converting collapse_key: $collapseKey to notificationId int: $notifId"
                            )
                        }
                    }
                    notifId = Math.abs(notifId) //Notification Id always needs to be positive
                    config.logger.debug(
                        config.accountId,
                        "Creating the notification id: $notifId from collapse_key: $collapseKey"
                    )
                }
            } catch (e: NumberFormatException) {
                // no-op
            }
        } else {
            config.logger.debug(
                config.accountId,
                "Have user provided notificationId: $notifId won't use collapse_key (if any) as basis for notificationId"
            )
        }

        // if after trying collapse_key notification is still empty set to random int
        if (notifId == Constants.EMPTY_NOTIFICATION_ID) {
            notifId = (Math.random() * 100).toInt()
            config.logger.debug(config.accountId, "Setting random notificationId: $notifId")
        }

        var nb: NotificationCompat.Builder
        if (requiresChannelId) {
            nb = NotificationCompat.Builder(context, updatedChannelId!!)

            // choices here are Notification.BADGE_ICON_NONE = 0, Notification.BADGE_ICON_SMALL = 1,
            // Notification.BADGE_ICON_LARGE = 2.  Default is  Notification.BADGE_ICON_LARGE
            val badgeIconParam = extras.getString(Constants.WZRK_BADGE_ICON, null)
            if (badgeIconParam != null) {
                try {
                    val badgeIconType = badgeIconParam.toInt()
                    if (badgeIconType >= 0) {
                        nb.setBadgeIconType(badgeIconType)
                    }
                } catch (t: Throwable) {
                    // no-op
                }
            }

            val badgeCountParam = extras.getString(Constants.WZRK_BADGE_COUNT, null) //cbi
            if (badgeCountParam != null) {
                try {
                    val badgeCount = badgeCountParam.toInt()
                    if (badgeCount >= 0) {
                        nb.setNumber(badgeCount)
                    }
                } catch (t: Throwable) {
                    // no-op
                }
            }
        } else {
            @Suppress("DEPRECATION")
            nb = NotificationCompat.Builder(context)
        }

        nb.setPriority(priorityInt)

        //remove sound for fallback notif
        if (iNotificationRenderer is AudibleNotification) {
            nb = (iNotificationRenderer as AudibleNotification).setSound(context, extras, nb, config)
        }

        // template renderer can return null if template type is null
        val builtNb = iNotificationRenderer.renderNotification(extras, context, nb, config, notifId) ?: return

        val n = builtNb.build()

        // Live Update Mode B (SDK-rendered): attach the dismissal delete intent so swipe-away
        // raises the "Dismissed" event — respecting any delete intent a Push Template already set.
        if (extras.getString(Constants.WZRK_LIVE_ACTIVITY, "").equals("true", ignoreCase = true)) {
            applyLiveActivityDismissIntent(context, n, extras, notifId)
        }

        notificationManager.notify(notifId, n)
        config.logger.debug(config.accountId, "Rendered notification: $n") //cb

        postNotificationRendered(context, extras)
    }

    /**
     * Renders a Live Activity (live update) push via the client-supplied factory.
     *
     * Unlike a regular custom-factory render, the SDK owns lifecycle here to stay on par with
     * the iOS Live Activity contract.
     */
    private fun triggerLiveActivityNotification(
        context: Context, extras: Bundle,
        customFactory: ICleverTapNotificationFactory?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (notificationManager == null) {
            config.logger.debug(config.accountId, "Unable to render notification, Notification Manager is null.")
            return
        }

        var notification: Notification
        try {
            notification = customFactory!!.onCreateNotification(context, extras) ?: run {
                config.logger.debug(
                    config.accountId,
                    "ICleverTapNotificationFactory returned null, not rendering notification"
                )
                return
            }
        } catch (t: Throwable) {
            config.logger.debug(config.accountId, "ICleverTapNotificationFactory threw an exception", t)
            return
        }

        // SDK owns the id so updates land on the same notification (in-place). It is derived from
        // the stable wzrk_activityId; if a malformed push omits it, fall back to the per-send
        // wzrk_pid so the notification still shows (it just won't update in place).
        val activityNotifId = LiveActivityRouter.stableId(extras.getString(Constants.WZRK_LIVE_ACTIVITY_ID))
        val notificationId: Int
        if (activityNotifId != null) {
            notificationId = activityNotifId
        } else {
            val pidNotifId = LiveActivityRouter.stableId(extras.getString(Constants.WZRK_PUSH_ID))
            notificationId = pidNotifId ?: clock.currentTimeSeconds().toInt()
            config.logger.debug(
                config.accountId,
                "Live Update push missing wzrk_activityId; in-place updates will not work."
            )
        }

        val event = extras.getString(
            Constants.WZRK_LIVE_ACTIVITY_EVENT,
            Constants.WZRK_LIVE_ACTIVITY_EVENT_UPDATE
        )

        // Guarantee the notification lands in a real channel (same fallback as core push). This may
        // rebuild the notification, so resolve the channel BEFORE attaching the dismiss intent.
        notification = ensureLiveActivityChannel(context, notification, notificationManager)

        // Respect a user-blocked channel: don't post (or record lifecycle/Viewed) for a notification
        // nobody will see — same gate triggerNotification applies to core pushes.
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val channelId = notification.channelId
            if (channelId != null && !context.isNotificationChannelEnabled(channelId)) {
                config.logger.verbose(
                    config.accountId,
                    "Not rendering Live Update as channel = $channelId is blocked by user"
                )
                return
            }
        }

        // Attach a delete intent for dismissal tracking, without clobbering one the client set.
        applyLiveActivityDismissIntent(context, notification, extras, notificationId)

        notificationManager.notify(notificationId, notification)
        config.logger.debug(
            config.accountId,
            "Rendered Live Activity notification (id=$notificationId, event=$event)"
        )

        // Lifecycle events (Started/Updated/Ended) are raised centrally in postNotificationRendered
        // so both the factory (Mode A) and SDK-rendered (Mode B) paths report them exactly once.
        postNotificationRendered(context, extras)
    }

    /**
     * Guards against Android O+ silently dropping a Live Activity (Mode A / factory) notification
     * posted to a channel that does not exist — reusing the same channel resolution + fallback
     * as ordinary CleverTap push notifications.
     */
    private fun ensureLiveActivityChannel(
        context: Context, notification: Notification,
        notificationManager: NotificationManager
    ): Notification {
        if (VERSION.SDK_INT < VERSION_CODES.O) {
            return notification // channels are not required before Android O
        }
        var result = notification
        val desired = result.channelId
        // Same resolution + fallback as core push (payload -> manifest -> shared fcm fallback).
        val resolved = notificationManager.getOrCreateChannel(desired, context, false)
        if (resolved == null) {
            config.logger.debug(
                config.accountId,
                "Live Update: could not resolve a notification channel; posting as-is."
            )
            return result
        }
        if (resolved != desired) {
            // Notification was built against a missing/empty channel; move it to the shared fallback.
            try {
                result = Notification.Builder.recoverBuilder(context, result)
                    .setChannelId(resolved)
                    .build()
                config.logger.debug(
                    config.accountId,
                    "Live Update: retargeted notification to fallback channel '$resolved'."
                )
            } catch (t: Throwable) {
                config.logger.debug(
                    config.accountId,
                    "Live Update: failed to retarget notification to channel '$resolved'.", t
                )
            }
        }
        return result
    }

    /**
     * Attaches the CleverTap dismissal delete intent to a Live Update notification so a swipe-away
     * raises the "Dismissed" event — but only if the notification does not already carry a delete
     * intent set by the client (Mode A factory) or a Push Template.
     */
    private fun applyLiveActivityDismissIntent(
        context: Context, notification: Notification,
        extras: Bundle, notificationId: Int
    ) {
        if (notification.deleteIntent == null) {
            notification.deleteIntent = liveActivityDismissIntent(context, extras, notificationId)
        } else {
            config.logger.debug(
                config.accountId,
                "Live Update: notification already has a delete intent set by the client/template; " +
                    "CleverTap will not override it and will not track the Dismissed event for this push."
            )
        }
    }

    private fun liveActivityDismissIntent(context: Context, extras: Bundle, notificationId: Int): PendingIntent {
        val dismissIntent = Intent(context, CTLiveActivityDismissReceiver::class.java)
        dismissIntent.putExtras(extras)
        dismissIntent.putExtra(Constants.KEY_CT_TYPE, CTLiveActivityDismissReceiver.TYPE_DISMISS)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, notificationId, dismissIntent, flags)
    }

    private fun postNotificationRendered(context: Context, extras: Bundle) {
        // Raise the "Live Activity" lifecycle event for any live-update render (factory Mode A or
        // SDK-rendered Mode B) — exactly once, and independent of the wzrk_rnv (viewed) gate below.
        if (extras.getString(Constants.WZRK_LIVE_ACTIVITY, "").equals("true", ignoreCase = true)) {
            // State is driven entirely by the BE-sent wzrk_la_event (start/update/end) — no local
            // first-render tracking. See LiveActivityLifecycle.
            val state = LiveActivityLifecycle.state(extras.getString(Constants.WZRK_LIVE_ACTIVITY_EVENT))
            analyticsManager.raiseLiveActivityLifecycleEvent(extras, state)
        }

        val ttl = extras.getString(Constants.WZRK_TIME_TO_LIVE)
        var wzrkTtl = clock.currentTimeSeconds() + Constants.DEFAULT_PUSH_TTL_SECONDS
        if (ttl != null) {
            wzrkTtl = ttl.toLong()
        }
        val wzrkPid = extras.getString(Constants.WZRK_PUSH_ID)
        val dbAdapter = baseDatabaseManager.loadDBAdapter(context)
        if (wzrkPid != null) {
            config.logger.verbose("Storing Push Notification...$wzrkPid - with ttl - $ttl")
            dbAdapter.storePushNotificationId(wzrkPid, wzrkTtl)
        } else {
            config.logger.verbose("Will not save Push Notification in DB due to invalid id")
        }

        val notificationViewedEnabled = "true" == extras.getString(Constants.WZRK_RNV, "")
        if (!notificationViewedEnabled) {
            val notificationViewedError = ValidationResultFactory
                .create(ValidationError.NOTIFICATION_VIEWED_DISABLED, extras.toString())
            config.logger.debug(notificationViewedError.errorDesc)
            validationResultStack.pushValidationResult(notificationViewedError)
            return
        }

        val omrStart = extras.getLong(Constants.OMR_INVOKE_TIME_IN_MILLIS, -1)
        if (omrStart >= 0) {
            val prt = clock.currentTimeMillis() - omrStart
            config.logger.verbose("Rendered Push Notification in $prt millis")
        }

        ctWorkManager.init()
        analyticsManager.pushNotificationViewedEvent(extras)
    }

    companion object {

        private const val TAG = "PushProviders"

        private const val DEFAULT_FLEX_INTERVAL = 5
        private const val PING_FREQUENCY_VALUE = 240
        private const val PF_JOB_ID = "pfjobid"
        private const val PF_WORK_ID = "pfworkid"
        private const val PING_FREQUENCY = "pf"
        private const val inputFormat = "HH:mm"

        /**
         * Factory method to load push providers.
         *
         * @return A PushProviders class with the loaded providers.
         */
        @JvmStatic
        internal fun load(
            context: Context,
            config: CleverTapInstanceConfig,
            baseDatabaseManager: BaseDatabaseManager,
            validationResultStack: ValidationResultStack,
            analyticsManager: AnalyticsManager,
            controllerManager: ControllerManager,
            ctWorkManager: CTWorkManager,
            clock: Clock
        ): PushProviders {
            val providers = PushProviders(
                context, config, baseDatabaseManager, validationResultStack,
                analyticsManager, ctWorkManager, clock
            )
            providers.init()
            controllerManager.pushProviders = providers
            return providers
        }
    }
}
