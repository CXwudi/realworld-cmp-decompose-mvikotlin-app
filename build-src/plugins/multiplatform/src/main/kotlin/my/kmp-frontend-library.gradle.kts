package my

import dev.mokkery.MockMode
import my.util.Libs

/**
 * Common Kotlin Multiplatform setup for frontend modules,
 * built on top of [my.kmp-library] with frontend specific common dependencies.
 *
 * Spcifically, it adds Coroutines, Decompose, Ktor, Koin, and Kotlin Logging.
 *
 * Must not contain any Compose Multiplatform related dependencies.
 * Failing to do so will break the WASM target unit tests for non-compose gradle modules.
 */
plugins {
  id("my.kmp-library")
  id("dev.mokkery")
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(project.dependencies.platform(Libs.CoroutinesBom))
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
      implementation(Libs.EssentyLifecycleCoroutines)
      implementation(Libs.Decompose)
      implementation(Libs.KotlinLogging)
      implementation(project.dependencies.platform(Libs.KoinBom))
      implementation("io.insert-koin:koin-core")
      implementation(project.dependencies.platform(Libs.KtorBom))
      implementation("io.ktor:ktor-client-core")
    }

    commonTest.dependencies {
      implementation("io.insert-koin:koin-test")
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    }

    val commonJvmMain by getting {
      dependencies {
        implementation("io.ktor:ktor-client-okhttp")
      }
    }

    webMain.dependencies {
      implementation("io.ktor:ktor-client-js")
    }

    androidMain.dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
      implementation(Libs.AndroidXCoreKtx)
      implementation(Libs.AndroidXLifecycleKtx)
      implementation(Libs.AndroidXAppCompat)
      implementation(Libs.Slf4jAndroid)
    }

    jvmMain.dependencies {
      implementation(Libs.Logback)
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
    }

    iosMain.dependencies {
      implementation("io.ktor:ktor-client-darwin")
    }
  }
}

mokkery {
  defaultMockMode = MockMode.autoUnit
}