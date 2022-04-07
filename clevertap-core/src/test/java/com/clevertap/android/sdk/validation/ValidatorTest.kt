package com.clevertap.android.sdk.validation

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ValidatorTest : BaseTestCase() {
    private lateinit var validator: Validator

    override fun setUp() {
        super.setUp()
        validator = Validator()
    }

    @Test
    fun test_cleanEventName_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanMultiValuePropertyKey_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanMultiValuePropertyValue_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanObjectKey_when_ABC_should_XYZ() {
    }

    @Test
    fun test_cleanObjectValue_when_ABC_should_XYZ() {
    }

    @Test
    fun test_isEventDiscarded(): Unit {
    }

    @Test
    fun test_isRestrictedEventName(): Unit {
    }

    @Test
    fun test_mergeMultiValuePropertyForKey_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setDiscardedEvents_when_ABC_should_XYZ() {
    }
}