package com.clevertap.android.sdk.profile.traversal

/**
 * Utilities for performing arithmetic operations on numbers during profile merging.
 * Preserves number types when possible (Int, Long, Float, Double).
 */
internal object NumberOperationUtils {

    /**
     * Adds two numbers, preserving type when possible.
     * 
     * Type promotion rules (checked in order):
     * - Double: If either operand is Double, result is Double
     * - Float: If either operand is Float (and no Double), result is Float
     * - Long: If either operand is Long (and no Float/Double), result is Long
     * - Int: Only if both operands are Int, result is Int
     *
     * @param a First number
     * @param b Second number
     * @return Sum of the numbers with appropriate type promotion
     */
    fun addNumbers(a: Number, b: Number): Number {
        return when {
            a is Double || b is Double -> a.toDouble() + b.toDouble()
            a is Float || b is Float -> a.toFloat() + b.toFloat()
            a is Long || b is Long -> a.toLong() + b.toLong()
            else -> a.toInt() + b.toInt()
        }
    }

    /**
     * Subtracts two numbers, preserving type when possible.
     * 
     * Type promotion rules (checked in order):
     * - Double: If either operand is Double, result is Double
     * - Float: If either operand is Float (and no Double), result is Float
     * - Long: If either operand is Long (and no Float/Double), result is Long
     * - Int: Only if both operands are Int, result is Int
     *
     * @param a First number (minuend)
     * @param b Second number (subtrahend)
     * @return Difference of the numbers (a - b) with appropriate type promotion
     */
    fun subtractNumbers(a: Number, b: Number): Number {
        return when {
            a is Double || b is Double -> a.toDouble() - b.toDouble()
            a is Float || b is Float -> a.toFloat() - b.toFloat()
            a is Long || b is Long -> a.toLong() - b.toLong()
            else -> a.toInt() - b.toInt()
        }
    }

    /**
     * Negates a number, preserving its type.
     */
    fun negateNumber(n: Number): Number {
        return when (n) {
            is Int -> -n
            is Long -> -n
            is Float -> -n
            is Double -> -n
            else -> -n.toDouble()
        }
    }
}
