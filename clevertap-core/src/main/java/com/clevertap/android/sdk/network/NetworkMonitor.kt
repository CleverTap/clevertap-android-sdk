package com.clevertap.android.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ILogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import com.clevertap.android.sdk.Utils

internal class NetworkMonitor(
     context: Context,
    private val config: CleverTapInstanceConfig,
    private val logger: ILogger = config.logger
) {
    private val appContext: Context = context.applicationContext
    companion object {
        @JvmStatic
        fun isNetworkOnline(context: Context): Boolean {
            return isNetworkConnected(context)
        }

        private fun isNetworkConnected(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = connectivityManager?.activeNetwork
                    val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
                    capabilities != null
                            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                            && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                } else {
                    @Suppress("DEPRECATION")
                    val networkInfo = connectivityManager?.activeNetworkInfo
                    @Suppress("DEPRECATION")
                    networkInfo != null && networkInfo.isConnected
                }
            } catch (e: Exception) {
                return false //Fail-safe: treat unknown network state as offline
            }
        }
    }

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

    private val _stateFlow = MutableSharedFlow<NetworkState>(replay = 1)
    val networkState: Flow<NetworkState> = _stateFlow.asSharedFlow().distinctUntilChanged()

    // Internal network state managed by the monitor
    @Volatile
    private var _currentState: NetworkState = NetworkState.DISCONNECTED

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
    private fun initializeNetworkMonitoring() {
        if (connectivityManager == null) {
            logger.debug(config.accountId, "ConnectivityManager not available")
            _currentState = NetworkState.DISCONNECTED
            _stateFlow.tryEmit(_currentState)
            return
        }

        // Calculate initial state
        updateInternalNetworkState()

        // Register network callback to monitor changes
        registerNetworkCallback()

        logger.debug(config.accountId, "NetworkMonitor initialized with state: $_currentState")
    }

    /**
     * Updates internal state based on current network status
     */
    private fun updateInternalNetworkState() {
        _currentState = calculateCurrentNetworkState()
        _stateFlow.tryEmit(_currentState)
    }

    private fun calculateCurrentNetworkState(): NetworkState {
        return try {
            val hasInternet = isNetworkConnected(appContext)
            if (!hasInternet) {
                NetworkState.DISCONNECTED
            } else {
                val networkType = getCurrentNetworkType()
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
                updateInternalNetworkState() // Update internal state
                logger.verbose(config.accountId, "Network became available: ${_currentState.networkType}")
            }

            override fun onLost(network: Network) {
                updateInternalNetworkState() // Update internal state
                logger.verbose(config.accountId, "Network lost")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                updateInternalNetworkState() // Update internal state
                logger.verbose(config.accountId, "Network capabilities changed: ${_currentState.networkType}")
            }

            override fun onUnavailable() {
                updateInternalNetworkState() // Update internal state
                logger.verbose(config.accountId, "Network unavailable")
            }
        }

        networkCallback = callback

        try {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()

            connectivityManager?.registerNetworkCallback(networkRequest, callback)
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
    fun getCurrentNetworkState(): NetworkState = _currentState

    fun isNetworkOnline(): Boolean = _currentState.isAvailable

    fun isWifiConnected(): Boolean = _currentState.isWifiConnected

    fun getNetworkType(): NetworkType = _currentState.networkType

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getNetworkTypeFromCapabilities()
            } else {
                getNetworkTypeFromNetworkInfo()
            }
        } catch (e: Exception) {
            logger.debug(config.accountId, "Network type detection failed: ${e.message}")
            NetworkType.UNKNOWN
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun getNetworkTypeFromCapabilities(): NetworkType {
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return NetworkType.UNKNOWN

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.UNKNOWN
        }
    }

    @Suppress("DEPRECATION")
    private fun getNetworkTypeFromNetworkInfo(): NetworkType {
        val networkInfo = connectivityManager?.activeNetworkInfo ?: return NetworkType.DISCONNECTED
        return when (networkInfo.type) {
            ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
            ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
            ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
            ConnectivityManager.TYPE_VPN -> NetworkType.VPN
            else -> NetworkType.UNKNOWN
        }
    }

    fun cleanup() {
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
                logger.verbose(config.accountId, "Network callback unregistered")
            } catch (e: Exception) {
                logger.debug(config.accountId, "Network callback un-registration failed: ${e.message}")
            }
        }
        networkCallback = null
    }
}