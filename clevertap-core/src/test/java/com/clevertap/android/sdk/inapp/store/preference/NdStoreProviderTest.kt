package com.clevertap.android.sdk.inapp.store.preference

import android.content.Context
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StoreProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * NdStoreProvider is the single owner of the ND store user-switch lifecycle (it replaced the
 * ChangeUserCallback registration). These tests pin that contract: no provisioning before the device id
 * resolves, one instance per user reused across accesses, and a full rebuild when the user changes — the
 * guarantee that stops user B from reading user A's ND metadata / impressions / counts.
 */
class NdStoreProviderTest {

    private val accountId = "acc"
    private lateinit var context: Context
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var storeProvider: StoreProvider
    private lateinit var provider: NdStoreProvider

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        deviceInfo = mockk(relaxed = true)
        storeProvider = mockk(relaxed = true)
        provider = NdStoreProvider(context, deviceInfo, accountId, storeProvider)
    }

    @Test
    fun `stores are null and unprovisioned while the device id is null`() {
        every { deviceInfo.deviceID } returns null

        assertNull(provider.ndStore)
        assertNull(provider.ndImpressionStore)
        assertNull(provider.ndCountsStore)
        verify(exactly = 0) { storeProvider.provideNdStore(any(), any(), any()) }
        verify(exactly = 0) { storeProvider.provideNdImpressionStore(any(), any(), any()) }
        verify(exactly = 0) { storeProvider.provideNdCountsStore(any(), any(), any()) }
    }

    @Test
    fun `builds each store once for a device id and reuses it across accesses`() {
        every { deviceInfo.deviceID } returns "d1"
        val nd = mockk<NdStore>()
        val imp = mockk<ImpressionStore>()
        val counts = mockk<NdCountsStore>()
        every { storeProvider.provideNdStore(context, "d1", accountId) } returns nd
        every { storeProvider.provideNdImpressionStore(context, "d1", accountId) } returns imp
        every { storeProvider.provideNdCountsStore(context, "d1", accountId) } returns counts

        assertSame(nd, provider.ndStore)
        assertSame(nd, provider.ndStore)          // repeat access
        assertSame(imp, provider.ndImpressionStore)
        assertSame(counts, provider.ndCountsStore)

        verify(exactly = 1) { storeProvider.provideNdStore(context, "d1", accountId) }
        verify(exactly = 1) { storeProvider.provideNdImpressionStore(context, "d1", accountId) }
        verify(exactly = 1) { storeProvider.provideNdCountsStore(context, "d1", accountId) }
    }

    @Test
    fun `rebuilds stores with the new id when the device id changes`() {
        val nd1 = mockk<NdStore>()
        val nd2 = mockk<NdStore>()
        every { storeProvider.provideNdStore(context, "d1", accountId) } returns nd1
        every { storeProvider.provideNdStore(context, "d2", accountId) } returns nd2
        every { storeProvider.provideNdImpressionStore(any(), any(), any()) } returns mockk()
        every { storeProvider.provideNdCountsStore(any(), any(), any()) } returns mockk()

        every { deviceInfo.deviceID } returns "d1"
        assertSame(nd1, provider.ndStore)

        every { deviceInfo.deviceID } returns "d2"
        assertSame(nd2, provider.ndStore)         // new user -> new instance

        verify(exactly = 1) { storeProvider.provideNdStore(context, "d1", accountId) }
        verify(exactly = 1) { storeProvider.provideNdStore(context, "d2", accountId) }
    }
}
