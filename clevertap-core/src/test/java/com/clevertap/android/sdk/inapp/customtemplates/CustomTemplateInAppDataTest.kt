package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.copyFrom
import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import io.mockk.*
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals

class CustomTemplateInAppDataTest {

    private lateinit var templatesManager: TemplatesManager
    private val keyTemplateName = "templateName"
    private val keyVars = "vars"
    private val presenter = object : TemplatePresenter {
        override fun onClose(context: TemplateContext) {
            // NO OP
        }

        override fun onPresent(context: TemplateContext) {
            // NO OP
        }
    }

    @Before
    fun setUp() {
        templatesManager = mockk<TemplatesManager>()
    }

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

    @Test
    fun `getFileArgsUrls returns empty list when templateName is null`() {
        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, null)
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getFileArgsUrls returns empty list when template is not found`() {
        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, "non_existent_template")
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)
        every { templatesManager.getTemplate("non_existent_template") } returns null

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getFileArgsUrls returns empty list when template is found but args are empty`() {
        val templateName = "test_template"
        val template = template {
            name(templateName)
            presenter(presenter)
        }

        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, templateName)
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)
        every { templatesManager.getTemplate(templateName) } returns template

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getFileArgsUrls returns list of file URLs when template is found`() {
        val templateName = "test_template"
        val template = template {
            name(templateName)
            fileArgument("arg1")
            fileArgument("arg2")
            presenter(presenter)
        }

        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, templateName)
        obj.put(keyVars, JSONObject().put("arg1", "file_url_1")
            .put("arg2", "file_url_2"))
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)
        every { templatesManager.getTemplate(templateName) } returns template

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(listOf("file_url_1", "file_url_2"), result)
    }

    @Test
    fun `getFileArgsUrls skips non-file arguments`() {
        val templateName = "test_template"
        val template = template {
            name(templateName)
            fileArgument("arg1")
            fileArgument("arg2")
            stringArgument("arg3", "str1")
            booleanArgument("arg4", true)
            doubleArgument("arg5",3.1444)
            intArgument("arg6",100)
            presenter(presenter)
        }

        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, templateName)
        obj.put(keyVars, JSONObject().put("arg1", "file_url_1")
            .put("arg2", "file_url_2").put("arg3","newStr1")
            .put("arg4",false).put("arg5",0.00001).put("arg6",999))
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)
        every { templatesManager.getTemplate(templateName) } returns template

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(listOf("file_url_1", "file_url_2"), result)
    }


    @Test
    fun `getFileArgsUrls skips arguments not present in args`() {
        val templateName = "test_template"
        val template = template {
            name(templateName)
            fileArgument("arg1")
            fileArgument("arg2")
            presenter(presenter)
        }

        val obj = JSONObject()
        obj.copyFrom(inAppDataJson)
        obj.put(keyTemplateName, templateName)
        obj.put(keyVars, JSONObject().put("arg1", "file_url_1"))
        val customTemplateInAppData = CustomTemplateInAppData.createFromJson(obj)
        every { templatesManager.getTemplate(templateName) } returns template

        val result = customTemplateInAppData!!.getFileArgsUrls(templatesManager)
        assertEquals(listOf("file_url_1"), result)
    }

    @Test
    fun `getFileArgsUrls retrieves file args in both the template and its actions`() {
        val url1 = "url1"
        val url2 = "url2"
        val mainTemplateName = "templateWithAction"
        val actionTemplateName = "actionTemplate"
        val inAppDataJson = JSONObject(
            """
            {
            "templateName": "$mainTemplateName",
            "templateId": "templateWithActionId",
            "templateDescription": "Description",
            "type": "$CUSTOM_CODE",
            "vars": {
                "file": "$url1",
                "action": {
                    "templateName": "$actionTemplateName",
                    "templateId": "actionTemplateId",
                    "templateDescription": "Description",
                    "type": "$CUSTOM_CODE",
                    "vars": {
                        "file": "$url2"
                    }
                }
            }
            }
        """.trimIndent()
        )

        val mainTemplate = template {
            name(mainTemplateName)
            presenter(presenter)
            fileArgument("file")
            actionArgument("action")
        }
        val actionTemplate = function(false) {
            name(actionTemplateName)
            presenter {}
            fileArgument("file")
        }

        every { templatesManager.getTemplate(mainTemplateName) } returns mainTemplate
        every { templatesManager.getTemplate(actionTemplateName) } returns actionTemplate

        val customInAppData = CustomTemplateInAppData.createFromJson(inAppDataJson)
        val fileUrls = customInAppData!!.getFileArgsUrls(templatesManager)

        assertEquals(listOf(url1, url2), fileUrls)
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
