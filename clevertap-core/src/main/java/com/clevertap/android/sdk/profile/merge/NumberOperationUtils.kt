package com.clevertap.android.sdk.profile.merge

/**
 * Utilities for performing arithmetic operations on numbers during profile merging.
 * Preserves number types when possible (Int, Long, Float, Double).
 */
internal object NumberOperationUtils {

    /**
     * Adds two numbers, preserving type when possible.
     * Promotes to wider types as needed: Int -> Long -> Float -> Double
     *
     * @param a First number
     * @param b Second number
     * @return Sum of the numbers
     */
    fun addNumbers(a: Number, b: Number): Number {
        return when {
            a is Int && b is Int -> a + b
            a is Long || b is Long -> a.toLong() + b.toLong()
            a is Float || b is Float -> a.toFloat() + b.toFloat()
            else -> a.toDouble() + b.toDouble()
        }
    }

    /**
     * Subtracts two numbers, preserving type when possible.
     * Promotes to wider types as needed: Int -> Long -> Float -> Double
     *
     * @param a First number (minuend)
     * @param b Second number (subtrahend)
     * @return Difference of the numbers (a - b)
     */
    fun subtractNumbers(a: Number, b: Number): Number {
        return when {
            a is Int && b is Int -> a - b
            a is Long || b is Long -> a.toLong() - b.toLong()
            a is Float || b is Float -> a.toFloat() - b.toFloat()
            else -> a.toDouble() - b.toDouble()
        }
    }
}
