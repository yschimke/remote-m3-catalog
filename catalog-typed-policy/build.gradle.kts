plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktfmt)
}

kotlin { jvmToolchain(21) }

ktfmt { googleStyle() }

dependencies {
  implementation(platform(libs.composeai.contracts.bom))
  implementation("ee.schimke.composeai:component-catalog-protocol")
  implementation("ee.schimke.composeai:ui-builder-protocol")
  testImplementation(kotlin("test"))
}

val catalogRoot = layout.projectDirectory.dir("..")
val generatePolicies =
  tasks.register<JavaExec>("generatePolicies") {
    group = "code generation"
    description = "Generate parallel UI-builder policies from authored Kotlin."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ee.schimke.catalog.typed.GeneratePoliciesKt")
    val output = layout.buildDirectory.dir("catalog")
    outputs.dir(output)
    argumentProviders.add(CommandLineArgumentProvider { listOf(output.get().asFile.absolutePath) })
  }

tasks.test {
  inputs.files(catalogRoot.file("remote-catalog/ui-builder.policy.json"))
  systemProperty("catalogRoot", catalogRoot.asFile.absolutePath)
}
