package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PIPConfigFactoryTest {

    private val mockLogger = mockk<Logger>(relaxed = true)
    private val mockCallbacks = mockk<PIPCallbacks>()

    // ─── Helper ───────────────────────────────────────────────────────────────────

    private fun mockNotification(
        pipJson: JSONObject? = JSONObject(),
        mediaUrl: String = "https://example.com/video.m3u8",
        isVideo: Boolean = true,
        isGIF: Boolean = false,
        isImage: Boolean = false,
        contentType: String = "video/mp4",
        fallbackUrl: String = "",
        topLevelJson: JSONObject? = null,
    ): CTInAppNotification {
        val media = mockk<CTInAppNotificationMedia> {
            every { this@mockk.mediaUrl } returns mediaUrl
            every { this@mockk.contentType } returns contentType
            every { isVideo() } returns isVideo
            every { isGIF() } returns isGIF
            every { isImage() } returns isImage
        }

        val rawJson = topLevelJson ?: JSONObject().apply {
            if (pipJson != null) put("pip", pipJson)
            put("media", JSONObject().apply {
                put("url", mediaUrl)
                put("content_type", contentType)
                if (fallbackUrl.isNotBlank()) put("fallback_url", fallbackUrl)
            })
        }

        return mockk {
            every { pipConfigJson } returns pipJson
            every { getInAppMediaForOrientation(any()) } returns media
            every { jsonDescription } returns rawJson
        }
    }

    // ─── Null/invalid input tests ─────────────────────────────────────────────────

    @Test
    fun `returns null when pipConfigJson is null`() {
        val notification = mockk<CTInAppNotification> {
            every { pipConfigJson } returns null
        }
        assertNull(PIPConfigFactory.create(notification, mockCallbacks, mockLogger))
    }

    @Test
    fun `returns null when media is null`() {
        val notification = mockk<CTInAppNotification> {
            every { pipConfigJson } returns JSONObject()
            every { getInAppMediaForOrientation(any()) } returns null
        }
        assertNull(PIPConfigFactory.create(notification, mockCallbacks, mockLogger))
    }

    @Test
    fun `returns null when media url is blank`() {
        val notification = mockNotification(mediaUrl = "", isVideo = true)
        assertNull(PIPConfigFactory.create(notification, mockCallbacks, mockLogger))
    }

    @Test
    fun `returns null for unsupported media type`() {
        val notification = mockNotification(
            isVideo = false, isGIF = false, isImage = false,
            contentType = "audio/mp3"
        )
        assertNull(PIPConfigFactory.create(notification, mockCallbacks, mockLogger))
    }

    // ─── Media type mapping ───────────────────────────────────────────────────────

    @Test
    fun `maps video content type to VIDEO`() {
        val notification = mockNotification(isVideo = true, isGIF = false, isImage = false)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(PIPMediaType.VIDEO, config.mediaType)
    }

    @Test
    fun `maps gif content type to GIF`() {
        val notification = mockNotification(
            mediaUrl = "https://example.com/anim.gif",
            isVideo = false, isGIF = true, isImage = false,
            contentType = "image/gif"
        )
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(PIPMediaType.GIF, config.mediaType)
    }

    @Test
    fun `maps image content type to IMAGE`() {
        val notification = mockNotification(
            mediaUrl = "https://example.com/photo.jpg",
            isVideo = false, isGIF = false, isImage = true,
            contentType = "image/jpeg"
        )
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(PIPMediaType.IMAGE, config.mediaType)
    }

    // ─── Position mapping ─────────────────────────────────────────────────────────

    @Test
    fun `maps all position strings correctly`() {
        val positionMap = mapOf(
            "top-left" to PIPPosition.TOP_LEFT,
            "top-center" to PIPPosition.TOP_CENTER,
            "top-right" to PIPPosition.TOP_RIGHT,
            "center-left" to PIPPosition.LEFT_CENTER,
            "center" to PIPPosition.CENTER,
            "center-right" to PIPPosition.RIGHT_CENTER,
            "bottom-left" to PIPPosition.BOTTOM_LEFT,
            "bottom-center" to PIPPosition.BOTTOM_CENTER,
            "bottom-right" to PIPPosition.BOTTOM_RIGHT,
        )
        for ((jsonValue, expectedPosition) in positionMap) {
            val pipJson = JSONObject().put("position", jsonValue)
            val notification = mockNotification(pipJson = pipJson)
            val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
            assertNotNull(config, "Config should not be null for position: $jsonValue")
            assertEquals(expectedPosition, config.initialPosition, "Position mismatch for: $jsonValue")
        }
    }

    @Test
    fun `unknown position falls back to default`() {
        val pipJson = JSONObject().put("position", "invalid-position")
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        // Default position is BOTTOM_RIGHT (from PIPConfig.Builder)
        assertEquals(PIPPosition.BOTTOM_RIGHT, config.initialPosition)
    }

    // ─── Animation mapping ────────────────────────────────────────────────────────

    @Test
    fun `maps animation strings correctly`() {
        val animMap = mapOf(
            "instant" to PIPAnimation.INSTANT,
            "dissolve" to PIPAnimation.DISSOLVE,
            "move-in" to PIPAnimation.MOVE_IN,
            "move_in" to PIPAnimation.MOVE_IN,
            "movein" to PIPAnimation.MOVE_IN,
        )
        for ((jsonValue, expectedAnim) in animMap) {
            val pipJson = JSONObject().put("animation", jsonValue)
            val notification = mockNotification(pipJson = pipJson)
            val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
            assertNotNull(config, "Config should not be null for animation: $jsonValue")
            assertEquals(expectedAnim, config.animation, "Animation mismatch for: $jsonValue")
        }
    }

    @Test
    fun `unknown animation falls back to default`() {
        val pipJson = JSONObject().put("animation", "slide-up")
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        // Default animation is DISSOLVE (from PIPConfig.Builder)
        assertEquals(PIPAnimation.DISSOLVE, config.animation)
    }

    // ─── Config field extraction ──────────────────────────────────────────────────

    @Test
    fun `reads fallback url from media json`() {
        val notification = mockNotification(fallbackUrl = "https://fallback.com/img.jpg")
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals("https://fallback.com/img.jpg", config.fallbackUrl)
    }

    @Test
    fun `reads margins from pip json`() {
        val pipJson = JSONObject().put("margins", JSONObject().apply {
            put("vertical", 10)
            put("horizontal", 20)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(10, config.verticalEdgeMarginDp)
        assertEquals(20, config.horizontalEdgeMarginDp)
    }

    @Test
    fun `reads width from pip json`() {
        val pipJson = JSONObject().put("width", 40)
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(40, config.widthPercent)
    }

    @Test
    fun `reads corner radius from pip json`() {
        val pipJson = JSONObject().put("cornerRadius", 12)
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(12, config.cornerRadiusDp)
    }

    @Test
    fun `reads border config from pip json`() {
        val pipJson = JSONObject().put("border", JSONObject().apply {
            put("enabled", true)
            put("color", "#FF0000")
            put("width", 2)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertNotNull(config.border)
        assertTrue(config.border!!.enabled)
        assertEquals("#FF0000", config.border!!.color)
        assertEquals(2, config.border!!.widthDp)
    }

    @Test
    fun `reads redirect url from onClick`() {
        val pipJson = JSONObject().put("onClick", JSONObject().apply {
            put("android", "https://www.example.com")
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals("https://www.example.com", config.redirectUrl)
    }

    @Test
    fun `reads close button from top-level json`() {
        val pipJson = JSONObject()
        val topLevelJson = JSONObject().apply {
            put("pip", pipJson)
            put("close", false)
            put("media", JSONObject().apply {
                put("url", "https://example.com/video.m3u8")
                put("content_type", "video/mp4")
            })
        }
        val notification = mockNotification(pipJson = pipJson, topLevelJson = topLevelJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(false, config.showCloseButton)
    }

    // ─── Builder validation failure ───────────────────────────────────────────────

    @Test
    fun `returns null when builder validation fails`() {
        // width=0 is outside the valid range 10..90
        val pipJson = JSONObject().put("width", 0)
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNull(config)
    }
}
