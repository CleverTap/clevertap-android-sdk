---
name: pr-review
description: Review a pull request or diff for the CleverTap Android SDK. Use when asked to review a PR or changes, or to check a branch before merge. Checks public API, threading, resources, tests, release files and whether CI ran.
---

# Review a PR

Read the diff first, then go through each item. Report only what you verified in the code, and say what
you could not check. Give the findings to the user; post them on the PR only if asked.

## 1. Is it safe for customer apps?

- Public API: changes to `CleverTapAPI.java` or to any class not marked `@RestrictTo` or `internal`
  (signatures, behaviour, defaults, nullability). Anything here can break customer code.
- Behaviour change hidden in a cleanup or refactor PR. If the PR says "no behaviour change", use the
  `verify-no-behaviour-change` skill to confirm it.
- New logs, permissions, manifest changes or dependencies.

## 2. Threading and lifecycle

- Database, file or network work on the main thread.
- Background work should go through `CTExecutors`. Database code is annotated `@WorkerThread`.
- Context or Activity references kept in static fields or long-lived objects.
- Screen size or orientation changing while the app runs (in-app and inbox views).

## 3. Resources and shrinking

- A resource looked up by name with `getIdentifier` must be listed in
  `clevertap-core/src/main/res/raw/com_clevertap_android_sdk_keep.xml` or referenced statically.
  Strict resource shrinking removes it otherwise.
- A layout that exists only in a qualified folder (for example `layout-sw600dp`) has no fallback on
  other screens.

## 4. Tests

- New behaviour has a test, and the test fails when the behaviour is broken.
- No `Thread.sleep`, no real network, and no test that passes only because a mock never runs the code.
- Ignored tests have a reason.

## 5. Release files and CI

- Changelog entries in `docs/` and `templates/` for changes that users can see.
- Did CI run? A PR from a branch name outside the accepted prefixes, or into a branch other than
  `develop` or `master`, gets no lint, detekt, checkstyle, tests or build. If CI did not run, say so and
  ask for the "Manually validate any branch" workflow.
- Lint, detekt and checkstyle do not fail the build. Look at their reports.

## Output

List the findings from most to least serious. For each one give the file and line, what is wrong, and a
concrete case where it fails. End with what you did not check.
