// `:widget-catalog` — the `remote-widgets` system: Mobile Launcher Widgets in Remote Compose.
//
// A design system of its own, not a mode of `:remote-catalog`'s `remote-m3`. Both record real
// `RemoteDocument`s through the same vendored Remote Compose port and rasterise them with the same
// player, and that is everything they share. What separates them:
//
// - **No `remote-material3`.** `remote-m3` is the Wear OS kit drawn with Wear's Remote Material 3.
//   A phone launcher widget has no Material component library on the Remote side, so this sheet
//   is drawn from `remote-creation-compose` and `remote-foundation` alone, and
//   `verifyNoRemoteMaterial3` below fails the build if the library reaches this module's compile
//   classpath by any route.
// - **No Glance Wear.** A launcher widget is an `AppWidgetProvider` — upstream's
//   `RemoteComposeWidget` — not a `GlanceWearWidget`, and the launcher, not a watch host, frames it.
// - **Frames are launcher grid cells**, `@Widget3x2` and friends in `WidgetSizes.kt`, not device
//   sizes or Wear container sizes.
// - **No kit.** There is no Figma kit for launcher widgets, so this sheet is outside design-parity
//   and the kit-coverage records; every component states `noReference`.
//
// It rides `:remote-catalog`'s snapshot lane only in the sense that both depend on the vendored
// port; this module adds no lane of its own and stays on the released alpha line.
@file:Suppress("RestrictedApiAndroidX")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.composePreview)
}

composePreview {
  // Same Robolectric pin as `:remote-catalog`: compileSdk 37 here, Robolectric ships up to 36.
  sdkVersion.set(35)
}

android {
  namespace = "ee.schimke.remotewidgets"
  // The Remote Compose alpha AARs declare minCompileSdk 37.
  compileSdk = 37

  defaultConfig {
    applicationId = "ee.schimke.remotewidgets"
    // The Remote Compose alpha artifacts require API 29+.
    minSdk = 29
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures { compose = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  // The Remote Compose APIs — `RemoteComposeWidget` included — are `@RestrictTo(LIBRARY_GROUP)`;
  // see `remote-catalog/build.gradle.kts`.
  lint { disable += "RestrictedApi" }

  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// The vendored port answers for the published Remote coordinates, exactly as in
// `:remote-catalog`, so the two sheets record with the same writer. `remote-material3` is
// deliberately absent from this list: nothing here may ask for it.
configurations.configureEach {
  resolutionStrategy.dependencySubstitution {
    substitute(module("androidx.compose.remote:remote-creation-compose"))
      .using(project(":vendor:remote-creation-compose"))
    substitute(module("androidx.compose.remote:foundation"))
      .using(project(":vendor:remote-foundation"))
  }
  exclude(group = "org.jetbrains.skiko")
}

dependencies {
  implementation(libs.androidx.emoji2)
  // NO Compose BOM, for the reason `:remote-catalog` takes none: the alpha Remote line aligns with
  // the prerelease Compose UI pinned here.
  implementation(libs.compose.ui.tooling.preview.prerelease)
  implementation(libs.compose.remote.tooling.preview)
  implementation(libs.compose.remote.creation)
  // The whole component vocabulary of this sheet: the creation DSL and the foundation.
  implementation(project(":vendor:remote-creation-compose"))
  implementation(project(":vendor:remote-foundation"))

  implementation(libs.activity.compose)
  implementation(libs.composeai.preview.annotations)
  // `RemoteOverridablePreview` — the sticker frame, which lands each document in the render's
  // `.rc` sidecar (see `WidgetSticker`).
  implementation(libs.composeai.remotecompose.connector)
  // The embedded player the connector replays through; see `remote-catalog/build.gradle.kts` for
  // why it is the vendored coordinate and why a missing one fails silently.
  implementation(libs.composeai.rc.embedded.player)

  debugImplementation(libs.compose.ui.tooling.prerelease)

  testImplementation(libs.junit)
  testImplementation(libs.truth)
  testImplementation(libs.robolectric)
  testImplementation(libs.kotlinx.serialization.json)
}

/**
 * The one rule that makes this a different design system from `remote-m3`, held where a refactor
 * cannot quietly break it: Wear's Remote Material 3 must not be on the compile classpath. A
 * transitive that brought it would let a sticker call `RemoteButton` and still compile.
 */
abstract class VerifyNoRemoteMaterial3 : DefaultTask() {
  @get:Input abstract val coordinates: ListProperty<String>

  @TaskAction
  fun verify() {
    val offending = coordinates.get().filter { "remote-material3" in it }
    check(offending.isEmpty()) {
      "remote-widgets must not depend on Wear's Remote Material 3, but its compile classpath " +
        "resolves $offending"
    }
  }
}

val verifyNoRemoteMaterial3 by
  tasks.registering(VerifyNoRemoteMaterial3::class) {
    group = "verification"
    coordinates.set(
      configurations.named("debugCompileClasspath").flatMap { classpath ->
        classpath.incoming.resolutionResult.rootComponent.map { root ->
          val seen = mutableSetOf<String>()
          fun walk(component: org.gradle.api.artifacts.result.ResolvedComponentResult) {
            if (!seen.add(component.id.displayName)) return
            component.dependencies
              .filterIsInstance<org.gradle.api.artifacts.result.ResolvedDependencyResult>()
              .forEach { walk(it.selected) }
          }
          walk(root)
          seen.sorted()
        }
      }
    )
  }

tasks.named("check") { dependsOn(verifyNoRemoteMaterial3) }
