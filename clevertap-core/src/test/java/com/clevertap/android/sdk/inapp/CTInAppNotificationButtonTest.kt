package com.clevertap.android.sdk.inapp

import android.os.Parcel
import com.clevertap.android.shared.test.AndroidTest
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CTInAppNotificationButtonTest: AndroidTest() {

    @Test
    fun `Constructor should parse json objects into field values`() {
        val button = CTInAppNotificationButton(JSONObject(buttonJson))

        assertEquals(TEXT, button.text)
        assertEquals(TEXT_COLOR, button.textColor)
        assertEquals(BACKGROUND, button.backgroundColor)
        assertEquals(BORDER_COLOR, button.borderColor)
        assertEquals(BORDER_RADIUS, button.borderRadius)
        assertTrue(button.action?.type == InAppActionType.CLOSE)
    }

    @Test
    fun `Objects should be correctly parceled`() {
        val parcel = Parcel.obtain()
        val button = CTInAppNotificationButton(JSONObject(buttonJson))

        button.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val buttonFromParcel = CTInAppNotificationButton.CREATOR.createFromParcel(parcel)
        assertEquals(button.text, buttonFromParcel.text)
        assertEquals(button.textColor, buttonFromParcel.textColor)
        assertEquals(button.backgroundColor, buttonFromParcel.backgroundColor)
        assertEquals(button.borderColor, buttonFromParcel.borderColor)
        assertEquals(button.borderRadius, buttonFromParcel.borderRadius)
        assertEquals(button.action?.type, buttonFromParcel.action?.type)
    }

    companion object {
        private const val TEXT = "Button"
        private const val TEXT_COLOR = "#000000"
        private const val BACKGROUND = "#1EB858"
        private const val BORDER_COLOR = "#D9D9D9"
        private const val BORDER_RADIUS = "4"
        private val buttonJson =
            """
{
    "text": "$TEXT",
    "color": "$TEXT_COLOR",
    "bg": "$BACKGROUND",
    "border": "$BORDER_COLOR",
    "radius": "$BORDER_RADIUS",
    "actions": {
        "close": true,
        "type": "close",
        "android": "",
        "ios": "",
        "kv": {}
    }
}""".trimIndent()
    }
}
