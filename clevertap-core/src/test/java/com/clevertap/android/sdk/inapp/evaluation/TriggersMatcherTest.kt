package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.mockito.*

class TriggersMatcherTest : BaseTestCase() {

    @Mock
    lateinit var eventAdapter: EventAdapter
    private lateinit var triggersMatcher: TriggersMatcher

    @Before
    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)
        triggersMatcher = TriggersMatcher()
    }

    @Test
    fun `test matching event with a single trigger`() {
        val triggersMatcher = TriggersMatcher()
        val triggerJson = JSONObject()
        triggerJson.put("eventName", "TestEvent")
        val triggerArray = JSONArray()
        triggerArray.put(triggerJson)

        //`when`(eventAdapter.eventName).thenReturn("TestEvent")

        val matchResult = triggersMatcher.matchEvent(triggerArray, "TestEvent", emptyMap())
        assertTrue(matchResult)
    }

    @Test
    fun `test non-matching event with a single trigger`() {
        val triggersMatcher = TriggersMatcher()
        val triggerJson = JSONObject()
        triggerJson.put("eventName", "TestEvent")
        val triggerArray = JSONArray()
        triggerArray.put(triggerJson)

//        `when`(eventAdapter.eventName).thenReturn("AnotherEvent")

        val matchResult = triggersMatcher.matchEvent(triggerArray, "AnotherEvent", emptyMap())
        assertFalse(matchResult)
    }

    @Test
    fun testMatchEvent_WhenMultipleTriggersExistAndOneEventMatches_ShouldReturnTrue() {
        val whenTriggers = JSONArray()
        val eventProperties = mapOf("Property1" to "Value1")

        // Adding a trigger with one condition that matches the event
        val triggerJSON1 = JSONObject().apply {
            put("eventName", "EventA")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property2")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value2")
            }))
        }
        whenTriggers.put(triggerJSON1)

        // Adding another trigger with a different event name (should not match)
        val triggerJSON2 = JSONObject().apply {
            put("eventName", "EventB")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value1")
            }))
        }
        whenTriggers.put(triggerJSON2)

        assertTrue(triggersMatcher.matchEvent(whenTriggers, "EventB", eventProperties))
    }

    @Test
    fun testMatchEvent_WhenMultipleTriggersExistAndNoEventMatches_ShouldReturnFalse() {
        val whenTriggers = JSONArray()
        val eventProperties = mapOf("Property1" to "Value1")

        // Adding a trigger with one condition that matches the event
        val triggerJSON1 = JSONObject().apply {
            put("eventName", "EventA")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property2")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value2")
            }))
        }
        whenTriggers.put(triggerJSON1)

        // Adding another trigger with a different event name (should not match)
        val triggerJSON2 = JSONObject().apply {
            put("eventName", "EventB")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value1")
            }))
        }
        whenTriggers.put(triggerJSON2)

        assertFalse(triggersMatcher.matchEvent(whenTriggers, "EventX", eventProperties))
    }

    @Test
    fun testMatchChargedEvent_WhenMultipleTriggersExistAndOneEventMatches_ShouldReturnTrue() {
        val whenTriggers = JSONArray()
        val details = mapOf("Property1" to "Value1")
        val items = listOf(
            mapOf("ItemProperty1" to "ItemValue1", "ItemProperty2" to "SomeItemValue2")
        )

        // Adding a trigger with one condition that should not match the charged event (should not match)
        val triggerJSON1 = JSONObject().apply {
            put("eventName", "ChargedEvent")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value9")
            }))
            put("itemProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "ItemProperty1")
                put("op", TriggerOperator.Contains.operatorValue)
                put("value", "nike")
            }))
        }
        whenTriggers.put(triggerJSON1)

        // Adding another trigger with a charged event name (should match)
        val triggerJSON2 = JSONObject().apply {
            put("eventName", "ChargedEvent")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "Value1")
            }))
            put("itemProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "ItemProperty1")
                put("op", TriggerOperator.Contains.operatorValue)
                put("value", "ItemValue1")
            }))
        }
        whenTriggers.put(triggerJSON2)

        assertTrue(triggersMatcher.matchChargedEvent(whenTriggers, "ChargedEvent", details, items))
    }

    @Test
    fun testMatchChargedEvent_WhenMultipleTriggersExistAndNoEventMatches_ShouldReturnFalse() {
        val whenTriggers = JSONArray()
        val details = mapOf("Property1" to "Value1")
        val items = listOf(
            mapOf("ItemProperty1" to "DifferentItemValue1", "ItemProperty2" to "SomeItemValue2")
        )

        // Adding a trigger with a different event condition (should not match)
        val triggerJSON1 = JSONObject().apply {
            put("eventName", "ChargedEvent")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "DifferentValue")
            }))
            put("itemProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "ItemProperty1")
                put("op", TriggerOperator.Contains.operatorValue)
                put("value", "nike")
            }))
        }
        whenTriggers.put(triggerJSON1)

        // Adding another trigger with a charged event name (should not match)
        val triggerJSON2 = JSONObject().apply {
            put("eventName", "ChargedEvent")
            put("eventProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "Property1")
                put("op", TriggerOperator.Equals.operatorValue)
                put("value", "hello")
            }))
            put("itemProperties", JSONArray().put(JSONObject().apply {
                put("propertyName", "ItemProperty1")
                put("op", TriggerOperator.Contains.operatorValue)
                put("value", "world")
            }))
        }
        whenTriggers.put(triggerJSON2)

        assertFalse(triggersMatcher.matchChargedEvent(whenTriggers, "ChargedEvent", details, items))
    }

    @Test
    fun testMatchEvent_WhenNoTriggerConditions_ShouldReturnFalse() {
        val whenTriggers = JSONArray()
        val eventName = "EventA"
        val eventProperties = mapOf("Property1" to "Value1")

        assertFalse(triggersMatcher.matchEvent(whenTriggers, eventName, eventProperties))
    }

    @Test
    fun testMatchChargedEvent_WhenNoTriggerConditions_ShouldReturnFalse() {
        val whenTriggers = JSONArray()
        val eventName = "ChargedEvent"
        val eventProperties = mapOf("Property1" to "Value1")

        assertFalse(
            triggersMatcher.matchChargedEvent(
                whenTriggers, eventName, eventProperties,
                listOf()
            )
        )
    }


    @Test
    fun `test actualIsInRangeOfExpected when actual value within the expected range`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0, 10.0))
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when actual value outside the expected range`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0, 10.0))
        val actualValue = TriggerValue(15.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when invalid expected values`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("5.0", "10.0")) // Invalid expected values
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when actual value as a list`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0, 10.0))
        val actualValue = TriggerValue(listOf(7.0, 8.0, 9.0))

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when actual value not a number`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0, 10.0))
        val actualValue = TriggerValue("NotANumber")

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list is null`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(null) // Expected list is null
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list is empty`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(emptyList<Double>()) // Expected list is empty
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list has one element`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0)) // Expected list has one element
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list has three elements`() {
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(1.0, 5.0, 10.0)) // Expected list has three elements
        val actualValue = TriggerValue(3.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list with string and double values (string first)`() {
        // Test when the expected list has a mix of string and double values with the string value first.
        // Expects a negative result as the actual value is not a string.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("text", 10.0))
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualIsInRangeOfExpected when expected list with string and double values (double first)`() {
        // Test when the expected list has a mix of string and double values with the double value first.
        // Expects a negative result as the actual value is not a string.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(10.0, "text"))
        val actualValue = TriggerValue(7.0)

        val result = triggersMatcher.actualIsInRangeOfExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual string contains expected string`() {
        // Test when the actual string contains the expected string.
        // Expects a positive result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("apple")
        val actualValue = TriggerValue("pineapple")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual string does not contain expected string`() {
        // Test when the actual string does not contain the expected string.
        // Expects a negative result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("banana")
        val actualValue = TriggerValue("apple")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actual string contains one of the expected strings`() {
        // Test when the actual string contains one of the expected strings.
        // Expects a positive result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue("banana")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual string does not contain any of the expected strings`() {
        // Test when the actual string does not contain any of the expected strings.
        // Expects a negative result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue("grape")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual list contains expected string`() {
        // Test when the actual list contains the expected string.
        // Expects a positive result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("apple")
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual list does not contain expected string`() {
        // Test when the actual list does not contain the expected string.
        // Expects a negative result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("grape")
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual list contains expected string in expected list`() {
        // Test when the actual list contains the expected string.
        // Expects a positive result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple"))
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual list does not contain expected string in expected list`() {
        // Test when the actual list does not contain the expected string.
        // Expects a negative result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("grape"))
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual list contains all expected strings`() {
        // Test when the actual list contains all of the expected strings.
        // Expects a positive result.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana"))
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual list does not contain all expected strings`() {
        // Test when the actual list does not contain all of the expected strings.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "grape"))
        val actualValue = TriggerValue(listOf("banana", "apple", "cherry"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual list contains expected number`() {
        // Test when the actual list contains the expected number.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0))
        val actualValue = TriggerValue(listOf(3.0, 5.0, 7.0))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual list of string contains expected number`() {
        // Test when the actual list contains the expected number.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("5.0"))
        val actualValue = TriggerValue(listOf(3.0, 5.0, 7.0))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when expected list of string contains actual number`() {
        // Test when the actual list contains the expected number.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf(5.0))
        val actualValue = TriggerValue(listOf("3.0", "5.0", "7.0"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual number equals expected number`() {
        // Test when the actual number equals the expected number.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }


    @Test
    fun `test actualContainsExpected when actual and expected both null`() {
        // Test when both the actual and expected values are null
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue(null)

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual is string and expected null`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue("5.0")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual is null and expected is string`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("5.0")
        val actualValue = TriggerValue(null)

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual is null and expected is list`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("5.0"))
        val actualValue = TriggerValue(null)

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual is list and expected is null`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue(listOf("5.0"))

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual contains expected - empty string vs non-empty string`() {
        // Test when the actual string is a non-empty string, and the expected string is an empty string.
        // Expects a positive result, indicating that the non-empty string contains the empty string.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("")
        val actualValue = TriggerValue("Hello")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test actualContainsExpected when actual does not contain expected (non-empty string vs empty string)`() {
        // Test when the actual string is an empty string, and the expected string is a non-empty string.
        // Expects a negative result, indicating that an empty string does not contain a non-empty string.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("Hello")
        val actualValue = TriggerValue("")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test actualContainsExpected when actual and expected both empty strings`() {
        // Test when both the actual and expected strings are empty strings.
        // Expects a positive result, indicating that an empty string contains another empty string.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("")
        val actualValue = TriggerValue("")

        val result = triggersMatcher.actualContainsExpected(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected list equals actual list`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue(listOf("cherry", "banana", "apple"))

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when  expected list does not equal actual list`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue(listOf("grape", "apple", "banana"))

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in expected list equals actual string`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue("banana")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in expected list not equals actual string`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue("orange")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in expected list not equals actual null string`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val actualValue = TriggerValue(null)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in actual list equals expected string`() {

        val triggersMatcher = TriggersMatcher()
        val actualValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val expectedValue = TriggerValue("banana")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in actual list not equals expected string`() {

        val triggersMatcher = TriggersMatcher()
        val actualValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val expectedValue = TriggerValue("orange")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when any element in actual list not equals expected null string`() {

        val triggersMatcher = TriggersMatcher()
        val actualValue = TriggerValue(listOf("apple", "banana", "cherry"))
        val expectedValue = TriggerValue(null)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test  expectedValueEqualsActual when expected string equals actual string`() {
        // Test when both the expected and actual values are strings, and they are equal.
        // Expects a positive result, indicating that the strings are equal.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("apple")
        val actualValue = TriggerValue("apple")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test  expectedValueEqualsActual when expected string and actual list of numbers`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("apple")
        val actualValue = TriggerValue(listOf(1, 2, 3, 4))

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test  expectedValueEqualsActual when actual string does not equals expected null string`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue("apple")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected number equals actual number`() {
        // Test when both the expected and actual values are numbers (doubles), and they are equal.
        // Expects a positive result, indicating that the numbers are equal.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected number does not equal actual number`() {
        // Test when both the expected and actual values are numbers (doubles), but they are not equal.
        // Expects a negative result, indicating that the numbers are not equal.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(8.0)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected string equals actual number (converted)`() {
        // Test when the expected value is a string, and the actual value is a number (converted from string) and they are equal.
        // Expects a positive result, indicating that the string is equal to the number.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("5.0")
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when actual string equals expected number (converted)`() {

        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue("5.0")

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected value is not equal to actual value (various types)`() {
        // Test when the expected and actual values have different types and are not equal.
        // Expects a negative result, indicating that the values are not equal.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue("apple")
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected empty list equals actual empty list`() {
        // Test when both the expected and actual values are empty lists.
        // Expects a positive result, indicating that empty lists are equal.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(emptyList<String>())
        val actualValue = TriggerValue(emptyList<String>())

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected empty list does not equal actual non-empty list`() {
        // Test when the expected value is an empty list, and the actual value is a non-empty list.
        // Expects a negative result, indicating that an empty list is not equal to a non-empty list.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(emptyList<String>())
        val actualValue = TriggerValue(listOf("apple", "banana"))

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test expectedValueEqualsActual when expected non-empty list does not equal actual empty list`() {
        // Test when the expected value is a non-empty list, and the actual value is an empty list.
        // Expects a negative result, indicating that a non-empty list is not equal to an empty list.
        val triggersMatcher = TriggersMatcher()
        val expectedValue = TriggerValue(listOf("apple", "banana"))
        val actualValue = TriggerValue(emptyList<String>())

        val result = triggersMatcher.expectedValueEqualsActual(expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with Set operator and actual is null`() {

        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Set
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue(null)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with Set operator and actual is not null`() {
        // Test when the operator is Set, and the actual value is not null.
        // Expects a positive result, as Set matches when the actual value exists (not null).
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Set
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue("SomeValue")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with LessThan operator (numbers)`() {
        // Test when the operator is LessThan, and both expected and actual values are numbers.
        // Expects a positive result, as the expected number is less than the actual number.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.LessThan
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(10.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with LessThan operator (invalid)`() {
        // Test when the operator is LessThan, but the actual value is not a number.
        // Expects a negative result, as the operator should not apply to non-numeric values.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.LessThan
        val expectedValue = TriggerValue(10.0)
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with LessThan operator (expected invalid)`() {

        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.LessThan
        val expectedValue = TriggerValue("invalid")
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with LessThan operator (expected null)`() {

        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.LessThan
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with GreaterThan operator (numbers)`() {
        // Test when the operator is GreaterThan, and both expected and actual values are numbers.
        // Expects a positive result, as the expected number is greater than the actual number.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.GreaterThan
        val expectedValue = TriggerValue(10.0)
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with GreaterThan operator (invalid)`() {
        // Test when the operator is GreaterThan, but the actual value is not a number.
        // Expects a negative result, as the operator should not apply to non-numeric values.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.GreaterThan
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with GreaterThan operator (expected invalid)`() {

        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.GreaterThan
        val expectedValue = TriggerValue("Invalid")
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with GreaterThan operator (expected null)`() {

        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.GreaterThan
        val expectedValue = TriggerValue(null)
        val actualValue = TriggerValue("Invalid")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with Equals operator (strings)`() {
        // Test when the operator is Equals, and both expected and actual values are strings.
        // Expects a positive result, as the strings are equal.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Equals
        val expectedValue = TriggerValue("Hello")
        val actualValue = TriggerValue("Hello")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with Equals operator (numbers)`() {
        // Test when the operator is Equals, and both expected and actual values are numbers.
        // Expects a positive result, as the numbers are equal.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Equals
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with NotEquals operator (strings)`() {
        // Test when the operator is NotEquals, and both expected and actual values are strings.
        // Expects a negative result, as the strings are equal, and NotEquals should be false in this case.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.NotEquals
        val expectedValue = TriggerValue("Hello")
        val actualValue = TriggerValue("Hello")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with NotEquals operator (numbers)`() {
        // Test when the operator is NotEquals, and both expected and actual values are numbers.
        // Expects a negative result, as the numbers are equal, and NotEquals should be false in this case.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.NotEquals
        val expectedValue = TriggerValue(5.0)
        val actualValue = TriggerValue(5.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with Between operator (numbers)`() {
        // Test when the operator is Between, and both expected and actual values are numbers within the range.
        // Expects a positive result, as the actual number is within the expected range.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Between
        val expectedValue = TriggerValue(listOf(5.0, 10.0))
        val actualValue = TriggerValue(8.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with Between operator (invalid)`() {
        // Test when the operator is Between, but the expected value is not a valid range.
        // Expects a negative result, as the operator should not apply to an invalid range.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Between
        val expectedValue = TriggerValue(listOf("Invalid", "Range"))
        val actualValue = TriggerValue(8.0)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with Contains operator (strings)`() {
        // Test when the operator is Contains, and both expected and actual values are strings.
        // Expects a positive result, as the actual string contains the expected string.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Contains
        val expectedValue = TriggerValue("world")
        val actualValue = TriggerValue("Hello, world!")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun `test evaluate with Contains operator (invalid)`() {
        // Test when the operator is Contains, but the actual value is not a string.
        // Expects a negative result, as the operator should not apply to non-string values.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.Contains
        val expectedValue = TriggerValue("world")
        val actualValue = TriggerValue(123)

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with NotContains operator (strings)`() {
        // Test when the operator is NotContains, and both expected and actual values are strings.
        // Expects a negative result, as the actual string contains the expected string, and NotContains should be false.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.NotContains
        val expectedValue = TriggerValue("world")
        val actualValue = TriggerValue("Hello, world!")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertFalse(result)
    }

    @Test
    fun `test evaluate with NotContains operator (invalid)`() {
        // Test when the operator is NotContains, but the actual value is not a string.
        // Expects a negative result, as the operator should not apply to non-string values.
        val triggersMatcher = TriggersMatcher()
        val operator = TriggerOperator.NotContains
        val expectedValue = TriggerValue("world")
        val actualValue = TriggerValue("hello")

        val result = triggersMatcher.evaluate(operator, expectedValue, actualValue)
        assertTrue(result)
    }

    @Test
    fun testMatch_WhenEventNameDoesNotMatch_ShouldReturnFalse() {
        val trigger = createTriggerAdapter("EventA")
        val event = createEventAdapter("EventB")

        assertFalse(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenEventNameMatchesAndNoProperties_ShouldReturnTrue() {
        val trigger = createTriggerAdapter("EventA")
        val event = createEventAdapter("EventA")

        assertTrue(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenEventNameMatchesAndPropertyConditionsAreMet_ShouldReturnTrue() {
        val trigger = createTriggerAdapter(
            "EventA", listOf(
                TriggerCondition("Property1", TriggerOperator.Equals, TriggerValue("Value1")),
                TriggerCondition("Property2", TriggerOperator.Contains, TriggerValue("Value2"))
            )
        )
        val event = createEventAdapter(
            "EventA", mapOf(
                "Property1" to "Value1",
                "Property2" to "SomeValue2"
            )
        )

        assertTrue(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenEventNameMatchesAndPropertyConditionsAreNotMet_ShouldReturnFalse() {
        val trigger = createTriggerAdapter(
            "EventA", listOf(
                TriggerCondition("Property1", TriggerOperator.Equals, TriggerValue("Value1")),
                TriggerCondition("Property2", TriggerOperator.Contains, TriggerValue("Value2"))
            )
        )
        val event = createEventAdapter(
            "EventA", mapOf(
                "Property1" to "DifferentValue1",
                "Property2" to "SomeValue2"
            )
        )

        assertFalse(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenChargedEventItemPropertyConditionsAreMet_ShouldReturnTrue() {
        val trigger = createTriggerAdapter(
            "ChargedEvent", listOf(), listOf(
                TriggerCondition(
                    "ItemProperty1",
                    TriggerOperator.Equals,
                    TriggerValue("ItemValue1")
                ),
                TriggerCondition(
                    "ItemProperty2",
                    TriggerOperator.Contains,
                    TriggerValue("ItemValue2")
                )
            )
        )
        val event = createEventAdapter(
            "ChargedEvent", emptyMap(), listOf(
                mapOf("ItemProperty1" to "ItemValue1", "ItemProperty2" to "SomeItemValue2"),
                mapOf("ItemProperty1" to "DifferentItemValue1", "ItemProperty2" to "ItemValue2")
            )
        )

        assertTrue(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenChargedEventItemPropertyConditionsAreNotMet_ShouldReturnFalse() {
        val trigger = createTriggerAdapter(
            "ChargedEvent", listOf(), listOf(
                TriggerCondition(
                    "ItemProperty1",
                    TriggerOperator.Equals,
                    TriggerValue("ItemValue1")
                ),
                TriggerCondition(
                    "ItemProperty2",
                    TriggerOperator.Contains,
                    TriggerValue("ItemValue2")
                )
            )
        )
        val event = createEventAdapter(
            "ChargedEvent", emptyMap(), listOf(
                mapOf(
                    "ItemProperty1" to "DifferentItemValue1",
                    "ItemProperty2" to "SomeItemValue2"
                ),
                mapOf(
                    "ItemProperty1" to "AnotherItemValue1",
                    "ItemProperty2" to "AnotherItemValue2"
                )
            )
        )

        assertFalse(triggersMatcher.match(trigger, event))
    }


    @Test
    fun testMatch_WhenEventNameMatchesAndBothPropertyTypesAreEvaluated_ShouldReturnTrue() {
        val trigger = createTriggerAdapter(
            "ChargedEvent", listOf(
                // Event property condition
                TriggerCondition("Property1", TriggerOperator.Equals, TriggerValue("Value1"))
            ),
            listOf(
                // Item property condition
                TriggerCondition(
                    "ItemProperty1",
                    TriggerOperator.Contains,
                    TriggerValue("ItemValue1")
                )
            )
        )
        val event = createEventAdapter(
            "ChargedEvent", mapOf(
                "Property1" to "Value1"
            ), listOf(
                mapOf("ItemProperty1" to "ItemValue1", "ItemProperty2" to "SomeItemValue2")
            )
        )

        assertTrue(triggersMatcher.match(trigger, event))
    }

    @Test
    fun testMatch_WhenBothPropertyTypesAreEvaluatedButConditionsAreNotMet_ShouldReturnFalse() {
        val trigger = createTriggerAdapter(
            "ChargedEvent", listOf(
                // Event property condition
                TriggerCondition("Property1", TriggerOperator.Equals, TriggerValue("Value1")),
            ), listOf(
                // Item property condition
                TriggerCondition(
                    "ItemProperty1",
                    TriggerOperator.Contains,
                    TriggerValue("ItemValue1")
                )
            )
        )
        val event = createEventAdapter(
            "ChargedEvent", mapOf(
                "Property1" to "DifferentValue1"
            ), listOf(
                mapOf(
                    "ItemProperty1" to "AnotherItemValue1",
                    "ItemProperty2" to "AnotherItemValue2"
                )
            )
        )

        assertFalse(triggersMatcher.match(trigger, event))
    }

    // Helper functions to create EventAdapter and TriggerAdapter instances
    private fun createEventAdapter(
        eventName: String,
        eventProperties: Map<String, Any> = emptyMap(),
        items: List<Map<String, Any>> = emptyList()
    ): EventAdapter {
        return EventAdapter(eventName, eventProperties, items)
    }

    private fun createTriggerAdapter(
        eventName: String,
        propertyConditions: List<TriggerCondition> = emptyList(),
        itemConditions: List<TriggerCondition> = emptyList()
    ): TriggerAdapter {
        val triggerJSON = JSONObject().apply {
            put("eventName", eventName)
            if (propertyConditions.isNotEmpty()) {
                put(
                    "eventProperties",
                    JSONArray(propertyConditions.map { createPropertyConditionJSON(it) })
                )
            }
            if (itemConditions.isNotEmpty()) {
                put(
                    "itemProperties",
                    JSONArray(itemConditions.map { createPropertyConditionJSON(it) })
                )
            }
        }
        return TriggerAdapter(triggerJSON)
    }

    private fun createPropertyConditionJSON(condition: TriggerCondition): JSONObject {
        return JSONObject().apply {
            put("propertyName", condition.propertyName)
            put("operator", condition.op.operatorValue)
            put("value", condition.value.value)
        }
    }

}
