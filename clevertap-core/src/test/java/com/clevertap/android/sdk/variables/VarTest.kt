package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.junit.*
import org.junit.Assert.*

class VarTest : BaseTestCase() {

    private lateinit var ctVariables: CTVariables
    private lateinit var varCache: VarCache

    @Before
    override fun setUp() {
        MockKAnnotations.init(this)
        ctVariables = mockk(relaxed = true)
        varCache = mockk(relaxed = true)
        every { ctVariables.getVarCache() } returns varCache
        mockkStatic(Logger::class)
        mockkStatic(Utils::class)
    }

    @Test
    fun `define() should return null for empty name`() {
        val variableName = ""
        val result = Var.define(variableName, "default", ctVariables)
        assertNull(result)
        verify { Logger.v("variable", "Empty name parameter provided.") }
    }

    @Test
    fun `define() should return null for name starts with dot`() {
        val variableName = ".abc"
        val result = Var.define(variableName, "default", ctVariables)
        assertNull(result)
        verify { Logger.v("variable", "Variable name starts or ends with a `.` which is not allowed: $variableName") }
    }

    @Test
    fun `define() should return null for name ends with dot`() {
        val variableName = "abc."
        val result = Var.define(variableName, "default", ctVariables)
        assertNull(result)
        verify { Logger.v("variable", "Variable name starts or ends with a `.` which is not allowed: $variableName") }
    }

    @Test
    fun `define() should return null for null defaultValue when kind is not FILE`() {
        val variableName = "testVar"
        val kind = "string"
        val result = Var.define<String>(variableName, null, kind, ctVariables)

        assertNull(result)
        verify {
            Logger.d("Invalid Operation! Null values are not allowed as default values when defining the variable 'testVar'.")
        }
    }

    @Test
    fun `define() should return null for null defaultValue when kind is  FILE`() {
        val variableName = "testVar"
        val kind = "file"

        every { varCache.getVariable<String>("testVar") } returns null
        val result = Var.define<String>(variableName, null, kind, ctVariables)

        assertEquals(variableName, result.name())
        assertNull(result.defaultValue())
        assertEquals(kind, result.kind())
    }

    @Test
    fun `define() should return existing variable if already defined`() {
        val existingVar: Var<String> = mockk()
        every { varCache.getVariable<String>("existingVar") } returns existingVar
        val result: Var<String>? = Var.define("existingVar", "default", ctVariables)
        assertEquals(existingVar, result)
    }

    @Test
    fun `define() should create a new variable if not already defined`() {
        every { varCache.getVariable<String>("newVar") } returns null
        every { ctVariables.hasVarsRequestCompleted() } returns true
        val result = Var.define("newVar", "default", ctVariables)
        assertEquals("newVar", result?.name())
        assertEquals("default", result?.defaultValue())

        verify {
            varCache.registerVariable(result)
        }
    }

