# Embedded documents: prototype and plan

**Prototype. Not for landing.** This directory and the `PROTOTYPE (embedded documents)` changes
under `vendor/remote-creation-compose` exist to answer three questions:

1. Should the CMP creation frontend (`vendor/`) move to rc-players?
2. Can a composition build the types the CMP player consumes (`RcDocument`) directly, skipping the
   encoded `.rc`?
3. Could that replace the UI builder's "fake remote-m3" editor stand-ins?

## What the prototype does

| Piece | Where | What it shows |
| --- | --- | --- |
| `captureCommonRemoteDocumentInto(writer, …)` | `vendor/…/capture/CaptureRemoteDocument.common.kt` | Capture takes any `RemoteWriter`. The byte path becomes one caller of it. |
| `RcOperationWriter : RemoteWriter` | `src/main/…/RcOperationWriter.kt` | Builds `ee.schimke.composeai.rcplayer.protocol.RcDocument` directly. It mirrors `RemoteDocumentWriter`'s id allocation, dedup and declaration hoisting. It covers the layout, text, modifier and data subset; any other call throws, naming itself. |
| `RemoteDocumentSession` | `vendor/…/capture/RemoteDocumentSession.kt` | A remote composition that stays alive under a host `CompositionContext`. Each edit recomposes only what changed, then `render(writer)` re-records the retained node tree. This mirrors upstream's Android capture loop: a fresh writer and cleared caches per render, and no writes during composition. |
| `RemoteCreationState.resetComponentIds()` | `vendor/…/capture/CommonCreationState.kt` | Without a reset, every render of a live composition shifts every component id. |

### Results (JVM, `./gradlew :prototypes:embedded-document:test`)

`EmbeddedDocumentTest.typedCaptureIsTheDecodedByteCapture` checks two things:

- `RcDocumentCodec.decode(byteCapture) == typedCapture`;
- `RcDocumentCodec.encode(typedCapture)` is **byte-identical** to `RemoteDocumentWriter`'s output.

The typed model is therefore an exact substitute for the byte writer, not an approximation.

`LiveSessionTest` builds one live session, applies 3×200 title edits, and asserts the live document
equals a fresh capture before and after the edits.

Timings are for a 37-operation, 394-byte screen. Each figure is the mean of 200 runs, from the last
of three interleaved rounds. They are ranges across four runs on a shared, noisy cloud container, so
read them for shape, not precision.

| Path | Per document |
| --- | --- |
| fresh capture → bytes | 0.9–3.0 ms |
| fresh capture → bytes → `RcDocument` | 0.7–1.9 ms |
| fresh capture → `RcDocument` (typed) | 0.7–1.0 ms |
| decode only | 10–30 µs |
| **live session: recompose after an edit** | 0.20–0.37 ms |
| **live session: render → `RcDocument`** | 0.08–0.18 ms |
| **live session: whole edit** | **0.28–0.55 ms** |

What the numbers say:

- **Skipping encode and decode is not where the time goes.** Decode is 1–3% of a capture.
- **The cost is composing from nothing on every edit.** `captureCommonRemoteDocument` builds a
  Recomposer, composes, waits for idle, renders and throws the whole composition away. That is
  exactly what the UI builder's Browser Preview does per edit today (`RemoteM3DevicePreview.kt`
  `captureDocument`).
- A live session cuts each edit to one recomposition plus one render: roughly 2–5× cheaper here,
  and the gap grows with screen size, because a fresh capture recomposes everything.

The typed writer earns its place on architecture, not speed. It removes a second encoder; see the
plan below.

### Two things the prototype found

1. **Component ids drift across renders.** `RemoteComposeNode.resolveModifier` allocates
   `renderedComponentId` from a per-state counter that is never reset, so every re-render of a
   retained tree shifts every id. Upstream's Android `captureRemoteDocument` flow has the same
   behaviour.

   Ids must be stable before anything can be keyed on them. Two consumers are affected:

   - editor bounds (authored node → component id);
   - player state retention: scroll, touch, animation and named state.

   Resetting the counter per render makes ids deterministic for a given tree shape. Inserting a node
   still renumbers every node after it. Editor nodes need **explicit ids derived from the authored
   node id** (`RemoteModifierData.componentId` already carries a requested id).
2. **Not every writer call goes through the IR.** `remote-creation-compose` makes 89 direct
   `creationState.writer.*` calls, for state-id allocation and `Declaration` closures. That is why
   the seam is the whole `RemoteWriter` interface rather than `WriterOp`. It is also why a session
   must start every render from a fresh writer.

## The Browser Preview on rc-players' live player

The plan below moved forward in yschimke/rc-players#672: creation lives there now. It is split into a
fork that keeps AndroidX's package and an rc-players layer: `RcHostBridge` and
`RcEmbeddedPlayer`. Build against an rc-players checkout to try it here:

