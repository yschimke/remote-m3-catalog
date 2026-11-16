plugins {
  id("org.jetbrains.kotlin.jvm")
  application
  id("org.jetbrains.kotlin.plugin.compose")
}

application { mainClass.set("ee.schimke.remote.desktop.MainKt") }

dependencies {
  // rc-players' Compose Multiplatform creation fork: AndroidX's creation DSL in rc-players'
  // package,
  // over its own common writer, so a desktop JVM captures a document with no Android on the path.
  implementation(platform(libs.composeai.rc.players.bom))
  implementation(libs.composeai.rc.creation.compose)
  implementation(libs.composeai.rc.creation.writer)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(kotlin("test"))
  testImplementation(libs.junit)
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
