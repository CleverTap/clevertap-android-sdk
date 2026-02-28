package com.clevertap.android.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import com.clevertap.android.sdk.network.NetworkMonitor.NetworkState
import com.clevertap.android.sdk.network.NetworkMonitor.NetworkType
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowConnectivityManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class NetworkMonitorTest : BaseTestCase() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowCM: ShadowConnectivityManager

    @Before
    override fun setUp() {
        super.setUp()
        connectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowCM = Shadows.shadowOf(connectivityManager)
    }

    private fun createNetworkMonitor() = NetworkMonitor(application, cleverTapInstanceConfig)

    // ─── NetworkState data class tests ────────────────────────────────────────

    @Test
    fun test_networkState_disconnectedConstant_isAvailableFalse() {
        assertFalse(NetworkState.DISCONNECTED.isAvailable)
    }

    @Test
    fun test_networkState_disconnectedConstant_networkTypeIsDisconnected() {
        assertEquals(NetworkType.DISCONNECTED, NetworkState.DISCONNECTED.networkType)
    }

    @Test
    fun test_networkState_disconnectedConstant_isWifiConnectedFalse() {
        assertFalse(NetworkState.DISCONNECTED.isWifiConnected)
    }

    @Test
    fun test_networkState_dataClass_twoEqualObjects_areEqual() {
        val state1 = NetworkState(isAvailable = true, networkType = NetworkType.WIFI, isWifiConnected = true)
        val state2 = NetworkState(isAvailable = true, networkType = NetworkType.WIFI, isWifiConnected = true)
        assertEquals(state1, state2)
    }

    @Test
    fun test_networkState_dataClass_differentTypes_areNotEqual() {
        val wifiState = NetworkState(isAvailable = true, networkType = NetworkType.WIFI, isWifiConnected = true)
        val cellularState = NetworkState(isAvailable = true, networkType = NetworkType.CELLULAR, isWifiConnected = false)
        assertNotEquals(wifiState, cellularState)
    }

    // ─── Static companion method tests ────────────────────────────────────────

    @Test
    fun test_staticIsNetworkOnline_whenNoActiveNetwork_returnsFalse() {
        // Robolectric default: no active network with validated capability
        val result = NetworkMonitor.isNetworkOnline(application)
        assertFalse(result)
    }

    // ─── Instance method tests (Robolectric default = no network) ─────────────

    @Test
    fun test_isNetworkOnline_whenNoNetwork_returnsFalse() {
        val monitor = createNetworkMonitor()
        assertFalse(monitor.isNetworkOnline())
        monitor.cleanup()
    }

    @Test
    fun test_isWifiConnected_whenNoNetwork_returnsFalse() {
        val monitor = createNetworkMonitor()
        assertFalse(monitor.isWifiConnected())
        monitor.cleanup()
    }

    @Test
    fun test_getNetworkType_whenNoNetwork_returnsDisconnected() {
        val monitor = createNetworkMonitor()
        assertEquals(NetworkType.DISCONNECTED, monitor.getNetworkType())
        monitor.cleanup()
    }

    @Test
    fun test_getNetworkTypeString_whenDisconnected_returnsUnavailable() {
        val monitor = createNetworkMonitor()
        assertEquals("Unavailable", monitor.getNetworkTypeString())
        monitor.cleanup()
    }

    @Test
    fun test_getCurrentNetworkState_whenNoNetwork_returnsNonNull() {
        val monitor = createNetworkMonitor()
        assertNotNull(monitor.getCurrentNetworkState())
        monitor.cleanup()
    }

    @Test
    fun test_getCurrentNetworkState_whenNoNetwork_isAvailableIsFalse() {
        val monitor = createNetworkMonitor()
        assertFalse(monitor.getCurrentNetworkState().isAvailable)
        monitor.cleanup()
    }

    // ─── networkState Flow test ────────────────────────────────────────────────

    @Test
    fun test_networkStateFlow_isNotNull() {
        val monitor = createNetworkMonitor()
        assertNotNull(monitor.networkState)
        monitor.cleanup()
    }

    // ─── Cleanup tests ────────────────────────────────────────────────────────

    @Test
    fun test_cleanup_firstCall_doesNotThrow() {
        val monitor = createNetworkMonitor()
        monitor.cleanup()
    }

    @Test
    fun test_cleanup_calledTwice_doesNotThrow() {
        // networkCallback is null after first cleanup — second call should be safe
        val monitor = createNetworkMonitor()
        monitor.cleanup()
        monitor.cleanup()
    }

    @Test
    fun test_cleanup_afterCleanup_isNetworkOnlineStillCallable() {
        // _currentState is still valid after cleanup (only callback is removed)
        val monitor = createNetworkMonitor()
        monitor.cleanup()
        assertFalse(monitor.isNetworkOnline())
    }

    // ─── NetworkType enum tests ────────────────────────────────────────────────

    @Test
    fun test_networkTypeEnum_totalCount_isSix() {
        assertEquals(6, NetworkType.values().size)
    }

    @Test
    fun test_networkTypeEnum_containsWifi() {
        assertTrue(NetworkType.WIFI in NetworkType.values())
    }

    @Test
    fun test_networkTypeEnum_containsCellular() {
        assertTrue(NetworkType.CELLULAR in NetworkType.values())
    }

    @Test
    fun test_networkTypeEnum_containsEthernet() {
        assertTrue(NetworkType.ETHERNET in NetworkType.values())
    }

    @Test
    fun test_networkTypeEnum_containsVpn() {
        assertTrue(NetworkType.VPN in NetworkType.values())
    }

    @Test
    fun test_networkTypeEnum_containsUnknown() {
        assertTrue(NetworkType.UNKNOWN in NetworkType.values())
    }

    @Test
    fun test_networkTypeEnum_containsDisconnected() {
        assertTrue(NetworkType.DISCONNECTED in NetworkType.values())
    }
}
