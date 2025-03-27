package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.evaluation.EventType
import com.clevertap.android.sdk.store.preference.ICTPreference
import io.mockk.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT
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
        val clientSideInApps = JSONArray("[{\"id\":1},{\"id\":2}]")
        every { cryptHandler.encrypt(any()) } returns "encryptedString"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeClientSideInApps(clientSideInApps)

        // Assert
        assertEquals(clientSideInApps.toString(), inAppStore.readClientSideInApps().toString())
        verify { ctPreference.writeString(Constants.PREFS_INAPP_KEY_CS, "encryptedString") }
    }

    @Test
    fun `readClientSideInApps returns decrypted JSONArray from ctPreference`() {
        // Arrange
        val csInAppsEncrypted = "encryptedString"
        val decryptedClientSideInApps = JSONArray("[{\"id\":3},{\"id\":4}]")
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
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `readClientSideInApps returns empty JSONArray when ctPreference does not contain PREFS_INAPP_KEY_CS`() {
        // Arrange
        val csInAppsEncrypted = ""
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_CS, any()) } returns csInAppsEncrypted

        // Act
        val result = inAppStore.readClientSideInApps()

        // Assert
        assertEquals(JSONArray(), result)
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
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `storeServerSideInAppsMetaData writes JSONArray to ctPreference`() {
        // Arrange
        val serverSideInAppsMetaData = JSONArray("[{\"id\":5},{\"id\":6}]")
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInAppsMetaData(serverSideInAppsMetaData)

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_INAPP_KEY_SS, "[{\"id\":5},{\"id\":6}]") }
    }

    @Test
    fun `storeEvaluatedServerSideInAppIds writes JSONObject to ctPreference`() {
        // Arrange
        val evaluatedServerSideInAppIds = JSONObject()
            .put(EventType.PROFILE.key, listOf(100000,1000002,100000001))
            .put(EventType.RAISED.key, listOf(200000,2000002,200000001))
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeEvaluatedServerSideInAppIds(evaluatedServerSideInAppIds)

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, """{"profile":"[100000, 1000002, 100000001]","raised":"[200000, 2000002, 200000001]"}""") }
    }

    @Test
    fun `storeSuppressedClientSideInAppIds writes JSONObject to ctPreference`() {
        // Arrange
        val evaluatedServerSideInAppIds = JSONObject()
            .put(EventType.PROFILE.key, listOf(mapOf(Constants.NOTIFICATION_ID_TAG to "id1"), mapOf(Constants.NOTIFICATION_ID_TAG to "id2")))
            .put(EventType.RAISED.key, listOf(mapOf(Constants.NOTIFICATION_ID_TAG to "id2")))
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeSuppressedClientSideInAppIds(evaluatedServerSideInAppIds)

        // Assert
        verify { ctPreference.writeString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, """{"profile":"[{wzrk_id=id1}, {wzrk_id=id2}]","raised":"[{wzrk_id=id2}]"}""") }
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns JSONObject from ctPreference when saved value`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = """{"profile":"[100000, 1000002, 100000001]","raised":"[200000, 2000002, 200000001]"}"""
        every { ctPreference.readString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readEvaluatedServerSideInAppIds()

        // Assert
        assertEquals(evaluatedServerSideInAppIdsString, result.toString())
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns empty JSONObject from ctPreference when saved value is null`()
    {
        // Arrange
        val evaluatedServerSideInAppIdsString = null
        every { ctPreference.readString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readEvaluatedServerSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject(), result, STRICT)
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns empty JSONObject from ctPreference when saved value is empty`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = ""
        every { ctPreference.readString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readEvaluatedServerSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject(), result, STRICT)
    }

    @Test
    fun `readEvaluatedServerSideInAppIds returns populated JSONObject from ctPreference when saved value is legacy JSoNArray`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = "[100000, 1000002, 100000001]"
        every { ctPreference.readString(Constants.PREFS_EVALUATED_INAPP_KEY_SS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readEvaluatedServerSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject().put(EventType.RAISED.key, JSONArray("[100000, 1000002, 100000001]")), result, STRICT)
    }


    @Test
    fun `readSuppressedClientSideInAppIds() returns JSONObject from ctPreference when saved value`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = """{"profile":"[{wzrk_id=id1}, {wzrk_id=id2}]","raised":"[{wzrk_id=id2}]"}"""
        every { ctPreference.readString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readSuppressedClientSideInAppIds()

        // Assert
        assertEquals(evaluatedServerSideInAppIdsString, result.toString())
    }

    @Test
    fun `readSuppressedClientSideInAppIds() returns empty JSONObject from ctPreference when saved value is null`()
    {
        // Arrange
        val evaluatedServerSideInAppIdsString = null
        every { ctPreference.readString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readSuppressedClientSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject(), result, STRICT)
    }

    @Test
    fun `readSuppressedClientSideInAppIds() returns empty JSONObject from ctPreference when saved value is empty`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = ""
        every { ctPreference.readString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readSuppressedClientSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject(), result, STRICT)
    }

    @Test
    fun `readSuppressedClientSideInAppIds() returns populated JSONObject from ctPreference when saved value is legacy JSoNArray`() {
        // Arrange
        val evaluatedServerSideInAppIdsString = "[{wzrk_id=id1}, {wzrk_id=id2}]"
        every { ctPreference.readString(Constants.PREFS_SUPPRESSED_INAPP_KEY_CS, any()) } returns evaluatedServerSideInAppIdsString

        // Act
        val result = inAppStore.readSuppressedClientSideInAppIds()

        // Assert
        JSONAssert.assertEquals(JSONObject().put(EventType.RAISED.key, JSONArray("[{wzrk_id=id1}, {wzrk_id=id2}]")), result, STRICT)
    }

    @Test
    fun `readServerSideInAppsMetaData returns JSONArray from ctPreference`() {
        // Arrange
        val ssInAppsMetaData = JSONArray("[{\"id\":7},{\"id\":8}]")
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
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `readServerSideInAppsMetaData returns empty JSONArray when ctPreference does not contain PREFS_INAPP_KEY_SS`() {
        // Arrange
        val ssInAppsMetaData = ""
        every { ctPreference.readString(Constants.PREFS_INAPP_KEY_SS, any()) } returns ssInAppsMetaData

        // Act
        val result = inAppStore.readServerSideInAppsMetaData()

        // Assert
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `storeServerSideInApps writes encrypted JSONArray to ctPreference`() {
        // Arrange
        val serverSideInApps = JSONArray("[{\"id\":9},{\"id\":10}]")
        every { cryptHandler.encrypt(any()) } returns "encryptedString"
        every { ctPreference.writeString(any(), any()) } just Runs

        // Act
        inAppStore.storeServerSideInApps(serverSideInApps)

        // Assert
        assertEquals(serverSideInApps.toString(), inAppStore.readServerSideInApps().toString())
        verify { ctPreference.writeString(Constants.INAPP_KEY, "encryptedString") }
    }

    @Test
    fun `readServerSideInApps returns decrypted JSONArray from ctPreference`() {
        // Arrange
        val ssEncryptedInApps = "encryptedString"
        val decryptedServerSideInApps = JSONArray("[{\"id\":11},{\"id\":12}]")
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
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `readServerSideInApps returns empty JSONArray when ctPreference does not contain INAPP_KEY`() {
        // Arrange
        val ssEncryptedInApps = ""
        every { ctPreference.readString(Constants.INAPP_KEY, any()) } returns ssEncryptedInApps

        // Act
        val result = inAppStore.readServerSideInApps()

        // Assert
        assertEquals(JSONArray(), result)
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
        assertEquals(JSONArray(), result)
    }

    @Test
    fun `mode change to CLIENT_SIDE_MODE removes server-side In-App messages metadata`() {
        // Arrange
        inAppStore.mode = InAppStore.CLIENT_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_SS) }
        verify(exactly = 0) { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
        assertEquals(InAppStore.CLIENT_SIDE_MODE, inAppStore.mode)
    }

    @Test
    fun `mode change to SERVER_SIDE_MODE removes client-side In-App messages`() {
        // Arrange
        inAppStore.mode = InAppStore.SERVER_SIDE_MODE

        // Assert
        verify { ctPreference.remove(Constants.PREFS_INAPP_KEY_CS) }
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
}