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
16427341 and 16547072, in two refreshes (16427341 → 16480970, then 16480970 → 16547072), each
applied onto the port rather than re-copied.

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
`4307936-ps17-cmp09`. The version is derived from `remote-compose-upstream.json`; bump its
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
  implementation("ee.schimke.remotecompose:remote-material3:4307936-ps17-cmp09")
}
```

`./gradlew publishToMavenLocal` publishes locally. `./gradlew publishRemoteComposeToBuildDir`
produces the exact repository tree CI pushes under `build/remote-compose-maven`. On an empty output
repository, dispatch the publish workflow before any render workflow: the UI-builder renderer
resolves the port from that branch.

The vendored source is Apache 2.0 licensed; each source file retains its Android Open Source Project
header.
