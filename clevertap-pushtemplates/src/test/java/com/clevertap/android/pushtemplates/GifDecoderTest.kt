package com.clevertap.android.pushtemplates

import android.graphics.Bitmap
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import pl.droidsonroids.gif.GifDrawable

@RunWith(RobolectricTestRunner::class)
class GifDecoderTest {

    private lateinit var gifDecoderImpl: GifDecoderImpl
    private lateinit var mockAdapter: GifDrawableAdapter
    private lateinit var mockGifDrawable: GifDrawable
    private lateinit var mockBitmap1: Bitmap
    private lateinit var mockBitmap2: Bitmap
    private lateinit var mockBitmap3: Bitmap

    private val testBytes = byteArrayOf(1, 2, 3, 4, 5)

    @Before
    fun setUp() {
        mockAdapter = mockk<GifDrawableAdapter>(relaxed = true)
        mockGifDrawable = mockk<GifDrawable>(relaxed = true)
        mockBitmap1 = mockk<Bitmap>(relaxed = true)
        mockBitmap2 = mockk<Bitmap>(relaxed = true)
        mockBitmap3 = mockk<Bitmap>(relaxed = true)
        gifDecoderImpl = GifDecoderImpl(mockAdapter)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `decode should return successful result when gif processing succeeds`() {
        // Arrange
        val expectedFrames = listOf(mockBitmap1, mockBitmap2)
        val expectedDuration = 2000

        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 2
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 1) } returns mockBitmap2
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertEquals(expectedFrames, result.frames)
        assertEquals(expectedDuration, result.duration)
        
