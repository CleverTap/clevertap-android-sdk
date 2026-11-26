package com.clevertap.android.geofence

import android.Manifest
import android.app.PendingIntent.FLAG_NO_CREATE
import android.content.Context
import android.location.Location
import androidx.annotation.RestrictTo
import com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LATITUDE
import com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LONGITUDE
import com.clevertap.android.geofence.GoogleLocationAdapter.Companion.INTERVAL_IN_MILLIS
import com.clevertap.android.geofence.GoogleLocationAdapter.Companion.SMALLEST_DISPLACEMENT_IN_METERS
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import com.clevertap.android.geofence.interfaces.CTLocationCallback
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.GeofenceCallback
import org.json.JSONObject
import java.util.concurrent.Future

/**
 * Main Entry point for communicating with Geofence SDK.
 * Singleton class Responsible for initializing, activating and deactivating geofence sdk as
 * requested by applications. It implements [GeofenceCallback] to communicate with CleverTap SDK
 * for location pings and update of new geofence list.
 */
public open class CTGeofenceAPI private constructor(context: Context) : GeofenceCallback {

    public fun interface OnGeofenceApiInitializedListener {

        @Suppress("FunctionName")
        public fun OnGeofenceApiInitialized()
    }

    private var mAccountId: String? = null

    internal var cleverTapApi: CleverTapAPI? = null
        private set

    private val context: Context = context.applicationContext

    internal var ctGeofenceAdapter: CTGeofenceAdapter? = null
        private set

    private var mCtGeofenceEventsListener: CTGeofenceEventsListener? = null

    private var ctGeofenceSettings: CTGeofenceSettings? = null

    internal var ctLocationAdapter: CTLocationAdapter? = null
        private set

    private var mCtLocationUpdatesListener: CTLocationUpdatesListener? = null

    internal var isActivated = false
        private set

    private var onGeofenceApiInitializedListener: OnGeofenceApiInitializedListener? = null

    init {
        try {
            ctLocationAdapter = CTLocationFactory.createLocationAdapter(this.context)
            ctGeofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(this.context)
        } catch (e: IllegalStateException) {
            if (e.message != null) {
                getLogger().debug(
                    GEOFENCE_LOG_TAG,
                    e.message
                )
            }
        }
    }

    /**
     * Unregisters geofences, background location updates and clears file storage and preferences
     * This operation will be sent to queue and get's executed by background thread using
     * [CTGeofenceTaskManager]
     */
    @Suppress("unused")
    public open fun deactivate() {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return
        }

