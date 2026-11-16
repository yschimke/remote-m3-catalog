plugins {
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.compose.multiplatform)
  id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    outputModuleName.set("remoteComposeWasm")
    binaries.executable()
  }

  sourceSets {
    commonMain.dependencies {
      // rc-players' Compose Multiplatform creation fork and its Wear Remote Material 3, which
      // encode a document entirely in Wasm.
      implementation(project.dependencies.platform(libs.composeai.rc.players.bom))
      implementation(libs.composeai.rc.creation.compose)
      implementation(libs.composeai.rc.creation.wear.material3)
      @Suppress("DEPRECATION") implementation(compose.runtime)
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    }
  }
}
