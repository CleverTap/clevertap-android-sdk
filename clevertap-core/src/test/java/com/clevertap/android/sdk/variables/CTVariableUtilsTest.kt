package com.clevertap.android.sdk.variables

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTVariableUtilsTest : BaseTestCase() {

  @Test
  fun updateValuesAndKinds() {
  }

  @Test
  fun testGetNameComponents() {
    assertTrue(arrayOf("a").contentEquals(CTVariableUtils.getNameComponents("a")))
    assertTrue(arrayOf("a", "b", "c").contentEquals(CTVariableUtils.getNameComponents("a.b.c")))
  }

  @Test
  fun `test mergeHelper with null diff`() {
    val vars = mapOf("a" to 1, "b" to 2)
    val result = CTVariableUtils.mergeHelper(vars, null)
    Assert.assertEquals(vars, result)
  }

  @Test
  fun `test mergeHelper with primitives`() {
    val vars = "a"
    val diff = 1
    val result = CTVariableUtils.mergeHelper(vars, diff)
    Assert.assertEquals(diff, result)
  }

  @Test
  fun `test mergeHelper with booleans`() {
    val boolean1 = true
    val boolean2 = false
    val result = CTVariableUtils.mergeHelper(boolean1, boolean2)
    Assert.assertTrue(result is Boolean)
    Assert.assertEquals(false, result)
  }

  @Test
  fun `test mergeHelper with map and primitive`() {
    val vars = mapOf("key1" to "value1", "key2" to "value2")
    val diff = "diff"
    val expectedResult = "diff"

    Assert.assertEquals(expectedResult, CTVariableUtils.mergeHelper(vars, diff))
  }

  @Test
  fun `test mergeHelper with dictionaries`() {
    val vars = mapOf("a" to 1, "b" to 2)
    val diff = mapOf("b" to 42, "c" to 3)
    val result = CTVariableUtils.mergeHelper(vars, diff)
    Assert.assertEquals(mapOf("a" to 1, "b" to 42, "c" to 3), result)
  }

  @Test
  fun `test mergeHelper with nested dictionaries`() {
    val vars = mapOf(
      "A" to mapOf("a" to 1, "b" to 2),
      "B" to mapOf("x" to 24, "y" to 25)
    )
    val diff = mapOf(
      "A" to mapOf("b" to -2, "c" to -3),
      "B" to mapOf("x" to -24)
    )

    val expected = mapOf(
      "A" to mapOf("a" to 1, "b" to -2, "c" to -3),
      "B" to mapOf("x" to -24, "y" to 25)
    )

    Assert.assertEquals(expected, CTVariableUtils.mergeHelper(vars, diff))
  }

  @Test
  fun `test mergeHelper with different type vars`() {
    val vars = hashMapOf("k1" to 20, "k2" to "hi", "k3" to true, "k4" to 4.3)
    val diffs = hashMapOf("k1" to 21, "k3" to false, "k4" to 4.8)
    val expected = hashMapOf("k1" to 21, "k2" to "hi", "k3" to false, "k4" to 4.8)
    val result = CTVariableUtils.mergeHelper(vars, diffs)
    Assert.assertEquals(expected, result)
  }

  @Test
  fun `test mergeHelper with different type vars and nested maps`() {
    val vars = hashMapOf(
      "k2" to hashMapOf("m1" to 1, "m2" to "hello", "m3" to false),
      "k3" to hashMapOf("m1" to 1, "m2" to "hello", "m3" to false),
      "k4" to hashMapOf("m1" to 1, "m2" to "hello", "m3" to false),
      "k5" to 4.3,
    )
    val diffs = hashMapOf(
      "k2" to hashMapOf("m1" to 1, "m2" to "hello", "m3" to false), //map not changing
      "k3" to hashMapOf("m1" to 2, "m2" to "bye", "m3" to true),// map changing
      "k4" to hashMapOf(
        "m1" to 2,
        "m3" to true,
        "m4" to "new key"
      ),// map changing and adding new items while removing old items
    )
    val expected = hashMapOf(
      "k2" to hashMapOf("m1" to 1, "m2" to "hello", "m3" to false),
      "k3" to hashMapOf("m1" to 2, "m2" to "bye", "m3" to true),
      "k4" to hashMapOf("m1" to 2, "m2" to "hello", "m3" to true, "m4" to "new key"),
      "k5" to 4.3,
    )
    val result = CTVariableUtils.mergeHelper(vars, diffs)
    Assert.assertEquals(expected, result)
  }

  @Test
  fun `test mergeHelper with array`() {
    val vars = arrayOf(1, 2, 3, 4)
    val diffs = arrayOf(1, 2, 3, 4)
    Assert.assertThrows(java.lang.ClassCastException::class.java) {
      CTVariableUtils.mergeHelper(vars, diffs)
    }
  }

  @Test
  fun `test mergeHelper with map of array`() {
    val vars = mapOf("arr" to arrayOf(1, 2, 3, 4))
    val diffs = mapOf("arr" to arrayOf(1, 2, 3, 4))
    Assert.assertThrows(java.lang.ClassCastException::class.java) {
      CTVariableUtils.mergeHelper(vars, diffs)
    }
  }

  @Test
  fun `test convertNestedMapsToFlatMap`() {
    val input = mapOf("a" to 1)
    val output = mutableMapOf<String, Any>()

    val expected: Map<String, Any> = mapOf("a" to 1)

    CTVariableUtils.convertNestedMapsToFlatMap("", input, output)
    assertEquals(expected, output)
  }

  @Test
  fun `test convertNestedMapsToFlatMap with group`() {
    val input = mapOf("group" to mapOf("a" to 1))
    val output = mutableMapOf<String, Any>()

    val expected: Map<String, Any> = mapOf("group.a" to 1)

    CTVariableUtils.convertNestedMapsToFlatMap("", input, output)
    assertEquals(expected, output)
  }

  @Test
  fun `test convertNestedMapsToFlatMap with two groups`() {
    val input = mapOf("group1" to mapOf("group2" to mapOf("a" to 1)))

    val output = mutableMapOf<String, Any>()
    val expected: Map<String, Any> = mapOf("group1.group2.a" to 1)

    CTVariableUtils.convertNestedMapsToFlatMap("", input, output)
    assertEquals(expected, output)
  }

  @Test
  fun `test convertNestedMapsToFlatMap with several variables`() {
    val input = mapOf(
      "group1" to mapOf("group11" to mapOf("v1" to 1, "v2" to "val")),
      "v3" to "v3",
      "group2" to mapOf("v4" to 4, "v5" to true))

    val output = mutableMapOf<String, Any>()
    val expected: Map<String, Any> = mapOf(
      "group1.group11.v1" to 1,
      "group1.group11.v2" to "val",
      "v3" to "v3",
      "group2.v4" to 4,
      "group2.v5" to true,
    )

    CTVariableUtils.convertNestedMapsToFlatMap("", input, output)
    assertEquals(expected, output)
  }

  @Test
  fun `test convertFlatMapToNestedMaps`() {
    val input = mapOf("a" to 1)
    val expected = mapOf("a" to 1)

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test convertFlatMapToNestedMaps with group`() {
    val input = mapOf("group.a" to 1)
    val expected = mapOf("group" to mapOf("a" to 1))

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test convertFlatMapToNestedMaps with two groups`() {
    val input = mapOf("group1.group2.a" to 1)
    val expected = mapOf("group1" to mapOf("group2" to mapOf("a" to 1)))

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test convertFlatMapToNestedMaps with several variables`() {
    val input = mapOf(
      "group1.group11.v1" to 1,
      "group1.group11.v2" to "val",
      "v3" to "v3",
      "group2.v4" to 4,
      "group2.v5" to true,
    )
    val expected = mapOf(
      "group1" to mapOf(
        "group11" to mapOf(
          "v1" to 1,
          "v2" to "val"
        )
      ),
      "v3" to "v3",
      "group2" to mapOf(
        "v4" to 4,
        "v5" to true
      )
    )

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test convertFlatMapToNestedMaps with invalid data`() {
    val input = mapOf(
      "a.b.c.d" to "d value",
      "a.b.c" to "c value",
      "a.e" to "e value",
      "a.b" to "b value",
    )
    val expected = mapOf("a" to mapOf("b" to "b value", "e" to "e value"))

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test convertFlatMapToNestedMaps with invalid data2`() {
    val input = mapOf(
      "a.b.c" to "c value",
      "a.b.c.d" to "d value",
      "a.e" to "e value",
      "a.b" to "b value",
    )
    val expected = mapOf("a" to mapOf("b" to "b value", "e" to "e value"))

    assertEquals(expected, CTVariableUtils.convertFlatMapToNestedMaps(input))
  }

  @Test
  fun `test getFlatVarsJson`() {
    val values = mutableMapOf("var1" to "str", "group" to mutableMapOf("var2" to 1, "var3" to 2.0))
    val kinds = mutableMapOf("group.var2" to "number", "var1" to "string", "group" to "group")

    val expected = mapOf(
      "type" to "varsPayload",
      "vars" to mapOf(
        "var1" to mapOf(
          "type" to "string",
          "defaultValue" to "str"
        ),
        "group.var2" to mapOf(
          "type" to "number",
          "defaultValue" to 1
        ),
        "group.var3" to mapOf(
          "type" to "number",
          "defaultValue" to 2.0
        )
      )
    )

    val jsonObject = CTVariableUtils.getFlatVarsJson(values, kinds)
    val jsonMap = JsonUtil.mapFromJson<Any>(jsonObject)
    assertEquals(expected, jsonMap)
  }
}
