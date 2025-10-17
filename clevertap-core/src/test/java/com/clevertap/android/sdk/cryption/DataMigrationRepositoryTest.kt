package com.clevertap.android.sdk.cryption

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataMigrationRepositoryTest : BaseTestCase() {

    internal lateinit var dataMigrationRepository: DataMigrationRepository
    internal lateinit var dbAdapter: DBAdapter

    @Before
    fun setup() {
        dbAdapter = mockk(relaxed = true)
        dataMigrationRepository = DataMigrationRepository(
            context = appCtx,
            config = cleverTapInstanceConfig,
            dbAdapter = dbAdapter
        )
    }

    @After
    fun tearDown() {
        // Clean up any saved data
        confirmVerified(dbAdapter)
        dataMigrationRepository.removeCachedGuidJson()
        clearAllMocks()
    }

    // ==================== cachedGuidString() Tests ====================

    @Test
    fun `cachedGuidString should return cached GUID when it exists`() {
        // Arrange
        val expectedGuid = "test-guid-12345"
        dataMigrationRepository.saveCachedGuidJson(expectedGuid)

        // Act
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertEquals(expectedGuid, result)
    }

    @Test
    fun `cachedGuidString should return null when no cached GUID exists`() {
        // Arrange - ensure no cached GUID
        dataMigrationRepository.removeCachedGuidJson()

        // Act
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertNull(result)
    }

    // ==================== cachedGuidJsonObject() Tests ====================

    @Test
    fun `cachedGuidJsonObject should return valid JSONObject when cached GUID is valid JSON`() {
        // Arrange
        val jsonString = """{"key":"value","id":123}"""
        dataMigrationRepository.saveCachedGuidJson(jsonString)

        // Act
        val result = dataMigrationRepository.cachedGuidJsonObject()

        // Assert
        assertNotNull(result)
        assertEquals("value", result.optString("key"))
        assertEquals(123, result.optInt("id"))
    }

    @Test
    fun `cachedGuidJsonObject should return empty JSONObject when cached GUID is null`() {
        // Arrange
        dataMigrationRepository.removeCachedGuidJson()

        // Act
        val result = dataMigrationRepository.cachedGuidJsonObject()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.length())
    }

    @Test
    fun `cachedGuidJsonObject should handle malformed JSON gracefully`() {
        // Arrange
        val malformedJson = "not-a-valid-json{{"
        dataMigrationRepository.saveCachedGuidJson(malformedJson)

        // Act
        val result = dataMigrationRepository.cachedGuidJsonObject()

        // Assert
        assertNotNull(result)
        // CTJsonConverter should handle this gracefully and return empty JSONObject
        assertEquals(0, result.length())
    }

    @Test
    fun `cachedGuidJsonObject should handle empty JSON object`() {
        // Arrange
        val emptyJsonString = "{}"
        dataMigrationRepository.saveCachedGuidJson(emptyJsonString)

        // Act
        val result = dataMigrationRepository.cachedGuidJsonObject()

        // Assert
        assertNotNull(result)
        assertEquals(0, result.length())
    }

    // ==================== saveCachedGuidJson() Tests ====================

    @Test
    fun `saveCachedGuidJson should save valid JSON string and retrieve it`() {
        // Arrange
        val jsonString = """{"key":"value"}"""

        // Act
        dataMigrationRepository.saveCachedGuidJson(jsonString)
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertEquals(jsonString, result)
    }

    @Test
    fun `saveCachedGuidJson should save and overwrite existing value`() {
        // Arrange
        val firstValue = """{"key":"value1"}"""
        val secondValue = """{"key":"value2"}"""

        // Act
        dataMigrationRepository.saveCachedGuidJson(firstValue)
        val firstResult = dataMigrationRepository.cachedGuidString()
        
        dataMigrationRepository.saveCachedGuidJson(secondValue)
        val secondResult = dataMigrationRepository.cachedGuidString()

        // Assert
        assertEquals(firstValue, firstResult)
        assertEquals(secondValue, secondResult)
    }

    @Test
    fun `saveCachedGuidJson should save null value`() {
        // Arrange
        dataMigrationRepository.saveCachedGuidJson("some-value")

        // Act
        dataMigrationRepository.saveCachedGuidJson(null)
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertNull(result)
    }

    @Test
    fun `saveCachedGuidJson should save empty string`() {
        // Arrange & Act
        dataMigrationRepository.saveCachedGuidJson("")
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertEquals("", result)
    }

    // ==================== removeCachedGuidJson() Tests ====================

    @Test
    fun `removeCachedGuidJson should remove cached GUID`() {
        // Arrange
        val guid = "test-guid-12345"
        dataMigrationRepository.saveCachedGuidJson(guid)
        
        // Verify it was saved
        assertNotNull(dataMigrationRepository.cachedGuidString())

        // Act
        dataMigrationRepository.removeCachedGuidJson()
        val result = dataMigrationRepository.cachedGuidString()

        // Assert
        assertNull(result)
    }

    @Test
    fun `removeCachedGuidJson should handle removing non-existent GUID`() {
        // Arrange
        dataMigrationRepository.removeCachedGuidJson()

        // Act & Assert (should not throw exception)
        dataMigrationRepository.removeCachedGuidJson()
        val result = dataMigrationRepository.cachedGuidString()
        assertNull(result)
    }

    // ==================== saveCachedGuidJsonLength() Tests ====================

    @Test
    fun `saveCachedGuidJsonLength should save positive length`() {
        // Arrange & Act
        val length = 150
        dataMigrationRepository.saveCachedGuidJsonLength(length)

        val op = StorageHelper.getIntFromPrefs(
            appCtx,
            cleverTapInstanceConfig.accountId,
            Constants.CACHED_GUIDS_LENGTH_KEY,
            -1
        )

        assertEquals(op, length)
    }

    // ==================== userProfilesInAccount() Tests ====================

    @Test
    fun `userProfilesInAccount should return profiles map when profiles exist`() {
        // Arrange
        val expectedProfiles = mapOf(
            "device1" to JSONObject("""{"name":"User1"}"""),
            "device2" to JSONObject("""{"name":"User2"}""")
        )
        every {
            dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId)
        } returns expectedProfiles

        // Act
        val result = dataMigrationRepository.userProfilesInAccount()

        // Assert
        assertEquals(expectedProfiles, result)
        verify(exactly = 1) {
            dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId)
        }
    }

    @Test
    fun `userProfilesInAccount should return empty map when no profiles exist`() {
        // Arrange
        every {
            dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId)
        } returns emptyMap()

        // Act
        val result = dataMigrationRepository.userProfilesInAccount()

        // Assert
        assertEquals(0, result.size)
        verify(exactly = 1) {
            dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId)
        }
    }

    @Test
    fun `userProfilesInAccount should return multiple profiles correctly`() {
        // Arrange
        val expectedProfiles = mapOf(
            "device1" to JSONObject("""{"name":"User1","age":25}"""),
            "device2" to JSONObject("""{"name":"User2","age":30}"""),
            "device3" to JSONObject("""{"name":"User3","age":35}""")
        )
        every {
            dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId)
        } returns expectedProfiles

        // Act
        val result = dataMigrationRepository.userProfilesInAccount()

        verify { dbAdapter.fetchUserProfilesByAccountId(cleverTapInstanceConfig.accountId) }
        // Assert
        assertEquals(3, result.size)
        assertEquals(expectedProfiles, result)
    }

    // ==================== saveUserProfile() Tests ====================

    @Test
    fun `saveUserProfile should save profile and return row ID`() {
        // Arrange
        val deviceId = "device-123"
        val profile = JSONObject("""{"name":"Test User"}""")
        val expectedRowId = 42L
        every {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        } returns expectedRowId

        // Act
        val result = dataMigrationRepository.saveUserProfile(deviceId, profile)

        // Assert
        assertEquals(expectedRowId, result)
        verify(exactly = 1) {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        }
    }

    @Test
    fun `saveUserProfile should handle empty device ID`() {
        // Arrange
        val deviceId = ""
        val profile = JSONObject("""{"name":"Test User"}""")
        val expectedRowId = 1L
        every {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        } returns expectedRowId

        // Act
        val result = dataMigrationRepository.saveUserProfile(deviceId, profile)

        // Assert
        assertEquals(expectedRowId, result)
        verify(exactly = 1) {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        }
    }

    @Test
    fun `saveUserProfile should handle empty JSONObject`() {
        // Arrange
        val deviceId = "device-123"
        val profile = JSONObject()
        val expectedRowId = 1L
        every {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        } returns expectedRowId

        // Act
        val result = dataMigrationRepository.saveUserProfile(deviceId, profile)

        // Assert
        assertEquals(expectedRowId, result)
        verify(exactly = 1) {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        }
    }

    @Test
    fun `saveUserProfile should handle complex profile data`() {
        // Arrange
        val deviceId = "device-456"
        val profile = JSONObject("""
            {
                "name":"John Doe",
                "age":30,
                "email":"john@example.com",
                "tags":["premium","active"]
            }
        """.trimIndent())
        val expectedRowId = 100L
        every {
            dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile)
        } returns expectedRowId

        // Act
        val result = dataMigrationRepository.saveUserProfile(deviceId, profile)

        // Assert
        assertEquals(expectedRowId, result)

        verify { dbAdapter.storeUserProfile(cleverTapInstanceConfig.accountId, deviceId, profile) }
    }

    // ==================== inAppDataFiles() Tests ====================

    @Test
    fun `inAppDataFiles should handle empty keys list gracefully`() {
        // Arrange
        val keysToMigrate = emptyList<String>()
        val migrateFunction: (String) -> String? = { it }

        // Act & Assert (should not throw exception)
        dataMigrationRepository.inAppDataFiles(keysToMigrate, migrateFunction)
        assertTrue(true)
    }

    @Test
    fun `inAppDataFiles should handle migration with no matching files`() {
        // Arrange
        val keysToMigrate = listOf("key1", "key2")
        val migrateFunction: (String) -> String? = { "encrypted-$it" }

        // Act & Assert (should not throw exception)
        dataMigrationRepository.inAppDataFiles(keysToMigrate, migrateFunction)
        assertTrue(true)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should save retrieve and remove cached GUID in sequence`() {
        // Arrange
        val guid1 = "guid-12345"
        val guid2 = "guid-67890"

        // Act & Assert - Save first GUID
        dataMigrationRepository.saveCachedGuidJson(guid1)
        assertEquals(guid1, dataMigrationRepository.cachedGuidString())

        // Act & Assert - Overwrite with second GUID
        dataMigrationRepository.saveCachedGuidJson(guid2)
        assertEquals(guid2, dataMigrationRepository.cachedGuidString())

        // Act & Assert - Remove GUID
        dataMigrationRepository.removeCachedGuidJson()
        assertNull(dataMigrationRepository.cachedGuidString())
    }

    @Test
    fun `should handle complex JSON operations`() {
        // Arrange
        val complexJson = """
            {
                "users": [
                    {"id": 1, "name": "Alice"},
                    {"id": 2, "name": "Bob"}
                ],
                "metadata": {
                    "version": "1.0",
                    "timestamp": 1234567890
                }
            }
        """.trimIndent()

        // Act
        dataMigrationRepository.saveCachedGuidJson(complexJson)
        val result = dataMigrationRepository.cachedGuidJsonObject()

        // Assert
        assertNotNull(result)
        assertTrue(result.has("users"))
        assertTrue(result.has("metadata"))
        val metadata = result.getJSONObject("metadata")
        assertEquals("1.0", metadata.getString("version"))
    }
}
