package com.clevertap.android.geofence

import android.Manifest
import android.content.Context
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
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.beans.SamePropertyValuesAs
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.function.ThrowingRunnable
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import org.robolectric.util.ReflectionHelpers
import org.skyscreamer.jsonassert.JSONAssert

class UtilsTest : BaseTestCase() {
    @Mock
    lateinit var cleverTapAPI: CleverTapAPI

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    @Mock
    lateinit var logger: Logger

    private lateinit var shadowApplication: ShadowApplication

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
    }

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)

        shadowApplication = Shadows.shadowOf(application)
        ctGeofenceAPIMockedStatic = Mockito.mockStatic<CTGeofenceAPI?>(CTGeofenceAPI::class.java)

        Mockito.`when`<CTGeofenceAPI?>(CTGeofenceAPI.getInstance(application))
            .thenReturn(ctGeofenceAPI)
        Mockito.`when`<Logger?>(CTGeofenceAPI.getLogger()).thenReturn(logger)
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
        val shadowApplication = Shadows.shadowOf(application)

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

        Mockito.mockStatic<FileUtils>(FileUtils::class.java).use { fileUtilsMockedStatic ->
            fileUtilsMockedStatic.`when`<Any?>(MockedStatic.Verification {
                FileUtils.readFromFile(
                    ArgumentMatchers.any<Context?>(
                        Context::class.java
                    ), ArgumentMatchers.anyString()
                )
            }).thenReturn("")
            fileUtilsMockedStatic.`when`<Any?>(MockedStatic.Verification {
                FileUtils.getCachedFullPath(
                    ArgumentMatchers.any<Context?>(Context::class.java),
                    ArgumentMatchers.anyString()
                )
            }).thenReturn("")

            val actualWhenSettingsAndCTApiIsNull = Utils.initCTGeofenceApiIfRequired(application)
            Assert.assertFalse(
                "Must be false when cleverTapApi and settings is null",
                actualWhenSettingsAndCTApiIsNull
            )

            // when cleverTapApi is null and settings is not null
            fileUtilsMockedStatic.`when`<Any?>(MockedStatic.Verification {
                FileUtils.readFromFile(
                    ArgumentMatchers.any<Context?>(Context::class.java),
                    ArgumentMatchers.anyString()
                )
            }).thenReturn(settingsJsonString)
            val actualWhenSettingsNonNullAndCTApiIsNull =
                Utils.initCTGeofenceApiIfRequired(application)
            Assert.assertFalse(
                "Must be false when cleverTapApi is null and settings is not null",
                actualWhenSettingsNonNullAndCTApiIsNull
            )
            Mockito.mockStatic<CleverTapAPI?>(CleverTapAPI::class.java)
                .use { clevertapApiMockedStatic ->
                    clevertapApiMockedStatic.`when`<Any?>(MockedStatic.Verification {
                        CleverTapAPI.getGlobalInstance(
                            ArgumentMatchers.any<Context?>(
                                Context::class.java
                            ), ArgumentMatchers.anyString()
                        )
                    })
                        .thenReturn(cleverTapAPI)
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
        Mockito.mockStatic<com.clevertap.android.sdk.Utils>(
            com.clevertap.android.sdk.Utils::class.java
        ).use { coreUtilsMockedStatic ->
            val locationUpdatesListener =
                Mockito.mock<CTLocationUpdatesListener?>(CTLocationUpdatesListener::class.java)
            Mockito.`when`<CTLocationUpdatesListener?>(ctGeofenceAPI.ctLocationUpdatesListener)
                .thenReturn(locationUpdatesListener)

            Utils.notifyLocationUpdates(
                application, Mockito.mock<Location?>(Location::class.java)
            )

            val runnableArgumentCaptor =
                ArgumentCaptor.forClass<Runnable?, Runnable?>(Runnable::class.java)

            coreUtilsMockedStatic.verify(
                MockedStatic.Verification {
                    com.clevertap.android.sdk.Utils.runOnUiThread(
                        runnableArgumentCaptor.capture()
                    )
                })
            runnableArgumentCaptor.getValue().run()
            Mockito.verify<CTLocationUpdatesListener?>(locationUpdatesListener).onLocationUpdates(
                ArgumentMatchers.any<Location?>(
                    Location::class.java
                )
            )
        }
    }

    @Test
    fun testReadSettingsFromFile() {
        Mockito.mockStatic<FileUtils?>(FileUtils::class.java).use { fileUtilsMockedStatic ->
            fileUtilsMockedStatic.`when`<Any?>(MockedStatic.Verification {
                FileUtils.getCachedFullPath(
                    ArgumentMatchers.any<Context?>(Context::class.java),
                    ArgumentMatchers.anyString()
                )
            }).thenReturn("")
            // when settings in file is not blank
            fileUtilsMockedStatic.`when`<Any?>(MockedStatic.Verification {
                FileUtils.readFromFile(
                    ArgumentMatchers.any<Context?>(Context::class.java),
                    ArgumentMatchers.anyString()
                )
            }).thenReturn(settingsJsonString)

            val settingsActualWhenNotEmpty = Utils.readSettingsFromFile(application)
            val settingsExpectedWhenNotEmpty = getSettings(settingsJsonObject)

            MatcherAssert.assertThat<CTGeofenceSettings?>(
                settingsActualWhenNotEmpty,
                SamePropertyValuesAs.samePropertyValuesAs<CTGeofenceSettings?>(
                    settingsExpectedWhenNotEmpty
                )
            )

            // when settings in file is blank
            Mockito.`when`<String?>(
                FileUtils.readFromFile(
                    ArgumentMatchers.any<Context?>(Context::class.java),
                    ArgumentMatchers.anyString()
                )
            ).thenReturn("")

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
        Mockito.mockStatic<FileUtils?>(FileUtils::class.java).use { fileUtilsMockedStatic ->
            Mockito.`when`<String?>(FileUtils.getCachedDirName(application)).thenReturn("")
            Mockito.`when`<Boolean?>(
                FileUtils.writeJsonToFile(
                    ArgumentMatchers.any<Context?>(),
                    ArgumentMatchers.any<String?>(),
                    ArgumentMatchers.any<String?>(),
                    ArgumentMatchers.any<JSONObject?>()
                )
            ).thenReturn(false)

            Utils.writeSettingsToFile(
                application,
                getSettings(settingsJsonObject)
            )
            fileUtilsMockedStatic.verify(
                MockedStatic.Verification {
                    FileUtils.writeJsonToFile(
                        application, FileUtils.getCachedDirName(application),
                        CTGeofenceConstants.SETTINGS_FILE_NAME, settingsJsonObject
                    )
                })
            Mockito.verify<Logger?>(logger).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to write new settings to file"
            )
        }
    }

    @Test
    fun testWriteSettingsToFileSuccess() {
        Mockito.mockStatic<FileUtils?>(FileUtils::class.java).use { fileUtilsMockedStatic ->
            Mockito.`when`<String?>(FileUtils.getCachedDirName(application)).thenReturn("")
            Mockito.`when`<Boolean?>(
                FileUtils.writeJsonToFile(
                    ArgumentMatchers.any<Context?>(),
                    ArgumentMatchers.any<String?>(),
                    ArgumentMatchers.any<String?>(),
                    ArgumentMatchers.any<JSONObject?>()
                )
            ).thenReturn(true)

            Utils.writeSettingsToFile(
                application,
                getSettings(settingsJsonObject)
            )
            fileUtilsMockedStatic.verify(
                MockedStatic.Verification {
                    FileUtils.writeJsonToFile(
                        application, FileUtils.getCachedDirName(application),
                        CTGeofenceConstants.SETTINGS_FILE_NAME, settingsJsonObject
                    )
                })
            Mockito.verify<Logger?>(logger).debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "New settings successfully written to file"
            )
        }
    }
}
