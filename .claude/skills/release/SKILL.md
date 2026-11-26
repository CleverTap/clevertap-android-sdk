---
name: release
description: Drives an end-to-end CleverTap Android SDK release — release branch, version bumps, changelog, copyTemplates, chores PR, merge to master, Maven Central publish, git tag, GitHub release, Slack message and the Confluence release page. Use when releasing any module (core, push templates, Huawei, geofence). Confirms every state-changing step with the user before doing it.
disable-model-invocation: true
---

# CleverTap Android SDK — Release Driver

You drive a full SDK release from start to finish. Every output format, derivation rule and verified
command lives in the reference files — read them before you begin, and follow them literally. Do not
invent formats.

| Reference | Covers |
|---|---|
| **`refs/mined-patterns.md`** | The manifest, version-bump touch points, changelog / GitHub release / Slack / Confluence formats, and the verified publish commands |
| **`refs/bearded-robot.md`** | Phase 7 — the BR app: paths, the sync workflow that owns the dependency bump, build commands, and the `-PctSdkSource=false` trap |

---

## Arguments

Anything the user types after `/release` is free-form input. Parse what you can, then ask only for
what is still missing. Never guess a version number or a Jira ticket.

| What the user typed | How to read it |
|---|---|
| `/release` | Nothing supplied. Ask for everything in Phase 0. |
| `/release core 8.4.0` | Module `core` → version `8.4.0`. Still ask for the date, Jira ticket, testing scope, Dev QA. |
| `/release core 8.4.0, pushtemplates 2.5.0` | Multi-module. Combined tag `corev8.4.0_ptv2.5.0`. |
| `/release core 8.4.0 SDK-5955 on July 30` | Version, Jira ticket and date all supplied. Confirm your reading of it back before using it. |
| `/release dry run for core 8.4.0` | Walk the flow but **stop after the chores PR** — no merge, no publish, no tag, no announcement. Say clearly at the start that this is where you will stop. |
| `/release resume` | Do not assume anything. Inspect the actual state first — current branch, whether the chores PR exists or is merged, whether the tag exists, whether the version is on Maven Central — report it, then ask which step to continue from. |

Module aliases: `core` → `clevertap-core`, `pushtemplates` / `pt` → `clevertap-pushtemplates`,
`hms` / `huawei` → `clevertap-hms`, `geofence` → `clevertap-geofence`.

**Always echo your interpretation back and get a yes before acting on it.** A misread version
number would propagate into every file, the tag, and the announcement.

---

## The one rule that overrides everything: confirm before you act

**Read freely. Never change state without showing the user exactly what you are about to do and
getting an explicit "yes".**

| Action type | Behaviour |
|---|---|
| Reading files, `git log`, `git status`, `gh pr view`, listing tags, reading Confluence | Just do it. No confirmation. |
| Anything that writes a file, creates a branch, commits, pushes, opens or merges a PR, runs a Gradle publish, creates a tag or release, or creates or edits a Confluence page | **Preview, then confirm, then act.** |

A preview must show the **literal thing** — the exact tag name, the exact file diff, the exact
message body. Never a summary like "I'll bump the version". Show `8.3.0` → `8.4.0`.

If the user says no or asks for a change, adjust and re-preview. Do not proceed on a maybe.

**Never batch confirmations.** One step, one preview, one yes. The point is to catch mistakes
while they are still cheap.

Two standing preferences that reinforce this:
- **Never push automatically.** Every `git push` is its own confirmation.
- **Never stage local design or task `.md` files.** Release commits contain production code,
  changelogs, docs and version files only.

---

## Phase 0 — Build the release manifest

Everything downstream renders from this one object. Get it right and the rest is mechanical.

### Ask the user for

| Input | Notes |
|---|---|
| Which modules are shipping, and the target version for each | e.g. core `8.4.0`, push templates `2.5.0` |
| The planned release date | Used for **both** the changelog and the GitHub release name — they must match |
| The Jira release ticket | e.g. `SDK-5955`. Always ask; never guess. |
| Testing scope bullets | Goes on the Confluence page |
| Who did Dev QA | Confluence mention |
| GitHub issues fixed | Links, or `N/A` |

### Derive, never ask

