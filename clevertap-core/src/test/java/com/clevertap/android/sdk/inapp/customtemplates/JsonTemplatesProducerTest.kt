package com.clevertap.android.sdk.inapp.customtemplates

import io.mockk.mockk
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals


class JsonTemplatesProducerTest {

    private val templatesPresenter = object : TemplatePresenter {
        override fun onClose(context: CustomTemplateContext.TemplateContext) {
        }

        override fun onPresent(context: CustomTemplateContext.TemplateContext) {
        }

    }

    private val functionPresenter = FunctionPresenter { }

    @Test
    fun `producer should create correct templates from json definitions`() {
        val templatesJson = "{$template1,$function1}"

        val templates = JsonTemplatesProducer(templatesJson, templatesPresenter, functionPresenter)
            .defineTemplates(mockk())

        assertEquals(2, templates.size)

        val template1 = templates.find { it.name == "template-1" }!!
        assertEquals(CustomTemplateType.TEMPLATE, template1.type)

        val args1 = template1.args
        assertEquals("Default", args1.find { it.name == "string" }!!.defaultValue)
        assertEquals(0.0, args1.find { it.name == "number" }!!.defaultValue)
        assertEquals(true, args1.find { it.name == "boolean" }!!.defaultValue)
        assertEquals(null, args1.find { it.name == "file" }!!.defaultValue)
        assertEquals(null, args1.find { it.name == "action" }!!.defaultValue)
        assertEquals("Inner Default", args1.find { it.name == "map.innerString" }!!.defaultValue)
        assertEquals(1.0, args1.find { it.name == "map.innerNumber" }!!.defaultValue)
        assertEquals(false, args1.find { it.name == "map.innerBoolean" }!!.defaultValue)
        assertEquals(
            "Innermost Default",
            args1.find { it.name == "map.innerMap.innermostString" }!!.defaultValue
        )

        val function2 = templates.find { it.name == "function-2" }!!
        assertEquals(CustomTemplateType.FUNCTION, function2.type)
        assertEquals(false, function2.isVisual)

        val args2 = function2.args
        assertEquals("Default", args2.find { it.name == "functionString" }!!.defaultValue)
        assertEquals(0.0, args2.find { it.name == "functionNumber" }!!.defaultValue)
        assertEquals(true, args2.find { it.name == "functionBoolean" }!!.defaultValue)
        assertEquals(null, args2.find { it.name == "functionFile" }!!.defaultValue)
        assertEquals(
            "Inner Default",
            args2.find { it.name == "functionMap.innerString" }!!.defaultValue
        )
    }

    @Test
    fun `producer should throw exception when an invalid json is provided`() {
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer("[]", templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }
    }

    @Test
    fun `producer should throw when appropriate presenter is not provided`() {
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer("{$template1}", null, functionPresenter)
                .defineTemplates(mockk())
        }
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer("{$function1}", templatesPresenter, null)
                .defineTemplates(mockk())
        }
    }

    @Test
    fun `producer should throw when invalid values are provided in the json templates`() {
        val invalidTemplateTypeJson = """
            {
                "template": {
                    "type": "string",
                    "arguments": {
                        "string": {
                            "type": "string",
                            "value": "Text"
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidTemplateTypeJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }

        val invalidArgumentTypeJson = """
            {
                "template": {
                    "type": "template",
                    "arguments": {
                        "json": {
                            "type": "json",
                            "value": {
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidArgumentTypeJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }

        val invalidFileValueJson = """
            {
                "template": {
                    "type": "template",
                    "arguments": {
                        "file": {
                            "type": "file",
                            "value": "https://files.example.com/file.pdf"
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidFileValueJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }
        val invalidFunctionFileValueJson = """
            {
                "template": {
                    "type": "function",
                    "arguments": {
                        "file": {
                            "type": "file",
                            "value": "https://files.example.com/file.pdf"
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(
                invalidFunctionFileValueJson,
                templatesPresenter,
                functionPresenter
            )
                .defineTemplates(mockk())
        }

        val invalidActionValueJson = """
            {
                "template": {
                    "type": "template",
                    "arguments": {
                        "action": {
                            "type": "action",
                            "value": "function1"
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidActionValueJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }

        val invalidFunctionActionJson = """
            {
                "template": {
                    "type": "function",
                    "isVisual": true,
                    "arguments": {
                        "action": {
                            "type": "action"
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidFunctionActionJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }

        val invalidNestedFileJson = """
            {
                "template": {
                    "type": "template",
                    "arguments": {
                        "map": {
                            "type": "object",
                            "value": {
                                "file": {
                                    "type": "file"
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        assertThrows<CustomTemplateException> {
            JsonTemplatesProducer(invalidNestedFileJson, templatesPresenter, functionPresenter)
                .defineTemplates(mockk())
        }
    }

    private val template1 = """
                "template-1": {
                    "type": "template",
                    "arguments": {
                        "string": {
                            "type": "string",
                            "value": "Default"
                        },
                        "number": {
                            "type": "number",
                            "value": 0
                        },
                        "boolean": {
                            "type": "boolean",
                            "value": true
                        },
                        "file": {
                            "type": "file"
                        },
                        "action": {
                            "type": "action"
                        },
                        "map": {
                            "type": "object",
                            "value": {
                                "innerString": {
                                    "type": "string",
                                    "value": "Inner Default"
                                },
                                "innerNumber": {
                                    "type": "number",
                                    "value": 1
                                },
                                "innerBoolean": {
                                    "type": "boolean",
                                    "value": "false"
                                },
                                "innerMap": {
                                    "type": "object",
                                    "value": {
                                        "innermostString": {
                                            "type": "string",
                                            "value": "Innermost Default"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    """.trimIndent()

    private val function1 = """
                "function-2": {
                    "type": "function",
                    "isVisual": false,
                    "arguments": {
                        "functionString": {
                            "type": "string",
                            "value": "Default"
                        },
                        "functionNumber": {
                            "type": "number",
                            "value": 0
                        },
                        "functionBoolean": {
                            "type": "boolean",
                            "value": true
                        },
                        "functionFile": {
                            "type": "file"
                        },
                        "functionMap": {
                            "type": "object",
                            "value": {
                                "innerString": {
                                    "type": "string",
                                    "value": "Inner Default"
                                }
                            }
                        }
                    }
                }
    """.trimIndent()
}
