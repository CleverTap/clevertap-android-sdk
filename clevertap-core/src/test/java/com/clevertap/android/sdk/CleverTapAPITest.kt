package com.clevertap.android.sdk

import android.app.Activity
import android.os.Bundle
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapAPITest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun testActivity() {
        val activity = Mockito.mock(Activity::class.java)
        val bundle = Bundle()
        //create
        activity.onCreate(bundle, null)
        CleverTapAPI.onActivityCreated(activity, null)
    }
}