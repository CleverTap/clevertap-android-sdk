package com.clevertap.android.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

internal class NetworkMonitor constructor(
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

    private val connectivityManager: ConnectivityManager?
        get() = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    @Volatile
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _stateFlow = MutableStateFlow(NetworkState.UNDETECTED)
    val networkState: Flow<NetworkState> = _stateFlow.asStateFlow()

    val networkRestoreEvents: Flow<Unit> = _stateFlow
        .drop(1)
        .distinctUntilChanged { old, new -> old.isAvailable == new.isAvailable }
        .filter { it.isAvailable }
        .map { }

    init {
        logger.debug(accountId, "NetworkMonitor initializing...")
        initializeNetworkMonitoring()
    }

    private fun initializeNetworkMonitoring() {
        if (connectivityManager == null) {
            logger.debug(accountId, "ConnectivityManager not available")
            _stateFlow.value = NetworkState.UNDETECTED
            return
        }

        _stateFlow.value = calculateCurrentNetworkState()
        registerNetworkCallback()

        logger.debug(accountId, "NetworkMonitor initialized with state: ${_stateFlow.value}")
    }

    private fun calculateCurrentNetworkState(): NetworkState {
        return try {
            val cm: ConnectivityManager = connectivityManager ?: return NetworkState.UNDETECTED
            val activeNetwork: Network = cm.activeNetwork ?: return NetworkState.DISCONNECTED
            val capabilities: NetworkCapabilities = cm.getNetworkCapabilities(activeNetwork)
                ?: return NetworkState.UNDETECTED
            val hasInternet: Boolean = capabilities.hasInternet()
            val networkType: NetworkType = getNetworkTypeFromCapabilities(capabilities)

            if (!hasInternet) {
                NetworkState.DISCONNECTED
            } else {
                NetworkState(
                    isAvailable = true,
                    networkType = networkType,
                )
            }
        } catch (_: SecurityException) {
            logger.debug(
                accountId,
                "Missing ACCESS_NETWORK_STATE permission. Add it to AndroidManifest.xml"
            )
            NetworkState.UNDETECTED
        } catch (e: Exception) {
            logger.debug(accountId, "Network state calculation failed: ${e.message}")
            NetworkState.UNDETECTED
        }
    }

    private fun registerNetworkCallback() {
        val callback = object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                logger.verbose(accountId, "NetworkCallback#onAvailable: network=$network")
            }

            override fun onLost(network: Network) {
                logger.verbose(accountId, "NetworkCallback#onLost: network=$network")
                _stateFlow.value = NetworkState.DISCONNECTED
                logger.verbose(accountId, "NetworkCallback#onLost: updated state=${_stateFlow.value}")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                logger.verbose(
                    accountId,
                    "NetworkCallback#onCapabilitiesChanged: network=$network, capabilities=$networkCapabilities"
                )
                _stateFlow.value = NetworkState(
                    isAvailable = networkCapabilities.hasInternet(),
                    networkType = getNetworkTypeFromCapabilities(networkCapabilities)
                )
                logger.verbose(
                    accountId,
                    "NetworkCallback#onCapabilitiesChanged: updated state=${_stateFlow.value}"
                )
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager?.registerDefaultNetworkCallback(callback)
                networkCallback = callback
                logger.verbose(accountId, "Network callback registered successfully")
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()
                connectivityManager?.registerNetworkCallback(request, callback)
                networkCallback = callback
                logger.verbose(config.accountId, "API < 24: registered NetworkCallback via NetworkRequest")
            }
        } catch (e: Exception) {
            logger.debug(accountId, "Network callback registration failed: ${e.message}")
        }
    }

    internal fun getCurrentNetworkState(): NetworkState {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            calculateCurrentNetworkState()
        } else {
            _stateFlow.value
        }
    }

    fun isNetworkOnline(): Boolean {
        val state = getCurrentNetworkState()
        val online = state.isAvailable || state.networkType == NetworkType.UNDETECTED
        logger.verbose(accountId, "isNetworkOnline: state=$state, result=$online")
        return online
    }

    fun isWifiConnected(): Boolean {
        val connected = getCurrentNetworkState().isWifiConnected
        logger.verbose(accountId, "isWifiConnected: result=$connected")
        return connected
    }

    fun getNetworkType(): NetworkType {
        val type = getCurrentNetworkState().networkType
        logger.verbose(accountId, "getNetworkType: result=$type")
        return type
    }

    fun getNetworkTypeString(): String? {
        val typeString = when (getNetworkType()) {
            NetworkType.WIFI -> "WiFi"
            NetworkType.CELLULAR -> Utils.getDeviceNetworkType(appContext)
            NetworkType.ETHERNET -> "Ethernet"
            NetworkType.VPN -> "VPN"
            NetworkType.DISCONNECTED -> "Unavailable"
            NetworkType.UNKNOWN -> "Unknown"
            NetworkType.UNDETECTED -> null
        }
        logger.verbose(accountId, "getNetworkTypeString: result=$typeString")
        return typeString
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
        val callback = networkCallback
        networkCallback = null
        callback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
                logger.verbose(accountId, "Network callback unregistered")
            } catch (e: Exception) {
                logger.debug(
                    accountId,
                    "Network callback un-registration failed: ${e.message}"
                )
            }
        }
    }
}
