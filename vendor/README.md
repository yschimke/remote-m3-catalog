# Vendored Remote Compose

These modules are copied from AndroidX change
[4307936](https://android-review.googlesource.com/c/platform/frameworks/support/+/4307936), patch set
17, commit `01398020c7ec20f51e51bfc2b65b9b425e338be9`:

| Local module | AndroidX source |
| --- | --- |
| `:vendor:remote-creation-compose` | `compose/remote/remote-creation-compose` |
| `:vendor:remote-foundation` | `compose/remote/foundation/foundation` |
| `:vendor:remote-material3` | `wear/compose/remote/remote-material3` |
| `:vendor:remote-core` | write-side sources from `remote-core`, `remote-creation-core`, and `remote-creation` |
| `:vendor:remote-write-core` | common write-only encoder extracted from those core sources |

## Later upstream changes

Since that patch set, the modules have taken the upstream delta between androidx.dev snapshot builds
16427341 and 16639771, in three refreshes (16427341 → 16480970, 16480970 → 16547072, then
16547072 → 16639771), each applied onto the port rather than re-copied.

16427341 → 16480970:

- `remote-material3`: the selection controls animate their progress, and `AnimateTick.kt` is new.
  Upstream's removal of `RemotePageIndicator.inverseLerp`'s divide-by-zero guard is not taken.
- `remote-creation-compose`: custom-function enter/exit transitions
  (`RemoteEnterTransition.Custom` / `RemoteExitTransition.Custom`) and enter/exit sequencing
  (`RemoteAnimationSequence`), the "h:mm" 12-hour `RemoteTimeDefaults`, the `RemoteVector` group-matrix
  removal, and the Android capture loop's single-threaded recomposer and snapshot-write monitor.
  Upstream's move of canvas recording from `RecordingCanvas` into `RemoteCanvas` and its
  `CanvasOperationBuffer` is not mirrored: the port records through its own `RemoteDocumentProgram`,
  and the buffer's optimisations are off by default upstream.
- `remote-core`: the matching write-side changes, including `AnimationSpec`'s packed
  animation/sequence ints and function ids, and optional document compression (`Header.COMPRESS`).

16480970 → 16547072:

- `remote-material3`: `RemoteSlider` animates its value and clamps the segmented bar to the track;
  the split checkbox/radio/switch buttons take their outer corners from the container shape
  (`splitSectionShapes`); `RemoteRoundButton` takes a `role`; `RemoteTimeText` passes font feature
  settings through; the indeterminate `RemoteCircularProgressIndicator` reads
  `RemoteTimeVariables.continuousSeconds`; `RemoteCurvedProgressIndicator`'s intro slide starts from
  the collapse-freeze fraction. The new components (`RemoteIconToggleButton`,
  `RemoteTextToggleButton`, `RemoteToggleButtonShape`, `RemoteSegmentedCircularProgressIndicator`,
  `RemoteOneHandedGestureClickIndicator`, `RemoteMotionTokens`) are not taken: nothing in the port
  needs them, and `RemoteToggleButtonShape` reads Android profiles.
- `remote-creation-compose`: `RemoteTimeVariables`, `RemoteConfiguration` and the NaN-id-aware
  expression cache (`NanIdEquality`, `LoweredFloatExpressionKey` / `LoweredIntExpressionKey`), which
  fixes expressions that differ only in a variable id or operator sharing one cached id; font feature
  settings carried on `RemotePaint` rather than folded into `RemoteTextStyle`
  (`combinedFontVariationSettings` is gone); `RemoteCustomComponent` recorded as a layout component
  instead of inside a canvas; the "h:mm" time string built from the integer hour/minute variables;
  and the Android capture loop's write tracker, frame budget, render-invalidation tracker and
  `CaptureUpdateThrottle`. `RemoteComposeApplier.changeCount` uses `kotlin.concurrent.atomics` so it
  stays in `commonMain`, and the JVM capture loop is unchanged. Upstream's further move of
  `RecordingCanvas` onto `RemoteCanvas`, and its split of `RemoteComposeCreationState` /
  `RemoteImageVector` into Android files, are not mirrored, as before: the port already has its own
  platform split.
- `remote-core`: `Operations.EVENT_ACTION` moves from 110 to 100, `MODIFIER_ALIGN_BY` leaves the
  experimental v7 profiles, `RemoteComposeWriter` gains the path-id `drawBitmapFontTextRunOnPath`
  overload (the DSL's `drawTextOnPath` previously passed the id as path data), and the DSL gains
  `bitmapFontGlyph`. The antialiased 2D mesh API (`addMesh2DAntialias`, `remoteMesh2DAntialias`,
  `AddMesh2D` / `Mesh2DGenerator` changes) is not taken, because the port never carried
  `AddMesh2D`; nor are the JSON importer and the Android `RcPlatformProfiles` annotation.

16547072 → 16639771:

- `remote-material3`, colour: `RemoteColorScheme` resolves each role through `RemoteColorRole`, and
  the new `dynamicRemoteColorScheme()` follows the player's Android system (dynamic) colours, falling
  back to the named `"WearM3.*"` scheme on profiles without themed colours (Wear widgets); the default
  scheme stays named. Button icons take `iconColor` (now `primary`, was `onSurface`) through
  `provideScopeContent`; the checked checkbox mark is `primaryContainer` (was `onPrimary`) and its
  disabled mark derives from `background`; the outlined card border is `outline`; the page
  indicator's selected/unselected dots are `onBackground` and `onBackground` at 0.3 alpha (were
  `onSurface` / `onSurfaceVariant`). Upstream stores `android.R.color` ids in `RemoteColorRole`; the
  port carries a common `SystemColorToken` and maps it to the resource id in the Android actual of
  `themedRemoteColor`, while the JVM and Wasm actuals return the named colour, since only an Android
  profile reports themed-colour support.
- `remote-material3`, other: `RemoteMotionScheme` (`RemoteMaterialTheme.motionScheme`), which the
  selection controls now take their spring from, and the slider's tween from `RemoteMotionTokens`,
  which is taken now that something needs it; the timer overload of `RemoteCurvedProgressIndicator` becomes
  `RemoteIntroOutroCurvedProgressIndicator`, with the intro/outro springs played over the plain
  progress geometry instead of a separate timer arc; `RemoteTypography` takes a default font family;
  `RemoteEdgeButton`'s ellipsis/ellipse rename. `RemoteIconToggleButton`, `RemoteTextToggleButton`,
  `RemoteToggleButtonShape`, `RemoteSegmentedCircularProgressIndicator` and
  `RemoteOneHandedGestureClickIndicator` changed upstream but are still not carried, as before.
- `remote-creation-compose`: `RemoteProfileCapabilities` (`currentProfileSupportsThemedColors`,
  `currentProfileSupportsTouchModifiers`), read from the port's `supportedOperations` with the
  operation ids spelled out, since commonMain cannot read the Java `Operations`; `hostAction(id,
  metadata)` / `hostAction(name, RemoteBoolean)`; `pendingIntentAction` extras, written as an id
  action (`FIRST_ACTION_ID` + index) for launcher widgets and for extras outside Wear widgets, plus
  `PendingIntentActionHandler`, which the snapshot embedded `RcPlayer` links against this module;
  `RCWidget` registering those id actions; `RemoteCustomComponent` content children and image-bitmap
  properties; `animateEnterExit` no longer writing animation id 0, which the player reads as
  "disabled"; the `RemoteBoolean.select` peepholes; and `RemoteTextStyle.fromTextStyle` picking
  the variation settings of the font that best matches the style's weight and style. Upstream's
  next step of decoupling the API from Compose UI types is not mirrored: `RemoteDp` becoming a
  `BaseRemoteState<Float>`, `graphicsLayer` taking a `RemoteShape` (which also replaces the
  hardcoded 40px rounded-layer radius), the `RemoteBlendMode` / `RemoteTileMode` / `RemoteFontWeight`
  / `RemoteFontStyle` / `RemoteContentScale` value types with the Compose-typed overloads moved to
  `*Android.kt` files, the remaining move of `RecordingCanvas` onto `RemoteCanvas`, and the
  promotion of the cache-key classes to restricted public API. That is an API break for every
  caller here, and the blend-mode and tile-mode values it encodes are the same `PaintBundle`
  constants, so documents do not change. Nor is the capture loop's rework (a Context-free
  `captureRemoteDocumentImpl` with `HeadlessFrameClock`, saved-state and retained-values plumbing,
  and a single capture that throws on content that never settles): the port keeps the 16547072 loop,
  since the new failure mode would change how animated catalog stickers capture and needs its own
  change.
