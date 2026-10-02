plugins {
  id("org.jetbrains.kotlin.multiplatform")
  id("com.android.kotlin.multiplatform.library")
  alias(libs.plugins.compose.multiplatform)
  id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
  android {
    namespace = "androidx.compose.remote.foundation"
    compileSdk = 37
    minSdk = 29
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
      }
    }
  }
  // Java 17 bytecode, like remote-core and the android target. Unset, the JVM classes took the JDK
  // the publish job runs on (21), and a Java 17 consumer failed at its first call with
  // UnsupportedClassVersionError rather than at dependency resolution.
  jvm { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs { browser() }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(project(":vendor:remote-creation-compose"))
        @Suppress("DEPRECATION") api(compose.runtime)
        @Suppress("DEPRECATION") api(compose.ui)
        @Suppress("DEPRECATION") api(compose.foundation)
      }
    }
    val jvmAndAndroidMain by creating {
      dependsOn(commonMain)
    }
    val androidMain by getting { dependsOn(jvmAndAndroidMain) }
    val jvmMain by getting { dependsOn(jvmAndAndroidMain) }
  }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(17) }
