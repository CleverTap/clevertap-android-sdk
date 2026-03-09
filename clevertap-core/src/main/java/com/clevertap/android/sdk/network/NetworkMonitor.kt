package com.clevertap.android.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NetworkMonitor(
    context: Context,
    private val config: CleverTapInstanceConfig,
    private val logger: ILogger = config.logger
) {
    private val appContext: Context = context.applicationContext


    enum class NetworkType {
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        UNKNOWN,
        DISCONNECTED,
        UNDETECTED
    }

    data class NetworkState(
        val isAvailable: Boolean = false,
        val networkType: NetworkType = NetworkType.UNKNOWN,
    ) {
        val isWifiConnected: Boolean
            get() = isAvailable && networkType == NetworkType.WIFI
        companion object {
            val DISCONNECTED = NetworkState(
                isAvailable = false,
                networkType = NetworkType.DISCONNECTED
            )
            val UNDETECTED = NetworkState(
                isAvailable = false,
                networkType = NetworkType.UNDETECTED
            )
        }

    }

    private val connectivityManager by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    @Volatile
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    //used to store the network call back object

    private val _stateFlow = MutableStateFlow(NetworkState.UNDETECTED)
    //hot flow = store + update + broadcast
    val networkState: Flow<NetworkState> = _stateFlow.asStateFlow()


    /**
     * EXECUTION STARTS IMMEDIATELY WHEN OBJECT IS CREATED
     */
    init {
        logger.debug(config.accountId, "NetworkMonitor initializing...")
        initializeNetworkMonitoring()
    }


    /**
     * Starts network monitoring immediately upon object creation
     */
    private fun initializeNetworkMonitoring() {   //main
        if (connectivityManager == null) {
            logger.debug(config.accountId, "ConnectivityManager not available")
            _stateFlow.value = NetworkState.UNDETECTED
            return
        }

        // Calculate initial state

        _stateFlow.value = calculateCurrentNetworkState()

        // Register network callback to monitor changes
        registerNetworkCallback()

        logger.debug(config.accountId, "NetworkMonitor initialized with state: ${_stateFlow.value}")
    }

    private fun calculateCurrentNetworkState(): NetworkState {
        return try {
            val cm = connectivityManager ?: return NetworkState.DISCONNECTED
            val activeNetwork = cm.activeNetwork ?: return NetworkState.DISCONNECTED
            val capabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return NetworkState.DISCONNECTED
            val hasInternet = capabilities.hasInternet()
            val networkType =  getNetworkTypeFromCapabilities(capabilities)
            if (!hasInternet || networkType == NetworkType.DISCONNECTED) {
                NetworkState.DISCONNECTED
            } else {
                NetworkState(
                    isAvailable = true,
                    networkType = networkType,
                )
            }
        } catch (e: Exception) {
            logger.debug(config.accountId, "Network state calculation failed: ${e.message}")
            NetworkState.UNDETECTED
        }
    }

    /**
     * Registers network callback to monitor system network changes
     */
    private fun registerNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                logger.verbose(config.accountId, "NetworkCallback#onAvailable: network=$network")
                _stateFlow.value = calculateCurrentNetworkState()
                logger.verbose(config.accountId, "NetworkCallback#onAvailable: updated state=${_stateFlow.value}")
            }

            override fun onLost(network: Network) {
                logger.verbose(config.accountId, "NetworkCallback#onLost: network=$network")
                _stateFlow.value = NetworkState.DISCONNECTED
                logger.verbose(config.accountId, "NetworkCallback#onLost: updated state=${_stateFlow.value}")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                logger.verbose(config.accountId, "NetworkCallback#onCapabilitiesChanged: network=$network, capabilities=$networkCapabilities")
                val type = getNetworkTypeFromCapabilities(networkCapabilities)
                val newState = NetworkState(
                    isAvailable = networkCapabilities.hasInternet(),
                    networkType = type,
                )
                if (_stateFlow.value != newState) {
                    _stateFlow.value = newState
                    logger.verbose(config.accountId, "NetworkCallback#onCapabilitiesChanged: updated state=${_stateFlow.value}")
                }
            }

            override fun onUnavailable() {
                logger.verbose(config.accountId, "NetworkCallback#onUnavailable: no network satisfies the request")
                _stateFlow.value = NetworkState.DISCONNECTED
                logger.verbose(config.accountId, "NetworkCallback#onUnavailable: updated state=${_stateFlow.value}")
            }
        }


        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()
            connectivityManager?.registerNetworkCallback(networkRequest, callback)
            networkCallback = callback
            logger.verbose(config.accountId, "Network callback registered successfully")

        }  catch (e: Exception) {
            logger.debug(config.accountId, "Network callback registration failed: ${e.message}")
        }
    }

    /**
     * Synchronous methods that check the current internal state
     */
    fun getCurrentNetworkState(): NetworkState = _stateFlow.value

    fun isNetworkOnline(): Boolean = _stateFlow.value.isAvailable

    fun isWifiConnected(): Boolean = _stateFlow.value.isWifiConnected

    fun getNetworkType(): NetworkType = _stateFlow.value.networkType

    fun getNetworkTypeString(): String {
        return when (getNetworkType()) {
            NetworkType.WIFI -> "WiFi"
            NetworkType.CELLULAR -> Utils.getDeviceNetworkType(appContext)
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.VPN -> "VPN"
            NetworkType.DISCONNECTED -> "Unavailable"
            NetworkType.UNKNOWN -> "Unknown"
            NetworkType.UNDETECTED -> "Undetected"
        }
    }


    private fun getNetworkTypeFromCapabilities(capabilities: NetworkCapabilities): NetworkType {
        return when {

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
    }

    private fun NetworkCapabilities.hasInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    fun cleanup() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
                logger.verbose(config.accountId, "Network callback unregistered")
            } catch (e: Exception) {
                logger.debug(
                    config.accountId,
                    "Network callback un-registration failed: ${e.message}"
                )
            }
        }
        networkCallback = null
    }
}