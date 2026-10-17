# Remote M3 UI Builder rendering contract

`remote-m3` has three rendering surfaces. They share one semantic UI Builder document. The Visual
Editor and the Browser Preview share a renderer, the CMP Remote M3 one; Native / Live stays its own.

| Surface | Purpose | Implementation | Fidelity |
| --- | --- | --- | --- |
| Visual Editor | Select, insert, move and inspect nodes | The Browser Preview's recording, played, with each authored node tagged so the player reports its inspection (`RemoteM3AuthoringSurface`) | Real Remote Compose browser preview |
| Browser Preview | Inspect the Remote Compose result in each selected widget host | CMP/Wasm Remote M3 composed under rc-players' `RcEmbeddedPlayer`, a remote composition kept alive across edits | Real Remote Compose browser preview |
| Native / Live | Make the release decision | AndroidX Remote M3 records on Android; the Android player renders it | Authoritative |

The runtime handles protocol-v2 surfaces by walking the semantic document with the CMP Remote
creation APIs, composing real Remote M3 components, and playing them. Glance Wear host chrome
remains outside the document, just as it does on Android, and is applied per small/large widget
frame around the player. Neither surface rewrites the stored document.

The catalog does **not** declare the legacy `browserPreview: remote-compose-document` capability.
That capability asks the host/server to compile an RC document and causes the editor to bypass the
catalog runtime's DEVICE surface. It remains useful to catalogs without an executable runtime, but it
is the wrong ownership boundary here.

## Live playback

The design's remote composition stays alive under rc-players' `RcEmbeddedPlayer`, and an edit
recomposes only what changed: measured on the golden designs, 0.28–0.55 ms, against 0.7–3 ms for
recording a fresh document and decoding it. The capture engine that did that, and the Wear Compose
stand-ins the Visual Editor drew until then, are gone.

On the Visual Editor (`AUTHORING_UNROLLED`) each authored node carries an explicit component id,
and the player reports, per node:

- **bounds**, which drive selection, hit-testing, drop planning and the editor's empty-slot
  placeholders;
- **text layout** (line count and baselines) for each remote text;
- **a click**, which the runtime's `activate` action runs, as a tap would;
- **undrawn**: a component the tree can only draw as an "Unsupported" diagnostic is listed in the
  generation's `undrawn` reply, so the editor notes it and the authoring-canvas coverage gate fails.

Scroll modifiers are dropped there, so a scrolling container plays as a plain one; on the device
surface it scrolls. Not reported: `selected` follows the design's initial state, and `scrollBy` and
`revealNode`, which the editor sends only to the device view, are not available.

## Compatibility and pinning

Writer and player move as an explicit compatibility pair:

- writer: the vendored Remote Compose port identified by `vendor/remote-compose-upstream.json`
  (`4307936-ps17-cmp01` at the time this contract was added) for Remote M3, over rc-players'
  creation fork (`third-party-remote-creation-*`) for creation and foundation, declared together as `<port>+rc-players-<version>`;
- player: the `rc-players` release in `gradle/libs.versions.toml` (`1.69.0` at the same point).

Both values are pinned by the renderer's dependency graph and declared by the versioned runtime
contract. `remote-m3` emits `compose-ui-builder-runtime/v2`, which adds `remoteComposeWriter` and
`rcPlayer` to v1's five fields. The renderer ZIP verification checks the exact seven-field set and
both pinned values; neither schema accepts undeclared keys. The runtime is then published under an
immutable, exact runtime id. No part of recovery or preview may resolve `latest`, substitute a
merely compatible system, or follow a different catalog pin.

## Failure rules

- Unknown components and unsupported modifiers fail visibly in Browser Preview; they are not
  silently redrawn by a generic Compose component.
- Browser Preview is evidence that the CMP writer/player pair can represent the document, not a
  replacement for Native / Live.
- Native / Live remains authoritative for rectangular, Samsung squircle and Pixel Watch round hosts.
- A writer/player upgrade is reviewed as a rendering change and ships with the catalog runtime that
  names both exact versions.

This keeps the editor responsive, makes browser feedback real, and preserves Android as the final
source of truth without introducing a second stored design model.

## Layout vocabulary, and what a Wear widget can carry

The palette offers every core `remote-creation-compose` layout, and every surface draws each one.
A `remote-m3` design is still a Glance Wear widget, though, and `GlanceWearProfiles`
(glance-wear 1.0.0-alpha18) fixes which operations a widget document may contain. A layout outside
that profile is authored, previewed and exported as normal, and it **fails on Native / Live**:
the Android writer throws "Operation … is not supported for this version" while it captures the
document. The failure is meant to be obvious rather than silent, so the palette note says so, and
the generated Kotlin carries a comment above the call naming the missing operation.

| Remote Compose | Builder | In the widget profile | Native / Live |
| --- | --- | --- | --- |
| `RemoteBox`, `RemoteRow`, `RemoteColumn` | `layout/box`, `layout/row`, `layout/column` | yes | renders |
| `RemoteFitBox` | `layout/fit-box` | `LAYOUT_FIT_BOX` | renders |
| `RemoteStateLayout` | "Show by state" on `layout/box` | `LAYOUT_STATE` | renders |
| `Modifier.sharedElement` | `sharedElement` modifier | `ANIMATION_SPEC` | renders |
| `RemoteFlowRow` | `layout/flow-row` | no (`LAYOUT_FLOW` is experimental-only) | fails at capture |
| `RemoteCollapsibleColumn` / `Row` | `layout/collapsible-column` / `-row` | no | fails at capture |
| `collapsiblePriority` | `collapsiblePriority` modifier | no | fails at capture |

"Show by state" needs a build with Remote Compose authoring enabled (`-PuiBuilderRemoteCompose=true`).

`sharedElement` exports as `animationSpec(key, true)`. That overload exists in both the released
alpha19 (the native lane) and the vendored port the Browser Preview records with, and it produces the
same `AnimationSpec` operation as `sharedElement(key)`.

![Browser Preview, squircle host](../evidence/remote-m3-fit-box-state/browser-preview-squircle.png)
![Native, Android player](../evidence/remote-m3-fit-box-state/native-android-squircle.png)

The same Browser Preview drawing the three layouts outside the widget profile (their Native / Live render fails at capture):

![Browser Preview, flow row and collapsibles](../evidence/remote-m3-fit-box-state/browser-preview-outside-widget-profile.png)
