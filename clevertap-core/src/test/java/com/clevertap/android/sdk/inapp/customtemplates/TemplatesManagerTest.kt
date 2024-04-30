package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.createCtInAppNotification
import io.mockk.*
import org.json.JSONObject
import org.junit.*
import org.junit.Test
import org.junit.jupiter.api.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplatesManagerTest {

    private val mockTemplatePresenter = mockk<TemplatePresenter>()
    private val mockFunctionPresenter = mockk<FunctionPresenter>()

    @After
    fun cleanUp() {
        TemplatesManager.clearRegisteredProducers()
    }

    @Test
    fun `registered templates should be available in TemplateManager instances`() {
        val templateName1 = "template1"
        val templateName2 = "template2"
        val functionName1 = "function1"

        TemplatesManager.register {
            templatesSet(
                template {
                    name(templateName1)
                    presenter(mockTemplatePresenter)
                    stringArgument("arg", "string")
                },
                template {
                    name(templateName2)
                    presenter(mockTemplatePresenter)
                    booleanArgument("bool", false)
                    fileArgument("file")
                },
                function(isVisual = false) {
                    name(functionName1)
                    presenter(mockFunctionPresenter)
                    intArgument("int", 10)
                }
            )
        }

        val ctConfig1 = getMockedCtInstanceConfig("account1", "token1")
        val ctConfig2 = getMockedCtInstanceConfig("account2", "token2")

        val templatesManager = TemplatesManager.createInstance(ctConfig1)

        assertTrue(templatesManager.isTemplateRegistered(templateName1))
        assertTrue(templatesManager.isTemplateRegistered(templateName2))
        assertTrue(templatesManager.isTemplateRegistered(functionName1))

        assertFalse(templatesManager.isTemplateRegistered("non-registered"))

        val templatesManagerNewConfig = TemplatesManager.createInstance(ctConfig2)

        assertTrue(templatesManagerNewConfig.isTemplateRegistered(templateName1))
        assertTrue(templatesManagerNewConfig.isTemplateRegistered(templateName2))
        assertTrue(templatesManagerNewConfig.isTemplateRegistered(functionName1))

        assertFalse(templatesManagerNewConfig.isTemplateRegistered("non-registered"))
    }

    @Test
    fun `createInstance() should throw when templates with the same names are registered`() {
        val templateName = "template"

        TemplatesManager.register {
            templatesSet(
                template {
                    name(templateName)
                    presenter(mockTemplatePresenter)
                }
            )
        }

        TemplatesManager.register {
            templatesSet(
                template {
                    name(templateName)
                    presenter(mockTemplatePresenter)
                }
            )
        }
        assertThrows<CustomTemplateException> {
            TemplatesManager.createInstance(getMockedCtInstanceConfig("account", "token"))
        }
    }

    @Test
    fun `presentTemplate should trigger presenter onPresent`() {
        val functionPresenter = mockk<FunctionPresenter>(relaxed = true)
        val templatePresenter = mockk<TemplatePresenter>(relaxed = true)

        TemplatesManager.register {
            templatesSet(
                function(isVisual = false) {
                    name(SIMPLE_FUNCTION_NAME)
                    presenter(functionPresenter)
                    stringArgument("string", "Default")
                },
                template {
                    name(SIMPLE_TEMPLATE_NAME)
                    presenter(templatePresenter)
                    stringArgument("string", "Default")
                }
            )
        }

        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templatesManager = TemplatesManager.createInstance(getMockedCtInstanceConfig("account", "token"))

        templatesManager.presentTemplate(
            notification = createCtInAppNotification(simpleFunctionNotificationJson),
            inAppListener = mockInAppListener
        )
        verify { functionPresenter.onPresent(any()) }

        templatesManager.presentTemplate(
            notification = createCtInAppNotification(simpleTemplateNotificationJson),
            inAppListener = mockInAppListener
        )
        verify { templatePresenter.onPresent(any()) }
    }

    @Test
    fun `presentTemplate should do nothing on notification with non-registered template`() {
        val functionPresenter = mockk<FunctionPresenter>(relaxed = true)

        TemplatesManager.register {
            templatesSet(
                function(isVisual = false) {
                    name(SIMPLE_FUNCTION_NAME)
                    presenter(functionPresenter)
                    stringArgument("string", "Default")
                }
            )
        }

        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templatesManager = TemplatesManager.createInstance(getMockedCtInstanceConfig("account", "token"))

        templatesManager.presentTemplate(createCtInAppNotification(simpleTemplateNotificationJson), mockInAppListener)

        verify { functionPresenter wasNot called }
    }

    @Test
    fun `closeTemplate should trigger presenter onClose`() {
        val templatePresenter = mockk<TemplatePresenter>(relaxed = true)

        TemplatesManager.register {
            templatesSet(
                template {
                    name(SIMPLE_TEMPLATE_NAME)
                    presenter(templatePresenter)
                    stringArgument("string", "Default")
                }
            )
        }

        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templatesManager = TemplatesManager.createInstance(getMockedCtInstanceConfig("account", "token"))

        templatesManager.closeTemplate(createCtInAppNotification(simpleTemplateNotificationJson), mockInAppListener)
        verify { templatePresenter.onClose(any()) }
    }

    private fun getMockedCtInstanceConfig(account: String, token: String): CleverTapInstanceConfig {
        return mockk<CleverTapInstanceConfig>().apply {
            every { accountId } returns account
            every { accountToken } returns token
            every { logger } returns mockk<Logger>(relaxed = true)
        }
    }

    private val simpleTemplateNotificationJson = JSONObject(
        """
        {
            "templateName": "$SIMPLE_TEMPLATE_NAME",
            "type": "$CUSTOM_CODE",
            "vars": {
                "string": "Template"
            }
        }
        """.trimIndent()
    )

    private val simpleFunctionNotificationJson = JSONObject(
        """
        {
            "templateName": "$SIMPLE_FUNCTION_NAME",
            "type": "$CUSTOM_CODE",
            "vars": {
                "string": "Function"
            }
        }
        """.trimIndent()
    )

    private companion object {

        private const val SIMPLE_FUNCTION_NAME = "function"
        private const val SIMPLE_TEMPLATE_NAME = "template"
    }
}
