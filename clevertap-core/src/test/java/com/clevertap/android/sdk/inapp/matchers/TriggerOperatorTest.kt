package com.clevertap.android.sdk.inapp.matchers

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerOperatorTest {

    @Test
    fun testFromOperatorValueWithInvalidValue() {
        // Arrange
        val invalidValue = 100 // An invalid value.

        // Act
        val triggerOperator = TriggerOperator.fromOperatorValue(invalidValue)

        // Assert
        assertEquals(TriggerOperator.Equals, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithGreaterThan() {
        val operatorValue = 0
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.GreaterThan, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithEquals() {
        val operatorValue = 1
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.Equals, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithLessThan() {
        val operatorValue = 2
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.LessThan, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithContains() {
        val operatorValue = 3
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.Contains, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithBetween() {
        val operatorValue = 4
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.Between, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithNotEquals() {
        val operatorValue = 15
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.NotEquals, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithSet() {
        val operatorValue = 26
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.Set, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithNotSet() {
        val operatorValue = 27
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.NotSet, triggerOperator)
    }

    @Test
    fun testFromOperatorValueWithNotContains() {
        val operatorValue = 28
        val triggerOperator = TriggerOperator.fromOperatorValue(operatorValue)
        assertEquals(TriggerOperator.NotContains, triggerOperator)
    }
}