Compute these and show your working. Rules and worked examples are in `refs/mined-patterns.md` §1.

- Current version per module — read `gradle/libs.versions.toml`
- Previous tag per module — `git tag --list 'corev*' --sort=-v:refname | head -1`.
  **Same module only.** For core 8.4.0 that is `corev8.3.0`, never an intervening `ptv*` tag.
- Module tags and the combined tag — `corev8.4.0`, or `corev8.4.0_ptv2.5.0` for multi-module
- Sample app `versionCode` — `major * 1_000_000 + minor * 10_000 + patch * 100`
- Changelog heading anchor — for the Slack deep link

### Draft the changelog prose

Read the merged PRs and commits since the previous tag:

```bash
git log <previous-tag>..HEAD --oneline --no-merges
gh pr list --state merged --base develop --limit 40 --json number,title,mergedAt
```

Turn that into changelog entries in the exact house style — bold label, colon, prose, backticked
identifiers, nested `**Note:**` bullets for caveats. Section order is fixed: **New Features →
Improvements → Behavioral Notes → Breaking API Changes → Bug Fixes**. Omit empty sections.

This is the one genuinely creative step. Take care over it — every other surface re-renders it.

### 🚦 Confirm

Show the complete manifest and the drafted changelog. Get a yes before touching anything.

---

## Phase 1 — Create the Confluence release page

Do this **first**: the Slack message at the end needs this page's URL.

- Parent page id `2139258925` (SDK Releases)
- Title `[Android Release] Month D, YYYY`, or `[Android Hotfix] …` for a hotfix
- Body follows `refs/mined-patterns.md` §5 exactly — SDK table, changelog code block, TAN,
  Testing Scope, Jira Tickets, Dev QA, QA Testing, Github Issues Fixed, Checklist
- The tag URL in the SDK table is predictable and safe to write now — it 404s until you publish
  the GitHub release, then starts working
- Every checklist row starts at `No`

🚦 **Preview the full page body, then confirm, then create.** Record the resulting page URL.

---

## Phase 2 — Branches, version bumps, changelog, chores PR

### Pre-flight (read-only)

Verify and report: on `develop`, working tree clean, `develop` up to date with origin, and no
existing tag or branch that would collide. Stop and ask if any check fails.

### Steps, each individually confirmed

1. 🚦 Create the release branch `task/release/<module>/<version>` from `develop`
2. 🚦 Create the chores branch `chore/release/<version>` from the release branch
3. 🚦 Bump `gradle/libs.versions.toml`
4. 🚦 Bump `sample/build.gradle` — **four separate places** in that one file: `versionCode`,
   `versionName`, `remoteImplementation` and `stagingImplementation`. Missing one is the classic
   error. Preview each as a diff.
5. 🚦 Prepend the changelog entry to `templates/CTCORECHANGELOG.md` — and the equivalent template
   for every other shipping module
6. 🚦 Prepend the dated entry to the root `CHANGELOG.md`
7. 🚦 Update `templates/EXAMPLES.md` only if the release changes public API usage
8. 🚦 Run `./gradlew copyTemplates` — **must come after step 3**, since it reads versions from
   `libs.versions.toml`. Then show `git status` and the diff.

   **Never hand-edit root `README.md` or `docs/`, except through `templates/`: change `templates/`, then run
   `copyTemplates`.** Both are generated by
   `copyTemplates` via Groovy token substitution (`build.gradle:53`). The version strings in the
   README's dependency snippets update by themselves. Expect the diff to include files you never
   touched — `docs/CTGEOFENCE.md`, `docs/CTPUSHTEMPLATES.md` and others change because their
   embedded version strings were re-substituted. That is correct. See `refs/mined-patterns.md` §2.
9. 🚦 Commit. Preview the message first. Stage production files only.
10. 🚦 Push the release branch first if it is not on the remote yet (the PR base must exist there), confirm
    that push separately, then push the chores branch
11. 🚦 Open the PR — title `docs changes and version bump <JIRA-ID>`, base = the release branch

### 🛑 Human gate — PR review

Stop. The user gets the PR reviewed and approved. Do not proceed until they say it is approved.

---

## Phase 3 — Merge to master

1. 🚦 Merge the chores PR into the release branch
2. 🚦 Open and merge the PR into `develop`
3. 🚦 Open and merge the PR from `develop` into `master`

