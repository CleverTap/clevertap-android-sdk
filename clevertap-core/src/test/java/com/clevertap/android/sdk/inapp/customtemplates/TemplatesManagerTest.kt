package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig
import io.mockk.*
import org.junit.*
import org.junit.Test
import org.junit.jupiter.api.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplatesManagerTest {

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
                    stringArgument("arg", "string")
                },
                template {
                    name(templateName2)
                    booleanArgument("bool", false)
                    fileArgument("file")
                },
                function(isVisual = false) {
                    name(functionName1)
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
                template { name(templateName) }
            )
        }

        TemplatesManager.register {
            templatesSet(
                template { name(templateName) }
            )
        }
        assertThrows<CustomTemplateException> {
            TemplatesManager.createInstance(getMockedCtInstanceConfig("account", "token"))
        }
    }

    private fun getMockedCtInstanceConfig(account: String, token: String): CleverTapInstanceConfig {
        return mockk<CleverTapInstanceConfig>().apply {
            every { accountId } returns account
            every { accountToken } returns token
        }
    }
}
