package com.clevertap.android.sdk.inapp.evaluation

import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TriggerValueTest {

    @Test
    fun testStringValue() {
        val triggerValue = TriggerValue(" HellO ")
        assertEquals(" HellO ", triggerValue.stringValue())
        assertEquals("hello", triggerValue.stringValueCleaned())
        assertNull(triggerValue.numberValue())
        assertFalse(triggerValue.isList())
        assertNull(triggerValue.listValue())
    }

    @Test
    fun testNumberValue() {
        val triggerValue = TriggerValue(42)
        assertNull(triggerValue.stringValue())
        assertFalse(triggerValue.isList())
        assertNull(triggerValue.listValue())
        assertEquals(42, triggerValue.numberValue())
    }

    @Test
    fun testListValue() {
        val list = listOf(1, 2, 3)
        val triggerValue = TriggerValue(list)
        assertNull(triggerValue.stringValue())
        assertNull(triggerValue.numberValue())
        assertTrue(triggerValue.isList())
        assertEquals(list, triggerValue.listValue())
    }

    @Test
    fun testListValueWithUpperCase() {
        val list = listOf("APPLE", "Banana", "Cherry")
        val triggerValue = TriggerValue(list)
        assertNull(triggerValue.stringValue())
        assertNull(triggerValue.numberValue())
        assertTrue(triggerValue.isList())

        val expected = listOf("apple", "banana", "cherry")
        assertEquals(expected, triggerValue.listValueWithCleanedStringIfPresent())
        assertEquals(list, triggerValue.listValue())
    }

    @Test
    fun testListValueWithUpperCaseAndExtraLeadingAndTrailingSpace() {
        val list = listOf("  APPLE  ", "  Banana  ", "Cherry")
        val triggerValue = TriggerValue(list)
        assertNull(triggerValue.stringValue())
        assertNull(triggerValue.numberValue())
        assertTrue(triggerValue.isList())

        val expected = listOf("apple", "banana", "cherry")
        assertEquals(list, triggerValue.listValue())
        assertEquals(expected, triggerValue.listValueWithCleanedStringIfPresent())
    }

    @Test
    fun `testListValueWith Number And String With UpperCase And Extra Leading And Trailing Space`() {
        val list = listOf(1, "  Banana  ", "Cherry", 2, 4.44)
        val triggerValue = TriggerValue(list)
        assertNull(triggerValue.stringValue())
        assertNull(triggerValue.numberValue())
        assertTrue(triggerValue.isList())

        val expected = listOf(1, "banana", "cherry", 2, 4.44)
        assertEquals(list, triggerValue.listValue())
        assertEquals(expected, triggerValue.listValueWithCleanedStringIfPresent())
    }

    @Test
    fun testIsListWithList() {
        val list = listOf(1, 2, 3)
        val triggerValue = TriggerValue(list)
        assertTrue(triggerValue.isList())
    }

    @Test
    fun testIsListWithNullList() {
        val triggerValue = TriggerValue(null)
        assertFalse(triggerValue.isList())
    }

    @Test
    fun testIsListWithString() {
        val triggerValue = TriggerValue("Hello")
        assertFalse(triggerValue.isList())
    }

    @Test
    fun testIsListWithNumber() {
        val triggerValue = TriggerValue(42)
        assertFalse(triggerValue.isList())
    }

    @Test
    fun testStringValueWithNull() {
        val triggerValue = TriggerValue(null)
        assertNull(triggerValue.stringValue())
    }

    @Test
    fun testNumberValueWithNull() {
        val triggerValue = TriggerValue(null)
        assertNull(triggerValue.numberValue())
    }

    @Test
    fun testListValueWithNull() {
        val triggerValue = TriggerValue(null)
        assertNull(triggerValue.listValue())
    }
}
