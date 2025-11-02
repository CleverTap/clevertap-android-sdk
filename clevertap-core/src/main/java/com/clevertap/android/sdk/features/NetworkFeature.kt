package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.NetworkRepo
import org.json.JSONObject

/**
 * NetworkFeature encapsulates all network-related components
 */
internal data class NetworkFeature(
    val networkManager: NetworkManager,
    val encryptionManager: NetworkEncryptionManager,
    val networkRepo: NetworkRepo,
) : CleverTapFeature {

    override fun coreContract(coreContract: CoreContract) {
        networkManager.coreContract = coreContract
    }

    override fun handleApiData(response: JSONObject, stringBody: String, context: Context) {
    }
}