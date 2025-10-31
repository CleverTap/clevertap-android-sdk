package com.clevertap.android.sdk.features

import android.content.Context
import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.network.NetworkManager
import org.json.JSONObject

/**
 * NetworkFeature encapsulates all network-related components
 */
internal data class NetworkFeature(
    val networkManager: NetworkManager,
    val encryptionManager: NetworkEncryptionManager,
    val networkHeadersListeners: MutableList<NetworkHeadersListener> = mutableListOf()
) : CleverTapFeature {

    fun addNetworkHeadersListener(listener: NetworkHeadersListener) {
        networkHeadersListeners.add(listener)
    }

    fun removeNetworkHeadersListener(listener: NetworkHeadersListener) {
        networkHeadersListeners.remove(listener)
    }

    override fun coreContract(coreContract: CoreContract) {
        networkManager.coreContract = coreContract
    }

    override fun handleApiData(response: JSONObject, stringBody: String, context: Context) {
    }
}