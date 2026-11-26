# Release Output Patterns — mined from history

**Mined on:** 2026-07-28
**Sources:** PR #1005, GitHub releases `corev8.3.0` → `corev8.0.0`, Confluence SDK Releases space
**Purpose:** the exact, literal formats the `/release` skill must produce. Copy these shapes;
do not invent new wording.

## Decisions locked (2026-07-28)

| # | Decision | Consequence |
|---|---|---|
| 1 | Slack message is **authored fresh**, not copied from history — format in §7 | We own the format; no legacy inconsistency to inherit |
| 2 | "Previous tag" in a compare link means the **previous tag of the same module** | Core 8.4.0 compares against `corev8.3.0`, never against an intervening `ptv*` tag |
| 3 | The changelog date and the GitHub release date **must match** | Both come from one `release_date` field in the manifest. See §6 for the slippage edge case. |
| 4 | The Confluence page is created **at the start** of the release | Its URL is needed for the Slack message at the end. The skill ticks off checklist rows as it goes. |
| 5 | The Jira release ticket is **asked for**, never derived | It is an input to the manifest |

---

## 1. Release manifest — the single source of truth

Everything below is rendered from this. Fill this once per release.

```yaml
release_date: "2026-07-30"        # the day you actually publish to Maven Central
jira_release_ticket: "SDK-5955"   # the parent ticket holding all sub-tickets

modules:                          # only the modules actually shipping
  core:
    from: "8.3.0"
    to:   "8.4.0"

changes:                          # grouped exactly as the changelog headings below
  new_features: []
  improvements: []
  behavioral_notes: []
  breaking_api_changes: []
  bug_fixes:
    - "Fixes an issue where App Inbox messages were not saved on Android 6.0 to 10."

testing_scope:                    # bullet list, goes on the Confluence page
  - "App Inbox message save and read on Android 6.0, 7, 8, 9, 10"

dev_qa: "Lalitkumar Patil"
github_issues_fixed: []           # links, or N/A
```

### Derived values — never typed by hand

| Value | Rule | Example |
|---|---|---|
| Module tag | `core` → `corev<version>`, `pushtemplates` → `ptv<version>`, `hms` → `hmsv<version>`, `geofence` → `gfv<version>` (unverified — no recent geofence-only release to confirm) | `corev8.4.0` |
| Full tag | Module tags joined by `_`, **core first**, in the order core → hms → pt | `corev7.3.0_hmsv1.5.0_ptv1.4.0` |
| Sample app `versionCode` | `major * 1_000_000 + minor * 10_000 + patch * 100` | `8.2.0` → `8020000`; `8.1.0` → `8010000` |
| GitHub release name | The release date as `Month D, YYYY` (no zero-padding on the day) | `June 4, 2026` |
| Confluence page title | `[Android Release] Month D, YYYY`, or `[Android Hotfix] Month D, YYYY` for a hotfix | `[Android Release] June 4, 2026` |

---

## 2. The version-bump edits — and what is generated instead

