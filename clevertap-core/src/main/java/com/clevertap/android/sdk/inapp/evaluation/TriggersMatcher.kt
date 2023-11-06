package com.clevertap.android.sdk.inapp.evaluation

import androidx.annotation.VisibleForTesting
import org.json.JSONArray
import org.json.JSONObject

/**
 * The `TriggersMatcher` class provides methods for matching trigger conditions with event properties,
 * both for standard events and charged events. It allows you to determine whether a given event
 * satisfies the conditions specified in a set of triggers.
 *
 * @constructor Creates an instance of the `TriggersMatcher` class.
 */
class TriggersMatcher {

    /**
     * Matches a standard event against a set of trigger conditions.
     *
     * This function evaluates the trigger conditions for a standard event and returns `true`
     * if all conditions within any of the events are met. The events in the `whenTriggers`
     * list are checked in an OR-ed manner, meaning that if any event matches, and all conditions
     * within that event are met, the function returns `true`.
     *
     * @param whenTriggers A list of event triggers with conditions to match against the event.
     * @param eventName The name of the event to be matched.
     * @param eventProperties A map of event properties where keys are property names and
     *        values are property values.
     * @return `true` if any event matches, and all conditions
     * within that event are met, `false` otherwise.
     */
    fun matchEvent(
        whenTriggers: List<TriggerAdapter>,
        eventName: String,
        eventProperties: Map<String, Any>
    ): Boolean {

        // events in array are OR-ed
        val event = EventAdapter(eventName, eventProperties)
        // Check if any TriggerAdapter in the list matches the event
        return whenTriggers.any { match(it, event) }
    }

    /**
     * Matches a charged event against a set of trigger conditions.
     *
     * This function evaluates the trigger conditions for a charged event and returns `true`
     * if all conditions within any of the charged events are met. The events in the `whenTriggers`
     * array are checked in an OR-ed manner, meaning that if any charged event with all conditions
     * within that event are met, the function returns `true`.
     *
     * @param whenTriggers A JSON array of event triggers with conditions to match against the charged event.
     * @param eventName The name of the charged event to be matched.
     * @param details A map of event details or properties where keys are property names and
     *        values are property values.
     * @param items A list of items associated with the charged event. Each item is represented as
     *        a map of properties where keys are property names and values are property values.
     * @return `true` if any event matches, and all conditions
     * within that event are met, `false` otherwise.
     */
    fun matchChargedEvent(
        whenTriggers: JSONArray,
        eventName: String,
        details: Map<String, Any>,
        items: List<Map<String, Any>>,
    ): Boolean {

        // events in array are OR-ed
        val event = EventAdapter(eventName, details, items)
        return (0 until whenTriggers.length())
            .map { TriggerAdapter(whenTriggers[it] as JSONObject) }
            .any { match(it, event) }

    }

    /**
     * Helper function to match all trigger conditions against an event.
     *
     * @param trigger The [TriggerAdapter] having trigger condition to be matched against the event.
     * @param event The [EventAdapter] having event to be matched against the trigger condition.
     * @return `true` if all conditions within the trigger condition are met, `false` otherwise.
     */
    @VisibleForTesting
    internal fun match(trigger: TriggerAdapter, event: EventAdapter): Boolean {
        if (event.eventName != trigger.eventName) {
            return false
        }

        // property conditions are AND-ed
        ((0 until trigger.propertyCount)
            .mapNotNull { trigger.propertyAtIndex(it) }
            .all {
                evaluate(
                    it.op,
                    it.value,
                    event.getPropertyValue(it.propertyName)
                )
            }
            .takeIf { it }) ?: return false


        // (chargedEvent only) property conditions for items are AND-ed
        return (0 until trigger.itemsCount)
            .mapNotNull { trigger.itemAtIndex(it) }
            .all {
                evaluate(
                    it.op,
                    it.value,
                    event.getItemValue(it.propertyName)
                )
            }


    }

