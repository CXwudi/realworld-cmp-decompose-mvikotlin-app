/**
 * The core frontend business logic and navigation module with multiplatform support,
 * powered by MVIKotlin and native AndroidX/CMP ViewModels.
 *
 * Must not contain any Compose Multiplatform related dependencies.
 * Failing to do so will break the WASM target unit tests.
 */
plugins {
  id("my.kmp-frontend-library")
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktorfit)
}

ktorfit {
  // The auto version resolution doesn't work for us. Reason unknown
  compilerPluginVersion.set(libs.versions.ktorfit.compilerPlugin)
}

kotlin {
  android {
    namespace = "mikufan.cx.conduit.frontend.logic"
  }

  sourceSets {
    commonMain.dependencies {
      // add dependencies that are specific for this frontend logic module
      // dependencies used in both this module and compose ui module are extracted into the precompiled script plugin
      implementation("mikufan.cx.conduit:conduit-common")

      implementation(libs.dev.datetime)
      implementation(libs.dev.frontend.mvikotlin)
      implementation(libs.dev.frontend.mvikotlinMain)
      implementation(libs.dev.frontend.mvikotlinCoroutines)
      implementation(libs.dev.frontend.mvikotlinLogging)

      implementation(libs.dev.frontend.androidx.lifecycle.common)
      implementation(libs.dev.frontend.androidx.lifecycle.viewmodel)
      implementation(libs.dev.frontend.androidx.lifecycle.viewmodelSavedstate)
      implementation(libs.dev.frontend.androidx.savedstate)

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

    androidHostTest.dependencies {
      implementation(libs.dev.frontend.test.robolectric)
    }
  }
}
