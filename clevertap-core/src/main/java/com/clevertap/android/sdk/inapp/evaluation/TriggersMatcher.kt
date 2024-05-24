package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.isValid

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
        event: EventAdapter
    ): Boolean {

        // events in array are OR-ed
        //val event = EventAdapter(eventName, eventProperties)
        // Check if any TriggerAdapter in the list matches the event
        return whenTriggers.any { match(it, event) }
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
        if (event.eventName != trigger.eventName && (event.profileAttrName == null || event.profileAttrName.equals(trigger.profileAttrName, true))) {
            return false
        }

        if (!matchPropertyConditions(trigger, event)) {
            return false
        }

        if (event.isChargedEvent() && !matchChargedItemConditions(trigger, event)) {
            return false
        }

        if (trigger.geoRadiusCount > 0 && !matchGeoRadius(event, trigger)) {
            return false
        }

        return true
    }

    private fun matchPropertyConditions(trigger: TriggerAdapter, event: EventAdapter): Boolean {
        // Property conditions are AND-ed
        return (0 until trigger.propertyCount)
            .mapNotNull { trigger.propertyAtIndex(it) }
            .all {
                evaluate(
                    it.op,
                    it.value,
                    event.getPropertyValue(it.propertyName)
                )
            }
    }

    private fun matchChargedItemConditions(trigger: TriggerAdapter, event: EventAdapter): Boolean {
        // (chargedEvent only) Property conditions for items are AND-ed
        return (0 until trigger.itemsCount)
            .mapNotNull { trigger.itemAtIndex(it) }
            .all { condition ->
                event.getItemValue(condition.propertyName)
                    .any {
                        evaluate(
                            condition.op,
                            condition.value,
                            it
                        )
                    }
            }
    }

    /**
     * Matches the user's location against geo-radius conditions in the trigger.
     *
     * Conditions are OR-ed; returns true if any condition is satisfied.
     *
     * @param trigger The [TriggerAdapter] having trigger condition to be matched against the event.
     * @param event The [EventAdapter] having event to be matched against the trigger condition.
     * @return True if user location matches any geo-radius condition; otherwise, false.
     */
    @VisibleForTesting
    internal fun matchGeoRadius(event: EventAdapter, trigger: TriggerAdapter): Boolean {
        if (event.userLocation != null && event.userLocation.isValid()) {
            // GeoRadius conditions are OR-ed
            for (i in 0 until trigger.geoRadiusCount) {
                val triggerRadius = trigger.geoRadiusAtIndex(i)
                val expected = Location("")
                expected.latitude = triggerRadius!!.latitude
                expected.longitude = triggerRadius.longitude

                try {
                    if (evaluateDistance(triggerRadius.radius, expected, event.userLocation)) {
                        return true
                    }
                } catch (e: Exception) {
                    Logger.d("Error matching GeoRadius triggers for event named ${event.eventName}. Reason: ${e.localizedMessage}")
                }
            }
        }
        return false
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
            TriggerOperator.LessThan -> expectedValueLessThanGreaterThanActual(expected, actual, true)
            TriggerOperator.GreaterThan -> expectedValueLessThanGreaterThanActual(expected, actual, false)
            TriggerOperator.Equals -> expectedValueEqualsActual(expected, actual)
            TriggerOperator.NotEquals -> !expectedValueEqualsActual(expected, actual)
            TriggerOperator.Between -> actualIsInRangeOfExpected(expected, actual)
            TriggerOperator.Contains -> actualContainsExpected(expected, actual)
            TriggerOperator.NotContains -> !actualContainsExpected(expected, actual)
            TriggerOperator.NotSet -> false
        }
    }

    /**
     * Internal function to evaluate a haversine distance condition.
     *
     * This function evaluates if the haversine distance between two locations is within a specified radius.
     * The haversine formula is used to compute the distance between two locations.
     *
     * @param radius The radius to check against, in kilometers.
     * @param expected The expected location.
     * @param actual The actual location.
     * @return `true` if the haversine distance is within the specified radius, `false` otherwise.
     */
    @VisibleForTesting
    internal fun evaluateDistance(radius: Double, expected: Location, actual: Location): Boolean {
        val distance = Utils.haversineDistance(expected, actual)
        return distance <= radius
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
                expected.listValueWithCleanedStringIfPresent()!!
                    .toHashSet() == actual.listValueWithCleanedStringIfPresent()!!.toHashSet()
            }

            actual.isList() ->
                checkGivenElementEqualsAnyElementInList(
                    actual.listValueWithCleanedStringIfPresent()!!,
                    expected.value
                )

            expected.isList() -> checkGivenElementEqualsAnyElementInList(
                expected.listValueWithCleanedStringIfPresent()!!,
                actual.value
            )

            expected.numberValue() != null -> {
                val actualNumber =
                    actual.numberValue()?.toDouble() ?: actual.stringValueCleaned()?.toDoubleOrNull()
                    ?: return false
                expected.numberValue()!!.toDouble() == actualNumber
            }

            actual.numberValue() != null -> {
                val expectedNumber =
                    expected.stringValueCleaned()?.toDoubleOrNull() ?: return false
                actual.numberValue()!!.toDouble() == expectedNumber
            }

            actual.stringValue() != null -> expected.stringValueCleaned() == actual.stringValueCleaned()
            else -> false
        }
    }

    @VisibleForTesting
    internal fun expectedValueLessThanGreaterThanActual(
        expected: TriggerValue,
        actual: TriggerValue,
        isLessThan: Boolean
    ): Boolean {

        val actualNumber =
            actual.numberValue()?.toDouble() ?: actual.stringValue()?.toDoubleOrNull()
            ?: return false

        expected.listValue()?.firstOrNull()?.let {
            when (it) {
                is String -> {
                    it.toDoubleOrNull()
                }

                is Number -> {
                    it.toDouble()
                }

                else -> null
            }
        }?.also { return if (isLessThan) actualNumber < it else actualNumber > it }

        val expectedNumber =
            expected.numberValue()?.toDouble() ?: expected.stringValue()?.toDoubleOrNull()
            ?: return false

        return if (isLessThan) actualNumber < expectedNumber else actualNumber > expectedNumber
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
            actual.stringValue() != null && expected.stringValue() != null -> actual.stringValueCleaned()!!
                .contains(expected.stringValueCleaned()!!)

            expected.isList() && actual.stringValue() != null -> expected.listValueWithCleanedStringIfPresent()!!
                .asSequence()
                .filterNotNull()
                .filterIsInstance<String>()
                .any { actual.stringValueCleaned()!!.contains(it) }

            expected.isList() && actual.isList() -> {
                val actualSet = actual.listValueWithCleanedStringIfPresent()!!.filterIsInstance<String>().toSet()
                expected.listValueWithCleanedStringIfPresent()!!.filterIsInstance<String>().any {
                    actualSet.contains(it)
                }
            }

            actual.isList() && expected.stringValue() != null ->
                actual.listValueWithCleanedStringIfPresent()!!.filterIsInstance<String>().toSet()
                    .contains(expected.stringValueCleaned())

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
            ?.takeIf { it.size >= 2 }
            ?.take(2)
            ?.map {
                when (it) {
                    is String -> {
                        it.toDoubleOrNull()
                    }

                    is Number -> {
                        it.toDouble()
                    }

                    else -> null
                }
            }
            ?.let {
                if (it.contains(null))
                    return false

                val actualNumber =
                    actual.numberValue()?.toDouble() ?: actual.stringValue()?.toDoubleOrNull()
                    ?: return false

                actualNumber in it[0]!!..it[1]!!
            })
            ?: false
    }

    private fun checkGivenElementEqualsAnyElementInList(list: List<*>, elementToCheckForEquality: Any?): Boolean {
        when (elementToCheckForEquality) {
            is String -> {
                // Check for equality in strings
                return list.asSequence().filterIsInstance<String>()
                    .any { it == elementToCheckForEquality.trim().lowercase() }
                        || list.asSequence().filterIsInstance<Number>()
                    .any { it.toDouble() == elementToCheckForEquality.trim().lowercase().toDoubleOrNull() }
            }

            is Number -> {
                // Check for equality in numbers
                val numberToCheck = elementToCheckForEquality.toDouble()
                return list.asSequence().filterIsInstance<Number>().any { it.toDouble() == numberToCheck }
                        || list.asSequence().filterIsInstance<String>()
                    .any { it.trim().lowercase().toDoubleOrNull() == numberToCheck }
            }

            is Boolean -> {
                // Check for equality in booleans
                return list.asSequence().filterIsInstance<String>().any { it == elementToCheckForEquality.toString() }
            }
            else -> {
                // Handle other cases or return false if the type is not supported
                return false
            }
        }
    }
}
