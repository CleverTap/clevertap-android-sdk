package com.clevertap.android.sdk.login

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class LoginInfoProviderTest : BaseTestCase() {

    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var cryptHandler: CryptHandler
    private lateinit var loginInfoProvider: LoginInfoProvider

    override fun setUp() {
        super.setUp()
        coreMetaData = CoreMetaData()
        defConfig = CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region")
        deviceInfo = mockk(relaxed = true)
        cryptHandler = mockk(relaxed = true)
        loginInfoProvider = spyk(
            LoginInfoProvider(
                appCtx,
                defConfig,
                cryptHandler
            )
        )
    }

    @Test
    fun `cacheGUIDForIdentifier saves all values when all keys are correct`() {
        val guid = "__789"
        val key = "Email"
        val identifier = "abc@gmail.com"
        val initialGuids = JSONObject().apply { put("Phone_id1", "__1234567") }

        every { cryptHandler.encrypt(any(), any(), CryptHandler.EncryptionAlgorithm.AES_GCM) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        verify { loginInfoProvider.setCachedGUIDsAndLength("dummy_encrypted", 2) }
    }

    @Test
    fun `cacheGUIDForIdentifier saves value without key when key is empty`() {
        val guid = "__1234567"
        val key = ""
        val identifier = "abc@gmail.com"
        val initialGuids = JSONObject().apply { put("Phone_id1", "__1234567") }

        every { cryptHandler.encrypt(any(), any(), CryptHandler.EncryptionAlgorithm.AES_GCM) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        verify { loginInfoProvider.setCachedGUIDsAndLength("dummy_encrypted", 2) }
    }

    @Test
    fun `cacheGUIDForIdentifier saves value when identifier is empty`() {
        val guid = "__1234567"
        val key = "Email"
        val identifier = ""
        val initialGuids = JSONObject().apply { put("Phone_id1", "__1234567") }

        every { cryptHandler.encrypt(any(), any(), CryptHandler.EncryptionAlgorithm.AES_GCM) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        verify { loginInfoProvider.setCachedGUIDsAndLength("dummy_encrypted", 2) }
    }

    @Test
    fun `cacheGUIDForIdentifier does not save value when GUID is empty`() {
        val guid = ""
        val key = "Email"
        val identifier = "abc@gmail.com"
        val initialGuids = JSONObject().apply { put("Phone_id1", "__1234567") }

        every { cryptHandler.encrypt(any(), any(), CryptHandler.EncryptionAlgorithm.AES_GCM) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        verify(exactly = 0) { loginInfoProvider.setCachedGUIDsAndLength("dummy_encrypted", 2) }
    }

    @Test
    fun `cacheGUIDForIdentifier saves plain text when encryption fails`() {
        val guid = "__1234567"
        val key = "email"
        val identifier = "abc@gmail.com"
        val cryptedKey = "${key}_$identifier"
        val initialGuids = JSONObject().apply { put("Phone_id1", "__1234567") }
        val finalGuids = JSONObject().apply {
            put("Phone_id1", "__1234567")
            put(cryptedKey, guid)
        }

        every { cryptHandler.encrypt(any(), any(), CryptHandler.EncryptionAlgorithm.AES_GCM) } returns null
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.cacheGUIDForIdentifier(guid, key, identifier)

        verify { cryptHandler.updateMigrationFailureCount(false) }
        verify { loginInfoProvider.setCachedGUIDsAndLength(finalGuids.toString(), 2) }
    }

    @Test
    fun `getGUIDForIdentifier returns GUID when already cached`() {
        val guid = "__1234567"
        val key = "email"
        val identifier = "abc@gmail.com"
        val cryptedKey = "${key}_$identifier"
        val initialGuids = JSONObject().apply {
            put("Phone_id1", "__789")
            put(cryptedKey, guid)
        }

        every { cryptHandler.encrypt(identifier, key) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        val actualGuid = loginInfoProvider.getGUIDForIdentifier(key, identifier)

        assertEquals(guid, actualGuid)
    }

    @Test
    fun `getGUIDForIdentifier returns null when GUID is not cached`() {
        val guid = "__1234567"
        val key = "email"
        val identifier = "abc@gmail.com"
        val cryptedKey = "${key}_$identifier"
        val initialGuids = JSONObject().apply { put(cryptedKey, guid) }

        every { cryptHandler.encrypt(identifier, key) } returns "dummy_encrypted"
        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        val actualGuid = loginInfoProvider.getGUIDForIdentifier(key, "not_cached@gmail.com")

        assertNull(actualGuid)
    }

    @Test
    fun `removeValueFromCachedGUIDForIdentifier removes value by key`() {
        val guid = "__1234567"
        val key = "Email"
        val initialGuids = JSONObject().apply {
            put("Email_donjoe2862@gmail.com", "__1234567")
            put("Identity_00002", "__1234567")
        }
        val resultGuids = JSONObject().apply { put("Identity_00002", "__1234567") }

        every { loginInfoProvider.getDecryptedCachedGUIDs() } returns initialGuids

        loginInfoProvider.removeValueFromCachedGUIDForIdentifier(guid, key)

        verify { loginInfoProvider.setCachedGUIDsAndLength(resultGuids.toString(), 1) }
    }

    @Test
    fun `deviceIsMultiUser returns true when multiple guids in the prefs`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 2)

        assertTrue { loginInfoProvider.deviceIsMultiUser() }
    }

    @Test
    fun `deviceIsMultiUser returns false when single guid in the prefs`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 1)

        assertFalse{ loginInfoProvider.deviceIsMultiUser() }
    }

    @Test
    fun `isLegacyProfileLoggedIn returns false when guid in the prefs and non-empty identiy keys`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 1)

        every { loginInfoProvider.cachedIdentityKeysForAccount } returns "dummy"

        assertFalse{ loginInfoProvider.isLegacyProfileLoggedIn() }
    }

    @Test
    fun `isLegacyProfileLoggedIn returns true when guid in the prefs but empty identity keys`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 1)

        every { loginInfoProvider.cachedIdentityKeysForAccount } returns ""

        assertTrue{ loginInfoProvider.isLegacyProfileLoggedIn() }
    }

    @Test
    fun `isLegacyProfileLoggedIn returns false when no guids in the prefs`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 0)

        assertFalse { loginInfoProvider.isLegacyProfileLoggedIn() }
    }

    @Test
    fun `isAnonymousDevice returns true when no guids in the prefs`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 0)

        assertTrue { loginInfoProvider.isAnonymousDevice() }
    }

    @Test
    fun `isAnonymousDevice returns false when guids in the prefs`() {
        loginInfoProvider.setCachedGUIDsAndLength("abc", 1)

        assertFalse{ loginInfoProvider.isAnonymousDevice() }
    }
}
