plugins { id("org.jetbrains.kotlin.jvm") }

// The typed-export pilot: Kotlin adapter definitions on the REAL Remote Compose composables, each
// with the `exportProjection` compose-ui-builder's generic `compose-component-records` export
// lowers a design through, published as a hidden parallel `remote-m3-typed-pilot` pair. Only
// included under `-PtypedRemoteCatalog=true` (settings.gradle.kts); production discovery,
// rendering and delivery never read it. docs/design/TYPED_REMOTE_PILOT.md.

// The two releases the pilot is written against, pinned to the release tags
// (compose-ai-tools v2.52.0 = 9310fba0, compose-ui-builder v3.109.0 = 34701265). Deliberately NOT
// the catalog's Renovate-owned `composePreviewPlugin` / `composePreviewServer`: the projections are
// written against these APIs, and a pilot moving with production would stop measuring anything.
val pilotTools = "2.70.7"
val pilotBuilder = "3.121.0"

kotlin {
  jvmToolchain(21)
  compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

// The authored production policy is the base the pilot keeps its builtins, menu and templates from.
sourceSets.named("main") {
  kotlin.srcDir(rootProject.layout.projectDirectory.dir("catalog-typed-policy/src/main/kotlin"))
}

dependencies {
  // The typed adapter API (`TypedComponentAdapter`, `ExportProjection`, `ScreenGenerator`) and the
  // library-metadata discovery the real records are read with, at the compose-ai-tools release
  // the pilot is written against.
  implementation(platform(libs.composeai.contracts.bom))
  implementation(platform("ee.schimke.composeai:compose-ai-tools-bom:$pilotTools"))
  implementation("ee.schimke.composeai:screen-model")
  implementation("ee.schimke.composeai:preview-discovery")
  implementation("ee.schimke.composeai:ui-builder-protocol")
  implementation("ee.schimke.composeai:component-catalog-protocol")
  implementation(libs.kotlinx.serialization.json)
  // The real composables, read from their metadata on this classpath — never loaded or run. The
  // vendored CMP port publishes the same `androidx.compose.remote` / `androidx.wear.compose.remote`
  // API the Android export calls.
  runtimeOnly(project(":vendor:remote-material3"))
  runtimeOnly(project(":vendor:remote-creation-compose"))

  testImplementation(kotlin("test"))
  // The builder's own export, so the pilot pair's projections are exported end to end the way the
  // editor exports them: `ScreenExportGate` for the pilot, `WearWidgetCodeExporter` for production.
  testImplementation("ee.schimke.composeai:compose-preview-ui-builder-export:$pilotBuilder")
}

val catalogRoot = rootProject.layout.projectDirectory
val pilotOutput = layout.buildDirectory.dir("catalog")

// The pilot pair from production's discovery of `:remote-catalog`, which this task deliberately
// does not depend on: discovery compiles the Android module, and the export tests above need none
// of it. Run `:remote-catalog:composePreviewDiscover` first
// (scripts/validate-typed-remote-pilot.sh).
val generateTypedPilot by
  tasks.registering(JavaExec::class) {
    group = "code generation"
    description = "Generate the hidden remote-m3-typed-pilot catalog pair."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ee.schimke.wearm3catalog.remote.typed.GenerateTypedPilotKt")
    inputs.file(catalogRoot.file("remote-catalog/build/compose-previews/components.json"))
    inputs.files(fileTree(catalogRoot.dir("remote-catalog/ui-builder/designs")))
    outputs.dir(pilotOutput)
    val root = catalogRoot.asFile.absolutePath
    val output = pilotOutput
    argumentProviders.add(
      CommandLineArgumentProvider { listOf(root, output.get().asFile.absolutePath) }
    )
  }