    @Test
    fun `update() should change value and trigger callbacks`() {
        // Define a variable
        every { varCache.getVariable<String>("testVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        val varObj = Var.define("testVar", "default", ctVariables)

        val callback: VariableCallback<String> = mockk(relaxed = true)
        varObj?.addValueChangedCallback(callback)

        // Update the variable
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "newValue"
        every { ctVariables.hasVarsRequestCompleted() } returns true

        varObj?.update()

        assertEquals("newValue", varObj?.value())
        verify { callback.setVariable(varObj) }
        verify { callback.run() }
    }

    @Test
    fun `update() should change value, trigger callbacks and call fileVarUpdated`() {
        // Define a file variable
        every { varCache.getVariable<String>("testFileVar") } returns null
        val varObj = Var.define("testFileVar", null, "file", ctVariables)

        val callback: VariableCallback<Nothing?> = mockk(relaxed = true)
        varObj?.addValueChangedCallback(callback)

        // Update the variable
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "https://dummy.pdf"
        every { ctVariables.hasVarsRequestCompleted() } returns true

        varObj?.update()

        assertEquals("https://dummy.pdf", varObj?.stringValue)
        verify { callback.setVariable(varObj) }
        verify { callback.run() }
        verify { varCache.fileVarUpdated(varObj as Var<String>) }
    }

    @Test
    fun `update() should not trigger callback if value has not changed`() {
        // Define a variable
        every { varCache.getVariable<String>("testVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        every { ctVariables.hasVarsRequestCompleted() } returns true
        val varObj = Var.define("testVar", "default", ctVariables)

        val callback: VariableCallback<String> = mockk(relaxed = true)
        varObj?.addValueChangedCallback(callback)

        // Update the variable
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        every { ctVariables.hasVarsRequestCompleted() } returns true

        varObj?.update()

        assertEquals("default", varObj?.value())
        verify(exactly = 0) { callback.setVariable(varObj) }
        verify(exactly = 0) { callback.run() }
    }

    @Test
    fun `value() should return correct value`() {
        every { varCache.getVariable<String>("testVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        every { ctVariables.hasVarsRequestCompleted() } returns true
        val varObj = Var.define("testVar", "default", ctVariables)

        assertEquals("default", varObj?.value())
    }

    @Test
    fun `value() should return correct value for kind file`() {
        every { varCache.getVariable<String>("testFileVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "https://dummy.pdf"
        every { varCache.filePathFromDisk("https://dummy.pdf") } returns "dummy.a.b"
        val varObj = Var.define("testFileVar", null, "file", ctVariables)

        assertEquals("dummy.a.b", varObj.value() as String)
    }

    @Test
    fun `numberValue() should return correct number representation for Different Data Types`() {
        val diffDateTypeValueList = listOf<Number>(10.toByte(), 20.toShort(), 30, 40L, 50.5f, 60.5)
        every { varCache.getVariable<Int>("testVar") } returns null
        diffDateTypeValueList.forEach {
            every { varCache.getMergedValueFromComponentArray<Any>(any()) } returns it
            val varObj = Var.define("testVar", it, ctVariables)
            assertEquals(it.toDouble(), varObj?.numberValue())
            assertEquals(it, varObj?.value())
        }
    }

    @Test
    fun `stringValue() should return correct string representation`() {
        every { varCache.getVariable<String>("testVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        every { ctVariables.hasVarsRequestCompleted() } returns true
        val varObj = Var.define("testVar", "default", ctVariables)

        assertEquals("default", varObj?.stringValue())
    }

    @Test
    fun `stringValue() should return correct string representation for kind file`() {
        every { varCache.getVariable<String>("testFileVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "https://dummy.pdf"
        every { varCache.filePathFromDisk("https://dummy.pdf") } returns "dummy.a.b"
        val varObj = Var.define("testFileVar", null, "file", ctVariables)

        assertEquals("dummy.a.b", varObj?.stringValue())
    }

    @Test
    fun `rawFileValue() should return null for non file kind`() {
        every { varCache.getVariable<String>("testVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "default"
        every { ctVariables.hasVarsRequestCompleted() } returns true
        val varObj = Var.define("testVar", "default", ctVariables)
        assertNull(varObj?.rawFileValue())
    }

    @Test
    fun `rawFileValue() should return value for file kind`() {
        every { varCache.getVariable<String>("testFileVar") } returns null
        every { varCache.getMergedValueFromComponentArray<String>(any()) } returns "https://dummy.pdf"
        val varObj = Var.define("testFileVar", "default", "file", ctVariables)

        assertEquals("https://dummy.pdf", varObj.rawFileValue())
    }


    @Test
    fun `triggerFileIsReady() should invoke all registered file ready handlers`() {
        every { varCache.getVariable<String>("testFileVar") } returns null
        val varObj = Var.define("testFileVar", null, "file", ctVariables)

        val handler1: VariableCallback<Nothing?> = mockk(relaxed = true)
        val handler2: VariableCallback<Nothing?> = mockk(relaxed = true)
        varObj?.addFileReadyHandler(handler1)
        varObj?.addFileReadyHandler(handler2)

        varObj?.triggerFileIsReady()
        verify { handler1.setVariable(any()) }
        verify { handler1.run() }
        verify { handler2.setVariable(any()) }
        verify { handler2.run() }
    }
}