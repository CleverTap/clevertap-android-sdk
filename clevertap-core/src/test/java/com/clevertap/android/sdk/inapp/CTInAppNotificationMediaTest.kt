package com.clevertap.android.sdk.inapp

import android.os.Parcel
import com.clevertap.android.shared.test.AndroidTest
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CTInAppNotificationMediaTest : AndroidTest() {

    @Test
    fun `create should parse json objects into field values`() {
        val media = CTInAppNotificationMedia.create(JSONObject(mediaJson), orientation = 1)!!

        assertEquals(URL, media.mediaUrl)
        assertEquals(CONTENT_TYPE, media.contentType)
        assertTrue(media.cacheKey!!.contains(KEY))
    }

    @Test
    fun `objects should be parceled correctly`() {
        val parcel = Parcel.obtain()
        val media = CTInAppNotificationMedia.create(JSONObject(mediaJson), orientation = 1)!!

        media.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val mediaFromParcel = CTInAppNotificationMedia.CREATOR.createFromParcel(parcel)

        assertEquals(media.mediaUrl, mediaFromParcel.mediaUrl)
        assertEquals(media.contentType, mediaFromParcel.contentType)
        assertEquals(media.cacheKey, mediaFromParcel.cacheKey)
        assertEquals(media.orientation, mediaFromParcel.orientation)
    }

    companion object {
        private const val URL = "https://clevertap.example/test.jpeg"
        private const val KEY = "047e83120624491e9512dcbd59763767"
        private const val CONTENT_TYPE = "image/jpeg"

        private val mediaJson = """
        {
            "url": "$URL",
            "poster": "",
            "key": "$KEY",
            "content_type": "$CONTENT_TYPE",
            "filename": "",
            "processing": false
        }
        """.trimIndent()
    }
}