package com.clevertap.android.sdk.network.api

import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.network.NetworkRepo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CtApiWrapperTest : BaseTestCase() {

    private lateinit var ctApiWrapper: CtApiWrapper
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var guid: String

    private lateinit var networkRepo: NetworkRepo

    @Before
    override fun setUp() {
        super.setUp()
        guid = "1212121221"
        coreMetaData = CoreMetaData()
        deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
        networkRepo = NetworkRepo(application, cleverTapInstanceConfig)
        ctApiWrapper = CtApiWrapper(networkRepo, cleverTapInstanceConfig, deviceInfo)
    }

    @Test
    fun `test ctApi initialization`() {
        assertNotNull(ctApiWrapper.ctApi)
    }

    @Test
    fun `test ctApi when multiple times get is called returns same CtApi instance`() {
        val ctApi1 = ctApiWrapper.ctApi
        val ctApi2 = ctApiWrapper.ctApi
        val ctApi3 = ctApiWrapper.ctApi
        assertNotNull(ctApi1)
        assertNotNull(ctApi2)
        assertNotNull(ctApi3)
        assertEquals(ctApi1,ctApi2)
        assertEquals(ctApi1,ctApi3)
        assertEquals(ctApi2,ctApi3)
    }

    @Test
    fun `test ctApi when different CtApiWrapper creates unique CtApi instance`() {

        val ctApiWrapper1 = CtApiWrapper(networkRepo, cleverTapInstanceConfig, deviceInfo)
        val ctApiWrapper2 = CtApiWrapper(networkRepo, cleverTapInstanceConfig, deviceInfo)
        val ctApiWrapper3 = CtApiWrapper(networkRepo, cleverTapInstanceConfig, deviceInfo)
        val ctApi1 = ctApiWrapper1.ctApi
        val ctApi2 = ctApiWrapper2.ctApi
        val ctApi3 = ctApiWrapper3.ctApi
        assertNotNull(ctApi1)
        assertNotNull(ctApi2)
        assertNotNull(ctApi3)
        assertNotEquals(ctApi1,ctApi2)
        assertNotEquals(ctApi1,ctApi3)
        assertNotEquals(ctApi2,ctApi3)
    }
}