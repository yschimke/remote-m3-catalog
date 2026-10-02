plugins { id("org.jetbrains.kotlin.multiplatform") }

kotlin {
  // Java 17 bytecode, like remote-core and the android target. Unset, the JVM classes took the JDK
  // the publish job runs on (21), and a Java 17 consumer failed at its first call with
  // UnsupportedClassVersionError rather than at dependency resolution.
  jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    nodejs()
  }

  sourceSets {
    commonTest.dependencies { implementation(kotlin("test")) }
    jvmTest.dependencies { implementation(project(":vendor:remote-core")) }
  }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
