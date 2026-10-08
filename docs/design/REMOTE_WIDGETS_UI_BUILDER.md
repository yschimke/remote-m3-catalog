# `remote-widgets`: Mobile Launcher Widgets in Remote Compose

`:widget-catalog` publishes a second design system from this repository, `remote-widgets`, for
**phone launcher widgets** drawn with Remote Compose. It is deliberately a design system of its own
and not a mode of `remote-m3`. This note says why, what the two share, and what is still to do.

## Why it is not `remote-m3`

| | `remote-m3` (`:remote-catalog`) | `remote-widgets` (`:widget-catalog`) |
| --- | --- | --- |
| Surface | Wear OS widget, Wear components | Phone home-screen widget |
| Component library | `remote-material3` (Wear) over `remote-creation-compose` | `remote-creation-compose` + `remote-foundation` only |
| Theme | `RemoteMaterialTheme`, 29 colour roles, Wear type scale | None. Colours are literals (`WidgetColors`) |
| Widget class | `GlanceWearWidget` → `WearWidgetDocument` | `RemoteComposeWidget` (an `AppWidgetProvider`) → `Content(context, widgetId)` |
| Capture profile | `ANDROIDX` (stickers), Glance Wear's profile (widgets) | `RcPlatformProfiles.WIDGETS_V6`, what `RemoteComposeWidget` records with |
| Frame | Round watch sizes; Wear host containers (Small 216×76, Large 216×124) | **Launcher grid cells**: 1x1 … 5x2 |
| Design kit | M3 Wear OS kit, design-parity, kit coverage | None. Every component states `noReference` |

A design authored against one cannot run on the other: a `remote-m3` design names
`RemoteMaterialTheme` roles and Remote Material 3 components a launcher widget's classpath does not
have, and a launcher widget is not something a Wear host can frame. `verifyNoRemoteMaterial3`
fails `:widget-catalog`'s `check` if Remote Material 3 reaches its compile classpath by any route.

## Sizes are grid cells, not devices

