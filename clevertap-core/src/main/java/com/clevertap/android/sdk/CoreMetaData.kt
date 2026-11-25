package com.clevertap.android.sdk

import android.app.Activity
import android.location.Location
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.clevertap.android.sdk.utils.CTJsonConverter
import java.lang.ref.WeakReference
import java.util.HashMap
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

    private var currentScreenName: String? = null

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

    fun setCurrentScreenName(currentScreenName: String?) {
        this.currentScreenName = currentScreenName
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
            if (this.campaignValue == null) {
                this.campaignValue = campaign
            }
        }

    fun getCustomSdkVersion(customSdkName: String): Int {
        var version = customSdkVersions[customSdkName]
        if (version == null) {
            version = sharedCustomSdkVersions[customSdkName]
        }
        return version ?: 0
    }

    fun setCustomSdkVersion(customSdkName: String, customSdkVersion: Int) {
        customSdkVersions[customSdkName] = customSdkVersion
    }

    val allCustomSdkVersions: MutableMap<String, Int>
        get() {
            val allCustomSdkVersions: MutableMap<String, Int> = HashMap(sharedCustomSdkVersions)
            allCustomSdkVersions.putAll(customSdkVersions)
            return allCustomSdkVersions
        }

    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var medium: String?
        get() = mediumValue
        set(medium) {
            if (this.mediumValue == null) {
                this.mediumValue = medium
            }
        }

    //UTM
    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var source: String?
        get() = sourceValue
        set(source) {
            if (this.sourceValue == null) {
                this.sourceValue = source
            }
        }

    val screenName: String?
        get() = currentScreenName

    // only set if not already set during the session
    @get:Synchronized
    @set:Synchronized
    var wzrkParams: JSONObject?
        get() = wzrkParamsValue
        set(wzrkParams) {
            if (this.wzrkParamsValue == null) {
                this.wzrkParamsValue = wzrkParams
            }
        }

    fun inCurrentSession(): Boolean {
        return currentSessionId > 0
    }

    var isAppLaunchPushed: Boolean
        get() {
            synchronized(appLaunchPushedLock) {
                return appLaunchPushed
            }
        }
        set(pushed) {
            synchronized(appLaunchPushedLock) {
                appLaunchPushed = pushed
            }
        }

    var isCurrentUserOptedOut: Boolean
        get() {
            synchronized(optOutFlagLock) {
                return currentUserOptedOut
            }
        }
        set(enable) {
            synchronized(optOutFlagLock) {
                currentUserOptedOut = enable
            }
        }

    var enabledSystemEvents: Boolean
        get() {
            synchronized(optOutFlagLock) {
                return allowSystemEvents
            }
        }
        set(enable) {
            synchronized(optOutFlagLock) {
                allowSystemEvents = enable
            }
        }

    /**
     * Checks if CT instance should send fresh app launch flag (fl: true).
     * Returns true only if:
     * 1. App has been foregrounded (initialAppEnteredForegroundTime > 0)
     * 2. This instance has NOT yet sent fl:true
     */
    val isFreshAppLaunch: Boolean
        get() {
            synchronized(freshAppLaunchLock) {
                return getInitialAppEnteredForegroundTime() > 0 && !freshAppLaunchSent
            }
        }

    /**
     * Marks that this instance has sent the fresh app launch flag.
     */
    fun setFreshAppLaunchSent() {
        synchronized(freshAppLaunchLock) {
            freshAppLaunchSent = true
        }
    }

    companion object {

        private var appForeground = false

        private var currentActivity: WeakReference<Activity>? = null

        private var activityCount = 0

        // Versions registered by modules for every instance, so they apply whether or not an instance exists yet
        private val sharedCustomSdkVersions: MutableMap<String, Int> = ConcurrentHashMap()

        private var initialAppEnteredForegroundTime = 0

        @JvmStatic
        fun getCurrentActivity(): Activity? {
            return currentActivity?.get()
        }

        @JvmStatic
        fun getInitialAppEnteredForegroundTime(): Int {
            return initialAppEnteredForegroundTime
        }

        @JvmStatic
        fun setCurrentActivity(activity: Activity?) {
            if (activity == null) {
                currentActivity = null
                return
            }
            if (!activity.localClassName.contains("InAppNotificationActivity")) {
                currentActivity = WeakReference(activity)
            }
        }

        @JvmStatic
        fun getCurrentActivityName(): String? {
            val current = getCurrentActivity()
            return current?.localClassName
        }

        @JvmStatic
        fun isAppForeground(): Boolean {
            return appForeground
        }

        @JvmStatic
        fun setAppForeground(isForeground: Boolean) {
            appForeground = isForeground
        }

        @JvmStatic
        fun setInitialAppEnteredForegroundTime(initialAppEnteredForegroundTime: Int) {
            this.initialAppEnteredForegroundTime = initialAppEnteredForegroundTime
        }

        @JvmStatic
        fun getActivityCount(): Int {
            return activityCount
        }

        @JvmStatic
        fun setSharedCustomSdkVersion(customSdkName: String, customSdkVersion: Int) {
            // Shared versions reach af for every instance, so they must not replace a built-in af field
            if (CTJsonConverter.RESERVED_AF_KEYS.contains(customSdkName)) {
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
