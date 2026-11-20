// `:samples-catalog` — the AndroidX Wear Remote Compose Material 3 samples, rendered.
//
// Everything under `src/main/kotlin/upstream/` is VENDORED: upstream's bytes, in upstream's
// package (`androidx.wear.compose.remote.material3.samples` and `previews.utils`), fetched by
// `scripts/import-samples.mjs` from the commit pinned in `samples/import.json`. Never edited in
// place and never formatted — a fix is a patch in `samples/patches/` with a stated reason.
//
// Upstream's samples already carry `@WearPreviewDevices` and
// `@PreviewWrapper(RemoteComponentPreviewWrapper::class)`, so discovery finds them directly.
@file:Suppress("RestrictedApiAndroidX")

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.composePreview)
}

composePreview {
  // Pin Robolectric to SDK 35; same pin `:remote-catalog` carries.
  sdkVersion.set(35)
}

android {
  // Upstream's own package so unqualified `R.drawable.*` in vendored samples resolves without
  // patching every file.
  namespace = "androidx.wear.compose.remote.material3.samples"
  compileSdk = 37

  defaultConfig {
    applicationId = "ee.schimke.remotem3catalog.samples"
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

  lint { disable += "RestrictedApi" }
}

dependencies {
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.emoji2)
  implementation(libs.compose.ui.tooling.preview.prerelease)
  implementation(libs.compose.material.icons.core.prerelease)
  implementation(libs.compose.material.icons.extended.prerelease)
  implementation(libs.compose.remote.tooling.preview)
  implementation(libs.compose.remote.creation)
  implementation(libs.compose.remote.creation.compose)
  implementation(libs.wear.compose.remote.material3)
  implementation(libs.wear.compose.material3)
  implementation(libs.wear.compose.foundation)
  implementation(libs.wear.compose.ui.tooling)
  implementation(libs.wear.tooling.preview)
  implementation(libs.composeai.preview.annotations)
  implementation(platform(libs.composeai.rc.players.bom))
  implementation(libs.composeai.rc.embedded.player)

  debugImplementation(libs.compose.ui.tooling.prerelease)
}

// Vendored sources are upstream's bytes — exclude from ktfmt.
tasks.withType<com.ncorti.ktfmt.gradle.tasks.KtfmtBaseTask>().configureEach {
  exclude { it.file.absolutePath.replace('\\', '/').contains("/src/main/kotlin/upstream/") }
}
