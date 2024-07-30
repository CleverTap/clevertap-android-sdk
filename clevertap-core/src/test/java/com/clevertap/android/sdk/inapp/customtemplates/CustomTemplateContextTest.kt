package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.InAppActionType.CLOSE
import com.clevertap.android.sdk.inapp.InAppActionType.CUSTOM_CODE
import com.clevertap.android.sdk.inapp.InAppActionType.OPEN_URL
import com.clevertap.android.sdk.inapp.InAppListener
import com.clevertap.android.sdk.inapp.createCtInAppNotification
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.ContextDismissListener
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.FunctionContext
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import io.mockk.*
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomTemplateContextTest {

    @Test
    fun `createContext factory method should create contexts of correct types`() {
        val templateContext = CustomTemplateContext.createContext(
            template = templateDefinition,
            notification = createCtInAppNotification(templateNotificationJson),
            inAppListener = mockk(),
            resourceProvider = mockk(),
            dismissListener = mockk(),
            logger = mockk()
        )

        assertTrue(templateContext is TemplateContext)

        val functionContext = CustomTemplateContext.createContext(
            template = functionDefinition,
            notification = createCtInAppNotification(functionNotificationJson),
            inAppListener = mockk(),
            resourceProvider = mockk(),
            dismissListener = mockk(),
            logger = mockk()
        )

        assertTrue(functionContext is FunctionContext)
    }

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

        val functionContext = CustomTemplateContext.createContext(
            template = functionDefinition,
            notification = createCtInAppNotification(functionNotificationJson),
            inAppListener = mockk(),
            resourceProvider = mockk(),
            dismissListener = mockk(),
            logger = mockk()
        )

        assertEquals(VARS_OVERRIDE_STRING, functionContext.getMap("map")?.get("string"))
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

    @Test
    fun `getMap should include actions as action name or type`() {
        val templateContext = createTestTemplateContext()

        val actionsMap = templateContext.getMap("map.actions")!!
        assertEquals(VARS_ACTION_FUNCTION_NAME, actionsMap["function"])
        assertEquals(CLOSE.toString(), actionsMap["close"])
    }

    @Test
    fun `triggerAction should trigger the correct action through inAppListener`() {
        val mockInAppListener = createMockInAppListener()
        val templateContext = createTestTemplateContext(mockInAppListener)

        val closeActionArg = "map.actions.close"
        templateContext.triggerActionArgument(closeActionArg)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match {
                    it.type == CLOSE
                },
                callToAction = closeActionArg,
                additionalData = any(),
                activityContext = any()
            )
        }

        templateContext.triggerActionArgument("map.actions.function")
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match {
                    it.type == CUSTOM_CODE && it.customTemplateInAppData?.templateName == VARS_ACTION_FUNCTION_NAME
                },
                callToAction = VARS_ACTION_FUNCTION_NAME,
                additionalData = any(),
                activityContext = any()
            )
        }

        val openUrlActionArg = "map.actions.openUrl"
        templateContext.triggerActionArgument(openUrlActionArg)
        verify {
            mockInAppListener.inAppNotificationActionTriggered(
                inAppNotification = any(),
                action = match {
                    it.type == OPEN_URL && it.actionUrl == VARS_ACTION_OPEN_URL_ADDRESS
                },
                callToAction = openUrlActionArg,
                additionalData = any(),
                activityContext = any()
            )
        }
    }

    @Test
    fun `triggerAction should not trigger inAppListener for non-existent action arguments`() {
        val mockInAppListener = createMockInAppListener()
        val templateContext = createTestTemplateContext(mockInAppListener)

        templateContext.triggerActionArgument("nonexistent")
        verify { mockInAppListener wasNot called }
    }

    @Test
    fun `setDismissed should notify InAppListener`() {
        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templateContext = createTestTemplateContext(mockInAppListener)

        templateContext.setDismissed()
        verify { mockInAppListener.inAppNotificationDidDismiss(any(), any()) }
    }

    @Test
    fun `setDismissed should notify dismissListener exactly once`() {
        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val dismissListener = mockk<ContextDismissListener>(relaxed = true)
        val templateContext = createTestTemplateContext(mockInAppListener, dismissListener)

        templateContext.setDismissed()
        verify(exactly = 1) { dismissListener.onDismissContext(templateContext) }

        templateContext.setDismissed()
        verify(exactly = 1) { dismissListener.onDismissContext(templateContext) }
    }

    @Test
    fun `setPresented should notify InAppListener`() {
        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templateContext = createTestTemplateContext(mockInAppListener)

        templateContext.setPresented()
        verify { mockInAppListener.inAppNotificationDidShow(any(), any()) }
    }

    @Test
    fun `InAppListener should not be called again after setDismissed`() {
        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val templateContext = createTestTemplateContext(mockInAppListener)

        templateContext.setDismissed()
        verify(exactly = 1) { mockInAppListener.inAppNotificationDidDismiss(any(), any()) }

        templateContext.setPresented()
        verify(exactly = 0) { mockInAppListener.inAppNotificationDidShow(any(), any()) }

        templateContext.setDismissed()
        verify(exactly = 1) { mockInAppListener.inAppNotificationDidDismiss(any(), any()) }
    }

    @Test
    fun `setPresented and setDismissed should not call InAppListener for templates that are triggered as actions`() {
        val mockInAppListener = mockk<InAppListener>(relaxed = true)
        val notification = createCtInAppNotification(functionNotificationJson)
        notification.customTemplateData.isAction = true
        val functionContext = CustomTemplateContext.createContext(
            template = functionDefinition,
            notification = notification,
            inAppListener = mockInAppListener,
            resourceProvider = mockk(),
            dismissListener = mockk(relaxed = true),
            logger = mockk(relaxed = true)
        )

        functionContext.setPresented()
        functionContext.setDismissed()
        verify { mockInAppListener wasNot called }
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

    private fun createMockInAppListener() = mockk<InAppListener>(relaxed = true)

    private fun createTestTemplateContext(
        inAppListener: InAppListener = mockk(),
        dismissListener: ContextDismissListener? = null
    ) = TemplateContext(
        templateDefinition,
        createCtInAppNotification(templateNotificationJson),
        inAppListener,
        mockk(),
        dismissListener,
        mockk<Logger>(relaxed = true)
    )

    private val templateDefinition = template {
        name(TEMPLATE_NAME_NESTED)
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
                "innerMap.boolean" to false,
                "innerMap" to mapOf(
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
        actionArgument("map.actions.function")
        actionArgument("map.actions.close")
        actionArgument("map.actions.openUrl")
    }

    private val templateNotificationJson = JSONObject(
        """
        {
            "templateName": "$TEMPLATE_NAME_NESTED",
            "type": "$CUSTOM_CODE",
            "vars": {
                "boolean": $VARS_OVERRIDE_BOOLEAN,
                "string": "$VARS_OVERRIDE_STRING",
                "byte": $VARS_OVERRIDE_BYTE,
                "long": $VARS_OVERRIDE_LONG,
                "double": $VARS_OVERRIDE_DOUBLE,
                "overrideWithoutDefinitionBoolean": false,
                "map.actions.close": {
                    "actions": {
                        "type": "$CLOSE"
                    }
                },
                "map.actions.function": {
                    "actions": {
                        "templateName": "$VARS_ACTION_FUNCTION_NAME",
                        "type": "$CUSTOM_CODE",
                        "vars": {
                            "boolean": $VARS_ACTION_OVERRIDE_BOOLEAN,
                            "string": "$VARS_ACTION_OVERRIDE_STRING",
                            "int": $VARS_ACTION_OVERRIDE_INT
                        }
                    }
                },
                "map.actions.openUrl": {
                    "actions": {
                        "type": "$OPEN_URL",
                        "android": "$VARS_ACTION_OPEN_URL_ADDRESS"
                    }
                },
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

    private val functionNotificationJson = JSONObject(
        """
            {
                "templateName": "$FUNCTION_NAME_TOP_LEVEL",
                "type": "$CUSTOM_CODE",
                "vars": {
                    "boolean": $VARS_OVERRIDE_BOOLEAN,
                    "string": "$VARS_OVERRIDE_STRING",
                    "byte": $VARS_OVERRIDE_BYTE,
                    "long": $VARS_OVERRIDE_LONG,
                    "double": $VARS_OVERRIDE_DOUBLE,
                    "map.string": "$VARS_OVERRIDE_STRING",
                    "overrideWithoutDefinitionBoolean": false
                }
            }
        """.trimIndent()
    )

    private val functionDefinition = function(isVisual = false) {
        name(FUNCTION_NAME_TOP_LEVEL)
        presenter(mockk())
        booleanArgument("boolean", false)
        stringArgument("string", "Default")
        byteArgument("byte", 0)
        longArgument("long", 5435050)
        doubleArgument("double", 12.5)
        intArgument("noOverrideInt", 35)
        mapArgument(
            "map", mapOf(
                "string" to "Default"
            )
        )
    }

    companion object {

        private const val TEMPLATE_NAME_NESTED = "nestedArgsTemplate"

        private const val VARS_OVERRIDE_BOOLEAN = true
        private const val VARS_OVERRIDE_STRING = "Text"
        private const val VARS_OVERRIDE_BYTE = 1.toByte()
        private const val VARS_OVERRIDE_LONG = 21474836475L
        private const val VARS_OVERRIDE_DOUBLE = 3402823466385285.0

        private const val VARS_ACTION_FUNCTION_NAME = "function"
        private const val VARS_ACTION_OVERRIDE_BOOLEAN = true
        private const val VARS_ACTION_OVERRIDE_STRING = "Function text"
        private const val VARS_ACTION_OVERRIDE_INT = 5421

        private const val VARS_ACTION_OPEN_URL_ADDRESS = "https://clevertap.com"

        private const val FUNCTION_NAME_TOP_LEVEL = "topLevelFunction"
    }
}
