package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class LocalDataStoreTest : BaseTestCase() {
    private lateinit var localDataStoreDefConfig:  LocalDataStore
    private lateinit var localDataStore:  LocalDataStore

    override fun setUp() {
        super.setUp()
        localDataStoreDefConfig = LocalDataStore(appCtx, CleverTapInstanceConfig.createDefaultInstance(appCtx,"id","token","region"))
        localDataStore = LocalDataStore(appCtx, CleverTapInstanceConfig.createInstance(appCtx,"id","token","region"))
    }

    @Test
    fun test_changeUser_when_ABC_should_XYZ() {
        localDataStoreDefConfig.changeUser()
        // todo verify if resetLocalProfileSync is called

    }

    @Test
    fun test_getEventDetail_when_ABC_should_XYZ() {
        val details = localDataStoreDefConfig.getEventDetail("eventName")
        //todo verify if isPersonalisationEnabled() called
        // todo assert  isPersonalisationEnabled() returned false and verify getEventDetail returned null
        // todo assert  isPersonalisationEnabled() returned true and verify getEventDetail not returned null
        // todo for  localDataStoreDefConfig, verify if decodeEventDetails(eventName, getStringFromPrefs(eventName, null, namespace)); called with namespace = eventNamespace + ":" + this.config.getAccountId();
        // todo for  localDataStore, verify if decodeEventDetails(eventName, getStringFromPrefs(eventName, null, namespace)); called with namespace = eventNamespace;

    }

    @Test
    fun test_getEventHistory_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getProfileProperty_when_ABC_should_XYZ() {
    }

    @Test
    fun test_getProfileValueForKey_when_ABC_should_XYZ() {
    }

    @Test
    fun test_persistEvent_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeProfileField_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeProfileFields_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setDataSyncFlag_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setProfileField_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setProfileFields_when_ABC_should_XYZ() {
    }

    @Test
    fun test_syncWithUpstream_when_ABC_should_XYZ() {
    }


}