        CTGeofenceTaskManager.getInstance().postAsyncSafely("DeactivateApi", Runnable {
            // stop geofence monitoring
            val geofencePendingIntent = PendingIntentFactory.getPendingIntent(
                context,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_NO_CREATE
            )
            ctGeofenceAdapter!!.stopGeofenceMonitoring(geofencePendingIntent)

            // stop location updates
            val locationPendingIntent = PendingIntentFactory.getPendingIntent(
                context,
                PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_NO_CREATE
            )
            ctLocationAdapter!!.removeLocationUpdates(locationPendingIntent)

            // delete cached files
            FileUtils.deleteDirectory(context, FileUtils.getCachedDirName(context))

            // reset preference
            GeofenceStorageHelper.putDouble(
                context, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE
            )
            GeofenceStorageHelper.putDouble(
                context, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE
            )
            GeofenceStorageHelper.putLong(
                context, CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0
            )

            isActivated = false
        })
    }

    public open fun getCtGeofenceEventsListener(): CTGeofenceEventsListener? {
        return mCtGeofenceEventsListener
    }

    public open fun setCtGeofenceEventsListener(ctGeofenceEventsListener: CTGeofenceEventsListener?) {
        this.mCtGeofenceEventsListener = ctGeofenceEventsListener
    }

    /** Kotlin property syntax, equivalent to [getCtGeofenceEventsListener] / [setCtGeofenceEventsListener]. */
    public var ctGeofenceEventsListener: CTGeofenceEventsListener?
        @JvmSynthetic @JvmName("ktGetCtGeofenceEventsListener") inline get() = getCtGeofenceEventsListener()
        @JvmSynthetic @JvmName("ktSetCtGeofenceEventsListener") inline set(value) = setCtGeofenceEventsListener(value)

    public open fun getCtLocationUpdatesListener(): CTLocationUpdatesListener? {
        return mCtLocationUpdatesListener
    }

    public open fun setCtLocationUpdatesListener(ctLocationUpdatesListener: CTLocationUpdatesListener?) {
        this.mCtLocationUpdatesListener = ctLocationUpdatesListener
    }

    /** Kotlin property syntax, equivalent to [getCtLocationUpdatesListener] / [setCtLocationUpdatesListener]. */
    public var ctLocationUpdatesListener: CTLocationUpdatesListener?
        @JvmSynthetic @JvmName("ktGetCtLocationUpdatesListener") inline get() = getCtLocationUpdatesListener()
        @JvmSynthetic @JvmName("ktSetCtLocationUpdatesListener") inline set(value) = setCtLocationUpdatesListener(value)

    public open fun getGeofenceSettings(): CTGeofenceSettings? {
        return ctGeofenceSettings
    }

    /** Kotlin property syntax, equivalent to [getGeofenceSettings]. */
    public val geofenceSettings: CTGeofenceSettings?
        @JvmSynthetic @JvmName("ktGetGeofenceSettings") inline get() = getGeofenceSettings()

    /**
     * Sets geofence API configuration settings with provided [CTGeofenceSettings]
     *
     * @param ctGeofenceSettings instance of [CTGeofenceSettings]
     */
    private fun setGeofenceSettings(ctGeofenceSettings: CTGeofenceSettings?) {
        if (this.ctGeofenceSettings != null) {
            logger.verbose(
                GEOFENCE_LOG_TAG,
                "Settings already configured"
            )
            return
        }

        this.ctGeofenceSettings = ctGeofenceSettings
    }

    /**
     * Creates [GeofenceUpdateTask](to register list of geofences) and sends it to Queue using
     * [CTGeofenceTaskManager] if application has [Manifest.permission.ACCESS_FINE_LOCATION]
     * and [Manifest.permission.ACCESS_BACKGROUND_LOCATION] permissions.
     * This method is for internal usage only, apps must not call this externally.
     *
     * @param fenceList instance of [JSONObject] containing geofence list to register to OS
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun handleGeoFences(fenceList: JSONObject?) {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return
        }

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "We don't have ACCESS_FINE_LOCATION permission! dropping geofence update call"
            )
            if (this.cleverTapApi != null) {
                this.cleverTapApi!!.pushGeoFenceError(
                    CTGeofenceConstants.ERROR_CODE,
                    "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call"
                )
            }
            return
        }

        if (!Utils.hasBackgroundLocationPermission(context)) {
            getLogger().debug(
                GEOFENCE_LOG_TAG,
                "We don't have ACCESS_BACKGROUND_LOCATION permission! dropping geofence update call"
            )
            if (this.cleverTapApi != null) {
                this.cleverTapApi!!.pushGeoFenceError(
                    CTGeofenceConstants.ERROR_CODE,
                    "We don't have ACCESS_BACKGROUND_LOCATION permission! dropping geofence update call"
                )
            }
            return
        }

        if (fenceList == null) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "Geofence response is null! dropping further processing"
            )
            return
        }

        val geofenceUpdateTask = GeofenceUpdateTask(context, fenceList)

        CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "ProcessGeofenceUpdates",
            geofenceUpdateTask
        )
    }

    /**
     * Initializes and activates SDK with provided [CTGeofenceSettings] and [CleverTapAPI]
     * instance.
     *
     * @param ctGeofenceSettings instance of [CTGeofenceSettings].Can be null in which case
     * default settings will be applied.
     * @param cleverTapAPI       NonNull instance of  [CleverTapAPI].
     */
    public open fun init(ctGeofenceSettings: CTGeofenceSettings?, cleverTapAPI: CleverTapAPI?) {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return
        }

        setCleverTapApi(cleverTapAPI)
        setGeofenceSettings(ctGeofenceSettings)
        setAccountId(cleverTapAPI!!.accountId)
        activate()
    }

    /**
     * Creates [LocationUpdateTask](to register location updates) and sends it to Queue using
     * [CTGeofenceTaskManager]
     * if application has [Manifest.permission.ACCESS_FINE_LOCATION] permission. SDK initialization
     * callback([OnGeofenceApiInitializedListener.OnGeofenceApiInitialized]) will be raised as soon as
     * [LocationUpdateTask] finishes it's execution.
     *
     * @throws IllegalStateException if Geofence SDK is not initialized before calling this method
     */
    public open fun initBackgroundLocationUpdates() {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return
        }

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call"
            )
            if (this.cleverTapApi != null) {
                this.cleverTapApi!!.pushGeoFenceError(
                    CTGeofenceConstants.ERROR_CODE,
                    "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call"
                )
            }
            return
        }

        logger.debug(
            GEOFENCE_LOG_TAG,
            "requestBackgroundLocationUpdates() called"
        )

        if (!isActivated) {
            throw IllegalStateException(
                "Geofence SDK must be initialized before initBackgroundLocationUpdates()"
            )
        }

        val locationUpdateTask = LocationUpdateTask(context)
        locationUpdateTask.setOnCompleteListener(CTGeofenceTask.OnCompleteListener {
            if (onGeofenceApiInitializedListener != null) {
                com.clevertap.android.sdk.Utils.runOnUiThread {
                    onGeofenceApiInitializedListener!!.OnGeofenceApiInitialized()
                }
            }
        })

        CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "IntitializeLocationUpdates",
            locationUpdateTask
        )
    }

    /**
     * Sets [OnGeofenceApiInitializedListener] for Geofence SDK initialized callback on main thread
     *
     * @param onGeofenceApiInitializedListener instance of [OnGeofenceApiInitializedListener]
     */
    @Suppress("unused")
    public open fun setOnGeofenceApiInitializedListener(
        onGeofenceApiInitializedListener: OnGeofenceApiInitializedListener?
    ) {
        this.onGeofenceApiInitializedListener = onGeofenceApiInitializedListener
    }

    /**
     * Fetches last known location from OS and delivers it to APP through [CTLocationUpdatesListener]
     * if application has [Manifest.permission.ACCESS_FINE_LOCATION] permission.
     * Fetched Location will be passed to [processTriggeredLocation] to send it to
     * server for latest geofence list
     *
     * @throws IllegalStateException if Geofence SDK is not initialized before calling this method
     */
    @Suppress("unused")
    override fun triggerLocation() {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null || cleverTapApi == null) {
            return
        }

        logger.debug(
            GEOFENCE_LOG_TAG,
            "triggerLocation() called"
        )

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "We don't have ACCESS_FINE_LOCATION permission! Dropping triggerLocation() call"
            )
            if (this.cleverTapApi != null) {
                this.cleverTapApi!!.pushGeoFenceError(
                    CTGeofenceConstants.ERROR_CODE,
                    "We don't have ACCESS_FINE_LOCATION permission! Dropping triggerLocation() call"
                )
            }
            return
        }

        if (!isActivated) {
            throw IllegalStateException("Geofence SDK must be initialized before triggerLocation()")
        }

        CTGeofenceTaskManager.getInstance().postAsyncSafely(
            "TriggerLocation",
            Runnable {
                ctLocationAdapter!!.getLastLocation(CTLocationCallback { location ->
                    //get's called on bg thread

                    if (location != null) {
                        processTriggeredLocation(location)
                    }

                    Utils.notifyLocationUpdates(context, location)
                })
            })
    }

    internal val accountId: String
        get() = Utils.emptyIfNull(mAccountId)

    /**
     * Sets CleverTap account id which will be used by SDK to recreate [CleverTapAPI]
     * instance when an app is in killed state. Error will be sent to CleverTap in case null or empty.
     *
     * @param accountId CleverTap account id
     */
    private fun setAccountId(accountId: String?) {
        if (accountId == null || accountId.isEmpty()) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "Account Id is null or empty"
            )
            if (this.cleverTapApi != null) {
                this.cleverTapApi!!.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE, "Account Id is null or empty")
            }
            return
        }

        this.mAccountId = accountId
    }

    /**
     * Sets [CleverTapAPI] instance to be used by SDK
     *
     * @param cleverTapAPI instance of [CleverTapAPI]
     */
    private fun setCleverTapApi(cleverTapAPI: CleverTapAPI?) {
        this.cleverTapApi = cleverTapAPI
    }

    /**
     * Creates default instance of [CTGeofenceSettings]
     *
     * @return default instance of [CTGeofenceSettings]
     */
    internal fun initDefaultConfig(): CTGeofenceSettings {
        return CTGeofenceSettings.Builder().build()
    }

    /**
     * Sends Location to CleverTap SDK to send it to server with throttling limit of `minimum
     * 30 minutes` and `minimum displacement of 200 meters` between two location pings.
     * Throttling logic is determined by comparing last pinged location and current one using
     * shared preferences
     *
     * @param location instance of [Location], must be nonnull
     * @return a Future representing pending completion of the task of sending location to server,
     * can be null if CleverTap SDK decides not to send it to server
     */
    internal fun processTriggeredLocation(location: Location): Future<*>? {
        var future: Future<*>? = null

        try {
            val cleverTapAPI = cleverTapApi ?: return null

            val lastStoredLocation = Location("")

            lastStoredLocation.latitude = GeofenceStorageHelper
                .getDouble(context, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE)
            lastStoredLocation.longitude = GeofenceStorageHelper
                .getDouble(context, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE)

            val lastStoredLocationEPMillis = GeofenceStorageHelper.getLong(
                context, CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0
            )
            val nowMillis = System.currentTimeMillis()

            val deltaT = nowMillis - lastStoredLocationEPMillis
            val deltaD = location.distanceTo(lastStoredLocation)

            logger.debug(
                GEOFENCE_LOG_TAG,
                "Delta T for last two locations = $deltaT"
            )
            logger.debug(
                GEOFENCE_LOG_TAG,
                "Delta D for last two locations = $deltaD"
            )

            if (deltaT > INTERVAL_IN_MILLIS && deltaD > SMALLEST_DISPLACEMENT_IN_METERS) {
                logger.debug(
                    GEOFENCE_LOG_TAG,
                    "Sending last location to CleverTap.."
                )

                future = cleverTapAPI.setLocationForGeofences(location, Utils.getGeofenceSDKVersion())

                GeofenceStorageHelper.putDouble(
                    context, CTGeofenceConstants.KEY_LATITUDE, location.latitude
                )
                GeofenceStorageHelper.putDouble(
                    context, CTGeofenceConstants.KEY_LONGITUDE, location.longitude
                )
                GeofenceStorageHelper.putLong(
                    context, CTGeofenceConstants.KEY_LAST_LOCATION_EP, System.currentTimeMillis()
                )
            } else {
                logger.debug(
                    GEOFENCE_LOG_TAG,
                    "Not sending last location to CleverTap"
                )
            }
        } catch (e: Exception) {
            logger.debug(
                GEOFENCE_LOG_TAG,
                "Exception while processing last location for geofences", e
            )
        }

        return future
    }

    /**
     * Activates SDK by registering [GeofenceCallback]
     * and background location updates on background thread by
     * reading config settings if provided or will use default settings.
     */
    private fun activate() {
        if (ctLocationAdapter == null || ctGeofenceAdapter == null || cleverTapApi == null) {
            return
        }

        if (isActivated) {
            logger.verbose(
                GEOFENCE_LOG_TAG,
                "Geofence API already activated! dropping activate() call"
            )
            return
        }

        if (ctGeofenceSettings == null) {
            ctGeofenceSettings = initDefaultConfig()
        }

        logger.setDebugLevel(ctGeofenceSettings!!.logLevel)

        cleverTapApi!!.setGeofenceCallback(this)
        logger.debug(GEOFENCE_LOG_TAG, "geofence callback registered")

        isActivated = true
        initBackgroundLocationUpdates()
    }

    public companion object {
        public const val GEOFENCE_LOG_TAG: String = "CTGeofence"

        private var ctGeofenceAPI: CTGeofenceAPI? = null

        private val logger: Logger = Logger(Logger.DEBUG)

        /**
         * Retrieves the `default` singleton instance of [CTGeofenceAPI].
         *
         * @param context A [Context] for Application
         * @return The singleton instance of [CTGeofenceAPI]
         */
        @JvmStatic
        public fun getInstance(context: Context): CTGeofenceAPI {
            synchronized(CTGeofenceAPI::class.java) {
                if (ctGeofenceAPI == null) {
                    ctGeofenceAPI = CTGeofenceAPI(context)
                }
                return ctGeofenceAPI!!
            }
        }

        @JvmStatic
        public fun getLogger(): Logger {
            return logger
        }
    }
}
