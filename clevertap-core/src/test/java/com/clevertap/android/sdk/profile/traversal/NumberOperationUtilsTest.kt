package com.clevertap.android.sdk.profile.traversal

import org.junit.Assert.*
import org.junit.Test

class NumberOperationUtilsTest {

    // addNumbers tests - Int + Int
    @Test
    fun `addNumbers returns Int when both are Int`() {
        val result = NumberOperationUtils.addNumbers(5, 3)

        assertTrue(result is Int)
        assertEquals(8, result)
    }

    @Test
    fun `addNumbers handles Int addition with negative numbers`() {
        val result = NumberOperationUtils.addNumbers(5, -3)

        assertTrue(result is Int)
        assertEquals(2, result)
    }

    // addNumbers tests - Long promotion
    @Test
    fun `addNumbers promotes to Long when one is Long`() {
        val result = NumberOperationUtils.addNumbers(5, 3L)

        assertTrue(result is Long)
        assertEquals(8L, result)
    }

    @Test
    fun `addNumbers promotes to Long when both are Long`() {
        val result = NumberOperationUtils.addNumbers(5L, 3L)

        assertTrue(result is Long)
        assertEquals(8L, result)
    }

    @Test
    fun `addNumbers handles Long with Int`() {
        val result = NumberOperationUtils.addNumbers(100L, 50)

        assertTrue(result is Long)
        assertEquals(150L, result)
    }

    // addNumbers tests - Float promotion
    @Test
    fun `addNumbers promotes to Float when one is Float`() {
        val result = NumberOperationUtils.addNumbers(5, 3.5f)

        assertTrue(result is Float)
        assertEquals(8.5f, result)
    }

    @Test
    fun `addNumbers promotes to Float when both are Float`() {
        val result = NumberOperationUtils.addNumbers(5.5f, 3.5f)

        assertTrue(result is Float)
        assertEquals(9.0f, result)
    }

    @Test
    fun `addNumbers promotes Float over Long`() {
        val result = NumberOperationUtils.addNumbers(5L, 3.5f)

        assertTrue(result is Float)
        assertEquals(8.5f, result)
    }

    // addNumbers tests - Double promotion
    @Test
    fun `addNumbers promotes to Double when one is Double`() {
        val result = NumberOperationUtils.addNumbers(5, 3.5)

        assertTrue(result is Double)
        assertEquals(8.5, result as Double, 0.0001)
    }

    @Test
    fun `addNumbers promotes to Double when both are Double`() {
        val result = NumberOperationUtils.addNumbers(5.5, 3.5)

        assertTrue(result is Double)
        assertEquals(9.0, result as Double, 0.0001)
    }

    @Test
    fun `addNumbers promotes Double over Float`() {
        val result = NumberOperationUtils.addNumbers(5.5f, 3.5)

        assertTrue(result is Double)
        assertEquals(9.0, result as Double, 0.0001)
    }

    @Test
    fun `addNumbers promotes Double over Long`() {
        val result = NumberOperationUtils.addNumbers(5L, 3.5)

        assertTrue(result is Double)
        assertEquals(8.5, result as Double, 0.0001)
    }

    // subtractNumbers tests - Int - Int
    @Test
    fun `subtractNumbers returns Int when both are Int`() {
        val result = NumberOperationUtils.subtractNumbers(10, 3)

        assertTrue(result is Int)
        assertEquals(7, result)
    }

    @Test
    fun `subtractNumbers handles Int with negative result`() {
        val result = NumberOperationUtils.subtractNumbers(3, 10)

        assertTrue(result is Int)
        assertEquals(-7, result)
    }

    @Test
    fun `subtractNumbers handles Int subtraction with negative numbers`() {
        val result = NumberOperationUtils.subtractNumbers(5, -3)

        assertTrue(result is Int)
        assertEquals(8, result)
    }

    @Test
    fun `subtractNumbers handles Int subtraction with zero`() {
        val result = NumberOperationUtils.subtractNumbers(5, 0)

        assertTrue(result is Int)
        assertEquals(5, result)
    }

    @Test
    fun `subtractNumbers handles Int underflow boundary`() {
        val result = NumberOperationUtils.subtractNumbers(Int.MIN_VALUE, 1)

        // Int underflow wraps around
        assertTrue(result is Int)
        assertEquals(Int.MAX_VALUE, result)
    }

