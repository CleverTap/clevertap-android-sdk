#!/usr/bin/env bash
# Downloads every artifact listed in consumers.txt from Maven Central into <dest-dir> as
# <artifact>-<version>.<ext>, for consumer_link_check.py. Fails if any download fails.
#
#   bash config/api-interop/fetch_consumers.sh build/consumer-jars
set -euo pipefail

dest="${1:?usage: fetch_consumers.sh <dest-dir> [consumers.txt]}"
list="${2:-$(dirname "$0")/consumers.txt}"
repo="${MAVEN_REPO_URL:-https://repo1.maven.org/maven2}"
mkdir -p "$dest"

failed=0
while read -r coord ext _rest; do
  case "$coord" in ''|'#'*) continue ;; esac
  IFS=: read -r group artifact version <<<"$coord"
  file="$dest/$artifact-$version.$ext"
  if [ -s "$file" ]; then
    continue
  fi
  url="$repo/${group//.//}/$artifact/$version/$artifact-$version.$ext"
  if curl -fsSL --retry 3 --retry-delay 2 -o "$file.part" "$url"; then
    mv "$file.part" "$file"
    echo "fetched $coord"
  else
    rm -f "$file.part"
    echo "FAILED to fetch $url" >&2
    failed=1
  fi
done < <(sed 's/#.*//' "$list")
exit $failed