Confirm each merge separately. After the last one, verify `master` contains the expected version
in `gradle/libs.versions.toml` and report it back.

---

## Phase 4 — Build and publish to Maven Central

### Pre-flight checks (read-only)

Report a table of pass/fail. **Never print secret values — key names only.**

| Check | How |
|---|---|
| On `master`, tree clean, up to date | `git status` |
| Signing config present | key names in `~/.gradle/gradle.properties`: `signing.keyId`, `signing.password`, `signing.secretKeyRingFile` |
| Portal credentials present | `mavenCentralUsername`, `mavenCentralPassword` |
| Keyring file exists | test the path in `signing.secretKeyRingFile` |
| GPG key usable | `gpg --list-secret-keys`, not expired or revoked |

If anything is missing, stop and walk the user through the relevant part of the
[publish runbook](https://wizrocket.atlassian.net/wiki/spaces/EN/pages/2894692467/Android+SDK+Publish+Process)
— key generation, keyserver upload, and the ~30 minute wait for the public key to replicate before
signing will verify.

### Date drift check

Compare today against the planned release date. If it slipped, **stop and ask**: either correct
the date in `CHANGELOG.md`, `templates/CTCORECHANGELOG.md` and `docs/CTCORECHANGELOG.md` with a
follow-up commit to master, or knowingly accept the mismatch. Never let the GitHub release name
disagree with the changelog.

### Publish

Use the **proven** commands — see `refs/mined-patterns.md` §8 for why these and not others.

1. 🚦 `./gradlew clean`
2. 🚦 `./gradlew :<module>:assembleRelease`
3. 🚦 `./gradlew :<module>:publishMavenPublicationToMavenLocal` — optional dry run
4. 🚦 **`./gradlew :<module>:publishMavenPublicationToMavenCentralRepository`** — the real upload.
   Confirm this one loudly and explicitly. Repeat per module.

**Never run `publishAndReleaseToMavenCentral`.** It auto-releases and destroys the gate below.

### 🛑 Human gate — the irreversible one

Send the user to <https://central.sonatype.com/publishing/deployments>. They confirm a deployment
exists and reads **Validated**, check the Components list, then click **Publish** themselves.

Say plainly why this gate exists: **a version published to Maven Central can never be deleted or
changed.** A bad `8.4.0` can only be answered with `8.4.1`, forever.

Then wait for it to go live — 5 to 10 minutes — by polling:

```bash
# one ARTIFACT:VERSION pair for every module in the release manifest, not only core
for PAIR in clevertap-android-sdk:8.4.0; do
  A=${PAIR%%:*}; V=${PAIR##*:}
  curl -sI -o /dev/null -w "$A $V %{http_code}\n" \
    "https://repo1.maven.org/maven2/com/clevertap/android/$A/$V/$A-$V.aar"
done
```

Go on only when every module prints `200`.

Search indexing on `search.maven.org` takes about two hours. Not being there yet is normal and
does not mean the release failed.

---

## Phase 5 — Tag and GitHub release

The user asked for this to be explicitly consulted. Be thorough.

### 🚦 Confirm the tag before creating it

Show all of:

| | |
|---|---|
| Exact tag name | `corev8.4.0` — or the combined form for multi-module |
| Target commit | the SHA and subject line it will point at |
| Branch | must be `master` |
| Previous tag it follows | `corev8.3.0` |

Only after a yes: create the tag, then 🚦 confirm again before pushing it.

### 🚦 Confirm the release, then publish it

Build the release exactly per `refs/mined-patterns.md` §4 — name is the date, body is two lines,
compare range is `<previous>...<current>` with previous **first**.

Double-check the compare direction. Two of the last five releases had this wrong, one with the
range reversed.

Create it as a **draft** first, share the URL, let the user look, then 🚦 confirm and publish.

---

## Phase 6 — Slack message

Render the **paste-safe** variant from `refs/mined-patterns.md` §7 — bare URLs and `*bold*`.

Slack is not Markdown. Do not use `[label](url)`, and do not use `<url|label>` either: that is
API-only syntax and pastes as literal text. Bare URLs auto-link.

The changelog link deep-links to the version heading, e.g.
`docs/CTCORECHANGELOG.md#version-840-july-30-2026`.

Output it in a plain code block for easy copying. **The user sends it** — there is no Slack
credential configured. Do not claim it was sent.

---

## Phase 7 — Bearded Robot (BR) app

Full detail in **`refs/bearded-robot.md`**. Read it before starting this phase.

This must run **after Phase 4**, because the `remoteAars` flavor resolves the CleverTap SDK from
Maven Central — the artifacts have to be live first.

A note on timing so it does not confuse you: the Confluence build doc says BR goes to the internal
testing track *3 days before* the SDK release. That is a separate pre-release QA activity. **This
phase is the post-release sync** — point BR at the published versions, build, and record it.

### 7a. Is BR in sync with the release?

Read the `remoteAarsImplementation` lines in
`CT-Apps/BeardedRobot/BeardedRobot/build.gradle.kts` and compare against what was just published.

Two **independent** questions. Answer both, and do not let the first block the second.

**1. Are the release's new public APIs showcased in the app?**

- If yes — say so and move on.
- If not — **inform, do not block.** Name the APIs that are not demonstrated, and say plainly that
  it looks like showcasing them was missed. The user may well choose to leave it for later; that is
  fine and expected. Point them at the **`br-sdk-integration`** skill in
  `CT-Apps/BeardedRobot/.claude/skills/` — it is the proper entry point for that work, with its own
  approval gates. Do not attempt to add showcase code from this skill.
- Either way, **carry on to the dependency update.**

**2. Are the pinned dependency versions the ones just published?**

- Already correct → go straight to 7b.
- Not correct → 7a-bump below.

### 7a-bump. Dependency bump — prefer the workflow, fall back to hand

`CT-Apps/.github/workflows/br-sync-core-sdk.yml` normally **owns** those version lines: it runs daily,
reads the latest version from Maven Central, files a Jira ticket, bumps, validates, and opens a PR.
When it is healthy, do not edit `build.gradle.kts` yourself — you would collide with it.

**But it does break.** Check its health before relying on it.

**Step 1 — is a PR already open?**

```bash
gh pr list --repo CleverTap/CT-Apps --state open --search "bump-core-android" --json number,title,headRefName
```

If one exists for this version, there is nothing to do — go to the human gate below.

**Step 2 — check the last few runs.**

```bash
gh run list --workflow br-sync-core-sdk.yml --repo CleverTap/CT-Apps --limit 5
```

| What you see | Do this |
|---|---|
| Recent runs succeed | 🚦 Dispatch it rather than waiting ~24 hours: `gh workflow run br-sync-core-sdk.yml --repo CleverTap/CT-Apps -f platform=android -f force_version_android=<version>` |
| Recent runs **fail** | Read the reason with `gh run view <id> --repo CleverTap/CT-Apps --log-failed`, **report it to the user**, then use the manual fallback below |

Always tell the user *why* it failed. A broken sync workflow is a real problem in its own right, not
just an obstacle — do not silently route around it.

**Step 3 — manual fallback, only when the workflow is broken.**

⚠️ **Use the workflow's own branch name.** `br-sync-decide.sh` treats an existing remote branch as
"a PR is already in flight" and skips. Matching the name means that once the workflow is fixed it
will *not* raise a duplicate PR for the same bump:

```
chore/bump-core-android-<version>          e.g. chore/bump-core-android-8.4.0
```

Then, each step 🚦 confirmed separately:

1. Branch from an up-to-date `main` in `CT-Apps`
2. Edit **only** the `remoteAarsImplementation` lines in
   `BeardedRobot/BeardedRobot/build.gradle.kts` — core, geofence, HMS, push-templates as applicable
3. Commit, referencing the Jira ticket if one exists — the workflow would normally create it, so if
   it never ran, ask the user whether to file one
4. Push, and open a PR to `main`

### 🛑 Human gate — either route

The PR needs the user's review and merge. **Do not merge it.** Wait for confirmation, then build from
the merged `main`.

### 7b. Build the APKs

⚠️ **Always pass `-PctSdkSource=false`.** `CT-Apps/BeardedRobot/local.properties` contains
`com.clevertap.sdkSource=true`, which switches on the composite build and substitutes the Maven
coordinates with the **local SDK working tree**. Without the override you would produce an APK
containing unreleased local code while its filename claims it is the released build. Nothing errors.

Before running these commands, set `CT_APPS_DIR` to your local CT-Apps clone. If it is not set, ask the user for the path.

🚦 Confirm, then:

```bash
cd $CT_APPS_DIR/BeardedRobot
./gradlew :BeardedRobot:assembleRemoteAarsRelease -PctSdkSource=false
./gradlew :BeardedRobot:assembleRemoteAarsDebug   -PctSdkSource=false
```

Then **prove it resolved from Maven** before trusting the output:

```bash
./gradlew :BeardedRobot:dependencies \
  --configuration remoteAarsReleaseRuntimeClasspath -PctSdkSource=false | grep clevertap
```

Real Maven coordinates (`com.clevertap.android:clevertap-android-sdk:8.4.0`) mean it is genuine;
`project :clevertap-core` means the substitution happened and the APK must be discarded.

Report the output paths and the APK version. The BR `versionName` comes from `gradle.properties`
(`<major>.<minor>.<patch>.<qa>[-<suffix>]`) — 🚦 confirm the **suffix** with the user, since that is a
human choice, not derivable.

### 7c. Drive upload and the builds page

**Upload with the script** — set up and verified working on 2026-07-30:

```bash
# SDK_DIR = the path of this SDK repo (the working directory may be CT-Apps by now)
~/.venvs/br-drive/bin/python "$SDK_DIR/.claude/skills/release/scripts/upload_br_apks.py" \
  <release.apk> <debug.apk>
```

It streams each file to the Drive folder and prints the `drive.google.com/file/d/<id>/view` links
the builds page needs. Authentication is cached, so it runs without a browser. 🚦 Confirm before
running — it writes into a shared team folder.

Useful flags: `--replace` updates a same-named file instead of creating a duplicate (plain uploads
do **not** deduplicate); `--anyone` grants "anyone with the link" if folder inheritance is not
enough.

If it fails, the two likely causes are the Drive API being disabled on the project, or the
`drive.file` scope being too narrow — the script detects the first and explains the fix, and
`refs/bearded-robot.md` covers the second.

**Fallback:** if the script is unavailable — a different machine, missing credentials — ask the user
to upload both APKs to the
[Drive folder](https://drive.google.com/drive/folders/1fK79PXJh4gz3-LqZAYa-AcMNsR6MVcZV) by hand and
give you the two share links. Nothing else in the phase changes.

🚦 Preview the new row, confirm, then add it to the top of the table on Confluence page
`5327192075`. Every row carries **all four module versions** — core, geofence, HMS, push templates —
read from `build.gradle.kts`, not just the ones that changed.

### 7d. Play Store internal testing — ask, never assume

Ask whether they want to upload to the internal testing track now. It may have been done already, or
deliberately skipped.

- **Yes** → the Play Store needs an **AAB, not an APK**, so build
  `./gradlew :BeardedRobot:bundleRemoteAarsRelease -PctSdkSource=false`, then hand off — the upload
  itself is manual, as is the `beardedrobot` Slack announcement.
- **No** → leave it, and list it as outstanding in the Phase 8 summary.

---

## Phase 8 — Close out the Confluence page

The historic failure mode is that this page is created and abandoned — one real page from a release
two months old still had ten of eleven checklist rows on `No`.

🚦 Preview the updated checklist, confirm, then update the page: tick every completed row, fill in
the tag link, the QA Testing status, and anything else now known.

Some checklist rows cover work this skill does **not** do — `Update sample apps - BR, CTSample` and
`Clevertap website updates [docs page]` among them. **Ask the user about those; never tick a row on
their behalf.** Leave them at `No` if unconfirmed.

Then report a final summary: versions shipped, tag, GitHub release URL, Confluence URL, and — stated
explicitly — anything still outstanding. Do not call the release complete while any step is pending.
List what is left.

---

## If something fails halfway

Say exactly where it stopped and what state things are in — branch, whether the PR is open, whether
the upload happened, whether the tag exists. Never re-run a state-changing step without checking
whether it already took effect. Publishing twice, or pushing a tag that already exists, creates a
worse mess than stopping and asking.
