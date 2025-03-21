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
  id("com.android.library")
  kotlin("plugin.serialization")
}

kotlin {
  androidTarget()
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

    val commonWebMain by creating {
      dependsOn(commonMain.get())
    }
    jsMain.get().dependsOn(commonWebMain)
    wasmJsMain.get().dependsOn(commonWebMain)
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(Versions.Java)
  }
}

android {
  compileSdk = 35
  defaultConfig {
    minSdk = 26
  }
  // setting targetSdk on per with compileSdk has great benefits, such as layout inspector
  testOptions {
    targetSdk = 35
  }
  lint {
    targetSdk = 35
  }
  // jvm version is covered by java toolchain above
}

// convenient way to automatically update yarn.lock if dep changes
plugins.withType<YarnPlugin> {
  the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
    yarnLockAutoReplace = true
  }
}
