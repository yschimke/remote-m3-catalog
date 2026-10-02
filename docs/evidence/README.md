# Visual evidence for pull requests

Renders committed here so a PR body can **embed** the pixels a change produces, per the visual
evidence rule in [AGENTS.md](../../AGENTS.md). A reviewer must see the actual before/after in the
description, and a container with no image host has nowhere else to put them.

Most are `./gradlew :remote-catalog:composePreviewRender` outputs, copied out of
`remote-catalog/build/compose-previews/renders/` with the content hash stripped from the name.
Evidence for the Wear Compose sheet moved with it to
[yschimke/wear-m3-catalog](https://github.com/yschimke/wear-m3-catalog/tree/main/docs/evidence);
only the `remote-*` and `icon-remote-*` files came here with the split. They are
**evidence, not inventory**: the published sheet renders from the annotations on every run, the CI
visual-diff bot posts its own comparison, and nothing reads this directory. Replace a file when the
render it shows moves, and delete one whose component is gone.

## Kinds of file

**Plain renders.** A single `composePreviewRender` output, untouched.

**Contact sheets** (`remote-m3-folded-cells.png`, `remote-m3-shapes*.png`,
`remote-m3-position-and-text-cells.png`, `remote-m3-crossing-cells.png`). One component's variant
cells composited onto a dark board with each cell's name under it. A change that adds cells by the
dozen cannot be evidenced one PNG at a time, and the reviewer's question — does every cell draw
something, and is it the cell it claims to be — is about the grid rather than any one frame.

**Before/after pairs** (`remote-m3-*-{before,after}.png`, `icon-remote-*-{before,after}-*.png`).
The same preview rendered from `main` and from the change, as two files.

**Before/after boards** (`remote-m3-snapshot-lane.png`,
`remote-stepper-level-rail-{before,after}.png`). One component's affected cells rendered from `main`
on the top band and from the change on the bottom, with the lane in the title. The interesting thing
is almost always a *pair* — a type role, a glyph, a second label — so reading it means seeing the
same cell twice.

**Directories** for evidence that is a set rather than a frame:
[`remote-m3-hero/`](remote-m3-hero/) (the preview server's front-door hero before and after moving
`display.hero` to `AppCard`, fetched from the deployment rather than re-rendered — see its README),
`remote-m3-fit-box-state/` (the UI builder's Browser Preview against the native Android player, cited
by [docs/design/REMOTE_M3_UI_BUILDER.md](../design/REMOTE_M3_UI_BUILDER.md)) and
`remote-paint-reset/`.

## Compositing

A `remote-m3` sticker rasterises onto transparency, and several are near-white or a flat
`primaryDim` silhouette — invisible on a light page, so a reviewer opening the PR sees nothing at
all. Those frames are composited onto the sheet's own `#141418` ground; **no pixel of a render is
otherwise touched.** Stickers that draw a coloured container read on any background and are left
raw.

## Known-broken baselines

`remote-m3-*-break.png` are what `remote-snapshot-probe.py` compares each tracked issue's weekly
capture against; byte-identical means "still broken" with certainty.

**Refresh one only when THIS repo moved the sticker and the symptom is verified unchanged** — never
to quiet a probe that has started reporting, because a capture that stopped matching is the single
most interesting thing that job can say. Record what was verified: for
[yschimke/wear-m3-catalog#91](https://github.com/yschimke/wear-m3-catalog/issues/91)'s button
baseline, that is max alpha 31, the same container colour, and no pixel above the container's alpha
(so no label) across both captures — the framing moved, the bug did not.

`remote-m3-edge-button-label-spill-break.png` is a retired snapshot-lane baseline: build `16399547`
reduced its measured overhang to 0dp and the
[yschimke/wear-m3-catalog#249](https://github.com/yschimke/wear-m3-catalog/issues/249) probe was
removed. It remains as the record of the fixed defect; it cannot be reproduced with an empty
`-PremoteSnapshot=` because the component is absent from the released alphas.
