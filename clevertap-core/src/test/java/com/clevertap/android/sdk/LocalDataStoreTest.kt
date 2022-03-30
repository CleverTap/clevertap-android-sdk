package com.clevertap.android.sdk

import com.clevertap.android.sdk.events.EventDetail
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test;
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


@RunWith(RobolectricTestRunner::class)
class LocalDataStoreTest : BaseTestCase() {
    private lateinit var defConfig: CleverTapInstanceConfig
    private lateinit var config: CleverTapInstanceConfig

    private lateinit var localDataStoreWithDefConfig:  LocalDataStore
    private lateinit var localDataStoreWithConfig:  LocalDataStore
    private lateinit var localDataStoreWithConfigSpy: LocalDataStore

    override fun setUp() {
        super.setUp()
        defConfig = CleverTapInstanceConfig.createDefaultInstance(appCtx,"id","token","region")
        localDataStoreWithDefConfig = LocalDataStore(appCtx,defConfig)
        config = CleverTapInstanceConfig.createInstance(appCtx,"id","token","region")
        localDataStoreWithConfig = LocalDataStore(appCtx,config)
        localDataStoreWithConfigSpy = Mockito.spy(localDataStoreWithConfig)
    }

    @Test
    fun test_changeUser_when_ABC_should_XYZ() {
        localDataStoreWithDefConfig.changeUser()
        // since changeUser() is a void function calls resetLocalProfileSync() which is a private function,
        // we can't further test it or verify its calling
        assertTrue { true }
    }

    @Test
    fun test_getEventDetail_when_EventNameIsPassed_should_ReturnEventDetail() {
        //if configuration has personalisation disabled, getEventDetail() will return null
        defConfig.enablePersonalization(false)
        localDataStoreWithDefConfig.getEventDetail("eventName").let {
            println(it)
            assertNull(it)
        }

        //resetting personalisation for further tests :defConfig.enablePersonalization(true)

        //if default config is used, decodeEventDetails/getStringFromPrefs will be called with namespace = eventNamespace //todo how to check for private functions
        // if non default config is used,  decodeEventDetails/getStringFromPrefs will be called with namespace = eventNamespace + ":" + this.config.getAccountId();//todo how to check for private functions
        // todo how to cause error and check for exception?
    }

    @Test
    fun test_getEventHistory_when_FunctionIsCalled_should_ReturnAMapOfEventNameAndDetails() {
        var results :Map<String, EventDetail> = mutableMapOf()
        var eventDetail:EventDetail?=null

        //if default config is used, events are stored in local_events file
        StorageHelper.getPreferences(appCtx,"local_events").edit().putString("event","2|1648192535|1648627865").commit()
        results = localDataStoreWithDefConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull( results["event"] )
        assertTrue { results["event"] is EventDetail }
        assertEquals( 2, results["event"]?.count )
        assertEquals( 1648192535, results["event"]?.firstTime )
        assertEquals( 1648627865, results["event"]?.lastTime )


        //if  default instance is not used, events are stored in "local_events:$accountID"
        StorageHelper.getPreferences(appCtx,"local_events:id").edit().putString("event2","33|1234|2234").commit()
        results = localDataStoreWithConfig.getEventHistory(appCtx)
        assertTrue { results.isNotEmpty() }
        assertNotNull( results["event2"] )
        assertTrue { results["event2"] is EventDetail }
        assertEquals( 33, results["event2"]?.count )
        assertEquals( 1234, results["event2"]?.firstTime )
        assertEquals( 2234, results["event2"]?.lastTime )

    }

    @Test
    fun test_getProfileProperty_when_FunctionIsCalledWithSomeKey_should_CallGetProfileValueForKey() {
        localDataStoreWithConfigSpy.getProfileProperty("key")
        Mockito.verify(localDataStoreWithConfigSpy,Mockito.times(1)).getProfileValueForKey("key")
    }

    @Test
    fun test_getProfileValueForKey_when_FunctionIsCalledWithSomeKey_should_ReturnAssociatedValue() {
        // since getProfileValueForKey() calls _getProfileProperty() which is a private function,
        // we can't further test it or verify its calling. we can only verify the working of _getProfileProperty
        localDataStoreWithConfig.getProfileProperty(null).let {
            println(it)
            assertNull(it)
        }
        localDataStoreWithConfig.setProfileField("key","val")
        localDataStoreWithConfig.getProfileProperty("key").let {
            println(it)
            assertNotNull(it)
            assertEquals("val",it)
        }

    }

    @Test
    fun test_persistEvent_when_ABC_should_XYZ() {
       // localDataStoreWithConfig.persistEvent(appCtx,)
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