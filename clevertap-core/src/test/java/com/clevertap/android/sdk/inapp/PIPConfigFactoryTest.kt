package com.clevertap.android.sdk.inapp

import android.graphics.Color
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.pipsdk.PIPAnimation
import com.clevertap.android.sdk.inapp.pipsdk.PIPCallbacks
import com.clevertap.android.sdk.inapp.pipsdk.PIPMediaType
import com.clevertap.android.sdk.inapp.pipsdk.PIPPosition
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
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
            every { this@mockk.contentDescription } returns ""
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
    fun `maps animation object type correctly`() {
        val animMap = mapOf(
            "instant" to PIPAnimation.INSTANT,
            "dissolve" to PIPAnimation.DISSOLVE,
            "move-in" to PIPAnimation.MOVE_IN,
            "move_in" to PIPAnimation.MOVE_IN,
            "movein" to PIPAnimation.MOVE_IN,
        )
        for ((jsonValue, expectedAnim) in animMap) {
            val animJson = JSONObject().put("type", jsonValue).put("duration", 500)
            val pipJson = JSONObject().put("animation", animJson)
            val notification = mockNotification(pipJson = pipJson)
            val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
            assertNotNull(config, "Config should not be null for animation type: $jsonValue")
            assertEquals(expectedAnim, config.animationConfig.type, "Animation type mismatch for: $jsonValue")
            assertEquals(500L, config.animationConfig.durationMs)
        }
    }

    @Test
    fun `missing animation object falls back to default`() {
        val pipJson = JSONObject() // no animation field
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        // Default animation is DISSOLVE (from PIPConfig.Builder)
        assertEquals(PIPAnimation.DISSOLVE, config.animationConfig.type)
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
        assertEquals(10, config.verticalEdgeMarginPercent)
        assertEquals(20, config.horizontalEdgeMarginPercent)
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

    // ─── Aspect ratio ──────────────────────────────────────────────────────────────

    @Test
    fun `reads aspect ratio from pip json`() {
        val pipJson = JSONObject().put("aspectRatio", JSONObject().apply {
            put("numerator", 9)
            put("denominator", 16)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(9.0, config.aspectRatioNumerator)
        assertEquals(16.0, config.aspectRatioDenominator)
    }

    @Test
    fun `ignores invalid aspect ratio values`() {
        val pipJson = JSONObject().put("aspectRatio", JSONObject().apply {
            put("numerator", 0)
            put("denominator", -1)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        // Should keep defaults
        assertEquals(16.0, config.aspectRatioNumerator)
        assertEquals(9.0, config.aspectRatioDenominator)
    }

    // ─── Controls ─────────────────────────────────────────────────────────────────

    @Test
    fun `reads controls from pip json`() {
        val pipJson = JSONObject().put("controls", JSONObject().apply {
            put("drag", false)
            put("playPause", false)
            put("mute", false)
            put("expandCollapse", false)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(false, config.dragEnabled)
        assertEquals(false, config.showPlayPauseButton)
        assertEquals(false, config.showMuteButton)
        assertEquals(false, config.showExpandCollapseButton)
    }

    @Test
    fun `controls default to true when not present`() {
        val pipJson = JSONObject()
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(true, config.dragEnabled)
        assertEquals(true, config.showPlayPauseButton)
        assertEquals(true, config.showMuteButton)
        assertEquals(true, config.showExpandCollapseButton)
    }

    // ─── Border & corner radius ──────────────────────────────────────────────────

    @Test
    fun `reads cornerRadius from pip json`() {
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
        assertTrue(config.borderEnabled)
        assertEquals(Color.RED, config.borderColor)
        assertEquals(2, config.borderWidthDp)
    }

    @Test
    fun `border defaults to disabled when not present`() {
        val pipJson = JSONObject()
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(false, config.borderEnabled)
        assertEquals(0, config.cornerRadiusDp)
        assertEquals(0, config.borderWidthDp)
    }

    @Test
    fun `invalid border color falls back to black`() {
        val pipJson = JSONObject().put("border", JSONObject().apply {
            put("enabled", true)
            put("color", "not-a-color")
            put("width", 1)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertEquals(Color.BLACK, config.borderColor)
    }

    // ─── Action / onClick ───────────────────────────────────────────────────────

    @Test
    fun `action is null when onClick type is empty (no action)`() {
        val pipJson = JSONObject().put("onClick", JSONObject().apply {
            put("type", "")
            put("android", "")
            put("ios", "")
            put("kv", JSONObject())
            put("close", false)
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertNull(config.action)
    }

    @Test
    fun `action is set when onClick type is url`() {
        val pipJson = JSONObject().put("onClick", JSONObject().apply {
            put("type", "url")
            put("android", "https://example.com")
        })
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNotNull(config)
        assertNotNull(config.action)
    }

    // ─── Validation failure ───────────────────────────────────────────────────────

    @Test
    fun `returns null when validation fails`() {
        // width=0 is outside the valid range 10..90
        val pipJson = JSONObject().put("width", 0)
        val notification = mockNotification(pipJson = pipJson)
        val config = PIPConfigFactory.create(notification, mockCallbacks, mockLogger)
        assertNull(config)
    }
}