A launcher sizes a widget in cells, so this sheet does too. Each frame is named by its cell count,
and its dp is the portrait size Android documents for that count, measured on a Pixel 4's 5x4 grid
([Determine a size for your widget](https://developer.android.com/develop/ui/views/appwidgets/layouts)):

    width  = 73n − 16 dp        height = 118m − 16 dp

| Cells | dp | Cells | dp | Cells | dp |
| --- | --- | --- | --- | --- | --- |
| 1x1 | 57×102 | 3x1 | 203×102 | 4x2 | 276×220 |
| 2x1 | 130×102 | 3x2 | 203×220 | 4x3 | 276×338 |
| 2x2 | 130×220 | 4x1 | 276×102 | 5x2 | 349×220 |

Real launchers vary the cell, which is why a design states the **count** and this dp is the count's
reference rendering. The table is written four times because annotations cannot compute and JSON
cannot read Kotlin: `WidgetSize` and the `@Widget<n>x<m>` frames in `WidgetSizes.kt`, the
`frame.geometry.sizesDp` the builder reads, and the `breakpoints` the sheet publishes.
`WidgetSizeTest` holds all four to the formula.

In the UI builder, `frame.geometry.sizesDp` is what the Screen dock's **Set frame from** menu opens
on, labelled `3x2` and so on, ahead of any device. That is compose-ui-builder's
`UiBuilderFrameGeometry.sizes`. The exporter maps a design's size back to its cell count
(`LauncherWidgetGrid`) to name the generated preview.

## What is shared, and what is not

**Shared:**

- **The vendored Remote Compose port.** Both modules substitute `:vendor:remote-creation-compose`
  and `:vendor:remote-foundation` for the published coordinates. They record with the same writer,
  and a port bump moves both sheets.
- **The exporter's body vocabulary.** compose-ui-builder's `RemoteContentEmitter` writes both
  catalogs' layouts, modifiers, state, actions and record-driven component calls. Only text
  differs: `RemoteTextVocabulary.CREATION` writes `remote-creation-compose`'s `RemoteText`, with
  no theme. Everything around the body is per catalog: `WearWidgetCodeExporter` for `remote-m3` and
  `LauncherWidgetCodeExporter` for `remote-widgets`.
- **The builder's generic Remote Compose palette.** `layout/box`, `layout/row`, `layout/column` and
  the rest are the builder's donor vocabulary for any `remote-compose` catalog, so neither policy
  re-declares them.
- **What the browser renderer is built from.** That is the CMP writer/player pair and
  `:ui-builder-foundation-adapters`. A `remote-widgets` Browser Preview would reuse them; see
  below.
- **The publishing pipeline.** It uses the same reusable `design-artifacts` workflow and the same
  output repository, with its own delivery branch.

**Not shared:**

- `remote-material3` and the material and wear adapters.
- Glance Wear and the Wear host frames.
- The theme catalogs (`RemoteThemeCatalogs.kt` are Wear M3 palettes).
- Design-parity, kit coverage, the page join and the `parallel` pairing gate. All of them exist to
  hold `remote-m3` to the Wear kit, which `remote-widgets` does not reproduce.

## The generated code

A design rooted on `remote-widgets/launcher-widget` exports as a `RemoteComposeWidget`. The shape
is AndroidX's `MyWidget` demo (`compose/remote/integration-tests/player-view-demos/…/widgets/`):

```kotlin
class CounterWidget : RemoteComposeWidget() {
    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color(0xFFF3EDF7).rc)) {
            RemoteRow(…) {
                WidgetButton(text = "-", modifier = RemoteModifier.weight(1f))
                RemoteText(text = "0".rs, color = Color(0xFF1D1B20).rc, fontSize = 48.rsp)
                WidgetButton(text = "+", modifier = RemoteModifier.weight(1f))
            }
        }
    }
}

@Preview(name = "3x2", widthDp = 203, heightDp = 220)
@Composable
fun CounterWidgetPreview() =
    RemoteContentPreview(profile = RcPlatformProfiles.WIDGETS_V6) {
        CounterWidget().Content(LocalContext.current, 0)
    }
```

`widget-catalog/src/test/kotlin/…/generated/CounterWidget.kt` is that file, generated from the
`counter-widget` template. It sits in the unit-test source set so that
`compileDebugUnitTestKotlin` builds it against this module's own classpath, the same gate
`WidgetTemplateRoundTripTest` gives `remote-m3`. Until a compose-ui-builder release carries
`LauncherWidgetCodeExporter`, the golden is the text that exporter's own
`LauncherWidgetExportTest` pins. Once the release is pinned here, an equality assertion against
the live exporter joins it.

## The hello world starter

`hello-widget` is the template the builder's built-in seed opens a new launcher widget on. The policy lists it first, but the schema cannot mark a default yet, so after the catalog-owned cutover the first entry is only the chooser's order. It shows "Hello, World!" centred on
the accent at 3x1, which is 203×102dp. That is the smallest grid size where the line fits at 24sp;
a 2x1 is only 130dp wide.

Two copies of the same design exist, one for each way the builder seeds a new design:

- compose-ui-builder's `LauncherWidgetTemplates` is the built-in seed, used while the
  catalog-owned cutover is off.
- `widget-catalog/ui-builder/designs/hello-widget.json` is the copy this catalog publishes, read
  once the cutover hands the catalog its own templates.

Its generated Kotlin is `generated/HelloWidget.kt`, compiled here:

```kotlin
class HelloWidget : RemoteComposeWidget() {
    @RemoteComposable
    @Composable
    override fun Content(context: Context, widgetId: Int) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color(0xFF6750A4).rc)) {
            RemoteBox(modifier = RemoteModifier.fillMaxSize(), contentAlignment = RemoteAlignment.Center) {
                RemoteText(text = "Hello, World!".rs, color = Color(0xFFFFFFFF).rc, fontSize = 24.rsp)
            }
        }
    }
}
```

## Not done yet

- **Visual Editor and Browser Preview.** The policy declares only the native surface. The
  editor's canvas draws the layouts through the generic donor adapters. A catalog-owned runtime
  would add a launcher-widget frame and record and play the document in the browser. It would
  reuse `:remote-catalog-ui-builder-renderer`'s CMP pair and foundation adapters, but not its Wear
  and Material ones.
- **New-design templates.** The builder's built-in seed offers only `hello-widget` for this
  catalog. The policy's other two templates, `launcher-widget-2x1` and `counter-widget`, are
  served once the catalog-owned cutover reads the policy's `templates`
  ([SEED_TEMPLATES](https://github.com/yschimke/compose-preview-server/blob/main/docs/design/UI_BUILDER_SEED_TEMPLATES.md)).
- **Actions.** The `counter-widget` template is the counter's layout, and its buttons are inert.
  `MyWidget`'s clicks are widget lambda actions (`RemoteModifier.onClick { … }`), Kotlin run in the
  app's process, which a builder document cannot author. `WidgetButton` takes no Remote action a
  design could bind instead. The working counter is `CounterWidget.kt`. Wiring a design's buttons
  needs one of two things: `WidgetButton` taking a Remote action (`valueChange` on document state),
  or the exporter writing `onClick` lambdas.
- **Pictures.** The launcher exporter refuses `asset/image` for now.
- **Publishing.** The `remote-widgets` design-artifacts lane needs a delivery branch in the output
  repository and a registration on the preview server.
