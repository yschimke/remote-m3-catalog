# Typed Remote export pilot (`remote-m3-typed-pilot`)

Can the generic `compose-component-records` export from compose-ui-builder, which lowers a design
through typed adapters on the real library records, replace the hand-written Remote Compose emitter
for `remote-m3`? Production `remote-catalog/ui-builder.policy.json` declares `composeSourceExport`
`remote-compose` v1. That routes to `WearWidgetCodeExporter` and `RemoteContentEmitter` in
compose-ui-builder. This note answers the question against the pinned releases, on the five
committed designs in `remote-catalog/ui-builder/designs/`, and describes the pilot module that
measures it.

Pins: compose-ai-tools **v2.52.0** (`9310fba0`), compose-ui-builder **v3.109.0** (`34701265`). The
reference approach is wear-m3-catalog's `catalog-typed-adapters` (`TYPED_WEAR_REAL_RECORDS.md`), and
the checklist is compose-ai-tools' `docs/design/TYPED_EXPORT_ADOPTION.md`.

## Verdict

**Not feasible at these pins. Every committed design is refused.** The generic path type-checks
every value against the real record. It never writes wrong Remote source. It refuses instead, and
that part holds. But it has no vocabulary for the four things a Remote widget is made of:

1. Remote value types: `RemoteString`, `RemoteColor`, `RemoteTextUnit`, `RemoteDp` and `Action`.
2. `RemoteModifier`.
3. Parent-decided alignment.
4. The widget/document wrapper, with three container roots.

