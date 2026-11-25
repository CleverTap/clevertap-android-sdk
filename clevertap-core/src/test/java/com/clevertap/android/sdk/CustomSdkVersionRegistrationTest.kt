package com.clevertap.android.sdk

import android.location.Location
import android.os.Bundle
import com.clevertap.android.sdk.utils.CTJsonConverter
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

        val instance = CleverTapAPI.instanceWithConfig(application, cleverTapInstanceConfig)!!

        assertEquals(20601, instance.getCustomSdkVersion("LaterInstanceModule"))
    }

    @Test
    fun test_getAllCustomSdkVersions_includesSharedAndInstanceVersions() {
        val coreMetaData = CoreMetaData()
        CoreMetaData.setSharedCustomSdkVersion("SharedModule", 1)
        coreMetaData.setCustomSdkVersion("InstanceModule", 2)

        val all = coreMetaData.allCustomSdkVersions

        assertEquals(1, all["SharedModule"])
        assertEquals(2, all["InstanceModule"])
    }

    @Test
    fun test_getAllCustomSdkVersions_whenSameName_instanceVersionWins() {
        val coreMetaData = CoreMetaData()
        CoreMetaData.setSharedCustomSdkVersion("OverriddenModule", 1)
        coreMetaData.setCustomSdkVersion("OverriddenModule", 2)

        assertEquals(2, coreMetaData.allCustomSdkVersions["OverriddenModule"])
        assertEquals(2, coreMetaData.getCustomSdkVersion("OverriddenModule"))
    }

    @Test
    fun test_registerModuleSdkVersions_registersIntegerAndNumericStringValues() {
        val metaData = Bundle().apply {
            putInt(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestIntModule", 10400)
            putString(ManifestInfo.LABEL_MODULE_SDK_VERSION_PREFIX + "ManifestStringModule", "10500")
        }

        ManifestInfo.registerModuleSdkVersions(metaData)

        val all = CoreMetaData().allCustomSdkVersions
        assertEquals(10400, all["ManifestIntModule"])
        assertEquals(10500, all["ManifestStringModule"])
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
        assertFalse(all.containsKey("ManifestBadModule"))
        assertFalse(all.containsKey("ManifestNullModule"))
        assertFalse(all.containsKey(""))
        assertFalse(all.containsKey("ManifestUnrelatedModule"))
    }

    @Test
    fun test_setSharedCustomSdkVersion_whenReservedAfKey_isIgnored() {
        CoreMetaData.setSharedCustomSdkVersion("Build", 10400)
        CoreMetaData.setSharedCustomSdkVersion("SDK Version", 10400)

        val all = CoreMetaData().allCustomSdkVersions
        assertFalse(all.containsKey("Build"))
        assertFalse(all.containsKey("SDK Version"))
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
        assertEquals("1", af.get("Build"))
        assertEquals(10400, af.get("gfSDKVersion"))
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
            assertTrue(af.has("LIAMC"))
            af.keys().asSequence().filterNot { it in customKeys }.forEach {
                assertTrue(it in CTJsonConverter.RESERVED_AF_KEYS, "af key $it missing from RESERVED_AF_KEYS")
            }
        }
    }
}
