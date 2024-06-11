package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals

class CustomTemplateInAppDataTest {

    @Test
    fun `copy() should create objects that are equal to the original`() {
        val inAppData = CustomTemplateInAppData.createFromJson(inAppDataJson)!!
        val copy = inAppData.copy()

        assertEquals(inAppData, copy)
        assertEquals(inAppData.hashCode(), copy.hashCode())
    }

    @Test
    fun `writeFieldsToJson should apply all fields set in the object`() {
        val inAppData = CustomTemplateInAppData.createFromJson(inAppDataJson)!!
        val json = JSONObject()
        json.put("type", CUSTOM_CODE.toString())
        inAppData.writeFieldsToJson(json)
        val newInAppData = CustomTemplateInAppData.createFromJson(json)!!
        assertEquals(inAppData, newInAppData)
    }

    private val inAppDataJson = JSONObject(
        """
        {
            "templateName": "template",
            "templateId": "templateId",
            "templateDescription": "Description",
            "type": "$CUSTOM_CODE",
            "vars": {
                "boolean": true,
                "string": "Text",
                "byte": 1
            }
        }
    """.trimIndent()
    )
}
