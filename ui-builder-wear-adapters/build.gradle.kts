import javax.inject.Inject

/**
 * wear-m3-catalog's `ui-builder-wear-adapters` sources at one commit, sparse-fetched.
 *
 * The visual editor draws every remote-m3 component with its Wear Compose stand-in
 * (docs/design/REMOTE_M3_UI_BUILDER.md), so those stand-ins are wear-m3-catalog's adapters, not a
 * copy of them: a copy drifted, and remote-m3's canvas lost the button, icon-button and progress
 * colours wear-m3 draws. The commit is the one the parallel gate already pins,
 * `.github/ci/wear-m3-catalog-ref`, so an adapter change lands in wear-m3-catalog first and reaches
 * this canvas with the pin bump, as an id rename does.
 *
 * Fetched by the build rather than by a workflow step, because the renderer runtime is built inside
 * compose-ai-tools' reusable workflow, which checks out only this repository.
 */
abstract class FetchWearAdapterSources : DefaultTask() {
  @get:Input abstract val ref: Property<String>

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @get:Inject abstract val exec: ExecOperations

  @TaskAction
  fun fetch() {
    val dir = outputDirectory.get().asFile
    dir.deleteRecursively()
    dir.mkdirs()
    fun git(vararg args: String) {
      exec.exec {
        workingDir = dir
        commandLine("git", *args)
      }
    }
    git("init", "--quiet")
    git("remote", "add", "origin", "https://github.com/yschimke/wear-m3-catalog.git")
    git("config", "core.sparseCheckout", "true")
    dir
      .resolve(".git/info/sparse-checkout")
      .writeText(
        "/ui-builder-wear-adapters/src/commonMain/\n/ui-builder-wear-adapters/src/wasmJsMain/\n"
      )
    git("fetch", "--quiet", "--depth=1", "--filter=blob:none", "origin", ref.get())
    git("checkout", "--quiet", "--detach", "FETCH_HEAD")
  }
}

plugins {
  id("org.jetbrains.kotlin.multiplatform")
  alias(libs.plugins.compose.multiplatform)
  id("org.jetbrains.kotlin.plugin.compose")
}

val wearM3CatalogRef =
  rootProject.layout.projectDirectory
    .file(".github/ci/wear-m3-catalog-ref")
    .asFile
    .readText()
    .trim()

val fetchWearAdapterSources =
  tasks.register<FetchWearAdapterSources>("fetchWearAdapterSources") {
    ref.set(wearM3CatalogRef)
    outputDirectory.set(layout.buildDirectory.dir("wear-m3-catalog"))
  }

// `-PwearM3CatalogDir=../wear-m3-catalog` compiles against a local checkout instead, to try an
// adapter change before it lands there. It is then that checkout's sources, whatever its commit.
val localWearM3Catalog: File? =
  providers.gradleProperty("wearM3CatalogDir").orNull?.let { rootProject.file(it) }

// A fixed path carrying the fetch as its producer, not the task's output provider: the ktfmt plugin
// and IDE sync read source directories while the build is still being configured.
fun wearAdapterSources(sourceSet: String) =
  files(
      (localWearM3Catalog ?: layout.buildDirectory.dir("wear-m3-catalog").get().asFile).resolve(
        "ui-builder-wear-adapters/src/$sourceSet/kotlin"
      )
    )
    .apply { if (localWearM3Catalog == null) builtBy(fetchWearAdapterSources) }

// The fetched sources are wear-m3-catalog's to format. The one file this module owns is checked by
// the root `ktfmtCheckUiBuilderRendererSources`, which reads `ui-builder-wear-adapters/src`.
tasks.matching { it.name.startsWith("ktfmt") }.configureEach { enabled = false }

kotlin {
  jvm()

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }

  sourceSets {
    commonMain {
      kotlin.srcDir(wearAdapterSources("commonMain"))
      dependencies {
        api(libs.composeai.ui.builder.renderer.sdk.source)
        implementation(libs.wearcmp.compose.material3)
        @Suppress("DEPRECATION") implementation(compose.foundation)
        @Suppress("DEPRECATION") implementation(compose.runtime)
        @Suppress("DEPRECATION") implementation(compose.ui)
      }
    }
    wasmJsMain {
      kotlin.srcDir(wearAdapterSources("wasmJsMain"))
    }
  }
}
