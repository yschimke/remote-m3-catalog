// A local, interactive demo of the `cranky-bandit` UI builder design (a calculator in the Glance
// Wear large widget container), with real calculator logic behind it. See README.md.
//
// Opt-in from settings.gradle.kts (-PcalculatorDemo=true): nothing in the catalog, its records or
// CI reads it.
plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.compose.multiplatform)
  id("org.jetbrains.kotlin.plugin.compose")
}

// The same Remote Compose stack the remote-m3 renderer plays a design through: rc-players'
// embedded player, which brings its creation fork (AndroidX's creation and foundation layers in
// their own artifacts), with this repository's published `remote-material3` port linked against
// that fork. The port's own copies of the lower layers must not come along: a second copy of
// every class.
configurations.configureEach {
  exclude(group = "ee.schimke.remotecompose", module = "remote-write-core")
  exclude(group = "ee.schimke.remotecompose", module = "remote-creation-compose")
  exclude(group = "ee.schimke.remotecompose", module = "remote-foundation")
}

dependencies {
  implementation(platform(libs.composeai.rc.players.bom))
  implementation(libs.composeai.rc.player.embedded)
  implementation(libs.composeai.rc.player.compose)
  implementation(libs.remotecompose.material3)
  implementation(compose.desktop.currentOs)
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.11.0")

  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
  @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class) testImplementation(compose.uiTest)
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    optIn.addAll(
      "ee.schimke.composeai.rccreation.bridge.ExperimentalRcCreationApi",
      "ee.schimke.composeai.rcplayer.compose.ExperimentalRcPlayerApi",
    )
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

compose.desktop { application { mainClass = "ee.schimke.remote.calculator.MainKt" } }
