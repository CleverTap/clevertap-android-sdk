package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.inapp.CTLocalInApp.InAppType.ALERT
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.*
import org.skyscreamer.jsonassert.JSONAssert

class CTLocalInAppTest : BaseTestCase() {

    @Test
    fun test_CTLocalInApp() {
        val actualJsonObject =
            CTLocalInApp.builder().setInAppType(ALERT).setTitleText("titleText").setMessageText("messageText")
                .followDeviceOrientation(true).setPositiveBtnText("Agree").setNegativeBtnText("Decline")
                .setBackgroundColor("#FF0000").setBtnBackgroundColor("#00FF00").setBtnBorderColor("#000000")
                .setBtnBorderRadius("#FFFFFF").setBtnTextColor("#FFFF00").setFallbackToSettings(true)
                .setTitleTextColor("#F0F000").setMessageTextColor("#F000F0").setImageUrl("https://abc.com/xyz.jpg")
                .build()

        val expectedJsonObject =
            JSONObject(
                "{\"type\":\"alert-template\",\"isLocalInApp\":true,\"close\":true,\"title\":" +
                        "{\"text\":\"titleText\",\"color\":\"#F0F000\"},\"message\":{\"text\":\"messageText\"," +
                        "\"color\":\"#F000F0\"},\"hasPortrait\":true,\"hasLandscape\":true,\"buttons\":" +
                        "[{\"text\":\"Agree\",\"radius\":\"#FFFFFF\",\"bg\":\"#00FF00\",\"border\":\"#000000\"," +
                        "\"color\":\"#FFFF00\"},{\"text\":\"Decline\",\"radius\":\"#FFFFFF\",\"bg\":\"#00FF00\"," +
                        "\"border\":\"#000000\",\"color\":\"#FFFF00\"}],\"bg\":\"#FF0000\"," +
                        "\"fallbackToNotificationSettings\":true,\"media\":{\"url\":\"https:\\/\\/abc.com\\/xyz.jpg\"," +
                        "\"content_type\":\"image\"},\"mediaLandscape\":{\"url\":\"https:\\/\\/abc.com\\/xyz.jpg\"," +
                        "\"content_type\":\"image\"}}"
            )
        JSONAssert.assertEquals(expectedJsonObject, actualJsonObject, true)
    }
}