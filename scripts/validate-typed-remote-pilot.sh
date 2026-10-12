#!/usr/bin/env bash
# The typed-export pilot: real-record definitions on the Remote Compose composables, the hidden
# `remote-m3-typed-pilot` pair, and every committed remote design exported through
# compose-ui-builder's generic `compose-component-records` export (docs/design/TYPED_REMOTE_PILOT.md).
# No production input or delivery is rewritten.
#
#   scripts/validate-typed-remote-pilot.sh [<compose-ai-tools checkout> [<compose-ui-builder checkout>]]
#
# With no checkouts the pilot resolves compose-ai-tools 2.52.0 and compose-ui-builder 3.109.0 as
# released; with them, from source (CI passes both at those tags).
set -euo pipefail

root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
sources=()
if [[ $# -gt 0 ]]; then sources+=("-PcomposeAiToolsDir=$(cd "$1" && pwd)"); shift; fi
if [[ $# -gt 0 ]]; then sources+=("-PcomposeUiBuilderDir=$(cd "$1" && pwd)"); shift; fi

runner=("$root/scripts/agent-gradle.sh")
if [[ ${CI:-false} == true ]]; then runner=("$root/gradlew"); fi
# Production discovery is the pair's inventory; the released lane, as the committed designs are.
"${runner[@]}" -p "$root" -PremoteSnapshot= :remote-catalog:composePreviewDiscover "$@"
"${runner[@]}" -p "$root" -PremoteSnapshot= -PtypedRemoteCatalog=true "${sources[@]}" \
  :catalog-typed-adapters:test :catalog-typed-adapters:generateTypedPilot \
  :catalog-typed-adapters:ktfmtCheck "$@"

printf 'Pilot pair and readiness: %s\n' "$root/catalog-typed-adapters/build/catalog"
