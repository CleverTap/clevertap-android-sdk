package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.validation.ValidationConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class SessionManager(
    private val config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val validationConfig: ValidationConfig?,
    private val localDataStore: LocalDataStore
) : BaseSessionManager() {

    var appLastSeen: Long = 0

    var lastVisitTime: Int = 0
        private set

    var userLastVisitTs: Long = 0
        private set

    // SessionManager/session management
    fun checkTimeoutSession() {
        if (appLastSeen <= 0) {
            return
        }
        val now = System.currentTimeMillis()
        if ((now - appLastSeen) > Constants.SESSION_LENGTH_MINS * 60 * 1000) {
            config.logger.verbose(config.accountId, "Session Timed Out")
            destroySession()
        }
    }

    override fun destroySession() {
        coreMetaData.currentSessionId = 0
        coreMetaData.isRelaxNetwork = false
        coreMetaData.isAppLaunchPushed = false
        if (coreMetaData.isFirstSession) {
            coreMetaData.isFirstSession = false
        }
        config.logger.verbose(config.accountId, "Session destroyed; Session ID is now 0")
        coreMetaData.clearSource()
        coreMetaData.clearMedium()
        coreMetaData.clearCampaign()
        coreMetaData.clearWzrkParams()
    }

    override fun lazyCreateSession(context: Context) {
        if (!coreMetaData.inCurrentSession()) {
            coreMetaData.isFirstRequestInSession = true
            validationConfig?.updateDiscardedEventNames(null)
            createSession(context)
        }
    }

    //Session
    fun updateLastVisitTime() {
        lastVisitTime = localDataStore.getEventDetail(Constants.APP_LAUNCHED_EVENT)?.lastTime ?: -1
    }

    @WorkerThread
    fun updateUserLastVisitTs() {
        userLastVisitTs = localDataStore.readUserEventLog(Constants.APP_LAUNCHED_EVENT)?.lastTs ?: -1
    }

    private fun createSession(context: Context) {
        coreMetaData.currentSessionId = now

        config.logger.verbose(config.accountId, "Session created with ID: ${coreMetaData.currentSessionId}")

        val lastSessionID = StorageHelper.getIntFromPrefs(context, config.accountId, Constants.SESSION_ID_LAST, 0)
        val lastSessionTime = StorageHelper.getIntFromPrefs(context, config.accountId, Constants.LAST_SESSION_EPOCH, 0)
        if (lastSessionTime > 0) {
            coreMetaData.lastSessionLength = lastSessionTime - lastSessionID
        }

        config.logger.verbose(config.accountId, "Last session length: ${coreMetaData.lastSessionLength} seconds")

        if (lastSessionID == 0) {
            coreMetaData.isFirstSession = true
        }
        StorageHelper.putInt(
            context,
            config.accountId,
            Constants.SESSION_ID_LAST,
            coreMetaData.currentSessionId
        )
    }

    val now: Int
        get() = (System.currentTimeMillis() / 1000).toInt()
}
