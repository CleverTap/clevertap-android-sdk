package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkHeadersListener
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.response.ARPResponse
import com.clevertap.android.sdk.response.ClevertapResponseHandler

/**
 * NetworkFeature encapsulates all network-related components
 */
internal data class NetworkFeature(
    val networkManager: NetworkManager,
    val contentFetchManager: ContentFetchManager,
    val encryptionManager: NetworkEncryptionManager,
    val arpResponse: ARPResponse,
    val clevertapResponseHandler: ClevertapResponseHandler,
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
}