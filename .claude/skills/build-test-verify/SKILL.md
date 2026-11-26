---
name: build-test-verify
description: Build, test and check one module of the CleverTap Android SDK the way CI does. Use when running unit tests, lint, detekt or checkstyle, building AARs, installing the sample app, or when asked whether a change is safe to merge.
---

# Build, test and verify

Use JDK 17 (CI uses Temurin 17). Run tasks for the module you changed, not the whole project.
Modules: `:clevertap-core`, `:clevertap-pushtemplates`, `:clevertap-geofence`, `:clevertap-hms`, `:sample`.

## Commands

Replace `:clevertap-core` with your module.

| What | Command | Report (under the module's `build/`) |
|---|---|---|
| Unit tests | `./gradlew :clevertap-core:testDebugUnitTest` | `reports/tests/` |
| One test class | `./gradlew :clevertap-core:testDebugUnitTest --tests "*SomeTest"` | same |
| One test method | `... --tests "com.clevertap.android.sdk.SomeTest.methodName"` | same |
| Lint | `./gradlew :clevertap-core:lint` | `reports/lint-results-debug.html` |
| Detekt | `./gradlew :clevertap-core:detekt` | `reports/detekt/` |
| Checkstyle | `./gradlew :clevertap-core:checkstyle` | `reports/checkstyle/` |
| Coverage | `./gradlew :clevertap-core:jacocoTestReportDebug -Pcoverage=true` | `reports/jacoco/` |
| Release AAR | `./gradlew :clevertap-core:assembleRelease` | `outputs/aar/` |
| Sample app | `./gradlew :sample:installLocalDebug` | installs on a device |

The sample reads account values from `sample/src/main/AndroidManifest.xml`. Never commit real values.

## Reading the results

- A failing test is a failure. Fix it.
- Lint, detekt and checkstyle never fail the build. Open the report and check the findings for the
  files you changed. Do not clear old warnings in files you did not touch.
- Lint sometimes crashes with an internal error. Run it once more before treating it as a real failure.
- Coverage has a 65% target (overall and for changed files). It is shown in the PR comment.

## Writing tests and device checks

Use the `write-unit-tests` skill for tests and the `verify-on-device` skill for UI, push and screen-size
changes.
