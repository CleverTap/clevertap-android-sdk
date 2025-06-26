package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.CTLocalInApp.InAppType.ALERT
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.assertEquals

class CTLocalInAppTest : BaseTestCase() {

    @Test
    fun test_CTLocalInApp() {
        val actualJsonObject =
            CTLocalInApp.builder().setInAppType(ALERT).setTitleText("titleText").setMessageText("messageText")
                .followDeviceOrientation(true).setPositiveBtnText("Agree").setNegativeBtnText("Decline")
                .setBackgroundColor("#FF0000").setBtnBackgroundColor("#00FF00").setBtnBorderColor("#000000")
                .setBtnBorderRadius("#FFFFFF").setBtnTextColor("#FFFF00").setFallbackToSettings(true)
                .setTitleTextColor("#F0F000").setMessageTextColor("#F000F0").setImageUrl("https://abc.com/xyz.jpg", "Alt Text")
                .build()

        // Arrange: Create the expected JSON object
        val expectedJsonObject = JSONObject().apply {
            put("type", "alert-template")
            put("isLocalInApp", true)
            put("close", true)

            put("title", JSONObject().apply {
                put("text", "titleText")
                put("color", "#F0F000")
            })

            put("message", JSONObject().apply {
                put("text", "messageText")
                put("color", "#F000F0")
            })

            put("hasPortrait", true)
            put("hasLandscape", true)

            put("buttons", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", "Agree")
                    put("radius", "#FFFFFF")
                    put("actions", JSONObject().apply {
                        put("type", "close")
                    })
                    put("bg", "#00FF00")
                    put("border", "#000000")
                    put("color", "#FFFF00")
                })
                put(JSONObject().apply {
                    put("text", "Decline")
                    put("radius", "#FFFFFF")
                    put("bg", "#00FF00")
                    put("border", "#000000")
                    put("color", "#FFFF00")
                })
            })

            put("bg", "#FF0000")
            put("fallbackToNotificationSettings", true)

            val mediaObject = JSONObject().apply {
                put("url", "https://abc.com/xyz.jpg")
                put("content_type", "image")
            }

            put("media", mediaObject)
            put("mediaLandscape", mediaObject)
        }

        // Assert: Check if the actual JSON object matches the expected JSON object
        assertEquals(expectedJsonObject.toString(), actualJsonObject.toString())
    }
}