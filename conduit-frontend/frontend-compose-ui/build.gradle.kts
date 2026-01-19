/**
 * The pure UI module in Compose Multiplatform.
 *
 * All navigation, state, routing, etc. should go to the [frontend-decompose-logic] module.
 */
plugins {
  id("my.kmp-frontend-library")
  alias(libs.plugins.kotlinCompose)
  alias(libs.plugins.compose)
}

android {
  namespace = "mikufan.cx.conduit.frontend.ui"
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      implementation(project(":frontend-decompose-logic"))

      implementation(libs.dev.frontend.compose.runtime)
      implementation(libs.dev.frontend.compose.ui)
      implementation(libs.dev.frontend.compose.foundation)
      implementation(libs.dev.frontend.compose.material3)
      implementation(libs.dev.frontend.compose.material3AdaptiveNavigationSuite)
      implementation(libs.dev.frontend.compose.adaptive)
      implementation(libs.dev.frontend.compose.adaptiveLayout)
      implementation(libs.dev.frontend.compose.adaptiveNavigation)
      implementation(libs.dev.frontend.compose.materialIconsCore)
      implementation(libs.dev.frontend.compose.resources)
      implementation(libs.dev.frontend.compose.uiToolingPreview)

      implementation(libs.dev.frontend.decomposeCompose)
      implementation(libs.dev.frontend.decomposeComposeExperimental)
      implementation("io.insert-koin:koin-compose")

      implementation(libs.dev.frontend.coil.compose)
      implementation(libs.dev.frontend.coil.ktor3)
      implementation(libs.dev.frontend.coil.cacheControl)
      implementation(libs.dev.frontend.coil.svg)

      implementation(libs.dev.frontend.markdownRenderer)
      implementation(libs.dev.frontend.markdownRenderer.m3)
      implementation(libs.dev.frontend.markdownRenderer.coil3)
      implementation(libs.dev.frontend.markdownRenderer.code)

      implementation(libs.dev.datetime)

    }
    commonJvmMain.dependencies {
      implementation(libs.dev.frontend.compose.uiTooling)
    }
    jvmMain.dependencies {
      implementation(libs.dev.frontend.compose.desktopCommon)
    }
    androidMain.dependencies {
      implementation(libs.dev.frontend.androidx.activityCompose)
      implementation("io.insert-koin:koin-androidx-compose")
    }
  }
}

compose.resources {
  packageOfResClass = "mikufan.cx.conduit.frontend.ui.resources"
}

composeCompiler {
  // string skipping mode is now enabled by default since Kotlin 2.0.20
  stabilityConfigurationFiles.addAll(layout.projectDirectory.file("compose-stability.txt"))
  reportsDestination = layout.buildDirectory.dir("compose-reports")
  metricsDestination = layout.buildDirectory.dir("compose-reports")
}
