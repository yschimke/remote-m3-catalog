// PROTOTYPE — not for landing. Captures a Remote Compose composition straight into the rc-players
// wire model (`RcDocument`) through the vendored `RemoteWriter` seam, and checks it against the
// byte encoder. See README.md.
plugins {
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
  implementation(project(":vendor:remote-creation-compose"))
  implementation(project(":vendor:remote-write-core"))
  implementation(project(":vendor:remote-foundation"))
  implementation(platform(libs.composeai.rc.players.bom))
  implementation("ee.schimke.composeai:rc-player-protocol")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

configurations.configureEach {
  resolutionStrategy.dependencySubstitution {
    substitute(module("androidx.compose.remote:remote-core")).using(project(":vendor:remote-core"))
    substitute(module("androidx.compose.remote:remote-creation-core"))
      .using(project(":vendor:remote-core"))
    substitute(module("androidx.compose.remote:remote-creation"))
      .using(project(":vendor:remote-core"))
  }
}

tasks.test { testLogging { showStandardStreams = true } }