    // subtractNumbers tests - Long promotion
    @Test
    fun `subtractNumbers promotes to Long when one is Long`() {
        val result = NumberOperationUtils.subtractNumbers(10, 3L)

        assertTrue(result is Long)
        assertEquals(7L, result)
    }

    @Test
    fun `subtractNumbers promotes to Long when both are Long`() {
        val result = NumberOperationUtils.subtractNumbers(10L, 3L)

        assertTrue(result is Long)
        assertEquals(7L, result)
    }

    @Test
    fun `subtractNumbers handles Long with Int`() {
        val result = NumberOperationUtils.subtractNumbers(100L, 50)

        assertTrue(result is Long)
        assertEquals(50L, result)
    }

    // subtractNumbers tests - Float promotion
    @Test
    fun `subtractNumbers promotes to Float when one is Float`() {
        val result = NumberOperationUtils.subtractNumbers(10, 3.5f)

        assertTrue(result is Float)
        assertEquals(6.5f, result)
    }

    @Test
    fun `subtractNumbers promotes to Float when both are Float`() {
        val result = NumberOperationUtils.subtractNumbers(10.5f, 3.5f)

        assertTrue(result is Float)
        assertEquals(7.0f, result)
    }

    @Test
    fun `subtractNumbers promotes Float over Long`() {
        val result = NumberOperationUtils.subtractNumbers(10L, 3.5f)

        assertTrue(result is Float)
        assertEquals(6.5f, result)
    }

    // subtractNumbers tests - Double promotion
    @Test
    fun `subtractNumbers promotes to Double when one is Double`() {
        val result = NumberOperationUtils.subtractNumbers(10, 3.5)

        assertTrue(result is Double)
        assertEquals(6.5, result as Double, 0.0001)
    }

    @Test
    fun `subtractNumbers promotes to Double when both are Double`() {
        val result = NumberOperationUtils.subtractNumbers(10.5, 3.5)

        assertTrue(result is Double)
        assertEquals(7.0, result as Double, 0.0001)
    }

    @Test
    fun `subtractNumbers promotes Double over Float`() {
        val result = NumberOperationUtils.subtractNumbers(10.5f, 3.5)

        assertTrue(result is Double)
        assertEquals(7.0, result as Double, 0.0001)
    }

    // negateNumber tests - Int
    @Test
    fun `negateNumber preserves Int type for positive`() {
        val result = NumberOperationUtils.negateNumber(5)

        assertTrue(result is Int)
        assertEquals(-5, result)
    }

    @Test
    fun `negateNumber preserves Int type for negative`() {
        val result = NumberOperationUtils.negateNumber(-5)

        assertTrue(result is Int)
        assertEquals(5, result)
    }

    @Test
    fun `negateNumber handles Int zero`() {
        val result = NumberOperationUtils.negateNumber(0)

        assertTrue(result is Int)
        assertEquals(0, result)
    }

    @Test
    fun `negateNumber handles Int MIN_VALUE boundary`() {
        val result = NumberOperationUtils.negateNumber(Int.MIN_VALUE)

        // Negating Int.MIN_VALUE causes overflow
        assertTrue(result is Int)
        assertEquals(Int.MIN_VALUE, result) // Wraps around
    }

    // negateNumber tests - Long
    @Test
    fun `negateNumber preserves Long type for positive`() {
        val result = NumberOperationUtils.negateNumber(5L)

        assertTrue(result is Long)
        assertEquals(-5L, result)
    }

    @Test
    fun `negateNumber preserves Long type for negative`() {
        val result = NumberOperationUtils.negateNumber(-5L)

        assertTrue(result is Long)
        assertEquals(5L, result)
    }

    @Test
    fun `negateNumber handles Long zero`() {
        val result = NumberOperationUtils.negateNumber(0L)

        assertTrue(result is Long)
        assertEquals(0L, result)
    }

    @Test
    fun `negateNumber handles large Long values`() {
        val result = NumberOperationUtils.negateNumber(1_000_000_000L)

        assertTrue(result is Long)
        assertEquals(-1_000_000_000L, result)
    }

    // negateNumber tests - Float
    @Test
    fun `negateNumber preserves Float type for positive`() {
        val result = NumberOperationUtils.negateNumber(5.5f)

        assertTrue(result is Float)
        assertEquals(-5.5f, result)
    }

    @Test
    fun `negateNumber preserves Float type for negative`() {
        val result = NumberOperationUtils.negateNumber(-5.5f)

        assertTrue(result is Float)
        assertEquals(5.5f, result)
    }

