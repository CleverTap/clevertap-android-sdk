package com.clevertap.android.sdk;

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*;
import org.robolectric.RobolectricTestRunner;
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class CleverTapFactoryTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_getCoreState_returnsNonNull(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState)
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullCoreMetaData(){
        val coreState = CleverTapFactory.getCoreState(application, cleverTapInstanceConfig, "12345")
        assertNotNull(coreState.coreMetaData)
    }

}
