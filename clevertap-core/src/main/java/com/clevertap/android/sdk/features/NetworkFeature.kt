package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract
import com.clevertap.android.sdk.cryption.CTKeyGenerator
import com.clevertap.android.sdk.cryption.CryptFactory
import com.clevertap.android.sdk.network.NetworkEncryptionManager
import com.clevertap.android.sdk.network.NetworkManager
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.sdk.network.api.CtApiWrapper
import org.json.JSONObject

/**
 * NetworkFeature encapsulates all network-related components
 * Manages network requests, encryption, and API communication
 */
internal class NetworkFeature(
    val networkRepo: NetworkRepo,  // Shared with DBManager
    private val ctKeyGenerator: CTKeyGenerator,
    private val cryptFactory: CryptFactory
) : CleverTapFeature {

    lateinit var coreContract: CoreContract

    // Lazy-initialized Network dependencies (initialized after coreContract is set)
    val encryptionManager: NetworkEncryptionManager by lazy {
        NetworkEncryptionManager(
            keyGenerator = ctKeyGenerator,
            aesgcm = cryptFactory.getAesGcmCrypt()
        )
    }

    val ctApiWrapper: CtApiWrapper by lazy {
        CtApiWrapper(
            networkRepo = networkRepo,
            config = coreContract.config(),
            deviceInfo = coreContract.deviceInfo()
        )
    }

    val networkManager: NetworkManager by lazy {
        NetworkManager(
            ctApiWrapper = ctApiWrapper,
            encryptionManager = encryptionManager,
            networkRepo = networkRepo
        ).apply {
            coreContract = this@NetworkFeature.coreContract
        }
    }

    override fun handleApiData(response: JSONObject) {
        //no-op
    }
}
