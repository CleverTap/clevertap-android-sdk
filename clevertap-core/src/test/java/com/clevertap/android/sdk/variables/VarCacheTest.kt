package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.variables.VariableDefinitions.NullDefaultValue
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class VarCacheTest : BaseTestCase() {

  private lateinit var varCache: VarCache
  private lateinit var ctVariables: CTVariables
  private lateinit var parser: Parser

  @Before
  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()

    varCache = VarCache(cleverTapInstanceConfig, application)
    ctVariables = CTVariables(varCache)
    parser = Parser(ctVariables)
  }

  @Test
  fun `test updateDiffsAndTriggerHandlers`() {
    ctVariables.init()

    val var1 = Var.define("var1", 1, ctVariables)
    val var2 = Var.define("group.var2", 2, ctVariables)

    varCache.updateDiffsAndTriggerHandlers(mapOf(
      "var1" to 10,
      "group" to mapOf("var2" to 20, "var3" to 30),
    ))

    assertEquals(10, var1.value())
    assertEquals(20, var2.value())
    assertEquals(30, varCache.getMergedValue("group.var3"))
  }

  @Test
  fun `testRegisterVariable with Parser and null default value`() {
    ctVariables.init()

    parser.parseVariables(NullDefaultValue())

    assertNull(varCache.getVariable<Int>("string_with_null").value())
  }

  @Test
  fun `testRegisterVariable with Factory Method and null default value`() {
    ctVariables.init()

    Var.define<String>("var_string", null, ctVariables)

    assertNull(varCache.getVariable<Int>("var_string").value())
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
    assertEquals(mapOf("var1" to 1, "var2" to 2, "var3" to 3), varCache.getVariable<Map<*,*>>("group").value())
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
      "var1" to 1,
      "var2" to 2,
      "group2" to mapOf("var3" to 3, "var4" to 4))
    assertEquals(expected, varCache.getVariable<Map<*,*>>("group1").value())

    expected = mapOf("var3" to 3, "var4" to 4)
    assertEquals(expected, varCache.getVariable<Map<*,*>>("group1.group2").value())
  }

  @Test
  fun `testRegisterVariable with group and default value`() {
    ctVariables.init()

    Var.define("group.var1", 1, ctVariables)
    Var.define("group", mapOf("var2" to 2), ctVariables)

    assertEquals(2, varCache.variablesCount)
    assertEquals(1, varCache.getVariable<Int>("group.var1").value())
    assertEquals(1, varCache.getVariable<Int>("group.var1").defaultValue())
    assertEquals(mapOf("var1" to 1, "var2" to 2), varCache.getVariable<Map<*,*>>("group").value())
    assertEquals(mapOf("var2" to 2), varCache.getVariable<Map<*,*>>("group").defaultValue())
  }

  @Test
  fun `testRegisterVariable with nested groups and default value`() {
    /*
      The setup:
      {
        group1.var1: 1,
        group1.var2: 2,
        group1.group2.var3: 3,
        group1.group2.var4: 4
      }
     */

    ctVariables.init()

    Var.define("group1.var1", 1, ctVariables)
    Var.define("group1.group2.var3", 3, ctVariables)
    Var.define("group1", mapOf("var2" to 2, "group2" to mapOf("var4" to 4)), ctVariables)

    assertEquals(3, varCache.variablesCount)
    assertEquals(1, varCache.getVariable<Int>("group1.var1").value())
    assertEquals(3, varCache.getVariable<Int>("group1.group2.var3").value())

    val expectedValue = mapOf(
      "var1" to 1,
      "var2" to 2,
      "group2" to mapOf("var3" to 3, "var4" to 4))
    assertEquals(expectedValue, varCache.getVariable<Map<*,*>>("group1").value())

    val expectedDefaultValue = mapOf(
      "var2" to 2,
      "group2" to mapOf("var4" to 4))
    assertEquals(expectedDefaultValue, varCache.getVariable<Map<*,*>>("group1").defaultValue())
  }

  @Test
  fun `test getMergedValue`() {
    ctVariables.init()
    Var.define("var", 100, ctVariables)
    assertEquals(100, varCache.getMergedValue("var"));
  }

  @Test
  fun `test getMergedValue with group`() {
    ctVariables.init()

    Var.define("group.var1", 1, ctVariables)
    Var.define("group", mapOf("var2" to 2, "var3" to 3), ctVariables)
    Var.define("var4", 4, ctVariables)

    assertEquals(1, varCache.getMergedValue("group.var1"));
    assertEquals(2, varCache.getMergedValue("group.var2"));
    assertEquals(3, varCache.getMergedValue("group.var3"));
    assertEquals(4, varCache.getMergedValue("var4"));
  }

  fun `test getMergedValue with groups`() {
    ctVariables.init()

    Var.define("group1.group2.var3", 3, ctVariables)
    Var.define("group1.var1", 1, ctVariables)
    Var.define("group1", mapOf("var2" to 2), ctVariables)
    Var.define("group1.group2", mapOf("var4" to 4), ctVariables)

    assertEquals(1, varCache.getMergedValue("group1.var1"));
    assertEquals(2, varCache.getMergedValue("group1.var2"));
    assertEquals(3, varCache.getMergedValue("group1.group2.var3"));
    assertEquals(3, varCache.getMergedValue("group1.group2.var4"));
  }

  @Test
  fun `test getMergedValue with annotation and groups`() {
    ctVariables.init()

    parser.parseVariablesForClasses(VariableDefinitions.Groups::class.java)

    assertEquals(3, varCache.variablesCount)
    assertEquals(1, varCache.getMergedValue("group1.var_int1"));
    assertEquals(2, varCache.getMergedValue("group1.group2.var_int2"));
    assertEquals(3, varCache.getMergedValue("var_int3"));
    assertEquals("str1", varCache.getMergedValue("group1.var_string1"));
    assertEquals("str2", varCache.getMergedValue("group1.var_string2"));
  }

}
