package com.clevertap.android.sdk.db.dao

import TestCryptHandler
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.TestLogger
import com.clevertap.android.sdk.cryption.EncryptionLevel
import com.clevertap.android.sdk.db.DBEncryptionHandler
import com.clevertap.android.sdk.db.DatabaseHelper
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
class UserProfileDAOImplTest : BaseTestCase() {

    private lateinit var userProfileDAO: UserProfileDAO
    private lateinit var instanceConfig: CleverTapInstanceConfig
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var dbEncryptionHandler: DBEncryptionHandler

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"

    override fun setUp() {
        super.setUp()
        dbEncryptionHandler = DBEncryptionHandler(TestCryptHandler(), TestLogger(), EncryptionLevel.NONE)
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx, accID, accToken, accRegion)
        dbHelper = DatabaseHelper(
            context = appCtx,
            accountId = instanceConfig.accountId,
            dbName = "test_db",
            logger = instanceConfig.logger
        )
        userProfileDAO = UserProfileDAOImpl(
            dbHelper = dbHelper,
            logger = instanceConfig.logger,
            dbEncryptionHandler = dbEncryptionHandler
        )
    }

    @After
    fun cleanup() {
        dbHelper.deleteDatabase()
    }

    @Test
    fun test_storeUserProfile_when_validParams_should_storeProfile() {
        val accountId = "userID"
        val deviceId = "deviceID"
        val profile = JSONObject().apply {
            put("name", "john")
            put("father", "daniel")
        }

        val result = userProfileDAO.storeUserProfile(accountId, deviceId, profile)
        assertEquals(1, result)

        val fetchedProfile = userProfileDAO.fetchUserProfile(accountId, deviceId)
        assertNotNull(fetchedProfile)
        assertEquals("john", fetchedProfile.getString("name"))
        assertEquals("daniel", fetchedProfile.getString("father"))
    }

    @Test
    fun test_fetchUserProfilesByAccountId_when_calledWithAccountID_should_returnUserProfiles() {
        // Store profiles
        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID1",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }
        )

        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID2",
            JSONObject().also { it.put("name", "wick") }.also { it.put("father", "akshay") }
        )

        // Validation: profile is fetched
        userProfileDAO.fetchUserProfilesByAccountId("accountID").let {
            assertEquals("john", it["deviceID1"]?.getString("name")!!)
            assertEquals("daniel", it["deviceID1"]?.getString("father")!!)

            assertEquals("wick", it["deviceID2"]?.getString("name")!!)
            assertEquals("akshay", it["deviceID2"]?.getString("father")!!)
        }

        // Validation: empty map for non-existent account
        assertEquals(emptyMap(), userProfileDAO.fetchUserProfilesByAccountId("notAvailable"))
    }

    @Test
    fun test_fetchUserProfile_when_CorrectDeviceIdAndAccountId_returnsProfile() {
        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID1",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }
        )

        val profile = userProfileDAO.fetchUserProfile("accountID", "deviceID1")

        assertEquals("john", profile?.getString("name"))
        assertEquals("daniel", profile?.getString("father"))
    }

    @Test
    fun test_fetchUserProfile_when_IncorrectDeviceId_returnsNull() {
        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID1",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }
        )

        val profile = userProfileDAO.fetchUserProfile("accountID", "inc-deviceID1")
        assertNull(profile)
    }

    @Test
    fun test_fetchUserProfile_when_IncorrectAccountId_returnsNull() {
        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID1",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }
        )

        val profile = userProfileDAO.fetchUserProfile("inc-accountID", "deviceID1")
        assertNull(profile)
    }

    @Test
    fun test_fetchUserProfile_when_IncorrectAccountIdAndDeviceId_returnsNull() {
        userProfileDAO.storeUserProfile(
            "accountID",
            "deviceID1",
            JSONObject().also { it.put("name", "john") }.also { it.put("father", "daniel") }
        )

        val profile = userProfileDAO.fetchUserProfile("inc-accountID", "inc-deviceID1")
        assertNull(profile)
    }

    @Test
    fun test_fetchUserProfilesByAccountId_when_noProfiles_should_returnEmptyMap() {
        val result = userProfileDAO.fetchUserProfilesByAccountId("nonExistentAccount")
        assertTrue(result.isEmpty())
    }
}
