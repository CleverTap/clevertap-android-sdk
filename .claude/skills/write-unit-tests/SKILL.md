---
name: write-unit-tests
description: Write unit tests for the CleverTap Android SDK in the style this repo uses. Use when adding or fixing tests, when a PR needs tests, or when a test is flaky or passes without testing anything.
---

# Write unit tests

Tests live in `<module>/src/test`. Most of them are Kotlin. Run them with the `build-test-verify` skill.

## Pick the base

- Needs an Android `Context`, `Application` or resources: extend `BaseTestCase` (in `test_shared`). It sets
  up Robolectric (default SDK 28, override with `@Config(sdk = [...])`), a test `Application`, a relaxed
  mock `CleverTapAPI` and a `CleverTapInstanceConfig`. Call `super.setUp()` if you override `setUp`.
- Pure logic with no Android classes: a plain JUnit class is faster, and about half of the core tests
  are written that way. Prefer it.
- Needs a real device or real SQLite behaviour: use `src/androidTest` (instrumentation).

## Naming

- Class: `<ClassUnderTest>Test.kt`.
- Method: both styles exist in `clevertap-core`. Backtick sentences are the most common
  (``fun `when config is null should throw`()``), and the style documented in `BaseTestCase` is
  `test_<method>_<condition>_<expectedBehavior>`. Follow the style of the file you are editing; use
  backtick sentences in a new file.

## Rules

1. Use MockK for mocks. Use `relaxed = true` only when the result does not matter to the test.
2. Do not use `Thread.sleep`. Use a `CountDownLatch` or Awaitility (used in the geofence tests) with a timeout.
3. A mocked executor never runs the task. If the test is about what the task does, use a real executor
   (and shut it down in `finally`).
4. No real network and no real account values.
5. Check the test can fail: break the code under test, or revert your fix, and see the test go red.
6. Do not leave `@Ignore` without a reason in a comment.
7. Test the changed behaviour, then the edge cases (null, empty, boundary, second call).

## Threading tests

For a race or duplicate-instance bug, start several threads behind one `CountDownLatch` so they run
together, then assert on the result. Run the test many times before trusting it.
