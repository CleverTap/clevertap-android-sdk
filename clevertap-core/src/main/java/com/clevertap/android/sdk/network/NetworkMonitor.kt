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
        DISCONNECTED
    }

    data class NetworkState(
        val isAvailable: Boolean = false,
        val networkType: NetworkType = NetworkType.UNKNOWN,
        val isWifiConnected: Boolean = false
    ) {
        companion object {
            val DISCONNECTED = NetworkState(
                isAvailable = false,
                networkType = NetworkType.DISCONNECTED,
                isWifiConnected = false
            )
        }

    }

    private val connectivityManager by lazy {
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _stateFlow = MutableStateFlow(NetworkState.DISCONNECTED)
    val networkState: Flow<NetworkState> = _stateFlow.asStateFlow()


    /**
     * EXECUTION STARTS IMMEDIATELY WHEN OBJECT IS CREATED
     */
    init {
        logger.debug(config.accountId, "NetworkMonitor initializing...")
        initializeNetworkMonitoring()
    }

    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            capabilities != null
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            logger.verbose(config.accountId, "isNetworkOnline check failed: ${e.message}")
            false
        }
    }

    /**
     * Starts network monitoring immediately upon object creation
     */
    private fun initializeNetworkMonitoring() {
        if (connectivityManager == null) {
            logger.debug(config.accountId, "ConnectivityManager not available")
            _stateFlow.value = NetworkState.DISCONNECTED
            return
        }

        // Calculate initial state
        updateInternalNetworkState()

        // Register network callback to monitor changes
        registerNetworkCallback()

        logger.debug(config.accountId, "NetworkMonitor initialized with state: ${_stateFlow.value}")
    }

    /**
     * Updates internal state based on current network status
     */
    private fun updateInternalNetworkState() {
        _stateFlow.value = calculateCurrentNetworkState()
    }

    private fun calculateCurrentNetworkState(): NetworkState {
        return try {
            val hasInternet = checkCurrentConnectivity()
            val networkType = getCurrentNetworkType()
            if (!hasInternet || networkType == NetworkType.DISCONNECTED) {
                NetworkState.DISCONNECTED
            } else {
                NetworkState(
                    isAvailable = true,
                    networkType = networkType,
                    isWifiConnected = networkType == NetworkType.WIFI
                )
            }
        } catch (e: Exception) {
            logger.debug(config.accountId, "Network state calculation failed: ${e.message}")
            NetworkState.DISCONNECTED
        }
    }

    /**
     * Registers network callback to monitor system network changes
     */
    private fun registerNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _stateFlow.value = _stateFlow.value.copy(isAvailable = true)
                logger.verbose(config.accountId, "Network available")
            }

            override fun onLost(network: Network) {
                _stateFlow.value = NetworkState.DISCONNECTED
                logger.verbose(config.accountId, "Network lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val type = getNetworkTypeFromCapabilities(networkCapabilities)
                val newState = NetworkState(
                    isAvailable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                    networkType = type,
                    isWifiConnected = type == NetworkType.WIFI
                )
                _stateFlow.value = newState
                logger.verbose(
                    config.accountId,
                    "Network capabilities changed: ${newState.networkType}"
                )
            }

            override fun onUnavailable() {
                _stateFlow.value = NetworkState.DISCONNECTED
                logger.verbose(config.accountId, "Network unavailable")
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

        } catch (e: SecurityException) {
            logger.debug(config.accountId, "Network callback registration failed: ${e.message}")
        } catch (e: Exception) {
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
        }
    }


    /**
     * Gets current network type (internal implementation)
     */
    private fun getCurrentNetworkType(): NetworkType {
        return try {
            val activeNetwork = connectivityManager?.activeNetwork
                ?: return NetworkType.DISCONNECTED
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
                ?: return NetworkType.DISCONNECTED
            getNetworkTypeFromCapabilities(capabilities)
        } catch (e: Exception) {
            logger.debug(config.accountId, "Network type detection failed: ${e.message}")
            NetworkType.UNKNOWN
        }
    }

    private fun getNetworkTypeFromCapabilities(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
    }

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