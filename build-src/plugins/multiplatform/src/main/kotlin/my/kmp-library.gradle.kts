package my

import my.util.Libs
import my.util.Versions
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin

/**
 * Common Kotlin Multiplatform setup for both common and frontend modules.
 */
plugins {
  kotlin("multiplatform")
  id("com.android.kotlin.multiplatform.library")
  kotlin("plugin.serialization")
}

kotlin {
  compilerOptions {
    optIn.add("kotlin.time.ExperimentalTime")
  }
  androidLibrary {
    compileSdk = Versions.AndroidCompileSdk
    minSdk = Versions.AndroidMinSdk
    // namespace will be set in consumer modules
  }
  jvm()
  js(IR) {
    browser {
      testTask {
        useKarma {
          if (System.getenv("CI") == "true") {
            useChromeHeadlessNoSandbox()
          } else {
            // for developers, please use your own browsers in convenience
            useChromiumHeadless()
          }
        }
      }
    }
    // added for experimenting js export, not needed for consuming in other modules
    binaries.executable()
  }
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    // unsupported libraries:
    // - io.github.theapache64:rebugger:1.0.0-rc03
    browser {
      testTask {
        useKarma {
          if (System.getenv("CI") == "true") {
            useChromeHeadlessNoSandbox()
          } else {
            // for developers, please use your own browsers in convenience
            useChromiumHeadless()
          }
        }
      }
    }
    binaries.executable()
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "shared" // should we set same baseName for all gradle modules?
      isStatic = true
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      implementation(kotlin("stdlib"))
      implementation(Libs.SerializationJson)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
    }

    val commonJvmMain by creating {
      dependsOn(commonMain.get())
    }
    jvmMain.get().dependsOn(commonJvmMain)
    androidMain.get().dependsOn(commonJvmMain)
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(Versions.Java)
  }
}

// convenient way to automatically update yarn.lock if dep changes
plugins.withType<YarnPlugin> {
  the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
    yarnLockAutoReplace = true
  }
}
