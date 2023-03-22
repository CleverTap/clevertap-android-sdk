package com.clevertap.android.sdk.variables

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTVariableUtilsTest : BaseTestCase() {

  @Test
  fun updateValuesAndKinds() {
  }

  @Test
  fun traverse() {
  }

  @Test
  fun kindFromValue() {
  }

  @Test
  fun getNameComponents() {
  }

  @Test
  fun uncheckedCast() {
  }

  @Test
  fun fromJson() {
  }

  @Test
  fun mapFromJson() {
  }

  @Test
  fun toJson() {
  }

  @Test
  fun maybeThrowException() {
  }

  @Test
  fun getVarsJson() {
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
}
