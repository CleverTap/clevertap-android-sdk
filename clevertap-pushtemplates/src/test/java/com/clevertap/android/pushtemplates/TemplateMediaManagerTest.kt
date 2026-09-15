package com.clevertap.android.pushtemplates

import android.content.Context
import android.graphics.Bitmap
import com.clevertap.android.pushtemplates.media.GifDecoderImpl
import com.clevertap.android.pushtemplates.media.GifResult
import com.clevertap.android.pushtemplates.media.TemplateMediaManager
import com.clevertap.android.pushtemplates.media.TemplateRepository
import com.clevertap.android.sdk.network.DownloadedBitmap
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TemplateMediaManagerTest {

    private lateinit var mockTemplateRepository: TemplateRepository
    private lateinit var mockGifDecoder: GifDecoderImpl
    private lateinit var mockContext: Context
    private lateinit var templateMediaManager: TemplateMediaManager

    @Before
    fun setUp() {
        mockTemplateRepository = mockk<TemplateRepository>(relaxed = true)
        mockGifDecoder = mockk<GifDecoderImpl>(relaxed = true)
        mockContext = mockk<Context>(relaxed = true)
        
        templateMediaManager = TemplateMediaManager(mockTemplateRepository, mockGifDecoder)

        // Mock static methods
        mockkStatic(Utils::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Tests for getGifFrames method

    @Test
    fun `getGifFrames should return failure when gifUrl is null`() {
        // Given
        val maxFrames = 10

        // When
        val result = templateMediaManager.getGifFrames(null, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Invalid GIF URL"))
        verify(exactly = 0) { mockTemplateRepository.getBytes(any()) }
    }

    @Test
    fun `getGifFrames should return failure when gifUrl is blank`() {
        // Given
        val gifUrl = "   "
        val maxFrames = 10

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Invalid GIF URL"))
        verify(exactly = 0) { mockTemplateRepository.getBytes(any()) }
    }

    @Test
    fun `getGifFrames should return failure when gifUrl does not start with https`() {
        // Given
        val gifUrl = "http://example.com/image.gif"
        val maxFrames = 10

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Invalid GIF URL"))
        verify(exactly = 0) { mockTemplateRepository.getBytes(any()) }
    }

    @Test
    fun `getGifFrames should return failure when gifUrl does not end with gif`() {
        // Given
        val gifUrl = "https://example.com/image.jpg"
        val maxFrames = 10

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Invalid GIF URL"))
        verify(exactly = 0) { mockTemplateRepository.getBytes(any()) }
    }

    @Test
    fun `getGifFrames should return success when gifUrl is valid and download succeeds`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 10
        val mockBytes = ByteArray(100)
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        val expectedGifResult = mockk<GifResult>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns mockBytes
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap
        every { mockGifDecoder.decode(mockBytes, maxFrames) } returns expectedGifResult

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertEquals(expectedGifResult, result)
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify { mockGifDecoder.decode(mockBytes, maxFrames) }
    }

    @Test
    fun `getGifFrames should return failure when download fails`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 10
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.DOWNLOAD_FAILED
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Failed to download GIF"))
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify(exactly = 0) { mockGifDecoder.decode(any(), any()) }
    }


    @Test
    fun `getGifFrames should return failure when bytes are null`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 10
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns null
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue(result is GifResult.Error)
        assertTrue((result as GifResult.Error).reason.contains("Failed to download GIF"))
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify(exactly = 0) { mockGifDecoder.decode(any(), any()) }
    }

    @Test
    fun `getGifFrames should handle uppercase GIF extension`() {
        // Given
        val gifUrl = "https://example.com/image.GIF" // Uppercase GIF extension
        val maxFrames = 10
        val mockBytes = ByteArray(100)
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        val expectedGifResult = mockk<GifResult>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns mockBytes
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap
        every { mockGifDecoder.decode(mockBytes, maxFrames) } returns expectedGifResult

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertEquals(expectedGifResult, result)
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify { mockGifDecoder.decode(mockBytes, maxFrames) }
    }

    @Test
    fun `getGifFrames should handle case insensitive gif extension`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 5
        val mockBytes = ByteArray(100)
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        val expectedGifResult = mockk<GifResult>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns mockBytes
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap
        every { mockGifDecoder.decode(mockBytes, maxFrames) } returns expectedGifResult

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertEquals(expectedGifResult, result)
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify { mockGifDecoder.decode(mockBytes, maxFrames) }
    }

    @Test
    fun `getGifFrames should work with different maxFrames values`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 1
        val mockBytes = ByteArray(50)
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        val expectedGifResult = mockk<GifResult>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns mockBytes
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap
        every { mockGifDecoder.decode(mockBytes, maxFrames) } returns expectedGifResult

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertEquals(expectedGifResult, result)
        verify { mockGifDecoder.decode(mockBytes, maxFrames) }
    }

    @Test
    fun `getGifFrames should handle empty url`() {
        // Given
        val gifUrl = ""
        val maxFrames = 10

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertTrue((result as GifResult.Error).reason.contains("Invalid GIF URL"))
        verify(exactly = 0) { mockTemplateRepository.getBytes(any()) }
    }

    @Test
    fun `getGifFrames should handle various download failure statuses`() {
        // Given
        val gifUrl = "https://example.com/image.gif"
        val maxFrames = 10
        val statuses = listOf(
            DownloadedBitmap.Status.DOWNLOAD_FAILED,
            DownloadedBitmap.Status.NO_IMAGE,
            DownloadedBitmap.Status.SIZE_LIMIT_EXCEEDED
        )

        statuses.forEach { status ->
            val mockDownloadedBitmap = mockk<DownloadedBitmap>()
            every { mockDownloadedBitmap.status } returns status
            every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap

            // When
            val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

            // Then
            assertTrue(result is GifResult.Error)
        }
    }

    // Tests for getNotificationBitmap method

    @Test
    fun `getNotificationBitmap should return null when icoPath is null and fallbackToAppIcon is false`() {
        // Given
        val icoPath: String? = null
        val fallbackToAppIcon = false

        // When
        val result = templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertNull(result)
        verify(exactly = 0) { Utils.getAppIcon(any()) }
    }

    @Test
    fun `getNotificationBitmap should return null when icoPath is empty and fallbackToAppIcon is false`() {
        // Given
        val icoPath = ""
        val fallbackToAppIcon = false

        // When
        val result = templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertNull(result)
        verify(exactly = 0) { Utils.getAppIcon(any()) }
    }

    @Test
    fun `getNotificationBitmap should return app icon when icoPath is null and fallbackToAppIcon is true`() {
        // Given
        val icoPath: String? = null
        val fallbackToAppIcon = true
        val mockBitmap = mockk<Bitmap>()

        every { Utils.getAppIcon(mockContext) } returns mockBitmap

        // When
        val result = templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockBitmap, result)
        verify { Utils.getAppIcon(mockContext) }
    }

    @Test
    fun `getNotificationBitmap should return app icon when icoPath is empty and fallbackToAppIcon is true`() {
        // Given
        val icoPath = ""
        val fallbackToAppIcon = true
        val mockBitmap = mockk<Bitmap>()

        every { Utils.getAppIcon(mockContext) } returns mockBitmap

        // When
        val result = templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockBitmap, result)
        verify { Utils.getAppIcon(mockContext) }
    }

    @Test
    fun `getNotificationBitmap should return image bitmap when valid icoPath provided and image loads successfully`() {
        // Given
        val icoPath = "https://example.com/icon.png"
        val fallbackToAppIcon = false
        val mockBitmap = mockk<Bitmap>()
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap
        every { mockTemplateRepository.getBitmap(icoPath) } returns mockDownloadedBitmap

        // Spy on templateMediaManager to mock getImageBitmap
        val spyTemplateMediaManager = spyk(templateMediaManager)
        every { spyTemplateMediaManager.getImageBitmap(icoPath) } returns mockBitmap

        // When
        val result = spyTemplateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockBitmap, result)
        verify { spyTemplateMediaManager.getImageBitmap(icoPath) }
        verify(exactly = 0) { Utils.getAppIcon(any()) }
    }

    @Test
    fun `getNotificationBitmap should return app icon when image loading fails and fallbackToAppIcon is true`() {
        // Given
        val icoPath = "https://example.com/icon.png"
        val fallbackToAppIcon = true
        val mockAppBitmap = mockk<Bitmap>()

        every { Utils.getAppIcon(mockContext) } returns mockAppBitmap

        // Spy on templateMediaManager to mock getImageBitmap
        val spyTemplateMediaManager = spyk(templateMediaManager)
        every { spyTemplateMediaManager.getImageBitmap(icoPath) } returns null

        // When
        val result = spyTemplateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockAppBitmap, result)
        verify { spyTemplateMediaManager.getImageBitmap(icoPath) }
        verify { Utils.getAppIcon(mockContext) }
    }

    @Test
    fun `getNotificationBitmap should return null when image loading fails and fallbackToAppIcon is false`() {
        // Given
        val icoPath = "https://example.com/icon.png"
        val fallbackToAppIcon = false

        // Spy on templateMediaManager to mock getImageBitmap
        val spyTemplateMediaManager = spyk(templateMediaManager)
        every { spyTemplateMediaManager.getImageBitmap(icoPath) } returns null

        // When
        val result = spyTemplateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertNull(result)
        verify { spyTemplateMediaManager.getImageBitmap(icoPath) }
        verify(exactly = 0) { Utils.getAppIcon(any()) }
    }

    @Test(expected = NullPointerException::class)
    fun `getNotificationBitmap should throw NullPointerException when context is null and fallback needed`() {
        // Given
        val icoPath: String? = null
        val fallbackToAppIcon = true
        val nullContext: Context? = null

        every { Utils.getAppIcon(nullContext) } throws NullPointerException("Context cannot be null")

        // When
        templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, nullContext)

        // Then - Exception should be thrown
    }

    @Test
    fun `getNotificationBitmap should handle whitespace in icoPath`() {
        // Given
        val icoPath = "   "
        val fallbackToAppIcon = true
        val mockBitmap = mockk<Bitmap>()

        every { Utils.getAppIcon(mockContext) } returns mockBitmap

        // When
        val result = templateMediaManager.getNotificationBitmap(icoPath, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockBitmap, result)
        verify { Utils.getAppIcon(mockContext) }
    }

    // Tests for getImageBitmap method

    @Test
    fun `getImageBitmap should return null when imageUrl is null`() {
        // Given
        val imageUrl: String? = null

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }

    @Test
    fun `getImageBitmap should return null when imageUrl is blank`() {
        // Given
        val imageUrl = "   "

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }

    @Test
    fun `getImageBitmap should return null when imageUrl does not start with https`() {
        // Given
        val imageUrl = "http://example.com/image.jpg"

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }

    @Test
    fun `getImageBitmap should return bitmap when valid imageUrl and download succeeds`() {
        // Given
        val imageUrl = "https://example.com/image.jpg"
        val mockBitmap = mockk<Bitmap>()
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap
        every { mockTemplateRepository.getBitmap(imageUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertEquals(mockBitmap, result)
        verify { mockTemplateRepository.getBitmap(imageUrl) }
    }

    @Test
    fun `getImageBitmap should return null when download fails`() {
        // Given
        val imageUrl = "https://example.com/image.jpg"
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.DOWNLOAD_FAILED
        every { mockTemplateRepository.getBitmap(imageUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify { mockTemplateRepository.getBitmap(imageUrl) }
    }

    @Test
    fun `getImageBitmap should handle empty string imageUrl`() {
        // Given
        val imageUrl = ""

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }


    @Test
    fun `getImageBitmap should handle various download failure statuses`() {
        // Given
        val imageUrl = "https://example.com/image.jpg"
        val statuses = listOf(
            DownloadedBitmap.Status.DOWNLOAD_FAILED,
            DownloadedBitmap.Status.NO_IMAGE,
            DownloadedBitmap.Status.SIZE_LIMIT_EXCEEDED
        )

        statuses.forEach { status ->
            val mockDownloadedBitmap = mockk<DownloadedBitmap>()
            every { mockDownloadedBitmap.status } returns status
            every { mockTemplateRepository.getBitmap(imageUrl) } returns mockDownloadedBitmap

            // When
            val result = templateMediaManager.getImageBitmap(imageUrl)

            // Then
            assertNull(result)
        }
    }

    @Test
    fun `getImageBitmap should work with different image extensions`() {
        // Given
        val imageUrls = listOf(
            "https://example.com/image.jpg",
            "https://example.com/image.png",
            "https://example.com/image.webp",
            "https://example.com/image.jpeg"
        )
        val mockBitmap = mockk<Bitmap>()
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap
        every { mockTemplateRepository.getBitmap(any()) } returns mockDownloadedBitmap

        imageUrls.forEach { imageUrl ->
            // When
            val result = templateMediaManager.getImageBitmap(imageUrl)

            // Then
            assertEquals(mockBitmap, result)
            verify { mockTemplateRepository.getBitmap(imageUrl) }
        }
    }

    @Test
    fun `getImageBitmap should handle URLs with fragments`() {
        // Given
        val imageUrl = "https://example.com/image.jpg#section1"
        val mockBitmap = mockk<Bitmap>()
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap
        every { mockTemplateRepository.getBitmap(imageUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertEquals(mockBitmap, result)
        verify { mockTemplateRepository.getBitmap(imageUrl) }
    }

    @Test
    fun `getImageBitmap should reject ftp URLs`() {
        // Given
        val imageUrl = "ftp://example.com/image.jpg"

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }

    @Test
    fun `getImageBitmap should reject file URLs`() {
        // Given
        val imageUrl = "file:///path/to/image.jpg"

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertNull(result)
        verify(exactly = 0) { mockTemplateRepository.getBitmap(any()) }
    }

    @Test
    fun `should handle complete gif processing flow`() {
        // Given
        val gifUrl = "https://example.com/animation.gif"
        val maxFrames = 5
        val mockBytes = ByteArray(200)
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()
        val mockFrames = listOf(mockk<Bitmap>(), mockk<Bitmap>())
        val expectedGifResult = GifResult.Success(mockFrames, 3000)

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bytes } returns mockBytes
        every { mockTemplateRepository.getBytes(gifUrl) } returns mockDownloadedBitmap
        every { mockGifDecoder.decode(mockBytes, maxFrames) } returns expectedGifResult

        // When
        val result = templateMediaManager.getGifFrames(gifUrl, maxFrames)

        // Then
        assertEquals(expectedGifResult, result)
        assertTrue(result is GifResult.Success)
        assertEquals(mockFrames, (result as GifResult.Success).frames)
        assertEquals(3000, result.duration)
        verify { mockTemplateRepository.getBytes(gifUrl) }
        verify { mockGifDecoder.decode(mockBytes, maxFrames) }
    }

    @Test
    fun `should handle complete image processing flow`() {
        // Given
        val imageUrl = "https://example.com/image.jpg"
        val mockBitmap = mockk<Bitmap>()
        val mockDownloadedBitmap = mockk<DownloadedBitmap>()

        every { mockDownloadedBitmap.status } returns DownloadedBitmap.Status.SUCCESS
        every { mockDownloadedBitmap.bitmap } returns mockBitmap
        every { mockTemplateRepository.getBitmap(imageUrl) } returns mockDownloadedBitmap

        // When
        val result = templateMediaManager.getImageBitmap(imageUrl)

        // Then
        assertEquals(mockBitmap, result)
        verify { mockTemplateRepository.getBitmap(imageUrl) }
    }

    @Test
    fun `should handle notification icon with fallback flow`() {
        // Given
        val invalidIconUrl = "https://example.com/nonexistent.png"
        val fallbackToAppIcon = true
        val mockAppBitmap = mockk<Bitmap>()

        every { Utils.getAppIcon(mockContext) } returns mockAppBitmap

        // Spy on templateMediaManager to mock getImageBitmap
        val spyTemplateMediaManager = spyk(templateMediaManager)
        every { spyTemplateMediaManager.getImageBitmap(invalidIconUrl) } returns null

        // When
        val result = spyTemplateMediaManager.getNotificationBitmap(invalidIconUrl, fallbackToAppIcon, mockContext)

        // Then
        assertEquals(mockAppBitmap, result)
        verify { spyTemplateMediaManager.getImageBitmap(invalidIconUrl) }
        verify { Utils.getAppIcon(mockContext) }
    }

    // Tests for getStyledImageBitmap
    //
    // RemoteViews de-duplicates bitmaps by object identity when it marshals its parcel, and from
    // Android 17 a parcel that exceeds 1.5 x screen width x height x 4 bytes of bitmaps is a fatal
    // error. Handing back the same instance for a repeated (url, style) is what keeps a styled
    // notification the same size on the wire as an unstyled one.

    private fun stubSuccessfulDownload(url: String): Bitmap {
        val bitmap = Bitmap.createBitmap(120, 60, Bitmap.Config.ARGB_8888)
        every { mockTemplateRepository.getBitmap(url) } returns
                DownloadedBitmap(bitmap, DownloadedBitmap.Status.SUCCESS, 1L, null)
        return bitmap
    }

    @Test
    fun `getStyledImageBitmap should return the raw bitmap when styling is inactive`() {
        // Given
        val url = "https://example.com/image.png"
        val raw = stubSuccessfulDownload(url)

        // Then - no styling keys, and a payload that parses but cannot draw, both skip compositing
        assertSame(raw, templateMediaManager.getStyledImageBitmap(url, null))
        assertSame(raw, templateMediaManager.getStyledImageBitmap(url, ImageBorderData()))
        assertSame(
            raw,
            templateMediaManager.getStyledImageBitmap(url, ImageBorderData(borderWidthPercent = 5f))
        )
    }

    @Test
    fun `getStyledImageBitmap should reuse one styled instance per url and style`() {
        // Given - the shape that Text over Image and Vertical Image with CTA produce, where the
        // collapsed view falls back to the expanded view's image URL
        val url = "https://example.com/image.png"
        val raw = stubSuccessfulDownload(url)
        val style = ImageBorderData(cornerRadiusPercent = 20f)

        // When - two slots ask for the same image with the same styling
        val first = templateMediaManager.getStyledImageBitmap(url, style)
        val second = templateMediaManager.getStyledImageBitmap(url, style)

        // Then - one styled bitmap, shared, so the parcel carries the image once
        assertNotNull(first)
        assertNotSame(raw, first)
        assertSame(first, second)
    }

    @Test
    fun `getStyledImageBitmap should composite separately for a different style`() {
        // Given
        val url = "https://example.com/image.png"
        stubSuccessfulDownload(url)

        // When - the same image with two different styles
        val rounded = templateMediaManager.getStyledImageBitmap(url, ImageBorderData(cornerRadiusPercent = 20f))
        val moreRounded = templateMediaManager.getStyledImageBitmap(url, ImageBorderData(cornerRadiusPercent = 40f))

        // Then - the cache key includes the style, so these must not be conflated
        assertNotSame(rounded, moreRounded)
    }

    @Test
    fun `getStyledImageBitmap should return null when the image cannot be fetched`() {
        // Given
        val url = "https://example.com/missing.png"
        every { mockTemplateRepository.getBitmap(url) } returns
                DownloadedBitmap(null, DownloadedBitmap.Status.DOWNLOAD_FAILED, 1L, null)

        // Then - a failed download is not masked by the styling step
        assertNull(templateMediaManager.getStyledImageBitmap(url, ImageBorderData(cornerRadiusPercent = 20f)))
        assertNull(templateMediaManager.getStyledImageBitmap(null, ImageBorderData(cornerRadiusPercent = 20f)))
    }

    @Test
    fun `clearCaches should drop styled bitmaps too`() {
        // Given - a styled bitmap is cached
        val url = "https://example.com/image.png"
        stubSuccessfulDownload(url)
        val style = ImageBorderData(cornerRadiusPercent = 20f)
        val first = templateMediaManager.getStyledImageBitmap(url, style)

        // When
        templateMediaManager.clearCaches()
        stubSuccessfulDownload(url)

        // Then - nothing survives the clear, so a later render starts from a fresh download
        assertNotSame(first, templateMediaManager.getStyledImageBitmap(url, style))
    }
}
