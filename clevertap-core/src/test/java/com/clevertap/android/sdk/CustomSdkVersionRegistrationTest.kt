package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CustomSdkVersionRegistrationTest : BaseTestCase() {

    @Before
    override fun setUp() {
        super.setUp()
        CleverTapAPI.setInstances(null)
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null)
    }

    @Test
    fun test_registerCustomSdkVersion_whenCalledBeforeInstanceIsCreated_appliesToNewInstance() {
        CleverTapAPI.registerCustomSdkVersion("LaterInstanceModule", 20601)

        val instance = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

        Assert.assertEquals(20601, instance.getCustomSdkVersion("LaterInstanceModule"))
    }

    @Test
    fun test_getAllCustomSdkVersions_includesSharedAndInstanceVersions() {
        val coreMetaData = CoreMetaData()
        CleverTapAPI.registerCustomSdkVersion("SharedModule", 1)
        coreMetaData.setCustomSdkVersion("InstanceModule", 2)

        val all = coreMetaData.allCustomSdkVersions

        Assert.assertEquals(1, all["SharedModule"])
        Assert.assertEquals(2, all["InstanceModule"])
    }

    @Test
    fun test_getAllCustomSdkVersions_whenSameName_instanceVersionWins() {
        val coreMetaData = CoreMetaData()
        CleverTapAPI.registerCustomSdkVersion("OverriddenModule", 1)
        coreMetaData.setCustomSdkVersion("OverriddenModule", 2)

        Assert.assertEquals(2, coreMetaData.allCustomSdkVersions["OverriddenModule"])
        Assert.assertEquals(2, coreMetaData.getCustomSdkVersion("OverriddenModule"))
    }
}
