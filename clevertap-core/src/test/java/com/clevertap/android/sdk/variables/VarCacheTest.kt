package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.StorageHelper
import com.clevertap.android.sdk.inapp.data.CtCacheType.FILES
import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl
import com.clevertap.android.sdk.variables.VariableDefinitions.NullDefaultValue
import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class VarCacheTest : BaseTestCase() {

    private lateinit var varCache: VarCache
    private lateinit var ctVariables: CTVariables
    private lateinit var parser: Parser
    private lateinit var fileResourcesRepoImpl: FileResourcesRepoImpl
    private lateinit var fileResourceProvider: FileResourceProvider

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        fileResourcesRepoImpl = mockk(relaxed = true)
        fileResourceProvider = mockk(relaxed = true)

        varCache = VarCache(
            cleverTapInstanceConfig, application, fileResourcesRepoImpl, fileResourceProvider
        )
        ctVariables = CTVariables(varCache)
        parser = Parser(ctVariables)
    }

    @Test
    fun `test updateDiffsAndTriggerHandlers`() {
        ctVariables.init()
        val func = mockk<Function0<Unit>>(relaxed = true)

        val var1 = Var.define("var1", 1, ctVariables)
        val var2 = Var.define("group.var2", 2, ctVariables)


        varCache.updateDiffsAndTriggerHandlers(
            mapOf(
                "var1" to 10,
                "group" to mapOf("var2" to 20, "var3" to 30),
            ), func
        )

        assertEquals(10, var1.value())
        assertEquals(20, var2.value())
        assertEquals(30, varCache.getMergedValue("group.var3"))
    }

    @Test
    fun `testUpdateDiffsAndTriggerHandlers for kind file starts download`() {
        ctVariables.init()
        val func = mockk<Function0<Unit>>(relaxed = true)

        val var1 = Var.define("var1", null, "file", ctVariables)
        every { fileResourceProvider.isFileCached(any()) } returns false

        // Call the method under test
        varCache.updateDiffsAndTriggerHandlers(
            mapOf(
                "var1" to "http://example.com/file2",
            ), func
        )

        // Verify interactions and functionality
        assertEquals("http://example.com/file2", var1.stringValue)
        verify { fileResourcesRepoImpl.preloadFilesAndCache(listOf(Pair("http://example.com/file2", FILES)), any()) }
    }

    @Test
    fun `testUpdateDiffsAndTriggerHandlers for kind file when all files are cached`() {
        ctVariables.init()
        val func = mockk<Function0<Unit>>(relaxed = true)

        val var1 = Var.define("var1", null, "file", ctVariables)
        val var2 = Var.define("var2", null, "file", ctVariables)
        every { fileResourceProvider.isFileCached(any()) } returns true

        // Call the method under test
        varCache.updateDiffsAndTriggerHandlers(
            mapOf(
                "var1" to "http://example.com/file2",
                "var2" to "http://dummy.com/file2",
            ), func
        )

        // Verify interactions and functionality
        assertEquals("http://example.com/file2", var1.stringValue)
        assertEquals("http://dummy.com/file2", var2.stringValue)
        verify { func.invoke() }
        verify(exactly = 0) { fileResourcesRepoImpl.preloadFilesAndCache(any(), any()) }
    }

    @Test
    fun `testUpdateDiffsAndTriggerHandlers for kind file when one file is cached`() {
        ctVariables.init()
        val func = mockk<Function0<Unit>>(relaxed = true)

        val var1 = Var.define("var1", null, "file", ctVariables)
        val var2 = Var.define("var2", null, "file", ctVariables)
        every { fileResourceProvider.isFileCached("http://example.com/file2") } returns true
        every { fileResourceProvider.isFileCached("http://dummy.com/file2") } returns false

        // Call the method under test
        varCache.updateDiffsAndTriggerHandlers(
            mapOf(
                "var1" to "http://example.com/file2",
                "var2" to "http://dummy.com/file2",
            ), func
        )

        // Verify interactions and functionality
        assertEquals("http://example.com/file2", var1.stringValue)
        assertEquals("http://dummy.com/file2", var2.stringValue)
        verify { fileResourcesRepoImpl.preloadFilesAndCache(listOf(Pair("http://dummy.com/file2", FILES)), any()) }
    }

    @Test
    fun `testUpdateDiffsAndTriggerHandlers for kind file when url is null`() {
        ctVariables.init()
        val func = mockk<Function0<Unit>>(relaxed = true)

        Var.define("var1", null, "file", ctVariables)
        every { fileResourceProvider.isFileCached(any()) } returns false

        // Call the method under test
        varCache.updateDiffsAndTriggerHandlers(
            mapOf(
                "var1" to null,
            ), func
        )

        // Verify interactions and functionality
        verify { func.invoke() }
        verify(exactly = 0) { fileResourcesRepoImpl.preloadFilesAndCache(any(), any()) }
    }

    @Test
    fun `testRegisterVariable with Parser and null default value`() {
        ctVariables.init()

        parser.parseVariables(NullDefaultValue())

        assertNull(varCache.getVariable<Int>("string_with_null"))
    }

    @Test
    fun `testRegisterVariable with Factory Method and null default value`() {
        ctVariables.init()

        Var.define<String>("var_string", null, ctVariables)

        assertNull(varCache.getVariable<Int>("var_string"))
    }

    @Test
    fun `testRegisterVariable with Factory Method`() {
        ctVariables.init()

        Var.define("var1", 1, ctVariables)
        Var.define("var2", 2, ctVariables)

        assertEquals(2, varCache.variablesCount)
        assertEquals(1, varCache.getVariable<Int>("var1").value())
        assertEquals(2, varCache.getVariable<Int>("var2").value())
    }

    @Test
    fun `testRegisterVariable with group`() {
        ctVariables.init()

        Var.define("group.var1", 1, ctVariables)
        Var.define("group.var2", 2, ctVariables)
        Var.define("group", mutableMapOf("var3" to 3), ctVariables)

        assertEquals(3, varCache.variablesCount)
        assertEquals(1, varCache.getVariable<Int>("group.var1").value())
        assertEquals(2, varCache.getVariable<Int>("group.var2").value())
        assertEquals(mapOf("var1" to 1, "var2" to 2, "var3" to 3), varCache.getVariable<Map<*, *>>("group").value())
    }

    @Test
    fun `testRegisterVariable with nested groups`() {
        ctVariables.init()

        Var.define("group1.group2.var3", 3, ctVariables)
        Var.define("group1.var1", 1, ctVariables)
        Var.define("group1", mapOf("var2" to 2), ctVariables)
        Var.define("group1.group2", mapOf("var4" to 4), ctVariables)

        assertEquals(4, varCache.variablesCount)
        assertEquals(3, varCache.getVariable<Int>("group1.group2.var3").value())
        assertEquals(1, varCache.getVariable<Int>("group1.var1").value())

        var expected = mapOf(
            "var1" to 1, "var2" to 2, "group2" to mapOf("var3" to 3, "var4" to 4)
        )
        assertEquals(expected, varCache.getVariable<Map<*, *>>("group1").value())

        expected = mapOf("var3" to 3, "var4" to 4)
        assertEquals(expected, varCache.getVariable<Map<*, *>>("group1.group2").value())
    }

    @Test
    fun `testRegisterVariable with group and default value`() {
        ctVariables.init()

        Var.define("group.var1", 1, ctVariables)
        Var.define("group", mapOf("var2" to 2), ctVariables)

        assertEquals(2, varCache.variablesCount)
        assertEquals(1, varCache.getVariable<Int>("group.var1").value())
        assertEquals(1, varCache.getVariable<Int>("group.var1").defaultValue())
        assertEquals(mapOf("var1" to 1, "var2" to 2), varCache.getVariable<Map<*, *>>("group").value())
        assertEquals(mapOf("var2" to 2), varCache.getVariable<Map<*, *>>("group").defaultValue())
    }

    @Test
    fun `testRegisterVariable with nested groups and default value`() {
        ctVariables.init()

        Var.define("group1.var1", 1, ctVariables)
        Var.define("group1.group2.var3", 3, ctVariables)
        Var.define("group1", mapOf("var2" to 2, "group2" to mapOf("var4" to 4)), ctVariables)

        assertEquals(3, varCache.variablesCount)
        assertEquals(1, varCache.getVariable<Int>("group1.var1").value())
        assertEquals(3, varCache.getVariable<Int>("group1.group2.var3").value())

        val expectedValue = mapOf(
            "var1" to 1, "var2" to 2, "group2" to mapOf("var3" to 3, "var4" to 4)
        )
        assertEquals(expectedValue, varCache.getVariable<Map<*, *>>("group1").value())

        val expectedDefaultValue = mapOf(
            "var2" to 2, "group2" to mapOf("var4" to 4)
        )
        assertEquals(expectedDefaultValue, varCache.getVariable<Map<*, *>>("group1").defaultValue())
    }

    @Test
    fun `test getMergedValue`() {
        ctVariables.init()
        Var.define("var", 100, ctVariables)
        assertEquals(100, varCache.getMergedValue("var"))
    }

    @Test
    fun `test getMergedValue for kind file`() {
        ctVariables.init()
        Var.define("var1", null, "file", ctVariables)

        every { fileResourceProvider.cachedFilePath(any()) } returns "cachedFilePath"
        assertEquals("cachedFilePath", varCache.getMergedValue("var1"))
    }

    @Test
    fun `test getMergedValue with group`() {
        ctVariables.init()

        Var.define("group.var1", 1, ctVariables)
        Var.define("group", mapOf("var2" to 2, "var3" to 3), ctVariables)
        Var.define("var4", 4, ctVariables)

        assertEquals(1, varCache.getMergedValue("group.var1"))
        assertEquals(2, varCache.getMergedValue("group.var2"))
        assertEquals(3, varCache.getMergedValue("group.var3"))
        assertEquals(4, varCache.getMergedValue("var4"))
    }

    fun `test getMergedValue with groups`() {
        ctVariables.init()

        Var.define("group1.group2.var3", 3, ctVariables)
        Var.define("group1.var1", 1, ctVariables)
        Var.define("group1", mapOf("var2" to 2), ctVariables)
        Var.define("group1.group2", mapOf("var4" to 4), ctVariables)

        assertEquals(1, varCache.getMergedValue("group1.var1"))
        assertEquals(2, varCache.getMergedValue("group1.var2"))
        assertEquals(3, varCache.getMergedValue("group1.group2.var3"))
        assertEquals(3, varCache.getMergedValue("group1.group2.var4"))
    }

    @Test
    fun `test getMergedValue with annotation and groups`() {
        ctVariables.init()

        parser.parseVariablesForClasses(VariableDefinitions.Groups::class.java)

        assertEquals(3, varCache.variablesCount)
        assertEquals(1, varCache.getMergedValue("group1.var_int1"))
        assertEquals(2, varCache.getMergedValue("group1.group2.var_int2"))
        assertEquals(3, varCache.getMergedValue("var_int3"))
        assertEquals("str1", varCache.getMergedValue("group1.var_string1"))
        assertEquals("str2", varCache.getMergedValue("group1.var_string2"))
    }

    @Test
    fun `test getMergedValueFromComponentArray`() {
        val components = arrayOf("a", "b", "c")
        val values = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "c" to "finalValue"
                )
            )
        )
        val result: String? = varCache.getMergedValueFromComponentArray(components, values)
        assertEquals("finalValue", result)
    }

    @Test
    fun `test getMergedValueFromComponentArray for wrong value`() {
        val components = arrayOf("a", "b", "c")
        val values = mapOf(
            "a" to mapOf(
                "b" to mapOf(
                    "d" to "finalValue"
                )
            )
        )
        val result: String? = varCache.getMergedValueFromComponentArray(components, values)
        assertNull(result)
    }

    @Test
    fun `test loadDiffs`() {
        val var1 = Var.define("var1", 1, ctVariables)
        mockkStatic(StorageHelper::class)
        every { StorageHelper.getString(any(), any(), any()) } returns """{"var1":2}"""

        varCache.loadDiffs { }

        assertEquals(2, var1.value())
        verify { StorageHelper.getString(application, "variablesKey:" + cleverTapInstanceConfig.accountId, "{}") }
    }

    @Test
    fun `test loadDiffs for kind file`() {
        ctVariables.init()
        val var1 = Var.define("var1", null, "file", ctVariables)
        mockkStatic(StorageHelper::class)
        every { fileResourceProvider.isFileCached(any()) } returns false
        every { StorageHelper.getString(any(), any(), any()) } returns """{"var1":"http://example.com/file"}"""

        varCache.loadDiffs {}

        assertEquals("http://example.com/file", var1.stringValue)
        verify { fileResourcesRepoImpl.preloadFilesAndCache(listOf(Pair("http://example.com/file", FILES)), any()) }
    }

    @Test
    fun `test loadDiffsAndTriggerHandlers`() {
        val var1 = Var.define("var1", 1, ctVariables)
        mockkStatic(StorageHelper::class)
        val globalCallbackRunnable = mockk<Runnable>(relaxed = true)
        every { StorageHelper.getString(any(), any(), any()) } returns """{"var1":2}"""
        varCache.setGlobalCallbacksRunnable(globalCallbackRunnable)

        varCache.loadDiffsAndTriggerHandlers {}

        assertEquals(2, var1.value())
        verify { StorageHelper.getString(application, "variablesKey:" + cleverTapInstanceConfig.accountId, "{}") }
        verify { globalCallbackRunnable.run() }
    }

    @Ignore("this is flaky")
    @Test
    fun `test clearUserContent`() {
        Var.define("var1", 1, ctVariables)
        Var.define("var2", "default", ctVariables)
        mockkStatic(StorageHelper::class)

        varCache.clearUserContent()

        verify { StorageHelper.putString(application, "variablesKey:" + cleverTapInstanceConfig.accountId, "{}") }
    }

    @Test
    fun `test fileVarUpdated when file is cached`() {
        val var1 : Var<String> = Var.define("var1", null, "file", ctVariables)
        val handler1: VariableCallback<String> = mockk(relaxed = true)
        var1.addFileReadyHandler(handler1)

        every { fileResourceProvider.isFileCached(any()) } returns true

        varCache.fileVarUpdated(var1)

        verify { handler1.setVariable(var1) }
        verify { handler1.run() }
    }

    @Test
    fun `test fileVarUpdated when file is not cached`() {
        val var1 : Var<String> = Var.define("var1", null, "file", ctVariables)
        val handler1: VariableCallback<String> = mockk(relaxed = true)
        var1.addFileReadyHandler(handler1)

        every { fileResourceProvider.isFileCached(any()) } returns false

        varCache.fileVarUpdated(var1)

        verify(exactly = 0) { handler1.setVariable(var1) }
        verify(exactly = 0) { handler1.run() }
        verify { fileResourcesRepoImpl.preloadFilesAndCache(any(), any()) }
    }
}
