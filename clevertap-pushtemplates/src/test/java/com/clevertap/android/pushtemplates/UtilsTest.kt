package com.clevertap.android.pushtemplates

import android.os.Bundle
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UtilsTest {

    private lateinit var mockBundle: Bundle

    @Before
    fun setUp() {
        mockBundle = mockk<Bundle>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getFlipInterval should return default value when bundle has no flip interval key`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns null

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is empty string`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns ""

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is null`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns null

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is not a valid number`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "abc"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval contains special characters`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "123abc!@#"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is zero`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "0"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is negative`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "-500"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is less than default`() {
        // Given
        val smallInterval = (PTConstants.PT_FLIP_INTERVAL_TIME - 1000).toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns smallInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return provided value when it equals default`() {
        // Given
        val defaultInterval = PTConstants.PT_FLIP_INTERVAL_TIME.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns defaultInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return provided value when it is greater than default`() {
        // Given
        val largeInterval = (PTConstants.PT_FLIP_INTERVAL_TIME + 2000).toString()
        val expectedValue = PTConstants.PT_FLIP_INTERVAL_TIME + 2000
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns largeInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `getFlipInterval should handle very large valid numbers`() {
        // Given
        val largeInterval = "999999"
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns largeInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(999999, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval has leading spaces`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "  5000"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval has trailing spaces`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "5000  "

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when flip interval is decimal number`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "5000.5"

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should return default value when parsing throws NumberFormatException`() {
        // Given
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns "2147483648" // Integer.MAX_VALUE + 1

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should verify correct method calls`() {
        // Given
        val testInterval = "6000"
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns testInterval

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        verify(exactly = 1) { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) }
        assertEquals(6000, result)
    }

    @Test
    fun `getFlipInterval should handle edge case of Integer MIN_VALUE`() {
        // Given
        val minValue = Integer.MIN_VALUE.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns minValue

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(PTConstants.PT_FLIP_INTERVAL_TIME, result)
    }

    @Test
    fun `getFlipInterval should handle edge case of Integer MAX_VALUE`() {
        // Given
        val maxValue = Integer.MAX_VALUE.toString()
        every { mockBundle.getString(PTConstants.PT_FLIP_INTERVAL) } returns maxValue

        // When
        val result = Utils.getFlipInterval(mockBundle)

        // Then
        assertEquals(Integer.MAX_VALUE, result)
    }
}