    @Test
    fun `negateNumber handles Float zero`() {
        val result = NumberOperationUtils.negateNumber(0.0f)

        assertTrue(result is Float)
        assertEquals(-0.0f, result)
    }

    @Test
    fun `negateNumber handles Float infinity`() {
        val result = NumberOperationUtils.negateNumber(Float.POSITIVE_INFINITY)

        assertTrue(result is Float)
        assertEquals(Float.NEGATIVE_INFINITY, result)
    }

    // negateNumber tests - Double
    @Test
    fun `negateNumber preserves Double type for positive`() {
        val result = NumberOperationUtils.negateNumber(5.5)

        assertTrue(result is Double)
        assertEquals(-5.5, result as Double, 0.0001)
    }

    @Test
    fun `negateNumber preserves Double type for negative`() {
        val result = NumberOperationUtils.negateNumber(-5.5)

        assertTrue(result is Double)
        assertEquals(5.5, result as Double, 0.0001)
    }

    @Test
    fun `negateNumber handles Double zero`() {
        val result = NumberOperationUtils.negateNumber(0.0)

        assertTrue(result is Double)
        assertEquals(0.0, result as Double, 0.0001)
    }

    @Test
    fun `negateNumber handles Double infinity`() {
        val result = NumberOperationUtils.negateNumber(Double.POSITIVE_INFINITY)

        assertTrue(result is Double)
        assertEquals(Double.NEGATIVE_INFINITY, result as Double, 0.0001)
    }

    // negateNumber tests - Other Number types (fallback to Double)
    @Test
    fun `negateNumber converts Short to Double`() {
        val result = NumberOperationUtils.negateNumber(5.toShort())

        assertTrue(result is Double)
        assertEquals(-5.0, result as Double, 0.0001)
    }

    @Test
    fun `negateNumber converts Byte to Double`() {
        val result = NumberOperationUtils.negateNumber(5.toByte())

        assertTrue(result is Double)
        assertEquals(-5.0, result as Double, 0.0001)
    }

    // Edge cases and type promotion hierarchy
    @Test
    fun `addNumbers type promotion hierarchy Int-Long-Float-Double`() {
        // Int stays Int
        assertTrue(NumberOperationUtils.addNumbers(1, 2) is Int)
        
        // Long promotes Int
        assertTrue(NumberOperationUtils.addNumbers(1, 2L) is Long)
        
        // Float promotes Int and Long
        assertTrue(NumberOperationUtils.addNumbers(1, 2.0f) is Float)
        assertTrue(NumberOperationUtils.addNumbers(1L, 2.0f) is Float)
        
        // Double promotes all
        assertTrue(NumberOperationUtils.addNumbers(1, 2.0) is Double)
        assertTrue(NumberOperationUtils.addNumbers(1L, 2.0) is Double)
        assertTrue(NumberOperationUtils.addNumbers(1.0f, 2.0) is Double)
    }

    @Test
    fun `subtractNumbers type promotion hierarchy Int-Long-Float-Double`() {
        // Int stays Int
        assertTrue(NumberOperationUtils.subtractNumbers(5, 2) is Int)
        
        // Long promotes Int
        assertTrue(NumberOperationUtils.subtractNumbers(5, 2L) is Long)
        
        // Float promotes Int and Long
        assertTrue(NumberOperationUtils.subtractNumbers(5, 2.0f) is Float)
        assertTrue(NumberOperationUtils.subtractNumbers(5L, 2.0f) is Float)
        
        // Double promotes all
        assertTrue(NumberOperationUtils.subtractNumbers(5, 2.0) is Double)
        assertTrue(NumberOperationUtils.subtractNumbers(5L, 2.0) is Double)
        assertTrue(NumberOperationUtils.subtractNumbers(5.0f, 2.0) is Double)
    }

    @Test
    fun `operations handle decimal precision with Float`() {
        val result = NumberOperationUtils.addNumbers(0.1f, 0.2f)
        
        assertTrue(result is Float)
        // Float precision issues
        assertEquals(0.3f, result as Float, 0.0001f)
    }

    @Test
    fun `operations handle decimal precision with Double`() {
        val result = NumberOperationUtils.addNumbers(0.1, 0.2)
        
        assertTrue(result is Double)
        // Double has better precision but still not perfect
        assertEquals(0.3, result as Double, 0.00001)
    }
}
