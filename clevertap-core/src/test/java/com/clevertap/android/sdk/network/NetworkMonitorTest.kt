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
import org.robolectric.shadows.ShadowNetworkInfo
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
    fun test_networkTypeEnum_containsAllExpectedValues() {
        val expectedTypes = setOf(
            NetworkType.WIFI, NetworkType.CELLULAR, NetworkType.ETHERNET,
            NetworkType.VPN, NetworkType.UNKNOWN, NetworkType.DISCONNECTED
        )
        assertEquals(expectedTypes, NetworkType.values().toSet())
    }


    @Test
    @Suppress("DEPRECATION")
    fun test_isNetworkOnline_whenNetworkConnected_returnsTrue() {
        val netInfo = ShadowNetworkInfo.newInstance(
            android.net.NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, true
        )
        shadowCM.setActiveNetworkInfo(netInfo)
        val monitor = createNetworkMonitor()
        assertTrue(monitor.isNetworkOnline())
        monitor.cleanup()
    }

    @Test
    @Suppress("DEPRECATION")
    fun test_isNetworkOnline_whenNetworkUnavailable_returnsFalse() {
        shadowCM.setActiveNetworkInfo(null)
        val monitor = createNetworkMonitor()
        assertFalse(monitor.isNetworkOnline())
        monitor.cleanup()
    }

}
