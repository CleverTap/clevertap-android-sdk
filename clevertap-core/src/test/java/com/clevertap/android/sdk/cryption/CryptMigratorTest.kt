package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.ILogger
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class CryptMigratorTest {

    @MockK(relaxed = true)
    private lateinit var logger: ILogger

    @MockK(relaxed = true)
    private lateinit var cryptHandler: CryptHandler

    @MockK(relaxed = true)
    private lateinit var cryptRepository: CryptRepository

    @MockK(relaxed = true)
    private lateinit var dataMigrationRepository: DataMigrationRepository

    private lateinit var cryptMigratorMedium: CryptMigrator

    private lateinit var cryptMigratorNone: CryptMigrator

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        cryptMigratorMedium = CryptMigrator(
            logPrefix = "[CryptMigratorTest]",
            configEncryptionLevel = EncryptionLevel.MEDIUM.intValue(),
            logger = logger,
            cryptHandler = cryptHandler,
            cryptRepository = cryptRepository,
            dataMigrationRepository = dataMigrationRepository
        )

        cryptMigratorNone = CryptMigrator(
            logPrefix = "[CryptMigratorTest]",
            configEncryptionLevel = EncryptionLevel.NONE.intValue(),
            logger = logger,
            cryptHandler = cryptHandler,
            cryptRepository = cryptRepository,
            dataMigrationRepository = dataMigrationRepository
        )
    }

    @Test
    fun `migrateEncryption should not migrate when stored and configured encryption levels match, no migration failure and SSInApp data migrated`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        cryptMigratorMedium.migrateEncryption()

        verify(exactly = 0) { cryptRepository.updateMigrationFailureCount(any()) }
    }

    @Test
    fun `migrateEncryption should migrate when encryption levels differ and no migration failure`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { CryptHandler.isTextAESEncrypted(any()) } returns true

        cryptMigratorMedium.migrateEncryption()

        verify { cryptRepository.updateEncryptionLevel(EncryptionLevel.MEDIUM.intValue()) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateEncryption should migrate when SSInAppData is not migrated`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns false

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { CryptHandler.isTextAESEncrypted(any()) } returns true

        cryptMigratorMedium.migrateEncryption()

        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateEncryption should migrate when levels are the same and migration failure`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 2

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { CryptHandler.isTextAESEncrypted(any()) } returns true

        cryptMigratorMedium.migrateEncryption()

        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateEncryption should migrate when it's the first upgrade`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { CryptHandler.isTextAESEncrypted(any()) } returns true

        cryptMigratorMedium.migrateEncryption()

        verify { cryptRepository.updateMigrationFailureCount(true) }
    }


    // ------------------------------------------------------------------------------------------------------------
    //. --------------------------------- CGK related migration -----------------------------------------------------'
    // ------------------------------------------------------------------------------------------------------------
    @Test
    fun `migrateCachedGuidsKeyPref should migrate encrypted data when encryption level changes from none to medium`() {
        val encryptedText = "encrypted_data"
        val decryptedText = "decrypted_data"
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.cachedGuidString() } returns decryptedText

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { cryptHandler.encrypt(decryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns encryptedText

        cryptMigratorMedium.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(encryptedText) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should save data in plain-text when encryption fails and level changes from none to medium`() {
        val decryptedText = "decrypted_data"
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.cachedGuidString() } returns decryptedText

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { cryptHandler.encrypt(decryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null

        cryptMigratorMedium.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(decryptedText) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should migrate when encryption level changes from medium to none`() {
        val encryptedText = "encrypted_data"
        val decryptedText = "decrypted_data"
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.cachedGuidString() } returns encryptedText

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns true
        every { cryptHandler.decrypt(encryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns decryptedText

        cryptMigratorNone.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(decryptedText) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should save encrypted data when decryption fails and level changes from medium to none`() {
        val encryptedText = "encrypted_data"
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.cachedGuidString() } returns encryptedText

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns true
        every { cryptHandler.decrypt(encryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null


        cryptMigratorNone.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(encryptedText) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should not migrate if data is already migrated`() {
        val encryptedText = "encrypted_data"
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.cachedGuidString() } returns encryptedText

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns true

        cryptMigratorMedium.migrateEncryption()

        verify(exactly = 0) { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM) }
        verify(exactly = 0) { cryptHandler.decrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should migrate format and then encrypt for first upgrade`() {
        val encryptedJson = JSONObject().apply {
            put("Email_[encrypted1]", "_i123")
            put("Phone_[encrypted2]", "_i456")
        }

        val migratedFormatJson = "migratedFormatJson"
        val encryptedText = "encrypted_data"

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.isSSInAppDataMigrated() } returns true
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.cachedGuidJsonObject() } returns encryptedJson

        every { cryptHandler.decrypt(any(), CryptHandler.EncryptionAlgorithm.AES)} returns migratedFormatJson
        every { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM)} returns encryptedText

        mockkObject(CryptHandler)

        cryptMigratorMedium.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(encryptedText) }
        verify { dataMigrationRepository.saveCachedGuidJsonLength(encryptedJson.length()) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should remove cached data and save length 0 when decryption fails from AES during first upgrade`() {
        val encryptedJson = JSONObject().apply {
            put("Email_[encrypted1]", "_i123")
            put("Phone_[encrypted2]", "_i456")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.isSSInAppDataMigrated() } returns true
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.cachedGuidJsonObject() } returns encryptedJson
        every { cryptHandler.decrypt(any(), CryptHandler.EncryptionAlgorithm.AES)} returns null

        mockkObject(CryptHandler)

        cryptMigratorMedium.migrateEncryption()

        verify { dataMigrationRepository.removeCachedGuidJson() }
        verify { dataMigrationRepository.saveCachedGuidJsonLength(0) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateCachedGuidsKeyPref should save data as plaintext when encryption to AES_GCM fails during first upgrade`() {
        val encryptedJson = JSONObject().apply {
            put("Email_[encrypted1]", "_i123")
            put("Phone_[encrypted2]", "_i456")
        }

        val migratedFormat = "decrypted"

        val migratedJson = JSONObject().apply {
            put("Email_$migratedFormat", "_i123")
            put("Phone_$migratedFormat", "_i456")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.isSSInAppDataMigrated() } returns true
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.cachedGuidJsonObject() } returns encryptedJson

        every { cryptHandler.decrypt(any(), CryptHandler.EncryptionAlgorithm.AES)} returns migratedFormat
        every { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null

        mockkObject(CryptHandler)

        cryptMigratorMedium.migrateEncryption()

        verify { dataMigrationRepository.saveCachedGuidJson(migratedJson.toString()) }
        verify { dataMigrationRepository.saveCachedGuidJsonLength(2) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
    }

    // ------------------------------------------------------------------------------------------------------------
    //. --------------------------------- DB related migration -----------------------------------------------------'
    // ------------------------------------------------------------------------------------------------------------
    @Test
    fun `migrateDBProfile should migrate encrypted data when encryption level changes from none to medium`() {
        val encryptedText = "encrypted_data"
        val decryptedText = "decrypted_data"

        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedText)
            put("Custom", "no_encrypt")
        }
        val decryptedJSONObject = JSONObject().apply {
            put("Email", decryptedText)
            put("Custom", "no_encrypt")
        }

        val decryptedMap = mapOf("deviceId" to decryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.userProfilesInAccount() } returns decryptedMap

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM)} returns encryptedText
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        JSONAssert.assertEquals(encryptedJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should save data in plain-text when encryption fails and level changes from none to medium`() {
        val decryptedJSONObject = JSONObject().apply {
            put("Email", "value1")
            put("Custom", "no_encrypt")
        }

        val decryptedMap = mapOf("deviceId" to decryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.userProfilesInAccount() } returns decryptedMap

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns false
        every { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        JSONAssert.assertEquals(decryptedJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should migrate when encryption level changes from medium to none`() {
        val encryptedText = "encrypted_data"
        val decryptedText = "decrypted_data"
        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedText)
            put("Custom", "no_encrypt")
        }

        val decryptedJSONObject = JSONObject().apply {
            put("Email", decryptedText)
            put("Custom", "no_encrypt")
        }

        val encryptedMap = mapOf("deviceId" to encryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.userProfilesInAccount() } returns encryptedMap

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns true

        every { cryptHandler.decrypt(encryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns decryptedText
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorNone.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }

        JSONAssert.assertEquals(decryptedJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should save encrypted data when decryption fails and level changes from medium to none`() {
        val encryptedText = "encrypted_data"
        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedText)
            put("Custom", "no_encrypt")
        }

        val encryptedMap = mapOf("deviceId" to encryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns 0

        every { dataMigrationRepository.userProfilesInAccount() } returns encryptedMap

        mockkObject(CryptHandler)
        every { CryptHandler.isTextAESEncrypted(any()) } returns false
        every { CryptHandler.isTextAESGCMEncrypted(any()) } returns true

        every { cryptHandler.decrypt(encryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorNone.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        JSONAssert.assertEquals(encryptedJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should migrate for first upgrade`() {
        val encryptedTextV1 = "[encrypted_data]"
        val decryptedText = "decrypted_data"
        val encryptedTextV2 = "<ct<encrypted_data>ct>"
        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedTextV1)
            put("Custom", "no_encrypt")
        }

        val resultJSONObject = JSONObject().apply {
            put("Email", encryptedTextV2)
            put("Custom", "no_encrypt")
        }

        val encryptedMap = mapOf("deviceId" to encryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.userProfilesInAccount() } returns encryptedMap

        mockkObject(CryptHandler)

        every { cryptHandler.decrypt(encryptedTextV1, CryptHandler.EncryptionAlgorithm.AES)} returns decryptedText
        every { cryptHandler.encrypt(decryptedText, CryptHandler.EncryptionAlgorithm.AES_GCM)} returns encryptedTextV2
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        JSONAssert.assertEquals(resultJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should store field in plain text when encryption fails to AES_GCM during first upgrade`() {
        val encryptedText = "[encrypted_data]"
        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedText)
            put("Custom", "no_encrypt")
        }

        val droppedJSONObject = JSONObject().apply {
            put("Custom", "no_encrypt")
        }

        val encryptedMap = mapOf("deviceId" to encryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.userProfilesInAccount() } returns encryptedMap

        mockkObject(CryptHandler)

        every { cryptHandler.decrypt(encryptedText, CryptHandler.EncryptionAlgorithm.AES)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        JSONAssert.assertEquals(droppedJSONObject, jsonObjectSlot.captured, false)
    }

    @Test
    fun `migrateDBProfile should remove field from profile when decryption fails from AES during first upgrade`() {
        val encryptedText = "[encrypted_data]"
        val decryptedText = "decrypted_data"
        val encryptedJSONObject = JSONObject().apply {
            put("Email", encryptedText)
            put("Custom", "no_encrypt")
        }

        val decryptedJSONObject = JSONObject().apply {
            put("Email", decryptedText)
            put("Custom", "no_encrypt")
        }

        val encryptedMap = mapOf("deviceId" to encryptedJSONObject)

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_FIRST_UPGRADE

        every { dataMigrationRepository.userProfilesInAccount() } returns encryptedMap

        mockkObject(CryptHandler)

        every { cryptHandler.decrypt(any(), CryptHandler.EncryptionAlgorithm.AES)} returns decryptedText
        every { cryptHandler.encrypt(any(), CryptHandler.EncryptionAlgorithm.AES_GCM)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        JSONAssert.assertEquals(decryptedJSONObject, jsonObjectSlot.captured, false)
    }
}
