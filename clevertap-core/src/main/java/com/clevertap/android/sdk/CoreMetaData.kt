package com.clevertap.android.sdk

import android.app.Activity
import android.location.Location
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.utils.CTJsonConverter
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * This class stores run time state of CleverTap's instance
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
open class CoreMetaData : CleverTapMetaData() {

    private var appInboxActivityReference: WeakReference<Activity?>? = null

    var appInstallTime: Long = 0

    private var appLaunchPushed = false

    private val appLaunchPushedLock = Any()

    var currentSessionId: Int = 0

    private var currentUserOptedOut = false

    private var allowSystemEvents = true

    var isFirstRequestInSession: Boolean = false

    //Session
    var isFirstSession: Boolean = false

    var geofenceSDKVersion: Int = 0

    var isInstallReferrerDataSent: Boolean = false

    var isBgPing: Boolean = false

    var isLocationForGeofence: Boolean = false

    var isProductConfigRequested: Boolean = false

    //Session
    var lastSessionLength: Int = 0

    var locationFromUser: Location? = null

    var isOffline: Boolean = false

    /**
     * Flag indicating whether the [CTWebInterface] is initialized from outside of the SDK.
     *
     * When this flag is true, it means that the [CTWebInterface] has been initialized externally, typically by the application.
     */
    var isWebInterfaceInitializedExternally: Boolean = false

    /**
     * Last notification received on device from CleverTap in an active session of a process(before process is killed)
     */

    private val optOutFlagLock = Any()

    // Concurrent because modules register from their own threads while the network thread reads it
    private val customSdkVersions: MutableMap<String, Int> = ConcurrentHashMap()

    var referrerClickTime: Long = 0

    private var sourceValue: String? = null
    private var mediumValue: String? = null
    private var campaignValue: String? = null

    private var wzrkParamsValue: JSONObject? = null

    var isRelaxNetwork: Boolean = false

    private var freshAppLaunchSent = false
    private val freshAppLaunchLock = Any()

    // Setter wraps the activity in a WeakReference; getter returns null once it is collected.
    var appInboxActivity: Activity?
        get() = appInboxActivityReference?.get()
        set(activity) {
            appInboxActivityReference = WeakReference(activity)
        }

    var screenName: String? = null
        private set

    fun setCurrentScreenName(currentScreenName: String?) {
        screenName = currentScreenName
    }

    @Synchronized
    fun clearCampaign() {
        campaignValue = null
    }

    @Synchronized
    fun clearMedium() {
        mediumValue = null
    }

    @Synchronized
    fun clearSource() {
        sourceValue = null
    }

    @Synchronized
    fun clearWzrkParams() {
        wzrkParamsValue = null
    }

    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var campaign: String?
        get() = campaignValue
        set(campaign) {
            if (campaignValue == null) {
                campaignValue = campaign
            }
        }

    fun getCustomSdkVersion(customSdkName: String): Int =
        customSdkVersions[customSdkName] ?: sharedCustomSdkVersions[customSdkName] ?: 0

    fun setCustomSdkVersion(customSdkName: String, customSdkVersion: Int) {
        customSdkVersions[customSdkName] = customSdkVersion
    }

    // Instance versions override the shared ones registered by modules.
    val allCustomSdkVersions: MutableMap<String, Int>
        get() = HashMap(sharedCustomSdkVersions).apply { putAll(customSdkVersions) }

    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var medium: String?
        get() = mediumValue
        set(medium) {
            if (mediumValue == null) {
                mediumValue = medium
            }
        }

    //UTM
    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var source: String?
        get() = sourceValue
        set(source) {
            if (sourceValue == null) {
                sourceValue = source
            }
        }

    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var wzrkParams: JSONObject?
        get() = wzrkParamsValue
        set(wzrkParams) {
            if (wzrkParamsValue == null) {
                wzrkParamsValue = wzrkParams
            }
        }

    fun inCurrentSession(): Boolean = currentSessionId > 0

    var isAppLaunchPushed: Boolean
        get() = synchronized(appLaunchPushedLock) { appLaunchPushed }
        set(pushed) = synchronized(appLaunchPushedLock) { appLaunchPushed = pushed }

    var isCurrentUserOptedOut: Boolean
        get() = synchronized(optOutFlagLock) { currentUserOptedOut }
        set(enable) = synchronized(optOutFlagLock) { currentUserOptedOut = enable }

    var enabledSystemEvents: Boolean
        get() = synchronized(optOutFlagLock) { allowSystemEvents }
        set(enable) = synchronized(optOutFlagLock) { allowSystemEvents = enable }

    /**
     * Checks if CT instance should send fresh app launch flag (fl: true).
     * Returns true only if:
     * 1. App has been foregrounded (initialAppEnteredForegroundTime > 0)
     * 2. This instance has NOT yet sent fl:true
     */
    val isFreshAppLaunch: Boolean
        get() = synchronized(freshAppLaunchLock) {
            getInitialAppEnteredForegroundTime() > 0 && !freshAppLaunchSent
        }

    /**
     * Marks that this instance has sent the fresh app launch flag.
     */
    fun setFreshAppLaunchSent() {
        synchronized(freshAppLaunchLock) {
            freshAppLaunchSent = true
        }
    }

    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    companion object {

        private var appForeground = false

        private var currentActivity: WeakReference<Activity>? = null

        private var activityCount = 0

        // Versions registered by modules for every instance, so they apply whether or not an instance exists yet
        private val sharedCustomSdkVersions: MutableMap<String, Int> = ConcurrentHashMap()

        private var initialAppEnteredForegroundTime = 0

        @JvmStatic
        fun getCurrentActivity(): Activity? = currentActivity?.get()

        @JvmStatic
        fun getInitialAppEnteredForegroundTime(): Int = initialAppEnteredForegroundTime

        @JvmStatic
        fun setCurrentActivity(activity: Activity?) {
            when {
                activity == null -> currentActivity = null
                !activity.localClassName.contains("InAppNotificationActivity") ->
                    currentActivity = WeakReference(activity)
            }
        }

        @JvmStatic
        fun getCurrentActivityName(): String? = getCurrentActivity()?.localClassName

        @JvmStatic
        fun isAppForeground(): Boolean = appForeground

        @JvmStatic
        fun setAppForeground(isForeground: Boolean) {
            appForeground = isForeground
        }

        @JvmStatic
        fun setInitialAppEnteredForegroundTime(time: Int) {
            initialAppEnteredForegroundTime = time
        }

        @JvmStatic
        fun getActivityCount(): Int = activityCount

        @JvmStatic
        fun setSharedCustomSdkVersion(customSdkName: String, customSdkVersion: Int) {
            // Shared versions reach af for every instance, so they must not replace a built-in af field
            if (customSdkName in CTJsonConverter.RESERVED_AF_KEYS) {
                Logger.v("Ignoring SDK version for reserved af key $customSdkName")
                return
            }
            sharedCustomSdkVersions[customSdkName] = customSdkVersion
        }

        @JvmStatic
        fun setActivityCount(count: Int) {
            activityCount = count
        }

        @JvmStatic
        fun incrementActivityCount() {
            activityCount++
        }
    }
}
