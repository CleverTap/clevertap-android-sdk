---
name: pr-prepare
description: Prepare a pull request for the CleverTap Android SDK. Use when asked to "prepare pr", "create pr", "open pr" or "ready for review", or before pushing a branch for review. Checks the branch name, changelog and version files, checks to run, secrets and the PR description.
---

# Prepare a PR

Work through the steps in order. If a step fails, stop and tell the user. Do not skip it.

## 1. Branch

- Name: `task/SDK-1234/short-name` or `bug/SDK-1234/short-name`. The other accepted prefixes are in
  `.github/workflows/on_pr_from_task_to_develop.yml`.
- A branch whose name does not start with an accepted prefix gets no CI. Rename it before opening the PR.
- The base branch is `develop`. The only other normal base is `master`, for the release PR (`develop` to `master`,
  see the `release` skill); it has its own checks. A PR into any other branch (for example a release branch) gets no CI.

## 2. Commits and title

Follow the `conventional-commits` skill for every commit and for the PR title.

## 3. Release files

If the change is visible to SDK users, use the `changelog` skill (edit `templates/`, then run `copyTemplates` to regenerate `docs/`). The release PR (`develop` to `master`) also needs `CHANGELOG.md` and the
version in `gradle/libs.versions.toml`; the release owner updates those unless the user asks you to.

## 4. Checks

Run the `build-test-verify` skill for every changed module. Fix failing tests. For lint, detekt and
checkstyle, make sure the files you touched have no new warnings.

## 5. Secrets and local files

Check what will be pushed, not only the working tree: `git status`, then `git diff origin/develop...HEAD` and
`git log -p origin/develop..HEAD` (a secret removed in a later commit is still in history). There must be no `local.properties`, tokens, signing files, or real
account values in `sample/src/main/AndroidManifest.xml`.

## 6. Description

Use these headings, short and in plain English:

- **Problem**: what is wrong or missing, and who is affected
- **Change**: what you changed and where
- **Testing**: what you ran and what you saw, and what you did not test
- **Ticket**: the Jira key

Give exact results (counts, versions), not just "tests pass".

## 7. When CI will not run

Say so in the description, and run the "Manually validate any branch" workflow on the branch
(lint, detekt, checkstyle, tests, build).
