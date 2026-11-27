#!/usr/bin/env bash
# detekt baseline ratchet: fails when a module's detekt-baseline-release.xml holds MORE findings than
# the same file on the base branch. detekt itself only fails on findings missing from the baseline, so
# regenerating a baseline (./gradlew :<module>:detektBaselineRelease) could otherwise quietly absorb
# new debt. Shrinking or reshuffling entries (renames, refactors) is fine; growing the count is not.
#
# Usage: config/detekt/baseline_ratchet.sh [base-ref]
#   base-ref defaults to origin/$GITHUB_BASE_REF (set on pull_request events) and is fetched if needed.
set -euo pipefail

base_ref="${1:-}"
if [[ -z "$base_ref" ]]; then
  if [[ -z "${GITHUB_BASE_REF:-}" ]]; then
    echo "baseline ratchet: no base ref (not a pull request) - skipping"
    exit 0
  fi
  git fetch --no-tags --depth=1 origin "$GITHUB_BASE_REF"
  base_ref="FETCH_HEAD"
fi

status=0
for baseline in clevertap-*/detekt-baseline-release.xml; do
  head_count=$(grep -c '<ID>' "$baseline" || true)
  if ! base_content=$(git show "$base_ref:$baseline" 2>/dev/null); then
    echo "baseline ratchet: $baseline is new on this branch ($head_count findings) - skipping"
    continue
  fi
  base_count=$(grep -c '<ID>' <<<"$base_content" || true)
  if (( head_count > base_count )); then
    echo "::error file=$baseline::detekt baseline grew from $base_count to $head_count findings. Fix the new findings instead of baselining them."
    status=1
  else
    echo "baseline ratchet: $baseline $base_count -> $head_count"
  fi
done
exit $status
