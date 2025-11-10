package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CTLockManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.features.callbacks.CoreClientCallbacks
import com.clevertap.android.sdk.network.ArpRepo
import com.clevertap.android.sdk.network.IJRepo
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.ConsoleResponse
import com.clevertap.android.sdk.response.MetadataResponse
import com.clevertap.android.sdk.task.CTExecutors
import com.clevertap.android.sdk.task.MainLooperHandler
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import org.json.JSONObject

/**
 * Core infrastructure used by all features
 * Contains fundamental components like context, config, device info, executors, etc.
 */
internal data class CoreFeature(
    val context: Context,
    val config: CleverTapInstanceConfig,
    val deviceInfo: DeviceInfo,
    val coreMetaData: CoreMetaData,
    val executors: CTExecutors,
    val mainLooperHandler: MainLooperHandler,
    val validationResultStack: ValidationResultStack,
    val cryptHandler: ICryptHandler,
    val clock: Clock,
    val ctLockManager: CTLockManager,
    val arpRepo: ArpRepo = ArpRepo(config.accountId, config.logger, deviceInfo),
    val ijRepo: IJRepo = IJRepo(config.accountId),
    val arpResponse: ARPResponse = ARPResponse(config.accountId, config.logger),
    val metadataResponse: MetadataResponse = MetadataResponse(config.accountId, config.logger),
    val consoleResponse: ConsoleResponse = ConsoleResponse(config.accountId, config.logger),
    val coreCallbacks: CoreClientCallbacks = CoreClientCallbacks()
) : CleverTapFeature {

    private lateinit var coreContract: CoreContract

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(response: JSONObject, stringBody: String, context: Context) {
        consoleResponse.processResponse(response)
        metadataResponse.processResponse(response, context, ijRepo, deviceInfo)
        arpResponse.processResponse(response, context, arpRepo)
    }
}
