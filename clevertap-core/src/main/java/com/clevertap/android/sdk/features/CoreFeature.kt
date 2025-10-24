package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.cryption.ICryptHandler
import com.clevertap.android.sdk.product_config.CTProductConfigController
import com.clevertap.android.sdk.response.ARPResponse
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
    val arpResponse: ARPResponse
) : CleverTapFeature {

    private lateinit var coreContract: CoreContract

    override fun coreContract(coreContract: CoreContract) {
        this.coreContract = coreContract
    }

    override fun handleApiData(response: JSONObject, stringBody: String, context: Context) {
        arpResponse.processResponse(response, stringBody, context)
    }
}
