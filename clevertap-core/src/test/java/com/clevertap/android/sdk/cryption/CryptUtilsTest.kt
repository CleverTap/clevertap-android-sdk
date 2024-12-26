package com.clevertap.android.sdk.cryption


import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Constants.CACHED_GUIDS_KEY
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.cryption.CryptUtils.migrateEncryptionLevel
import com.clevertap.android.sdk.cryption.CryptUtils.updateEncryptionFlagOnFailure
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.Mockito.verify
import org.mockito.kotlin.*
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals

class CryptUtilsTest : BaseTestCase() {

    @Mock
    private lateinit var mockCryptHandler: CryptHandler

    @Mock
    private lateinit var mockDBAdapter: DBAdapter


    private lateinit var config: CleverTapInstanceConfig

    @Mock
    lateinit var logger: Logger


    @Before
    override fun setUp() {
        super.setUp()
        config = CleverTapInstanceConfig.createInstance(application, "id", "token")
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 3 and failed is 2 `() {
        val failedFlag = Constants.ENCRYPTION_FLAG_DB_SUCCESS
        val initialEncryptionFlagStatus = 3
        val updatedEncryptionFlag = 1

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 2 and failed is 2`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_DB_SUCCESS
        val initialEncryptionFlagStatus = 2
        val updatedEncryptionFlag = 0

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 0 and failed is 2`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_DB_SUCCESS
        val initialEncryptionFlagStatus = 0
        val updatedEncryptionFlag = 0

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 1 and failed is 2`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_DB_SUCCESS
        val initialEncryptionFlagStatus = 1
        val updatedEncryptionFlag = 1

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 3 and failed is 1 `() {
        val failedFlag = Constants.ENCRYPTION_FLAG_CGK_SUCCESS
        val initialEncryptionFlagStatus = 3
        val updatedEncryptionFlag = 2

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 2 and failed is 1`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_CGK_SUCCESS
        val initialEncryptionFlagStatus = 2
        val updatedEncryptionFlag = 2

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 0 and failed is 1`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_CGK_SUCCESS
        val initialEncryptionFlagStatus = 0
        val updatedEncryptionFlag = 0

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test updateEncryptionFlagOnFailure when initialEncryptionFlagStatus is 1 and failed is 1`() {
        val failedFlag = Constants.ENCRYPTION_FLAG_CGK_SUCCESS
        val initialEncryptionFlagStatus = 1
        val updatedEncryptionFlag = 0

        `when`(mockCryptHandler.encryptionFlagStatus).thenReturn(initialEncryptionFlagStatus)
        updateEncryptionFlagOnFailure(appCtx, config, failedFlag, mockCryptHandler)

        val sharedPreferences = appCtx.getSharedPreferences("WizRocket", Context.MODE_PRIVATE)
        verify(mockCryptHandler).encryptionFlagStatus = updatedEncryptionFlag
        assertEquals(updatedEncryptionFlag, sharedPreferences.getInt("encryptionFlagStatus:id", -1))
    }

    @Test
    fun `test migrateEncryptionLevel when encryption level is not present in prefs`() {
        config.setEncryptionLevel(EncryptionLevel.NONE)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            -1
        )
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        verifyNoMoreInteractions(mockCryptHandler, mockDBAdapter)
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level is 1 and stored encryption level is 0`() {
        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        /* sequenceOf<String>("ARP:id:12345678","ARP:id:123456666","ARP:id:1234567777","ARP:WWW-111-000:1234567777")
             .forEach {
                 if (it == "ARP:id:12345678") {
                     StorageHelper.getPreferences(application, it).run {
                         edit()
                     }.putString(Constants.KEY_ENCRYPTION_k_n, "[ \"xyz@ct.com\" ]")
                         .commit()
                 } else{
                     StorageHelper.getPreferences(application, it).run {
                         edit()
                     }.putString(Constants.KEY_TEXT, "abc")
                         .commit()
                 }
         }*/


        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            0
        )

        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        verify(mockCryptHandler).encryptionFlagStatus = 3
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level and stored encryption level are equal`() {
        config.setEncryptionLevel(EncryptionLevel.MEDIUM)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            1
        )


        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        verify(mockCryptHandler).encryptionFlagStatus = 3
        verify(mockDBAdapter).fetchUserProfilesByAccountId("id")
    }