```
./gradlew -PcomposeUiBuilderDir=../compose-ui-builder -PrcPlayersDir=../rc-players \
  :remote-catalog-ui-builder-renderer:jvmTest
```

`-PrcPlayersDir` makes two changes:

- It substitutes the `ee.schimke.remotecompose` port and the `rc-player-*` modules with rc-players'
  projects (`settings.gradle.kts`).
- It builds the renderer's Browser Preview from `src/embeddedPreview` instead of
  `src/capturePreview` (`remote-catalog-ui-builder-renderer/build.gradle.kts`).

The default build, and CI with it, keeps the capture path.

| Path | Each edit | `RemoteM3DevicePreview*` suites here |
| --- | --- | --- |
| `src/capturePreview` (default) | records a fresh document (`captureCommonRemoteDocument`) and plays it | 25 of 26 |
| `src/embeddedPreview` (`-PrcPlayersDir`) | recomposes the retained remote tree and plays its document (`RcEmbeddedPlayer`) | 25 of 26 |

The failure is the same on both: `RemoteM3DeviceFontSettingsUiTest` reads a `Roboto-Regular.ttf`
the compose-ui-builder checkout does not have. In about one run in six, the live path also timed out
`a settled pane holds the wall clock` under full-suite load; it passed every isolated re-run.

The vendored writer has the custom-component id bug fixed in rc-players: `startCustom`'s content
section reuses the layout's component id.

## Upstream (AndroidX) status, as of 2026-10-15

- **Published:** `androidx.compose.remote:*` `1.0.0-alpha21`, Wear `remote-material3` `1.0.0-alpha13`.
  This repository is on both. rc-players is still on `alpha19`/`alpha10`.
- **Snapshots:** the port's last refresh is snapshot `16547072` (2026-10-08). Latest is `16617687`,
  53 commits later under `compose/remote` and `wear/compose/remote`.
- **Commits to take before or alongside any move:**
  - `97010f58b` compare NaN-encoded ids in `RcFloat` equality (a follow-up to the NaN-id cache fix
    already taken);
  - `d3e5a08a0` / `9cda2fbc7` host action dispatch and APIs;
  - `7ce0408b0` default path winding;
  - `915075137` `animateEnterExit` id default;
  - `04130d542` / `a4e385e70` font variation matching;
  - `83b3b6737` / `7b5e7c3cc` decouple `RemoteDensity` and `RemoteDp` from Compose UI. These shrink
    the port's diff.
  - `ae4347854` explicit profile in context-free capture;
  - Material3: `d8f8d67ae` / `e4e805ec9` `dynamicRemoteColorScheme`, `956dc9429`
    `RemoteMotionScheme`, `a1ca8dbaa` / `0e2f7e7ea` curved progress timers.
- **There is no AndroidX "compose transform" library** on androidx-main or in any snapshot.
  Compose-to-remote happens in two other places:
  - **remote-creation-compose's own capture:** a custom `Applier` over a remote-only node tree,
    rendered after quiescence, plus a continuous `captureRemoteDocument` flow that keeps the
    composition alive and re-renders whole documents.
  - **Glance's `GlanceRemoteComposeTranslator`:** it walks a closed set of emittables after
    composition and degrades unknown ones to spacers with a warning.

  Both lessons apply here:

  - keep the applier-based model, which this prototype's session does;
  - make degradation explicit. `RcOperationWriter` throws so the caller can fall back to the byte
    path, rather than drawing something wrong.
- **Upstream has no in-memory document path.** Every upstream player parses a buffer
  (`CoreDocument.initFromBuffer`), and `reinflate()` was removed on 2026-10-13. Live updates exist
  only as variable updates. rc-players' typed `RcDocument` model is already ahead of upstream here.

## Plan

### Should creation move to rc-players? Yes, the portable layers, in stages

Reasons:

- The writer and player are already pinned as a compatibility pair
  (`docs/design/REMOTE_M3_UI_BUILDER.md`). Each needs the other's wire model.
- rc-players already owns a complete typed wire model and an encoder (`rc-player-protocol`:
  `RcModel`, `RcDocumentCodec.encode`, round-trip tested). `vendor/remote-write-core`'s 1,636-line
  `RemoteDocumentWriter` is a second, hand-kept encoder of the same format. The prototype proves the
  typed model plus `RcDocumentCodec.encode` reproduces it byte for byte.
- The embedded path only exists where both halves live together. The live session hands an
  `RcDocument` to `RcComposePlayer` without crossing a publication boundary.
- rc-players builds iOS and macOS. The vendored modules have no Apple targets yet, and moving them
  is the natural point to add them.

What moves and what stays:

