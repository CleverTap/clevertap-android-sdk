package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.FunctionBuilder
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.TemplateBuilder
import io.mockk.*
import org.junit.Test
import org.junit.jupiter.api.*
import kotlin.test.assertEquals

class CustomTemplateTest {

    private val mockTemplatePresenter = mockk<TemplatePresenter>()
    private val mockFunctionPresenter = mockk<FunctionPresenter>()

    @Test
    fun `builder should throw when empty map is set as argument`() {
        template {
            name("template")
            presenter(mockTemplatePresenter)
            assertThrows<CustomTemplateException> {
                mapArgument("emptyMap", mapOf())
            }
        }
    }

    @Test
    fun `builder should throw if name is not set when build is called`() {
        assertThrows<CustomTemplateException> {
            TemplateBuilder().presenter(mockTemplatePresenter).build()
        }
        assertThrows<CustomTemplateException> {
            FunctionBuilder(isVisual = false).presenter(mockFunctionPresenter).build()
        }
    }

    @Test
    fun `builder should throw when name is blank or empty`() {
        assertThrows<CustomTemplateException> {
            template {
                name("")
                presenter(mockTemplatePresenter)
            }
        }
        assertThrows<CustomTemplateException> {
            template {
                name("  ")
                presenter(mockTemplatePresenter)
            }
        }
    }

    @Test
    fun `builder should throw when arg name has invalid dots`() {
        assertThrows<CustomTemplateException> {
            template {
                name("template")
                actionArgument(".name")
                presenter(mockTemplatePresenter)
            }
        }
        assertThrows<CustomTemplateException> {
            template {
                name("template")
                fileArgument("name.")
                presenter(mockTemplatePresenter)
            }
        }
        assertThrows<CustomTemplateException> {
            template {
                name("template")
                fileArgument("na..me")
                presenter(mockTemplatePresenter)
            }
        }
        assertDoesNotThrow {
            template {
                name("template")
                intArgument("na.me", 0)
                presenter(mockTemplatePresenter)
            }
        }
    }

    @Test
    fun `builder should throw when blank argument name is provided`() {
        template {
            name("template")
            presenter(mockTemplatePresenter)
            assertThrows<CustomTemplateException> {
                stringArgument("", "")
            }
            assertThrows<CustomTemplateException> {
                intArgument("  ", 1)
            }
        }
    }

    @Test
    fun `builder should throw when no presenter if provided`() {
        assertThrows<CustomTemplateException> {
            template {
                name("template")
            }
        }
    }

    @Test
    fun `builder should flatten map values and keep order`() {

        val expectedOrder = listOf("a.b", "a.c", "a.d.e", "a.d.f", "a.d.g", "a.d.h.i", "a.j", "a.k")

        val template = template {
            name("template")
            presenter(mockTemplatePresenter)
            mapArgument(
                "a",
                mapOf(
                    "b" to 1.toByte(),
                    "c" to 2.toShort(),
                    "d" to mapOf(
                        "e" to 3,
                        "f" to 4.5,
                        "g" to 2.3f,
                        "h" to mapOf(
                            "i" to 20L
                        )
                    ),
                    "j" to true,
                    "k" to "string"
                )
            )
        }

        assertEquals(expectedOrder, template.args.map { it.name })
    }

    @Test
    fun `builder should throw when name is set more than once`() {
        val name = "template"
        template {
            name(name)
            presenter(mockTemplatePresenter)
            assertThrows<CustomTemplateException> {
                name(name)
            }
        }
    }

    @Test
    fun `builder should throw when unsupported map values are provided`() {
        template {
            name("template")
            presenter(mockTemplatePresenter)
            assertThrows<CustomTemplateException> {
                mapArgument(
                    "map",
                    mapOf("a" to Unit)
                )
            }

            assertThrows<CustomTemplateException> {
                mapArgument(
                    "map",
                    mapOf(
                        "a" to mapOf(
                            "b" to Unit
                        )
                    )
                )
            }

            assertThrows<CustomTemplateException> {
                mapArgument(
                    "map",
                    mapOf("" to "string")
                )
            }
        }
    }

