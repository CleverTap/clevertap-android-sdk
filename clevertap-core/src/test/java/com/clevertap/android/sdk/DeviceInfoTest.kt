package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class DeviceInfoTest : BaseTestCase() {

    private fun newDeviceInfo(cleverTapID: String?): DeviceInfo =
        DeviceInfo(application, cleverTapInstanceConfig, cleverTapID, CoreMetaData(), null)

    @Test
    fun `requireDeviceID returns the resolved id once initDeviceID has run`() {
        // SDK-6254: on the per-account serial queue the device id is already resolved by the
        // barrier, so requireDeviceID() returns it (and matches getDeviceID()).
        val deviceInfo = newDeviceInfo(null)
        deviceInfo.initDeviceID(null)

        val id = deviceInfo.requireDeviceID()
        assertNotNull(id)
        assertEquals(deviceInfo.deviceID, id)
    }

    @Test(expected = IllegalStateException::class)
    fun `requireDeviceID throws when the id has not been resolved yet`() {
        // Fresh DeviceInfo, initDeviceID() never called → no persisted id → getDeviceID() is null,
        // so requireDeviceID() throws. On the serial queue this cannot happen (the barrier runs
        // first); the throw guards against being called off-queue before resolution.
        newDeviceInfo(null).requireDeviceID()
    }
}
