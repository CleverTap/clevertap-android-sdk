package com.clevertap.android.sdk.network.api

import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.customtemplates.templatesSet
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals

class DefineTemplatesRequestBodyTest {

    @Test
    fun `template body should construct proper json`() {
        val expectedJson = JSONArray(expectedJsonString)

        val header = JSONObject().apply {
            put("type", "meta")
        }
        val templates = templatesSet(
            template {
                name("templateName1")
                booleanArgument("var1", true)
                mapArgument(
                    "var2", mapOf(
                        "option2" to "X",
                        "option1" to 137
                    )
                )
                fileArgument("var3")
                actionArgument("var4")
            },
            function(true) {
                name("templateName2")
                floatArgument("var1", 12.5f)
            }
        )

        assertEquals(expectedJson.toString(), DefineTemplatesRequestBody(header, templates).toString())
    }

    private val expectedJsonString = """
        [
          {
            "type": "meta"
          },
          {
            "type": "templatePayload",
            "definitions": {
                "templateName1": {
                    "type": "template",
                    "vars": {
                        "var1": {
                            "defaultValue": true,
                            "type": "boolean",
                            "order": 0
                        },
                        "var2.option2": {
                            "defaultValue": "X",
                            "type": "string",
                            "order": 2
                        },
                        "var2.option1": {
                            "defaultValue": 137,
                            "type": "number",
                            "order": 1
                        },
                        "var3": {
                            "type": "file",
                            "order": 3
                        },
                        "var4": {
                            "type": "action",
                            "order": 4
                        }
                    }
                },
                "templateName2": {
                    "type": "function",
                    "vars": {
                        "var1" : {
                            "defaultValue": 12.5,
                            "type": "number",
                            "order": 0
                        }
                    }
                }
            }
          }
        ]
    """
}
