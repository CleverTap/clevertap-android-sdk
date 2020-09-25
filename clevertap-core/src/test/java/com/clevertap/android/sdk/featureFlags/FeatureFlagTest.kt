package com.clevertap.android.sdk.featureFlags

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeatureFlagTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testFetch() {
        Mockito.`when`(cleverTapAPI!!.featureFlag())
            .thenReturn(CTFeatureFlagsController(application, "12121", cleverTapInstanceConfig, cleverTapAPI))
        cleverTapAPI!!.featureFlag().fetchFeatureFlags()
        Mockito.verify(cleverTapAPI)!!.fetchFeatureFlags()
    }

    @Test
    fun testGet() {
        val ctFeatureFlagsController = Mockito.mock(CTFeatureFlagsController::class.java)
        Mockito.`when`(cleverTapAPI!!.featureFlag()).thenReturn(ctFeatureFlagsController)
        Mockito.`when`(ctFeatureFlagsController["isFeatureA", true]).thenReturn(false)
        Assert.assertFalse(cleverTapAPI!!.featureFlag()["isFeatureA", true])
    }
}