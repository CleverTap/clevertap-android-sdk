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

//  @After
//  fun tearDown() {
//    CleverTapAPI.setInstances(null)
//  }

  @Test
  fun registerVariable() {
  }

  @Test
  fun getMergedValueFromComponentArray() {
  }

  @Test
  fun testGetMergedValueFromComponentArray() {
  }

  @Test
  fun loadDiffsSync() {
  }

  @Test
  fun loadDiffsAsync() {
  }

  @Test
  fun loadDiffsAndTriggerHandlers() {
  }

  @Test
  fun updateDiffsAndTriggerHandlers() {
  }

  @Test
  fun saveDiffs() {
  }

  @Test
  fun triggerHasReceivedDiffs() {
  }

  @Test
  fun getDefineVarsData() {
  }

  @Test
  fun reset() {
  }

  @Test
  fun getVariable() {
  }

  @Test
  fun setCacheUpdateBlock() {
  }

  @Test
  fun hasReceivedDiffs() {
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

  // TODO test default value of group variable

  // TODO annotated variable

  // TODO define group as Map<>

  // TODO getVariable -> should return group, or maybe only if it is defined before that?
}
