package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
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

  @Test
  fun `test individual callback`() {
    ctVariables.init()
    var counter = 0

    val var1 = Var.define("var1", 1, ctVariables)
    var1.addValueChangedCallback(object : VariableCallback<Int>() {
      override fun onValueChanged(variable: Var<Int>) {
        counter++
      }
    })

    // callback invoked on start
    ctVariables.handleVariableResponse(JSONObject(), null)
    assertEquals(1, counter)

    // callback not invoked when value is not changed
    ctVariables.handleVariableResponse(JSONObject(), null)
    assertEquals(1, counter)

    // callback invoked when value is changed
    ctVariables.handleVariableResponse(JSONObject().put("var1", 2), null)
    assertEquals(2, counter)
  }

  @Test
  fun `test individual callback on error`() {
    ctVariables.init()
    var success = false
    var callback = false
    var counter = 0

    val var1 = Var.define("var1", 1, ctVariables)
    var1.addValueChangedCallback(object : VariableCallback<Int>() {
      override fun onValueChanged(variable: Var<Int>) {
        counter++
      }
    })

    ctVariables.handleVariableResponseError { isSuccessful ->
      success = isSuccessful
      callback = true
    }

    assertTrue(callback)
    assertFalse(success)
    assertEquals(1, counter)

    // callback is not ivoked again
    ctVariables.handleVariableResponseError(null)
    assertEquals(1, counter)
  }

  @Test
  fun `test global callbacks`() {
    ctVariables.init()
    var callbackCounter = 0
    var oneTimeCounter = 0

    ctVariables.addVariablesChangedCallback(object : VariablesChangedCallback() {
      override fun variablesChanged() {
        callbackCounter++
      }
    })

    ctVariables.addOneTimeVariablesChangedCallback(object : VariablesChangedCallback() {
      override fun variablesChanged() {
        oneTimeCounter++
      }
    })

    ctVariables.handleVariableResponse(JSONObject(), null)
    ctVariables.handleVariableResponse(JSONObject(), null)

    assertEquals(2, callbackCounter)
    assertEquals(1, oneTimeCounter)
  }

  @Test
  fun `test global callbacks on error`() {
    ctVariables.init()
    var callbackCounter = 0
    var oneTimeCounter = 0

    ctVariables.addVariablesChangedCallback(object : VariablesChangedCallback() {
      override fun variablesChanged() {
        callbackCounter++
      }
    })

    ctVariables.addOneTimeVariablesChangedCallback(object : VariablesChangedCallback() {
      override fun variablesChanged() {
        oneTimeCounter++
      }
    })

    ctVariables.handleVariableResponse(null, null)
    assertEquals(1, callbackCounter)
    assertEquals(1, oneTimeCounter)

    // won't be called on consecutive errors

    ctVariables.handleVariableResponse(null, null)
    assertEquals(1, callbackCounter)
    assertEquals(1, oneTimeCounter)
  }

  @Test
  fun `test clearUserContent resets var to default value`() {
    ctVariables.init()
    val var1 = Var.define("var1", 1, ctVariables)
    ctVariables.handleVariableResponse(JSONObject().put("var1", 2), null)
    assertEquals(2, var1.value())

    ctVariables.clearUserContent()
    assertFalse(ctVariables.hasVarsRequestCompleted())
    assertEquals(1, var1.value())

    ctVariables.handleVariableResponse(JSONObject().put("var1", 3), null)
    assertEquals(3, var1.value())
  }

  @Test
  fun `test clearUserContent resets group to default value`() {
    ctVariables.init()
    val group = Var.define("group", mapOf("var1" to 1, "var2" to 2), ctVariables)
    ctVariables.handleVariableResponse(JSONObject().put("group.var1", 111), null)

    assertEquals(111, varCache.getMergedValue("group.var1"))
    assertEquals(2, varCache.getMergedValue("group.var2"))
    assertEquals(mapOf("var1" to 111, "var2" to 2), group.value())

    ctVariables.clearUserContent()
    assertFalse(ctVariables.hasVarsRequestCompleted())
    assertEquals(mapOf("var1" to 1, "var2" to 2), group.value())

    ctVariables.handleVariableResponse(JSONObject().put("group.var2", 222), null)
    assertEquals(mapOf("var1" to 1, "var2" to 222), group.value())
  }

}