- `remote-core`: the text-align fix in `CoreText` / `TextLayout` (complex text layout gets the
  alignment masked to its low 16 bits, so flags packed above it no longer break wrapped alignment),
  `PathData.markPathChanged`, the NaN-id-aware `RemoteFloatEquality` used by the DSL's `RcFloat`
  equality, and the read-side getters and widened visibility the embedded player now uses instead of
  reflection. The JSON importer change and the Android `RcPlatformProfiles` rewrite are not taken,
  as before.

The copied Kotlin sources started as upstream bytes; the port then moved the portable Creation,
Foundation, and Material 3 closure to `commonMain`. Local build files expose Android, JVM, and Wasm
targets. Narrow actuals retain Android/JVM time, display, image, and legacy-writer integration.

The port's platform-neutral path is `androidx.compose.remote.creation.compose.path.RemotePath`, not
upstream's `androidx.compose.remote.creation.RemotePath`. The Android graph still carries the
published `remote-creation`, which ships a different class under that name, and a duplicate FQN on
one classpath makes whichever jar loads first serve both. In the cmp06 bundle that was the port's,
so 35 of `remote-creation-android`'s own references failed to link
([yschimke/wear-m3-catalog#652](https://github.com/yschimke/wear-m3-catalog/issues/652)).

`:remote-desktop` is the phase-2 client. Its `run` task performs a real Compose recomposition through
the vendored JVM applier and writes the encoded document to
`remote-desktop/build/desktop-sample.rc`; it does not use Robolectric or any Android API.
`DesktopCaptureTest` repeats the capture and checks that its bytes are non-empty and deterministic.

## Port fixes

- `remote-write-core`'s `RemoteDocumentWriter.startCustom` gave a custom layout's content section
  the layout's own component id. Upstream's `RemoteComposeWriter.startCustom` allocates it a fresh
  one (`addContentStart`), and a player that indexes components by id (rc-players') rejected the
  duplicate. `ProtocolParityTest.customLayoutMatchesAndroidxCore` had accepted the duplicate: its
  AndroidX buffer allocated the content id from a separate counter, whose first id happened to match
  the layout's.

## Write-only core extraction

`:vendor:remote-core` begins phase 3 by merging the JVM writer stack that AndroidX publishes as
`remote-core`, `remote-creation-core`, and `remote-creation`. The JSON-to-document importer has been
removed. The Desktop graph substitutes all three published coordinates with this one project; the
Android graph temporarily retains the published `remote-creation` platform variant for its Android
bitmap and path adapters.

`remote-core`, `remote-creation-core`, and `remote-creation` remain Maven dependencies: their
AndroidX artifacts already publish standard JVM variants. On JVM, Remote Material 3 uses this
Wear Compose CMP port (`ee.schimke.wearcmp`) for the Wear token types it references. That port is
published by yschimke/wear-m3-catalog and consumed here from the `wear-compose-cmp-maven` branch of
`yschimke/wear-m3-catalog-out`. Android configurations substitute it back to the real AndroidX Wear
Compose artifacts.

## Published artifacts

The five vendored modules publish under `ee.schimke.remotecompose` at
`4307936-ps17-cmp10`. The version is derived from `remote-compose-upstream.json`; bump its
`portRevision` whenever published bytes change without moving to a newer AndroidX patch set.

[`publish-remote-compose.yml`](../.github/workflows/publish-remote-compose.yml) publishes to GitHub
Packages (`https://maven.pkg.github.com/yschimke/remote-m3-catalog`, which needs a token) and to a
credential-free Maven tree on the `remote-compose-cmp-maven` branch of the output repository,
`yschimke/remote-m3-catalog-out`. Before this repository was split out of yschimke/wear-m3-catalog
the branch lived on `yschimke/wear-m3-catalog-out`, and that tree is no longer updated.

```kotlin
repositories {
  maven("https://raw.githubusercontent.com/yschimke/remote-m3-catalog-out/remote-compose-cmp-maven/")
}

dependencies {
  implementation("ee.schimke.remotecompose:remote-material3:4307936-ps17-cmp10")
}
```

`./gradlew publishToMavenLocal` publishes locally. `./gradlew publishRemoteComposeToBuildDir`
produces the exact repository tree CI pushes under `build/remote-compose-maven`. On an empty output
repository, dispatch the publish workflow before any render workflow: the UI-builder renderer
resolves the port from that branch.

The vendored source is Apache 2.0 licensed; each source file retains its Android Open Source Project
header.
