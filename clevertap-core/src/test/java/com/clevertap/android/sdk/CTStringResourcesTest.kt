package com.clevertap.android.sdk

import android.content.res.Resources
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.jupiter.api.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class CTStringResourcesTest : BaseTestCase() {

    @Test
    fun test_destructuring_declaration_for_CTStringResources_when_passed_1_string_id_should_return_value_and_rest_must_be_null() {
        val (s1, s2, s3, s4, s5) = CTStringResources(application, R.string.ct_txt_cancel)
        assertEquals("Cancel", s1)
        listOf(s2, s3, s4, s5).forEach { assertNull(it) }
    }

    @Test
    fun test_destructuring_declaration_for_CTStringResources_when_passed_2_string_id_should_return_value_and_rest_must_be_null() {
        val (s1, s2, s3, s4, s5) = CTStringResources(
            application,
            R.string.ct_txt_cancel,
            R.string.ct_permission_not_available_title
        )
        assertEquals("Cancel", s1)
        assertEquals("Permission Not Available", s2)
        listOf(s3, s4, s5).forEach { assertNull(it) }
    }

    @Test
    fun test_destructuring_declaration_for_CTStringResources_when_passed_5_string_id_should_return_value() {
        val (s1, s2, s3, s4, s5) = CTStringResources(
            application,
            R.string.ct_txt_cancel,
            R.string.ct_txt_cancel,
            R.string.ct_txt_cancel,
            R.string.ct_txt_cancel,
            R.string.ct_txt_cancel
        )

        listOf(s1, s2, s3, s4, s5).forEach { assertEquals("Cancel", it) }
    }

    @Test
    fun test_destructuring_declaration_for_CTStringResources_when_passed_0_string_id_should_return_all_value_null() {
        val (s1, s2, s3, s4, s5) = CTStringResources(
            application
        )

        listOf(s1, s2, s3, s4, s5).forEach { assertNull(it) }
    }

    @Test
    fun test_destructuring_declaration_for_CTStringResources_when_passed_invalid_string_id_should_throw_exception() {
        assertThrows<Resources.NotFoundException> {
            val (s1, s2, s3, s4, s5) = CTStringResources(
                application, -1, -1, -1, -1, -1
            )
        }
    }
}