---
name: verify-no-behaviour-change
description: Prove that a cleanup, refactor, import or annotation change did not change what the SDK does. Use when a PR says "no behaviour change", for lint or detekt warning fixes, unused code removal, import changes, or when asked to confirm a change is safe.
---

# Prove there is no behaviour change

A cleanup must not change anything for app developers or users: no new logs, no signature changes,
no UI changes. Reading the diff is not enough, so compare the compiled output.

## Steps

1. Build the base branch (usually `develop`) and keep the classes:
   `./gradlew :<changed-module>:compileDebugKotlin :<changed-module>:compileDebugJavaWithJavac`
   (use the module you changed, for example `clevertap-core` or `clevertap-geofence`; repeat for each one).
   For `sample`, which has flavors (`local`, `remote`, `staging`), use the flavor task and path instead, for example
   `:sample:compileLocalDebugKotlin` with `build/tmp/kotlin-classes/localDebug`, and
   `:sample:compileLocalDebugJavaWithJavac` with
   `build/intermediates/javac/localDebug/compileLocalDebugJavaWithJavac/classes`.
   Copy these folders, relative to `<changed-module>/`: `build/tmp/kotlin-classes/debug` and
   `build/intermediates/javac/debug/compileDebugJavaWithJavac/classes` to a scratch folder.
2. Check out your branch, build the same module the same way, and copy the same folders to a second scratch folder.
3. Compare the two folders byte by byte. Files that are identical need no more work.
4. For files that differ, disassemble both with `javap -c -p -constants` and compare. Remove the constant
   pool numbers (`#12`) and the `Compiled from` lines first, because those move when imports change.
5. `-c` does not show annotations. Also run `javap -v -p` on each differing class and compare the
   `RuntimeVisibleAnnotations` and `RuntimeInvisibleAnnotations` blocks and any `AnnotationDefault` entries (for example
   `javap -v -p X.class | grep -A3 'Runtime.*Annotations'`). A runtime annotation that changed is a real change.
6. Whatever still differs is a real change. Explain each one or revert it.

## What counts as safe

- Identical bytecode: safe.
- Only constant pool order or source file differences: safe.
- Anything else (a call added, a branch changed, a string changed, an annotation with runtime effect):
  not a cleanup. Say so in the PR, or split it out.

## Also check by hand

- Public API signatures and nullability annotations in the diff.
- New or changed log messages.
- Resource and layout files (not covered by bytecode comparison).
- ProGuard and consumer rules.

## Report

Write how many classes were compared, how many were identical, and the explanation for each that was not.
