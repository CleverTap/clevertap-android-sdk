package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.createCtInAppNotification
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import io.mockk.*
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomTemplateContextTest {

    @Test
    fun `getValue functions should apply overrides correctly`() {
        val templateContext = createTestTemplateContext()

        assertEquals(VARS_OVERRIDE_BOOLEAN, templateContext.getBoolean("boolean"))
        assertEquals(VARS_OVERRIDE_STRING, templateContext.getString("string"))
        assertEquals(VARS_OVERRIDE_BYTE, templateContext.getByte("byte"))
        assertEquals(VARS_OVERRIDE_LONG, templateContext.getLong("long"))
        assertEquals(VARS_OVERRIDE_DOUBLE, templateContext.getDouble("double"))
        assertEquals(35, templateContext.getInt("noOverrideInt"))
        assertNull(templateContext.getBoolean("overrideWithoutDefinitionBoolean"))
    }

    @Test
    fun `getValue functions should return null when argument name is not defined in the template`() {
        val templateContext = createTestTemplateContext()

        assertNull(templateContext.getBoolean("overrideWithoutDefinitionBoolean"))
        assertNull(templateContext.getMap("notDefinedMap"))
        assertNull(templateContext.getLong("notDefinedLong"))
        assertNull(templateContext.getByte("notDefinedByte"))
        assertNull(templateContext.getShort("notDefinedShort"))
        assertNull(templateContext.getInt("notDefinedInt"))
        assertNull(templateContext.getDouble("notDefinedDouble"))
        assertNull(templateContext.getFloat("notDefinedFloat"))
        assertNull(templateContext.getString("notDefinedString"))
    }

    @Test
    fun `getMap should directly inflate inner maps, apply overrides and cast correctly`() {
        val templateContext = createTestTemplateContext()
        val notificationVars = templateNotificationJson.getJSONObject("vars")

        val innerMap = templateContext.getMap("map.innerMap")!!
        verifyInnerMap(notificationVars, innerMap)

        val innermostMap = templateContext.getMap("map.innerMap.innermostMap")!!
        verifyInnermostMap(notificationVars, innermostMap)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `getMap should inflate arguments to maps, apply overrides  and cast correctly`() {
        val templateContext = createTestTemplateContext()
        val notificationVars = templateNotificationJson.getJSONObject("vars")

        val map = templateContext.getMap("map")!!
        assertEquals(notificationVars.getInt("map.short").toShort(), map["short"])
        assertEquals(notificationVars.getDouble("map.float").toFloat(), map["float"])
        assertEquals(25, map["noOverrideInt"])

        val innerMap = map["innerMap"] as Map<String, Any>
        verifyInnerMap(notificationVars, innerMap)

        val innermostMap = innerMap["innermostMap"] as Map<String, Any>
        verifyInnermostMap(notificationVars, innermostMap)
    }

    private fun verifyInnerMap(vars: JSONObject, map: Map<String, Any>) {
        assertEquals(vars.getBoolean("map.innerMap.boolean"), map["boolean"])
        assertEquals(vars.getString("map.innerMap.string"), map["string"])
        assertEquals(vars.getInt("map.innerMap.byte").toByte(), map["byte"])
        assertEquals(vars.getInt("map.innerMap.int"), map["int"])
        assertEquals(vars.getLong("map.innerMap.long"), map["long"])
        assertEquals(vars.getDouble("map.innerMap.double"), map["double"])
        assertEquals(15, map["noOverrideInt"])
    }

    private fun verifyInnermostMap(vars: JSONObject, map: Map<String, Any>) {
        assertEquals(vars.getInt("map.innerMap.innermostMap.int"), map["int"])
        assertEquals(vars.getString("map.innerMap.innermostMap.string"), map["string"])
        assertEquals(vars.getBoolean("map.innerMap.innermostMap.boolean"), map["boolean"])
        assertEquals(true, map["noOverrideBoolean"])
    }

    private fun createTestTemplateContext() = TemplateContext(
        templateDefinition,
        createCtInAppNotification(templateNotificationJson),
        mockk<InAppListener>(),
        mockk<Logger>()
    )

    private val templateDefinition = template {
        name("nestedArgsTemplate")
        presenter(mockk<TemplatePresenter>())
        booleanArgument("boolean", false)
        stringArgument("string", "Default")
        byteArgument("byte", 0)
        longArgument("long", 5435050)
        doubleArgument("double", 12.5)
        shortArgument("map.short", 0)
        intArgument("noOverrideInt", 35)
        intArgument("map.noOverrideInt", 25)
        mapArgument(
            "map", mapOf(
                "float" to 15.6.toFloat(),
                "innerMap" to mapOf(
                    "boolean" to false,
                    "string" to "Default",
                    "noOverrideInt" to 15
                )
            )
        )
        mapArgument(
            "map.innerMap", mapOf(
                "byte" to 0.toByte(),
                "int" to 0,
                "long" to 0.toLong(),
                "innermostMap" to mapOf(
                    "int" to 0,
                    "string" to "Default",
                    "boolean" to false,
                    "noOverrideBoolean" to true
                )
            )
        )
        doubleArgument("map.innerMap.double", 0.0)
    }

    private val templateNotificationJson = JSONObject(
        """
        {
            "templateName": "nestedArgsTemplate",
            "type": "custom-code",
            "vars": {
                "boolean": $VARS_OVERRIDE_BOOLEAN,
                "string": "$VARS_OVERRIDE_STRING",
                "byte": $VARS_OVERRIDE_BYTE,
                "long": $VARS_OVERRIDE_LONG,
                "double": $VARS_OVERRIDE_DOUBLE,
                "overrideWithoutDefinitionBoolean": false,
                "map.short": 123,
                "map.float": 15.6,
                "map.innerMap.boolean": true,
                "map.innerMap.string": "String",
                "map.innerMap.byte": 1,
                "map.innerMap.int": 1345,
                "map.innerMap.long": 21474836470,
                "map.innerMap.double": 3402823466385288.0,
                "map.innerMap.innermostMap.int": 1024,
                "map.innerMap.innermostMap.string": "innerText",
                "map.innerMap.innermostMap.boolean": true
                }
            }
        }
    """.trimIndent()
    )

    companion object {

        private const val VARS_OVERRIDE_BOOLEAN = true
        private const val VARS_OVERRIDE_STRING = "Text"
        private const val VARS_OVERRIDE_BYTE = 1.toByte()
        private const val VARS_OVERRIDE_LONG = 21474836475L
        private const val VARS_OVERRIDE_DOUBLE = 3402823466385285.0
    }
}
