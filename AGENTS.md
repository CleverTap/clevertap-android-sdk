# AGENTS.md

CleverTap Android SDK. This is a public library that runs inside customer apps, so a mistake here
reaches every app that uses it. These rules are for AI coding agents and for people.
`CLAUDE.md` imports this file.

## Modules

| Module | What it is |
|---|---|
| `clevertap-core` | The SDK: events, user profile, in-app, inbox, push, network, database |
| `clevertap-pushtemplates` | Push templates (rating, timer, carousel and others) |
| `clevertap-geofence` | Geofence SDK |
| `clevertap-hms` | Huawei push |
| `sample` | Sample app. Flavor `local` uses the modules above, `remote` uses published artifacts, `staging` is a third variant |
| `instantapp`, `test_shared` | Instant app sample, shared test helpers (`BaseTestCase`) |

minSdk 23, compileSdk 36. Java and Kotlin are mixed. `CleverTapAPI.java` is the public entry point.

## Commands

Use JDK 17 (CI uses it). Run from the repo root, for the module you changed, not the whole project.

| Task | Command |
|---|---|
| Unit tests | `./gradlew :clevertap-core:testDebugUnitTest` |
| One test class | `./gradlew :clevertap-core:testDebugUnitTest --tests "*SomeTest"` |
| Lint | `./gradlew :clevertap-core:lint` |
| Detekt (Kotlin) | `./gradlew :clevertap-core:detekt` |
| Checkstyle (Java) | `./gradlew :clevertap-core:checkstyle` |
| Release AAR | `./gradlew :clevertap-core:assembleRelease` |
| Install the sample | `./gradlew :sample:installLocalDebug` |

Reports are in `<module>/build/reports/`. The `build-test-verify` skill has the details.

## Before you finish

1. Run the unit tests of every module you changed.
2. Run lint, detekt and checkstyle for those modules. Detekt and checkstyle only report, they never
   fail the build, so open the report and check that the files you touched have no new warnings.
3. Add a changelog entry for changes that SDK users can see: edit `templates/CT<MODULE>CHANGELOG.md`, then
   run `./gradlew copyTemplates` to regenerate `docs/` (never hand-edit `docs/`). The task also rewrites the
   root `README.md` and other `docs/` files, so check `git status` first and keep any edits you already
   have there; afterwards revert only the unrelated hunks the task produced. The `develop` to `master` PR fails unless `CHANGELOG.md`, one file in
   `docs/`, one file in `templates/` and the version in `gradle/libs.versions.toml` were all changed
   when `src/` changed, so the release owner needs them.
4. Do not commit real values in `sample/src/main/AndroidManifest.xml` (account id, token, region).
   The file holds placeholders.
5. Do not commit `local.properties`, signing files or tokens.

## Branches, commits and PRs

- Branch from `develop`: `task/SDK-1234/short-name` or `bug/SDK-1234/short-name`. The other accepted
  prefixes (`feat/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/`, `ci/` and more) are listed in
  `.github/workflows/on_pr_from_task_to_develop.yml`.
- **CI runs only for PRs into `develop` or `master`, and for `develop` only when the branch name starts
  with an accepted prefix.** Other PRs get no lint, detekt, checkstyle, tests or build. For those, run the
  "Manually validate any branch" workflow.
- Commit messages and PR titles use Conventional Commits with the Jira key as scope:
  `fix(SDK-1234): short description`. See the `conventional-commits` skill.
- One logical change per PR. Task PRs are usually squash-merged, so the PR title becomes the commit message.

## Ask the user first

Do not do these without the user saying so in this session:

- Post comments or reviews on a PR or issue, or resolve review threads.
- Merge a PR, push to `develop` or `master`, force-push, or delete a branch.
- Release steps: version bumps, tags, publishing to Maven Central, GitHub releases (the `release` skill
  asks at every step).
- Commit or push the user's own uncommitted changes. Stage only what you changed.
- Commit `sample/src/main/AndroidManifest.xml` with real account values, `local.properties`, signing files or tokens.
- Change public API, licenses or dependencies' versions as a side effect of another task.

## Skills

Project skills are in `.claude/skills/`. Use the one that matches the task.

| Skill | Use when |
|---|---|
| `conventional-commits` | Writing a commit message or PR title |
| `pr-prepare` | Opening a PR or getting a branch ready for review |
| `pr-review` | Reviewing a PR or a diff |
| `build-test-verify` | Running tests, lint, detekt, checkstyle, builds |
| `write-unit-tests` | Adding or fixing tests |
| `changelog` | Adding changelog entries, or preparing release files |
| `verify-on-device` | Checking UI, push or screen-size changes on an emulator or phone |
| `verify-no-behaviour-change` | Proving a cleanup or refactor changed nothing |
| `crash-repro-ticket` | Investigating a crash report and writing the ticket |
| `release` | Releasing a module (manual only, asks before each step) |

## SDK rules

- Public API: `CleverTapAPI.java` and every class that is not marked `@RestrictTo` (Java) or `internal`
  (Kotlin) is used by customers. Do not change signatures, behaviour or defaults without discussion.
  Internal classes use `@RestrictTo(Scope.LIBRARY)` or `internal`.
- Threading: never do database, file or network work on the main thread. Background work goes through
  `CTExecutors` (`clevertap-core/src/main/java/com/clevertap/android/sdk/task/CTExecutors.java`;
  `postAsyncSafelyTask(tag)` runs tasks of one feature one after another). Database code is annotated
  `@WorkerThread`.
- Resources that code looks up by name (`getIdentifier`) are removed by strict resource shrinking unless
  they are referenced elsewhere or listed in `clevertap-core/src/main/res/raw/com_clevertap_android_sdk_keep.xml`.
- A layout that exists only in a qualified folder (for example `layout-sw600dp`) has no fallback on
  other screens. Check that the code only loads it on screens where it exists.

## What CI runs

| When | What |
|---|---|
| PR into `develop` (accepted branch name) | lint, detekt and checkstyle, then unit tests with JaCoCo, then the release AAR build |
| PR `develop` to `master` | The release files check first, then the same chain |
| After a merge into `master` | The same chain again |
| Manual: "Manually validate any branch" | Any of the above on any branch |

Lint, detekt and checkstyle report only. A green job does not mean zero warnings. Coverage is posted
as a PR comment with a 65% target.

## Known gotchas

- Lint sometimes crashes with an internal error (seen locally). Run it once more before assuming
  your change broke it.
- `sample` needs your own CleverTap account values in its manifest to run against a real account.
  Never commit them.
