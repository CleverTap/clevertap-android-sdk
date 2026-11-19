package com.clevertap.android.sdk

import android.location.Location
import android.os.Bundle
import com.clevertap.android.sdk.utils.CTJsonConverter
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
    fun test_setSharedCustomSdkVersion_whenCalledBeforeInstanceIsCreated_appliesToNewInstance() {
        CoreMetaData.setSharedCustomSdkVersion("LaterInstanceModule", 20601)

        val instance = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)

        Assert.assertEquals(20601, instance.getCustomSdkVersion("LaterInstanceModule"))
    }

    @Test
    fun test_getAllCustomSdkVersions_includesSharedAndInstanceVersions() {
        val coreMetaData = CoreMetaData()
        CoreMetaData.setSharedCustomSdkVersion("SharedModule", 1)
        coreMetaData.setCustomSdkVersion("InstanceModule", 2)

        val all = coreMetaData.allCustomSdkVersions

        Assert.assertEquals(1, all["SharedModule"])
        Assert.assertEquals(2, all["InstanceModule"])
    }

    @Test
    fun test_getAllCustomSdkVersions_whenSameName_instanceVersionWins() {
        val coreMetaData = CoreMetaData()
        CoreMetaData.setSharedCustomSdkVersion("OverriddenModule", 1)
        coreMetaData.setCustomSdkVersion("OverriddenModule", 2)

        Assert.assertEquals(2, coreMetaData.allCustomSdkVersions["OverriddenModule"])
        Assert.assertEquals(2, coreMetaData.getCustomSdkVersion("OverriddenModule"))
    }

    @Test
    fun test_registerModuleSdkVersions_registersIntegerAndNumericStringValues() {
        val metaData = Bundle().apply {
            putInt(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestIntModule", 10400)
            putString(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestStringModule", "10500")
        }

        ManifestInfo.registerModuleSdkVersions(metaData)

        val all = CoreMetaData().allCustomSdkVersions
        Assert.assertEquals(10400, all["ManifestIntModule"])
        Assert.assertEquals(10500, all["ManifestStringModule"])
    }

    @Test
    fun test_registerModuleSdkVersions_skipsInvalidAndUnrelatedKeys() {
        val metaData = Bundle().apply {
            putString(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestBadModule", "1.4.0")
            putString(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestNullModule", null)
            putInt(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX, 1)
            putInt("ManifestUnrelatedModule", 1)
        }

        ManifestInfo.registerModuleSdkVersions(metaData)

        val all = CoreMetaData().allCustomSdkVersions
        Assert.assertFalse(all.containsKey("ManifestBadModule"))
        Assert.assertFalse(all.containsKey("ManifestNullModule"))
        Assert.assertFalse(all.containsKey(""))
        Assert.assertFalse(all.containsKey("ManifestUnrelatedModule"))
    }

    @Test
    fun test_setSharedCustomSdkVersion_whenReservedAfKey_isIgnored() {
        CoreMetaData.setSharedCustomSdkVersion("Build", 10400)
        CoreMetaData.setSharedCustomSdkVersion("SDK Version", 10400)

        val all = CoreMetaData().allCustomSdkVersions
        Assert.assertFalse(all.containsKey("Build"))
        Assert.assertFalse(all.containsKey("SDK Version"))
    }

    @Test
    fun test_registerModuleSdkVersions_whenReservedAfKey_keepsBuiltInAfValue() {
        val metaData = Bundle().apply {
            putInt(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "Build", 10400)
            putInt(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "gfSDKVersion", 10400)
        }

        ManifestInfo.registerModuleSdkVersions(metaData)

        val coreMetaData = CoreMetaData()
        val deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, "id", coreMetaData)
        val af = CTJsonConverter.from(deviceInfo, coreMetaData, false, false)
        Assert.assertEquals("1", af.get("Build"))
        Assert.assertEquals(10400, af.get("gfSDKVersion"))
    }

    @Test
    fun test_reservedAfKeys_coverEveryKeyWrittenByFrom() {
        val coreMetaData = CoreMetaData()
        coreMetaData.locationFromUser = Location("test")
        val deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, "id", coreMetaData)
        val customKeys = coreMetaData.allCustomSdkVersions.keys

        for (multiUser in listOf(false, true)) {
            val af = CTJsonConverter.from(deviceInfo, coreMetaData, true, multiUser)
            // from() swallows errors, so make sure it got to the end
            Assert.assertTrue(af.has("LIAMC"))
            af.keys().asSequence().filterNot { it in customKeys }.forEach {
                Assert.assertTrue("af key $it missing from RESERVED_AF_KEYS", it in CTJsonConverter.RESERVED_AF_KEYS)
            }
        }
    }
}