    /**
     * Internal function to evaluate a trigger condition.
     *
     * This function evaluates a single trigger condition by comparing the expected value with the actual value.
     * The specified operator (`op`) is used to perform the comparison.
     *
     * @param op The [TriggerOperator] used for comparison.
     * @param expected The expected [TriggerValue] for the condition.
     * @param actual The actual [TriggerValue] to be compared with the expected value.
     * @return `true` if the condition is satisfied, `false` otherwise.
     */
    @VisibleForTesting
    internal fun evaluate(
        op: TriggerOperator,
        expected: TriggerValue,
        actual: TriggerValue
    ): Boolean {
        if (actual.value == null) {
            return op == TriggerOperator.NotSet
        }

        return when (op) {
            TriggerOperator.Set -> true
            TriggerOperator.LessThan ->
                expected.numberValue()
                    ?.takeIf { actual.numberValue() != null }
                    ?.let { it.toDouble() < actual.numberValue()!!.toDouble() } ?: false


            TriggerOperator.GreaterThan ->
                expected.numberValue()
                    ?.takeIf { actual.numberValue() != null }
                    ?.let { it.toDouble() > actual.numberValue()!!.toDouble() } ?: false


            TriggerOperator.Equals -> expectedValueEqualsActual(expected, actual)
            TriggerOperator.NotEquals -> !expectedValueEqualsActual(expected, actual)
            TriggerOperator.Between -> actualIsInRangeOfExpected(expected, actual)
            TriggerOperator.Contains -> actualContainsExpected(expected, actual)
            TriggerOperator.NotContains -> !actualContainsExpected(expected, actual)
            else -> false // TODO: Implement all cases as per the backend evaluation and remove this line
        }
    }

    /**
     * Internal function to check if the expected value equals the actual value.
     *
     * This function checks if the expected value is equal to the actual value. It handles various
     * data types including strings, numbers, and lists.
     *
     * @param expected The expected [TriggerValue].
     * @param actual The actual [TriggerValue].
     * @return `true` if the expected value equals the actual value, `false` otherwise.
     */
    @VisibleForTesting
    internal fun expectedValueEqualsActual(expected: TriggerValue, actual: TriggerValue): Boolean {
        return when {
            expected.isList() && actual.isList() -> {
                expected.listValue()!!.toHashSet() == actual.listValue()!!.toHashSet()

            }

            actual.isList() -> expected.stringValue() in (actual.listValue() ?: listOf<String>())
            expected.isList() -> actual.stringValue() in (expected.listValue() ?: listOf<String>())
            expected.numberValue() != null -> {
                val actualNumber =
                    actual.numberValue()?.toDouble() ?: actual.stringValue()?.toDoubleOrNull()
                    ?: return false
                expected.numberValue()!!.toDouble() == actualNumber
            }

            actual.numberValue() != null -> {
                val expectedNumber =
                    expected.stringValue()?.toDoubleOrNull() ?: return false
                actual.numberValue()!!.toDouble() == expectedNumber
            }

            actual.stringValue() != null -> expected.stringValue() == actual.stringValue()
            else -> false
        }
    }

    /**
     * Internal function to check if the actual value contains the expected value.
     *
     * This function checks if the actual value (e.g., a string or a list) contains the expected value.
     *
     * @param expected The expected [TriggerValue].
     * @param actual The actual [TriggerValue].
     * @return `true` if the actual value contains the expected value, `false` otherwise.
     */
    @VisibleForTesting
    internal fun actualContainsExpected(expected: TriggerValue, actual: TriggerValue): Boolean {
        return when {
            actual.stringValue() != null && expected.stringValue() != null -> actual.stringValue()!!
                .contains(expected.stringValue()!!)

            expected.isList() && actual.stringValue() != null -> expected.listValue()!!.asSequence()
                .filterNotNull()
                .filterIsInstance<String>()
                .any { actual.stringValue()!!.contains(it) }

            expected.isList() && actual.isList() -> {
                val actualSet = actual.listValue()!!.filterIsInstance<String>().toSet()
                expected.listValue()!!.filterIsInstance<String>().any {
                    actualSet.contains(it)
                }
            }

            actual.isList() && expected.stringValue() != null ->
                actual.listValue()!!.filterIsInstance<String>().toSet()
                    .contains(expected.stringValue())

            else -> false
        }

    }

    /**
     * Internal function to check if the actual value is within the expected range.
     *
     * This function checks if the actual value is within the specified range defined by the expected
     * value. This is used for the "Between" operator.
     *
     * @param expected The [TriggerValue] having expected range represented as a list with two values.
     * @param actual The actual [TriggerValue] to be checked.
     * @return `true` if the actual value is within the expected range, `false` otherwise.
     */
    @VisibleForTesting
    internal fun actualIsInRangeOfExpected(
        expected: TriggerValue,
        actual: TriggerValue
    ): Boolean {
        return (expected.listValue()
            ?.takeIf { it.size >= 2 && it[0] is Double && it[1] is Double }
            ?.takeIf { !actual.isList() && actual.numberValue() != null }
            ?.let { actual.numberValue()!!.toDouble() in it[0] as Double..it[1] as Double })
            ?: false
    }
}
