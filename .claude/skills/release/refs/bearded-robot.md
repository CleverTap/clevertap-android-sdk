# Bearded Robot (BR) app — keeping it in sync with SDK releases

**Verified on:** 2026-07-29, against the local CT-Apps checkout and Maven Central.

The BR app is the internal showcase/QA app. After every SDK release it must (a) demonstrate the new
public APIs and (b) depend on the just-published versions. This file records how, and the traps.

---

## Where things are

| Thing | Path / location |
|---|---|
| CT-Apps repo | `$CT_APPS_DIR` — branch `main`, remote `github.com/CleverTap/CT-Apps` |
| BR app root (Gradle root) | `CT-Apps/BeardedRobot` |
| App module | `CT-Apps/BeardedRobot/BeardedRobot` |
| **Pinned SDK versions** | `CT-Apps/BeardedRobot/BeardedRobot/build.gradle.kts` — the `remoteAarsImplementation` lines |
| BR app's own version | `CT-Apps/BeardedRobot/gradle.properties` — `com.clevertap.version.*` |
| Secrets (signing, Firebase, etc.) | `CT-Apps/BeardedRobot/local.properties` |
| Knowledge base | `CT-Apps/BeardedRobot/android-br-knowledge-base/` — see `build-and-flavors.md`, `clevertap-integration.md` |
| Existing BR skills | `CT-Apps/BeardedRobot/.claude/skills/` — 8 of them |

### Reuse the existing BR skills — do not reimplement

`CT-Apps/BeardedRobot/.claude/skills/` already contains `br-sdk-integration` (a master
orchestrator with its own approval gates), plus `br-analyze`, `br-plan`, `br-implement`, `br-test`,
`br-report`, `br-error-handler` and `br-rollback`.

**`br-sdk-integration` is the entry point for showcasing a new SDK API in BR.** When new APIs need
demonstrating, point the user at it rather than editing BR screens from the release skill. It
explicitly does not manage git — the developer does that manually.

---

## ⚠️ The trap: `remoteAars` may not use Maven at all

`CT-Apps/BeardedRobot/local.properties` currently contains:

```
com.clevertap.sdkSource=true
```

That switches on the Gradle **composite build** in `settings.gradle.kts`, which carries a
`dependencySubstitution` block:

```kotlin
substitute(module("com.clevertap.android:clevertap-android-sdk")).using(project(":clevertap-core"))
// … and geofence, hms, push-templates
```

**So with that flag on, a `remoteAars` build silently ignores Maven Central and compiles the SDK
from your local `clevertap-android-sdk` working tree instead.**

Why that matters concretely: the local SDK checkout is often on a feature branch with uncommitted
work — at the time of writing, `bug/inbox-upsert-sqlite/SDK-5955` with an unreleased P0 fix. A BR
APK built that way would contain **unreleased code** while its filename and version claim it is the
released build. It would be published to the builds page as if it verified the release. Nothing
errors; the APK just silently isn't what it says it is.

### Always pass the override explicitly

```bash
./gradlew :BeardedRobot:assembleRemoteAarsRelease -PctSdkSource=false
```

This is safe to always pass. `settings.gradle.kts` resolves the flag in precedence order —
`-PctSdkSource` first, then `-Pcom.clevertap.sdkSource`, then the properties files — so the
command-line value wins over `local.properties`:

```kotlin
val ctSdkSource = (
    gradle.startParameter.projectProperties["ctSdkSource"]
        ?: gradle.startParameter.projectProperties["com.clevertap.sdkSource"]
        ?: settings.extra.properties["com.clevertap.sdkSource"] as String?
    )?.toBoolean() ?: false
```

**Verification after building:** confirm the APK really resolved from Maven. The point of a
`remoteAars` release build is to prove the published artifacts work. If in doubt, check the
dependency resolution:

```bash
./gradlew :BeardedRobot:dependencies --configuration remoteAarsReleaseRuntimeClasspath \
  -PctSdkSource=false | grep clevertap
```

A Maven-resolved build shows coordinates like `com.clevertap.android:clevertap-android-sdk:8.4.0`.
A substituted build shows `project :clevertap-core` instead.

---

## The dependency bump is already automated — do not hand-edit it

`CT-Apps/.github/workflows/br-sync-core-sdk.yml` — **BR Core SDK Sync**.

