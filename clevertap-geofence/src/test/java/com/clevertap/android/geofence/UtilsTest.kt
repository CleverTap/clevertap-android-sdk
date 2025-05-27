package com.clevertap.android.geofence

import android.Manifest
import android.location.Location
import android.os.Build
import com.clevertap.android.geofence.fakes.CTGeofenceSettingsFake.getSettings
import com.clevertap.android.geofence.fakes.CTGeofenceSettingsFake.settingsJsonObject
import com.clevertap.android.geofence.fakes.CTGeofenceSettingsFake.settingsJsonString
import com.clevertap.android.geofence.fakes.GeofenceJSON.emptyGeofence
import com.clevertap.android.geofence.fakes.GeofenceJSON.emptyJson
import com.clevertap.android.geofence.fakes.GeofenceJSON.firstFromGeofenceArray
import com.clevertap.android.geofence.fakes.GeofenceJSON.geofence
import com.clevertap.android.geofence.fakes.GeofenceJSON.geofenceArray
import com.clevertap.android.geofence.fakes.GeofenceJSON.lastFromGeofenceArray
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener
import com.clevertap.android.sdk.CleverTapAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import org.robolectric.util.ReflectionHelpers
import org.skyscreamer.jsonassert.JSONAssert

class UtilsTest : BaseTestCase() {
    private lateinit var cleverTapAPI: CleverTapAPI
    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var logger: Logger
    private lateinit var shadowApplication: ShadowApplication

    override fun setUp() {
        super.setUp()

        cleverTapAPI = mockk(relaxed = true)
        ctGeofenceAPI = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        shadowApplication = Shadows.shadowOf(application)

        mockkStatic(CTGeofenceAPI::class)

        every { CTGeofenceAPI.getInstance(application) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
    }

    @Test
    fun testEmptyIfNull() {
        //when string is null
        val actualWhenStringIsNull = Utils.emptyIfNull(null)
        Assert.assertEquals("", actualWhenStringIsNull)

        //when string is non null
        val actualWhenStringIsNonNull = Utils.emptyIfNull("1")
        Assert.assertEquals("1", actualWhenStringIsNonNull)
    }

    @Test
    fun testHasBackgroundLocationPermission() {
        val originalSdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.P
        )

        // when SDK Level is less than Q
        val actualWhenSdkIsP = Utils.hasBackgroundLocationPermission(application)
        Assert.assertTrue(
            "hasBackgroundLocationPermission must return true when sdk int is less than Q",
            actualWhenSdkIsP
        )

        // when SDK Level is greater than P and permission denied
        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            Build.VERSION_CODES.Q
        )

        shadowApplication.denyPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        val actualWhenPermissionDenied = Utils.hasBackgroundLocationPermission(application)
        Assert.assertFalse(
            "hasBackgroundLocationPermission must return false when permission " +
                    "is denied and sdk int is greater than P", actualWhenPermissionDenied
        )