Each of these is a tools or builder change, listed under [Gaps](#gaps-each-a-tools-or-builder-change).
The pilot stays honest by refusing each one by name. `RealRemoteExportTest` pins every refusal
exactly.

What does lower correctly today:

- the builder's `layout/box` and `layout/column` onto the real `RemoteBox` and `RemoteColumn`;
- a box's `contentAlignment` onto `RemoteAlignment.<Member>`;
- `RemoteText`'s `style` (`RemoteMaterialTheme.typography.<role>`), `textAlign` and `maxLines`,
  exactly as the hand emitter writes them.

These are not enough to export any design, because `RemoteText`'s required `text` cannot be
written, and neither can `RemoteButton`'s required `onClick`.

## What the hand emitter writes

For `hello-widget.json`, compose-ui-builder v3.109.0's `WearWidgetCodeExporter` writes:

```kotlin
@file:Suppress("RestrictedApi")

@RemoteComposable
@Composable
fun HelloWidgetContent() {
    RemoteMaterialTheme {
        RemoteBox(
            modifier = RemoteModifier.fillMaxSize(),
            contentAlignment = RemoteAlignment.Center,   // hoisted from the text's `align`
        ) {
            RemoteText(
                text = "Hello, World!".rs,
                color = RemoteMaterialTheme.colorScheme.onPrimary,
                fontSize = 20.rsp,
            )
        }
    }
}

class HelloWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(context: Context, params: WearWidgetParams): WearWidgetData {
        val colorScheme = RemoteColorScheme()
        return WearWidgetDocument(background = WearWidgetBrush.color(colorScheme.primary)) {
            HelloWidgetContent()
        }
    }
}

@Preview(name = "Rectangular Preview") @Composable
fun HelloWidgetRectangularPreview() = WearWidgetPreview(HelloWidget(), RectangularSmallWidgetPreviewParams()…)
// … Squircle and Round previews
```

The other designs add the following:

- `weather-widget.json`: `Color(0xFFFFFFFF).rc` for literal colours, `RemoteArrangement.spacedBy(4.rdp)`,
  `RemoteModifier.fillMaxWidth()`, `TextAlign.Center`, and the column's own `alignment` hoisted
  into the parent box's `contentAlignment`.
- `wear-widget-adaptive.json`: `fun WearWidgetContent(large: Boolean)` with an
  `if (large) { RemoteColumn … } else { RemoteRow … }` body. The body is synthesized by
  `AdaptiveWearWidget.resolve` from the container's `headline` / `supporting` / `action` slots,
  with `RemoteModifier.weight(1.rf)`. The adaptive design also has `RemoteButton(onClick = lambdaAction {})`
  and `params.containerType == ContainerInfo.CONTAINER_TYPE_LARGE`.
- `wear-widget-small.json` and `wear-widget-large.json`: `RemoteBox(modifier = RemoteModifier.fillMaxSize())`
  inside the same widget class and previews.

The document JSON lane (`RemoteDocumentJsonExporter`) is a separate output and is out of scope
here. The pilot only concerns Kotlin source export.

## What the generic path does with them

The test `RealRemoteExportTest` sends each design through `ScreenExportGate.export(document, record,
CatalogExportProjections.from(policy), CatalogScreenStructure.from(policy))`. It uses
compose-ui-builder's own readers over the pilot policy. The pinned refusals are:

| Design | Refused for |
| --- | --- |
| `hello-widget` | `text`, `color`, `fontSize` not written; the text's `align` needs a `BoxScope`; no component `widget-container-small` |
| `weather-widget` | the column's `verticalSpacingDp` and `alignment`; each text's `text`, `color`, `fontSize`; no component `widget-container-large` |
| `wear-widget-adaptive` | each text's `text` (and `color`); no component `widget-container-adaptive` |
| `wear-widget-small` / `-large` | no component `widget-container-*`; `RemoteBox.modifier` is `RemoteModifier`, the value is `androidx.compose.ui.Modifier` |

`ScreenExportGate` reports the projection's refusals and does not run the generator when the
projection refuses. So for a design refused by its projection, the generator's own refusals
(modifiers, and so on) are not listed. The `fillMaxSize` probe in the test shows the modifier
refusal directly.

A spike that wrote the Remote values with the codecs that exist was refused by the generator's type
check. These are the generator's own words:

- `RemoteText.color is RemoteColor, and this value is a androidx.compose.ui.graphics.Color` (a theme
  token as well as a literal)
- `RemoteText.fontSize is RemoteTextUnit, and this value is a androidx.compose.ui.unit.TextUnit`
- `RemoteArrangement.spacedBy`: `space takes RemoteDp, written androidx.compose.ui.unit.Dp`
- `RemoteColumn.verticalArrangement is RemoteArrangement.Vertical, and this value is a androidx.compose.foundation.layout.Arrangement$Vertical`
  (this is the builder's built-in `layout/column` lowering, before the pilot published a projection for it)
- `` `RemoteButton`.`onClick`.`callback` binds a handler with no actions `` (for an empty `lambdaAction({})`)

## Gaps, each a tools or builder change

None of these is changed here. Each is refused in the pilot until the API exists.

### 1. Remote value codecs (compose-ai-tools: `ExportProjection.Codec`, screen-model)

`ExportProjection.Codec` is sealed. The codecs that write the values `RemoteText` and the layouts
take all produce Compose types:

| Remote parameter | Hand emitter writes | Codec today writes |
| --- | --- | --- |
| `text: RemoteString` | `"…".rs` | `Text` → `"…"` (`kotlin.String`) |
| `color: RemoteColor?` | `RemoteMaterialTheme.colorScheme.<role>`, or `Color(0x…).rc` | `Color` → typed `androidx.compose.ui.graphics.Color` for both |
| `fontSize: RemoteTextUnit?` | `20.rsp` (`14.5f.sp.asRemoteTextUnit()` for fractions) | `Unit("sp")` → `20.sp` |
| `spacedBy(space: RemoteDp)` | `4.rdp` | `Unit("dp")` → `4.dp` |
| `enabled: RemoteBoolean` | `true.rb` | `Bool` → `true` |

**What is needed:** a codec that writes a value followed by a postfix extension conversion and
states the resulting type. For example, `Codec.Converted(inner: Codec, conversion: ChainLink(fqn,
property = true), typeFqn)` would write `"…".rs`, `20.rsp` and `4.rdp`. `Codec.Unit` also needs
`rsp` / `rdp` among its `UNITS`, or a conversion. In addition, `Codec.Color` needs:

- a `typeFqn` (a role through `RemoteMaterialTheme.colorScheme` is already a `RemoteColor`);
- a literal conversion (`.rc`) that applies only to literals.

Wrapping the whole value in a `call` is not a substitute:

- `RemoteColor(value = …)` (the public companion `invoke`) is correct for a literal. Around a theme
  role it type-checks against the codec's declared `Color`, yet would not compile, because the role
  is already a `RemoteColor`.
- `RemoteString(value = …)` is the same call `.rs` makes, but it is `@RestrictTo(LIBRARY_GROUP)`. The
  generator also printed it as `import …RemoteString.Companion` / `Companion.invoke(value = "…")`, so
  it is not used.

`text` is `RemoteText`'s only required parameter, so this one gap blocks every text and every
design.

### 2. A fixed Remote `Action` (compose-ai-tools: `ScreenGenerator` / `ExportProjection.Value.Fixed`)

`RemoteButton.onClick` is an `androidx.compose.remote.creation.compose.action.Action`, not a
`() -> Unit`. The hand emitter writes `lambdaAction {}` for an unbound click. The pilot tried a
`Fixed(Construct("…action.lambdaAction", positional = [ActionLambda([])]))`, and the generator
refused it ("binds a handler with no actions"). So the record's required `onClick` cannot be
written.

**What is needed:** either a fixed empty trailing lambda the generator accepts, or an event
mapping whose unbound value is a catalog-declared expression. Separately, `lambdaAction` is in the
CMP port's `androidMain` only. JVM discovery therefore cannot read it as a factory; it would have
to be admitted as a fixed callable.

### 3. A catalog modifier vocabulary (compose-ui-builder: `ScreenDocumentProjection.modifiers` / `link`)

Every authored modifier (`fillMaxSize`, `fillMaxWidth`, `padding`, …) is lowered onto
`androidx.compose.ui.Modifier` with `androidx.compose.foundation.layout.*` links. That is fixed by
the `MODIFIER`, `LAYOUT` and `DRAW` constants in `ScreenDocumentProjection`. Every Remote record's
`modifier` is a `RemoteModifier` with links in `androidx.compose.remote.creation.compose.modifier`,
so every authored modifier is refused.

**What is needed:** a catalog-published modifier vocabulary. That means the receiver
(`RemoteModifier`) and the callable for each authored modifier type. It could be published as a
`code.modifiers` block in the policy and read beside `code.locals`.

### 4. Parent-decided alignment (compose-ui-builder: the projection's `align` handling)

`RemoteBox`'s `content` is a plain `() -> Unit` with no scope, so a child cannot `align` itself.
The hand emitter hoists the children's `align` modifier into the parent's
`contentAlignment = RemoteAlignment.<corner>`. It refuses the design when the children disagree.
The same applies to `layout/column`'s own `alignment` property, which says how the column sits in
its parent.

A projection lowers one node and cannot read its children. The builder therefore refuses the child
("aligns itself, which `Modifier.align` supplies from a box's scope"), and the pilot refuses the
column's `alignment`.

**What is needed:** a projection target value read from its slot's children, such as a
"children's agreed alignment" value. Alternatively, a builder-side hoist for catalogs whose box has
no scope.

### 5. The widget frame (compose-ui-builder: `CatalogScreenStructure`, `ScreenRootFrame`; compose-ai-tools: `ScreenGenerator`)

- **Three roots.** `CatalogScreenStructure.from` takes the catalog's single `screen-root` builtin
  (`.singleOrNull()`). `remote-m3` has three: `widget-container-small`, `-large` and `-adaptive`. So
  `rootComponent` is null, the frame is never applied, and every design is refused as "no component
  `remote-m3/widget-container-*` in this catalog". **Needed:** a frame for each root component.
- **Root properties.** Even with one root, `ScreenRootFrame.unwrap` refuses every root property
  except `timeText` and `scrollIndicator`. The widget's `background` must become
  `WearWidgetDocument(background = WearWidgetBrush.color(…))`, and the theme typefaces must become a
  `RemoteMaterialTheme(typography = …)`. **Needed:** frame holes written from root properties
  through codecs.
- **Outside the function.** `ScreenGenerator` writes the frame inside
  `@Composable fun <Name>() { … }`. The hand export's `GlanceWearWidget` class, its
  `provideWidgetData` and the per-host-shape `@Preview`s live at file level, beside a
  `@RemoteComposable` content function. **Needed:** a file-level frame, or a catalog-declared
  wrapper around the generated function. `androidx.glance.wear` is also outside
  `ScreenExportGate.EXPRESSION_PACKAGES`.
- **Adaptive.** The adaptive container's body is synthesized per size (`AdaptiveWearWidget.resolve`)
  and chosen at runtime by `large`. That is a structural transform, not a template. It stays with
  the hand emitter, or becomes its own builder strategy.

### 6. Shared layout ids (compose-ui-builder host: foundation record pack)

The builder's `layout/box` and `layout/column` are lowered as Compose's `Box` and `Column` by
default. A host also joins compose-ui-builder's foundation record pack, which records them under
those same ids. The pilot publishes projections keyed by `layout/box` and `layout/column`, so
`ScreenDocumentProjection` lowers them onto `RemoteBox` and `RemoteColumn`, and their records carry
the layout ids. In the export test that works because the test builds the record the way a host
would without the pack. A real host that joins the pack would find two records for one id.

**Needed:** a catalog way to replace the shared layouts' record rather than add to it. Not verified
against a live host.

## The pilot module

`catalog-typed-adapters/` is opt-in through `-PtypedRemoteCatalog=true` (settings.gradle.kts) and is
not in the default build:

- `RealRemoteRecords` reads `RemoteText`, `RemoteButton` (the content overload), `RemoteBox` (the
  content overload) and `RemoteColumn` with `TypedAdapterDiscovery.discoverLibrary`. It reads
  them from the vendored CMP port's metadata on the JVM classpath. The same
  `androidx.compose.remote` / `androidx.wear.compose.remote` API is what the Android export calls.
  `RealRemoteRecordsTest` pins the Remote types the gaps are about.
- `RealRemoteAdapters` holds the four definitions. Each property is either written or
  `exportRefuses`d by name. `RealRemoteAdaptersTest` holds the layouts to an empty
  `validateAgainst`. It pins the two shelf findings, which are the required `text` and `onClick`.
- `TypedRemotePilot` builds the pilot policy from the production authored policy
  (`catalog-typed-policy`):
  - the catalog id is `remote-m3-typed-pilot`;
  - the shelf components gain `exportProjection`;
  - the real records ship as `remote-m3/export-*` targets off the shelf, and the layout
    projections are keyed by `layout/box` and `layout/column`;
  - a `screen-root` frame of `${content}` is added (gap 5);
  - `compose-component-records` v1 is selected.
- `generateTypedPilot` writes the pair (`components.json`, `ui-builder.json`,
  `ui-builder.policy.json`, the templates, `readiness.json`) under
  `build/catalog/remote-m3-typed-pilot/`. It reads production's discovery of `:remote-catalog` and
  fails on any generation diagnostic other than the off-shelf exclusions.
- `RealRemoteExportTest` exports all five committed designs through `ScreenExportGate` and pins each
  refusal list. It also confirms that `WearWidgetCodeExporter` still writes all five, and that a box
  and column with nothing Remote-valued emit `RemoteBox(contentAlignment = RemoteAlignment.Center) {
  RemoteColumn { … } }`.

Run it with `scripts/validate-typed-remote-pilot.sh [<compose-ai-tools> <compose-ui-builder>]`. CI
runs it in `typed-catalog.yml`'s `remote-pilot` job, against source checkouts of both repositories
at the pinned tags.

## Publishing: not done, and why

The pilot builds, but no publish lane to `yschimke/remote-m3-catalog-out`
`design-artifacts/remote-m3-typed-pilot` is added, for two reasons:

1. **Nothing would be A/B tested.** Every committed design is refused by the pilot's export. A served
   pilot would offer the builder a Kotlin export that refuses everything, against production's that
   writes everything.
2. **The reusable workflow cannot publish a generated pair.** compose-ai-tools'
   `design-artifacts-reusable.yml` derives the catalog from the module's spec and its committed
   `ui-builder.policy.json`. No input substitutes a generated policy, record and catalog id. This
   repository's rule (AGENTS.md) is that such a capability becomes a generic input upstream, never
   a forked pipeline here.

Once gaps 1 to 5 land and at least one design emits, the lane is the production `remote` job
plus an upstream input, for example `ui-builder-pair-artifact`, that points at
`catalog-typed-adapters/build/catalog/remote-m3-typed-pilot`. It would publish with `system:
remote-m3-typed-pilot` and reuse the existing secrets.

## Order

1. Gaps 1 and 2 in compose-ai-tools. Then `RemoteText` and `RemoteButton` validate, and a box of
   texts exports.
2. Gaps 3 and 4 in compose-ui-builder. Then `weather` and the starters lower, apart from the frame.
3. Gap 5 (and 6) across both. Then the four non-adaptive designs emit through a per-container
   frame.
4. The publish lane, then the shelf beyond the five designs, then cutover. Cutover is the
   production policy's `composeSourceExport` switching in the same change as the runtime.
