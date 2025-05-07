package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.createCtInAppNotification
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplatesManagerTest {

    private val mockTemplatePresenter = mockk<TemplatePresenter>()
    private val mockFunctionPresenter = mockk<FunctionPresenter>()
    private val systemTemplates = setOf(
        template {
            isSystemDefined = true
            name(SYSTEM_TEMPLATE_NAME)
            presenter(mockTemplatePresenter)
        },
        function(isVisual = true) {
            isSystemDefined = true
            name(SYSTEM_FUNCTION_NAME)
            presenter(mockFunctionPresenter)
        }
    )

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

        val templatesManager = createTemplatesManager(ctConfig1)

        assertTrue(templatesManager.isTemplateRegistered(templateName1))
        assertTrue(templatesManager.isTemplateRegistered(templateName2))
        assertTrue(templatesManager.isTemplateRegistered(functionName1))

        assertFalse(templatesManager.isTemplateRegistered("non-registered"))

        val templatesManagerNewConfig = createTemplatesManager(ctConfig2)

        assertTrue(templatesManagerNewConfig.isTemplateRegistered(templateName1))
        assertTrue(templatesManagerNewConfig.isTemplateRegistered(templateName2))
        assertTrue(templatesManagerNewConfig.isTemplateRegistered(functionName1))

        assertFalse(templatesManagerNewConfig.isTemplateRegistered("non-registered"))
    }

    @Test
    fun `system templates should always be registered`() {
        val ctConfig = getMockedCtInstanceConfig("account", "token")
        val templatesManager = createTemplatesManager(ctConfig)
        for (systemTemplate in systemTemplates) {
            assertTrue(templatesManager.isTemplateRegistered(systemTemplate.name))
        }
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
        assertThrows(CustomTemplateException::class.java) {
            createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        }
    }

    @Test
    fun `createInstance() should throw when a template with system defined name is registered`() {
        TemplatesManager.register {
            templatesSet(
                template {
                    name(SYSTEM_TEMPLATE_NAME)
                    presenter(mockTemplatePresenter)
                }
            )
        }

        assertThrows(CustomTemplateException::class.java) {
            createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        }
    }

    @Test
    fun `createInstance() should throw when a system template is registered from the public api`() {
        TemplatesManager.register {
            templatesSet(
                template {
                    // possible because isSystemDefined has internal visibility, but it could also
                    // be accessed through java code outside of the library module
                    isSystemDefined = true
                    name("Custom system template")
                    presenter(mockTemplatePresenter)
                }
            )
        }

        assertThrows(CustomTemplateException::class.java) {
            createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        }
    }


    @Test
    fun `getTemplate should return registered templates by name`() {
        TemplatesManager.register {
            templatesSet(
                function(isVisual = false) {
                    name(SIMPLE_FUNCTION_NAME)
                    presenter(mockk())
                    stringArgument("string", "Default")
                },
                template {
                    name(SIMPLE_TEMPLATE_NAME)
                    presenter(mockk())
                    stringArgument("string", "Default")
                }
            )
        }

        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))

        assertEquals(SIMPLE_FUNCTION_NAME, templatesManager.getTemplate(SIMPLE_FUNCTION_NAME)?.name)
        assertEquals(SIMPLE_TEMPLATE_NAME, templatesManager.getTemplate(SIMPLE_TEMPLATE_NAME)?.name)
        assertNull(templatesManager.getTemplate("non-registered"))
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
        val mockFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))

        templatesManager.presentTemplate(
            notification = createCtInAppNotification(simpleFunctionNotificationJson),
            inAppListener = mockInAppListener,
            resourceProvider = mockFileResourceProvider
        )
        verify { functionPresenter.onPresent(any()) }

        templatesManager.presentTemplate(
            notification = createCtInAppNotification(simpleTemplateNotificationJson),
            inAppListener = mockInAppListener,
            resourceProvider = mockFileResourceProvider
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
        val mockFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))

        templatesManager.presentTemplate(createCtInAppNotification(simpleTemplateNotificationJson), mockInAppListener,mockFileResourceProvider)

        verify { functionPresenter wasNot called }
    }

    @Test
    fun `getActiveContextForTemplate should return the same context for currently active templates until they are dismissed`() {
        val functionPresenter = object : FunctionPresenter {
            var templatesManager: TemplatesManager? = null

            override fun onPresent(context: FunctionContext) {
                assertTrue { context === templatesManager?.getActiveContextForTemplate(SIMPLE_FUNCTION_NAME) }
            }
        }

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
        val mockFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        functionPresenter.templatesManager = templatesManager

        templatesManager.presentTemplate(createCtInAppNotification(simpleFunctionNotificationJson), mockInAppListener,mockFileResourceProvider)
        val context = templatesManager.getActiveContextForTemplate(SIMPLE_FUNCTION_NAME)!!
        assertEquals(SIMPLE_FUNCTION_NAME, context.templateName)

        context.setDismissed()
        assertNull(templatesManager.getActiveContextForTemplate(SIMPLE_FUNCTION_NAME))
    }

    @Test
    fun `getActiveContextForTemplate should return null for non-active templates`() {
        TemplatesManager.register {
            templatesSet(
                function(isVisual = false) {
                    name(SIMPLE_FUNCTION_NAME)
                    presenter(mockk(relaxed = true))
                    stringArgument("string", "Default")
                }
            )
        }

        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        assertNull(templatesManager.getActiveContextForTemplate(SIMPLE_TEMPLATE_NAME))
        assertNull(templatesManager.getActiveContextForTemplate(SIMPLE_FUNCTION_NAME))
    }

    @Test
    fun `closeTemplate should trigger presenter onClose for currently active templates`() {
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
        val mockFileResourceProvider = mockk<FileResourceProvider>(relaxed = true)
        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        val notification = createCtInAppNotification(simpleTemplateNotificationJson)

        templatesManager.presentTemplate(notification, mockInAppListener, mockFileResourceProvider)
        templatesManager.closeTemplate(notification)
        verify { templatePresenter.onClose(any()) }
    }

    @Test
    fun `closeTemplate should not trigger presenter onClose for not currently active templates`() {
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
        val templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))

        // not registered template
        templatesManager.closeTemplate(createCtInAppNotification(simpleFunctionNotificationJson))
        verify { templatePresenter wasNot called }

        // not active template
        val notification = createCtInAppNotification(simpleTemplateNotificationJson)
        templatesManager.closeTemplate(notification)
        verify { templatePresenter wasNot called }
    }

    @Test
    fun `getAllRegisteredTemplates should return only non-system templates`() {
        var templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        assertEquals(0, templatesManager.getAllRegisteredTemplates().size)

        TemplatesManager.register {
            templatesSet(
                template {
                    name(SIMPLE_TEMPLATE_NAME)
                    presenter(mockTemplatePresenter)
                    stringArgument("string", "Default")
                }
            )
        }

        templatesManager = createTemplatesManager(getMockedCtInstanceConfig("account", "token"))
        assertEquals(1, templatesManager.getAllRegisteredTemplates().size)
    }

    private fun getMockedCtInstanceConfig(account: String, token: String): CleverTapInstanceConfig {
        return mockk<CleverTapInstanceConfig>().apply {
            every { accountId } returns account
            every { accountToken } returns token
            every { logger } returns mockk<Logger>(relaxed = true)
        }
    }

    private fun createTemplatesManager(ctConfig: CleverTapInstanceConfig): TemplatesManager {
        return TemplatesManager.createInstance(ctConfig, HashSet(systemTemplates))
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
        private const val SYSTEM_TEMPLATE_NAME = "system-template"
        private const val SYSTEM_FUNCTION_NAME = "system-function"
    }
}
