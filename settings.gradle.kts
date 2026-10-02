pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()

    // ── The CMP Wear port, GROUP-FENCED ───────────────────────────────────────────────────────
    // `ee.schimke.wearcmp:*` — Wear Compose Material 3 / Foundation compiled for Compose
    // Multiplatform, published to the `wear-compose-cmp-maven` branch of
    // `yschimke/wear-m3-catalog-out` by the port lane of yschimke/wear-m3-catalog. Only the
    // UI-builder adapters and the browser renderer link it (the Wear frame and Material canvas
    // stand-ins the Remote renderer draws around a document); `:remote-catalog` never does.
    //
    // Fenced to the one group it can legitimately answer for: `ee.schimke.wearcmp` is a coordinate
    // namespace nothing else in this build or on Maven Central uses, so this repository can never
    // satisfy a request for an `androidx.*` or `ee.schimke.composeai` artifact even by accident.
    maven(
      "https://raw.githubusercontent.com/yschimke/wear-m3-catalog-out/wear-compose-cmp-maven/"
    ) {
      name = "wearComposeCmpPort"
      content { includeGroup("ee.schimke.wearcmp") }
    }

    // The published CMP Remote Compose writer used by the remote-m3 UI Builder Browser Preview —
    // this repository's own `vendor/` port, published by `publish-remote-compose.yml` to the
    // `remote-compose-cmp-maven` branch of the output repository `yschimke/remote-m3-catalog-out`.
    // Kept separate from the Wear port above because the two release independently. The exact
    // coordinate is pinned in libs.versions.toml and embedded in the renderer manifest; this
    // repository is fenced to its own namespace so it cannot answer any AndroidX dependency.
    maven(
      "https://raw.githubusercontent.com/yschimke/remote-m3-catalog-out/remote-compose-cmp-maven/"
    ) {
      name = "remoteComposeCmpPort"
      content { includeGroup("ee.schimke.remotecompose") }
    }

    // ── The androidx.dev snapshot lane, PINNED IN-TREE and GROUP-FENCED ───────────────────────
    // Selected by `.github/ci/remote-snapshot-pin` — one line, an androidx.dev build id or
    // `latest` — with `-PremoteSnapshot=<id>` as a per-invocation override and an empty or absent
    // pin file meaning the released line.
    //
    // IT READS THE FILE rather than requiring the property because a Gradle property is not
    // reachable from where it has to be. `design-artifacts.yml` renders this catalog through a
    // reusable workflow it cannot pass arguments to, and the one hook it does have —
    // `design-map-command` — is documented as running "before every step that READS the map",
    // which is after the render, not before it. Appending `remoteSnapshot=` to `gradle.properties`
    // there therefore reached the design map and not the stickers: run #164 rendered 392 previews
    // on the released lane at 20:00:40 and projected a 419-preview map at 20:07:49, so the eleven
    // snapshot-only cells were declared with no sticker behind them and every job stayed green.
    // A file the checkout already carries is reachable from every invocation, including that one.
    //
    // The `content` filter is what makes the lane SAFE rather than merely off by default. A
    // settings-level repository is visible to every project, so scoping matters twice over: this
    // one can only ever serve the Remote groups, so nothing outside the Remote Compose line can
    // resolve from a snapshot by accident.
    //
    // The version substitution that actually selects `1.0.0-SNAPSHOT` is deliberately NOT here —
    // it lives in `remote-catalog/build.gradle.kts` as a resolution strategy on that module's own
    // configurations, which is a second, independent fence: even if a coordinate did become
    // shared, a module that does not opt in would keep resolving the pinned alpha.
    // A PRESENT property wins outright, blank included — `-PremoteSnapshot=` is how you force the
    // released lane for one invocation now that the pin file is on by default, and it can only mean
    // that if a blank property is distinguished from an absent one before the file is consulted.
    val remoteSnapshotProperty = providers.gradleProperty("remoteSnapshot").orNull
    val remoteSnapshot =
      if (remoteSnapshotProperty != null) {
        remoteSnapshotProperty.takeIf { it.isNotBlank() }
      } else {
        rootDir
          .resolve(".github/ci/remote-snapshot-pin")
          .takeIf { it.isFile }
          ?.readText()
          ?.trim()
          ?.takeIf { it.isNotBlank() }
      }
    if (remoteSnapshot != null) {
      val path = if (remoteSnapshot == "latest") "latest" else "builds/$remoteSnapshot"
      maven("https://androidx.dev/snapshots/$path/artifacts/repository") {
        content {
          includeGroupByRegex("androidx\\.compose\\.remote.*")
          includeGroupByRegex("androidx\\.wear\\.compose\\.remote.*")
          includeGroupByRegex("androidx\\.glance\\.wear.*")
        }
      }
    }
  }
}

rootProject.name = "remote-m3-catalog"

// Renderer builds consume the renderer SDK from source. Opt in explicitly so ordinary
// catalog builds keep resolving exactly as before:
//
//   ./gradlew :remote-catalog-ui-builder-renderer:rendererArchive \
//     -PcomposeUiBuilderDir=../compose-ui-builder
//
// The synthetic coordinate is intentionally absent from Maven, so asking for a renderer without
// this checkout fails closed rather than silently compiling against a different release.
providers.gradleProperty("composeUiBuilderDir").orNull?.let { path ->
  val directory = file(path).canonicalFile
  require(directory.resolve("settings.gradle.kts").isFile) {
    "-PcomposeUiBuilderDir names $directory, which is not a compose-ui-builder Gradle checkout."
  }
  logger.lifecycle("Composite build: uiBuilder -> $directory")
  includeBuild(directory) {
    dependencySubstitution {
      substitute(module("ee.schimke.composeai:ui-builder-renderer-sdk-source"))
        .using(project(":ui-builder-renderer-sdk"))
    }
  }
}

// The renderer links the source-only SDK. Keep it outside ordinary catalog builds: without the
// composite there is deliberately no Maven fallback for that coordinate.
if (providers.gradleProperty("composeUiBuilderDir").isPresent) {
  include(":remote-catalog-ui-builder-renderer")
  include(":ui-builder-foundation-adapters")
  include(":ui-builder-material-adapters")
  include(":ui-builder-wear-adapters")
}

// The Remote Compose rendition of the M3 Wear OS Apps Design Kit — the `remote-m3` system. Its
// Wear Compose sibling, the `wear-m3-catalog` system, lives in yschimke/wear-m3-catalog; the two
// pair component by component through `parallel` (scripts/parallel-map.sh). See
// remote-catalog/build.gradle.kts for why it is on the alpha line with no Compose BOM.
include(":remote-catalog")

// Source vendoring of the three Remote Compose layers being moved to CMP JVM by AndroidX CL
// 4307936. The upstream sources stay byte-for-byte under vendor/; only their standalone Gradle
// wiring lives here. Phase 3 also merges the JVM write path from `remote-core`,
// `remote-creation-core`, and `remote-creation` into one local module.
include(":vendor:remote-creation-compose")

include(":vendor:remote-foundation")

include(":vendor:remote-material3")

include(":vendor:remote-core")

include(":vendor:remote-write-core")

include(":remote-wasm")

include(":remote-desktop")
