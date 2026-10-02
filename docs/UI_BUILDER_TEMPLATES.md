# The UI builder's template designs, and why four of them belong here

The UI builder offers a starting point when somebody makes a new design. For the `remote-m3`
catalog that is two widget host frames and two worked widget samples. All of them were **Kotlin
document builders in the preview server**, under `ui-builder-export/…/UiBuilderTemplates.kt`,
drawing components only this repository publishes.

The plan to move them is
[`UI_BUILDER_SEED_TEMPLATES.md`](https://github.com/yschimke/compose-preview-server/blob/main/docs/design/UI_BUILDER_SEED_TEMPLATES.md)
in that repository, under the
[catalog contract](https://github.com/yschimke/compose-preview-server/blob/main/docs/design/UI_BUILDER_CATALOG_CONTRACT.md)'s
phase 3a and phase 2 item 11. **The four `remote-m3` templates have arrived**, each as a design
document `:remote-catalog` declares in its `ui-builder.policy.json` and gates with a round-trip test,
under `remote-catalog/ui-builder/designs/` (`WidgetTemplateRoundTripTest`).

The Wear screen templates — `wear-screen` and `wear-list`, gated by `WearScreenTemplateRoundTripTest`
— belong to the Wear Compose sheet and live in
[yschimke/wear-m3-catalog](https://github.com/yschimke/wear-m3-catalog), which this repository was
split out of.

## What arrived

| Catalog | Template | What it draws |
| --- | --- | --- |
| `remote-m3` | `wear-widget-small` | the 216×76dp host frame, one empty content slot |
| `remote-m3` | `wear-widget-large` | the 216×124dp host frame, one empty content slot |
| `remote-m3` | `hello-widget` | centred text on the theme's primary, in the small host |
| `remote-m3` | `weather-widget` | location over a large reading on the sample's sunny blue, in the large host |

Each is a design document at `remote-catalog/ui-builder/designs/<template>.json`, beside the module's
cover sheet, named from `remote-catalog/ui-builder.policy.json` in a `templates` entry. The pipeline
copies `ui-builder/designs/` to the delivery branch beside `ui-builder.json`, the same way it copies
the record. (The schema takes paths only today; the label / supporting-text / order / default fields
are
[SEED_TEMPLATES step 3](https://github.com/yschimke/compose-preview-server/blob/main/docs/design/UI_BUILDER_SEED_TEMPLATES.md)
in compose-ai-tools, and the policy's `$comment_templates` records them until then.)

The two host frames are worth one line of their own: their dimensions are **already** authored here,
in `remote-catalog/ui-builder.policy.json`'s `frame.geometry.sizesDp`, from
`WidgetContainerPreviews.kt`'s pinned `@Preview(widthDp = 216, heightDp = 76)` and `(216, 124)`. The
document that starts a design in one of those frames being authored somewhere else is the split this
move closes.

One deviation from the server's Kotlin seeds, found by the round trip the seeds could never run: the
empty host frames carry a `layout/box` with `fillMaxSize` in their content slot rather than nothing,
because `WearWidgetCodeExporter` writes a literal `RemoteBox(modifier = RemoteModifier.fillMaxSize())`
for an empty content slot **without the imports for any of the three** — source that generates and
does not compile
([compose-ui-builder#26](https://github.com/yschimke/compose-ui-builder/issues/26)). The box is the
same starter `wearWidgetSampleDocument` gives the worked samples, and the compile gate holds it
there; drop it from both host documents when that lands.

## The test is the deliverable, not the copy

A template is the one document nobody authored, so nothing catches a property its catalog does not
declare except a check that runs. The preview server measured every template before proposing the
move: all validate against their catalog and all generate Kotlin. **What it could not do is compile
that Kotlin** — the generated `hello-widget` names `androidx.glance.wear.GlanceWearWidget`,
`WearWidgetDocument`, `androidx.wear.compose.remote.material3.RemoteText` and three
`WearWidgetPreview` parameter sets. Nothing in that repository has any of it on a classpath. This
module does.

So the round trip is what this repository owes: every template document → `ui-builder.json` →
generated Kotlin → **compiles against `:remote-catalog`'s own classpath** — its alpha Remote trio —
and rasterises on Robolectric through the real player, the same lane that produces every sticker in
it. It consumes the published `ui-builder-export` and `screen-model` coordinates, which the layer
rule allows — a leaf depends down. **Regenerating a golden is deliberate, never a hand-edit:** run
the module's test with `-PwriteGolden=true` and read the diff; a green compile on the new text is the
review.

The generated Kotlin is **compiled, not rasterised, in this repository's tests**. The player lane
that rasterises every `remote-m3` sticker runs `composePreviewRender` over the main source set's
`@Preview`s, and a generated file placed there would join the component record and need an exclusion
per preview symbol. Pixels are the native render lane's answer — `compose-preview-server design
render` against the same documents — which is also the lane the deployment uses.

## Two of them are somebody else's sample, and that does not make them vendored

`hello-widget` and `weather-widget` reproduce the widgets in [android/wear-os-samples'
`WearWidget` sample](https://github.com/android/wear-os-samples/pull/1386). Vendored sources — a
catalog holding upstream's bytes under upstream's package, declaring its inventory in a spec's
`groups` because an annotation written into one would be destroyed by the next import — are an
exception the sibling repositories make. None live here.

**These are not that.** They are designs drawn from the sample's layout, colours, type sizes and
strings, authored by hand, re-expressible in any catalog — not upstream's bytes, not re-fetched, not
patched. They enter as ordinary authored documents under `remote-catalog/ui-builder/designs/`, and no
import script will overwrite them. What they are not is a compile of the sample: the sample runs
Remote Compose on a watch and these reproduce its *design*, so the fidelity question belongs to the
parity lanes rather than to a template.