    @Test
    fun `builder should throw when arguments have the same name`() {
        function(isVisual = false) {
            name("function")
            presenter(mockFunctionPresenter)
            stringArgument("arg", "default")
            assertThrows<CustomTemplateException> {
                intArgument("arg", 0)
            }
            assertThrows<CustomTemplateException> {
                booleanArgument("arg", false)
            }
            assertThrows<CustomTemplateException> {
                longArgument("arg", 0)
            }
            assertThrows<CustomTemplateException> {
                shortArgument("arg", 0)
            }
            assertThrows<CustomTemplateException> {
                byteArgument("arg", 0)
            }
            assertThrows<CustomTemplateException> {
                floatArgument("arg", 0f)
            }
            assertThrows<CustomTemplateException> {
                doubleArgument("arg", 0.0)
            }
            assertThrows<CustomTemplateException> {
                fileArgument("arg")
            }
        }

        template {
            name("template")
            presenter(mockTemplatePresenter)
            actionArgument("arg")
            assertThrows<CustomTemplateException> {
                mapArgument("arg", mapOf("asd" to 5))
            }
        }
    }

    @Test
    fun `builder should throw when parent args are already defined`() {
        template {
            name("template")
            presenter(mockTemplatePresenter)
            stringArgument("a.b", "")
            assertThrows<CustomTemplateException> {
                stringArgument("a", "")
            }
            assertThrows<CustomTemplateException> {
                stringArgument("a.b", "")
            }
        }

        template {
            name("template")
            presenter(mockTemplatePresenter)
            stringArgument("a.b.c.d", "")
            assertThrows<CustomTemplateException> {
                stringArgument("a", "")
            }
            assertThrows<CustomTemplateException> {
                stringArgument("a.b", "")
            }
        }

        template {
            name("template")
            presenter(mockTemplatePresenter)
            stringArgument("a.a.a", "")
            stringArgument("a.a.b", "")
            assertThrows<CustomTemplateException> {
                stringArgument("a.a.a.d", "")
            }
            assertThrows<CustomTemplateException> {
                stringArgument("a", "")
            }
            assertThrows<CustomTemplateException> {
                stringArgument("a.a", "")
            }
        }
    }

    @Test
    fun `builder should merge and sort hierarchical args and maintain order for other args`() {

        val expectedOrder = listOf(
            "b", "c", "d", "e.f.a", "e.f.c", "e.f.d", "e.f.e", "e.g", "e.h", "e.w", "l", "k", "a.m", "a.n"
        )

        val template = template {
            name("name")
            presenter(mockTemplatePresenter)
            stringArgument("b", "")
            stringArgument("c", "")
            stringArgument("d", "")
            mapArgument(
                "e",
                mapOf(
                    "g" to "",
                    "h" to "",
                    "f" to mapOf(
                        "c" to "",
                        "e" to "",
                        "d" to ""
                    )
                ),
            )
            stringArgument("l", "")
            stringArgument("k", "")
            stringArgument("e.w", "")
            stringArgument("e.f.a", "")
            mapArgument(
                "a",
                mapOf(
                    "n" to "",
                    "m" to ""
                ),
            )
        }

        assertEquals(expectedOrder, template.args.map { it.name })
    }

    @Test
    fun `builder should support map keys with dots`() {
        val expectedArgNames = listOf("e.g.h", "e.j.k.l")

        val template = function(isVisual = false) {
            name("name")
            presenter(mockFunctionPresenter)
            mapArgument(
                "e",
                mapOf(
                    "g.h" to "Text",
                    "j.k.l" to "Text"
                )
            )
        }

        assertEquals(expectedArgNames, template.args.map { it.name })
    }
}
