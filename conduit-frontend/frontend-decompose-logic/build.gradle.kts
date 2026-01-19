/**
 * The core frontend business logic and navigation module with multiplatform support,
 * powered by Decompose and MviKotlin.
 *
 * Must not contain any Compose Multiplatform related dependencies.
 * Failing to do so will break the WASM target unit tests.
 *
 * Thanks to Decompose, you can plug in any UI framework you want,
 * by simply importing this gradle module.
 */
plugins {
  id("my.kmp-frontend-library")
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktorfit)
}

ktorfit {
  compilerPluginVersion.set("2.3.3")
}

kotlin {
  androidLibrary {
    namespace = "mikufan.cx.conduit.frontend.logic"
  }

  sourceSets {
    commonMain.dependencies {
      // add dependencies that are specific for this decompose logic module
      // dependencies used in both this module and compose ui module are extracted into the precompiled script plugin
      implementation("mikufan.cx.conduit:conduit-common")

      implementation(libs.dev.datetime)
      implementation(libs.dev.frontend.mvikotlin)
      implementation(libs.dev.frontend.mvikotlinMain)
      implementation(libs.dev.frontend.mvikotlinCoroutines)
      implementation(libs.dev.frontend.mvikotlinLogging)

      implementation(libs.dev.frontend.kstore)
      implementation(dependencies.platform(libs.dev.frontend.ktorBom))
      implementation("io.ktor:ktor-client-core")
      implementation("io.ktor:ktor-client-content-negotiation")
      implementation("io.ktor:ktor-serialization-kotlinx-json")
      implementation("io.ktor:ktor-client-logging")
      implementation(libs.dev.frontend.ktorfitLibLight)
      implementation(libs.dev.frontend.ktorfitConverter)
    }

    // and platform specific dependencies only used in this module

    commonJvmMain.dependencies {
      implementation(libs.dev.frontend.kstore.file)
    }

    webMain.dependencies {
      implementation(libs.dev.frontend.kstore.storage)
    }

    androidMain.dependencies {
    }

    jvmMain.dependencies {
    }

    jsMain.dependencies {
    }

    wasmJsMain.dependencies {
      implementation(libs.dev.frontend.kotlinxBrowser) // needed by kstore
    }

    iosMain.dependencies {
      implementation(libs.dev.frontend.kstore.file)
    }
  }
}
