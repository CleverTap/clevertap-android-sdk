package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.store.preference.InAppStore.Companion.PREFS_DELAYED_INAPP_KEY_CS
import com.clevertap.android.sdk.store.preference.ICTPreference
import com.clevertap.android.sdk.toList
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals

class InAppStoreTest {

    private lateinit var ctPreference: ICTPreference
    private lateinit var cryptHandler: CryptHandler
    private lateinit var inAppStore: InAppStore

    @Before
    fun setUp() {
        ctPreference = mockk(relaxed = true)
        cryptHandler = mockk(relaxed = true)
        inAppStore = InAppStore(ctPreference, cryptHandler)
    }

    @Test
    fun `storeClientSideInApps writes encrypted JSONArray to ctPreference`() {
        // Arrange
        val clientSideInApps = JSONArray("[{\"id\":1},{\"id\":2}]").toList<JSONObject>()
        every { cryptHandler.encrypt(any()) } returns "encryptedString"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeClientSideInApps(clientSideInApps.toList())

        // Assert
        assertEquals(clientSideInApps.toString(), inAppStore.readClientSideInApps().toString())
        verify { ctPreference.writeString(Constants.PREFS_INAPP_KEY_CS, "encryptedString") }
    }

    @Test
    fun `readClientSideInApps returns decrypted JSONArray from ctPreference`() {
        // Arrange
        val csInAppsEncrypted = "encryptedString"
        val decryptedClientSideInApps = JSONArray("[{\"id\":3},{\"id\":4}]").toList<JSONObject>()
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_CS, any()) } returns csInAppsEncrypted
        every { cryptHandler.decrypt(csInAppsEncrypted) } returns decryptedClientSideInApps.toString()

        // Act
        val result = inAppStore.readClientSideInApps()

        // Assert
        assertEquals(decryptedClientSideInApps.toString(), result.toString())
    }

    @Test
    fun `readClientSideInApps returns empty JSONArray when ctPreference returns null encrypted string`() {
        // Arrange
        val csInAppsEncrypted = null
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_CS, any()) } returns csInAppsEncrypted

        // Act
        val result = inAppStore.readClientSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readClientSideInApps returns empty JSONArray when ctPreference does not contain PREFS_INAPP_KEY_CS`() {
        // Arrange
        val csInAppsEncrypted = ""
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_CS, any()) } returns csInAppsEncrypted

        // Act
        val result = inAppStore.readClientSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readClientSideInApps returns empty JSONArray when decryption fails`() {
        // Arrange
        val csInAppsEncrypted = "encryptedString"
        val csInAppsDecrypted = null
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_CS, any()) } returns csInAppsEncrypted
        every { cryptHandler.decrypt(csInAppsEncrypted) } returns csInAppsDecrypted

        // Act
        val result = inAppStore.readClientSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `storeServerSideInAppsMetaData writes JSONArray to ctPreference`() {
        // Arrange
        val serverSideInAppsMetaData = JSONArray("[{\"id\":5},{\"id\":6}]")
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInAppsMetaData(serverSideInAppsMetaData.toList())

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_INAPP_KEY_SS, "[{\"id\":5},{\"id\":6}]") }
    }

    @Test
    fun `storeServerSideInAppsMetaData updates cache for immediate read without hitting preference`() {
        // Arrange
        val serverSideInAppsMetaData = JSONArray("[{\"id\":5},{\"id\":6}]").toList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInAppsMetaData(serverSideInAppsMetaData)
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert - Should return cached data without reading from preference
        assertEquals(serverSideInAppsMetaData.toString(), result.toString())
        // Verify preference read is NOT called since cache is populated by store
        verify(exactly = 0) { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) }
    }

    @Test
    fun `storeEvaluatedServerSideInAppIds writes JSONObject to ctPreference`() {
        // Arrange
        val evaluatedServerSideInAppIds = JSONArray().apply {
            put(100000)
            put(1000002)
            put(100000001)
        }
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeEvaluatedServerSideInAppIds(evaluatedServerSideInAppIds)

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, "[100000,1000002,100000001]") }
    }

    @Test
    fun `storeSuppressedClientSideInAppIds writes JSONObject to ctPreference`() {

        val evaluatedServerSideInAppIds = JSONArray().apply {
            put(
                JSONObject().apply {
                    put(Constants.NOTIFICATION_ID_TAG, "id1")
                }
            )
            put(
                JSONObject().apply {
                    put(Constants.NOTIFICATION_ID_TAG, "id2")
                }
            )
        }
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeSuppressedClientSideInAppIds(evaluatedServerSideInAppIds)

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, evaluatedServerSideInAppIds.toString()) }
    }

    @Test
    fun `readServerSideInAppsMetaData returns JSONArray from ctPreference`() {
        // Arrange
        val ssInAppsMetaData = JSONArray("[{\"id\":7},{\"id\":8}]").toList<JSONObject>()
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData.toString()

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(ssInAppsMetaData.toString(), result.toString())
    }

    @Test
    fun `readServerSideInAppsMetaData returns empty JSONArray when ctPreference returns null`() {
        // Arrange
        val ssInAppsMetaData = null
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readServerSideInAppsMetaData returns empty JSONArray when ctPreference does not contain PREFS_INAPP_KEY_SS`() {
        // Arrange
        val ssInAppsMetaData = ""
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readServerSideInAppsMetaData returns JSONArray from ctPreference when cache is empty`() {
        // Arrange
        val ssInAppsMetaData = JSONArray("[{\"id\":7},{\"id\":8}]").toList<JSONObject>()
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData.toString()

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(ssInAppsMetaData.toString(), result.toString())
        verify(exactly = 1) { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) }
    }

    @Test
    fun `readServerSideInAppsMetaData uses cache on subsequent calls`() {
        // Arrange
        val ssInAppsMetaData = "[{\"id\":7},{\"id\":8}]"
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData

        // Act - First call reads from preference and caches
        val result1 = inAppStore.readServerSideInAppsMetaData()
        // Second call should use cache
        val result2 = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(result1.toString(), result2.toString())
        // Verify preference is read only once (cache is used for second call)
        verify(exactly = 1) { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) }
    }

    @Test
    fun `readServerSideInAppsMetaData returns empty list when JSON parsing fails`() {
        // Arrange
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns "invalid json {"

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(emptyList<JSONObject>(), result)
    }


    @Test
    fun `storeServerSideInApps writes encrypted JSONArray to ctPreference`() {
        // Arrange
        val serverSideInApps = JSONArray("[{\"id\":9},{\"id\":10}]").toList<JSONObject>()
        every { cryptHandler.encrypt(any()) } returns "encryptedString"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInApps(serverSideInApps.toList())

        // Assert
        assertEquals(serverSideInApps.toString(), inAppStore.readServerSideInApps().toString())
        verify { ctPreference.writeString(Constants.INAPP_KEY, "encryptedString") }
    }

    @Test
    fun `readServerSideInApps returns decrypted JSONArray from ctPreference`() {
        // Arrange
        val ssEncryptedInApps = "encryptedString"
        val decryptedServerSideInApps = JSONArray("[{\"id\":11},{\"id\":12}]").toList<JSONObject>()
        every { ctPreference.readString(any(), any()) } returns ssEncryptedInApps
        every { cryptHandler.decrypt(any()) } returns decryptedServerSideInApps.toString()

        // Act
        val result = inAppStore.readServerSideInApps()

        // Assert
        assertEquals(decryptedServerSideInApps.toString(), result.toString())
    }

    @Test
    fun `readServerSideInApps returns empty JSONArray when ctPreference returns null encrypted string`() {
        // Arrange
        val ssEncryptedInApps = null
        every { ctPreference.readString(Constants.INAPP_KEY, any()) } returns ssEncryptedInApps

        // Act
        val result = inAppStore.readServerSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readServerSideInApps returns empty JSONArray when ctPreference does not contain INAPP_KEY`() {
        // Arrange
        val ssEncryptedInApps = ""
        every { ctPreference.readString(Constants.INAPP_KEY, any()) } returns ssEncryptedInApps

        // Act
        val result = inAppStore.readServerSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `readServerSideInApps returns empty JSONArray when decryption fails`() {
        // Arrange
        val ssEncryptedInApps = "encryptedString"
        val ssDecryptedInApps = null
        every { ctPreference.readString(Constants.INAPP_KEY, any()) } returns ssEncryptedInApps
        every { cryptHandler.decrypt(ssEncryptedInApps) } returns ssDecryptedInApps

        // Act
        val result = inAppStore.readServerSideInApps()

        // Assert
        assertEquals(JSONArray().toList(), result)
    }

    @Test
    fun `mode change to CLIENT_SIDE_MODE removes server-side In-App messages metadata`() {
        // Arrange
        inAppStore.mode = InAppStore.CLIENT_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify { ctPreference.remove(InAppStore.PREFS_INACTION_INAPP_KEY_SS) }
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        assertEquals(InAppStore.CLIENT_SIDE_MODE, inAppStore.mode)
    }

    @Test
    fun `mode change to SERVER_SIDE_MODE removes client-side In-App messages`() {
        // Arrange
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        assertEquals(InAppStore.SERVER_SIDE_MODE, inAppStore.mode)
    }

    @Test
    fun `mode change to NO_MODE removes both client-side and server-side In-App messages`() {
        // Arrange
        inAppStore.mode = InAppStore.NO_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
        verify { ctPreference.remove(InAppStore.PREFS_INACTION_INAPP_KEY_SS) }
        assertEquals(InAppStore.NO_MODE, inAppStore.mode)
    }

    @Test
    fun `when mode is same remove for client-side and server-side does not invoke`() {
        // Act
        // first time mode set
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // when same mode set second time
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Assert
        // gets called when first time mode set
        verify(exactly = 1) { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        // both times doesn't get invoked
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        assertEquals(InAppStore.SERVER_SIDE_MODE, inAppStore.mode)
    }

    @Test
    fun `onChangeUser updates preference name in ctPreference`() {
        // Arrange
        val newPrefName = "${Constants.INAPP_KEY}:deviceId123:accountId456"
        every { ctPreference.changePreferenceName(any()) } just Runs

        // Act
        inAppStore.onChangeUser("deviceId123", "accountId456")

        // Assert
        verify { ctPreference.changePreferenceName(newPrefName) }
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns data correctly from current format`() {
        // Arrange
        val savedData = "[100000,1000002,100000001]"
        every { ctPreference.readString(any(), any()) } returns savedData
        val expectedData = JSONArray(savedData)

        // Act
        val data: JSONArray = inAppStore.readEvaluatedServerSideInAppIds()

        assertEquals(expectedData.toString(), data.toString())
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns data correctly from older format pre 70501 SDK version`() {
        // Arrange - legacy data
        val savedData = """
            {"profile": [100000, 1000002, 100000001],"raised": [200000, 2000002, 200000001]}
        """.trimIndent()
        every { ctPreference.readString(any(), any()) } returns savedData
        val expectedData = JSONArray().apply {
            put(200000)
            put(2000002)
            put(200000001)
            put(100000)
            put(1000002)
            put(100000001)
        }

        // Act
        val data: JSONArray = inAppStore.readEvaluatedServerSideInAppIds()

        assertEquals(expectedData.toString(), data.toString())
    }

    @Test
    fun `readSuppressedClientSideInAppIds returns data correctly from current format`() {
        // Arrange
        val savedData = """
            [{"wzrk_id":"id1"}, {"wzrk_id":"id2"}]
        """.trimIndent()
        every { ctPreference.readString(any(), any()) } returns savedData

        val expectedData = JSONArray().apply {
            put(JSONObject().apply {
                put(Constants.NOTIFICATION_ID_TAG, "id1")
            })
            put(JSONObject().apply {
                put(Constants.NOTIFICATION_ID_TAG, "id2")
            })
        }

        // Act
        val data: JSONArray = inAppStore.readSuppressedClientSideInAppIds()

        assertEquals(expectedData.toString(), data.toString())
    }

    @Test
    fun `readSuppressedClientSideInAppIds returns data correctly from older format pre 70501 SDK version`() {
        // Arrange
        val savedData = """
            {"profile":[{"wzrk_id":"id1"}, {"wzrk_id":"id2"}],"raised":[{"wzrk_id":"id3"}]}
        """.trimIndent()
        every { ctPreference.readString(any(), any()) } returns savedData

        val expectedData = JSONArray().apply {
            put(JSONObject().apply {
                put(Constants.NOTIFICATION_ID_TAG, "id3")
            })
            put(JSONObject().apply {
                put(Constants.NOTIFICATION_ID_TAG, "id1")
            })
            put(JSONObject().apply {
                put(Constants.NOTIFICATION_ID_TAG, "id2")
            })
        }

        // Act
        val data: JSONArray = inAppStore.readSuppressedClientSideInAppIds()

        assertEquals(expectedData.toString(), data.toString())
    }

    // ==================== DELAYED CLIENT SIDE IN-APPS TESTS ====================

    @Test
    fun `storeClientSideDelayedInApps writes encrypted JSONArray to ctPreference`() {
        // Arrange
        val clientSideDelayedInApps = JSONArray("[{\"id\":1},{\"id\":2}]").toList<JSONObject>()
        every { cryptHandler.encrypt(any()) } returns "encryptedString"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeClientSideDelayedInApps(clientSideDelayedInApps.toList())

        // Assert
        assertEquals(clientSideDelayedInApps.toString(), inAppStore.readClientSideDelayedInApps().toString())
        verify { ctPreference.writeString(PREFS_DELAYED_INAPP_KEY_CS, "encryptedString") }
    }

    @Test
    fun `readClientSideDelayedInApps returns decrypted JSONArray from ctPreference`() {
        // Arrange
        val csDelayedInAppsEncrypted = "encryptedString"
        val decryptedClientSideDelayedInApps = JSONArray("[{\"id\":3},{\"id\":4}]").toList<JSONObject>()
        every { ctPreference.readString(PREFS_DELAYED_INAPP_KEY_CS, any()) } returns csDelayedInAppsEncrypted
        every { cryptHandler.decrypt(csDelayedInAppsEncrypted) } returns decryptedClientSideDelayedInApps.toString()

        // Act
        val result = inAppStore.readClientSideDelayedInApps()

        // Assert
        assertEquals(decryptedClientSideDelayedInApps.toString(), result.toString())
    }

    @Test
    fun `readClientSideDelayedInApps returns empty JSONArray when ctPreference returns null encrypted string`() {
        // Arrange
        val csDelayedInAppsEncrypted = null
        every { ctPreference.readString(PREFS_DELAYED_INAPP_KEY_CS, any()) } returns csDelayedInAppsEncrypted

        // Act
        val result = inAppStore.readClientSideDelayedInApps()

        // Assert
        assertEquals(JSONArray().toString(), result.toString())
    }

    @Test
    fun `readClientSideDelayedInApps returns empty JSONArray when ctPreference does not contain PREFS_DELAYED_INAPP_KEY_CS`() {
        // Arrange
        val csDelayedInAppsEncrypted = ""
        every { ctPreference.readString(PREFS_DELAYED_INAPP_KEY_CS, any()) } returns csDelayedInAppsEncrypted

        // Act
        val result = inAppStore.readClientSideDelayedInApps()

        // Assert
        assertEquals(JSONArray().toString(), result.toString())
    }

    @Test
    fun `readClientSideDelayedInApps returns empty JSONArray when decryption fails`() {
        // Arrange
        val csDelayedInAppsEncrypted = "encryptedString"
        val csDelayedInAppsDecrypted = null
        every { ctPreference.readString(PREFS_DELAYED_INAPP_KEY_CS, any()) } returns csDelayedInAppsEncrypted
        every { cryptHandler.decrypt(csDelayedInAppsEncrypted) } returns csDelayedInAppsDecrypted

        // Act
        val result = inAppStore.readClientSideDelayedInApps()

        // Assert
        assertEquals(JSONArray().toString(), result.toString())
    }

    @Test
    fun `mode change to SERVER_SIDE_MODE also removes delayed client-side in-apps`() {
        // Arrange - Store delayed in-apps first
        val delayedInApps = JSONArray().put(JSONObject().put("ti", "delayed1"))
        every { cryptHandler.encrypt(any()) } returns "encrypted"
        every { ctPreference.writeString(any(), any()) } just Runs
        inAppStore.storeClientSideDelayedInApps(delayedInApps.toList())

        // Act
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
    }

    @Test
    fun `mode change to NO_MODE also removes delayed client-side in-apps`() {
        // Arrange - Store delayed in-apps first
        val delayedInApps = JSONArray().put(JSONObject().put("ti", "delayed1"))
        every { cryptHandler.encrypt(any()) } returns "encrypted"
        every { ctPreference.writeString(any(), any()) } just Runs
        inAppStore.storeClientSideDelayedInApps(delayedInApps.toList())

        // Act
        inAppStore.mode = InAppStore.NO_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
    }

    // ==================== SERVER-SIDE IN-ACTION METADATA TESTS ====================

    @Test
    fun `storeServerSideInActionMetaData writes JSONArray to ctPreference`() {
        // Arrange
        val serverSideInActionMetaData = JSONArray("[{\"id\":100,\"inactionDuration\":60},{\"id\":101,\"inactionDuration\":120}]")
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInActionMetaData(serverSideInActionMetaData.toList<JSONObject>())

        // Assert
        verify { ctPreference.writeString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, serverSideInActionMetaData.toString()) }
    }

    @Test
    fun `storeServerSideInActionMetaData writes empty JSONArray to ctPreference`() {
        // Arrange
        val serverSideInActionMetaData = emptyList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInActionMetaData(serverSideInActionMetaData)

        // Assert
        verify { ctPreference.writeString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, "[]") }
    }

    @Test
    fun `readServerSideInActionMetaData returns JSONArray from ctPreference`() {
        // Arrange
        val ssInActionMetaData = JSONArray("[{\"id\":200,\"inactionDuration\":60},{\"id\":201,\"inactionDuration\":120}]").toList<JSONObject>()
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns ssInActionMetaData.toString()

        // Act
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(ssInActionMetaData.toString(), result.toString())
    }

    @Test
    fun `readServerSideInActionMetaData returns empty list when ctPreference returns null`() {
        // Arrange
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns null

        // Act
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `readServerSideInActionMetaData returns empty list when ctPreference returns empty string`() {
        // Arrange
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns ""

        // Act
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `readServerSideInActionMetaData returns empty list when ctPreference returns blank string`() {
        // Arrange
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns "   "

        // Act
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `readServerSideInActionMetaData returns empty list when JSON parsing fails`() {
        // Arrange
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns "invalid json {"

        // Act
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `readServerSideInActionMetaData uses cache on subsequent calls`() {
        // Arrange
        val ssInActionMetaData = "[{\"id\":300,\"inactionDuration\":60}]"
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns ssInActionMetaData

        // Act - First call reads from preference
        val result1 = inAppStore.readServerSideInActionMetaData()
        // Second call should use cache
        val result2 = inAppStore.readServerSideInActionMetaData()

        // Assert
        assertEquals(result1.toString(), result2.toString())
        // Verify preference is read only once (cache is used)
        verify(exactly = 1) { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) }
    }

    @Test
    fun `storeServerSideInActionMetaData updates cache for immediate read`() {
        // Arrange
        val serverSideInActionMetaData = JSONArray("[{\"id\":400,\"inactionDuration\":60}]").toList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInActionMetaData(serverSideInActionMetaData)
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert - Should return cached data without reading from preference
        assertEquals(serverSideInActionMetaData.toString(), result.toString())
        // Verify preference read is not called since cache is populated by store
        verify(exactly = 0) { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) }
    }
    @Test
    fun `mode change to CLIENT_SIDE_MODE removes server-side in-action metadata`() {
        // Arrange & Act
        inAppStore.mode = InAppStore.CLIENT_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify { ctPreference.remove(InAppStore.PREFS_INACTION_INAPP_KEY_SS) }
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify(exactly = 0) { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
    }

    @Test
    fun `mode change to SERVER_SIDE_MODE does not remove server-side in-action metadata`() {
        // Arrange & Act
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify(exactly = 0) { ctPreference.remove(InAppStore.PREFS_INACTION_INAPP_KEY_SS) }
    }

    @Test
    fun `mode change to NO_MODE removes server-side in-action metadata`() {
        // Arrange & Act
        inAppStore.mode = InAppStore.NO_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify { ctPreference.remove(PREFS_DELAYED_INAPP_KEY_CS) }
        verify { ctPreference.remove(InAppStore.PREFS_INACTION_INAPP_KEY_SS) }
    }

    @Test
    fun `mode change to CLIENT_SIDE_MODE clears in-action cache`() {
        // Arrange - Store in-action data first
        val inActionData = JSONArray("[{\"id\":500,\"inactionDuration\":60}]").toList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs
        inAppStore.storeServerSideInActionMetaData(inActionData)

        // Verify cache is populated
        assertEquals(inActionData.toString(), inAppStore.readServerSideInActionMetaData().toString())

        // Act - Change mode to CS
        inAppStore.mode = InAppStore.CLIENT_SIDE_MODE

        // Now read should go to preference (cache cleared)
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns "[]"
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert - Cache was cleared, so it should read from preference
        verify { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) }
        assertEquals(emptyList<JSONObject>(), result)
    }

    @Test
    fun `mode change to NO_MODE clears in-action cache`() {
        // Arrange - Store in-action data first
        val inActionData = JSONArray("[{\"id\":600,\"inactionDuration\":60}]").toList<JSONObject>()
        every { ctPreference.writeString(any(), any()) } just Runs
        inAppStore.storeServerSideInActionMetaData(inActionData)

        // Verify cache is populated
        assertEquals(inActionData.toString(), inAppStore.readServerSideInActionMetaData().toString())

        // Act - Change mode to NO_MODE
        inAppStore.mode = InAppStore.NO_MODE

        // Now read should go to preference (cache cleared)
        every { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) } returns "[]"
        val result = inAppStore.readServerSideInActionMetaData()

        // Assert - Cache was cleared, so it should read from preference
        verify { ctPreference.readString(InAppStore.PREFS_INACTION_INAPP_KEY_SS, any()) }
        assertEquals(emptyList<JSONObject>(), result)
    }
}