        ReflectionHelpers.setStaticField(
            Build.VERSION::class.java,
            "SDK_INT",
            originalSdk
        )
    }

    @Test
    fun testHasPermission() {
        // when permission is null
        val actual = Utils.hasPermission(application, null)
        Assert.assertFalse("hasPermission must return false when permission is null", actual)

        // when permission not null and checkSelfPermission returns permission granted
        shadowApplication.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val actualWhenPermissionGranted = Utils
            .hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        Assert.assertTrue(
            "hasPermission must return true when permission is granted",
            actualWhenPermissionGranted
        )

        // when permission not null and checkSelfPermission returns permission denied
        shadowApplication.denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val actualWhenPermissionDenied = Utils
            .hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        Assert.assertFalse(
            "hasPermission must return false when permission is denied",
            actualWhenPermissionDenied
        )
    }

    @Test
    fun testInitCTGeofenceApiIfRequired() {
        //when cleverTapApi and settings is null

        mockkStatic(FileUtils::class) {
            every { FileUtils.readFromFile(any(), any()) } returns ""
            every { FileUtils.getCachedFullPath(any(), any()) } returns ""
            every { ctGeofenceAPI.cleverTapApi } returns null

            val actualWhenSettingsAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application)
            Assert.assertFalse(
                "Must be false when cleverTapApi and settings is null",
                actualWhenSettingsAndCTApiIsNull
            )

            // when cleverTapApi is null and settings is not null
            every { FileUtils.readFromFile(any(), any()) } returns settingsJsonString

            val actualWhenSettingsNonNullAndCTApiIsNull =
                Utils.initCTGeofenceApiIfRequired(application)
            Assert.assertFalse(
                "Must be false when cleverTapApi is null and settings is not null",
                actualWhenSettingsNonNullAndCTApiIsNull
            )

            mockkStatic(CleverTapAPI::class) {
                every { CleverTapAPI.getGlobalInstance(any(), any()) } returns cleverTapAPI

                val actualWhenSettingsNonNullAndCTApiNonNull =
                    Utils.initCTGeofenceApiIfRequired(application)
                Assert.assertTrue(
                    "Must be true when cleverTapApi is not null and settings is not null",
                    actualWhenSettingsNonNullAndCTApiNonNull
                )

                // when cleverTapApi is not null
                val actualWhenCTApiNonNull = Utils.initCTGeofenceApiIfRequired(application)
                Assert.assertTrue(
                    "Must be true when cleverTapApi is not null",
                    actualWhenCTApiNonNull
                )
            }
        }
    }

    @Test
    fun testJsonToGeoFenceList() {
        val actualList = Utils.jsonToGeoFenceList(geofence)
        val expectedList = mutableListOf<String?>("310001", "310002")

        MatcherAssert.assertThat<MutableList<String?>?>(
            actualList,
            CoreMatchers.`is`<MutableList<String?>?>(expectedList)
        )

        val actualListWhenJsonArrayIsEmpty = Utils.jsonToGeoFenceList(emptyGeofence)
        val expectedListWhenJsonArrayIsEmpty: MutableList<String?> = ArrayList<String?>()

        MatcherAssert.assertThat<MutableList<String?>?>(
            actualListWhenJsonArrayIsEmpty,
            CoreMatchers.`is`<MutableList<String?>?>(expectedListWhenJsonArrayIsEmpty)
        )

        val actualListWhenJsonIsEmpty = Utils.jsonToGeoFenceList(emptyJson)
        val expectedListWhenJsonIsEmpty: MutableList<String?> = ArrayList<String?>()

        MatcherAssert.assertThat<MutableList<String?>?>(
            actualListWhenJsonIsEmpty,
            CoreMatchers.`is`<MutableList<String?>?>(expectedListWhenJsonIsEmpty)
        )
    }

    @Test
    fun testNotifyLocationUpdates() {
        mockkStatic(com.clevertap.android.sdk.Utils::class) {
            val locationUpdatesListener = mockk<CTLocationUpdatesListener>(relaxed = true)
            every { ctGeofenceAPI.ctLocationUpdatesListener } returns locationUpdatesListener

            val location = mockk<Location>(relaxed = true)
            Utils.notifyLocationUpdates(application, location)

            val runnableSlot = slot<Runnable>()

            verify {
                com.clevertap.android.sdk.Utils.runOnUiThread(capture(runnableSlot))
            }

            runnableSlot.captured.run()
            verify { locationUpdatesListener.onLocationUpdates(any()) }
        }
    }

    @Test
    fun testReadSettingsFromFile() {
        mockkStatic(FileUtils::class) {
            every { FileUtils.getCachedFullPath(any(), any()) } returns ""
            // when settings in file is not blank
            every { FileUtils.readFromFile(any(), any()) } returns settingsJsonString

            val settingsActualWhenNotEmpty = Utils.readSettingsFromFile(application)
            val settingsExpectedWhenNotEmpty = getSettings(settingsJsonObject)

            MatcherAssert.assertThat(
                settingsActualWhenNotEmpty,
                samePropertyValuesAs(settingsExpectedWhenNotEmpty)
            )

            // when settings in file is blank
            every { FileUtils.readFromFile(any(), any()) } returns ""

            val settingsActualWhenEmpty = Utils.readSettingsFromFile(application)
            Assert.assertNull(settingsActualWhenEmpty)
        }
    }

    @Test
    fun testSubArray() {
        val geofenceArray = geofenceArray

        val expectedSubArrayFirst = firstFromGeofenceArray
        val actualSubArrayFirst = Utils.subArray(geofenceArray, 0, 1)

        JSONAssert.assertEquals(expectedSubArrayFirst, actualSubArrayFirst, true)

        val actualSubArrayFull = Utils.subArray(geofenceArray, 0, geofenceArray.length())
        JSONAssert.assertEquals(geofenceArray, actualSubArrayFull, true)

        val expectedSubArrayLast = lastFromGeofenceArray
        val actualSubArrayLast = Utils.subArray(geofenceArray, 1, geofenceArray.length())

        JSONAssert.assertEquals(expectedSubArrayLast, actualSubArrayLast, true)

        Assert.assertThrows<IllegalArgumentException?>(
            "IllegalArgumentException must be thrown when fromIndex is greater than lastIndex",
            IllegalArgumentException::class.java,
            ThrowingRunnable {
                Utils.subArray(
                    geofenceArray,
                    geofenceArray.length(),
                    0
                )
            })
    }

    @Test
    fun testWriteSettingsToFileFailure() {
        mockkStatic(FileUtils::class) {
            every { FileUtils.getCachedDirName(application) } returns ""
            every { FileUtils.writeJsonToFile(any(), any(), any(), any()) } returns false
            // Utils.writeSettingsToFile replaces the id in the settings with the one provided by
            // CTGeofenceAPI. Make them the same so we can check the saved json for equality with the
            // provided one
            every { ctGeofenceAPI.accountId } returns settingsJsonObject.getString(
                CTGeofenceConstants.KEY_ID
            )

            Utils.writeSettingsToFile(application, getSettings(settingsJsonObject))

            verify {
                FileUtils.writeJsonToFile(
                    application,
                    FileUtils.getCachedDirName(application),
                    CTGeofenceConstants.SETTINGS_FILE_NAME,
                    withArg {
                        JSONAssert.assertEquals(settingsJsonObject, it, true)
                    }
                )
            }

            verify {
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new settings to file"
                )
            }
        }
    }

    @Test
    fun testWriteSettingsToFileSuccess() {
        mockkStatic(FileUtils::class) {
            every { FileUtils.getCachedDirName(application) } returns ""
            every { FileUtils.writeJsonToFile(any(), any(), any(), any()) } returns true
            // Utils.writeSettingsToFile replaces the id in the settings with the one provided by
            // CTGeofenceAPI. Make them the same so we can check the saved json for equality with the
            // provided one
            every { ctGeofenceAPI.accountId } returns settingsJsonObject.getString(
                CTGeofenceConstants.KEY_ID
            )

            Utils.writeSettingsToFile(application, getSettings(settingsJsonObject))
            val cachedDirName = FileUtils.getCachedDirName(application)
            verify {
                FileUtils.writeJsonToFile(
                    application,
                    cachedDirName,
                    CTGeofenceConstants.SETTINGS_FILE_NAME,
                    withArg {
                        JSONAssert.assertEquals(settingsJsonObject, it, true)
                    }
                )
            }

            verify {
                logger.debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "New settings successfully written to file"
                )
            }
        }
    }
}
