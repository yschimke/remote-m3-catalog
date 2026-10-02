#!/usr/bin/env bash
# Check that the two sheets' `parallel` declarations still pair — see scripts/parallel-map.mjs.
#
#   scripts/parallel-map.sh <wear-previews.json>
#
# Needs the discovered preview manifest of BOTH sheets, because the pairing is a statement about
# both and neither manifest carries the other's components. This repository holds the Remote sheet;
# the Wear sheet is `:catalog` in yschimke/wear-m3-catalog, so its manifest comes from a checkout of
# that repository (CI pins the commit in `.github/ci/wear-m3-catalog-ref`):
#
#   git clone https://github.com/yschimke/wear-m3-catalog .wear-m3-catalog
#   git -C .wear-m3-catalog checkout "$(cat .github/ci/wear-m3-catalog-ref)"
#   ./gradlew -p .wear-m3-catalog :catalog:composePreviewDiscover
#   ./gradlew :remote-catalog:composePreviewDiscover
#   scripts/parallel-map.sh .wear-m3-catalog/catalog/build/compose-previews/previews.json
#
# There is nothing to regenerate and nothing to commit, so there is no `--check`: this is a gate,
# and running it IS the check.
set -euo pipefail

WEAR="${1:?usage: scripts/parallel-map.sh <wear-m3-catalog catalog/build/compose-previews/previews.json>}"
REMOTE="remote-catalog/build/compose-previews/previews.json"
for manifest in "$WEAR" "$REMOTE"; do
  if [ ! -f "$manifest" ]; then
    echo "::error::$manifest is missing — discover both sheets first (see the top of this script)" >&2
    exit 1
  fi
done

node scripts/parallel-map.mjs \
  --previews "catalog=$WEAR" \
  --previews "remote-catalog=$REMOTE" \
  --canvas-policy remote-catalog/ui-builder.policy.json
