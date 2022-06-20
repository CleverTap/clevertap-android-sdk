package com.clevertap.android.sdk.db

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DBAdapterTest:BaseTestCase() {
    private lateinit var dbAdapter: DBAdapter
    private lateinit var instanceConfig: CleverTapInstanceConfig

    private val accID = "accountID"
    private val accToken = "token"
    private val accRegion = "sk1"
    private val dbName = "clevertap_$accID"



    override fun setUp() {
        super.setUp()
        instanceConfig = CleverTapInstanceConfig.createInstance(appCtx,accID,accToken,accRegion)
        dbAdapter = DBAdapter(appCtx,instanceConfig)
    }


    @Test
    fun test_deleteMessageForId_when_ABC_should_XYZ() {
        var msgId:String? = null
        var userID:String? = null
        var result  = false

        //case 1 : when msgId or user id is null, false is returned
        result = dbAdapter.deleteMessageForId(msgId,userID)
        assertEquals(false,result)

        //case 2 : when msgId or user id is not null, the sqlite query is executed accordingly on the table and therefore true is returned. note, even empty values for msg or user id are allowed
        // todo : ensure the query run by adding additional data into the db(somehow)
        msgId = "msg_1234"
        userID = "user_11"
        result = dbAdapter.deleteMessageForId(msgId,userID)
        assertEquals(true,result)

    }

    @Test
    fun test_doesPushNotificationIdExist_when_ABC_should_XYZ() {
       // dbAdapter.doesPushNotificationIdExist()
    }

    @Test
    fun test_fetchPushNotificationIds_when_ABC_should_XYZ() {
    }

    @Test
    fun test_fetchUserProfileById_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getLastUninstallTimestamp_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getMessages_when_ABC_should_XYZ() {
    }

    @Test
    fun test_markReadMessageForId_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeUserProfile_when_ABC_should_XYZ() {
    }

    @Test
    fun test_storeUninstallTimestamp_when_ABC_should_XYZ() {
    }

    @Test
    fun test_storeUserProfile_when_ABC_should_XYZ() {
    }

    @Test
    fun test_upsertMessages_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanUpPushNotifications_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanupEventsFromLastId_when_ABC_should_XYZ() {
    }

    @Test
    fun test_storePushNotificationId_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanupStaleEvents_when_ABC_should_XYZ() {
    }

    @Test
    fun test_fetchEvents_when_ABC_should_XYZ() {
    }

    @Test
    fun test_updatePushNotificationIds_when_ABC_should_XYZ() {
    }

    @Test
    fun test_storeObject_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeEvents_when_ABC_should_XYZ() {
    }
}