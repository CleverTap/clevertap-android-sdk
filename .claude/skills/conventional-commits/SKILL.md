---
name: conventional-commits
description: Write commit messages and PR titles in the Conventional Commits format used by this repo. Use when committing, changing a commit message, or writing a PR title or squash-merge message.
---

# Conventional Commits

Format: `type(scope): description`

- `type`: one of the types below.
- `scope`: the Jira key, for example `SDK-1234`. Leave it out only when there is no ticket.
- `description`: imperative, lower case, no full stop, about 72 characters or fewer.

## Types

| Type | Use for |
|---|---|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code change that keeps behaviour the same |
| `perf` | Performance improvement |
| `test` | Adding or fixing tests only |
| `docs` | Documentation only (README, `docs/`, changelog text) |
| `style` | Formatting only, no logic change |
| `build` | Gradle, dependencies, versions |
| `ci` | Workflows under `.github/` |
| `chore` | Cleanup and anything that fits nowhere else |
| `revert` | Reverting an earlier commit |

The branch prefixes `task/` and `bug/` are not commit types. For a `task/` branch use `feat`, `refactor`
or `chore`, and for a `bug/` branch use `fix`.

## Rules

1. One logical change per commit.
2. The PR title uses the same format. Task PRs are squash-merged, so the title becomes the commit message.
3. Breaking change: put `!` after the type or scope, for example `feat(SDK-1234)!: remove deprecated init`,
   and say what breaks in the PR description.
4. Revert: `revert(SDK-1234): short description`.

## Examples

- `fix(SDK-1234): use the current screen size to pick the tablet layout`
- `feat(SDK-1234): subscribe to the broadcast topic explicitly`
- `chore(SDK-1234): remove unused imports`
- `test(SDK-1234): complete the ignored executor test`
- `ci: run PR checks for release branches`
