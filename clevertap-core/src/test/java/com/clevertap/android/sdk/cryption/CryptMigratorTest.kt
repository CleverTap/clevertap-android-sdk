package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.inbox.CTMessageDAO
import com.clevertap.android.sdk.variables.repo.VariablesRepo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CryptMigratorTest {

    @MockK(relaxed = true)
    private lateinit var logger: ILogger

    @MockK(relaxed = true)
    private lateinit var cryptHandler: CryptHandler

    @MockK(relaxed = true)
    private lateinit var cryptRepository: CryptRepository

    @MockK(relaxed = true)
    private lateinit var dataMigrationRepository: DataMigrationRepository

    @MockK(relaxed = true)
    private lateinit var variablesRepo: VariablesRepo

    @MockK(relaxed = true)
    private lateinit var dbAdapter: DBAdapter

    private lateinit var cryptMigratorMedium: CryptMigrator

    private lateinit var cryptMigratorNone: CryptMigrator

    private lateinit var cryptMigratorFullData: CryptMigrator

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        cryptMigratorMedium = CryptMigrator(
            logPrefix = "[CryptMigratorTest]",
            configEncryptionLevel = EncryptionLevel.MEDIUM.intValue(),
            logger = logger,
            cryptHandler = cryptHandler,
            cryptRepository = cryptRepository,
            dataMigrationRepository = dataMigrationRepository,
            variablesRepo = variablesRepo,
            dbAdapter = dbAdapter
        )

        cryptMigratorNone = CryptMigrator(
            logPrefix = "[CryptMigratorTest]",
            configEncryptionLevel = EncryptionLevel.NONE.intValue(),
            logger = logger,
            cryptHandler = cryptHandler,
            cryptRepository = cryptRepository,
            dataMigrationRepository = dataMigrationRepository,
            variablesRepo = variablesRepo,
            dbAdapter = dbAdapter
        )

        cryptMigratorFullData = CryptMigrator(
            logPrefix = "[CryptMigratorTest]",
            configEncryptionLevel = EncryptionLevel.FULL_DATA.intValue(),
            logger = logger,
            cryptHandler = cryptHandler,
            cryptRepository = cryptRepository,
            dataMigrationRepository = dataMigrationRepository,
            variablesRepo = variablesRepo,
            dbAdapter = dbAdapter
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
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

        verify { cryptRepository.updateIsSSInAppDataMigrated(true) }
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
        every { cryptHandler.encrypt(decryptedText)} returns encryptedText

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
        every { cryptHandler.encrypt(decryptedText)} returns null

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
        every { cryptHandler.decrypt(encryptedText)} returns decryptedText

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
        every { cryptHandler.decrypt(encryptedText)} returns null


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

        verify(exactly = 0) { cryptHandler.encrypt(any()) }
        verify(exactly = 0) { cryptHandler.decrypt(any()) }
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

        every { cryptHandler.decryptWithAlgorithm(any(), CryptHandler.EncryptionAlgorithm.AES)} returns migratedFormatJson
        every { cryptHandler.encrypt(any())} returns encryptedText

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
        every { cryptHandler.decryptWithAlgorithm(any(), CryptHandler.EncryptionAlgorithm.AES)} returns null

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

        every { cryptHandler.decryptWithAlgorithm(any(), CryptHandler.EncryptionAlgorithm.AES)} returns migratedFormat
        every { cryptHandler.encrypt(any())} returns null

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
        every { cryptHandler.encrypt(any())} returns encryptedText
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        assertEquals(encryptedJSONObject.toString(), jsonObjectSlot.captured.toString())
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
        every { cryptHandler.encrypt(any())} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        assertEquals(decryptedJSONObject.toString(), jsonObjectSlot.captured.toString())
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

        every { cryptHandler.decrypt(encryptedText)} returns decryptedText
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorNone.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }

        assertEquals(decryptedJSONObject.toString(), jsonObjectSlot.captured.toString())
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

        every { cryptHandler.decrypt(encryptedText)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorNone.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        assertEquals(encryptedJSONObject, jsonObjectSlot.captured)
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

        every { cryptHandler.decryptWithAlgorithm(encryptedTextV1, CryptHandler.EncryptionAlgorithm.AES)} returns decryptedText
        every { cryptHandler.encrypt(decryptedText)} returns encryptedTextV2
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        assertEquals(resultJSONObject.toString(), jsonObjectSlot.captured.toString())
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

        every { cryptHandler.decryptWithAlgorithm(encryptedText, CryptHandler.EncryptionAlgorithm.AES)} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
        assertEquals(droppedJSONObject.toString(), jsonObjectSlot.captured.toString())
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

        every { cryptHandler.decryptWithAlgorithm(any(), CryptHandler.EncryptionAlgorithm.AES)} returns decryptedText
        every { cryptHandler.encrypt(any())} returns null
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()

        verify { dataMigrationRepository.saveUserProfile(eq("deviceId"), capture(jsonObjectSlot)) }
        verify { cryptRepository.updateMigrationFailureCount(false) }
        assertEquals(decryptedJSONObject.toString(), jsonObjectSlot.captured.toString())
    }

    // ------------------------------------------------------------------------------------------------------------
    // --------------------------------- FULL_DATA Encryption Level Tests ----------------------------------------
    // ------------------------------------------------------------------------------------------------------------

    // ========== 1. Migration Triggering Tests ==========

    @Test()
    fun `migrateEncryption should trigger migration when level changes from MEDIUM to FULL_DATA`() {
        val varsJson = JSONObject(mapOf(
            "var1" to 10,
            "group.var2" to 20,
        ))
        val userId = "userId"
        val listOfMessages = arrayListOf(CTMessageDAO())
        val userProfileSample = JSONObject().apply {
            put("Email", "<ct<email>ct>")
        }
        val cachedGuidString = "<ct<some-cached-guid-map>ct>"

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { dbAdapter.getMessages(userId) } returns listOfMessages
        every { variablesRepo.loadDataFromCache() } returns varsJson.toString()
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf(userId to userProfileSample)
        every { dataMigrationRepository.cachedGuidString() } returns cachedGuidString
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        // verify fetch of current stats
        verifyOrder {
            cryptRepository.storedEncryptionLevel()
            cryptRepository.migrationFailureCount()
            cryptRepository.isSSInAppDataMigrated()

            dataMigrationRepository.cachedGuidString()
            dataMigrationRepository.saveCachedGuidJson(cachedGuidString)

            dataMigrationRepository.userProfilesInAccount()
            dataMigrationRepository.saveUserProfile(userId, userProfileSample)
            dataMigrationRepository.inAppDataFiles(any(), any())

            // verify that vars are encrypted
            variablesRepo.loadDataFromCache()
            variablesRepo.storeDataInCache(varsJson.toString())

            // verify inbox related stuff
            dataMigrationRepository.userProfilesInAccount()
            dbAdapter.getMessages(userId)
            dbAdapter.upsertMessages(listOfMessages)

            // verify status of migrations for success
            cryptRepository.updateEncryptionLevel(EncryptionLevel.FULL_DATA.intValue())
            cryptRepository.updateIsSSInAppDataMigrated(true)
            cryptRepository.updateMigrationFailureCount(true)
        }

        confirmVerified(dbAdapter, cryptRepository, variablesRepo, dataMigrationRepository)
    }

    @Test
    fun `migrateEncryption should trigger migration when level changes from FULL_DATA to MEDIUM`() {
        val varsJson = JSONObject(mapOf(
            "var1" to 10,
            "group.var2" to 20,
        ))
        val userId = "userId"
        val listOfMessages = arrayListOf(CTMessageDAO())
        val userProfileSample = JSONObject().apply {
            put("Email", "email@email")
        }
        val cachedGuidString = "<ct<some-cached-guid-map>ct>"

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { dbAdapter.getMessages(userId) } returns listOfMessages
        every { variablesRepo.loadDataFromCache() } returns varsJson.toString()
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf(userId to userProfileSample)
        every { dataMigrationRepository.cachedGuidString() } returns cachedGuidString
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        // We will get this instance for migration in this case created from CoreFactory.
        cryptMigratorMedium.migrateEncryption()

        // verify fetch of current stats
        verifyOrder {
            cryptRepository.storedEncryptionLevel()
            cryptRepository.migrationFailureCount()
            cryptRepository.isSSInAppDataMigrated()

            dataMigrationRepository.cachedGuidString()
            dataMigrationRepository.saveCachedGuidJson(cachedGuidString)

            dataMigrationRepository.userProfilesInAccount()
            dataMigrationRepository.saveUserProfile(userId, userProfileSample)
            dataMigrationRepository.inAppDataFiles(any(), any())

            // verify that vars are encrypted
            variablesRepo.loadDataFromCache()
            variablesRepo.storeDataInCache(varsJson.toString())

            // verify inbox related stuff
            dataMigrationRepository.userProfilesInAccount()
            dbAdapter.getMessages(userId)
            dbAdapter.upsertMessages(listOfMessages)

            // verify status of migrations for success
            cryptRepository.updateEncryptionLevel(EncryptionLevel.MEDIUM.intValue())
            cryptRepository.updateIsSSInAppDataMigrated(true)
            cryptRepository.updateMigrationFailureCount(true)
        }
    }

    @Test
    fun `migrateEncryption should trigger migration when level changes from NONE to FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { cryptRepository.updateEncryptionLevel(EncryptionLevel.FULL_DATA.intValue()) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    @Test
    fun `migrateEncryption should trigger migration when level changes from FULL_DATA to NONE`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorNone.migrateEncryption()

        verify { cryptRepository.updateEncryptionLevel(EncryptionLevel.NONE.intValue()) }
        verify { cryptRepository.updateMigrationFailureCount(true) }
    }

    // ========== 2. Variables Data Migration Tests ==========

    @Test
    fun `migrateVariablesData should be called when target encryption level is FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns "test_variables_data"
        every { variablesRepo.storeDataInCache(any()) } returns Unit
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { variablesRepo.loadDataFromCache() }
        verify { variablesRepo.storeDataInCache("test_variables_data") }
    }

    @Test
    fun `migrateVariablesData should be called when stored encryption level is FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns "test_variables_data"
        every { variablesRepo.storeDataInCache(any()) } returns Unit
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorMedium.migrateEncryption()

        verify { variablesRepo.loadDataFromCache() }
        verify { variablesRepo.storeDataInCache("test_variables_data") }
    }

    @Test
    fun `migrateVariablesData should load and store variables data when data exists`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        val variablesData = """{"var1":"value1","var2":"value2"}"""
        every { variablesRepo.loadDataFromCache() } returns variablesData
        every { variablesRepo.storeDataInCache(variablesData) } returns Unit
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { variablesRepo.loadDataFromCache() }
        verify { variablesRepo.storeDataInCache(variablesData) }
    }

    @Test
    fun `migrateVariablesData should skip migration when variables data is null`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { variablesRepo.loadDataFromCache() }
        verify(exactly = 0) { variablesRepo.storeDataInCache(any()) }
    }

    @Test
    fun `migrateVariablesData should not be called when encryption level is not FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorMedium.migrateEncryption()

        verify(exactly = 0) { variablesRepo.loadDataFromCache() }
        verify(exactly = 0) { variablesRepo.storeDataInCache(any()) }
    }

    // ========== 3. Inbox Data Migration Tests ==========

    @Test
    fun `migrateInboxData should be called when target encryption level is FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        val deviceIds = mapOf("device1" to JSONObject(), "device2" to JSONObject())
        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns deviceIds
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages("device1") } returns arrayListOf()
        every { dbAdapter.getMessages("device2") } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { dbAdapter.getMessages("device1") }
        verify { dbAdapter.getMessages("device2") }
        verify(exactly = 2) { dbAdapter.upsertMessages(arrayListOf()) }
    }

    @Test
    fun `migrateInboxData should be called when stored encryption level is FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        val deviceIds = mapOf("device1" to JSONObject())
        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns deviceIds
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages("device1") } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorMedium.migrateEncryption()

        verify { dbAdapter.getMessages("device1") }
        verify { dbAdapter.upsertMessages(arrayListOf()) }
    }

    @Test
    fun `migrateInboxData should migrate messages for all user profiles`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        val deviceIds = mapOf(
            "device1" to JSONObject(),
            "device2" to JSONObject(),
            "device3" to JSONObject()
        )
        
        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns deviceIds
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify { dbAdapter.getMessages("device1") }
        verify { dbAdapter.getMessages("device2") }
        verify { dbAdapter.getMessages("device3") }
        verify(exactly = 3) { dbAdapter.upsertMessages(arrayListOf()) }
    }

    @Test
    fun `migrateInboxData should handle empty user profiles list`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorFullData.migrateEncryption()

        verify(exactly = 0) { dbAdapter.getMessages(any()) }
        verify(exactly = 0) { dbAdapter.upsertMessages(any()) }
    }

    @Test
    fun `migrateInboxData should not be called when encryption level is not FULL_DATA`() {
        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.NONE.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { dataMigrationRepository.userProfilesInAccount() } returns emptyMap()
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit

        cryptMigratorMedium.migrateEncryption()

        verify(exactly = 0) { dbAdapter.getMessages(any()) }
        verify(exactly = 0) { dbAdapter.upsertMessages(any()) }
    }

    // ========== 4. DB Profile with FULL_DATA Tests ==========

    @Test
    fun `migrateDBProfile should convert PII fields to plain text when target level is FULL_DATA`() {
        // AES_GCM format: <ct<encrypted_email>ct>
        val encryptedEmail = "<ct<encrypted_email>ct>"
        val decryptedEmail = "test@example.com"
        
        val profileJSON = JSONObject().apply {
            put("Email", encryptedEmail)
            put("Custom", "no_encrypt")
        }
        
        val expectedJSON = JSONObject().apply {
            put("Email", decryptedEmail)
            put("Custom", "no_encrypt")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf("device1" to profileJSON)
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        every { cryptHandler.decrypt(encryptedEmail) } returns decryptedEmail

        cryptMigratorFullData.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()
        verify { dataMigrationRepository.saveUserProfile(eq("device1"), capture(jsonObjectSlot)) }
        assertEquals(expectedJSON.toString(), jsonObjectSlot.captured.toString())
    }

    @Test
    fun `migrateDBProfile should preserve non-PII fields when migrating to FULL_DATA`() {
        // AES_GCM format
        val encryptedEmail = "<ct<encrypted_email>ct>"
        val decryptedEmail = "test@example.com"
        
        val profileJSON = JSONObject().apply {
            put("Email", encryptedEmail)
            put("CustomField1", "value1")
            put("CustomField2", 123)
            put("CustomField3", true)
        }
        
        val expectedJSON = JSONObject().apply {
            put("Email", decryptedEmail)
            put("CustomField1", "value1")
            put("CustomField2", 123)
            put("CustomField3", true)
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf("device1" to profileJSON)
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        every { cryptHandler.decrypt(encryptedEmail) } returns decryptedEmail

        cryptMigratorFullData.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()
        verify { dataMigrationRepository.saveUserProfile(eq("device1"), capture(jsonObjectSlot)) }
        
        // Verify all fields are preserved
        assertEquals("value1", jsonObjectSlot.captured.getString("CustomField1"))
        assertEquals(123, jsonObjectSlot.captured.getInt("CustomField2"))
        assertTrue(jsonObjectSlot.captured.getBoolean("CustomField3"))
    }

    @Test
    fun `migrateDBProfile should handle FULL_DATA to MEDIUM transition correctly`() {
        val plainEmail = "test@example.com"
        val encryptedEmail = "<ct<encrypted_email>ct>"
        
        val profileJSON = JSONObject().apply {
            put("Email", plainEmail)
            put("Custom", "no_encrypt")
        }
        
        val expectedJSON = JSONObject().apply {
            put("Email", encryptedEmail)
            put("Custom", "no_encrypt")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf("device1" to profileJSON)
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        every { cryptHandler.encrypt(plainEmail) } returns encryptedEmail

        cryptMigratorMedium.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()
        verify { dataMigrationRepository.saveUserProfile(eq("device1"), capture(jsonObjectSlot)) }
        assertEquals(expectedJSON.toString(), jsonObjectSlot.captured.toString())
    }

    @Test
    fun `migrateDBProfile should handle FULL_DATA to NONE transition correctly`() {
        val plainEmail = "test@example.com"
        
        val profileJSON = JSONObject().apply {
            put("Email", plainEmail)
            put("Custom", "no_encrypt")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.FULL_DATA.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf("device1" to profileJSON)
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        cryptMigratorNone.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()
        verify { dataMigrationRepository.saveUserProfile(eq("device1"), capture(jsonObjectSlot)) }
        
        // Profile should remain plain text
        assertTrue(jsonObjectSlot.captured.getString("Email") == plainEmail)
        assertTrue(jsonObjectSlot.captured.getString("Custom") == "no_encrypt")
    }

    @Test
    fun `migrateDBProfile should handle multiple PII fields with FULL_DATA level`() {
        // AES_GCM format
        val encryptedEmail = "<ct<encrypted_email>ct>"
        val encryptedPhone = "<ct<encrypted_phone>ct>"
        val encryptedName = "<ct<encrypted_name>ct>"
        val decryptedEmail = "test@example.com"
        val decryptedPhone = "+1234567890"
        val decryptedName = "John Doe"
        
        val profileJSON = JSONObject().apply {
            put("Email", encryptedEmail)
            put("Phone", encryptedPhone)
            put("Name", encryptedName)
            put("Custom", "no_encrypt")
        }
        
        val expectedJSON = JSONObject().apply {
            put("Email", decryptedEmail)
            put("Phone", decryptedPhone)
            put("Name", decryptedName)
            put("Custom", "no_encrypt")
        }

        every { cryptRepository.storedEncryptionLevel() } returns EncryptionLevel.MEDIUM.intValue()
        every { cryptRepository.migrationFailureCount() } returns CryptMigrator.MIGRATION_NOT_NEEDED
        every { cryptRepository.isSSInAppDataMigrated() } returns true

        every { variablesRepo.loadDataFromCache() } returns null
        every { dataMigrationRepository.userProfilesInAccount() } returns mapOf("device1" to profileJSON)
        every { dataMigrationRepository.cachedGuidString() } returns null
        every { dataMigrationRepository.inAppDataFiles(any(), any()) } returns Unit
        every { dataMigrationRepository.saveUserProfile(any(), any()) } returns 1
        every { dbAdapter.getMessages(any()) } returns arrayListOf()
        every { dbAdapter.upsertMessages(any()) } returns Unit

        every { cryptHandler.decrypt(encryptedEmail) } returns decryptedEmail
        every { cryptHandler.decrypt(encryptedPhone) } returns decryptedPhone
        every { cryptHandler.decrypt(encryptedName) } returns decryptedName

        cryptMigratorFullData.migrateEncryption()

        val jsonObjectSlot = slot<JSONObject>()
        verify { dataMigrationRepository.saveUserProfile(eq("device1"), capture(jsonObjectSlot)) }
        assertEquals(expectedJSON.toString(), jsonObjectSlot.captured.toString())
    }
}
