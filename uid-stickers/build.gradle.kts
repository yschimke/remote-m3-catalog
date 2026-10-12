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

// `exportComponents` for Remote is not in a released exporter yet, so this resolves from Maven
// local: publish yschimke/compose-ui-builder's branch with `:ui-builder-export:publishToMavenLocal`
// (`-PuidExportVersion=` picks another).
repositories {
  mavenLocal()
  mavenCentral()
  google()
}

val generator by sourceSets.creating

dependencies {
  "generatorImplementation"(
    "ee.schimke.composeai:compose-preview-ui-builder-export-jvm:" +
      providers.gradleProperty("uidExportVersion").getOrElse("3.110.1-SNAPSHOT")
  )
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
