package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.validation.ValidationConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class SessionManager(
    private val config: CleverTapInstanceConfig,
    coreMetaData: CoreMetaData,
    private val validationConfig: ValidationConfig?,
    private val localDataStore: LocalDataStore
) : BaseSessionManager() {

    var appLastSeen: Long = 0

    var lastVisitTime: Int = 0
        private set

    var userLastVisitTs: Long = 0
        private set

    private val cleverTapMetaData: CoreMetaData = coreMetaData

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
        cleverTapMetaData.currentSessionId = 0
        cleverTapMetaData.isRelaxNetwork = false
        cleverTapMetaData.isAppLaunchPushed = false
        if (cleverTapMetaData.isFirstSession) {
            cleverTapMetaData.isFirstSession = false
        }
        config.logger.verbose(config.accountId, "Session destroyed; Session ID is now 0")
        cleverTapMetaData.clearSource()
        cleverTapMetaData.clearMedium()
        cleverTapMetaData.clearCampaign()
        cleverTapMetaData.clearWzrkParams()
    }

    override fun lazyCreateSession(context: Context) {
        if (!cleverTapMetaData.inCurrentSession()) {
            cleverTapMetaData.isFirstRequestInSession = true
            validationConfig?.updateDiscardedEventNames(null)
            createSession(context)
        }
    }

    //Session
    fun setLastVisitTime() {
        val ed = localDataStore.getEventDetail(Constants.APP_LAUNCHED_EVENT)
        lastVisitTime = ed?.lastTime ?: -1
    }

    @WorkerThread
    fun setUserLastVisitTs() {
        val appLaunchedEventLog = localDataStore.readUserEventLog(Constants.APP_LAUNCHED_EVENT)
        userLastVisitTs = appLaunchedEventLog?.lastTs ?: -1
    }

    private fun createSession(context: Context) {
        val sessionId = now
        cleverTapMetaData.currentSessionId = sessionId

        config.logger.verbose(
            config.accountId,
            "Session created with ID: " + cleverTapMetaData.currentSessionId
        )

        val lastSessionID = StorageHelper.getIntFromPrefs(context, config.accountId, Constants.SESSION_ID_LAST, 0)
        val lastSessionTime = StorageHelper.getIntFromPrefs(context, config.accountId, Constants.LAST_SESSION_EPOCH, 0)
        if (lastSessionTime > 0) {
            cleverTapMetaData.lastSessionLength = lastSessionTime - lastSessionID
        }

        config.logger.verbose(
            config.accountId,
            "Last session length: " + cleverTapMetaData.lastSessionLength + " seconds"
        )

        if (lastSessionID == 0) {
            cleverTapMetaData.isFirstSession = true
        }
        StorageHelper.putInt(
            context,
            config.accountId,
            Constants.SESSION_ID_LAST,
            cleverTapMetaData.currentSessionId
        )
    }

    val now: Int
        get() = (System.currentTimeMillis() / 1000).toInt()
}
