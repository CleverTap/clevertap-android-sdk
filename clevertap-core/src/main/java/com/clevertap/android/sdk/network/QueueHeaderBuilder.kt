package com.clevertap.android.sdk.network

import android.content.Context
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.login.IdentityRepoFactory
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONException
import org.json.JSONObject

/**
 * Generates JsonObject of the header to be sent with the request for clevertap apis
 */
internal class QueueHeaderBuilder(
    private val context: Context,
    private val config: CleverTapInstanceConfig,
    private val coreMetaData: CoreMetaData,
    private val controllerManager: ControllerManager,
    private val deviceInfo: DeviceInfo,
    private val arpRepo: ArpRepo,
    private val ijRepo: IJRepo,
    private val databaseManager: BaseDatabaseManager,
    private val validationResultStack: ValidationResultStack,
    private val firstRequestTs: () -> Int,
    private val lastRequestTs: () -> Int,
    private val logger: ILogger
) {
    fun buildHeader(caller: String?): JSONObject? {
        val accountId = config.accountId
        val token = config.accountToken
        if (accountId == null || token == null) {
            logger.debug(config.accountId, "Account ID/token not found, unable to configure queue request")
            return null
        }
        try {
            val header = JSONObject()
            addCaller(header, caller)
            addDeviceId(header)
            addType(header)
            addAppFields(header)
            addIJ(header)
            addConfigFields(header)
            addIdentities(header)
            addDoNotDisturb(header)
            addBackgroundPing(header)
            addRenderedTargetList(header)
            addInstallReferrerData(header)
            addFirstRequestInSession(header)
            addDebugFlag(header)
            addARP(header)
            addReferrerInfo(header)
            addWzrkParams(header)
            addInAppFC(header)
            return header
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "CommsManager: Failed to attach header", e)
            return null
        }
    }

    private fun addCaller(header: JSONObject, caller: String?) {
        if (caller != null) {
            header.put(Constants.D_SRC, caller)
        }
    }

    private fun addDeviceId(header: JSONObject) {
        val deviceId = deviceInfo.deviceID
        if (!deviceId.isNullOrEmpty()) {
            header.put("g", deviceId)
        } else {
            logger.verbose(config.accountId, "CRITICAL: Couldn't finalise on a device ID! Using error device ID instead!")
        }
    }

    private fun addType(header: JSONObject) {
        header.put("type", "meta")
    }

    private fun addAppFields(header: JSONObject) {
        val appFields = deviceInfo.appLaunchedFields
        if (coreMetaData.isWebInterfaceInitializedExternally) {
            appFields.put("wv_init", true)
        }
        header.put("af", appFields)
    }

    private fun addIJ(header: JSONObject) {
        val i = ijRepo.getI(context)
        if (i > 0) {
            header.put("_i", i)
        }
        val j = ijRepo.getJ(context)
        if (j > 0) {
            header.put("_j", j)
        }
    }

    private fun addConfigFields(header: JSONObject) {
        val accountId = config.accountId
        val token = config.accountToken
        header.apply {
            put("id", accountId)
            put("tk", token)
            put("l_ts", lastRequestTs())
            put("f_ts", firstRequestTs())
        }
    }

    private fun addIdentities(header: JSONObject) {
        header.put("ct_pi", IdentityRepoFactory.getRepo(context, config, validationResultStack).identitySet.toString())
    }

    private fun addDoNotDisturb(header: JSONObject) {
        header.put("ddnd", !(context.areAppNotificationsEnabled() && (controllerManager.pushProviders == null || controllerManager.pushProviders.isNotificationSupported)))
    }

    private fun addBackgroundPing(header: JSONObject) {
        if (coreMetaData.isBgPing) {
            header.put("bk", 1)
            coreMetaData.isBgPing = false
        }
    }

    private fun addRenderedTargetList(header: JSONObject) {
        val pushIds = databaseManager.loadDBAdapter(context).fetchPushNotificationIds()
        header.put("rtl", CTJsonConverter.pushIdsToJSONArray(pushIds))
    }

    private fun addInstallReferrerData(header: JSONObject) {
        if (!coreMetaData.isInstallReferrerDataSent) {
            header.put("rct", coreMetaData.referrerClickTime)
            header.put("ait", coreMetaData.appInstallTime)
        }
    }

    private fun addFirstRequestInSession(header: JSONObject) {
        header.put("frs", coreMetaData.isFirstRequestInSession)
        coreMetaData.isFirstRequestInSession = false
    }

    private fun addDebugFlag(header: JSONObject) {
        if (CleverTapAPI.getDebugLevel() == 3) {
            header.put("debug", true)
        }
    }

    private fun addARP(header: JSONObject) {
        try {
            val arp = arpRepo.getARP(context)
            if (arp != null && arp.length() > 0) {
                header.put("arp", arp)
            }
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "Failed to attach ARP", e)
        }
    }

    private fun addReferrerInfo(header: JSONObject) {
        try {
            val ref = JSONObject().apply {
                coreMetaData.source?.let { put("us", it) }
                coreMetaData.medium?.let { put("um", it) }
                coreMetaData.campaign?.let { put("uc", it) }
            }

            if (ref.length() > 0) {
                header.put("ref", ref)
            }
        } catch (e: JSONException) {
            logger.verbose(config.accountId, "Failed to attach ref", e)
        }
    }

    private fun addWzrkParams(header: JSONObject) {
        val wzrkParams = coreMetaData.wzrkParams
        if (wzrkParams != null && wzrkParams.length() > 0) {
            header.put("wzrk_ref", wzrkParams)
        }
    }

    private fun addInAppFC(header: JSONObject) {
        controllerManager.inAppFCManager?.let {
            Logger.v("Attaching InAppFC to Header")
            header.put("imp", it.shownTodayCount)
            header.put("tlc", it.getInAppsCount(context))
        } ?: logger.verbose(config.accountId, "controllerManager.getInAppFCManager() is NULL, not Attaching InAppFC to Header")
    }
}
