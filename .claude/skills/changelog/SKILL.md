---
name: changelog
description: Add changelog entries for the CleverTap Android SDK in the right files and format. Use when a change is visible to SDK users, when asked to "add changelog", or before opening a PR that touches module code.
---

# Changelog entries

Add an entry only for changes that SDK users can see (new feature, behaviour change, bug fix, new
dependency). Pure refactors, tests and CI changes need none.

## Which files

| Module | File you edit |
|---|---|
| Core | `templates/CTCORECHANGELOG.md` |
| Push Templates | `templates/CTPUSHTEMPLATESCHANGELOG.md` |
| Geofence | `templates/CTGEOFENCECHANGELOG.md` |
| HMS | `templates/CTHUAWEIPUSHCHANGELOG.md` |

`docs/CT*CHANGELOG.md` is generated from `templates/` by the `copyTemplates` task. Edit only `templates/`,
then run `./gradlew copyTemplates` and commit the regenerated `docs/` file too. Never hand-edit `docs/`.
Check `git status` afterwards: other `docs/` files may change if their version strings were out of date.
The changelog templates have no `${...}` tokens today, so `docs/` ends up identical to `templates/`.

## Format

The newest version is at the top, under the `## CleverTap Android SDK CHANGE LOG` heading:

```
### Version 8.6.0 (November 10, 2026)

#### Improvements
* **Short Title:** What changed, in plain words, and what the app developer must do (if anything).
  Continue on the next line, indented by two spaces.

#### Bug Fixes
* **Short Title:** What was wrong, who was affected, and what happens now.
```

- Use `#### Improvements` for features and behaviour changes, `#### Bug Fixes` for fixes.
- Write for an app developer, not for the SDK team. Name the exception or symptom for a crash fix.
- Say if apps must change something, and which apps are not affected.
- If the version header for the release already exists, add to it. Do not start a new one.

## Release PR only

The release PR (`develop` to `master`) also needs:

- `CHANGELOG.md` at the root: a new `### <Month D, YYYY>` block with a link line such as
  `* [CleverTap Android SDK v8.6.0](docs/CTCORECHANGELOG.md).`
- The module version in `gradle/libs.versions.toml`. The root `build.gradle` task `copyTemplates` reads
  versions from there, so do not edit the generated version files by hand.

The workflow `mandatory_filechanges` needs both a `docs/` and a `templates/` changelog file to change, which
`copyTemplates` gives you. It blocks a release PR that changes a module's `src/` without these files.
