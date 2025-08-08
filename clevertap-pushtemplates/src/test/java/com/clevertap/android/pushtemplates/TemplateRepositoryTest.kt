package com.clevertap.android.pushtemplates

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TemplateRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockConfig: CleverTapInstanceConfig
    private lateinit var templateRepository: TemplateRepository

    @Before
    fun setUp() {
        mockContext = mockk<Context>(relaxed = true)
        mockConfig = mockk<CleverTapInstanceConfig>(relaxed = true)
        
        templateRepository = TemplateRepository(mockContext, mockConfig)

        // Mock static methods
        mockkStatic(HttpBitmapLoader::class)
        mockkStatic(PTHttpBitmapLoader::class)
        mockkStatic(DownloadedBitmapFactory::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Tests for getBytes method
    @Test
    fun `getBytes should return NO_IMAGE status when url is empty string`() {
        // Given
        val url = ""
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBytes(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { PTHttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBytes should return NO_IMAGE status when url is blank with spaces`() {
        // Given
        val url = "   "
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBytes(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { PTHttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBytes should return NO_IMAGE status when url is tab and newline characters`() {
        // Given
        val url = "\t\n"
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBytes(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { PTHttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBytes should create correct BitmapDownloadRequest and call PTHttpBitmapLoader when url is valid`() {
        // Given
        val url = "https://example.com/image.gif"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            PTHttpBitmapLoader.getHttpBitmap(
                PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
                any()
            )
        } returns expectedDownloadedBitmap

        // When
        val result = templateRepository.getBytes(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        
        // Verify PTHttpBitmapLoader was called with correct parameters
        verify { 
            PTHttpBitmapLoader.getHttpBitmap(
                PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
                match { request ->
                    request.bitmapPath == url &&
                    request.fallbackToAppIcon == false &&
                    request.context == mockContext &&
                    request.instanceConfig == mockConfig &&
                    request.downloadTimeLimitInMillis == 5000L
                }
            )
        }
    }

    @Test
    fun `getBytes should work with different valid URLs`() {
        // Given
        val urls = listOf(
            "https://example.com/animation.gif",
            "https://cdn.example.com/files/image.gif",
            "https://sub.domain.example.com/path/to/file.gif?param=value",
            "http://example.com/image.gif" // even http should work (validation happens elsewhere)
        )
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            PTHttpBitmapLoader.getHttpBitmap(
                PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
                any()
            )
        } returns expectedDownloadedBitmap

        urls.forEach { url ->
            // When
            val result = templateRepository.getBytes(url)

            // Then
            assertEquals(expectedDownloadedBitmap, result)
            verify { 
                PTHttpBitmapLoader.getHttpBitmap(
                    PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
                    match { request -> request.bitmapPath == url }
                )
            }
        }
    }

    @Test
    fun `getBytes should use correct timeout value`() {
        // Given
        val url = "https://example.com/image.gif"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            PTHttpBitmapLoader.getHttpBitmap(any(), any())
        } returns expectedDownloadedBitmap

        // When
        templateRepository.getBytes(url)

        // Then
        verify { 
            PTHttpBitmapLoader.getHttpBitmap(
                any(),
                match { request -> request.downloadTimeLimitInMillis == 5000L }
            )
        }
    }

    @Test
    fun `getBytes should set fallbackToAppIcon to false`() {
        // Given
        val url = "https://example.com/image.gif"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            PTHttpBitmapLoader.getHttpBitmap(any(), any())
        } returns expectedDownloadedBitmap

        // When
        templateRepository.getBytes(url)

        // Then
        verify { 
            PTHttpBitmapLoader.getHttpBitmap(
                any(),
                match { request -> request.fallbackToAppIcon == false }
            )
        }
    }
    

    @Test
    fun `getBitmap should return NO_IMAGE status when url is empty string`() {
        // Given
        val url = ""
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBitmap(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { HttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBitmap should return NO_IMAGE status when url is blank with spaces`() {
        // Given
        val url = "   "
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBitmap(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { HttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBitmap should create correct BitmapDownloadRequest and call HttpBitmapLoader when url is valid`() {
        // Given
        val url = "https://example.com/image.jpg"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                any()
            )
        } returns expectedDownloadedBitmap

        // When
        val result = templateRepository.getBitmap(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        
        // Verify HttpBitmapLoader was called with correct parameters
        verify { 
            HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                match { request ->
                    request.bitmapPath == url &&
                    request.fallbackToAppIcon == false &&
                    request.context == mockContext &&
                    request.instanceConfig == null
                }
            )
        }
    }

    @Test
    fun `getBitmap should work with different valid URLs`() {
        // Given
        val urls = listOf(
            "https://example.com/image.jpg",
            "https://cdn.example.com/files/image.png",
            "https://sub.domain.example.com/path/to/file.webp?param=value&another=test",
            "http://example.com/image.jpeg" // even http should work
        )
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                any()
            )
        } returns expectedDownloadedBitmap

        urls.forEach { url ->
            // When
            val result = templateRepository.getBitmap(url)

            // Then
            assertEquals(expectedDownloadedBitmap, result)
            verify { 
                HttpBitmapLoader.getHttpBitmap(
                    HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                    match { request -> request.bitmapPath == url }
                )
            }
        }
    }

    @Test
    fun `getBitmap should pass context correctly and config as null`() {
        // Given
        val url = "https://example.com/image.jpg"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            HttpBitmapLoader.getHttpBitmap(any(), any())
        } returns expectedDownloadedBitmap

        // When
        templateRepository.getBitmap(url)

        // Then
        verify { 
            HttpBitmapLoader.getHttpBitmap(
                any(),
                match { request -> 
                    request.context == mockContext && 
                    request.instanceConfig == null 
                }
            )
        }
    }

    @Test
    fun `getBitmap should set fallbackToAppIcon to false`() {
        // Given
        val url = "https://example.com/image.jpg"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { 
            HttpBitmapLoader.getHttpBitmap(any(), any())
        } returns expectedDownloadedBitmap

        // When
        templateRepository.getBitmap(url)

        // Then
        verify { 
            HttpBitmapLoader.getHttpBitmap(
                any(),
                match { request -> request.fallbackToAppIcon == false }
            )
        }
    }

    @Test
    fun `getBitmap should handle tab and newline characters in URL`() {
        // Given
        val url = "\t\n"
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        // When
        val result = templateRepository.getBitmap(url)

        // Then
        assertEquals(expectedDownloadedBitmap, result)
        verify(exactly = 0) { HttpBitmapLoader.getHttpBitmap(any(), any()) }
    }


    // Integration tests for mixed scenarios
    @Test
    fun `should handle different types of blank URLs correctly for getBytes`() {
        // Given
        val blankUrls = listOf("", "   ", "\t", "\n", "\r", "\t\n\r   ")
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        blankUrls.forEach { url ->
            // When
            val result = templateRepository.getBytes(url)

            // Then
            assertEquals(expectedDownloadedBitmap, result)
        }
        
        // Verify that PTHttpBitmapLoader was never called for any blank URL
        verify(exactly = 0) { PTHttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `should handle different types of blank URLs correctly for getBitmap`() {
        // Given
        val blankUrls = listOf("", "   ", "\t", "\n", "\r", "\t\n\r   ")
        val expectedDownloadedBitmap = DownloadedBitmap(null, DownloadedBitmap.Status.NO_IMAGE, -1)

        blankUrls.forEach { url ->
            // When
            val result = templateRepository.getBitmap(url)

            // Then
            assertEquals(expectedDownloadedBitmap, result)
        }
        
        // Verify that HttpBitmapLoader was never called for any blank URL
        verify(exactly = 0) { HttpBitmapLoader.getHttpBitmap(any(), any()) }
    }

    @Test
    fun `getBytes and getBitmap should use different operations and configurations`() {
        // Given
        val url = "https://example.com/image.jpg"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { PTHttpBitmapLoader.getHttpBitmap(any(), any()) } returns expectedDownloadedBitmap
        every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns expectedDownloadedBitmap

        // When
        templateRepository.getBytes(url)
        templateRepository.getBitmap(url)

        // Then
        // getBytes should use PTHttpBitmapLoader with DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT and config
        verify { 
            PTHttpBitmapLoader.getHttpBitmap(
                PTHttpBitmapLoader.PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT,
                match { request -> 
                    request.instanceConfig == mockConfig && 
                    request.downloadTimeLimitInMillis == 5000L
                }
            )
        }
        
        // getBitmap should use HttpBitmapLoader with DOWNLOAD_ANY_BITMAP and null config
        verify { 
            HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                match { request -> request.instanceConfig == null }
            )
        }
    }

    @Test
    fun `should handle URLs with special characters and encoding`() {
        // Given
        val specialUrls = listOf(
            "https://example.com/image with spaces.jpg",
            "https://example.com/image%20encoded.jpg",
            "https://example.com/image-with-dashes.jpg",
            "https://example.com/image_with_underscores.jpg",
            "https://example.com/image.jpg?param=value&other=test",
            "https://example.com/image.jpg#fragment"
        )
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns expectedDownloadedBitmap
        every { PTHttpBitmapLoader.getHttpBitmap(any(), any()) } returns expectedDownloadedBitmap

        specialUrls.forEach { url ->
            // When
            val bitmapResult = templateRepository.getBitmap(url)
            val bytesResult = templateRepository.getBytes(url)

            // Then
            assertEquals(expectedDownloadedBitmap, bitmapResult)
            assertEquals(expectedDownloadedBitmap, bytesResult)
        }
    }

    @Test
    fun `should create separate BitmapDownloadRequest instances for each call`() {
        // Given
        val url = "https://example.com/image.jpg"
        val expectedDownloadedBitmap = mockk<DownloadedBitmap>()
        
        every { HttpBitmapLoader.getHttpBitmap(any(), any()) } returns expectedDownloadedBitmap

        // When
        templateRepository.getBitmap(url)
        templateRepository.getBitmap(url)

        // Then
        // Should create separate request instances for each call
        verify(exactly = 2) { 
            HttpBitmapLoader.getHttpBitmap(
                HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_ANY_BITMAP,
                any()
            )
        }
    }
}