| Aspect | Detail |
|---|---|
| Trigger | Daily cron `30 4 * * *` (04:30 UTC ≈ 10:00 IST), plus `workflow_dispatch` |
| How it finds the version | Reads the latest from **Maven Central `<release>`** directly, so it can only pick a version that is already downloadable. No release-time dispatch or polling buffer needed. |
| What it does | 1. Creates a Jira Task first, under the newest "SDK Infra" epic (resolved dynamically each run). 2. Bumps the pinned version in `BeardedRobot/BeardedRobot/build.gradle.kts`. 3. Runs a lightweight validation build. 4. Opens a PR referencing the ticket for human review and merge. |
| Scope | Android and iOS are fully independent — one PR and one ticket per platform |

`workflow_dispatch` inputs:

| Input | Values |
|---|---|
| `platform` | `both` \| `android` \| `ios` (default `both`) |
| `force_version_android` | optional; use this specific version instead of Maven latest |
| `force_version_ios` | optional; same for iOS |

### ⚠️ This workflow is currently broken (as of 2026-07-30)

Do not assume it works. Check its recent runs before relying on it.

**Root cause, diagnosed 2026-07-30.** Android and iOS detection share the **same `detect` job**,
which runs under `set -euo pipefail`. The iOS step reads `BR-IOS/Podfile.lock` — but that file is
listed in `BR-IOS/.gitignore` and was untracked by commit `84311ef` ("remove podfile.lock from
br-ios", 2026-07-28 16:50 IST). It is therefore **absent on a fresh CI checkout**, `grep` exits 1,
and `set -e` kills the whole job.

The log shows Android succeeding immediately before iOS kills it:

```
Android: pinned=8.3.0 latest=8.4.0
android: 8.3.0 -> 8.4.0 is newer and no open branch exists -> bump needed
...
grep: BR-IOS/Podfile.lock: No such file or directory
##[error]Process completed with exit code 1.
```

Timeline confirming the regression:

| Date | Event |
|---|---|
| 2026-07-23 | Sync workflow added (`ab8a032`) |
| 2026-07-28 05:30 UTC | Last successful run |
| 2026-07-28 16:50 IST | `84311ef` untracks `BR-IOS/Podfile.lock` |
| 2026-07-29, 07-30 | Failing — Android bump for 8.4.0 never raised |

**This contradicts the workflow's own design intent**, which its header states as *"Android and iOS
are fully independent -> one PR + one ticket per platform"*. They are not independent in `detect`.

**The fix** (in CT-Apps, not this repo — offer it, do not assume it is wanted): make iOS detection
survive a missing lockfile so it cannot take Android down. Either guard it —

```bash
if [ ! -f "$PODFILE_LOCK" ]; then
  echo "ios: Podfile.lock absent (gitignored) -> skipping iOS detection" >&2
  echo "needed=false" >> "$GITHUB_OUTPUT"; exit 0
fi
```

— or read the pinned iOS version from `BR-IOS/Podfile`, which **is** tracked. Note `bump-ios` already
runs `pod install --repo-update`, so it regenerates the lockfile itself and does not need it
committed.

### Branch naming — matters for the manual fallback

`br-sync-decide.sh` builds `chore/bump-core-${PLATFORM}-${TO}` and treats an existing remote branch
of that name as "a PR is already in flight -> skip". So a hand-made bump PR should use **exactly**
that name — e.g. `chore/bump-core-android-8.4.0` — and the workflow will not raise a duplicate once
it is fixed.

**Consequences for the release skill:**

1. **Never hand-edit the `remoteAarsImplementation` versions.** The workflow owns that file.
2. On a release day the daily job may not have run yet. Rather than wait ~24 hours, dispatch it:

   ```bash
   gh workflow run br-sync-core-sdk.yml --repo CleverTap/CT-Apps \
     -f platform=android -f force_version_android=8.4.0
   ```

3. The resulting PR needs **human review and merge** — that is the intended gate. The skill does not
   merge it.
4. If the versions are already correct, there is nothing to bump. Skip straight to building.

Verified: the release machine's `gh` login has `MAINTAIN` on the private `CleverTap/CT-Apps`.

---

## BR app version scheme — derivable, don't invent it

From `CT-Apps/BeardedRobot/gradle.properties`:

```
com.clevertap.version.major=8
com.clevertap.version.minor=4
com.clevertap.version.patch=0
com.clevertap.version.qa=0
com.clevertap.version.suffix=inboxv2
```

**versionName** (`build.gradle.kts`, `versionName()`):
`<major>.<minor>.<patch>.<qa>` plus `-<suffix>` when a suffix is set → **`8.4.0.0-inboxv2`**

**versionCode** (`versionCode()`), verified against real APK filenames:

```
10^6 × minSdk  +  10^5 × major  +  10^4 × minor  +  10^3 × patch  +  qa
```

With `minSdk = 23`: `8.4.0.0` → `23,000,000 + 800,000 + 40,000 + 0 + 0` = **`23840000`**. That
matches the observed `BRApp-8.4.0.0-inboxv2-23840000-…apk`. Note the leading `23` is the **minSdk**,
which is easy to mistake for part of the version.

**APK filename pattern**, from real build outputs:

```
BRApp-<versionName>-<versionCode>-<flavor>-<buildType>.apk
```

| Real example | Reading |
|---|---|
| `BRApp-8.4.0.0-inboxv2-23840000-remoteAars-debug-androidTest.apk` | core 8.4.0, suffix `inboxv2` |
| `BRApp-8.2.0.0-inboxv2-23820000-remoteAars-release.apk` | core 8.2.0, suffix `inboxv2` |
| `BRApp-8.1.0.0-PT-2.4.0-23810000-localAars-debug.apk` | core 8.1.0, suffix `PT-2.4.0` |

**This resolves the builds-page naming puzzle.** The "BR App Version" column is simply the
**versionName**. What looked like two inconsistent conventions (`8.1.0.0-PT-2.4.0` versus
`BRApp-8.2.0.0-inboxv2`) is one convention with a different `suffix` property. So derive the version
from `gradle.properties` — but the **suffix is a human choice**, so confirm it.

---

## Build variants

Only **two** flavors exist: `remoteAars` and `localAars`. Combined with debug/release that gives
`remoteAarsDebug`, `remoteAarsRelease`, `localAarsDebug`, `localAarsRelease`.

| Flavor | Source of the CleverTap SDK |
|---|---|
| **`remoteAars`** | Maven Central — the published artifacts. **This is what a release build must use.** |
| `localAars` | Locally built AARs in `BeardedRobot/libsCT/`. Requires `-PctSdkSource=true`; fails fast with guidance without it. |

### Commands for a release build

Before running these commands, set `CT_APPS_DIR` to your local CT-Apps clone. If it is not set, ask the user for the path.

```bash
cd $CT_APPS_DIR/BeardedRobot
./gradlew :BeardedRobot:assembleRemoteAarsRelease -PctSdkSource=false
./gradlew :BeardedRobot:assembleRemoteAarsDebug   -PctSdkSource=false
```

Outputs land in `BeardedRobot/BeardedRobot/build/outputs/apk/remoteAars/{release,debug}/`.

Release builds are **signed by default**. Verified present in `local.properties`:
`com.clevertap.signing.storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

Also available: `-PcleanSdk=true` forces a clean SDK rebuild (only relevant to `localAars`, e.g.
after a branch switch or suspected stale output).

---

## Confluence "SDK Bearded Robot Builds" page

Page id **`5327192075`** — the `/wiki/x/C4CGPQE` short link. One table, **newest row first**.

| Column | Value |
|---|---|
| Core Version | e.g. `8.4.0` |
| Geofence Version | current pinned version, even if unchanged |
| HMS Version | current pinned version |
| Push Templates Version | current pinned version |
| BR App Version | the app's `versionName`, e.g. `8.4.0.0-inboxv2` |
| Debug Build | Google Drive link, link text `Download` |
| Release Build | Google Drive link, link text `Download` |

Every row lists **all four module versions**, not only the ones that changed. Read them from
`BeardedRobot/BeardedRobot/build.gradle.kts`.

### Drive upload — automated via `scripts/upload_br_apks.py`

Built and **verified working on 2026-07-30** (real 10 MB APK uploaded end to end).

```bash
~/.venvs/br-drive/bin/python .claude/skills/release/scripts/upload_br_apks.py <apk> [<apk> ...]
```

Prints the `https://drive.google.com/file/d/<id>/view` links the builds-page table needs.

| Piece | Location | Notes |
|---|---|---|
| Script | `.claude/skills/release/scripts/upload_br_apks.py` | Safe to commit |
| Virtualenv | `~/.venvs/br-drive` | `google-api-python-client`, `google-auth-oauthlib` |
| OAuth client | `~/.config/br-drive-upload/client_secret.json` | **Never commit.** Type must be *Desktop app* (`installed` key), not Web. Lives in GCP project `bearded-robot-b21a8`. |
| Cached token | `~/.config/br-drive-upload/token.json` | Auto-created, `chmod 600`, holds the refresh token. Delete it to force re-consent. |

Both credential files live **outside the repo** by design, so they cannot be committed by accident.

Verified behaviours:

- **Scope `drive.file` is sufficient** — it can write into the shared folder even though the script
  did not create it. No need for full `.../auth/drive`.
- **Silent after first run** — the cached refresh token means no browser.
- **The upload streams from disk** (`MediaFileUpload(resumable=True)`), so file size is irrelevant.

Flags: `--replace` (update a same-named file rather than duplicating — plain uploads do *not*
deduplicate), `--anyone` (grant "anyone with the link"), `--folder` (override the destination).

Failure modes the script handles with a clear message: `accessNotConfigured` (Drive API not enabled
on the project) and `storageQuotaExceeded` (only hit by service accounts — see below).

### Why a script and not the Drive MCP

Investigated 2026-07-29. The MCP route is **impossible**, not merely slower.

The Drive MCP's `create_file` only accepts content as `base64Content` / `textContent` — there is no
file-path, URL or resumable-upload parameter. So the bytes would have to be *generated as tool-call
arguments*, which does not fit:

| | Release APK | Debug APK |
|---|---|---|
| Raw size | 10,522,004 bytes | ~26 MB |
| As base64 | 14,029,341 chars | ~35 MB |
| Est. tokens | ~4.7 million | ~12 million |
| Versus a 1M context window | **4.7× over** | **12× over** |

Base64 is not the problem — it is imposed by the JSON tool boundary, and even a hypothetical
zero-overhead text encoding would still be ~3.5× over. The problem is the file size. There is also no
chunking path: `create_file` is one-shot, with no append or resume.

**The general rule:** a tool that reads the file *from a path* handles any size; a tool that needs the
bytes *as an argument* cannot handle real binaries. The Python script above takes the path, so it is
unaffected — as would be `rclone copy` or `gh release upload`.

### Service accounts will not work here

Worth recording so it is not attempted. A service account has no Drive storage quota, so uploading
into a normal My Drive folder fails with:

```
storageQuotaExceeded: Service Accounts do not have storage quota.
Leverage shared drives, or use OAuth delegation instead.
```

The builds folder is a regular My Drive folder shared by link, not a Shared Drive. That is why this
uses **OAuth user credentials** (acting as you) rather than a service account. If the upload ever
needs to run in CI, that has to be solved first — either convert the folder to a Shared Drive, or set
up domain-wide delegation, which needs a Workspace admin.

---

## Doc drift — page 4469228169 is out of date

[BeardedRobot(BR) Android Release](https://wizrocket.atlassian.net/wiki/spaces/EN/pages/4469228169)
(also reachable as `/wiki/x/iQJjCgE`) is broadly right but four things have changed. The user
flagged the Gradle performance change; the others turned up while verifying.

| # | The doc says | Reality |
|---|---|---|
| 1 | Comment / uncomment `com.clevertap.sdkRepoRelativeLocation` in `gradle.properties` to switch between local and remote | **Obsolete.** That line stays as it is. The composite build is now opt-in via `-PctSdkSource=true` (or `com.clevertap.sdkSource=true`), default **off** — this is the Gradle performance change, so `remoteAars` no longer recompiles the SDK. |
| 2 | Three flavors: `localAars`, `remoteAars`, `usingProject` | **`usingProject` no longer exists.** Only `remoteAars` and `localAars`. |
| 3 | `google-services.json` must exist in `src/main/`, `src/localAars/` and `src/remoteAars/` | One file at the app-module root — `BeardedRobot/BeardedRobot/google-services.json` — serves all flavors. No per-flavor copies exist. **It IS gitignored** (`BeardedRobot/BeardedRobot/.gitignore:2`, verified 2026-08-10), so a fresh clone or worktree lacks it — copy it from an existing checkout before building. The signing keystore `BeardedRobot/BRKey` is likewise gitignored and referenced by a relative path from `local.properties`, so release builds in a fresh worktree also need it copied in. |
| 4 | App source is at Bitbucket `DA/br-android` | It is in `CleverTap/CT-Apps` under `BeardedRobot/`. |

Not in the doc at all: `-PcleanSdk=true`, and the whole `br-sync-core-sdk.yml` automation.

**The doc is worth updating**, but that is a separate task — do not silently rely on it.
