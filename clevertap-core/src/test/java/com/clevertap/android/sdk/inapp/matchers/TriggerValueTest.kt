package com.clevertap.android.sdk.inapp.matchers

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TriggerValueTest {

    @Test
    fun testStringValue() {
        val triggerValue = TriggerValue("Hello")
        assertEquals("Hello", triggerValue.stringValue())
        assertEquals(.0, triggerValue.numberValue())
        assertFalse(triggerValue.isList())
        assertNull(triggerValue.listValue())
    }

    @Test
    fun testNumberValue() {
        val triggerValue = TriggerValue(42)
        assertEquals("", triggerValue.stringValue())
        assertFalse(triggerValue.isList())
        assertNull(triggerValue.listValue())
        assertEquals(42, triggerValue.numberValue())
    }

    @Test
    fun testListValue() {
        val list = listOf(1, 2, 3)
        val triggerValue = TriggerValue(list)
        assertEquals("", triggerValue.stringValue())
        assertEquals(.0, triggerValue.numberValue())
        assertTrue(triggerValue.isList())
        assertEquals(list, triggerValue.listValue())
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
        assertEquals("", triggerValue.stringValue())
    }

    @Test
    fun testNumberValueWithNull() {
        val triggerValue = TriggerValue(null)
        assertEquals(.0, triggerValue.numberValue())
    }

    @Test
    fun testListValueWithNull() {
        val triggerValue = TriggerValue(null)
        assertNull(triggerValue.listValue())
    }
}