Taken from the real diff of [PR #1005](https://github.com/CleverTap/clevertap-android-sdk/pull/1005)
(`8.1.0` → `8.2.0`), cross-checked against the `copyTemplates` task in `build.gradle:53`.

### `copyTemplates` is a template renderer, not a copy

This is the fact that determines everything else. From `build.gradle:53-98`, the task loads every
version from `gradle/libs.versions.toml` into a property map, then:

```groovy
copy { from 'templates/README.md'; into "."      ; expand('ext': props) }   // → root README.md
copy { from 'templates'; exclude "README.md"
       into "docs/"                              ; expand('ext': props) }   // → docs/*
```

`expand()` performs Groovy token substitution. `templates/README.md` contains placeholders such as
`${ext.clevertap_android_sdk}`, `${ext.firebase_messaging}` and `${ext.exoplayer}`.

**Two consequences:**

1. **Root `README.md` is generated, not hand-edited.** The version strings in its dependency
   snippets update by themselves once `libs.versions.toml` is bumped. Editing `README.md` directly
   would be overwritten on the next `copyTemplates`.
2. **Ordering is mandatory:** bump `gradle/libs.versions.toml` *first*, then run `copyTemplates`.
   Run it before the bump and the docs get the old version.

### Hand-edited files — the real list

| # | File | Edit |
|---|---|---|
| 1 | `gradle/libs.versions.toml` | `clevertap_android_sdk = "8.2.0"` under `#SDK Versions` |
| 2 | `sample/build.gradle` | `versionCode 8020000` and `versionName "8.2.0"` |
| 3 | `sample/build.gradle` | `remoteImplementation("com.clevertap.android:clevertap-android-sdk:8.2.0")` |
| 4 | `sample/build.gradle` | `stagingImplementation("com.clevertap.android:clevertap-android-sdk:8.2.0")` |
| 5 | `templates/CTCORECHANGELOG.md` | prepend the version entry — see §3. One per shipping module. |
| 6 | `CHANGELOG.md` (root) | prepend the dated entry — see §3. **Not** generated; there is no `templates/CHANGELOG.md`. |
| 7 | `templates/EXAMPLES.md` | only when the release changes public API usage |

So: four edits inside two version files, plus the changelogs. `sample/build.gradle` is the one to
be careful with — it carries the version in four separate places.

### Generated — never hand-edit (change `templates/`, then run `copyTemplates`)

| Generated file | Source |
|---|---|
| root `README.md` | `templates/README.md` |
| `docs/*` (19 files) | the matching `templates/*` file |

**Expect `git status` after `copyTemplates` to show files you never touched** — `docs/CTGEOFENCE.md`
and `docs/CTPUSHTEMPLATES.md` changed in PR #1005 purely because embedded version strings were
re-substituted. That is correct behaviour, not an accident.

One oddity: `docs/InAppNotificationsJavascriptInterface.md` has no counterpart in `templates/`, so
it is never regenerated. Leave it alone.

In PR #1005 the `docs/CTCORECHANGELOG.md` diff happens to be byte-identical to the
`templates/CTCORECHANGELOG.md` diff — but only because that particular changelog text contains no
placeholders. Do not generalise that to a plain copy.

### Branch and PR naming

| Thing | Pattern | PR #1005 actual |
|---|---|---|
| Release branch (cut from `develop`) | `task/release/<module>/<version>` | `task/release/core/8.2.0` |
| Chores branch (cut from the release branch) | `chore/release/<version>` | `chore/release/8.2.0` |
| PR title | `docs changes and version bump <JIRA-ID>` | `docs changes and version bump SDK-5709` |
| PR body | *(empty in practice)* | empty |

---

## 3. Changelog formats

### `templates/CTCORECHANGELOG.md` — prepend directly under the `##` title

```markdown
## CleverTap Android SDK CHANGE LOG
### Version 8.2.0 (May 19, 2026)

#### New Features
* **Feature Name:** One-sentence description ending in a full stop.
  * **Sub-point:** Nested detail.
    * **Note:** Caveats and gotchas go in a `**Note:**` bullet.

#### Improvements
* Description.

#### Behavioral Notes
* **Topic:** Description.

#### Breaking API Changes
* **Breaking Change:**
  * Details text

#### Bug Fixes
* Description.
```

Section heading order, exactly as observed: **New Features → Improvements → Behavioral
Notes → Breaking API Changes → Bug Fixes**. Omit any section with no entries.

Style rules observed consistently:
- Feature bullets lead with a **bold label**, then a colon, then prose.
- Code identifiers in backticks: `` `fetchInbox()` ``, `` `CleverTapAPI` ``.
- Nesting goes three levels deep for caveats.
- Every bullet ends with a full stop.

### `CHANGELOG.md` (root) — a two-line dated entry, newest first

```markdown
## CHANGE LOG.

### May 19, 2026
* [CleverTap Android SDK v8.2.0](docs/CTCORECHANGELOG.md).

### April 17, 2026
* [CleverTap Android SDK v8.1.0](docs/CTCORECHANGELOG.md).
```

Note the trailing full stop after the link, and the **relative** path.

---

## 4. GitHub release — fully deterministic, two lines

Release **name** is the date. Body is exactly:

```markdown
* [CleverTap Android SDK v8.3.0](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md)

**Full Changelog**: https://github.com/CleverTap/clevertap-android-sdk/compare/corev8.2.0...corev8.3.0
```

Per-module link targets:

| Module | Bullet text | Changelog path |
|---|---|---|
| Core | `CleverTap Android SDK v<version>` | `docs/CTCORECHANGELOG.md` |
| Push Templates | `CleverTap Push Templates SDK v<version>` | `docs/CTPUSHTEMPLATESCHANGELOG.md` |
| Huawei | `CleverTap Huawei Push SDK v<version>` (unverified) | `docs/CTHUAWEIPUSHCHANGELOG.md` |
| Geofence | `CleverTap Geofence SDK v<version>` (unverified) | `docs/CTGEOFENCECHANGELOG.md` |

### Multi-module release body — verified against `corev8.4.0_ptv2.5.0`

This is the real body of the July 28, 2026 release (core 8.4.0 + push templates 2.5.0), so treat it
as authoritative rather than inferred:

```markdown
* [CleverTap Android SDK v8.4.0](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md)
* [CleverTap Push Templates SDK v2.5.0](https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTPUSHTEMPLATESCHANGELOG.md)

**Full Changelog**: https://github.com/CleverTap/clevertap-android-sdk/compare/corev8.3.0...corev8.4.0_ptv2.5.0
```

Three rules it confirms:

| Rule | Detail |
|---|---|
| One bullet per shipping module | Core first, then the others |
| The compare range mixes tag forms | It runs from the **previous same-module tag** (`corev8.3.0`) to the **new combined tag** (`corev8.4.0_ptv2.5.0`). Not previous-combined to new-combined. |
| No trailing full stop | The lone `ptv2.4.0` release had a stray `.` after its link; this one does not. Omit it. |

Release name was `July 28, 2026` and the Confluence page
`[Android Release] July 28, 2026` (id `6958383117`) lists **both** modules as separate rows in the
SDK table, each linking the same combined tag.

### Real inconsistencies found in the last 5 releases

This body is 100% derivable from the manifest, yet **two of the last five are wrong**. This is
the clearest single argument for automating it.

| Tag | Actual compare link | Problem |
|---|---|---|
| `corev8.3.0` | `compare/corev8.3.0...corev8.2.0` | **Range is reversed.** Should be `corev8.2.0...corev8.3.0`. Renders an empty or wrong diff. |
| `corev8.2.0` | `compare/corev8.1.0...corev8.2.0` | Correct |
| `corev8.1.0` | `compare/ptv2.4.0...corev8.1.0` | Compares against the *chronologically* previous tag (a Push Templates release) rather than the previous **core** tag `corev8.0.0`. See open question 2. |
| `ptv2.4.0` | `compare/ptv2.3.0...ptv2.4.0` | Correct — but the bullet line has a stray trailing `.` that core releases don't have |
| `corev8.0.0` | `compare/corev7.8.0...corev8.0.0` | Correct |

**Rule to implement:** `compare/<previous>...<current>`, previous first, where `<previous>` is
the **most recent tag for the same module** — decision 2. For core 8.4.0 that is `corev8.3.0`,
even if a `ptv*` tag was created more recently.

Derive it, do not read it from the last release: `git tag --list 'corev*' --sort=-v:refname | head -1`.

---

## 5. Confluence page

- **Parent:** SDK Releases — page id `2139258925`
- **Canonical template:** page id `4401070108`, `[<Platform> Release] Month DD, YYYY template`
- **Title:** `[Android Release] Month D, YYYY`
- **The space is shared across platforms** — sibling pages exist for ReactNative, Flutter and
  Cordova, and the template literally uses `<Platform>` as a variable. This is the strongest
  evidence that the renderer layer is genuinely reusable across SDKs.

### Structure, from the most recent real page (June 4, 2026, core 8.3.0)

```markdown
### SDK Releases:

| **SDK** | **Version** | **Link** | **New release** |
| --- | --- | --- | --- |
| Core | 8.3.0 | `<link to the github release tag>` | Yes |

### Changelog [Core]

```
<VERBATIM copy of the templates/CTCORECHANGELOG.md entry for this version>
```

### TAN
N/A

### Testing Scope
* bullet list

### Jira Tickets
<link to the parent Jira ticket>

_This above ticket has all the sub tickets that were addressed in this release_

### Dev QA
@mention

### QA Testing
TBD

### Github Issues Fixed
N/A

## Checklist

Publish process details : https://wizrocket.atlassian.net/wiki/spaces/EN/pages/2894692467

| **Tasks** |  |
| --- | --- |
| Develop branch ready | Yes |
| QA Done | No |
| Github Changelog Merged | No |
| Version Bump SDK | No |
| Master merge | No |
| Test sample app with latest changes (locally compiled project+app together) | No |
| Publishing process [link](https://wizrocket.atlassian.net/wiki/spaces/EN/pages/2894692467) | No |
| Test sample app with remote changes once changes are live | No |
| Update tickets marked in deployment/open bug status | No |
| Update sample apps - BR, CTSample | No |
| Clevertap website updates [docs page] Changelog, Version updates, new features | No |
```

Key point: the **Changelog [Core] code block is a verbatim copy** of the changelog entry
already written in `templates/CTCORECHANGELOG.md`. Zero new writing. Pure re-render.

Older pages (2025 and earlier) use a slightly different checklist that also has a "Release
ticket" row and a "Master tag" row instead of "Master merge". Follow the **June 2026** shape.

### Two gaps found in the current practice

1. **Pages are missing entirely for recent releases.** There is no page for `8.1.0`
   (April 17, 2026) or `8.2.0` (May 20, 2026). Confirmed by CQL search across the whole EN
   space, not just this parent. The step gets skipped under time pressure.
2. **The checklist is never updated after the release.** On the June 4, 2026 page — a release
   that shipped nearly two months ago — ten of eleven rows still read `No`. The page is
   created at draft time and abandoned.

**Fixed by decision 4:** the page is still created at the start — its URL is needed for the
Slack message — but the skill must **update the checklist as each step completes**, and the
final gate does not pass until every row reads `Yes`.

---

## 6. Date inconsistency worth deciding

For core 8.2.0 there are two different dates in play:

| Where | Date |
|---|---|
| `templates/CTCORECHANGELOG.md` heading | `Version 8.2.0 (May 19, 2026)` |
| `CHANGELOG.md` (root) entry | `### May 19, 2026` |
| GitHub release name | `May 20, 2026` |

The changelog date is when the chores PR was written; the release name is when it actually
published.

**Decision 3: they must match.** Both render from a single `release_date` in the manifest.

**Edge case the skill must handle.** The changelog date is committed *before* publishing, so if
the publish slips to the next day the committed date becomes wrong. Immediately before creating
the GitHub release, the skill must compare today's date against `release_date`:

| Situation | Action |
|---|---|
| Today matches `release_date` | Proceed |
| Publish slipped | Stop and ask. Either correct the date in `CHANGELOG.md`, `templates/CTCORECHANGELOG.md` and `docs/CTCORECHANGELOG.md` with a follow-up commit to master, or knowingly accept the mismatch. Never silently name the GitHub release a different date than the changelog. |

---

## 7. Slack message for `#sdk-scrumteam`

Authored fresh (decision 1), not mined — no Slack MCP server is configured in `.mcp.json`, and
we would rather own a clean format than inherit an inconsistent one.

Agreed shape: **headline naming what went live → changelog → release tag → Confluence page →
thanks**.

### Slack does NOT understand Markdown — this matters for copy-paste

Slack uses its own syntax called `mrkdwn`. It overlaps with Markdown just enough to be
misleading.

| Thing | Markdown | Slack `mrkdwn` |
|---|---|---|
| Bold | `**text**` | `*text*` |
| Italic | `*text*` | `_text_` |
| Strikethrough | `~~text~~` | `~text~` |
| Link with a label | `[label](url)` | `<url\|label>` |
| Bullet list | `- item` | no syntax at all — paste a literal `•` character |
| Heading | `# Heading` | not supported |

**The catch that decides our format:** `<url|label>` is **API-only syntax**. It works when a
message is sent through `chat.postMessage` or an incoming webhook. If you *paste* it into the
Slack message box it stays literal — the reader sees
`<https://github.com/...|CTCORECHANGELOG.md>`, which looks broken.

So there are two variants. **Use the paste-safe one today.**

### Variant 1 — paste-safe (current default)

Bare URLs, which Slack auto-links on paste, and `*bold*`, which the composer converts.

```
🚀 *CleverTap Core Android SDK v8.4.0 is live!*

📝 *Changelog* → https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md#version-840-july-30-2026
🏷️ *Release tag* → https://github.com/CleverTap/clevertap-android-sdk/releases/tag/corev8.4.0
📄 *Release page* → https://wizrocket.atlassian.net/wiki/spaces/EN/pages/1234567890

🙌 Huge thanks to everyone involved in this release!
```

Multi-module — one combined tag, so one tag link, but a changelog line per module:

```
🚀 *CleverTap Android SDKs are live!* — Core v8.4.0, Push Templates v2.5.0

📝 *Changelogs*
    • Core → https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md#version-840-july-30-2026
    • Push Templates → https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTPUSHTEMPLATESCHANGELOG.md#version-250-july-30-2026
🏷️ *Release tag* → https://github.com/CleverTap/clevertap-android-sdk/releases/tag/corev8.4.0_ptv2.5.0
📄 *Release page* → https://wizrocket.atlassian.net/wiki/spaces/EN/pages/1234567890

🙌 Huge thanks to everyone involved in this release!
```

### Variant 2 — webhook / API (only if a Slack credential is obtained later)

Identical content, but labelled links become possible and the message reads much cleaner:

```
📝 *Changelog* → <https://github.com/CleverTap/clevertap-android-sdk/blob/master/docs/CTCORECHANGELOG.md#version-840-july-30-2026|Version 8.4.0>
🏷️ *Release tag* → <https://github.com/CleverTap/clevertap-android-sdk/releases/tag/corev8.4.0|corev8.4.0>
```

### The changelog link is a deep link to the version heading

Not the top of the file. GitHub generates a heading anchor by lowercasing the heading, removing
anything that is not alphanumeric / space / hyphen, then replacing spaces with hyphens.

| Changelog heading | Anchor |
|---|---|
| `### Version 8.4.0 (July 30, 2026)` | `#version-840-july-30-2026` |
| `### Version 8.2.0 (May 19, 2026)` | `#version-820-may-19-2026` |

Note the version's dots vanish (`8.4.0` → `840`) and the parentheses and comma disappear.

### Formatting notes

- Module display names: `Core`, `Push Templates`, `Huawei Push`, `Geofence`.
- The headline uses the full product name — `CleverTap Core Android SDK`, not `core`.
- **Ordering constraint:** this message is last, because it needs both the GitHub release URL
  and the Confluence page URL. The Confluence page is created first (decision 4), so its URL is
  already in hand.

### Delivery

No Slack credential exists today, so the skill **renders variant 1 and stops**. You copy and
send it. An incoming webhook would unlock variant 2 and direct posting — tracked as an open
question in `RELEASE_AUTOMATION_APPROACHES.md`.

---

## 8. Publishing to Maven Central — verified commands

### Which publish task to use, and why

The project uses the **`com.vanniktech.maven.publish` plugin, version 0.32.0**
(`gradle-scripts/commons.gradle:3`), configured as:

```groovy
mavenPublishing {
    publishToMavenCentral("CENTRAL_PORTAL")
    signAllPublications()
    coordinates(publishedGroupId, artifact, libraryVersion)
    // pom { name / description / licenses / developers / scm }
}
```

The [runbook](https://wizrocket.atlassian.net/wiki/spaces/EN/pages/2894692467/Android+SDK+Publish+Process)
says to run `publishMavenPublicationToMavenCentralRepository`, and that is what the team actually
clicks in the Android Studio Gradle panel (`clevertap-core` → Tasks → publishing). **This is the
proven path — the skill uses it.**

For context, here is how it relates to the plugin's own task, compared with `--dry-run`:

| Task run | Task graph that actually executes |
|---|---|
| `publishMavenPublicationToMavenCentralRepository` | `prepareLintJarForPublish` → `bundleReleaseAar` → `signMavenPublication` → `publishMavenPublicationToMavenCentralRepository` |
| **`publishToMavenCentral`** | the same four, **plus** `publishAllPublicationsToMavenCentralRepository` → `publishToMavenCentral` |

So `publishToMavenCentral` is a **strict superset** — it runs the runbook's task first, then two
more steps.

**How the difference arises.** In `CENTRAL_PORTAL` mode the repository named `mavenCentral` is
not a remote server. The Central Portal API accepts a *zipped bundle*, not individual artifact
uploads, so the plugin stages the signed AAR, POM and sources into a local folder first, then
zips and POSTs that bundle. Splitting it that way means:

| Task | What it does |
|---|---|
| `publishMavenPublicationToMavenCentralRepository` | Signs the artifacts and writes them into the local staging folder. A prerequisite step. |
| `publishToMavenCentral` | The above, **then** zips the bundle and uploads it to the Central Portal, creating the deployment that shows as *Validated* |

### …but the per-repository task does upload, and that is settled empirically

The plugin's documentation
([vanniktech.github.io/gradle-maven-publish-plugin/central](https://vanniktech.github.io/gradle-maven-publish-plugin/central/))
documents only `publishToMavenCentral` and `publishAndReleaseToMavenCentral`, and never mentions
the per-repository task. That raised a worry that the runbook's task might stage artifacts
locally without uploading.

**That worry is unfounded.** Every CleverTap Android release — including core 8.3.0 — has shipped
by clicking `publishMavenPublicationToMavenCentralRepository` in the IDE and then releasing the
resulting deployment from the Central Portal website. If it did not upload, there would be no
deployment on the website to release. The plugin evidently completes the bundle upload at build
end via its build service, even when only the per-repository task is invoked.

Shipped releases beat inference from documentation. **Do not change the publish command.**

Worth noting *why* the team uses this task: as the IDE Gradle panel screenshot shows, the
`publishing` group lists `publishAllPublicationsToMavenCentralRepository`,
`publishMavenPublicationToMavenCentralRepository`, `publishMavenPublicationToMavenLocal` and
`publishToMavenLocal` — but **not** `publishToMavenCentral`. The runbook documented what was
clickable. (Gradle's own `tasks --all` does list `publishToMavenCentral`, so the IDE panel is
likely stale and needs a Gradle sync — irrelevant now, but explains the history.)

### The three tasks, and which to use

| Task | What it does | Use it? |
|---|---|---|
| `publishMavenPublicationToMavenLocal` | Publishes to the local `~/.m2` cache | ✅ Safe dry run |
| **`publishMavenPublicationToMavenCentralRepository`** | Uploads a deployment to the Central Portal and **leaves it for manual publishing** | ✅ **This is the one** — proven in production, and it preserves the human gate |
| `publishToMavenCentral` | Same outcome via the plugin's documented entry point (a superset of the above) | 🟡 Works, but there is no reason to change a proven command |
| `publishAndReleaseToMavenCentral` | Uploads **and auto-releases to Maven Central** | ❌ **Never.** It destroys the irreversible-action gate. |

### Commands the skill runs

The command-line equivalent of what the team clicks in the IDE:

```bash
./gradlew clean
./gradlew :clevertap-core:assembleRelease
./gradlew :clevertap-core:publishMavenPublicationToMavenLocal              # optional dry run
./gradlew :clevertap-core:publishMavenPublicationToMavenCentralRepository  # the real upload
```

Repeat the module-scoped tasks for every module being released — `:clevertap-pushtemplates`,
`:clevertap-hms`, `:clevertap-geofence`.

**Verification gate.** After the upload, the skill must confirm a deployment exists and reads
*Validated* at <https://central.sonatype.com/publishing/deployments> before going any further. You
then click **Publish** yourself. That check is what makes the choice of task low-risk: a command
that silently failed to upload would be caught here, not after the release was announced.

### Prerequisites — all verified present on this machine (2026-07-28)

The publish is fully non-interactive: the GPG passphrase lives in `signing.password`, so nothing
prompts for input and the skill can run it directly.

| Requirement | Status |
|---|---|
| `~/.gradle/gradle.properties` → `signing.keyId`, `signing.password`, `signing.secretKeyRingFile` | ✅ all present |
| `~/.gradle/gradle.properties` → `mavenCentralUsername`, `mavenCentralPassword` | ✅ present |
| `developerId`, `developerName`, `developerEmail` | ✅ present in both `~/.gradle/gradle.properties` and `local.properties` |
| The `.gpg` keyring file that `signing.secretKeyRingFile` points at | ✅ exists |
| GPG installed, secret key present, not expired or revoked | ✅ `gpg` found, 1 secret key, no expiry set |

`local.properties` also carries `ossrhUsername` / `ossrhPassword`. Those are legacy OSSRH
credentials; the Central Portal path does not read them. Harmless, but not what authenticates
the upload.

**If a future machine is missing any of the above**, the skill stops and walks the user through
the relevant section of the runbook — key generation, keyserver upload, then the 30-minute wait
for the public key to replicate before signing will verify.

---

## 9. Bearded Robot (BR) app

Moved to its own file: **`bearded-robot.md`**, in this same folder.

It covers the local CT-Apps paths, the existing `br-*` skills to reuse, the `br-sync-core-sdk.yml`
automation that owns the dependency bump, the BR version scheme, the build commands, the
`SDK Bearded Robot Builds` page format, and four points of drift in the Confluence build doc —
including the one trap that matters most: with `com.clevertap.sdkSource=true` set in
`local.properties`, a `remoteAars` build silently compiles the SDK from your local working tree
instead of Maven Central.