    @Test
    fun `test migrateEncryptionLevel when config encryption level and stored encryption level are equal and flagStatus is 3`() {
        config.setEncryptionLevel(EncryptionLevel.MEDIUM)
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_LEVEL),
            1
        )
        StorageHelper.putInt(
            application,
            StorageHelper.storageKeyWithSuffix(config, Constants.KEY_ENCRYPTION_FLAG_STATUS),
            3
        )

        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        verify(mockCryptHandler).encryptionFlagStatus = 3
        verifyNoMoreInteractions(mockDBAdapter, mockCryptHandler)
    }

    @Test
    fun testMigration() {

        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"
        val originalKey = "originalKey"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, 0)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)
        `when`(
            mockCryptHandler.decrypt(anyString(), anyString())
        ).thenReturn(originalIdentifier)

        // DB
        val db = mutableMapOf<String, JSONObject>()
        val data = JSONObject()
        data.put("Email", originalIdentifier)
        db["deviceID1"] = data
        db["deviceID2"] = data

        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        // pref
        val cachedGuidJsonObj = JSONObject()
        cachedGuidJsonObj.put("${originalKey}_$originalIdentifier", "value")

        put(CACHED_GUIDS_KEY, cachedGuidJsonObj.toString())

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        //--------Assert----------

        val neexpectedCGK = JSONObject()
        neexpectedCGK.put("${originalKey}_$encryptedIdentifier", "value")

        val actualCGK = get(CACHED_GUIDS_KEY, "null")

        assertEquals(
            config.encryptionLevel, get(Constants.KEY_ENCRYPTION_LEVEL, -1)
        )
        assertEquals(neexpectedCGK.toString(), actualCGK)

        val captorData = argumentCaptor<JSONObject>()
        val captorDeviceID = argumentCaptor<String>()
        val dbActual = JSONObject()
        dbActual.put("Email", encryptedIdentifier)

        verify(mockDBAdapter, Mockito.times(2)).storeUserProfile(anyString(), captorDeviceID.capture(), captorData.capture())

        assertEquals("deviceID1", captorDeviceID.firstValue)
        JSONAssert.assertEquals(dbActual, captorData.firstValue, true)
        assertEquals("deviceID2", captorDeviceID.secondValue)
        JSONAssert.assertEquals(dbActual, captorData.secondValue, true)
    }

    @Test
    fun `testMigration when level changes from 1 to 0`() {

        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"
        val originalKey = "originalKey"

        config.setEncryptionLevel(EncryptionLevel.NONE)

        put(Constants.KEY_ENCRYPTION_LEVEL, 1)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)
        `when`(
            mockCryptHandler.decrypt(anyString(), anyString())
        ).thenReturn(originalIdentifier)

        // DB
        val db = mutableMapOf<String, JSONObject>()
        val data = JSONObject()
        data.put("Email", encryptedIdentifier)
        db["deviceID1"] = data
        db["deviceID2"] = data

        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        // pref
        val cachedGuidJsonObj = JSONObject()
        cachedGuidJsonObj.put("${originalKey}_$encryptedIdentifier", "value")

        put(CACHED_GUIDS_KEY, cachedGuidJsonObj.toString())

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        //--------Assert----------

        val neexpectedCGK = JSONObject()
        neexpectedCGK.put("${originalKey}_$originalIdentifier", "value")

        val actualCGK = get(CACHED_GUIDS_KEY, "null")

        assertEquals(
            config.encryptionLevel, get(Constants.KEY_ENCRYPTION_LEVEL, -1)
        )
        assertEquals(neexpectedCGK.toString(), actualCGK)

        val captorData = argumentCaptor<JSONObject>()
        val captorDeviceID = argumentCaptor<String>()
        val dbActual = JSONObject()
        dbActual.put("Email", originalIdentifier)

        verify(mockDBAdapter, Mockito.times(2)).storeUserProfile(anyString(), captorDeviceID.capture(), captorData.capture())

        assertEquals("deviceID1", captorDeviceID.firstValue)
        JSONAssert.assertEquals(dbActual, captorData.firstValue, true)
        assertEquals("deviceID2", captorDeviceID.secondValue)
        JSONAssert.assertEquals(dbActual, captorData.secondValue, true)

    }

    @Test
    fun `testMigration when encryptionFlagStatus is 2 then no migration for DB`() {

        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"
        val originalKey = "originalKey"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, EncryptionLevel.MEDIUM.intValue())
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_DB_SUCCESS)

        // Pref
        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)
        `when`(
            mockCryptHandler.decrypt(anyString(), anyString())
        ).thenReturn(originalIdentifier)

        val cachedGuidJsonObj = JSONObject()
        cachedGuidJsonObj.put("${originalKey}_$originalIdentifier", "value")

        put(CACHED_GUIDS_KEY, cachedGuidJsonObj.toString())

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        //--------Assert----------

        val neexpectedCGK = JSONObject()
        neexpectedCGK.put("${originalKey}_$encryptedIdentifier", "value")

        val actualCGK = get(CACHED_GUIDS_KEY, "null")

        assertEquals(
            config.encryptionLevel, get(Constants.KEY_ENCRYPTION_LEVEL, -1)
        )
        assertEquals(neexpectedCGK.toString(), actualCGK)
        verifyNoMoreInteractions(mockDBAdapter)
        verify(mockCryptHandler).encryptionFlagStatus = 3
        assertEquals(3, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, -1))
    }

    @Test
    fun `testMigration when encryptionFlagStatus is 1 then no migration for CGK`() {

        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"
        val originalKey = "originalKey"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, EncryptionLevel.MEDIUM.intValue())
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_CGK_SUCCESS)

        // Pref
        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)
        `when`(
            mockCryptHandler.decrypt(anyString(), anyString())
        ).thenReturn(originalIdentifier)

        val cachedGuidJsonObj = JSONObject()
        cachedGuidJsonObj.put("${originalKey}_$encryptedIdentifier", "value")

        put(CACHED_GUIDS_KEY, cachedGuidJsonObj.toString())

        // DB
        val db = mutableMapOf<String, JSONObject>()
        val data = JSONObject()
        data.put("Email", originalIdentifier)
        db["deviceID1"] = data
        db["deviceID2"] = data


        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)
        //--------Assert----------

        assertEquals(cachedGuidJsonObj.toString(), get(CACHED_GUIDS_KEY, "null"))
        assertEquals(
            config.encryptionLevel, get(Constants.KEY_ENCRYPTION_LEVEL, -1)
        )
        verify(mockCryptHandler).encryptionFlagStatus = 3
        assertEquals(3, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, -1))

        val captorData = argumentCaptor<JSONObject>()
        val captorDeviceID = argumentCaptor<String>()
        val dbActual = JSONObject()
        dbActual.put("Email", encryptedIdentifier)

        verify(mockDBAdapter, Mockito.times(2)).storeUserProfile(anyString(), captorDeviceID.capture(), captorData.capture())

        assertEquals("deviceID1", captorDeviceID.firstValue)
        JSONAssert.assertEquals(dbActual, captorData.firstValue, true)
        assertEquals("deviceID2", captorDeviceID.secondValue)
        JSONAssert.assertEquals(dbActual, captorData.secondValue, true)
    }

    @Test
    fun `testMigration when encryptionLevel changes and db gives update error then encryptionFlagStatus bit for DB should be 0`() {
        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, 0)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)

        val db = mutableMapOf<String, JSONObject>()
        val data1 = JSONObject()
        data1.put("Email", originalIdentifier)
        db["deviceID1"] = data1


        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        `when`(mockDBAdapter.storeUserProfile(anyString(), anyString(), org.mockito.kotlin.any())).thenReturn(-1L)

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        //--------Assert----------
        assertEquals(1, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, 0))
    }

    @Test
    fun `testMigration when encryptionLevel changes and db is out of memory then encryptionFlagStatus bit for DB should be 0`() {
        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, 0)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(encryptedIdentifier)

        val db = mutableMapOf<String, JSONObject>()
        val data1 = JSONObject()
        data1.put("Email", originalIdentifier)
        db["deviceID1"] = data1


        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        `when`(mockDBAdapter.storeUserProfile(anyString(), anyString(),  org.mockito.kotlin.any())).thenReturn(-2L)

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        //--------Assert----------
        assertEquals(1, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, 0))
    }

    @Test
    fun `testMigration when encryptionLevel changes and encryption fails then encryptionFlagStatus should be 0`() {
        //--------Arrange----------
        val originalIdentifier = "originalIdentifier"
        val originalKey = "originalKey"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, 0)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        `when`(
            mockCryptHandler.encrypt(anyString(), anyString())
        ).thenReturn(null)

        val db = mutableMapOf<String, JSONObject>()
        val data1 = JSONObject()
        data1.put("Email", originalIdentifier)
        db["deviceID1"] = data1


        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        val cachedGuidJsonObj = JSONObject()
        cachedGuidJsonObj.put("${originalKey}_$originalIdentifier", "value")

        put(CACHED_GUIDS_KEY, cachedGuidJsonObj.toString())

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        //--------Assert----------
        assertEquals(0, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, 0))
    }

    @Test
    fun `testMigration when encryptionLevel changes and encryption fails for only one profile in db then encryptionFlagStatus bit for DB should be 0`() {
        //--------Arrange----------
        val encryptedIdentifier = "encryptedIdentifier"
        val originalIdentifier = "originalIdentifier"
        val failOriginalIdentifier = "failOriginalIdentifier"

        config.setEncryptionLevel(EncryptionLevel.MEDIUM)

        put(Constants.KEY_ENCRYPTION_LEVEL, 0)
        put(Constants.KEY_ENCRYPTION_FLAG_STATUS, Constants.ENCRYPTION_FLAG_FAIL)

        // Only fail for one identifier
        `when`(
            mockCryptHandler.encrypt(failOriginalIdentifier, "Email")
        ).thenReturn(null)
        `when`(
            mockCryptHandler.encrypt(originalIdentifier, "Email")
        ).thenReturn(encryptedIdentifier)

        val db = mutableMapOf<String, JSONObject>()
        val data1 = JSONObject()
        data1.put("Email", originalIdentifier)

        val data2 = JSONObject()
        data2.put("Email", failOriginalIdentifier)
        db["deviceID1"] = data1
        db["deviceID2"] = data2


        `when`(
            mockDBAdapter.fetchUserProfilesByAccountId(config.accountId)
        ).thenReturn(db)

        //--------Act----------
        migrateEncryptionLevel(application, config, mockCryptHandler, mockDBAdapter)

        //--------Assert----------
        assertEquals(1, get(Constants.KEY_ENCRYPTION_FLAG_STATUS, 0))

        val captorData = argumentCaptor<JSONObject>()
        val captorDeviceID = argumentCaptor<String>()
        val dbActual1 = JSONObject()
        dbActual1.put("Email", encryptedIdentifier)
        val dbActual2 = JSONObject()
        dbActual2.put("Email", failOriginalIdentifier)

        verify(mockDBAdapter, Mockito.times(2)).storeUserProfile(anyString(), captorDeviceID.capture(), captorData.capture())

        assertEquals("deviceID1", captorDeviceID.firstValue)
        JSONAssert.assertEquals(dbActual1, captorData.firstValue, true)
        assertEquals("deviceID2", captorDeviceID.secondValue)
        JSONAssert.assertEquals(dbActual2, captorData.secondValue, true)
    }

    private fun <T> put(key: String, value: T) {
        when (value) {
            is Int -> StorageHelper.putInt(
                application,
                StorageHelper.storageKeyWithSuffix(config, key),
                value
            )

            is String -> StorageHelper.putString(
                application,
                StorageHelper.storageKeyWithSuffix(config, key),
                value
            )
        }
    }

    private fun <T> get(key: String, default: T): Any? {
        return when (default) {
            is Int -> StorageHelper.getInt(
                application,
                StorageHelper.storageKeyWithSuffix(config, key),
                default
            )

            is String -> StorageHelper.getString(
                application,
                StorageHelper.storageKeyWithSuffix(config, key),
                default
            )

            else -> default
        }
    }

}