| Today (`remote-m3-catalog`) | Proposed home | Notes |
| --- | --- | --- |
| `vendor/remote-write-core` | **replaced** by `rc-creation-writer` in rc-players | `RemoteWriter` plus the typed `RcDocumentWriter` (this prototype's `RcOperationWriter`, completed). Bytes are `RcDocumentCodec.encode(document)`. One encoder. |
| `vendor/remote-creation-compose` | `rc-creation-compose` in rc-players | Keeps the `androidx.*` packages so upstream refreshes stay a diff, published under `ee.schimke.composeai`. |
| `vendor/remote-foundation` | `rc-creation-foundation` in rc-players | 4 files; foundation has had no upstream commits since the port. |
| `vendor/remote-material3` | **stays** in remote-m3-catalog | It is the component catalog. It depends on the Wear CMP port, and its stand-ins and policy live here. |
| `vendor/remote-core` (legacy Java writer, JVM only) | **deleted** | The JVM capture switches to the common writer; only the Android lane still needs AndroidX. |

Gating in rc-players:

- Creation modules start `explicitApi()` with ABI dumps, like the player modules.
- They are marked experimental, so AndroidX-shaped API churn is reviewed, not blocked.
- The duplicate-FQN hazard (`RemotePath`, yschimke/wear-m3-catalog#652) still applies. The rule
  stays: never ship a class under a name the published `remote-creation` also ships.

### Order of work

1. **Refresh the port to snapshot 16617687** here, in remote-m3-catalog, as its own PR, using the
   same "apply the delta onto the port" method as the earlier refreshes. Moving and changing code in
   one step makes both unreviewable. Bump rc-players to `alpha21`/`alpha13` alongside it.
2. **Complete the typed writer** in rc-players `rc-creation-writer`. Cover every `RemoteWriter`
   method, not a subset. Rather than hand-porting 116 methods, port them **against the conformance
   corpus as the oracle**: capture each `remote-catalog` sticker and `rc-conformance` source both
   ways, then assert byte equality, as this prototype's test does.
3. **Move `remote-creation-compose` and `remote-foundation`** to rc-players, pointed at
   `rc-creation-writer`. Delete `RemoteDocumentWriter` and `vendor/remote-core`. remote-m3-catalog
   then consumes `ee.schimke.composeai:rc-creation-*` from the rc-players BOM, so the writer/player
   pair becomes a single version.
4. **Live session API** (`RemoteDocumentSession`, from this prototype) with stable component ids:
   - reset the counter per render;
   - honour explicit ids set from the authored node.
5. **Player: keep state across documents.** `RcComposePlayer` keys everything on
   `remember(document)`, so each new document rebuilds `RcPlayerState` and resets scroll,
   animation and touch state.
   - Add a state key, or an `RcPlayerState` the host owns, that survives a structural edit.
   - Re-link and re-layout per document, but carry variables and per-component state over by
     component id. That is why step 4's ids must be stable.
6. **UI builder:** replace `RemoteM3DevicePreview`'s per-edit `captureCommonRemoteDocument` with a
   session held in the preview composition. The first measurable win lands here.
7. **Only then consider the Visual Editor.** The stand-ins exist mostly because the editor needs
   things the real document does not give it today, not because real capture is too slow. It needs
   per-node bounds, an `AUTHORING_UNROLLED` layout (lazy lists drawn whole, every slot reachable,
   empty slots placeable), and text layouts (`UiBuilderInspectionSnapshot`). The real path needs:
   - authored node → explicit component id (step 4);
   - player inspection reporting bounds per component id (`rc-player-compose` has an inspection
     hook);
   - an unrolled authoring mode in creation: a composition local that remote lazy and pager
     components honour by emitting every item.

   Each remote-m3 component whose `canvasMapping` stand-in can be dropped is then a policy change
   (`RemoteM3Policy`), not a renderer change. The "surfaces must not be collapsed" rule in
   `REMOTE_M3_UI_BUILDER.md` would need revisiting at that point, deliberately.

### Building the player's types as part of the composition

The composition already builds a retained tree (`RemoteRootNode`) through its own applier. The
player's types are a function of that tree. A host can therefore compose the remote content inside
its own UI, with no capture coroutine and no second recomposer:

```kotlin
@Composable
fun EmbeddedRemote(display: RemoteCreationDisplayInfo, content: @Composable @RemoteComposable () -> Unit) {
  val parent = rememberCompositionContext()
  val session = remember { RemoteDocumentSession(display, parent) }
  DisposableEffect(session) { onDispose { session.dispose() } }
  session.setContent(content)               // child composition; recomposes with the host
  val document by remember(session) {       // re-render only when the applier changed
    derivedStateOf { session.changeCount; session.render(RcDocumentWriter(display)).document() }
  }
  RcComposePlayer(document, stateKey = session)  // step 5: state survives edits
}
```

The open design point is when `render` runs. It must be after the child composition's changes are
applied and outside composition. In a real host that means a `SideEffect` or a `snapshotFlow` on the
applier's change count, rather than the `derivedStateOf` sketched above. Writing during composition
is exactly what upstream's capture loop forbids. The prototype's `LiveSessionTest` drives this by
hand: settle the recomposer, then render.