        verify { mockAdapter.create(testBytes) }
        verify { mockAdapter.getFrameCount(mockGifDrawable) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 1) }
        verify { mockAdapter.getDuration(mockGifDrawable) }
    }

    @Test
    fun `decode should return failure result when adapter create throws exception`() {
        // Arrange
        every { mockAdapter.create(testBytes) } throws RuntimeException("Failed to create GIF")

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertNull(result.frames)
        assertEquals(-1, result.duration)
        
        verify { mockAdapter.create(testBytes) }
        verify(exactly = 0) { mockAdapter.getFrameCount(any()) }
    }

    @Test
    fun `decode should return failure result when getFrameCount throws exception`() {
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } throws RuntimeException("Failed to get frame count")

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertNull(result.frames)
        assertEquals(-1, result.duration)
        
        verify { mockAdapter.create(testBytes) }
        verify { mockAdapter.getFrameCount(mockGifDrawable) }
    }

    @Test
    fun `decode should skip failed frames but continue processing`() {
        // Arrange
        val expectedDuration = 3000
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 3
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 1) } throws RuntimeException("Frame extraction failed")
        every { mockAdapter.getFrameAt(mockGifDrawable, 2) } returns mockBitmap3
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 3)

        // Assert
        assertNotNull(result.frames)
        assertEquals(listOf(mockBitmap1, mockBitmap3), result.frames)
        assertEquals(expectedDuration, result.duration)
        
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 1) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 2) }
    }

    @Test
    fun `decode should return empty frames when all frame extractions fail`() {
        // Arrange
        val expectedDuration = 1500
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 2
        every { mockAdapter.getFrameAt(mockGifDrawable, any()) } throws RuntimeException("Frame extraction failed")
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
        assertEquals(expectedDuration, result.duration)
    }

    @Test
    fun `decode should limit frames when maxFrames is less than total frames`() {
        // Arrange
        val expectedDuration = 4000
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 10
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 5) } returns mockBitmap2
        every { mockAdapter.getFrameAt(mockGifDrawable, 9) } returns mockBitmap3
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 3)

        // Assert
        assertNotNull(result.frames)
        assertEquals(3, result.frames!!.size)
        assertEquals(listOf(mockBitmap1, mockBitmap2, mockBitmap3), result.frames)
        assertEquals(expectedDuration, result.duration)
        
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 5) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 9) }
    }

    @Test
    fun `decode should return all frames when maxFrames is greater than total frames`() {
        // Arrange
        val expectedFrames = listOf(mockBitmap1, mockBitmap2)
        val expectedDuration = 2500
        
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 2
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 1) } returns mockBitmap2
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 5)

        // Assert
        assertEquals(expectedFrames, result.frames)
        assertEquals(expectedDuration, result.duration)
        
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 1) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 2) }
    }

    @Test
    fun `decode should handle single frame extraction when maxFrames is 1`() {
        // Arrange
        val expectedDuration = 1000
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 5
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 1)

        // Assert
        assertNotNull(result.frames)
        assertEquals(1, result.frames!!.size)
        assertEquals(listOf(mockBitmap1), result.frames)
        assertEquals(expectedDuration, result.duration)
        
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 1) }
    }

    @Test
    fun `decode should return empty frames when maxFrames is zero`() {
        // Arrange
        val expectedDuration = 2000
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 3
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 0)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
        assertEquals(expectedDuration, result.duration)
        
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, any()) }
    }

    @Test
    fun `decode should return empty frames when maxFrames is negative`() {
        // Arrange
        val expectedDuration = 1800
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 4
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, -1)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
        assertEquals(expectedDuration, result.duration)
        
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, any()) }
    }

    @Test
    fun `decode should return empty frames when total frames is zero`() {
        // Arrange
        val expectedDuration = 0
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 0
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 5)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
        assertEquals(expectedDuration, result.duration)
        
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, any()) }
    }

    @Test
    fun `decode should return empty frames when total frames is negative`() {
        // Arrange
        val expectedDuration = 1000
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns -1
        every { mockAdapter.getDuration(mockGifDrawable) } returns expectedDuration

        // Act
        val result = gifDecoderImpl.decode(testBytes, 3)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
        assertEquals(expectedDuration, result.duration)
        
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, any()) }
    }

    @Test
    fun `selectFrameIndices should return empty list when totalFrames is zero`() {
        // Test the private method indirectly through decode
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 0
        every { mockAdapter.getDuration(mockGifDrawable) } returns 1000

        // Act
        val result = gifDecoderImpl.decode(testBytes, 5)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
    }

    @Test
    fun `selectFrameIndices should return empty list when maxFrames is zero`() {
        // Test the private method indirectly through decode
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 5
        every { mockAdapter.getDuration(mockGifDrawable) } returns 1000

        // Act
        val result = gifDecoderImpl.decode(testBytes, 0)

        // Assert
        assertNotNull(result.frames)
        assertTrue(result.frames!!.isEmpty())
    }

    @Test
    fun `selectFrameIndices should distribute frames evenly when maxFrames is less than totalFrames`() {
        // Test frame distribution indirectly through verification of which frames are requested
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 7 // 0,1,2,3,4,5,6
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 3) } returns mockBitmap2
        every { mockAdapter.getFrameAt(mockGifDrawable, 6) } returns mockBitmap3
        every { mockAdapter.getDuration(mockGifDrawable) } returns 2000

        // Act - request 3 frames from 7 total should give us frames 0, 3, 6
        gifDecoderImpl.decode(testBytes, 3)

        // Assert
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 3) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 6) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 1) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 2) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 4) }
        verify(exactly = 0) { mockAdapter.getFrameAt(mockGifDrawable, 5) }
    }

    @Test
    fun `selectFrameIndices should handle edge case where step calculation results in duplicate indices`() {
        // Test with specific frame counts that might cause rounding issues
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 3 // 0,1,2
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 1) } returns mockBitmap2
        every { mockAdapter.getFrameAt(mockGifDrawable, 2) } returns mockBitmap3
        every { mockAdapter.getDuration(mockGifDrawable) } returns 1500

        // Act - request 5 frames from 3 total should return all 3 frames
        val result = gifDecoderImpl.decode(testBytes, 5)

        // Assert
        assertNotNull(result.frames)
        assertEquals(3, result.frames!!.size)
        verify { mockAdapter.getFrameAt(mockGifDrawable, 0) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 1) }
        verify { mockAdapter.getFrameAt(mockGifDrawable, 2) }
    }

    @Test
    fun `decode should work with actual GifResult failure method`() {
        // This tests integration with GifResult.failure()
        // Arrange
        every { mockAdapter.create(testBytes) } throws RuntimeException("Creation failed")

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertEquals(GifResult.failure().frames, result.frames)
        assertEquals(GifResult.failure().duration, result.duration)
    }

    @Test
    fun `decode should handle getDuration throwing exception`() {
        // Test case where duration extraction fails but frame processing succeeds
        // Arrange
        every { mockAdapter.create(testBytes) } returns mockGifDrawable
        every { mockAdapter.getFrameCount(mockGifDrawable) } returns 2
        every { mockAdapter.getFrameAt(mockGifDrawable, 0) } returns mockBitmap1
        every { mockAdapter.getFrameAt(mockGifDrawable, 1) } returns mockBitmap2
        every { mockAdapter.getDuration(mockGifDrawable) } throws RuntimeException("Duration extraction failed")

        // Act
        val result = gifDecoderImpl.decode(testBytes, 2)

        // Assert
        assertNull(result.frames)
        assertEquals(-1, result.duration)
    }

    @Test
    fun `decode should handle empty byte array`() {
        // Arrange
        val emptyBytes = byteArrayOf()
        every { mockAdapter.create(emptyBytes) } throws IllegalArgumentException("Empty byte array")

        // Act
        val result = gifDecoderImpl.decode(emptyBytes, 2)

        // Assert
        assertNull(result.frames)
        assertEquals(-1, result.duration)
    }
}