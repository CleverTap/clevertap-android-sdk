package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CTVariablesTest : BaseTestCase() {

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
  fun `test handleVariableResponse and callbacks`() {
    ctVariables.init()
    var var1_notified = false
    var var2_notified = false
    var callback_notified = false

    val var1 = Var.define("var1", 1, ctVariables)
    var1.addValueChangedCallback(object : VariableCallback<Int>() {
      override fun onValueChanged(variable: Var<Int>) {
        var1_notified = true
      }
    })

    val var2 = Var.define("group.var2", 2, ctVariables)
    var2.addValueChangedCallback(object : VariableCallback<Int>() {
      override fun onValueChanged(variable: Var<Int>) {
        var2_notified = true
      }
    })

    val json = JSONObject(mapOf(
      "var1" to 10,
      "group.var2" to 20,
    ))

    ctVariables.handleVariableResponse(json) {
      callback_notified = true
    }

    assertTrue(callback_notified)
    assertTrue(var1_notified)
    assertTrue(var2_notified)
    assertEquals(10, var1.value())
    assertEquals(20, var2.value())
  }

  @Test
  fun `test handleVariableResponse with invalid data`() {
    ctVariables.init()
    var success = false
    var callback = false

    ctVariables.handleVariableResponse(null) { isSuccessful ->
      success = isSuccessful
      callback = true
    }

    assertTrue(callback)
    assertFalse(success)
  }

}
