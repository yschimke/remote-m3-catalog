// `:uid-stickers` — the SHADOW catalog: Remote stickers whose body is a UI builder `.uid` document.
//
// Opt-in (`-PuidStickers=true`), outside the published inventory. `stickers/<name>.uid` is a
// sticker body (one component definition placed once) and its sibling `stickers/<name>.json` lists
// its cells (builder operations applied before export) and `pairs` (shell → hand-written sticker).
// `generateUidStickers` exports every document and cell through the UI builder's own Remote
// exporter (`InlineRemoteContentExporter.exportComponents`) into `build/generated/uid/kotlin`. With
// the same flag, `:remote-catalog` compiles that output and the shells in `shells/kotlin` beside
// the
// hand-written stickers, so one render draws both and `compare-local.py` pairs them capture for
// capture. The same method as m3-catalog's and wear-m3-catalog's `uid-stickers`.
plugins {
  // By id: AGP already put KGP on the buildscript classpath.
  id("org.jetbrains.kotlin.jvm")
}

kotlin { jvmToolchain(21) }

// The released exporter: `InlineRemoteContentExporter.exportComponents` since compose-ui-builder
// 3.111. `-PuidExportVersion=<x>-SNAPSHOT` tries an unreleased one from Maven local.
val exportVersion = providers.gradleProperty("uidExportVersion")

repositories {
  if (exportVersion.isPresent) mavenLocal()
  mavenCentral()
  google()
}

val generator by sourceSets.creating

dependencies {
  // `composePreviewServer` in gradle/libs.versions.toml, which Renovate owns.
  "generatorImplementation"(libs.composeai.ui.builder.export) {
    exportVersion.orNull?.let { version { strictly(it) } }
  }
}

val generatedDir = layout.buildDirectory.dir("generated/uid/kotlin")

val generateUidStickers by
  tasks.registering(JavaExec::class) {
    group = "build"
    description = "Exports every sticker document and variant cell to Kotlin with the UI builder."
    classpath = generator.runtimeClasspath
    mainClass = "ee.schimke.wearm3catalog.remote.uid.GenerateUidStickersKt"
    inputs.dir("stickers")
    outputs.dir(generatedDir)
    args(layout.projectDirectory.asFile.absolutePath, generatedDir.get().asFile.absolutePath)
  }
